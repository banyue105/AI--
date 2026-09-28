<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue'
import { FileText } from 'lucide-vue-next'
import type { Citation, Evidence } from '../types'
const props = defineProps<{ evidence: Evidence | null; citation: Citation | null; historical: boolean }>()
const element = ref<HTMLElement | null>(null)
const lines = computed(() => props.evidence?.content.split('\n') || [])
watch(() => props.citation, async () => { await nextTick(); element.value?.querySelector('.practice-code-highlight')?.scrollIntoView({ block: 'nearest', behavior: 'smooth' }) })
</script>

<template>
  <aside ref="element" class="practice-evidence-viewer" aria-label="证据原文">
    <div class="practice-viewer-heading"><FileText :size="17" /><div><strong>{{ evidence?.title || '核对成果证据' }}</strong><small v-if="evidence">证据版本 {{ evidence.revision }} · {{ historical ? '复盘时的材料快照' : '当前保存的材料' }}</small></div></div>
    <template v-if="evidence">
      <p v-if="citation" class="practice-hint">引用第 {{ citation.startLine }}–{{ citation.endLine }} 行；原文按保存时的版本显示。</p>
      <pre class="practice-code-view" tabindex="0" aria-label="带行号的证据正文"><span v-for="(line, index) in lines" :key="index" class="practice-code-line" :class="{ 'practice-code-highlight': citation && index + 1 >= citation.startLine && index + 1 <= citation.endLine }"><span class="practice-line-number" aria-hidden="true">{{ index + 1 }}</span><code>{{ line || ' ' }}</code></span></pre>
    </template>
    <div v-else class="practice-viewer-empty"><FileText :size="30" /><p>选择一份材料，或点击判定中的引用，查看具体依据。</p></div>
  </aside>
</template>
