<script setup lang="ts">
import { ref } from 'vue'
import { CheckCircle2, CircleAlert, FileCheck2, FileSearch, Pencil, Quote } from 'lucide-vue-next'
import FindingCorrection from './FindingCorrection.vue'
import { verdictLabels } from '../utils/drafts'
import type { Citation, Criterion, Finding, ManualOverride, Review } from '../types'
const props = defineProps<{ criteria: Criterion[]; review: Review | null; previous: Review | null; selectedId: string; busy: boolean; overrides: ManualOverride[]; canEdit: boolean }>()
const emit = defineEmits<{ select: [id: string]; cite: [citation: Citation]; overrides: [value: ManualOverride[]] }>()
const correcting = ref<Criterion | null>(null)
function finding(id: string): Finding | undefined { return props.review?.findings.find(item => item.criterionId === id) }
function correction(id: string) { return props.overrides.find(item => item.criterionId === id) || props.review?.confirmation?.overrides.find(item => item.criterionId === id) }
function verdict(id: string) { return correction(id)?.verdict || finding(id)?.verdict }
function citations(id: string) { return correction(id)?.citations || finding(id)?.citations || [] }
function nextAction(criterion: Criterion) {
  if (!props.review || verdict(criterion.id) === 'supported') return null
  return finding(criterion.id)?.nextAction || `补充：${criterion.expectedEvidence}`
}
function delta(criterion: Criterion) {
  const before = props.previous?.input.criteria.find(item => item.id === criterion.id)
  if (!before) return '本次新增验收项'
  if (before.standard !== criterion.standard || before.required !== criterion.required) return '验收标准已修改，不能直接视为结果改善'
  const oldVerdict = props.previous?.confirmation?.overrides.find(item => item.criterionId === criterion.id)?.verdict
    || props.previous?.findings.find(item => item.criterionId === criterion.id)?.verdict
  return oldVerdict && verdict(criterion.id) ? `${verdictLabels[oldVerdict]} → ${verdictLabels[verdict(criterion.id)!]}` : '尚无可比较判定'
}
function saveCorrection(value: ManualOverride) { emit('overrides', [...props.overrides.filter(item => item.criterionId !== value.criterionId), value]) }
</script>

<template>
  <div class="practice-findings" aria-label="验收项与材料判定">
    <article v-for="(criterion, index) in criteria" :key="criterion.id" class="practice-finding" :class="{ 'practice-finding-selected': criterion.id === selectedId }">
      <button type="button" class="practice-criterion-select" :aria-label="`查看验收项：${criterion.title}`" :aria-expanded="criterion.id === selectedId" @click="emit('select', criterion.id)">
        <span class="practice-step-number">{{ String(index + 1).padStart(2, '0') }}</span>
        <span class="practice-criterion-title"><strong>{{ criterion.title }}</strong><small>{{ criterion.required ? '必需项' : '可选项' }}<template v-if="correction(criterion.id)"> · {{ review?.confirmation ? '人工确认' : '待确认修正' }}</template></small></span>
        <span class="practice-verdict" :class="verdict(criterion.id) || 'unreviewed'">
          <CheckCircle2 v-if="verdict(criterion.id) === 'supported'" :size="15" /><CircleAlert v-else-if="verdict(criterion.id) === 'conflicting'" :size="15" /><FileSearch v-else :size="15" />
          {{ verdict(criterion.id) ? verdictLabels[verdict(criterion.id)!] : '待检查' }}
        </span>
      </button>
      <div v-if="criterion.id === selectedId" class="practice-finding-details">
        <p class="practice-standard">{{ criterion.standard }}</p>
        <p v-if="finding(criterion.id)">{{ correction(criterion.id)?.reason || finding(criterion.id)?.reason }}</p>
        <p v-else class="practice-hint">需要材料：{{ criterion.expectedEvidence }}</p>
        <p v-if="previous" class="practice-version-change">与版本 {{ previous.number }} 比较：{{ delta(criterion) }}</p>
        <div v-if="citations(criterion.id).length" class="practice-citations">
          <button v-for="(citation, citationIndex) in citations(criterion.id)" :key="citationIndex" type="button" class="practice-citation" @click="emit('cite', citation)">
            <Quote :size="14" /><span>{{ review?.input.evidence.find(item => item.id === citation.evidenceId)?.title }} · 第 {{ citation.startLine }}{{ citation.endLine !== citation.startLine ? `–${citation.endLine}` : '' }} 行</span>
          </button>
        </div>
        <div v-if="nextAction(criterion)" class="practice-next-action"><FileCheck2 :size="17" /><p>{{ nextAction(criterion) }}</p></div>
        <div v-if="canEdit && review" class="practice-inline-actions"><button type="button" class="text-button" :disabled="busy" @click="correcting = criterion"><Pencil :size="15" /> 纠正判定</button><button v-if="overrides.some(item => item.criterionId === criterion.id)" type="button" class="text-button" @click="emit('overrides', overrides.filter(item => item.criterionId !== criterion.id))">撤销待确认修正</button></div>
        <p v-if="review?.confirmation?.overrides.some(item => item.criterionId === criterion.id)" class="practice-hint">原检查判定：{{ verdictLabels[finding(criterion.id)!.verdict] }} · {{ finding(criterion.id)?.reason }}</p>
      </div>
    </article>
    <FindingCorrection v-if="correcting && review" :criterion="correcting" :review="review" :current="correction(correcting.id) || null" @save="saveCorrection" @close="correcting = null" />
  </div>
</template>
