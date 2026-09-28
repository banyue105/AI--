import { apiRequest } from '../../core/api/apiClient'
import type { CareerJob, CareerJobMatch, CareerPreference, CareerReport } from './types'

export interface JobFilters {
  keyword?: string
  category?: string
  city?: string
  jobType?: string
}

export const careerService = {
  jobs(filters: JobFilters = {}) {
    const params = new URLSearchParams()
    Object.entries(filters).forEach(([key, value]) => { if (value) params.set(key, value) })
    return apiRequest<CareerJob[]>(`/career/jobs?${params.toString()}`)
  },
  matches: () => apiRequest<CareerJobMatch[]>('/career/matches'),
  preferences: () => apiRequest<CareerPreference>('/career/preferences'),
  savePreferences: (category: string, city: string) => apiRequest<CareerPreference>('/career/preferences', {
    method: 'PUT', body: JSON.stringify({ category, city }),
  }),
  setTarget: (jobId: string) => apiRequest<CareerPreference>('/career/target', {
    method: 'PUT', body: JSON.stringify({ jobId }),
  }),
  target: () => apiRequest<{ job: CareerJob | null }>('/career/target'),
  reports: () => apiRequest<CareerReport[]>('/career/reports'),
  report: (id: string) => apiRequest<CareerReport>(`/career/reports/${encodeURIComponent(id)}`),
  createReport: (input: { jobId?: string | null; jobTitle?: string; jdText: string }) =>
    apiRequest<CareerReport>('/career/reports', { method: 'POST', body: JSON.stringify(input) }),
}
