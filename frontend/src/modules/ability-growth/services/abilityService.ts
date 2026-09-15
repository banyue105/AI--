import type {
  AbilityGraphData,
  GrowthPathStep,
  ParseResult,
  SkillLevel,
  SkillNode,
  SkillRelation,
  SkillStatus,
} from '../types'

const STORAGE_KEY = 'ican:ability-graph:v2'

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
    const stored = localStorage.getItem(STORAGE_KEY)
    return stored ? JSON.parse(stored) : cloneSeed()
  } catch {
    return cloneSeed()
  }
}

function writeLocal(data: AbilityGraphData) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(data))
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const controller = new AbortController()
  const timeout = window.setTimeout(() => controller.abort(), 900)
  try {
    const response = await fetch(`/api/v1${path}`, {
      ...init,
      signal: controller.signal,
      headers: { 'Content-Type': 'application/json', ...init?.headers },
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
      await request('/ability/skills', { method: 'POST', body: JSON.stringify(node) })
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

  async addRelation(relation: SkillRelation): Promise<AbilityGraphData> {
    const graph = readLocal()
    if (!graph.relations.some((item) => item.from === relation.from && item.to === relation.to)) {
      graph.relations.push(relation)
    }
    graph.updatedAt = new Date().toISOString()
    writeLocal(graph)
    return graph
  },

  async parseInput(input: string): Promise<ParseResult> {
    try {
      return await request<ParseResult>('/ability/parse', { method: 'POST', body: JSON.stringify({ input }) })
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

  async generatePath(graph: AbilityGraphData): Promise<GrowthPathStep[]> {
    try {
      return await request<GrowthPathStep[]>('/ability/path', {
        method: 'POST',
        body: JSON.stringify({ nodes: graph.nodes, relations: graph.relations }),
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
  },
}
