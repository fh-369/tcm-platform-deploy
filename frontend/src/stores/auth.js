import { defineStore } from 'pinia'

import {
  clearSession,
  getBrowserStorage,
  loadSession,
  saveSession,
} from './authSession'

export const useAuthStore = defineStore('auth', {
  state: () => loadSession(getBrowserStorage()),
  getters: {
    isAuthenticated: (state) => Boolean(state.token),
    isPatient: (state) => state.role === 'patient',
    isAdmin: (state) => ['doctor', 'admin'].includes(state.role),
  },
  actions: {
    setSession(session) {
      Object.assign(this, session)
      saveSession(getBrowserStorage(), this.$state)
    },
    logout() {
      clearSession(getBrowserStorage())
      this.$reset()
    },
  },
})
