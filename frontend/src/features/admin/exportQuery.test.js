import { describe, expect, it } from 'vitest'

import {
  applyExportQueryError,
  applyExportQueryResult,
  createExportQueryState,
  exportQuerySignature,
  invalidateExportQuery,
} from './exportQuery'

describe('export query state', () => {
  it('starts without a query result', () => {
    expect(createExportQueryState()).toEqual({
      matchedCount: null,
      countError: '',
      queried: false,
    })
  })

  it('stores a successful query result', () => {
    const state = createExportQueryState()

    applyExportQueryResult(state, '15')

    expect(state).toEqual({
      matchedCount: 15,
      countError: '',
      queried: true,
    })
  })

  it('invalidates a previous result after filters change', () => {
    const state = {
      matchedCount: 15,
      countError: '',
      queried: true,
    }

    invalidateExportQuery(state)

    expect(state).toEqual({
      matchedCount: null,
      countError: '',
      queried: false,
    })
  })

  it('keeps a failed query distinct from an unqueried state', () => {
    const state = createExportQueryState()

    applyExportQueryError(state, '导出数量查询失败')

    expect(state).toEqual({
      matchedCount: null,
      countError: '导出数量查询失败',
      queried: true,
    })
  })

  it('creates a stable signature for the requested filters', () => {
    expect(exportQuerySignature({
      dateFrom: '2026-06-01',
      status: '待接诊',
    })).toBe('{"dateFrom":"2026-06-01","status":"待接诊"}')
  })
})
