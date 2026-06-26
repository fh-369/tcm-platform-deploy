import { describe, expect, it } from 'vitest'

import {
  approvalMeta,
  canEnableDoctor,
  doctorReviewActions,
} from './doctorAdmission'

describe('doctor admission display rules', () => {
  it('maps each approval status to clear operator copy', () => {
    expect(approvalMeta('PENDING')).toMatchObject({
      label: '待审核',
      tone: 'pending',
    })
    expect(approvalMeta('APPROVED').label).toBe('已通过')
    expect(approvalMeta('REJECTED').label).toBe('未通过')
  })

  it('only permits approved doctors to be enabled', () => {
    expect(canEnableDoctor({ approvalStatus: 'APPROVED' })).toBe(true)
    expect(canEnableDoctor({ approvalStatus: 'PENDING' })).toBe(false)
    expect(canEnableDoctor({ approvalStatus: 'REJECTED' })).toBe(false)
  })

  it('offers review actions for applications that are not already approved', () => {
    expect(doctorReviewActions({ approvalStatus: 'PENDING' })).toEqual([
      'APPROVED',
      'REJECTED',
    ])
    expect(doctorReviewActions({ approvalStatus: 'REJECTED' })).toEqual([
      'APPROVED',
    ])
    expect(doctorReviewActions({ approvalStatus: 'APPROVED' })).toEqual([])
  })
})
