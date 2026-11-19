import { computed, ref } from 'vue';
import { defineStore } from 'pinia';
import { getAdmission } from '../api/offer';

export const useAdmissionStore = defineStore('admission', () => {
  const admission = ref(null);
  const loading = ref(false);
  const error = ref(false);
  const loaded = ref(false);

  const admitted = computed(() => Boolean(admission.value));

  async function fetch() {
    if (loading.value || loaded.value) {
      return admission.value;
    }

    loading.value = true;
    error.value = false;

    try {
      const data = await getAdmission();
      admission.value = data?.admitted ? data.admission : null;
      loaded.value = true;
      return admission.value;
    } catch (requestError) {
      error.value = true;
      admission.value = null;
      throw requestError;
    } finally {
      loading.value = false;
    }
  }

  function setAdmission(value) {
    admission.value = value ?? null;
    loaded.value = true;
    error.value = false;
  }

  function clear() {
    admission.value = null;
    loading.value = false;
    error.value = false;
    loaded.value = false;
  }

  return {
    admission,
    loading,
    error,
    loaded,
    admitted,
    fetch,
    setAdmission,
    clear,
  };
});
