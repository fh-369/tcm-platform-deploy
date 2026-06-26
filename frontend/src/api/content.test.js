import { beforeEach, describe, expect, it, vi } from 'vitest'

const request = {
  delete: vi.fn(),
  get: vi.fn(),
  post: vi.fn(),
  put: vi.fn(),
}

vi.mock('./request', () => ({ default: request }))

vi.mock('../stores/authSession', () => ({
  getBrowserStorage: () => ({
    getItem: () => JSON.stringify({ token: 'stream-token' }),
  }),
  loadSession: () => ({ token: 'stream-token' }),
}))

describe('content and AI API', () => {
  beforeEach(() => {
    Object.values(request).forEach((mock) => mock.mockReset())
  })

  it('loads public knowledge and recipes', async () => {
    request.get.mockResolvedValue({ data: { code: 200, data: [] } })
    const { getPublishedKnowledge, getPublishedKnowledgeCategories, getPublishedRecipes } =
      await import('./content')

    await getPublishedKnowledge({ current: 1, size: 6 })
    await getPublishedKnowledgeCategories()
    await getPublishedRecipes()

    expect(request.get).toHaveBeenCalledWith('/patient/knowledge', {
      params: { current: 1, size: 6 },
    })
    expect(request.get).toHaveBeenCalledWith('/patient/knowledge/categories')
    expect(request.get).toHaveBeenCalledWith('/patient/recipe')
  })

  it('asks the patient AI endpoint', async () => {
    request.post.mockResolvedValue({ data: { code: 200, data: { answer: '建议规律作息' } } })
    const { askAI } = await import('./content')

    await askAI('最近睡眠不好怎么办？', [
      { role: 'user', content: '我最近总是下午疲倦' },
      { role: 'assistant', content: '可以先观察作息和饮食' },
    ])

    expect(request.post).toHaveBeenCalledWith(
      '/patient/ai/question',
      {
        question: '最近睡眠不好怎么办？',
        context: [
          { role: 'user', content: '我最近总是下午疲倦' },
          { role: 'assistant', content: '可以先观察作息和饮食' },
        ],
      },
      { timeout: 60000 },
    )
  })

  it('streams the patient AI endpoint and emits decoded chunks', async () => {
    const encoder = new TextEncoder()
    const chunks = ['建议先', '清淡饮食。']
    const controller = new AbortController()
    globalThis.fetch = vi.fn().mockResolvedValue({
      ok: true,
      body: new ReadableStream({
        start(controller) {
          chunks.forEach((chunk) => controller.enqueue(encoder.encode(chunk)))
          controller.close()
        },
      }),
    })
    const { askAIStream } = await import('./content')
    const received = []

    await askAIStream({
      question: '胃口不好怎么办？',
      context: [{ role: 'user', content: '我想结合问诊单' }],
      consultationId: 12,
      onChunk: (chunk) => received.push(chunk),
      signal: controller.signal,
    })

    expect(fetch).toHaveBeenCalledWith(
      '/api/patient/ai/question/stream',
      expect.objectContaining({
        method: 'POST',
        headers: expect.objectContaining({
          Authorization: 'Bearer stream-token',
          'Content-Type': 'application/json',
        }),
        body: JSON.stringify({
          question: '胃口不好怎么办？',
          context: [{ role: 'user', content: '我想结合问诊单' }],
          consultationId: 12,
        }),
        signal: controller.signal,
      }),
    )
    expect(received).toEqual(chunks)
  })

  it('loads platform recommendations separately from the AI answer', async () => {
    request.post.mockResolvedValue({
      data: {
        code: 200,
        data: [
          { id: 6, type: 'knowledge', title: '一餐如何吃得更均衡' },
          { id: 9, type: 'recipe', title: '山药香菇鸡肉粥' },
        ],
      },
    })
    const { getAIRecommendations } = await import('./content')

    const result = await getAIRecommendations({ question: '晚饭怎么吃？' })

    expect(request.post).toHaveBeenCalledWith('/patient/ai/recommendations', {
      question: '晚饭怎么吃？',
      consultationId: null,
      conversationId: null,
    })
    expect(result).toHaveLength(2)
  })

  it('manages admin content through the correct resource path', async () => {
    request.get.mockResolvedValue({ data: { code: 200, data: { records: [] } } })
    request.post.mockResolvedValue({ data: { code: 200, data: { id: 1 } } })
    request.put.mockResolvedValue({ data: { code: 200, data: { id: 1 } } })
    request.delete.mockResolvedValue({ data: { code: 200, data: null } })
    const { createAdminContent, deleteAdminContent, getAdminContent, updateAdminContent } =
      await import('./content')
    const payload = { title: '春季养肝', published: true }

    await getAdminContent('knowledge', { current: 1 })
    await createAdminContent('knowledge', payload)
    await updateAdminContent('knowledge', 1, payload)
    await deleteAdminContent('knowledge', 1)

    expect(request.get).toHaveBeenCalledWith('/admin/knowledge', { params: { current: 1 } })
    expect(request.post).toHaveBeenCalledWith('/admin/knowledge', payload)
    expect(request.put).toHaveBeenCalledWith('/admin/knowledge/1', payload)
    expect(request.delete).toHaveBeenCalledWith('/admin/knowledge/1')
  })

  it('updates publication without resubmitting the whole content record', async () => {
    request.put.mockResolvedValue({ data: { code: 200, data: { id: 1, published: true } } })
    const { updateAdminContentPublication } = await import('./content')

    await updateAdminContentPublication('knowledge', 1, true)

    expect(request.put).toHaveBeenCalledWith('/admin/knowledge/1/publication', {
      published: true,
    })
  })

  it('uploads a content cover as multipart form data', async () => {
    request.post.mockResolvedValue({
      data: { code: 200, data: { url: '/uploads/content/cover.png' } },
    })
    const { uploadAdminContentImage } = await import('./content')
    const file = new File(['cover'], 'cover.png', { type: 'image/png' })

    await uploadAdminContentImage(file)

    expect(request.post).toHaveBeenCalledWith(
      '/admin/content-images',
      expect.any(FormData),
      { headers: { 'Content-Type': 'multipart/form-data' } },
    )
    const formData = request.post.mock.calls[0][1]
    expect(formData.get('file')).toBe(file)
  })

  it('counts and downloads filtered consultation CSV records', async () => {
    const csv = new Blob(['id,status'])
    request.get
      .mockResolvedValueOnce({ data: { code: 200, data: 4 } })
      .mockResolvedValueOnce({ data: csv })
    const { countExportConsultations, exportConsultations } = await import('./content')
    const filters = {
      dateFrom: '2026-06-01',
      dateTo: '2026-06-22',
      status: '接诊中',
      departmentId: 2,
    }

    const count = await countExportConsultations(filters)
    const result = await exportConsultations(filters)

    expect(request.get).toHaveBeenCalledWith('/admin/export/consultations/count', {
      params: filters,
    })
    expect(request.get).toHaveBeenCalledWith('/admin/export/consultations', {
      params: filters,
      responseType: 'blob',
    })
    expect(count).toBe(4)
    expect(result).toBe(csv)
  })

  it('normalizes dashboard SQL aliases for display', async () => {
    request.get.mockResolvedValue({
      data: {
        code: 200,
        data: {
          statusDistribution: [{ status: '待接诊', count: 2 }],
          urgencyDistribution: [{ urgency: '普通', count: 3 }],
          trendLast6Months: [{ month: '2026-06', count: 4 }],
          metrics: { registeredPatients: 11 },
          departmentDistribution: [{ department: '中医内科', count: 4 }],
          doctorWorkloads: [{ doctorName: '李医生', activeCount: 2 }],
          scope: 'platform',
        },
      },
    })
    const { getDashboardSummary } = await import('./content')

    const result = await getDashboardSummary()

    expect(result.statusDistribution).toEqual([{ label: '待接诊', value: 2 }])
    expect(result.urgencyDistribution).toEqual([{ label: '普通', value: 3 }])
    expect(result.trendLast6Months).toEqual([{ label: '2026-06', value: 4 }])
    expect(result.departmentDistribution).toEqual([{ label: '中医内科', value: 4 }])
    expect(result.doctorWorkloads).toEqual([
      { doctorName: '李医生', activeCount: 2 },
    ])
    expect(result.metrics.registeredPatients).toBe(11)
    expect(result.scope).toBe('platform')
  })

  it('loads a dashboard trend without reloading the other dashboard data', async () => {
    request.get.mockResolvedValue({
      data: {
        code: 200,
        data: [{ period: '2026-06-16', count: 5 }],
      },
    })
    const { getDashboardTrend } = await import('./content')

    const result = await getDashboardTrend('week')

    expect(request.get).toHaveBeenCalledWith('/admin/dashboard/trend', {
      params: { period: 'week' },
    })
    expect(result).toEqual([{ label: '2026-06-16', value: 5 }])
  })
})
