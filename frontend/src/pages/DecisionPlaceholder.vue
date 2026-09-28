<script setup lang="ts">
import { ArrowLeft, GitBranch } from 'lucide-vue-next'
</script>

<template>
  <main class="placeholder-page">
    <RouterLink class="back-link" to="/"><ArrowLeft :size="18" /> 返回工作台</RouterLink>
    <div class="placeholder-icon"><GitBranch :size="30" /></div>
    <p class="eyebrow">模块二 · 独立开发中</p>
    <h1>决策沙盒</h1>
    <p>这里已保留稳定的路由入口，后续将接入条件建模、方案推演与分支比较。</p>
  </main>
</template>
