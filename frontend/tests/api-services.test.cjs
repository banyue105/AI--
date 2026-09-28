const { test, after } = require('node:test')
const assert = require('node:assert/strict')
const { execFileSync } = require('node:child_process')
const { mkdtempSync, rmSync, writeFileSync } = require('node:fs')
const path = require('node:path')

const frontendRoot = path.resolve(__dirname, '..')
const outputRoot = mkdtempSync(path.join(frontendRoot, '.service-tests-'))
writeFileSync(path.join(outputRoot, 'package.json'), '{"type":"commonjs"}')
execFileSync(process.execPath, [
  path.join(frontendRoot, 'node_modules/typescript/bin/tsc'),
  '--target', 'ES2022', '--module', 'commonjs', '--moduleResolution', 'node',
  '--lib', 'ES2022,DOM', '--strict', '--skipLibCheck',
  '--rootDir', path.join(frontendRoot, 'src'), '--outDir', outputRoot,
  path.join(frontendRoot, 'src/core/api/apiClient.ts'),
  path.join(frontendRoot, 'src/core/api/homeService.ts'),
], { cwd: frontendRoot, stdio: 'pipe' })

const originalFetch = globalThis.fetch
after(() => {
  globalThis.fetch = originalFetch
  const resolved = path.resolve(outputRoot)
  if (path.dirname(resolved) !== frontendRoot || !path.basename(resolved).startsWith('.service-tests-')) {
    throw new Error('Unexpected temporary test directory')
  }
  rmSync(resolved, { recursive: true, force: true })
})

function freshServices() {
  for (const key of Object.keys(require.cache)) {
    if (key.startsWith(outputRoot + path.sep)) delete require.cache[key]
  }
  return {
    ...require(path.join(outputRoot, 'core/api/apiClient.js')),
    ...require(path.join(outputRoot, 'core/api/homeService.js')),
  }
}

function jsonResponse(data, status = 200) {
  return new Response(JSON.stringify(data), { status, headers: { 'Content-Type': 'application/json' } })
}

const homeData = {
  profile: { id: 'demo-user', name: '测试用户', goals: ['完成项目'] },
  currentGoal: '完成项目', recentActivity: '已保存', modules: [],
}

test('home uses a valid backend response', async () => {
  const { homeService } = freshServices()
  globalThis.fetch = async () => jsonResponse(homeData)
  assert.deepEqual(await homeService.getHome(), homeData)
})

test('first offline visit can show the local home demo', async () => {
  const { homeService } = freshServices()
  globalThis.fetch = async () => { throw new TypeError('network unavailable') }
  assert.equal((await homeService.getHome()).profile.name, '林澈')
})

test('HTML from an unrelated local service is treated as unavailable', async () => {
  const { homeService } = freshServices()
  globalThis.fetch = async () => new Response('<html>printer service</html>', {
    status: 200, headers: { 'Content-Type': 'text/html' },
  })
  assert.equal((await homeService.getHome()).profile.name, '林澈')
})

test('structured backend errors are shown instead of an offline demo', async () => {
  const { homeService, ApiHttpError } = freshServices()
  globalThis.fetch = async () => jsonResponse({ error: { message: '数据库不可用' } }, 500)
  await assert.rejects(homeService.getHome(), (error) =>
    error instanceof ApiHttpError && error.status === 500 && error.message === '数据库不可用')
})

test('a disconnect after a valid response does not turn into offline demo', async () => {
  const { homeService, ApiUnavailableError } = freshServices()
  globalThis.fetch = async () => jsonResponse(homeData)
  await homeService.getHome()
  globalThis.fetch = async () => { throw new TypeError('connection lost') }
  await assert.rejects(homeService.getHome(), ApiUnavailableError)
})
