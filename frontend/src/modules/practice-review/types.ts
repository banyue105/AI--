export type Verdict = 'supported' | 'insufficient' | 'conflicting'
export type EvidenceKind = 'note' | 'config' | 'log'
export interface Criterion { id: string; title: string; standard: string; expectedEvidence: string; required: boolean; order: number }
export interface CriterionInput { id: string | null; title: string; standard: string; expectedEvidence: string; required: boolean }
export interface PracticeTemplate { id: string; title: string; description: string; criteria: Criterion[] }
export interface Evidence { id: string; revision: number; title: string; kind: EvidenceKind; content: string; criterionIds: string[]; updatedAt: string }
export interface Citation { evidenceId: string; evidenceRevision: number; startLine: number; endLine: number; quote: string }
export interface Finding { criterionId: string; verdict: Verdict; reason: string; citations: Citation[]; nextAction: string | null }
export interface Expected { min: number; max: number }
export interface InputMetric {
  metric: 'effort' | 'duration' | 'cost'; unit: 'hour' | 'day' | 'CNY'; scope: string; basis: string
  expected: Expected | null; actual: number | null; baselineTiming: 'before_practice' | 'retrospective'; baselineRecordedAt: string | null
}
export interface MetricComparison {
  metric: InputMetric['metric']; unit: string; scope: string; basis: string; expected: Expected | null; actual: number | null
  comparable: boolean; reason: string; position: 'below' | 'within' | 'above' | null
  deltaFromMin: number | null; deltaFromMax: number | null; percentDelta: number | null; baselineTiming: string
}
export interface SourceRange { min: number; max: number; unit: string }
export interface DecisionOrigin {
  scenarioId: string; versionId: string; optionKey: 'baseline' | 'changed'; capturedAt: string
  snapshot: { title: string; goal: string; optionName: string; timeRange: SourceRange; budgetRange: SourceRange; assumptions: string[] }
}
export interface PracticeProject {
  id: string; title: string; goal: string; templateId: string; inputRevision: number
  criteria: Criterion[]; evidence: Evidence[]; metrics: InputMetric[]; processNote: string; decisionOrigin: DecisionOrigin | null
  createdAt: string; updatedAt: string; comparisons: MetricComparison[]
}
export interface ProjectSummary { id: string; title: string; goal: string; inputRevision: number; evidenceCount: number; reviewCount: number; updatedAt: string }
export interface ManualOverride { criterionId: string; verdict: Verdict; reason: string; citations: Citation[] }
export interface Review {
  id: string; projectId: string; number: number; inputRevision: number; input: PracticeProject; findings: Finding[]
  source: 'ai' | 'mock'; providerNotice: string | null; createdAt: string
  confirmation: { confirmedAt: string; overrides: ManualOverride[] } | null; isStale: boolean
}
export interface ReviewSummary { id: string; number: number; inputRevision: number; source: 'ai' | 'mock'; createdAt: string; confirmedAt: string | null; isStale: boolean }
export interface FeedbackItem { target: 'ability' | 'decision'; targetId: string | null; targetName: string | null; title: string; note: string; criterionIds: string[]; evidenceIds: string[] }
export interface FeedbackRecord extends FeedbackItem { id: string; projectId: string; reviewId: string; delivery: 'reference_only' | 'pending' | 'applied' | 'failed'; externalRecordId: string | null; confirmedAt: string }
export interface Suggestions { candidates: Omit<CriterionInput, 'id'>[]; assumptions: string[]; source: 'ai' | 'mock'; providerNotice: string | null }
export interface ProjectDraft { title: string; goal: string; templateId: string; metrics: InputMetric[]; processNote: string }
export interface EvidenceDraft { id: string | null; title: string; kind: EvidenceKind; content: string; criterionIds: string[] }
export interface SourceScenario { id: string; title: string; goal: string }
export interface SourceVersion { id: string; number: number; results: Array<{ optionKey: 'baseline' | 'changed'; optionName: string; timeRange: SourceRange; budgetRange: SourceRange }> }
