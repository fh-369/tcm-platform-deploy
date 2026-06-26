<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'

import { login, registerPatient } from '../../api/auth'
import { defaultRouteForRole } from '../../router/access'
import { useAuthStore } from '../../stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const submitting = ref(false)
const mode = ref(route.path === '/register' ? 'register' : 'login')
const isRegister = computed(() => mode.value === 'register')
const loginForm = reactive({ username: '', password: '' })
const registerForm = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  displayName: '',
  phone: '',
})

watch(() => route.path, (path) => {
  mode.value = path === '/register' ? 'register' : 'login'
})

function errorMessage(error, fallback) {
  return error.response?.data?.message || error.message || fallback
}

function switchMode(nextMode) {
  mode.value = nextMode
  router.replace(nextMode === 'register' ? '/register' : '/login')
}

async function submitLogin() {
  if (!loginForm.username.trim() || !loginForm.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }

  submitting.value = true
  try {
    const session = await login(loginForm)
    auth.setSession(session)
    ElMessage.success('欢迎回来')
    const requested = typeof route.query.redirect === 'string' ? route.query.redirect : ''
    await router.replace(requested.startsWith('/') && !requested.startsWith('//')
      ? requested
      : defaultRouteForRole(session.role))
  } catch (error) {
    ElMessage.error(errorMessage(error, '登录失败，请稍后重试'))
  } finally {
    submitting.value = false
  }
}

async function submitRegister() {
  if (registerForm.username.trim().length < 3) {
    ElMessage.warning('用户名至少需要 3 位')
    return
  }
  if (registerForm.password.length < 6) {
    ElMessage.warning('密码至少需要 6 位')
    return
  }
  if (registerForm.password !== registerForm.confirmPassword) {
    ElMessage.warning('两次输入的密码不一致')
    return
  }

  submitting.value = true
  try {
    const session = await registerPatient(registerForm)
    auth.setSession(session)
    ElMessage.success('账号创建成功')
    await router.replace('/')
  } catch (error) {
    ElMessage.error(errorMessage(error, '注册失败，请稍后重试'))
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <main class="auth-stage" :class="{ 'is-register': isRegister }">
    <section class="auth-form-panel login-panel">
      <div class="auth-form">
        <p class="eyebrow">WELCOME BACK</p>
        <h1>用户登录</h1>
        <p class="description">同一个入口，连接你的问诊记录、医生工作台与管理后台。</p>

        <el-form label-position="top" @submit.prevent="submitLogin">
          <el-form-item label="用户名">
            <el-input v-model="loginForm.username" autocomplete="username" placeholder="请输入用户名" />
          </el-form-item>
          <el-form-item label="密码">
            <el-input
              v-model="loginForm.password"
              autocomplete="current-password"
              placeholder="请输入密码"
              show-password
              type="password"
              @keyup.enter="submitLogin"
            />
          </el-form-item>
          <el-button class="submit-button" type="primary" :loading="submitting" @click="submitLogin">
            登录
          </el-button>
        </el-form>

        <p class="switch-copy">还没有账号？<button type="button" @click="switchMode('register')">注册账号</button></p>
      </div>
    </section>

    <section class="auth-visual" aria-label="知身问养草药插画">
      <img src="../../assets/brand/auth-herbal-hero.png" alt="草药、枝叶与中式养护器皿" />
      <div class="visual-copy">
        <p>知身，而后问养</p>
        <h2>{{ isRegister ? '从今天开始，认真记录身体的每一次回应。' : '让每一次问诊，都成为了解自己的开始。' }}</h2>
      </div>
    </section>

    <section class="auth-form-panel register-panel">
      <div class="auth-form register-form">
        <p class="eyebrow">CREATE ACCOUNT</p>
        <h1>注册账号</h1>
        <p class="description">创建你的个人账号，开始记录问诊与日常养护。</p>

        <el-form label-position="top" @submit.prevent="submitRegister">
          <div class="field-grid">
            <el-form-item label="用户名">
              <el-input v-model="registerForm.username" autocomplete="username" placeholder="至少 3 位" />
            </el-form-item>
            <el-form-item label="昵称">
              <el-input v-model="registerForm.displayName" autocomplete="name" placeholder="选填" />
            </el-form-item>
          </div>
          <el-form-item label="手机号">
            <el-input v-model="registerForm.phone" autocomplete="tel" placeholder="选填" />
          </el-form-item>
          <div class="field-grid">
            <el-form-item label="密码">
              <el-input v-model="registerForm.password" autocomplete="new-password" show-password type="password" placeholder="至少 6 位" />
            </el-form-item>
            <el-form-item label="确认密码">
              <el-input v-model="registerForm.confirmPassword" autocomplete="new-password" show-password type="password" placeholder="再次输入" />
            </el-form-item>
          </div>
          <el-button class="submit-button" type="primary" :loading="submitting" @click="submitRegister">
            注册并登录
          </el-button>
        </el-form>

        <p class="switch-copy">已经有账号？<button type="button" @click="switchMode('login')">返回登录</button></p>
        <RouterLink class="doctor-entry" to="/doctor/apply">我是医生，申请入驻</RouterLink>
      </div>
    </section>
  </main>
</template>

<style scoped>
.auth-stage {
  position: relative;
  display: grid;
  width: min(1240px, calc(100vw - 68px));
  min-width: 980px;
  min-height: 680px;
  grid-template-columns: 1fr 1.08fr;
  margin: 24px auto 0;
  overflow: hidden;
  border: 1px solid rgb(47 74 60 / 12%);
  border-radius: 28px;
  background: #fff;
  box-shadow: 0 30px 80px rgb(43 66 54 / 14%);
}

.auth-form-panel,
.auth-visual {
  position: absolute;
  top: 0;
  bottom: 0;
  width: 50%;
  transition: transform 680ms cubic-bezier(.77, 0, .18, 1), opacity 300ms ease;
}

.auth-form-panel {
  display: grid;
  place-items: center;
  padding: 58px 64px;
  background: #fff;
}

.login-panel {
  left: 0;
  z-index: 2;
}

.register-panel {
  right: 0;
  z-index: 1;
  opacity: 0;
  transform: translateX(30%);
}

.auth-visual {
  right: 0;
  z-index: 3;
  overflow: hidden;
  background: #e9eee7;
}

.auth-visual::after {
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, rgb(17 54 38 / 5%), rgb(17 54 38 / 58%));
  content: "";
}

.auth-visual img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.visual-copy {
  position: absolute;
  z-index: 1;
  right: 52px;
  bottom: 48px;
  left: 52px;
  color: #fff;
}

.visual-copy p {
  margin: 0 0 12px;
  font-size: 13px;
  font-weight: 700;
  letter-spacing: .2em;
}

.visual-copy h2 {
  max-width: 500px;
  margin: 0;
  font-family: "Noto Serif SC", serif;
  font-size: clamp(26px, 3vw, 38px);
  font-weight: 600;
  line-height: 1.45;
}

.is-register .auth-visual {
  transform: translateX(-100%);
}

.is-register .login-panel {
  z-index: 1;
  opacity: 0;
  transform: translateX(-30%);
}

.is-register .register-panel {
  z-index: 2;
  opacity: 1;
  transform: translateX(0);
}

.auth-form {
  width: min(420px, 100%);
}

.register-form {
  width: min(500px, 100%);
}

.eyebrow {
  margin: 0 0 12px;
  color: #a3533c;
  font-size: 11px;
  font-weight: 800;
  letter-spacing: .18em;
}

h1 {
  margin: 0;
  color: #183d2c;
  font-family: "Noto Serif SC", serif;
  font-size: 42px;
  letter-spacing: .06em;
}

.description {
  margin: 14px 0 34px;
  color: #738078;
  font-size: 14px;
  line-height: 1.8;
}

.field-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}

.submit-button {
  width: 100%;
  min-height: 48px;
  margin-top: 8px;
  border: 0;
  border-radius: 12px;
  background: #245b41;
  font-weight: 700;
}

.switch-copy {
  margin: 24px 0 0;
  color: #7a857f;
  font-size: 13px;
  text-align: center;
}

.switch-copy button {
  border: 0;
  background: transparent;
  color: #a3533c;
  cursor: pointer;
  font: inherit;
  font-weight: 800;
}

.doctor-entry {
  display: block;
  width: fit-content;
  margin: 14px auto 0;
  color: #476b58;
  font-size: 12px;
  font-weight: 700;
}

.doctor-entry:hover {
  color: var(--color-cinnabar);
}

:deep(.el-input__wrapper) {
  min-height: 44px;
  border-radius: 10px;
  box-shadow: 0 0 0 1px #d9e0dc inset;
}

:deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #397356 inset, 0 0 0 3px rgb(57 115 86 / 10%);
}

:deep(.el-input__inner:focus),
:deep(.el-input__wrapper:focus-visible) {
  outline: none;
}
</style>
