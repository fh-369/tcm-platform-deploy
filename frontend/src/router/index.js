import { createRouter, createWebHistory } from 'vue-router'

import {
  canAccessRole,
  consultationWorkspaceRouteForRole,
  defaultRouteForRole,
} from './access'
import { pinia } from '../stores'
import { useAuthStore } from '../stores/auth'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      component: () => import('../layouts/PatientLayout.vue'),
      children: [
        {
          path: '',
          name: 'home',
          component: () => import('../views/patient/HomeView.vue'),
        },
        {
          path: 'consultation/new',
          name: 'consultation-new',
          component: () => import('../views/patient/ConsultationFormView.vue'),
          meta: {
            title: '在线问诊',
            description: '患者问诊表单将在 Frontend Phase 3 接入真实接口。',
            requiresAuth: true,
            roles: ['patient'],
          },
        },
        {
          path: 'consultation/my',
          name: 'consultation-my',
          component: () => import('../views/patient/MyConsultationsView.vue'),
          meta: {
            title: '我的问诊',
            description: '患者问诊记录将在 Frontend Phase 3 接入真实接口。',
            requiresAuth: true,
            roles: ['patient'],
          },
        },
        {
          path: 'knowledge',
          name: 'knowledge',
          component: () => import('../views/patient/KnowledgeListView.vue'),
          meta: { title: '养生知识' },
        },
        {
          path: 'knowledge/:id',
          name: 'knowledge-detail',
          component: () => import('../views/patient/KnowledgeDetailView.vue'),
        },
        {
          path: 'recipes',
          name: 'recipes',
          component: () => import('../views/patient/RecipeListView.vue'),
          meta: { title: '药膳推荐' },
        },
        {
          path: 'recipes/:id',
          name: 'recipe-detail',
          component: () => import('../views/patient/RecipeDetailView.vue'),
        },
        {
          path: 'ai/ask',
          name: 'ai-ask',
          component: () => import('../views/patient/AIAskView.vue'),
          meta: { requiresAuth: true, roles: ['patient'] },
        },
        {
          path: 'profile',
          name: 'profile',
          component: () => import('../views/patient/PatientPlaceholderView.vue'),
          meta: {
            title: '我的',
            description: '个人资料与我的问诊将在后续阶段实现。',
            requiresAuth: true,
            roles: ['patient'],
          },
        },
      ],
    },
    {
      path: '/',
      component: () => import('../layouts/AuthLayout.vue'),
      children: [
        {
          path: 'login',
          name: 'login',
          component: () => import('../views/auth/AuthView.vue'),
          meta: { guestOnly: true },
        },
        {
          path: 'login/patient',
          name: 'patient-login',
          component: () => import('../views/auth/AuthView.vue'),
          meta: { guestOnly: true },
        },
        {
          path: 'login/admin',
          name: 'admin-login',
          component: () => import('../views/auth/AuthView.vue'),
          meta: { guestOnly: true },
        },
        {
          path: 'register',
          name: 'register',
          component: () => import('../views/auth/AuthView.vue'),
          meta: { guestOnly: true },
        },
        {
          path: 'doctor/apply',
          name: 'doctor-apply',
          component: () => import('../views/auth/DoctorApplicationView.vue'),
          meta: { guestOnly: true },
        },
      ],
    },
    {
      path: '/admin',
      component: () => import('../layouts/AdminLayout.vue'),
      meta: { requiresAuth: true, roles: ['doctor', 'admin'] },
      children: [
        {
          path: '',
          name: 'admin-dashboard',
          component: () => import('../views/admin/DashboardView.vue'),
          meta: { title: '数据概览', roles: ['doctor', 'admin'] },
        },
        {
          path: 'consultations',
          name: 'admin-consultations',
          component: () => import('../views/admin/ConsultationManagementView.vue'),
          meta: { title: '问诊调度', roles: ['admin'] },
        },
        {
          path: 'department-pool',
          name: 'doctor-department-pool',
          component: () => import('../views/admin/DoctorDepartmentPoolView.vue'),
          meta: { title: '科室问诊池', roles: ['doctor'] },
        },
        {
          path: 'my-consultations',
          name: 'doctor-my-consultations',
          component: () => import('../views/admin/DoctorMyConsultationsView.vue'),
          meta: { title: '我的问诊', roles: ['doctor'] },
        },
        {
          path: 'users',
          name: 'admin-users',
          component: () => import('../views/admin/PersonnelManagementView.vue'),
          props: { resource: 'users' },
          meta: {
            title: '用户管理',
            roles: ['admin'],
          },
        },
        {
          path: 'doctors',
          name: 'admin-doctors',
          component: () => import('../views/admin/PersonnelManagementView.vue'),
          props: { resource: 'doctors' },
          meta: {
            title: '医生管理',
            roles: ['admin'],
          },
        },
        {
          path: 'knowledge',
          name: 'admin-knowledge',
          component: () => import('../views/admin/ContentManagementView.vue'),
          props: { resource: 'knowledge' },
          meta: { title: '养生文章', roles: ['admin'] },
        },
        {
          path: 'recipes',
          name: 'admin-recipes',
          component: () => import('../views/admin/ContentManagementView.vue'),
          props: { resource: 'recipe' },
          meta: { title: '药膳管理', roles: ['admin'] },
        },
        {
          path: 'export',
          name: 'admin-export',
          component: () => import('../views/admin/ExportView.vue'),
          meta: { title: '数据导出', roles: ['admin'] },
        },
      ],
    },
    {
      path: '/:pathMatch(.*)*',
      name: 'not-found',
      component: () => import('../views/NotFoundView.vue'),
    },
  ],
})

router.beforeEach((to) => {
  const auth = useAuthStore(pinia)

  if (to.meta.guestOnly && auth.isAuthenticated) {
    return defaultRouteForRole(auth.role)
  }

  if (to.meta.requiresAuth && !auth.isAuthenticated) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }

  if (to.name === 'admin-consultations' && auth.role === 'doctor') {
    return consultationWorkspaceRouteForRole(auth.role)
  }

  if (to.meta.roles && !canAccessRole(auth.role, to.meta.roles)) {
    return defaultRouteForRole(auth.role)
  }

  return true
})

export default router
