<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'

import { getPublishedRecipes } from '../../api/content'
import { filterRecipes, paginateRecipes, sortSeasons } from '../../features/recipe/display'

const recipes = ref([])
const loading = ref(false)
const season = ref('')
const constitution = ref('')
const keyword = ref('')
const current = ref(1)
const pageSize = 6
const seasons = computed(() => sortSeasons([...new Set(recipes.value.map((item) => item.season).filter(Boolean))]))
const constitutions = computed(() => [...new Set(recipes.value.map((item) => item.constitution).filter(Boolean))])
const filteredRecipes = computed(() => filterRecipes(recipes.value, {
  season: season.value,
  constitution: constitution.value,
  keyword: keyword.value,
}))
const visibleRecipes = computed(() => paginateRecipes(filteredRecipes.value, current.value, pageSize))
const heroStyle = computed(() => ({
  backgroundImage: "linear-gradient(90deg, rgba(17, 54, 40, .9), rgba(17, 54, 40, .38)), url('/recipes/recipe-hero.png')",
}))

function resetPage() {
  current.value = 1
}

onMounted(async () => {
  loading.value = true
  try { recipes.value = await getPublishedRecipes() }
  catch (error) { ElMessage.error(error.message || '药膳加载失败') }
  finally { loading.value = false }
})
</script>

<template>
  <section class="recipe-page page-container">
    <header class="recipe-hero" :style="heroStyle">
      <div>
        <p>顺季而食 · 温和调养</p>
        <h1>药膳推荐</h1>
        <span>从常见食材开始，找到适合日常餐桌的温和搭配。</span>
      </div>
    </header>
    <div class="filters">
      <div class="filter-selects">
        <el-select v-model="season" clearable placeholder="全部季节" popper-class="recipe-filter-popper" @change="resetPage" @clear="resetPage"><el-option v-for="item in seasons" :key="item" :label="item" :value="item" /></el-select>
        <el-select v-model="constitution" clearable placeholder="全部体质" popper-class="recipe-filter-popper" @change="resetPage" @clear="resetPage"><el-option v-for="item in constitutions" :key="item" :label="item" :value="item" /></el-select>
      </div>
      <el-input
        v-model="keyword"
        class="recipe-search"
        clearable
        placeholder="搜索药膳或食材"
        @clear="resetPage"
        @input="resetPage"
      >
        <template #append><el-button :icon="Search" aria-label="搜索药膳" /></template>
      </el-input>
    </div>
    <div v-loading="loading" class="recipe-grid">
      <RouterLink v-for="recipe in visibleRecipes" :key="recipe.id" :to="`/recipes/${recipe.id}`" class="recipe-card">
        <div class="recipe-mark"><img :src="recipe.imageUrl || '/recipes/recipe-hero.png'" :alt="recipe.name"></div>
        <div class="recipe-copy">
          <span>{{ recipe.season || '四季' }} · {{ recipe.constitution || '日常调养' }}</span>
          <h2>{{ recipe.name }}</h2>
          <p>{{ recipe.summary }}</p>
          <small>适合：{{ recipe.suitableFor || '一般人群' }}</small>
        </div>
      </RouterLink>
      <el-empty v-if="!loading && filteredRecipes.length === 0" description="暂无符合条件的药膳" />
    </div>
    <el-pagination
      v-if="filteredRecipes.length > pageSize"
      background
      layout="prev, pager, next"
      :current-page="current"
      :page-size="pageSize"
      :total="filteredRecipes.length"
      @current-change="current = $event"
    />
  </section>
</template>

<style scoped>
.recipe-page { padding-block: 34px 42px; }
.recipe-hero { display: flex; min-height: 300px; align-items: end; padding: 44px; border-radius: 26px; background-position: center bottom; background-size: cover; box-shadow: 0 22px 60px rgb(23 60 45 / 16%); color: white; }
.recipe-hero div { max-width: 630px; }
.recipe-hero p { margin: 0 0 10px; color: #f0c5b8; font-size: 12px; font-weight: 800; letter-spacing: .18em; }
.recipe-hero h1 { margin: 0; font-family: "Noto Serif SC", "STSong", serif; font-size: clamp(3rem, 6vw, 5rem); letter-spacing: -.05em; }
.recipe-hero span { display: block; margin-top: 15px; font-size: 15px; line-height: 1.8; opacity: .88; }
.filters { display: flex; gap: 18px; align-items: center; margin: 28px 0 18px; }
.filter-selects { display: flex; flex: 1; gap: 10px; }
.filters .el-select { width: 190px; }
.recipe-search { flex: 0 0 250px; }
.filters :deep(.el-select__wrapper) { border-radius: 999px; background: rgb(255 255 255 / 72%); box-shadow: 0 0 0 1px rgb(79 138 108 / 18%) inset; backdrop-filter: blur(12px); }
.recipe-search :deep(.el-input__wrapper) { border-radius: 999px 0 0 999px; background: rgb(255 255 255 / 78%); box-shadow: 0 0 0 1px rgb(79 138 108 / 18%) inset; }
.recipe-search :deep(.el-input-group__append) { border-radius: 0 999px 999px 0; background: var(--color-ink); color: white; box-shadow: none; }
.recipe-grid { display: grid; min-height: 220px; grid-template-columns: repeat(2, 1fr); gap: 14px; }
.recipe-card { display: grid; grid-template-columns: 168px 1fr; gap: 22px; min-height: 222px; padding: 18px; border: 1px solid rgb(79 138 108 / 15%); border-radius: 26px; background: linear-gradient(145deg, rgb(255 255 255 / 96%), rgb(248 252 249 / 92%)); box-shadow: 0 14px 34px rgb(23 60 45 / 7%); }
.recipe-card:hover { border-color: rgb(79 138 108 / 44%); transform: translateY(-3px); box-shadow: 0 20px 42px rgb(23 60 45 / 13%); }
.recipe-mark { overflow: hidden; display: grid; place-items: center; border-radius: 20px; background: radial-gradient(circle at top, #fffaf1, #e7f1ea); }
.recipe-mark img { width: 100%; height: 100%; object-fit: contain; padding: 8px; }
.recipe-copy { display: flex; flex-direction: column; justify-content: center; min-width: 0; }
.recipe-card span, .recipe-card small { color: var(--color-cinnabar); font-size: 10px; font-weight: 800; }
.recipe-card h2 { margin: 16px 0 8px; color: var(--color-ink); font-family: "Noto Serif SC", "STSong", serif; font-size: 24px; letter-spacing: -.03em; }
.recipe-card p { min-height: 48px; color: var(--color-text-muted); font-size: 13px; line-height: 1.8; }
.recipe-card small { color: var(--color-jade); }
.el-pagination { justify-content: center; margin-top: 28px; }
:global(.recipe-filter-popper.el-popper) { overflow: hidden; border: 1px solid rgb(79 138 108 / 18%); border-radius: 18px; box-shadow: 0 18px 44px rgb(23 60 45 / 14%); }
:global(.recipe-filter-popper .el-select-dropdown) { background: rgb(250 253 250 / 96%); backdrop-filter: blur(16px); }
:global(.recipe-filter-popper .el-select-dropdown__item) { margin: 4px 8px; border-radius: 11px; color: #506b60; font-weight: 700; }
:global(.recipe-filter-popper .el-select-dropdown__item.hover),
:global(.recipe-filter-popper .el-select-dropdown__item:hover) { background: rgb(79 138 108 / 10%); color: var(--color-ink); }
:global(.recipe-filter-popper .el-select-dropdown__item.selected) { background: var(--color-ink); color: white; font-weight: 800; }
@media (max-width: 900px) { .recipe-grid { grid-template-columns: 1fr; } .filters { align-items: stretch; flex-direction: column; } .filter-selects { flex-wrap: wrap; } .recipe-search { flex-basis: auto; width: min(100%, 360px); } }
@media (max-width: 560px) { .recipe-card { grid-template-columns: 1fr; } .recipe-mark { min-height: 160px; } }
</style>
