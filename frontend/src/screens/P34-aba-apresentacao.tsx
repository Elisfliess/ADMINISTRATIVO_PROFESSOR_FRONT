interface P34AbaApresentacaoProps {
  nomePublico: string
  tituloProfissional: string
  sobreMim: string
  formacao: string
  onChangeNomePublico: (valor: string) => void
  onChangeTituloProfissional: (valor: string) => void
  onChangeSobreMim: (valor: string) => void
  onChangeFormacao: (valor: string) => void
}

export function P34AbaApresentacao({
  nomePublico,
  tituloProfissional,
  sobreMim,
  formacao,
  onChangeNomePublico,
  onChangeTituloProfissional,
  onChangeSobreMim,
  onChangeFormacao,
}: P34AbaApresentacaoProps) {
  return (
    <div
      className="max-w-2xl space-y-5 rounded-3xl border p-6 sm:p-8"
      style={{
        background: '#FAF6F0',
        borderColor: 'color-mix(in srgb, var(--border) 35%, transparent)',
      }}
    >
      <div className="space-y-1.5">
        <label htmlFor="pub-nome" className="block text-xs font-medium" style={{ color: '#2B1B10' }}>
          Nome público
        </label>
        <input
          id="pub-nome"
          type="text"
          value={nomePublico}
          onChange={(e) => onChangeNomePublico(e.target.value)}
          className="w-full rounded-xl border px-4 py-3 text-sm outline-none"
          style={{ background: '#F5EFE6', borderColor: '#E5DDD2', color: '#3B281B' }}
        />
      </div>

      <div className="space-y-1.5">
        <label htmlFor="pub-titulo" className="block text-xs font-medium" style={{ color: '#2B1B10' }}>
          Título
        </label>
        <input
          id="pub-titulo"
          type="text"
          value={tituloProfissional}
          onChange={(e) => onChangeTituloProfissional(e.target.value)}
          className="w-full rounded-xl border px-4 py-3 text-sm outline-none"
          style={{ background: '#F5EFE6', borderColor: '#E5DDD2', color: '#3B281B' }}
        />
      </div>

      <div className="space-y-1.5">
        <label htmlFor="pub-sobre" className="block text-xs font-medium" style={{ color: '#2B1B10' }}>
          Sobre mim
        </label>
        <textarea
          id="pub-sobre"
          rows={4}
          value={sobreMim}
          onChange={(e) => onChangeSobreMim(e.target.value)}
          className="w-full rounded-xl border px-4 py-3 text-sm outline-none"
          style={{ background: '#F5EFE6', borderColor: '#E5DDD2', color: '#3B281B' }}
        />
      </div>

      <div className="space-y-1.5">
        <label htmlFor="pub-formacao" className="block text-xs font-medium" style={{ color: '#2B1B10' }}>
          Formação
        </label>
        <input
          id="pub-formacao"
          type="text"
          value={formacao}
          onChange={(e) => onChangeFormacao(e.target.value)}
          className="w-full rounded-xl border px-4 py-3 text-sm outline-none"
          style={{ background: '#F5EFE6', borderColor: '#E5DDD2', color: '#3B281B' }}
        />
      </div>
    </div>
  )
}