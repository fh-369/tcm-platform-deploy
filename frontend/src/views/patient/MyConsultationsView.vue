<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { ArrowDown, Bell, ChatLineRound, Plus, User } from '@element-plus/icons-vue'

import {
  getConsultationMessages,
  getMyConsultations,
  sendConsultationMessage,
} from '../../api/consultation'
import {
  canReplyToConsultation,
  latestMessagePreview,
  messageAuthorLabel,
} from '../../features/consultation/communication'
import {
  formatConsultationTime,
  reminderDisplay,
  statusDisplay,
  urgencyDisplay,
} from '../../features/consultation/display'

const loading = ref(false)
const consultations = ref([])
const total = ref(0)
const expandedId = ref(null)
const communication = reactive({})
const filters = reactive({
  current: 1,
  size: 6,
  status: '',
  urgency: '',
})

function errorMessage(error) {
  return error.response?.data?.message || error.message || '问诊记录加载失败，请稍后重试'
}

async function loadConsultations() {
  loading.value = true
  try {
    const page = await getMyConsultations(filters)
    consultations.value = page.records || []
    total.value = page.total || 0
  } catch (error) {
    ElMessage.error(errorMessage(error))
  } finally {
    loading.value = false
  }
}

function applyFilters() {
  filters.current = 1
  loadConsultations()
}

function changePage(page) {
  filters.current = page
  loadConsultations()
}

function communicationState(id) {
  if (!communication[id]) {
    communication[id] = {
      loaded: false,
      loading: false,
      sending: false,
      error: '',
      draft: '',
      messages: [],
    }
  }
  return communication[id]
}

async function loadMessages(item) {
  const state = communicationState(item.id)
  state.loading = true
  state.error = ''
  try {
    state.messages = await getConsultationMessages(item.id)
    state.loaded = true
  } catch (error) {
    state.error = errorMessage(error)
  } finally {
    state.loading = false
  }
}

async function toggleDetails(item) {
  if (expandedId.value === item.id) {
    expandedId.value = null
    return
  }
  expandedId.value = item.id
  const state = communicationState(item.id)
  if (!state.loaded && !state.loading) {
    await loadMessages(item)
  }
}

async function sendMessage(item) {
  const state = communicationState(item.id)
  const content = state.draft.trim()
  if (!content) {
    ElMessage.warning('请先填写回复内容')
    return
  }
  state.sending = true
  try {
    const message = await sendConsultationMessage(item.id, content)
    state.messages.push(message)
    state.draft = ''
    item.messageCount = state.messages.length
    item.latestMessage = message.content
    item.latestMessageSenderType = message.senderType
    item.latestMessageAt = message.createdAt
    ElMessage.success('回复已发送')
  } catch (error) {
    ElMessage.error(errorMessage(error))
  } finally {
    state.sending = false
  }
}

onMounted(loadConsultations)
</script>

<template>
  <section class="records-page page-container">
    <section class="filters" aria-label="问诊记录筛选">
      <h1>我的问诊</h1>
      <el-select v-model="filters.status" clearable placeholder="全部状态" @change="applyFilters">
        <el-option label="待接诊" value="待接诊" />
        <el-option label="接诊中" value="接诊中" />
        <el-option label="已完成" value="已完成" />
      </el-select>
      <el-select v-model="filters.urgency" clearable placeholder="全部紧急程度" @change="applyFilters">
        <el-option label="普通" value="普通" />
        <el-option label="紧急" value="紧急" />
        <el-option label="非常紧急" value="非常紧急" />
      </el-select>
      <span>共 {{ total }} 条问诊记录</span>
      <RouterLink class="new-consultation-link" to="/consultation/new">
        <el-icon><Plus /></el-icon>
        新建问诊单
      </RouterLink>
    </section>

    <div v-loading="loading" class="records">
      <article v-for="item in consultations" :key="item.id" class="record-card">
        <header class="record-header">
          <div class="record-tags">
            <span :class="['tag', `tag-${statusDisplay(item.status).tone}`]">
              {{ statusDisplay(item.status).label }}
            </span>
            <span :class="['tag', `tag-${urgencyDisplay(item.urgency).tone}`]">
              {{ urgencyDisplay(item.urgency).label }}
            </span>
            <span class="tag tag-department">
              {{ item.departmentName || '综合咨询' }}
            </span>
          </div>
          <time>{{ formatConsultationTime(item.createdAt) }}</time>
        </header>

        <section class="symptom-summary">
          <span>主要症状</span>
          <h2>{{ item.symptoms }}</h2>
        </section>

        <div class="record-details">
          <section class="detail-card patient-detail">
            <header>
              <el-icon><User /></el-icon>
              <h3>患者信息</h3>
            </header>
            <dl>
              <div>
                <dt>患者</dt>
                <dd>{{ item.patientName }}</dd>
              </div>
              <div>
                <dt>年龄 / 性别</dt>
                <dd>{{ item.age || '未填写' }} / {{ item.gender || '未填写' }}</dd>
              </div>
              <div>
                <dt>持续时间</dt>
                <dd>{{ item.duration || '未填写' }}</dd>
              </div>
              <div>
                <dt>问诊科室</dt>
                <dd>{{ item.departmentName || '综合咨询' }}</dd>
              </div>
            </dl>
            <p :class="['reminder-level', `text-${reminderDisplay(item.reminderLevel).tone}`]">
              {{ reminderDisplay(item.reminderLevel).label }}
            </p>
          </section>

          <section class="detail-card feedback-detail">
            <header>
              <el-icon><ChatLineRound /></el-icon>
              <h3>问诊反馈</h3>
            </header>
            <div class="feedback-block system-feedback">
              <strong><el-icon><Bell /></el-icon>系统提醒</strong>
              <p>{{ item.reminderText || '暂无系统提醒' }}</p>
            </div>
            <div
              :class="[
                'feedback-block',
                'doctor-feedback',
                { 'has-reply': item.progressRecords?.length || item.doctorNote },
              ]"
            >
              <strong>{{ latestMessagePreview(item).label }}</strong>
              <p class="feedback-preview">{{ latestMessagePreview(item).text }}</p>
              <button class="details-toggle" type="button" @click="toggleDetails(item)">
                <span>{{ latestMessagePreview(item).countText }}</span>
                <span>{{ expandedId === item.id ? '收起详情' : '查看详细沟通' }}</span>
                <el-icon :class="{ rotated: expandedId === item.id }"><ArrowDown /></el-icon>
              </button>
            </div>
          </section>
        </div>

        <section v-if="expandedId === item.id" class="communication-panel">
          <div class="progress-column">
            <header>
              <span>问诊处理</span>
              <strong>处理进度</strong>
            </header>
            <ol v-if="item.progressRecords?.length" class="patient-timeline">
              <li v-for="record in item.progressRecords" :key="record.id">
                <div>
                  <span>{{ record.doctorName || '接诊医生' }}</span>
                  <time>{{ formatConsultationTime(record.createdAt) }}</time>
                </div>
                <small>{{ record.previousStatus }} → {{ record.status }}</small>
              </li>
            </ol>
            <p v-else class="empty-copy">医生接诊后，处理变化会显示在这里。</p>
          </div>

          <div class="conversation-column">
            <header>
              <span>医患沟通</span>
              <strong>回复详情</strong>
            </header>

            <div v-if="communicationState(item.id).loading" class="conversation-state">
              正在加载沟通记录...
            </div>
            <div v-else-if="communicationState(item.id).error" class="conversation-state error">
              <p>{{ communicationState(item.id).error }}</p>
              <button type="button" @click="loadMessages(item)">重新加载</button>
            </div>
            <div
              v-else-if="communicationState(item.id).messages.length"
              class="message-list"
            >
              <article
                v-for="message in communicationState(item.id).messages"
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
            <div v-else class="conversation-state">暂时没有沟通记录。</div>

            <div v-if="canReplyToConsultation(item.status)" class="patient-reply">
              <el-input
                v-model="communicationState(item.id).draft"
                :rows="3"
                maxlength="2000"
                resize="vertical"
                show-word-limit
                placeholder="补充身体变化，或回复医生的建议"
                type="textarea"
              />
              <el-button
                type="primary"
                :loading="communicationState(item.id).sending"
                @click="sendMessage(item)"
              >
                发送回复
              </el-button>
            </div>
            <p v-else class="reply-hint">
              {{ item.status === '已完成' ? '问诊已完成，沟通记录仅供查看。' : '医生接诊后可以在这里回复。' }}
            </p>
          </div>
        </section>
      </article>

      <el-empty
        v-if="!loading && consultations.length === 0"
        description="暂时没有符合条件的问诊记录"
      >
        <RouterLink class="primary-link" to="/consultation/new">建立第一张问诊单</RouterLink>
      </el-empty>
    </div>

    <el-pagination
      v-if="total > filters.size"
      background
      layout="prev, pager, next"
      :current-page="filters.current"
      :page-size="filters.size"
      :total="total"
      @current-change="changePage"
    />
  </section>
</template>

<style scoped>
.records-page {
  padding-top: 24px;
  padding-bottom: 40px;
}

.filters,
.record-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
}

.filters h1 {
  flex: 0 0 auto;
  margin: 0 12px 0 0;
  color: var(--color-ink);
  font-family: "Noto Serif SC", "STSong", serif;
  font-size: 24px;
  letter-spacing: 0.035em;
}

.filters > span,
.record-card time {
  color: var(--color-text-muted);
  font-size: 12px;
}

.filters {
  justify-content: start;
  min-height: 72px;
  padding: 12px 14px 12px 20px;
  border: 1px solid var(--color-border);
  border-radius: 20px;
  background:
    radial-gradient(circle at 100% 0%, rgb(255 255 255 / 88%), transparent 32%),
    rgb(247 251 248 / 82%);
  box-shadow:
    inset 0 1px 0 rgb(255 255 255 / 86%),
    0 12px 32px rgb(23 60 45 / 6%);
  backdrop-filter: blur(16px);
}

.filters .el-select {
  width: 170px;
}

.filters :deep(.el-select__wrapper) {
  min-height: 42px;
  border: 1px solid rgb(67 126 97 / 13%);
  border-radius: 12px;
  background: rgb(255 255 255 / 72%);
  box-shadow: inset 0 1px 2px rgb(30 80 57 / 3%);
}

.filters > span {
  margin-left: auto;
  white-space: nowrap;
  font-size: 13px;
}

.new-consultation-link {
  display: inline-flex;
  min-height: 44px;
  flex: 0 0 auto;
  align-items: center;
  gap: 7px;
  padding: 0 17px;
  border: 1px solid rgb(255 255 255 / 40%);
  border-radius: 999px;
  background: var(--color-ink);
  box-shadow: 0 10px 24px rgb(17 66 47 / 18%);
  color: white;
  font-size: 13px;
  font-weight: 800;
}

.new-consultation-link:hover {
  background: #236e50;
  box-shadow: 0 13px 28px rgb(17 66 47 / 23%);
  transform: translateY(-1px);
}

.records {
  display: grid;
  min-height: 260px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
  margin-top: 16px;
}

.record-card {
  padding: 22px;
  border: 1px solid var(--color-border);
  border-radius: 22px;
  background:
    radial-gradient(circle at 100% 0%, rgb(235 246 239 / 70%), transparent 30%),
    white;
  box-shadow: 0 14px 38px rgb(23 60 45 / 8%);
}

.record-tags {
  display: flex;
  gap: 8px;
}

.tag {
  display: inline-flex;
  padding: 6px 10px;
  border-radius: 999px;
  background: var(--color-jade-light);
  color: var(--color-ink);
  font-size: 13px;
  font-weight: 800;
}

.tag-active,
.tag-attention {
  background: #fff0d6;
  color: #8a5c0f;
}

.tag-urgent {
  background: var(--color-cinnabar-soft);
  color: #9f3f2e;
}

.tag-complete {
  background: #e5eee9;
  color: var(--color-text-muted);
}

.tag-department {
  background: #edf3f0;
  color: #3f6754;
}

.record-header time {
  font-size: 13px;
  font-weight: 600;
}

.symptom-summary {
  margin-top: 18px;
  padding: 17px 18px;
  border: 1px solid rgb(65 126 96 / 12%);
  border-radius: 16px;
  background:
    linear-gradient(145deg, rgb(244 250 246 / 92%), rgb(235 245 239 / 76%));
  box-shadow: inset 0 1px 0 rgb(255 255 255 / 86%);
}

.symptom-summary span {
  color: var(--color-jade);
  font-size: 13px;
  font-weight: 800;
  letter-spacing: 0.1em;
}

.symptom-summary h2 {
  display: -webkit-box;
  margin: 7px 0 0;
  overflow: hidden;
  color: var(--color-ink);
  font-family: "Noto Serif SC", "STSong", serif;
  font-size: 23px;
  letter-spacing: 0.02em;
  line-height: 1.55;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.record-details {
  display: grid;
  grid-template-columns: minmax(0, 0.88fr) minmax(0, 1.12fr);
  gap: 12px;
  margin-top: 12px;
}

.detail-card {
  padding: 16px;
  border: 1px solid rgb(65 126 96 / 11%);
  border-radius: 16px;
  background: rgb(248 251 249 / 86%);
}

.detail-card > header {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--color-ink);
}

.detail-card > header .el-icon {
  display: grid;
  width: 30px;
  height: 30px;
  place-items: center;
  border-radius: 50%;
  background: var(--color-jade-light);
  font-size: 16px;
}

.detail-card h3 {
  margin: 0;
  font-size: 17px;
  letter-spacing: 0.03em;
}

.patient-detail {
  text-align: center;
}

.patient-detail > header {
  justify-content: center;
}

dl {
  display: grid;
  grid-template-columns: 1fr;
  gap: 14px;
  margin: 16px 0 0;
}

.patient-detail dl > div {
  padding: 11px 12px;
  border: 1px solid rgb(65 126 96 / 10%);
  border-radius: 12px;
  background: rgb(255 255 255 / 72%);
}

dt {
  color: #557065;
  font-size: 13px;
  font-weight: 700;
}

dd {
  margin: 6px 0 0;
  overflow-wrap: anywhere;
  color: #153f31;
  font-size: 14px;
  font-weight: 800;
}

.text-attention,
.text-urgent {
  color: var(--color-cinnabar);
}

.reminder-level {
  display: inline-flex;
  margin: 16px 0 0;
  padding: 6px 10px;
  border-radius: 999px;
  background: var(--color-jade-light);
  font-size: 13px;
  font-weight: 800;
}

.feedback-block {
  margin-top: 12px;
  padding: 12px 13px;
  border-left: 3px solid var(--color-jade);
  border-radius: 0 12px 12px 0;
  background: white;
}

.feedback-block strong {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  color: var(--color-ink);
  font-size: 15px;
}

.feedback-block p {
  margin: 7px 0 0;
  color: #466157;
  font-size: 15px;
  line-height: 1.75;
}

.doctor-feedback {
  border-color: rgb(99 118 108 / 30%);
  background: rgb(244 247 245 / 78%);
}

.doctor-feedback.has-reply {
  border-color: var(--color-cinnabar);
  background: #fff7f4;
}

.doctor-feedback.has-reply strong {
  color: var(--color-cinnabar);
}

.feedback-preview {
  display: -webkit-box;
  min-height: 52px;
  overflow: hidden;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.details-toggle {
  display: flex;
  width: 100%;
  align-items: center;
  gap: 8px;
  margin-top: 12px;
  padding: 10px 0 0;
  border: 0;
  border-top: 1px solid rgb(65 126 96 / 10%);
  background: transparent;
  color: #315e49;
  cursor: pointer;
  font: inherit;
}

.details-toggle span:first-child {
  color: var(--color-text-muted);
  font-size: 12px;
}

.details-toggle span:nth-child(2) {
  margin-left: auto;
  font-size: 13px;
  font-weight: 800;
}

.details-toggle .el-icon {
  transition: transform 180ms ease;
}

.details-toggle .el-icon.rotated {
  transform: rotate(180deg);
}

.patient-timeline {
  display: grid;
  gap: 10px;
  margin: 12px 0 0;
  padding: 0;
  list-style: none;
}

.patient-timeline li {
  position: relative;
  padding: 12px 12px 12px 17px;
  border: 1px solid rgb(198 89 69 / 12%);
  border-radius: 12px;
  background: rgb(255 255 255 / 78%);
}

.patient-timeline li::before {
  position: absolute;
  top: 17px;
  left: 7px;
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: var(--color-cinnabar);
  content: "";
}

.patient-timeline div {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.patient-timeline span {
  color: var(--color-ink);
  font-size: 13px;
  font-weight: 800;
}

.patient-timeline time,
.patient-timeline small {
  color: var(--color-text-muted);
  font-size: 11px;
}

.patient-timeline small {
  display: block;
  margin-top: 5px;
}

.patient-timeline p {
  margin-top: 8px;
  white-space: pre-wrap;
}

.communication-panel {
  display: grid;
  grid-template-columns: minmax(220px, 0.72fr) minmax(0, 1.28fr);
  gap: 14px;
  margin-top: 14px;
  padding: 16px;
  border: 1px solid rgb(65 126 96 / 13%);
  border-radius: 18px;
  background:
    radial-gradient(circle at 100% 0%, rgb(235 246 239 / 78%), transparent 34%),
    rgb(249 252 250 / 96%);
}

.communication-panel > div {
  min-width: 0;
  padding: 16px;
  border: 1px solid rgb(65 126 96 / 10%);
  border-radius: 15px;
  background: rgb(255 255 255 / 82%);
}

.communication-panel header {
  display: grid;
  gap: 4px;
}

.communication-panel header span {
  color: var(--color-cinnabar);
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.12em;
}

.communication-panel header strong {
  color: var(--color-ink);
  font-size: 17px;
}

.message-list {
  display: grid;
  max-height: 360px;
  gap: 10px;
  margin-top: 14px;
  overflow-y: auto;
  padding-right: 4px;
}

.message-item {
  padding: 12px 14px;
  border-radius: 14px;
}

.message-doctor {
  margin-right: 34px;
  background: #f0f6f2;
}

.message-patient {
  margin-left: 34px;
  background: #fff2ed;
}

.message-item > div {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.message-item strong {
  color: var(--color-ink);
  font-size: 13px;
}

.message-item time {
  color: var(--color-text-muted);
  font-size: 11px;
}

.message-item p {
  margin: 7px 0 0;
  color: #365a49;
  font-size: 14px;
  line-height: 1.7;
  white-space: pre-wrap;
}

.conversation-state,
.empty-copy,
.reply-hint {
  margin: 14px 0 0;
  color: var(--color-text-muted);
  font-size: 13px;
  line-height: 1.7;
}

.conversation-state.error {
  color: var(--color-cinnabar);
}

.conversation-state button {
  margin-top: 8px;
  border: 0;
  background: transparent;
  color: var(--color-jade);
  cursor: pointer;
  font-weight: 800;
}

.patient-reply {
  display: grid;
  gap: 10px;
  margin-top: 14px;
}

.patient-reply .el-button {
  justify-self: end;
}


.el-pagination {
  justify-content: center;
  margin-top: 26px;
}

@media (max-width: 850px) {
  .records {
    grid-template-columns: 1fr;
  }

  .record-details {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 620px) {
  .filters {
    display: grid;
    justify-content: stretch;
  }

  .filters h1 {
    margin-right: 0;
  }

  .filters .el-select {
    width: 100%;
  }

  .filters > span {
    margin-left: 0;
  }

  .new-consultation-link {
    justify-content: center;
  }

  .record-details,
  dl {
    grid-template-columns: 1fr;
  }

  .communication-panel {
    grid-template-columns: 1fr;
  }
}
</style>
