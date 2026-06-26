<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

import {
  assignAdminConsultation,
  getAdminConsultations,
  updateAdminConsultationDepartment,
} from '../../api/adminConsultation'
import { getDepartments } from '../../api/auth'
import { getPersonnel } from '../../api/personnel'
import { getApiErrorMessage as errorMessage } from '../../features/feedback'
import {
  formatConsultationTime,
  isCrossDepartmentConsultation,
  reminderDisplay,
  statusDisplay,
  urgencyDisplay,
} from '../../features/consultation/display'
import { eligibleDoctorsForDepartment } from '../../features/admin/scheduling'

const loading = ref(false)
const assigning = ref(false)
const drawerVisible = ref(false)
const consultations = ref([])
const doctors = ref([])
const departments = ref([])
const selected = ref(null)
const total = ref(0)
const filters = reactive({
  current: 1,
  size: 10,
  status: '',
  urgency: '',
  keyword: '',
  assignment: 'all',
  departmentId: '',
})
const assignmentDoctorId = ref('')
const consultationDepartmentId = ref('')
const generalDepartmentId = computed(
  () => departments.value.find((department) => department.code === 'general')?.id,
)
const activeDoctors = computed(
  () => eligibleDoctorsForDepartment(
    doctors.value,
    null,
    generalDepartmentId.value,
  ),
)
const assignmentDoctors = computed(
  () => eligibleDoctorsForDepartment(
    doctors.value,
    consultationDepartmentId.value,
    generalDepartmentId.value,
  ),
)

async function loadConsultations() {
  loading.value = true
  try {
    const params = {
      current: filters.current,
      size: filters.size,
      status: filters.status || undefined,
      urgency: filters.urgency || undefined,
      keyword: filters.keyword || undefined,
    }
    if (filters.assignment === 'unassigned') {
      params.unassigned = true
    } else if (filters.assignment.startsWith('doctor:')) {
      params.doctorId = Number(filters.assignment.slice(7))
    }
    if (filters.departmentId) {
      params.departmentId = Number(filters.departmentId)
    }
    const page = await getAdminConsultations(params)
    consultations.value = page.records || []
    total.value = page.total || 0
  } catch (error) {
    ElMessage.error(errorMessage(error, '问诊列表加载失败'))
  } finally {
    loading.value = false
  }
}

async function loadDoctors() {
  try {
    const page = await getPersonnel('doctors', { current: 1, size: 100 })
    doctors.value = page.records || []
  } catch (error) {
    ElMessage.error(errorMessage(error, '医生列表加载失败'))
  }
}

async function loadDepartments() {
  try {
    departments.value = await getDepartments()
  } catch (error) {
    ElMessage.error(errorMessage(error, '科室信息加载失败'))
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
    status: '',
    urgency: '',
    keyword: '',
    assignment: 'all',
    departmentId: '',
  })
  loadConsultations()
}

function openDetails(item) {
  selected.value = item
  assignmentDoctorId.value = item.doctorId || ''
  consultationDepartmentId.value = item.departmentId || ''
  drawerVisible.value = true
}

async function saveDepartment() {
  if (!consultationDepartmentId.value) {
    ElMessage.warning('请选择问诊科室')
    return
  }
  const nextDepartmentId = Number(consultationDepartmentId.value)
  if (nextDepartmentId === Number(selected.value.departmentId)) {
    ElMessage.info('问诊科室未发生变化')
    return
  }
  const nextDepartment = departments.value.find(
    (department) => Number(department.id) === nextDepartmentId,
  )
  try {
    await ElMessageBox.confirm(
      `转入“${nextDepartment?.name || '所选科室'}”后，将解除当前负责医生，并把问诊恢复为待接诊。`,
      '确认转科',
      {
        confirmButtonText: '确认转科',
        cancelButtonText: '取消',
        type: 'warning',
      },
    )
  } catch {
    consultationDepartmentId.value = selected.value.departmentId
    return
  }
  assigning.value = true
  try {
    const updated = await updateAdminConsultationDepartment(
      selected.value.id,
      nextDepartmentId,
    )
    selected.value = {
      ...selected.value,
      departmentId: updated.departmentId,
      departmentName: updated.departmentName,
      doctorId: null,
      doctorName: null,
      doctorDepartment: null,
      status: updated.status || '待接诊',
    }
    assignmentDoctorId.value = ''
    ElMessage.success('转科完成，原医生已解除')
    await loadConsultations()
  } catch (error) {
    ElMessage.error(errorMessage(error, '问诊科室更新失败'))
  } finally {
    assigning.value = false
  }
}

async function saveAssignment() {
  assigning.value = true
  try {
    const doctorId = assignmentDoctorId.value === '' ? null : Number(assignmentDoctorId.value)
    await assignAdminConsultation(selected.value.id, doctorId)
    ElMessage.success(doctorId ? '问诊已分配给指定医生' : '已取消问诊分配')
    drawerVisible.value = false
    await loadConsultations()
  } catch (error) {
    ElMessage.error(errorMessage(error, '问诊分配失败'))
  } finally {
    assigning.value = false
  }
}

onMounted(async () => {
  await Promise.all([loadConsultations(), loadDoctors(), loadDepartments()])
})
</script>

<template>
  <section class="management-page">
    <header class="management-heading">
      <h1>问诊调度</h1>
      <span>共 {{ total }} 张问诊单</span>
    </header>

    <section class="filters">
      <el-input
        v-model="filters.keyword"
        class="search-input"
        clearable
        placeholder="搜索患者姓名或症状"
        @keyup.enter="search"
      >
        <template #prefix><span class="search-mark">⌕</span></template>
      </el-input>
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
      <el-select
        v-model="filters.departmentId"
        clearable
        placeholder="全部问诊科室"
        popper-class="jade-select-popper"
      >
        <el-option
          v-for="department in departments"
          :key="department.id"
          :label="department.name"
          :value="department.id"
        />
      </el-select>
      <el-select
        v-model="filters.assignment"
        class="assignment-filter"
        placeholder="全部分配状态"
        popper-class="jade-select-popper"
      >
        <el-option label="全部分配状态" value="all" />
        <el-option label="尚未分配" value="unassigned" />
        <el-option
          v-for="doctor in activeDoctors"
          :key="doctor.userId"
          :label="`${doctor.displayName || doctor.username} · ${doctor.department || '未配置科室'}`"
          :value="`doctor:${doctor.userId}`"
        />
      </el-select>
      <el-button class="filter-button" type="primary" @click="search">筛选</el-button>
      <el-button class="reset-button" @click="resetFilters">重置</el-button>
    </section>

    <section class="table-card">
      <el-table v-loading="loading" :data="consultations" stripe>
        <el-table-column label="患者" min-width="110" prop="patientName" />
        <el-table-column label="主要症状" min-width="240" show-overflow-tooltip prop="symptoms" />
        <el-table-column label="状态" min-width="100">
          <template #default="{ row }">
            <span :class="['status-tag', `status-${statusDisplay(row.status).tone}`]">
              {{ statusDisplay(row.status).label }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="紧急程度" min-width="110">
          <template #default="{ row }">
            <span :class="['status-tag', `status-${urgencyDisplay(row.urgency).tone}`]">
              {{ urgencyDisplay(row.urgency).label }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="提醒" min-width="110">
          <template #default="{ row }">{{ reminderDisplay(row.reminderLevel).label }}</template>
        </el-table-column>
        <el-table-column label="问诊科室" min-width="130">
          <template #default="{ row }">
            <span class="department-chip">{{ row.departmentName || '综合咨询' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="负责医生" min-width="145">
          <template #default="{ row }">
            <span v-if="row.doctorId" class="doctor-chip">
              {{ row.doctorName || '已分配医生' }}
              <small v-if="row.doctorDepartment">{{ row.doctorDepartment }}</small>
            </span>
            <span v-else class="unassigned-chip">尚未分配</span>
            <small v-if="isCrossDepartmentConsultation(row)" class="cross-department">
              跨科室处理
            </small>
          </template>
        </el-table-column>
        <el-table-column label="提交时间" min-width="170">
          <template #default="{ row }">{{ formatConsultationTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column fixed="right" label="操作" width="105">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetails(row)">查看调度</el-button>
          </template>
        </el-table-column>
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

    <el-drawer
      v-model="drawerVisible"
      size="min(580px, 94vw)"
      title="问诊调度详情"
    >
      <div v-if="selected" class="details">
        <div class="detail-summary">
          <span :class="['status-tag', `status-${urgencyDisplay(selected.urgency).tone}`]">
            {{ urgencyDisplay(selected.urgency).label }}
          </span>
          <time>{{ formatConsultationTime(selected.createdAt) }}</time>
          <h2>{{ selected.symptoms }}</h2>
          <p>{{ selected.reminderText || '暂无系统提醒' }}</p>
        </div>

        <dl>
          <div><dt>患者姓名</dt><dd>{{ selected.patientName }}</dd></div>
          <div><dt>年龄 / 性别</dt><dd>{{ selected.age || '未填' }} / {{ selected.gender || '未填' }}</dd></div>
          <div><dt>手机号</dt><dd>{{ selected.phone || '未填' }}</dd></div>
          <div><dt>问诊科室</dt><dd>{{ selected.departmentName || '综合咨询' }}</dd></div>
          <div><dt>持续时间</dt><dd>{{ selected.duration || '未填' }}</dd></div>
          <div><dt>过敏史</dt><dd>{{ selected.allergyHistory || '未填' }}</dd></div>
          <div><dt>患者备注</dt><dd>{{ selected.patientNote || '未填' }}</dd></div>
        </dl>

        <section class="assignment-panel department-panel">
          <div>
            <strong>问诊科室</strong>
            <span>转科会解除当前负责医生，并将问诊恢复为待接诊。</span>
          </div>
          <el-select
            v-model="consultationDepartmentId"
            :disabled="selected.status === '已完成'"
            placeholder="选择问诊科室"
          >
            <el-option
              v-for="department in departments"
              :key="department.id"
              :label="department.name"
              :value="department.id"
            />
          </el-select>
          <el-button
            :disabled="selected.status === '已完成'"
            :loading="assigning"
            @click="saveDepartment"
          >
            保存科室
          </el-button>
          <small v-if="selected.status === '已完成'">已完成问诊不能修改科室。</small>
        </section>

        <section class="assignment-panel">
          <div>
            <strong>负责医生</strong>
            <span>分配或转派后，问诊会回到待接诊状态。</span>
          </div>
          <el-select
            v-model="assignmentDoctorId"
            :disabled="selected.status === '已完成'"
            placeholder="选择负责医生"
          >
            <el-option label="暂不分配" value="" />
            <el-option
              v-for="doctor in assignmentDoctors"
              :key="doctor.userId"
              :label="`${doctor.displayName || doctor.username}${doctor.department ? ` · ${doctor.department}` : ''}`"
              :value="doctor.userId"
            />
          </el-select>
          <el-button
            type="primary"
            :disabled="selected.status === '已完成'"
            :loading="assigning"
            @click="saveAssignment"
          >
            保存分配
          </el-button>
          <small v-if="selected.status === '已完成'">已完成问诊不能重新分配。</small>
        </section>

      </div>
    </el-drawer>
  </section>
</template>

<style scoped>
.management-heading,
.filters {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.management-heading h1 {
  margin: 0;
  color: var(--color-ink);
  font-size: 30px;
}

.management-heading > span {
  color: var(--color-text-muted);
  font-size: 12px;
}

.filters {
  justify-content: start;
  margin-top: 22px;
  padding: 14px;
  border: 1px solid rgb(47 95 72 / 12%);
  border-radius: 20px;
  background:
    radial-gradient(circle at 0 50%, rgb(225 240 231 / 72%), transparent 25%),
    rgb(255 255 255 / 92%);
  box-shadow: 0 12px 32px rgb(21 56 42 / 6%);
}

.filters .el-input {
  width: 260px;
}

.filters .el-select {
  width: 160px;
}

.filters .assignment-filter {
  width: 190px;
}

.filters :deep(.el-input__wrapper),
.filters :deep(.el-select__wrapper) {
  min-height: 44px;
  border-radius: 13px;
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
  min-height: 44px;
  border-radius: 13px;
  font-weight: 800;
}

.filter-button {
  min-width: 78px;
  border: 0;
  background: linear-gradient(135deg, #174f3a, #2b7656);
  box-shadow: 0 7px 16px rgb(18 72 50 / 16%);
}

.reset-button {
  min-width: 68px;
  border-color: transparent;
  background: #eef5f1;
  color: #50685d;
}

.reset-button:hover {
  border-color: rgb(45 118 87 / 12%);
  background: #e3f0e8;
  color: #174f3a;
}

.table-card {
  margin-top: 14px;
  padding: 14px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: white;
  box-shadow: var(--shadow-card);
}

.status-tag {
  display: inline-flex;
  padding: 5px 8px;
  border-radius: 5px;
  background: var(--color-jade-light);
  color: var(--color-ink);
  font-size: 10px;
  font-weight: 800;
}

.status-active,
.status-attention {
  background: #fff0d6;
  color: #8a5c0f;
}

.status-urgent {
  background: var(--color-cinnabar-soft);
  color: #9f3f2e;
}

.status-complete {
  background: #e5eee9;
  color: var(--color-text-muted);
}

.doctor-chip,
.unassigned-chip,
.department-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 9px;
  border-radius: 999px;
  font-size: 10px;
  font-weight: 800;
}

.doctor-chip {
  background: #e7f2eb;
  color: var(--color-ink);
}

.department-chip {
  background: #edf3f0;
  color: #345f4a;
}

.doctor-chip small {
  color: var(--color-text-muted);
  font-size: 9px;
  font-weight: 600;
}

.unassigned-chip {
  background: #f2f3ef;
  color: var(--color-text-muted);
}

.cross-department {
  display: block;
  margin-top: 5px;
  color: var(--color-cinnabar);
  font-size: 9px;
  font-weight: 800;
}

.el-pagination {
  justify-content: center;
  margin-top: 18px;
}

.detail-summary {
  padding: 20px;
  border-radius: var(--radius-sm);
  background: var(--color-mist);
}

.detail-summary time {
  float: right;
  color: var(--color-text-muted);
  font-size: 11px;
}

.detail-summary h2 {
  margin: 18px 0 8px;
  color: var(--color-ink);
  font-size: 20px;
  line-height: 1.6;
}

.detail-summary p {
  margin: 0;
  color: var(--color-text-muted);
  font-size: 12px;
  line-height: 1.7;
}

dl {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
  margin: 20px 0;
}

dl div {
  padding: 12px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
}

dt {
  color: var(--color-text-muted);
  font-size: 10px;
}

dd {
  margin: 6px 0 0;
  font-size: 12px;
  line-height: 1.6;
}

.assignment-panel {
  display: grid;
  gap: 14px;
  padding: 18px;
  border: 1px solid rgb(47 95 72 / 12%);
  border-radius: 18px;
  background: #f5f8f5;
}

.department-panel {
  margin-bottom: 14px;
  background: #f8faf8;
}

.assignment-panel > div strong,
.assignment-panel > div span {
  display: block;
}

.assignment-panel > div strong {
  color: var(--color-ink);
  font-size: 15px;
}

.assignment-panel > div span,
.assignment-panel > small {
  margin-top: 5px;
  color: var(--color-text-muted);
  font-size: 10px;
  line-height: 1.6;
}

@media (max-width: 900px) {
  .filters {
    flex-wrap: wrap;
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
