const SESSION_KEY = 'tcm-auth-session'

export const EMPTY_SESSION = Object.freeze({
  token: '',
  role: '',
  userId: null,
  displayName: '',
})

function normalizeSession(session = {}) {
  return {
    token: typeof session.token === 'string' ? session.token : '',
    role: typeof session.role === 'string' ? session.role.toLowerCase() : '',
    userId: session.userId ?? null,
    displayName: typeof session.displayName === 'string' ? session.displayName : '',
  }
}

export function loadSession(storage) {
  if (!storage) {
    return { ...EMPTY_SESSION }
  }

  try {
    return normalizeSession(JSON.parse(storage.getItem(SESSION_KEY) || '{}'))
  } catch {
    return { ...EMPTY_SESSION }
  }
}

export function saveSession(storage, session) {
  storage?.setItem(SESSION_KEY, JSON.stringify(normalizeSession(session)))
}

export function clearSession(storage) {
  storage?.removeItem(SESSION_KEY)
}

export function getBrowserStorage() {
  return typeof window === 'undefined' ? null : window.localStorage
}
