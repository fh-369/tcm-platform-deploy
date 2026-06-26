export function canAccessRole(role, acceptedRoles = []) {
  return acceptedRoles.includes((role || '').toLowerCase())
}

export function defaultRouteForRole(role) {
  return ['doctor', 'admin'].includes((role || '').toLowerCase()) ? '/admin' : '/'
}

export function consultationWorkspaceRouteForRole(role) {
  return (role || '').toLowerCase() === 'doctor'
    ? '/admin/department-pool'
    : '/admin/consultations'
}
