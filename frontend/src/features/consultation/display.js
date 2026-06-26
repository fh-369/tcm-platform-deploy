const STATUS_MAP = {
  待接诊: { label: '待接诊', tone: 'waiting' },
  接诊中: { label: '接诊中', tone: 'active' },
  已完成: { label: '已完成', tone: 'complete' },
}

const URGENCY_MAP = {
  普通: { label: '普通', tone: 'normal' },
  紧急: { label: '紧急', tone: 'attention' },
  非常紧急: { label: '非常紧急', tone: 'urgent' },
}

const REMINDER_MAP = {
  normal: { label: '常规跟进', tone: 'normal' },
  attention: { label: '需要关注', tone: 'attention' },
  urgent: { label: '优先提醒', tone: 'urgent' },
}

function fallback(value) {
  return { label: value || '暂无', tone: 'neutral' }
}

export function statusDisplay(status) {
  return STATUS_MAP[status] || fallback(status)
}

export function urgencyDisplay(urgency) {
  return URGENCY_MAP[urgency] || fallback(urgency)
}

export function reminderDisplay(level) {
  return REMINDER_MAP[level] || fallback(level)
}

export function formatConsultationTime(value) {
  if (!value) {
    return '暂无时间'
  }

  return String(value).replace('T', ' ')
}

export function isCrossDepartmentConsultation(consultation) {
  const department = consultation?.departmentName
  const doctorDepartment = consultation?.doctorDepartment
  if (!department || !doctorDepartment || department === '综合咨询') {
    return false
  }
  return department !== doctorDepartment
}
