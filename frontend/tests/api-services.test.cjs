const { test, after } = require('node:test')
const assert = require('node:assert/strict')
const { execFileSync } = require('node:child_process')
const { mkdtempSync, rmSync, writeFileSync } = require('node:fs')
const path = require('node:path')

// Compile the actual services with the project's existing TypeScript dependency.
// No test runtime or additional npm dependency is required.
const frontendRoot = path.resolve(__dirname, '..')
const outputRoot = mkdtempSync(path.join(frontendRoot, '.service-tests-'))
writeFileSync(path.join(outputRoot, 'package.json'), '{"type":"commonjs"}')
execFileSync(process.execPath, [
  path.join(frontendRoot, 'node_modules/typescript/bin/tsc'),
  '--target', 'ES2022', '--module', 'commonjs', '--moduleResolution', 'node',
  '--lib', 'ES2022,DOM', '--strict', '--skipLibCheck',
  '--rootDir', path.join(frontendRoot, 'src'), '--outDir', outputRoot,
  path.join(frontendRoot, 'src/modules/ability-growth/services/abilityService.ts'),
  path.join(frontendRoot, 'src/modules/ability-growth/services/knowledgeCatalogService.ts'),
  path.join(frontendRoot, 'src/core/api/homeService.ts'),
  path.join(frontendRoot, 'src/modules/ability-growth/stores/abilityStore.ts'),
], { cwd: frontendRoot, stdio: 'pipe' })

const originalFetch = globalThis.fetch
const originalStorage = globalThis.localStorage
after(() => {
  globalThis.fetch = originalFetch
  if (originalStorage === undefined) delete globalThis.localStorage
  else globalThis.localStorage = originalStorage
  const resolved = path.resolve(outputRoot)
  if (path.dirname(resolved) !== frontendRoot || !path.basename(resolved).startsWith('.service-tests-')) {
    throw new Error('Unexpected temporary test directory')
  }
  rmSync(resolved, { recursive: true, force: true })
})

const storageKey = 'ican:ability-graph:v2'
const sampleNode = { id: 'java', name: 'Java', description: 'Java 21', level: 2, status: 'mastered', evidenceIds: [], x: 20, y: 30 }
function serverGraph(overrides = {}) {
  return {
    nodes: [sampleNode], relations: [], evidence: [], source: 'api',
    updatedAt: '2026-09-16T00:00:00Z', goal: { title: '服务开发', deadline: '2026-11-13' },
    ...overrides,
  }
}
function freshServices() {
  for (const key of Object.keys(require.cache)) {
    if (key.startsWith(outputRoot + path.sep)) delete require.cache[key]
  }
  const storage = new Map()
  globalThis.localStorage = {
    getItem: (key) => storage.get(key) ?? null,
    setItem: (key, value) => storage.set(key, String(value)),
  }
  return {
    ...require(path.join(outputRoot, 'modules/ability-growth/services/abilityService.js')),
    ...require(path.join(outputRoot, 'modules/ability-growth/services/knowledgeCatalogService.js')),
    ...require(path.join(outputRoot, 'core/api/apiClient.js')),
    ...require(path.join(outputRoot, 'modules/ability-growth/stores/abilityStore.js')),
    storage,
  }
}
function jsonResponse(data, status = 200) {
  return new Response(JSON.stringify(data), { status, headers: { 'Content-Type': 'application/json' } })
}

test('successful node save uses and caches the entire server graph', async () => {
  const { abilityService, storage } = freshServices()
  storage.set(storageKey, JSON.stringify(serverGraph({ source: 'mock', nodes: [] })))
  const expected = serverGraph({ nodes: [{ ...sampleNode, id: 'server-java', x: 96 }], updatedAt: 'server-revision' })
  globalThis.fetch = async (url, init) => {
    assert.equal(url, '/api/v1/ability/skills')
    assert.equal(init.method, 'POST')
    return jsonResponse(expected)
  }
  assert.deepEqual(await abilityService.saveNode(sampleNode), expected)
  assert.deepEqual(JSON.parse(storage.get(storageKey)), expected)
})

test('HTTP 400 is displayed as a validation failure without a local write', async () => {
  const { abilityService, ApiHttpError, storage } = freshServices()
  globalThis.fetch = async () => jsonResponse({ error: { code: 'VALIDATION_ERROR', message: '能力名称不能为空', details: [] } }, 400)
  await assert.rejects(abilityService.saveNode(sampleNode), (error) => error instanceof ApiHttpError && error.status === 400 && error.message === '能力名称不能为空')
  assert.equal(storage.size, 0)
})

test('structured HTTP 500 never becomes a successful demo write', async () => {
  const { abilityService, ApiHttpError, storage } = freshServices()
  globalThis.fetch = async () => jsonResponse({ error: { code: 'INTERNAL_ERROR', message: '数据库不可用' } }, 500)
  await assert.rejects(abilityService.saveNode(sampleNode), ApiHttpError)
  assert.equal(storage.size, 0)
})

test('a network write failure after server load preserves the server cache and rejects', async () => {
  const { abilityService, ApiUnavailableError, storage } = freshServices()
  const expected = serverGraph()
  globalThis.fetch = async () => jsonResponse(expected)
  assert.deepEqual(await abilityService.getGraph(), expected)
  globalThis.fetch = async () => { throw new TypeError('fetch failed') }
  await assert.rejects(abilityService.saveNode({ ...sampleNode, level: 4 }), ApiUnavailableError)
  assert.deepEqual(JSON.parse(storage.get(storageKey)), expected)
})

test('an API snapshot from a previous session cannot be edited as a local demo', async () => {
  const { abilityService, ApiUnavailableError, storage } = freshServices()
  const expected = serverGraph()
  storage.set(storageKey, JSON.stringify(expected))
  globalThis.fetch = async () => { throw new TypeError('fetch failed') }
  await assert.rejects(abilityService.getGraph(), ApiUnavailableError)
  await assert.rejects(abilityService.saveNode(sampleNode), ApiUnavailableError)
  assert.deepEqual(JSON.parse(storage.get(storageKey)), expected)
})

test('first offline visit retains usable local graph, parsing, paths and node saves', async () => {
  const { abilityService, knowledgeCatalogService, storage } = freshServices()
  globalThis.fetch = async () => { throw new TypeError('fetch failed') }
  const graph = await abilityService.getGraph()
  assert.equal(graph.source, 'mock')
  assert.ok(graph.nodes.length > 0)
  const saved = await abilityService.saveNode(sampleNode)
  assert.equal(saved.source, 'mock')
  assert.ok(saved.nodes.some((node) => node.id === 'java'))
  assert.deepEqual(JSON.parse(storage.get(storageKey)), saved)
  assert.ok((await abilityService.parseInput('我会 Python')).suggestedNodes.length > 0)
  assert.ok((await abilityService.generatePath(saved)).length > 0)
  assert.ok((await knowledgeCatalogService.getCatalog()).length > 0)
  assert.equal((await knowledgeCatalogService.generateTrack('数据分析', saved.nodes)).source, 'mock')
})

test('empty local proxy 500 supports first-visit demo but rejects writes after a server response', async () => {
  const { abilityService, ApiUnavailableError } = freshServices()
  globalThis.fetch = async () => new Response('', { status: 500 })
  assert.equal((await abilityService.getGraph()).source, 'mock')
  globalThis.fetch = async () => jsonResponse(serverGraph())
  await abilityService.getGraph()
  globalThis.fetch = async () => new Response('', { status: 500 })
  await assert.rejects(abilityService.saveNode(sampleNode), ApiUnavailableError)
})

test('saving a suggested relation posts it and uses the returned graph', async () => {
  const { abilityService } = freshServices()
  const relation = { from: 'java', to: 'spring', type: 'prerequisite', confidence: 0.9 }
  const expected = serverGraph({ relations: [relation] })
  globalThis.fetch = async (url, init) => {
    assert.equal(url, '/api/v1/ability/relations')
    assert.equal(init.method, 'POST')
    assert.deepEqual(JSON.parse(init.body), relation)
    return jsonResponse(expected)
  }
  assert.deepEqual(await abilityService.addRelation(relation), expected)
})

test('knowledge generation preserves the backend declared mock source', async () => {
  const { knowledgeCatalogService } = freshServices()
  const track = {
    id: 'data', title: '数据分析', shortTitle: '数据', description: '本地规则生成候选',
    outcome: '完成实践', source: 'mock', stages: [{ id: 'base', title: '基础', description: '基础能力', items: [] }],
  }
  globalThis.fetch = async () => jsonResponse(track)
  assert.deepEqual(await knowledgeCatalogService.generateTrack('数据分析', []), track)
})

test('store keeps the graph and reports a failed write without a success notice', async () => {
  const { useAbilityStore } = freshServices()
  const { createPinia, setActivePinia } = require(path.join(frontendRoot, 'node_modules/pinia'))
  setActivePinia(createPinia())
  const store = useAbilityStore()
  globalThis.fetch = async (url) => jsonResponse(url.endsWith('/path') ? [] : serverGraph())
  await store.load()
  globalThis.fetch = async () => { throw new TypeError('fetch failed') }
  assert.equal(await store.saveNode({ ...sampleNode, level: 4 }), false)
  assert.equal(store.graph.nodes[0].level, 2)
  assert.match(store.error, /未保存/)
  assert.equal(store.notice, '')
  assert.equal(store.saving, false)
})

test('accepting suggestions translates temporary node IDs before persisting their relations', async () => {
  const { useAbilityStore } = freshServices()
  const { createPinia, setActivePinia } = require(path.join(frontendRoot, 'node_modules/pinia'))
  setActivePinia(createPinia())
  const store = useAbilityStore()
  let current = serverGraph({ nodes: [] })
  let savedRelation
  globalThis.fetch = async (url, init) => {
    if (url.endsWith('/path')) return jsonResponse([])
    if (url.endsWith('/skills')) {
      const submitted = JSON.parse(init.body)
      current = { ...current, nodes: [...current.nodes, { ...submitted, id: `server-${submitted.id}` }] }
    }
    if (url.endsWith('/relations')) {
      savedRelation = JSON.parse(init.body)
      current = { ...current, relations: [savedRelation] }
    }
    return jsonResponse(current)
  }
  await store.load()
  store.parseResult = {
    summary: '候选能力', assumptions: [],
    suggestedNodes: [{ ...sampleNode, id: 'one', name: 'Java' }, { ...sampleNode, id: 'two', name: 'Spring Boot' }],
    suggestedRelations: [{ from: 'one', to: 'two', type: 'prerequisite', confidence: 0.9 }],
  }
  await store.acceptSuggestions()
  assert.deepEqual(savedRelation, { from: 'server-one', to: 'server-two', type: 'prerequisite', confidence: 0.9 })
  assert.deepEqual(store.graph.relations[0], savedRelation)
  assert.equal(store.parseResult, null)
  assert.equal(store.error, '')
  assert.match(store.notice, /已保存到后端/)
})
