<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'

import { applyDoctor, getDepartments } from '../../api/auth'

const router = useRouter()
const loadingDepartments = ref(false)
const submitting = ref(false)
const submitted = ref(false)
const departments = ref([])
const form = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  displayName: '',
  departmentId: null,
  phone: '',
  qualification: '',
  profile: '',
})

function errorMessage(error, fallback) {
  return error.response?.data?.message || error.message || fallback
}

async function loadDepartments() {
  loadingDepartments.value = true
  try {
    departments.value = await getDepartments()
  } catch (error) {
    ElMessage.error(errorMessage(error, '科室信息加载失败'))
  } finally {
    loadingDepartments.value = false
  }
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
  if (form.password !== form.confirmPassword) {
    ElMessage.warning('两次输入的密码不一致')
    return
  }
  if (!form.displayName.trim()) {
    ElMessage.warning('请填写医生姓名')
    return
  }
  if (!form.departmentId) {
    ElMessage.warning('请选择申请科室')
    return
  }
  if (!/^1\d{10}$/.test(form.phone.trim())) {
    ElMessage.warning('请输入 11 位手机号')
    return
  }
  if (!form.qualification.trim()) {
    ElMessage.warning('请填写资质或执业信息')
    return
  }

  submitting.value = true
  try {
    await applyDoctor({
      username: form.username.trim(),
      password: form.password,
      displayName: form.displayName.trim(),
      departmentId: form.departmentId,
      phone: form.phone.trim(),
      qualification: form.qualification.trim(),
      profile: form.profile.trim(),
    })
    submitted.value = true
  } catch (error) {
    ElMessage.error(errorMessage(error, '申请提交失败，请稍后重试'))
  } finally {
    submitting.value = false
  }
}

onMounted(loadDepartments)
</script>

<template>
  <main class="doctor-application">
    <section class="admission-intro">
      <div>
        <p class="eyebrow">DOCTOR ADMISSION</p>
        <h1>让专业判断<br>进入真实问诊</h1>
        <p class="intro-copy">
          医生账号采用审核准入。资料核验通过后，即可进入专属工作台。
        </p>
      </div>

      <div class="admission-flow">
        <ol class="admission-steps">
          <li>
            <span>01</span>
            <div><strong>提交资料</strong><small>填写账号、科室与执业信息</small></div>
          </li>
          <li>
            <span>02</span>
            <div><strong>管理员审核</strong><small>核验资料与平台准入资格</small></div>
          </li>
          <li>
            <span>03</span>
            <div><strong>进入工作台</strong><small>审核通过后使用申请账号登录</small></div>
          </li>
        </ol>

        <div class="review-note">
          <span>审核机制</span>
          <strong>实名资料 · 科室匹配 · 账号准入</strong>
        </div>
      </div>
    </section>

    <section v-if="!submitted" class="application-card">
      <header class="application-heading">
        <div>
          <h2>创建医生申请</h2>
        </div>
        <span>提交后由管理员审核</span>
      </header>

      <el-form label-position="top" @submit.prevent="submit">
        <div class="form-section">
          <div class="section-label">
            <span>01</span>
            <div><strong>账号信息</strong><small>创建用于登录医生工作台的账号</small></div>
          </div>
          <div class="field-grid">
            <el-form-item label="登录用户名">
              <el-input v-model="form.username" autocomplete="username" placeholder="至少 3 位" />
            </el-form-item>
            <el-form-item label="医生姓名">
              <el-input v-model="form.displayName" autocomplete="name" placeholder="用于后台展示" />
            </el-form-item>
            <el-form-item label="登录密码">
              <el-input v-model="form.password" autocomplete="new-password" show-password type="password" placeholder="至少 6 位" />
            </el-form-item>
            <el-form-item label="确认密码">
              <el-input v-model="form.confirmPassword" autocomplete="new-password" show-password type="password" placeholder="再次输入密码" />
            </el-form-item>
          </div>
        </div>

        <div class="form-section">
          <div class="section-label">
            <span>02</span>
            <div><strong>执业信息</strong><small>完善科室归属和专业资料</small></div>
          </div>
          <div class="field-grid">
            <el-form-item label="申请科室">
              <el-select
                v-model="form.departmentId"
                :loading="loadingDepartments"
                placeholder="请选择所属科室"
                popper-class="admission-select-popper"
              >
                <el-option
                  v-for="department in departments"
                  :key="department.id"
                  :label="department.name"
                  :value="department.id"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="联系电话">
              <el-input v-model="form.phone" autocomplete="tel" maxlength="11" placeholder="请输入 11 位手机号" />
            </el-form-item>
          </div>
          <div class="professional-grid">
            <el-form-item label="资质或执业信息">
              <el-input
                v-model="form.qualification"
                :rows="2"
                maxlength="500"
                placeholder="填写执业资质与年限"
                resize="none"
                type="textarea"
              />
            </el-form-item>
            <el-form-item label="个人简介">
              <el-input
                v-model="form.profile"
                :rows="2"
                maxlength="1000"
                placeholder="简要说明擅长方向，选填"
                resize="none"
                type="textarea"
              />
            </el-form-item>
          </div>
        </div>

        <div class="form-actions">
          <div class="action-note">
            <span>提交即表示资料真实有效</span>
            <small>审核结果将影响医生账号的启用状态</small>
          </div>
          <div class="action-buttons">
            <button type="button" @click="router.push('/register')">
              <span>←</span> 返回普通注册
            </button>
            <el-button type="primary" :loading="submitting" @click="submit">
              提交入驻申请
            </el-button>
          </div>
        </div>
      </el-form>
    </section>

    <section v-else class="application-card success-card">
      <span class="success-mark">✓</span>
      <p>申请已进入审核队列</p>
      <h2>资料提交成功</h2>
      <div>
        管理员审核通过后，你就可以使用刚才创建的用户名和密码登录医生工作台。
      </div>
      <el-button type="primary" @click="router.push('/login')">返回登录</el-button>
    </section>
  </main>
</template>

<style scoped>
.doctor-application {
  display: grid;
  width: min(1240px, calc(100vw - 68px));
  height: calc(100vh - 116px);
  min-width: 980px;
  min-height: 620px;
  max-height: 790px;
  grid-template-columns: .72fr 1.28fr;
  margin: 12px auto 0;
  overflow: hidden;
  border: 1px solid rgb(47 74 60 / 12%);
  border-radius: 28px;
  background: white;
  box-shadow: 0 30px 80px rgb(43 66 54 / 14%);
}

.admission-intro {
  position: relative;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: clamp(30px, 6vh, 52px);
  padding: 38px 44px 34px;
  overflow: hidden;
  background:
    radial-gradient(circle at 88% 12%, rgb(255 255 255 / 18%), transparent 24%),
    linear-gradient(148deg, #123f2c, #28684c);
  color: white;
}

.admission-intro::after {
  position: absolute;
  right: -90px;
  bottom: -120px;
  width: 320px;
  height: 320px;
  border: 1px solid rgb(255 255 255 / 13%);
  border-radius: 50%;
  content: "";
  box-shadow: 0 0 0 42px rgb(255 255 255 / 4%), 0 0 0 86px rgb(255 255 255 / 3%);
}

.eyebrow {
  margin: 0 0 14px;
  color: #f0b09f;
  font-size: 11px;
  font-weight: 900;
  letter-spacing: .18em;
}

.admission-intro h1 {
  max-width: 340px;
  margin: 0;
  font-family: "Noto Serif SC", "STSong", serif;
  font-size: clamp(32px, 2.5vw, 40px);
  font-weight: 600;
  line-height: 1.34;
}

.intro-copy {
  max-width: 330px;
  margin: 18px 0 0;
  color: rgb(255 255 255 / 72%);
  font-size: 13px;
  line-height: 1.75;
}

.admission-flow {
  position: relative;
  z-index: 1;
}

.admission-steps {
  position: relative;
  display: grid;
  gap: 12px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.admission-steps::before {
  position: absolute;
  top: 29px;
  bottom: 29px;
  left: 19px;
  width: 1px;
  background: linear-gradient(rgb(255 255 255 / 28%), rgb(255 255 255 / 6%));
  content: "";
}

.admission-steps li {
  position: relative;
  display: flex;
  align-items: center;
  gap: 13px;
  padding: 10px 12px;
  border: 1px solid rgb(255 255 255 / 9%);
  border-radius: 15px;
  background: rgb(255 255 255 / 5%);
  backdrop-filter: blur(8px);
}

.admission-steps li > span {
  display: grid;
  width: 36px;
  height: 36px;
  flex: 0 0 auto;
  border: 1px solid rgb(255 255 255 / 28%);
  background: #18533b;
  border-radius: 50%;
  color: #f0b09f;
  font-size: 10px;
  font-weight: 900;
  place-items: center;
}

.admission-steps strong,
.admission-steps small {
  display: block;
}

.admission-steps strong {
  font-size: 13px;
}

.admission-steps small {
  margin-top: 3px;
  color: rgb(255 255 255 / 60%);
  font-size: 10px;
}

.review-note {
  display: grid;
  gap: 4px;
  margin-top: 14px;
  padding: 12px 15px;
  border-left: 2px solid #efa38f;
  border-radius: 0 13px 13px 0;
  background: rgb(5 39 26 / 20%);
}

.review-note span {
  color: #f0b09f;
  font-size: 9px;
  font-weight: 900;
  letter-spacing: .12em;
}

.review-note strong {
  color: rgb(255 255 255 / 78%);
  font-size: 10px;
  font-weight: 650;
}

.application-card {
  display: flex;
  flex-direction: column;
  padding: 28px 38px 24px;
  overflow: hidden;
}

.application-heading {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24px;
}

.success-card > p {
  margin: 0 0 8px;
  color: var(--color-cinnabar);
  font-size: 11px;
  font-weight: 900;
  letter-spacing: .12em;
}

.application-card h2 {
  margin: 0;
  color: var(--color-ink);
  font-family: "Noto Serif SC", "STSong", serif;
  font-size: 30px;
}

.application-card header > span {
  padding: 7px 11px;
  border: 1px solid rgb(47 95 72 / 10%);
  border-radius: 999px;
  background: #edf5f0;
  color: var(--color-text-muted);
  font-size: 10px;
  white-space: nowrap;
}

.form-section {
  margin-top: 14px;
  padding: 14px 16px 3px;
  border: 1px solid rgb(47 95 72 / 10%);
  border-radius: 18px;
  background:
    radial-gradient(circle at 96% 0, rgb(207 231 216 / 42%), transparent 24%),
    #f8fbf9;
}

.section-label {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 11px;
  color: var(--color-ink);
}

.section-label span {
  display: grid;
  width: 30px;
  height: 30px;
  border-radius: 10px;
  background: #e2eee7;
  color: var(--color-cinnabar);
  font-size: 10px;
  font-weight: 900;
  place-items: center;
}

.section-label strong,
.section-label small {
  display: block;
}

.section-label strong {
  font-size: 14px;
}

.section-label small {
  margin-top: 2px;
  color: var(--color-text-muted);
  font-size: 9px;
}

.field-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0 14px;
}

.professional-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0 14px;
}

.el-select {
  width: 100%;
}

.application-card > .el-form {
  display: flex;
  min-height: 0;
  flex: 1;
  flex-direction: column;
}

.form-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  margin-top: 14px;
  padding: 12px 14px;
  border: 1px solid rgb(47 95 72 / 9%);
  border-radius: 16px;
  background: #f6f9f7;
}

.action-note {
  display: grid;
  gap: 2px;
}

.action-note span {
  color: var(--color-ink);
  font-size: 10px;
  font-weight: 800;
}

.action-note small {
  color: var(--color-text-muted);
  font-size: 9px;
}

.action-buttons {
  display: flex;
  align-items: center;
  gap: 9px;
}

.action-buttons > button {
  min-height: 42px;
  padding: 0 14px;
  border: 0;
  border-radius: 12px;
  background: #e8f1ec;
  color: #3c6250;
  cursor: pointer;
  font-size: 11px;
  font-weight: 750;
  transition: background .18s ease, color .18s ease;
}

.action-buttons > button:hover {
  background: #dcebe2;
  color: #174f3a;
}

.form-actions .el-button {
  min-width: 148px;
  min-height: 42px;
  border: 0;
  border-radius: 12px;
  background: linear-gradient(135deg, #174f3a, #2b7656);
  color: white;
  box-shadow: 0 8px 18px rgb(18 72 50 / 18%);
  font-weight: 800;
}

.form-actions .el-button:hover,
.form-actions .el-button:focus {
  background: linear-gradient(135deg, #103f2e, #24694c);
  color: white;
}

.form-actions .el-button.is-disabled {
  background: #8eaa9b;
  color: rgb(255 255 255 / 82%);
}

:deep(.el-input__wrapper),
:deep(.el-select__wrapper),
:deep(.el-textarea__inner) {
  border-radius: 12px;
  background: rgb(255 255 255 / 90%);
  box-shadow: inset 0 0 0 1px rgb(43 92 68 / 13%);
  transition: box-shadow .18s ease, background .18s ease;
}

:deep(.el-input__wrapper),
:deep(.el-select__wrapper) {
  min-height: 40px;
}

:deep(.el-input__wrapper:hover),
:deep(.el-select__wrapper:hover),
:deep(.el-textarea__inner:hover) {
  background: white;
  box-shadow: inset 0 0 0 1px rgb(39 101 72 / 28%);
}

:deep(.el-input__wrapper.is-focus),
:deep(.el-select__wrapper.is-focused),
:deep(.el-textarea__inner:focus) {
  background: white;
  box-shadow:
    inset 0 0 0 1px #2d7657,
    0 0 0 3px rgb(45 118 87 / 9%);
}

:deep(.el-form-item) {
  margin-bottom: 11px;
}

:deep(.el-form-item__label) {
  height: auto;
  padding-bottom: 5px;
  color: #486256;
  font-size: 10px;
  font-weight: 800;
}

.success-card {
  display: grid;
  align-content: center;
  justify-items: start;
}

.success-mark {
  display: grid;
  width: 64px;
  height: 64px;
  margin-bottom: 28px;
  border-radius: 20px;
  background: #e2efe7;
  color: #236043;
  font-size: 28px;
  font-weight: 900;
  place-items: center;
}

.success-card div {
  max-width: 480px;
  margin: 22px 0 30px;
  color: var(--color-text-muted);
  line-height: 1.9;
}

:global(.admission-select-popper.el-popper) {
  overflow: hidden;
  border: 1px solid rgb(48 98 74 / 13%);
  border-radius: 15px;
  box-shadow: 0 16px 38px rgb(24 63 45 / 14%);
}

:global(.admission-select-popper .el-select-dropdown__item) {
  margin: 4px 7px;
  border-radius: 9px;
  color: #4f675c;
  font-weight: 650;
}

:global(.admission-select-popper .el-select-dropdown__item.is-hovering) {
  background: #edf5f0;
  color: #174f3a;
}

:global(.admission-select-popper .el-select-dropdown__item.is-selected) {
  background: #dceee4;
  color: #174f3a;
  font-weight: 850;
}

@media (max-height: 760px) {
  .doctor-application {
    min-height: 590px;
  }

  .admission-intro {
    padding-top: 28px;
    padding-bottom: 26px;
  }

  .application-card {
    padding-top: 20px;
    padding-bottom: 18px;
  }

  .form-section {
    margin-top: 10px;
    padding-top: 11px;
  }

  .section-label {
    margin-bottom: 7px;
  }

  :deep(.el-form-item) {
    margin-bottom: 8px;
  }

  .form-actions {
    margin-top: 9px;
    padding-top: 9px;
    padding-bottom: 9px;
  }
}
</style>
