import { useCallback, useSyncExternalStore } from 'react'

export interface RouteState {
  path: string
  slug: string
  search: URLSearchParams
}

function subscribe(onStoreChange: () => void) {
  window.addEventListener('hashchange', onStoreChange)
  return () => window.removeEventListener('hashchange', onStoreChange)
}

function getSnapshot(): string {
  return window.location.hash || '#/'
}

function parse(hash: string): RouteState {
  const raw = hash.replace(/^#\/?/, '')
  const [pathWithQuery = '', query = ''] = raw.split('?')
  let slug = pathWithQuery
  try {
    slug = decodeURIComponent(pathWithQuery)
  } catch {
    // Mantém o valor bruto quando o hash não está codificado corretamente.
  }
  return { path: pathWithQuery, slug, search: new URLSearchParams(query) }
}

export function useHashRoute(): RouteState {
  const hash = useSyncExternalStore(subscribe, getSnapshot, () => '#/')
  return parse(hash)
}

export function navigate(to: string, options: { replace?: boolean } = {}) {
  const target = to.startsWith('#') ? to : `#${to.startsWith('/') ? to : `/${to}`}`

  if (options.replace) {
    window.history.replaceState(null, '', target)
    window.dispatchEvent(new Event('hashchange'))
    return
  }

  window.location.hash = target
}

export function useNavigate() {
  return useCallback((to: string, options?: { replace?: boolean }) => navigate(to, options), [])
}
