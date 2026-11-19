import { reactive } from 'vue';

let seed = 0;

export const toastState = reactive({
  items: [],
});

export function showToast(message, type = 'info', duration = 2800) {
  const id = ++seed;
  toastState.items.push({ id, message, type });

  window.setTimeout(() => {
    removeToast(id);
  }, duration);
}

export function removeToast(id) {
  const index = toastState.items.findIndex((item) => item.id === id);
  if (index !== -1) {
    toastState.items.splice(index, 1);
  }
}
