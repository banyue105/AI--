import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { abilityService } from '../services/abilityService'
import type { AbilityGraphData, GrowthPathStep, ParseResult, SkillNode, SkillRelation } from '../types'
import { findFreeNodePosition, resolveNodeOverlaps } from '../utils/graphLayout'

export const useAbilityStore = defineStore('ability-growth', () => {
  const graph = ref<AbilityGraphData | null>(null)
  const path = ref<GrowthPathStep[]>([])
  const parseResult = ref<ParseResult | null>(null)
  const selectedId = ref<string | null>(null)
  const loading = ref(false)
  const parsing = ref(false)
  const error = ref('')
  const notice = ref('')

  const selectedNode = computed(() => graph.value?.nodes.find((node) => node.id === selectedId.value) ?? null)
  const progress = computed(() => {
    if (!graph.value?.nodes.length) return 0
    const score = graph.value.nodes.reduce((sum, node) => sum + node.level, 0)
    return Math.round((score / (graph.value.nodes.length * 4)) * 100)
  })
  const evidenceForSelected = computed(() => {
    if (!graph.value || !selectedNode.value) return []
    return graph.value.evidence.filter((item) => selectedNode.value?.evidenceIds.includes(item.id))
  })

  async function load() {
    loading.value = true
    error.value = ''
    try {
      const loadedGraph = await abilityService.getGraph()
      graph.value = { ...loadedGraph, nodes: resolveNodeOverlaps(loadedGraph.nodes) }
      path.value = await abilityService.generatePath(graph.value)
      selectedId.value = graph.value.nodes[0]?.id ?? null
    } catch (cause) {
      error.value = cause instanceof Error ? cause.message : '能力数据加载失败'
    } finally {
      loading.value = false
    }
  }

  async function saveNode(node: SkillNode) {
    const existing = graph.value?.nodes.find(
      (item) => item.id === node.id || item.name.toLowerCase() === node.name.toLowerCase(),
    )
    const positionedNode = existing
      ? { ...node, id: existing.id, x: existing.x, y: existing.y }
      : graph.value
        ? { ...node, ...findFreeNodePosition(graph.value.nodes) }
        : node
    const savedGraph = await abilityService.saveNode(positionedNode)
    graph.value = { ...savedGraph, nodes: resolveNodeOverlaps(savedGraph.nodes) }
    path.value = await abilityService.generatePath(graph.value)
    selectedId.value = graph.value.nodes.find(
      (item) => item.id === positionedNode.id || item.name.toLowerCase() === positionedNode.name.toLowerCase(),
    )?.id ?? null
    flash('能力节点已保存')
  }

  async function deleteNode(nodeId: string) {
    if (!graph.value?.nodes.some((node) => node.id === nodeId)) return
    const savedGraph = await abilityService.deleteNode(nodeId)
    graph.value = { ...savedGraph, nodes: resolveNodeOverlaps(savedGraph.nodes) }
    path.value = await abilityService.generatePath(graph.value)
    selectedId.value = graph.value.nodes[0]?.id ?? null
    flash('能力节点已删除')
  }
  async function saveRelations(relations: SkillRelation[]) {
    if (!graph.value || !relations.length) return
    let savedGraph = graph.value
    for (const relation of relations) savedGraph = await abilityService.addRelation(relation)
    graph.value = { ...savedGraph, nodes: resolveNodeOverlaps(savedGraph.nodes) }
    path.value = await abilityService.generatePath(graph.value)
    flash(`已建立 ${relations.length} 条方向关联`)
  }

  async function parse(input: string) {
    parsing.value = true
    parseResult.value = null
    try {
      parseResult.value = await abilityService.parseInput(input)
    } finally {
      parsing.value = false
    }
  }

  async function acceptSuggestions() {
    if (!parseResult.value || !graph.value) return
    for (const node of parseResult.value.suggestedNodes) {
      const existing = graph.value.nodes.find((item) => item.name.toLowerCase() === node.name.toLowerCase())
      if (existing) {
        await saveNode({
          ...existing,
          level: Math.max(existing.level, node.level) as SkillNode['level'],
          status: existing.status === 'target' ? 'target' : Math.max(existing.level, node.level) >= 2 ? 'mastered' : 'developing',
        })
      } else {
        await saveNode(node)
      }
    }
    parseResult.value = null
    flash('候选能力已加入图谱')
  }

  async function regeneratePath() {
    if (!graph.value) return
    path.value = await abilityService.generatePath(graph.value)
    flash('成长路径已重新生成')
  }

  function flash(message: string) {
    notice.value = message
    window.setTimeout(() => (notice.value = ''), 2200)
  }

  return {
    graph,
    path,
    parseResult,
    selectedId,
    selectedNode,
    evidenceForSelected,
    progress,
    loading,
    parsing,
    error,
    notice,
    load,
    saveNode,
    deleteNode,
    saveRelations,
    parse,
    acceptSuggestions,
    regeneratePath,
  }
})
