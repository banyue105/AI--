import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { abilityService } from '../services/abilityService'
import type { AbilityGraphData, GrowthPathStep, ParseResult, SkillNode } from '../types'
import { findFreeNodePosition, resolveNodeOverlaps } from '../utils/graphLayout'

export const useAbilityStore = defineStore('ability-growth', () => {
  const graph = ref<AbilityGraphData | null>(null)
  const path = ref<GrowthPathStep[]>([])
  const parseResult = ref<ParseResult | null>(null)
  const selectedId = ref<string | null>(null)
  const loading = ref(false)
  const parsing = ref(false)
  const saving = ref(false)
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
      selectedId.value = graph.value.nodes[0]?.id ?? null
      path.value = await abilityService.generatePath(graph.value)
    } catch (cause) {
      error.value = cause instanceof Error ? cause.message : '能力数据加载失败'
    } finally {
      loading.value = false
    }
  }

  async function persistNode(node: SkillNode): Promise<string> {
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
    selectedId.value = graph.value.nodes.find(
      (item) => item.id === positionedNode.id || item.name.toLowerCase() === positionedNode.name.toLowerCase(),
    )?.id ?? null
    return selectedId.value ?? positionedNode.id
  }

  async function refreshSavedPath() {
    if (!graph.value) return
    path.value = []
    try {
      path.value = await abilityService.generatePath(graph.value)
    } catch (cause) {
      error.value = `数据已保存，但成长路径刷新失败：${messageOf(cause)}`
    }
  }

  async function saveNode(node: SkillNode): Promise<boolean> {
    if (saving.value) return false
    saving.value = true
    error.value = ''
    notice.value = ''
    try {
      await persistNode(node)
      await refreshSavedPath()
      if (!error.value) flash(graph.value?.source === 'api' ? '能力节点已保存到后端' : '能力节点已保存到本地演示')
      return true
    } catch (cause) {
      error.value = `能力节点未保存：${messageOf(cause)}`
      return false
    } finally {
      saving.value = false
    }
  }

  async function parse(input: string) {
    parsing.value = true
    parseResult.value = null
    error.value = ''
    notice.value = ''
    try {
      parseResult.value = await abilityService.parseInput(input)
    } catch (cause) {
      error.value = `能力解析失败：${messageOf(cause)}`
    } finally {
      parsing.value = false
    }
  }

  async function acceptSuggestions() {
    if (!parseResult.value || !graph.value || saving.value) return
    saving.value = true
    error.value = ''
    notice.value = ''
    try {
      const suggestions = parseResult.value
      const savedIds = new Map<string, string>()
      for (const node of suggestions.suggestedNodes) {
        const existing = graph.value!.nodes.find((item) => item.name.toLowerCase() === node.name.toLowerCase())
        const savedId = await persistNode(existing ? {
            ...existing,
            level: Math.max(existing.level, node.level) as SkillNode['level'],
            status: existing.status === 'target' ? 'target' : Math.max(existing.level, node.level) >= 2 ? 'mastered' : 'developing',
          } : node)
        savedIds.set(node.id, savedId)
      }
      for (const relation of suggestions.suggestedRelations) {
        const savedGraph = await abilityService.addRelation({
          ...relation,
          from: savedIds.get(relation.from) ?? relation.from,
          to: savedIds.get(relation.to) ?? relation.to,
        })
        graph.value = { ...savedGraph, nodes: resolveNodeOverlaps(savedGraph.nodes) }
      }
      parseResult.value = null
      await refreshSavedPath()
      if (!error.value) flash(graph.value?.source === 'api' ? '候选能力与关系已保存到后端' : '候选能力与关系已保存到本地演示')
    } catch (cause) {
      error.value = `候选信息尚未全部保存，请重试：${messageOf(cause)}`
    } finally {
      saving.value = false
    }
  }

  async function regeneratePath() {
    if (!graph.value) return
    error.value = ''
    notice.value = ''
    try {
      path.value = await abilityService.generatePath(graph.value)
      flash('成长路径已重新生成')
    } catch (cause) {
      error.value = `成长路径生成失败：${messageOf(cause)}`
    }
  }

  function messageOf(cause: unknown) {
    return cause instanceof Error ? cause.message : '请稍后重试。'
  }

  function flash(message: string) {
    notice.value = message
    globalThis.setTimeout(() => (notice.value = ''), 2200)
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
    saving,
    error,
    notice,
    load,
    saveNode,
    parse,
    acceptSuggestions,
    regeneratePath,
  }
})
