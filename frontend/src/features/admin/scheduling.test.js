import { describe, expect, it } from 'vitest'

import { eligibleDoctorsForDepartment } from './scheduling'

const doctors = [
  {
    id: 101,
    userId: 11,
    displayName: '张医生',
    departmentId: 2,
    department: '中医内科',
    approvalStatus: 'APPROVED',
    enabled: true,
  },
  {
    id: 102,
    userId: 12,
    displayName: '李医生',
    departmentId: 3,
    department: '中医妇科',
    approvalStatus: 'APPROVED',
    enabled: true,
  },
  {
    id: 103,
    userId: 13,
    displayName: '待审核医生',
    departmentId: 2,
    approvalStatus: 'PENDING',
    enabled: false,
  },
]

describe('administrator scheduling doctor choices', () => {
  it('only returns approved enabled doctors from the consultation department', () => {
    const result = eligibleDoctorsForDepartment(doctors, 2, 1)

    expect(result.map((doctor) => doctor.userId)).toEqual([11])
  })

  it('allows all approved enabled doctors for general consultations', () => {
    const result = eligibleDoctorsForDepartment(doctors, 1, 1)

    expect(result.map((doctor) => doctor.userId)).toEqual([11, 12])
  })

  it('excludes records without a doctor profile id', () => {
    const result = eligibleDoctorsForDepartment(
      [{ ...doctors[0], userId: null }],
      2,
      1,
    )

    expect(result).toEqual([])
  })
})
