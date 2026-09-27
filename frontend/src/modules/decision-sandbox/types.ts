export type RiskLevel = 'low' | 'medium' | 'high'
export type BusyAction = 'loading' | 'saving' | 'parsing' | 'simulating' | 'comparing' | 'restoring' | null

export interface DecisionResourceInput {
  id?: string
  type: 'person' | 'money' | 'equipment' | 'skill' | 'other'
  label: string
  quantity?: number | null
  unit?: string
}

export interface DecisionRelationInput {
  id: string
  from: string
  to: string
  label: string
  confidence: number
  assumption: string
}

export interface DecisionScenarioInput {
  title: string
  goal: string
  timeLimitDays: number
  budgetYuan: number
  peopleCount: number
  hasServer: boolean
  changeRequest: string
  resources: DecisionResourceInput[]
  relations: DecisionRelationInput[]
}

export interface DecisionConstraint {
  id: string
  type: string
  label: string
  value: string
  source: string
  confidence: number
  assumption: string
}

export interface DecisionResource extends DecisionResourceInput {
  id: string
  source: string
  confidence: number
  assumption: string
}

export interface DecisionNode {
  id: string
  type: 'goal' | 'resource' | 'task' | 'constraint' | 'risk'
  label: string
  source: string
  confidence: number
  assumption: string
}

export interface DecisionRelation extends DecisionRelationInput {
  source: string
}

export interface DecisionRange {
  min: number
  max: number
  unit: string
}

export interface SimulationResult {
  id: string
  scenarioId: string
  optionKey: 'baseline' | 'changed'
  optionName: string
  timeRange: DecisionRange
  budgetRange: DecisionRange
  peopleRange: DecisionRange
  resources: string[]
  riskLevel: RiskLevel
  assumptions: string[]
  impacts: string[]
  source: 'rule' | 'ai' | 'mock'
  explanation: string
  explanationSource: 'ai' | 'mock'
}

export interface DecisionScenario extends DecisionScenarioInput {
  id: string
  constraints: DecisionConstraint[]
  resources: DecisionResource[]
  nodes: DecisionNode[]
  relations: DecisionRelation[]
  latestResults: SimulationResult[]
  versionCount: number
  revision: number
  createdAt: string
  updatedAt: string
}

export interface DecisionSummary {
  id: string
  title: string
  goal: string
  updatedAt: string
  versionCount: number
}

export interface DecisionSuggestion {
  kind: 'people' | 'budget' | 'time' | 'server' | 'change'
  label: string
  value: string
  confidence: number
  assumption: string
  source: 'ai' | 'mock'
  selected?: boolean
}

export interface DecisionParseResult {
  summary: string
  candidates: DecisionSuggestion[]
  assumptions: string[]
  source: 'ai' | 'mock'
}

export interface DecisionVersion {
  id: string
  number: number
  scenarioRevision: number
  createdAt: string
  changeSummary: string
  snapshot: DecisionScenario
  results: SimulationResult[]
}

export interface DecisionComparison {
  leftVersionId: string
  rightVersionId: string
  summary: string
  changedInputs: string[]
  timeMinDelta: number
  timeMaxDelta: number
  budgetMinDelta: number
  budgetMaxDelta: number
  riskChange: string
}
