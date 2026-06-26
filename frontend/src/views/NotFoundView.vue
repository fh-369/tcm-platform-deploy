<template>
  <main class="route-placeholder">
    <div class="placeholder-card">
      <p class="error-code">404</p>
      <h1>没有找到这个页面</h1>
      <p>地址可能已经变化，也可能尚未在当前开发阶段开放。</p>
      <RouterLink class="primary-link" to="/">返回患者端首页</RouterLink>
    </div>
  </main>
</template>

<style scoped>
.error-code {
  margin-bottom: 12px !important;
  color: var(--color-cinnabar) !important;
  font-size: 14px;
  font-weight: 800;
  letter-spacing: 0.16em;
}

.primary-link {
  margin-top: 24px;
}
</style>
