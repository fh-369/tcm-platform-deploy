<script setup>
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import DOMPurify from 'dompurify'
import MarkdownIt from 'markdown-it'
import { useRouter } from 'vue-router'

import {
  askAIStream,
  createAIConversation,
  deleteAIConversation,
  getAIConversation,
  getAIConversations,
  importLegacyAIConversation,
} from '../../api/content'
import { getMyConsultations } from '../../api/consultation'
import {
  buildAIContext,
  buildLegacyImportPayload,
  createAssistantMessage,
  createConversation,
  createUserMessage,
  normalizeConversation,
  removeConversation,
  removeEmptyConversations,
  summarizeConversation,
} from '../../features/ai/session'

const STORAGE_KEY = 'tcm-ai-conversations'
const router = useRouter()
const input = ref('')
const activeId = ref('')
const conversations = ref([])
const messagesRef = ref(null)
const helpVisible = ref(false)
const consultations = ref([])
const selectedConsultationId = ref(null)
const activeRequest = ref(null)
const examples = ['最近容易疲倦，日常作息可以怎么调整？', '春季饮食有哪些温和的养生建议？', '睡眠不安稳时可以注意哪些生活习惯？']
const markdown = new MarkdownIt({
  html: false,
  linkify: true,
  breaks: true,
})

const activeConversation = computed(() =>
  conversations.value.find((conversation) => conversation.id === activeId.value) || conversations.value[0],
)

const activeMessages = computed(() => activeConversation.value?.messages || [])
const isGenerating = computed(() => Boolean(activeRequest.value))

function readConversations() {
  try {
    return JSON.parse(localStorage.getItem(STORAGE_KEY) || '[]').map((conversation) => ({
      ...conversation,
      messages: (conversation.messages || []).map((message) => ({
        ...message,
        streaming: false,
      })),
    }))
  } catch {
    return []
  }
}

function ensureConversation(seed = '') {
  if (activeConversation.value) {
    return activeConversation.value
  }
  return createNewConversation(seed)
}

function createNewConversation(seed = '') {
  conversations.value = conversations.value.filter(
    (conversation) => conversation.persisted || conversation.messages.length,
  )
  const conversation = createConversation(seed)
  conversations.value.unshift(conversation)
  activeId.value = conversation.id
  selectedConsultationId.value = null
  return conversation
}

async function deleteConversation(id) {
  const target = conversations.value.find((conversation) => conversation.id === id)
  if (target?.persisted) {
    try {
      await deleteAIConversation(id)
    } catch (error) {
      return ElMessage.error(error.message || '对话删除失败')
    }
  }
  const result = removeConversation(conversations.value, id)
  conversations.value = result.conversations.length ? result.conversations : [createConversation()]
  activeId.value = result.activeId || conversations.value[0].id
  selectedConsultationId.value = activeConversation.value?.consultationId ?? null
}

async function selectConversation(id) {
  activeId.value = id
  selectedConsultationId.value = activeConversation.value?.consultationId ?? null
  if (activeConversation.value?.persisted && !activeConversation.value.messages.length) {
    await loadConversationDetail(activeConversation.value)
  }
}

function setExample(item) {
  input.value = item
}

function scrollToLatest() {
  nextTick(() => {
    if (messagesRef.value) {
      messagesRef.value.scrollTop = messagesRef.value.scrollHeight
    }
  })
}

function isNearMessageBottom() {
  if (!messagesRef.value) return true
  const distance = messagesRef.value.scrollHeight - messagesRef.value.scrollTop - messagesRef.value.clientHeight
  return distance < 80
}

function renderMarkdown(content) {
  return DOMPurify.sanitize(markdown.render(content || ''))
}

function openRecommendation(item) {
  router.push(`/knowledge/${item.id}`)
}

function handleComposerKeydown(event) {
  if (event.key !== 'Enter') return
  if (event.shiftKey) return

  event.preventDefault()
  if (!isGenerating.value) {
    submit()
  }
}

function appendMessage(conversation, message) {
  conversation.messages.push(message)
  conversation.title = summarizeConversation(conversation)
  conversation.updatedAt = new Date().toISOString()
  scrollToLatest()
  return conversation.messages.at(-1)
}

function touchConversation(conversation, options = {}) {
  conversation.title = summarizeConversation(conversation)
  conversation.updatedAt = new Date().toISOString()
  if (options.scroll !== false) {
    scrollToLatest()
  }
}

function consultationLabel(item) {
  const symptom = item.symptoms || '未填写症状'
  const date = item.createdAt ? item.createdAt.slice(0, 10) : '未知日期'
  return `${date} · ${symptom}`
}

async function persistConversation(conversation, question) {
  if (conversation.persisted) return conversation
  const created = normalizeConversation(await createAIConversation({
    title: summarizeConversation({ ...conversation, messages: [createUserMessage(question)] }),
    consultationId: selectedConsultationId.value,
  }))
  Object.assign(conversation, created)
  conversation.consultationId = selectedConsultationId.value
  return conversation
}

async function loadConversationDetail(conversation, messageCurrent = 1, prepend = false) {
  const detail = normalizeConversation(await getAIConversation(conversation.id, {
    messageCurrent,
    messageSize: 30,
  }))
  const existingMessages = conversation.messages || []
  const expanded = conversation.recommendationsExpanded
  Object.assign(conversation, detail)
  conversation.recommendationsExpanded = expanded
  conversation.messageCurrent = messageCurrent
  if (prepend) {
    conversation.messages = [...detail.messages, ...existingMessages]
  }
  selectedConsultationId.value = conversation.consultationId ?? null
}

async function loadEarlierMessages() {
  if (!activeConversation.value?.persisted || !activeConversation.value.hasMoreMessages) return
  await loadConversationDetail(
    activeConversation.value,
    (activeConversation.value.messageCurrent || 1) + 1,
    true,
  )
}

async function migrateLegacyConversations(savedConversations) {
  for (const legacy of savedConversations.slice(0, 100)) {
    const created = await createAIConversation({
      title: summarizeConversation(legacy),
      consultationId: legacy.consultationId ?? null,
      legacyKey: String(legacy.id),
    })
    if (!created.messageTotal) {
      await importLegacyAIConversation(
        created.id,
        buildLegacyImportPayload(legacy),
      )
    }
  }
  localStorage.removeItem(STORAGE_KEY)
  const page = await getAIConversations({ current: 1, size: 20 })
  return (page.records || []).map(normalizeConversation)
}

async function loadConversations() {
  try {
    const page = await getAIConversations({ current: 1, size: 20 })
    let loaded = (page.records || []).map(normalizeConversation)
    const legacy = removeEmptyConversations(readConversations())
    if (legacy.length) {
      loaded = await migrateLegacyConversations(legacy)
    }
    conversations.value = loaded.length ? loaded : [createConversation()]
    activeId.value = conversations.value[0].id
    selectedConsultationId.value = activeConversation.value?.consultationId ?? null
    if (activeConversation.value?.persisted) {
      await loadConversationDetail(activeConversation.value)
    }
  } catch (error) {
    conversations.value = [createConversation()]
    activeId.value = conversations.value[0].id
    ElMessage.error(error.message || 'AI 对话记录加载失败')
  }
}

async function loadConsultations() {
  try {
    const page = await getMyConsultations({ current: 1, size: 20 })
    consultations.value = page.records || []
  } catch {
    consultations.value = []
  }
}

function stopGeneration() {
  activeRequest.value?.controller.abort()
}

async function submit() {
  if (isGenerating.value) return

  const question = input.value.trim()
  if (!question) return ElMessage.warning('请先输入想了解的问题')

  const conversation = ensureConversation(question)
  try {
    await persistConversation(conversation, question)
  } catch (error) {
    return ElMessage.error(error.message || '对话创建失败')
  }
  const context = buildAIContext(conversation.messages)
  appendMessage(conversation, createUserMessage(question))
  const assistantMessage = reactive(createAssistantMessage('', {
    disclaimer: '本回答仅供一般养护参考，不能替代医生诊断和治疗。',
  }))
  assistantMessage.streaming = true
  const renderedAssistantMessage = appendMessage(conversation, assistantMessage)
  const controller = new AbortController()
  activeRequest.value = {
    controller,
    messageId: renderedAssistantMessage.id,
  }
  input.value = ''
  let receivedAnswer = ''
  try {
    await askAIStream({
      question,
      context,
      consultationId: conversation.consultationId,
      conversationId: conversation.id,
      onChunk: (chunk) => {
        const shouldFollow = isNearMessageBottom()
        receivedAnswer += chunk
        renderedAssistantMessage.content += chunk
        if (shouldFollow) {
          scrollToLatest()
        }
      },
      signal: controller.signal,
    })
    if (!receivedAnswer.trim() && !renderedAssistantMessage.content.trim()) {
      renderedAssistantMessage.fallback = true
      renderedAssistantMessage.content = '暂时无法获取智能回答。建议保持规律作息、均衡饮食和适量运动；如症状严重、持续不缓解或出现明显不适，请及时就医。'
    }
  } catch (error) {
    if (error.name === 'AbortError') {
      if (!renderedAssistantMessage.content.trim()) {
        renderedAssistantMessage.content = '已停止生成。'
      }
    } else {
      renderedAssistantMessage.fallback = true
      renderedAssistantMessage.content = 'AI 问答暂时不可用，请稍后再试。'
      ElMessage.error(error.message || 'AI 问答暂时不可用')
    }
  } finally {
    renderedAssistantMessage.streaming = false
    if (activeRequest.value?.messageId === renderedAssistantMessage.id) {
      activeRequest.value = null
    }
    touchConversation(conversation)
    try {
      await loadConversationDetail(conversation)
    } catch {
      // The streamed answer remains visible even if refreshing persisted metadata fails.
    }
  }
}

onMounted(async () => {
  await loadConversations()
  loadConsultations()
  scrollToLatest()
})
</script>

<template>
  <section class="ai-page page-container">
    <div class="chat-shell">
      <aside class="conversation-panel">
        <button class="new-chat" type="button" @click="createNewConversation()">+ 新建对话</button>
        <button class="help-toggle" type="button" @click="helpVisible = !helpVisible">
          {{ helpVisible ? '收起使用说明' : '查看使用说明' }}
        </button>
        <div v-if="helpVisible" class="ai-guide">
          <strong>AI养护问答可以做什么？</strong>
          <p>它可以结合你选择的问诊单提供一般养护建议，并根据问题推荐平台内值得继续阅读的内容。</p>
          <ul>
            <li>可选择一张问诊单作为本轮对话依据</li>
            <li>回答后推荐相关养生文章和药膳详情</li>
            <li>内容仅用于健康科普，不替代医生诊断或处方</li>
          </ul>
        </div>
        <div class="conversation-list">
          <div
            v-for="conversation in conversations"
            :key="conversation.id"
            :class="['conversation-item', { active: conversation.id === activeConversation?.id }]"
          >
            <button class="conversation-select" type="button" @click="selectConversation(conversation.id)">
              <strong>{{ summarizeConversation(conversation) }}</strong>
              <span>{{ conversation.messageTotal || conversation.messages.length ? `${conversation.messageTotal || conversation.messages.length} 条消息` : '尚未开始' }}</span>
            </button>
            <button class="delete-chat" type="button" aria-label="删除对话" @click.stop="deleteConversation(conversation.id)">删除</button>
          </div>
        </div>
      </aside>

      <section class="chat-panel">
        <div class="chat-heading">
          <div>
            <span>当前对话</span>
            <h2>{{ summarizeConversation(activeConversation || {}) }}</h2>
          </div>
          <div class="consultation-picker">
            <small>选择问诊单后，AI 会结合症状、持续时间和备注回答。</small>
            <el-select
              v-model="selectedConsultationId"
              clearable
              filterable
              placeholder="选择问诊单（可选）"
              size="large"
              :disabled="Boolean(activeConversation?.persisted)"
            >
              <el-option
                v-for="item in consultations"
                :key="item.id"
                :label="consultationLabel(item)"
                :value="item.id"
              />
            </el-select>
          </div>
        </div>

        <div ref="messagesRef" class="messages">
          <button
            v-if="activeConversation?.hasMoreMessages"
            class="load-earlier"
            type="button"
            @click="loadEarlierMessages"
          >
            加载更早消息
          </button>
          <div v-if="!activeMessages.length" class="empty-state">
            <p>可以从一个具体问题开始</p>
            <span>尽量说清楚场景、持续时间和你想追问的方向。</span>
            <div class="examples">
              <button v-for="item in examples" :key="item" type="button" @click="setExample(item)">{{ item }}</button>
            </div>
          </div>

          <article
            v-for="message in activeMessages"
            :key="message.id"
            :class="['message', message.role, { fallback: message.fallback, thinking: message.streaming && !message.content }]"
          >
            <span>{{ message.role === 'user' ? '我' : (message.fallback ? '基础建议模式' : 'AI 参考建议') }}</span>
            <p v-if="message.content && message.streaming" class="streaming-text">{{ message.content }}</p>
            <div v-else-if="message.content" class="markdown-body" v-html="renderMarkdown(message.content)"></div>
            <p v-else class="typing-placeholder">正在结合当前对话与所选问诊单整理回答……</p>
            <i
              v-if="message.streaming"
              class="stream-cursor"
              aria-hidden="true"
            ></i>
            <footer v-if="message.disclaimer">{{ message.disclaimer }}</footer>
          </article>

          <section
            v-if="activeConversation?.recommendations?.length"
            class="conversation-recommendations"
          >
            <button
              class="recommendation-toggle"
              type="button"
              @click="activeConversation.recommendationsExpanded = !activeConversation.recommendationsExpanded"
            >
              <span>
                <b>站内延伸阅读</b>
                <small>根据本对话首次提问智能匹配 {{ activeConversation.recommendations.length }} 篇养生文章</small>
              </span>
              <i aria-hidden="true">{{ activeConversation.recommendationsExpanded ? '收起' : '展开' }}</i>
            </button>
            <div
              v-if="activeConversation.recommendationsExpanded"
              class="recommendation-list"
            >
              <button
                v-for="item in activeConversation.recommendations"
                :key="`${item.type}-${item.id}`"
                type="button"
                @click="openRecommendation(item)"
              >
                <span>养生知识</span>
                <div>
                  <b>{{ item.title }}</b>
                  <small v-if="item.description">{{ item.description }}</small>
                </div>
                <i aria-hidden="true">→</i>
              </button>
            </div>
          </section>
        </div>

        <div class="composer-wrap">
          <p class="ai-safety-note">健康建议仅供参考；如出现胸痛、呼吸困难、意识异常等紧急情况，请及时就医。</p>
          <form class="composer" @submit.prevent="submit">
            <el-input
              id="ai-question"
              v-model="input"
              class="question-input"
              :maxlength="500"
              :rows="2"
              show-word-limit
              type="textarea"
              :disabled="isGenerating"
              placeholder="继续追问，或描述一个新的养护问题"
              @keydown="handleComposerKeydown"
            />
            <el-button v-if="isGenerating" class="stop-button" type="danger" native-type="button" @click="stopGeneration">
              停止生成
            </el-button>
            <el-button v-else type="primary" native-type="submit">发送</el-button>
          </form>
        </div>
      </section>
    </div>
  </section>
</template>

<style scoped>
.ai-page { height: calc(100vh - 90px); padding-block: 14px 16px; overflow: hidden; }
.chat-shell { display: grid; grid-template-columns: 300px minmax(0, 1fr); gap: 18px; height: 100%; min-height: 0; }
.conversation-panel, .chat-panel { border: 1px solid rgb(79 138 108 / 16%); border-radius: 28px; background: rgb(255 255 255 / 88%); box-shadow: 0 16px 44px rgb(23 60 45 / 8%); backdrop-filter: blur(14px); }
.conversation-panel { display: flex; flex-direction: column; gap: 14px; padding: 18px; overflow: hidden; }
.new-chat { padding: 13px 16px; border: 0; border-radius: 999px; background: var(--color-ink); color: white; cursor: pointer; font-weight: 900; box-shadow: 0 12px 26px rgb(23 60 45 / 15%); }
.help-toggle { padding: 11px 14px; border: 1px solid rgb(79 138 108 / 18%); border-radius: 999px; background: rgb(244 250 246 / 90%); color: var(--color-ink); cursor: pointer; font-size: 13px; font-weight: 900; }
.ai-guide { padding: 15px 16px; border: 1px solid rgb(79 138 108 / 14%); border-radius: 22px; background:
  radial-gradient(circle at 90% 0%, rgb(240 197 184 / 22%), transparent 38%),
  linear-gradient(145deg, rgb(255 255 255 / 92%), rgb(244 250 246 / 88%)); color: #49685b; box-shadow: 0 12px 26px rgb(23 60 45 / 7%); }
.ai-guide strong { display: block; color: var(--color-ink); font-family: "Noto Serif SC", "STSong", serif; font-size: 18px; }
.ai-guide p { margin: 9px 0 10px; font-size: 12px; line-height: 1.8; }
.ai-guide ul { display: grid; gap: 7px; margin: 0; padding-left: 18px; font-size: 12px; line-height: 1.6; }
.conversation-list { display: grid; gap: 10px; overflow: auto; padding-right: 2px; }
.conversation-item { display: grid; grid-template-columns: minmax(0, 1fr) auto; gap: 8px; align-items: center; padding: 8px; border: 1px solid transparent; border-radius: 18px; background: rgb(244 250 246 / 84%); transition: .18s ease; }
.conversation-item:hover, .conversation-item.active { border-color: rgb(79 138 108 / 24%); background: white; box-shadow: 0 10px 24px rgb(23 60 45 / 8%); }
.conversation-select { min-width: 0; padding: 6px; border: 0; background: transparent; color: var(--color-text-muted); cursor: pointer; text-align: left; }
.delete-chat { padding: 7px 9px; border: 1px solid rgb(201 81 61 / 14%); border-radius: 999px; background: rgb(255 250 246 / 76%); color: var(--color-cinnabar); cursor: pointer; font-size: 11px; font-weight: 800; opacity: .7; }
.delete-chat:hover { border-color: rgb(201 81 61 / 30%); background: #fff5ef; opacity: 1; }
.conversation-list strong { display: block; overflow: hidden; color: var(--color-ink); font-size: 14px; text-overflow: ellipsis; white-space: nowrap; }
.conversation-list span { display: block; margin-top: 7px; font-size: 11px; }
.chat-panel { display: grid; grid-template-rows: auto minmax(0, 1fr) auto; overflow: hidden; }
.chat-heading { display: grid; grid-template-columns: minmax(0, 1fr) minmax(260px, 380px); align-items: center; gap: 18px; padding: 16px 22px; border-bottom: 1px solid rgb(79 138 108 / 12%); }
.chat-heading span { color: var(--color-cinnabar); font-size: 11px; font-weight: 900; letter-spacing: .14em; }
.chat-heading h2 { margin: 5px 0 0; color: var(--color-ink); font-family: "Noto Serif SC", "STSong", serif; font-size: 24px; letter-spacing: -.04em; }
.consultation-picker { display: grid; gap: 8px; }
.consultation-picker small { color: var(--color-text-muted); font-size: 12px; line-height: 1.6; text-align: right; }
.consultation-picker :deep(.el-select__wrapper) { border-radius: 999px; background: rgb(244 250 246 / 88%); box-shadow: 0 0 0 1px rgb(79 138 108 / 16%) inset; }
.messages { display: flex; flex-direction: column; gap: 14px; overflow-y: auto; padding: 18px 22px; background:
  radial-gradient(circle at 12% 10%, rgb(79 138 108 / 8%), transparent 24%),
  linear-gradient(180deg, rgb(250 253 250 / 72%), rgb(246 251 247 / 92%)); }
.load-earlier { align-self: center; padding: 8px 16px; border: 1px solid rgb(79 138 108 / 18%); border-radius: 999px; background: rgb(255 255 255 / 88%); color: var(--color-ink); cursor: pointer; font-size: 12px; font-weight: 800; }
.empty-state { margin: auto; max-width: 560px; text-align: center; }
.empty-state p { margin: 0; color: var(--color-ink); font-family: "Noto Serif SC", "STSong", serif; font-size: 30px; font-weight: 800; }
.empty-state span { display: block; margin-top: 10px; color: var(--color-text-muted); line-height: 1.8; }
.examples { display: flex; flex-wrap: wrap; justify-content: center; gap: 9px; margin-top: 22px; }
.examples button { padding: 9px 13px; border: 1px solid rgb(79 138 108 / 18%); border-radius: 99px; background: rgb(255 255 255 / 86%); color: #506b60; cursor: pointer; font-size: 12px; font-weight: 700; transition: .18s ease; }
.examples button:hover { border-color: rgb(79 138 108 / 42%); color: var(--color-ink); transform: translateY(-1px); }
.message { width: min(76%, 680px); padding: 16px 18px; border: 1px solid rgb(79 138 108 / 13%); border-radius: 22px; background: white; box-shadow: 0 12px 30px rgb(23 60 45 / 7%); }
.message.user { align-self: flex-end; border-color: rgb(17 54 40 / 16%); background: var(--color-ink); color: white; }
.message.assistant { align-self: flex-start; }
.message.fallback { border-color: rgb(223 181 115 / 46%); background: #fffaf0; }
.message span { display: block; margin-bottom: 8px; color: var(--color-cinnabar); font-size: 11px; font-weight: 900; letter-spacing: .1em; }
.message.user span { color: #f0c5b8; }
.typing-placeholder { margin: 0; color: var(--color-text-muted); font-size: 14px; line-height: 1.95; }
.streaming-text { margin: 0; color: #405e51; font-size: 14px; line-height: 1.95; white-space: pre-wrap; word-break: break-word; }
.markdown-body { color: #405e51; font-size: 14px; line-height: 1.95; word-break: break-word; }
.message.user .markdown-body { color: white; }
.markdown-body :deep(*) { margin-top: 0; }
.markdown-body :deep(p) { margin: 0 0 12px; }
.markdown-body :deep(p:last-child) { margin-bottom: 0; }
.markdown-body :deep(h1),
.markdown-body :deep(h2),
.markdown-body :deep(h3) { margin: 14px 0 8px; color: var(--color-ink); font-family: "Noto Serif SC", "STSong", serif; line-height: 1.45; letter-spacing: -.03em; }
.message.user .markdown-body :deep(h1),
.message.user .markdown-body :deep(h2),
.message.user .markdown-body :deep(h3) { color: white; }
.markdown-body :deep(h1) { font-size: 22px; }
.markdown-body :deep(h2) { font-size: 19px; }
.markdown-body :deep(h3) { font-size: 17px; }
.markdown-body :deep(ul),
.markdown-body :deep(ol) { display: grid; gap: 6px; margin: 8px 0 12px; padding-left: 22px; }
.markdown-body :deep(li) { padding-left: 2px; }
.markdown-body :deep(strong) { color: var(--color-ink); font-weight: 900; }
.message.user .markdown-body :deep(strong) { color: white; }
.markdown-body :deep(blockquote) { margin: 12px 0; padding: 12px 14px; border-left: 4px solid var(--color-cinnabar); border-radius: 12px; background: rgb(244 250 246 / 82%); color: #506b60; }
.markdown-body :deep(code) { padding: 2px 6px; border-radius: 7px; background: rgb(17 54 40 / 8%); color: var(--color-ink); font-family: Consolas, "Courier New", monospace; font-size: .92em; }
.markdown-body :deep(pre) { overflow: auto; margin: 12px 0; padding: 12px 14px; border-radius: 14px; background: #113628; color: white; }
.markdown-body :deep(pre code) { padding: 0; background: transparent; color: inherit; }
.stream-cursor { display: inline-block; width: 8px; height: 18px; margin: 6px 0 0 2px; border-radius: 999px; background: var(--color-cinnabar); animation: stream-cursor-blink .9s infinite; vertical-align: text-bottom; }
@keyframes stream-cursor-blink { 0%, 45% { opacity: 1; } 46%, 100% { opacity: .18; } }
.conversation-recommendations { width: min(78%, 760px); padding: 10px; border: 1px solid rgb(79 138 108 / 16%); border-radius: 22px; background: rgb(255 255 255 / 84%); box-shadow: 0 12px 28px rgb(23 60 45 / 7%); }
.recommendation-toggle { display: flex; align-items: center; justify-content: space-between; gap: 16px; width: 100%; padding: 10px 12px; border: 0; background: transparent; color: var(--color-ink); cursor: pointer; text-align: left; }
.recommendation-toggle span { min-width: 0; }
.recommendation-toggle b { display: block; font-size: 14px; }
.recommendation-toggle small { display: block; margin-top: 4px; color: var(--color-text-muted); font-size: 11px; line-height: 1.5; }
.recommendation-toggle > i { flex: 0 0 auto; padding: 6px 10px; border-radius: 999px; background: rgb(79 138 108 / 10%); color: var(--color-ink); font-size: 11px; font-style: normal; font-weight: 800; }
.recommendation-list { display: grid; gap: 8px; padding: 4px 4px 2px; }
.recommendation-list > button { display: grid; grid-template-columns: auto minmax(0, 1fr) auto; gap: 10px; align-items: center; width: 100%; padding: 11px 12px; border: 1px solid rgb(79 138 108 / 14%); border-radius: 16px; background: linear-gradient(135deg, rgb(248 252 249 / 96%), rgb(240 248 243 / 90%)); color: #405e51; cursor: pointer; text-align: left; transition: .18s ease; }
.recommendation-list > button:hover { border-color: rgb(79 138 108 / 34%); box-shadow: 0 9px 20px rgb(23 60 45 / 8%); transform: translateY(-1px); }
.recommendation-list > button > span { margin: 0; padding: 5px 8px; border-radius: 999px; background: rgb(79 138 108 / 11%); color: var(--color-ink); font-size: 10px; letter-spacing: 0; white-space: nowrap; }
.recommendation-list > button div { min-width: 0; }
.recommendation-list > button b { display: block; overflow: hidden; color: var(--color-ink); font-size: 13px; text-overflow: ellipsis; white-space: nowrap; }
.recommendation-list > button small { display: -webkit-box; overflow: hidden; margin-top: 4px; color: var(--color-text-muted); font-size: 11px; line-height: 1.55; -webkit-box-orient: vertical; -webkit-line-clamp: 2; }
.recommendation-list > button > i { color: var(--color-cinnabar); font-size: 16px; font-style: normal; }
.message footer { margin-top: 14px; padding-top: 12px; border-top: 1px solid rgb(79 138 108 / 12%); color: var(--color-text-muted); font-size: 11px; line-height: 1.7; }
.thinking .typing-placeholder { color: var(--color-text-muted); }
.composer-wrap { padding: 10px 16px 14px; border-top: 1px solid rgb(79 138 108 / 12%); background: rgb(255 255 255 / 92%); }
.composer { display: grid; grid-template-columns: minmax(0, 1fr) 96px; gap: 12px; align-items: end; }
.question-input :deep(.el-textarea__inner) { min-height: 54px !important; max-height: 160px; padding: 12px 16px; border: 0; border-radius: 18px; background: linear-gradient(145deg, #fbfdf9, #f1f8f4); box-shadow: 0 0 0 1px rgb(79 138 108 / 14%) inset; color: var(--color-ink); font-size: 14px; line-height: 1.65; resize: vertical; }
.question-input :deep(.el-input__count) { right: 14px; bottom: 8px; background: transparent; color: rgb(68 94 81 / 56%); }
.composer :deep(.el-button) { height: 48px; border: 0; border-radius: 999px; background: var(--color-ink); font-weight: 900; box-shadow: 0 12px 26px rgb(23 60 45 / 15%); }
.composer :deep(.stop-button) { background: var(--color-cinnabar); box-shadow: 0 12px 26px rgb(201 81 61 / 18%); }
.ai-safety-note { margin: 0 0 8px; color: rgb(68 94 81 / 72%); font-size: 11px; line-height: 1.6; text-align: right; }
@media (max-width: 900px) { .ai-page { height: auto; overflow: visible; } .chat-shell { grid-template-columns: 1fr; height: auto; } .conversation-panel { max-height: 360px; } .chat-panel { min-height: 620px; } .chat-heading { grid-template-columns: 1fr; align-items: flex-start; } .consultation-picker small, .ai-safety-note { text-align: left; } .message { width: min(88%, 680px); } }
</style>
