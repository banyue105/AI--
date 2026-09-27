<script setup lang="ts">
import { nextTick, ref, toRaw, watch } from 'vue'
import { Plus, Trash2, X } from 'lucide-vue-next'
import type { DecisionScenarioInput } from '../types'

const props = defineProps<{ open: boolean; isNew: boolean; initial: DecisionScenarioInput | null }>()
const emit = defineEmits<{ close: []; save: [input: DecisionScenarioInput] }>()
const titleField = ref<HTMLInputElement | null>(null)
const draft = ref<DecisionScenarioInput>(blank())
const validation = ref('')

function blank(): DecisionScenarioInput {
  return { title: '', goal: '', timeLimitDays: 60, budgetYuan: 100000, peopleCount: 3,
    hasServer: true, changeRequest: '', resources: [], relations: [] }
}

watch(() => props.open, async (open) => {
  if (!open) return
  draft.value = props.isNew || !props.initial ? blank() : structuredClone(props.initial)
  validation.value = ''
  await nextTick()
  titleField.value?.focus()
})

function addResource() {
  draft.value.resources.push({ type: 'skill', label: '', quantity: 1, unit: '项' })
}

function save() {
  if (!draft.value.title.trim() || !draft.value.goal.trim()) {
    validation.value = '请填写场景名称和目标。'
    return
  }
  if (draft.value.timeLimitDays < 1 || draft.value.peopleCount < 1 || draft.value.budgetYuan < 0) {
    validation.value = '期限和人数至少为 1，预算不能为负。'
    return
  }
  if (draft.value.resources.some((item) => !item.label.trim())) {
    validation.value = '请补全资源名称，或删除空资源行。'
    return
  }
  emit('save', structuredClone(toRaw(draft.value)))
}
</script>

<template>
  <div v-if="open" class="modal-backdrop decision-backdrop" @click.self="emit('close')" @keydown.esc="emit('close')">
    <form class="editor-dialog decision-dialog" role="dialog" aria-modal="true" aria-labelledby="decision-dialog-title" @submit.prevent="save">
      <div class="dialog-header">
        <div><p class="eyebrow">现实条件</p><h2 id="decision-dialog-title">{{ isNew ? '创建场景' : '编辑场景条件' }}</h2></div>
        <button type="button" class="icon-button" aria-label="关闭编辑" @click="emit('close')"><X :size="17" /></button>
      </div>
      <label>场景名称<input ref="titleField" v-model="draft.title" maxlength="120" required placeholder="例如：60 天完成 AI 网页项目" /></label>
      <label>目标<textarea v-model="draft.goal" rows="2" maxlength="500" required placeholder="描述需要交付什么" /></label>
      <div class="decision-form-grid">
        <label>参与人数<input v-model.number="draft.peopleCount" type="number" min="1" max="1000" required /></label>
        <label>预算（元）<input v-model.number="draft.budgetYuan" type="number" min="0" step="1" required /></label>
        <label>期限（天）<input v-model.number="draft.timeLimitDays" type="number" min="1" max="3650" required /></label>
        <label class="decision-check-label"><input v-model="draft.hasServer" type="checkbox" /> 已有可复用服务器</label>
      </div>
      <label>提出的变更<textarea v-model="draft.changeRequest" rows="2" maxlength="500" placeholder="例如：增加视觉识别功能" /></label>
      <div class="decision-resource-head"><strong>人员角色 / 设备 / 技术资源</strong><button type="button" class="secondary-button compact" @click="addResource"><Plus :size="15" /> 添加资源</button></div>
      <p class="decision-hint">人员总数在上方填写；这里可补充角色、设备或技能。</p>
      <div v-for="(resource, index) in draft.resources" :key="index" class="decision-resource-row">
        <select v-model="resource.type" aria-label="资源类型">
          <option value="person">角色</option><option value="equipment">设备</option><option value="skill">技术</option><option value="money">资金</option><option value="other">其他</option>
        </select>
        <input v-model="resource.label" maxlength="120" aria-label="资源名称" placeholder="例如：视觉开发" />
        <input v-model.number="resource.quantity" type="number" min="0" aria-label="数量" />
        <input v-model="resource.unit" maxlength="32" aria-label="单位" placeholder="单位" />
        <button type="button" class="icon-button" :aria-label="`删除资源 ${index + 1}`" @click="draft.resources.splice(index, 1)"><Trash2 :size="16" /></button>
      </div>
      <p v-if="validation" class="decision-error" role="alert">{{ validation }}</p>
      <div class="dialog-actions"><button type="button" class="secondary-button" @click="emit('close')">取消</button><button type="submit" class="primary-button">保存条件</button></div>
    </form>
  </div>
</template>
