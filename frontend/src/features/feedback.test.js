import { describe, expect, it } from 'vitest'

import { getApiErrorMessage } from './feedback'

describe('administrator feedback', () => {
  it('prefers a backend business message', () => {
    expect(getApiErrorMessage({
      response: { data: { message: '开始日期不能晚于结束日期' } },
    }, '导出失败')).toBe('开始日期不能晚于结束日期')
  })

  it('describes network failures without exposing axios wording', () => {
    expect(getApiErrorMessage({ code: 'ERR_NETWORK' }, '加载失败'))
      .toBe('无法连接服务器，请检查后端服务和网络连接')
  })

  it('falls back to the operation-specific message', () => {
    expect(getApiErrorMessage({}, '统计数据加载失败')).toBe('统计数据加载失败')
  })
})
