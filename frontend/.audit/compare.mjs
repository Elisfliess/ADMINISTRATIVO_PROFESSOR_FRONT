import fs from 'node:fs/promises'
import path from 'node:path'

const FIGMA = 'C:/Users/vinic/Documents/Projeto Padrão/figma-plugin-export/figma-export'
const APP = process.env.APP_DIR || 'C:/Users/vinic/Documents/Projeto Padrão/app-export-1280'
const OUT = 'C:/Users/vinic/Documents/Projeto Padrão/comparacao'

const norm = (s) =>
  String(s || '')
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/[\u200b-\u200f\u2028\u2029\ufeff]/g, '')
    .replace(/\s+/g, ' ')
    .trim()
    .toLowerCase()

const figmaFiles = (await fs.readdir(FIGMA)).filter((f) => f.endsWith('.json'))
const appFiles = (await fs.readdir(APP)).filter((f) => f.endsWith('.json') && !f.startsWith('_'))

const figmaByCode = new Map()
for (const file of figmaFiles) {
  const meta = JSON.parse(await fs.readFile(path.join(FIGMA, file), 'utf8'))
  const code = (meta.code || file.match(/^([PVG]\d+)/)?.[1] || '').toUpperCase()
  const theme = /claro$/i.test(meta.name) ? 'claro' : /escuro$/i.test(meta.name) ? 'escuro' : 'unico'
  const key = `${code}|${theme}`
  if (!figmaByCode.has(key)) figmaByCode.set(key, { file, meta })
}

const screens = []
for (const file of appFiles) {
  const slug = file.replace(/\.json$/, '')
  const app = JSON.parse(await fs.readFile(path.join(APP, file), 'utf8'))
  const code = slug.match(/^([PVG]\d+)/)?.[1]?.toUpperCase()
  const fig = figmaByCode.get(`${code}|claro`) || figmaByCode.get(`${code}|unico`)
  if (!fig) {
    screens.push({ slug, code, missingFrame: true })
    continue
  }

  const appText = norm(app.textNodes.map((t) => t.text).join(' \n '))
  const figTexts = fig.meta.texts.filter((t) => t.visible !== false).map((t) => t.text)
  const figUnique = []
  const seen = new Set()
  for (const text of figTexts) {
    const n = norm(text)
    if (!n || seen.has(n)) continue
    seen.add(n)
    figUnique.push(text)
  }

  const missing = figUnique.filter((t) => !appText.includes(norm(t)))

  const figFull = norm(figUnique.join(' \n '))
  const appUnique = []
  const seenApp = new Set()
  for (const t of app.textNodes) {
    const n = norm(t.text)
    if (!n || seenApp.has(n)) continue
    seenApp.add(n)
    appUnique.push(t.text)
  }
  const extra = appUnique.filter((t) => !figFull.includes(norm(t)))

  screens.push({
    slug,
    code,
    figmaFrame: fig.meta.name,
    figmaSize: `${fig.meta.width}x${fig.meta.height}`,
    figmaTexts: figUnique.length,
    appTexts: appUnique.length,
    missing,
    extra,
  })
}

await fs.mkdir(OUT, { recursive: true })
await fs.writeFile(path.join(OUT, 'text-diff.json'), JSON.stringify(screens, null, 2), 'utf8')

let md = '# Diferenças de texto — Figma × app (tema claro)\n\n'
for (const s of screens) {
  if (s.missingFrame) {
    md += `## ${s.slug}\n- Frame não encontrado no Figma.\n\n`
    continue
  }
  md += `## ${s.slug} (${s.figmaFrame})\n`
  md += `- Figma: ${s.figmaTexts} textos · App: ${s.appTexts} textos\n`
  if (!s.missing.length && !s.extra.length) {
    md += `- Sem diferenças de texto.\n\n`
    continue
  }
  if (s.missing.length) {
    md += `- **Falta no app (${s.missing.length}):**\n`
    for (const t of s.missing) md += `  - ${JSON.stringify(t)}\n`
  }
  if (s.extra.length) {
    md += `- **Só no app (${s.extra.length}):**\n`
    for (const t of s.extra) md += `  - ${JSON.stringify(t)}\n`
  }
  md += `\n`
}
await fs.writeFile(path.join(OUT, 'text-diff.md'), md, 'utf8')

console.log(`Comparadas ${screens.length} telas. Relatório em ${OUT}`)
for (const s of screens) {
  if (s.missingFrame) {
    console.log(`?? ${s.slug}: frame ausente`)
    continue
  }
  console.log(
    `${s.slug}: falta ${s.missing.length} · sobra ${s.extra.length} (figma ${s.figmaSize})`
  )
}
