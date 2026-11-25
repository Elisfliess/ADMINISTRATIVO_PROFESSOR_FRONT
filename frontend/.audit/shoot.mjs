import { chromium } from '@playwright/test'
import fs from 'node:fs/promises'
import path from 'node:path'

const BASE = process.env.APP_BASE || 'http://127.0.0.1:5173'
const OUT = process.env.OUT_DIR || 'C:/Users/vinic/Documents/Projeto Padrão/app-export'
const THEME = process.env.THEME || 'claro'
const WIDTH = Number(process.env.WIDTH || 1440)
const HEIGHT = Number(process.env.HEIGHT || 900)

// Telas após o login (autenticação/termos públicos ficam de fora).
const ROUTES = [
  'P10-sair',
  'P11-lista-de-alunos',
  'P12-cadastrar-aluno',
  'P13-ficha-do-aluno',
  'P14-aba-historico-de-aulas',
  'P15-aba-cobrancas',
  'P16-editar-aluno',
  'P17-inativar-ou-reativar',
  'P18-exportar-dados',
  'P19-excluir-ou-anonimizar',
  'P21-agenda',
  'P22-criar-ou-editar-aula',
  'P23-detalhe-da-aula',
  'P25-remarcar-aula',
  'P26-cancelar-aula',
  'P27-marcar-status',
  'P28-visao-geral-do-mes',
  'P29-filtros-em-atraso-e-pendentes',
  'P30-dar-baixa-na-cobranca',
  'P32-exportar-recebimentos',
  'P33-editor-da-pagina-publica',
  'P34-aba-apresentacao',
  'P35-aba-modalidades-e-valores',
  'P36-aba-contatos-e-whatsapp',
  'P40-previa-simples',
  'P41-confirmar-publicacao',
  'P42-dashboard-da-pagina-publica',
  'P43-configuracoes-de-conta',
  'P45-tema-claro-e-escuro',
  'P46-encerrar-conta',
  'P48-termos-e-politica-leitura',
  'P49-plano-encerrar-e-novo',
  'P50-cancelar-cobranca',
  'V01-pagina-publica-do-professor',
  'V03-pagina-nao-encontrada',
]

await fs.mkdir(OUT, { recursive: true })

const browser = await chromium.launch({
  executablePath: process.env.CHROME_PATH || 'C:/Program Files/Google/Chrome/Application/chrome.exe',
})
const context = await browser.newContext({
  viewport: { width: WIDTH, height: HEIGHT },
  deviceScaleFactor: 1,
  colorScheme: THEME === 'escuro' ? 'dark' : 'light',
  locale: 'pt-BR',
})
await context.addInitScript((theme) => {
  window.localStorage.setItem('compasso-tema', theme)
}, THEME)

const results = []

for (const slug of ROUTES) {
  const url = `${BASE}/#/${slug}`
  const routeErrors = []
  const page = await context.newPage()
  page.on('pageerror', (err) => routeErrors.push(String(err.message || err)))
  try {
    await page.goto(url, { waitUntil: 'networkidle' })
    await page.evaluate(() => document.fonts.ready)
    await page.waitForTimeout(150)

    const meta = await page.evaluate(() => {
      const pick = (selector) => Array.from(document.querySelectorAll(selector)).map((el) => el.textContent?.trim() || '')
      const rect = (el) => {
        const r = el.getBoundingClientRect()
        return { x: Math.round(r.x), y: Math.round(r.y), w: Math.round(r.width), h: Math.round(r.height) }
      }
      const textNodes = []
      const walker = document.createTreeWalker(document.body, NodeFilter.SHOW_TEXT)
      let node
      while ((node = walker.nextNode())) {
        const text = (node.textContent || '').replace(/\s+/g, ' ').trim()
        if (!text) continue
        const parent = node.parentElement
        if (!parent) continue
        const style = getComputedStyle(parent)
        if (style.visibility === 'hidden' || style.display === 'none') continue
        const r = parent.getBoundingClientRect()
        if (r.width === 0 && r.height === 0) continue
        textNodes.push({
          text,
          tag: parent.tagName.toLowerCase(),
          box: rect(parent),
          font: { family: style.fontFamily, size: style.fontSize, weight: style.fontWeight, lineHeight: style.lineHeight },
          color: style.color,
        })
      }
      return {
        title: document.title,
        scrollWidth: document.documentElement.scrollWidth,
        scrollHeight: document.documentElement.scrollHeight,
        headings: pick('h1, h2, h3'),
        buttons: pick('button'),
        links: pick('a'),
        labels: pick('label'),
        textNodes,
      }
    })

    await page.screenshot({ path: path.join(OUT, `${slug}.png`), fullPage: true })
    await fs.writeFile(path.join(OUT, `${slug}.json`), JSON.stringify({ ...meta, pageErrors: routeErrors }, null, 2), 'utf8')
    results.push({ slug, ok: true, width: meta.scrollWidth, height: meta.scrollHeight, pageErrors: routeErrors })
    console.log(`ok ${slug} ${meta.scrollWidth}x${meta.scrollHeight}${routeErrors.length ? ` · ERRO: ${routeErrors.join(' | ')}` : ''}`)
  } catch (error) {
    results.push({ slug, ok: false, error: String(error?.message || error) })
    console.log(`ERRO ${slug}: ${error?.message || error}`)
  } finally {
    await page.close()
  }
}

await browser.close()
await fs.writeFile(path.join(OUT, '_results.json'), JSON.stringify(results, null, 2), 'utf8')
console.log(`\n${results.filter((r) => r.ok).length}/${results.length} telas capturadas em ${OUT}`)
