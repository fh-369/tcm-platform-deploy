import { afterEach, describe, expect, it, vi } from 'vitest'

const requestMocks = vi.hoisted(() => ({
  get: vi.fn(),
  post: vi.fn(),
}))

vi.mock('./request', () => ({
  default: requestMocks,
}))

import {
  getConsultationMessages,
  sendConsultationMessage,
} from './consultation'
import {
  getDoctorConsultationMessages,
  sendDoctorConsultationMessage,
} from './doctorConsultation'

afterEach(() => {
  vi.clearAllMocks()
})

describe('consultation message APIs', () => {
  it('loads and sends patient consultation messages', async () => {
    const messages = [{ id: 1, content: '请继续观察。' }]
    requestMocks.get.mockResolvedValue({
      data: { code: 200, data: messages },
    })
    requestMocks.post.mockResolvedValue({
      data: { code: 200, data: messages[0] },
    })

    await expect(getConsultationMessages(9)).resolves.toEqual(messages)
    await expect(sendConsultationMessage(9, '今天好一些了。')).resolves.toEqual(messages[0])

    expect(requestMocks.get).toHaveBeenCalledWith('/patient/consultation/9/messages')
    expect(requestMocks.post).toHaveBeenCalledWith(
      '/patient/consultation/9/messages',
      { content: '今天好一些了。' },
    )
  })

  it('loads and sends doctor consultation messages', async () => {
    const messages = [{ id: 2, content: '今天好一些了。' }]
    requestMocks.get.mockResolvedValue({
      data: { code: 200, data: messages },
    })
    requestMocks.post.mockResolvedValue({
      data: { code: 200, data: messages[0] },
    })

    await expect(getDoctorConsultationMessages(9)).resolves.toEqual(messages)
    await expect(sendDoctorConsultationMessage(9, '请保持清淡饮食。'))
      .resolves.toEqual(messages[0])

    expect(requestMocks.get).toHaveBeenCalledWith('/doctor/consultations/9/messages')
    expect(requestMocks.post).toHaveBeenCalledWith(
      '/doctor/consultations/9/messages',
      { content: '请保持清淡饮食。' },
    )
  })
})

