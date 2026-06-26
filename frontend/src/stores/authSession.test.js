import { describe, expect, it } from 'vitest'

import { clearSession, loadSession, saveSession } from './authSession'

function createStorage() {
  const values = new Map()

  return {
    getItem: (key) => values.get(key) ?? null,
    removeItem: (key) => values.delete(key),
    setItem: (key, value) => values.set(key, value),
  }
}

describe('auth session storage', () => {
  it('saves and restores the login response fields', () => {
    const storage = createStorage()
    const session = {
      token: 'sample-token',
      role: 'patient',
      userId: 7,
      displayName: '林女士',
    }

    saveSession(storage, session)

    expect(loadSession(storage)).toEqual(session)
  })

  it('returns an empty session when stored JSON is invalid', () => {
    const storage = createStorage()
    storage.setItem('tcm-auth-session', 'not-json')

    expect(loadSession(storage)).toEqual({
      token: '',
      role: '',
      userId: null,
      displayName: '',
    })
  })

  it('clears the stored session', () => {
    const storage = createStorage()
    saveSession(storage, { token: 'sample-token', role: 'patient' })

    clearSession(storage)

    expect(loadSession(storage).token).toBe('')
  })
})
