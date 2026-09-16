<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ArrowDown, ArrowRight, ListTree, Maximize2, Minus, Plus } from 'lucide-vue-next'
import type { SkillNode, SkillRelation } from '../types'
import { layoutAbilityGraph, type AbilityGraphLayout, type LayoutSkillNode } from '../utils/graphLayout'

const props = defineProps<{
  nodes: SkillNode[]
  relations: SkillRelation[]
  selectedId: string | null
}>()

const emit = defineEmits<{ select: [id: string] }>()
type TreeMode = 'personal' | 'direction' | 'path'
type Orientation = 'lr' | 'tb'
type RenderLane = { id: number; label: string; x: number; y: number; width: number; height: number }
type RenderColumn = { depth: number; label: string; x: number; y: number }
type RenderLayout = {
  nodes: LayoutSkillNode[]
  relations: AbilityGraphLayout['relations']
  lanes: RenderLane[]
  columns: RenderColumn[]
  width: number
  height: number
}

const scale = ref(0.9)
const listMode = ref(false)
const treeMode = ref<TreeMode>('personal')
const orientation = ref<Orientation>('lr')
const selectedLaneIds = ref<number[]>([])
const baseDirectionLayout = computed(() => layoutAbilityGraph(props.nodes, props.relations))
const directionOptions = computed(() => baseDirectionLayout.value.lanes.map((lane) => ({ id: lane.id, label: lane.label })))

watch(directionOptions, (options) => {
  const validIds = new Set(options.map((option) => option.id))
  const retained = selectedLaneIds.value.filter((id) => validIds.has(id))
  selectedLaneIds.value = retained.length ? retained : options.map((option) => option.id)
}, { immediate: true })

const pathNodeIds = computed(() => {
  const target = props.nodes.find((node) => node.status === 'target')
    ?? baseDirectionLayout.value.nodes.reduce<LayoutSkillNode | undefined>((best, node) => !best || node.layoutDepth > best.layoutDepth ? node : best, undefined)
  if (!target) return new Set<string>()
  const ids = new Set<string>([target.id])
  const visit = (id: string) => {
    props.relations
      .filter((relation) => relation.type === 'prerequisite' && relation.to === id)
      .forEach((relation) => {
        if (ids.has(relation.from)) return
        ids.add(relation.from)
        visit(relation.from)
      })
  }
  visit(target.id)
  return ids
})

function compactPersonalLayout(): AbilityGraphLayout {
  if (!props.nodes.length) return layoutAbilityGraph([], [])
  const xValues = [...new Set(props.nodes.map((node) => node.x))].sort((a, b) => a - b)
  const yValues = [...new Set(props.nodes.map((node) => node.y))].sort((a, b) => a - b)
  const nodes = props.nodes.map((node) => ({
    ...node,
    x: 40 + xValues.indexOf(node.x) * 174,
    y: 48 + yValues.indexOf(node.y) * 78,
    layoutDepth: xValues.indexOf(node.x),
    layoutLane: 0,
  }))
  const nodeIds = new Set(nodes.map((node) => node.id))
  const relations = props.relations
    .filter((relation) => nodeIds.has(relation.from) && nodeIds.has(relation.to))
    .map((relation) => ({ ...relation, crossDirection: false }))
  return {
    nodes,
    relations,
    lanes: [],
    columns: [],
    width: Math.max(780, 40 + xValues.length * 174),
    height: Math.max(470, 48 + yValues.length * 78),
  }
}

const modeLayout = computed<AbilityGraphLayout>(() => {
  if (treeMode.value === 'personal') return compactPersonalLayout()
  if (treeMode.value === 'path') {
    const nodes = props.nodes.filter((node) => pathNodeIds.value.has(node.id))
    const relations = props.relations.filter((relation) => pathNodeIds.value.has(relation.from) && pathNodeIds.value.has(relation.to))
    return layoutAbilityGraph(nodes, relations)
  }
  const laneByNode = new Map(baseDirectionLayout.value.nodes.map((node) => [node.id, node.layoutLane]))
  const allowedIds = new Set(props.nodes.filter((node) => selectedLaneIds.value.includes(laneByNode.get(node.id) ?? -1)).map((node) => node.id))
  const nodes = props.nodes.filter((node) => allowedIds.has(node.id))
  const relations = props.relations.filter((relation) => allowedIds.has(relation.from) && allowedIds.has(relation.to))
  return layoutAbilityGraph(nodes, relations)
})

const renderLayout = computed<RenderLayout>(() => {
  const layout = modeLayout.value
  const showGuides = treeMode.value !== 'personal'
  const horizontal: RenderLayout = {
    nodes: layout.nodes,
    relations: layout.relations,
    lanes: showGuides ? layout.lanes.map((lane) => ({ id: lane.id, label: lane.label, x: 16, y: lane.y, width: layout.width - 32, height: lane.height })) : [],
    columns: showGuides ? layout.columns.map((column) => ({ ...column, y: 12 })) : [],
    width: layout.width,
    height: layout.height,
  }
  if (orientation.value === 'lr') return horizontal
  return {
    nodes: horizontal.nodes.map((node) => ({ ...node, x: node.y, y: node.x })),
    relations: horizontal.relations,
    lanes: horizontal.lanes.map((lane) => ({ ...lane, x: lane.y, y: 16, width: lane.height, height: horizontal.width - 32 })),
    columns: horizontal.columns.map((column) => ({ ...column, x: 8, y: column.x })),
    width: horizontal.height,
    height: horizontal.width,
  }
})

const lineData = computed(() => renderLayout.value.relations.map((relation) => {
  const from = renderLayout.value.nodes.find((node) => node.id === relation.from)
  const to = renderLayout.value.nodes.find((node) => node.id === relation.to)
  if (!from || !to) return null
  if (orientation.value === 'tb') {
    const x1 = from.x + 72
    const y1 = from.y + 54
    const x2 = to.x + 72
    const y2 = to.y
    const controlY = (y1 + y2) / 2
    return { ...relation, path: `M ${x1} ${y1} C ${x1} ${controlY}, ${x2} ${controlY}, ${x2} ${y2}` }
  }
  const sameColumn = Math.abs(to.x - from.x) < 20
  const x1 = sameColumn ? from.x + 144 : to.x >= from.x ? from.x + 144 : from.x
  const y1 = from.y + 27
  const x2 = sameColumn ? to.x + 144 : to.x >= from.x ? to.x : to.x + 144
  const y2 = to.y + 27
  const controlX = sameColumn ? x1 + 38 : (x1 + x2) / 2
  return { ...relation, path: `M ${x1} ${y1} C ${controlX} ${y1}, ${controlX} ${y2}, ${x2} ${y2}` }
}).filter((line) => line !== null))

function toggleLane(id: number) {
  selectedLaneIds.value = selectedLaneIds.value.includes(id)
    ? selectedLaneIds.value.filter((laneId) => laneId !== id)
    : [...selectedLaneIds.value, id]
}

function setScale(value: number) {
  scale.value = Math.min(1.15, Math.max(0.7, value))
}
</script>

<template>
  <section class="graph-panel" aria-label="能力关系图谱">
    <div class="graph-toolbar">
      <div class="tree-mode-switch" aria-label="技能树类型">
        <button type="button" :class="{ active: treeMode === 'personal' }" @click="treeMode = 'personal'">个人技能树</button>
        <button type="button" :class="{ active: treeMode === 'direction' }" @click="treeMode = 'direction'">方向技能树</button>
        <button type="button" :class="{ active: treeMode === 'path' }" @click="treeMode = 'path'">路径技能树</button>
      </div>
      <div class="graph-actions">
        <div class="orientation-switch" aria-label="布局方向">
          <button type="button" :class="{ active: orientation === 'lr' }" title="从左到右" aria-label="从左到右布局" @click="orientation = 'lr'"><ArrowRight :size="16" /></button>
          <button type="button" :class="{ active: orientation === 'tb' }" title="从上到下" aria-label="从上到下布局" @click="orientation = 'tb'"><ArrowDown :size="16" /></button>
        </div>
        <span class="toolbar-divider" />
        <button class="icon-button" type="button" aria-label="缩小图谱" title="缩小" @click="setScale(scale - 0.1)"><Minus :size="17" /></button>
        <button class="zoom-value" type="button" title="恢复默认缩放" @click="setScale(0.9)">{{ Math.round(scale * 100) }}%</button>
        <button class="icon-button" type="button" aria-label="放大图谱" title="放大" @click="setScale(scale + 0.1)"><Plus :size="17" /></button>
        <button class="icon-button" type="button" :aria-label="listMode ? '切换到图谱' : '切换到分层列表'" :title="listMode ? '图谱视图' : '分层列表'" @click="listMode = !listMode">
          <Maximize2 v-if="listMode" :size="17" />
          <ListTree v-else :size="17" />
        </button>
      </div>
    </div>

    <div class="graph-subbar">
      <div v-if="treeMode === 'direction'" class="direction-picker" aria-label="选择展示方向">
        <span>展示方向</span>
        <label v-for="option in directionOptions" :key="option.id" :class="{ active: selectedLaneIds.includes(option.id) }">
          <input type="checkbox" :checked="selectedLaneIds.includes(option.id)" @change="toggleLane(option.id)" />
          {{ option.label }}
        </label>
      </div>
      <div v-else class="view-description">
        {{ treeMode === 'personal' ? '全部技能积累' : '目标能力的前置学习链' }}
      </div>
      <div class="legend" aria-label="节点状态图例">
        <span><i class="dot mastered" />已掌握</span>
        <span><i class="dot developing" />进行中</span>
        <span><i class="dot gap" />缺口</span>
        <span><i class="dot target" />目标</span>
      </div>
    </div>

    <div v-if="listMode" class="skill-list">
      <button v-for="node in renderLayout.nodes" :key="node.id" type="button" class="skill-list-item" :class="{ selected: selectedId === node.id }" @click="emit('select', node.id)">
        <span class="node-status" :class="node.status">{{ node.status === 'mastered' ? '已掌握' : node.status === 'developing' ? '进行中' : node.status === 'target' ? '目标' : '缺口' }}</span>
        <strong>{{ node.name }}</strong>
        <small>等级 {{ node.level }}/4</small>
      </button>
    </div>

    <div v-else class="graph-viewport">
      <div v-if="!renderLayout.nodes.length" class="graph-empty">至少选择一个方向</div>
      <div v-else class="graph-canvas" :class="[`orientation-${orientation}`, `mode-${treeMode}`]" :style="{ transform: `scale(${scale})`, width: `${renderLayout.width}px`, height: `${renderLayout.height}px` }">
        <div class="graph-columns" aria-hidden="true">
          <span v-for="column in renderLayout.columns" :key="column.depth" :style="{ left: `${column.x}px`, top: `${column.y}px` }">{{ column.label }}</span>
        </div>
        <div v-for="lane in renderLayout.lanes" :key="lane.id" class="graph-lane" :style="{ left: `${lane.x}px`, top: `${lane.y}px`, width: `${lane.width}px`, height: `${lane.height}px` }" aria-hidden="true"><span>{{ lane.label }}</span></div>
        <svg class="edges" :width="renderLayout.width" :height="renderLayout.height" :viewBox="`0 0 ${renderLayout.width} ${renderLayout.height}`" aria-hidden="true">
          <defs><marker id="arrow" viewBox="0 0 10 10" refX="8" refY="5" markerWidth="6" markerHeight="6" orient="auto-start-reverse"><path d="M 0 0 L 10 5 L 0 10 z" /></marker></defs>
          <path v-for="line in lineData" :key="`${line.from}-${line.to}`" :d="line.path" :class="[line.type, { 'cross-direction': line.crossDirection }]" marker-end="url(#arrow)" />
        </svg>
        <button v-for="node in renderLayout.nodes" :key="node.id" type="button" class="skill-node" :class="[node.status, { selected: selectedId === node.id }]" :style="{ left: `${node.x}px`, top: `${node.y}px` }" @click="emit('select', node.id)">
          <span>{{ node.name }}</span>
          <small>{{ node.level }}/4 · {{ node.status === 'mastered' ? '已掌握' : node.status === 'developing' ? '进行中' : node.status === 'target' ? '目标' : '缺口' }}</small>
        </button>
      </div>
    </div>
  </section>
</template>