import { describe, expect, it } from 'vitest'

import {
  getAdminNavigation,
  getSidebarWidth,
  loadSidebarCollapsed,
  saveSidebarCollapsed,
  getWorkspaceIdentity,
} from './workspace'

describe('admin workspace role configuration', () => {
  it('shows platform management sections to administrators', () => {
    const navigation = getAdminNavigation('admin')

    expect(navigation.map((section) => section.label)).toEqual([
      '运营中心',
      '人员管理',
      '内容管理',
      '数据与系统',
    ])
    expect(navigation.flatMap((section) => section.items).map((item) => item.to)).toContain(
      '/admin/users',
      '/admin/export',
    )
  })

  it('shows a focused consultation workspace to doctors', () => {
    const navigation = getAdminNavigation('doctor')
    const destinations = navigation.flatMap((section) => section.items).map((item) => item.to)

    expect(navigation.map((section) => section.label)).toEqual(['我的工作', '参考内容'])
    expect(destinations).toContain('/admin/department-pool')
    expect(destinations).toContain('/admin/my-consultations')
    expect(destinations).not.toContain('/admin/consultations')
    expect(destinations).toContain('/knowledge')
    expect(destinations).not.toContain('/admin/users')
    expect(destinations).not.toContain('/admin/export')
  })

  it('describes administrator and doctor workspaces differently', () => {
    expect(getWorkspaceIdentity('admin')).toEqual({
      badge: '管',
      roleLabel: '系统管理员',
      workspaceLabel: '平台运营中心',
      headline: '全局运营与资源调度',
    })
    expect(getWorkspaceIdentity('doctor')).toEqual({
      badge: '医',
      roleLabel: '医生',
      workspaceLabel: '个人接诊工作台',
      headline: '问诊接诊与处理记录',
    })
  })

  it('persists the desktop sidebar collapsed preference', () => {
    const values = new Map()
    const storage = {
      getItem: (key) => values.get(key) ?? null,
      setItem: (key, value) => values.set(key, value),
    }

    expect(loadSidebarCollapsed(storage)).toBe(false)

    saveSidebarCollapsed(storage, true)
    expect(loadSidebarCollapsed(storage)).toBe(true)

    saveSidebarCollapsed(storage, false)
    expect(loadSidebarCollapsed(storage)).toBe(false)
  })

  it('fully removes the desktop sidebar width when collapsed', () => {
    expect(getSidebarWidth(false)).toBe('252px')
    expect(getSidebarWidth(true)).toBe('0px')
  })
})
