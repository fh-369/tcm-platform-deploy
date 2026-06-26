<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'

import {
  getDoctorConsultationMessages,
  getMyDoctorConsultations,
  sendDoctorConsultationMessage,
  updateDoctorConsultation,
} from '../../api/doctorConsultation'
import { messageAuthorLabel } from '../../features/consultation/communication'
import {
  formatConsultationTime,
  reminderDisplay,
  statusDisplay,
  urgencyDisplay,
} from '../../features/consultation/display'
import { getDoctorWorkflow } from '../../features/consultation/workflow'
import { getApiErrorMessage as errorMessage } from '../../features/feedback'

const loading = ref(false)
const saving = ref(false)
const drawerVisible = ref(false)
const consultations = ref([])
const selected = ref(null)
const total = ref(0)
const filters = reactive({
  current: 1,
  size: 10,
  status: '',
  urgency: '',
  keyword: '',
})
const updateForm = reactive({
  doctorNote: '',
})
const messageState = reactive({
  loading: false,
  sending: false,
  error: '',
  messages: [],
})
const workflow = computed(() => getDoctorWorkflow(selected.value?.status))

async function loadConsultations() {
  loading.value = true
  try {
    const page = await getMyDoctorConsultations({
      current: filters.current,
      size: filters.size,
      status: filters.status || undefined,
      urgency: filters.urgency || undefined,
      keyword: filters.keyword || undefined,
    })
    consultations.value = page.records || []
    total.value = page.total || 0
  } catch (error) {
    ElMessage.error(errorMessage(error, '我的问诊加载失败'))
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
    status: '',
    urgency: '',
    keyword: '',
  })
  loadConsultations()
}

async function loadMessages(id) {
  messageState.loading = true
  messageState.error = ''
  try {
    messageState.messages = await getDoctorConsultationMessages(id)
  } catch (error) {
    messageState.error = errorMessage(error, '沟通记录加载失败')
  } finally {
    messageState.loading = false
  }
}

function openDetails(item) {
  selected.value = item
  updateForm.doctorNote = ''
  messageState.messages = []
  messageState.error = ''
  drawerVisible.value = true
  loadMessages(item.id)
}

async function submitUpdate(payload, successMessage) {
  saving.value = true
  try {
    const updated = await updateDoctorConsultation(selected.value.id, payload)
    selected.value = { ...selected.value, ...updated }
    updateForm.doctorNote = ''
    ElMessage.success(successMessage)
    if (payload.doctorNote) {
      await loadMessages(selected.value.id)
    }
    await loadConsultations()
  } catch (error) {
    ElMessage.error(errorMessage(error, '问诊更新失败'))
  } finally {
    saving.value = false
  }
}

function startConsultation() {
  submitUpdate({ status: '接诊中' }, '已开始接诊')
}

async function saveReply() {
  const doctorNote = updateForm.doctorNote.trim()
  if (!doctorNote) {
    ElMessage.warning('请先填写本次医生回复')
    return
  }
  messageState.sending = true
  try {
    const message = await sendDoctorConsultationMessage(selected.value.id, doctorNote)
    messageState.messages.push(message)
    updateForm.doctorNote = ''
    ElMessage.success('医生回复已发送')
  } catch (error) {
    ElMessage.error(errorMessage(error, '医生回复发送失败'))
  } finally {
    messageState.sending = false
  }
}

function completeConsultation() {
  const doctorNote = updateForm.doctorNote.trim()
  if (!doctorNote) {
    ElMessage.warning('完成问诊前请填写本次医生回复')
    return
  }
  submitUpdate({ status: '已完成', doctorNote }, '问诊已完成')
}

onMounted(loadConsultations)
</script>

<template>
  <section class="doctor-page">
    <header class="page-heading">
      <h1>我的问诊</h1>
      <div class="record-count">
        <strong>{{ total }}</strong>
        <small>张负责问诊</small>
      </div>
    </header>

    <section class="filters">
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
      <el-button class="filter-button" type="primary" @click="search">筛选</el-button>
      <el-button class="reset-button" @click="resetFilters">重置</el-button>
    </section>

    <section class="table-card">
      <el-table v-loading="loading" :data="consultations" stripe>
        <el-table-column label="患者" min-width="110" prop="patientName" />
        <el-table-column label="主要症状" min-width="260" show-overflow-tooltip prop="symptoms" />
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
        <el-table-column label="问诊科室" min-width="130">
          <template #default="{ row }">
            <span class="department-chip">{{ row.departmentName || '综合咨询' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="提醒" min-width="110">
          <template #default="{ row }">{{ reminderDisplay(row.reminderLevel).label }}</template>
        </el-table-column>
        <el-table-column label="更新时间" min-width="170">
          <template #default="{ row }">
            {{ formatConsultationTime(row.updatedAt || row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column fixed="right" label="操作" width="105">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetails(row)">查看处理</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="当前筛选范围内没有负责的问诊" />
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

    <el-drawer v-model="drawerVisible" size="min(580px, 94vw)" title="问诊处理详情">
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
          <div><dt>问诊科室</dt><dd>{{ selected.departmentName || '综合咨询' }}</dd></div>
          <div><dt>持续时间</dt><dd>{{ selected.duration || '未填' }}</dd></div>
          <div><dt>过敏史</dt><dd>{{ selected.allergyHistory || '未填' }}</dd></div>
          <div><dt>患者备注</dt><dd>{{ selected.patientNote || '未填' }}</dd></div>
        </dl>

        <section class="progress-panel">
          <header>
            <div>
              <h3>处理时间线</h3>
              <p>每次状态变化和医生回复都会保留。</p>
            </div>
            <span>{{ selected.progressRecords?.length || 0 }} 条记录</span>
          </header>
          <el-timeline v-if="selected.progressRecords?.length">
            <el-timeline-item
              v-for="record in selected.progressRecords"
              :key="record.id"
              :timestamp="formatConsultationTime(record.createdAt)"
              placement="top"
            >
              <article class="progress-entry">
                <div class="progress-entry-heading">
                  <strong>{{ record.doctorName || '接诊医生' }}</strong>
                  <span>{{ record.previousStatus }} → {{ record.status }}</span>
                </div>
                <p v-if="record.doctorNote">{{ record.doctorNote }}</p>
              </article>
            </el-timeline-item>
          </el-timeline>
          <el-empty v-else :image-size="64" description="尚无处理记录" />
        </section>

        <section class="conversation-panel">
          <header>
            <div>
              <h3>医患沟通</h3>
              <p>患者补充和医生回复按时间顺序保留。</p>
            </div>
            <span>{{ messageState.messages.length }} 条消息</span>
          </header>

          <div v-if="messageState.loading" class="conversation-empty">正在加载沟通记录...</div>
          <div v-else-if="messageState.error" class="conversation-empty error">
            <p>{{ messageState.error }}</p>
            <el-button link type="primary" @click="loadMessages(selected.id)">重新加载</el-button>
          </div>
          <div v-else-if="messageState.messages.length" class="message-list">
            <article
              v-for="message in messageState.messages"
              :key="message.id"
              :class="['message-item', `message-${message.senderType}`]"
            >
              <div>
                <strong>{{ messageAuthorLabel(message) }}</strong>
                <time>{{ formatConsultationTime(message.createdAt) }}</time>
              </div>
              <p>{{ message.content }}</p>
            </article>
          </div>
          <div v-else class="conversation-empty">暂时没有沟通记录。</div>
        </section>

        <section v-if="workflow.canStart" class="workflow-action start-action">
          <div>
            <strong>准备开始接诊</strong>
            <p>开始后问诊将进入处理中，随后可以逐条追加医生回复。</p>
          </div>
          <el-button type="primary" :loading="saving" @click="startConsultation">
            开始接诊
          </el-button>
        </section>

        <el-form v-else-if="workflow.canReply" label-position="top">
          <el-form-item label="回复患者">
            <el-input
              v-model="updateForm.doctorNote"
              :rows="5"
              maxlength="2000"
              show-word-limit
              placeholder="填写患者可以看到的回复内容"
              type="textarea"
            />
          </el-form-item>
          <div class="workflow-buttons">
            <el-button :loading="messageState.sending" @click="saveReply">发送回复</el-button>
            <el-button type="primary" :loading="saving" @click="completeConsultation">
              完成问诊
            </el-button>
          </div>
        </el-form>

        <section v-else class="workflow-action completed-action">
          <div>
            <strong>问诊已完成</strong>
            <p>当前记录仅支持查看，历史回复会继续保留在处理时间线中。</p>
          </div>
        </section>
      </div>
    </el-drawer>
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
  border: 0;
  border-radius: 26px;
  background:
    radial-gradient(circle at 88% 12%, rgb(164 211 184 / 28%), transparent 32%),
    linear-gradient(132deg, #0d4934 0%, #145b40 54%, #2f7a5b 100%);
  box-shadow: 0 18px 42px rgb(18 65 47 / 14%);
}

.page-heading h1 {
  margin: 0;
  color: white;
  font-family: "Noto Serif SC", "STSong", serif;
  font-size: 32px;
  letter-spacing: .015em;
  line-height: 1.2;
}

.record-count {
  display: inline-flex;
  min-height: 58px;
  align-items: center;
  gap: 12px;
  padding: 0 20px;
  border: 1px solid rgb(255 255 255 / 22%);
  border-radius: 999px;
  background: rgb(255 255 255 / 12%);
  box-shadow: inset 0 1px 0 rgb(255 255 255 / 12%);
  color: white;
  backdrop-filter: blur(14px);
}

.record-count strong {
  font-family: "Noto Serif SC", "STSong", serif;
  font-size: 30px;
  line-height: 1;
}

.record-count small {
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
    radial-gradient(circle at 0 50%, rgb(225 240 231 / 68%), transparent 28%),
    rgb(255 255 255 / 90%);
  box-shadow: 0 10px 28px rgb(21 56 42 / 5%);
}

.filters .el-input {
  width: 280px;
}

.filters .el-select {
  width: 160px;
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
  background: #e7f2eb;
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

.progress-panel {
  margin: 20px 0;
  padding: 18px;
  border: 1px solid var(--color-border);
  border-radius: 18px;
  background: #f8fbf9;
}

.conversation-panel {
  margin: 20px 0;
  padding: 18px;
  border: 1px solid var(--color-border);
  border-radius: 18px;
  background: #fffaf7;
}

.conversation-panel > header,
.message-item > div {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.conversation-panel h3 {
  margin: 0;
  color: var(--color-ink);
  font-size: 18px;
}

.conversation-panel header p,
.conversation-panel header > span,
.message-item time {
  color: var(--color-text-muted);
  font-size: 11px;
}

.conversation-panel header p {
  margin: 5px 0 0;
}

.message-list {
  display: grid;
  max-height: 360px;
  gap: 10px;
  margin-top: 16px;
  overflow-y: auto;
  padding-right: 4px;
}

.message-item {
  padding: 13px 14px;
  border-radius: 14px;
}

.message-doctor {
  margin-left: 34px;
  background: #eaf4ee;
}

.message-patient {
  margin-right: 34px;
  background: #fff0ea;
}

.message-item strong {
  color: var(--color-ink);
  font-size: 13px;
}

.message-item p {
  margin: 8px 0 0;
  color: #365a49;
  font-size: 13px;
  line-height: 1.75;
  white-space: pre-wrap;
}

.conversation-empty {
  margin-top: 16px;
  color: var(--color-text-muted);
  font-size: 12px;
}

.conversation-empty.error {
  color: var(--color-cinnabar);
}

.progress-panel > header,
.progress-entry-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.progress-panel h3 {
  margin: 0;
  color: var(--color-ink);
  font-size: 18px;
}

.progress-panel header p {
  margin: 5px 0 0;
  color: var(--color-text-muted);
  font-size: 11px;
}

.progress-panel header > span,
.progress-entry-heading span {
  color: var(--color-text-muted);
  font-size: 11px;
}

.progress-panel :deep(.el-timeline) {
  margin-top: 20px;
  padding-left: 5px;
}

.progress-entry {
  padding: 14px;
  border: 1px solid rgb(47 95 72 / 10%);
  border-radius: 14px;
  background: white;
}

.progress-entry strong {
  color: var(--color-ink);
  font-size: 13px;
}

.progress-entry p {
  margin: 10px 0 0;
  color: #365a49;
  font-size: 13px;
  line-height: 1.7;
  white-space: pre-wrap;
}

.workflow-action {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 18px;
  border: 1px solid var(--color-border);
  border-radius: 18px;
  background: white;
}

.workflow-action strong {
  color: var(--color-ink);
  font-size: 16px;
}

.workflow-action p {
  margin: 6px 0 0;
  color: var(--color-text-muted);
  font-size: 12px;
  line-height: 1.6;
}

.completed-action {
  background: var(--color-mist);
}

.workflow-buttons {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
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
