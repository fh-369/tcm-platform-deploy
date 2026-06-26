import { beforeEach, describe, expect, it, vi } from 'vitest'

const request = {
  get: vi.fn(),
  post: vi.fn(),
}

vi.mock('./request', () => ({ default: request }))

describe('doctor admission auth API', () => {
  beforeEach(() => {
    request.get.mockReset()
    request.post.mockReset()
  })

  it('loads public departments', async () => {
    request.get.mockResolvedValue({
      data: { code: 200, data: [{ id: 1, name: '综合咨询' }] },
    })
    const { getDepartments } = await import('./auth')

    const departments = await getDepartments()

    expect(request.get).toHaveBeenCalledWith('/auth/departments')
    expect(departments).toHaveLength(1)
  })

  it('submits a doctor application without creating a login session', async () => {
    const profile = {
      username: 'doctor2',
      password: 'doctor123',
      displayName: '李医生',
      departmentId: 2,
    }
    request.post.mockResolvedValue({
      data: {
        code: 200,
        data: { accountId: 12, username: 'doctor2', approvalStatus: 'PENDING' },
      },
    })
    const { applyDoctor } = await import('./auth')

    const result = await applyDoctor(profile)

    expect(request.post).toHaveBeenCalledWith('/auth/doctor-applications', profile)
    expect(result.approvalStatus).toBe('PENDING')
  })
})
