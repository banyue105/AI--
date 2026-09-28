<script setup lang="ts">
import { computed, ref } from 'vue'
import { CheckCircle2, Copy, Save } from 'lucide-vue-next'
import { codePoints, dateLabel, useDraft, verdictLabels } from '../utils/drafts'
import type { FeedbackItem, FeedbackRecord, Review } from '../types'
const props = defineProps<{ review: Review; feedback: FeedbackRecord[]; stale: boolean; busy: boolean; save: (items: FeedbackItem[]) => Promise<boolean> }>()
const emit = defineEmits<{ notice: [message: string] }>()
const records = computed(() => props.feedback.filter(item => item.reviewId === props.review.id))
const savedAbility = computed(() => records.value.some(item => item.target === 'ability'))
const savedDecision = computed(() => records.value.some(item => item.target === 'decision'))
function excerpt(value: string, max: number) {
  const characters = Array.from(value.trim())
  return characters.length > max ? `${characters.slice(0, max).join('')}…（节选，详情见复盘快照）` : value.trim()
}
function summary() {
  const input = props.review.input
  const outcomes = input.criteria.map(criterion => {
    const finding = props.review.findings.find(item => item.criterionId === criterion.id)
    const correction = props.review.confirmation?.overrides.find(item => item.criterionId === criterion.id)
    const verdict = correction?.verdict || finding?.verdict
    const action = verdict && verdict !== 'supported'
      ? excerpt(correction && correction.verdict !== finding?.verdict ? `补充：${criterion.expectedEvidence}` : finding?.nextAction || `补充：${criterion.expectedEvidence}`, 180)
      : ''
    return `${excerpt(criterion.title, 80)}：${verdict ? verdictLabels[verdict] : '待检查'}${action ? `；补验建议：${action}` : ''}`
  }).join('；')
  return excerpt(`${input.title}，复盘版本 ${props.review.number}。${outcomes}。成果材料仅用于支持本次验收，不自动调整能力等级。`, 4800)
}
const metricNames: Record<string, string> = { effort: '个人有效投入', duration: '项目持续时间', cost: '资金成本' }
const units: Record<string, string> = { hour: '小时', day: '天', CNY: '元', yuan: '元' }
const bases: Record<string, string> = { effective_work: '有效操作时间', calendar_days: '日历天数', cash: '现金支出', full_cost: '含人工的完整成本' }
const scopes: Record<string, string> = { self: '本人', project: '整个项目' }
const inputSummary = props.review.input.comparisons.map(item => `${metricNames[item.metric]}（${scopes[item.scope] || item.scope}，${bases[item.basis] || item.basis}）：预期${item.expected ? `${item.expected.min}–${item.expected.max}` : '未记录'}，实际${item.actual ?? '未记录'} ${units[item.unit] || item.unit}；${item.reason}`).join('；')
const origin = props.review.input.decisionOrigin
const originSummary = origin ? `原方案：${origin.snapshot.title} / ${origin.snapshot.optionName}（场景 ${origin.scenarioId}，版本 ${origin.versionId}，方案 ${origin.optionKey}）。来源规则估算：工期 ${origin.snapshot.timeRange.min}–${origin.snapshot.timeRange.max} ${units[origin.snapshot.timeRange.unit] || origin.snapshot.timeRange.unit}，成本 ${origin.snapshot.budgetRange.min}–${origin.snapshot.budgetRange.max} ${units[origin.snapshot.budgetRange.unit] || origin.snapshot.budgetRange.unit}；这些数值仅作来源参考，不自动成为个人工时或现金基线。原假设：${excerpt(origin.snapshot.assumptions.join('；'), 700)}。\n` : ''
const initial = { ability: true, decision: true, abilityTargetName: '', abilityNote: summary(), decisionNote: excerpt(`${originSummary}${inputSummary || '本次尚未记录投入。'}\n保存为后续方案参考；原推演与参数保持原值。\n用户过程说明：${props.review.input.processNote}`, 4800) }
const { form, restored, clear } = useDraft(`practice:draft:feedback:${props.review.id}`, initial, value => !!value && typeof value === 'object' && typeof (value as typeof initial).abilityNote === 'string' && typeof (value as typeof initial).decisionNote === 'string')
if (typeof form.value.abilityTargetName !== 'string') form.value.abilityTargetName = ''
const localError = ref('')
const copyFallback = ref('')
async function submit() {
  localError.value = ''
  const items: FeedbackItem[] = []
  const shared = { criterionIds: props.review.input.criteria.map(item => item.id), evidenceIds: props.review.input.evidence.map(item => item.id) }
  const title = Array.from(props.review.input.title).slice(0, 80).join('')
  if (form.value.ability && !savedAbility.value) {
    const targetName = form.value.abilityTargetName.trim()
    if (!targetName || codePoints(targetName) > 120) { localError.value = '请填写 1–120 字的拟关联技能名称。'; return }
    items.push({ ...shared, target: 'ability', targetId: null, targetName, title: `${title} · 实践证据`, note: form.value.abilityNote.trim() })
  }
  if (form.value.decision && !savedDecision.value) items.push({ ...shared, target: 'decision', targetId: origin?.scenarioId || null, targetName: origin?.snapshot.title || null, title: `${title} · 投入参考`, note: form.value.decisionNote.trim() })
  if (!items.length) { localError.value = '请选择尚未保存的反馈类型。'; return }
  if (items.some(item => !item.note || codePoints(item.note) > 5000)) { localError.value = '反馈说明须为 1–5000 字。'; return }
  if (await props.save(items)) clear()
}
async function copy(record: FeedbackRecord) {
  const text = `${record.title}\n${record.targetName ? `拟关联：${record.targetName}\n` : ''}${record.note}\n复盘：${props.review.input.title} / 版本 ${props.review.number}\n状态：已保存参考，尚未写入其他模块`
  try { await navigator.clipboard.writeText(text); emit('notice', '反馈摘要已复制') }
  catch { copyFallback.value = text }
}
</script>

<template>
  <div class="practice-feedback-panel">
    <p v-if="!review.confirmation" class="practice-hint">请先确认当前复盘，再保存反馈。人工修正及证据引用会随确认记录保留。</p>
    <p v-else-if="stale" class="practice-hint">该版本已成为历史输入。已有反馈可以查看；保存新反馈需要重新检查并确认。</p>
    <form v-else-if="!savedAbility || !savedDecision" class="practice-feedback-form" @submit.prevent="submit">
      <p v-if="restored" class="practice-hint">已恢复未提交的反馈草稿。</p>
      <div v-if="!savedAbility" class="practice-feedback-entry"><label class="practice-check"><input v-model="form.ability" type="checkbox" />保存能力证据建议</label><label v-if="form.ability" class="practice-feedback-target">拟关联技能名称（仅供后续核对）<input v-model="form.abilityTargetName" required maxlength="240" placeholder="例如：网络服务部署" /></label><textarea v-model="form.abilityNote" aria-label="能力反馈说明" rows="3" :required="form.ability" /></div>
      <div v-if="!savedDecision" class="practice-feedback-entry"><label class="practice-check"><input v-model="form.decision" type="checkbox" />保存实际投入参考</label><textarea v-model="form.decisionNote" aria-label="决策反馈说明" rows="3" :required="form.decision" /></div>
      <p v-if="localError" class="practice-alert" role="alert">{{ localError }}</p>
      <div class="practice-feedback-footer"><p class="practice-hint">保存于实践模块。能力证据写入与估算更新仍待联调。</p><button type="submit" class="primary-button" :disabled="busy || (!form.ability && !form.decision)"><Save :size="16" /> {{ busy ? '保存中' : '保存反馈参考' }}</button></div>
    </form>
    <div v-if="records.length" class="practice-saved-feedback">
      <article v-for="record in records" :key="record.id"><CheckCircle2 :size="18" /><div><strong>{{ record.title }}</strong><span>已保存参考 · {{ dateLabel(record.confirmedAt) }}<template v-if="record.targetName"> · 拟关联 {{ record.targetName }}</template></span><p>{{ record.note }}</p></div><button type="button" class="secondary-button compact" @click="copy(record)"><Copy :size="15" /> 复制摘要</button></article>
    </div>
    <label v-if="copyFallback" class="practice-copy-fallback">可手动复制摘要<textarea :value="copyFallback" readonly rows="4" @focus="($event.target as HTMLTextAreaElement).select()" /></label>
  </div>
</template>
