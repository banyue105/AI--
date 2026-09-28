<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { Check } from 'lucide-vue-next'
import PracticeDialog from './PracticeDialog.vue'
import { codePoints, verdictLabels } from '../utils/drafts'
import type { Criterion, ManualOverride, Review, Verdict } from '../types'
const props = defineProps<{ criterion: Criterion; review: Review; current: ManualOverride | null }>()
const emit = defineEmits<{ close: []; save: [value: ManualOverride] }>()
const original = props.review.findings.find(item => item.criterionId === props.criterion.id)
const eligibleEvidence = props.review.input.evidence.filter(item => !item.criterionIds.length || item.criterionIds.includes(props.criterion.id))
const initialCitation = props.current?.citations[0] || original?.citations[0]
const form = reactive({ verdict: (props.current?.verdict || original?.verdict || 'insufficient') as Verdict,
  reason: props.current?.reason || '', evidenceId: initialCitation?.evidenceId || eligibleEvidence[0]?.id || '',
  startLine: initialCitation?.startLine || 1, endLine: initialCitation?.endLine || 1 })
const error = ref('')
const evidence = computed(() => props.review.input.evidence.find(item => item.id === form.evidenceId))
const lines = computed(() => evidence.value?.content.split('\n') || [])
const quote = computed(() => lines.value.slice(form.startLine - 1, form.endLine).join('\n'))
function submit() {
  error.value = ''
  if (!form.reason.trim() || codePoints(form.reason) > 2000) { error.value = '请填写 1–2000 字的人工修正理由。'; return }
  const required = form.verdict !== 'insufficient'
  if (required && (!evidence.value || !Number.isInteger(form.startLine) || !Number.isInteger(form.endLine) || form.startLine < 1 || form.endLine < form.startLine || form.endLine > lines.value.length || !quote.value.trim())) {
    error.value = '材料支持或存在矛盾的结论需要选择有效的原文行。'; return
  }
  emit('save', { criterionId: props.criterion.id, verdict: form.verdict, reason: form.reason.trim(), citations: required ? [{
    evidenceId: evidence.value!.id, evidenceRevision: evidence.value!.revision, startLine: form.startLine, endLine: form.endLine, quote: quote.value,
  }] : [] })
  emit('close')
}
</script>

<template>
  <PracticeDialog title="纠正材料判定" :error="error" @close="emit('close')">
    <form class="practice-form" @submit.prevent="submit">
      <p class="practice-hint">{{ criterion.title }} · 原检查结果会保留，修正在确认复盘后保存。</p>
      <label>人工判定<select v-model="form.verdict"><option v-for="(label, value) in verdictLabels" :key="value" :value="value">{{ label }}</option></select></label>
      <label>修正理由<textarea v-model="form.reason" required rows="3" placeholder="说明材料中的哪些内容支持你的判断" /></label>
      <template v-if="form.verdict !== 'insufficient'">
        <label>原始证据<select v-model="form.evidenceId" @change="form.startLine = 1; form.endLine = 1"><option v-if="!eligibleEvidence.length" value="">当前验收项无可引用材料</option><option v-for="item in eligibleEvidence" :key="item.id" :value="item.id">{{ item.title }} · 版本 {{ item.revision }}</option></select></label>
        <div class="practice-metric-form"><label>起始行<input v-model.number="form.startLine" type="number" min="1" :max="lines.length" step="1" /></label><label>结束行<input v-model.number="form.endLine" type="number" min="1" :max="lines.length" step="1" /></label></div>
        <p class="practice-hint">引用从本次复盘快照中生成，共 {{ lines.length }} 行。</p><pre class="practice-quote-preview">{{ quote || '请选择有效的证据行' }}</pre>
      </template>
      <div class="dialog-actions"><button type="button" class="text-button" @click="emit('close')">取消</button><button type="submit" class="primary-button"><Check :size="16" /> 暂存人工修正</button></div>
    </form>
  </PracticeDialog>
</template>
