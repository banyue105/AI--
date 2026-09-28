<script setup lang="ts">
import { ArrowLeft, BriefcaseBusiness, FileSearch, ListFilter, Sparkles } from 'lucide-vue-next'
import { RouterLink, RouterView } from 'vue-router'
import './career.css'

const tabs = [
  { to: '/module4/match', label: '岗位匹配', icon: Sparkles },
  { to: '/module4/jobs', label: '岗位筛选', icon: ListFilter },
  { to: '/module4/jd', label: 'JD 分析', icon: FileSearch },
]
</script>

<template>
  <main class="career-shell">
    <header class="career-header">
      <div class="career-header-inner">
        <RouterLink class="career-back" to="/" aria-label="返回工作台"><ArrowLeft :size="18" /></RouterLink>
        <span class="page-icon"><BriefcaseBusiness :size="20" /></span>
        <div class="career-header-title"><strong>职业规划</strong><small>从能力出发，找到下一步</small></div>
        <span class="career-demo-label">本地演示岗位库</span>
      </div>
    </header>
    <div class="career-content">
      <nav class="career-tabs" aria-label="职业规划子页面">
        <RouterLink v-for="tab in tabs" :key="tab.to" :to="tab.to" :class="{ active: $route.path === tab.to }">
          <component :is="tab.icon" :size="17" />{{ tab.label }}
        </RouterLink>
      </nav>
      <RouterView />
      <p class="career-disclaimer">岗位和公司均为演示数据；匹配度根据当前能力图谱与岗位技能词计算，仅供规划参考。</p>
    </div>
  </main>
</template>
