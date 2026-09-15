<script setup lang="ts">
import { computed, ref } from 'vue'
import { ListTree, Maximize2, Minus, Plus } from 'lucide-vue-next'
import type { SkillNode, SkillRelation } from '../types'

const props = defineProps<{
  nodes: SkillNode[]
  relations: SkillRelation[]
  selectedId: string | null
}>()

const emit = defineEmits<{ select: [id: string] }>()
const scale = ref(0.9)
const listMode = ref(false)
const canvasHeight = computed(() => Math.max(470, ...props.nodes.map((node) => node.y + 110)))

const lineData = computed(() =>
  props.relations
    .map((relation) => {
      const from = props.nodes.find((node) => node.id === relation.from)
      const to = props.nodes.find((node) => node.id === relation.to)
      if (!from || !to) return null
      return {
        ...relation,
        x1: from.x + 72,
        y1: from.y + 26,
        x2: to.x,
        y2: to.y + 26,
      }
    })
    .filter((line) => line !== null),
)

function setScale(value: number) {
  scale.value = Math.min(1.15, Math.max(0.7, value))
}
</script>

<template>
  <section class="graph-panel" aria-label="能力关系图谱">
    <div class="graph-toolbar">
      <div class="legend" aria-label="节点状态图例">
        <span><i class="dot mastered" />已掌握</span>
        <span><i class="dot developing" />进行中</span>
        <span><i class="dot gap" />缺口</span>
        <span><i class="dot target" />目标</span>
      </div>
      <div class="graph-actions">
        <button class="icon-button" type="button" aria-label="缩小图谱" title="缩小" @click="setScale(scale - 0.1)">
          <Minus :size="18" />
        </button>
        <button class="zoom-value" type="button" title="恢复默认缩放" @click="setScale(0.9)">{{ Math.round(scale * 100) }}%</button>
        <button class="icon-button" type="button" aria-label="放大图谱" title="放大" @click="setScale(scale + 0.1)">
          <Plus :size="18" />
        </button>
        <button class="icon-button" type="button" :aria-label="listMode ? '切换到图谱' : '切换到分层列表'" :title="listMode ? '图谱视图' : '分层列表'" @click="listMode = !listMode">
          <Maximize2 v-if="listMode" :size="18" />
          <ListTree v-else :size="18" />
        </button>
      </div>
    </div>

    <div v-if="listMode" class="skill-list">
      <button
        v-for="node in nodes"
        :key="node.id"
        type="button"
        class="skill-list-item"
        :class="{ selected: selectedId === node.id }"
        @click="emit('select', node.id)"
      >
        <span class="node-status" :class="node.status">{{ node.status === 'mastered' ? '已掌握' : node.status === 'developing' ? '进行中' : node.status === 'target' ? '目标' : '缺口' }}</span>
        <strong>{{ node.name }}</strong>
        <small>等级 {{ node.level }}/4</small>
      </button>
    </div>

    <div v-else class="graph-viewport">
      <div class="graph-canvas" :style="{ transform: `scale(${scale})`, height: `${canvasHeight}px` }">
        <svg class="edges" width="1040" :height="canvasHeight" :viewBox="`0 0 1040 ${canvasHeight}`" aria-hidden="true">
          <defs>
            <marker id="arrow" viewBox="0 0 10 10" refX="8" refY="5" markerWidth="6" markerHeight="6" orient="auto-start-reverse">
              <path d="M 0 0 L 10 5 L 0 10 z" />
            </marker>
          </defs>
          <line
            v-for="line in lineData"
            :key="`${line.from}-${line.to}`"
            :x1="line.x1"
            :y1="line.y1"
            :x2="line.x2"
            :y2="line.y2"
            :class="line.type"
            marker-end="url(#arrow)"
          />
        </svg>
        <button
          v-for="node in nodes"
          :key="node.id"
          type="button"
          class="skill-node"
          :class="[node.status, { selected: selectedId === node.id }]"
          :style="{ left: `${node.x}px`, top: `${node.y}px` }"
          @click="emit('select', node.id)"
        >
          <span>{{ node.name }}</span>
          <small>{{ node.level }}/4 · {{ node.status === 'mastered' ? '已掌握' : node.status === 'developing' ? '进行中' : node.status === 'target' ? '目标' : '缺口' }}</small>
        </button>
      </div>
    </div>
  </section>
</template>
