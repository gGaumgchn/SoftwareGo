<template>
  <Teleport to="body">
    <Transition name="modal-fade">
      <div v-if="open" class="modal-mask" @click.self="emit('close')">
        <div
          ref="panel"
          class="modal-panel"
          role="dialog"
          aria-modal="true"
          :aria-label="ariaLabel"
          tabindex="-1"
        >
          <slot />
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
import { nextTick, onMounted, onUnmounted, ref, watch } from 'vue';

const props = defineProps({
  open: {
    type: Boolean,
    default: false,
  },
  ariaLabel: {
    type: String,
    default: '对话框',
  },
});

const emit = defineEmits(['close']);

const panel = ref(null);

function handleKeydown(event) {
  if (event.key === 'Escape' && props.open) {
    emit('close');
  }
}

watch(
  () => props.open,
  async (open) => {
    if (open) {
      await nextTick();
      panel.value?.focus();
    }
  },
);

onMounted(() => {
  window.addEventListener('keydown', handleKeydown);
});

onUnmounted(() => {
  window.removeEventListener('keydown', handleKeydown);
});
</script>

<style scoped>
.modal-mask {
  position: fixed;
  inset: 0;
  z-index: 98;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: rgba(15, 23, 42, 0.45);
  backdrop-filter: blur(3px);
}

.modal-panel {
  width: 560px;
  max-width: 92vw;
  max-height: 86vh;
  overflow: auto;
  padding: 28px;
  border: 1px solid var(--line2);
  border-radius: 14px;
  outline: none;
  background: var(--panel);
  box-shadow: 0 20px 60px rgba(15, 23, 42, 0.18);
}

.modal-fade-enter-active,
.modal-fade-leave-active {
  transition: opacity 0.18s ease;
}

.modal-fade-enter-from,
.modal-fade-leave-to {
  opacity: 0;
}
</style>
