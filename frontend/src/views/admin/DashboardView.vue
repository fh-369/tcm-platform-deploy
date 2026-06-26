<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'

import { getDashboardSummary, getDashboardTrend } from '../../api/content'
import { getWorkspaceIdentity } from '../../features/admin/workspace'
import { getApiErrorMessage } from '../../features/feedback'
import { useAuthStore } from '../../stores/auth'

const auth = useAuthStore()
const loading = ref(false)
const trendLoading = ref(false)
const loadError = ref('')
const summary = ref({
  scope: 'platform',
  statusDistribution: [],
  urgencyDistribution: [],
  trendLast6Months: [],
  metrics: {},
  departmentDistribution: [],
  doctorWorkloads: [],
})
const trend = ref([])
const trendPeriod = ref('month')
const identity = computed(() => getWorkspaceIdentity(auth.role))
const isDoctor = computed(() => auth.role === 'doctor')
const total = computed(() =>
  summary.value.statusDistribution.reduce(
    (sum, item) => sum + Number(item.value || 0),
    0,
  ),
)
const maxTrend = computed(() =>
  Math.max(...trend.value.map((item) => Number(item.value || 0)), 1),
)
const maxDepartment = computed(() =>
  Math.max(
    ...summary.value.departmentDistribution.map((item) => Number(item.value || 0)),
    1,
  ),
)
const maxWorkload = computed(() =>
  Math.max(
    ...summary.value.doctorWorkloads.map((item) => Number(item.activeCount || 0)),
    1,
  ),
)
const trendPeriods = [
  { label: '近 7 天', value: 'day' },
  { label: '近 4 周', value: 'week' },
  { label: '近 6 个月', value: 'month' },
]

function distributionValue(items, label) {
  return Number(items.find((item) => item.label === label)?.value || 0)
}

const pending = computed(() =>
  distributionValue(summary.value.statusDistribution, '待接诊'),
)
const inProgress = computed(() =>
  distributionValue(summary.value.statusDistribution, '接诊中'),
)
const completed = computed(() =>
  distributionValue(summary.value.statusDistribution, '已完成'),
)
const urgent = computed(() =>
  distributionValue(summary.value.urgencyDistribution, '紧急')
  + distributionValue(summary.value.urgencyDistribution, '非常紧急'),
)
const metricCards = computed(() => (
  isDoctor.value
    ? [
        { label: '我的问诊', value: summary.value.metrics.assignedTotal || total.value, note: '已分配给当前账号' },
        { label: '待接诊', value: pending.value, note: '等待开始处理' },
        { label: '接诊中', value: inProgress.value, note: '正在持续处理' },
        { label: '已完成', value: completed.value, note: '已完成诊疗回复' },
      ]
    : [
        { label: '注册用户', value: summary.value.metrics.registeredPatients || 0, note: '平台患者账号' },
        { label: '启用医生', value: summary.value.metrics.enabledDoctors || 0, note: '已审核且可登录' },
        { label: '已发文章', value: summary.value.metrics.publishedKnowledge || 0, note: '用户端可阅读' },
        { label: '已发药膳', value: summary.value.metrics.publishedRecipes || 0, note: '用户端可查看' },
      ]
))

async function loadDashboard() {
  loading.value = true
  loadError.value = ''
  try {
    summary.value = await getDashboardSummary()
    trend.value = summary.value.trendLast6Months
    trendPeriod.value = 'month'
  } catch (error) {
    loadError.value = getApiErrorMessage(error, '工作台加载失败')
    ElMessage.error(loadError.value)
  } finally {
    loading.value = false
  }
}

async function changeTrendPeriod(period) {
  if (period === trendPeriod.value || trendLoading.value) return

  trendLoading.value = true
  try {
    trend.value = await getDashboardTrend(period)
    trendPeriod.value = period
  } catch (error) {
    ElMessage.error(getApiErrorMessage(error, '趋势数据加载失败'))
  } finally {
    trendLoading.value = false
  }
}

function formatTrendLabel(label) {
  if (trendPeriod.value === 'month') return label
  const [, month, day] = String(label).split('-')
  return month && day ? `${month}-${day}` : label
}

onMounted(loadDashboard)
</script>

<template>
  <section v-loading="loading" class="dashboard">
    <section class="welcome-panel">
      <div>
        <small>{{ isDoctor ? '个人接诊概览' : '平台运营概览' }}</small>
        <h1>{{ auth.displayName || identity.roleLabel }}，欢迎回来</h1>
      </div>
      <div class="welcome-actions">
        <RouterLink :to="isDoctor ? '/admin/my-consultations' : '/admin/consultations'">
          {{ isDoctor ? '处理我的问诊' : '查看问诊调度' }}
        </RouterLink>
        <RouterLink v-if="!isDoctor" class="secondary" to="/admin/export">
          筛选导出数据
        </RouterLink>
      </div>
    </section>

    <section v-if="loadError" class="load-error">
      <div>
        <strong>数据暂时无法显示</strong>
        <span>{{ loadError }}</span>
      </div>
      <el-button @click="loadDashboard">重新加载</el-button>
    </section>

    <template v-else>
      <section class="metric-grid">
        <article v-for="card in metricCards" :key="card.label">
          <span>{{ card.label }}</span>
          <strong>{{ card.value }}</strong>
          <small>{{ card.note }}</small>
        </article>
      </section>

      <section class="action-strip">
        <div>
          <small>{{ isDoctor ? '个人处理进度' : '平台处理进度' }}</small>
          <h2>
            {{ isDoctor
              ? `你有 ${pending} 张待接诊、${inProgress} 张正在处理`
              : `已有 ${completed} 张完成处理，仍有 ${pending} 张等待接诊` }}
          </h2>
        </div>
        <span v-if="urgent">{{ urgent }} 张紧急问诊需要优先关注</span>
      </section>

      <div class="dashboard-grid">
        <section v-loading="trendLoading" class="data-card trend-card">
          <header>
            <div>
              <small>{{ isDoctor ? '个人接诊节奏' : '平台业务变化' }}</small>
              <h2>问诊趋势</h2>
            </div>
            <div class="trend-periods" aria-label="问诊趋势周期">
              <button
                v-for="period in trendPeriods"
                :key="period.value"
                type="button"
                :class="{ active: trendPeriod === period.value }"
                @click="changeTrendPeriod(period.value)"
              >
                {{ period.label }}
              </button>
            </div>
          </header>
          <div v-if="trend.length" class="trend-chart">
            <div v-for="item in trend" :key="item.label">
              <strong>{{ item.value }}</strong>
              <i :style="{ height: `${Math.max(Number(item.value) / maxTrend * 150, 4)}px` }"></i>
              <span>{{ formatTrendLabel(item.label) }}</span>
            </div>
          </div>
          <div v-else class="chart-empty">当前周期暂无问诊记录</div>
        </section>

        <section class="data-card distribution-card">
          <header>
            <div>
              <small>{{ isDoctor ? '我的任务构成' : '问诊风险结构' }}</small>
              <h2>{{ isDoctor ? '个人问诊状态' : '状态与风险' }}</h2>
            </div>
          </header>
          <div class="distribution">
            <div v-for="item in summary.statusDistribution" :key="item.label">
              <span>{{ item.label }}</span>
              <strong>{{ item.value }}</strong>
              <i :style="{ width: `${total ? Number(item.value) / total * 100 : 0}%` }"></i>
            </div>
          </div>
          <div class="urgency-list">
            <div v-for="item in summary.urgencyDistribution" :key="item.label">
              <span>{{ item.label }}</span>
              <strong>{{ item.value }}</strong>
            </div>
          </div>
        </section>
      </div>

      <div v-if="!isDoctor" class="operations-grid">
        <section class="data-card">
          <header>
            <div>
              <small>问诊来源结构</small>
              <h2>科室分布</h2>
            </div>
          </header>
          <div v-if="summary.departmentDistribution.length" class="horizontal-chart">
            <div v-for="item in summary.departmentDistribution" :key="item.label">
              <span>{{ item.label }}</span>
              <i><b :style="{ width: `${Number(item.value) / maxDepartment * 100}%` }"></b></i>
              <strong>{{ item.value }}</strong>
            </div>
          </div>
          <div v-else class="chart-empty compact">暂无科室统计</div>
        </section>

        <section class="data-card">
          <header>
            <div>
              <small>当前未完成问诊</small>
              <h2>医生处理负载</h2>
            </div>
          </header>
          <div v-if="summary.doctorWorkloads.length" class="workload-list">
            <div v-for="doctor in summary.doctorWorkloads" :key="doctor.doctorId">
              <span>{{ doctor.doctorName }}</span>
              <i><b :style="{ width: `${Number(doctor.activeCount) / maxWorkload * 100}%` }"></b></i>
              <strong>{{ doctor.activeCount }}</strong>
            </div>
          </div>
          <div v-else class="chart-empty compact">暂无启用医生数据</div>
        </section>
      </div>
    </template>
  </section>
</template>

<style scoped>
.dashboard {
  max-width: 1480px;
  margin: 0 auto;
}

.welcome-panel {
  display: flex;
  min-height: 146px;
  align-items: center;
  justify-content: space-between;
  gap: 30px;
  padding: 28px 32px;
  border-radius: 28px;
  background:
    radial-gradient(circle at 78% 0%, rgb(148 199 172 / 36%), transparent 32%),
    linear-gradient(135deg, #0e4937, #17614a);
  color: white;
  box-shadow: 0 18px 44px rgb(14 73 55 / 18%);
}

.welcome-panel small,
.data-card header small,
.action-strip small {
  color: var(--color-cinnabar);
  font-size: 9px;
  font-weight: 900;
  letter-spacing: .12em;
}

.welcome-panel small {
  color: #f4c3b9;
}

.welcome-panel h1 {
  margin: 8px 0 0;
  font-family: "Noto Serif SC", "STSong", serif;
  font-size: clamp(28px, 3vw, 42px);
  letter-spacing: -.04em;
}

.welcome-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 10px;
}

.welcome-actions a {
  display: inline-flex;
  min-height: 44px;
  align-items: center;
  padding: 0 18px;
  border: 1px solid white;
  border-radius: 999px;
  background: white;
  color: var(--color-ink);
  font-size: 12px;
  font-weight: 900;
  transition: .18s ease;
}

.welcome-actions a.secondary {
  border-color: rgb(255 255 255 / 28%);
  background: transparent;
  color: white;
}

.welcome-actions a:hover {
  transform: translateY(-1px);
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-top: 16px;
}

.metric-grid article {
  position: relative;
  min-height: 124px;
  padding: 20px;
  overflow: hidden;
  border: 1px solid rgb(47 95 72 / 10%);
  border-radius: 21px;
  background: rgb(255 255 255 / 90%);
  box-shadow: 0 12px 28px rgb(21 56 42 / 5%);
}

.metric-grid article::after {
  position: absolute;
  top: -34px;
  right: -24px;
  width: 92px;
  height: 92px;
  border-radius: 50%;
  background: rgb(100 158 128 / 9%);
  content: "";
}

.metric-grid span,
.metric-grid small,
.metric-grid strong {
  display: block;
}

.metric-grid span {
  color: var(--color-text-muted);
  font-size: 10px;
  font-weight: 800;
}

.metric-grid strong {
  margin-top: 12px;
  color: var(--color-ink);
  font-family: "Noto Serif SC", "STSong", serif;
  font-size: 29px;
}

.metric-grid small {
  margin-top: 7px;
  color: #89988f;
  font-size: 9px;
}

.action-strip,
.data-card,
.load-error {
  border: 1px solid rgb(47 95 72 / 11%);
  border-radius: 22px;
  background: rgb(255 255 255 / 88%);
  box-shadow: 0 12px 30px rgb(21 56 42 / 5%);
}

.action-strip {
  display: flex;
  min-height: 94px;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  margin-top: 16px;
  padding: 22px 26px;
}

.action-strip h2 {
  margin: 6px 0 0;
  color: var(--color-ink);
  font-size: 19px;
}

.action-strip > span {
  padding: 9px 13px;
  border-radius: 999px;
  background: var(--color-cinnabar-soft);
  color: var(--color-cinnabar);
  font-size: 10px;
  font-weight: 900;
}

.dashboard-grid,
.operations-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.55fr) minmax(320px, .75fr);
  gap: 14px;
  margin-top: 16px;
}

.operations-grid {
  grid-template-columns: 1fr 1fr;
}

.data-card {
  padding: 22px;
}

.data-card header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
}

.data-card h2 {
  margin: 5px 0 0;
  color: var(--color-ink);
  font-size: 18px;
}

.trend-periods {
  display: inline-flex;
  gap: 4px;
  padding: 4px;
  border: 1px solid rgb(47 95 72 / 10%);
  border-radius: 999px;
  background: #f2f7f3;
}

.trend-periods button {
  min-height: 30px;
  padding: 0 12px;
  border: 0;
  border-radius: 999px;
  background: transparent;
  color: var(--color-text-muted);
  cursor: pointer;
  font: inherit;
  font-size: 10px;
  font-weight: 800;
}

.trend-periods button.active {
  background: var(--color-ink);
  color: white;
}

.trend-chart {
  display: flex;
  min-height: 228px;
  align-items: end;
  gap: 20px;
  margin-top: 20px;
  padding: 18px 16px 0;
  border-radius: 18px;
  background: linear-gradient(rgb(37 83 61 / 5%) 1px, transparent 1px);
  background-size: 100% 38px;
}

.trend-chart div {
  display: grid;
  flex: 1;
  justify-items: center;
  gap: 7px;
  color: var(--color-text-muted);
  font-size: 9px;
}

.trend-chart i {
  width: min(48px, 72%);
  border-radius: 8px 8px 2px 2px;
  background: linear-gradient(180deg, #77ae91, var(--color-ink));
}

.trend-chart strong {
  color: var(--color-ink);
  font-size: 11px;
}

.distribution {
  display: grid;
  gap: 15px;
  margin-top: 22px;
}

.distribution div {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 8px;
  color: var(--color-text-muted);
  font-size: 11px;
}

.distribution strong {
  color: var(--color-ink);
}

.distribution i {
  height: 7px;
  grid-column: 1 / -1;
  border-radius: 99px;
  background: linear-gradient(90deg, #83b79a, var(--color-ink));
}

.urgency-list {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 7px;
  margin-top: 24px;
}

.urgency-list div {
  padding: 10px;
  border-radius: 14px;
  background: #f2f7f3;
  text-align: center;
}

.urgency-list span,
.urgency-list strong {
  display: block;
}

.urgency-list span {
  color: var(--color-text-muted);
  font-size: 9px;
}

.urgency-list strong {
  margin-top: 5px;
  color: var(--color-ink);
  font-size: 17px;
}

.horizontal-chart,
.workload-list {
  display: grid;
  gap: 14px;
  margin-top: 22px;
}

.horizontal-chart > div,
.workload-list > div {
  display: grid;
  grid-template-columns: 110px 1fr 34px;
  align-items: center;
  gap: 12px;
  font-size: 10px;
}

.horizontal-chart span,
.workload-list span {
  overflow: hidden;
  color: var(--color-text-muted);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.horizontal-chart i,
.workload-list i {
  height: 8px;
  overflow: hidden;
  border-radius: 99px;
  background: #eaf1ed;
}

.horizontal-chart b,
.workload-list b {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #75ae8f, var(--color-ink));
}

.horizontal-chart strong,
.workload-list strong {
  color: var(--color-ink);
  text-align: right;
}

.chart-empty {
  display: grid;
  min-height: 228px;
  margin-top: 20px;
  border-radius: 18px;
  background: #f5f8f5;
  color: var(--color-text-muted);
  font-size: 12px;
  place-items: center;
}

.chart-empty.compact {
  min-height: 150px;
}

.load-error {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  margin-top: 16px;
  padding: 22px;
}

.load-error strong,
.load-error span {
  display: block;
}

.load-error strong {
  color: var(--color-ink);
}

.load-error span {
  margin-top: 5px;
  color: var(--color-text-muted);
  font-size: 11px;
}

@media (max-width: 1000px) {
  .metric-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .dashboard-grid,
  .operations-grid {
    grid-template-columns: 1fr;
  }
}
</style>
