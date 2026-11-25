import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import App from '../src/App'

describe('App', () => {
  it('abre o login como ponto de entrada, sem anotações de protótipo', () => {
    window.location.hash = ''
    render(<App />)

    expect(screen.getByRole('heading', { name: 'Entrar' })).toBeInTheDocument()
    expect(
      screen.queryByRole('navigation', { name: 'Navegação principal' }),
    ).not.toBeInTheDocument()
    expect(screen.queryByText(/Figma|protótipo|dados fictícios/i)).not.toBeInTheDocument()
  })

  it('mostra a navegação administrativa nas telas internas', () => {
    window.location.hash = '#/P21-agenda'
    render(<App />)

    expect(screen.getByRole('navigation', { name: 'Navegação principal' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Alunos' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Sair' })).toBeInTheDocument()
  })

  it('leva a uma página 404 para rotas desconhecidas', () => {
    window.location.hash = '#/rota-inexistente'
    render(<App />)

    expect(screen.getByRole('heading', { name: 'Página não encontrada' })).toBeInTheDocument()
  })
})
