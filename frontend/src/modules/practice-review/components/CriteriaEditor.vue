<script setup lang="ts">
import { computed, ref } from 'vue'
import { Plus, Save, Trash2 } from 'lucide-vue-next'
import PracticeDialog from './PracticeDialog.vue'
import { codePoints, useDraft } from '../utils/drafts'
import type { CriterionInput, PracticeProject, Suggestions } from '../types'
const props = defineProps<{ project: PracticeProject; suggestions: Suggestions | null; busy: boolean; error: string; save: (criteria: CriterionInput[]) => Promise<boolean> }>()
const emit = defineEmits<{ close: [] }>()
interface Row extends CriterionInput { selected: boolean }
const rows: Row[] = props.suggestions ? props.suggestions.candidates.map(item => ({ ...item,
  id: props.project.criteria.find(existing => existing.title === item.title)?.id || null, selected: true }))
  : props.project.criteria.map(item => ({ id: item.id, title: item.title, standard: item.standard, expectedEvidence: item.expectedEvidence, required: item.required, selected: true }))
if (props.suggestions) for (const item of props.project.criteria) if (!rows.some(row => row.id === item.id)) rows.push({ ...item, selected: false })
const { form, restored, clear } = useDraft(`practice:draft:criteria:${props.project.id}:${props.suggestions ? 'suggestions' : 'edit'}`, { rows },
  value => !!value && typeof value === 'object' && Array.isArray((value as { rows: Row[] }).rows))
const localError = ref('')
const selectedCount = computed(() => form.value.rows.filter(item => item.selected).length)
async function submit() {
  localError.value = ''
  const selected = form.value.rows.filter(item => item.selected)
  if (selected.length < 1 || selected.length > 12) { localError.value = '请选择 1–12 项验收标准。'; return }
  if (selected.some(item => codePoints(item.title) > 120 || codePoints(item.standard) > 1000 || codePoints(item.expectedEvidence) > 1000)) { localError.value = '名称最多 120 字，标准与材料要求各最多 1000 字。'; return }
  const input = selected.map(item => ({ id: props.project.criteria.some(existing => existing.id === item.id) ? item.id : null,
    title: item.title.trim(), standard: item.standard.trim(), expectedEvidence: item.expectedEvidence.trim(), required: item.required }))
  if (await props.save(input)) { clear(); emit('close') }
}
</script>

<template>
  <PracticeDialog :title="suggestions ? '确认候选验收标准' : '编辑验收清单'" :busy="busy" :error="error || localError" @close="emit('close')">
    <form class="practice-form" @submit.prevent="submit">
      <p v-if="restored" class="practice-hint">已恢复未提交的验收清单草稿。</p>
      <p class="practice-hint">已选择 {{ selectedCount }} 项。保存后使用勾选的完整清单；未选择的旧项从当前清单移除，历史记录保留。</p>
      <fieldset v-for="(item, index) in form.rows" :key="index" class="practice-criterion-form">
        <legend><label class="practice-check"><input v-model="item.selected" type="checkbox" />验收项 {{ index + 1 }}</label></legend>
        <label>验收名称<input v-model="item.title" :required="item.selected" :aria-label="`验收名称 ${index + 1}`" /></label>
        <label>可观察的验收标准<textarea v-model="item.standard" :required="item.selected" rows="2" :aria-label="`验收标准 ${index + 1}`" /></label>
        <label>需要提交的材料<textarea v-model="item.expectedEvidence" :required="item.selected" rows="2" :aria-label="`材料要求 ${index + 1}`" /></label>
        <div class="practice-inline-heading"><label class="practice-check"><input v-model="item.required" type="checkbox" />必需验收项</label><button type="button" class="text-button" @click="form.rows.splice(index, 1)"><Trash2 :size="15" /> 移除</button></div>
      </fieldset>
      <button type="button" class="secondary-button" :disabled="form.rows.length >= 12" @click="form.rows.push({ id: null, title: '', standard: '', expectedEvidence: '', required: true, selected: true })"><Plus :size="16" /> 添加验收项</button>
      <div class="dialog-actions"><button type="button" class="text-button" :disabled="busy" @click="emit('close')">取消</button><button type="submit" class="primary-button" :disabled="busy || !selectedCount || selectedCount > 12"><Save :size="16" /> {{ busy ? '保存中' : '确认并保存清单' }}</button></div>
    </form>
  </PracticeDialog>
</template>
