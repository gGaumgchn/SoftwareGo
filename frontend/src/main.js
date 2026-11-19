import { createApp } from 'vue';
import { createPinia } from 'pinia';
import App from './App.vue';
import router from './router';
import { useUserStore } from './stores/user';
import { useAdmissionStore } from './stores/admission';
import { showToast } from './utils/toast';
import './styles/variables.css';
import './styles/base.css';
import './styles/components.css';

const app = createApp(App);
const pinia = createPinia();

app.use(pinia);
app.use(router);

window.addEventListener('auth:unauthorized', () => {
  const userStore = useUserStore(pinia);
  const admissionStore = useAdmissionStore(pinia);
  const hadUser = Boolean(userStore.user);
  userStore.clear();
  admissionStore.clear();

  if (hadUser) {
    showToast('登录已过期，请重新登录', 'error');
  }

  const current = router.currentRoute.value;
  if (current.name !== 'login') {
    router.replace({
      name: 'login',
      query: { redirect: current.fullPath },
    });
  }
});

app.mount('#app');
