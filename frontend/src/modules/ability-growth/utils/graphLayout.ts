import type { SkillNode, SkillRelation } from '../types'
import type { KnowledgeTrack } from '../services/knowledgeCatalogService'

export interface NodePosition {
  x: number
  y: number
}

const NODE_WIDTH = 144
const NODE_HEIGHT = 54
const HORIZONTAL_GAP = 24
const VERTICAL_GAP = 32
const COLUMNS = [48, 248, 450, 650, 850]
const BASE_ROWS = [332, 72, 202]
const EXTRA_ROW_START = 462
const ROW_STEP = 130
const TREE_LEFT = 48
const TREE_TOP = 58
const TREE_COLUMN_GAP = 202
const TREE_ROW_GAP = 82
const TREE_LANE_GAP = 92
const TREE_NODE_HEIGHT = 54

export interface LayoutSkillNode extends SkillNode {
  layoutDepth: number
  layoutLane: number
}

export interface LayoutRelation extends SkillRelation {
  crossDirection: boolean
}

export interface GraphLane {
  id: number
  label: string
  y: number
  height: number
}

export interface AbilityGraphLayout {
  nodes: LayoutSkillNode[]
  relations: LayoutRelation[]
  lanes: GraphLane[]
  columns: Array<{ depth: number; x: number; label: string }>
  width: number
  height: number
}

function normalizedSkillName(value: string) {
  return value.trim().toLocaleLowerCase().replace(/[\s./_-]+/g, '')
}

function catalogMatchesBySkill(nodes: SkillNode[], tracks: KnowledgeTrack[]) {
  const matchesById = new Map<string, Array<{ trackIndex: number; stageIndex: number; itemIndex: number }>>()
  for (const node of nodes) {
    const nodeName = normalizedSkillName(node.name)
    const matches: Array<{ trackIndex: number; stageIndex: number; itemIndex: number }> = []
    tracks.forEach((track, trackIndex) => {
      track.stages.forEach((stage, stageIndex) => stage.items.forEach((item, itemIndex) => {
        const names = [item.name, ...item.aliases].map(normalizedSkillName)
        if (names.some((name) => name === nodeName || (name.length >= 5 && nodeName.length >= 5 && (name.includes(nodeName) || nodeName.includes(name))))) {
          matches.push({ trackIndex, stageIndex, itemIndex })
        }
      }))
    })
    if (matches.length) matchesById.set(node.id, matches)
  }
  return matchesById
}

/**
 * Manual layout for the personal tree. Every preset direction receives one row;
 * catalog stages define its left-to-right order. It is called only from the
 * explicit "重新排布" action.
 */
export function layoutPersonalSkillTree(
  nodes: SkillNode[], relations: SkillRelation[], tracks: KnowledgeTrack[], includeEmptyTracks = false,
): AbilityGraphLayout {
  const nodeById = new Map(nodes.map((node) => [node.id, node]))
  const nodeOrder = new Map(nodes.map((node, index) => [node.id, index]))
  const validRelations = relations.filter((relation) => nodeById.has(relation.from) && nodeById.has(relation.to))
  const catalogMatches = catalogMatchesBySkill(nodes, tracks)
  const adjacency = new Map(nodes.map((node) => [node.id, new Set<string>()]))
  validRelations.forEach((relation) => {
    adjacency.get(relation.from)?.add(relation.to)
    adjacency.get(relation.to)?.add(relation.from)
  })
  const trackByNode = new Map<string, number>()
  nodes.forEach((node) => {
    const candidates = [...new Set((catalogMatches.get(node.id) ?? []).map((match) => match.trackIndex))]
    if (candidates.length === 1) trackByNode.set(node.id, candidates[0])
  })
  nodes.forEach((node) => {
    if (trackByNode.has(node.id)) return
    const candidates = [...new Set((catalogMatches.get(node.id) ?? []).map((match) => match.trackIndex))]
    if (!candidates.length) return
    const scores = candidates.map((candidate) => ({
      candidate,
      score: [...(adjacency.get(node.id) ?? [])].reduce((score, neighbor) => {
        if (trackByNode.get(neighbor) === candidate) return score + 3
        return score + ((catalogMatches.get(neighbor) ?? []).some((match) => match.trackIndex === candidate) ? 1 : 0)
      }, 0),
    }))
    scores.sort((left, right) => right.score - left.score || left.candidate - right.candidate)
    trackByNode.set(node.id, scores[0].candidate)
  })
  nodes.forEach((node) => {
    if (trackByNode.has(node.id)) return
    const neighborTracks = [...(adjacency.get(node.id) ?? [])]
      .map((neighbor) => trackByNode.get(neighbor)).filter((track): track is number => track !== undefined)
    if (neighborTracks.length) {
      const counts = new Map<number, number>()
      neighborTracks.forEach((track) => counts.set(track, (counts.get(track) ?? 0) + 1))
      trackByNode.set(node.id, [...counts.entries()].sort((left, right) => right[1] - left[1] || left[0] - right[0])[0][0])
    }
  })

  const relatedParents = new Map(nodes.map((node) => [node.id, node.id]))
  const findRelated = (id: string): string => {
    const parent = relatedParents.get(id) ?? id
    if (parent === id) return id
    const root = findRelated(parent)
    relatedParents.set(id, root)
    return root
  }
  const unionRelated = (left: string, right: string) => {
    if (trackByNode.get(left) !== trackByNode.get(right)) return
    const leftRoot = findRelated(left)
    const rightRoot = findRelated(right)
    if (leftRoot !== rightRoot) relatedParents.set(rightRoot, leftRoot)
  }
  validRelations.filter((relation) => relation.type === 'related').forEach((relation) => unionRelated(relation.from, relation.to))
  const groups = new Map<string, string[]>()
  nodes.forEach((node) => {
    const root = findRelated(node.id)
    const group = groups.get(root) ?? []
    group.push(node.id)
    groups.set(root, group)
  })
  const groupByNode = new Map<string, string>()
  groups.forEach((members, id) => members.forEach((member) => groupByNode.set(member, id)))

  const parents = new Map<string, Set<string>>([...groups.keys()].map((id) => [id, new Set<string>()]))
  validRelations.filter((relation) => relation.type === 'prerequisite').forEach((relation) => {
    const parent = groupByNode.get(relation.from)
    const child = groupByNode.get(relation.to)
    if (parent && child && parent !== child) parents.get(child)?.add(parent)
  })
  const laneByGroup = new Map<string, number>()
  const baseDepth = new Map<string, number>()
  groups.forEach((members, id) => {
    const lane = trackByNode.get(members[0]) ?? tracks.length
    laneByGroup.set(id, lane)
    const stages = members.flatMap((member) => catalogMatches.get(member) ?? [])
      .filter((match) => match.trackIndex === lane).map((match) => match.stageIndex)
    baseDepth.set(id, stages.length ? Math.min(...stages) : 0)
  })
  const depthByGroup = new Map<string, number>()
  const visiting = new Set<string>()
  const getDepth = (id: string): number => {
    const cached = depthByGroup.get(id)
    if (cached !== undefined) return cached
    if (visiting.has(id)) return baseDepth.get(id) ?? 0
    visiting.add(id)
    const lane = laneByGroup.get(id)
    const prerequisiteDepth = Math.max(-1, ...[...(parents.get(id) ?? [])]
      .filter((parent) => laneByGroup.get(parent) === lane).map((parent) => getDepth(parent))) + 1
    const depth = Math.max(baseDepth.get(id) ?? 0, prerequisiteDepth)
    visiting.delete(id)
    depthByGroup.set(id, depth)
    return depth
  }
  groups.forEach((_, id) => getDepth(id))

  const laidOutNodes: LayoutSkillNode[] = []
  const lanes: GraphLane[] = []
  let laneTop = TREE_TOP
  const laneOrder = [...tracks.map((_, index) => index), tracks.length]
  laneOrder.forEach((laneId) => {
    const laneGroups = [...groups.keys()].filter((group) => laneByGroup.get(group) === laneId)
    if (!laneGroups.length && (!includeEmptyTracks || laneId === tracks.length)) return
    const laneIndex = lanes.length
    const componentGroups = laneGroups
      .sort((left, right) => getDepth(left) - getDepth(right)
        || Math.min(...(groups.get(left) ?? []).map((id) => nodeOrder.get(id) ?? 0)) - Math.min(...(groups.get(right) ?? []).map((id) => nodeOrder.get(id) ?? 0)))
    const byDepth = new Map<number, string[]>()
    componentGroups.forEach((group) => {
      const depth = getDepth(group)
      const bucket = byDepth.get(depth) ?? []
      bucket.push(group)
      byDepth.set(depth, bucket)
    })
    const rows = Math.max(1, ...[...byDepth.values()].map((bucket) => bucket.reduce((count, group) => count + (groups.get(group)?.length ?? 0), 0)))
    const laneHeight = (rows - 1) * TREE_ROW_GAP + TREE_NODE_HEIGHT
    const title = laneId < tracks.length ? tracks[laneId].title : '其他技能'
    lanes.push({ id: laneIndex, label: title, y: laneTop - 26, height: laneHeight + 44 })
    byDepth.forEach((bucket, depth) => {
      let row = 0
      bucket.forEach((group) => (groups.get(group) ?? [])
        .slice().sort((left, right) => {
          const leftMatch = (catalogMatches.get(left) ?? []).find((match) => match.trackIndex === laneId)
          const rightMatch = (catalogMatches.get(right) ?? []).find((match) => match.trackIndex === laneId)
          return (leftMatch?.itemIndex ?? 999) - (rightMatch?.itemIndex ?? 999)
            || (nodeOrder.get(left) ?? 0) - (nodeOrder.get(right) ?? 0)
        })
        .forEach((id) => {
          const node = nodeById.get(id)!
          laidOutNodes.push({ ...node, x: TREE_LEFT + depth * TREE_COLUMN_GAP, y: laneTop + row * TREE_ROW_GAP, layoutDepth: depth, layoutLane: laneIndex })
          row += 1
        }))
    })
    laneTop += laneHeight + TREE_LANE_GAP
  })
  const laidOutById = new Map(laidOutNodes.map((node) => [node.id, node]))
  const maxDepth = Math.max(0, ...laidOutNodes.map((node) => node.layoutDepth))
  return {
    nodes: laidOutNodes,
    relations: validRelations.map((relation) => ({ ...relation, crossDirection: laidOutById.get(relation.from)?.layoutLane !== laidOutById.get(relation.to)?.layoutLane })),
    lanes,
    columns: Array.from({ length: maxDepth + 1 }, (_, depth) => ({
      depth, x: TREE_LEFT + depth * TREE_COLUMN_GAP,
      label: depth === 0 ? '基础' : depth === 1 ? '核心能力' : depth === 2 ? '工程交付' : `进阶 ${depth}`,
    })),
    width: Math.max(1040, TREE_LEFT + maxDepth * TREE_COLUMN_GAP + NODE_WIDTH + 48),
    height: Math.max(470, laneTop - TREE_LANE_GAP + 36),
  }
}

function overlaps(a: NodePosition, b: NodePosition) {
  return (
    Math.abs(a.x - b.x) < NODE_WIDTH + HORIZONTAL_GAP &&
    Math.abs(a.y - b.y) < NODE_HEIGHT + VERTICAL_GAP
  )
}

export function findFreeNodePosition(nodes: Array<Pick<SkillNode, 'x' | 'y'>>): NodePosition {
  const rows = [...BASE_ROWS]
  const requiredExtraRows = Math.ceil((nodes.length + 1) / COLUMNS.length) + 1
  for (let index = 0; index < requiredExtraRows; index += 1) {
    rows.push(EXTRA_ROW_START + index * ROW_STEP)
  }

  for (const y of rows) {
    for (const x of COLUMNS) {
      const candidate = { x, y }
      if (!nodes.some((node) => overlaps(candidate, node))) return candidate
    }
  }

  return { x: COLUMNS[0], y: EXTRA_ROW_START + requiredExtraRows * ROW_STEP }
}

export function resolveNodeOverlaps(nodes: SkillNode[]): SkillNode[] {
  const placed: SkillNode[] = []

  for (const node of nodes) {
    const hasValidPosition = Number.isFinite(node.x) && Number.isFinite(node.y)
    const collides = hasValidPosition && placed.some((item) => overlaps(node, item))
    const position = !hasValidPosition || collides ? findFreeNodePosition(placed) : { x: node.x, y: node.y }
    placed.push({ ...node, ...position })
  }

  return placed
}
export function layoutAbilityGraph(nodes: SkillNode[], relations: SkillRelation[]): AbilityGraphLayout {
  const nodeById = new Map(nodes.map((node) => [node.id, node]))
  const nodeOrder = new Map(nodes.map((node, index) => [node.id, index]))
  const validRelations = relations.filter((relation) => nodeById.has(relation.from) && nodeById.has(relation.to))
  const parents = new Map<string, string[]>()
  const related = new Map<string, string[]>()

  for (const node of nodes) {
    parents.set(node.id, [])
    related.set(node.id, [])
  }
  for (const relation of validRelations) {
    if (relation.type === 'prerequisite') parents.get(relation.to)?.push(relation.from)
    else {
      related.get(relation.from)?.push(relation.to)
      related.get(relation.to)?.push(relation.from)
    }
  }

  const depthById = new Map<string, number>()
  const visiting = new Set<string>()
  function getDepth(id: string): number {
    const cached = depthById.get(id)
    if (cached !== undefined) return cached
    if (visiting.has(id)) return 0
    visiting.add(id)
    const depth = Math.max(0, ...(parents.get(id) ?? []).map((parentId) => getDepth(parentId) + 1))
    visiting.delete(id)
    depthById.set(id, depth)
    return depth
  }
  nodes.forEach((node) => getDepth(node.id))

  const sortedNodes = [...nodes].sort((a, b) =>
    (depthById.get(a.id) ?? 0) - (depthById.get(b.id) ?? 0) ||
    a.y - b.y ||
    a.x - b.x ||
    (nodeOrder.get(a.id) ?? 0) - (nodeOrder.get(b.id) ?? 0),
  )
  const laneById = new Map<string, number>()
  let nextLane = 0

  for (const node of sortedNodes) {
    const parentCandidates = (parents.get(node.id) ?? [])
      .filter((id) => laneById.has(id))
      .sort((a, b) => Math.abs((nodeById.get(a)?.y ?? 0) - node.y) - Math.abs((nodeById.get(b)?.y ?? 0) - node.y))
    const relatedCandidate = (related.get(node.id) ?? []).find((id) => laneById.has(id))
    const inheritedId = parentCandidates[0] ?? relatedCandidate
    laneById.set(node.id, inheritedId ? laneById.get(inheritedId)! : nextLane++)
  }

  // A root connected only by a related edge belongs beside that established branch.
  for (const relation of validRelations) {
    if (relation.type !== 'related') continue
    const fromDepth = depthById.get(relation.from) ?? 0
    const toDepth = depthById.get(relation.to) ?? 0
    if (fromDepth === 0 && toDepth > 0) laneById.set(relation.from, laneById.get(relation.to)!)
    if (toDepth === 0 && fromDepth > 0) laneById.set(relation.to, laneById.get(relation.from)!)
  }

  const laneIds = [...new Set(sortedNodes.map((node) => laneById.get(node.id)!))]
  const compactLane = new Map(laneIds.map((id, index) => [id, index]))
  const laneNodes = laneIds.map((laneId) => sortedNodes.filter((node) => laneById.get(node.id) === laneId))
  const lanes: GraphLane[] = []
  const laidOutNodes: LayoutSkillNode[] = []
  let laneTop = TREE_TOP

  laneNodes.forEach((items, laneIndex) => {
    const byDepth = new Map<number, SkillNode[]>()
    for (const node of items) {
      const depth = depthById.get(node.id) ?? 0
      const bucket = byDepth.get(depth) ?? []
      bucket.push(node)
      byDepth.set(depth, bucket)
    }
    const rows = Math.max(1, ...[...byDepth.values()].map((bucket) => bucket.length))
    const laneHeight = (rows - 1) * TREE_ROW_GAP + TREE_NODE_HEIGHT
    const root = [...items].sort((a, b) => (depthById.get(a.id) ?? 0) - (depthById.get(b.id) ?? 0))[0]
    lanes.push({ id: laneIndex, label: `${root?.name ?? '独立技能'}路径`, y: laneTop - 26, height: laneHeight + 44 })

    for (const [depth, bucket] of byDepth) {
      bucket.sort((a, b) => a.y - b.y || (nodeOrder.get(a.id) ?? 0) - (nodeOrder.get(b.id) ?? 0))
      bucket.forEach((node, row) => {
        laidOutNodes.push({
          ...node,
          x: TREE_LEFT + depth * TREE_COLUMN_GAP,
          y: laneTop + row * TREE_ROW_GAP,
          layoutDepth: depth,
          layoutLane: compactLane.get(laneById.get(node.id)!)!,
        })
      })
    }
    laneTop += laneHeight + TREE_LANE_GAP
  })

  const laidOutById = new Map(laidOutNodes.map((node) => [node.id, node]))
  const layoutRelations = validRelations.map((relation) => ({
    ...relation,
    crossDirection: laidOutById.get(relation.from)?.layoutLane !== laidOutById.get(relation.to)?.layoutLane,
  }))
  const maxDepth = Math.max(0, ...laidOutNodes.map((node) => node.layoutDepth))

  return {
    nodes: laidOutNodes,
    relations: layoutRelations,
    lanes,
    columns: Array.from({ length: maxDepth + 1 }, (_, depth) => ({
      depth,
      x: TREE_LEFT + depth * TREE_COLUMN_GAP,
      label: depth === 0 ? '基础' : depth === maxDepth ? '综合应用' : `进阶 ${depth}`,
    })),
    width: Math.max(1040, TREE_LEFT + maxDepth * TREE_COLUMN_GAP + NODE_WIDTH + 48),
    height: Math.max(470, laneTop - TREE_LANE_GAP + 36),
  }
}
