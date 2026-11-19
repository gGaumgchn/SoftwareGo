<template>
  <span class="status-badge" :class="`status-badge--${type}`">
    <slot />
  </span>
</template>

<script setup>
defineProps({
  type: {
    type: String,
    default: 'gray',
  },
});
</script>

<style scoped>
.status-badge {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 3px 9px;
  border: 1px solid;
  border-radius: 20px;
  font-size: 12px;
  white-space: nowrap;
}

.status-badge--cyan {
  color: var(--cyan);
  border-color: rgba(255, 122, 24, 0.35);
  background: rgba(255, 122, 24, 0.08);
}

.status-badge--green {
  color: var(--green);
  border-color: rgba(52, 211, 153, 0.35);
  background: rgba(52, 211, 153, 0.08);
}

.status-badge--amber {
  color: var(--amber);
  border-color: rgba(251, 191, 36, 0.35);
  background: rgba(251, 191, 36, 0.08);
}

.status-badge--gray {
  color: var(--text2);
  border-color: var(--line);
  background: rgba(148, 163, 184, 0.06);
}

.status-badge--red {
  color: var(--red);
  border-color: rgba(248, 113, 113, 0.35);
  background: rgba(248, 113, 113, 0.08);
}
</style>
