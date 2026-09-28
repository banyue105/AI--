import aiService from './aiService'
import type {
  AbilityGraphData,
  Evidence,
  GrowthPathStep,
  ParseResult,
  SkillLevel,
  SkillNode,
  SkillRelation,
  SkillStatus,
} from '../types'

const LEGACY_STORAGE_KEY = 'ican:ability-graph:v2'
const REQUEST_TIMEOUT_MS = 35000

const seed: AbilityGraphData = {
  nodes: [
    { id: 'python', name: 'Python', description: '脚本与后端开发基础', level: 3, status: 'mastered', evidenceIds: ['ev-python'], x: 48, y: 72 },
    { id: 'linux', name: 'Linux', description: '常用命令与系统管理', level: 2, status: 'mastered', evidenceIds: ['ev-linux'], x: 48, y: 202 },
    { id: 'tcpip', name: 'TCP/IP', description: '网络分层与传输协议', level: 2, status: 'developing', evidenceIds: [], x: 248, y: 72 },
    { id: 'git', name: 'Git 协作', description: '版本控制与团队工作流', level: 2, status: 'mastered', evidenceIds: [], x: 248, y: 202 },
    { id: 'http', name: 'HTTP 服务', description: '请求、响应与接口设计', level: 1, status: 'developing', evidenceIds: [], x: 450, y: 72 },
    { id: 'shell', name: 'Shell 自动化', description: '部署脚本与环境配置', level: 1, status: 'developing', evidenceIds: [], x: 450, y: 202 },
    { id: 'docker', name: '容器化', description: '镜像、容器与 Compose', level: 0, status: 'gap', evidenceIds: [], x: 650, y: 202 },
    { id: 'security', name: '服务安全', description: '权限、密钥与攻击面', level: 0, status: 'gap', evidenceIds: [], x: 650, y: 332 },
    { id: 'monitor', name: '监控诊断', description: '日志、指标与故障定位', level: 0, status: 'gap', evidenceIds: [], x: 850, y: 72 },
    { id: 'deploy', name: '网络服务部署', description: '独立完成可复现的服务上线', level: 0, status: 'target', evidenceIds: [], x: 850, y: 202 },
  ],
  relations: [
    { from: 'python', to: 'http', type: 'prerequisite', confidence: 0.95 },
    { from: 'tcpip', to: 'http', type: 'prerequisite', confidence: 0.9 },
    { from: 'linux', to: 'shell', type: 'prerequisite', confidence: 0.92 },
    { from: 'git', to: 'shell', type: 'related', confidence: 0.72 },
    { from: 'shell', to: 'docker', type: 'prerequisite', confidence: 0.88 },
    { from: 'tcpip', to: 'security', type: 'prerequisite', confidence: 0.84 },
    { from: 'http', to: 'monitor', type: 'prerequisite', confidence: 0.85 },
    { from: 'docker', to: 'deploy', type: 'prerequisite', confidence: 0.95 },
    { from: 'security', to: 'deploy', type: 'prerequisite', confidence: 0.86 },
    { from: 'monitor', to: 'deploy', type: 'prerequisite', confidence: 0.9 },
  ],
  evidence: [
    { id: 'ev-python', title: '课程项目', note: '使用 FastAPI 完成数据接口', createdAt: '2026-09-10' },
    { id: 'ev-linux', title: '实践记录', note: '独立配置 Linux 开发环境', createdAt: '2026-09-07' },
  ],
  updatedAt: '2026-09-14T21:32:00+08:00',
  source: 'mock',
  goal: { title: '独立完成一个可复现的网络服务部署', deadline: '2026-11-13' },
}

function cloneSeed() {
  return structuredClone(seed)
}

function readLocal(): AbilityGraphData {
  try {
    const scopedKey = storageKey()
    const stored = localStorage.getItem(scopedKey)
      ?? (currentUserId() === 'demo-user' ? localStorage.getItem(LEGACY_STORAGE_KEY) : null)
    if (stored && !localStorage.getItem(scopedKey)) localStorage.setItem(scopedKey, stored)
    return stored ? JSON.parse(stored) : cloneSeed()
  } catch {
    return cloneSeed()
  }
}

function writeLocal(data: AbilityGraphData) {
  localStorage.setItem(storageKey(), JSON.stringify(data))
}

function currentUserId() {
  try { return localStorage.getItem('ican:user-id')?.trim() || 'demo-user' } catch { return 'demo-user' }
}

function storageKey() {
  return LEGACY_STORAGE_KEY + ':' + currentUserId()
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const controller = new AbortController()
  const timeout = window.setTimeout(() => controller.abort(), REQUEST_TIMEOUT_MS)
  try {
    const response = await fetch(`/api/v1${path}`, {
      ...init,
      signal: controller.signal,
      headers: { 'Content-Type': 'application/json', 'X-User-Id': currentUserId(), ...init?.headers },
    })
    if (!response.ok) throw new Error(`API ${response.status}`)
    return await response.json()
  } finally {
    window.clearTimeout(timeout)
  }
}

export const abilityService = {
  async getGraph(): Promise<AbilityGraphData> {
    try {
      return await request<AbilityGraphData>('/ability/graph')
    } catch {
      return readLocal()
    }
  },

  async saveNode(node: SkillNode): Promise<AbilityGraphData> {
    try {
      const remote = await request<AbilityGraphData>('/ability/skills', { method: 'POST', body: JSON.stringify(node) })
      writeLocal(remote)
      return remote
    } catch {
      // The local fallback remains fully usable while the backend is unavailable.
    }
    const graph = readLocal()
    const index = graph.nodes.findIndex((item) => item.id === node.id || item.name.toLowerCase() === node.name.toLowerCase())
    if (index >= 0) graph.nodes[index] = { ...node, id: graph.nodes[index].id, x: graph.nodes[index].x, y: graph.nodes[index].y }
    else graph.nodes.push(node)
    graph.updatedAt = new Date().toISOString()
    writeLocal(graph)
    return graph
  },

  async deleteNodes(nodeIds: string[]): Promise<AbilityGraphData> {
    const ids = [...new Set(nodeIds)]
    await Promise.allSettled(ids.map((nodeId) =>
      request(`/ability/skills/${encodeURIComponent(nodeId)}`, { method: 'DELETE' }),
    ))
    const idSet = new Set(ids)
    const graph = readLocal()
    graph.nodes = graph.nodes.filter((node) => !idSet.has(node.id))
    graph.relations = graph.relations.filter((relation) => !idSet.has(relation.from) && !idSet.has(relation.to))
    graph.updatedAt = new Date().toISOString()
    writeLocal(graph)
    return graph
  },
  async deleteNode(nodeId: string): Promise<AbilityGraphData> {
    try {
      const remote = await request<AbilityGraphData>(`/ability/skills/${encodeURIComponent(nodeId)}`, { method: 'DELETE' })
      writeLocal(remote)
      return remote
    } catch {
      // The local fallback remains fully usable while the backend is unavailable.
    }
    const graph = readLocal()
    graph.nodes = graph.nodes.filter((node) => node.id !== nodeId)
    graph.relations = graph.relations.filter((relation) => relation.from !== nodeId && relation.to !== nodeId)
    graph.updatedAt = new Date().toISOString()
    writeLocal(graph)
    return graph
  },
  async addEvidence(skillId: string, input: Pick<Evidence, 'title' | 'note'>): Promise<AbilityGraphData> {
    const evidence: Evidence = {
      id: `evidence-${Date.now()}`,
      title: input.title.trim(),
      note: input.note.trim(),
      createdAt: new Date().toISOString(),
    }
    try {
      await request(`/ability/skills/${encodeURIComponent(skillId)}/evidence`, {
        method: 'POST',
        body: JSON.stringify(evidence),
      })
    } catch {
      // Keep practice records usable locally until the backend evidence API is available.
    }
    const graph = readLocal()
    const node = graph.nodes.find((item) => item.id === skillId)
    if (!node) throw new Error('未找到需要记录实践的技能节点')
    graph.evidence.push(evidence)
    node.evidenceIds = [...new Set([...node.evidenceIds, evidence.id])]
    graph.updatedAt = new Date().toISOString()
    writeLocal(graph)
    return graph
  },

  async addRelation(relation: SkillRelation): Promise<AbilityGraphData> {
    try {
      const remote = await request<AbilityGraphData>('/ability/relations', { method: 'POST', body: JSON.stringify(relation) })
      writeLocal(remote)
      return remote
    } catch {
      // Keep the relationship available in the local demo when the backend is unavailable.
    }
    const graph = readLocal()
    if (!graph.relations.some(
      (item) => item.from === relation.from && item.to === relation.to && item.type === relation.type,
    )) {
      graph.relations.push(relation)
    }
    graph.updatedAt = new Date().toISOString()
    writeLocal(graph)
    return graph
  },

  async parseInput(input: string, existingNodes: SkillNode[] = [], existingRelations: SkillRelation[] = []): Promise<ParseResult> {
    try {
      return await request<ParseResult>('/ability/parse', { method: 'POST', body: JSON.stringify({ input, existingNodes, existingRelations }) })
    } catch {
      const text = input.toLowerCase()
      const candidates: Array<{ key: string; name: string; level: SkillLevel; status: SkillStatus }> = [
        { key: 'python', name: 'Python', level: 2, status: 'mastered' },
        { key: 'linux', name: 'Linux', level: 2, status: 'mastered' },
        { key: 'tcp', name: 'TCP/IP', level: 1, status: 'developing' },
        { key: 'docker', name: '容器化', level: 0, status: 'gap' },
      ]
      const detected = candidates.filter((item) => text.includes(item.key) || input.includes(item.name))
      const baseX = 120
      const suggestedNodes = detected.map((item, index) => ({
        id: `parsed-${item.key}-${Date.now()}`,
        name: item.name,
        description: '由自然语言输入提取，保存前请确认',
        level: item.level,
        status: item.status,
        evidenceIds: [],
        x: baseX + index * 190,
        y: 360,
      }))
      return {
        summary: detected.length
          ? `识别到 ${detected.length} 项能力信息，已转换为可确认的结构化节点。`
          : '没有识别出明确技能，请补充技能名称或手动新增节点。',
        suggestedNodes,
        suggestedRelations: [],
        assumptions: ['“会/掌握”暂按能实践处理', 'AI 提取仅是候选信息，不代表真实掌握程度'],
      }
    }
  },

  async generatePath(graph: AbilityGraphData, targetSkillId?: string | null): Promise<GrowthPathStep[]> {
    const scope = new Set<string>()
    const collect = (id: string) => {
      if (scope.has(id)) return
      scope.add(id)
      graph.relations.filter((relation) => relation.to === id).forEach((relation) => collect(relation.from))
    }
    if (targetSkillId) collect(targetSkillId)
    const pathNodes = targetSkillId ? graph.nodes.filter((node) => scope.has(node.id)) : graph.nodes
    const pathRelations = targetSkillId ? graph.relations.filter((relation) => scope.has(relation.from) && scope.has(relation.to)) : graph.relations
    try {
      const aiPath = await aiService.generateExplainablePath({
        goalId: 'current-goal',
        goalTitle: graph.goal.title,
        skillIds: pathNodes.map((node) => node.id),
      })
      const byId = new Map(pathNodes.map((node) => [node.id, node]))
      const edges = aiPath.edges ?? []
      const steps = aiPath.nodes
        .filter((node) => byId.has(node.skillId))
        .map((node) => {
          const current = byId.get(node.skillId)!
          const prerequisiteIds = edges.filter((edge) => edge.to === node.skillId && byId.has(edge.from)).map((edge) => edge.from)
          const blocked = prerequisiteIds.some((id) => (byId.get(id)?.level ?? 0) < 1)
          return {
            skillId: node.skillId,
            prerequisiteIds,
            status: current.level >= 2 ? 'done' : blocked ? 'blocked' : 'next',
            reason: node.description || edges.find((edge) => edge.to === node.skillId)?.reason || '根据 AI 分析的目标依赖关系安排。',
          } satisfies GrowthPathStep
        })
      if (steps.length) return steps
      throw new Error('AI path returned no usable nodes')
    } catch {
      try {
        return await request<GrowthPathStep[]>('/ability/path', {
          method: 'POST',
          body: JSON.stringify({ nodes: pathNodes, relations: pathRelations }),
        })
      } catch {
        const orderedIds = ['tcpip', 'http', 'shell', 'docker', 'security', 'monitor', 'deploy']
        return orderedIds
          .map((skillId) => {
            const node = graph.nodes.find((item) => item.id === skillId)
            if (!node) return null
            const prerequisiteIds = graph.relations.filter((item) => item.to === skillId).map((item) => item.from)
            const blocked = prerequisiteIds.some((id) => (graph.nodes.find((item) => item.id === id)?.level ?? 0) < 1)
            return {
              skillId,
              prerequisiteIds,
              status: node.level >= 2 ? 'done' : blocked ? 'blocked' : 'next',
              reason: node.level >= 2
                ? '已有基础证据，可作为后续能力的支点'
                : blocked
                  ? '前置能力尚未达到可实践状态'
                  : '前置条件基本具备，优先补齐可缩短目标路径',
            } satisfies GrowthPathStep
          })
          .filter((item): item is GrowthPathStep => item !== null)
      }
    }
  },
}
