import { beforeEach, describe, expect, it, vi } from 'vitest'

const request = {
  get: vi.fn(),
  put: vi.fn(),
}

vi.mock('./request', () => ({ default: request }))

describe('admin consultation API', () => {
  beforeEach(() => {
    request.get.mockReset()
    request.put.mockReset()
  })

  it('queries the admin consultation endpoint with filters', async () => {
    request.get.mockResolvedValue({ data: { code: 200, data: { records: [] } } })
    const { getAdminConsultations } = await import('./adminConsultation')
    const params = { current: 1, size: 10, status: '待接诊', keyword: '胃痛' }

    await getAdminConsultations(params)

    expect(request.get).toHaveBeenCalledWith('/admin/consultation', { params })
  })

  it('updates a consultation through the admin endpoint', async () => {
    request.put.mockResolvedValue({ data: { code: 200, data: { id: 9 } } })
    const { updateAdminConsultation } = await import('./adminConsultation')
    const payload = { status: '接诊中', doctorNote: '已查看' }

    await updateAdminConsultation(9, payload)

    expect(request.put).toHaveBeenCalledWith('/admin/consultation/9', payload)
  })

  it('assigns and claims consultations through dedicated endpoints', async () => {
    request.put.mockResolvedValue({ data: { code: 200, data: { id: 9, doctorId: 6 } } })
    const { assignAdminConsultation, claimAdminConsultation } =
      await import('./adminConsultation')

    await assignAdminConsultation(9, 6)
    await claimAdminConsultation(10)

    expect(request.put).toHaveBeenCalledWith('/admin/consultation/9/assignment', {
      doctorId: 6,
    })
    expect(request.put).toHaveBeenCalledWith('/admin/consultation/10/claim')
  })

  it('updates consultation department', async () => {
    request.put.mockResolvedValue({
      data: {
        code: 200,
        data: { id: 9, departmentId: 3, departmentName: '中医妇科' },
      },
    })
    const { updateAdminConsultationDepartment } = await import('./adminConsultation')

    const result = await updateAdminConsultationDepartment(9, 3)

    expect(request.put).toHaveBeenCalledWith('/admin/consultation/9/department', {
      departmentId: 3,
    })
    expect(result.departmentName).toBe('中医妇科')
  })
})
