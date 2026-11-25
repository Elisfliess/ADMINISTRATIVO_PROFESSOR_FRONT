interface P36AbaContatosEWhatsappProps {
  whatsapp: string
  emailPublico: string
  instagram: string
  mostrarBotaoWhatsapp: boolean
  onChangeWhatsapp: (valor: string) => void
  onChangeEmailPublico: (valor: string) => void
  onChangeInstagram: (valor: string) => void
  onToggleMostrarBotaoWhatsapp: () => void
}

export function P36AbaContatosEWhatsapp({
  whatsapp,
  emailPublico,
  instagram,
  mostrarBotaoWhatsapp,
  onChangeWhatsapp,
  onChangeEmailPublico,
  onChangeInstagram,
  onToggleMostrarBotaoWhatsapp,
}: P36AbaContatosEWhatsappProps) {
  return (
    <div
      className="max-w-2xl space-y-5 rounded-3xl border p-6 sm:p-8"
      style={{
        background: '#FAF6F0',
        borderColor: 'color-mix(in srgb, var(--border) 35%, transparent)',
      }}
    >
      <div className="space-y-1.5">
        <label htmlFor="pub-whatsapp" className="block text-xs font-medium" style={{ color: '#2B1B10' }}>
          WhatsApp
        </label>
        <input
          id="pub-whatsapp"
          type="text"
          value={whatsapp}
          onChange={(e) => onChangeWhatsapp(e.target.value)}
          className="w-full rounded-xl border px-4 py-3 text-sm outline-none"
          style={{ background: '#F5EFE6', borderColor: '#E5DDD2', color: '#3B281B' }}
        />
      </div>

      <div className="space-y-1.5">
        <label htmlFor="pub-email" className="block text-xs font-medium" style={{ color: '#2B1B10' }}>
          E-mail público
        </label>
        <input
          id="pub-email"
          type="email"
          value={emailPublico}
          onChange={(e) => onChangeEmailPublico(e.target.value)}
          className="w-full rounded-xl border px-4 py-3 text-sm outline-none"
          style={{ background: '#F5EFE6', borderColor: '#E5DDD2', color: '#3B281B' }}
        />
      </div>

      <div className="space-y-1.5">
        <label htmlFor="pub-instagram" className="block text-xs font-medium" style={{ color: '#2B1B10' }}>
          Instagram
        </label>
        <input
          id="pub-instagram"
          type="text"
          value={instagram}
          onChange={(e) => onChangeInstagram(e.target.value)}
          className="w-full rounded-xl border px-4 py-3 text-sm outline-none"
          style={{ background: '#F5EFE6', borderColor: '#E5DDD2', color: '#3B281B' }}
        />
      </div>

      <div className="flex items-center justify-between gap-4 pt-2">
        <span className="text-xs font-medium" style={{ color: '#2B1B10' }}>
          Mostrar botão do WhatsApp na página
        </span>
        <button
          type="button"
          role="switch"
          aria-checked={mostrarBotaoWhatsapp}
          onClick={onToggleMostrarBotaoWhatsapp}
          className="relative h-7 w-12 rounded-full transition"
          style={{ background: mostrarBotaoWhatsapp ? '#5C3419' : '#D8CEBF' }}
        >
          <span
            className={`block h-5 w-5 rounded-full bg-white transition-transform ${
              mostrarBotaoWhatsapp ? 'translate-x-6' : 'translate-x-1'
            }`}
          />
        </button>
      </div>
    </div>
  )
}