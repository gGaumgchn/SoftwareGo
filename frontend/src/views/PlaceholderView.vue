<template>
  <DefaultLayout :title="title">
    <section class="card">
      <div class="card-head">
        <div class="card-title"><span class="dot"></span>{{ title }}</div>
      </div>
      <p class="placeholder-text">页面待实现，后续按开发计划补充。</p>
    </section>
  </DefaultLayout>
</template>

<script setup>
import { computed } from 'vue';
import { useRoute } from 'vue-router';
import DefaultLayout from '../layouts/DefaultLayout.vue';

const route = useRoute();
const title = computed(() => route.meta.title || '页面');
</script>

<style scoped>
.placeholder-text {
  margin: 0;
  color: var(--text3);
}
</style>
