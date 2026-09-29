<script setup lang="ts">
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { ArrowUpRight, Menu, X } from 'lucide-vue-next'
import { RouterLink, useRoute } from 'vue-router'
import logo from '../assets/tuojie-logo.svg'
import { moduleIsActive, presentedModules } from './modulePresentation'
import './navigation.css'

const route = useRoute()
const open = ref(false)
const trigger = ref<HTMLButtonElement | null>(null)
const header = ref<HTMLElement | null>(null)

function close(restoreFocus = false) {
  open.value = false
  if (restoreFocus) nextTick(() => trigger.value?.focus())
}
function onKeydown(event: KeyboardEvent) {
  if (open.value && event.key === 'Escape') { event.preventDefault(); close(true) }
}
function onPointerDown(event: PointerEvent) {
  if (open.value && event.target instanceof Node && !header.value?.contains(event.target)) close()
}
function onFocusOut(event: FocusEvent) {
  if (open.value && event.relatedTarget instanceof Node && !header.value?.contains(event.relatedTarget)) close()
}
function onResize() { if (window.innerWidth > 900) close() }
watch(() => route.fullPath, () => close())
onMounted(() => {
  document.addEventListener('keydown', onKeydown)
  document.addEventListener('pointerdown', onPointerDown)
  window.addEventListener('resize', onResize)
})
onBeforeUnmount(() => {
  document.removeEventListener('keydown', onKeydown)
  document.removeEventListener('pointerdown', onPointerDown)
  window.removeEventListener('resize', onResize)
})
</script>

<template>
  <a class="app-skip-link" href="#page-content">跳到主要内容</a>
  <header ref="header" class="app-nav" @focusout="onFocusOut">
    <div class="app-nav-inner">
      <RouterLink class="app-nav-brand" to="/" aria-label="拓界，返回首页" @click="close()">
        <img :src="logo" width="118" height="32" alt="拓界" />
      </RouterLink>
      <span class="app-nav-tagline">个人成长与决策工作台</span>
      <nav class="app-nav-desktop" aria-label="全站导航">
        <RouterLink to="/" class="app-nav-link" :class="{ 'is-active': route.path === '/' }" :aria-current="route.path === '/' ? 'page' : undefined">首页</RouterLink>
        <RouterLink v-for="module in presentedModules" :key="module.id" :to="module.route" class="app-nav-link" :class="{ 'is-active': moduleIsActive(module.id, route.path) }" :aria-current="moduleIsActive(module.id, route.path) ? 'page' : undefined">{{ module.title }}</RouterLink>
      </nav>
      <button ref="trigger" class="icon-button app-nav-toggle" type="button" :aria-label="open ? '关闭导航菜单' : '打开导航菜单'" :aria-expanded="open" aria-controls="app-mobile-menu" @click="open = !open">
        <X v-if="open" :size="20" /><Menu v-else :size="20" />
      </button>
    </div>
    <nav v-if="open" id="app-mobile-menu" class="app-nav-mobile" aria-label="手机全站导航">
      <RouterLink to="/" class="app-nav-mobile-link" :class="{ 'is-active': route.path === '/' }" :aria-current="route.path === '/' ? 'page' : undefined" @click="close()"><span>首页</span><ArrowUpRight :size="16" /></RouterLink>
      <RouterLink v-for="module in presentedModules" :key="module.id" :to="module.route" class="app-nav-mobile-link" :class="{ 'is-active': moduleIsActive(module.id, route.path) }" :aria-current="moduleIsActive(module.id, route.path) ? 'page' : undefined" @click="close()"><span><component :is="module.icon" :size="17" />{{ module.title }}</span><ArrowUpRight :size="16" /></RouterLink>
    </nav>
  </header>
</template>
