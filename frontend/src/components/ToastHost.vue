<template>
  <div class="toast-stack" aria-live="polite">
    <TransitionGroup name="toast">
      <div
        v-for="item in toastState.items"
        :key="item.id"
        class="toast"
        :class="`toast--${item.type}`"
        role="status"
      >
        <span class="toast__dot" aria-hidden="true"></span>
        <span class="toast__message">{{ item.message }}</span>
      </div>
    </TransitionGroup>
  </div>
</template>

<script setup>
import { toastState } from '../utils/toast';
</script>

<style scoped>
.toast-stack {
  position: fixed;
  top: 22px;
  left: 50%;
  z-index: 2000;
  display: flex;
  flex-direction: column;
  gap: 10px;
  width: min(420px, calc(100vw - 32px));
  transform: translateX(-50%);
  pointer-events: none;
}

.toast {
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 11px 16px;
  border: 1px solid var(--line2);
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.98);
  box-shadow: 0 16px 40px rgba(15, 23, 42, 0.16);
  color: var(--text);
  font-size: 13.5px;
  pointer-events: auto;
}

.toast__dot {
  flex: 0 0 auto;
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--cyan);
}

.toast--error .toast__dot {
  background: var(--red);
}

.toast--success .toast__dot {
  background: var(--green);
}

.toast__message {
  word-break: break-word;
}

.toast-enter-active,
.toast-leave-active {
  transition:
    opacity 0.18s ease,
    transform 0.18s ease;
}

.toast-enter-from,
.toast-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}
</style>
