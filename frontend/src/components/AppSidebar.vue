<template>
  <aside class="sidebar">
    <div class="brand">
      <div class="brand-mark">
        <BaseIcon name="logo" :size="21" />
      </div>
      <div>
        <div class="brand-name">实训室选拔系统</div>
        <div class="brand-sub">Lab Selection</div>
      </div>
    </div>

    <nav class="menu">
      <div class="menu-cap">功能菜单 · {{ roleLabel }}</div>
      <button
        v-for="item in menuItems"
        :key="item.to"
        class="menu-item"
        :class="{ active: isActive(item) }"
        type="button"
        @click="handleMenuClick(item)"
      >
        <BaseIcon :name="item.icon" :size="18" />
        <span>{{ item.label }}</span>
      </button>
    </nav>

    <div class="sidebar-foot">
      <div>软件学院 · 实训室选拔系统</div>
      <button class="logout-link" type="button" :disabled="loggingOut" @click="handleLogout">
        {{ loggingOut ? '退出中…' : '退出并重新登录' }}
      </button>
    </div>
  </aside>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import BaseIcon from './BaseIcon.vue';
import { useUserStore } from '../stores/user';
import { useAdmissionStore } from '../stores/admission';
import { showToast } from '../utils/toast';

const MENUS = {
  STUDENT: [
    { label: '主页面', to: '/home', icon: 'home' },
    { label: '公告', to: '/announcements', icon: 'bell' },
    { label: '查看实训室信息', to: '/student/labs', icon: 'building' },
    { label: '志愿填报', to: '/student/volunteer', icon: 'edit' },
    { label: '拟录取确认', to: '/student/offers', icon: 'check' },
    { label: '个人待办', to: '/todos', icon: 'todo' },
  ],
  TEACHER: [
    { label: '主页面', to: '/home', icon: 'home' },
    { label: '公告', to: '/announcements', icon: 'bell' },
    { label: '实训室信息', to: '/teacher/lab', icon: 'building' },
    { label: '申请与录取管理', to: '/teacher/admissions', icon: 'users' },
    { label: '个人待办', to: '/todos', icon: 'todo' },
  ],
  MANAGER: [
    { label: '主页面', to: '/home', icon: 'home' },
    { label: '公告', to: '/announcements', icon: 'bell' },
    { label: '阶段管理', to: '/manager/stages', icon: 'calendar' },
    { label: '个人待办', to: '/todos', icon: 'todo' },
  ],
};

const route = useRoute();
const router = useRouter();
const userStore = useUserStore();
const admissionStore = useAdmissionStore();
const loggingOut = ref(false);

const roleLabel = computed(() => {
  const labels = {
    STUDENT: '学生',
    TEACHER: '导师',
    MANAGER: '负责人',
  };
  return labels[userStore.role] || '';
});

const menuItems = computed(() => MENUS[userStore.role] || []);

function isActive(item) {
  return route.path === item.to || route.path.startsWith(`${item.to}/`);
}

function handleMenuClick(item) {
  if (route.path === item.to) {
    return;
  }

  if (item.to === '/student/volunteer') {
    if (admissionStore.admitted) {
      showToast('已被录取！', 'info');
      return;
    }
    if (admissionStore.error) {
      showToast('录取状态加载失败，请重试', 'error');
      return;
    }
    if (!admissionStore.loaded) {
      showToast('正在加载录取状态，请稍候', 'info');
      return;
    }
  }

  if (route.path !== item.to) {
    router.push(item.to);
  }
}

async function handleLogout() {
  if (loggingOut.value) {
    return;
  }

  loggingOut.value = true;
  try {
    await userStore.logout();
  } catch {
    admissionStore.clear();
  } finally {
    admissionStore.clear();
    loggingOut.value = false;
    await router.replace({ name: 'login' });
  }
}

onMounted(() => {
  if (userStore.role === 'STUDENT') {
    admissionStore.fetch().catch(() => {
      // 错误状态由主页面展示；这里只避免未处理的 Promise。
      return undefined;
    });
  }
});
</script>

<style scoped>
.sidebar {
  position: sticky;
  top: 0;
  display: flex;
  flex: 0 0 238px;
  flex-direction: column;
  width: 238px;
  height: 100vh;
  border-right: 1px solid var(--line);
  background: rgba(255, 255, 255, 0.94);
  backdrop-filter: blur(6px);
}

.brand {
  display: flex;
  align-items: center;
  gap: 11px;
  padding: 19px 18px;
  border-bottom: 1px solid var(--line);
}

.brand-mark {
  display: grid;
  flex: 0 0 38px;
  place-items: center;
  width: 38px;
  height: 38px;
  border: 1px solid var(--line2);
  border-radius: 9px;
  background: linear-gradient(135deg, rgba(255, 122, 24, 0.22), rgba(255, 154, 69, 0.07));
  color: var(--cyan);
}

.brand-name {
  font-size: 15px;
  font-weight: 700;
  letter-spacing: 0.4px;
}

.brand-sub {
  margin-top: 2px;
  font-family: var(--tech);
  font-size: 10px;
  letter-spacing: 2px;
  color: var(--text3);
  text-transform: uppercase;
}

.menu {
  flex: 1;
  overflow: auto;
  padding: 12px;
}

.menu-cap {
  padding: 12px 13px 6px;
  font-size: 11px;
  letter-spacing: 1px;
  color: var(--text3);
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 11px;
  width: 100%;
  margin-bottom: 4px;
  padding: 11px 13px;
  border: 1px solid transparent;
  border-radius: 9px;
  background: transparent;
  color: var(--text2);
  font-size: 14px;
  text-align: left;
  cursor: pointer;
  transition: 0.15s;
}

.menu-item:hover {
  color: var(--text);
  background: rgba(255, 154, 69, 0.06);
}

.menu-item.active {
  border-color: rgba(255, 122, 24, 0.22);
  background: rgba(255, 122, 24, 0.1);
  color: var(--cyan);
}

.sidebar-foot {
  padding: 14px 18px;
  border-top: 1px solid var(--line);
  font-size: 11px;
  line-height: 1.8;
  color: var(--text3);
}

.logout-link {
  padding: 0;
  border: 0;
  background: transparent;
  color: var(--text3);
  font-size: 11px;
  cursor: pointer;
}

.logout-link:hover:not(:disabled) {
  color: var(--cyan);
}

.logout-link:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>
