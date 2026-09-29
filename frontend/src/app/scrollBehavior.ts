import { nextTick } from 'vue'
import type { RouterScrollBehavior } from 'vue-router'

let navigation = 0

async function findTarget(id: string, current: number): Promise<HTMLElement | null> {
  await nextTick()
  const existing = document.getElementById(id)
  if (existing) return existing
  // Lazy routes can mount after the router's tick. Observe mounting, with a
  // bounded fallback and cancellation when another navigation supersedes it.
  return new Promise(resolve => {
    const finish = (target: HTMLElement | null) => {
      observer.disconnect()
      window.clearTimeout(timeout)
      resolve(target)
    }
    const observer = new MutationObserver(() => {
      if (current !== navigation) return finish(null)
      const target = document.getElementById(id)
      if (target) finish(target)
    })
    const timeout = window.setTimeout(() => finish(null), 3000)
    observer.observe(document.getElementById('app') ?? document.body, { childList: true, subtree: true })
    const target = document.getElementById(id)
    if (target) finish(target)
  })
}

export const scrollBehavior: RouterScrollBehavior = async (to, _from, savedPosition) => {
  const current = ++navigation
  if (savedPosition) return { ...savedPosition, behavior: 'instant' }
  if (!to.hash) {
    await nextTick()
    if (current !== navigation) return false
    document.getElementById('page-content')?.focus({ preventScroll: true })
    return { top: 0, behavior: 'instant' }
  }

  let id: string
  try { id = decodeURIComponent(to.hash.slice(1)) } catch { return { top: 0 } }
  const target = await findTarget(id, current)
  if (!target || current !== navigation) return false
  if (target instanceof HTMLDetailsElement) target.open = true
  await nextTick()
  await new Promise<void>(resolve => requestAnimationFrame(() => resolve()))
  if (current !== navigation) return false

  const focusTarget = target.querySelector<HTMLElement>('[data-anchor-focus]') ?? target
  if (!focusTarget.hasAttribute('tabindex') && !focusTarget.matches('summary, a, button, input, select, textarea')) focusTarget.setAttribute('tabindex', '-1')
  focusTarget.focus({ preventScroll: true })
  const margin = Number.parseFloat(getComputedStyle(target).scrollMarginTop) || 0
  return { el: target, top: margin, behavior: window.matchMedia('(prefers-reduced-motion: reduce)').matches ? 'instant' : 'smooth' }
}
