import { describe, expect, it } from 'vitest'

import {
  canAccessRole,
  consultationWorkspaceRouteForRole,
  defaultRouteForRole,
} from './access'

describe('role route access', () => {
  it('allows patients to enter patient-only routes', () => {
    expect(canAccessRole('patient', ['patient'])).toBe(true)
  })

  it('allows doctors and admins to enter admin routes', () => {
    expect(canAccessRole('doctor', ['doctor', 'admin'])).toBe(true)
    expect(canAccessRole('admin', ['doctor', 'admin'])).toBe(true)
  })

  it('rejects a role outside the accepted role list', () => {
    expect(canAccessRole('patient', ['doctor', 'admin'])).toBe(false)
    expect(canAccessRole('', ['patient'])).toBe(false)
  })

  it('chooses the correct home route for each role', () => {
    expect(defaultRouteForRole('patient')).toBe('/')
    expect(defaultRouteForRole('doctor')).toBe('/admin')
    expect(defaultRouteForRole('admin')).toBe('/admin')
  })

  it('keeps admin scheduling separate from the doctor department pool', () => {
    expect(consultationWorkspaceRouteForRole('admin')).toBe('/admin/consultations')
    expect(consultationWorkspaceRouteForRole('doctor')).toBe('/admin/department-pool')
  })
})
