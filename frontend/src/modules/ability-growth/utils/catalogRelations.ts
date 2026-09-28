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
    return names.some((name) => name.length >= 5 && nodeName.length >= 5 && (nodeName.includes(name) || name.includes(nodeName)))
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
  const sameStage = matched.filter((entry) => entry.stageId === current.stageId)
  const previousSibling = sameStage.filter((entry) => entry.order < current.order).at(-1)
  const nextSibling = sameStage.find((entry) => entry.order > current.order)
  const previousStage = matched.filter((entry) => entry.stageIndex < current.stageIndex).at(-1)
  const nextStage = matched.find((entry) => entry.stageIndex > current.stageIndex)
  const relations: SkillRelation[] = []

  if (previousSibling) {
    relations.push({
      from: previousSibling.node.id,
      to: currentNodeId,
      type: 'related',
      confidence: 0.72,
    })
  }
  if (nextSibling) {
    relations.push({
      from: currentNodeId,
      to: nextSibling.node.id,
      type: 'related',
      confidence: 0.72,
    })
  }
  if (previousStage) {
    relations.push({
      from: previousStage.node.id,
      to: currentNodeId,
      type: 'prerequisite',
      confidence: 0.86,
    })
  }
  if (nextStage) {
    relations.push({
      from: currentNodeId,
      to: nextStage.node.id,
      type: 'prerequisite',
      confidence: 0.86,
    })
  }

  return relations
}
