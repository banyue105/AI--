import type { CriterionInput, EvidenceDraft, FeedbackItem, FeedbackRecord, ManualOverride, PracticeProject, PracticeTemplate, ProjectDraft, ProjectSummary, Review, ReviewSummary, SourceScenario, SourceVersion, Suggestions } from '../types'

export class PracticeApiError extends Error {
  constructor(message: string, readonly code: string, readonly status: number, readonly details: Record<string, string> = {}) { super(message) }
}

async function request<T>(path: string, init: RequestInit = {}, timeoutMs = 10000): Promise<T> {
  const controller = new AbortController()
  const timeout = setTimeout(() => controller.abort(), timeoutMs)
  try {
    const response = await fetch(`/api/v1${path}`, { ...init, signal: controller.signal, headers: { 'Content-Type': 'application/json', ...init.headers } })
    const data = await response.json().catch(() => null)
    if (!response.ok) throw new PracticeApiError(data?.error?.message || '服务暂不可用，请重试。', data?.error?.code || 'SERVICE_UNAVAILABLE', response.status, data?.error?.details || {})
    if (data === null) throw new PracticeApiError('服务返回了无法读取的数据，请重试。', 'INVALID_RESPONSE', response.status)
    return data as T
  } catch (error) {
    if (error instanceof PracticeApiError) throw error
    throw new PracticeApiError('暂时无法连接后端，输入草稿已保留。请确认后端运行后重试。', 'NETWORK_ERROR', 0)
  } finally { clearTimeout(timeout) }
}

const json = (method: string, payload: unknown): RequestInit => ({ method, body: JSON.stringify(payload) })
const base = (id: string) => `/practice/projects/${encodeURIComponent(id)}`

export const practiceService = {
  templates: () => request<PracticeTemplate[]>('/practice/templates'),
  list: () => request<ProjectSummary[]>('/practice/projects'),
  get: (id: string) => request<PracticeProject>(base(id)),
  create: (draft: ProjectDraft) => request<PracticeProject>('/practice/projects', json('POST', { title: draft.title, goal: draft.goal, templateId: draft.templateId })),
  update: (project: PracticeProject, draft: ProjectDraft) => request<PracticeProject>(base(project.id), json('PUT', { inputRevision: project.inputRevision, title: draft.title, goal: draft.goal, metrics: draft.metrics, processNote: draft.processNote })),
  suggest: (project: PracticeProject) => request<Suggestions>(`${base(project.id)}/criteria-suggestions`, json('POST', { inputRevision: project.inputRevision }), 35000),
  criteria: (project: PracticeProject, criteria: CriterionInput[]) => request<PracticeProject>(`${base(project.id)}/criteria`, json('PUT', { inputRevision: project.inputRevision, criteria })),
  saveEvidence: (project: PracticeProject, draft: EvidenceDraft) => request<PracticeProject>(`${base(project.id)}/evidence${draft.id ? `/${encodeURIComponent(draft.id)}` : ''}`, json(draft.id ? 'PUT' : 'POST', { inputRevision: project.inputRevision, title: draft.title, kind: draft.kind, content: draft.content, criterionIds: draft.criterionIds })),
  removeEvidence: (project: PracticeProject, id: string) => request<PracticeProject>(`${base(project.id)}/evidence/${encodeURIComponent(id)}?inputRevision=${project.inputRevision}`, { method: 'DELETE' }),
  review: (project: PracticeProject, requestId: string) => request<Review>(`${base(project.id)}/reviews`, json('POST', { inputRevision: project.inputRevision, requestId }), 40000),
  history: (id: string) => request<ReviewSummary[]>(`${base(id)}/reviews`),
  getReview: (id: string, reviewId: string) => request<Review>(`${base(id)}/reviews/${encodeURIComponent(reviewId)}`),
  confirm: (project: PracticeProject, review: Review, overrides: ManualOverride[]) => request<Review>(`${base(project.id)}/reviews/${encodeURIComponent(review.id)}/confirm`, json('POST', { inputRevision: project.inputRevision, overrides })),
  feedback: (id: string) => request<FeedbackRecord[]>(`${base(id)}/feedback`),
  saveFeedback: (projectId: string, reviewId: string, requestId: string, items: FeedbackItem[]) => request<FeedbackRecord[]>(`${base(projectId)}/feedback`, json('POST', { reviewId, requestId, items })),
  sourceScenarios: () => request<SourceScenario[]>('/decisions'),
  sourceVersions: (id: string) => request<SourceVersion[]>(`/decisions/${encodeURIComponent(id)}/versions`),
  linkDecision: (project: PracticeProject, scenarioId: string, versionId: string, optionKey: string) => request<PracticeProject>(`${base(project.id)}/decision-origin`, json('POST', { inputRevision: project.inputRevision, scenarioId, versionId, optionKey })),
}
