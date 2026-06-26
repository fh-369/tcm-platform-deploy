const DEFAULT_TITLE = '新的对话'
const CONTEXT_MESSAGE_LIMIT = 12

function createId() {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return crypto.randomUUID()
  }
  return `${Date.now()}-${Math.random().toString(16).slice(2)}`
}

function now() {
  return new Date().toISOString()
}

function trimTitle(value) {
  const title = String(value || '').trim()
  return title ? title.slice(0, 24) : DEFAULT_TITLE
}

export function createConversation(seed = '') {
  const createdAt = now()
  return {
    id: createId(),
    title: trimTitle(seed),
    consultationId: null,
    createdAt,
    updatedAt: createdAt,
    messages: [],
    recommendations: [],
    recommendationInitialized: false,
    recommendationsExpanded: false,
    persisted: false,
    messageTotal: 0,
    hasMoreMessages: false,
    messageCurrent: 1,
  }
}

export function normalizeConversation(conversation = {}) {
  return {
    ...conversation,
    id: conversation.id,
    title: trimTitle(conversation.title),
    consultationId: conversation.consultationId ?? null,
    createdAt: conversation.createdAt || now(),
    updatedAt: conversation.updatedAt || conversation.createdAt || now(),
    messages: (conversation.messages || []).map((message) => ({
      ...message,
      streaming: false,
    })),
    recommendations: conversation.recommendations || [],
    recommendationInitialized: Boolean(conversation.recommendationInitialized),
    recommendationsExpanded: false,
    persisted: true,
    messageTotal: Number(conversation.messageTotal || 0),
    hasMoreMessages: Boolean(conversation.hasMoreMessages),
    messageCurrent: 1,
  }
}

export function buildLegacyImportPayload(conversation = {}) {
  const recommendations = []
  const seen = new Set()
  for (const message of conversation.messages || []) {
    for (const item of message.recommendations || []) {
      const key = `${item.type}-${item.id}`
      if (item.type !== 'knowledge' || seen.has(key) || recommendations.length >= 4) {
        continue
      }
      seen.add(key)
      recommendations.push({
        id: item.id,
        type: item.type,
        title: item.title,
        description: item.description || '',
      })
    }
  }
  return {
    messages: (conversation.messages || [])
      .filter((message) => ['user', 'assistant'].includes(message.role))
      .map((message) => ({
        role: message.role,
        content: String(message.content || '').trim(),
        fallback: Boolean(message.fallback),
        disclaimer: message.disclaimer || '',
      }))
      .filter((message) => message.content)
      .slice(0, 500),
    recommendations,
  }
}

export function createUserMessage(content) {
  return {
    id: createId(),
    role: 'user',
    content: String(content || '').trim(),
    createdAt: now(),
  }
}

export function createAssistantMessage(content, options = {}) {
  return {
    id: createId(),
    role: 'assistant',
    content: String(content || '').trim(),
    fallback: Boolean(options.fallback),
    disclaimer: options.disclaimer || '',
    createdAt: now(),
  }
}

export function summarizeConversation(conversation) {
  const firstUserMessage = conversation?.messages?.find((message) => message.role === 'user')
  return trimTitle(firstUserMessage?.content || conversation?.title)
}

export function buildAIContext(messages = [], limit = CONTEXT_MESSAGE_LIMIT) {
  return messages
    .filter((message) => ['user', 'assistant'].includes(message.role))
    .map((message) => ({
      role: message.role,
      content: String(message.content || '').trim(),
    }))
    .filter((message) => message.content)
    .slice(-limit)
}

export function removeEmptyConversations(conversations = []) {
  return conversations.filter((conversation) => conversation?.messages?.length > 0)
}

export function removeConversation(conversations = [], id) {
  const nextConversations = conversations.filter((conversation) => conversation.id !== id)
  return {
    conversations: nextConversations,
    activeId: nextConversations[0]?.id || '',
  }
}
