<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'

import { getPublishedKnowledge, getPublishedKnowledgeCategories } from '../../api/content'

const loading = ref(false)
const articles = ref([])
const categories = ref([])
const category = ref('')
const keyword = ref('')
const current = ref(1)
const pageSize = 6
const total = ref(0)

const heroStyle = computed(() => ({
  backgroundImage: "linear-gradient(90deg, rgba(17, 54, 40, .9), rgba(17, 54, 40, .35)), url('/knowledge/knowledge-hero.png')",
}))

async function loadArticles() {
  loading.value = true
  try {
    const page = await getPublishedKnowledge({
      current: current.value,
      size: pageSize,
      category: category.value || undefined,
      keyword: keyword.value.trim() || undefined,
    })
    articles.value = page.records || []
    total.value = Number(page.total || 0)
  } catch (error) {
    ElMessage.error(error.message || '养生知识加载失败')
  } finally {
    loading.value = false
  }
}

async function selectCategory(value) {
  category.value = value
  current.value = 1
  await loadArticles()
}

async function searchArticles() {
  current.value = 1
  await loadArticles()
}

async function clearSearch() {
  keyword.value = ''
  await searchArticles()
}

async function changePage(page) {
  current.value = page
  await loadArticles()
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

onMounted(async () => {
  try {
    categories.value = await getPublishedKnowledgeCategories()
  } catch (error) {
    ElMessage.error(error.message || '文章分类加载失败')
  }
  await loadArticles()
})
</script>

<template>
  <section class="content-page page-container">
    <header class="content-hero" :style="heroStyle">
      <div>
        <p>顺时而养 · 知行于日常</p>
        <h1>养生知识</h1>
        <span>从饮食、起居到运动与情志，找到适合日常坚持的健康方法。</span>
      </div>
    </header>

    <div class="filter-row">
      <div class="category-filter">
        <button :class="{ active: !category }" type="button" @click="selectCategory('')">全部</button>
        <button
          v-for="item in categories"
          :key="item"
          :class="{ active: category === item }"
          type="button"
          @click="selectCategory(item)"
        >{{ item }}</button>
      </div>
      <el-input
        v-model="keyword"
        class="article-search"
        clearable
        placeholder="搜索文章关键词"
        @clear="clearSearch"
        @keyup.enter="searchArticles"
      >
        <template #append>
          <el-button :icon="Search" aria-label="搜索文章" @click="searchArticles" />
        </template>
      </el-input>
    </div>

    <div v-loading="loading" class="article-grid">
      <RouterLink
        v-for="article in articles"
        :key="article.id"
        :to="`/knowledge/${article.id}`"
        class="article-card"
      >
        <div
          class="article-cover"
          :style="{ backgroundImage: `url('${article.coverImageUrl || '/knowledge/knowledge-hero.png'}')` }"
        >
          <span>{{ article.category || '养生常识' }}</span>
        </div>
        <div class="article-copy">
          <h2>{{ article.title }}</h2>
          <p>{{ article.summary || '了解适合融入日常的健康方法。' }}</p>
          <small>阅读全文 <i></i> {{ article.viewCount || 0 }} 次浏览</small>
        </div>
      </RouterLink>
      <el-empty v-if="!loading && articles.length === 0" description="暂无相关文章" />
    </div>

    <el-pagination
      v-if="total > pageSize"
      background
      layout="prev, pager, next"
      :current-page="current"
      :page-size="pageSize"
      :total="total"
      @current-change="changePage"
    />
  </section>
</template>

<style scoped>
.content-page { padding-block: 34px 42px; }
.content-hero { min-height: 300px; display: flex; align-items: end; padding: 44px; border-radius: 26px; background-position: center bottom; background-size: cover; box-shadow: 0 22px 60px rgb(23 60 45 / 16%); color: white; }
.content-hero div { max-width: 630px; }
.content-hero p { margin: 0 0 10px; color: #f0c5b8; font-size: 12px; font-weight: 800; letter-spacing: .18em; }
.content-hero h1 { margin: 0; font-family: "Noto Serif SC", "STSong", serif; font-size: clamp(3rem, 6vw, 5rem); letter-spacing: -.05em; }
.content-hero span { display: block; margin-top: 15px; font-size: 15px; line-height: 1.8; opacity: .88; }
.filter-row { display: flex; align-items: center; gap: 18px; margin: 28px 0 18px; }
.category-filter { display: flex; flex: 1; gap: 9px; overflow-x: auto; }
.category-filter button { flex: 0 0 auto; padding: 9px 16px; border: 1px solid rgb(79 138 108 / 22%); border-radius: 99px; background: rgb(255 255 255 / 70%); color: var(--color-text-muted); cursor: pointer; backdrop-filter: blur(12px); }
.category-filter button:hover { border-color: var(--color-jade); color: var(--color-ink); }
.category-filter button.active { border-color: var(--color-ink); background: var(--color-ink); color: white; box-shadow: 0 8px 20px rgb(23 60 45 / 18%); }
.article-search { flex: 0 0 250px; }
.article-search :deep(.el-input__wrapper) { border-radius: 999px 0 0 999px; background: rgb(255 255 255 / 78%); box-shadow: 0 0 0 1px rgb(79 138 108 / 18%) inset; }
.article-search :deep(.el-input-group__append) { border-radius: 0 999px 999px 0; background: var(--color-ink); color: white; box-shadow: none; }
.article-grid { display: grid; min-height: 260px; grid-template-columns: repeat(3, 1fr); gap: 18px; }
.article-card { overflow: hidden; border: 1px solid rgb(79 138 108 / 16%); border-radius: 20px; background: white; box-shadow: 0 14px 36px rgb(23 60 45 / 8%); }
.article-card:hover { border-color: rgb(79 138 108 / 48%); transform: translateY(-5px); box-shadow: 0 22px 42px rgb(23 60 45 / 14%); }
.article-cover { min-height: 190px; padding: 17px; background-position: center; background-size: cover; }
.article-cover span { display: inline-flex; padding: 6px 10px; border: 1px solid rgb(255 255 255 / 48%); border-radius: 999px; background: rgb(23 60 45 / 72%); color: white; font-size: 11px; font-weight: 800; backdrop-filter: blur(8px); }
.article-copy { padding: 22px 23px 24px; }
.article-card h2 { margin: 0; color: var(--color-ink); font-family: "Noto Serif SC", "STSong", serif; font-size: 21px; line-height: 1.55; }
.article-card p { min-height: 72px; margin: 11px 0 0; color: var(--color-text-muted); font-size: 13px; line-height: 1.85; }
.article-card small { display: flex; align-items: center; gap: 9px; margin-top: 20px; color: var(--color-jade); font-size: 11px; font-weight: 800; }
.article-card small i { width: 3px; height: 3px; border-radius: 50%; background: var(--color-cinnabar); }
.el-pagination { justify-content: center; margin-top: 30px; }
@media (max-width: 900px) { .filter-row { align-items: stretch; flex-direction: column; } .article-search { flex-basis: auto; width: min(100%, 360px); } .article-grid { grid-template-columns: repeat(2, 1fr); } }
@media (max-width: 620px) { .content-hero { min-height: 250px; padding: 28px; } .article-grid { grid-template-columns: 1fr; } }
</style>
