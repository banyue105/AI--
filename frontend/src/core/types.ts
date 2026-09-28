export type ModuleId = 'ability-growth' | 'decision-sandbox' | 'module3' | 'module4'

export interface UserProfile {
  id: string
  name: string
  avatar?: string
  goals: string[]
}

export interface Goal {
  id: string
  title: string
  description?: string
  deadline?: string
  status: 'active' | 'paused' | 'completed'
}

export interface ModuleManifest {
  id: ModuleId
  title: string
  description: string
  route: string
  status: 'ready' | 'prototype' | 'pending'
  updatedAt: string
}
