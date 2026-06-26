import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { useAuthStore } from './auth'

function createStorage() {
  const values = new Map()

  return {
    getItem: vi.fn((key) => values.get(key) ?? null),
    removeItem: vi.fn((key) => values.delete(key)),
    setItem: vi.fn((key, value) => values.set(key, value)),
  }
}

let storage

beforeEach(() => {
  storage = createStorage()
  vi.stubGlobal('window', { localStorage: storage })
  setActivePinia(createPinia())
})

describe('auth store', () => {
  it('clears the in-memory and stored session with one logout', () => {
    const auth = useAuthStore()
    auth.setSession({
      token: 'sample-token',
      role: 'patient',
      userId: 7,
      displayName: '林女士',
    })

    auth.logout()

    expect(auth.$state).toEqual({
      token: '',
      role: '',
      userId: null,
      displayName: '',
    })
    expect(auth.isAuthenticated).toBe(false)
    expect(storage.removeItem).toHaveBeenCalledWith('tcm-auth-session')
  })
})
