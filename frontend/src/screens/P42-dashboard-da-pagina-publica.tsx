const CARD = {
  background: '#FAF6F0',
  borderColor: 'color-mix(in srgb, var(--border) 35%, transparent)',
} as const

/** Frame do Figma: P42-dashboard-da-pagina-publica — claro / ... — escuro. */
export function P42DashboardDaPaginaPublica() {
  return (
    <section className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight" style={{ color: 'var(--text)' }}>
          Indicadores
        </h1>
        <p className="mt-1 text-xs" style={{ color: 'var(--muted)' }}>
          Desempenho da página pública (totais)
        </p>
      </div>

      <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
        <Metrica rotulo="Visitas" valor="482" />
        <Metrica rotulo="Cliques no WhatsApp" valor="37" />
        <Metrica rotulo="Taxa de conversão" valor="7,7%" />
      </div>

      <p className="text-xs" style={{ color: '#6E5D52' }}>
        De 482 visitas, 37 pessoas clicaram no botão do WhatsApp (7,7%).
      </p>
    </section>
  )
}

function Metrica({ rotulo, valor }: { rotulo: string; valor: string }) {
  return (
    <div className="rounded-2xl border p-6" style={CARD}>
      <div className="text-xs font-medium" style={{ color: '#6E5D52' }}>
        {rotulo}
      </div>
      <div className="mt-3 text-2xl font-bold" style={{ color: '#2B1B10' }}>
        {valor}
      </div>
    </div>
  )
}
