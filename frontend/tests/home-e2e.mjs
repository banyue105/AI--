import assert from 'node:assert/strict'
import { mkdir, writeFile } from 'node:fs/promises'
import { resolve } from 'node:path'
import { chromium } from 'playwright-core'

const origin = process.env.HOME_ORIGIN || 'http://127.0.0.1:5173'
const output = resolve(process.env.HOME_VISUAL_DIR || 'test-results/home-redesign')
await mkdir(output, { recursive: true })
const browser = await chromium.launch({ executablePath: process.env.CHROME_PATH || 'C:/Program Files/Google/Chrome/Application/chrome.exe', headless: true })
const context = await browser.newContext({ viewport: { width: 1280, height: 720 }, reducedMotion: 'reduce', locale: 'zh-CN' })
const page = await context.newPage()
const checks = [], measurements = [], pageErrors = [], consoleErrors = [], blockedWrites = []
const expectedBlockedUrls = new Set()
const calculationPaths = new Set(['/api/v1/ai/explainable-path', '/api/v1/ability/path'])
let expectedFault = false
page.on('pageerror', error => pageErrors.push(error.message))
page.on('console', message => {
  if (message.type() !== 'error') return
  if (message.location().url.endsWith('/favicon.ico') && message.text().includes('404')) return
  if (expectedBlockedUrls.has(message.location().url) && /Failed to load resource/.test(message.text())) return
  if (expectedFault && /Failed to load resource/.test(message.text())) return
  consoleErrors.push(message.text())
})
await context.route('**/api/v1/**', async route => {
  if (['GET', 'HEAD', 'OPTIONS'].includes(route.request().method())) return route.continue()
  blockedWrites.push({ method: route.request().method(), url: route.request().url() })
  if (calculationPaths.has(new URL(route.request().url()).pathname)) {
    expectedBlockedUrls.add(route.request().url())
    // Ability initialization uses POST for path calculations. Keep this suite
    // read-only and exercise its existing local fallback for these requests.
    return route.fulfill({ status: 503, contentType: 'application/json', body: '{"error":{"message":"Read-only visual verification"}}' })
  }
  return route.abort('blockedbyclient')
})
const entries = [
  { id: 'ability-growth', path: '/ability', anchor: 'ability-highlights', title: '能力成长', header: '.ability-header' },
  { id: 'decision-sandbox', path: '/decision', anchor: 'decision-highlights', title: '决策沙盒', header: '.decision-header' },
  { id: 'module3', path: '/module3', anchor: 'practice-highlights', title: '实践验证与复盘', header: '.practice-header' },
  { id: 'module4', path: '/module4/match', anchor: 'career-highlights', title: '职业规划', header: '.career-header' },
]
function mark(name) { checks.push(name); console.log(`PASS ${name}`) }
async function settled() {
  await page.evaluate(() => document.fonts.ready)
  await page.waitForFunction(() => !document.querySelector('[class*="-enter-active"], [class*="-leave-active"]'))
  await page.evaluate(() => new Promise(resolve => requestAnimationFrame(() => requestAnimationFrame(resolve))))
}
async function measure(name) {
  const result = await page.evaluate(() => ({ width: innerWidth, scrollWidth: document.documentElement.scrollWidth, height: innerHeight }))
  assert.ok(result.scrollWidth <= result.width, `${name}: horizontal overflow ${result.scrollWidth - result.width}px`)
  measurements.push({ name, ...result })
}
async function atAnchor(entry) {
  await page.locator(`#${entry.anchor}`).waitFor()
  await page.waitForFunction(({ anchor, header }) => {
    const target = document.getElementById(anchor)
    const nav = document.querySelector('.app-nav').getBoundingClientRect()
    const toolbar = document.querySelector(header).getBoundingClientRect()
    const top = target.getBoundingClientRect().top
    return target.open && top >= Math.max(nav.bottom, toolbar.bottom) - 1 && top < innerHeight
  }, entry)
  assert.ok(await page.locator(`#${entry.anchor} summary`).evaluate(element => element === document.activeElement), 'highlight focus')
  await measure(entry.anchor)
}
async function assertActive(title) {
  const selector = page.viewportSize().width > 900 ? '.app-nav-desktop' : '#app-mobile-menu'
  if (page.viewportSize().width <= 900) await page.getByRole('button', { name: '打开导航菜单', exact: true }).click()
  const links = page.locator(`${selector} a[aria-current="page"]`)
  assert.equal(await links.count(), 1)
  assert.equal((await links.innerText()).trim(), title)
  if (page.viewportSize().width <= 900) await page.keyboard.press('Escape')
}

try {
  for (const viewport of [{ width: 1280, height: 720 }, { width: 360, height: 800 }]) {
    await page.setViewportSize(viewport)
    await page.goto(origin + '/')
    await settled()
    assert.deepEqual(await page.locator('.home-page > section').evaluateAll(items => items.map(item => item.id)), ['home-top', 'home-modules', 'home-highlights'])
    assert.equal(await page.locator('.home-entry-link').count(), 4)
    assert.equal(await page.locator('.home-feature-action a').count(), 4)
    assert.equal(await page.locator('.app-nav').count(), 1)
    assert.equal(await page.locator('.app-nav-toggle').isVisible(), viewport.width <= 900)
    assert.ok(await page.locator('.home-hero-visual img').evaluate(image => image.complete && image.naturalWidth > 0))
    await measure('homepage')
    for (let i = 0; i < 4; i++) {
      await page.locator('.home-preview img').nth(i).scrollIntoViewIfNeeded()
      await page.waitForFunction(index => {
        const image = document.querySelectorAll('.home-preview img')[index]
        return image.complete && image.naturalWidth > 0
      }, i)
    }
    await page.evaluate(() => scrollTo({ top: 0, behavior: 'instant' }))
    await page.screenshot({ path: resolve(output, `home-${viewport.width}.png`), fullPage: true })
    await page.screenshot({ path: resolve(output, `home-first-screen-${viewport.width}.png`) })
    mark(`${viewport.width} homepage structure, assets and layout`)
    await page.locator('.home-hero-actions a').first().click()
    await page.waitForURL('**/#home-modules')
    await page.waitForFunction(() => document.activeElement?.id === 'home-modules-title')
    assert.ok(await page.locator('#home-modules').evaluate(element => element.getBoundingClientRect().top >= document.querySelector('.app-nav').getBoundingClientRect().bottom))
    mark(`${viewport.width} banner CTA and focus`)

    for (const entry of entries) {
      await page.goto(origin + '/')
      await page.locator(`.home-entry-link[href="${entry.path}"]`).click()
      await page.waitForURL(url => url.pathname === entry.path)
      await page.locator(`#${entry.anchor}`).waitFor()
      assert.equal(await page.locator(`#${entry.anchor}`).evaluate(element => element.open), false)
      await assertActive(entry.title)
      await measure(entry.path)
      mark(`${viewport.width} default entry ${entry.id}`)

      await page.goto(origin + '/')
      const link = page.locator(`.home-feature[data-module="${entry.id}"] .home-feature-action a`)
      await link.scrollIntoViewIfNeeded()
      await settled()
      const previousScroll = await page.evaluate(() => scrollY)
      await link.click()
      await page.waitForURL(url => url.pathname === entry.path && url.hash === '#' + entry.anchor)
      await atAnchor(entry)
      await page.screenshot({ path: resolve(output, `${entry.id}-${viewport.width}.png`) })
      await page.goBack()
      await page.waitForURL(url => url.pathname === '/')
      await settled()
      await page.waitForFunction(value => Math.abs(scrollY - value) < 8, previousScroll)
      await link.click()
      await page.waitForURL(url => url.hash === '#' + entry.anchor)
      await atAnchor(entry)
      mark(`${viewport.width} highlight click, back restoration and re-entry ${entry.id}`)

      await page.reload()
      await atAnchor(entry)
      await page.locator(`#${entry.anchor} summary`).click()
      await page.waitForFunction(id => !document.getElementById(id).open, entry.anchor)
      // Exercise the real router's duplicate-navigation path after collapsing.
      await page.evaluate(async entry => {
        const { router } = await import('/src/app/router.ts')
        await router.push({ path: entry.path, hash: '#' + entry.anchor })
      }, entry)
      await atAnchor(entry)
      mark(`${viewport.width} refresh and repeated hash ${entry.id}`)
    }

    for (const path of ['/module4/jobs', '/module4/jd']) {
      await page.goto(origin + path)
      await page.locator('.career-tabs').waitFor()
      await assertActive('职业规划')
      await measure(path)
    }
    const projects = await (await page.request.get(origin + '/api/v1/practice/projects')).json()
    if (projects.length) {
      await page.goto(origin + '/module3/' + projects[0].id)
      await page.locator('.practice-overview').waitFor()
      await assertActive('实践验证与复盘')
      await measure('practice detail')
    }
    mark(`${viewport.width} nested-route navigation`)

    await page.goto(origin + '/ability')
    await page.locator('.skill-node').first().waitFor()
    await page.locator('.skill-node').first().click()
    await page.locator('.skill-node-summary').waitFor()
    await page.getByRole('button', { name: '切换到分层列表', exact: true }).click()
    await page.locator('.skill-list-item').first().waitFor()
    await page.getByRole('button', { name: '切换到图谱', exact: true }).click()
    await page.locator('.skill-node').first().waitFor()
    await measure('ability graph interactions')
    mark(`${viewport.width} existing ability node and list interactions`)

    await page.goto(origin + '/module4/jobs')
    await page.locator('.career-list-card').first().waitFor()
    const keyword = page.getByPlaceholder('岗位名称、公司或要求')
    await keyword.fill('没有匹配的示范岗位_home_verification')
    await page.getByRole('button', { name: '筛选岗位', exact: true }).click()
    await page.getByText('没有符合条件的岗位。试着减少筛选条件。', { exact: true }).waitFor()
    await keyword.fill('')
    await page.getByRole('button', { name: '筛选岗位', exact: true }).click()
    const job = page.locator('.career-list-card').first()
    await job.waitFor()
    await job.getByRole('button', { name: '查看详情', exact: true }).click()
    await job.locator('.career-job-detail').waitFor()
    await job.getByRole('button', { name: '分析 JD', exact: true }).click()
    await page.waitForURL(url => url.pathname === '/module4/jd' && url.searchParams.has('jobId'))
    await page.waitForFunction(() => (document.querySelector('.career-jd-editor textarea')?.value.trim().length ?? 0) > 20)
    await measure('career filtering and JD import')
    mark(`${viewport.width} existing career filter, empty state, detail and JD import`)
  }

  await page.setViewportSize({ width: 360, height: 800 })
  await page.goto(origin + '/')
  const trigger = page.getByRole('button', { name: '打开导航菜单', exact: true })
  await trigger.focus(); await page.keyboard.press('Enter')
  assert.equal(await page.locator('#app-mobile-menu a').count(), 5)
  await page.keyboard.press('Tab')
  assert.ok(await page.locator('#app-mobile-menu a').first().evaluate(element => element === document.activeElement))
  await page.keyboard.press('Escape')
  assert.equal(await page.locator('#app-mobile-menu').count(), 0)
  assert.ok(await trigger.evaluate(element => element === document.activeElement))
  await trigger.click()
  await page.locator('#app-mobile-menu a[href="/module4/match"]').click()
  await page.waitForURL('**/module4/match')
  assert.equal(await page.locator('#app-mobile-menu').count(), 0)
  mark('mobile menu keyboard, Escape, focus and navigation')

  await page.emulateMedia({ reducedMotion: 'no-preference' })
  await page.goto(origin + '/decision#decision-highlights')
  await atAnchor(entries[1])
  await page.emulateMedia({ reducedMotion: 'reduce' })
  assert.equal(await page.evaluate(() => getComputedStyle(document.documentElement).scrollBehavior), 'auto')
  mark('direct hash with smooth scrolling and reduced motion')

  expectedFault = true
  await page.route('**/api/v1/**', route => route.fulfill({ status: 503, contentType: 'application/json', body: '{"error":{"message":"Simulated offline"}}' }))
  await page.goto(origin + '/')
  await page.locator('.home-entry-link').first().waitFor()
  await settled()
  assert.equal(await page.locator('.home-entry-link').count(), 4)
  for (const entry of entries) {
    await page.goto(origin + entry.path + '#' + entry.anchor)
    await atAnchor(entry)
    assert.ok(await page.locator(`#${entry.anchor} .app-module-intro-body`).isVisible())
  }
  mark('offline homepage and four independent introductions')
  await page.unroute('**/api/v1/**')
  await page.route('**/assets/home/*.jpg', route => route.abort('failed'))
  await page.goto(origin + '/')
  await page.locator('.home-banner-fallback').waitFor()
  assert.equal(await page.locator('.home-entry-link').count(), 4)
  await measure('image failure')
  mark('banner asset failure preserves navigation and layout')
  assert.deepEqual(pageErrors, [])
  assert.deepEqual(consoleErrors, [])
  assert.deepEqual(blockedWrites.filter(request => !calculationPaths.has(new URL(request.url).pathname)), [])
  mark('no page errors, unexpected console errors or backend writes')
} catch (error) {
  await page.screenshot({ path: resolve(output, 'failure.png'), fullPage: true }).catch(() => {})
  throw error
} finally {
  await writeFile(resolve(output, 'acceptance.json'), JSON.stringify({ checks, measurements, pageErrors, consoleErrors, blockedWrites }, null, 2))
  await browser.close()
}
console.log(`Homepage acceptance: ${checks.length} checks passed`)
