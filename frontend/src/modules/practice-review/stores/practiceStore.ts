import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { PracticeApiError, practiceService } from '../services/practiceService'
import type { CriterionInput, EvidenceDraft, FeedbackItem, FeedbackRecord, ManualOverride, PracticeProject, PracticeTemplate, ProjectDraft, ProjectSummary, Review, ReviewSummary, SourceScenario, SourceVersion, Suggestions } from '../types'

export const usePracticeStore = defineStore('practice-review', () => {
  const templates = ref<PracticeTemplate[]>([])
  const projects = ref<ProjectSummary[]>([])
  const project = ref<PracticeProject | null>(null)
  const history = ref<ReviewSummary[]>([])
  const review = ref<Review | null>(null)
  const previousReview = ref<Review | null>(null)
  const feedback = ref<FeedbackRecord[]>([])
  const suggestions = ref<Suggestions | null>(null)
  const suggestionRevision = ref<number | null>(null)
  const sourceScenarios = ref<SourceScenario[]>([])
  const sourceVersions = ref<SourceVersion[]>([])
  const busy = ref<string | null>(null)
  const error = ref('')
  const notice = ref('')
  const loaded = ref(false)
  const selectedCriterionId = ref('')
  const pendingReview = ref<{ projectId: string; inputRevision: number; requestId: string } | null>(null)
  const feedbackRetry = ref<{ signature: string; requestId: string } | null>(null)
  let noticeTimer: ReturnType<typeof setTimeout> | undefined

  const isStale = computed(() => !!review.value && review.value.inputRevision !== project.value?.inputRevision)
  const canConfirm = computed(() => !!review.value && !isStale.value && !review.value.confirmation && !busy.value)
  const displayCriteria = computed(() => review.value?.input.criteria || project.value?.criteria || [])
  const supportedRequired = computed(() => {
    if (!review.value) return 0
    const overrides = review.value.confirmation?.overrides || []
    return review.value.input.criteria.filter(criterion => criterion.required &&
      (overrides.find(item => item.criterionId === criterion.id)?.verdict || review.value?.findings.find(item => item.criterionId === criterion.id)?.verdict) === 'supported').length
  })

  function notify(message: string) {
    notice.value = message
    clearTimeout(noticeTimer)
    noticeTimer = setTimeout(() => { notice.value = '' }, 3500)
  }

  async function run(kind: string, action: () => Promise<void>): Promise<boolean> {
    if (busy.value) return false
    busy.value = kind; error.value = ''
    try { await action(); return true }
    catch (cause) { error.value = cause instanceof Error ? cause.message : '操作未完成，请重试。'; return false }
    finally { busy.value = null }
  }

  async function loadData(id: string) {
    const [data, versions, saved] = await Promise.all([practiceService.get(id), practiceService.history(id), practiceService.feedback(id)])
    const latest = versions[0] ? await practiceService.getReview(id, versions[0].id) : null
    project.value = data; history.value = versions; feedback.value = saved; review.value = latest; previousReview.value = null
    suggestions.value = null; suggestionRevision.value = null; sourceVersions.value = []
    selectedCriterionId.value = latest?.input.criteria[0]?.id || data.criteria[0]?.id || ''
    pendingReview.value = null
    try {
      const savedRequest = JSON.parse(sessionStorage.getItem(`practice:pending:${id}`) || 'null')
      if (savedRequest?.projectId === id && Number.isInteger(savedRequest.inputRevision) && typeof savedRequest.requestId === 'string') pendingReview.value = savedRequest
    } catch { /* An unreadable temporary request never replaces server data. */ }
  }

  async function init(id?: string) {
    await run('loading', async () => {
      const [templateList, projectList] = await Promise.all([practiceService.templates(), practiceService.list()])
      templates.value = templateList; projects.value = projectList
      const target = id || projectList[0]?.id
      if (target) await loadData(target)
      else { project.value = null; review.value = null; history.value = []; feedback.value = [] }
    })
    loaded.value = true
  }

  async function selectProject(id: string) { return run('loading', () => loadData(id)) }

  function applyProject(data: PracticeProject) {
    project.value = data
    if (review.value) review.value.isStale = review.value.inputRevision !== data.inputRevision
    history.value = history.value.map(item => ({ ...item, isStale: item.inputRevision !== data.inputRevision }))
  }

  async function refreshMetadata() {
    if (!project.value) return
    try {
      const [list, versions, saved] = await Promise.all([practiceService.list(), practiceService.history(project.value.id), practiceService.feedback(project.value.id)])
      projects.value = list; history.value = versions; feedback.value = saved
    } catch { error.value = '内容已保存，但列表更新失败。请重新加载查看最新记录。' }
  }

  async function saveProject(draft: ProjectDraft, create: boolean) {
    return run('saving', async () => {
      const data = create ? await practiceService.create(draft) : await practiceService.update(project.value!, draft)
      if (create) { review.value = null; previousReview.value = null; history.value = []; feedback.value = []; suggestions.value = null; pendingReview.value = null }
      applyProject(data); selectedCriterionId.value = data.criteria[0]?.id || ''; await refreshMetadata(); notify('实践项目已保存')
    })
  }

  async function suggest() {
    if (!project.value) return false
    return run('suggesting', async () => {
      const inputRevision = project.value!.inputRevision
      suggestions.value = await practiceService.suggest(project.value!)
      suggestionRevision.value = inputRevision
    })
  }

  async function saveCriteria(criteria: CriterionInput[]) {
    if (!project.value) return false
    return run('saving', async () => {
      applyProject(await practiceService.criteria(project.value!, criteria)); suggestions.value = null; suggestionRevision.value = null
      await refreshMetadata(); notify('验收标准已保存；旧复盘保留在历史中')
    })
  }

  async function saveEvidence(draft: EvidenceDraft) {
    if (!project.value) return false
    return run('saving', async () => { applyProject(await practiceService.saveEvidence(project.value!, draft)); await refreshMetadata(); notify('证据已保存，请检查更新后的材料') })
  }

  async function removeEvidence(id: string) {
    if (!project.value) return false
    return run('saving', async () => { applyProject(await practiceService.removeEvidence(project.value!, id)); await refreshMetadata(); notify('当前证据已移除，历史引用仍保留') })
  }

  async function check(retry = false) {
    if (!project.value) return false
    return run('reviewing', async () => {
      const current = project.value!
      const pending = retry && pendingReview.value?.projectId === current.id ? pendingReview.value : {
        projectId: current.id, inputRevision: current.inputRevision, requestId: crypto.randomUUID(),
      }
      pendingReview.value = pending
      try { sessionStorage.setItem(`practice:pending:${current.id}`, JSON.stringify(pending)) } catch { /* In-memory retries remain available. */ }
      try {
        review.value = await practiceService.review({ ...current, inputRevision: pending.inputRevision }, pending.requestId)
        previousReview.value = null; selectedCriterionId.value = review.value.input.criteria[0]?.id || ''
        pendingReview.value = null
        try { sessionStorage.removeItem(`practice:pending:${current.id}`) } catch { /* Server data remains authoritative. */ }
        await refreshMetadata(); notify('材料检查完成，点击引用核对依据')
      } catch (cause) {
        if (cause instanceof PracticeApiError && ['REVISION_CONFLICT', 'IDEMPOTENCY_CONFLICT'].includes(cause.code)) {
          pendingReview.value = null
          try { sessionStorage.removeItem(`practice:pending:${current.id}`) } catch { /* Server data remains authoritative. */ }
        }
        throw cause
      }
    })
  }

  async function selectReview(id: string) {
    if (!project.value) return false
    return run('history', async () => {
      review.value = await practiceService.getReview(project.value!.id, id)
      previousReview.value = null; selectedCriterionId.value = review.value.input.criteria[0]?.id || ''
    })
  }

  async function compare(id: string) {
    if (!project.value || !review.value || id === review.value.id || !id) { previousReview.value = null; return false }
    return run('history', async () => { previousReview.value = await practiceService.getReview(project.value!.id, id) })
  }

  async function confirm(overrides: ManualOverride[]) {
    if (!project.value || !review.value) return false
    return run('confirming', async () => { review.value = await practiceService.confirm(project.value!, review.value!, overrides); await refreshMetadata(); notify('复盘已确认，原检查结果与人工修正均已保留') })
  }

  async function saveFeedback(items: FeedbackItem[]) {
    if (!project.value || !review.value) return false
    return run('feedback', async () => {
      const signature = JSON.stringify([project.value!.id, review.value!.id, items])
      const requestKey = `practice:feedback-request:${review.value!.id}`
      if (feedbackRetry.value?.signature !== signature) {
        let saved: { signature: string; requestId: string } | null = null
        try { saved = JSON.parse(sessionStorage.getItem(requestKey) || 'null') } catch { /* Optional retry cache. */ }
        feedbackRetry.value = saved?.signature === signature && typeof saved.requestId === 'string'
          ? saved : { signature, requestId: crypto.randomUUID() }
      }
      try { sessionStorage.setItem(requestKey, JSON.stringify(feedbackRetry.value)) } catch { /* In-memory retry is available. */ }
      await practiceService.saveFeedback(project.value!.id, review.value!.id, feedbackRetry.value!.requestId, items)
      feedbackRetry.value = null
      try { sessionStorage.removeItem(requestKey) } catch { /* A stale cache cannot replace server data. */ }
      await refreshMetadata(); notify('反馈已保存为参考，尚未写入其他模块')
    })
  }

  async function loadSources() { return run('sources', async () => { sourceScenarios.value = await practiceService.sourceScenarios() }) }
  async function loadSourceVersions(id: string) { return run('sources', async () => { sourceVersions.value = []; sourceVersions.value = await practiceService.sourceVersions(id) }) }
  async function linkSource(scenarioId: string, versionId: string, optionKey: string) {
    if (!project.value) return false
    return run('saving', async () => { applyProject(await practiceService.linkDecision(project.value!, scenarioId, versionId, optionKey)); await refreshMetadata(); notify('选定方案已保存为固定快照') })
  }

  return { templates, projects, project, history, review, previousReview, feedback, suggestions, suggestionRevision,
    sourceScenarios, sourceVersions, busy, error, notice, loaded, selectedCriterionId, pendingReview,
    isStale, canConfirm, displayCriteria, supportedRequired, init, selectProject, saveProject, suggest, saveCriteria,
    saveEvidence, removeEvidence, check, selectReview, compare, confirm, saveFeedback, loadSources, loadSourceVersions, linkSource, notify }
})
