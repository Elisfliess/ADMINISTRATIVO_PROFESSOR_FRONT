import { chromium } from '@playwright/test'

const routes = process.argv.slice(2)
const theme = process.env.THEME === 'escuro' ? 'escuro' : 'claro'
const browser = await chromium.launch({ executablePath: 'C:/Program Files/Google/Chrome/Application/chrome.exe' })

for (const slug of routes) {
  const page = await browser.newPage({ viewport: { width: 1280, height: 800 } })
  await page.addInitScript((t) => window.localStorage.setItem('compasso-tema', t), theme)
  const errors = []
  const onConsole = (msg) => {
    if (msg.type() === 'error') errors.push(msg.text())
  }
  const onPageError = (err) => errors.push(`PAGEERROR: ${err.message}`)
  page.on('console', onConsole)
  page.on('pageerror', onPageError)
  await page.goto(`http://localhost:5173/#/${slug}`, { waitUntil: 'networkidle' })
  await page.waitForTimeout(400)
  const info = await page.evaluate(() => ({
    hash: location.hash,
    root: document.getElementById('root')?.innerHTML.slice(0, 200) ?? null,
    text: document.body.innerText.slice(0, 500),
    mainHtml: document.querySelector('main')?.innerHTML.slice(0, 300) ?? null,
  }))
  console.log(`\n=== ${slug} ===`)
  console.log('hash:', info.hash)
  console.log('root:', info.root)
  console.log('main:', info.mainHtml)
  console.log('text:', info.text.replace(/\n+/g, ' | '))
  console.log('errors:', errors.length ? errors : 'nenhum')
  await page.close()
}

await browser.close()
