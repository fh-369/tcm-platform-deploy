<script setup>
import { onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

import {
  countExportConsultations,
  exportConsultations,
} from '../../api/content'
import { getDepartments } from '../../api/auth'
import { getPersonnel } from '../../api/personnel'
import {
  applyExportQueryError,
  applyExportQueryResult,
  createExportQueryState,
  exportQuerySignature,
  invalidateExportQuery,
} from '../../features/admin/exportQuery'
import { getApiErrorMessage } from '../../features/feedback'

const counting = ref(false)
const downloading = ref(false)
const optionsLoading = ref(false)
const dateRange = ref([])
const departments = ref([])
const doctors = ref([])
const queryState = reactive(createExportQueryState())
const filters = reactive({
  status: '',
  urgency: '',
  departmentId: '',
  doctorId: '',
})

function buildParams() {
  const [dateFrom, dateTo] = dateRange.value || []
  return Object.fromEntries(
    Object.entries({
      dateFrom,
      dateTo,
      ...filters,
    }).filter(([, value]) => value !== '' && value != null),
  )
}

function hasSelectedFilters() {
  return Object.keys(buildParams()).length > 0
}

async function loadOptions() {
  optionsLoading.value = true
  try {
    const [departmentRecords, doctorPage] = await Promise.all([
      getDepartments(),
      getPersonnel('doctors', {
        current: 1,
        size: 100,
        approvalStatus: 'APPROVED',
      }),
    ])
    departments.value = departmentRecords || []
    doctors.value = (doctorPage.records || []).filter((doctor) => doctor.enabled)
  } catch (error) {
    ElMessage.error(getApiErrorMessage(error, '筛选选项加载失败'))
  } finally {
    optionsLoading.value = false
  }
}

async function refreshCount() {
  if (!hasSelectedFilters()) {
    try {
      await ElMessageBox.confirm(
        '当前未设置任何筛选条件，将查询全部问诊记录。',
        '查询全部记录',
        {
          confirmButtonText: '继续查询',
          cancelButtonText: '返回筛选',
          type: 'warning',
        },
      )
    } catch {
      return
    }
  }

  const params = buildParams()
  const requestSignature = exportQuerySignature(params)
  counting.value = true
  queryState.countError = ''
  try {
    const count = await countExportConsultations(params)
    if (requestSignature === exportQuerySignature(buildParams())) {
      applyExportQueryResult(queryState, count)
    }
  } catch (error) {
    const message = getApiErrorMessage(error, '导出数量查询失败')
    if (requestSignature === exportQuerySignature(buildParams())) {
      applyExportQueryError(queryState, message)
      ElMessage.error(message)
    }
  } finally {
    counting.value = false
  }
}

async function download() {
  if (!queryState.matchedCount) {
    ElMessage.warning('当前筛选条件下没有可导出的问诊记录')
    return
  }

  downloading.value = true
  try {
    const params = buildParams()
    const blob = await exportConsultations(params)
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `consultations-${params.dateFrom || 'all'}-to-${params.dateTo || 'all'}.csv`
    link.click()
    URL.revokeObjectURL(url)
    ElMessage.success(`已导出 ${queryState.matchedCount} 条问诊记录`)
  } catch (error) {
    ElMessage.error(getApiErrorMessage(error, '导出失败'))
  } finally {
    downloading.value = false
  }
}

function reset() {
  dateRange.value = []
  Object.assign(filters, {
    status: '',
    urgency: '',
    departmentId: '',
    doctorId: '',
  })
  invalidateExportQuery(queryState)
}

onMounted(async () => {
  await loadOptions()
})

watch(
  [
    dateRange,
    () => filters.status,
    () => filters.urgency,
    () => filters.departmentId,
    () => filters.doctorId,
  ],
  () => invalidateExportQuery(queryState),
  { deep: true },
)
</script>

<template>
  <section class="export-page">
    <header class="export-heading">
      <h1>问诊数据导出</h1>
      <div class="format-badge">
        <strong>CSV</strong>
        <span>UTF-8</span>
      </div>
    </header>

    <div class="export-layout">
      <section v-loading="optionsLoading" class="filter-card">
        <header>
          <span>01</span>
          <div>
            <h2>选择数据范围</h2>
            <p>日期范围包含结束日期当天，筛选条件可以组合使用。</p>
          </div>
        </header>

        <el-form label-position="top">
          <el-form-item label="问诊创建日期">
            <el-date-picker
              v-model="dateRange"
              class="range-picker"
              end-placeholder="结束日期"
              range-separator="至"
              start-placeholder="开始日期"
              type="daterange"
              unlink-panels
              value-format="YYYY-MM-DD"
              popper-class="jade-date-popper"
            />
          </el-form-item>

          <div class="field-grid">
            <el-form-item label="问诊状态">
              <el-select
                v-model="filters.status"
                clearable
                placeholder="全部状态"
                popper-class="jade-select-popper"
              >
                <el-option label="待接诊" value="待接诊" />
                <el-option label="接诊中" value="接诊中" />
                <el-option label="已完成" value="已完成" />
              </el-select>
            </el-form-item>
            <el-form-item label="紧急程度">
              <el-select
                v-model="filters.urgency"
                clearable
                placeholder="全部紧急程度"
                popper-class="jade-select-popper"
              >
                <el-option label="普通" value="普通" />
                <el-option label="紧急" value="紧急" />
                <el-option label="非常紧急" value="非常紧急" />
              </el-select>
            </el-form-item>
            <el-form-item label="问诊科室">
              <el-select
                v-model="filters.departmentId"
                clearable
                filterable
                placeholder="全部科室"
                popper-class="jade-select-popper"
              >
                <el-option
                  v-for="department in departments"
                  :key="department.id"
                  :label="department.name"
                  :value="department.id"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="接诊医生">
              <el-select
                v-model="filters.doctorId"
                clearable
                filterable
                placeholder="全部医生"
                popper-class="jade-select-popper"
              >
                <el-option
                  v-for="doctor in doctors"
                  :key="doctor.userId"
                  :label="`${doctor.displayName || doctor.username} · ${doctor.department || '未设置科室'}`"
                  :value="doctor.userId"
                />
              </el-select>
            </el-form-item>
          </div>
        </el-form>

        <div class="filter-actions">
          <el-button class="reset-button" @click="reset">重置条件</el-button>
          <el-button class="query-button" type="primary" :loading="counting" @click="refreshCount">
            查询匹配数量
          </el-button>
        </div>
      </section>

      <aside class="export-summary">
        <div class="summary-orbit" aria-hidden="true"></div>
        <span>{{ queryState.queried ? '当前筛选结果' : '等待查询' }}</span>
        <strong v-if="queryState.matchedCount !== null">{{ queryState.matchedCount }}</strong>
        <strong v-else>--</strong>
        <p v-if="queryState.countError">{{ queryState.countError }}</p>
        <p v-else-if="queryState.queried">条问诊记录可以导出</p>
        <p v-else>设置筛选条件后，查询可导出的记录数量</p>

        <ul>
          <li>包含患者信息、症状和处理状态</li>
          <li>包含问诊科室、接诊医生与分配时间</li>
          <li>仅系统管理员可以执行数据导出</li>
        </ul>

        <el-button
          class="download-button"
          type="primary"
          :disabled="queryState.matchedCount === null || queryState.matchedCount === 0"
          :loading="downloading"
          @click="download"
        >
          {{ queryState.matchedCount ? `导出 ${queryState.matchedCount} 条记录` : '查询后可导出' }}
        </el-button>
      </aside>
    </div>
  </section>
</template>

<style scoped>
.export-page {
  max-width: 1320px;
  margin: 0 auto;
}

.export-heading {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 24px;
}

.export-heading h1 {
  margin: 0;
  color: var(--color-ink);
  font-family: "Noto Serif SC", "STSong", serif;
  font-size: 32px;
  letter-spacing: -.04em;
}

.format-badge {
  display: flex;
  align-items: baseline;
  gap: 8px;
  padding: 11px 16px;
  border: 1px solid rgb(47 95 72 / 12%);
  border-radius: 16px;
  background: rgb(255 255 255 / 80%);
}

.format-badge strong {
  color: var(--color-ink);
  font-size: 18px;
}

.format-badge span {
  color: var(--color-text-muted);
  font-size: 9px;
}

.export-layout {
  display: grid;
  grid-template-columns: minmax(0, 1.45fr) minmax(300px, .65fr);
  align-items: stretch;
  gap: 16px;
  margin-top: 22px;
}

.filter-card,
.export-summary {
  border: 1px solid rgb(47 95 72 / 11%);
  border-radius: 24px;
  background: rgb(255 255 255 / 90%);
  box-shadow: 0 16px 36px rgb(21 56 42 / 6%);
}

.filter-card {
  padding: 24px 26px;
}

.filter-card > header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 24px;
}

.filter-card > header > span {
  display: grid;
  width: 38px;
  height: 38px;
  border-radius: 12px;
  background: #e7f1eb;
  color: var(--color-ink);
  font-size: 10px;
  font-weight: 900;
  place-items: center;
}

.filter-card h2 {
  margin: 0;
  color: var(--color-ink);
  font-size: 18px;
}

.filter-card header p {
  margin: 5px 0 0;
  color: var(--color-text-muted);
  font-size: 10px;
}

.field-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 2px 14px;
}

:deep(.el-date-editor),
:deep(.el-select) {
  width: 100%;
}

:deep(.el-input__wrapper),
:deep(.el-select__wrapper),
:deep(.el-date-editor.el-input__wrapper) {
  min-height: 44px;
  border-radius: 13px;
  background: #f7faf8;
  box-shadow: inset 0 0 0 1px rgb(43 92 68 / 12%);
  transition: box-shadow .18s ease, background .18s ease;
}

:deep(.el-input__wrapper:hover),
:deep(.el-select__wrapper:hover),
:deep(.el-date-editor.el-input__wrapper:hover) {
  background: white;
  box-shadow: inset 0 0 0 1px rgb(39 101 72 / 28%);
}

:deep(.el-input__wrapper.is-focus),
:deep(.el-select__wrapper.is-focused),
:deep(.el-date-editor.el-input__wrapper.is-active) {
  background: white;
  box-shadow:
    inset 0 0 0 1px #2d7657,
    0 0 0 3px rgb(45 118 87 / 10%);
}

:deep(.el-form-item) {
  margin-bottom: 17px;
}

:deep(.el-form-item__label) {
  color: #486256;
  font-size: 11px;
  font-weight: 800;
}

.filter-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding-top: 14px;
  border-top: 1px solid rgb(47 95 72 / 9%);
}

.query-button,
.reset-button {
  min-height: 44px;
  border-radius: 13px;
  font-weight: 800;
}

.query-button {
  min-width: 132px;
  border: 0;
  background: linear-gradient(135deg, #c84e3c, #dc6754);
  box-shadow: 0 8px 18px rgb(184 70 51 / 18%);
}

.reset-button {
  min-width: 96px;
  border-color: transparent;
  background: #eef5f1;
  color: #50685d;
}

.reset-button:hover {
  border-color: rgb(45 118 87 / 12%);
  background: #e3f0e8;
  color: #174f3a;
}

.export-summary {
  position: relative;
  display: flex;
  min-height: 390px;
  flex-direction: column;
  padding: 30px;
  overflow: hidden;
  background:
    radial-gradient(circle at 82% 8%, rgb(134 188 159 / 28%), transparent 28%),
    linear-gradient(155deg, #0f4938, #183f31);
  color: white;
}

.summary-orbit {
  position: absolute;
  top: -70px;
  right: -70px;
  width: 210px;
  height: 210px;
  border: 1px solid rgb(255 255 255 / 12%);
  border-radius: 50%;
  box-shadow:
    0 0 0 30px rgb(255 255 255 / 3%),
    0 0 0 65px rgb(255 255 255 / 2%);
}

.export-summary > span {
  color: #f1b4a8;
  font-size: 10px;
  font-weight: 900;
  letter-spacing: .12em;
}

.export-summary > strong {
  display: block;
  margin-top: 22px;
  font-family: "Noto Serif SC", "STSong", serif;
  font-size: 66px;
  line-height: 1;
}

.export-summary > p {
  margin: 10px 0 0;
  color: rgb(255 255 255 / 68%);
  font-size: 12px;
}

.export-summary ul {
  display: grid;
  gap: 13px;
  margin: 34px 0 24px;
  padding: 0;
  color: rgb(255 255 255 / 72%);
  font-size: 11px;
  line-height: 1.7;
  list-style: none;
}

.export-summary li {
  position: relative;
  padding-left: 18px;
}

.export-summary li::before {
  position: absolute;
  top: .65em;
  left: 0;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #82b99a;
  content: "";
}

.download-button {
  width: 100%;
  min-height: 46px;
  margin-top: auto;
  border: 0;
  border-radius: 13px;
  background: linear-gradient(135deg, #c84e3c, #dc6754);
  font-weight: 800;
}

:global(.jade-select-popper.el-popper) {
  overflow: hidden;
  border: 1px solid rgb(48 98 74 / 13%);
  border-radius: 15px;
  box-shadow: 0 16px 38px rgb(24 63 45 / 14%);
}

:global(.jade-select-popper .el-select-dropdown__item) {
  margin: 4px 7px;
  border-radius: 9px;
  color: #4f675c;
  font-weight: 650;
}

:global(.jade-select-popper .el-select-dropdown__item.is-hovering) {
  background: #edf5f0;
  color: #174f3a;
}

:global(.jade-select-popper .el-select-dropdown__item.is-selected) {
  background: #dceee4;
  color: #174f3a;
  font-weight: 850;
}

:global(.jade-date-popper.el-popper) {
  overflow: hidden;
  border: 1px solid rgb(48 98 74 / 13%);
  border-radius: 17px;
  box-shadow: 0 18px 42px rgb(24 63 45 / 15%);
}

:global(.jade-date-popper .el-date-table td.available:hover),
:global(.jade-date-popper .el-date-table td.today .el-date-table-cell__text) {
  color: #236649;
}

:global(.jade-date-popper .el-date-table td.start-date .el-date-table-cell__text),
:global(.jade-date-popper .el-date-table td.end-date .el-date-table-cell__text) {
  background: #236649;
}

@media (max-width: 900px) {
  .export-layout {
    grid-template-columns: 1fr;
  }

  .export-summary {
    min-height: auto;
  }
}
</style>
