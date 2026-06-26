<script setup>
import { computed, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'

import { loginAdmin, loginPatient } from '../../api/auth'
import { defaultRouteForRole } from '../../router/access'
import { useAuthStore } from '../../stores/auth'

const props = defineProps({
  mode: {
    type: String,
    required: true,
  },
})

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()
const submitting = ref(false)
const form = reactive({ username: '', password: '' })
const isAdminLogin = computed(() => props.mode === 'admin')
const title = computed(() => (isAdminLogin.value ? '后台登录' : '患者登录'))

function errorMessage(error) {
  return error.response?.data?.message || error.message || '登录失败，请稍后重试'
}

function redirectAfterLogin(role) {
  const requested = typeof route.query.redirect === 'string' ? route.query.redirect : ''
  return requested.startsWith('/') && !requested.startsWith('//')
    ? requested
    : defaultRouteForRole(role)
}

async function submit() {
  if (!form.username.trim() || !form.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }

  submitting.value = true
  try {
    const session = isAdminLogin.value
      ? await loginAdmin(form)
      : await loginPatient(form)
    auth.setSession(session)
    ElMessage.success('登录成功')
    await router.replace(redirectAfterLogin(session.role))
  } catch (error) {
    ElMessage.error(errorMessage(error))
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="auth-card">
    <p class="auth-eyebrow">{{ isAdminLogin ? '医生与管理员入口' : '患者入口' }}</p>
    <h2>{{ title }}</h2>
    <p class="auth-description">
      {{ isAdminLogin ? '登录后进入问诊工作台。' : '登录后可以提交问诊并查看个人记录。' }}
    </p>

    <el-form label-position="top" @submit.prevent="submit">
      <el-form-item label="用户名">
        <el-input v-model="form.username" autocomplete="username" placeholder="请输入用户名" />
      </el-form-item>
      <el-form-item label="密码">
        <el-input
          v-model="form.password"
          autocomplete="current-password"
          placeholder="请输入密码"
          show-password
          type="password"
          @keyup.enter="submit"
        />
      </el-form-item>
      <el-button class="auth-submit" type="primary" :loading="submitting" @click="submit">
        登录
      </el-button>
    </el-form>

    <div class="auth-links">
      <template v-if="isAdminLogin">
        <span>患者请使用患者入口</span>
        <RouterLink to="/login/patient">患者登录</RouterLink>
      </template>
      <template v-else>
        <span>还没有账号？</span>
        <RouterLink to="/register">注册患者账号</RouterLink>
      </template>
    </div>
  </div>
</template>

<style scoped>
.auth-card {
  width: min(360px, 100%);
}

.auth-eyebrow {
  margin: 0;
  color: var(--color-cinnabar);
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.16em;
}

h2 {
  margin: 12px 0;
  color: var(--color-ink);
  font-size: 32px;
}

.auth-description {
  margin: 0 0 30px;
  color: var(--color-text-muted);
  font-size: 13px;
  line-height: 1.7;
}

.auth-submit {
  width: 100%;
  min-height: 44px;
  margin-top: 6px;
}

.auth-links {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  margin-top: 22px;
  color: var(--color-text-muted);
  font-size: 12px;
}

.auth-links a {
  color: var(--color-cinnabar);
  font-weight: 800;
}
</style>
