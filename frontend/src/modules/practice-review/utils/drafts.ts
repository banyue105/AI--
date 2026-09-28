import { nextTick, ref, watch, type Ref } from 'vue'

export function useDraft<T extends object>(key: string, initial: T, valid: (value: unknown) => boolean) {
  let value = initial
  let didRestore = false
  try {
    const saved = JSON.parse(localStorage.getItem(key) || 'null')
    if (valid(saved)) { value = saved as T; didRestore = true }
  } catch { /* Draft storage is optional; server data remains authoritative. */ }
  const form = ref(value) as Ref<T>
  const restored = ref(didRestore)
  let cleared = false
  watch(form, data => { if (!cleared) try { localStorage.setItem(key, JSON.stringify(data)) } catch { /* Keep the form in memory. */ } }, { deep: true })
  function clear() {
    cleared = true; restored.value = false
    try { localStorage.removeItem(key) } catch { /* Optional storage. */ }
    void nextTick(() => { cleared = false })
  }
  return { form, restored, clear }
}

export function codePoints(value: string) { return Array.from(value).length }
export function dateLabel(value: string) { return new Intl.DateTimeFormat('zh-CN', { month: 'numeric', day: 'numeric', hour: '2-digit', minute: '2-digit' }).format(new Date(value)) }
export const verdictLabels = { supported: '材料支持', insufficient: '证据不足', conflicting: '存在矛盾' } as const
