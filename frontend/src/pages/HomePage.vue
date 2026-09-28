<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowRight, BrainCircuit, ClipboardCheck, Clock3, GitBranch, Target } from 'lucide-vue-next'
import tuojieLogo from '../assets/tuojie-logo.svg'
import { homeService, type HomeData } from '../core/api/homeService'

const router = useRouter()
const data = ref<HomeData | null>(null)
const loading = ref(true)
const error = ref('')

const dateLabel = computed(() => new Intl.DateTimeFormat('zh-CN', { month: 'long', day: 'numeric', weekday: 'short' }).format(new Date()))

async function load() {
  loading.value = true
  error.value = ''
  try {
    data.value = await homeService.getHome()
  } catch {
    error.value = '暂时无法载入工作台，请重试。'
  } finally {
    loading.value = false
  }
}

function openModule(route: string, status: string) {
  if (status !== 'pending' && route) router.push(route)
}

onMounted(load)
</script>

<template>
  <main class="home-shell">
    <header class="home-header">
      <div class="brand-mark"><img :src="tuojieLogo" alt="拓界团队" /></div>
      <time>{{ dateLabel }}</time>
    </header>

    <div v-if="loading" class="state-panel">
      <span class="spinner" />
      <p>正在整理你的成长状态…</p>
    </div>
    <div v-else-if="error" class="state-panel error-state">
      <p>{{ error }}</p>
      <button class="secondary-button" type="button" @click="load">重新加载</button>
    </div>

    <template v-else-if="data">
      <section class="profile-band">
        <div class="profile-copy">
          <p class="eyebrow">个人成长工作台</p>
          <h1>晚上好，{{ data.profile.name }}</h1>
          <p>你正在把零散经验，变成一条看得见的成长路径。</p>
        </div>
        <div class="avatar" aria-label="用户头像">{{ data.profile.name.slice(0, 1) }}</div>
      </section>

      <section class="goal-strip">
        <div class="goal-icon"><Target :size="22" /></div>
        <div>
          <span>当前目标</span>
          <strong>{{ data.currentGoal }}</strong>
        </div>
        <ArrowRight :size="20" />
      </section>

      <section class="module-section">
        <div class="section-heading">
          <div>
            <p class="eyebrow">你的工具</p>
            <h2>从哪里继续？</h2>
          </div>
          <span>{{ data.modules.filter((item) => item.status !== 'pending').length }} 个可用</span>
        </div>

        <div class="module-grid">
          <button
            v-for="(module, index) in data.modules"
            :key="module.id"
            type="button"
            class="module-card"
            :class="[module.status, { primary: index === 0 }]"
            :disabled="module.status === 'pending'"
            @click="openModule(module.route, module.status)"
          >
            <span class="module-icon">
              <BrainCircuit v-if="module.id === 'ability-growth'" :size="24" />
              <GitBranch v-else-if="module.id === 'decision-sandbox'" :size="24" />
              <ClipboardCheck v-else :size="24" />
            </span>
            <span class="module-content">
              <span class="module-meta">
                <span class="status-badge" :class="module.status">{{ module.status === 'ready' ? '可使用' : module.status === 'prototype' ? '原型' : '待确定' }}</span>
                <small>{{ module.updatedAt }}</small>
              </span>
              <strong>{{ module.title }}</strong>
              <span>{{ module.description }}</span>
            </span>
            <ArrowRight v-if="module.status !== 'pending'" class="module-arrow" :size="20" />
          </button>
        </div>
      </section>

      <section class="activity-row">
        <Clock3 :size="19" />
        <div><span>最近活动</span><strong>{{ data.recentActivity }}</strong></div>
        <span class="live-dot">已同步</span>
      </section>
    </template>
  </main>
</template>
