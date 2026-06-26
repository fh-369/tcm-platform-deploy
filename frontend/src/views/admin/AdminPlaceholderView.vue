<script setup>
import { useRoute } from 'vue-router'

const route = useRoute()
</script>

<template>
  <section class="admin-placeholder">
    <h1>{{ route.meta.title }}</h1>
  </section>
</template>

<style scoped>
.admin-placeholder {
  min-height: 54vh;
  padding: 42px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: white;
  box-shadow: var(--shadow-card);
}

.admin-placeholder h1 {
  margin: 0;
  font-size: 32px;
}
</style>
