import type { ModuleManifest, UserProfile } from '../types'

export interface HomeData {
  profile: UserProfile
  currentGoal: string
  recentActivity: string
  modules: ModuleManifest[]
}

const fallback: HomeData = {
  profile: { id: 'demo-user', name: '林澈', goals: ['独立完成网络服务部署'] },
  currentGoal: '独立完成一个可复现的网络服务部署',
  recentActivity: '今天更新了 TCP/IP 的掌握状态',
  modules: [
    {
      id: 'ability-growth',
      title: '能力成长',
      description: '把经验转成能力图谱，找到下一步',
      route: '/ability',
      status: 'ready',
      updatedAt: '今天 21:32',
    },
    {
      id: 'decision-sandbox',
      title: '决策沙盒',
      description: '改变现实条件，比较不同选择的代价',
      route: '/decision',
      status: 'prototype',
      updatedAt: '今天 18:40',
    },
    {
      id: 'module3',
      title: '第三模块',
      description: '产品方向仍在论证中',
      route: '',
      status: 'pending',
      updatedAt: '待确定',
    },
  ],
}

export const homeService = {
  async getHome(): Promise<HomeData> {
    try {
      const controller = new AbortController()
      const timeout = window.setTimeout(() => controller.abort(), 900)
      const response = await fetch('/api/v1/home', { signal: controller.signal })
      window.clearTimeout(timeout)
      if (!response.ok) throw new Error('Home API unavailable')
      return await response.json()
    } catch {
      return structuredClone(fallback)
    }
  },
}
