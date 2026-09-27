import { defineStore } from 'pinia'
import { ref } from 'vue'
import { decisionService } from '../services/decisionService'
import type {
  BusyAction, DecisionComparison, DecisionParseResult, DecisionScenario,
  DecisionScenarioInput, DecisionSummary, DecisionVersion,
} from '../types'

export const useDecisionStore = defineStore('decision-sandbox', () => {
  const summaries = ref<DecisionSummary[]>([])
  const scenario = ref<DecisionScenario | null>(null)
  const versions = ref<DecisionVersion[]>([])
  const parsed = ref<DecisionParseResult | null>(null)
  const comparison = ref<DecisionComparison | null>(null)
  const busy = ref<BusyAction>(null)
  const error = ref('')
  const notice = ref('')

  async function run<T>(action: Exclude<BusyAction, null>, task: () => Promise<T>): Promise<T | null> {
    busy.value = action
    error.value = ''
    try { return await task() }
    catch (cause) {
      error.value = cause instanceof Error ? cause.message : '操作失败，请稍后重试。'
      return null
    } finally { busy.value = null }
  }

  async function initialize() {
    await run('loading', async () => {
      summaries.value = await decisionService.list()
      if (summaries.value.length) {
        scenario.value = await decisionService.get(summaries.value[0]!.id)
        versions.value = await decisionService.versions(scenario.value.id)
      }
    })
  }

  async function select(id: string) {
    await run('loading', async () => {
      scenario.value = await decisionService.get(id)
      versions.value = await decisionService.versions(id)
      parsed.value = null
      comparison.value = null
    })
  }

  async function save(input: DecisionScenarioInput, isNew = false) {
    return run('saving', async () => {
      scenario.value = isNew || !scenario.value
        ? await decisionService.create(input)
        : await decisionService.update(scenario.value.id, input)
      summaries.value = await decisionService.list()
      if (isNew) versions.value = []
      parsed.value = null
      comparison.value = null
      notice.value = '场景条件已保存。推演后会生成可回溯版本。'
      return scenario.value
    })
  }

  async function parse(input: string) {
    if (!scenario.value) return null
    return run('parsing', async () => {
      parsed.value = await decisionService.parseInput(scenario.value!.id, input)
      return parsed.value
    })
  }

  async function simulate() {
    if (!scenario.value) return null
    return run('simulating', async () => {
      const version = await decisionService.simulate(scenario.value!.id)
      versions.value = await decisionService.versions(scenario.value!.id)
      scenario.value = await decisionService.get(scenario.value!.id)
      summaries.value = await decisionService.list()
      notice.value = `方案已计算并保存为版本 ${version.number}。`
      return version
    })
  }

  async function compare(left: string, right: string) {
    if (!scenario.value) return null
    return run('comparing', async () => {
      comparison.value = await decisionService.compare(scenario.value!.id, left, right)
      return comparison.value
    })
  }

  async function restore(versionId: string) {
    if (!scenario.value) return null
    return run('restoring', async () => {
      scenario.value = await decisionService.restore(scenario.value!.id, versionId)
      parsed.value = null
      comparison.value = null
      notice.value = '已恢复该版本的条件。请重新推演以保存新分支。'
      return scenario.value
    })
  }

  return { summaries, scenario, versions, parsed, comparison, busy, error, notice,
    initialize, select, save, parse, simulate, compare, restore }
})
