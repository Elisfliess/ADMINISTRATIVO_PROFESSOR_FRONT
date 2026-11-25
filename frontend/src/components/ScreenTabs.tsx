import { navigate, useHashRoute } from '../router/hashRouter'

export function ScreenTabs({ items }: { items: { slug: string; label: string }[] }) {
  const { slug } = useHashRoute()

  return (
    <div className="mb-6 flex flex-wrap gap-2" role="tablist" aria-label="Seções da tela">
      {items.map((item) => (
        <button
          key={item.slug}
          role="tab"
          aria-selected={slug === item.slug}
          aria-current={slug === item.slug ? 'page' : undefined}
          className="nav-link surface min-h-11"
          onClick={() => navigate(`/${item.slug}`)}
        >
          {item.label}
        </button>
      ))}
    </div>
  )
}
