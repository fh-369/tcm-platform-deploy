<template>
  <div class="auth-shell">
    <RouterLink class="auth-brand" to="/" aria-label="返回知身问养首页">
      <img src="../assets/brand/logo-mark.png" alt="" />
      <span>
        <strong>知身问养</strong>
        <small>中医问诊与日常养护平台</small>
      </span>
    </RouterLink>
    <RouterView />
  </div>
</template>

<style scoped>
.auth-shell {
  min-height: 100vh;
  padding: 26px 34px 34px;
  background: #f3f0e8;
}

.auth-brand {
  display: inline-flex;
  align-items: center;
  gap: 12px;
  color: var(--color-ink);
}

.auth-brand img {
  width: 44px;
  height: 44px;
  object-fit: contain;
}

.auth-brand span {
  display: grid;
  gap: 2px;
}

.auth-brand strong {
  font-family: "Noto Serif SC", serif;
  font-size: 20px;
  letter-spacing: 0.12em;
}

.auth-brand small {
  color: #738078;
  font-size: 10px;
  letter-spacing: 0.08em;
}
</style>
