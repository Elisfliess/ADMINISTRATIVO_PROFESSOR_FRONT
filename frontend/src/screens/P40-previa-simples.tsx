import { useState } from 'react'

export type TemaPaginaPublica = 'claro' | 'escuro'

export interface ModalidadePublica {
  id: string
  modalidade: string
  descricao: string
  duracao: string
  valor: string
  imagemUrl?: string
}

export type TipoIconeInstrumento = 'violao' | 'teclado' | 'guitarra' | 'partitura'

export interface InstrumentoPublicado {
  id: string
  nome: string
  descricao: string
  icone?: TipoIconeInstrumento
}

export interface DadosPaginaPublica {
  nomePublico: string
  tituloProfissional: string
  sobreMim: string
  formacao: string
  bannerUrl?: string
  tema?: TemaPaginaPublica
  instrumentos?: InstrumentoPublicado[]
  modalidades: ModalidadePublica[]
  whatsapp: string
  emailPublico: string
  instagram: string
  mostrarBotaoWhatsapp: boolean
}

const INSTRUMENTOS_PADRAO_PREVIA: InstrumentoPublicado[] = [
  {
    id: 'i1',
    nome: 'Violão',
    descricao: 'Acordes, ritmos, dedilhado e repertório do zero ao avançado.',
    icone: 'violao',
  },
  {
    id: 'i2',
    nome: 'Teclado',
    descricao: 'Leitura, harmonia, independência das mãos e prática musical.',
    icone: 'teclado',
  },
  {
    id: 'i3',
    nome: 'Guitarra',
    descricao: 'Técnica, escalas, improvisação e bases de vários estilos.',
    icone: 'guitarra',
  },
  {
    id: 'i4',
    nome: 'Teoria e Canto',
    descricao: 'Percepção rítmica, afinação e apoio para acompanhar ao tocar.',
    icone: 'partitura',
  },
]

const DADOS_PADRAO_PREVIA: DadosPaginaPublica = {
  nomePublico: 'Prof.º José Carlos',
  tituloProfissional: 'Professor',
  sobreMim:
    'Aulas individuais e em dupla, com acompanhamento próximo e plano de estudo para cada aluno.',
  formacao: 'Licenciatura',
  bannerUrl: '',
  tema: 'claro',
  instrumentos: INSTRUMENTOS_PADRAO_PREVIA,
  modalidades: [
    {
      id: 'm1',
      modalidade: 'Aula individual',
      descricao: 'Atenção total ao seu ritmo.',
      duracao: '60 minutos',
      valor: 'R$ 150,00',
    },
    {
      id: 'm2',
      modalidade: 'Aula em dupla',
      descricao: 'Aprenda junto com alguém.',
      duracao: '60 minutos',
      valor: 'R$ 270,00',
    },
  ],
  whatsapp: '(11) 90000-0000',
  emailPublico: 'contato@exemplo.com',
  instagram: '@usuario',
  mostrarBotaoWhatsapp: true,
}

export interface P40PreviaSimplesProps {
  dados?: DadosPaginaPublica
  tema?: TemaPaginaPublica
  aviso?: string | null
  /** Quando verdadeiro, esconde os controles de edição e mostra o cabeçalho público (V01). */
  modoPublicado?: boolean
  onVoltarEditor?: () => void
  onAbrirPublicar?: () => void
  onAlternarTema?: (novoTema: TemaPaginaPublica) => void
  onAviso?: (mensagem: string) => void
}

const FUNDOS_FALLBACK_CARD_VALOR = [
  'linear-gradient(135deg, #6E584B 0%, #4A3529 100%)',
  'linear-gradient(135deg, #6E1C14 0%, #3E100C 100%)',
  'linear-gradient(135deg, #74643E 0%, #4D4126 100%)',
]

const ESTILOS_CIRCULO_INSTRUMENTO = [
  { bg: '#F4C938', border: '#D9AE24', color: '#2B1B10' },
  { bg: '#8CC4FA', border: '#66A3DF', color: '#1B2836' },
  { bg: '#F26444', border: '#CE4A2D', color: '#24110C' },
  { bg: '#A7C97E', border: '#84A85B', color: '#1D2912' },
]

function extrairIniciais(nome: string): string {
  const limpo = nome.replace(/^Prof\.?\s*[ºo]?\s*/i, '').trim()
  const partes = limpo.split(/\s+/).filter(Boolean)
  if (partes.length === 0) return 'JC'
  if (partes.length === 1) return partes[0]!.slice(0, 2).toUpperCase()
  return `${partes[0]![0] ?? ''}${partes[partes.length - 1]![0] ?? ''}`.toUpperCase()
}

function dividirTituloEditorial(titulo: string): { principal: string; destaque: string } {
  const palavras = titulo.trim().split(/\s+/).filter(Boolean)
  if (palavras.length <= 1) {
    return { principal: titulo || 'Professor', destaque: '' }
  }
  const destaque = palavras.pop() ?? ''
  return { principal: palavras.join(' '), destaque }
}

function resolverIconeInstrumento(
  nome: string,
  icone?: TipoIconeInstrumento,
): TipoIconeInstrumento {
  if (icone) return icone
  const texto = nome.toLowerCase()
  if (texto.includes('teclado') || texto.includes('piano')) return 'teclado'
  if (texto.includes('guitarra') || texto.includes('baixo')) return 'guitarra'
  if (texto.includes('violão') || texto.includes('violao')) return 'violao'
  return 'partitura'
}

function IconeInstrumentoSvg({ tipo }: { tipo: TipoIconeInstrumento }) {
  if (tipo === 'teclado') {
    return (
      <svg
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        strokeWidth="1.8"
        strokeLinecap="round"
        strokeLinejoin="round"
        className="h-7 w-7"
        aria-hidden="true"
      >
        <rect x="3" y="6" width="18" height="12" rx="2" />
        <path d="M8 6v6M12 6v6M16 6v6M6.5 18v0M11.5 18v0M16.5 18v0" />
      </svg>
    )
  }

  if (tipo === 'guitarra') {
    return (
      <svg
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        strokeWidth="1.8"
        strokeLinecap="round"
        strokeLinejoin="round"
        className="h-7 w-7"
        aria-hidden="true"
      >
        <path d="m15 4 5 5" />
        <path d="m17.5 6.5-6.2 6.2" />
        <path d="M11.3 12.7c-1.3-1.3-3.5-1.1-4.6.4-.8 1.1-.8 2.4-1.8 3.3-1.2 1.1-1.3 3-.1 4.2 1.2 1.2 3.1 1.1 4.2-.1.9-1 2.2-1 3.3-1.8 1.5-1.1 1.7-3.3.4-4.6l-.4-.4Z" />
        <circle cx="9.8" cy="14.2" r="1.1" />
      </svg>
    )
  }

  if (tipo === 'violao') {
    return (
      <svg
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        strokeWidth="1.8"
        strokeLinecap="round"
        strokeLinejoin="round"
        className="h-7 w-7"
        aria-hidden="true"
      >
        <path d="m16 3 5 5" />
        <path d="m18.5 5.5-5.8 5.8" />
        <path d="M12.7 11.3c-1.4-1.4-3.7-1.1-4.8.5-.7 1-.7 2.2-1.6 3.1-1.2 1.2-1.2 3.2 0 4.4 1.2 1.2 3.2 1.2 4.4 0 .9-.9 2.1-.9 3.1-1.6 1.6-1.1 1.9-3.4.5-4.8l-.6-.6Z" />
        <circle cx="10.4" cy="13.6" r="1.4" />
      </svg>
    )
  }

  return (
    <svg
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.8"
      strokeLinecap="round"
      strokeLinejoin="round"
      className="h-7 w-7"
      aria-hidden="true"
    >
      <path d="M9 18V5l12-2v13" />
      <circle cx="6" cy="18" r="3" />
      <circle cx="18" cy="16" r="3" />
    </svg>
  )
}

export function P40PreviaSimples({
  dados = DADOS_PADRAO_PREVIA,
  tema,
  aviso = null,
  modoPublicado = false,
  onVoltarEditor,
  onAbrirPublicar,
  onAlternarTema,
  onAviso,
}: P40PreviaSimplesProps) {
  const [temaInterno, setTemaInterno] = useState<TemaPaginaPublica>(
    tema ?? dados.tema ?? 'claro',
  )
  const temaAtivo: TemaPaginaPublica = tema ?? temaInterno
  const escuro = temaAtivo === 'escuro'

  const paleta = escuro
    ? {
        fundoPagina: '#140E0A',
        textoPrincipal: '#F5EDE3',
        textoSecundario: '#CBBBA8',
        textoSuave: '#A89582',
        destaqueEditorial: '#D49A6A',
        subtituloEtiqueta: '#C49A72',
        headerBg: '#23140A',
        headerBorda: '#3A2315',
        btnSecundarioBg: '#332014',
        btnSecundarioTexto: '#F5EDE3',
        btnPrimarioBg: '#F5EDE3',
        btnPrimarioTexto: '#23140A',
        superficieCard: '#1F150F',
        superficieElevada: '#281B13',
        bordaCard: '#38261B',
        divisor: '#342318',
        avatarBg: '#342217',
        avatarBorda: '#523726',
        avatarTexto: '#F2C99E',
        pillBg: 'rgba(20, 14, 10, 0.85)',
        badgeBg: '#322217',
        badgeTexto: '#E5C39E',
        ctaBg: '#241810',
        ctaBorda: '#3F2B1E',
        avisoBg: '#241810',
        avisoBorda: '#4A3222',
        avisoTexto: '#F2C99E',
        footerBg: '#0E0906',
        footerBorda: '#24170F',
        footerTexto: '#DFD2C2',
        footerSubtexto: '#9E8B78',
      }
    : {
        fundoPagina: '#F7F2EA',
        textoPrincipal: '#2B1B10',
        textoSecundario: '#6E5D52',
        textoSuave: '#8A786B',
        destaqueEditorial: '#7A4926',
        subtituloEtiqueta: '#8A6A52',
        headerBg: '#5C3419',
        headerBorda: '#472611',
        btnSecundarioBg: '#EAE1D5',
        btnSecundarioTexto: '#3B281B',
        btnPrimarioBg: '#FFFFFF',
        btnPrimarioTexto: '#3B281B',
        superficieCard: '#FAF6F0',
        superficieElevada: '#F5EFE4',
        bordaCard: '#E6DDD0',
        divisor: '#EAE2D7',
        avatarBg: '#E7DDD0',
        avatarBorda: '#D8CBB8',
        avatarTexto: '#5C3419',
        pillBg: 'rgba(247, 242, 234, 0.92)',
        badgeBg: '#EFE7DC',
        badgeTexto: '#5C3419',
        ctaBg: '#EFE7DC',
        ctaBorda: '#DFD2C0',
        avisoBg: '#FAF6F0',
        avisoBorda: '#D8CEBF',
        avisoTexto: '#5C3419',
        footerBg: '#2C180B',
        footerBorda: '#3E2413',
        footerTexto: '#E7DDD0',
        footerSubtexto: '#A99682',
      }

  const iniciais = extrairIniciais(dados.nomePublico)
  const digitosWhatsapp = dados.whatsapp.replace(/\D/g, '')
  const linkWhatsapp = digitosWhatsapp
    ? `https://wa.me/${digitosWhatsapp.startsWith('55') ? digitosWhatsapp : `55${digitosWhatsapp}`}`
    : null
  const instagramLimpo = dados.instagram.replace(/^@+/, '').trim()
  const tituloEditorial = dividirTituloEditorial(dados.tituloProfissional)
  const temBannerImagem = Boolean(dados.bannerUrl && dados.bannerUrl.trim().length > 0)
  const instrumentos =
    dados.instrumentos && dados.instrumentos.length > 0
      ? dados.instrumentos
      : INSTRUMENTOS_PADRAO_PREVIA

  const proximoTema: TemaPaginaPublica = escuro ? 'claro' : 'escuro'

  function alternarTema() {
    setTemaInterno(proximoTema)
    onAlternarTema?.(proximoTema)
  }

  const bannerBackgroundImage = temBannerImagem
    ? escuro
      ? `linear-gradient(180deg, rgba(31, 21, 15, 0.84) 0%, rgba(20, 14, 10, 0.94) 100%), url("${dados.bannerUrl}")`
      : `linear-gradient(180deg, rgba(250, 246, 240, 0.86) 0%, rgba(247, 242, 234, 0.93) 100%), url("${dados.bannerUrl}")`
    : escuro
      ? 'radial-gradient(circle at top right, rgba(122, 73, 38, 0.28), transparent 55%), linear-gradient(180deg, #241811 0%, #1A110C 100%)'
      : 'radial-gradient(circle at top right, rgba(231, 221, 208, 0.65), transparent 55%), linear-gradient(180deg, #FCF9F4 0%, #F5EFE4 100%)'

  return (
    <div
      className="flex min-h-screen flex-col transition-colors"
      style={{ background: paleta.fundoPagina, color: paleta.textoPrincipal }}
    >
      {/* Barra superior: prévia (editor) ou cabeçalho público (V01) */}
      <header
        className="sticky top-0 z-20 border-b px-6 py-3.5 sm:px-10"
        style={{
          background: paleta.headerBg,
          borderColor: paleta.headerBorda,
          color: '#FFFFFF',
        }}
      >
        <div className="mx-auto flex max-w-5xl flex-wrap items-center justify-between gap-3">
          {modoPublicado ? (
            <div className="flex items-center gap-3">
              <div
                className="grid h-9 w-9 place-items-center rounded-full border text-[11px] font-semibold"
                style={{
                  background: 'rgba(255, 255, 255, 0.14)',
                  borderColor: 'rgba(255, 255, 255, 0.26)',
                  color: '#FFFFFF',
                }}
                aria-hidden="true"
              >
                {iniciais}
              </div>
              <div>
                <div className="text-xs font-semibold tracking-wide">{dados.nomePublico}</div>
                <div className="text-[11px]" style={{ color: 'rgba(255, 255, 255, 0.78)' }}>
                  {dados.tituloProfissional}
                </div>
              </div>
            </div>
          ) : (
            <div className="flex items-center gap-2.5">
              <span
                className="inline-block h-2 w-2 rounded-full"
                style={{ background: escuro ? '#D49A6A' : '#E7DDD0' }}
              />
              <span className="text-xs font-medium tracking-wide">
                Prévia simples: só você vê esta página
              </span>
            </div>
          )}

          <div className="flex flex-wrap items-center gap-2.5">
            <button
              type="button"
              onClick={alternarTema}
              aria-pressed={escuro}
              className="cursor-pointer rounded-xl border px-3.5 py-2 text-xs font-medium transition hover:opacity-90"
              style={{
                background: 'transparent',
                borderColor: 'rgba(255, 255, 255, 0.22)',
                color: '#FFFFFF',
              }}
            >
              {escuro ? 'Modo claro' : 'Modo escuro'}
            </button>

            {!modoPublicado ? (
              <>
                <button
                  type="button"
                  onClick={() => onVoltarEditor?.()}
                  className="cursor-pointer rounded-xl px-4 py-2 text-xs font-medium transition hover:brightness-95"
                  style={{
                    background: paleta.btnSecundarioBg,
                    color: paleta.btnSecundarioTexto,
                  }}
                >
                  Voltar ao editor
                </button>
                <button
                  type="button"
                  onClick={() => onAbrirPublicar?.()}
                  className="cursor-pointer rounded-xl px-5 py-2 text-xs font-semibold transition hover:brightness-95"
                  style={{
                    background: paleta.btnPrimarioBg,
                    color: paleta.btnPrimarioTexto,
                  }}
                >
                  Publicar
                </button>
              </>
            ) : null}
          </div>
        </div>
      </header>

      {/* Banner de aviso */}
      {aviso ? (
        <div className="mx-auto w-full max-w-5xl px-6 pt-5 sm:px-10">
          <div
            role="status"
            className="rounded-xl border px-4 py-3 text-xs font-medium"
            style={{
              background: paleta.avisoBg,
              borderColor: paleta.avisoBorda,
              color: paleta.avisoTexto,
            }}
          >
            {aviso}
          </div>
        </div>
      ) : null}

      <main className="mx-auto w-full max-w-5xl flex-1 space-y-14 px-6 py-12 sm:px-10 sm:py-16">
        {/* Hero / Banner editorial */}
        <section
          className="relative overflow-hidden rounded-3xl border px-6 py-11 text-center sm:px-12 sm:py-14"
          style={{
            backgroundColor: paleta.superficieCard,
            borderColor: paleta.bordaCard,
            backgroundImage: bannerBackgroundImage,
            backgroundSize: 'cover',
            backgroundPosition: 'center',
          }}
        >
          <div className="relative z-10 mx-auto flex flex-col items-center">
            <div
              className="grid h-24 w-24 place-items-center rounded-full border text-xl font-semibold tracking-wide shadow-xs"
              style={{
                background: paleta.avatarBg,
                borderColor: paleta.avatarBorda,
                color: paleta.avatarTexto,
              }}
            >
              {iniciais}
            </div>

            <span
              className="mt-5 text-[11px] font-semibold uppercase tracking-[0.18em]"
              style={{ color: paleta.subtituloEtiqueta }}
            >
              {dados.nomePublico}
            </span>

            <h1
              className="mt-2 font-serif text-3xl font-bold tracking-tight sm:text-4xl"
              style={{ color: paleta.textoPrincipal }}
            >
              {tituloEditorial.principal}{' '}
              {tituloEditorial.destaque ? (
                <span style={{ color: paleta.destaqueEditorial }}>
                  {tituloEditorial.destaque}
                </span>
              ) : null}
            </h1>

            <p
              className="mt-4 max-w-xl text-sm leading-relaxed"
              style={{ color: paleta.textoSecundario }}
            >
              {dados.sobreMim}
            </p>

            <div
              className="mt-6 inline-flex items-center gap-2 rounded-full border px-4 py-1.5 text-xs font-medium"
              style={{
                background: paleta.pillBg,
                borderColor: paleta.bordaCard,
                color: paleta.textoSecundario,
              }}
            >
              <span>Formação:</span>
              <strong
                className="font-semibold"
                style={{ color: paleta.textoPrincipal }}
              >
                {dados.formacao}
              </strong>
            </div>
          </div>
        </section>

        {/* Faixa de Instrumentos entre o banner e as Modalidades */}
        <section
          aria-label="Instrumentos lecionados"
          className="rounded-2xl border px-6 py-8 sm:px-8"
          style={{
            background: paleta.superficieCard,
            borderColor: paleta.bordaCard,
          }}
        >
          <div className="mb-6 text-center sm:text-left">
            <span
              className="text-[11px] font-semibold uppercase tracking-[0.16em]"
              style={{ color: paleta.subtituloEtiqueta }}
            >
              — O que você vai aprender —
            </span>
            <h2
              className="mt-1 font-serif text-xl font-bold sm:text-2xl"
              style={{ color: paleta.textoPrincipal }}
            >
              Instrumentos
            </h2>
          </div>

          <div className="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-4">
            {instrumentos.map((item, index) => {
              const estiloCirculo =
                ESTILOS_CIRCULO_INSTRUMENTO[index % ESTILOS_CIRCULO_INSTRUMENTO.length]!
              const tipoIcone = resolverIconeInstrumento(item.nome, item.icone)
              const temDivisorDireita = index < instrumentos.length - 1

              return (
                <div
                  key={item.id}
                  className={`flex flex-col items-center text-center sm:items-start sm:text-left ${
                    temDivisorDireita ? 'lg:border-r lg:pr-6' : ''
                  }`}
                  style={{ borderColor: paleta.divisor }}
                >
                  <div
                    className="grid h-16 w-16 place-items-center rounded-full border-2 shadow-xs"
                    style={{
                      backgroundColor: estiloCirculo.bg,
                      borderColor: estiloCirculo.border,
                      color: estiloCirculo.color,
                    }}
                  >
                    <IconeInstrumentoSvg tipo={tipoIcone} />
                  </div>

                  <h3
                    className="mt-4 text-xs font-extrabold uppercase tracking-wider"
                    style={{ color: paleta.textoPrincipal }}
                  >
                    {item.nome}
                  </h3>

                  <p
                    className="mt-1.5 text-xs leading-relaxed"
                    style={{ color: paleta.textoSecundario }}
                  >
                    {item.descricao}
                  </p>
                </div>
              )
            })}
          </div>
        </section>

        {/* Modalidades em grade editorial */}
        <section className="space-y-5">
          <div className="flex flex-wrap items-end justify-between gap-2">
            <div>
              <span
                className="text-[11px] font-semibold uppercase tracking-[0.16em]"
                style={{ color: paleta.subtituloEtiqueta }}
              >
                — Como funcionam as aulas —
              </span>
              <h2
                className="mt-1 font-serif text-2xl font-bold"
                style={{ color: paleta.textoPrincipal }}
              >
                Modalidades
              </h2>
            </div>
          </div>

          <div className="grid grid-cols-1 gap-5 md:grid-cols-2">
            {dados.modalidades.map((item) => (
              <article
                key={item.id}
                className="flex flex-col justify-between rounded-2xl border p-6 transition"
                style={{
                  background: paleta.superficieCard,
                  borderColor: paleta.bordaCard,
                }}
              >
                <div>
                  <div className="flex items-center justify-between gap-3">
                    <h3
                      className="font-serif text-lg font-bold"
                      style={{ color: paleta.textoPrincipal }}
                    >
                      {item.modalidade}
                    </h3>
                    <span
                      className="rounded-lg px-2.5 py-1 text-[11px] font-medium"
                      style={{
                        background: paleta.badgeBg,
                        color: paleta.badgeTexto,
                      }}
                    >
                      {item.duracao}
                    </span>
                  </div>

                  <p
                    className="mt-3 text-xs leading-relaxed sm:text-sm"
                    style={{ color: paleta.textoSecundario }}
                  >
                    {item.descricao}
                  </p>
                </div>

                <div
                  className="mt-6 flex items-center justify-between border-t pt-4 text-xs"
                  style={{ borderColor: paleta.divisor }}
                >
                  <span style={{ color: paleta.textoSuave }}>Duração</span>
                  <span
                    className="font-semibold"
                    style={{ color: paleta.textoPrincipal }}
                  >
                    {item.duracao}
                  </span>
                </div>
              </article>
            ))}
          </div>
        </section>

        {/* Cards de Valores com imagem em cima e valor embaixo */}
        <section className="space-y-5">
          <div>
            <span
              className="text-[11px] font-semibold uppercase tracking-[0.16em]"
              style={{ color: paleta.subtituloEtiqueta }}
            >
              — Tabela transparente —
            </span>
            <h2
              className="mt-1 font-serif text-2xl font-bold"
              style={{ color: paleta.textoPrincipal }}
            >
              Valores
            </h2>
          </div>

          <div className="grid grid-cols-1 gap-6 sm:grid-cols-2">
            {dados.modalidades.map((item, index) => {
              const temImagem = Boolean(item.imagemUrl && item.imagemUrl.trim().length > 0)
              const fundoFallback =
                FUNDOS_FALLBACK_CARD_VALOR[index % FUNDOS_FALLBACK_CARD_VALOR.length]

              return (
                <article
                  key={item.id}
                  className="flex flex-col overflow-hidden rounded-2xl border shadow-xs"
                  style={{
                    background: paleta.superficieCard,
                    borderColor: paleta.bordaCard,
                  }}
                >
                  {/* Bloco superior de imagem */}
                  <div
                    className="h-44 w-full overflow-hidden"
                    style={{
                      background: fundoFallback,
                    }}
                  >
                    {temImagem ? (
                      <img
                        src={item.imagemUrl}
                        alt={item.modalidade}
                        className="h-full w-full object-cover"
                      />
                    ) : (
                      <div className="flex h-full w-full items-end justify-between p-4 text-white/90">
                        <span className="rounded-md bg-black/30 px-2.5 py-1 text-[11px] font-medium backdrop-blur-xs">
                          {item.duracao}
                        </span>
                      </div>
                    )}
                  </div>

                  {/* Conteúdo e valor na parte inferior */}
                  <div className="flex flex-1 flex-col justify-between p-6">
                    <div>
                      <h3
                        className="font-serif text-lg font-bold"
                        style={{ color: paleta.textoPrincipal }}
                      >
                        {item.modalidade}
                      </h3>
                      <p
                        className="mt-2 text-xs leading-relaxed sm:text-sm"
                        style={{ color: paleta.textoSecundario }}
                      >
                        {item.descricao}
                      </p>
                    </div>

                    <div
                      className="mt-6 flex items-baseline justify-between border-t pt-4"
                      style={{ borderColor: paleta.divisor }}
                    >
                      <span
                        className="text-[11px] font-semibold uppercase tracking-wider"
                        style={{ color: paleta.textoSuave }}
                      >
                        {item.duracao}
                      </span>
                      <span
                        className="font-serif text-xl font-bold"
                        style={{ color: paleta.destaqueEditorial }}
                      >
                        {item.valor}
                      </span>
                    </div>
                  </div>
                </article>
              )
            })}
          </div>
        </section>

        {/* Bloco CTA WhatsApp */}
        {dados.mostrarBotaoWhatsapp ? (
          <section
            className="flex flex-col items-center justify-between gap-6 rounded-2xl border px-6 py-8 text-center sm:flex-row sm:px-8 sm:text-left"
            style={{
              background: paleta.ctaBg,
              borderColor: paleta.ctaBorda,
            }}
          >
            <div className="space-y-1">
              <h2
                className="font-serif text-xl font-bold"
                style={{ color: paleta.textoPrincipal }}
              >
                Pronto para começar suas aulas?
              </h2>
              <p
                className="text-xs sm:text-sm"
                style={{ color: paleta.textoSecundario }}
              >
                Tire dúvidas sobre horários, disponibilidade e formatos diretamente no WhatsApp.
              </p>
            </div>

            {linkWhatsapp ? (
              <a
                href={linkWhatsapp}
                target="_blank"
                rel="noreferrer"
                className="shrink-0 rounded-xl px-7 py-3.5 text-xs font-bold transition hover:brightness-95"
                style={{ background: '#25D366', color: '#0E2416' }}
              >
                Falar pelo WhatsApp
              </a>
            ) : (
              <button
                type="button"
                onClick={() =>
                  onAviso?.('Número de WhatsApp não informado para contato direto.')
                }
                className="shrink-0 cursor-pointer rounded-xl px-7 py-3.5 text-xs font-bold transition hover:brightness-95"
                style={{ background: '#25D366', color: '#0E2416' }}
              >
                Falar pelo WhatsApp
              </button>
            )}
          </section>
        ) : null}

        {/* Outros canais */}
        <section className="space-y-5">
          <div>
            <span
              className="text-[11px] font-semibold uppercase tracking-[0.16em]"
              style={{ color: paleta.subtituloEtiqueta }}
            >
              — Redes e contato —
            </span>
            <h2
              className="mt-1 font-serif text-2xl font-bold"
              style={{ color: paleta.textoPrincipal }}
            >
              Outros canais
            </h2>
          </div>

          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <div
              className="rounded-2xl border p-5"
              style={{
                background: paleta.superficieCard,
                borderColor: paleta.bordaCard,
              }}
            >
              <div
                className="text-[11px] font-semibold uppercase tracking-wider"
                style={{ color: paleta.textoSuave }}
              >
                Instagram
              </div>
              <div
                className="mt-2 text-sm font-semibold"
                style={{ color: paleta.textoPrincipal }}
              >
                {instagramLimpo ? (
                  <a
                    href={`https://instagram.com/${instagramLimpo}`}
                    target="_blank"
                    rel="noreferrer"
                    className="underline decoration-[#8A6A52] underline-offset-4 hover:opacity-80"
                  >
                    @{instagramLimpo}
                  </a>
                ) : (
                  dados.instagram
                )}
              </div>
            </div>

            <div
              className="rounded-2xl border p-5"
              style={{
                background: paleta.superficieCard,
                borderColor: paleta.bordaCard,
              }}
            >
              <div
                className="text-[11px] font-semibold uppercase tracking-wider"
                style={{ color: paleta.textoSuave }}
              >
                E-mail
              </div>
              <div
                className="mt-2 text-sm font-semibold"
                style={{ color: paleta.textoPrincipal }}
              >
                {dados.emailPublico ? (
                  <a
                    href={`mailto:${dados.emailPublico}`}
                    className="underline decoration-[#8A6A52] underline-offset-4 hover:opacity-80"
                  >
                    {dados.emailPublico}
                  </a>
                ) : (
                  'contato@exemplo.com'
                )}
              </div>
            </div>
          </div>
        </section>
      </main>

      {/* Rodapé */}
      <footer
        className="border-t px-6 py-12 sm:px-10"
        style={{
          background: paleta.footerBg,
          borderColor: paleta.footerBorda,
          color: paleta.footerTexto,
        }}
      >
        <div className="mx-auto max-w-5xl space-y-6 text-xs">
          <div
            className="flex flex-wrap items-center justify-between gap-4 border-b pb-6"
            style={{ borderColor: paleta.footerBorda }}
          >
            <div className="font-semibold" style={{ color: '#FFFFFF' }}>
              {dados.nomePublico}, professor autônomo · CNPJ 00.000.000/0001-00
            </div>

            <div className="flex flex-wrap items-center gap-6">
              <button
                type="button"
                onClick={() => onAviso?.('Política de privacidade exibida.')}
                className="cursor-pointer underline decoration-[#8A6A52] underline-offset-4 transition hover:text-white"
              >
                Política de Privacidade
              </button>
              <button
                type="button"
                onClick={() => onAviso?.('Termos de uso exibidos.')}
                className="cursor-pointer underline decoration-[#8A6A52] underline-offset-4 transition hover:text-white"
              >
                Termos de Uso
              </button>
            </div>
          </div>

          <div
            className="flex flex-col justify-between gap-3 text-[11px] sm:flex-row sm:items-center"
            style={{ color: paleta.textoSecundario }}
          >
            <p>
              Medimos visitas e cliques de forma anônima, sem cookies nem IP. O botão abre o WhatsApp.
            </p>
            <p style={{ color: paleta.footerSubtexto }}>
              Página criada com o Compasso
            </p>
          </div>
        </div>
      </footer>
    </div>
  )
}