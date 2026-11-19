<template>
  <div class="empty-state" :class="`empty-state--${kind}`">
    <template v-if="kind === 'loading'">
      <span class="spinner" aria-hidden="true"></span>
      <p>加载中…</p>
    </template>

    <template v-else>
      <BaseIcon :name="iconName" :size="40" class="empty-icon" />
      <p>{{ kind === 'error' ? errorText : text }}</p>
      <button v-if="kind === 'error'" class="retry-btn" type="button" @click="$emit('retry')">
        重试
      </button>
    </template>
  </div>
</template>

<script setup>
import { computed } from 'vue';
import BaseIcon from './BaseIcon.vue';

const props = defineProps({
  kind: {
    type: String,
    default: 'empty',
  },
  text: {
    type: String,
    default: '暂无数据',
  },
  errorText: {
    type: String,
    default: '加载失败，请重试',
  },
});

defineEmits(['retry']);

const iconName = computed(() => (props.kind === 'error' ? 'alert-circle' : 'inbox'));
</script>

<style scoped>
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 150px;
  padding: 28px 16px;
  text-align: center;
  color: var(--text3);
}

.empty-state p {
  margin: 10px 0 0;
  font-size: 13px;
}

.empty-icon {
  opacity: 0.45;
}

.empty-state--error .empty-icon {
  color: var(--red);
  opacity: 0.8;
}

.retry-btn {
  margin-top: 12px;
  padding: 6px 14px;
  border: 1px solid var(--line2);
  border-radius: 8px;
  background: var(--panel2);
  color: var(--text);
  font-size: 12.5px;
  cursor: pointer;
}

.retry-btn:hover {
  background: #fff1e3;
}

.spinner {
  width: 22px;
  height: 22px;
  border: 2px solid rgba(255, 122, 24, 0.2);
  border-top-color: var(--cyan);
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
