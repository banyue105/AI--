import type {
  DecisionComparison, DecisionParseResult, DecisionScenario, DecisionScenarioInput,
  DecisionSummary, DecisionVersion,
} from '../types'

const BASE = '/api/v1/decisions'

async function request<T>(path: string, method = 'GET', payload?: object): Promise<T> {
  const controller = new AbortController()
  const timeout = window.setTimeout(() => controller.abort(), 10000)
  try {
    const response = await fetch(`${BASE}${path}`, {
      method,
      signal: controller.signal,
      headers: { 'Content-Type': 'application/json' },
      body: payload ? JSON.stringify(payload) : undefined,
    })
    if (!response.ok) {
      const data = await response.json().catch(() => null)
      throw new Error(data?.error?.message || `接口返回 ${response.status}`)
    }
    return await response.json() as T
  } catch (error) {
    if (error instanceof DOMException && error.name === 'AbortError') throw new Error('请求超时，请检查后端服务。')
    if (error instanceof TypeError) throw new Error('无法连接后端。请启动 backend（demo 或 MySQL 模式）。')
    throw error
  } finally {
    window.clearTimeout(timeout)
  }
}

export const decisionService = {
  list: () => request<DecisionSummary[]>(''),
  get: (id: string) => request<DecisionScenario>(`/${id}`),
  create: (input: DecisionScenarioInput) => request<DecisionScenario>('', 'POST', input),
  update: (id: string, input: DecisionScenarioInput) => request<DecisionScenario>(`/${id}`, 'PUT', input),
  parseInput: (id: string, input: string) => request<DecisionParseResult>(`/${id}/parse`, 'POST', { input }),
  simulate: (id: string) => request<DecisionVersion>(`/${id}/simulate`, 'POST'),
  versions: (id: string) => request<DecisionVersion[]>(`/${id}/versions`),
  compare: (id: string, leftVersionId: string, rightVersionId: string) =>
    request<DecisionComparison>(`/${id}/compare`, 'POST', { leftVersionId, rightVersionId }),
  restore: (id: string, versionId: string) => request<DecisionScenario>(`/${id}/restore`, 'POST', { versionId }),
}
