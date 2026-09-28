<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import {
  ArrowLeft,
  ArrowRight,
  Bot,
  Check,
  CheckCircle2,
  ChevronDown,
  CircleAlert,
  ClipboardCheck,
  FileCheck2,
  GripVertical,
  Layers3,
  Network,
  Pencil,
  Plus,
  RefreshCw,
  Route,
  Save,
  Sparkles,
  Target,
  Trash2,
  X,
} from 'lucide-vue-next'
import AbilityGraph from './components/AbilityGraph.vue'
import KnowledgeCatalog from './components/KnowledgeCatalog.vue'
import aiService from './services/aiService'
import { useAbilityStore } from './stores/abilityStore'
import { skillAssessmentService, type SkillQuestion } from './services/skillAssessmentService'
import type { GrowthPathStep, SkillLevel, SkillNode, SkillStatus } from './types'
import { buildCatalogRelations } from './utils/catalogRelations'
import {
  knowledgeCatalogService,
  type KnowledgeStackItem,
  type KnowledgeStackStage,
  type KnowledgeTrack,
  type KnowledgeTrackId,
} from './services/knowledgeCatalogService'

const store = useAbilityStore()
const quickInput = ref('我会 Python、Linux，了解 TCP/IP，目标是完成网络服务部署')
const showEditor = ref(false)
const knowledgeTracks = ref<KnowledgeTrack[]>([])
const selectedKnowledgeTrackId = ref<KnowledgeTrackId>('network')
const generatingKnowledge = ref(false)
const knowledgeGenerationError = ref('')
const directionChoicesOpen = ref(false)
const unresolvedDirectionQuery = ref('')
type ProfileState = { id: string; name: string; avatar?: string | null; goals: string[] }
const profileState = ref<ProfileState | null>(null)
const goalSaving = ref(false)
const goalSelectionError = ref('')
const goalMenuOpen = ref(false)
const goalSlideDirection = ref<'left' | 'right'>('right')
const growthGoals = computed(() => (store.graph?.nodes ?? []).filter((node) => node.status === 'target'))
const currentGoalId = computed(() => store.currentTargetId ?? growthGoals.value[0]?.id ?? '')
const targetSkillLabel = computed(() => store.graph?.nodes.find((node) => node.id === currentGoalId.value)?.name ?? '')
const targetScopeNodes = computed(() => store.graph?.nodes.filter((node) => store.targetScopeIds.has(node.id)) ?? [])
type CatalogContext = {
  item: KnowledgeStackItem
  stage: KnowledgeStackStage
  track: KnowledgeTrack
}
const catalogSelection = ref<CatalogContext | null>(null)
const catalogPreview = ref<CatalogContext | null>(null)
const expandedPathSkillId = ref<string | null>(null)
type RelationNeighbor = { id: string | null; name: string; status: SkillStatus; meta?: string }
const graphRelationContext = ref<{
  nodeId: string
  previous: RelationNeighbor[]
  next: RelationNeighbor[]
} | null>(null)
const levelLabels = ['不了解', '了解', '能实践', '熟练', '可指导']
const statusOptions: Array<{ value: SkillStatus; label: string }> = [
  { value: 'mastered', label: '已掌握' },
  { value: 'developing', label: '进行中' },
  { value: 'gap', label: '能力缺口' },
  { value: 'target', label: '目标能力' },
]
const form = reactive({ id: '', name: '', description: '', level: 1 as SkillLevel, status: 'developing' as SkillStatus })

const selectedEvidence = computed(() => store.evidenceForSelected)
const selectedNeighbors = computed<{ previous: RelationNeighbor[]; next: RelationNeighbor[] }>(() => {
  const graph = store.graph
  const selectedId = store.selectedId
  if (!graph || !selectedId) return { previous: [] as RelationNeighbor[], next: [] as RelationNeighbor[] }
  if (graphRelationContext.value?.nodeId === selectedId) return graphRelationContext.value
  const previousIds = new Set<string>()
  const nextIds = new Set<string>()
  graph.relations.forEach((relation) => {
    if (relation.to === selectedId) previousIds.add(relation.from)
    if (relation.from === selectedId) nextIds.add(relation.to)
  })
  return {
    previous: graph.nodes.filter((node) => previousIds.has(node.id)).map(({ id, name, status }) => ({ id, name, status, meta: undefined })),
    next: graph.nodes.filter((node) => nextIds.has(node.id)).map(({ id, name, status }) => ({ id, name, status, meta: undefined })),
  }
})
const showPracticeForm = ref(false)
const practiceSaving = ref(false)
const practiceForm = reactive({ title: '', note: '' })
const showAssessment = ref(false)
const assessmentLoading = ref(false)
const assessmentSubmitted = ref(false)
const assessmentQuestions = ref<SkillQuestion[]>([])
const assessmentAnswers = ref<Record<string, number>>({})
const assessmentScore = computed(() => assessmentQuestions.value.reduce(
  (score, question) => score + (assessmentAnswers.value[question.id] === question.answerIndex ? 1 : 0),
  0,
))
const assessmentComplete = computed(() =>
  assessmentQuestions.value.length > 0
  && assessmentQuestions.value.every((question) => assessmentAnswers.value[question.id] !== undefined),
)
const assessmentSuggestion = computed(() => {
  if (!assessmentQuestions.value.length) return ''
  const ratio = assessmentScore.value / assessmentQuestions.value.length
  if (ratio === 1) return '基础概念稳定，建议继续用真实任务验证。'
  if (ratio >= 0.6) return '已有一定基础，建议针对错题补充实践。'
  return '基础概念仍需巩固，可以先完成一次最小练习。'
})

function nodeName(id: string) {
  return store.graph?.nodes.find((node) => node.id === id)?.name ?? id
}

function openNewNode() {
  catalogSelection.value = null
  Object.assign(form, { id: '', name: '', description: '', level: 1, status: 'developing' })
  showEditor.value = true
}

function openEditNode() {
  if (!store.selectedNode) return
  catalogSelection.value = null
  Object.assign(form, store.selectedNode)
  showEditor.value = true
}

async function deleteSelectedNode() {
  const node = store.selectedNode
  if (!node) return
  const confirmed = window.confirm(`确定删除“${node.name}”吗？该节点的所有关联连线也会被移除。`)
  if (!confirmed) return
  await store.deleteNode(node.id)
}
function openCatalogSkill(item: KnowledgeStackItem, track: KnowledgeTrack, stage: KnowledgeStackStage) {
  catalogPreview.value = null
  catalogSelection.value = { item, track, stage }
  Object.assign(form, {
    id: '',
    name: item.name,
    description: item.description,
    level: 0,
    status: 'developing',
  })
  showEditor.value = true
}

function openGraphCatalogPreview(payload: { trackId: string; stageId: string; itemId: string }) {
  const track = knowledgeTracks.value.find((item) => item.id === payload.trackId)
  const stage = track?.stages.find((item) => item.id === payload.stageId)
  const item = stage?.items.find((entry) => entry.id === payload.itemId)
  if (!track || !stage || !item) return
  store.selectedId = null
  catalogPreview.value = { track, stage, item }
}

function selectGraphSkill(id: string) {
  catalogPreview.value = null
  store.selectedId = id
}

function updateGraphRelations(payload: { nodeId: string; previous: RelationNeighbor[]; next: RelationNeighbor[] }) {
  graphRelationContext.value = payload
}

function selectCatalogSkill(id: string) {
  store.selectedId = id
  window.setTimeout(() => document.querySelector('.workspace-grid')?.scrollIntoView({ behavior: 'smooth', block: 'start' }), 100)
}

async function selectGrowthGoal(goalId: string) {
  const selected = growthGoals.value.find((goal) => goal.id === goalId)
  if (!selected || !store.graph) return
  const currentIndex = growthGoals.value.findIndex((goal) => goal.id === currentGoalId.value)
  const nextIndex = growthGoals.value.findIndex((goal) => goal.id === selected.id)
  goalSlideDirection.value = nextIndex >= currentIndex ? 'right' : 'left'
  goalMenuOpen.value = false
  closeInspector()
  expandedPathSkillId.value = null
  goalSaving.value = true
  goalSelectionError.value = ''
  try {
    await store.setCurrentTarget(selected.id)
  } catch {
    goalSelectionError.value = '目标已切换，但路径暂时无法刷新。'
  } finally {
    goalSaving.value = false
  }
}

async function loadProfileState() {
  try {
    const response = await fetch('/api/v1/profile', { headers: { 'X-User-Id': localStorage.getItem('ican:user-id') || 'demo-user' } })
    if (response.ok) profileState.value = await response.json() as ProfileState
  } catch {
    profileState.value = null
  }
}
async function generateKnowledgeTrack(query: string) {
  if (!store.graph) return
  generatingKnowledge.value = true
  knowledgeGenerationError.value = ''
  try {
    const track = await knowledgeCatalogService.generateTrack(query, store.graph.nodes)
    const existingIndex = knowledgeTracks.value.findIndex((item) => item.title === track.title)
    if (existingIndex >= 0) {
      const stableTrack = { ...track, id: knowledgeTracks.value[existingIndex].id }
      knowledgeTracks.value.splice(existingIndex, 1, stableTrack)
      selectedKnowledgeTrackId.value = stableTrack.id
    } else {
      knowledgeTracks.value.push(track)
      selectedKnowledgeTrackId.value = track.id
    }
    store.flash(`已切换到“${track.title}”知识分类`)
  } catch {
    directionChoicesOpen.value = true
    knowledgeGenerationError.value = '无法匹配预置方向，请输入更具体的目标，或直接从下方预置方向中选择。'
  } finally {
    generatingKnowledge.value = false
  }
}

function chooseGeneratedDirection(id: KnowledgeTrackId) {
  selectedKnowledgeTrackId.value = id
  directionChoicesOpen.value = false
  knowledgeGenerationError.value = ''
  const track = knowledgeTracks.value.find((item) => item.id === id)
  if (track) store.flash(`已选择“${track.title}”知识分类`)
}

function deleteKnowledgeTrack(id: KnowledgeTrackId) {
  const index = knowledgeTracks.value.findIndex((track) => track.id === id)
  if (index < 0) return
  const track = knowledgeTracks.value[index]
  if (!window.confirm('确定删除知识方向“' + track.title + '”吗？个人技能节点不会受到影响。')) return
  knowledgeTracks.value.splice(index, 1)
  if (catalogPreview.value?.track.id === id) catalogPreview.value = null
  if (selectedKnowledgeTrackId.value === id) {
    selectedKnowledgeTrackId.value = knowledgeTracks.value[Math.min(index, knowledgeTracks.value.length - 1)]?.id ?? ''
  }
}

async function submitNode() {
  if (!form.name.trim()) return
  const isNew = !form.id
  let aiRelations: Array<{ from: string; to: string; type: 'prerequisite' | 'related'; confidence: number }> = []
  let aiQuestions: Array<{ question: string; options?: string[]; answerIndex?: number; explanation?: string }> = []
  // Directory items already carry vetted descriptions and relations. Do not block
  // their addition on a second AI enrichment request.
  if (isNew && store.graph && !catalogSelection.value) {
    try {
      const enrichment = await aiService.enrichSkill(form.name.trim(), store.graph.nodes.map(({ id, name }) => ({ id, name })))
      if (!form.description.trim() && enrichment.skill.description) form.description = enrichment.skill.description
      if (enrichment.skill.proficiency !== undefined) form.level = Math.max(0, Math.min(4, enrichment.skill.proficiency)) as SkillLevel
      aiQuestions = enrichment.questions ?? []
      aiRelations = enrichment.relations.flatMap((relation) => {
        const existing = store.graph?.nodes.find((item) => item.id === relation.existingSkillId)
        return existing ? [{ from: existing.id, to: form.id || 'pending-ai-node', type: relation.type === 'prerequisite' ? 'prerequisite' : 'related', confidence: 0.72 }] : []
      })
    } catch {
      // Manual creation remains available when the model is unavailable.
    }
  }
  const node: SkillNode = {
    id: form.id || `skill-${Date.now()}`,
    name: form.name.trim(),
    description: form.description.trim(),
    level: form.level,
    status: form.status,
    evidenceIds: isNew ? [] : store.selectedNode?.evidenceIds ?? [],
    x: isNew ? 0 : store.selectedNode?.x ?? 0,
    y: isNew ? 0 : store.selectedNode?.y ?? 0,
  }
  const existingNodes = store.graph?.nodes ?? []
  const relations = isNew && catalogSelection.value
    ? buildCatalogRelations(
        catalogSelection.value.track,
        catalogSelection.value.stage,
        catalogSelection.value.item,
        node.id,
        existingNodes,
      )
    : []
  if (!(await store.saveNode(node))) return
  const savedNodeId = store.selectedId ?? node.id
  if (isNew && aiQuestions.length) skillAssessmentService.saveGeneratedQuestions(savedNodeId, aiQuestions)
  const normalizedAiRelations = aiRelations.map((relation) => ({ ...relation, to: savedNodeId }))
  if (!(await store.saveRelations([...relations, ...normalizedAiRelations]))) return
  catalogSelection.value = null
  showEditor.value = false
}

function closeInspector() {
  store.selectedId = null
  catalogPreview.value = null
  graphRelationContext.value = null
}

function nodeForPath(id: string) {
  return store.graph?.nodes.find((node) => node.id === id) ?? null
}

function togglePathDetail(skillId: string) {
  expandedPathSkillId.value = expandedPathSkillId.value === skillId ? null : skillId
}

const expandedPathStep = computed(() =>
  store.path.find((step) => step.skillId === expandedPathSkillId.value) ?? null,
)

function pathAction(step: GrowthPathStep) {
  if (step.status === 'done') return '用一次真实任务复验，并补充最新实践记录。'
  if (step.status === 'blocked') return `先完成前置能力：${step.prerequisiteIds.map(nodeName).join('、') || '基础知识准备'}。`
  return '完成一次最小实践，将结果和遇到的问题记录为能力证据。'
}

function pathFoundation(step: GrowthPathStep) {
  const node = nodeForPath(step.skillId)
  const status = statusOptions.find((item) => item.value === node?.status)?.label ?? '待确认'
  return `${levelLabels[node?.level ?? 0]} · ${status}`
}

function scrollToSkillDetail() {
  window.setTimeout(() => {
    document.querySelector('.skill-detail-panel')?.scrollIntoView({ behavior: 'smooth', block: 'start' })
  })
}

function resetInspectorWorkflows() {
  showPracticeForm.value = false
  practiceForm.title = ''
  practiceForm.note = ''
  showAssessment.value = false
  assessmentLoading.value = false
  assessmentSubmitted.value = false
  assessmentQuestions.value = []
  assessmentAnswers.value = {}
}

async function startAssessment() {
  if (!store.selectedNode) return
  showAssessment.value = true
  assessmentLoading.value = true
  assessmentSubmitted.value = false
  assessmentAnswers.value = {}
  try {
    assessmentQuestions.value = await skillAssessmentService.getQuestions(store.selectedNode)
  } finally {
    assessmentLoading.value = false
  }
}

function submitAssessment() {
  if (!assessmentComplete.value) return
  assessmentSubmitted.value = true
}

async function savePracticeRecord() {
  if (!store.selectedNode || !practiceForm.title.trim()) return
  practiceSaving.value = true
  try {
    const saved = await store.addEvidence(store.selectedNode.id, {
      title: practiceForm.title,
      note: practiceForm.note,
    })
    if (!saved) return
    practiceForm.title = ''
    practiceForm.note = ''
    showPracticeForm.value = false
  } finally {
    practiceSaving.value = false
  }
}

function formatEvidenceDate(value: string) {
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? value : date.toLocaleDateString('zh-CN')
}

watch(
  () => store.selectedId,
  () => resetInspectorWorkflows(),
)
onMounted(async () => {
  await Promise.all([
    store.load(),
    loadProfileState(),
    knowledgeCatalogService.getCatalog().then((catalog) => {
      const defaultDirectionIds = new Set(['frontend', 'backend', 'network'])
      knowledgeTracks.value = catalog.filter((track) => defaultDirectionIds.has(track.id))
      if (!catalog.some((track) => track.id === selectedKnowledgeTrackId.value)) {
        selectedKnowledgeTrackId.value = catalog[0]?.id ?? ''
      }
    }),
  ])

  const persistedGoal = profileState.value?.goals[0]
  if (persistedGoal && store.graph && persistedGoal !== store.graph.goal.title) {
    store.graph.goal = { ...store.graph.goal, title: persistedGoal }
    await store.regeneratePath()
  }
})
</script>

<template>
  <main class="ability-shell">
    <header class="ability-header">
      <div class="ability-header-inner">
        <RouterLink to="/" class="icon-button header-back" aria-label="返回工作台" title="返回工作台">
          <ArrowLeft :size="20" />
        </RouterLink>
        <div class="page-title">
          <span class="page-icon"><Network :size="20" /></span>
          <div><strong>能力成长</strong><small>结构化成长工作台</small></div>
        </div>
        <div class="sync-state"><span /> {{ store.error ? '未保存' : '已保存' }}</div>
        <button class="primary-button compact" type="button" @click="openNewNode"><Plus :size="17" /> 新增能力</button>
      </div>
    </header>

    <div v-if="store.loading" class="state-panel page-loading">
      <span class="spinner" />
      <p>正在构建能力图谱…</p>
    </div>
    <div v-else-if="store.error && !store.graph" class="state-panel error-state page-loading">
      <CircleAlert :size="28" />
      <p>{{ store.error }}</p>
      <button class="secondary-button" type="button" @click="store.load">重新加载</button>
    </div>

    <div v-else-if="store.graph" class="ability-content">
      <section class="ability-overview">
        <div class="overview-copy">
          <p class="eyebrow">当前目标</p>
          <div class="goal-heading-row">
            <Transition :name="`goal-title-${goalSlideDirection}`" mode="out-in"><h1 :key="currentGoalId">{{ targetSkillLabel || '请选择目标技能点' }}</h1></Transition>
            <Transition name="goal-selector" mode="out-in">
            <div :key="currentGoalId" class="goal-selector" :class="{ open: goalMenuOpen }" :style="{ '--goal-shift': goalSlideDirection === 'right' ? '10px' : '-10px' }">
              <span>切换目标</span>
              <button class="goal-menu-trigger" type="button" :aria-expanded="goalMenuOpen" @click="goalMenuOpen = !goalMenuOpen">
                <span>{{ targetSkillLabel || '请选择目标技能点' }}</span><ChevronDown :size="16" />
              </button>
              <Transition name="path-overlay">
                <div v-if="goalMenuOpen" class="goal-menu" role="listbox">
                  <button v-for="goal in growthGoals" :key="goal.id" type="button" :class="{ active: goal.id === currentGoalId }" @click="selectGrowthGoal(goal.id)">{{ goal.name }}</button>
                </div>
              </Transition>
            </div>
            </Transition>
          </div>
          <p v-if="goalSelectionError" class="goal-selection-error">{{ goalSelectionError }}</p>
        </div>
        <div class="overview-progress">
          <div class="progress-ring" :style="{ '--progress': `${store.progress * 3.6}deg` }">
            <Transition name="progress-value" mode="out-in"><span :key="currentGoalId"><strong>{{ store.progress }}%</strong><small>路径基础</small></span></Transition>
          </div>
          <div class="progress-copy"><Transition name="progress-value" mode="out-in"><strong :key="currentGoalId">{{ targetScopeNodes.filter((node) => node.level >= 2).length }}/{{ targetScopeNodes.length }}</strong></Transition><span>项当前目标前置能力已有实践基础</span></div>
        </div>
      </section>

      <section class="ai-input-band">
        <div class="ai-label">
          <span><Sparkles :size="18" /></span>
          <div><strong>用一句话更新能力</strong><small>AI 会先整理为候选节点，由你确认后再加入图谱</small></div>
        </div>
        <form class="quick-form" @submit.prevent="store.parse(quickInput)">
          <input v-model="quickInput" aria-label="描述你的能力和目标" placeholder="例如：我会 Python，正在学 Linux，目标是部署网络服务" />
          <button class="primary-button" type="submit" :disabled="store.parsing || !quickInput.trim()">
            <RefreshCw v-if="store.parsing" class="spin-icon" :size="17" />
            <Sparkles v-else :size="17" />
            {{ store.parsing ? 'AI 分析中' : 'AI 分析' }}
          </button>
        </form>

        <Transition name="parse-result">
          <div v-if="store.parseResult" class="parse-result">
          <div class="parse-summary"><Bot :size="19" /><strong>{{ store.parseResult.summary }}</strong></div>
          <div v-if="store.parseResult.suggestedNodes.length" class="suggestion-list">
            <span v-for="node in store.parseResult.suggestedNodes" :key="node.id"><Check :size="14" /> {{ node.name }} · {{ levelLabels[node.level] }}</span>
          </div>
          <div class="assumption-row"><Info :size="15" /> {{ store.parseResult.assumptions.join('；') }}</div>
          <div class="parse-actions">
            <button class="text-button" type="button" @click="store.parseResult = null">取消</button>
            <button class="secondary-button" type="button" :disabled="!store.parseResult.suggestedNodes.length" @click="store.acceptSuggestions"><Check :size="16" /> 确认加入</button>
          </div>
          </div>
        </Transition>
      </section>

      <div class="workspace-grid">
        <section class="graph-section">
          <div class="section-heading compact-heading">
            <div>
              <p class="eyebrow">个人技能树</p>
              <h2>我的当前能力与学习状态</h2>
            </div>
            <div class="tree-stats">
              <span><i class="dot mastered" /><strong>{{ store.graph.nodes.filter((node) => node.status === 'mastered').length }}</strong> 已掌握</span>
              <span><i class="dot developing" /><strong>{{ store.graph.nodes.filter((node) => node.status === 'developing').length }}</strong> 学习中</span>
              <span><i class="dot gap" /><strong>{{ store.graph.nodes.filter((node) => node.status === 'gap' || node.status === 'target').length }}</strong> 待补齐</span>
            </div>
          </div>
          <AbilityGraph
            :nodes="store.graph.nodes"
            :relations="store.graph.relations"
            :tracks="knowledgeTracks"
            :path="store.path"
            :current-target-id="store.currentTargetId"
            :selected-id="store.selectedId"
            @select="selectGraphSkill"
            @preview-catalog="openGraphCatalogPreview"
            @relation-context="updateGraphRelations"
            @delete-many="store.deleteNodes"
            @clear-selection="closeInspector"

          >
            <template #inspector="{ startDrag, closePopover }">
              <aside v-if="store.selectedNode" class="skill-node-summary" aria-label="技能摘要" @click.stop>
                <div class="summary-drag-handle" title="拖动信息框" @pointerdown="startDrag">
                  <GripVertical :size="15" />
                  <span>技能摘要</span>
                </div>
                <div class="summary-actions">
                  <button class="summary-action danger" type="button" aria-label="删除当前技能" title="删除技能" @click="deleteSelectedNode"><Trash2 :size="14" /></button>
                  <button class="summary-action" type="button" aria-label="关闭技能摘要" title="关闭" @click="closePopover"><X :size="15" /></button>
                </div>
                <span class="node-status" :class="store.selectedNode.status">{{ statusOptions.find((item) => item.value === store.selectedNode?.status)?.label }}</span>
                <h3>{{ store.selectedNode.name }}</h3>
                <p>{{ store.selectedNode.description || '暂未填写能力说明' }}</p>
                <div class="summary-level">
                  <span>掌握程度</span>
                  <strong>{{ levelLabels[store.selectedNode.level] }}</strong>
                </div>
                <button class="secondary-button summary-detail-button" type="button" @click="scrollToSkillDetail">
                  查看完整信息 <ArrowRight :size="15" />
                </button>
              </aside>
              <aside v-else-if="catalogPreview" class="skill-node-summary catalog-skill-summary" aria-label="待学习技能摘要" @click.stop>
                <div class="summary-drag-handle" title="拖动信息框" @pointerdown="startDrag">
                  <GripVertical :size="15" />
                  <span>待学习技能</span>
                </div>
                <div class="summary-actions">
                  <button class="summary-action" type="button" aria-label="关闭技能摘要" title="关闭" @click="closePopover"><X :size="15" /></button>
                </div>
                <span class="node-status gap">待学习</span>
                <h3>{{ catalogPreview.item.name }}</h3>
                <p>{{ catalogPreview.item.description }}</p>
                <div class="summary-level catalog-context">
                  <span>{{ catalogPreview.track.shortTitle }}</span>
                  <strong>{{ catalogPreview.stage.title }}</strong>
                </div>
                <button class="primary-button summary-detail-button" type="button" @click="openCatalogSkill(catalogPreview.item, catalogPreview.track, catalogPreview.stage); closePopover()">
                  去学习 <ArrowRight :size="15" />
                </button>
              </aside>
            </template>
          </AbilityGraph>

          <Transition name="skill-detail" mode="out-in">
            <section v-if="store.selectedNode" :key="store.selectedNode.id" class="skill-detail-panel" aria-label="当前技能完整信息">
            <header class="skill-detail-header">
              <div>
                <div class="skill-detail-kicker">
                  <span class="node-status" :class="store.selectedNode.status">{{ statusOptions.find((item) => item.value === store.selectedNode?.status)?.label }}</span>
                  <span>当前选中的技能</span>
                </div>
                <h2>{{ store.selectedNode.name }}</h2>
                <p>{{ store.selectedNode.description || '暂未填写能力说明' }}</p>
              </div>
              <div class="inspector-actions">
                <button class="secondary-button compact" type="button" @click="openEditNode"><Pencil :size="16" /> 编辑</button>
                <button class="icon-button danger-button" type="button" aria-label="删除当前能力" title="删除能力" @click="deleteSelectedNode"><Trash2 :size="16" /></button>
                <button class="icon-button" type="button" aria-label="关闭技能详情" title="关闭" @click="closeInspector"><X :size="17" /></button>
              </div>
            </header>

            <div class="skill-detail-content">
              <div class="skill-detail-overview">
                <div class="level-block">
                  <div><span>掌握程度</span><strong>{{ levelLabels[store.selectedNode.level] }}</strong></div>
                  <div class="level-track" :aria-label="'掌握程度 ' + store.selectedNode.level + '/4'">
                    <i v-for="index in 5" :key="index" :class="{ active: index - 1 <= store.selectedNode.level }" />
                  </div>
                </div>
                <div class="next-action">
                  <span><Route :size="17" /></span>
                  <div><small>建议下一步</small><strong>{{ store.selectedNode.level >= 2 ? '用一次真实任务验证稳定性' : '完成最小实践并记录过程' }}</strong></div>
                </div>
              </div>

              <div class="skill-detail-workflows">
                <section class="assessment-block">
                  <div class="inspector-section-title">
                    <h3><ClipboardCheck :size="17" /> 技能小测</h3>
                    <button v-if="!showAssessment" class="text-button compact" type="button" @click="startAssessment">开始小测</button>
                    <button v-else-if="assessmentSubmitted" class="text-button compact" type="button" @click="startAssessment">再测一次</button>
                  </div>
                  <Transition name="content-swap" mode="out-in">
                    <p v-if="!showAssessment" key="intro" class="inspector-section-note">通过 3 道基础选择题辅助校准当前水平。</p>
                    <div v-else-if="assessmentLoading" key="loading" class="inspector-inline-state"><span class="spinner" /> 正在加载题目…</div>
                    <div v-else key="questions" class="assessment-content">
                    <article v-for="(question, questionIndex) in assessmentQuestions" :key="question.id" class="assessment-question">
                      <strong><span>{{ questionIndex + 1 }}</span>{{ question.prompt }}</strong>
                      <div class="assessment-options">
                        <button
                          v-for="(option, optionIndex) in question.options"
                          :key="option"
                          type="button"
                          :disabled="assessmentSubmitted"
                          :class="{
                            selected: assessmentAnswers[question.id] === optionIndex,
                            correct: assessmentSubmitted && optionIndex === question.answerIndex,
                            wrong: assessmentSubmitted && assessmentAnswers[question.id] === optionIndex && optionIndex !== question.answerIndex,
                          }"
                          @click="assessmentAnswers[question.id] = optionIndex"
                        >
                          <i>{{ String.fromCharCode(65 + optionIndex) }}</i>{{ option }}
                        </button>
                      </div>
                      <p v-if="assessmentSubmitted" class="answer-explanation">{{ question.explanation }}</p>
                    </article>
                    <div v-if="assessmentSubmitted" class="assessment-result">
                      <CheckCircle2 :size="18" />
                      <div><strong>{{ assessmentScore }}/{{ assessmentQuestions.length }} 题正确</strong><small>{{ assessmentSuggestion }}</small></div>
                    </div>
                    <button v-else class="secondary-button assessment-submit" type="button" :disabled="!assessmentComplete" @click="submitAssessment">提交测评</button>
                      <p class="assessment-disclaimer">测评结果仅作辅助参考，不会自动修改技能等级。</p>
                    </div>
                  </Transition>
                </section>

                <section class="evidence-block">
                  <div class="inspector-section-title">
                    <h3><FileCheck2 :size="17" /> 实践记录 <span>{{ selectedEvidence.length }}</span></h3>
                    <button class="text-button compact" type="button" @click="showPracticeForm = !showPracticeForm">
                      <X v-if="showPracticeForm" :size="14" />
                      <Plus v-else :size="14" />
                      {{ showPracticeForm ? '取消' : '添加记录' }}
                    </button>
                  </div>
                  <Transition name="practice-form">
                    <form v-if="showPracticeForm" class="practice-form" @submit.prevent="savePracticeRecord">
                    <label>实践名称<input v-model="practiceForm.title" required maxlength="40" placeholder="例如：完成接口鉴权" /></label>
                    <label>过程与结果<textarea v-model="practiceForm.note" rows="3" maxlength="180" placeholder="记录完成内容、遇到的问题或产出" /></label>
                    <button class="primary-button compact" type="submit" :disabled="practiceSaving || !practiceForm.title.trim()">
                      {{ practiceSaving ? '保存中' : '保存记录' }}
                      </button>
                    </form>
                  </Transition>
                  <div v-if="selectedEvidence.length" class="evidence-list">
                    <div v-for="item in selectedEvidence" :key="item.id">
                      <CheckCircle2 :size="17" />
                      <span><strong>{{ item.title }}</strong><small>{{ item.note || '未填写补充说明' }}</small><time>{{ formatEvidenceDate(item.createdAt) }}</time></span>
                    </div>
                  </div>
                  <p v-else class="empty-evidence">尚无实践记录，可以添加项目、练习或真实任务作为能力依据。</p>
                </section>
              </div>
            </div>
            <section class="skill-relations-panel" aria-label="技能树关联">
              <div class="inspector-section-title">
                <h3><Network :size="17" /> 技能树关联</h3>
                <span>{{ selectedNeighbors.previous.length + selectedNeighbors.next.length }} 个相邻节点</span>
              </div>
              <div class="relation-detail-map">
                <div class="relation-detail-column previous">
                  <small>前置能力</small>
                  <button
                    v-for="node in selectedNeighbors.previous"
                    :key="`${node.id ?? node.name}-previous`"
                    type="button"
                    :disabled="!node.id"
                    @click="node.id && selectGraphSkill(node.id)"
                  >
                    <span class="node-status" :class="node.status">{{ node.meta || '前置' }}</span>
                    <strong>{{ node.name }}</strong>
                  </button>
                  <span v-if="!selectedNeighbors.previous.length" class="relation-empty">当前视图中没有前置节点</span>
                </div>
                <div class="relation-current-node">
                  <i />
                  <strong>{{ store.selectedNode.name }}</strong>
                  <small>当前技能</small>
                </div>
                <div class="relation-detail-column next">
                  <small>后续能力</small>
                  <button
                    v-for="node in selectedNeighbors.next"
                    :key="`${node.id ?? node.name}-next`"
                    type="button"
                    :disabled="!node.id"
                    @click="node.id && selectGraphSkill(node.id)"
                  >
                    <span class="node-status" :class="node.status">{{ node.meta || '后续' }}</span>
                    <strong>{{ node.name }}</strong>
                  </button>
                  <span v-if="!selectedNeighbors.next.length" class="relation-empty">当前视图中没有后续节点</span>
                </div>
              </div>
            </section>
            </section>
          </Transition>
        </section>
      </div>

      <KnowledgeCatalog
        :tracks="knowledgeTracks"
        :personal-skills="store.graph.nodes"
        :selected-track-id="selectedKnowledgeTrackId"
        :generating="generatingKnowledge"
        :generation-error="knowledgeGenerationError"
        @update:selected-track-id="selectedKnowledgeTrackId = $event"
        @generate="generateKnowledgeTrack"
        @add="openCatalogSkill"
        @select="selectCatalogSkill"
        @delete="deleteKnowledgeTrack"
      />

      <section class="path-section">
        <div class="section-heading compact-heading">
          <div>
            <p class="eyebrow">可解释路径</p>
            <h2>到达目标，还需要补齐什么</h2>
          </div>
          <button class="secondary-button compact" type="button" :disabled="store.pathLoading" @click="store.regeneratePath"><RefreshCw v-if="store.pathLoading" class="spin-icon" :size="16" /><Sparkles v-else :size="16" /> {{ store.pathLoading ? '正在生成路径' : 'AI 重新生成' }}</button>
        </div>
        <div class="path-stage" :class="{ 'is-loading': store.pathLoading }">
        <Transition name="path-loading">
          <div v-if="store.pathLoading" class="path-loading-state"><RefreshCw class="spin-icon" :size="18" /><span>正在为当前目标整理可解释路径</span></div>
        </Transition>
        <div class="path-list">
          <article v-for="(step, index) in store.path" :key="step.skillId" class="path-step" :class="[step.status, { expanded: expandedPathSkillId === step.skillId }]">
            <button class="path-step-main" type="button" :aria-expanded="expandedPathSkillId === step.skillId" @click="togglePathDetail(step.skillId)">
              <span class="path-marker">
                <Check v-if="step.status === 'done'" :size="16" />
                <span v-else>{{ index + 1 }}</span>
              </span>
              <span class="path-copy">
                <span><strong>{{ nodeName(step.skillId) }}</strong><span>{{ step.status === 'done' ? '已具备' : step.status === 'blocked' ? '等待前置' : '建议下一步' }}</span></span>
                <p>{{ step.reason }}</p>
                <small v-if="step.prerequisiteIds.length">依据：需先具备 {{ step.prerequisiteIds.map(nodeName).join('、') }}</small>
                <small v-else>依据：能力图谱中的目标依赖关系</small>
              </span>
              <ChevronDown class="path-toggle-icon" :size="18" />
            </button>
          </article>
        </div>
        <Transition name="path-overlay" mode="out-in">
          <section v-if="expandedPathStep" :key="expandedPathStep.skillId" class="path-detail-overlay">
            <header>
              <div><span>路径节点详情</span><strong>{{ nodeName(expandedPathStep.skillId) }}</strong></div>
              <button class="summary-action" type="button" aria-label="关闭路径详情" title="关闭" @click="expandedPathSkillId = null"><X :size="15" /></button>
            </header>
            <div class="path-detail-grid">
              <div><span>能力说明</span><p>{{ nodeForPath(expandedPathStep.skillId)?.description || '该能力尚未补充详细说明。' }}</p></div>
              <div><span>当前基础</span><strong>{{ pathFoundation(expandedPathStep) }}</strong></div>
              <div><span>下一步行动</span><p>{{ pathAction(expandedPathStep) }}</p></div>
            </div>
          </section>
        </Transition>
        </div>
      </section>
    </div>

    <Transition name="toast">
      <div v-if="store.notice" class="toast-message"><CheckCircle2 :size="18" /> {{ store.notice }}</div>
    </Transition>
    <div v-if="store.error && store.graph" class="toast-message error-state" role="alert"><CircleAlert :size="18" /> {{ store.error }}</div>

    <Teleport to="body">
      <Transition name="editor-modal">
        <div v-if="showEditor" class="modal-backdrop" @click.self="showEditor = false">
        <form class="editor-dialog" @submit.prevent="submitNode">
          <div class="dialog-header">
            <div><p class="eyebrow">结构化能力</p><h2>{{ form.id ? '编辑能力节点' : '新增能力节点' }}</h2></div>
            <button class="icon-button" type="button" aria-label="关闭" @click="showEditor = false"><X :size="19" /></button>
          </div>
          <label>能力名称<input v-model="form.name" required maxlength="24" placeholder="例如：Docker 容器化" /></label>
          <label>能力说明<textarea v-model="form.description" rows="3" maxlength="120" placeholder="说明这项能力能解决什么问题" /></label>
          <fieldset>
            <legend>掌握程度</legend>
            <div class="segmented-control five" :style="{ '--segment-shift': `calc(${form.level * 100}% + ${form.level * 2}px)` }">
              <button v-for="(label, index) in levelLabels" :key="label" type="button" :class="{ active: form.level === index }" @click="form.level = index as SkillLevel">{{ label }}</button>
            </div>
          </fieldset>
          <fieldset>
            <legend>节点状态</legend>
            <div class="segmented-control" :style="{ '--segment-shift': 'calc(' + (statusOptions.findIndex((item) => item.value === form.status) * 100) + '% + ' + (statusOptions.findIndex((item) => item.value === form.status) * 2) + 'px)' }">
              <button v-for="option in statusOptions" :key="option.value" type="button" :class="{ active: form.status === option.value }" @click="form.status = option.value">{{ option.label }}</button>
            </div>
          </fieldset>
          <div class="dialog-actions">
            <button class="text-button" type="button" @click="showEditor = false">取消</button>
            <button class="primary-button" type="submit"><Save :size="17" /> 保存节点</button>
          </div>
        </form>
        </div>
      </Transition>
    </Teleport>
  </main>
</template>
