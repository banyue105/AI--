<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowRight, CheckCircle2, MapPin, Target } from 'lucide-vue-next'
import { careerService } from './careerService'
import type { CareerJobMatch, CareerPreference } from './types'

const router = useRouter()
const matches = ref<CareerJobMatch[]>([])
const preference = ref<CareerPreference>({ category: '', city: '', targetJobId: null })
const category = ref('')
const city = ref('')
const loading = ref(true)
const busy = ref(false)
const error = ref('')
const notice = ref('')
const categories = computed(() => [...new Set(matches.value.map(item => item.job.category))])
const cities = computed(() => [...new Set(matches.value.map(item => item.job.city))])
const top = computed(() => matches.value[0])

async function load() {
  loading.value = true
  error.value = ''
  try {
    const [items, pref] = await Promise.all([careerService.matches(), careerService.preferences()])
    matches.value = items
    preference.value = pref
    category.value = pref.category
    city.value = pref.city
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '岗位匹配暂时无法加载。'
  } finally { loading.value = false }
}

async function savePreference() {
  busy.value = true
  error.value = ''
  try {
    preference.value = await careerService.savePreferences(category.value, city.value)
    matches.value = await careerService.matches()
    notice.value = '职业意向已保存，匹配结果已更新。'
  } catch (cause) { error.value = cause instanceof Error ? cause.message : '保存失败。' }
  finally { busy.value = false }
}

async function setTarget(jobId: string) {
  busy.value = true
  error.value = ''
  try {
    preference.value = await careerService.setTarget(jobId)
    notice.value = '目标岗位已保存，可继续分析 JD。'
  } catch (cause) { error.value = cause instanceof Error ? cause.message : '设置失败。' }
  finally { busy.value = false }
}

onMounted(load)
</script>

<template>
  <section class="career-page">
    <div class="career-page-heading"><div><p class="eyebrow">01 / 岗位匹配</p><h2>找到与你当前能力接近的岗位</h2><p class="career-page-description">先设定职业意向，再查看各岗位的匹配依据与能力缺口。</p></div><span v-if="!loading" class="career-count career-count-ready"><CheckCircle2 :size="14" />{{ matches.length }} 个岗位已对照</span></div>
    <div v-if="loading" class="career-state">正在对照能力图谱与岗位要求…</div>
    <template v-else>
      <div v-if="error" class="career-alert" role="alert">{{ error }} <button type="button" @click="load">重试</button></div>
      <p v-if="notice" class="career-notice" role="status">{{ notice }}</p>
      <article class="career-overview-card career-top-card">
        <div v-if="top" class="career-top-detail">
          <div><span class="career-label">当前最接近</span><strong>{{ top.job.title }}</strong><p>{{ top.job.company }} · {{ top.job.city }}</p><div class="career-top-evidence"><span class="matched"><CheckCircle2 :size="14" />{{ top.match.matched.length }} 项已有匹配</span><span class="gap"><Target :size="14" />{{ top.match.gaps.length }} 项待补齐</span></div><p class="career-top-hint">依据当前能力图谱与岗位技能要求计算</p></div>
          <div class="career-top-score"><b>{{ top.match.score }}%</b><span>参考匹配度</span></div>
        </div>
        <p v-else>暂无可匹配的岗位。</p>
      </article>
      <form class="career-preference-band" @submit.prevent="savePreference">
        <div class="career-preference-copy"><span class="career-panel-icon"><Target :size="18" /></span><div><strong>设置职业意向</strong><small>选定方向和城市，更新岗位排序</small></div></div>
        <div class="career-field-row">
          <label>方向<select v-model="category"><option value="">不限方向</option><option v-for="item in categories" :key="item">{{ item }}</option></select></label>
          <label>城市<select v-model="city"><option value="">不限城市</option><option v-for="item in cities" :key="item">{{ item }}</option></select></label>
        </div>
        <button type="submit" class="primary-button" :disabled="busy">保存并更新匹配</button>
      </form>
      <div class="career-section-heading"><div><h3>匹配结果</h3><p>按参考匹配度排序；方向和城市相同时各加 5 分。</p></div><button type="button" class="text-button compact" @click="router.push('/module4/jobs')">前往岗位筛选 <ArrowRight :size="15" /></button></div>
      <div class="career-match-grid">
        <article v-for="item in matches" :key="item.job.id" class="career-job-card" :class="{ 'is-target': preference.targetJobId === item.job.id }">
          <div class="career-job-identity"><span class="career-label">{{ item.job.category }} · {{ item.job.jobType }}</span><h3>{{ item.job.title }} <span v-if="preference.targetJobId === item.job.id" class="career-target-badge">当前目标</span></h3><p>{{ item.job.company }}</p><div class="career-meta"><span><MapPin :size="14" />{{ item.job.city }}</span><span>{{ item.job.salary }}</span></div><div class="career-chips"><span v-for="skill in item.job.skillTags" :key="skill">{{ skill }}</span></div></div>
          <div class="career-comparison"><div><strong><CheckCircle2 :size="14" /> 已有匹配</strong><p>{{ item.match.matched.length ? item.match.matched.slice(0, 2).join('、') : '暂无已掌握的对应技能' }}</p></div><div><strong><Target :size="14" /> 优先补齐</strong><p>{{ item.match.gaps.length ? item.match.gaps.slice(0, 2).join('；') : '岗位技能要求已覆盖' }}</p></div></div>
          <div class="career-job-outcome"><div class="career-score"><strong>{{ item.match.score }}%</strong><small>参考匹配</small></div><div class="career-card-actions"><button type="button" class="secondary-button compact" :disabled="busy || preference.targetJobId === item.job.id" @click="setTarget(item.job.id)"><CheckCircle2 v-if="preference.targetJobId === item.job.id" :size="15" /><Target v-else :size="15" />{{ preference.targetJobId === item.job.id ? '当前目标' : '设为目标' }}</button><button type="button" class="text-button compact" @click="router.push({ path: '/module4/jd', query: { jobId: item.job.id } })">分析 JD <ArrowRight :size="15" /></button></div></div>
        </article>
      </div>
    </template>
  </section>
</template>
