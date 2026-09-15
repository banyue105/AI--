<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import {
  ArrowLeft,
  ArrowRight,
  Bot,
  Check,
  CheckCircle2,
  CircleAlert,
  FileCheck2,
  Info,
  Layers3,
  Network,
  Pencil,
  Plus,
  RefreshCw,
  Route,
  Save,
  Sparkles,
  Target,
  X,
} from 'lucide-vue-next'
import AbilityGraph from './components/AbilityGraph.vue'
import KnowledgeCatalog from './components/KnowledgeCatalog.vue'
import { useAbilityStore } from './stores/abilityStore'
import type { SkillLevel, SkillNode, SkillStatus } from './types'
import {
  knowledgeCatalogService,
  type KnowledgeStackItem,
  type KnowledgeTrack,
  type KnowledgeTrackId,
} from './services/knowledgeCatalogService'

const store = useAbilityStore()
const quickInput = ref('我会 Python、Linux，了解 TCP/IP，目标是完成网络服务部署')
const showEditor = ref(false)
const knowledgeTracks = ref<KnowledgeTrack[]>([])
const selectedKnowledgeTrackId = ref<KnowledgeTrackId>('network')
const generatingKnowledge = ref(false)
const knowledgeGenerationError = ref('')
const levelLabels = ['不了解', '了解', '能实践', '熟练', '可指导']
const statusOptions: Array<{ value: SkillStatus; label: string }> = [
  { value: 'mastered', label: '已掌握' },
  { value: 'developing', label: '进行中' },
  { value: 'gap', label: '能力缺口' },
  { value: 'target', label: '目标能力' },
]
const form = reactive({ id: '', name: '', description: '', level: 1 as SkillLevel, status: 'developing' as SkillStatus })

const deadlineDays = computed(() => {
  if (!store.graph?.goal.deadline) return null
  return Math.max(0, Math.ceil((new Date(store.graph.goal.deadline).getTime() - Date.now()) / 86400000))
})
const selectedEvidence = computed(() => store.evidenceForSelected)

function nodeName(id: string) {
  return store.graph?.nodes.find((node) => node.id === id)?.name ?? id
}

function openNewNode() {
  Object.assign(form, { id: '', name: '', description: '', level: 1, status: 'developing' })
  showEditor.value = true
}

function openEditNode() {
  if (!store.selectedNode) return
  Object.assign(form, store.selectedNode)
  showEditor.value = true
}

function openCatalogSkill(item: KnowledgeStackItem) {
  Object.assign(form, {
    id: '',
    name: item.name,
    description: item.description,
    level: 0,
    status: 'developing',
  })
  showEditor.value = true
}

function selectCatalogSkill(id: string) {
  store.selectedId = id
  window.setTimeout(() => document.querySelector('.workspace-grid')?.scrollIntoView({ behavior: 'smooth', block: 'start' }), 100)
}

async function generateKnowledgeTrack(query: string) {
  if (!store.graph) return
  generatingKnowledge.value = true
  knowledgeGenerationError.value = ''
  try {
    const track = await knowledgeCatalogService.generateTrack(query, store.graph.nodes)
    const existingIndex = knowledgeTracks.value.findIndex((item) => item.title === track.title)
    if (existingIndex >= 0) knowledgeTracks.value.splice(existingIndex, 1, track)
    else knowledgeTracks.value.push(track)
    selectedKnowledgeTrackId.value = track.id
  } catch {
    knowledgeGenerationError.value = '暂时无法生成该方向，请稍后重试。'
  } finally {
    generatingKnowledge.value = false
  }
}

async function submitNode() {
  if (!form.name.trim()) return
  const isNew = !form.id
  const node: SkillNode = {
    id: form.id || `skill-${Date.now()}`,
    name: form.name.trim(),
    description: form.description.trim(),
    level: form.level,
    status: form.status,
    evidenceIds: isNew ? [] : store.selectedNode?.evidenceIds ?? [],
    x: isNew ? 0 : store.selectedNode?.x ?? 0,
    y: isNew ? 0 : store.selectedNode?.y ?? 0,
  }
  await store.saveNode(node)
  showEditor.value = false
}

watch(
  () => store.selectedId,
  () => {
    if (window.innerWidth < 720 && store.selectedId) {
      window.setTimeout(() => document.querySelector('.node-inspector')?.scrollIntoView({ behavior: 'smooth', block: 'nearest' }), 100)
    }
  },
)

onMounted(async () => {
  await Promise.all([
    store.load(),
    knowledgeCatalogService.getCatalog().then((catalog) => {
      knowledgeTracks.value = catalog
      if (!catalog.some((track) => track.id === selectedKnowledgeTrackId.value)) {
        selectedKnowledgeTrackId.value = catalog[0]?.id ?? ''
      }
    }),
  ])
})
</script>

<template>
  <main class="ability-shell">
    <header class="ability-header">
      <div class="ability-header-inner">
        <RouterLink to="/" class="icon-button header-back" aria-label="返回工作台" title="返回工作台">
          <ArrowLeft :size="20" />
        </RouterLink>
        <div class="page-title">
          <span class="page-icon"><Network :size="20" /></span>
          <div><strong>能力成长</strong><small>结构化成长工作台</small></div>
        </div>
        <div class="sync-state"><span /> 已保存</div>
        <button class="primary-button compact" type="button" @click="openNewNode"><Plus :size="17" /> 新增能力</button>
      </div>
    </header>

    <div v-if="store.loading" class="state-panel page-loading">
      <span class="spinner" />
      <p>正在构建能力图谱…</p>
    </div>
    <div v-else-if="store.error" class="state-panel error-state page-loading">
      <CircleAlert :size="28" />
      <p>{{ store.error }}</p>
      <button class="secondary-button" type="button" @click="store.load">重新加载</button>
    </div>

    <div v-else-if="store.graph" class="ability-content">
      <section class="ability-overview">
        <div class="overview-copy">
          <p class="eyebrow">当前成长目标</p>
          <h1>{{ store.graph.goal.title }}</h1>
          <div class="goal-meta">
            <span><Target :size="16" /> 目标能力：网络服务部署</span>
            <span v-if="deadlineDays !== null">{{ deadlineDays }} 天后复盘</span>
          </div>
        </div>
        <div class="overview-progress">
          <div class="progress-ring" :style="{ '--progress': `${store.progress * 3.6}deg` }">
            <span><strong>{{ store.progress }}%</strong><small>路径基础</small></span>
          </div>
          <div class="progress-copy"><strong>{{ store.graph.nodes.filter((node) => node.level >= 2).length }}/{{ store.graph.nodes.length }}</strong><span>项能力已有实践基础</span></div>
        </div>
      </section>

      <section class="ai-input-band">
        <div class="ai-label">
          <span><Sparkles :size="18" /></span>
          <div><strong>用一句话更新能力</strong><small>AI 会先整理为候选节点，由你确认后再加入图谱</small></div>
        </div>
        <form class="quick-form" @submit.prevent="store.parse(quickInput)">
          <input v-model="quickInput" aria-label="描述你的能力和目标" placeholder="例如：我会 Python，正在学 Linux，目标是部署网络服务" />
          <button class="primary-button" type="submit" :disabled="store.parsing || !quickInput.trim()">
            <RefreshCw v-if="store.parsing" class="spin-icon" :size="17" />
            <Sparkles v-else :size="17" />
            {{ store.parsing ? '分析中' : '结构化' }}
          </button>
        </form>

        <div v-if="store.parseResult" class="parse-result">
          <div class="parse-summary"><Bot :size="19" /><strong>{{ store.parseResult.summary }}</strong></div>
          <div v-if="store.parseResult.suggestedNodes.length" class="suggestion-list">
            <span v-for="node in store.parseResult.suggestedNodes" :key="node.id"><Check :size="14" /> {{ node.name }} · {{ levelLabels[node.level] }}</span>
          </div>
          <div class="assumption-row"><Info :size="15" /> {{ store.parseResult.assumptions.join('；') }}</div>
          <div class="parse-actions">
            <button class="text-button" type="button" @click="store.parseResult = null">取消</button>
            <button class="secondary-button" type="button" :disabled="!store.parseResult.suggestedNodes.length" @click="store.acceptSuggestions"><Check :size="16" /> 确认加入</button>
          </div>
        </div>
      </section>

      <div class="workspace-grid">
        <section class="graph-section">
          <div class="section-heading compact-heading">
            <div>
              <p class="eyebrow">个人技能树</p>
              <h2>我的当前能力与学习状态</h2>
            </div>
            <div class="tree-stats">
              <span><i class="dot mastered" /><strong>{{ store.graph.nodes.filter((node) => node.status === 'mastered').length }}</strong> 已掌握</span>
              <span><i class="dot developing" /><strong>{{ store.graph.nodes.filter((node) => node.status === 'developing').length }}</strong> 学习中</span>
              <span><i class="dot gap" /><strong>{{ store.graph.nodes.filter((node) => node.status === 'gap' || node.status === 'target').length }}</strong> 待补齐</span>
            </div>
          </div>
          <AbilityGraph
            :nodes="store.graph.nodes"
            :relations="store.graph.relations"
            :selected-id="store.selectedId"
            @select="store.selectedId = $event"
          />
        </section>

        <aside v-if="store.selectedNode" class="node-inspector">
          <div class="inspector-top">
            <span class="node-status" :class="store.selectedNode.status">{{ statusOptions.find((item) => item.value === store.selectedNode?.status)?.label }}</span>
            <button class="icon-button" type="button" aria-label="编辑当前能力" title="编辑能力" @click="openEditNode"><Pencil :size="17" /></button>
          </div>
          <h2>{{ store.selectedNode.name }}</h2>
          <p>{{ store.selectedNode.description || '暂未填写能力说明' }}</p>
          <div class="level-block">
            <div><span>掌握程度</span><strong>{{ levelLabels[store.selectedNode.level] }}</strong></div>
            <div class="level-track" :aria-label="`掌握程度 ${store.selectedNode.level}/4`">
              <i v-for="index in 5" :key="index" :class="{ active: index - 1 <= store.selectedNode.level }" />
            </div>
          </div>
          <div class="evidence-block">
            <h3><FileCheck2 :size="17" /> 实践证据 <span>{{ selectedEvidence.length }}</span></h3>
            <div v-if="selectedEvidence.length" class="evidence-list">
              <div v-for="item in selectedEvidence" :key="item.id">
                <CheckCircle2 :size="17" />
                <span><strong>{{ item.title }}</strong><small>{{ item.note }}</small></span>
              </div>
            </div>
            <p v-else class="empty-evidence">尚无实践证据，当前等级需要后续验证。</p>
          </div>
          <div class="next-action">
            <span><Route :size="17" /></span>
            <div><small>建议下一步</small><strong>{{ store.selectedNode.level >= 2 ? '用一次真实任务验证稳定性' : '完成最小实践并记录过程' }}</strong></div>
          </div>
        </aside>
      </div>

      <KnowledgeCatalog
        :tracks="knowledgeTracks"
        :personal-skills="store.graph.nodes"
        :selected-track-id="selectedKnowledgeTrackId"
        :generating="generatingKnowledge"
        :generation-error="knowledgeGenerationError"
        @update:selected-track-id="selectedKnowledgeTrackId = $event"
        @generate="generateKnowledgeTrack"
        @add="openCatalogSkill"
        @select="selectCatalogSkill"
      />

      <section class="path-section">
        <div class="section-heading compact-heading">
          <div>
            <p class="eyebrow">可解释路径</p>
            <h2>到达目标，还需要补齐什么</h2>
          </div>
          <button class="secondary-button compact" type="button" @click="store.regeneratePath"><RefreshCw :size="16" /> 重新生成</button>
        </div>
        <div class="path-list">
          <article v-for="(step, index) in store.path" :key="step.skillId" class="path-step" :class="step.status">
            <div class="path-marker">
              <Check v-if="step.status === 'done'" :size="16" />
              <span v-else>{{ index + 1 }}</span>
            </div>
            <div class="path-copy">
              <div><strong>{{ nodeName(step.skillId) }}</strong><span>{{ step.status === 'done' ? '已具备' : step.status === 'blocked' ? '等待前置' : '建议下一步' }}</span></div>
              <p>{{ step.reason }}</p>
              <small v-if="step.prerequisiteIds.length">依据：需先具备 {{ step.prerequisiteIds.map(nodeName).join('、') }}</small>
              <small v-else>依据：能力图谱中的目标依赖关系</small>
            </div>
            <ArrowRight :size="18" />
          </article>
        </div>
        <div class="path-assumption"><Info :size="16" /><span><strong>生成假设</strong> 当前路径仅依据你确认的能力等级与前置关系，不代表固定学时或结果保证。</span></div>
      </section>
    </div>

    <Transition name="toast">
      <div v-if="store.notice" class="toast-message"><CheckCircle2 :size="18" /> {{ store.notice }}</div>
    </Transition>

    <Teleport to="body">
      <div v-if="showEditor" class="modal-backdrop" @click.self="showEditor = false">
        <form class="editor-dialog" @submit.prevent="submitNode">
          <div class="dialog-header">
            <div><p class="eyebrow">结构化能力</p><h2>{{ form.id ? '编辑能力节点' : '新增能力节点' }}</h2></div>
            <button class="icon-button" type="button" aria-label="关闭" @click="showEditor = false"><X :size="19" /></button>
          </div>
          <label>能力名称<input v-model="form.name" required maxlength="24" placeholder="例如：Docker 容器化" /></label>
          <label>能力说明<textarea v-model="form.description" rows="3" maxlength="120" placeholder="说明这项能力能解决什么问题" /></label>
          <fieldset>
            <legend>掌握程度</legend>
            <div class="segmented-control five">
              <button v-for="(label, index) in levelLabels" :key="label" type="button" :class="{ active: form.level === index }" @click="form.level = index as SkillLevel">{{ label }}</button>
            </div>
          </fieldset>
          <fieldset>
            <legend>节点状态</legend>
            <div class="segmented-control">
              <button v-for="option in statusOptions" :key="option.value" type="button" :class="{ active: form.status === option.value }" @click="form.status = option.value">{{ option.label }}</button>
            </div>
          </fieldset>
          <div class="dialog-actions">
            <button class="text-button" type="button" @click="showEditor = false">取消</button>
            <button class="primary-button" type="submit"><Save :size="17" /> 保存节点</button>
          </div>
        </form>
      </div>
    </Teleport>
  </main>
</template>
