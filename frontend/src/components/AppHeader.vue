<template>
  <header class="topbar">
    <div class="topbar-title">{{ title }}</div>

    <div class="user">
      <div class="avatar">{{ avatarText }}</div>
      <div class="user-meta">
        <div class="uname">{{ userStore.displayName || '—' }}</div>
        <div class="urole">{{ roleLabel }}</div>
      </div>
    </div>
  </header>
</template>

<script setup>
import { computed } from 'vue';
import { useUserStore } from '../stores/user';

defineProps({
  title: {
    type: String,
    default: '',
  },
});

const userStore = useUserStore();

const roleLabel = computed(() => {
  const labels = {
    STUDENT: '学生',
    TEACHER: '导师',
    MANAGER: '负责人',
  };
  return labels[userStore.role] || '';
});

const avatarText = computed(() => (userStore.displayName || '—').charAt(0));
</script>

<style scoped>
.topbar {
  display: flex;
  flex: 0 0 64px;
  align-items: center;
  justify-content: space-between;
  height: 64px;
  padding: 0 28px;
  border-bottom: 1px solid var(--line);
  background: rgba(255, 255, 255, 0.82);
  backdrop-filter: blur(6px);
}

.topbar-title {
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 0.5px;
}

.user {
  display: flex;
  align-items: center;
  gap: 10px;
}

.avatar {
  display: grid;
  place-items: center;
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--cyan), var(--blue));
  color: #ffffff;
  font-size: 14px;
  font-weight: 700;
}

.user-meta {
  line-height: 1.2;
}

.uname {
  font-size: 13px;
  font-weight: 600;
}

.urole {
  font-size: 11px;
  color: var(--text3);
}
</style>
