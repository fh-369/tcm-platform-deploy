const APPROVAL_META = {
  PENDING: {
    label: '待审核',
    tone: 'pending',
    description: '资料已提交，等待管理员核验。',
  },
  APPROVED: {
    label: '已通过',
    tone: 'approved',
    description: '已具备医生后台访问资格。',
  },
  REJECTED: {
    label: '未通过',
    tone: 'rejected',
    description: '申请资料需要补充或调整。',
  },
}

export function approvalMeta(status) {
  return APPROVAL_META[status] || {
    label: '状态未知',
    tone: 'unknown',
    description: '暂时无法识别当前审核状态。',
  }
}

export function canEnableDoctor(doctor) {
  return doctor?.approvalStatus === 'APPROVED'
}

export function doctorReviewActions(doctor) {
  if (doctor?.approvalStatus === 'PENDING') {
    return ['APPROVED', 'REJECTED']
  }
  if (doctor?.approvalStatus === 'REJECTED') {
    return ['APPROVED']
  }
  return []
}
