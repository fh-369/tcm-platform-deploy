const WORKFLOWS = {
  待接诊: {
    canStart: true,
    canReply: false,
    canComplete: false,
    readOnly: false,
  },
  接诊中: {
    canStart: false,
    canReply: true,
    canComplete: true,
    readOnly: false,
  },
  已完成: {
    canStart: false,
    canReply: false,
    canComplete: false,
    readOnly: true,
  },
}

export function getDoctorWorkflow(status) {
  return WORKFLOWS[status] || WORKFLOWS.已完成
}
