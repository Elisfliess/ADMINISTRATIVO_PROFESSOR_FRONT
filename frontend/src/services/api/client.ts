import type { ApiError, ProblemDetails } from '../../types/api'

const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL ?? '/api/v1').replace(/\/$/, '')
const CSRF_COOKIE_NAME = 'XSRF-TOKEN'
const CSRF_HEADER_NAME = 'X-XSRF-TOKEN'
const SAFE_METHODS = new Set(['GET', 'HEAD', 'OPTIONS', 'TRACE'])

let csrfBootstrap: Promise<void> | null = null

function readCookie(name: string): string | null {
  const prefix = `${name}=`
  const cookie = document.cookie.split('; ').find((item) => item.startsWith(prefix))

  if (!cookie) return null

  try {
    return decodeURIComponent(cookie.slice(prefix.length))
  } catch {
    return cookie.slice(prefix.length)
  }
}

/**
 * O backend emite o cookie XSRF-TOKEN em qualquer requisição, inclusive em uma
 * GET não autenticada. Como o filtro CSRF roda antes de popular o cookie, a
 * primeira operação mutável precisa de uma leitura prévia. GET /auth/me é um
 * caminho do contrato e sua eventual resposta 401 é irrelevante para o bootstrap.
 */
function ensureCsrfCookie(): Promise<void> {
  if (readCookie(CSRF_COOKIE_NAME) || csrfBootstrap) return csrfBootstrap ?? Promise.resolve()

  csrfBootstrap = fetch(`${API_BASE_URL}/auth/me`, { credentials: 'include' })
    .catch(() => undefined)
    .then(() => undefined)
    .finally(() => {
      csrfBootstrap = null
    })

  return csrfBootstrap
}

function createApiError(status: number, problem: ProblemDetails | null): ApiError {
  const message = problem?.detail ?? problem?.title ?? `A API respondeu com HTTP ${status}.`
  return Object.assign(new Error(message), { status, problem })
}

async function sendApiRequest(path: string, init: RequestInit): Promise<Response> {
  const method = (init.method ?? 'GET').toUpperCase()
  const headers = new Headers(init.headers)

  if (!SAFE_METHODS.has(method)) {
    await ensureCsrfCookie()
    const csrfToken = readCookie(CSRF_COOKIE_NAME)
    if (csrfToken) headers.set(CSRF_HEADER_NAME, csrfToken)
  }

  if (init.body && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json')
  }

  const response = await fetch(`${API_BASE_URL}${path.startsWith('/') ? path : `/${path}`}`, {
    ...init,
    method,
    headers,
    credentials: 'include',
  })

  if (!response.ok) {
    const contentType = response.headers.get('content-type') ?? ''
    let problem: ProblemDetails | null = null

    if (contentType.includes('application/problem+json')) {
      problem = (await response.json()) as ProblemDetails
    }

    throw createApiError(response.status, problem)
  }

  return response
}

/** JSON API client. Session cookies are sent automatically; no bearer token is used. */
export async function apiRequest<T>(path: string, init: RequestInit = {}): Promise<T> {
  const response = await sendApiRequest(path, init)

  if (response.status === 204) return undefined as T

  const contentType = response.headers.get('content-type') ?? ''
  if (!contentType.includes('application/json') && !contentType.includes('+json')) {
    return undefined as T
  }

  return (await response.json()) as T
}

/** Use for endpoints that return files or another non-JSON representation. */
export async function apiDownload(path: string, init: RequestInit = {}): Promise<Blob> {
  const response = await sendApiRequest(path, init)
  return response.blob()
}
