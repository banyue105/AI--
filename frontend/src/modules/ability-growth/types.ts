export type SkillLevel = 0 | 1 | 2 | 3 | 4
export type SkillStatus = 'mastered' | 'developing' | 'gap' | 'target'

export interface SkillNode {
  id: string
  name: string
  description?: string
  level: SkillLevel
  status: SkillStatus
  evidenceIds: string[]
  x: number
  y: number
}

export interface SkillRelation {
  from: string
  to: string
  type: 'prerequisite' | 'related'
  confidence: number
}

export interface Evidence {
  id: string
  title: string
  note: string
  createdAt: string
}

export interface GrowthPathStep {
  skillId: string
  reason: string
  prerequisiteIds: string[]
  status: 'next' | 'blocked' | 'done'
}

export interface AbilityGraphData {
  nodes: SkillNode[]
  relations: SkillRelation[]
  evidence: Evidence[]
  updatedAt: string
  source: 'api' | 'mock'
  goal: {
    title: string
    deadline: string
  }
}

export interface ParseResult {
  summary: string
  suggestedNodes: SkillNode[]
  suggestedRelations: SkillRelation[]
  assumptions: string[]
}
