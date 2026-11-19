import { computed, ref } from 'vue';
import { defineStore } from 'pinia';
import { getMe, login as loginApi, logout as logoutApi } from '../api/auth';

export const useUserStore = defineStore('user', () => {
  const user = ref(null);
  const loading = ref(false);

  const isLoggedIn = computed(() => Boolean(user.value));
  const role = computed(() => user.value?.role ?? null);
  const displayName = computed(() => user.value?.name || user.value?.staffNo || '');

  async function login(payload) {
    loading.value = true;
    try {
      user.value = await loginApi(payload);
      return user.value;
    } finally {
      loading.value = false;
    }
  }

  async function fetchMe() {
    if (user.value) {
      return user.value;
    }

    loading.value = true;
    try {
      user.value = await getMe();
      return user.value;
    } catch (error) {
      user.value = null;
      throw error;
    } finally {
      loading.value = false;
    }
  }

  async function logout() {
    try {
      await logoutApi();
    } finally {
      clear();
    }
  }

  function clear() {
    user.value = null;
  }

  return {
    user,
    loading,
    isLoggedIn,
    role,
    displayName,
    login,
    fetchMe,
    logout,
    clear,
  };
});
