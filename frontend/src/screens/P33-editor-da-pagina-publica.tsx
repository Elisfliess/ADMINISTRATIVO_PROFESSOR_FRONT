import { useState } from 'react'
import { P34AbaApresentacao } from './P34-aba-apresentacao'
import { P35AbaModalidadesEValores } from './P35-aba-modalidades-e-valores'
import { P36AbaContatosEWhatsapp } from './P36-aba-contatos-e-whatsapp'
import {
  P40PreviaSimples,
  type DadosPaginaPublica,
  type ModalidadePublica,
} from './P40-previa-simples'
import { P41ConfirmarPublicacao } from './P41-confirmar-publicacao'

export type AbaPublica = 'Apresentação' | 'Modalidades e valores' | 'Contatos e WhatsApp'

export interface P33EditorDaPaginaPublicaProps {
  /** Aba aberta ao entrar. `null` mostra o checklist das três seções (frame P33). */
  abaInicial?: AbaPublica | null
  /** Abre o modal de publicação ao entrar (frame P41). */
  abrirPublicacaoInicial?: boolean
}

const MODALIDADES_INICIAIS: ModalidadePublica[] = [
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
]

export function P33EditorDaPaginaPublica({
  abaInicial = null,
  abrirPublicacaoInicial = false,
}: P33EditorDaPaginaPublicaProps) {
  const [abaAtiva, setAbaAtiva] = useState<AbaPublica | null>(abaInicial)
  const [modoPrevia, setModoPrevia] = useState(false)
  const [modalPublicacaoAberto, setModalPublicacaoAberto] = useState(abrirPublicacaoInicial)
  const [statusPublicacao, setStatusPublicacao] = useState<'Em edição' | 'Publicado'>('Em edição')
  const [aviso, setAviso] = useState<string | null>(null)

  const [nomePublico, setNomePublico] = useState('Prof.º José Carlos')
  const [tituloProfissional, setTituloProfissional] = useState('Professor')
  const [sobreMim, setSobreMim] = useState(
    'Aulas individuais e em dupla, com acompanhamento próximo e plano de estudo para cada aluno.'
  )
  const [formacao, setFormacao] = useState('Licenciatura')

  const [modalidades, setModalidades] = useState<ModalidadePublica[]>(MODALIDADES_INICIAIS)

  const [whatsapp, setWhatsapp] = useState('(11) 90000-0000')
  const [emailPublico, setEmailPublico] = useState('contato@exemplo.com')
  const [instagram, setInstagram] = useState('@usuario')
  const [mostrarBotaoWhatsapp, setMostrarBotaoWhatsapp] = useState(true)

  const apresentacaoCompleta =
    Boolean(nomePublico.trim()) &&
    Boolean(tituloProfissional.trim()) &&
    Boolean(sobreMim.trim()) &&
    Boolean(formacao.trim())

  const modalidadesCompletas = modalidades.length > 0

  const contatosCompletos =
    Boolean(whatsapp.trim()) &&
    Boolean(emailPublico.trim()) &&
    Boolean(instagram.trim())

  const dadosPrevia: DadosPaginaPublica = {
    nomePublico,
    tituloProfissional,
    sobreMim,
    formacao,
    modalidades,
    whatsapp,
    emailPublico,
    instagram,
    mostrarBotaoWhatsapp,
  }

  function handleAdicionarModalidade(nova: ModalidadePublica) {
    setModalidades((prev) => [...prev, nova])
    setAviso('Nova modalidade adicionada.')
  }

  function confirmarPublicacao() {
    setStatusPublicacao('Publicado')
    setModalPublicacaoAberto(false)
    setAviso('Página pública publicada com sucesso.')
  }

  function rejeitarPublicacao() {
    setModalPublicacaoAberto(false)
    setAviso('Publicação rejeitada. A página pública atual continua como está.')
  }

  if (modoPrevia) {
    return (
      <>
        <P40PreviaSimples
          dados={dadosPrevia}
          aviso={aviso}
          onVoltarEditor={() => setModoPrevia(false)}
          onAbrirPublicar={() => {
            setAviso(null)
            setModalPublicacaoAberto(true)
          }}
          onAviso={(msg) => setAviso(msg)}
        />
        <P41ConfirmarPublicacao
          aberto={modalPublicacaoAberto}
          onConfirmar={confirmarPublicacao}
          onRejeitar={rejeitarPublicacao}
        />
      </>
    )
  }

  return (
    <section className="relative space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div className="flex items-center gap-3">
          <h1 className="text-2xl font-bold tracking-tight" style={{ color: 'var(--text)' }}>
            Página pública
          </h1>
          <span
            className="rounded-full px-3.5 py-1 text-xs font-medium"
            style={{
              background: statusPublicacao === 'Publicado' ? '#DCECE3' : '#F4E6C8',
              color: statusPublicacao === 'Publicado' ? '#23533A' : '#6E4F16',
            }}
          >
            {statusPublicacao}
          </span>
        </div>

        <div className="flex items-center gap-3">
          <button
            type="button"
            onClick={() => {
              setAviso(null)
              setModoPrevia(true)
            }}
            className="rounded-xl px-6 py-3 text-xs font-medium transition"
            style={{ background: '#EAE1D5', color: '#3B281B' }}
          >
            Ver prévia
          </button>
          <button
            type="button"
            onClick={() => {
              setAviso(null)
              setModalPublicacaoAberto(true)
            }}
            className="rounded-xl px-7 py-3 text-xs font-semibold transition"
            style={{ background: '#5C3419', color: '#FFFFFF' }}
          >
            Publicar
          </button>
        </div>
      </div>

      {aviso ? (
        <div
          role="status"
          className="rounded-xl border px-4 py-2.5 text-xs font-medium"
          style={{ background: '#FAF6F0', borderColor: '#D8CEBF', color: '#5C3419' }}
        >
          {aviso}
        </div>
      ) : null}

      <div className="flex flex-wrap items-center gap-3">
        {(['Apresentação', 'Modalidades e valores', 'Contatos e WhatsApp'] as AbaPublica[]).map(
          (aba) => {
            const ativa = abaAtiva === aba
            return (
              <button
                key={aba}
                type="button"
                onClick={() => setAbaAtiva((prev) => (prev === aba ? null : aba))}
                className="rounded-full px-6 py-2.5 text-xs font-medium transition"
                style={{
                  background: ativa ? '#5C3419' : '#EAE1D5',
                  color: ativa ? '#FFFFFF' : '#3B281B',
                }}
              >
                {aba}
              </button>
            )
          }
        )}
      </div>

      {abaAtiva === null ? (
        <div
          className="divide-y rounded-3xl border px-6 py-2"
          style={{
            background: '#FAF6F0',
            borderColor: 'color-mix(in srgb, var(--border) 35%, transparent)',
          }}
        >
          <div className="py-5 text-xs" style={{ color: '#6E5D52' }}>
            Complete as três seções e publique quando quiser.
          </div>

          <div className="flex items-center justify-between gap-4 py-5">
            <button
              type="button"
              onClick={() => setAbaAtiva('Apresentação')}
              className="text-left text-xs font-semibold hover:underline"
              style={{ color: '#2B1B10' }}
            >
              Apresentação
            </button>
            <span
              className="rounded-full px-4 py-1.5 text-xs font-medium"
              style={{
                background: apresentacaoCompleta ? '#DCECE3' : '#F4E6C8',
                color: apresentacaoCompleta ? '#23533A' : '#6E4F16',
              }}
            >
              {apresentacaoCompleta ? 'Completo' : 'Falta preencher'}
            </span>
          </div>

          <div className="flex items-center justify-between gap-4 py-5">
            <button
              type="button"
              onClick={() => setAbaAtiva('Modalidades e valores')}
              className="text-left text-xs font-semibold hover:underline"
              style={{ color: '#2B1B10' }}
            >
              Modalidades e valores
            </button>
            <span
              className="rounded-full px-4 py-1.5 text-xs font-medium"
              style={{
                background: modalidadesCompletas ? '#DCECE3' : '#F4E6C8',
                color: modalidadesCompletas ? '#23533A' : '#6E4F16',
              }}
            >
              {modalidadesCompletas ? 'Completo' : 'Falta preencher'}
            </span>
          </div>

          <div className="flex items-center justify-between gap-4 py-5">
            <button
              type="button"
              onClick={() => setAbaAtiva('Contatos e WhatsApp')}
              className="text-left text-xs font-semibold hover:underline"
              style={{ color: '#2B1B10' }}
            >
              Contatos e WhatsApp
            </button>
            <span
              className="rounded-full px-4 py-1.5 text-xs font-medium"
              style={{
                background: contatosCompletos ? '#DCECE3' : '#F4E6C8',
                color: contatosCompletos ? '#23533A' : '#6E4F16',
              }}
            >
              {contatosCompletos ? 'Completo' : 'Falta preencher'}
            </span>
          </div>
        </div>
      ) : null}

      {abaAtiva === 'Apresentação' ? (
        <P34AbaApresentacao
          nomePublico={nomePublico}
          tituloProfissional={tituloProfissional}
          sobreMim={sobreMim}
          formacao={formacao}
          onChangeNomePublico={setNomePublico}
          onChangeTituloProfissional={setTituloProfissional}
          onChangeSobreMim={setSobreMim}
          onChangeFormacao={setFormacao}
        />
      ) : null}

      {abaAtiva === 'Modalidades e valores' ? (
        <P35AbaModalidadesEValores
          modalidades={modalidades}
          onAdicionarModalidade={handleAdicionarModalidade}
        />
      ) : null}

      {abaAtiva === 'Contatos e WhatsApp' ? (
        <P36AbaContatosEWhatsapp
          whatsapp={whatsapp}
          emailPublico={emailPublico}
          instagram={instagram}
          mostrarBotaoWhatsapp={mostrarBotaoWhatsapp}
          onChangeWhatsapp={setWhatsapp}
          onChangeEmailPublico={setEmailPublico}
          onChangeInstagram={setInstagram}
          onToggleMostrarBotaoWhatsapp={() => setMostrarBotaoWhatsapp((prev) => !prev)}
        />
      ) : null}

      <P41ConfirmarPublicacao
        aberto={modalPublicacaoAberto}
        onConfirmar={confirmarPublicacao}
        onRejeitar={rejeitarPublicacao}
      />
    </section>
  )
}