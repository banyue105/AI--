<script setup lang="ts">
import { Info } from 'lucide-vue-next'
import type { MetricComparison } from '../types'
defineProps<{ comparisons: MetricComparison[] }>()
const names: Record<string, string> = { effort: '个人有效投入', duration: '项目持续时间', cost: '资金成本' }
const units: Record<string, string> = { hour: '小时', day: '天', CNY: '元' }
const bases: Record<string, string> = { effective_work: '有效操作时间', calendar_days: '日历天数', cash: '现金支出', full_cost: '含人工的完整成本' }
const scopes: Record<string, string> = { self: '本人', project: '整个项目' }
function number(value: number) { return new Intl.NumberFormat('zh-CN', { maximumFractionDigits: 3 }).format(value) }
function difference(item: MetricComparison) {
  if (!item.comparable) return '暂不可比'
  if (item.position === 'above') return `高于上限 ${number(item.deltaFromMax!)} ${units[item.unit]}`
  if (item.position === 'below') return `低于下限 ${number(-item.deltaFromMin!)} ${units[item.unit]}`
  return '在预期区间内'
}
</script>

<template>
  <div class="practice-metric-list">
    <div v-if="!comparisons.length" class="practice-empty-inline"><Info :size="19" /><p>还没有投入记录。编辑目标与投入，记录预期和实际值。</p></div>
    <article v-for="(item, index) in comparisons" :key="index" class="practice-metric-row">
      <div class="practice-metric-identity"><strong>{{ names[item.metric] }}</strong><small>{{ scopes[item.scope] || item.scope }} · {{ bases[item.basis] || item.basis }}</small></div>
      <dl class="practice-metric-values">
        <div><dt>预期</dt><dd v-if="item.expected">{{ number(item.expected.min) }}<template v-if="item.expected.min !== item.expected.max">–{{ number(item.expected.max) }}</template> <small>{{ units[item.unit] }}</small></dd><dd v-else>未记录</dd></div>
        <div><dt>实际</dt><dd>{{ item.actual === null ? '未记录' : number(item.actual) }} <small v-if="item.actual !== null">{{ units[item.unit] }}</small></dd></div>
        <div class="practice-metric-delta"><dt>程序计算差异</dt><dd :class="{ 'practice-attention': item.position === 'above' }">{{ difference(item) }}<small v-if="item.percentDelta !== null">（{{ item.percentDelta > 0 ? '+' : '' }}{{ item.percentDelta }}%）</small></dd></div>
      </dl>
      <p class="practice-metric-reason"><Info :size="13" />{{ item.reason }}<span v-if="item.expected && item.baselineTiming === 'before_practice'">实践前记录</span></p>
    </article>
  </div>
</template>
