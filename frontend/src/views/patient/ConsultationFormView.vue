<script setup>
import { nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { CircleCheck } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'

import { createConsultation } from '../../api/consultation'
import { getDepartments } from '../../api/auth'

const router = useRouter()
const formRef = ref()
const submitting = ref(false)
const loadingDepartments = ref(false)
const departments = ref([])
const form = reactive({
  departmentId: null,
  patientName: '',
  age: null,
  gender: '',
  phone: '',
  symptoms: '',
  duration: '',
  allergyHistory: '',
  urgency: '普通',
  patientNote: '',
})

const namePattern = /^[\u4e00-\u9fa5A-Za-z·\s]{2,50}$/
const phonePattern = /^1[3-9]\d{9}$/
const rules = {
  departmentId: [{ required: true, message: '请选择问诊科室', trigger: 'change' }],
  patientName: [
    { required: true, message: '请输入患者姓名', trigger: 'blur' },
    {
      pattern: namePattern,
      message: '姓名应为 2-50 个中文、英文字母、空格或间隔号',
      trigger: 'blur',
    },
  ],
  age: [
    { required: true, message: '请输入患者年龄', trigger: 'change' },
    {
      validator: (_, value, callback) => {
        if (value >= 1 && value <= 150) {
          callback()
          return
        }
        callback(new Error('患者年龄必须在 1-150 岁之间'))
      },
      trigger: 'change',
    },
  ],
  gender: [{ required: true, message: '请选择性别', trigger: 'change' }],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: phonePattern, message: '请输入正确的 11 位手机号', trigger: 'blur' },
  ],
  symptoms: [
    { required: true, message: '请描述主要症状', trigger: 'blur' },
    { min: 2, max: 2000, message: '主要症状应为 2-2000 个字符', trigger: 'blur' },
  ],
  duration: [
    { required: true, message: '请输入症状持续时间', trigger: 'blur' },
    { max: 100, message: '症状持续时间不能超过 100 个字符', trigger: 'blur' },
  ],
  urgency: [{ required: true, message: '请选择紧急程度', trigger: 'change' }],
}

async function loadDepartments() {
  loadingDepartments.value = true
  try {
    departments.value = await getDepartments()
  } catch (error) {
    ElMessage.error(error.response?.data?.message || error.message || '科室信息加载失败')
  } finally {
    loadingDepartments.value = false
  }
}

function errorMessage(error) {
  return error.response?.data?.message || error.message || '问诊提交失败，请稍后重试'
}

async function scrollToFirstError() {
  await nextTick()
  const firstError = document.querySelector('.consultation-form .el-form-item.is-error')
  if (!firstError) {
    return
  }

  const headerHeight = document.querySelector('.patient-header')?.offsetHeight || 82
  const top = window.scrollY + firstError.getBoundingClientRect().top - headerHeight - 20
  window.scrollTo({ top: Math.max(0, top), behavior: 'smooth' })
}

async function submit() {
  try {
    await formRef.value.validate()
  } catch {
    await scrollToFirstError()
    return
  }

  submitting.value = true
  try {
    await createConsultation({
      ...form,
      patientName: form.patientName.trim(),
      phone: form.phone.trim(),
      symptoms: form.symptoms.trim(),
      duration: form.duration.trim(),
      allergyHistory: form.allergyHistory.trim() || null,
      patientNote: form.patientNote.trim() || null,
    })
    ElMessage.success('问诊单已提交')
    await router.push('/consultation/my')
  } catch (error) {
    ElMessage.error(errorMessage(error))
  } finally {
    submitting.value = false
  }
}

onMounted(loadDepartments)
</script>

<template>
  <section class="consultation-page page-container">
    <el-form
      ref="formRef"
      class="consultation-form"
      label-position="top"
      :model="form"
      :rules="rules"
      @submit.prevent="submit"
    >
      <div class="form-title">
        <el-icon><CircleCheck /></el-icon>
        新建问诊单
      </div>

      <section class="form-section">
        <div class="section-heading">
          <span>01</span>
          <div>
            <h2>患者信息</h2>
            <p>用于识别本次问诊对象。</p>
          </div>
        </div>

        <div class="field-grid">
          <el-form-item label="患者姓名（必填）" prop="patientName">
            <el-input v-model="form.patientName" maxlength="50" placeholder="请输入患者姓名" />
          </el-form-item>
          <el-form-item label="年龄（必填）" prop="age">
            <el-input-number v-model="form.age" :min="1" :max="150" controls-position="right" />
          </el-form-item>
          <el-form-item label="性别（必填）" prop="gender">
            <el-radio-group v-model="form.gender" class="gender-options">
              <el-radio-button label="男" value="男" />
              <el-radio-button label="女" value="女" />
            </el-radio-group>
          </el-form-item>
          <el-form-item label="手机号（必填）" prop="phone">
            <el-input v-model="form.phone" maxlength="11" placeholder="请输入 11 位手机号" />
          </el-form-item>
        </div>
      </section>

      <section class="form-section">
        <div class="section-heading">
          <span>02</span>
          <div>
            <h2>问诊方向</h2>
            <p>选择更接近当前问题的科室，帮助平台进行后续分诊。</p>
          </div>
        </div>

        <el-form-item
          class="department-field"
          label="问诊科室（必填）"
          prop="departmentId"
        >
          <el-radio-group
            v-model="form.departmentId"
            v-loading="loadingDepartments"
            class="department-options"
          >
            <el-radio
              v-for="department in departments"
              :key="department.id"
              :value="department.id"
              border
            >
              <strong>{{ department.name }}</strong>
              <small>{{ department.description }}</small>
            </el-radio>
          </el-radio-group>
        </el-form-item>
      </section>

      <section class="form-section">
        <div class="section-heading">
          <span>03</span>
          <div>
            <h2>症状描述</h2>
            <p>重点说明不适部位、程度、持续时间和变化。</p>
          </div>
        </div>

        <el-form-item class="symptom-field" label="主要症状（必填）" prop="symptoms">
          <el-input
            v-model="form.symptoms"
            :rows="5"
            maxlength="2000"
            placeholder="例如：近三天反复胃痛，饭后更明显，伴轻微恶心……"
            show-word-limit
            type="textarea"
          />
        </el-form-item>
        <div class="field-grid">
          <el-form-item label="持续时间（必填）" prop="duration">
            <el-input v-model="form.duration" maxlength="100" placeholder="例如：约三天" />
          </el-form-item>
          <el-form-item label="紧急程度（必填）" prop="urgency">
            <el-radio-group v-model="form.urgency">
              <el-radio-button label="普通" value="普通" />
              <el-radio-button label="紧急" value="紧急" />
              <el-radio-button label="非常紧急" value="非常紧急" />
            </el-radio-group>
          </el-form-item>
        </div>
      </section>

      <section class="form-section">
        <div class="section-heading">
          <span>04</span>
          <div>
            <h2>补充信息</h2>
            <p>过敏史和备注有助于医生了解整体情况。</p>
          </div>
        </div>

        <div class="field-grid">
          <el-form-item label="过敏史">
            <el-input v-model="form.allergyHistory" :rows="3" placeholder="选填" type="textarea" />
          </el-form-item>
          <el-form-item label="其他备注">
            <el-input v-model="form.patientNote" :rows="3" placeholder="选填" type="textarea" />
          </el-form-item>
        </div>
      </section>

      <el-button
        class="floating-submit"
        type="primary"
        :icon="CircleCheck"
        :loading="submitting"
        @click="submit"
      >
        提交问诊单
      </el-button>
    </el-form>
  </section>
</template>

<style scoped>
.consultation-page {
  padding-top: 22px;
  padding-bottom: 96px;
}

.section-heading p {
  color: var(--color-text-muted);
  font-size: 14px;
  line-height: 1.75;
}

.consultation-form {
  position: relative;
  margin-top: 18px;
}

.form-title {
  position: absolute;
  z-index: 2;
  top: -17px;
  left: 24px;
  display: inline-flex;
  height: 34px;
  align-items: center;
  gap: 7px;
  padding: 0 15px;
  border: 1px solid rgb(255 255 255 / 42%);
  border-radius: 999px;
  background: var(--color-ink);
  box-shadow: 0 10px 24px rgb(17 66 47 / 18%);
  color: white;
  font-size: 12px;
  font-weight: 800;
}

.form-section {
  position: relative;
  margin-top: 16px;
  padding: 24px 24px 24px 244px;
  border: 1px solid var(--color-border);
  border-radius: 22px;
  background:
    linear-gradient(135deg, rgb(255 255 255 / 96%), rgb(249 252 250 / 96%));
  box-shadow: 0 16px 42px rgb(25 74 54 / 8%);
  overflow: hidden;
}

.section-heading {
  position: absolute;
  top: 0;
  bottom: 0;
  left: 0;
  display: flex;
  width: 220px;
  align-items: center;
  gap: 16px;
  padding: 30px 24px;
  border-right: 1px solid rgb(37 112 78 / 11%);
  background:
    radial-gradient(circle at 18% 18%, rgb(255 255 255 / 92%), transparent 38%),
    linear-gradient(150deg, #f3faf6, #e6f3eb);
}

.section-heading > span {
  display: grid;
  flex: 0 0 auto;
  width: 36px;
  height: 36px;
  place-items: center;
  border: 1px solid rgb(37 112 78 / 16%);
  border-radius: 50%;
  background: white;
  color: var(--color-cinnabar);
  font-size: 10px;
  font-weight: 800;
  box-shadow: 0 8px 20px rgb(25 74 54 / 8%);
}

.section-heading h2 {
  margin: 0;
  color: var(--color-ink);
  font-family: "Noto Serif SC", "STSong", serif;
  font-size: 20px;
  letter-spacing: 0.03em;
}

.section-heading p {
  margin: 9px 0 0;
}

.field-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.form-section :deep(.el-form-item) {
  margin-bottom: 0;
  padding: 15px 16px 16px;
  border: 1px solid rgb(63 125 94 / 12%);
  border-radius: 16px;
  background:
    linear-gradient(145deg, rgb(246 250 247 / 88%), rgb(238 246 241 / 62%));
  box-shadow:
    inset 0 1px 0 rgb(255 255 255 / 86%),
    0 7px 20px rgb(33 93 67 / 4%);
}

.form-section :deep(.el-form-item.is-error) {
  border-color: rgb(201 81 61 / 34%);
  background: linear-gradient(145deg, #fff9f7, #fff2ee);
}

.form-section :deep(.el-form-item__error) {
  position: static;
  margin-top: 8px;
  color: var(--color-cinnabar);
  font-size: 12px;
  font-weight: 700;
  line-height: 1.45;
}

.form-section > :deep(.el-form-item) {
  margin-bottom: 16px;
}

.form-section :deep(.el-form-item__label) {
  height: auto;
  margin-bottom: 9px;
  padding: 0;
  color: var(--color-ink);
  font-size: 14px;
  font-weight: 750;
  letter-spacing: 0.015em;
  line-height: 1.45;
}

.form-section :deep(.el-input),
.form-section :deep(.el-input-number),
.form-section :deep(.el-select) {
  width: 100%;
}

.form-section :deep(.el-input__wrapper),
.form-section :deep(.el-select__wrapper) {
  min-height: 44px;
  padding-inline: 14px;
  border: 1px solid rgb(65 126 96 / 14%);
  border-radius: 12px;
  background: rgb(255 255 255 / 80%);
  box-shadow: inset 0 1px 3px rgb(30 80 57 / 4%);
  transition:
    border-color 180ms ease,
    background 180ms ease,
    box-shadow 180ms ease;
}

.form-section :deep(.el-input__wrapper.is-focus),
.form-section :deep(.el-select__wrapper.is-focused) {
  border-color: rgb(79 138 108 / 58%);
  background: white;
  box-shadow:
    0 0 0 4px rgb(79 138 108 / 10%),
    inset 0 1px 2px rgb(30 80 57 / 3%);
}

.form-section :deep(.el-input__inner),
.form-section :deep(.el-textarea__inner) {
  color: var(--color-text);
  font-size: 14px;
}

.form-section :deep(.el-input__inner::placeholder),
.form-section :deep(.el-textarea__inner::placeholder) {
  color: #91a69b;
}

.form-section :deep(.el-textarea__inner) {
  padding: 13px 14px;
  border: 1px solid rgb(65 126 96 / 14%);
  border-radius: 12px;
  background: rgb(255 255 255 / 80%);
  box-shadow: inset 0 1px 3px rgb(30 80 57 / 4%);
  line-height: 1.75;
  transition:
    border-color 180ms ease,
    background 180ms ease,
    box-shadow 180ms ease;
}

.form-section :deep(.el-textarea__inner:focus) {
  border-color: rgb(79 138 108 / 58%);
  background: white;
  box-shadow:
    0 0 0 4px rgb(79 138 108 / 10%),
    inset 0 1px 2px rgb(30 80 57 / 3%);
}

.symptom-field {
  background:
    radial-gradient(circle at 90% 5%, rgb(255 255 255 / 94%), transparent 32%),
    linear-gradient(145deg, #f3f8f4, #eaf4ee) !important;
}

.department-field {
  background:
    radial-gradient(circle at 92% 8%, rgb(255 255 255 / 94%), transparent 32%),
    linear-gradient(145deg, #f3f8f4, #eaf4ee) !important;
}

.department-options {
  display: grid;
  width: 100%;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.department-options :deep(.el-radio) {
  width: 100%;
  height: auto;
  min-height: 78px;
  align-items: flex-start;
  margin: 0;
  padding: 15px 16px;
  border-color: rgb(65 126 96 / 15%);
  border-radius: 14px;
  background: rgb(255 255 255 / 78%);
  white-space: normal;
}

.department-options :deep(.el-radio.is-bordered.is-checked) {
  border-color: var(--color-jade);
  background: #edf7f1;
  box-shadow: 0 9px 22px rgb(46 112 79 / 10%);
}

.department-options :deep(.el-radio__label) {
  display: grid;
  gap: 6px;
  padding-left: 10px;
  color: var(--color-ink);
  line-height: 1.5;
}

.department-options strong {
  font-size: 15px;
}

.department-options small {
  color: var(--color-text-muted);
  font-size: 12px;
  font-weight: 500;
}

.symptom-field :deep(.el-textarea__inner) {
  min-height: 142px !important;
}

.form-section :deep(.el-radio-group) {
  width: 100%;
  --el-color-primary: var(--color-jade);
}

.form-section :deep(.el-radio-button__inner) {
  display: inline-flex;
  min-height: 44px;
  align-items: center;
  justify-content: center;
  padding: 0 18px;
  border-color: rgb(65 126 96 / 16%);
  background: rgb(255 255 255 / 76%);
  color: var(--color-text-muted);
  font-size: 14px;
  font-weight: 700;
  box-shadow: none;
}

.form-section :deep(.el-radio-button:first-child .el-radio-button__inner) {
  border-radius: 12px 0 0 12px;
}

.form-section :deep(.el-radio-button:last-child .el-radio-button__inner) {
  border-radius: 0 12px 12px 0;
}

.form-section :deep(.el-radio-button.is-active .el-radio-button__inner) {
  border-color: var(--color-jade);
  background: var(--color-jade);
  box-shadow:
    inset 0 1px 0 rgb(255 255 255 / 30%),
    0 7px 18px rgb(46 112 79 / 16%);
  color: white;
}

.form-section :deep(.el-input-number__decrease),
.form-section :deep(.el-input-number__increase) {
  border-color: rgb(65 126 96 / 14%);
  background: rgb(237 246 241 / 84%);
  color: var(--color-ink-soft);
}

.gender-options {
  width: 100%;
}

.gender-options :deep(.el-radio-button) {
  flex: 1;
}

.gender-options :deep(.el-radio-button__inner) {
  width: 100%;
}

.floating-submit {
  position: fixed;
  z-index: 20;
  right: 28px;
  bottom: 28px;
  min-width: 168px;
  height: 50px;
  border: 1px solid rgb(255 255 255 / 32%);
  border-radius: 999px;
  background: var(--color-ink);
  box-shadow: 0 16px 34px rgb(17 66 47 / 24%);
  font-size: 15px;
  font-weight: 800;
}

.floating-submit:hover,
.floating-submit:focus-visible {
  background: #236e50;
  transform: translateY(-2px);
}

@media (max-width: 850px) {
  .form-section {
    padding: 92px 22px 22px;
  }

  .section-heading {
    right: 0;
    bottom: auto;
    width: auto;
    padding: 22px 26px;
    border-right: 0;
    border-bottom: 1px solid rgb(37 112 78 / 11%);
  }

  .floating-submit {
    right: 20px;
  }
}

@media (max-width: 620px) {
  .form-section {
    padding: 92px 14px 18px;
  }

  .section-heading {
    padding: 20px 18px;
  }

  .field-grid {
    grid-template-columns: 1fr;
  }

  .department-options {
    grid-template-columns: 1fr;
  }

  .floating-submit {
    right: 18px;
    bottom: 18px;
    min-width: 150px;
  }
}
</style>
