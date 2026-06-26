<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

import { getDepartments } from '../../api/auth'
import {
  getPersonnel,
  reviewDoctor,
  updateAccountEnabled,
  updateDoctorProfile,
} from '../../api/personnel'
import {
  approvalMeta,
  canEnableDoctor,
  doctorReviewActions,
} from '../../features/admin/doctorAdmission'
import { getApiErrorMessage as errorMessage } from '../../features/feedback'

const props = defineProps({
  resource: {
    type: String,
    required: true,
    validator: (value) => ['users', 'doctors'].includes(value),
  },
})

const loading = ref(false)
const updatingId = ref(null)
const records = ref([])
const total = ref(0)
const departments = ref([])
const reviewVisible = ref(false)
const profileVisible = ref(false)
const activeDoctor = ref(null)
const reviewForm = reactive({
  approvalStatus: 'APPROVED',
  approvalNote: '',
})
const profileForm = reactive({
  displayName: '',
  departmentId: null,
  phone: '',
  qualification: '',
  profile: '',
})
const filters = reactive({
  current: 1,
  size: 10,
  keyword: '',
  approvalStatus: '',
})

const isDoctors = computed(() => props.resource === 'doctors')
const reviewOptions = computed(() => doctorReviewActions(activeDoctor.value).map((value) => ({
  value,
  label: value === 'APPROVED' ? '通过申请' : '驳回申请',
})))
const pageCopy = computed(() => (
  isDoctors.value
    ? {
        title: '医生管理',
        search: '搜索用户名、医生姓名、科室或联系电话',
        empty: '暂无医生账号',
      }
    : {
        title: '用户管理',
        search: '搜索用户名、昵称或手机号',
        empty: '暂无注册用户',
      }
))

function formatTime(value) {
  if (!value) return '暂无'
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(value))
}

async function loadPersonnel() {
  loading.value = true
  try {
    const page = await getPersonnel(props.resource, filters)
    records.value = page.records || []
    total.value = Number(page.total || 0)
  } catch (error) {
    ElMessage.error(errorMessage(error, '人员列表加载失败'))
  } finally {
    loading.value = false
  }
}

async function loadDepartments() {
  if (!isDoctors.value) return
  try {
    departments.value = await getDepartments()
  } catch (error) {
    ElMessage.error(errorMessage(error, '科室信息加载失败'))
  }
}

function search() {
  filters.current = 1
  loadPersonnel()
}

function resetFilters() {
  filters.current = 1
  filters.keyword = ''
  filters.approvalStatus = ''
  loadPersonnel()
}

async function changeStatus(row, enabled) {
  if (isDoctors.value && enabled && !canEnableDoctor(row)) {
    ElMessage.warning('医生尚未通过审核，不能启用账号')
    return
  }
  const action = enabled ? '启用' : '停用'
  try {
    await ElMessageBox.confirm(
      `${action}账号“${row.displayName || row.username}”后，${enabled ? '该账号可以正常登录平台。' : '该账号现有登录状态也将失效。'}`,
      `${action}账号`,
      {
        confirmButtonText: `确认${action}`,
        cancelButtonText: '取消',
        type: enabled ? 'success' : 'warning',
      },
    )
  } catch {
    return
  }

  updatingId.value = row.id
  try {
    await updateAccountEnabled(row.id, enabled)
    row.enabled = enabled
    ElMessage.success(`账号已${action}`)
  } catch (error) {
    ElMessage.error(errorMessage(error, `${action}账号失败`))
  } finally {
    updatingId.value = null
  }
}

function openReview(row) {
  activeDoctor.value = row
  reviewForm.approvalStatus = doctorReviewActions(row)[0] || 'APPROVED'
  reviewForm.approvalNote = row.approvalNote || ''
  reviewVisible.value = true
}

async function submitReview() {
  if (reviewForm.approvalStatus === 'REJECTED' && !reviewForm.approvalNote.trim()) {
    ElMessage.warning('驳回申请时请填写审核备注')
    return
  }

  updatingId.value = activeDoctor.value.id
  try {
    const result = await reviewDoctor(activeDoctor.value.id, {
      approvalStatus: reviewForm.approvalStatus,
      approvalNote: reviewForm.approvalNote.trim(),
    })
    activeDoctor.value.approvalStatus = result.approvalStatus
    activeDoctor.value.approvalNote = result.approvalNote
    activeDoctor.value.enabled = result.enabled
    reviewVisible.value = false
    ElMessage.success(result.approvalStatus === 'APPROVED' ? '医生申请已通过' : '医生申请已驳回')
    await loadPersonnel()
  } catch (error) {
    ElMessage.error(errorMessage(error, '医生申请审核失败'))
  } finally {
    updatingId.value = null
  }
}

function openProfile(row) {
  activeDoctor.value = row
  Object.assign(profileForm, {
    displayName: row.displayName || '',
    departmentId: row.departmentId || null,
    phone: row.phone || '',
    qualification: row.qualification || '',
    profile: row.profile || '',
  })
  profileVisible.value = true
}

async function submitProfile() {
  if (!profileForm.displayName.trim()) {
    ElMessage.warning('请填写医生姓名')
    return
  }
  if (!profileForm.departmentId) {
    ElMessage.warning('请选择所属科室')
    return
  }
  if (!/^1\d{10}$/.test(profileForm.phone.trim())) {
    ElMessage.warning('请输入 11 位手机号')
    return
  }
  if (!profileForm.qualification.trim()) {
    ElMessage.warning('请填写资质或执业信息')
    return
  }

  updatingId.value = activeDoctor.value.id
  try {
    await updateDoctorProfile(activeDoctor.value.id, {
      displayName: profileForm.displayName.trim(),
      departmentId: profileForm.departmentId,
      phone: profileForm.phone.trim(),
      qualification: profileForm.qualification.trim(),
      profile: profileForm.profile.trim(),
    })
    profileVisible.value = false
    ElMessage.success('医生资料已更新')
    await loadPersonnel()
  } catch (error) {
    ElMessage.error(errorMessage(error, '医生资料更新失败'))
  } finally {
    updatingId.value = null
  }
}

watch(
  () => props.resource,
  () => {
    Object.assign(filters, { current: 1, size: 10, keyword: '', approvalStatus: '' })
    loadDepartments()
    loadPersonnel()
  },
)

onMounted(() => {
  loadDepartments()
  loadPersonnel()
})
</script>

<template>
  <section class="personnel-page">
    <header class="personnel-heading">
      <h1>{{ pageCopy.title }}</h1>
      <div class="total-badge">
        <strong>{{ total }}</strong>
        <span>{{ isDoctors ? '名医生' : '位用户' }}</span>
      </div>
    </header>

    <section class="personnel-toolbar">
      <el-input
        v-model="filters.keyword"
        class="search-input"
        clearable
        :placeholder="pageCopy.search"
        @clear="search"
        @keyup.enter="search"
      >
        <template #prefix><span class="search-mark">⌕</span></template>
      </el-input>
      <el-select
        v-if="isDoctors"
        v-model="filters.approvalStatus"
        clearable
        placeholder="全部审核状态"
        popper-class="jade-select-popper"
        @change="search"
      >
        <el-option label="待审核" value="PENDING" />
        <el-option label="已通过" value="APPROVED" />
        <el-option label="未通过" value="REJECTED" />
      </el-select>
      <el-button class="filter-button" type="primary" @click="search">搜索</el-button>
      <el-button class="reset-button" @click="resetFilters">重置</el-button>
    </section>

    <section class="personnel-table-card">
      <el-table v-loading="loading" :data="records" row-key="id">
        <el-table-column label="账号" min-width="190">
          <template #default="{ row }">
            <div class="identity-cell">
              <span>{{ (row.displayName || row.username || '?').slice(0, 1) }}</span>
              <div>
                <strong>{{ row.displayName || row.username }}</strong>
                <small>@{{ row.username }}</small>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column v-if="isDoctors" label="科室" min-width="150">
          <template #default="{ row }">{{ row.department || '暂未设置' }}</template>
        </el-table-column>
        <el-table-column v-if="isDoctors" label="联系电话" min-width="145">
          <template #default="{ row }">{{ row.phone || '暂未填写' }}</template>
        </el-table-column>
        <el-table-column v-else label="手机号" min-width="150">
          <template #default="{ row }">{{ row.phone || '暂未填写' }}</template>
        </el-table-column>

        <el-table-column label="注册时间" min-width="180">
          <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
        </el-table-column>

        <el-table-column v-if="isDoctors" label="审核状态" min-width="130">
          <template #default="{ row }">
            <span :class="['approval-status', approvalMeta(row.approvalStatus).tone]">
              {{ approvalMeta(row.approvalStatus).label }}
            </span>
          </template>
        </el-table-column>

        <el-table-column label="账号状态" min-width="130">
          <template #default="{ row }">
            <span :class="['account-status', { disabled: !row.enabled }]">
              <i></i>
              {{ row.enabled ? '正常使用' : '已停用' }}
            </span>
          </template>
        </el-table-column>

        <el-table-column align="right" label="操作" :width="isDoctors ? 250 : 130">
          <template #default="{ row }">
            <el-button
              v-if="isDoctors && doctorReviewActions(row).length"
              link
              type="primary"
              @click="openReview(row)"
            >
              审核申请
            </el-button>
            <el-button
              v-if="isDoctors"
              link
              @click="openProfile(row)"
            >
              编辑资料
            </el-button>
            <el-button
              link
              :loading="updatingId === row.id"
              :type="row.enabled ? 'danger' : 'success'"
              :disabled="isDoctors && !row.enabled && !canEnableDoctor(row)"
              @click="changeStatus(row, !row.enabled)"
            >
              {{ row.enabled ? '停用账号' : '恢复账号' }}
            </el-button>
          </template>
        </el-table-column>

        <template #empty>
          <div class="empty-state">
            <strong>{{ pageCopy.empty }}</strong>
            <span>可以尝试调整搜索关键词后重新查询。</span>
          </div>
        </template>
      </el-table>

      <el-pagination
        v-if="total > filters.size"
        background
        layout="prev, pager, next"
        :current-page="filters.current"
        :page-size="filters.size"
        :total="total"
        @current-change="(page) => { filters.current = page; loadPersonnel() }"
      />
    </section>

    <el-dialog
      v-model="reviewVisible"
      class="doctor-dialog"
      title="审核医生申请"
      width="520"
    >
      <div v-if="activeDoctor" class="review-summary">
        <span>{{ (activeDoctor.displayName || activeDoctor.username).slice(0, 1) }}</span>
        <div>
          <strong>{{ activeDoctor.displayName || activeDoctor.username }}</strong>
          <small>{{ activeDoctor.department || '暂未设置科室' }} · @{{ activeDoctor.username }}</small>
        </div>
      </div>

      <el-form label-position="top">
        <el-form-item label="审核结果">
          <el-segmented
            v-model="reviewForm.approvalStatus"
            :options="reviewOptions"
          />
        </el-form-item>
        <el-form-item :label="reviewForm.approvalStatus === 'REJECTED' ? '驳回原因' : '审核备注'">
          <el-input
            v-model="reviewForm.approvalNote"
            :rows="3"
            maxlength="500"
            :placeholder="reviewForm.approvalStatus === 'REJECTED'
              ? '请说明需要补充或修正的资料'
              : '可填写资料核验说明，选填'"
            resize="none"
            show-word-limit
            type="textarea"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="reviewVisible = false">取消</el-button>
        <el-button
          type="primary"
          :loading="updatingId === activeDoctor?.id"
          @click="submitReview"
        >
          确认审核
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="profileVisible"
      class="doctor-dialog"
      title="编辑医生资料"
      width="620"
    >
      <el-form label-position="top">
        <div class="dialog-field-grid">
          <el-form-item label="医生姓名">
            <el-input v-model="profileForm.displayName" />
          </el-form-item>
          <el-form-item label="所属科室">
            <el-select v-model="profileForm.departmentId" placeholder="请选择科室">
              <el-option
                v-for="department in departments"
                :key="department.id"
                :label="department.name"
                :value="department.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="联系电话">
            <el-input v-model="profileForm.phone" maxlength="11" />
          </el-form-item>
          <el-form-item label="资质或执业信息">
            <el-input v-model="profileForm.qualification" maxlength="500" />
          </el-form-item>
        </div>
        <el-form-item label="个人简介">
          <el-input
            v-model="profileForm.profile"
            :rows="4"
            maxlength="1000"
            resize="none"
            show-word-limit
            type="textarea"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="profileVisible = false">取消</el-button>
        <el-button
          type="primary"
          :loading="updatingId === activeDoctor?.id"
          @click="submitProfile"
        >
          保存资料
        </el-button>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped>
.personnel-page {
  max-width: 1480px;
  margin: 0 auto;
}

.personnel-heading {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 24px;
}

.personnel-heading h1 {
  margin: 0;
  color: var(--color-ink);
  font-family: "Noto Serif SC", "STSong", serif;
  font-size: 32px;
  letter-spacing: -.04em;
}

.total-badge {
  display: flex;
  min-width: 112px;
  align-items: baseline;
  justify-content: center;
  gap: 6px;
  padding: 12px 18px;
  border: 1px solid rgb(47 95 72 / 12%);
  border-radius: 18px;
  background: rgb(255 255 255 / 82%);
}

.total-badge strong {
  color: var(--color-ink);
  font-family: "Noto Serif SC", "STSong", serif;
  font-size: 24px;
}

.total-badge span {
  color: var(--color-text-muted);
  font-size: 10px;
}

.personnel-toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 22px;
  padding: 14px;
  border: 1px solid rgb(47 95 72 / 12%);
  border-radius: 20px;
  background:
    radial-gradient(circle at 0 50%, rgb(225 240 231 / 72%), transparent 30%),
    rgb(255 255 255 / 92%);
  box-shadow: 0 12px 32px rgb(21 56 42 / 6%);
}

.personnel-toolbar .el-input {
  width: min(420px, 100%);
}

.personnel-toolbar .el-select {
  width: 170px;
}

.personnel-toolbar :deep(.el-input__wrapper),
.personnel-toolbar :deep(.el-select__wrapper) {
  min-height: 44px;
  border-radius: 13px;
  background: #f7faf8;
  box-shadow: inset 0 0 0 1px rgb(43 92 68 / 12%);
  transition: box-shadow .18s ease, background .18s ease;
}

.personnel-toolbar :deep(.el-input__wrapper:hover),
.personnel-toolbar :deep(.el-select__wrapper:hover) {
  background: white;
  box-shadow: inset 0 0 0 1px rgb(39 101 72 / 28%);
}

.personnel-toolbar :deep(.el-input__wrapper.is-focus),
.personnel-toolbar :deep(.el-select__wrapper.is-focused) {
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

.personnel-table-card {
  margin-top: 14px;
  padding: 10px 14px 16px;
  border: 1px solid rgb(47 95 72 / 10%);
  border-radius: 22px;
  background: rgb(255 255 255 / 90%);
  box-shadow: 0 14px 34px rgb(21 56 42 / 5%);
}

.identity-cell {
  display: flex;
  align-items: center;
  gap: 11px;
}

.identity-cell > span {
  display: grid;
  width: 38px;
  height: 38px;
  flex: 0 0 auto;
  border-radius: 12px;
  background: #e5f0e9;
  color: var(--color-ink);
  font-family: "Noto Serif SC", "STSong", serif;
  font-size: 15px;
  font-weight: 900;
  place-items: center;
}

.identity-cell strong,
.identity-cell small {
  display: block;
}

.identity-cell strong {
  color: var(--color-ink);
  font-size: 13px;
}

.identity-cell small {
  margin-top: 4px;
  color: var(--color-text-muted);
  font-size: 10px;
}

.account-status {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  padding: 6px 10px;
  border-radius: 999px;
  background: #e7f2eb;
  color: #256343;
  font-size: 10px;
  font-weight: 800;
}

.account-status i {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #3b8a60;
}

.account-status.disabled {
  background: #f4e8e5;
  color: #9b4738;
}

.account-status.disabled i {
  background: var(--color-cinnabar);
}

.approval-status {
  display: inline-flex;
  min-width: 62px;
  justify-content: center;
  padding: 6px 10px;
  border-radius: 999px;
  font-size: 10px;
  font-weight: 900;
}

.approval-status.pending {
  background: #f6ecd8;
  color: #9a6a16;
}

.approval-status.approved {
  background: #e4f1e9;
  color: #256343;
}

.approval-status.rejected {
  background: #f6e6e2;
  color: #a64939;
}

.approval-status.unknown {
  background: #edf0ee;
  color: #6c7972;
}

.review-summary {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: -4px 0 22px;
  padding: 14px;
  border: 1px solid rgb(47 95 72 / 10%);
  border-radius: 15px;
  background: #f5f8f6;
}

.review-summary > span {
  display: grid;
  width: 42px;
  height: 42px;
  border-radius: 13px;
  background: var(--color-ink);
  color: white;
  font-family: "Noto Serif SC", "STSong", serif;
  font-weight: 900;
  place-items: center;
}

.review-summary strong,
.review-summary small {
  display: block;
}

.review-summary strong {
  color: var(--color-ink);
}

.review-summary small {
  margin-top: 5px;
  color: var(--color-text-muted);
  font-size: 11px;
}

.dialog-field-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0 14px;
}

.dialog-field-grid .el-select {
  width: 100%;
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

:deep(.doctor-dialog) {
  border-radius: 22px;
  overflow: hidden;
}

@media (max-width: 900px) {
  .personnel-toolbar {
    flex-wrap: wrap;
  }

  .personnel-toolbar .el-input {
    width: 100%;
  }
}

:deep(.doctor-dialog .el-dialog__header) {
  padding: 22px 24px 14px;
}

:deep(.doctor-dialog .el-dialog__title) {
  color: var(--color-ink);
  font-family: "Noto Serif SC", "STSong", serif;
  font-size: 22px;
  font-weight: 800;
}

:deep(.doctor-dialog .el-dialog__body) {
  padding: 12px 24px;
}

:deep(.doctor-dialog .el-dialog__footer) {
  padding: 14px 24px 22px;
}

:deep(.doctor-dialog .el-segmented) {
  width: 100%;
}

.empty-state {
  display: grid;
  min-height: 180px;
  place-content: center;
  text-align: center;
}

.empty-state strong {
  color: var(--color-ink);
  font-size: 16px;
}

.empty-state span {
  margin-top: 8px;
  color: var(--color-text-muted);
  font-size: 11px;
}

.el-pagination {
  justify-content: center;
  margin-top: 18px;
}

@media (max-width: 760px) {
  .personnel-heading,
  .personnel-toolbar {
    align-items: stretch;
    flex-direction: column;
  }

  .total-badge {
    align-self: flex-start;
  }
}
</style>
