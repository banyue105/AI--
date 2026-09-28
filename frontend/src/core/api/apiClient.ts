const REQUEST_TIMEOUT_MS = 4000
let serverConnected = false

export class ApiHttpError extends Error {
  constructor(public readonly status: number, message: string) {
    super(message)
    this.name = 'ApiHttpError'
  }
}

export class ApiUnavailableError extends Error {
  constructor() {
    super('无法连接后端，请检查服务是否启动后重试。')
    this.name = 'ApiUnavailableError'
  }
}

/** Demo mode is available only before this session has reached the backend. */
export function canUseOfflineDemo(cause: unknown): boolean {
  return cause instanceof ApiUnavailableError && !serverConnected
}

export async function apiRequest<T>(path: string, init?: RequestInit): Promise<T> {
  const controller = new AbortController()
  const timeout = globalThis.setTimeout(() => controller.abort(), REQUEST_TIMEOUT_MS)
  try {
    let response: Response
    let body: string
    try {
      response = await fetch(`/api/v1${path}`, {
        ...init,
        signal: controller.signal,
        headers: { 'Content-Type': 'application/json', ...init?.headers },
      })
      body = await response.text()
    } catch {
      throw new ApiUnavailableError()
    }

    let data: unknown
    try {
      data = body ? JSON.parse(body) : null
    } catch {
      data = null
    }
    if (!response.ok) {
      // Vite returns an empty 500 when the configured local proxy cannot connect.
      // A backend error body always remains a failed request, including 5xx errors.
      if (!body.trim() && [500, 502, 503, 504].includes(response.status)) {
        throw new ApiUnavailableError()
      }
      const error = data as { error?: { message?: unknown }; message?: unknown; detail?: unknown } | null
      const message = error?.error?.message ?? error?.message ?? error?.detail
      throw new ApiHttpError(response.status, typeof message === 'string' ? message : `请求失败（HTTP ${response.status}），请稍后重试。`)
    }
    serverConnected = true
    if (data === null) throw new Error('后端返回的数据格式无效，请稍后重试。')
    return data as T
  } finally {
    globalThis.clearTimeout(timeout)
  }
}
