import { afterEach, describe, expect, it, vi } from 'vitest'
import { apiRequest } from '../src/services/api/client'

describe('cliente HTTP', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
    document.cookie = 'XSRF-TOKEN=; max-age=0'
  })

  it('obtém o cookie CSRF em uma leitura prévia antes do primeiro POST', async () => {
    document.cookie = 'XSRF-TOKEN=; max-age=0'
    const fetchMock = vi.fn().mockImplementation((url: string) => {
      if (url.endsWith('/auth/me')) {
        if (!document.cookie.includes('XSRF-TOKEN=')) {
          document.cookie = 'XSRF-TOKEN=token-de-teste'
        }
        return Promise.resolve(new Response(null, { status: 401 }))
      }
      return Promise.resolve(new Response(null, { status: 204 }))
    })
    vi.stubGlobal('fetch', fetchMock)

    await apiRequest('/auth/login', { method: 'POST', body: '{}' })

    expect(fetchMock).toHaveBeenCalledTimes(2)
    expect(fetchMock.mock.calls[0]?.[0]).toEqual(expect.stringContaining('/auth/me'))
    expect(fetchMock.mock.calls[1]?.[0]).toEqual(expect.stringContaining('/auth/login'))

    const init = fetchMock.mock.calls[1]?.[1] as RequestInit | undefined
    expect(init?.method).toBe('POST')
    expect(init?.credentials).toBe('include')
    expect(((init?.headers as Headers | undefined) ?? new Headers()).get('X-XSRF-TOKEN')).toBe(
      'token-de-teste',
    )
  })

  it('não repete a leitura prévia quando o cookie já existe', async () => {
    document.cookie = 'XSRF-TOKEN=token-existente'
    const fetchMock = vi.fn().mockResolvedValue(new Response(null, { status: 204 }))
    vi.stubGlobal('fetch', fetchMock)

    await apiRequest('/auth/logout', { method: 'POST' })

    expect(fetchMock).toHaveBeenCalledTimes(1)
    const init = fetchMock.mock.calls[0]?.[1] as RequestInit | undefined
    expect(((init?.headers as Headers | undefined) ?? new Headers()).get('X-XSRF-TOKEN')).toBe(
      'token-existente',
    )
  })

  it('mantém o content-type de merge patch configurado pelo chamador', async () => {
    document.cookie = 'XSRF-TOKEN=token-existente'
    const fetchMock = vi
      .fn()
      .mockResolvedValue(
        new Response('{}', { status: 200, headers: { 'Content-Type': 'application/json' } }),
      )
    vi.stubGlobal('fetch', fetchMock)

    await apiRequest('/professor', {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/merge-patch+json' },
      body: '{"nome":"Professor Exemplo"}',
    })

    const init = fetchMock.mock.calls[0]?.[1] as RequestInit | undefined
    expect(((init?.headers as Headers | undefined) ?? new Headers()).get('Content-Type')).toBe(
      'application/merge-patch+json',
    )
  })
})
