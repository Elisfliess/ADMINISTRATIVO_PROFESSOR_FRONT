import type { ReactNode } from 'react'
import { navigate, useHashRoute } from '../router/hashRouter'
import { useTheme } from '../theme/useTheme'
import { ThemeToggle } from './ThemeToggle'

const adminLinks = [
  { slug: 'P21-agenda', label: 'Agenda', grupo: 'agenda' },
  { slug: 'P11-lista-de-alunos', label: 'Alunos', grupo: 'alunos' },
  { slug: 'P28-visao-geral-do-mes', label: 'Financeiro', grupo: 'financeiro' },
  { slug: 'P33-editor-da-pagina-publica', label: 'Página pública', grupo: 'pagina' },
  { slug: 'P42-dashboard-da-pagina-publica', label: 'Indicadores', grupo: 'indicadores' },
  { slug: 'P43-configuracoes-de-conta', label: 'Conta', grupo: 'conta' },
]

/** Agrupa as telas filhas (abas, modais e detalhes) sob o item de menu correspondente. */
const grupoPorTela: Record<string, string> = {
  'P21-agenda': 'agenda',
  'P22-criar-ou-editar-aula': 'agenda',
  'P23-detalhe-da-aula': 'agenda',
  'P25-remarcar-aula': 'agenda',
  'P26-cancelar-aula': 'agenda',
  'P27-marcar-status': 'agenda',

  'P11-lista-de-alunos': 'alunos',
  'P12-cadastrar-aluno': 'alunos',
  'P13-ficha-do-aluno': 'alunos',
  'P14-aba-historico-de-aulas': 'alunos',
  'P15-aba-cobrancas': 'alunos',
  'P16-editar-aluno': 'alunos',
  'P17-inativar-ou-reativar': 'alunos',
  'P18-exportar-dados': 'alunos',
  'P19-excluir-ou-anonimizar': 'alunos',
  'P49-plano-encerrar-e-novo': 'alunos',

  'P28-visao-geral-do-mes': 'financeiro',
  'P29-filtros-em-atraso-e-pendentes': 'financeiro',
  'P30-dar-baixa-na-cobranca': 'financeiro',
  'P32-exportar-recebimentos': 'financeiro',
  'P50-cancelar-cobranca': 'financeiro',

  'P33-editor-da-pagina-publica': 'pagina',
  'P34-aba-apresentacao': 'pagina',
  'P35-aba-modalidades-e-valores': 'pagina',
  'P36-aba-contatos-e-whatsapp': 'pagina',
  'P40-previa-simples': 'pagina',
  'P41-confirmar-publicacao': 'pagina',

  'P42-dashboard-da-pagina-publica': 'indicadores',

  'P43-configuracoes-de-conta': 'conta',
  'P45-tema-claro-e-escuro': 'conta',
  'P46-encerrar-conta': 'conta',
  'P48-termos-e-politica-leitura': 'conta',
}

const publicLinks = [
  { slug: 'P01-login', label: 'Entrar' },
  { slug: 'P02-cadastro', label: 'Cadastrar' },
]

const publicSlugs = new Set([
  'G04-sessao-expirada',
  'P01-login',
  'P02-cadastro',
  'P03-verifique-seu-e-mail',
  'P04-e-mail-confirmado-ou-link-invalido',
  'P05-recuperar-senha',
  'P06-e-mail-de-recuperacao-enviado',
  'P07-redefinir-senha',
  'P08-aceite-de-termos-e-politica',
  'V02-politica-de-privacidade',
  'V04-termos-de-uso',
])

/** Telas que trazem o próprio cabeçalho/rodapé público (sem o chrome do app). */
const standaloneSlugs = new Set([
  'P40-previa-simples',
  'V01-pagina-publica-do-professor',
  'V03-pagina-nao-encontrada',
])

export function AppShell({ children }: { children: ReactNode }) {
  const { slug } = useHashRoute()
  const { theme } = useTheme()

  if (slug && standaloneSlugs.has(slug)) return <>{children}</>

  const isPublic = !slug || slug === 'catalogo' || publicSlugs.has(slug)
  const home = isPublic ? 'P01-login' : 'P21-agenda'
  const grupoAtivo = slug ? grupoPorTela[slug] : undefined
  const escuro = theme === 'escuro'

  return (
    <div className="app-shell flex min-h-screen flex-col">
      <header
        className="border-b"
        style={{
          background: escuro ? '#23140A' : '#5C3419',
          borderColor: escuro ? '#3A2315' : '#4A2A14',
          color: '#FFFFFF',
        }}
      >
        <div className="mx-auto flex w-full max-w-[1400px] flex-wrap items-center justify-between gap-3 px-4 py-2.5 sm:px-6">
          {isPublic ? (
            <button
              className="flex min-h-11 items-center gap-2 text-left"
              onClick={() => navigate(`/${home}`)}
              aria-label="Compasso — início"
            >
              <span
                className="grid h-9 w-9 place-items-center rounded-xl text-base font-bold"
                style={{ background: 'rgba(255, 255, 255, 0.16)', color: '#FFFFFF' }}
                aria-hidden="true"
              >
                C
              </span>
              <span className="leading-tight">
                <strong className="block text-sm">Compasso</strong>
                <small className="block text-[11px]" style={{ color: 'rgba(255, 255, 255, 0.72)' }}>
                  Professor particular de música
                </small>
              </span>
            </button>
          ) : (
            <div className="flex items-center gap-3">
              <span
                className="grid h-8 w-8 place-items-center rounded-full border text-[11px] font-semibold"
                style={{
                  background: escuro ? '#342217' : '#E7DDD0',
                  borderColor: 'rgba(255, 255, 255, 0.28)',
                  color: escuro ? '#F2C99E' : '#5C3419',
                }}
                aria-hidden="true"
              >
                JC
              </span>
              <span className="text-xs font-medium" style={{ color: 'rgba(255, 255, 255, 0.9)' }}>
                Prof.º José Carlos
              </span>
            </div>
          )}

          <div className="flex flex-wrap items-center gap-2">
            {isPublic
              ? publicLinks.map((link) => (
                  <button
                    key={link.slug}
                    className="rounded-xl border px-3.5 py-2 text-xs font-medium transition hover:opacity-90"
                    style={{ borderColor: 'rgba(255, 255, 255, 0.28)', color: '#FFFFFF' }}
                    onClick={() => navigate(`/${link.slug}`)}
                  >
                    {link.label}
                  </button>
                ))
              : null}
            {!isPublic ? (
              <button
                type="button"
                onClick={() => navigate('/P10-sair')}
                className="rounded-xl border px-3.5 py-2 text-xs font-medium transition hover:opacity-90"
                style={{ borderColor: 'rgba(255, 255, 255, 0.28)', color: '#FFFFFF' }}
              >
                Sair
              </button>
            ) : null}
            <ThemeToggle />
          </div>
        </div>
      </header>

      {isPublic ? (
        <>
          <main className="mx-auto w-full max-w-7xl flex-1 px-4 py-8 sm:px-6">{children}</main>
          <footer
            className="divider mx-4 mt-auto flex flex-wrap items-center justify-between gap-3 py-6 text-sm sm:mx-6"
            style={{ color: 'var(--muted)' }}
          >
            <span>© 2026 Compasso</span>
            <div className="flex flex-wrap gap-4">
              <button className="link" onClick={() => navigate('/V02-politica-de-privacidade')}>
                Política de Privacidade
              </button>
              <button className="link" onClick={() => navigate('/V04-termos-de-uso')}>
                Termos de Uso
              </button>
            </div>
          </footer>
        </>
      ) : (
        <div className="app-body">
          <aside className="sidebar" aria-label="Navegação principal">
            {adminLinks.map((link) => {
              const ativo = grupoAtivo === link.grupo
              return (
                <button
                  key={link.slug}
                  className="nav-link w-full text-left"
                  aria-current={ativo ? 'page' : undefined}
                  onClick={() => navigate(`/${link.slug}`)}
                >
                  {link.label}
                </button>
              )
            })}
          </aside>
          <main className="main-content">{children}</main>
        </div>
      )}
    </div>
  )
}
