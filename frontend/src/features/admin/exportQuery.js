export function createExportQueryState() {
  return {
    matchedCount: null,
    countError: '',
    queried: false,
  }
}

export function invalidateExportQuery(state) {
  state.matchedCount = null
  state.countError = ''
  state.queried = false
}

export function applyExportQueryResult(state, count) {
  state.matchedCount = Number(count)
  state.countError = ''
  state.queried = true
}

export function applyExportQueryError(state, message) {
  state.matchedCount = null
  state.countError = message
  state.queried = true
}

export function exportQuerySignature(params) {
  return JSON.stringify(params)
}
