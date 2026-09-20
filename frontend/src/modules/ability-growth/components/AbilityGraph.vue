<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue'
import { ArrowDown, ArrowRight, Check, ListTree, Maximize2, Minus, Plus, Trash2, X } from 'lucide-vue-next'
import type { KnowledgeTrack } from '../services/knowledgeCatalogService'
import type { SkillNode, SkillRelation } from '../types'
import { layoutAbilityGraph, type AbilityGraphLayout, type LayoutSkillNode } from '../utils/graphLayout'

const props = defineProps<{
  nodes: SkillNode[]
  relations: SkillRelation[]
  tracks: KnowledgeTrack[]
  selectedId: string | null
}>()

const emit = defineEmits<{
  select: [id: string]
  deleteMany: [ids: string[]]
  clearSelection: []
  previewCatalog: [payload: { trackId: string; stageId: string; itemId: string }]
  relationContext: [payload: {
    nodeId: string
    previous: Array<{ id: string | null; name: string; status: SkillNode['status']; meta?: string }>
    next: Array<{ id: string | null; name: string; status: SkillNode['status']; meta?: string }>
  }]
}>()
type TreeMode = 'personal' | 'direction' | 'path'
type Orientation = 'lr' | 'tb'
type DisplayNode = LayoutSkillNode & {
  meta?: string
  selectId?: string
  catalogNode?: boolean
  catalogRef?: { trackId: string; stageId: string; itemId: string }
}
type RenderLane = { id: number; label: string; x: number; y: number; width: number; height: number }
type RenderColumn = { depth: number; label: string; x: number; y: number }
type DisplayLayout = Omit<AbilityGraphLayout, 'nodes'> & { nodes: DisplayNode[] }
type RenderLayout = {
  nodes: DisplayNode[]
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
const selectedTrackIds = ref<string[]>([])
const batchMode = ref(false)
const batchSelectedIds = ref<string[]>([])
const popoverPosition = ref({ x: 0, y: 0 })
const popoverElement = ref<HTMLElement | null>(null)
const draggingPopover = ref(false)
const popoverVisible = ref(false)
const previewNodeId = ref<string | null>(null)
let dragOrigin: { pointerX: number; pointerY: number; x: number; y: number } | null = null
const basePathLayout = computed(() => layoutAbilityGraph(props.nodes, props.relations))
const directionOptions = computed(() => props.tracks.map((track) => ({ id: track.id, label: track.shortTitle })))
const treeModeSliderStyle = computed(() => {
  const index = treeMode.value === 'personal' ? 0 : treeMode.value === 'direction' ? 1 : 2
  return { transform: `translateX(calc(${index * 100}% + ${index * 2}px))` }
})
const orientationSliderStyle = computed(() => ({
  transform: `translateX(calc(${orientation.value === 'lr' ? 0 : 100}% + ${orientation.value === 'lr' ? 0 : 2}px))`,
}))

watch(directionOptions, (options) => {
  const validIds = new Set(options.map((option) => option.id))
  const retained = selectedTrackIds.value.filter((id) => validIds.has(id))
  selectedTrackIds.value = retained.length ? retained : options.slice(0, 1).map((option) => option.id)
}, { immediate: true })

watch(treeMode, () => clearSelection())

const pathNodeIds = computed(() => {
  const target = props.nodes.find((node) => node.status === 'target')
    ?? basePathLayout.value.nodes.reduce<LayoutSkillNode | undefined>((best, node) => !best || node.layoutDepth > best.layoutDepth ? node : best, undefined)
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

function namesMatch(skill: SkillNode, name: string, aliases: string[]) {
  const personalName = skill.name.toLowerCase()
  return [name, ...aliases].some((candidate) => {
    const normalized = candidate.toLowerCase()
    return personalName.includes(normalized) || normalized.includes(personalName)
  })
}

function compactPersonalLayout(): DisplayLayout {
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
  const bestIncoming = new Map<string, SkillRelation>()
  props.relations.filter((relation) => relation.type === 'prerequisite').forEach((relation) => {
    const current = bestIncoming.get(relation.to)
    if (!current || relation.confidence > current.confidence) bestIncoming.set(relation.to, relation)
  })
  const nodeIds = new Set(nodes.map((node) => node.id))
  const relations = [...bestIncoming.values()]
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

function knowledgeDirectionLayout(): DisplayLayout {
  const tracks = props.tracks.filter((track) => selectedTrackIds.value.includes(track.id))
  const nodes: DisplayNode[] = []
  const relations: AbilityGraphLayout['relations'] = []
  const lanes: AbilityGraphLayout['lanes'] = []
  let laneTop = 34
  let maxStages = 1

  tracks.forEach((track, trackIndex) => {
    maxStages = Math.max(maxStages, track.stages.length)
    const maxItems = Math.max(1, ...track.stages.map((stage) => stage.items.length))
    const laneHeight = 64 + maxItems * 66
    lanes.push({ id: trackIndex, label: track.title, y: laneTop, height: laneHeight })

    track.stages.forEach((stage, stageIndex) => {
      stage.items.forEach((item, itemIndex) => {
        const match = props.nodes.find((skill) => namesMatch(skill, item.name, item.aliases))
        const id = `direction:${track.id}:${item.id}`
        nodes.push({
          id,
          name: item.name,
          description: item.description,
          level: match?.level ?? 0,
          status: match?.status ?? 'gap',
          evidenceIds: match?.evidenceIds ?? [],
          x: 176 + stageIndex * 208,
          y: laneTop + 42 + itemIndex * 66,
          layoutDepth: stageIndex,
          layoutLane: trackIndex,
          meta: stage.title,
          selectId: match?.id,
          catalogNode: true,
          catalogRef: { trackId: track.id, stageId: stage.id, itemId: item.id },
        })
        if (stageIndex > 0) {
          const previous = track.stages[stageIndex - 1]
          const previousItem = previous.items.length ? previous.items[itemIndex % previous.items.length] : null
          if (previousItem) {
            relations.push({
              from: `direction:${track.id}:${previousItem.id}`,
              to: id,
              type: 'prerequisite',
              confidence: 1,
              crossDirection: false,
            })
          }
        }
      })
    })
    laneTop += laneHeight + 54
  })

  return {
    nodes,
    relations,
    lanes,
    columns: [],
    width: Math.max(860, 176 + maxStages * 208),
    height: Math.max(470, laneTop),
  }
}

const modeLayout = computed<DisplayLayout>(() => {
  if (treeMode.value === 'personal') return compactPersonalLayout()
  if (treeMode.value === 'direction') return knowledgeDirectionLayout()
  const nodes = props.nodes.filter((node) => pathNodeIds.value.has(node.id))
  const relations = props.relations.filter((relation) => pathNodeIds.value.has(relation.from) && pathNodeIds.value.has(relation.to))
  return layoutAbilityGraph(nodes, relations)
})

const renderLayout = computed<RenderLayout>(() => {
  const layout = modeLayout.value
  const showGuides = treeMode.value !== 'personal'
  const horizontal: RenderLayout = {
    nodes: layout.nodes,
    relations: layout.relations,
    lanes: showGuides ? layout.lanes.map((lane) => ({ id: lane.id, label: lane.label, x: 16, y: lane.y, width: layout.width - 32, height: lane.height })) : [],
    columns: treeMode.value === 'path' ? layout.columns.map((column) => ({ ...column, y: 12 })) : [],
    width: layout.width,
    height: layout.height,
  }
  if (orientation.value === 'lr') return horizontal

  const laneIds = treeMode.value === 'direction'
    ? [...new Set(layout.nodes.map((node) => node.layoutLane))]
    : [0]
  const laneStarts = new Map<number, number>()
  const laneWidths = new Map<number, number>()
  let nextX = 48

  laneIds.forEach((laneId) => {
    const laneNodes = treeMode.value === 'direction'
      ? layout.nodes.filter((node) => node.layoutLane === laneId)
      : layout.nodes
    const depthCounts = new Map<number, number>()
    laneNodes.forEach((node) => depthCounts.set(node.layoutDepth, (depthCounts.get(node.layoutDepth) ?? 0) + 1))
    const maxInLevel = Math.max(1, ...depthCounts.values())
    const laneWidth = Math.max(176, maxInLevel * 144 + 32)
    laneStarts.set(laneId, nextX)
    laneWidths.set(laneId, laneWidth)
    nextX += laneWidth + 56
  })

  const rowIndexes = new Map<string, number>()
  const verticalNodes = layout.nodes.map((node) => {
    const laneId = treeMode.value === 'direction' ? node.layoutLane : 0
    const key = `${laneId}:${node.layoutDepth}`
    const row = rowIndexes.get(key) ?? 0
    rowIndexes.set(key, row + 1)
    return {
      ...node,
      x: (laneStarts.get(laneId) ?? 48) + 16 + row * 144,
      y: 62 + node.layoutDepth * 118,
    }
  })
  const maxDepth = Math.max(0, ...verticalNodes.map((node) => node.layoutDepth))
  const width = Math.max(720, nextX - 8)
  const height = Math.max(470, 62 + maxDepth * 118 + 112)
  const lanes = treeMode.value === 'direction'
    ? laneIds.map((laneId, index) => ({
        id: laneId,
        label: layout.lanes.find((lane) => lane.id === laneId)?.label ?? `方向 ${index + 1}`,
        x: laneStarts.get(laneId) ?? 48,
        y: 24,
        width: laneWidths.get(laneId) ?? 176,
        height: height - 48,
      }))
    : []
  const columns = treeMode.value === 'path'
    ? layout.columns.map((column) => ({ ...column, x: 8, y: 62 + column.depth * 118 }))
    : []

  return { nodes: verticalNodes, relations: layout.relations, lanes, columns, width, height }
})

const selectableVisibleIds = computed(() => [...new Set(
  renderLayout.value.nodes.map((node) => personalNodeId(node)).filter((id): id is string => Boolean(id)),
)])
const allVisibleSelected = computed(() =>
  selectableVisibleIds.value.length > 0 && selectableVisibleIds.value.every((id) => batchSelectedIds.value.includes(id)),
)
const lineData = computed(() => renderLayout.value.relations.map((relation) => {
  const from = renderLayout.value.nodes.find((node) => node.id === relation.from)
  const to = renderLayout.value.nodes.find((node) => node.id === relation.to)
  if (!from || !to) return null
  if (orientation.value === 'tb') {
    const x1 = from.x + 60
    const y1 = from.y + 48
    const x2 = to.x + 60
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

function toggleTrack(id: string) {
  selectedTrackIds.value = selectedTrackIds.value.includes(id)
    ? selectedTrackIds.value.filter((trackId) => trackId !== id)
    : [...selectedTrackIds.value, id]
}

function personalNodeId(node: DisplayNode) {
  if (treeMode.value === 'direction') return node.selectId ?? null
  return props.nodes.some((item) => item.id === node.id) ? node.id : null
}

function emitRelationContext(node: DisplayNode) {
  const toNeighbor = (id: string) => {
    const neighbor = renderLayout.value.nodes.find((item) => item.id === id)
    if (!neighbor) return null
    return {
      id: personalNodeId(neighbor),
      name: neighbor.name,
      status: neighbor.status,
      meta: neighbor.meta,
    }
  }
  const previous = renderLayout.value.relations
    .filter((relation) => relation.to === node.id)
    .map((relation) => toNeighbor(relation.from))
    .filter((item): item is NonNullable<typeof item> => Boolean(item))
  const next = renderLayout.value.relations
    .filter((relation) => relation.from === node.id)
    .map((relation) => toNeighbor(relation.to))
    .filter((item): item is NonNullable<typeof item> => Boolean(item))
  emit('relationContext', {
    nodeId: personalNodeId(node) ?? node.id,
    previous,
    next,
  })
}

function placePopover(node: DisplayNode) {
  const nodeWidth = orientation.value === 'tb' ? 120 : 144
  const popupWidth = popoverElement.value?.offsetWidth ?? 252
  const popupHeight = popoverElement.value?.offsetHeight ?? 148
  const gap = 12
  let x = node.x + nodeWidth + gap
  if (x + popupWidth > renderLayout.value.width - 8) x = node.x - popupWidth - gap
  popoverPosition.value = {
    x: Math.max(8, Math.min(x, renderLayout.value.width - popupWidth - 8)),
    y: Math.max(8, Math.min(node.y - 8, renderLayout.value.height - popupHeight - 8)),
  }
}

function selectNode(node: DisplayNode) {
  const id = personalNodeId(node)
  if (!id && node.catalogRef) {
    previewNodeId.value = node.id
    placePopover(node)
    popoverVisible.value = true
    emit('previewCatalog', node.catalogRef)
    void nextTick(() => placePopover(node))
    return
  }
  if (!id) return
  if (batchMode.value) {
    batchSelectedIds.value = batchSelectedIds.value.includes(id)
      ? batchSelectedIds.value.filter((item) => item !== id)
      : [...batchSelectedIds.value, id]
    return
  }
  previewNodeId.value = null
  placePopover(node)
  popoverVisible.value = true
  emitRelationContext(node)
  emit('select', id)
  void nextTick(() => placePopover(node))
}

function movePopover(event: PointerEvent) {
  if (!dragOrigin) return
  const popupWidth = popoverElement.value?.offsetWidth ?? 252
  const popupHeight = popoverElement.value?.offsetHeight ?? 148
  const nextX = dragOrigin.x + (event.clientX - dragOrigin.pointerX) / scale.value
  const nextY = dragOrigin.y + (event.clientY - dragOrigin.pointerY) / scale.value
  popoverPosition.value = {
    x: Math.max(8, Math.min(nextX, renderLayout.value.width - popupWidth - 8)),
    y: Math.max(8, Math.min(nextY, renderLayout.value.height - popupHeight - 8)),
  }
}

function stopPopoverDrag() {
  draggingPopover.value = false
  dragOrigin = null
  window.removeEventListener('pointermove', movePopover)
  window.removeEventListener('pointerup', stopPopoverDrag)
  window.removeEventListener('pointercancel', stopPopoverDrag)
}

function hidePopover() {
  stopPopoverDrag()
  popoverVisible.value = false
}

function clearSelection() {
  hidePopover()
  previewNodeId.value = null
  emit('clearSelection')
}

function startPopoverDrag(event: PointerEvent) {
  if (event.button !== 0) return
  event.preventDefault()
  draggingPopover.value = true
  dragOrigin = {
    pointerX: event.clientX,
    pointerY: event.clientY,
    x: popoverPosition.value.x,
    y: popoverPosition.value.y,
  }
  window.addEventListener('pointermove', movePopover)
  window.addEventListener('pointerup', stopPopoverDrag)
  window.addEventListener('pointercancel', stopPopoverDrag)
}

function toggleBatchMode() {
  batchMode.value = !batchMode.value
  batchSelectedIds.value = []
}

function toggleVisibleSelection() {
  const visibleIds = selectableVisibleIds.value
  if (allVisibleSelected.value) {
    const visibleSet = new Set(visibleIds)
    batchSelectedIds.value = batchSelectedIds.value.filter((id) => !visibleSet.has(id))
    return
  }
  batchSelectedIds.value = [...new Set([...batchSelectedIds.value, ...visibleIds])]
}
function deleteSelectedNodes() {
  if (!batchSelectedIds.value.length) return
  const names = props.nodes.filter((node) => batchSelectedIds.value.includes(node.id)).map((node) => node.name)
  if (!window.confirm(`确定删除已选择的 ${names.length} 个技能吗？相关连线也会一并移除。`)) return
  emit('deleteMany', [...batchSelectedIds.value])
  batchSelectedIds.value = []
  batchMode.value = false
}

watch(() => props.nodes.map((node) => node.id), (ids) => {
  const validIds = new Set(ids)
  batchSelectedIds.value = batchSelectedIds.value.filter((id) => validIds.has(id))
})

watch([() => props.selectedId, renderLayout], ([selectedId], [previousSelectedId]) => {
  if (!selectedId) {
    if (!previewNodeId.value) popoverVisible.value = false
    return
  }
  const node = renderLayout.value.nodes.find((item) => personalNodeId(item) === selectedId)
  if (node) {
    placePopover(node)
    emitRelationContext(node)
    if (selectedId !== previousSelectedId) popoverVisible.value = true
  }
}, { flush: 'post' })

onBeforeUnmount(() => {
  stopPopoverDrag()
})

function setScale(value: number) {
  scale.value = Math.min(1.15, Math.max(0.7, value))
}
</script>

<template>
  <section class="graph-panel" aria-label="能力关系图谱">
    <div class="graph-toolbar">
      <div class="tree-mode-switch" aria-label="技能树类型">
        <span class="segmented-slider" :style="treeModeSliderStyle" aria-hidden="true" />
        <button type="button" :class="{ active: treeMode === 'personal' }" @click="treeMode = 'personal'">个人技能树</button>
        <button type="button" :class="{ active: treeMode === 'direction' }" @click="treeMode = 'direction'">方向技能树</button>
        <button type="button" :class="{ active: treeMode === 'path' }" @click="treeMode = 'path'">路径技能树</button>
      </div>
      <div class="graph-actions">
        <div class="orientation-switch" aria-label="布局方向">
          <span class="segmented-slider" :style="orientationSliderStyle" aria-hidden="true" />
          <button type="button" :class="{ active: orientation === 'lr' }" title="从左到右" aria-label="从左到右布局" @click="orientation = 'lr'"><ArrowRight :size="16" /></button>
          <button type="button" :class="{ active: orientation === 'tb' }" title="从上到下" aria-label="从上到下布局" @click="orientation = 'tb'"><ArrowDown :size="16" /></button>
        </div>
        <span class="toolbar-divider" />
        <button class="icon-button" :class="{ active: batchMode }" type="button" aria-label="批量删除技能" :title="batchMode ? '退出批量选择' : '批量删除'" @click="toggleBatchMode"><Trash2 :size="17" /></button>
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
      <Transition name="subbar-content" mode="out-in">
      <div v-if="batchMode" key="batch" class="batch-delete-bar">
        <span>已选择 <strong>{{ batchSelectedIds.length }}</strong> 项</span>
        <button v-if="listMode" type="button" class="text-button compact" :disabled="!selectableVisibleIds.length" @click="toggleVisibleSelection">{{ allVisibleSelected ? '清空当前列表' : '选择当前列表' }}</button>
        <button type="button" class="text-button compact" @click="toggleBatchMode"><X :size="15" />取消</button>
        <button type="button" class="danger-action compact" :disabled="!batchSelectedIds.length" @click="deleteSelectedNodes"><Trash2 :size="15" />删除所选</button>
      </div>
      <div v-else-if="treeMode === 'direction'" key="direction" class="direction-picker" aria-label="选择展示方向">
        <span>展示方向</span>
        <TransitionGroup name="direction-option">
          <label v-for="option in directionOptions" :key="option.id" :class="{ active: selectedTrackIds.includes(option.id) }">
            <input type="checkbox" :checked="selectedTrackIds.includes(option.id)" @change="toggleTrack(option.id)" />
            {{ option.label }}
          </label>
        </TransitionGroup>
      </div>
      <div v-else :key="treeMode" class="view-description">{{ treeMode === 'personal' ? '全部技能积累 · 仅显示主要前置关系' : '目标能力的前置学习链' }}</div>
      </Transition>
      <div class="legend" aria-label="节点状态图例">
        <span><i class="dot mastered" />已掌握</span>
        <span><i class="dot developing" />进行中</span>
        <span><i class="dot gap" />缺口</span>
        <span><i class="dot target" />目标</span>
      </div>
    </div>

    <Transition name="graph-view" mode="out-in">
    <div v-if="listMode" key="list" class="skill-list" :class="{ 'batch-mode': batchMode }" @click.self="clearSelection">
      <button
        v-for="(node, nodeIndex) in renderLayout.nodes"
        :key="node.id"
        type="button"
        class="skill-list-item"
        :class="{
          selected: !batchMode && popoverVisible && selectedId === (node.selectId ?? node.id),
          'batch-selected': batchSelectedIds.includes(personalNodeId(node) ?? ''),
          'batch-disabled': batchMode && !personalNodeId(node),
        }"
        :aria-pressed="batchMode && personalNodeId(node) ? batchSelectedIds.includes(personalNodeId(node) ?? '') : undefined"
        :aria-disabled="batchMode && !personalNodeId(node)"
        :style="{ animationDelay: `${nodeIndex * 30}ms` }"
        @click.stop="selectNode(node)"
      >
        <i v-if="batchMode" class="list-batch-check" aria-hidden="true"><Check v-if="batchSelectedIds.includes(personalNodeId(node) ?? '')" :size="12" /></i>
        <span class="node-status" :class="node.status">{{ node.status === 'mastered' ? '已掌握' : node.status === 'developing' ? '进行中' : node.status === 'target' ? '目标' : '缺口' }}</span>
        <strong>{{ node.name }}</strong>
        <small>{{ node.meta ?? `等级 ${node.level}/4` }}</small>
      </button>
    </div>

    <div v-else key="graph" class="graph-viewport" @click.self="clearSelection">
      <div v-if="!renderLayout.nodes.length" class="graph-empty">至少选择一个方向</div>
      <div v-else class="graph-canvas" @click.self="clearSelection" :class="[`orientation-${orientation}`, `mode-${treeMode}`]" :style="{ transform: `scale(${scale})`, width: `${renderLayout.width}px`, height: `${renderLayout.height}px` }">
        <div class="graph-columns" aria-hidden="true"><span v-for="column in renderLayout.columns" :key="column.depth" :style="{ left: `${column.x}px`, top: `${column.y}px` }">{{ column.label }}</span></div>
        <div v-for="(lane, laneIndex) in renderLayout.lanes" :key="lane.id" class="graph-lane" :style="{ left: `${lane.x}px`, top: `${lane.y}px`, width: `${lane.width}px`, height: `${lane.height}px`, animationDelay: `${laneIndex * 55}ms` }" aria-hidden="true"><span>{{ lane.label }}</span></div>
        <svg class="edges" :width="renderLayout.width" :height="renderLayout.height" :viewBox="`0 0 ${renderLayout.width} ${renderLayout.height}`" aria-hidden="true">
          <defs><marker id="arrow" viewBox="0 0 10 10" refX="8" refY="5" markerWidth="6" markerHeight="6" orient="auto-start-reverse"><path d="M 0 0 L 10 5 L 0 10 z" /></marker></defs>
          <path v-for="(line, lineIndex) in lineData" :key="`${line.from}-${line.to}`" :d="line.path" :class="[line.type, { 'cross-direction': line.crossDirection }]" :style="{ animationDelay: `${lineIndex * 24}ms` }" marker-end="url(#arrow)" />
        </svg>
        <button v-for="(node, nodeIndex) in renderLayout.nodes" :key="node.id" type="button" class="skill-node" :class="[node.status, { selected: !batchMode && popoverVisible && (selectedId === (node.selectId ?? node.id) || previewNodeId === node.id), 'batch-selectable': batchMode && personalNodeId(node), 'batch-selected': batchSelectedIds.includes(personalNodeId(node) ?? ''), 'catalog-node': node.catalogNode, 'catalog-previewable': node.catalogNode && !node.selectId }]" :style="{ left: `${node.x}px`, top: `${node.y}px`, animationDelay: `${nodeIndex * 28}ms` }" @click.stop="selectNode(node)">
          <i v-if="batchMode && personalNodeId(node)" class="batch-check" aria-hidden="true"><Check v-if="batchSelectedIds.includes(personalNodeId(node) ?? '')" :size="12" /></i>
          <span>{{ node.name }}</span>
          <small>{{ node.meta ? `${node.meta} · ${node.selectId ? '个人已有' : '待学习'}` : `${node.level}/4 · ${node.status === 'mastered' ? '已掌握' : node.status === 'developing' ? '进行中' : node.status === 'target' ? '目标' : '缺口'}` }}</small>
        </button>
        <Transition name="node-popover">
          <div
            v-if="popoverVisible && $slots.inspector"
            ref="popoverElement"
            class="graph-node-popover"
            :class="{ dragging: draggingPopover }"
            :style="{ left: `${popoverPosition.x}px`, top: `${popoverPosition.y}px` }"
            @click.stop
          >
            <slot name="inspector" :start-drag="startPopoverDrag" :close-popover="clearSelection" />
          </div>
        </Transition>
      </div>
    </div>
    </Transition>
  </section>
</template>
