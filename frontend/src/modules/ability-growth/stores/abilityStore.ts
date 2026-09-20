import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { abilityService } from '../services/abilityService'
import type { AbilityGraphData, GrowthPathStep, ParseResult, SkillNode, SkillRelation } from '../types'
import { findFreeNodePosition, resolveNodeOverlaps } from '../utils/graphLayout'

export const useAbilityStore = defineStore('ability-growth', () => {
  const graph = ref<AbilityGraphData | null>(null)
  const path = ref<GrowthPathStep[]>([])
  const pathLoading = ref(false)
  const pathCache = new Map<string, GrowthPathStep[]>()
  const parseResult = ref<ParseResult | null>(null)
  const selectedId = ref<string | null>(null)
  const currentTargetId = ref<string | null>(null)
  const loading = ref(false)
  const parsing = ref(false)
  const error = ref('')
  const notice = ref('')

  const selectedNode = computed(() => graph.value?.nodes.find((node) => node.id === selectedId.value) ?? null)
  const targetScopeIds = computed(() => {
    if (!graph.value) return new Set<string>()
    const targetId = currentTargetId.value ?? graph.value.nodes.find((node) => node.status === 'target')?.id
    if (!targetId) return new Set(graph.value.nodes.map((node) => node.id))
    const scope = new Set<string>([targetId])
    const collect = (id: string) => graph.value?.relations.filter((relation) => relation.to === id).forEach((relation) => {
      if (!scope.has(relation.from)) { scope.add(relation.from); collect(relation.from) }
    })
    collect(targetId)
    return scope
  })
  const progress = computed(() => {
    const nodes = graph.value?.nodes.filter((node) => targetScopeIds.value.has(node.id)) ?? []
    if (!nodes.length) return 0
    return Math.round((nodes.reduce((sum, node) => sum + node.level, 0) / (nodes.length * 4)) * 100)
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
      currentTargetId.value = graph.value.nodes.find((node) => node.status === 'target')?.id ?? null
      await loadPathForTarget(currentTargetId.value, true)
      selectedId.value = null
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
    pathCache.clear()
    selectedId.value = graph.value.nodes.find(
      (item) => item.id === positionedNode.id || item.name.toLowerCase() === positionedNode.name.toLowerCase(),
    )?.id ?? null
    flash('能力节点已保存')
  }

  async function deleteNodes(nodeIds: string[]) {
    if (!graph.value) return
    const existingIds = nodeIds.filter((id) => graph.value?.nodes.some((node) => node.id === id))
    if (!existingIds.length) return
    const savedGraph = await abilityService.deleteNodes(existingIds)
    graph.value = { ...savedGraph, nodes: resolveNodeOverlaps(savedGraph.nodes) }
    pathCache.clear()
    if (!graph.value.nodes.some((node) => node.id === selectedId.value)) selectedId.value = null
    flash(`已删除 ${existingIds.length} 个能力节点`)
  }
  async function deleteNode(nodeId: string) {
    if (!graph.value?.nodes.some((node) => node.id === nodeId)) return
    const savedGraph = await abilityService.deleteNode(nodeId)
    graph.value = { ...savedGraph, nodes: resolveNodeOverlaps(savedGraph.nodes) }
    pathCache.clear()
    selectedId.value = null
    flash('能力节点已删除')
  }
  async function saveRelations(relations: SkillRelation[]) {
    if (!graph.value || !relations.length) return
    let savedGraph = graph.value
    for (const relation of relations) savedGraph = await abilityService.addRelation(relation)
    graph.value = { ...savedGraph, nodes: resolveNodeOverlaps(savedGraph.nodes) }
    pathCache.clear()
    flash(`已建立 ${relations.length} 条方向关联`)
  }

  async function addEvidence(skillId: string, input: { title: string; note: string }) {
    const savedGraph = await abilityService.addEvidence(skillId, input)
    graph.value = { ...savedGraph, nodes: resolveNodeOverlaps(savedGraph.nodes) }
    selectedId.value = skillId
    flash('实践记录已添加')
  }

  async function parse(input: string) {
    parsing.value = true
    parseResult.value = null
    try {
      parseResult.value = await abilityService.parseInput(input, graph.value?.nodes ?? [], graph.value?.relations ?? [])
    } finally {
      parsing.value = false
    }
  }

  async function acceptSuggestions() {
    if (!parseResult.value || !graph.value) return
    const result = parseResult.value
    const resolvedIds = new Map<string, string>()
    const pendingNodes: SkillNode[] = []
    for (const node of result.suggestedNodes) {
      const existing = graph.value.nodes.find((item) => item.name.toLowerCase() === node.name.toLowerCase())
      if (existing) {
        pendingNodes.push({
          ...existing,
          level: Math.max(existing.level, node.level) as SkillNode['level'],
          status: existing.status === 'target' ? 'target' : Math.max(existing.level, node.level) >= 2 ? 'mastered' : 'developing',
        })
      } else {
        pendingNodes.push({ ...node, ...findFreeNodePosition(graph.value.nodes) })
      }
      const saved = existing ?? pendingNodes.at(-1)
      if (saved) resolvedIds.set(node.id, saved.id)
    }
    // Save concurrently and regenerate the path once, instead of once for every selected node.
    await Promise.all(pendingNodes.map((node) => abilityService.saveNode(node)))
    const savedGraph = await abilityService.getGraph()
    graph.value = { ...savedGraph, nodes: resolveNodeOverlaps(savedGraph.nodes) }
    const relations = result.suggestedRelations.flatMap((relation) => {
      const from = resolvedIds.get(relation.from)
        ?? graph.value?.nodes.find((node) => node.id === relation.from || node.name.toLowerCase() === relation.from.toLowerCase())?.id
      const to = resolvedIds.get(relation.to)
        ?? graph.value?.nodes.find((node) => node.id === relation.to || node.name.toLowerCase() === relation.to.toLowerCase())?.id
      if (!from || !to || from === to) return []
      return [{ ...relation, from, to }]
    })
    if (relations.length) {
      await Promise.all(relations.map((relation) => abilityService.addRelation(relation)))
      const savedGraph = await abilityService.getGraph()
      graph.value = { ...savedGraph, nodes: resolveNodeOverlaps(savedGraph.nodes) }
    }
    path.value = await abilityService.generatePath(graph.value, currentTargetId.value)
    parseResult.value = null
    flash('候选能力已加入图谱')
  }

  async function loadPathForTarget(targetId: string | null, force = false) {
    if (!graph.value) return
    const key = targetId ?? '__all__'
    if (!force && pathCache.has(key)) { path.value = pathCache.get(key)!; return }
    pathLoading.value = true
    try {
      const generated = await abilityService.generatePath(graph.value, targetId)
      pathCache.set(key, generated)
      path.value = generated
    } finally { pathLoading.value = false }
  }

  async function regeneratePath() {
    await loadPathForTarget(currentTargetId.value, true)
    flash('成长路径已重新生成')
  }

  function flash(message: string) {
    notice.value = message
    window.setTimeout(() => (notice.value = ''), 2200)
  }

  return {
    graph,
    path,
    pathLoading,
    parseResult,
    selectedId,
    currentTargetId,
    targetScopeIds,
    selectedNode,
    evidenceForSelected,
    progress,
    loading,
    parsing,
    error,
    notice,
    flash,
    load,
    saveNode,
    deleteNode,
    deleteNodes,
    saveRelations,
    addEvidence,
    parse,
    acceptSuggestions,
    regeneratePath,
    setCurrentTarget(id: string) {
      currentTargetId.value = id
      return loadPathForTarget(id)
    },
  }
})
