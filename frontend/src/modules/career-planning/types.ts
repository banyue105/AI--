export interface CareerJob {
  id: string
  title: string
  company: string
  category: string
  city: string
  jobType: string
  salary: string
  description: string
  requirements: string
  skillTags: string[]
}

export interface CareerPreference {
  category: string
  city: string
  targetJobId: string | null
}

export interface CareerMatch {
  jobId: string
  score: number
  matched: string[]
  gaps: string[]
}

export interface CareerJobMatch {
  job: CareerJob
  match: CareerMatch
}

export interface CareerReport {
  id: string
  jobId: string | null
  jobTitle: string
  jdText: string
  score: number
  matched: string[]
  gaps: string[]
  actions: string[]
  recognizedSkills: string[]
  createdAt: string
}
