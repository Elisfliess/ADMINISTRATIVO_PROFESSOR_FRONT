import { afterEach, describe, expect, it, vi } from 'vitest'
import { apiRequest } from '../src/services/api/client'

describe('apiRequest', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
    document.cookie = 'XSRF-TOKEN=; Max-Age=0; path=/'
  })

  it('sends session credentials and the CSRF header for unsafe methods', async () => {
    document.cookie = 'XSRF-TOKEN=csrf-token; path=/'
    const fetchMock = vi.fn().mockResolvedValue(new Response(null, { status: 204 }))
    vi.stubGlobal('fetch', fetchMock)

    await apiRequest<void>('/auth/logout', { method: 'POST' })

    const [url, options] = fetchMock.mock.calls[0] as [string, RequestInit]
    expect(url).toBe('/api/v1/auth/logout')
    expect(options.credentials).toBe('include')
    expect(new Headers(options.headers).get('X-XSRF-TOKEN')).toBe('csrf-token')
  })

  it('does not attempt to parse a body for a 204 response', async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response(null, { status: 204 }))
    vi.stubGlobal('fetch', fetchMock)

    await expect(apiRequest<void>('/auth/logout', { method: 'POST' })).resolves.toBeUndefined()
  })
})
