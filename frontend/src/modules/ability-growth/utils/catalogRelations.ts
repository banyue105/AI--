import type { KnowledgeStackItem, KnowledgeStackStage, KnowledgeTrack } from '../services/knowledgeCatalogService'
import type { SkillNode, SkillRelation } from '../types'

interface OrderedStackItem {
  item: KnowledgeStackItem
  stageId: string
  stageIndex: number
  order: number
}

function normalizeName(value: string) {
  return value.trim().toLocaleLowerCase().replace(/[\s./_-]+/g, '')
}

function findMatchingNode(item: KnowledgeStackItem, nodes: SkillNode[]) {
  const names = [item.name, ...item.aliases].map(normalizeName).filter(Boolean)
  const exact = nodes.find((node) => names.includes(normalizeName(node.name)))
  if (exact) return exact

  return nodes.find((node) => {
    const nodeName = normalizeName(node.name)
    return names.some((name) => name.length >= 3 && (nodeName.includes(name) || name.includes(nodeName)))
  })
}

function relationType(from: OrderedStackItem, to: OrderedStackItem): SkillRelation['type'] {
  return from.stageId === to.stageId ? 'related' : 'prerequisite'
}

export function buildCatalogRelations(
  track: KnowledgeTrack,
  stage: KnowledgeStackStage,
  currentItem: KnowledgeStackItem,
  currentNodeId: string,
  existingNodes: SkillNode[],
): SkillRelation[] {
  const orderedItems: OrderedStackItem[] = track.stages.flatMap((trackStage, stageIndex) =>
    trackStage.items.map((item, itemIndex) => ({
      item,
      stageId: trackStage.id,
      stageIndex,
      order: stageIndex * 1000 + itemIndex,
    })),
  )
  const current = orderedItems.find(
    (entry) => entry.stageId === stage.id && entry.item.id === currentItem.id,
  )
  if (!current) return []

  const matched = orderedItems
    .filter((entry) => entry.item.id !== currentItem.id)
    .map((entry) => ({ ...entry, node: findMatchingNode(entry.item, existingNodes) }))
    .filter((entry): entry is OrderedStackItem & { node: SkillNode } => Boolean(entry.node))
  const previous = matched.filter((entry) => entry.order < current.order).at(-1)
  const next = matched.find((entry) => entry.order > current.order)
  const relations: SkillRelation[] = []

  if (previous) {
    relations.push({
      from: previous.node.id,
      to: currentNodeId,
      type: relationType(previous, current),
      confidence: previous.stageIndex === current.stageIndex ? 0.72 : 0.86,
    })
  }
  if (next) {
    relations.push({
      from: currentNodeId,
      to: next.node.id,
      type: relationType(current, next),
      confidence: next.stageIndex === current.stageIndex ? 0.72 : 0.86,
    })
  }

  return relations
}
