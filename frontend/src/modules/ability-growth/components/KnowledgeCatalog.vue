<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import {
  ArrowRight,
  BookOpen,
  Check,
  CircleDot,
  Code2,
  LoaderCircle,
  Network,
  Plus,
  ServerCog,
  Sparkles,
  Trash2,
} from 'lucide-vue-next'
import type { SkillNode } from '../types'
import type {
  KnowledgeStackItem,
  KnowledgeStackStage,
  KnowledgeTrack,
  KnowledgeTrackId,
} from '../services/knowledgeCatalogService'

const props = defineProps<{
  tracks: KnowledgeTrack[]
  personalSkills: SkillNode[]
  selectedTrackId: KnowledgeTrackId
  generating: boolean
  generationError: string
}>()

const emit = defineEmits<{
  add: [item: KnowledgeStackItem, track: KnowledgeTrack, stage: KnowledgeStackStage]
  select: [id: string]
  generate: [query: string]
  delete: [id: KnowledgeTrackId]
  'update:selectedTrackId': [id: KnowledgeTrackId]
}>()

const query = ref('')
const selectedTrack = computed(() => props.tracks.find((track) => track.id === props.selectedTrackId) ?? props.tracks[0] ?? null)
const trackSwitcher = ref<HTMLElement | null>(null)
const trackSliderStyle = ref({ width: '0px', transform: 'translateX(0px)', opacity: '0' })
const trackTransitionName = ref('track-slide-left')
const previousTrackIndex = ref(Math.max(0, props.tracks.findIndex((track) => track.id === props.selectedTrackId)))

async function updateTrackSlider() {
  await nextTick()
  const active = trackSwitcher.value?.querySelector<HTMLElement>('button[aria-selected="true"]')
  if (!active) {
    trackSliderStyle.value = { width: '0px', transform: 'translateX(0px)', opacity: '0' }
    return
  }
  trackSliderStyle.value = {
    width: `${active.offsetWidth}px`,
    transform: `translateX(${active.offsetLeft}px)`,
    opacity: '1',
  }
}

watch(() => props.selectedTrackId, (id) => {
  const nextIndex = props.tracks.findIndex((track) => track.id === id)
  if (nextIndex >= 0) {
    trackTransitionName.value = nextIndex >= previousTrackIndex.value ? 'track-slide-left' : 'track-slide-right'
    previousTrackIndex.value = nextIndex
  }
})
watch(() => [props.selectedTrackId, props.tracks.map((track) => track.id).join('|')], updateTrackSlider, { flush: 'post' })
onMounted(() => {
  updateTrackSlider()
  window.addEventListener('resize', updateTrackSlider)
})
onBeforeUnmount(() => window.removeEventListener('resize', updateTrackSlider))

function findPersonalSkill(item: KnowledgeStackItem) {
  const names = [item.name, ...item.aliases].map((name) => name.toLowerCase())
  return props.personalSkills.find((skill) =>
    names.some((name) => skill.name.toLowerCase().includes(name) || name.includes(skill.name.toLowerCase())),
  )
}

function statusLabel(item: KnowledgeStackItem) {
  const match = findPersonalSkill(item)
  if (!match) return '待学习'
  if (match.status === 'mastered' || match.level >= 2) return '已掌握'
  if (match.status === 'developing') return '学习中'
  return '待学习'
}

function statusClass(item: KnowledgeStackItem) {
  const label = statusLabel(item)
  return label === '已掌握' ? 'mastered' : label === '学习中' ? 'developing' : 'unstarted'
}

function trackCoverage(track: KnowledgeTrack) {
  const items = track.stages.flatMap((stage) => stage.items)
  return {
    matched: items.filter((item) => statusLabel(item) === '已掌握').length,
    total: items.length,
  }
}

function handleStackItem(item: KnowledgeStackItem, stage: KnowledgeStackStage) {
  const match = findPersonalSkill(item)
  if (match) emit('select', match.id)
  else if (selectedTrack.value) emit('add', item, selectedTrack.value, stage)
}
</script>

<template>
  <section class="knowledge-section" aria-labelledby="knowledge-title">
    <div class="section-heading compact-heading knowledge-heading">
      <div>
        <p class="eyebrow">知识分类</p>
        <h2 id="knowledge-title">选择预置方向或让 AI 生成</h2>
      </div>
      <span>结构化方向 · 阶段 · 技术项</span>
    </div>

    <form class="knowledge-query" @submit.prevent="query.trim() && emit('generate', query.trim())">
      <label for="knowledge-direction"><Sparkles :size="17" /> AI 目标方向</label>
      <input id="knowledge-direction" v-model="query" placeholder="例如：数据分析师、机器学习工程师、产品经理" />
      <button class="primary-button" type="submit" :disabled="generating || !query.trim()">
        <LoaderCircle v-if="generating" class="spin-icon" :size="17" />
        <Sparkles v-else :size="17" />
        {{ generating ? 'AI 生成中' : 'AI 生成技术栈' }}
      </button>
    </form>
    <p v-if="generationError" class="knowledge-error">{{ generationError }}</p>

    <div ref="trackSwitcher" class="track-switcher" role="tablist" aria-label="技术方向">
      <span class="track-slider" :style="trackSliderStyle" aria-hidden="true" />
      <TransitionGroup name="track-tab" @after-enter="updateTrackSlider" @after-leave="updateTrackSlider">
        <button
        v-for="track in tracks"
        :key="track.id"
        type="button"
        role="tab"
        :aria-selected="selectedTrack?.id === track.id"
        :class="{ active: selectedTrack?.id === track.id }"
        @click="emit('update:selectedTrackId', track.id)"
      >
        <Code2 v-if="track.id === 'frontend'" :size="19" />
        <ServerCog v-else-if="track.id === 'backend'" :size="19" />
        <Network v-else-if="track.id === 'network'" :size="19" />
        <Sparkles v-else :size="19" />
        <span>{{ track.shortTitle }}</span>
        </button>
      </TransitionGroup>
    </div>

    <Transition :name="trackTransitionName" mode="out-in">
      <div v-if="selectedTrack" :key="selectedTrack.id" class="track-content">
      <header class="track-summary">
        <div>
          <span class="track-kicker">{{ selectedTrack.source === 'ai' ? 'AI 生成' : selectedTrack.source === 'mock' ? '本地候选' : '预置方向' }}</span>
          <h3>{{ selectedTrack.title }}</h3>
          <p>{{ selectedTrack.description }}</p>
        </div>
        <div class="track-outcome">
          <span><BookOpen :size="16" /> 能力目标</span>
          <strong>{{ selectedTrack.outcome }}</strong>
          <small>当前已掌握 {{ trackCoverage(selectedTrack).matched }}/{{ trackCoverage(selectedTrack).total }} 项</small>
        </div>
        <button class="icon-button track-delete-button" type="button" aria-label="删除当前知识方向" title="删除知识方向" @click="emit('delete', selectedTrack.id)">
          <Trash2 :size="17" />
        </button>
      </header>

      <div class="stack-stages">
        <article v-for="(stage, stageIndex) in selectedTrack.stages" :key="stage.id" class="stack-stage">
          <div class="stage-heading">
            <span>{{ String(stageIndex + 1).padStart(2, '0') }}</span>
            <div><strong>{{ stage.title }}</strong><small>{{ stage.description }}</small></div>
          </div>
          <div class="stack-items">
            <button
              v-for="item in stage.items"
              :key="item.id"
              type="button"
              class="stack-item"
              :class="statusClass(item)"
              :aria-label="`${item.name}，${statusLabel(item)}`"
              @click="handleStackItem(item, stage)"
            >
              <span class="stack-status-icon">
                <Check v-if="statusLabel(item) === '已掌握'" :size="14" />
                <CircleDot v-else-if="statusLabel(item) === '学习中'" :size="14" />
                <Plus v-else :size="14" />
              </span>
              <span class="stack-item-copy">
                <strong>{{ item.name }}</strong>
                <small>{{ item.description }}</small>
              </span>
              <span class="stack-state">{{ statusLabel(item) }}</span>
            </button>
          </div>
          <ArrowRight v-if="stageIndex < selectedTrack.stages.length - 1" class="stage-arrow" :size="20" />
        </article>
      </div>
        <p class="catalog-hint"><Plus :size="14" /> 点击“待学习”技术可带入新增能力表单，已有技术会定位到个人图谱。</p>
      </div>
    </Transition>
  </section>
</template>
