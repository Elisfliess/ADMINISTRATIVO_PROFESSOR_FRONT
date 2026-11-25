import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it } from 'vitest'
import App from '../src/App'
import { screenRegistry } from '../src/screens/registry'

function renderAt(id: string) {
  window.location.hash = `#/${id}`
  return render(<App />)
}

describe('navegação do produto', () => {
  it('mantém os 46 fluxos implementados com seus códigos internos', () => {
    expect(screenRegistry).toHaveLength(46)
    expect(screenRegistry.map((entry) => entry.id)).toEqual(
      expect.arrayContaining([
        'P01-login',
        'P21-agenda',
        'P33-editor-da-pagina-publica',
        'V04-termos-de-uso',
      ]),
    )
    expect(screenRegistry.find((entry) => entry.id === 'P01-login')?.code).toBe('P01')
  })

  it('não mostra códigos de frame nem textos de protótipo na interface', () => {
    renderAt('P21-agenda')

    expect(screen.getByRole('heading', { name: 'Agenda' })).toBeInTheDocument()
    expect(screen.queryByText('P21')).not.toBeInTheDocument()
    expect(
      screen.queryByText(/Figma|protótipo|dados fictícios|RF\d+|RN\d+/i),
    ).not.toBeInTheDocument()
  })

  it('navega do login para cadastro e recuperação sem catálogo', async () => {
    const user = userEvent.setup()
    renderAt('P01-login')

    await user.click(screen.getByRole('button', { name: 'Criar conta' }))
    expect(window.location.hash).toBe('#/P02-cadastro')

    await user.click(screen.getByRole('button', { name: 'Já tenho conta' }))
    expect(window.location.hash).toBe('#/P01-login')

    await user.click(screen.getByRole('button', { name: 'Esqueci minha senha' }))
    expect(window.location.hash).toBe('#/P05-recuperar-senha')
  })
})

describe('fluxos críticos', () => {
  beforeEach(() => {
    window.localStorage.clear()
  })

  it('valida o login e abre o aceite de termos no primeiro acesso', async () => {
    const user = userEvent.setup()
    renderAt('P01-login')

    const form = screen.getByRole('form', { name: 'Entrar' })
    await user.click(within(form).getByRole('button', { name: 'Entrar' }))
    expect(within(form).getAllByText('Campo obrigatório.').length).toBeGreaterThan(0)

    await user.type(within(form).getByLabelText('E-mail'), 'professor@exemplo.invalid')
    await user.type(within(form).getByLabelText('Senha'), 'senha1234')
    await user.click(within(form).getByRole('button', { name: 'Entrar' }))

    expect(window.location.hash).toBe('#/P08-aceite-de-termos-e-politica')
    expect(screen.getByRole('heading', { name: 'Aceite de termos e política' })).toBeInTheDocument()
  })

  it('leva do cadastro para a confirmação de e-mail', async () => {
    const user = userEvent.setup()
    renderAt('P02-cadastro')

    const form = screen.getByRole('form', { name: 'Cadastrar' })
    await user.type(within(form).getByLabelText('Nome'), 'Professor Exemplo')
    await user.type(within(form).getByLabelText('Razão social'), 'Professor Exemplo Música LTDA')
    await user.type(within(form).getByLabelText('E-mail da conta'), 'professor@exemplo.invalid')
    await user.type(within(form).getByLabelText('Senha'), 'senha1234')
    await user.click(within(form).getByRole('button', { name: 'Cadastrar' }))

    expect(window.location.hash).toBe('#/P03-verifique-seu-e-mail')
  })

  it('exige aceite explícito antes de liberar as funções administrativas', async () => {
    const user = userEvent.setup()
    renderAt('P08-aceite-de-termos-e-politica')

    const confirmar = screen.getByRole('button', { name: 'Aceitar e continuar' })
    expect(confirmar).toBeDisabled()

    await user.click(
      screen.getByLabelText(
        'Li e aceito os Termos de Uso e a Política de Privacidade (versão 1.0).',
      ),
    )
    await user.click(confirmar)

    expect(window.location.hash).toBe('#/P11-lista-de-alunos')
  })

  it('publica a página apenas após prévia e aprovação explícita', async () => {
    const user = userEvent.setup()
    renderAt('P33-editor-da-pagina-publica')

    await user.click(screen.getByRole('button', { name: 'Ver prévia' }))
    expect(window.location.hash).toBe('#/P40-previa-simples')

    await user.click(screen.getByRole('button', { name: 'Aprovar e publicar' }))
    expect(window.location.hash).toBe('#/P41-confirmar-publicacao')

    const publicar = screen.getByRole('button', { name: 'Aprovar e publicar' })
    expect(publicar).toBeDisabled()
    await user.click(screen.getByLabelText('Aprovo explicitamente a publicação desta versão.'))
    await user.click(publicar)

    expect(window.location.hash).toBe('#/P42-dashboard-da-pagina-publica')
  })
})

describe('tema A11', () => {
  it('alterna entre claro e escuro em todas as telas', async () => {
    const user = userEvent.setup()
    window.localStorage.clear()
    renderAt('P45-tema-claro-e-escuro')

    await user.click(screen.getByRole('button', { name: 'Tema escuro' }))
    expect(document.documentElement.dataset.theme).toBe('escuro')

    await user.click(screen.getByRole('button', { name: 'Tema claro' }))
    expect(document.documentElement.dataset.theme).toBe('claro')
  })
})
