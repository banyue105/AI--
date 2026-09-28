<script setup lang="ts">
import { computed, ref } from 'vue'
import { Save } from 'lucide-vue-next'
import PracticeDialog from './PracticeDialog.vue'
import { codePoints, useDraft } from '../utils/drafts'
import type { Evidence, EvidenceDraft, PracticeProject } from '../types'
const props = defineProps<{ project: PracticeProject; evidence: Evidence | null; busy: boolean; error: string; save: (draft: EvidenceDraft) => Promise<boolean> }>()
const emit = defineEmits<{ close: [] }>()
const initial: EvidenceDraft = { id: props.evidence?.id || null, title: props.evidence?.title || '', kind: props.evidence?.kind || 'note', content: props.evidence?.content || '', criterionIds: [...(props.evidence?.criterionIds || [])] }
const { form, restored, clear } = useDraft(`practice:draft:evidence:${props.project.id}:${props.evidence?.id || 'new'}`, initial,
  value => !!value && typeof value === 'object' && typeof (value as EvidenceDraft).content === 'string' && typeof (value as EvidenceDraft).title === 'string' && Array.isArray((value as EvidenceDraft).criterionIds))
form.value.criterionIds = form.value.criterionIds.filter(id => props.project.criteria.some(item => item.id === id))
const localError = ref('')
const count = computed(() => codePoints(form.value.content))
async function submit() {
  localError.value = ''
  if (count.value > 12000 || codePoints(form.value.title.trim()) > 120) { localError.value = '证据标题最多 120 字，正文最多 12000 字。'; return }
  if (await props.save({ ...form.value, id: props.evidence?.id || null, title: form.value.title.trim(), content: form.value.content.replace(/\r\n?/g, '\n') })) { clear(); emit('close') }
}
</script>

<template>
  <PracticeDialog :title="evidence ? '编辑证据' : '添加成果证据'" :busy="busy" :error="error || localError" @close="emit('close')">
    <form class="practice-form" @submit.prevent="submit">
      <p v-if="restored" class="practice-hint">已恢复未提交的材料草稿；仍需保存到后端。</p>
      <label>证据名称<input v-model="form.title" required data-autofocus placeholder="例如：HTTPS 访问结果" /></label>
      <label>材料类型<select v-model="form.kind"><option value="note">说明 / 部署文档</option><option value="config">配置片段</option><option value="log">运行 / 访问日志</option></select></label>
      <label>材料正文<textarea v-model="form.content" class="practice-code-input" required rows="9" spellcheck="false" placeholder="粘贴原始材料。链接只作为备注，本次检查不读取外部地址。" /></label>
      <p class="practice-hint">{{ count }} / 12000 字符 · 配置和日志作为文本检查。</p>
      <fieldset class="practice-checkboxes"><legend>关联验收项（可多选）</legend><label v-for="item in project.criteria" :key="item.id"><input v-model="form.criterionIds" type="checkbox" :value="item.id" />{{ item.title }}</label></fieldset>
      <p class="practice-hint">未指定关联时，检查器可在所有验收项中寻找相关证据。</p>
      <div class="dialog-actions"><button type="button" class="text-button" :disabled="busy" @click="emit('close')">取消</button><button type="submit" class="primary-button" :disabled="busy || count > 12000"><Save :size="16" /> {{ busy ? '保存中' : '保存证据' }}</button></div>
    </form>
  </PracticeDialog>
</template>
