<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'

import { getPublishedRecipeDetail } from '../../api/content'
import { parseRecipeList } from '../../features/recipe/display'

const route = useRoute()
const recipe = ref(null)
const loading = ref(false)
const ingredients = computed(() => parseRecipeList(recipe.value?.ingredients))
const steps = computed(() => parseRecipeList(recipe.value?.steps))

onMounted(async () => {
  loading.value = true
  try { recipe.value = await getPublishedRecipeDetail(route.params.id) }
  catch (error) { ElMessage.error(error.message || '药膳详情加载失败') }
  finally { loading.value = false }
})
</script>

<template>
  <section v-loading="loading" class="recipe-detail page-container">
    <RouterLink v-if="recipe" class="recipe-back" to="/recipes" aria-label="返回药膳推荐">←</RouterLink>
    <article v-if="recipe">
      <header>
        <div>
          <span>{{ recipe.season }} · {{ recipe.constitution }}</span>
          <h1>{{ recipe.name }}</h1>
          <p>{{ recipe.summary }}</p>
        </div>
        <img :src="recipe.imageUrl || '/recipes/recipe-hero.png'" :alt="recipe.name">
      </header>
      <section class="suitable"><strong>适合人群</strong><p>{{ recipe.suitableFor }}</p></section>
      <div class="info-grid">
        <section><strong>食材准备</strong><ul><li v-for="item in ingredients" :key="item">{{ item }}</li></ul></section>
        <section><strong>制作步骤</strong><ol><li v-for="item in steps" :key="item">{{ item }}</li></ol></section>
      </div>
    </article>
  </section>
</template>

<style scoped>
.recipe-detail { min-height: 60vh; padding-block: 42px; }
.recipe-back { display: grid; width: 46px; height: 46px; margin: 0 auto 18px; max-width: 900px; place-items: center; border: 1px solid rgb(79 138 108 / 18%); border-radius: 50%; background: rgb(255 255 255 / 88%); color: var(--color-cinnabar); font-size: 18px; font-weight: 900; box-shadow: 0 10px 24px rgb(23 60 45 / 8%); transform: translateX(calc(-450px + 23px)); }
.recipe-back:hover { border-color: rgb(79 138 108 / 38%); transform: translateX(calc(-450px + 21px)); box-shadow: 0 14px 30px rgb(23 60 45 / 12%); }
article { max-width: 900px; margin: 0 auto 22px; padding: clamp(28px, 6vw, 64px); border: 1px solid var(--color-border); border-radius: var(--radius-lg); background: white; box-shadow: var(--shadow-soft); }
header { display: grid; grid-template-columns: 1fr 330px; gap: 30px; align-items: end; }
header span { color: var(--color-cinnabar); font-size: 11px; font-weight: 800; }
h1 { margin: 16px 0; font-size: clamp(2.4rem, 6vw, 4.8rem); letter-spacing: -.07em; }
p { color: var(--color-text-muted); line-height: 1.95; white-space: pre-wrap; }
header img { width: 100%; max-height: 310px; object-fit: contain; border-radius: 24px; background: linear-gradient(145deg, #fffaf1, #e8f2ec); box-shadow: 0 18px 42px rgb(23 60 45 / 12%); }
.suitable { margin-top: 24px; padding: 20px 22px; border-left: 4px solid var(--color-cinnabar); border-radius: 0 16px 16px 0; background: var(--color-mist); }
.info-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; margin-top: 38px; }
.info-grid section { padding: 24px; border: 1px solid rgb(79 138 108 / 16%); border-radius: 20px; background: linear-gradient(145deg, #ffffff, #f7fbf8); box-shadow: 0 12px 30px rgb(23 60 45 / 6%); }
strong { color: var(--color-ink); font-family: "Noto Serif SC", "STSong", serif; font-size: 19px; }
ul, ol { margin: 16px 0 0; padding-left: 1.3em; color: #405e51; line-height: 1.9; }
li { margin: 8px 0; padding-left: 4px; }
@media (max-width: 960px) { .recipe-back { margin-left: 0; transform: none; } .recipe-back:hover { transform: translateX(-2px); } }
@media (max-width: 760px) { header { grid-template-columns: 1fr; } .info-grid { grid-template-columns: 1fr; } }
</style>
