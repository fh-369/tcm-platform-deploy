<script setup>
import {
  ChatDotRound,
  Clock,
  DocumentAdd,
  Food,
  House,
  Reading,
  SwitchButton,
  User,
} from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'

import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const router = useRouter()

const navigation = [
  { label: '首页', to: '/', icon: House },
  { label: '在线问诊', to: '/consultation/new', icon: DocumentAdd },
  { label: '我的问诊', to: '/consultation/my', icon: Clock },
  { label: '养生知识', to: '/knowledge', icon: Reading },
  { label: '药膳推荐', to: '/recipes', icon: Food },
  { label: 'AI 问答', to: '/ai/ask', icon: ChatDotRound },
]

async function logout() {
  auth.logout()
  await router.replace('/login')
}
</script>

<template>
  <div class="patient-shell">
    <header class="patient-header">
      <div class="page-container header-inner">
        <RouterLink class="brand" to="/" aria-label="返回知身问养首页">
          <img class="brand-mark" src="../assets/brand/logo-mark.png" alt="" />
          <span>
            <strong>知身问养</strong>
            <small>中医问诊与日常养护平台</small>
          </span>
        </RouterLink>

        <nav class="patient-nav" aria-label="患者端主导航">
          <RouterLink v-for="item in navigation" :key="item.to" :to="item.to">
            <el-icon><component :is="item.icon" /></el-icon>
            {{ item.label }}
          </RouterLink>
          <div v-if="auth.isPatient" class="patient-account">
            <span><el-icon><User /></el-icon>{{ auth.displayName || '用户' }}</span>
            <button type="button" aria-label="退出登录" @click="logout">
              <el-icon><SwitchButton /></el-icon>
              退出
            </button>
          </div>
          <RouterLink v-else class="login-link" to="/login">登录</RouterLink>
        </nav>
      </div>
    </header>

    <main>
      <RouterView />
    </main>
  </div>
</template>

<style scoped>
.patient-shell {
  min-height: 100vh;
  background:
    radial-gradient(circle at 85% 8%, rgb(126 183 153 / 20%), transparent 26%),
    var(--color-mist);
}

.patient-header {
  position: sticky;
  z-index: 20;
  top: 0;
  border-bottom: 1px solid rgb(255 255 255 / 38%);
  background:
    linear-gradient(110deg, rgb(236 246 240 / 76%), rgb(218 236 226 / 68%));
  box-shadow:
    inset 0 -1px 0 rgb(31 92 66 / 8%),
    0 12px 38px rgb(23 60 45 / 7%);
  backdrop-filter: blur(24px) saturate(135%);
}

.header-inner {
  display: flex;
  min-height: 82px;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
}

.brand {
  display: inline-flex;
  min-height: 48px;
  align-items: center;
  gap: 11px;
}

.brand-mark {
  width: 42px;
  height: 42px;
  object-fit: contain;
}

.brand strong,
.brand small {
  display: block;
}

.brand strong {
  font-family: "Noto Serif SC", serif;
  font-size: 18px;
  letter-spacing: 0.08em;
}

.brand small {
  margin-top: 3px;
  color: var(--color-text-muted);
  font-size: 10px;
}

.patient-nav {
  display: flex;
  align-items: center;
  gap: 7px;
}

.patient-account {
  display: inline-flex;
  min-height: 44px;
  align-items: center;
  gap: 4px;
  margin-left: 7px;
  padding-left: 14px;
  border-left: 1px solid rgb(255 255 255 / 62%);
  color: var(--color-ink);
  font-size: 12px;
  font-weight: 700;
}

.patient-account > span,
.patient-account button {
  display: inline-flex;
  min-height: 36px;
  align-items: center;
  gap: 5px;
  padding: 0 9px;
  border: 1px solid rgb(255 255 255 / 64%);
  border-radius: 999px;
  box-shadow:
    inset 0 1px 0 rgb(255 255 255 / 76%),
    0 5px 16px rgb(23 60 45 / 6%);
  backdrop-filter: blur(10px);
}

.patient-account > span {
  background: rgb(228 242 234 / 68%);
}

.patient-account button {
  background: rgb(255 255 255 / 24%);
  color: var(--color-cinnabar);
  cursor: pointer;
  font-size: 11px;
  font-weight: 800;
}

.patient-account button:hover {
  border-color: rgb(194 75 55 / 22%);
  background: rgb(255 239 235 / 64%);
  transform: translateY(-1px);
}

.patient-nav .login-link {
  margin-left: 7px;
  background: var(--color-ink);
  color: white;
}

.patient-nav .login-link:hover {
  background: var(--color-ink-soft);
  color: white;
}

.patient-nav a {
  display: inline-flex;
  min-height: 44px;
  align-items: center;
  gap: 7px;
  padding: 0 14px;
  border: 1px solid rgb(255 255 255 / 52%);
  border-radius: 999px;
  background: rgb(255 255 255 / 18%);
  color: var(--color-text-muted);
  font-size: 13px;
  font-weight: 600;
  box-shadow:
    inset 0 1px 0 rgb(255 255 255 / 72%),
    inset 0 -1px 0 rgb(36 103 74 / 5%),
    0 5px 16px rgb(23 60 45 / 4%);
  backdrop-filter: blur(10px);
  transition:
    transform 180ms ease,
    border-color 180ms ease,
    background 180ms ease,
    box-shadow 180ms ease;
}

.patient-nav a:hover {
  border-color: rgb(255 255 255 / 82%);
  background: rgb(255 255 255 / 42%);
  color: var(--color-ink);
  box-shadow:
    inset 0 1px 0 white,
    0 8px 20px rgb(23 60 45 / 8%);
  transform: translateY(-1px);
}

.patient-nav a.router-link-exact-active {
  border-color: rgb(42 112 80 / 34%);
  background:
    radial-gradient(circle at 28% 12%, rgb(255 255 255 / 96%), transparent 35%),
    rgb(244 250 246 / 72%);
  color: var(--color-ink);
  box-shadow:
    inset 0 1px 0 white,
    inset 0 -1px 0 rgb(38 104 75 / 8%),
    0 8px 22px rgb(23 60 45 / 10%);
}

.patient-nav .el-icon,
.patient-account .el-icon {
  font-size: 16px;
}

@media (max-width: 850px) {
  .header-inner {
    display: block;
    padding-block: 12px;
  }

  .patient-nav {
    margin-top: 8px;
    overflow-x: auto;
  }

  .patient-nav a {
    flex: 0 0 auto;
  }

  .patient-account {
    flex: 0 0 auto;
  }
}

@media (max-width: 620px) {
  .brand small {
    display: none;
  }

}
</style>
