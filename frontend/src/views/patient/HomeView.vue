<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'
import {
  ArrowRight,
  CircleCheck,
  Close,
  Clock,
  DocumentAdd,
  EditPen,
  Guide,
} from '@element-plus/icons-vue'

const isJourneyOpen = ref(false)

const steps = [
  { index: '01', title: '记录身体状态', description: '填写症状、持续时间与需要补充的信息。', icon: EditPen },
  { index: '02', title: '提交问诊单', description: '形成结构清晰的记录，交由医生查看。', icon: DocumentAdd },
  { index: '03', title: '等待医生处理', description: '随时查看接诊状态与后续跟进安排。', icon: Clock },
  { index: '04', title: '查看建议与跟进', description: '了解医生建议，持续记录身体变化。', icon: CircleCheck },
]

function closeJourney() {
  isJourneyOpen.value = false
}

function handleKeydown(event) {
  if (event.key === 'Escape') {
    closeJourney()
  }
}

onMounted(() => window.addEventListener('keydown', handleKeydown))
onBeforeUnmount(() => window.removeEventListener('keydown', handleKeydown))
</script>

<template>
  <div class="home-page" :class="{ 'is-journey-open': isJourneyOpen }">
    <section class="hero">
      <img src="../../assets/brand/home-consultation-hero.png" alt="自然光下的现代东方草本问诊空间" />
      <div class="hero-shade"></div>
      <div class="hero-copy">
        <p class="eyebrow">知身，而后问养</p>
        <h1>让身体的变化<br />被认真听见</h1>
        <p class="hero-description">
          清楚记录身体状态，让每一次问诊都有迹可循。
        </p>
        <RouterLink class="consultation-entry" to="/consultation/new">
          <span>
            <small>从这里开始</small>
            <strong>记录身体状态</strong>
          </span>
          <span class="entry-action">
            创建问诊单
            <el-icon><ArrowRight /></el-icon>
          </span>
        </RouterLink>
      </div>
    </section>

    <button
      class="journey-trigger"
      type="button"
      aria-controls="journey-panel"
      :aria-expanded="isJourneyOpen"
      @click="isJourneyOpen = true"
    >
      <el-icon><Guide /></el-icon>
      <span>了解问诊流程</span>
    </button>

    <Transition name="journey-mask">
      <button
        v-if="isJourneyOpen"
        class="journey-backdrop"
        type="button"
        aria-label="关闭问诊流程"
        @click="closeJourney"
      ></button>
    </Transition>

    <aside
      id="journey-panel"
      class="journey-panel"
      :aria-hidden="!isJourneyOpen"
      aria-labelledby="journey-title"
      role="dialog"
    >
      <header class="journey-heading">
        <div>
          <p>HOW IT WORKS</p>
          <h2 id="journey-title">一次问诊，<br />从认真记录开始</h2>
        </div>
        <button type="button" aria-label="关闭问诊流程" @click="closeJourney">
          <el-icon><Close /></el-icon>
        </button>
      </header>

      <ol class="journey-steps">
        <li v-for="step in steps" :key="step.index">
          <div class="step-icon">
            <el-icon><component :is="step.icon" /></el-icon>
            <span>{{ step.index }}</span>
          </div>
          <div>
            <h3>{{ step.title }}</h3>
            <p>{{ step.description }}</p>
          </div>
        </li>
      </ol>

      <RouterLink class="journey-action" to="/consultation/new" @click="closeJourney">
        创建问诊单
        <el-icon><ArrowRight /></el-icon>
      </RouterLink>
    </aside>
  </div>
</template>

<style scoped>
.home-page {
  position: relative;
  height: calc(100dvh - 83px);
  overflow: hidden;
}

.hero {
  position: relative;
  display: grid;
  min-height: 100%;
  place-items: center;
  overflow: hidden;
  isolation: isolate;
  background: #64503a;
}

.hero > img,
.hero-shade {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
}

.hero > img {
  z-index: -2;
  object-fit: cover;
}

.hero-shade {
  z-index: -1;
  background:
    linear-gradient(180deg, rgb(18 42 30 / 12%), rgb(18 42 30 / 46%)),
    radial-gradient(circle at center, rgb(16 45 31 / 14%), rgb(16 38 27 / 34%));
}

.hero-copy {
  width: min(820px, calc(100% - 48px));
  padding: 70px 0 40px;
  color: white;
  text-align: center;
}

.eyebrow {
  margin: 0;
  color: rgb(255 255 255 / 82%);
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.3em;
}

.hero h1 {
  margin: 22px 0 18px;
  font-family: "Noto Serif SC", serif;
  font-size: clamp(4rem, 7vw, 7.2rem);
  font-weight: 700;
  letter-spacing: 0.04em;
  line-height: 1.14;
  text-shadow: 0 10px 40px rgb(14 34 24 / 34%);
}

.hero-description {
  margin: 0;
  color: rgb(255 255 255 / 78%);
  font-size: 17px;
  line-height: 1.9;
}

.consultation-entry {
  display: flex;
  width: min(620px, 100%);
  min-height: 76px;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  margin: 38px auto 0;
  padding: 10px 12px 10px 24px;
  border: 1px solid rgb(255 255 255 / 46%);
  border-radius: 999px;
  background: rgb(255 255 255 / 18%);
  box-shadow: 0 20px 54px rgb(16 35 25 / 22%);
  text-align: left;
  backdrop-filter: blur(18px) saturate(125%);
}

.consultation-entry:hover {
  background: rgb(255 255 255 / 26%);
  transform: translateY(-2px);
}

.consultation-entry small,
.consultation-entry strong {
  display: block;
}

.consultation-entry small {
  margin-bottom: 4px;
  color: rgb(255 255 255 / 65%);
  font-size: 10px;
  letter-spacing: 0.14em;
}

.consultation-entry strong {
  font-size: 16px;
}

.entry-action {
  display: inline-flex;
  min-height: 52px;
  align-items: center;
  gap: 10px;
  padding: 0 22px;
  border-radius: 999px;
  background: var(--color-cinnabar);
  font-size: 14px;
  font-weight: 800;
  white-space: nowrap;
}

.journey-trigger {
  position: absolute;
  z-index: 4;
  top: 50%;
  left: 0;
  display: flex;
  min-height: 138px;
  align-items: center;
  gap: 10px;
  padding: 16px 11px;
  border: 1px solid rgb(255 255 255 / 38%);
  border-left: 0;
  border-radius: 0 18px 18px 0;
  background: rgb(23 60 45 / 74%);
  color: white;
  cursor: pointer;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: .08em;
  transform: translateY(-50%);
  backdrop-filter: blur(14px);
}

.journey-trigger:hover {
  padding-left: 17px;
  background: rgb(23 60 45 / 88%);
}

.journey-trigger span {
  max-width: 1em;
  line-height: 1.45;
}

.journey-trigger .el-icon {
  font-size: 18px;
}

.journey-backdrop {
  position: fixed;
  z-index: 49;
  inset: 0;
  border: 0;
  background: rgb(12 31 22 / 34%);
  cursor: default;
  backdrop-filter: blur(5px);
}

.journey-mask-enter-active,
.journey-mask-leave-active {
  transition: opacity 280ms ease;
}

.journey-mask-enter-from,
.journey-mask-leave-to {
  opacity: 0;
}

.journey-panel {
  position: fixed;
  z-index: 50;
  top: 0;
  bottom: 0;
  left: 0;
  display: flex;
  width: min(480px, calc(100vw - 32px));
  flex-direction: column;
  padding: 42px 42px 34px;
  overflow-y: auto;
  background:
    radial-gradient(circle at 0 0, rgb(220 236 228 / 86%), transparent 34%),
    #f8faf8;
  box-shadow: 28px 0 80px rgb(11 32 21 / 22%);
  transform: translateX(-105%);
  transition: transform 460ms cubic-bezier(.77, 0, .18, 1);
  visibility: hidden;
}

.is-journey-open .journey-panel {
  transform: translateX(0);
  visibility: visible;
}

.journey-heading {
  display: flex;
  align-items: start;
  justify-content: space-between;
  gap: 20px;
}

.journey-heading p {
  margin: 0 0 10px;
  color: var(--color-cinnabar);
  font-size: 10px;
  font-weight: 800;
  letter-spacing: 0.24em;
}

.journey-heading h2 {
  margin: 0;
  color: var(--color-ink);
  font-family: "Noto Serif SC", serif;
  font-size: 34px;
  font-weight: 700;
  line-height: 1.35;
}

.journey-heading button {
  display: grid;
  width: 42px;
  height: 42px;
  flex: 0 0 auto;
  place-items: center;
  border: 1px solid var(--color-border);
  border-radius: 50%;
  background: white;
  color: var(--color-ink);
  cursor: pointer;
}

.journey-steps {
  display: grid;
  gap: 0;
  margin: 34px 0 0;
  padding: 0;
  list-style: none;
}

.journey-steps li {
  position: relative;
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 18px;
  padding: 0 0 22px;
}

.step-icon {
  position: relative;
  display: grid;
  width: 56px;
  height: 56px;
  place-items: center;
  border: 1px solid var(--color-border);
  border-radius: 50%;
  background: white;
  color: var(--color-ink);
  box-shadow: var(--shadow-card);
}

.step-icon .el-icon {
  font-size: 21px;
}

.step-icon span {
  position: absolute;
  top: -6px;
  right: -5px;
  display: grid;
  width: 25px;
  height: 25px;
  place-items: center;
  border-radius: 50%;
  background: var(--color-cinnabar);
  color: white;
  font-size: 9px;
  font-weight: 800;
}

.journey-steps h3 {
  margin: 3px 0 6px;
  color: var(--color-ink);
  font-size: 17px;
}

.journey-steps p {
  margin: 0;
  color: var(--color-text-muted);
  font-size: 12px;
  line-height: 1.8;
}

.journey-action {
  display: flex;
  min-height: 52px;
  align-items: center;
  justify-content: space-between;
  margin-top: auto;
  padding: 0 20px;
  border-radius: 999px;
  background: var(--color-ink);
  color: white;
  font-size: 14px;
  font-weight: 800;
}

@media (max-width: 620px) {
  .hero h1 {
    font-size: 3rem;
  }

  .consultation-entry {
    display: grid;
  }

  .entry-action {
    justify-content: center;
  }

  .journey-panel {
    padding: 28px 24px 24px;
  }
}
</style>
