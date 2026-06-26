<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import {
  getAdminNavigation,
  getSidebarWidth,
  getWorkspaceIdentity,
  loadSidebarCollapsed,
  saveSidebarCollapsed,
} from '../features/admin/workspace'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()

const navigation = computed(() => getAdminNavigation(auth.role))
const identity = computed(() => getWorkspaceIdentity(auth.role))
const pageTitle = computed(() => route.meta.title || identity.value.workspaceLabel)
const sidebarCollapsed = ref(loadSidebarCollapsed(globalThis.localStorage))
const sidebarWidth = computed(() => getSidebarWidth(sidebarCollapsed.value))
const currentDate = new Intl.DateTimeFormat('zh-CN', {
  month: 'long',
  day: 'numeric',
  weekday: 'long',
}).format(new Date())

function toggleSidebar() {
  sidebarCollapsed.value = !sidebarCollapsed.value
  saveSidebarCollapsed(globalThis.localStorage, sidebarCollapsed.value)
}

async function logout() {
  auth.logout()
  await router.replace('/login')
}
</script>

<template>
  <div
    :class="[
      'admin-shell',
      `role-${auth.role || 'admin'}`,
      { 'sidebar-collapsed': sidebarCollapsed },
    ]"
    :style="{ '--admin-sidebar-width': sidebarWidth }"
  >
    <aside class="admin-sidebar">
      <button
        class="sidebar-toggle"
        type="button"
        :aria-expanded="!sidebarCollapsed"
        :aria-label="sidebarCollapsed ? '展开侧边栏' : '收起侧边栏'"
        :title="sidebarCollapsed ? '展开侧边栏' : '收起侧边栏'"
        @click="toggleSidebar"
      >
        <span aria-hidden="true"></span>
      </button>

      <RouterLink class="admin-brand" to="/admin">
        <img class="admin-brand-mark" src="../assets/brand/logo-mark.png" alt="" />
        <span class="admin-brand-copy">
          <strong>知身问养</strong>
          <small>中医问诊与日常养护平台</small>
        </span>
      </RouterLink>

      <div class="workspace-card">
        <span>{{ identity.badge }}</span>
        <div class="workspace-copy">
          <small>{{ identity.roleLabel }}</small>
          <strong>{{ identity.workspaceLabel }}</strong>
        </div>
      </div>

      <nav class="admin-nav" aria-label="后台主导航">
        <section v-for="section in navigation" :key="section.label">
          <p>{{ section.label }}</p>
          <RouterLink
            v-for="item in section.items"
            :key="item.to"
            :to="item.to"
            :title="sidebarCollapsed ? item.label : undefined"
          >
            <span aria-hidden="true">{{ item.icon }}</span>
            <span class="nav-label">{{ item.label }}</span>
          </RouterLink>
        </section>
      </nav>

      <div class="sidebar-account">
        <span aria-hidden="true">{{ identity.badge }}</span>
        <div class="account-copy">
          <strong>{{ auth.displayName || identity.roleLabel }}</strong>
          <small>{{ identity.headline }}</small>
        </div>
      </div>
    </aside>

    <div class="admin-content">
      <header class="admin-topbar">
        <div>
          <small>后台工作区 / {{ pageTitle }}</small>
          <strong>{{ pageTitle }}</strong>
        </div>
        <div class="topbar-actions">
          <time>{{ currentDate }}</time>
          <RouterLink to="/">平台首页</RouterLink>
          <button type="button" @click="logout">退出登录</button>
        </div>
      </header>

      <main class="admin-main">
        <RouterView />
      </main>
    </div>
  </div>
</template>

<style scoped>
.admin-shell {
  display: grid;
  height: 100vh;
  grid-template-columns: var(--admin-sidebar-width, 252px) minmax(0, 1fr);
  overflow: hidden;
  background:
    radial-gradient(circle at 92% 6%, rgb(118 169 142 / 13%), transparent 24%),
    #f1f4f1;
  transition: grid-template-columns .24s ease;
}

.admin-sidebar {
  position: relative;
  display: flex;
  flex-direction: column;
  min-height: 0;
  padding: 20px 16px 16px;
  border-right: 1px solid rgb(29 74 54 / 10%);
  background: rgb(249 250 248 / 96%);
  color: var(--color-ink);
  box-shadow: 18px 0 42px rgb(21 56 42 / 5%);
  transition: padding .24s ease;
}

.sidebar-toggle {
  position: absolute;
  z-index: 3;
  top: 29px;
  right: -14px;
  display: grid;
  width: 28px;
  height: 28px;
  padding: 0;
  border: 1px solid rgb(47 95 72 / 16%);
  border-radius: 50%;
  background: rgb(255 255 255 / 96%);
  box-shadow: 0 7px 18px rgb(21 56 42 / 12%);
  cursor: pointer;
  place-items: center;
  transition: border-color .18s ease, box-shadow .18s ease, transform .18s ease;
}

.sidebar-toggle:hover {
  border-color: rgb(47 95 72 / 36%);
  box-shadow: 0 9px 20px rgb(21 56 42 / 18%);
  transform: translateY(-1px);
}

.sidebar-toggle span {
  width: 7px;
  height: 7px;
  border-bottom: 2px solid var(--color-ink);
  border-left: 2px solid var(--color-ink);
  transform: translateX(1px) rotate(45deg);
  transition: transform .24s ease;
}

.sidebar-collapsed .sidebar-toggle span {
  transform: translateX(-1px) rotate(225deg);
}

.admin-brand {
  display: flex;
  min-height: 56px;
  align-items: center;
  gap: 10px;
  padding: 0 10px;
  overflow: hidden;
}

.admin-brand-mark {
  width: 38px;
  height: 38px;
  object-fit: contain;
}

.admin-brand strong,
.admin-brand small,
.sidebar-account strong,
.sidebar-account small {
  display: block;
}

.admin-brand small {
  margin-top: 3px;
  color: var(--color-text-muted);
  font-size: 9px;
}

.admin-brand-copy,
.workspace-copy,
.account-copy,
.nav-label {
  opacity: 1;
  transition: opacity .14s ease;
}

.workspace-card {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 22px 4px 16px;
  padding: 13px;
  border: 1px solid rgb(62 116 88 / 14%);
  border-radius: 18px;
  background:
    radial-gradient(circle at 90% 0%, rgb(219 235 225 / 92%), transparent 44%),
    white;
}

.workspace-card > span,
.sidebar-account > span {
  display: grid;
  width: 36px;
  height: 36px;
  flex: 0 0 auto;
  place-items: center;
  border-radius: 12px;
  background: var(--color-ink);
  color: white;
  font-size: 13px;
  font-weight: 900;
}

.workspace-card small {
  color: var(--color-cinnabar);
  font-size: 9px;
  font-weight: 900;
  letter-spacing: .08em;
}

.workspace-card strong {
  display: block;
  margin-top: 4px;
  font-size: 13px;
}

.admin-nav {
  min-height: 0;
  flex: 1;
  overflow-y: auto;
  padding: 0 4px;
}

.admin-nav section + section {
  margin-top: 20px;
}

.admin-nav p {
  margin: 0 10px 7px;
  color: rgb(68 94 81 / 56%);
  font-size: 9px;
  font-weight: 900;
  letter-spacing: .14em;
}

.admin-nav a {
  display: flex;
  min-height: 44px;
  align-items: center;
  gap: 10px;
  padding: 0 11px;
  border: 1px solid transparent;
  border-radius: 13px;
  color: #61766d;
  font-size: 13px;
  font-weight: 700;
  transition: .18s ease;
}

.admin-nav a > span {
  display: grid;
  width: 25px;
  height: 25px;
  place-items: center;
  border-radius: 8px;
  background: rgb(44 93 68 / 7%);
  color: var(--color-ink);
  font-size: 10px;
  font-weight: 900;
}

.admin-nav a > .nav-label {
  display: inline;
  width: auto;
  height: auto;
  border-radius: 0;
  background: transparent;
  color: inherit;
  font-size: inherit;
  font-weight: inherit;
  place-items: initial;
}

.admin-nav a:hover,
.admin-nav a.router-link-exact-active {
  border-color: rgb(62 116 88 / 12%);
  background: white;
  color: var(--color-ink);
  box-shadow: 0 8px 18px rgb(21 56 42 / 6%);
}

.admin-nav a.router-link-exact-active {
  box-shadow: inset 3px 0 var(--color-cinnabar), 0 8px 18px rgb(21 56 42 / 6%);
}

.sidebar-account {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 16px;
  padding: 12px 10px 4px;
  border-top: 1px solid rgb(29 74 54 / 9%);
}

.sidebar-account > span {
  border-radius: 50%;
  background: #dcebe2;
  color: var(--color-ink);
}

.sidebar-account strong {
  overflow: hidden;
  max-width: 150px;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.sidebar-account small {
  margin-top: 3px;
  color: var(--color-text-muted);
  font-size: 9px;
}

.sidebar-collapsed .admin-sidebar {
  padding: 0;
  border-right: 0;
  background: transparent;
  box-shadow: none;
}

.sidebar-collapsed .admin-sidebar > :not(.sidebar-toggle) {
  visibility: hidden;
  opacity: 0;
  pointer-events: none;
}

.sidebar-collapsed .sidebar-toggle {
  top: 19px;
  right: -46px;
}

.sidebar-collapsed .admin-brand {
  justify-content: center;
  padding-inline: 0;
}

.sidebar-collapsed .admin-brand-copy,
.sidebar-collapsed .workspace-copy,
.sidebar-collapsed .account-copy,
.sidebar-collapsed .nav-label,
.sidebar-collapsed .admin-nav p {
  display: none;
  opacity: 0;
}

.sidebar-collapsed .workspace-card {
  justify-content: center;
  margin-inline: 0;
  padding: 10px 0;
}

.sidebar-collapsed .admin-nav {
  padding-inline: 0;
}

.sidebar-collapsed .admin-nav section + section {
  margin-top: 12px;
}

.sidebar-collapsed .admin-nav a {
  justify-content: center;
  gap: 0;
  padding-inline: 0;
}

.sidebar-collapsed .admin-nav a > span:first-child {
  width: 30px;
  height: 30px;
}

.sidebar-collapsed .sidebar-account {
  justify-content: center;
  padding-inline: 0;
}

.sidebar-collapsed .admin-topbar {
  padding-left: 64px;
}

.admin-content {
  display: grid;
  min-height: 0;
  min-width: 0;
  grid-template-rows: 70px minmax(0, 1fr);
}

.admin-topbar {
  display: flex;
  min-height: 70px;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 10px 26px;
  border-bottom: 1px solid rgb(29 74 54 / 9%);
  background: rgb(255 255 255 / 72%);
  backdrop-filter: blur(18px);
}

.admin-topbar strong,
.admin-topbar small {
  display: block;
}

.admin-topbar small {
  color: var(--color-text-muted);
  font-size: 9px;
}

.admin-topbar strong {
  margin-top: 4px;
  font-family: "Noto Serif SC", "STSong", serif;
  font-size: 18px;
}

.topbar-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.topbar-actions time {
  margin-right: 8px;
  color: var(--color-text-muted);
  font-size: 11px;
}

.topbar-actions a,
.topbar-actions button {
  display: inline-flex;
  min-height: 36px;
  align-items: center;
  padding: 0 13px;
  border: 1px solid rgb(62 116 88 / 15%);
  border-radius: 999px;
  background: rgb(255 255 255 / 76%);
  color: var(--color-ink);
  cursor: pointer;
  font-size: 11px;
  font-weight: 800;
}

.topbar-actions button {
  color: var(--color-cinnabar);
}

.topbar-actions a:hover,
.topbar-actions button:hover {
  border-color: var(--color-cinnabar);
  background: white;
}

.admin-main {
  min-height: 0;
  overflow-y: auto;
  padding: 24px 26px 30px;
}

@media (max-width: 880px) {
  .admin-shell {
    height: auto;
    grid-template-columns: 1fr;
    overflow: visible;
  }

  .admin-shell.sidebar-collapsed {
    grid-template-columns: 1fr;
  }

  .admin-sidebar {
    min-height: auto;
    padding: 12px;
  }

  .sidebar-toggle {
    display: none;
  }

  .sidebar-collapsed .admin-sidebar {
    padding: 12px;
    border-right: 1px solid rgb(29 74 54 / 10%);
    background: rgb(249 250 248 / 96%);
    box-shadow: 18px 0 42px rgb(21 56 42 / 5%);
  }

  .sidebar-collapsed .admin-sidebar > :not(.sidebar-toggle) {
    visibility: visible;
    opacity: 1;
    pointer-events: auto;
  }

  .sidebar-collapsed .admin-brand {
    justify-content: flex-start;
  }

  .sidebar-collapsed .admin-brand-copy,
  .sidebar-collapsed .nav-label {
    display: initial;
    opacity: 1;
  }

  .sidebar-collapsed .admin-nav a {
    width: auto;
    min-height: 44px;
    justify-content: flex-start;
    gap: 10px;
    margin-inline: 0;
    padding: 0 11px;
    border-radius: 13px;
  }

  .sidebar-collapsed .admin-topbar {
    padding-left: 26px;
  }

  .workspace-card,
  .sidebar-account,
  .admin-brand small {
    display: none;
  }

  .admin-nav {
    display: flex;
    margin-top: 12px;
    overflow-x: auto;
  }

  .admin-nav section {
    display: flex;
  }

  .admin-nav section + section {
    margin-top: 0;
  }

  .admin-nav p {
    display: none;
  }

  .admin-nav a {
    flex: 0 0 auto;
  }

  .admin-content {
    grid-template-rows: auto minmax(0, 1fr);
  }
}

@media (max-width: 620px) {
  .admin-topbar {
    padding: 12px;
  }

  .topbar-actions time {
    display: none;
  }

  .admin-main {
    padding: 16px 12px;
  }
}
</style>
