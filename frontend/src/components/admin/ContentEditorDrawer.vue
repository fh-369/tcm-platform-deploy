<script setup>
import { computed, reactive, watch } from 'vue'
import { ElMessage } from 'element-plus'
import DOMPurify from 'dompurify'
import MarkdownIt from 'markdown-it'

import {
  CONTENT_CONFIGS,
  listToEditorText,
  validateContentForm,
} from '../../features/admin/contentManagement'
import ContentCoverUploader from './ContentCoverUploader.vue'

const props = defineProps({
  visible: { type: Boolean, default: false },
  resource: { type: String, required: true },
  value: { type: Object, default: () => ({}) },
  saving: { type: Boolean, default: false },
})
const emit = defineEmits(['close', 'save'])

const markdown = new MarkdownIt({
  breaks: true,
  html: false,
  linkify: true,
  typographer: true,
})
const form = reactive({})
const config = computed(() => CONTENT_CONFIGS[props.resource])
const editing = computed(() => Boolean(props.value?.id))
const renderedContent = computed(() =>
  DOMPurify.sanitize(markdown.render(form.content || '')),
)

watch(
  () => [props.visible, props.resource, props.value],
  () => {
    if (!props.visible) return
    const source = {
      ...config.value.defaultForm,
      ...props.value,
    }
    if (props.resource === 'recipe') {
      source.ingredients = listToEditorText(source.ingredients)
      source.steps = listToEditorText(source.steps)
    }
    Object.keys(form).forEach((key) => delete form[key])
    Object.assign(form, source)
  },
  { deep: true, immediate: true },
)

function submit(published) {
  const issue = validateContentForm(props.resource, form, published)
  if (issue) {
    ElMessage.warning(issue.message)
    return
  }
  emit('save', { form: { ...form }, published })
}
</script>

<template>
  <el-drawer
    :model-value="visible"
    class="content-editor"
    direction="rtl"
    size="min(1080px, 92vw)"
    :show-close="false"
    @close="emit('close')"
  >
    <template #header>
      <div class="drawer-heading">
        <div>
          <small>{{ editing ? '编辑现有内容' : '创建新的内容' }}</small>
          <h2>{{ editing ? `编辑${config.singular}` : `新建${config.singular}` }}</h2>
        </div>
        <button type="button" aria-label="关闭编辑器" @click="emit('close')">×</button>
      </div>
    </template>

    <div class="editor-layout">
      <main class="editor-main">
        <section class="form-card">
          <header>
            <span>01</span>
            <div>
              <strong>基础信息</strong>
              <p>填写用户在列表和详情页首先看到的内容。</p>
            </div>
          </header>

          <el-form label-position="top">
            <template v-if="resource === 'knowledge'">
              <el-form-item label="文章标题（必填）">
                <el-input v-model="form.title" maxlength="200" show-word-limit />
              </el-form-item>
              <div class="field-grid">
                <el-form-item label="文章分类">
                  <el-select
                    v-model="form.category"
                    allow-create
                    clearable
                    filterable
                    placeholder="选择或输入分类，留空可自动判断"
                  >
                    <el-option
                      v-for="item in ['四季养护', '饮食调养', '睡眠起居', '运动养护', '情志调适', '养生常识']"
                      :key="item"
                      :label="item"
                      :value="item"
                    />
                  </el-select>
                </el-form-item>
                <el-form-item label="当前状态">
                  <div class="state-preview">
                    <i :class="{ published: form.published }"></i>
                    {{ form.published ? '已发布' : '草稿' }}
                  </div>
                </el-form-item>
              </div>
              <el-form-item label="文章摘要">
                <el-input
                  v-model="form.summary"
                  maxlength="500"
                  :rows="3"
                  resize="none"
                  show-word-limit
                  type="textarea"
                />
              </el-form-item>
            </template>

            <template v-else>
              <el-form-item label="药膳名称（必填）">
                <el-input v-model="form.name" maxlength="100" show-word-limit />
              </el-form-item>
              <div class="field-grid">
                <el-form-item label="适用季节">
                  <el-select v-model="form.season" clearable placeholder="请选择季节">
                    <el-option
                      v-for="item in config.seasonOptions"
                      :key="item"
                      :label="item"
                      :value="item"
                    />
                  </el-select>
                </el-form-item>
                <el-form-item label="适用体质">
                  <el-select v-model="form.constitution" clearable filterable placeholder="请选择体质">
                    <el-option
                      v-for="item in config.constitutionOptions"
                      :key="item"
                      :label="item"
                      :value="item"
                    />
                  </el-select>
                </el-form-item>
              </div>
              <el-form-item label="适合人群">
                <el-input v-model="form.suitableFor" maxlength="200" show-word-limit />
              </el-form-item>
              <el-form-item label="药膳摘要">
                <el-input
                  v-model="form.summary"
                  maxlength="500"
                  :rows="3"
                  resize="none"
                  show-word-limit
                  type="textarea"
                />
              </el-form-item>
            </template>
          </el-form>
        </section>

        <section class="form-card">
          <header>
            <span>02</span>
            <div>
              <strong>{{ resource === 'knowledge' ? '正文内容' : '制作内容' }}</strong>
              <p>{{ resource === 'knowledge' ? '使用 Markdown 组织标题、引用和列表。' : '每行填写一项，保存时会转换为结构化列表。' }}</p>
            </div>
          </header>

          <template v-if="resource === 'knowledge'">
            <div class="markdown-editor">
              <section>
                <div class="pane-title">Markdown 编辑</div>
                <el-input
                  v-model="form.content"
                  class="content-textarea"
                  :rows="20"
                  placeholder="## 小标题&#10;&#10;填写正文内容……"
                  resize="none"
                  type="textarea"
                />
              </section>
              <section class="markdown-preview">
                <div class="pane-title">阅读预览</div>
                <article v-if="form.content" v-html="renderedContent"></article>
                <div v-else class="preview-empty">开始输入后，这里会显示文章排版效果。</div>
              </section>
            </div>
            <el-form-item class="tips-field" label="日常提示">
              <el-input
                v-model="form.tips"
                :rows="3"
                placeholder="填写文章末尾需要特别提醒用户的内容"
                resize="none"
                type="textarea"
              />
            </el-form-item>
          </template>

          <template v-else>
            <div class="recipe-editor-grid">
              <el-form-item label="食材清单">
                <el-input
                  v-model="form.ingredients"
                  :rows="12"
                  placeholder="每行一种食材，例如：&#10;山药 100 克&#10;粳米 80 克"
                  resize="none"
                  type="textarea"
                />
              </el-form-item>
              <el-form-item label="制作步骤">
                <el-input
                  v-model="form.steps"
                  :rows="12"
                  placeholder="每行一个步骤，例如：&#10;食材洗净并切块&#10;小火慢煮至软烂"
                  resize="none"
                  type="textarea"
                />
              </el-form-item>
            </div>
          </template>
        </section>
      </main>

      <aside class="editor-aside">
        <ContentCoverUploader
          v-model="form[config.imageKey]"
          :fallback="resource === 'knowledge'
            ? '/knowledge/knowledge-hero.png'
            : '/recipes/recipe-hero.png'"
        />
        <section class="publish-note">
          <strong>发布说明</strong>
          <p>草稿仅管理员可见；发布后，普通用户可以立即在对应页面查看。</p>
        </section>
      </aside>
    </div>

    <template #footer>
      <div class="drawer-footer">
        <el-button @click="emit('close')">取消</el-button>
        <div>
          <el-button :loading="saving" @click="submit(false)">保存草稿</el-button>
          <el-button type="primary" :loading="saving" @click="submit(true)">
            保存并发布
          </el-button>
        </div>
      </div>
    </template>
  </el-drawer>
</template>

<style scoped>
.drawer-heading {
  display: flex;
  width: 100%;
  align-items: center;
  justify-content: space-between;
}

.drawer-heading small {
  color: var(--color-text-muted);
  font-size: 10px;
}

.drawer-heading h2 {
  margin: 5px 0 0;
  color: var(--color-ink);
  font-family: "Noto Serif SC", "STSong", serif;
  font-size: 25px;
}

.drawer-heading button {
  width: 38px;
  height: 38px;
  border: 1px solid rgb(47 95 72 / 12%);
  border-radius: 50%;
  background: white;
  color: var(--color-ink);
  cursor: pointer;
  font-size: 24px;
}

.editor-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 310px;
  gap: 18px;
}

.editor-main {
  display: grid;
  gap: 16px;
}

.form-card {
  padding: 20px;
  border: 1px solid rgb(47 95 72 / 10%);
  border-radius: 22px;
  background: white;
}

.form-card > header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
}

.form-card > header > span {
  display: grid;
  width: 34px;
  height: 34px;
  flex: 0 0 auto;
  border-radius: 11px;
  background: #e7f1eb;
  color: var(--color-ink);
  font-size: 10px;
  font-weight: 900;
  place-items: center;
}

.form-card header strong {
  color: var(--color-ink);
  font-family: "Noto Serif SC", "STSong", serif;
  font-size: 16px;
}

.form-card header p {
  margin: 4px 0 0;
  color: var(--color-text-muted);
  font-size: 10px;
}

.field-grid,
.recipe-editor-grid,
.markdown-editor {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}

.state-preview {
  display: flex;
  height: 32px;
  align-items: center;
  gap: 8px;
  color: var(--color-text-muted);
  font-size: 11px;
}

.state-preview i {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #9ba9a2;
}

.state-preview i.published {
  background: #3c8a61;
  box-shadow: 0 0 0 4px rgb(60 138 97 / 12%);
}

.markdown-editor {
  gap: 0;
  overflow: hidden;
  border: 1px solid rgb(47 95 72 / 12%);
  border-radius: 16px;
}

.markdown-editor > section + section {
  border-left: 1px solid rgb(47 95 72 / 12%);
}

.pane-title {
  padding: 10px 13px;
  border-bottom: 1px solid rgb(47 95 72 / 10%);
  background: #f5f8f6;
  color: var(--color-ink);
  font-size: 10px;
  font-weight: 900;
}

.content-textarea :deep(.el-textarea__inner) {
  min-height: 430px !important;
  border: 0;
  border-radius: 0;
  box-shadow: none;
  line-height: 1.8;
}

.markdown-preview article,
.preview-empty {
  height: 430px;
  padding: 18px;
  overflow-y: auto;
}

.preview-empty {
  display: grid;
  color: var(--color-text-muted);
  font-size: 11px;
  place-items: center;
}

.markdown-preview article {
  color: #355648;
  font-size: 13px;
  line-height: 1.9;
}

.markdown-preview article :deep(h1),
.markdown-preview article :deep(h2),
.markdown-preview article :deep(h3) {
  color: var(--color-ink);
  font-family: "Noto Serif SC", "STSong", serif;
}

.markdown-preview article :deep(h2) {
  margin-top: 26px;
  padding-bottom: 7px;
  border-bottom: 1px solid rgb(47 95 72 / 12%);
}

.markdown-preview article :deep(blockquote) {
  margin: 18px 0;
  padding: 12px 14px;
  border-left: 3px solid var(--color-cinnabar);
  background: #f7f3ed;
}

.tips-field {
  margin-top: 18px;
}

.editor-aside {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.publish-note {
  padding: 18px;
  border: 1px solid rgb(47 95 72 / 10%);
  border-radius: 18px;
  background: linear-gradient(145deg, #f8fbf9, #edf5f0);
}

.publish-note strong {
  color: var(--color-ink);
  font-size: 13px;
}

.publish-note p {
  margin: 8px 0 0;
  color: var(--color-text-muted);
  font-size: 10px;
  line-height: 1.8;
}

.drawer-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

:deep(.el-form-item__label) {
  color: #486256;
  font-size: 11px;
  font-weight: 700;
}

:deep(.el-select) {
  width: 100%;
}

@media (max-width: 900px) {
  .editor-layout {
    grid-template-columns: 1fr;
  }
}
</style>
