import { describe, expect, it } from 'vitest'

import {
  isCrossDepartmentConsultation,
  reminderDisplay,
  statusDisplay,
  urgencyDisplay,
} from './display'

describe('consultation display rules', () => {
  it('maps each consultation status to a readable display configuration', () => {
    expect(statusDisplay('待接诊')).toEqual({ label: '待接诊', tone: 'waiting' })
    expect(statusDisplay('接诊中')).toEqual({ label: '接诊中', tone: 'active' })
    expect(statusDisplay('已完成')).toEqual({ label: '已完成', tone: 'complete' })
  })

  it('falls back safely for an unknown status', () => {
    expect(statusDisplay('未知')).toEqual({ label: '未知', tone: 'neutral' })
  })

  it('maps urgency and reminder levels to visual tones', () => {
    expect(urgencyDisplay('普通').tone).toBe('normal')
    expect(urgencyDisplay('紧急').tone).toBe('attention')
    expect(urgencyDisplay('非常紧急').tone).toBe('urgent')
    expect(reminderDisplay('urgent').label).toBe('优先提醒')
    expect(reminderDisplay('attention').tone).toBe('attention')
  })

  it('detects cross-department handling without flagging general consultation', () => {
    expect(isCrossDepartmentConsultation({
      departmentName: '中医妇科',
      doctorDepartment: '中医内科',
    })).toBe(true)
    expect(isCrossDepartmentConsultation({
      departmentName: '中医内科',
      doctorDepartment: '中医内科',
    })).toBe(false)
    expect(isCrossDepartmentConsultation({
      departmentName: '综合咨询',
      doctorDepartment: '中医内科',
    })).toBe(false)
  })
})
