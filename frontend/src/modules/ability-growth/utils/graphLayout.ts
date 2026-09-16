import type { SkillNode, SkillRelation } from '../types'

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
