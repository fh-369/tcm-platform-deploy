<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import DOMPurify from 'dompurify'
import MarkdownIt from 'markdown-it'

import { getPublishedKnowledgeDetail } from '../../api/content'

const route = useRoute()
const loading = ref(false)
const article = ref(null)
const markdown = new MarkdownIt({
  breaks: true,
  html: false,
  linkify: true,
  typographer: true,
})

const renderedContent = computed(() =>
  DOMPurify.sanitize(markdown.render(article.value?.content || '')),
)

onMounted(async () => {
  loading.value = true
  try {
    article.value = await getPublishedKnowledgeDetail(route.params.id)
  } catch (error) {
    ElMessage.error(error.message || '文章加载失败')
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <article v-loading="loading" class="detail-page page-container">
    <div v-if="article" class="detail-card">
      <header
        class="detail-hero"
        :style="{ backgroundImage: `linear-gradient(0deg, rgba(15, 48, 36, .88), rgba(15, 48, 36, .18)), url('${article.coverImageUrl || '/knowledge/knowledge-hero.png'}')` }"
      >
        <div>
          <span>{{ article.category || '养生常识' }}</span>
          <h1>{{ article.title }}</h1>
          <p>{{ article.summary }}</p>
          <small>{{ article.viewCount || 0 }} 次浏览</small>
        </div>
      </header>
      <div class="reading-stage">
        <header class="reading-toolbar">
          <div class="toolbar-main">
            <RouterLink to="/knowledge" aria-label="返回养生知识">←</RouterLink>
            <div>
              <span>{{ article.category || '养生常识' }}</span>
              <strong>{{ article.title }}</strong>
            </div>
          </div>
          <small>{{ article.viewCount || 0 }} 次浏览</small>
        </header>
        <div class="reading-sheet">
          <section class="article-body" v-html="renderedContent"></section>
        <aside v-if="article.tips"><strong>日常提示</strong><p>{{ article.tips }}</p></aside>
        </div>
      </div>
    </div>
  </article>
</template>

<style scoped>
.detail-page { min-height: 60vh; padding-block: 42px; }
.detail-card { max-width: 980px; margin: 0 auto; border-radius: 26px; }
.detail-hero { min-height: 430px; display: flex; align-items: end; padding: clamp(32px, 6vw, 68px); background-position: center; background-size: cover; color: white; }
.detail-hero div { max-width: 720px; }
header span { display: inline-flex; padding: 6px 10px; border-radius: 999px; background: rgb(255 255 255 / 16%); font-size: 11px; font-weight: 800; letter-spacing: .14em; backdrop-filter: blur(8px); }
h1 { margin: 20px 0 14px; font-family: "Noto Serif SC", "STSong", serif; font-size: clamp(2.3rem, 5vw, 4.5rem); letter-spacing: -.05em; line-height: 1.2; }
header p { max-width: 650px; margin: 0; font-size: 15px; line-height: 1.9; opacity: .88; }
header small { display: block; margin-top: 18px; font-size: 11px; opacity: .75; }
.reading-stage { position: sticky; top: 86px; height: calc(100vh - 106px); margin-top: 22px; overflow: hidden; border: 1px solid var(--color-border); border-radius: 24px; background: white; box-shadow: var(--shadow-soft); }
.reading-toolbar { min-height: 70px; display: flex; align-items: center; justify-content: space-between; gap: 18px; padding: 13px 24px; border-bottom: 1px solid var(--color-border); background: rgb(247 251 248 / 94%); backdrop-filter: blur(18px); }
.toolbar-main { min-width: 0; display: flex; align-items: center; gap: 12px; }
.toolbar-main > a { display: grid; width: 34px; height: 34px; flex: 0 0 auto; place-items: center; border: 1px solid var(--color-border); border-radius: 50%; color: var(--color-cinnabar); font-size: 17px; font-weight: 800; }
.toolbar-main > a:hover { border-color: var(--color-cinnabar); background: var(--color-cinnabar-soft); }
.toolbar-main > div { min-width: 0; }
.reading-toolbar span { display: block; color: var(--color-cinnabar); font-size: 10px; font-weight: 800; letter-spacing: .12em; }
.reading-toolbar strong { display: block; margin-top: 4px; overflow: hidden; color: var(--color-ink); font-family: "Noto Serif SC", "STSong", serif; font-size: 16px; text-overflow: ellipsis; white-space: nowrap; }
.reading-toolbar small { flex: 0 0 auto; color: var(--color-text-muted); font-size: 11px; }
.reading-sheet { height: calc(100% - 70px); padding: clamp(32px, 7vw, 72px); overflow-y: auto; overscroll-behavior: contain; scrollbar-color: rgb(79 138 108 / 42%) transparent; }
.article-body { color: #29483b; font-size: 16px; line-height: 2.05; }
.article-body :deep(h1), .article-body :deep(h2), .article-body :deep(h3) { color: var(--color-ink); font-family: "Noto Serif SC", "STSong", serif; line-height: 1.45; }
.article-body :deep(h1) { margin: 0 0 28px; font-size: 32px; }
.article-body :deep(h2) { margin: 42px 0 16px; padding-bottom: 9px; border-bottom: 1px solid rgb(79 138 108 / 18%); font-size: 24px; }
.article-body :deep(h3) { margin: 30px 0 12px; font-size: 19px; }
.article-body :deep(p) { margin: 0 0 18px; }
.article-body :deep(strong) { color: var(--color-ink); }
.article-body :deep(ul), .article-body :deep(ol) { margin: 12px 0 24px; padding-left: 1.6em; }
.article-body :deep(li) { margin: 7px 0; }
.article-body :deep(blockquote) { margin: 28px 0; padding: 18px 20px; border-left: 4px solid var(--color-cinnabar); border-radius: 0 14px 14px 0; background: var(--color-mist); color: #506b60; }
.article-body :deep(blockquote p:last-child) { margin-bottom: 0; }
.article-body :deep(a) { color: var(--color-cinnabar); font-weight: 700; text-decoration: underline; text-decoration-color: rgb(201 81 61 / 30%); text-underline-offset: 4px; }
.article-body :deep(hr) { margin: 38px 0; border: 0; border-top: 1px solid var(--color-border); }
.article-body :deep(code) { padding: 2px 6px; border-radius: 5px; background: var(--color-mist); color: var(--color-cinnabar); }
aside { margin-top: 42px; padding: 22px; border-left: 3px solid var(--color-cinnabar); background: var(--color-mist); }
aside p { margin: 8px 0 0; color: var(--color-text-muted); line-height: 1.8; }
@media (max-width: 760px) { .detail-hero { min-height: 340px; } .reading-stage { position: static; height: auto; } .reading-toolbar { display: none; } .reading-sheet { height: auto; overflow: visible; } }
</style>
