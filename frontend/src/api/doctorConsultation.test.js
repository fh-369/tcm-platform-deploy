import { beforeEach, describe, expect, it, vi } from 'vitest'

const request = {
  get: vi.fn(),
  put: vi.fn(),
}

vi.mock('./request', () => ({ default: request }))

describe('doctor consultation API', () => {
  beforeEach(() => {
    request.get.mockReset()
    request.put.mockReset()
  })

  it('loads the department pool and personal consultations separately', async () => {
    request.get.mockResolvedValue({ data: { code: 200, data: { records: [] } } })
    const { getDepartmentPool, getMyDoctorConsultations } =
      await import('./doctorConsultation')
    const poolParams = { current: 1, size: 10, scope: 'all' }
    const mineParams = { current: 1, size: 10, status: '接诊中' }

    await getDepartmentPool(poolParams)
    await getMyDoctorConsultations(mineParams)

    expect(request.get).toHaveBeenNthCalledWith(1, '/doctor/consultations/pool', {
      params: poolParams,
    })
    expect(request.get).toHaveBeenNthCalledWith(2, '/doctor/consultations/mine', {
      params: mineParams,
    })
  })

  it('claims and updates through doctor-only endpoints', async () => {
    request.put.mockResolvedValue({ data: { code: 200, data: { id: 9 } } })
    const { claimDoctorConsultation, updateDoctorConsultation } =
      await import('./doctorConsultation')

    await claimDoctorConsultation(9)
    await updateDoctorConsultation(9, { status: '接诊中', doctorNote: '已接诊' })

    expect(request.put).toHaveBeenNthCalledWith(1, '/doctor/consultations/9/claim')
    expect(request.put).toHaveBeenNthCalledWith(2, '/doctor/consultations/9', {
      status: '接诊中',
      doctorNote: '已接诊',
    })
  })
})
