import { afterEach, describe, expect, it, vi } from 'vitest'
import { ElMessage } from 'element-plus'

import request from './request'

vi.mock('element-plus', () => ({
  ElMessage: {
    warning: vi.fn(),
  },
}))

afterEach(() => {
  vi.unstubAllGlobals()
  vi.clearAllMocks()
})

describe('authenticated request client', () => {
  it('adds a bearer token when a login session exists', async () => {
    vi.stubGlobal('window', {
      localStorage: {
        getItem: () =>
          JSON.stringify({
            token: 'sample-token',
            role: 'patient',
            userId: 7,
            displayName: '林女士',
          }),
      },
    })

    const response = await request.get('/protected-resource', {
      adapter: async (config) => ({
        config,
        data: null,
        headers: {},
        status: 200,
        statusText: 'OK',
      }),
    })

    expect(response.config.headers.Authorization).toBe('Bearer sample-token')
  })

  it('does not add Authorization for a guest request', async () => {
    vi.stubGlobal('window', {
      localStorage: {
        getItem: () => null,
      },
    })

    const response = await request.get('/public-resource', {
      adapter: async (config) => ({
        config,
        data: null,
        headers: {},
        status: 200,
        statusText: 'OK',
      }),
    })

    expect(response.config.headers.Authorization).toBeUndefined()
  })

  it('clears stale login state and redirects to login when the API returns 401', async () => {
    const removeItem = vi.fn()
    const assign = vi.fn()
    vi.stubGlobal('window', {
      localStorage: {
        getItem: () =>
          JSON.stringify({
            token: 'stale-token',
            role: 'patient',
            userId: 7,
            displayName: '林女士',
          }),
        removeItem,
      },
      location: {
        pathname: '/consultation/my',
        assign,
      },
    })

    await expect(request.get('/protected-resource', {
      adapter: async (config) => Promise.reject({
        config,
        message: 'Request failed with status code 401',
        response: { status: 401 },
      }),
    })).rejects.toMatchObject({
      message: '登录已过期，请重新登录',
    })

    expect(removeItem).toHaveBeenCalledWith('tcm-auth-session')
    expect(ElMessage.warning).toHaveBeenCalledWith('登录已过期，请重新登录')
    expect(assign).toHaveBeenCalledWith('/login')
  })

  it('keeps a valid session when the API returns 403 for insufficient permission', async () => {
    const removeItem = vi.fn()
    const assign = vi.fn()
    vi.stubGlobal('window', {
      localStorage: {
        getItem: () =>
          JSON.stringify({
            token: 'valid-token',
            role: 'patient',
            userId: 7,
            displayName: '林女士',
          }),
        removeItem,
      },
      location: {
        pathname: '/admin',
        assign,
      },
    })

    await expect(request.get('/admin/dashboard', {
      adapter: async (config) => Promise.reject({
        config,
        message: 'Request failed with status code 403',
        response: {
          status: 403,
          data: { message: '当前账号无权执行此操作' },
        },
      }),
    })).rejects.toMatchObject({
      message: '当前账号无权执行此操作',
    })

    expect(removeItem).not.toHaveBeenCalled()
    expect(assign).not.toHaveBeenCalled()
    expect(ElMessage.warning).toHaveBeenCalledWith('当前账号无权执行此操作')
  })
})
