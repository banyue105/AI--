<script setup lang="ts">
import { nextTick, onMounted, onUnmounted, ref } from 'vue'
import { X } from 'lucide-vue-next'
const props = defineProps<{ title: string; busy?: boolean; error?: string }>()
const emit = defineEmits<{ close: [] }>()
const element = ref<HTMLElement | null>(null)
const titleId = `practice-dialog-${crypto.randomUUID()}`
let previousFocus: HTMLElement | null = null
function close() { if (!props.busy) emit('close') }
function keyboard(event: KeyboardEvent) {
  if (event.key === 'Escape') { event.preventDefault(); close() }
  if (event.key !== 'Tab') return
  const controls = [...(element.value?.querySelectorAll<HTMLElement>('button:not(:disabled), input:not(:disabled), select:not(:disabled), textarea:not(:disabled), a[href], [tabindex="0"]') || [])].filter(item => item.offsetParent !== null)
  const first = controls[0], last = controls.at(-1)
  if (!first) { event.preventDefault(); element.value?.focus(); return }
  if (document.activeElement === element.value || !element.value?.contains(document.activeElement)) {
    event.preventDefault(); (event.shiftKey ? last : first)?.focus(); return
  }
  if (event.shiftKey && document.activeElement === first) { event.preventDefault(); last?.focus() }
  else if (!event.shiftKey && document.activeElement === last) { event.preventDefault(); first?.focus() }
}
onMounted(async () => {
  previousFocus = document.activeElement as HTMLElement
  await nextTick(); (element.value?.querySelector<HTMLElement>('[data-autofocus]:not(:disabled), input:not(:disabled), textarea:not(:disabled), select:not(:disabled)') || element.value)?.focus()
  document.addEventListener('keydown', keyboard)
})
onUnmounted(() => { document.removeEventListener('keydown', keyboard); if (previousFocus?.isConnected) previousFocus.focus() })
</script>

<template>
  <Teleport to="body">
    <div class="modal-backdrop practice-modal" @click.self="close">
      <section ref="element" class="editor-dialog practice-dialog" role="dialog" aria-modal="true" :aria-labelledby="titleId" tabindex="-1">
        <div class="dialog-header">
          <div><p class="eyebrow">实践验证与复盘</p><h2 :id="titleId">{{ title }}</h2></div>
          <button type="button" class="icon-button" aria-label="关闭弹窗" title="关闭弹窗" :disabled="busy" @click="close"><X :size="19" /></button>
        </div>
        <p v-if="error" class="practice-alert" role="alert">{{ error }}</p>
        <fieldset class="practice-dialog-body" :disabled="busy"><slot /></fieldset>
      </section>
    </div>
  </Teleport>
</template>
