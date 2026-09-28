<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ArrowRight, FileSearch, History, Save } from 'lucide-vue-next'
import { careerService } from './careerService'
import type { CareerJob, CareerReport } from './types'

const route = useRoute()
const jobs = ref<CareerJob[]>([])
const reports = ref<CareerReport[]>([])
const selectedJobId = ref('')
const jobTitle = ref('')
const jdText = ref('')
const report = ref<CareerReport | null>(null)
const loading = ref(true)
const busy = ref(false)
const error = ref('')
const notice = ref('')
const selectedJob = computed(() => jobs.value.find(job => job.id === selectedJobId.value))

function useJob(id: string) {
  selectedJobId.value = id
  const job = jobs.value.find(item => item.id === id)
  if (job) {
    jobTitle.value = job.title
    jdText.value = `${job.title}\n${job.description}\n岗位要求：${job.requirements}`
  }
  report.value = null
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    const [jobItems, target, history] = await Promise.all([careerService.jobs(), careerService.target(), careerService.reports()])
    jobs.value = jobItems
    reports.value = history
    const queryId = typeof route.query.jobId === 'string' ? route.query.jobId : ''
    const initialId = jobItems.some(job => job.id === queryId) ? queryId : target.job?.id ?? ''
    if (initialId) useJob(initialId)
  } catch (cause) { error.value = cause instanceof Error ? cause.message : 'JD 分析暂时无法加载。' }
  finally { loading.value = false }
}

function changeSource(event: Event) {
  const id = (event.target as HTMLSelectElement).value
  if (id) useJob(id)
  else { selectedJobId.value = ''; jobTitle.value = ''; jdText.value = ''; report.value = null }
}

async function analyze() {
  error.value = ''
  notice.value = ''
  if (jdText.value.trim().length < 20) { error.value = '请粘贴至少 20 个字的岗位描述。'; return }
  busy.value = true
  try {
    report.value = await careerService.createReport({ jobId: selectedJobId.value || null, jobTitle: jobTitle.value, jdText: jdText.value })
    reports.value = [report.value, ...reports.value]
    notice.value = '报告已生成并保存。'
  } catch (cause) { error.value = cause instanceof Error ? cause.message : '报告生成失败。' }
  finally { busy.value = false }
}

function openReport(item: CareerReport) {
  report.value = item
  selectedJobId.value = item.jobId ?? ''
  jobTitle.value = item.jobTitle
  jdText.value = item.jdText
  notice.value = '正在查看已保存的报告快照。'
}

onMounted(load)
</script>

<template>
  <section class="career-page">
    <div class="career-page-heading"><div><p class="eyebrow">03 / JD 分析</p><h2>把岗位要求拆成下一步行动</h2></div><span class="career-count">已保存 {{ reports.length }} 份报告</span></div>
    <div v-if="loading" class="career-state">正在读取目标岗位与历史报告…</div>
    <template v-else>
      <div v-if="error" class="career-alert" role="alert">{{ error }} <button v-if="jobs.length === 0" type="button" @click="load">重试</button></div>
      <p v-if="notice" class="career-notice" role="status">{{ notice }}</p>
      <div class="career-jd-layout">
        <div class="career-jd-main">
          <form class="career-panel career-jd-form" @submit.prevent="analyze">
            <div class="career-panel-heading"><FileSearch :size="19" /><div><h3>输入岗位 JD</h3><p>可带入演示岗位，也可以自行粘贴招聘描述。</p></div></div>
            <label>带入岗位<select :value="selectedJobId" @change="changeSource"><option value="">自定义 JD</option><option v-for="job in jobs" :key="job.id" :value="job.id">{{ job.title }} · {{ job.company }}</option></select></label>
            <label>报告标题<input v-model="jobTitle" maxlength="160" placeholder="例如：后端开发工程师" /></label>
            <label>岗位描述<textarea v-model="jdText" rows="11" maxlength="10000" placeholder="粘贴岗位职责、任职要求、技能关键词等，至少 20 字。" /></label>
            <p v-if="selectedJob" class="career-form-hint">当前带入：{{ selectedJob.company }} 的演示岗位。你可以修改 JD 文本再分析。</p>
            <div class="career-form-footer"><span>{{ jdText.trim().length }} / 10000 字 · 基于当前能力图谱</span><button class="primary-button" type="submit" :disabled="busy"><Save :size="16" />{{ busy ? '生成中…' : '生成并保存报告' }}</button></div>
          </form>

          <article v-if="report" class="career-panel career-report">
            <div class="career-report-header"><div><span class="career-label">已保存的匹配报告</span><h3>{{ report.jobTitle }}</h3><p>{{ new Date(report.createdAt).toLocaleString('zh-CN') }} · {{ report.recognizedSkills.length }} 项可比对技能</p></div><div class="career-score career-report-score"><strong>{{ report.score }}%</strong><small>参考匹配</small></div></div>
            <p class="career-report-note">这份报告是生成时的能力快照。修改能力图谱后，可重新生成报告查看变化。</p>
            <div class="career-report-grid"><section><h4>已有匹配</h4><ul v-if="report.matched.length"><li v-for="item in report.matched" :key="item">{{ item }}</li></ul><p v-else>当前能力图谱里没有达到岗位要求的明确匹配项。</p></section><section><h4>能力缺口</h4><ul v-if="report.gaps.length"><li v-for="item in report.gaps" :key="item">{{ item }}</li></ul><p v-else>已识别的技能没有明显缺口。</p></section></div>
            <section class="career-actions"><h4>下一步建议</h4><ol><li v-for="action in report.actions" :key="action">{{ action }}</li></ol></section>
          </article>
          <div v-else class="career-empty-report"><FileSearch :size="30" /><strong>报告会显示在这里</strong><p>带入岗位或粘贴 JD，生成与当前能力对应的匹配点、缺口和行动建议。</p></div>
        </div>
        <aside class="career-panel career-history"><div class="career-panel-heading"><History :size="19" /><div><h3>历史报告</h3><p>点击查看保存时的结果</p></div></div><div v-if="!reports.length" class="career-history-empty">还没有保存过报告。</div><button v-for="item in reports" :key="item.id" type="button" class="career-history-item" :class="{ active: report?.id === item.id }" @click="openReport(item)"><span><strong>{{ item.jobTitle }}</strong><small>{{ new Date(item.createdAt).toLocaleDateString('zh-CN') }} · {{ item.score }}% 参考匹配</small></span><ArrowRight :size="15" /></button></aside>
      </div>
    </template>
  </section>
</template>
