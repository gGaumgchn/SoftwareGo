<template>
  <div class="app-shell">
    <AppSidebar />

    <div class="app-main">
      <AppHeader :title="title" />
      <main class="app-content">
        <slot />
      </main>
    </div>
  </div>
</template>

<script setup>
import AppHeader from '../components/AppHeader.vue';
import AppSidebar from '../components/AppSidebar.vue';

defineProps({
  title: {
    type: String,
    default: '',
  },
});
</script>

<style scoped>
.app-shell {
  display: flex;
  min-height: 100vh;
}

.app-main {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-width: 0;
  height: 100vh;
}

.app-content {
  flex: 1;
  overflow: auto;
  padding: 24px 30px 44px;
}

@media (max-width: 900px) {
  .app-content {
    padding: 20px 18px 36px;
  }
}
</style>
