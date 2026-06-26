<script setup>
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'

import { uploadAdminContentImage } from '../../api/content'

const props = defineProps({
  modelValue: { type: String, default: '' },
  fallback: { type: String, required: true },
})
const emit = defineEmits(['update:modelValue'])

const input = ref(null)
const uploading = ref(false)
const previewSource = computed(() => props.modelValue || props.fallback)

function openPicker() {
  input.value?.click()
}

async function selectFile(event) {
  const [file] = event.target.files || []
  event.target.value = ''
  if (!file) return

  if (!['image/jpeg', 'image/png', 'image/webp'].includes(file.type)) {
    ElMessage.warning('封面仅支持 JPG、PNG 或 WebP 格式')
    return
  }
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.warning('封面图片不能超过 5 MB')
    return
  }

  uploading.value = true
  try {
    const result = await uploadAdminContentImage(file)
    emit('update:modelValue', result.url)
    ElMessage.success('封面上传成功')
  } catch (error) {
    ElMessage.error(error.response?.data?.message || error.message || '封面上传失败')
  } finally {
    uploading.value = false
  }
}
</script>

<template>
  <section class="cover-uploader">
    <div class="cover-preview">
      <img :src="previewSource" alt="内容封面预览">
      <span v-if="!modelValue">当前使用默认封面</span>
    </div>
    <div class="cover-actions">
      <div>
        <strong>内容封面</strong>
        <p>建议使用横向图片，支持 JPG、PNG、WebP，最大 5 MB。</p>
      </div>
      <div class="buttons">
        <el-button :loading="uploading" @click="openPicker">
          {{ modelValue ? '更换封面' : '上传封面' }}
        </el-button>
        <el-button v-if="modelValue" type="danger" plain @click="emit('update:modelValue', '')">
          移除引用
        </el-button>
      </div>
    </div>
    <input
      ref="input"
      class="file-input"
      accept="image/jpeg,image/png,image/webp"
      type="file"
      @change="selectFile"
    >
  </section>
</template>

<style scoped>
.cover-uploader {
  overflow: hidden;
  border: 1px solid rgb(47 95 72 / 12%);
  border-radius: 20px;
  background: #f7faf8;
}

.cover-preview {
  position: relative;
  height: 220px;
  overflow: hidden;
  background: #e7f0eb;
}

.cover-preview::after {
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, transparent 55%, rgb(17 54 39 / 30%));
  content: "";
  pointer-events: none;
}

.cover-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.cover-preview span {
  position: absolute;
  z-index: 1;
  right: 14px;
  bottom: 12px;
  padding: 6px 10px;
  border-radius: 999px;
  background: rgb(255 255 255 / 88%);
  color: var(--color-ink);
  font-size: 10px;
  font-weight: 800;
  backdrop-filter: blur(10px);
}

.cover-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 16px 18px;
}

.cover-actions strong {
  color: var(--color-ink);
  font-size: 13px;
}

.cover-actions p {
  margin: 5px 0 0;
  color: var(--color-text-muted);
  font-size: 10px;
}

.buttons {
  display: flex;
  flex: 0 0 auto;
}

.file-input {
  display: none;
}
</style>
