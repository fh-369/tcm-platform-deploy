export function canReplyToConsultation(status) {
  return status === '接诊中'
}

export function latestMessagePreview(consultation = {}) {
  const count = Number(consultation.messageCount || 0)
  if (!count || !consultation.latestMessage) {
    return {
      label: '处理进度',
      text: '医生暂未回复，请耐心等待。',
      countText: '暂无沟通记录',
    }
  }

  return {
    label: consultation.latestMessageSenderType === 'doctor' ? '医生最新回复' : '我的最新补充',
    text: consultation.latestMessage,
    countText: `${count} 条沟通记录`,
  }
}

export function messageAuthorLabel(message = {}) {
  const name = message.senderName || (message.senderType === 'doctor' ? '接诊医生' : '患者')
  return `${name} · ${message.senderType === 'doctor' ? '医生' : '我'}`
}

