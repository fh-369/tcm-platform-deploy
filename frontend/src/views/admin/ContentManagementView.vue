<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

import {
  createAdminContent,
  deleteAdminContent,
  getAdminContent,
  updateAdminContent,
  updateAdminContentPublication,
} from '../../api/content'
import ContentEditorDrawer from '../../components/admin/ContentEditorDrawer.vue'
import {
  CONTENT_CONFIGS,
  normalizeContentPayload,
} from '../../features/admin/contentManagement'
import { getApiErrorMessage as errorMessage } from '../../features/feedback'

const props = defineProps({
  resource: {
    type: String,
    required: true,
    validator: (value) => ['knowledge', 'recipe'].includes(value),
  },
})

const loading = ref(false)
const saving = ref(false)
const updatingId = ref(null)
const editorVisible = ref(false)
const records = ref([])
const total = ref(0)
const activeRecord = ref({})
const filters = reactive({
  current: 1,
  size: 10,
  keyword: '',
  published: '',
  category: '',
  season: '',
  constitution: '',
})

const config = computed(() => CONTENT_CONFIGS[props.resource])
const isKnowledge = computed(() => props.resource === 'knowledge')
const publishedCount = computed(() => records.value.filter((item) => item.published).length)

function formatTime(value) {
  if (!value) return '暂无'
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(value))
}

function openCreate() {
  activeRecord.value = {}
  editorVisible.value = true
}

function openEdit(row) {
  activeRecord.value = { ...row }
  editorVisible.value = true
}

async function load() {
  loading.value = true
  try {
    const params = Object.fromEntries(
      Object.entries(filters).filter(([, value]) => value !== ''),
    )
    const page = await getAdminContent(props.resource, params)
    records.value = page.records || []
    total.value = Number(page.total || 0)
  } catch (error) {
    ElMessage.error(errorMessage(error, `${config.value.title}加载失败`))
  } finally {
    loading.value = false
  }
}

function search() {
  filters.current = 1
  load()
}

function resetFilters() {
  Object.assign(filters, {
    current: 1,
    size: 10,
    keyword: '',
    published: '',
    category: '',
    season: '',
    constitution: '',
  })
  load()
}

async function save({ form, published }) {
  saving.value = true
  try {
    const payload = normalizeContentPayload(props.resource, {
      ...form,
      published,
    })
    if (form.id) {
      await updateAdminContent(props.resource, form.id, payload)
    } else {
      await createAdminContent(props.resource, payload)
    }
    editorVisible.value = false
    ElMessage.success(published ? `${config.value.singular}已保存并发布` : '草稿已保存')
    await load()
  } catch (error) {
    ElMessage.error(errorMessage(error, `${config.value.singular}保存失败`))
  } finally {
    saving.value = false
  }
}

async function changePublication(row) {
  const nextState = !row.published
  if (nextState) {
    try {
      await ElMessageBox.confirm(
        `发布“${row[config.value.nameKey]}”后，用户将立即可以查看。`,
        `发布${config.value.singular}`,
        {
          confirmButtonText: '确认发布',
          cancelButtonText: '取消',
          type: 'success',
        },
      )
    } catch {
      return
    }
  }

  updatingId.value = row.id
  try {
    await updateAdminContentPublication(props.resource, row.id, nextState)
    row.published = nextState
    ElMessage.success(nextState ? '内容已发布' : '内容已转为草稿')
  } catch (error) {
    ElMessage.error(errorMessage(error, '发布状态更新失败'))
  } finally {
    updatingId.value = null
  }
}

async function remove(row) {
  try {
    await ElMessageBox.confirm(
      `删除“${row[config.value.nameKey]}”后无法恢复，已上传的历史封面文件不会被自动删除。`,
      `删除${config.value.singular}`,
      {
        confirmButtonText: '确认删除',
        cancelButtonText: '取消',
        type: 'warning',
      },
    )
  } catch {
    return
  }

  updatingId.value = row.id
  try {
    await deleteAdminContent(props.resource, row.id)
    ElMessage.success('内容已删除')
    await load()
  } catch (error) {
    ElMessage.error(errorMessage(error, '删除失败'))
  } finally {
    updatingId.value = null
  }
}

watch(
  () => props.resource,
  () => {
    resetFilters()
    editorVisible.value = false
  },
)

onMounted(load)
</script>

<template>
  <section class="content-page">
    <header class="content-heading">
      <h1>{{ config.title }}</h1>
      <div class="heading-actions">
        <div class="content-summary">
          <strong>{{ total }}</strong>
          <span>共 {{ total }} 条 · 当前页 {{ publishedCount }} 条已发布</span>
        </div>
        <el-button type="primary" @click="openCreate">
          + 新建{{ config.singular }}
        </el-button>
      </div>
    </header>

    <section class="content-toolbar">
      <el-input
        v-model="filters.keyword"
        class="search-input"
        clearable
        :placeholder="isKnowledge ? '搜索文章标题或摘要' : '搜索药膳名称、摘要或适合人群'"
        @clear="search"
        @keyup.enter="search"
      >
        <template #prefix><span class="search-mark">⌕</span></template>
      </el-input>
      <el-select
        v-if="isKnowledge"
        v-model="filters.category"
        allow-create
        clearable
        filterable
        placeholder="全部文章分类"
        popper-class="jade-select-popper"
        @change="search"
      >
        <el-option
          v-for="item in ['四季养护', '饮食调养', '睡眠起居', '运动养护', '情志调适', '养生常识']"
          :key="item"
          :label="item"
          :value="item"
        />
      </el-select>
      <el-select
        v-else
        v-model="filters.season"
        clearable
        placeholder="全部季节"
        popper-class="jade-select-popper"
        @change="search"
      >
        <el-option
          v-for="item in config.seasonOptions"
          :key="item"
          :label="item"
          :value="item"
        />
      </el-select>
      <el-select
        v-if="!isKnowledge"
        v-model="filters.constitution"
        clearable
        filterable
        placeholder="全部体质"
        popper-class="jade-select-popper"
        @change="search"
      >
        <el-option
          v-for="item in config.constitutionOptions"
          :key="item"
          :label="item"
          :value="item"
        />
      </el-select>
      <el-select
        v-model="filters.published"
        clearable
        placeholder="全部发布状态"
        popper-class="jade-select-popper"
        @change="search"
      >
        <el-option label="已发布" :value="true" />
        <el-option label="草稿" :value="false" />
      </el-select>
      <el-button class="filter-button" type="primary" @click="search">搜索</el-button>
      <el-button class="reset-button" @click="resetFilters">重置</el-button>
    </section>

    <section class="content-table-card">
      <el-table v-loading="loading" :data="records" row-key="id">
        <el-table-column label="内容" min-width="330">
          <template #default="{ row }">
            <div class="content-identity">
              <img
                :src="row[config.imageKey] || (isKnowledge
                  ? '/knowledge/knowledge-hero.png'
                  : '/recipes/recipe-hero.png')"
                alt=""
              >
              <div>
                <strong>{{ row[config.nameKey] }}</strong>
                <p>{{ row.summary || '暂未填写摘要' }}</p>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column :label="config.filterLabel" min-width="125">
          <template #default="{ row }">
            <span class="category-chip">{{ row[config.filterKey] || '未分类' }}</span>
          </template>
        </el-table-column>
        <el-table-column v-if="!isKnowledge" label="适用体质" min-width="125">
          <template #default="{ row }">{{ row.constitution || '通用' }}</template>
        </el-table-column>
        <el-table-column label="发布状态" min-width="115">
          <template #default="{ row }">
            <span :class="['publication-status', { draft: !row.published }]">
              <i></i>
              {{ row.published ? '已发布' : '草稿' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="浏览量" min-width="90">
          <template #default="{ row }">{{ row.viewCount || 0 }}</template>
        </el-table-column>
        <el-table-column label="最近更新" min-width="175">
          <template #default="{ row }">{{ formatTime(row.updatedAt || row.createdAt) }}</template>
        </el-table-column>
        <el-table-column align="right" fixed="right" label="操作" width="220">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button
              link
              :loading="updatingId === row.id"
              :type="row.published ? 'warning' : 'success'"
              @click="changePublication(row)"
            >
              {{ row.published ? '取消发布' : '发布' }}
            </el-button>
            <el-button
              link
              type="danger"
              :loading="updatingId === row.id"
              @click="remove(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>

        <template #empty>
          <div class="empty-state">
            <span>{{ isKnowledge ? '文' : '膳' }}</span>
            <strong>暂无符合条件的{{ config.singular }}</strong>
            <p>可以调整筛选条件，或创建第一条内容。</p>
          </div>
        </template>
      </el-table>

      <el-pagination
        v-if="total > filters.size"
        background
        layout="prev, pager, next"
        :current-page="filters.current"
        :page-size="filters.size"
        :total="total"
        @current-change="(page) => { filters.current = page; load() }"
      />
    </section>

    <ContentEditorDrawer
      :resource="resource"
      :saving="saving"
      :value="activeRecord"
      :visible="editorVisible"
      @close="editorVisible = false"
      @save="save"
    />
  </section>
</template>

<style scoped>
.content-page {
  max-width: 1480px;
  margin: 0 auto;
}

.content-heading {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 24px;
}

.content-heading h1 {
  margin: 0;
  color: var(--color-ink);
  font-family: "Noto Serif SC", "STSong", serif;
  font-size: 32px;
  letter-spacing: -.04em;
}

.heading-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.content-summary {
  display: flex;
  min-width: 190px;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  border: 1px solid rgb(47 95 72 / 12%);
  border-radius: 16px;
  background: rgb(255 255 255 / 76%);
}

.content-summary strong {
  color: var(--color-ink);
  font-family: "Noto Serif SC", "STSong", serif;
  font-size: 23px;
}

.content-summary span {
  color: var(--color-text-muted);
  font-size: 9px;
}

.content-toolbar {
  display: flex;
  align-items: center;
  gap: 9px;
  margin-top: 22px;
  padding: 14px;
  flex-wrap: wrap;
  border: 1px solid rgb(47 95 72 / 12%);
  border-radius: 20px;
  background:
    radial-gradient(circle at 0 50%, rgb(225 240 231 / 72%), transparent 28%),
    rgb(255 255 255 / 92%);
  box-shadow: 0 12px 32px rgb(21 56 42 / 6%);
}

.content-toolbar .el-input {
  width: min(380px, 100%);
}

.content-toolbar .el-select {
  width: 150px;
}

.content-toolbar :deep(.el-input__wrapper),
.content-toolbar :deep(.el-select__wrapper) {
  min-height: 44px;
  border-radius: 13px;
  background: #f7faf8;
  box-shadow: inset 0 0 0 1px rgb(43 92 68 / 12%);
  transition: box-shadow .18s ease, background .18s ease;
}

.content-toolbar :deep(.el-input__wrapper:hover),
.content-toolbar :deep(.el-select__wrapper:hover) {
  background: white;
  box-shadow: inset 0 0 0 1px rgb(39 101 72 / 28%);
}

.content-toolbar :deep(.el-input__wrapper.is-focus),
.content-toolbar :deep(.el-select__wrapper.is-focused) {
  background: white;
  box-shadow:
    inset 0 0 0 1px #2d7657,
    0 0 0 3px rgb(45 118 87 / 10%);
}

.search-mark {
  color: #47715d;
  font-size: 20px;
  line-height: 1;
}

.filter-button,
.reset-button {
  min-height: 44px;
  border-radius: 13px;
  font-weight: 800;
}

.filter-button {
  min-width: 78px;
  border: 0;
  background: linear-gradient(135deg, #174f3a, #2b7656);
  box-shadow: 0 7px 16px rgb(18 72 50 / 16%);
}

.reset-button {
  min-width: 68px;
  border-color: transparent;
  background: #eef5f1;
  color: #50685d;
}

.reset-button:hover {
  border-color: rgb(45 118 87 / 12%);
  background: #e3f0e8;
  color: #174f3a;
}

.content-table-card {
  margin-top: 14px;
  padding: 10px 14px 16px;
  border: 1px solid rgb(47 95 72 / 10%);
  border-radius: 22px;
  background: rgb(255 255 255 / 92%);
  box-shadow: 0 14px 34px rgb(21 56 42 / 5%);
}

.content-identity {
  display: flex;
  align-items: center;
  gap: 13px;
}

.content-identity img {
  width: 72px;
  height: 52px;
  flex: 0 0 auto;
  border-radius: 12px;
  background: #e7f0eb;
  object-fit: cover;
}

.content-identity strong {
  display: block;
  max-width: 340px;
  overflow: hidden;
  color: var(--color-ink);
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.content-identity p {
  display: -webkit-box;
  margin: 6px 0 0;
  overflow: hidden;
  color: var(--color-text-muted);
  font-size: 10px;
  line-height: 1.5;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.category-chip,
.publication-status {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  padding: 6px 10px;
  border-radius: 999px;
  background: #edf4f0;
  color: var(--color-ink);
  font-size: 10px;
  font-weight: 800;
}

.publication-status {
  background: #e5f2ea;
  color: #286546;
}

.publication-status i {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #3b8a60;
}

.publication-status.draft {
  background: #eef0ef;
  color: #6c7972;
}

.publication-status.draft i {
  background: #98a39e;
}

.empty-state {
  display: grid;
  min-height: 230px;
  place-content: center;
  text-align: center;
}

.empty-state > span {
  display: grid;
  width: 48px;
  height: 48px;
  margin: 0 auto 14px;
  border-radius: 16px;
  background: #e7f0eb;
  color: var(--color-ink);
  font-family: "Noto Serif SC", "STSong", serif;
  font-size: 18px;
  font-weight: 900;
  place-items: center;
}

.empty-state strong {
  color: var(--color-ink);
  font-size: 15px;
}

.empty-state p {
  margin: 7px 0 0;
  color: var(--color-text-muted);
  font-size: 10px;
}

.el-pagination {
  justify-content: center;
  margin-top: 18px;
}

:global(.jade-select-popper.el-popper) {
  overflow: hidden;
  border: 1px solid rgb(48 98 74 / 13%);
  border-radius: 15px;
  box-shadow: 0 16px 38px rgb(24 63 45 / 14%);
}

:global(.jade-select-popper .el-select-dropdown__item) {
  margin: 4px 7px;
  border-radius: 9px;
  color: #4f675c;
  font-weight: 650;
}

:global(.jade-select-popper .el-select-dropdown__item.is-hovering) {
  background: #edf5f0;
  color: #174f3a;
}

:global(.jade-select-popper .el-select-dropdown__item.is-selected) {
  background: #dceee4;
  color: #174f3a;
  font-weight: 850;
}

@media (max-width: 900px) {
  .content-heading,
  .content-toolbar {
    align-items: stretch;
    flex-direction: column;
  }

  .heading-actions {
    justify-content: space-between;
  }

  .content-toolbar .el-input,
  .content-toolbar .el-select {
    width: 100%;
  }
}
</style>
