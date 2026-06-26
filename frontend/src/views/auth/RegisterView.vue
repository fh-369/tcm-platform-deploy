<script setup>
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'

import { registerPatient } from '../../api/auth'
import { useAuthStore } from '../../stores/auth'

const auth = useAuthStore()
const router = useRouter()
const submitting = ref(false)
const form = reactive({
  username: '',
  password: '',
  displayName: '',
  phone: '',
})

function errorMessage(error) {
  return error.response?.data?.message || error.message || '注册失败，请稍后重试'
}

async function submit() {
  if (form.username.trim().length < 3) {
    ElMessage.warning('用户名至少需要 3 位')
    return
  }
  if (form.password.length < 6) {
    ElMessage.warning('密码至少需要 6 位')
    return
  }

  submitting.value = true
  try {
    const session = await registerPatient(form)
    auth.setSession(session)
    ElMessage.success('注册成功，已为你登录')
    await router.replace('/')
  } catch (error) {
    ElMessage.error(errorMessage(error))
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="auth-card">
    <p class="auth-eyebrow">建立患者账号</p>
    <h2>患者注册</h2>
    <p class="auth-description">注册后会自动登录，昵称和手机号可暂时不填。</p>

    <el-form label-position="top" @submit.prevent="submit">
      <div class="field-grid">
        <el-form-item label="用户名">
          <el-input v-model="form.username" autocomplete="username" placeholder="至少 3 位" />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="form.displayName" autocomplete="name" placeholder="选填" />
        </el-form-item>
      </div>
      <el-form-item label="手机号">
        <el-input v-model="form.phone" autocomplete="tel" placeholder="选填" />
      </el-form-item>
      <el-form-item label="密码">
        <el-input
          v-model="form.password"
          autocomplete="new-password"
          placeholder="至少 6 位"
          show-password
          type="password"
          @keyup.enter="submit"
        />
      </el-form-item>
      <el-button class="auth-submit" type="primary" :loading="submitting" @click="submit">
        注册并登录
      </el-button>
    </el-form>

    <div class="auth-links">
      <span>已经有患者账号？</span>
      <RouterLink to="/login/patient">返回登录</RouterLink>
    </div>
  </div>
</template>

<style scoped>
.auth-card {
  width: min(390px, 100%);
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
  margin: 0 0 26px;
  color: var(--color-text-muted);
  font-size: 13px;
  line-height: 1.7;
}

.field-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
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

@media (max-width: 520px) {
  .field-grid {
    grid-template-columns: 1fr;
    gap: 0;
  }
}
</style>
