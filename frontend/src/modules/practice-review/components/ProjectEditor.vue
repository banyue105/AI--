<script setup lang="ts">
import { ref } from 'vue'
import { Plus, Save, Trash2 } from 'lucide-vue-next'
import PracticeDialog from './PracticeDialog.vue'
import { codePoints, useDraft } from '../utils/drafts'
import type { InputMetric, PracticeProject, PracticeTemplate, ProjectDraft } from '../types'

const props = defineProps<{ project: PracticeProject | null; templates: PracticeTemplate[]; create: boolean; busy: boolean; error: string; save: (draft: ProjectDraft) => Promise<boolean> }>()
const emit = defineEmits<{ close: [] }>()
interface MetricRow { metric: InputMetric['metric']; scope: string; basis: string; min: string; max: string; actual: string; baselineTiming: InputMetric['baselineTiming']; baselineRecordedAt: string | null }
const initial = {
  title: props.create ? '' : props.project?.title || '', goal: props.create ? '' : props.project?.goal || '',
  templateId: props.project?.templateId || props.templates[0]?.id || 'network-service-v1',
  processNote: props.create ? '' : props.project?.processNote || '',
  rows: (props.create ? [] : props.project?.metrics || []).map(item => ({ metric: item.metric, scope: item.scope, basis: item.basis,
    min: item.expected?.min.toString() || '', max: item.expected?.max.toString() || '', actual: item.actual?.toString() || '',
    baselineTiming: item.baselineTiming, baselineRecordedAt: item.baselineRecordedAt })),
}
const { form, restored, clear } = useDraft(`practice:draft:project:${props.create ? 'new' : props.project?.id}`, initial,
  value => !!value && typeof value === 'object' && typeof (value as typeof initial).title === 'string' && typeof (value as typeof initial).goal === 'string' && Array.isArray((value as typeof initial).rows))
const localError = ref('')
const metricNames = { effort: '个人有效投入', duration: '项目日历持续时间', cost: '资金成本' }
const scopeNames: Record<string, string> = { self: '本人', project: '整个项目' }
function changeMetric(row: MetricRow) {
  row.scope = row.metric === 'effort' ? 'self' : 'project'
  row.basis = row.metric === 'effort' ? 'effective_work' : row.metric === 'duration' ? 'calendar_days' : 'cash'
  row.min = ''; row.max = ''; row.actual = ''; row.baselineRecordedAt = null
}
function addMetric() {
  form.value.rows.push({ metric: 'effort', scope: 'self', basis: 'effective_work', min: '', max: '', actual: '', baselineTiming: 'retrospective', baselineRecordedAt: null })
}
async function submit() {
  localError.value = ''
  if (codePoints(form.value.title.trim()) > 120 || codePoints(form.value.goal.trim()) > 1000 || codePoints(form.value.processNote) > 5000) { localError.value = '项目名称最多 120 字，目标最多 1000 字，过程说明最多 5000 字。'; return }
  const metrics: InputMetric[] = []
  for (const row of form.value.rows) {
    if ((row.min === '') !== (row.max === '')) { localError.value = '请同时填写预期区间上下限；固定值填写相同数字。'; return }
    const expected = row.min === '' ? null : { min: Number(row.min), max: Number(row.max) }
    const actual = row.actual === '' ? null : Number(row.actual)
    const old = props.project?.metrics.find(item => item.metric === row.metric && item.scope === row.scope && item.basis === row.basis)
    const unchanged = expected && old?.expected?.min === expected.min && old.expected.max === expected.max && old.baselineTiming === row.baselineTiming
    metrics.push({ metric: row.metric, unit: row.metric === 'effort' ? 'hour' : row.metric === 'duration' ? 'day' : 'CNY', scope: row.scope, basis: row.basis,
      expected, actual, baselineTiming: row.baselineTiming, baselineRecordedAt: expected ? (unchanged ? old!.baselineRecordedAt : new Date().toISOString()) : null })
  }
  const saved = await props.save({ title: form.value.title.trim(), goal: form.value.goal.trim(), templateId: form.value.templateId, metrics, processNote: form.value.processNote })
  if (saved) { clear(); emit('close') }
}
</script>

<template>
  <PracticeDialog :title="create ? '新建实践' : '编辑目标与投入'" :busy="busy" :error="error || localError" @close="emit('close')">
    <form class="practice-form" @submit.prevent="submit">
      <p v-if="restored" class="practice-hint">已恢复未提交草稿；提交前请核对最新记录。</p>
      <label>项目名称<input v-model="form.title" required data-autofocus placeholder="例如：我的网络服务部署实践" /></label>
      <label>实践目标<textarea v-model="form.goal" required rows="3" placeholder="说明最终需要完成并验证的结果" /></label>
      <label v-if="create">验收模板<select v-model="form.templateId"><option v-for="item in templates" :key="item.id" :value="item.id">{{ item.title }}</option></select></label>
      <template v-else>
        <div class="practice-inline-heading"><h3>预期与实际投入</h3><button type="button" class="text-button" :disabled="form.rows.length >= 12" @click="addMetric"><Plus :size="16" /> 添加记录</button></div>
        <p class="practice-hint">只比较相同指标、范围和口径。预期或实际可留空；零是有效值。补填预期请选择“事后回忆”。</p>
        <fieldset v-for="(row, index) in form.rows" :key="index" class="practice-metric-form">
          <legend>投入记录 {{ index + 1 }}</legend>
          <label>指标<select v-model="row.metric" @change="changeMetric(row)"><option v-for="(label, value) in metricNames" :key="value" :value="value">{{ label }}</option></select></label>
          <label>范围<select v-model="row.scope"><option v-for="(label, value) in scopeNames" :key="value" :value="value">{{ label }}</option></select></label>
          <label class="practice-span-2">投入口径<select v-model="row.basis"><template v-if="row.metric === 'effort'"><option value="effective_work">有效操作小时（不含等待）</option></template><template v-else-if="row.metric === 'duration'"><option value="calendar_days">从开始到完成的日历天数</option></template><template v-else><option value="cash">现金支出（不含人工估值）</option><option value="full_cost">完整成本（包含人工）</option></template></select></label>
          <label>预期下限<input v-model="row.min" type="number" min="0" step="any" :aria-label="`记录 ${index + 1} 预期下限`" placeholder="可留空" /></label>
          <label>预期上限<input v-model="row.max" type="number" min="0" step="any" :aria-label="`记录 ${index + 1} 预期上限`" placeholder="可留空" /></label>
          <label>实际投入<input v-model="row.actual" type="number" min="0" step="any" :aria-label="`记录 ${index + 1} 实际投入`" placeholder="可留空" /></label>
          <label>基线记录时机<select v-model="row.baselineTiming"><option value="before_practice">实践前记录</option><option value="retrospective">事后回忆</option></select></label>
          <button type="button" class="text-button practice-span-2" @click="form.rows.splice(index, 1)"><Trash2 :size="15" /> 移除此投入记录</button>
        </fieldset>
        <label>过程与原因说明<textarea v-model="form.processNote" rows="3" placeholder="记录实际遇到的问题；这是你的说明，不作为自动推断的原因" /></label>
      </template>
      <div class="dialog-actions"><button type="button" class="text-button" :disabled="busy" @click="emit('close')">取消</button><button type="submit" class="primary-button" :disabled="busy"><Save :size="16" /> {{ busy ? '保存中' : create ? '创建实践' : '保存目标与投入' }}</button></div>
    </form>
  </PracticeDialog>
</template>
