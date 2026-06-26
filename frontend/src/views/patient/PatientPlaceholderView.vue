<script setup>
import { useRoute } from 'vue-router'

const route = useRoute()
</script>

<template>
  <section class="route-placeholder">
    <div class="placeholder-card">
      <p class="placeholder-label">Frontend Phase 1</p>
      <h1>{{ route.meta.title }}</h1>
      <p>{{ route.meta.description }}</p>
    </div>
  </section>
</template>

<style scoped>
.placeholder-label {
  margin-bottom: 10px;
  color: var(--color-cinnabar) !important;
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.14em;
}
</style>
