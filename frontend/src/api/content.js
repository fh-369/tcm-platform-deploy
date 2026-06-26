import request from './request'
import { getBrowserStorage, loadSession } from '../stores/authSession'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api'

function unwrapResult(response) {
  const result = response.data

  if (result?.code !== 200) {
    throw new Error(result?.message || '请求失败，请稍后重试')
  }

  return result.data
}

export async function getPublishedKnowledge(params) {
  return unwrapResult(await request.get('/patient/knowledge', { params }))
}

export async function getPublishedKnowledgeCategories() {
  return unwrapResult(await request.get('/patient/knowledge/categories'))
}

export async function getPublishedKnowledgeDetail(id) {
  return unwrapResult(await request.get(`/patient/knowledge/${id}`))
}

export async function getPublishedRecipes() {
  return unwrapResult(await request.get('/patient/recipe'))
}

export async function getPublishedRecipeDetail(id) {
  return unwrapResult(await request.get(`/patient/recipe/${id}`))
}

export async function askAI(question, context = []) {
  return unwrapResult(await request.post('/patient/ai/question', { question, context }, { timeout: 60000 }))
}

export async function askAIStream({
  question,
  context = [],
  consultationId = null,
  conversationId,
  onChunk,
  signal,
}) {
  const { token } = loadSession(getBrowserStorage())
  const response = await fetch(`${API_BASE_URL}/patient/ai/question/stream`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: JSON.stringify({ question, context, consultationId, conversationId }),
    signal,
  })

  if (!response.ok || !response.body) {
    if (response.status === 401 || response.status === 403) {
      throw new Error('登录状态已失效，请重新登录后再试')
    }
    throw new Error(`AI 问答暂时不可用（HTTP ${response.status || '网络错误'}）`)
  }

  const reader = response.body.getReader()
  const decoder = new TextDecoder()
  while (true) {
    const { done, value } = await reader.read()
    if (done) break
    const chunk = decoder.decode(value, { stream: true })
    if (chunk) {
      onChunk?.(chunk)
    }
  }
}

export async function getAIRecommendations({
  question,
  consultationId = null,
  conversationId = null,
}) {
  return unwrapResult(await request.post('/patient/ai/recommendations', {
    question,
    consultationId,
    conversationId,
  }))
}

export async function getAIConversations(params = { current: 1, size: 20 }) {
  return unwrapResult(await request.get('/patient/ai/conversations', { params }))
}

export async function createAIConversation(payload) {
  return unwrapResult(await request.post('/patient/ai/conversations', payload))
}

export async function getAIConversation(id, params = { messageCurrent: 1, messageSize: 30 }) {
  return unwrapResult(await request.get(`/patient/ai/conversations/${id}`, { params }))
}

export async function deleteAIConversation(id) {
  return unwrapResult(await request.delete(`/patient/ai/conversations/${id}`))
}

export async function importLegacyAIConversation(id, payload) {
  return unwrapResult(await request.post(
    `/patient/ai/conversations/${id}/legacy-content`,
    payload,
  ))
}

export async function getDashboardSummary() {
  const summary = unwrapResult(await request.get('/admin/dashboard'))
  return {
    scope: summary.scope || 'platform',
    statusDistribution: normalizeDistribution(summary.statusDistribution, 'status'),
    urgencyDistribution: normalizeDistribution(summary.urgencyDistribution, 'urgency'),
    trendLast6Months: normalizeDistribution(summary.trendLast6Months, 'month'),
    metrics: summary.metrics || {},
    departmentDistribution: normalizeDistribution(
      summary.departmentDistribution,
      'department',
    ),
    doctorWorkloads: (summary.doctorWorkloads || []).map((item) => ({
      doctorId: item.doctorId,
      doctorName: item.doctorName,
      activeCount: Number(item.activeCount || 0),
    })),
  }
}

export async function getDashboardTrend(period = 'month') {
  const trend = unwrapResult(await request.get('/admin/dashboard/trend', {
    params: { period },
  }))
  return normalizeDistribution(trend, 'period')
}

function normalizeDistribution(items = [], labelKey) {
  return items.map((item) => ({ label: item[labelKey], value: Number(item.count || 0) }))
}

export async function getAdminContent(resource, params) {
  return unwrapResult(await request.get(`/admin/${resource}`, { params }))
}

export async function createAdminContent(resource, payload) {
  return unwrapResult(await request.post(`/admin/${resource}`, payload))
}

export async function updateAdminContent(resource, id, payload) {
  return unwrapResult(await request.put(`/admin/${resource}/${id}`, payload))
}

export async function deleteAdminContent(resource, id) {
  return unwrapResult(await request.delete(`/admin/${resource}/${id}`))
}

export async function updateAdminContentPublication(resource, id, published) {
  return unwrapResult(await request.put(`/admin/${resource}/${id}/publication`, { published }))
}

export async function uploadAdminContentImage(file) {
  const formData = new FormData()
  formData.append('file', file)
  return unwrapResult(await request.post('/admin/content-images', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  }))
}

export async function countExportConsultations(params) {
  return unwrapResult(await request.get('/admin/export/consultations/count', { params }))
}

export async function exportConsultations(params) {
  const response = await request.get('/admin/export/consultations', {
    params,
    responseType: 'blob',
  })
  return response.data
}
