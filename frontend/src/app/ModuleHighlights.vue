<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ArrowDown, ChevronDown } from 'lucide-vue-next'
import { RouterLink, useRoute } from 'vue-router'
import type { ModuleId } from '../core/types'
import { modulePresentation } from './modulePresentation'

const props = defineProps<{ moduleId: ModuleId }>()
const route = useRoute()
const module = computed(() => modulePresentation(props.moduleId))
const expanded = ref(route.hash === '#' + module.value.anchor)
watch(() => route.hash, hash => {
  if (hash === '#' + module.value.anchor) expanded.value = true
})
function onToggle(event: Event) { expanded.value = (event.target as HTMLDetailsElement).open }
</script>

<template>
  <section class="app-module-intro" :aria-label="`${module.title}亮点介绍`">
    <details :id="module.anchor" class="app-module-details" :open="expanded" @toggle="onToggle">
      <summary class="app-module-summary" data-anchor-focus>
        <component :is="module.icon" :size="17" />
        <span>{{ module.title }} · 亮点介绍</span>
        <small>{{ expanded ? '收起介绍' : '了解这个模块' }}</small>
        <ChevronDown :size="16" />
      </summary>
      <div class="app-module-intro-body">
        <h2>{{ module.highlightTitle }}</h2>
        <p>{{ module.highlightIntro }}</p>
        <ul><li v-for="point in module.points" :key="point">{{ point }}</li></ul>
        <div class="app-module-intro-footer">
          <p>{{ module.note }}</p>
          <RouterLink class="secondary-button compact" :to="{ path: route.path, hash: '#' + module.workspace }">{{ module.entryLabel }}<ArrowDown :size="15" /></RouterLink>
        </div>
      </div>
    </details>
    <div :id="module.workspace" class="app-workspace-anchor" tabindex="-1" :aria-label="`${module.title}工作区`" />
  </section>
</template>
