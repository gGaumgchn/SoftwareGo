<template>
  <div class="stepper">
    <div
      v-for="stage in stages"
      :key="stage.stageCode"
      class="step"
      :class="stageClass(stage)"
    >
      <span class="node"></span>
      <div class="step-name">{{ stage.stageName }}</div>
      <div class="step-time">{{ formatDate(stage.startAt) }}</div>
    </div>
  </div>
</template>

<script setup>
import { formatDate } from '../utils/time';

const props = defineProps({
  stages: {
    type: Array,
    default: () => [],
  },
  currentStageCode: {
    type: String,
    default: '',
  },
  nextStageCode: {
    type: String,
    default: '',
  },
});

function stageClass(stage) {
  if (stage.stageCode === props.currentStageCode || stage.state === 'ONGOING') {
    return 'current';
  }
  if (stage.state === 'FINISHED') {
    return 'done';
  }
  if (stage.stageCode === props.nextStageCode) {
    return 'next';
  }
  return '';
}
</script>

<style scoped>
.stepper {
  display: flex;
  margin: 6px 0 4px;
}

.step {
  position: relative;
  flex: 1;
  padding-top: 24px;
  text-align: center;
}

.step .node {
  position: absolute;
  top: 0;
  left: 50%;
  width: 14px;
  height: 14px;
  border: 2px solid #cbd3e0;
  border-radius: 50%;
  background: #eef1f6;
  transform: translateX(-50%);
  transition: 0.2s;
}

.step.done .node {
  border-color: var(--cyan);
  background: var(--cyan);
  box-shadow: 0 0 9px rgba(255, 122, 24, 0.55);
}

.step.current .node {
  border-color: var(--cyan);
  background: var(--cyan);
  box-shadow: 0 0 14px rgba(255, 122, 24, 0.85);
}

.step.next .node {
  border-color: var(--amber);
  background: #fff7e6;
}

.step:not(:last-child)::after {
  content: '';
  position: absolute;
  top: 6px;
  left: calc(50% + 11px);
  width: calc(100% - 22px);
  height: 2px;
  background: #e4e8f0;
}

.step.done:not(:last-child)::after {
  background: linear-gradient(90deg, var(--cyan), rgba(255, 122, 24, 0.22));
}

.step-name {
  margin-top: 8px;
  font-size: 12.5px;
  color: var(--text2);
}

.step.current .step-name {
  color: var(--cyan);
  font-weight: 600;
}

.step.next .step-name {
  color: var(--amber);
}

.step.done .step-name {
  color: var(--text);
}

.step-time {
  margin-top: 2px;
  font-family: var(--tech);
  font-size: 11px;
  color: var(--text3);
}
</style>
