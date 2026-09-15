import type { SkillNode } from '../types'

export type KnowledgeTrackId = string
export type StackStageId = string

export interface KnowledgeStackItem {
  id: string
  name: string
  description: string
  aliases: string[]
  priority: 'required' | 'recommended'
}

export interface KnowledgeStackStage {
  id: StackStageId
  title: string
  description: string
  items: KnowledgeStackItem[]
}

export interface KnowledgeTrack {
  id: KnowledgeTrackId
  title: string
  shortTitle: string
  description: string
  outcome: string
  source: 'catalog' | 'ai' | 'mock'
  stages: KnowledgeStackStage[]
}

const fallbackCatalog: KnowledgeTrack[] = [
  {
    id: 'frontend',
    title: '前端工程师',
    shortTitle: '前端',
    description: '构建可访问、可维护且性能稳定的网页应用。',
    outcome: '能够独立完成中型 Web 应用并建立工程化交付流程',
    source: 'catalog',
    stages: [
      {
        id: 'foundation',
        title: 'Web 基础',
        description: '浏览器呈现与交互的底层能力',
        items: [
          { id: 'fe-html', name: 'HTML', description: '语义化结构、表单与可访问性', aliases: ['HTML5'], priority: 'required' },
          { id: 'fe-css', name: 'CSS', description: '布局、响应式与设计系统', aliases: ['CSS3'], priority: 'required' },
          { id: 'fe-js', name: 'JavaScript', description: '语言基础、DOM 与异步编程', aliases: ['JS'], priority: 'required' },
          { id: 'fe-browser', name: '浏览器原理', description: '渲染、事件循环与网络请求', aliases: [], priority: 'recommended' },
        ],
      },
      {
        id: 'core',
        title: '框架能力',
        description: '组件化开发与状态管理',
        items: [
          { id: 'fe-ts', name: 'TypeScript', description: '类型建模与大型项目维护', aliases: ['TS'], priority: 'required' },
          { id: 'fe-vue', name: 'Vue 3', description: '组合式 API 与组件设计', aliases: ['Vue'], priority: 'required' },
          { id: 'fe-router', name: '前端路由', description: '页面导航、鉴权与数据加载', aliases: ['Vue Router'], priority: 'recommended' },
          { id: 'fe-state', name: '状态管理', description: '跨组件状态与数据流设计', aliases: ['Pinia'], priority: 'recommended' },
        ],
      },
      {
        id: 'engineering',
        title: '工程交付',
        description: '从本地代码到稳定上线',
        items: [
          { id: 'fe-git', name: 'Git 协作', description: '分支、评审与版本管理', aliases: ['Git'], priority: 'required' },
          { id: 'fe-vite', name: '构建工具', description: 'Vite、依赖与环境配置', aliases: ['Vite'], priority: 'required' },
          { id: 'fe-test', name: '前端测试', description: '单元、组件与端到端测试', aliases: ['Vitest', 'Playwright'], priority: 'recommended' },
          { id: 'fe-perf', name: '性能优化', description: '性能指标、拆包与缓存策略', aliases: [], priority: 'recommended' },
        ],
      },
    ],
  },
  {
    id: 'backend',
    title: '后端工程师',
    shortTitle: '后端',
    description: '设计可靠接口、业务服务和数据持久化系统。',
    outcome: '能够设计并部署具备测试和可观测性的后端服务',
    source: 'catalog',
    stages: [
      {
        id: 'foundation',
        title: '编程基础',
        description: '服务端语言与计算机基础',
        items: [
          { id: 'be-java', name: 'Java', description: '面向对象、集合与并发基础', aliases: [], priority: 'required' },
          { id: 'be-ds', name: '数据结构', description: '常用结构与复杂度分析', aliases: ['算法'], priority: 'required' },
          { id: 'be-linux', name: 'Linux', description: '服务运行环境与常用命令', aliases: [], priority: 'required' },
          { id: 'be-network', name: 'TCP/IP', description: '传输协议与网络排障基础', aliases: ['网络基础'], priority: 'recommended' },
        ],
      },
      {
        id: 'core',
        title: '服务开发',
        description: '接口、框架与数据持久化',
        items: [
          { id: 'be-http', name: 'HTTP 服务', description: 'REST 接口与状态码设计', aliases: ['REST API', 'HTTP'], priority: 'required' },
          { id: 'be-spring', name: 'Spring Boot', description: '依赖注入、Web 与数据访问', aliases: ['Spring'], priority: 'required' },
          { id: 'be-mysql', name: 'MySQL', description: '关系建模、索引与事务', aliases: ['SQL'], priority: 'required' },
          { id: 'be-cache', name: 'Redis', description: '缓存、过期与一致性', aliases: [], priority: 'recommended' },
        ],
      },
      {
        id: 'engineering',
        title: '可靠交付',
        description: '测试、部署与运行保障',
        items: [
          { id: 'be-test', name: '服务测试', description: '单元测试与接口集成测试', aliases: ['JUnit'], priority: 'required' },
          { id: 'be-docker', name: '容器化', description: '镜像、容器与 Compose', aliases: ['Docker'], priority: 'required' },
          { id: 'be-security', name: '服务安全', description: '认证、授权与密钥管理', aliases: ['安全'], priority: 'recommended' },
          { id: 'be-monitor', name: '监控诊断', description: '日志、指标与链路追踪', aliases: ['可观测性'], priority: 'recommended' },
        ],
      },
    ],
  },
  {
    id: 'network',
    title: '网络工程师',
    shortTitle: '网络工程',
    description: '规划、部署并诊断稳定安全的网络与服务环境。',
    outcome: '能够完成中小型网络规划、服务部署与故障诊断',
    source: 'catalog',
    stages: [
      {
        id: 'foundation',
        title: '网络基础',
        description: '理解数据如何在网络中传输',
        items: [
          { id: 'ne-tcp', name: 'TCP/IP', description: '分层模型、寻址与传输协议', aliases: ['网络基础'], priority: 'required' },
          { id: 'ne-subnet', name: '子网规划', description: 'IPv4/IPv6 地址与子网划分', aliases: ['IP 地址'], priority: 'required' },
          { id: 'ne-route', name: '路由交换', description: 'VLAN、静态与动态路由', aliases: ['路由', '交换'], priority: 'required' },
          { id: 'ne-dns', name: 'DNS 与 HTTP', description: '域名解析和应用层访问链路', aliases: ['HTTP 服务', 'DNS'], priority: 'recommended' },
        ],
      },
      {
        id: 'core',
        title: '系统与运维',
        description: '配置网络服务运行环境',
        items: [
          { id: 'ne-linux', name: 'Linux', description: '系统、权限与网络命令', aliases: [], priority: 'required' },
          { id: 'ne-shell', name: 'Shell 自动化', description: '批量配置与重复任务自动化', aliases: ['Shell'], priority: 'required' },
          { id: 'ne-service', name: '网络服务部署', description: 'Web、DNS 与代理服务上线', aliases: ['服务部署'], priority: 'required' },
          { id: 'ne-docker', name: '容器化', description: '隔离并复现服务运行环境', aliases: ['Docker'], priority: 'recommended' },
        ],
      },
      {
        id: 'advanced',
        title: '安全与诊断',
        description: '保障网络可靠运行',
        items: [
          { id: 'ne-security', name: '网络安全', description: '访问控制、防火墙与风险识别', aliases: ['服务安全'], priority: 'required' },
          { id: 'ne-capture', name: '流量分析', description: '抓包、协议分析与异常定位', aliases: ['Wireshark'], priority: 'required' },
          { id: 'ne-monitor', name: '监控诊断', description: '日志、指标与告警处理', aliases: ['网络监控'], priority: 'required' },
          { id: 'ne-python', name: 'Python 自动化', description: '网络设备和配置自动化', aliases: ['Python'], priority: 'recommended' },
        ],
      },
    ],
  },
]

export const knowledgeCatalogService = {
  async getCatalog(): Promise<KnowledgeTrack[]> {
    try {
      const controller = new AbortController()
      const timeout = window.setTimeout(() => controller.abort(), 900)
      const response = await fetch('/api/v1/knowledge/catalog', { signal: controller.signal })
      window.clearTimeout(timeout)
      if (!response.ok) throw new Error('Knowledge catalog unavailable')
      return await response.json()
    } catch {
      return structuredClone(fallbackCatalog)
    }
  },

  async generateTrack(query: string, personalSkills: SkillNode[]): Promise<KnowledgeTrack> {
    try {
      const controller = new AbortController()
      const timeout = window.setTimeout(() => controller.abort(), 5000)
      const response = await fetch('/api/v1/knowledge/generate', {
        method: 'POST',
        signal: controller.signal,
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          query,
          currentSkills: personalSkills.map(({ name, level, status }) => ({ name, level, status })),
        }),
      })
      window.clearTimeout(timeout)
      if (!response.ok) throw new Error('Knowledge generation unavailable')
      const result: unknown = await response.json()
      if (!isKnowledgeTrack(result)) throw new Error('Invalid knowledge track response')
      return { ...result, source: 'ai' }
    } catch {
      return createMockTrack(query)
    }
  },
}

function isKnowledgeTrack(value: unknown): value is KnowledgeTrack {
  if (!value || typeof value !== 'object') return false
  const track = value as Partial<KnowledgeTrack>
  return Boolean(
    typeof track.id === 'string' &&
      typeof track.title === 'string' &&
      typeof track.shortTitle === 'string' &&
      typeof track.description === 'string' &&
      typeof track.outcome === 'string' &&
      Array.isArray(track.stages) &&
      track.stages.length > 0 &&
      track.stages.every(
        (stage) =>
          typeof stage.id === 'string' &&
          typeof stage.title === 'string' &&
          typeof stage.description === 'string' &&
          Array.isArray(stage.items) &&
          stage.items.every(
            (item) =>
              typeof item.id === 'string' &&
              typeof item.name === 'string' &&
              typeof item.description === 'string' &&
              Array.isArray(item.aliases) &&
              (item.priority === 'required' || item.priority === 'recommended'),
          ),
      ),
  )
}

function createMockTrack(query: string): KnowledgeTrack {
  const title = query.trim().replace(/[？?。！!]/g, '').slice(0, 24) || '自定义方向'
  const slug = title.toLowerCase().replace(/\s+/g, '-').replace(/[^a-z0-9\u4e00-\u9fa5-]/g, '')
  const dataTrack = /数据|分析|商业智能|bi/i.test(title)

  if (dataTrack) {
    return {
      id: `generated-${slug}-${Date.now()}`,
      title: title.includes('工程师') || title.includes('分析师') ? title : `${title}方向`,
      shortTitle: title.slice(0, 8),
      description: '从数据获取、分析建模到可解释呈现的完整能力结构。',
      outcome: '能够独立完成一次从业务问题到分析结论的数据项目',
      source: 'mock',
      stages: [
        {
          id: 'data-foundation',
          title: '数据基础',
          description: '建立数据处理与统计思维',
          items: [
            { id: 'data-python', name: 'Python', description: '数据处理与自动化分析', aliases: [], priority: 'required' },
            { id: 'data-sql', name: 'SQL', description: '查询、聚合与数据整理', aliases: ['MySQL'], priority: 'required' },
            { id: 'data-stats', name: '统计学', description: '描述统计、推断与假设检验', aliases: [], priority: 'required' },
          ],
        },
        {
          id: 'data-core',
          title: '分析建模',
          description: '把问题转换成可验证分析',
          items: [
            { id: 'data-clean', name: '数据清洗', description: '缺失、异常与质量处理', aliases: ['Pandas'], priority: 'required' },
            { id: 'data-model', name: '分析建模', description: '指标体系与基础预测模型', aliases: ['机器学习'], priority: 'required' },
            { id: 'data-visual', name: '数据可视化', description: '图表选择与信息表达', aliases: ['ECharts'], priority: 'recommended' },
          ],
        },
        {
          id: 'data-delivery',
          title: '业务交付',
          description: '让分析结果可以复用和决策',
          items: [
            { id: 'data-bi', name: 'BI 看板', description: '指标监控与交互看板', aliases: ['商业智能'], priority: 'required' },
            { id: 'data-story', name: '数据叙事', description: '结论、依据与限制说明', aliases: [], priority: 'required' },
            { id: 'data-project', name: '分析项目', description: '完成可复现的端到端案例', aliases: [], priority: 'recommended' },
          ],
        },
      ],
    }
  }

  return {
    id: `generated-${slug || 'custom'}-${Date.now()}`,
    title,
    shortTitle: title.slice(0, 8),
    description: `围绕“${title}”生成的候选知识结构，接入 AI 后将由模型补充领域细节。`,
    outcome: `能够完成一个可验证的${title}实践项目`,
    source: 'mock',
    stages: [
      {
        id: 'custom-foundation',
        title: '基础认知',
        description: '建立领域概念与问题框架',
        items: [
          { id: `${slug}-concept`, name: `${title}基础`, description: '核心概念、术语与边界', aliases: [], priority: 'required' },
          { id: `${slug}-method`, name: '基础方法', description: '常见工作方法与判断标准', aliases: [], priority: 'required' },
          { id: `${slug}-tool`, name: '核心工具', description: '完成基础任务所需工具', aliases: [], priority: 'recommended' },
        ],
      },
      {
        id: 'custom-core',
        title: '核心能力',
        description: '形成可重复的解决问题能力',
        items: [
          { id: `${slug}-workflow`, name: '标准流程', description: '从输入到结果的完整流程', aliases: [], priority: 'required' },
          { id: `${slug}-practice`, name: '案例实践', description: '通过真实案例验证方法', aliases: [], priority: 'required' },
          { id: `${slug}-quality`, name: '质量评估', description: '评价结果质量与风险', aliases: [], priority: 'recommended' },
        ],
      },
      {
        id: 'custom-delivery',
        title: '项目交付',
        description: '将能力转化为可验证成果',
        items: [
          { id: `${slug}-project`, name: '综合项目', description: '完成端到端实践成果', aliases: [], priority: 'required' },
          { id: `${slug}-review`, name: '复盘改进', description: '记录依据、问题与迭代方向', aliases: [], priority: 'required' },
          { id: `${slug}-portfolio`, name: '成果表达', description: '清晰呈现过程与能力证据', aliases: [], priority: 'recommended' },
        ],
      },
    ],
  }
}
