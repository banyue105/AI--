<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ArrowRight, ArrowUp, ArrowUpRight } from 'lucide-vue-next'
import { RouterLink } from 'vue-router'
import { presentedModules } from '../app/modulePresentation'
import { defaultModuleManifests, homeService } from '../core/api/homeService'
import type { ModuleManifest } from '../core/types'
import banner from '../assets/home/workbench-banner.jpg'
import logo from '../assets/tuojie-logo.svg'
import './home.css'

const manifests = ref<readonly ModuleManifest[]>(defaultModuleManifests)
const statusError = ref('')
const failedImages = ref(new Set<string>())
const modules = computed(() => presentedModules.map(module => ({
  ...module,
  status: manifests.value.find(item => item.id === module.id)?.status ?? module.status,
})))
const statusLabels: Record<ModuleManifest['status'], string> = { ready: '可使用', prototype: '演示原型', pending: '开发中' }

async function loadStatus() {
  statusError.value = ''
  try {
    const data = await homeService.getHome()
    if (!Array.isArray(data.modules)) throw new Error('Invalid module list')
    manifests.value = data.modules
  } catch {
    statusError.value = '模块状态暂时无法更新，你仍可以浏览介绍并进入模块。'
  }
}
function imageFailed(id: string) { failedImages.value = new Set([...failedImages.value, id]) }
onMounted(loadStatus)
</script>

<template>
  <main id="page-content" class="home-page" tabindex="-1">
    <section id="home-top" class="home-hero" aria-labelledby="home-title">
      <div class="home-container home-hero-inner">
        <div class="home-hero-copy">
          <p class="eyebrow">拓界 / AI 个人成长与决策工作台</p>
          <h1 id="home-title" data-anchor-focus tabindex="-1">让成长有路径，<br /><span>让选择有依据。</span></h1>
          <p class="home-hero-description">把目标、经验与现实条件，转化为可编辑的能力图谱、可比较的决策方案和可追溯的实践复盘。再从岗位要求出发，看清下一步的职业方向。</p>
          <div class="home-hero-actions">
            <RouterLink class="primary-button" :to="{ path: '/', hash: '#home-modules' }">探索四大模块<ArrowRight :size="17" /></RouterLink>
            <RouterLink class="secondary-button" to="/ability">开始能力成长<ArrowUpRight :size="16" /></RouterLink>
          </div>
          <p class="home-hero-principle">AI 整理信息，你确认方向，再继续行动。</p>
        </div>
        <figure class="home-hero-visual">
          <img v-if="!failedImages.has('banner')" :src="banner" width="1200" height="880" fetchpriority="high" loading="eager" decoding="async" alt="拓界真实产品界面：个人能力图谱、基准与变更方案对比，以及职业规划演示岗位。" @error="imageFailed('banner')" />
          <div v-else class="home-image-fallback home-banner-fallback"><span>拓界工作台</span><p>认识能力 · 比较选择 · 验证实践 · 规划方向</p></div>
          <figcaption><span><i />真实产品界面预览</span><span>从认识自己，到规划下一步</span></figcaption>
        </figure>
      </div>
    </section>

    <section id="home-modules" class="home-container home-modules" aria-labelledby="home-modules-title">
      <div class="section-heading home-section-heading">
        <div><p class="eyebrow">找到你的入口</p><h2 id="home-modules-title" data-anchor-focus tabindex="-1">从你的问题开始</h2></div>
        <p>认识能力，比较选择，验证实践，规划方向。</p>
      </div>
      <div v-if="statusError" class="home-status-error" role="status"><span>{{ statusError }}</span><button class="text-button compact" type="button" @click="loadStatus">重试</button></div>
      <div class="home-entry-grid">
        <article v-for="module in modules" :key="module.id" class="home-entry">
          <div class="home-entry-meta"><span>{{ module.number }}</span><span class="status-badge" :class="module.status">{{ statusLabels[module.status] }}</span></div>
          <h3><component :is="module.icon" :size="18" />{{ module.title }}</h3>
          <p class="home-entry-question">{{ module.question }}</p>
          <p class="home-entry-description">{{ module.intro }}</p>
          <RouterLink class="home-entry-link" :to="module.route">{{ module.entryLabel }}<ArrowUpRight :size="16" /></RouterLink>
        </article>
      </div>
    </section>

    <section id="home-highlights" class="home-container home-highlights" aria-labelledby="home-highlights-title">
      <div class="section-heading home-section-heading">
        <div><p class="eyebrow">不止一个答案</p><h2 id="home-highlights-title" data-anchor-focus tabindex="-1">看看拓界如何帮助你</h2></div>
        <p>让复杂的信息，变成可以看懂、核对和修改的依据。</p>
      </div>
      <article v-for="module in modules" :key="module.id" class="home-feature" :data-module="module.id">
        <div class="home-feature-copy">
          <p class="home-feature-kicker"><span>{{ module.number }}</span><component :is="module.icon" :size="16" />{{ module.title }}</p>
          <h3>{{ module.highlightTitle }}</h3>
          <p class="home-feature-description">{{ module.highlightIntro }}</p>
          <ul class="home-feature-points"><li v-for="(point, index) in module.points" :key="point"><span>{{ String(index + 1).padStart(2, '0') }}</span><p>{{ point }}</p></li></ul>
        </div>
        <figure class="home-preview">
          <div class="home-preview-bar"><span>{{ module.imageLabel }}</span><small>界面预览</small></div>
          <img v-if="!failedImages.has(module.id)" :src="module.image" width="1232" :height="module.imageHeight" :alt="module.imageAlt" loading="lazy" decoding="async" @error="imageFailed(module.id)" />
          <div v-else class="home-image-fallback"><component :is="module.icon" :size="28" /><span>{{ module.title }}界面预览暂时无法显示</span><p>点击下方按钮，继续查看模块介绍。</p></div>
          <figcaption>{{ module.note }}</figcaption>
        </figure>
        <div class="home-feature-action"><RouterLink class="secondary-button" :to="{ path: module.route, hash: '#' + module.anchor }">{{ module.highlightLabel }}<ArrowRight :size="16" /></RouterLink></div>
      </article>
    </section>

    <footer class="home-container home-footer">
      <div><img :src="logo" width="89" height="24" alt="拓界" /><p>让成长有路径，让选择有依据。</p></div>
      <RouterLink class="text-button" :to="{ path: '/', hash: '#home-top' }">回到顶部<ArrowUp :size="15" /></RouterLink>
    </footer>
  </main>
</template>
