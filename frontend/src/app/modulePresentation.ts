import type { Component } from 'vue'
import { BriefcaseBusiness, ClipboardCheck, GitBranch, Network } from 'lucide-vue-next'
import { defaultModuleManifests } from '../core/api/homeService'
import type { ModuleId, ModuleManifest } from '../core/types'
import abilityPreview from '../assets/home/ability-preview.jpg'
import decisionPreview from '../assets/home/decision-preview.jpg'
import practicePreview from '../assets/home/practice-preview.jpg'
import careerPreview from '../assets/home/career-preview.jpg'

interface Presentation {
  number: string
  icon: Component
  question: string
  intro: string
  highlightTitle: string
  highlightIntro: string
  points: readonly string[]
  note: string
  entryLabel: string
  highlightLabel: string
  anchor: string
  workspace: string
  image: string
  imageHeight: number
  imageAlt: string
  imageLabel: string
}

export type PresentedModule = ModuleManifest & Presentation

const presentation: Record<ModuleId, Presentation> = {
  'ability-growth': {
    number: '01', icon: Network, question: '下一步，学什么？',
    intro: '梳理已有能力与目标差距，用可编辑技能图谱找到下一步学习方向。',
    highlightTitle: '把零散经验，连成看得见的成长路径。',
    highlightIntro: '会什么、还缺什么、为什么先学这一步，都有迹可循。让学习从一张属于自己的能力图谱开始。',
    points: ['个人技能树呈现掌握状态与前置关系，节点和关系可继续编辑。', '按方向、阶段和技术项浏览知识分类，自然语言候选由你确认后加入。', '围绕目标识别能力缺口，查看成长路径的顺序与推荐依据。'],
    note: '演示界面 · 路径建议可查看依据并继续调整',
    entryLabel: '进入能力成长', highlightLabel: '查看能力图谱与成长路径',
    anchor: 'ability-highlights', workspace: 'ability-workspace',
    image: abilityPreview, imageHeight: 563, imageAlt: '能力成长中的个人技能树，展示技能节点、掌握状态和前置关系。', imageLabel: '个人技能树 / 从现状走向目标',
  },
  'decision-sandbox': {
    number: '02', icon: GitBranch, question: '不同选择，差在哪？',
    intro: '把时间、预算、人力和资源放进同一场景，比较不同选择的代价与风险。',
    highlightTitle: '先看见选择的代价，再做决定。',
    highlightIntro: '改变一个条件，不只是换一个答案。把基准方案与变更方案放在一起，看清时间、成本和风险如何变化。',
    points: ['一句话提取现实条件，确认候选后建立可编辑的条件关系。', '对比基准与变更方案的工期、成本、投入和风险，展开关键假设。', '保留每次推演版本，比较改变的条件，或恢复旧条件继续探索。'],
    note: '演示界面 · 数值来自规则估算，供决策参考',
    entryLabel: '进入决策沙盒', highlightLabel: '查看方案推演与版本比较',
    anchor: 'decision-highlights', workspace: 'decision-workspace',
    image: decisionPreview, imageHeight: 620, imageAlt: '决策沙盒对比基准方案和变更方案的工期、成本、风险与工期区间。', imageLabel: '方案对比 / 让变化有依据',
  },
  module3: {
    number: '03', icon: ClipboardCheck, question: '这次实践，证明了什么？',
    intro: '对照验收标准检查成果证据，比较实际投入，保留可追溯的复盘记录。',
    highlightTitle: '每一次复盘，都能回到原始依据。',
    highlightIntro: '把“做过”变成有材料支持的记录。逐项检查成果、定位原文，补交材料后再看判断发生了什么变化。',
    points: ['按验收项区分材料支持、证据不足与矛盾，引用可定位原文和材料版本。', '保存历史复盘，补交材料后对比判断变化；同口径比较预期与实际投入。', '确认或修正判断后保存参考反馈，让下一次行动有记录可查。'],
    note: '合成演示界面 · 反馈保存为参考，跨模块写入仍待联调',
    entryLabel: '进入实践复盘', highlightLabel: '查看证据追溯与复盘变化',
    anchor: 'practice-highlights', workspace: 'practice-workspace',
    image: practicePreview, imageHeight: 620, imageAlt: '实践复盘逐项显示材料支持程度，并在原始材料中定位证据引用。', imageLabel: '材料检查 / 结论可以追溯',
  },
  module4: {
    number: '04', icon: BriefcaseBusiness, question: '当前能力，能走向哪里？',
    intro: '对照当前能力与演示岗位要求，筛选方向、识别技能缺口，保存 JD 分析报告。',
    highlightTitle: '把当前能力，与下一段职业方向对照。',
    highlightIntro: '从岗位要求出发，看看已有能力能匹配哪些工作、还有哪些技能需要补齐，再把方向变成具体行动建议。',
    points: ['读取当前能力图谱，逐项呈现已有匹配与技能缺口，保存意向方向和城市。', '按关键词、方向、城市与用工类型筛选演示岗位，选择目标岗位。', '带入岗位或粘贴 JD，对照技能要求生成行动建议，保存报告快照供回看。'],
    note: '本地演示岗位库 · 技能词规则分析，匹配度供规划参考',
    entryLabel: '进入职业规划', highlightLabel: '查看岗位匹配与 JD 分析',
    anchor: 'career-highlights', workspace: 'career-workspace',
    image: careerPreview, imageHeight: 620, imageAlt: '职业规划演示岗位逐项展示已有匹配与待补齐技能，并支持设为目标和分析 JD。', imageLabel: '演示岗位 / 对照能力与要求',
  },
}

export const presentedModules: readonly PresentedModule[] = defaultModuleManifests.map(module => ({ ...module, ...presentation[module.id] }))

export function modulePresentation(id: ModuleId): PresentedModule {
  return presentedModules.find(module => module.id === id)!
}

export function moduleIsActive(id: ModuleId, path: string): boolean {
  const root = id === 'module4' ? '/module4' : modulePresentation(id).route
  return path === root || path.startsWith(root + '/')
}
