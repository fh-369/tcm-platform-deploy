import { describe, expect, it } from 'vitest'

import {
  canReplyToConsultation,
  latestMessagePreview,
  messageAuthorLabel,
} from './communication'

describe('consultation communication helpers', () => {
  it('allows replies only while a consultation is active', () => {
    expect(canReplyToConsultation('接诊中')).toBe(true)
    expect(canReplyToConsultation('待接诊')).toBe(false)
    expect(canReplyToConsultation('已完成')).toBe(false)
  })

  it('formats a compact latest-message preview', () => {
    expect(latestMessagePreview({
      latestMessage: '建议继续清淡饮食并记录身体变化。',
      latestMessageSenderType: 'doctor',
      messageCount: 3,
    })).toEqual({
      label: '医生最新回复',
      text: '建议继续清淡饮食并记录身体变化。',
      countText: '3 条沟通记录',
    })

    expect(latestMessagePreview({ messageCount: 0 })).toEqual({
      label: '处理进度',
      text: '医生暂未回复，请耐心等待。',
      countText: '暂无沟通记录',
    })
  })

  it('uses clear role labels for conversation messages', () => {
    expect(messageAuthorLabel({ senderType: 'doctor', senderName: '张医生' }))
      .toBe('张医生 · 医生')
    expect(messageAuthorLabel({ senderType: 'patient', senderName: '李女士' }))
      .toBe('李女士 · 我')
  })
})

