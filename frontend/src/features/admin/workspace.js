const ADMIN_NAVIGATION = [
  {
    label: '运营中心',
    items: [
      { label: '数据概览', to: '/admin', icon: '概' },
      { label: '问诊调度', to: '/admin/consultations', icon: '诊' },
    ],
  },
  {
    label: '人员管理',
    items: [
      { label: '用户管理', to: '/admin/users', icon: '用' },
      { label: '医生管理', to: '/admin/doctors', icon: '医' },
    ],
  },
  {
    label: '内容管理',
    items: [
      { label: '养生文章', to: '/admin/knowledge', icon: '文' },
      { label: '药膳管理', to: '/admin/recipes', icon: '膳' },
    ],
  },
  {
    label: '数据与系统',
    items: [
      { label: '数据导出', to: '/admin/export', icon: '数' },
    ],
  },
]

const DOCTOR_NAVIGATION = [
  {
    label: '我的工作',
    items: [
      { label: '医生工作台', to: '/admin', icon: '台' },
      { label: '科室问诊池', to: '/admin/department-pool', icon: '池' },
      { label: '我的问诊', to: '/admin/my-consultations', icon: '诊' },
    ],
  },
  {
    label: '参考内容',
    items: [
      { label: '养生知识', to: '/knowledge', icon: '知' },
      { label: '药膳资料', to: '/recipes', icon: '膳' },
    ],
  },
]

const WORKSPACE_IDENTITIES = {
  admin: {
    badge: '管',
    roleLabel: '系统管理员',
    workspaceLabel: '平台运营中心',
    headline: '全局运营与资源调度',
  },
  doctor: {
    badge: '医',
    roleLabel: '医生',
    workspaceLabel: '个人接诊工作台',
    headline: '问诊接诊与处理记录',
  },
}

const SIDEBAR_COLLAPSED_KEY = 'tcm-admin-sidebar-collapsed'

export function getAdminNavigation(role) {
  return role === 'doctor' ? DOCTOR_NAVIGATION : ADMIN_NAVIGATION
}

export function getWorkspaceIdentity(role) {
  return WORKSPACE_IDENTITIES[role] || WORKSPACE_IDENTITIES.admin
}

export function getSidebarWidth(collapsed) {
  return collapsed ? '0px' : '252px'
}

export function loadSidebarCollapsed(storage) {
  try {
    return storage?.getItem(SIDEBAR_COLLAPSED_KEY) === 'true'
  } catch {
    return false
  }
}

export function saveSidebarCollapsed(storage, collapsed) {
  try {
    storage?.setItem(SIDEBAR_COLLAPSED_KEY, String(Boolean(collapsed)))
  } catch {
    // The layout still works when browser storage is unavailable.
  }
}
