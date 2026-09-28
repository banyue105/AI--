<script setup lang="ts">
import { History, CheckCircle2 } from 'lucide-vue-next'
import { dateLabel } from '../utils/drafts'
import type { Review, ReviewSummary } from '../types'
defineProps<{ history: ReviewSummary[]; review: Review | null; previous: Review | null; busy: boolean }>()
const emit = defineEmits<{ select: [id: string]; compare: [id: string] }>()
</script>

<template>
  <div class="practice-history">
    <p v-if="!history.length" class="practice-hint">检查后会保存完整输入与结果快照。补交材料后可以比较变化。</p>
    <div class="practice-history-versions">
      <button v-for="item in history" :key="item.id" type="button" class="practice-history-item" :class="{ 'practice-history-selected': item.id === review?.id }" :aria-pressed="item.id === review?.id" :disabled="busy" @click="emit('select', item.id)">
        <History :size="18" /><span><strong>复盘版本 {{ item.number }}</strong><small>{{ dateLabel(item.createdAt) }} · 输入版本 {{ item.inputRevision }} · {{ item.source === 'ai' ? 'AI' : '确定性检查' }}</small></span>
        <span class="practice-history-state"><CheckCircle2 v-if="item.confirmedAt" :size="14" />{{ item.isStale ? '历史输入' : item.confirmedAt ? '已确认' : '待确认' }}</span>
      </button>
    </div>
    <label v-if="review && history.length > 1" class="practice-compare-select">对照版本<select :value="previous?.id || ''" :disabled="busy" @change="emit('compare', ($event.target as HTMLSelectElement).value)"><option value="">选择一个版本比较</option><option v-for="item in history.filter(item => item.id !== review?.id)" :key="item.id" :value="item.id">复盘版本 {{ item.number }} · {{ dateLabel(item.createdAt) }}</option></select></label>
    <p v-if="previous" class="practice-hint">正在比较版本 {{ previous.number }} 与版本 {{ review?.number }}。在上方验收项中查看判定变化；标准修改会单独标注。</p>
  </div>
</template>
