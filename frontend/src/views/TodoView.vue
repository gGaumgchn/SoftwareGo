<template>
  <DefaultLayout title="个人待办">
    <section class="card todo-card">
      <div class="card-head">
        <div class="card-title"><span class="dot"></span>我的待办</div>
        <StatusBadge :type="countType">{{ countText }}</StatusBadge>
      </div>

      <EmptyState v-if="loading" kind="loading" />
      <EmptyState v-else-if="loadError" kind="error" @retry="loadTodos" />
      <EmptyState v-else-if="todos.length === 0" text="暂无待办" />

      <template v-else>
        <div class="todo-list">
          <RouterLink
            v-for="(todo, index) in todos"
            :key="`${todo.type}-${index}`"
            class="todo"
            :to="todo.actionPath"
          >
            <div class="t-ico">
              <BaseIcon :name="todoIcon(todo.type)" :size="20" />
            </div>

            <div class="t-body">
              <div class="t-t">{{ todo.title }}</div>
              <div v-if="todo.description" class="t-desc">{{ todo.description }}</div>
              <div class="t-meta">
                <span v-if="todo.stageName" class="t-stage">{{ todo.stageName }}</span>
                <span class="t-deadline">{{ deadlineText(todo) }}</span>
              </div>
            </div>

            <div class="t-actions">
              <StatusBadge :type="statusType(todo.status)">
                {{ statusText(todo.status) }}
              </StatusBadge>
              <span class="t-go">{{ todo.actionLabel || '前往办理' }}</span>
            </div>
          </RouterLink>
        </div>

        <p class="list-hint">完成对应事项后，待办会自动从列表中移除。</p>
      </template>
    </section>
  </DefaultLayout>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue';
import DefaultLayout from '../layouts/DefaultLayout.vue';
import BaseIcon from '../components/BaseIcon.vue';
import EmptyState from '../components/EmptyState.vue';
import StatusBadge from '../components/StatusBadge.vue';
import { getTodos } from '../api/todo';
import { formatDateTime, setServerTime } from '../utils/time';

const TODO_ICONS = {
  VOLUNTEER_SUBMIT: 'edit',
  VOLUNTEER_MAKEUP: 'edit',
  OFFER_CONFIRM: 'check',
  LAB_INFO: 'building',
  SCORE_ENTRY: 'users',
  GRANT_OFFER: 'users',
  STAGE_CONFIG: 'calendar',
};

const STATUS_META = {
  TODO: { text: '待办理', type: 'cyan' },
};

const loading = ref(true);
const loadError = ref(false);
const todos = ref([]);

let loadSeq = 0;

const countText = computed(() => {
  if (loading.value) return '加载中…';
  if (loadError.value) return '加载失败';
  return todos.value.length > 0 ? `共 ${todos.value.length} 条待办` : '暂无待办';
});

const countType = computed(() => {
  if (loadError.value) return 'red';
  return todos.value.length > 0 ? 'cyan' : 'gray';
});

function todoIcon(type) {
  return TODO_ICONS[type] || 'todo';
}

function statusText(status) {
  return STATUS_META[status]?.text || '待办理';
}

function statusType(status) {
  return STATUS_META[status]?.type || 'cyan';
}

function deadlineText(todo) {
  return todo.deadline ? `截止 ${formatDateTime(todo.deadline)}` : '—';
}

async function loadTodos({ silent = false } = {}) {
  const seq = ++loadSeq;

  if (!silent) {
    loading.value = true;
    loadError.value = false;
  }

  try {
    const data = await getTodos();

    if (seq !== loadSeq) {
      return;
    }

    todos.value = Array.isArray(data?.list) ? data.list : [];
    setServerTime(data?.serverTime);
    loadError.value = false;
  } catch {
    if (seq !== loadSeq) {
      return;
    }
    if (!silent) {
      loadError.value = true;
      todos.value = [];
    }
  } finally {
    if (seq === loadSeq) {
      loading.value = false;
    }
  }
}

function handleVisibilityChange() {
  // 从其他页面/窗口完成事项后返回时，重新拉取，已完成的待办会自动消失。
  if (document.visibilityState === 'visible' && !loading.value) {
    loadTodos({ silent: true });
  }
}

onMounted(() => {
  document.addEventListener('visibilitychange', handleVisibilityChange);
  loadTodos();
});

onUnmounted(() => {
  document.removeEventListener('visibilitychange', handleVisibilityChange);
  loadSeq += 1;
});
</script>

<style scoped>
.todo-card {
  max-width: 1080px;
  padding: 24px 28px;
}

.todo-list {
  display: flex;
  flex-direction: column;
}

.todo {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 12px;
  padding: 16px 14px;
  border: 1px solid var(--line);
  border-radius: 11px;
  background: rgba(250, 250, 252, 0.92);
  color: inherit;
  text-decoration: none;
  transition: 0.15s;
}

.todo:hover {
  border-color: rgba(255, 154, 69, 0.4);
  background: rgba(250, 250, 252, 0.98);
}

.todo:hover .t-go {
  filter: brightness(1.07);
}

.t-ico {
  display: grid;
  flex: 0 0 42px;
  place-items: center;
  width: 42px;
  height: 42px;
  border-radius: 11px;
  background: rgba(255, 122, 24, 0.1);
  color: var(--cyan);
}

.t-body {
  flex: 1;
  min-width: 0;
}

.t-t {
  font-size: 14.5px;
  font-weight: 500;
  color: var(--text);
}

.t-desc {
  margin-top: 4px;
  font-size: 12.5px;
  color: var(--text2);
}

.t-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 4px 16px;
  margin-top: 4px;
  font-size: 12px;
  color: var(--text3);
}

.t-deadline {
  font-family: var(--tech);
  font-size: 12.5px;
}

.t-actions {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  gap: 12px;
}

.t-go {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 8px 16px;
  border: 1px solid rgba(255, 122, 24, 0.5);
  border-radius: 8px;
  background: linear-gradient(180deg, rgba(255, 122, 24, 0.92), rgba(255, 154, 69, 0.78));
  color: #ffffff;
  font-size: 13px;
  font-weight: 600;
  white-space: nowrap;
  transition: 0.15s;
}

.list-hint {
  margin: 10px 0 0;
  font-size: 12px;
  color: var(--text3);
}

@media (max-width: 760px) {
  .todo-card {
    padding: 20px 18px;
  }

  .todo {
    flex-wrap: wrap;
    align-items: flex-start;
  }

  .t-actions {
    width: 100%;
    justify-content: flex-end;
  }
}
</style>
