import assert from 'node:assert/strict'
import { readFile, writeFile } from 'node:fs/promises'
import { join } from 'node:path'

// Run capture, restart the backend with the same file database, then run verify.
const phase = process.argv[2]
assert.ok(['capture', 'verify'].includes(phase), 'Usage: node tests/practice-persistence.mjs capture|verify')
const origin = process.env.PRACTICE_ORIGIN || 'http://127.0.0.1:5173'
const visualDir = process.env.PRACTICE_VISUAL_DIR || './test-results/practice-e2e'
const report = JSON.parse(await readFile(join(visualDir, 'acceptance.json'), 'utf8'))
const base = `${origin}/api/v1/practice/projects/${report.projectId}`
async function get(url) {
  const response = await fetch(url)
  assert.equal(response.status, 200, `Cannot read ${url}`)
  return response.json()
}
const current = {
  project: await get(base), history: await get(`${base}/reviews`),
  reviews: await Promise.all(report.reviewIds.map(id => get(`${base}/reviews/${id}`))),
  feedback: await get(`${base}/feedback`),
}
assert.equal(current.history.length, 3)
assert.equal(current.feedback.length, 2)
assert.equal(current.reviews.at(-1).confirmation.overrides.length, 1)
const path = join(visualDir, 'persistence-snapshot.json')
if (phase === 'capture') {
  await writeFile(path, JSON.stringify(current, null, 2))
  console.log('已保存重启前快照：项目、4 份材料、3 次复盘、人工确认、2 条反馈。')
} else {
  const saved = JSON.parse(await readFile(path, 'utf8'))
  assert.deepEqual(current, saved)
  const result = { projectId: report.projectId, evidence: current.project.evidence.length,
    reviews: current.reviews.length, feedback: current.feedback.length, allDataEqual: true, verifiedAt: new Date().toISOString() }
  await writeFile(join(visualDir, 'persistence-result.json'), JSON.stringify(result, null, 2))
  console.log(JSON.stringify(result, null, 2))
}
