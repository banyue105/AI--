<script setup lang="ts">
import { computed, ref } from 'vue'
import { Link2 } from 'lucide-vue-next'
import PracticeDialog from './PracticeDialog.vue'
import type { SourceScenario, SourceVersion } from '../types'
const props = defineProps<{ scenarios: SourceScenario[]; versions: SourceVersion[]; busy: boolean; error: string; loadVersions: (id: string) => Promise<boolean>; link: (scenarioId: string, versionId: string, option: string) => Promise<boolean> }>()
const emit = defineEmits<{ close: [] }>()
const scenarioId = ref(''), versionId = ref(''), optionKey = ref('')
const version = computed(() => props.versions.find(item => item.id === versionId.value))
const option = computed(() => version.value?.results.find(item => item.optionKey === optionKey.value))
async function changeScenario() { versionId.value = ''; optionKey.value = ''; if (scenarioId.value) await props.loadVersions(scenarioId.value) }
async function submit() { if (await props.link(scenarioId.value, versionId.value, optionKey.value)) emit('close') }
</script>

<template>
  <PracticeDialog title="关联原决策方案" :busy="busy" :error="error" @close="emit('close')">
    <form class="practice-form" @submit.prevent="submit">
      <p class="practice-hint">选择具体历史版本和方案，保存当时的目标、估算和假设快照。</p>
      <label>决策场景<select v-model="scenarioId" aria-label="决策场景" required :disabled="busy" @change="changeScenario"><option value="">请选择场景</option><option v-for="item in scenarios" :key="item.id" :value="item.id">{{ item.title }}</option></select></label>
      <p v-if="!busy && !scenarios.length" class="practice-hint">暂无可关联场景。可以先在决策沙盒创建并推演，也可以独立完成本次实践。</p>
      <label>历史版本<select v-model="versionId" aria-label="历史版本" required :disabled="busy || !scenarioId" @change="optionKey = ''"><option value="">请选择历史版本</option><option v-for="item in versions" :key="item.id" :value="item.id">版本 {{ item.number }}</option></select></label>
      <p v-if="!busy && scenarioId && !versions.length" class="practice-hint">该场景还没有可选版本，请先在决策沙盒完成一次推演。</p>
      <label>具体方案<select v-model="optionKey" aria-label="具体方案" required :disabled="busy || !version"><option value="">请选择方案</option><option v-for="item in version?.results || []" :key="item.optionKey" :value="item.optionKey">{{ item.optionName }}</option></select></label>
      <div v-if="option" class="practice-source-preview"><strong>{{ option.optionName }}</strong><p>工期：{{ option.timeRange.min }}–{{ option.timeRange.max }} 天</p><p>规则估算成本：{{ option.budgetRange.min }}–{{ option.budgetRange.max }} 元</p></div>
      <p class="practice-hint">原方案估算作为来源参考。个人小时和现金支出需另建同口径记录后比较。</p>
      <div class="dialog-actions"><button type="button" class="text-button" :disabled="busy" @click="emit('close')">取消</button><button type="submit" class="primary-button" :disabled="busy || !option"><Link2 :size="16" /> 保存方案快照</button></div>
    </form>
  </PracticeDialog>
</template>
