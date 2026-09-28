<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ArrowRight, CheckCircle2, CircleAlert, FileSearch, History, Save } from 'lucide-vue-next'
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
    <div class="career-page-heading"><div><p class="eyebrow">03 / JD 分析</p><h2>拆解岗位要求，形成行动建议</h2><p class="career-page-description">导入演示岗位或粘贴 JD，与当前能力图谱逐项对照。</p></div><span class="career-count">已保存 {{ reports.length }} 份报告</span></div>
    <div v-if="loading" class="career-state">正在读取目标岗位与历史报告…</div>
    <template v-else>
      <div v-if="error" class="career-alert" role="alert">{{ error }} <button v-if="jobs.length === 0" type="button" @click="load">重试</button></div>
      <p v-if="notice" class="career-notice" role="status">{{ notice }}</p>
      <div class="career-jd-layout">
          <form class="career-panel career-jd-form" @submit.prevent="analyze">
            <div class="career-panel-heading"><span class="career-panel-icon"><FileSearch :size="18" /></span><div><h3>新建岗位分析</h3><p>填写岗位要求，生成并保存匹配报告。</p></div><span class="career-source-state" :class="{ imported: selectedJob }"><CheckCircle2 v-if="selectedJob" :size="14" />{{ selectedJob ? '已带入岗位' : '自定义 JD' }}</span></div>
            <div class="career-jd-fields"><label>带入岗位<select :value="selectedJobId" @change="changeSource"><option value="">自定义 JD</option><option v-for="job in jobs" :key="job.id" :value="job.id">{{ job.title }} · {{ job.company }}</option></select></label><label>报告标题<input v-model="jobTitle" maxlength="160" placeholder="例如：后端开发工程师" /></label></div>
            <label class="career-jd-editor"><span class="career-jd-editor-heading"><strong>岗位描述</strong><small>{{ jdText.trim().length }} / 10000 字</small></span><textarea v-model="jdText" rows="7" maxlength="10000" placeholder="粘贴岗位职责、任职要求、技能关键词等，至少 20 字。" /></label>
            <p v-if="selectedJob" class="career-form-hint"><CheckCircle2 :size="14" />已带入 {{ selectedJob.company }} 的演示岗位，可修改后再分析。</p>
            <div class="career-form-footer"><span>分析基于当前能力图谱，报告会自动保存。</span><button class="primary-button" type="submit" :disabled="busy"><Save :size="16" />{{ busy ? '生成中…' : '生成并保存报告' }}</button></div>
          </form>

          <article v-if="report" class="career-panel career-report">
            <div class="career-report-header"><div><span class="career-report-saved"><CheckCircle2 :size="14" />已保存 · 能力快照</span><h3>{{ report.jobTitle }}</h3><p>{{ new Date(report.createdAt).toLocaleString('zh-CN') }} · {{ report.recognizedSkills.length }} 项可比对技能</p></div><div class="career-score career-report-score"><strong>{{ report.score }}%</strong><small>参考匹配</small></div></div>
            <div class="career-report-summary"><span class="matched"><CheckCircle2 :size="15" />{{ report.matched.length }} 项已有匹配</span><span class="gap"><CircleAlert :size="15" />{{ report.gaps.length }} 项待补齐</span></div>
            <p class="career-report-note">这份报告是生成时的能力快照。修改能力图谱后，可重新生成报告查看变化。</p>
            <div class="career-report-grid"><section class="career-report-matched"><h4><CheckCircle2 :size="17" />已有匹配 <small>{{ report.matched.length }} 项</small></h4><ul v-if="report.matched.length"><li v-for="item in report.matched" :key="item">{{ item }}</li></ul><p v-else>当前能力图谱里没有达到岗位要求的明确匹配项。</p></section><section class="career-report-gaps"><h4><CircleAlert :size="17" />能力缺口 <small>{{ report.gaps.length }} 项</small></h4><ul v-if="report.gaps.length"><li v-for="item in report.gaps" :key="item">{{ item }}</li></ul><p v-else>已识别的技能没有明显缺口。</p></section></div>
            <section class="career-actions"><h4><ArrowRight :size="17" />下一步建议</h4><ol><li v-for="action in report.actions" :key="action">{{ action }}</li></ol></section>
          </article>
          <div v-else class="career-empty-report"><div><strong>生成后查看匹配报告</strong><p>报告会按当前能力图谱，依次整理匹配点、能力缺口和行动建议。</p></div><div class="career-empty-steps"><span class="matched"><CheckCircle2 :size="15" />已有匹配</span><span class="gap"><CircleAlert :size="15" />能力缺口</span><span class="action"><ArrowRight :size="15" />下一步建议</span></div></div>
        <aside class="career-panel career-history"><div class="career-panel-heading"><span class="career-panel-icon career-history-icon"><History :size="18" /></span><div><h3>历史报告</h3><p>查看已保存的分析快照</p></div></div><div v-if="!reports.length" class="career-history-empty">还没有保存过报告。</div><button v-for="item in reports" :key="item.id" type="button" class="career-history-item" :class="{ active: report?.id === item.id }" @click="openReport(item)"><span><strong>{{ item.jobTitle }}</strong><small><CheckCircle2 :size="12" />{{ new Date(item.createdAt).toLocaleDateString('zh-CN') }} · {{ item.score }}% 参考匹配</small></span><ArrowRight :size="15" /></button><div class="career-history-guide"><strong>{{ report ? '当前报告摘要' : '报告将整理' }}</strong><span class="matched"><CheckCircle2 :size="15" />{{ report ? `${report.matched.length} 项已有匹配` : '已有匹配' }}</span><span class="gap"><CircleAlert :size="15" />{{ report ? `${report.gaps.length} 项能力缺口` : '能力缺口' }}</span><span class="action"><ArrowRight :size="15" />{{ report ? `${report.actions.length} 条下一步建议` : '下一步建议' }}</span></div></aside>
      </div>
    </template>
  </section>
</template>
