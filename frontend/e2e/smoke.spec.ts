import { expect, test } from '@playwright/test'

test('abre o login, alterna o tema e navega pela área administrativa', async ({ page }) => {
  await page.goto('/')

  await expect(page.getByRole('heading', { name: 'Entrar' })).toBeVisible()
  await expect(page.getByRole('navigation', { name: 'Navegação principal' })).toHaveCount(0)

  await page.getByRole('button', { name: 'Ativar tema escuro' }).click()
  await expect(page.locator('html')).toHaveAttribute('data-theme', 'escuro')

  await page.goto('/#/P21-agenda')
  await expect(page.getByRole('heading', { name: 'Agenda' })).toBeVisible()
  await expect(page.getByRole('navigation', { name: 'Navegação principal' })).toBeVisible()
  await expect(page.locator('html')).toHaveAttribute('data-theme', 'escuro')
})
