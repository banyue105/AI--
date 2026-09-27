<script setup lang="ts">
import { GitBranch } from 'lucide-vue-next'
import type { DecisionScenario } from '../types'

defineProps<{ scenario: DecisionScenario; selectedNodeId: string; selectedRelationId: string }>()
const emit = defineEmits<{ node: [id: string]; relation: [id: string] }>()
</script>

<template>
  <section class="decision-model" aria-labelledby="decision-model-title">
    <div class="decision-section-heading">
      <div><p class="eyebrow">现实状态模型</p><h2 id="decision-model-title">条件关系</h2></div>
      <span>{{ scenario.nodes.length }} 个节点 · {{ scenario.relations.length }} 条关系</span>
    </div>
    <div class="decision-graph-panel">
      <div class="decision-graph-toolbar"><span><GitBranch :size="15" /> 条件 → 目标 → 影响</span><small>可横向滑动 · 选择节点查看依据</small></div>
      <div class="decision-graph-scroll" tabindex="0" aria-label="可横向滚动的条件关系图">
        <div class="decision-graph-flow">
          <div class="decision-graph-column">
            <span class="decision-graph-caption">现实条件</span>
            <button v-for="node in scenario.nodes.filter(item => item.type === 'constraint' || item.type === 'resource')" :key="node.id"
              type="button" class="decision-graph-node" :class="{ 'decision-selected': selectedNodeId === node.id }"
              @click="emit('node', node.id)"><small>{{ node.type === 'constraint' ? '约束' : '资源' }}</small><strong>{{ node.label }}</strong></button>
          </div>
          <div class="decision-graph-arrow" aria-hidden="true">→</div>
          <div class="decision-graph-column decision-graph-center">
            <span class="decision-graph-caption">目标</span>
            <button v-for="node in scenario.nodes.filter(item => item.type === 'goal')" :key="node.id"
              type="button" class="decision-graph-node decision-goal-node" :class="{ 'decision-selected': selectedNodeId === node.id }"
              @click="emit('node', node.id)"><small>目标</small><strong>{{ node.label }}</strong></button>
          </div>
          <div class="decision-graph-arrow" aria-hidden="true">→</div>
          <div class="decision-graph-column">
            <span class="decision-graph-caption">变更与后果</span>
            <button v-for="node in scenario.nodes.filter(item => item.type === 'task' || item.type === 'risk')" :key="node.id"
              type="button" class="decision-graph-node" :class="{ 'decision-selected': selectedNodeId === node.id, 'decision-risk-node': node.type === 'risk' }"
              @click="emit('node', node.id)"><small>{{ node.type === 'task' ? '任务' : '风险' }}</small><strong>{{ node.label }}</strong></button>
          </div>
        </div>
      </div>
      <div class="decision-relations">
        <strong>影响关系 · 可选中编辑</strong>
        <div class="decision-relation-list">
          <button v-for="relation in scenario.relations" :key="relation.id" type="button" class="decision-relation"
            :class="{ 'decision-selected': selectedRelationId === relation.id }" @click="emit('relation', relation.id)">
            <span>{{ scenario.nodes.find(node => node.id === relation.from)?.label || relation.from }}</span>
            <em>→ {{ relation.label }} →</em>
            <span>{{ scenario.nodes.find(node => node.id === relation.to)?.label || relation.to }}</span>
          </button>
        </div>
      </div>
    </div>
  </section>
</template>
