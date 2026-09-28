<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowRight, MapPin, Search, Target } from 'lucide-vue-next'
import { careerService } from './careerService'
import type { CareerJob, CareerPreference } from './types'

const router = useRouter()
const allJobs = ref<CareerJob[]>([])
const jobs = ref<CareerJob[]>([])
const preference = ref<CareerPreference>({ category: '', city: '', targetJobId: null })
const keyword = ref('')
const category = ref('')
const city = ref('')
const jobType = ref('')
const expanded = ref<string | null>(null)
const loading = ref(true)
const busy = ref(false)
const error = ref('')
const notice = ref('')
const categories = computed(() => [...new Set(allJobs.value.map(item => item.category))])
const cities = computed(() => [...new Set(allJobs.value.map(item => item.city))])
const jobTypes = computed(() => [...new Set(allJobs.value.map(item => item.jobType))])

async function load() {
  loading.value = true
  error.value = ''
  try {
    const [items, pref] = await Promise.all([careerService.jobs(), careerService.preferences()])
    allJobs.value = items
    jobs.value = items
    preference.value = pref
  } catch (cause) { error.value = cause instanceof Error ? cause.message : '岗位库暂时无法加载。' }
  finally { loading.value = false }
}

async function search() {
  busy.value = true
  error.value = ''
  try { jobs.value = await careerService.jobs({ keyword: keyword.value, category: category.value, city: city.value, jobType: jobType.value }) }
  catch (cause) { error.value = cause instanceof Error ? cause.message : '筛选失败。' }
  finally { busy.value = false }
}

async function setTarget(jobId: string) {
  busy.value = true
  error.value = ''
  try {
    preference.value = await careerService.setTarget(jobId)
    notice.value = '目标岗位已保存。'
  } catch (cause) { error.value = cause instanceof Error ? cause.message : '设置失败。' }
  finally { busy.value = false }
}

onMounted(load)
</script>

<template>
  <section class="career-page">
    <div class="career-page-heading"><div><p class="eyebrow">02 / 岗位筛选</p><h2>筛出值得进一步了解的岗位</h2></div><span class="career-count">{{ jobs.length }} / {{ allJobs.length }} 个岗位</span></div>
    <div v-if="loading" class="career-state">正在读取演示岗位库…</div>
    <template v-else>
      <div v-if="error" class="career-alert" role="alert">{{ error }} <button type="button" @click="load">重试</button></div>
      <p v-if="notice" class="career-notice" role="status">{{ notice }}</p>
      <form class="career-filters" @submit.prevent="search">
        <label class="career-keyword"><span>关键词</span><div><Search :size="17" /><input v-model="keyword" placeholder="岗位名称、公司或要求" /></div></label>
        <label><span>岗位方向</span><select v-model="category"><option value="">全部方向</option><option v-for="item in categories" :key="item">{{ item }}</option></select></label>
        <label><span>城市</span><select v-model="city"><option value="">全部城市</option><option v-for="item in cities" :key="item">{{ item }}</option></select></label>
        <label><span>类型</span><select v-model="jobType"><option value="">全部类型</option><option v-for="item in jobTypes" :key="item">{{ item }}</option></select></label>
        <button class="primary-button" type="submit" :disabled="busy">筛选岗位</button>
      </form>
      <p class="career-library-note">以下公司、薪资和岗位仅用于演示职业规划流程，不代表真实招聘信息。</p>
      <div v-if="jobs.length === 0" class="career-state">没有符合条件的岗位。试着减少筛选条件。</div>
      <div v-else class="career-list">
        <article v-for="job in jobs" :key="job.id" class="career-list-card">
          <div class="career-list-main"><div><span class="career-label">{{ job.category }} · {{ job.jobType }}</span><h3>{{ job.title }}</h3><p>{{ job.company }}</p></div><strong class="career-salary">{{ job.salary }}</strong></div>
          <div class="career-meta"><span><MapPin :size="14" />{{ job.city }}</span><span v-for="skill in job.skillTags.slice(0, 5)" :key="skill" class="career-mini-tag">{{ skill }}</span></div>
          <p class="career-description">{{ job.description }}</p>
          <div v-if="expanded === job.id" class="career-job-detail"><strong>岗位要求</strong><p>{{ job.requirements }}</p><strong>技能关键词</strong><div class="career-chips"><span v-for="skill in job.skillTags" :key="skill">{{ skill }}</span></div></div>
          <div class="career-card-actions"><button type="button" class="text-button compact" @click="expanded = expanded === job.id ? null : job.id">{{ expanded === job.id ? '收起详情' : '查看详情' }}</button><button type="button" class="secondary-button compact" :disabled="busy || preference.targetJobId === job.id" @click="setTarget(job.id)"><Target :size="15" />{{ preference.targetJobId === job.id ? '当前目标' : '设为目标' }}</button><button type="button" class="primary-button compact" @click="router.push({ path: '/module4/jd', query: { jobId: job.id } })">分析 JD <ArrowRight :size="15" /></button></div>
        </article>
      </div>
    </template>
  </section>
</template>
