/**
 * AI-assisted operations for the ability-growth module.
 *
 * The service intentionally owns only transport and response types. UI state
 * and graph mutations stay in the page/store so the same operations can be
 * used from the ability tree, goal picker and explainable-path views.
 */

const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL || '/api/v1').replace(/\/$/, '')
const USER_ID_STORAGE_KEY = 'ican:user-id'

export interface AbilityUpdateSkill {
  id?: string
  name: string
  proficiency: number
  description?: string
}

export interface AbilityUpdateRelation {
  from: string
  to: string
  type: 'prerequisite' | 'related'
  confidence: number
}

export interface AbilityUpdateResponse {
  skills: AbilityUpdateSkill[]
  relations?: AbilityUpdateRelation[]
  message?: string
}

export interface SkillEnrichmentRelation {
  existingSkillId: string
  type: string
  reason?: string
}

export interface SkillEnrichmentQuestion {
  question: string
  options?: string[]
  answer?: string
  answerIndex?: number
  explanation?: string
}

export interface SkillEnrichmentResponse {
  skill: AbilityUpdateSkill
  relations: SkillEnrichmentRelation[]
  questions: SkillEnrichmentQuestion[]
}

export interface TechStackStage {
  name: string
  description?: string
  skills: Array<Pick<AbilityUpdateSkill, 'name' | 'proficiency'> & { description?: string }>
}

export interface TechStackResponse {
  direction: string
  stages: TechStackStage[]
}

export interface ExplainablePathNode {
  skillId: string
  title: string
  description?: string
  stage?: string
}

export interface ExplainablePathEdge {
  from: string
  to: string
  reason?: string
}

export interface ExplainablePathResponse {
  nodes: ExplainablePathNode[]
  edges: ExplainablePathEdge[]
  description?: string
}

export interface ExplainablePathRequest {
  goalId: string
  goalTitle?: string
  skillIds?: string[]
}

function currentUserId(): string {
  if (typeof window === 'undefined') return 'demo-user'
  return window.localStorage.getItem(USER_ID_STORAGE_KEY) || 'demo-user'
}

async function post<T>(path: string, body: unknown): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    method: 'POST',
    headers: {
      Accept: 'application/json',
      'Content-Type': 'application/json',
      'X-User-Id': currentUserId(),
    },
    body: JSON.stringify(body),
  })

  if (!response.ok) {
    let detail = ''
    try {
      const payload = await response.json() as { message?: string; error?: string }
      detail = payload.message || payload.error || ''
    } catch {
      // Keep the status text when the server did not return JSON.
    }
    throw new Error(detail || `AI request failed (${response.status})`)
  }

  return response.json() as Promise<T>
}

export const aiService = {
  updateAbility(statement: string): Promise<AbilityUpdateResponse> {
    return post<AbilityUpdateResponse>('/ai/ability-update', { statement })
  },

  enrichSkill(
    skillName: string,
    existingSkills: Array<{ id: string; name: string }>,
  ): Promise<SkillEnrichmentResponse> {
    return post<SkillEnrichmentResponse>('/ai/skill-enrichment', { skillName, existingSkills })
  },

  generateTechStack(statement: string): Promise<TechStackResponse> {
    return post<TechStackResponse>('/ai/tech-stack', { statement })
  },

  generateExplainablePath(request: ExplainablePathRequest): Promise<ExplainablePathResponse> {
    return post<ExplainablePathResponse>('/ai/explainable-path', request)
  },
}

export default aiService
