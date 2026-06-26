import { describe, expect, it } from 'vitest'

import { getDoctorWorkflow } from './workflow'

describe('doctor consultation workflow', () => {
  it('only allows starting a pending consultation', () => {
    expect(getDoctorWorkflow('待接诊')).toEqual({
      canStart: true,
      canReply: false,
      canComplete: false,
      readOnly: false,
    })
  })

  it('allows replies and completion while consultation is active', () => {
    expect(getDoctorWorkflow('接诊中')).toEqual({
      canStart: false,
      canReply: true,
      canComplete: true,
      readOnly: false,
    })
  })

  it('makes completed consultation read only', () => {
    expect(getDoctorWorkflow('已完成')).toEqual({
      canStart: false,
      canReply: false,
      canComplete: false,
      readOnly: true,
    })
  })
})
