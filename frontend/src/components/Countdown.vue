<template>
  <div class="countdown">
    <div v-if="remaining && !remaining.finished" class="count-block">
      <span class="count">{{ remaining.days }}天</span>
      <small>{{ pad(remaining.hours) }}</small>
      <span class="sep">:</span>
      <small>{{ pad(remaining.minutes) }}</small>
      <span class="sep">:</span>
      <small>{{ pad(remaining.seconds) }}</small>
    </div>
    <div v-else class="count count--muted">{{ target ? '已结束' : '—' }}</div>
    <div v-if="label" class="count-label">{{ label }}</div>
  </div>
</template>

<script setup>
import { onMounted, onUnmounted, ref, watch } from 'vue';
import { getCountdown } from '../utils/time';

const props = defineProps({
  target: {
    type: String,
    default: '',
  },
  label: {
    type: String,
    default: '',
  },
});

const remaining = ref(null);
let timer = null;

function tick() {
  remaining.value = getCountdown(props.target);
}

function pad(value) {
  return String(value).padStart(2, '0');
}

onMounted(() => {
  tick();
  timer = window.setInterval(tick, 1000);
});

onUnmounted(() => {
  if (timer) {
    window.clearInterval(timer);
  }
});

watch(
  () => props.target,
  () => tick(),
);
</script>

<style scoped>
.countdown {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.count-block {
  display: flex;
  align-items: baseline;
  gap: 5px;
}

.count {
  font-family: var(--tech);
  font-size: 30px;
  font-weight: 700;
  letter-spacing: 1px;
  color: var(--cyan);
}

.count--muted {
  color: var(--text3);
}

.count-block small {
  font-family: var(--tech);
  font-size: 12px;
  font-weight: 500;
  color: var(--text3);
}

.sep {
  font-size: 12px;
  color: var(--text3);
}

.count-label {
  font-size: 12px;
  color: var(--text3);
}
</style>
