<script setup lang="ts">
import ModuleHighlights from '../../app/ModuleHighlights.vue'
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, CheckCircle2, CircleAlert, ClipboardCheck, FileCheck2, History, Info, Link2, Pencil, Plus, RefreshCw, Sparkles, Trash2 } from 'lucide-vue-next'
import { usePracticeStore } from './stores/practiceStore'
import ProjectEditor from './components/ProjectEditor.vue'
import CriteriaEditor from './components/CriteriaEditor.vue'
import EvidenceEditor from './components/EvidenceEditor.vue'
import EvidenceViewer from './components/EvidenceViewer.vue'
import ReviewFindings from './components/ReviewFindings.vue'
import MetricComparison from './components/MetricComparison.vue'
import ReviewHistory from './components/ReviewHistory.vue'
import FeedbackPanel from './components/FeedbackPanel.vue'
import DecisionSourceDialog from './components/DecisionSourceDialog.vue'
import { dateLabel } from './utils/drafts'
import type { Citation, Evidence, ManualOverride, ProjectDraft } from './types'
import './practice.css'

const store = usePracticeStore(), route = useRoute(), router = useRouter()
const projectEditor = ref(false), createMode = ref(false), criteriaEditor = ref(false), useSuggestions = ref(false), evidenceEditor = ref(false), sourceEditor = ref(false)
const editingEvidence = ref<Evidence | null>(null)
const selectedEvidenceId = ref(''), activeCitation = ref<Citation | null>(null), historicalEvidence = ref(false)
const overrides = ref<ManualOverride[]>([])
const activeEvidence = computed(() => (historicalEvidence.value ? store.review?.input.evidence : store.project?.evidence)?.find(item => item.id === selectedEvidenceId.value) || null)
const visibleEvidence = computed(() => (historicalEvidence.value ? store.review?.input.evidence : store.project?.evidence) || [])
const editingBusy = computed(() => !!store.busy)
const requiredCount = computed(() => (store.review?.input.criteria || store.project?.criteria || []).filter(item => item.required).length)
const isDemo = computed(() => store.project?.processNote.includes('所有材料与投入均为示例数据。'))

function resetEvidence() { activeCitation.value = null; historicalEvidence.value = false; selectedEvidenceId.value = store.project?.evidence[0]?.id || '' }
watch(() => store.review?.id, () => { overrides.value = []; resetEvidence() })
watch(() => store.project?.id, resetEvidence)
watch(() => store.project?.inputRevision, () => { if (!historicalEvidence.value && selectedEvidenceId.value && !store.project?.evidence.some(item => item.id === selectedEvidenceId.value)) resetEvidence() })
watch(() => route.params.projectId, async id => { if (typeof id === 'string' && id !== store.project?.id && !store.busy) await store.selectProject(id) })
onMounted(async () => { await store.init(typeof route.params.projectId === 'string' ? route.params.projectId : undefined); resetEvidence() })

function openProject(create: boolean) { store.error = ''; createMode.value = create; projectEditor.value = true }
async function saveProject(draft: ProjectDraft) {
  const saved = await store.saveProject(draft, createMode.value)
  if (saved && store.project) await router.replace(`/module3/${store.project.id}`)
  return saved
}
async function changeProject(id: string) { if (await store.selectProject(id)) await router.replace(`/module3/${id}`) }
function openEvidence(item: Evidence | null) { store.error = ''; editingEvidence.value = item; evidenceEditor.value = true }
function chooseEvidence(item: Evidence) { selectedEvidenceId.value = item.id; activeCitation.value = null }
function switchEvidenceSnapshot() { historicalEvidence.value = !historicalEvidence.value; selectedEvidenceId.value = visibleEvidence.value[0]?.id || ''; activeCitation.value = null }
function cite(citation: Citation) { selectedEvidenceId.value = citation.evidenceId; activeCitation.value = citation; historicalEvidence.value = true }
function openCriteria(candidates = false) { store.error = ''; useSuggestions.value = candidates; criteriaEditor.value = true }
async function remove(item: Evidence) { if (window.confirm(`从当前材料中移除“${item.title}”？历史复盘中的证据仍保留。`)) await store.removeEvidence(item.id) }
async function openSource() { store.error = ''; store.sourceVersions = []; sourceEditor.value = true; await store.loadSources() }
async function reload() { await store.init(store.project?.id || (typeof route.params.projectId === 'string' ? route.params.projectId : undefined)) }
</script>

<template>
  <main id="page-content" class="practice-shell" tabindex="-1">
    <header class="practice-header"><div class="practice-header-inner">
      <RouterLink to="/" class="icon-button" aria-label="返回工作台" title="返回工作台"><ArrowLeft :size="20" /></RouterLink>
      <div class="page-title"><span class="page-icon"><ClipboardCheck :size="20" /></span><div><strong>实践验证与复盘</strong><small>让成果证据成为下一次行动的依据</small></div></div>
      <div class="sync-state practice-sync"><span /><template v-if="store.busy === 'saving'">保存中</template><template v-else>后端记录</template></div>
      <button type="button" class="primary-button compact practice-header-action" aria-label="新建实践" title="新建实践" :disabled="editingBusy" @click="openProject(true)"><Plus :size="17" /><span>新建实践</span></button>
    </div></header>

    <ModuleHighlights module-id="module3" />

    <div v-if="!store.loaded || store.busy === 'loading'" class="state-panel practice-page-state"><span class="spinner" /><p>正在载入实践记录…</p></div>
    <div v-else class="practice-content">
      <div v-if="store.error && !projectEditor && !evidenceEditor && !criteriaEditor && !sourceEditor" class="practice-alert practice-page-alert" role="alert"><CircleAlert :size="19" /><span>{{ store.error }}</span><button type="button" class="text-button" :disabled="editingBusy" @click="reload">重新加载</button></div>
      <template v-if="store.project">
        <div class="practice-project-toolbar"><label>实践项目<select :value="store.project.id" :disabled="editingBusy" aria-label="切换实践项目" @change="changeProject(($event.target as HTMLSelectElement).value)"><option v-for="item in store.projects" :key="item.id" :value="item.id">{{ item.title }}</option></select></label><span>更新于 {{ dateLabel(store.project.updatedAt) }}</span></div>
        <section class="practice-overview">
          <div class="practice-overview-copy"><p class="eyebrow">从实践中校验判断</p><h1>{{ store.project.title }}</h1><p>{{ store.project.goal }}</p><div class="practice-overview-meta"><span><FileCheck2 :size="14" />输入版本 {{ store.project.inputRevision }}</span><span>{{ store.project.evidence.length }} 份成果材料</span><span v-if="isDemo">合成演示样例</span></div></div>
          <div class="practice-overview-status"><strong v-if="store.review">{{ store.supportedRequired }}<span>/ {{ requiredCount }}</span></strong><strong v-else>待检查</strong><span>{{ store.review ? `复盘 ${store.review.number} 的必需项有材料支持` : '先确认标准，再提交成果材料' }}</span><button type="button" class="secondary-button compact" :disabled="editingBusy" @click="openProject(false)"><Pencil :size="15" /> 编辑目标与投入</button></div>
        </section>

        <section class="ai-input-band practice-ai-band">
          <div class="ai-label"><span><Sparkles :size="18" /></span><div><strong>把目标变成验收标准</strong><small>生成候选，核对后保存为可检查的清单</small></div></div>
          <div class="practice-ai-action"><p>{{ store.project.goal }}</p><button type="button" class="primary-button" :disabled="editingBusy" @click="store.suggest"><RefreshCw v-if="store.busy === 'suggesting'" :size="16" class="spin-icon" /><Sparkles v-else :size="16" />{{ store.busy === 'suggesting' ? '生成中' : '生成验收候选' }}</button></div>
          <div v-if="store.suggestions" class="practice-suggestion-summary"><Info :size="16" /><span>{{ store.suggestions.providerNotice || store.suggestions.assumptions.join('；') }}</span><button type="button" class="secondary-button compact" :disabled="editingBusy || store.suggestionRevision !== store.project.inputRevision" @click="openCriteria(true)">核对 {{ store.suggestions.candidates.length }} 项候选</button></div>
        </section>

        <div class="practice-workspace">
          <section class="practice-review-section">
            <div class="section-heading compact-heading practice-review-heading"><div><p class="eyebrow">验收清单</p><h2>{{ store.review ? `复盘版本 ${store.review.number}：材料支持哪些结果` : '这次实践需要证明什么' }}</h2></div><div class="practice-heading-actions"><button type="button" class="secondary-button compact" :disabled="editingBusy" @click="openCriteria()"><Pencil :size="15" /> 编辑标准</button><button type="button" class="primary-button compact" :disabled="editingBusy" @click="store.check()"><RefreshCw :size="16" :class="{ 'spin-icon': store.busy === 'reviewing' }" />{{ store.busy === 'reviewing' ? '检查中' : '检查材料' }}</button></div></div>
            <div v-if="store.isStale" class="practice-alert"><Info :size="17" /><span>当前显示历史输入版本 {{ store.review?.inputRevision }}。材料或标准已变化，需重新检查后确认。</span></div>
            <div v-if="store.review" class="practice-review-source"><span>{{ store.review.source === 'ai' ? 'AI 材料检查' : '确定性材料检查' }} · {{ dateLabel(store.review.createdAt) }}</span><span v-if="store.review.confirmation"><CheckCircle2 :size="14" />已确认</span></div>
            <ReviewFindings :criteria="store.displayCriteria" :review="store.review" :previous="store.previousReview" :selected-id="store.selectedCriterionId" :busy="editingBusy" :overrides="overrides" :can-edit="store.canConfirm" @select="store.selectedCriterionId = $event" @cite="cite" @overrides="overrides = $event" />
            <div class="practice-check-actions"><div><p>检查依据与原文一起保存。</p><small>补交材料后重新检查，原复盘留在历史中。</small></div></div>
            <div v-if="store.pendingReview && !editingBusy" class="practice-retry"><p>上次检查结果尚未收到，可使用原请求重试。</p><button type="button" class="secondary-button compact" @click="store.check(true)">重试上次检查</button></div>
            <p v-if="store.review?.providerNotice" class="practice-provider-notice"><Info :size="14" />{{ store.review.providerNotice }}</p>
          </section>

          <section class="practice-evidence-section">
            <div class="practice-evidence-heading"><div><p class="eyebrow">成果证据</p><h2>核对原始材料</h2></div><button type="button" class="icon-button" aria-label="添加成果证据" title="添加成果证据" :disabled="editingBusy || store.project.evidence.length >= 10" @click="openEvidence(null)"><Plus :size="18" /></button></div>
            <div v-if="store.review" class="practice-evidence-mode"><span>{{ historicalEvidence ? `复盘 ${store.review.number} 的材料快照` : '当前保存的材料' }}</span><button type="button" class="text-button" @click="switchEvidenceSnapshot">{{ historicalEvidence ? '看当前材料' : '看复盘快照' }}</button></div>
            <div v-if="visibleEvidence.length" class="practice-evidence-list"><div v-for="item in visibleEvidence" :key="item.id" class="practice-evidence-item" :class="{ 'practice-evidence-active': selectedEvidenceId === item.id }"><button type="button" class="practice-evidence-select" @click="chooseEvidence(item)"><FileCheck2 :size="16" /><span><strong>{{ item.title }}</strong><small>{{ item.kind === 'note' ? '说明' : item.kind === 'config' ? '配置' : '日志' }} · 版本 {{ item.revision }}</small></span></button><button v-if="!historicalEvidence" type="button" class="icon-button practice-small-button" :aria-label="`编辑证据：${item.title}`" :title="`编辑 ${item.title}`" :disabled="editingBusy" @click="openEvidence(item)"><Pencil :size="14" /></button><button v-if="!historicalEvidence" type="button" class="icon-button practice-small-button" :aria-label="`移除证据：${item.title}`" :title="`移除 ${item.title}`" :disabled="editingBusy" @click="remove(item)"><Trash2 :size="14" /></button></div></div>
            <p v-else class="practice-empty-evidence">尚无成果材料。添加说明、配置或日志后开始检查。</p>
            <EvidenceViewer :evidence="activeEvidence" :citation="activeCitation" :historical="historicalEvidence" />
          </section>
        </div>

        <section class="practice-section"><div class="section-heading compact-heading"><div><p class="eyebrow">预期与实际</p><h2>投入发生了什么变化</h2></div><button type="button" class="secondary-button compact" :disabled="editingBusy" @click="openProject(false)"><Pencil :size="15" /> 记录投入</button></div><MetricComparison :comparisons="store.project.comparisons" /><p v-if="store.project.processNote" class="practice-process-note"><strong>用户过程说明</strong>{{ store.project.processNote }}</p><p class="practice-hint">显示当前保存的投入。比较结果由程序计算，过程说明是用户记录。</p><details v-if="store.review && store.isStale" class="practice-input-snapshot"><summary>查看复盘 {{ store.review.number }} 的投入快照</summary><MetricComparison :comparisons="store.review.input.comparisons" /><p v-if="store.review.input.processNote" class="practice-process-note"><strong>当时的过程说明</strong>{{ store.review.input.processNote }}</p></details></section>

        <section class="practice-section practice-source-section"><div class="section-heading compact-heading"><div><p class="eyebrow">原方案参考</p><h2>关联事前决策</h2></div><button type="button" class="secondary-button compact" :disabled="editingBusy" @click="openSource"><Link2 :size="15" /> 关联方案</button></div><div v-if="store.project.decisionOrigin" class="practice-origin"><Link2 :size="20" /><div><strong>{{ store.project.decisionOrigin.snapshot.title }} · {{ store.project.decisionOrigin.snapshot.optionName }}</strong><p>{{ store.project.decisionOrigin.snapshot.goal }}</p><span>原工期 {{ store.project.decisionOrigin.snapshot.timeRange.min }}–{{ store.project.decisionOrigin.snapshot.timeRange.max }} 天 · 原估算成本 {{ store.project.decisionOrigin.snapshot.budgetRange.min }}–{{ store.project.decisionOrigin.snapshot.budgetRange.max }} 元</span><details><summary>查看原方案假设</summary><ul><li v-for="assumption in store.project.decisionOrigin.snapshot.assumptions" :key="assumption">{{ assumption }}</li></ul></details></div></div><p v-else class="practice-hint">可以从决策沙盒选择历史方案，保存当时的估算与假设。当前实践也可以独立运行。</p></section>

        <section class="practice-section"><div class="section-heading compact-heading"><div><p class="eyebrow">可追溯记录</p><h2>查看复盘与补验变化</h2></div><span><History :size="15" /> {{ store.history.length }} 个版本</span></div><ReviewHistory :history="store.history" :review="store.review" :previous="store.previousReview" :busy="editingBusy" @select="store.selectReview" @compare="store.compare" /></section>

        <section v-if="store.review" class="practice-section practice-feedback-section"><div class="section-heading compact-heading"><div><p class="eyebrow">确认与反馈</p><h2>把这次结果用于下一次行动</h2></div><button v-if="!store.review.confirmation" type="button" class="primary-button compact" :disabled="!store.canConfirm" @click="store.confirm(overrides)"><CheckCircle2 :size="16" />{{ store.busy === 'confirming' ? '确认中' : '确认本次复盘' }}</button><span v-else><CheckCircle2 :size="16" />已确认于 {{ dateLabel(store.review.confirmation.confirmedAt) }}</span></div><FeedbackPanel :key="store.review.id" :review="store.review" :feedback="store.feedback" :stale="store.isStale" :busy="editingBusy" :save="store.saveFeedback" @notice="store.notify" /></section>
      </template>
      <section v-else-if="!store.error" class="state-panel practice-empty-project"><ClipboardCheck :size="32" /><h1>开始一项真实实践</h1><p>先确定验收标准，再用成果材料检查结果。</p><button type="button" class="primary-button" @click="openProject(true)"><Plus :size="17" /> 新建实践</button></section>
    </div>

    <Transition name="toast"><div v-if="store.notice" class="toast-message" role="status"><CheckCircle2 :size="18" />{{ store.notice }}</div></Transition>
    <ProjectEditor v-if="projectEditor" :project="store.project" :templates="store.templates" :create="createMode" :busy="editingBusy" :error="store.error" :save="saveProject" @close="projectEditor = false" />
    <CriteriaEditor v-if="criteriaEditor && store.project" :project="store.project" :suggestions="useSuggestions ? store.suggestions : null" :busy="editingBusy" :error="store.error" :save="store.saveCriteria" @close="criteriaEditor = false" />
    <EvidenceEditor v-if="evidenceEditor && store.project" :project="store.project" :evidence="editingEvidence" :busy="editingBusy" :error="store.error" :save="store.saveEvidence" @close="evidenceEditor = false" />
    <DecisionSourceDialog v-if="sourceEditor" :scenarios="store.sourceScenarios" :versions="store.sourceVersions" :busy="editingBusy" :error="store.error" :load-versions="store.loadSourceVersions" :link="store.linkSource" @close="sourceEditor = false" />
  </main>
</template>
