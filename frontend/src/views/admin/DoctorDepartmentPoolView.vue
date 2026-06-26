<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'

import {
  claimDoctorConsultation,
  getDepartmentPool,
} from '../../api/doctorConsultation'
import {
  formatConsultationTime,
  urgencyDisplay,
} from '../../features/consultation/display'
import { getApiErrorMessage as errorMessage } from '../../features/feedback'

const loading = ref(false)
const claimingId = ref(null)
const consultations = ref([])
const total = ref(0)
const filters = reactive({
  current: 1,
  size: 10,
  scope: 'all',
  urgency: '',
  keyword: '',
})

async function loadConsultations() {
  loading.value = true
  try {
    const page = await getDepartmentPool({
      current: filters.current,
      size: filters.size,
      scope: filters.scope,
      urgency: filters.urgency || undefined,
      keyword: filters.keyword || undefined,
    })
    consultations.value = page.records || []
    total.value = page.total || 0
  } catch (error) {
    ElMessage.error(errorMessage(error, '科室问诊池加载失败'))
  } finally {
    loading.value = false
  }
}

function search() {
  filters.current = 1
  loadConsultations()
}

function resetFilters() {
  Object.assign(filters, {
    current: 1,
    size: 10,
    scope: 'all',
    urgency: '',
    keyword: '',
  })
  loadConsultations()
}

async function claim(item) {
  claimingId.value = item.id
  try {
    await claimDoctorConsultation(item.id)
    ElMessage.success('认领成功')
    await loadConsultations()
  } catch (error) {
    ElMessage.error(errorMessage(error, '问诊认领失败'))
  } finally {
    claimingId.value = null
  }
}

onMounted(loadConsultations)
</script>

<template>
  <section class="doctor-page">
    <header class="page-heading">
      <h1>科室问诊池</h1>
      <div class="pool-count">
        <strong>{{ total }}</strong>
        <small>张等待认领</small>
      </div>
    </header>

    <section class="filters">
      <el-segmented
        v-model="filters.scope"
        :options="[
          { label: '全部可认领', value: 'all' },
          { label: '本科室', value: 'department' },
          { label: '综合咨询', value: 'general' },
        ]"
        @change="search"
      />
      <div class="filter-controls">
        <el-input
          v-model="filters.keyword"
          class="search-input"
          clearable
          placeholder="搜索患者姓名或主要症状"
          @keyup.enter="search"
        >
          <template #prefix><span class="search-mark">⌕</span></template>
        </el-input>
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
        <el-button class="filter-button" type="primary" @click="search">筛选</el-button>
        <el-button class="reset-button" @click="resetFilters">重置</el-button>
      </div>
    </section>

    <section class="table-card">
      <el-table v-loading="loading" :data="consultations" stripe>
        <el-table-column label="患者" min-width="110" prop="patientName" />
        <el-table-column label="主要症状" min-width="280" show-overflow-tooltip prop="symptoms" />
        <el-table-column label="问诊科室" min-width="130">
          <template #default="{ row }">
            <span class="department-chip">{{ row.departmentName || '综合咨询' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="紧急程度" min-width="110">
          <template #default="{ row }">
            <span :class="['status-tag', `status-${urgencyDisplay(row.urgency).tone}`]">
              {{ urgencyDisplay(row.urgency).label }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="持续时间" min-width="110">
          <template #default="{ row }">{{ row.duration || '未填写' }}</template>
        </el-table-column>
        <el-table-column label="提交时间" min-width="170">
          <template #default="{ row }">{{ formatConsultationTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column fixed="right" label="操作" width="116">
          <template #default="{ row }">
            <el-button
              link
              type="success"
              :loading="claimingId === row.id"
              @click="claim(row)"
            >
              认领问诊
            </el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="当前筛选范围内没有待认领问诊" />
        </template>
      </el-table>

      <el-pagination
        v-if="total > filters.size"
        background
        layout="prev, pager, next"
        :current-page="filters.current"
        :page-size="filters.size"
        :total="total"
        @current-change="(page) => { filters.current = page; loadConsultations() }"
      />
    </section>
  </section>
</template>

<style scoped>
.doctor-page {
  display: grid;
  gap: 16px;
}

.page-heading {
  display: flex;
  min-height: 120px;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  padding: 22px 30px;
  border-radius: 26px;
  background:
    radial-gradient(circle at 88% 12%, rgb(164 211 184 / 28%), transparent 32%),
    linear-gradient(132deg, #0d4934 0%, #145b40 54%, #2f7a5b 100%);
  color: white;
  box-shadow: 0 18px 42px rgb(18 65 47 / 14%);
}

.page-heading h1 {
  margin: 0;
  font-family: "Noto Serif SC", "STSong", serif;
  font-size: 32px;
  letter-spacing: .015em;
  line-height: 1.2;
}

.pool-count {
  display: inline-flex;
  min-height: 58px;
  align-items: center;
  gap: 12px;
  padding: 0 20px;
  border: 1px solid rgb(255 255 255 / 22%);
  border-radius: 999px;
  background: rgb(255 255 255 / 12%);
  box-shadow: inset 0 1px 0 rgb(255 255 255 / 12%);
  backdrop-filter: blur(14px);
}

.pool-count strong {
  font-family: "Noto Serif SC", "STSong", serif;
  font-size: 30px;
  line-height: 1;
}

.pool-count small {
  color: rgb(255 255 255 / 78%);
  font-size: 12px;
  font-weight: 700;
  white-space: nowrap;
}

.filters {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px;
  border: 1px solid var(--color-border);
  border-radius: 18px;
  background:
    radial-gradient(circle at 0 50%, rgb(225 240 231 / 68%), transparent 30%),
    rgb(255 255 255 / 90%);
  box-shadow: 0 10px 28px rgb(21 56 42 / 5%);
}

.filter-controls {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-left: auto;
  padding: 5px;
  border: 1px solid rgb(46 101 74 / 10%);
  border-radius: 16px;
  background: rgb(255 255 255 / 68%);
  box-shadow: inset 0 1px 0 white;
}

.filters :deep(.el-segmented) {
  --el-segmented-bg-color: transparent;
  --el-segmented-item-selected-color: white;
  --el-segmented-item-selected-bg-color: transparent;
  min-height: 46px;
  padding: 5px;
  border: 1px solid rgb(46 101 74 / 12%);
  border-radius: 999px;
  background: rgb(226 239 231 / 76%);
  box-shadow: inset 0 1px 3px rgb(25 68 48 / 6%);
}

.filters :deep(.el-segmented__group) {
  gap: 4px;
}

.filters :deep(.el-segmented__item) {
  min-height: 36px;
  padding: 0 17px;
  border-radius: 999px;
  color: #526b60;
  font-size: 13px;
  font-weight: 750;
  transition:
    color .18s ease,
    background .18s ease,
    box-shadow .18s ease,
    transform .18s ease;
}

.filters :deep(.el-segmented__item:hover) {
  background: rgb(255 255 255 / 55%);
  color: var(--color-ink);
}

.filters :deep(.el-segmented__item-selected) {
  border-radius: 999px;
  background: linear-gradient(135deg, #174f3a, #2b7656);
  box-shadow:
    0 7px 16px rgb(18 72 50 / 20%),
    inset 0 1px 0 rgb(255 255 255 / 16%);
}

.filters :deep(.el-segmented__item.is-selected) {
  color: white;
  transform: translateY(-1px);
}

.filters .el-input {
  width: 250px;
}

.filters .el-select {
  width: 150px;
}

.filters :deep(.el-input__wrapper),
.filters :deep(.el-select__wrapper) {
  min-height: 42px;
  border-radius: 12px;
  background: #f7faf8;
  box-shadow: inset 0 0 0 1px rgb(43 92 68 / 12%);
  transition: box-shadow .18s ease, background .18s ease;
}

.filters :deep(.el-input__wrapper:hover),
.filters :deep(.el-select__wrapper:hover) {
  background: white;
  box-shadow: inset 0 0 0 1px rgb(39 101 72 / 28%);
}

.filters :deep(.el-input__wrapper.is-focus),
.filters :deep(.el-select__wrapper.is-focused) {
  background: white;
  box-shadow:
    inset 0 0 0 1px #2d7657,
    0 0 0 3px rgb(45 118 87 / 10%);
}

.search-mark {
  color: #47715d;
  font-size: 20px;
  line-height: 1;
}

.filter-button,
.reset-button {
  min-height: 42px;
  border-radius: 12px;
  font-weight: 800;
}

.filter-button {
  min-width: 76px;
  border: 0;
  background: linear-gradient(135deg, #174f3a, #2b7656);
  box-shadow: 0 7px 16px rgb(18 72 50 / 16%);
}

.reset-button {
  min-width: 68px;
  border-color: transparent;
  background: transparent;
  color: #5c7067;
}

.reset-button:hover {
  border-color: rgb(45 118 87 / 12%);
  background: #eef5f1;
  color: #174f3a;
}

.table-card {
  padding: 14px;
  border: 1px solid var(--color-border);
  border-radius: 20px;
  background: white;
  box-shadow: var(--shadow-card);
}

.status-tag,
.department-chip {
  display: inline-flex;
  padding: 6px 9px;
  border-radius: 999px;
  font-size: 10px;
  font-weight: 800;
}

.status-tag,
.department-chip {
  background: #e7f2eb;
  color: var(--color-ink);
}

.status-attention {
  background: #fff0d6;
  color: #8a5c0f;
}

.status-urgent {
  background: var(--color-cinnabar-soft);
  color: #9f3f2e;
}

.el-pagination {
  justify-content: center;
  margin-top: 18px;
}

@media (max-width: 1050px) {
  .filters {
    flex-wrap: wrap;
  }

  .filter-controls {
    width: 100%;
    flex-wrap: wrap;
    margin-left: 0;
  }
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
</style>
