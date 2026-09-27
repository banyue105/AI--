<script setup lang="ts">
import { computed, onMounted, ref, toRaw, watch } from 'vue'
import { ArrowLeft, ArrowRight, Check, ChevronDown, GitBranch, History, Plus, RotateCcw, Sparkles, Pencil, AlertCircle } from 'lucide-vue-next'
import { useDecisionStore } from './stores/decisionStore'
import DecisionScenarioDialog from './components/DecisionScenarioDialog.vue'
import DecisionConditionGraph from './components/DecisionConditionGraph.vue'
import type { DecisionRelationInput, DecisionScenarioInput, DecisionSuggestion, RiskLevel } from './types'
import './decision.css'

const store = useDecisionStore()
const dialogOpen = ref(false)
const isNew = ref(false)
const naturalInput = ref('')
const suggestions = ref<DecisionSuggestion[]>([])
const selectedNodeId = ref('goal')
const selectedRelationId = ref('')
const relationDraft = ref<DecisionRelationInput | null>(null)
const leftVersionId = ref('')
const rightVersionId = ref('')

const scene = computed(() => store.scenario)
const baseline = computed(() => scene.value?.latestResults.find(item => item.optionKey === 'baseline'))
const changed = computed(() => scene.value?.latestResults.find(item => item.optionKey === 'changed'))
const selectedNode = computed(() => scene.value?.nodes.find(item => item.id === selectedNodeId.value))
const selectedRelation = computed(() => scene.value?.relations.find(item => item.id === selectedRelationId.value))
const hasResults = computed(() => Boolean(baseline.value && changed.value))
const risk = computed(() => changed.value?.riskLevel || 'uncomputed')

watch(() => store.parsed, result => { suggestions.value = result?.candidates.map(item => ({ ...item, selected: true })) || [] })
watch(() => store.notice, (message, _previous, onCleanup) => {
  if (!message) return
  const timeout = window.setTimeout(() => { if (store.notice === message) store.notice = '' }, 3500)
  onCleanup(() => window.clearTimeout(timeout))
})
watch(() => scene.value?.id, () => { selectedNodeId.value = 'goal'; selectedRelationId.value = '' })
watch(() => store.versions, history => {
  leftVersionId.value = history.length > 1 ? history[history.length - 1]!.id : ''
  rightVersionId.value = history[0]?.id || ''
}, { deep: true })

onMounted(() => store.initialize())

function asInput(): DecisionScenarioInput | null {
  if (!scene.value) return null
  const item = scene.value
  return { title: item.title, goal: item.goal, timeLimitDays: item.timeLimitDays,
    budgetYuan: Number(item.budgetYuan), peopleCount: item.peopleCount, hasServer: item.hasServer,
    changeRequest: item.changeRequest,
    resources: item.resources.map(resource => ({ id: resource.id, type: resource.type, label: resource.label,
      quantity: resource.quantity, unit: resource.unit })),
    relations: item.relations.map(relation => ({ id: relation.id, from: relation.from, to: relation.to,
      label: relation.label, confidence: relation.confidence, assumption: relation.assumption })) }
}

function openDialog(newScene = false) {
  isNew.value = newScene
  dialogOpen.value = true
}

async function saveForm(input: DecisionScenarioInput) {
  const saved = await store.save(input, isNew.value)
  if (saved) {
    dialogOpen.value = false
    await store.simulate()
  }
}

async function parseInput() {
  if (!naturalInput.value.trim()) { store.error = '请先输入希望改变的条件。'; return }
  await store.parse(naturalInput.value.trim())
}

async function confirmSuggestions() {
  const input = asInput()
  if (!input) return
  const selected = suggestions.value.filter(item => item.selected)
  if (!selected.length) { store.error = '请至少勾选一项候选条件，或打开表单手动编辑。'; return }
  if (selected.some(item => !item.value.trim() || (item.kind === 'server' && !['true', 'false'].includes(item.value)))) {
    store.error = '候选值不能为空；服务器条件只能填 true 或 false。'
    return
  }
  for (const item of selected) {
    if (item.kind === 'people') input.peopleCount = Number(item.value)
    if (item.kind === 'budget') input.budgetYuan = Number(item.value)
    if (item.kind === 'time') input.timeLimitDays = Number(item.value)
    if (item.kind === 'server') input.hasServer = item.value === 'true'
    if (item.kind === 'change') input.changeRequest = item.value.trim()
  }
  if (!Number.isInteger(input.peopleCount) || input.peopleCount < 1 || !Number.isFinite(input.budgetYuan)
      || input.budgetYuan < 0 || !Number.isInteger(input.timeLimitDays) || input.timeLimitDays < 1) {
    store.error = '候选数字无效，请修正后再确认，或打开表单手动编辑。'
    return
  }
  const saved = await store.save(input)
  if (saved) {
    naturalInput.value = ''
    await store.simulate()
  }
}

function selectNode(id: string) { selectedNodeId.value = id; selectedRelationId.value = ''; relationDraft.value = null }
function selectRelation(id: string) {
  selectedRelationId.value = id
  selectedNodeId.value = ''
  const relation = scene.value?.relations.find(item => item.id === id)
  relationDraft.value = relation ? { id: relation.id, from: relation.from, to: relation.to,
    label: relation.label, confidence: relation.confidence, assumption: relation.assumption } : null
}

async function saveRelation() {
  const input = asInput()
  if (!input || !relationDraft.value) return
  if (!relationDraft.value.label.trim() || relationDraft.value.confidence < 0 || relationDraft.value.confidence > 1) {
    store.error = '关系描述不能为空，置信度必须在 0 到 1 之间。'
    return
  }
  input.relations = input.relations.map(item => item.id === relationDraft.value!.id ? structuredClone(toRaw(relationDraft.value!)) : item)
  const saved = await store.save(input)
  if (saved) await store.simulate()
}

async function compareVersions() {
  if (!leftVersionId.value || !rightVersionId.value || leftVersionId.value === rightVersionId.value) {
    store.error = '请选择两个不同的版本。'; return
  }
  await store.compare(leftVersionId.value, rightVersionId.value)
}

async function restoreVersion(id: string) {
  if (!window.confirm('恢复此版本的条件？当前已保存版本不会删除，恢复后可重新推演。')) return
  await store.restore(id)
}

function formatDate(value?: string) {
  return value ? new Intl.DateTimeFormat('zh-CN', { month: 'numeric', day: 'numeric', hour: '2-digit', minute: '2-digit' }).format(new Date(value)) : '—'
}
function formatMoney(value: number) { return new Intl.NumberFormat('zh-CN').format(value) }
function signed(value: number) { return `${value > 0 ? '+' : ''}${formatMoney(value)}` }
function riskLabel(level: RiskLevel | 'uncomputed') { return ({ low: '低风险', medium: '中风险', high: '高风险', uncomputed: '待计算' })[level] }
function sourceLabel(source: string) { return ({ user: '用户确认', system: '系统规则', ai: 'AI 提取', mock: '离线模拟', rule: '计算规则' })[source] || source }
</script>

<template>
  <div class="decision-shell">
    <header class="decision-header">
      <div class="decision-header-inner">
        <RouterLink to="/" class="icon-button" aria-label="返回首页"><ArrowLeft :size="18" /></RouterLink>
        <div class="decision-page-icon"><GitBranch :size="19" /></div>
        <div class="decision-page-title"><strong>决策沙盒</strong><small>条件推演工作台</small></div>
        <span class="decision-sync"><span></span>{{ scene ? `已保存 · ${scene.versionCount} 个版本` : '等待场景' }}</span>
        <button type="button" class="primary-button decision-new-button" :disabled="Boolean(store.busy)" @click="openDialog(true)"><Plus :size="17" /><span>新建场景</span></button>
      </div>
    </header>

    <main class="decision-content">
      <div v-if="store.busy === 'loading' && !scene" class="state-panel page-loading" role="status"><span class="spinner"></span>正在加载决策场景…</div>
      <div v-else-if="!scene" class="state-panel decision-empty">
        <GitBranch :size="30" />
        <strong>{{ store.error ? '暂时无法加载场景' : '还没有决策场景' }}</strong>
        <p>{{ store.error || '创建一个场景，录入现实条件后即可开始推演。' }}</p>
        <div class="decision-empty-actions"><button type="button" class="primary-button" @click="openDialog(true)">创建场景</button><button v-if="store.error" type="button" class="secondary-button" @click="store.initialize()">重试连接</button></div>
      </div>
      <template v-else>
        <section class="decision-overview" aria-labelledby="decision-overview-title">
          <div class="decision-overview-main">
            <div class="decision-overview-top"><p class="eyebrow">当前决策场景</p><label class="decision-scene-switch"><span class="decision-visually-hidden">切换场景</span><select :value="scene.id" @change="store.select(($event.target as HTMLSelectElement).value)"><option v-for="item in store.summaries" :key="item.id" :value="item.id">{{ item.title }}</option></select><ChevronDown :size="14" /></label></div>
            <h1 id="decision-overview-title">{{ scene.title }}</h1>
            <p>{{ scene.goal }}</p>
            <div class="decision-meta"><span>{{ scene.peopleCount }} 人</span><span>预算 {{ formatMoney(Number(scene.budgetYuan)) }} 元</span><span>{{ scene.timeLimitDays }} 天</span><span>{{ scene.hasServer ? '已有服务器' : '需准备服务器' }}</span><span>更新于 {{ formatDate(scene.updatedAt) }}</span></div>
          </div>
          <div class="decision-overview-side"><div><small>当前方案</small><strong>{{ scene.latestResults.length }}</strong></div><div><small>变更风险</small><strong :class="`decision-risk-${risk}`">{{ riskLabel(risk) }}</strong></div><button type="button" class="secondary-button compact" :disabled="Boolean(store.busy)" @click="openDialog(false)"><Pencil :size="15" /> 编辑条件</button></div>
        </section>

        <section class="decision-input-band" aria-labelledby="decision-input-title">
          <div class="decision-input-label"><span class="decision-input-icon"><Sparkles :size="19" /></span><div><strong id="decision-input-title">一句话调整条件</strong><small>先提取候选项，再由你确认；数字由规则计算</small></div></div>
          <form class="decision-quick-form" @submit.prevent="parseInput"><label class="decision-visually-hidden" for="decision-natural-input">自然语言条件</label><input id="decision-natural-input" v-model="naturalInput" maxlength="1000" placeholder="例如：3 人、10 万元、60 天、已有服务器，增加视觉识别功能" /><button type="submit" class="primary-button" :disabled="Boolean(store.busy)">{{ store.busy === 'parsing' ? '解析中…' : '结构化条件' }}</button></form>
          <div v-if="store.parsed" class="decision-parse-result" aria-live="polite">
            <div class="decision-parse-heading"><strong>{{ store.parsed.summary }}</strong><small>{{ sourceLabel(store.parsed.source) }}</small></div>
            <div v-if="suggestions.length" class="decision-suggestions">
              <label v-for="(candidate, index) in suggestions" :key="index" class="decision-suggestion"><input v-model="candidate.selected" type="checkbox" /><span>{{ candidate.label }}</span><input v-model="candidate.value" :aria-label="`修正${candidate.label}`" /><small>可信度 {{ Math.round(candidate.confidence * 100) }}%</small></label>
            </div>
            <p v-for="assumption in store.parsed.assumptions" :key="assumption" class="decision-parse-assumption">{{ assumption }}</p>
            <div class="decision-parse-actions"><button type="button" class="secondary-button compact" @click="openDialog(false)">改用表单编辑</button><button type="button" class="primary-button compact" :disabled="Boolean(store.busy) || !suggestions.length" @click="confirmSuggestions"><Check :size="15" /> 确认并推演</button></div>
          </div>
        </section>

        <div v-if="store.error" class="decision-alert" role="alert"><AlertCircle :size="17" />{{ store.error }}<button type="button" aria-label="关闭错误提示" @click="store.error = ''">×</button></div>

        <div class="decision-workspace">
          <DecisionConditionGraph :scenario="scene" :selected-node-id="selectedNodeId" :selected-relation-id="selectedRelationId" @node="selectNode" @relation="selectRelation" />
          <aside class="decision-inspector" aria-label="节点和关系详情">
            <p class="eyebrow">条件依据</p>
            <template v-if="selectedRelation && relationDraft">
              <h2>关系说明</h2><p>{{ selectedRelation.from }} → {{ selectedRelation.to }}</p>
              <label>影响描述<input v-model="relationDraft.label" maxlength="120" /></label>
              <label>置信度（0–1）<input v-model.number="relationDraft.confidence" type="number" min="0" max="1" step="0.01" /></label>
              <label>依据与假设<textarea v-model="relationDraft.assumption" rows="3" maxlength="500" /></label>
              <div class="decision-inspector-facts"><span>来源</span><strong>{{ sourceLabel(selectedRelation.source) }}</strong></div>
              <button type="button" class="primary-button decision-inspector-button" :disabled="Boolean(store.busy)" @click="saveRelation">保存关系并推演</button>
            </template>
            <template v-else-if="selectedNode">
              <h2>{{ selectedNode.label }}</h2><p>{{ selectedNode.type === 'goal' ? '场景的交付目标' : selectedNode.type === 'risk' ? '由规则评估的交付风险' : '参与方案推演的现实条件' }}</p>
              <div class="decision-inspector-facts"><span>节点类型</span><strong>{{ ({ goal: '目标', resource: '资源', task: '任务', constraint: '约束', risk: '风险' })[selectedNode.type] }}</strong></div>
              <div class="decision-inspector-facts"><span>来源</span><strong>{{ sourceLabel(selectedNode.source) }}</strong></div>
              <div class="decision-inspector-facts"><span>置信度</span><strong>{{ Math.round(selectedNode.confidence * 100) }}%</strong></div>
              <div class="decision-inspector-note"><strong>依据与假设</strong><p>{{ selectedNode.assumption }}</p></div>
              <button type="button" class="secondary-button decision-inspector-button" @click="openDialog(false)"><Pencil :size="15" /> 编辑关联条件</button>
            </template>
          </aside>
        </div>

        <section class="decision-results" aria-labelledby="decision-results-title">
          <div class="decision-section-heading"><div><p class="eyebrow">方案推演</p><h2 id="decision-results-title">基准与变更</h2></div><button type="button" class="primary-button compact" :disabled="Boolean(store.busy)" @click="store.simulate()">{{ store.busy === 'simulating' ? '计算中…' : '重新计算并保存版本' }}</button></div>
          <div v-if="!hasResults" class="decision-result-empty"><GitBranch :size="22" /><strong>当前条件尚未推演</strong><p>点击“重新计算并保存版本”，查看基准方案与变更方案的差异。</p></div>
          <template v-else>
            <div class="decision-branch-strip" aria-label="方案分支"><span>当前条件</span><ArrowRight :size="18" /><strong>基准方案</strong><span class="decision-branch-divider">或</span><strong>变更方案</strong></div>
            <div class="decision-difference"><strong>关键差异</strong><p>加入“{{ scene.changeRequest || '当前变更' }}”后，工期上限 {{ signed(changed!.timeRange.max - baseline!.timeRange.max) }} 天、成本上限 {{ signed(changed!.budgetRange.max - baseline!.budgetRange.max) }} 元；变更风险为 {{ riskLabel(changed!.riskLevel) }}。</p></div>
            <div class="decision-compare-scroll" tabindex="0" aria-label="可横向滚动的方案对比表">
              <div class="decision-compare-table" role="table" aria-label="基准与变更方案对比">
                <div class="decision-compare-row decision-compare-head" role="row"><span role="columnheader">比较维度</span><strong role="columnheader">基准方案</strong><strong role="columnheader">变更方案</strong></div>
                <div class="decision-compare-row" role="row"><span role="cell">预计工期</span><strong role="cell">{{ baseline!.timeRange.min }}–{{ baseline!.timeRange.max }} 天</strong><strong role="cell">{{ changed!.timeRange.min }}–{{ changed!.timeRange.max }} 天</strong></div>
                <div class="decision-compare-row" role="row"><span role="cell">资金成本</span><strong role="cell">{{ formatMoney(baseline!.budgetRange.min) }}–{{ formatMoney(baseline!.budgetRange.max) }} 元</strong><strong role="cell">{{ formatMoney(changed!.budgetRange.min) }}–{{ formatMoney(changed!.budgetRange.max) }} 元</strong></div>
                <div class="decision-compare-row" role="row"><span role="cell">人力投入</span><strong role="cell">{{ baseline!.peopleRange.min }}–{{ baseline!.peopleRange.max }} 人</strong><strong role="cell">{{ changed!.peopleRange.min }}–{{ changed!.peopleRange.max }} 人</strong></div>
                <div class="decision-compare-row" role="row"><span role="cell">资源占用</span><strong role="cell">{{ baseline!.resources.join('、') }}</strong><strong role="cell">{{ changed!.resources.join('、') }}</strong></div>
                <div class="decision-compare-row" role="row"><span role="cell">交付风险</span><strong role="cell" :class="`decision-risk-${baseline!.riskLevel}`">{{ riskLabel(baseline!.riskLevel) }}</strong><strong role="cell" :class="`decision-risk-${changed!.riskLevel}`">{{ riskLabel(changed!.riskLevel) }}</strong></div>
              </div>
            </div>
            <p class="decision-scroll-hint">左右滑动可查看变更方案 →</p>
            <div class="decision-explanation"><span>{{ sourceLabel(changed!.explanationSource) }}解释</span><p>{{ changed!.explanation }}</p><small>数值来源：{{ sourceLabel(changed!.source) }}；这是规划估算，不是交付承诺。</small></div>
            <div class="decision-impact"><div><strong>可能的后续影响</strong><ul><li v-for="impact in changed!.impacts" :key="impact">{{ impact }}</li></ul></div><div><strong>关键假设</strong><ul><li v-for="assumption in changed!.assumptions" :key="assumption">{{ assumption }}</li></ul></div></div>
          </template>
        </section>

        <section class="decision-history" aria-labelledby="decision-history-title">
          <div class="decision-section-heading"><div><p class="eyebrow">比较与回溯</p><h2 id="decision-history-title">版本路径</h2></div><span>{{ store.versions.length }} 次推演</span></div>
          <div v-if="!store.versions.length" class="decision-history-empty">首次推演后，版本会保存在数据库中，并在这里显示。</div>
          <template v-else>
            <div class="decision-timeline" tabindex="0" aria-label="可横向滚动的版本时间线"><article v-for="version in [...store.versions].reverse()" :key="version.id" class="decision-version-card"><span class="decision-version-dot">{{ version.number }}</span><strong>版本 {{ version.number }}</strong><small>{{ formatDate(version.createdAt) }}</small><p>{{ version.changeSummary }}</p><span>{{ version.snapshot.peopleCount }} 人 · {{ version.snapshot.timeLimitDays }} 天 · {{ formatMoney(Number(version.snapshot.budgetYuan)) }} 元</span><button type="button" class="text-button compact" :disabled="Boolean(store.busy)" @click="restoreVersion(version.id)"><RotateCcw :size="14" /> 恢复条件</button></article></div>
            <p class="decision-scroll-hint">左右滑动可查看全部版本 →</p>
            <div class="decision-version-compare"><label>从版本<select v-model="leftVersionId"><option value="" disabled>选择</option><option v-for="version in store.versions" :key="version.id" :value="version.id">版本 {{ version.number }}</option></select></label><label>到版本<select v-model="rightVersionId"><option value="" disabled>选择</option><option v-for="version in store.versions" :key="version.id" :value="version.id">版本 {{ version.number }}</option></select></label><button type="button" class="secondary-button" :disabled="Boolean(store.busy) || store.versions.length < 2" @click="compareVersions"><History :size="16" /> 比较两个版本</button></div>
            <div v-if="store.comparison" class="decision-comparison" aria-live="polite"><strong>{{ store.comparison.summary }}</strong><ul><li v-for="item in store.comparison.changedInputs" :key="item">{{ item }}</li></ul><p>变更方案工期区间：{{ signed(store.comparison.timeMinDelta) }} / {{ signed(store.comparison.timeMaxDelta) }} 天；成本区间：{{ signed(store.comparison.budgetMinDelta) }} / {{ signed(store.comparison.budgetMaxDelta) }} 元；风险：{{ store.comparison.riskChange }}。</p></div>
          </template>
        </section>
        <p class="decision-footer-note">说明：采用 72–90 基准人日、65% 有效产能与规则区间估算；请结合实际任务、人力能力和报价校准。AI 仅辅助提取与解释，不生成计算数值。</p>
      </template>
    </main>

    <DecisionScenarioDialog :open="dialogOpen" :is-new="isNew" :initial="asInput()" @close="dialogOpen = false" @save="saveForm" />
    <div v-if="store.notice" class="toast-message decision-toast" role="status"><Check :size="16" />{{ store.notice }}<button type="button" aria-label="关闭通知" @click="store.notice = ''">×</button></div>
  </div>
</template>
