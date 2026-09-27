import assert from 'node:assert/strict'
import { mkdir } from 'node:fs/promises'
import { join } from 'node:path'
import { chromium } from 'playwright-core'

const chromePath = process.env.CHROME_PATH || 'C:/Program Files/Google/Chrome/Application/chrome.exe'
const origin = process.env.DECISION_ORIGIN || 'http://127.0.0.1:5173'
const visualDir = process.env.DECISION_VISUAL_DIR || './test-results/decision-e2e'
await mkdir(visualDir, { recursive: true })

const browser = await chromium.launch({ executablePath: chromePath, headless: true })
const page = await browser.newPage({ viewport: { width: 1280, height: 720 } })
const pageErrors = []
page.on('pageerror', error => pageErrors.push(error.message))

try {
  await page.goto(`${origin}/decision`, { waitUntil: 'networkidle' })
  await page.locator('.decision-overview h1').waitFor()
  await page.getByRole('button', { name: '新建场景' }).click()
  await page.getByLabel('场景名称').fill('验收案例：60 天 AI 网页项目')
  await page.getByLabel('目标').fill('在现有资源内交付可演示的 AI 网页项目')
  await page.getByLabel('参与人数').fill('3')
  await page.getByLabel('预算（元）').fill('100000')
  await page.getByLabel('期限（天）').fill('60')
  await page.getByRole('button', { name: '添加资源' }).click()
  await page.getByLabel('资源名称').fill('已有视觉开发技能')
  await page.getByRole('button', { name: '保存条件' }).click()
  await page.getByRole('dialog').waitFor({ state: 'hidden', timeout: 10000 }).catch(async error => {
    console.log('Save diagnostics:', await page.locator('.decision-error,.decision-alert').allTextContents())
    console.log('Invalid fields:', await page.locator('form[role=dialog] :invalid').evaluateAll(items => items.map(item => ({ html: item.outerHTML, message: item.validationMessage }))))
    console.log('Page errors:', pageErrors)
    console.log('Scene headings:', await page.locator('h1').allTextContents())
    throw error
  })
  await page.getByRole('heading', { name: '验收案例：60 天 AI 网页项目' }).waitFor()
  await page.getByLabel('自然语言条件').fill('3 人、10 万元、60 天、已有服务器，增加视觉识别功能')
  await page.getByRole('button', { name: '结构化条件' }).click()
  await page.getByText('提取到 5 项候选条件').waitFor()
  await page.getByRole('button', { name: '确认并推演' }).click()
  await page.getByText('版本 2', { exact: true }).first().waitFor()
  await page.getByRole('button', { name: '编辑条件' }).click()
  await page.getByLabel('期限（天）').fill('40')
  await page.getByLabel('预算（元）').fill('55000')
  await page.getByRole('button', { name: '保存条件' }).click()
  await page.getByText('版本 3', { exact: true }).first().waitFor()
  await page.getByRole('button', { name: '比较两个版本' }).click()
  await page.getByText('期限 60 → 40 天').waitFor()
  assert.equal(await page.getByText('高风险', { exact: true }).count() > 0, true)
  await page.evaluate(() => window.scrollTo(0, 0))
  await page.screenshot({ path: join(visualDir, 'decision-1280x720.png') })
  const desktopOverflow = await page.evaluate(() => document.documentElement.scrollWidth - window.innerWidth)
  assert.ok(desktopOverflow <= 0, `desktop horizontal overflow: ${desktopOverflow}px`)

  await page.setViewportSize({ width: 360, height: 800 })
  await page.reload({ waitUntil: 'networkidle' })
  await page.getByRole('heading', { name: '验收案例：60 天 AI 网页项目' }).waitFor()
  await page.screenshot({ path: join(visualDir, 'decision-360x800.png') })
  const mobileOverflow = await page.evaluate(() => document.documentElement.scrollWidth - window.innerWidth)
  assert.ok(mobileOverflow <= 0, `mobile horizontal overflow: ${mobileOverflow}px`)
  await page.getByRole('button', { name: '编辑条件' }).focus()
  await page.keyboard.press('Enter')
  await page.getByRole('dialog').waitFor()
  await page.keyboard.press('Escape')
  await page.getByRole('dialog').waitFor({ state: 'hidden' })
  await page.route('**/api/v1/decisions/*/simulate', route => route.fulfill({ status: 422,
    contentType: 'application/json', body: JSON.stringify({ error: { code: 'CALCULATION_FAILED', message: '方案计算失败，请核对条件后重试。' } }) }))
  await page.getByRole('button', { name: '重新计算并保存版本' }).click()
  await page.getByRole('alert').getByText('方案计算失败，请核对条件后重试。').waitFor()
  await page.unroute('**/api/v1/decisions/*/simulate')

  const emptyPage = await browser.newPage({ viewport: { width: 360, height: 800 } })
  await emptyPage.route('**/api/v1/decisions', route => route.fulfill({ status: 200, contentType: 'application/json', body: '[]' }))
  await emptyPage.goto(`${origin}/decision`, { waitUntil: 'networkidle' })
  await emptyPage.getByText('还没有决策场景').waitFor()
  await emptyPage.close()

  page.once('dialog', dialog => dialog.accept())
  await page.locator('.decision-version-card').nth(1).getByRole('button', { name: '恢复条件' }).click()
  await page.getByText('当前条件尚未推演').waitFor()
  await page.getByRole('button', { name: '重新计算并保存版本' }).click()
  await page.getByText('版本 4', { exact: true }).first().waitFor()
  assert.deepEqual(pageErrors, [])
  console.log(JSON.stringify({ desktopOverflow, mobileOverflow, versions: 4, parsedCandidates: 5,
    screenshots: [join(visualDir, 'decision-1280x720.png'), join(visualDir, 'decision-360x800.png')] }, null, 2))
} finally {
  await browser.close()
}
