<template>
  <DefaultLayout title="公告">
    <section class="card announcement-card">
      <div class="card-head">
        <div class="card-title"><span class="dot"></span>公告列表</div>
        <StatusBadge type="gray">只读 · 发布后不可修改或删除</StatusBadge>
      </div>

      <EmptyState v-if="loading" kind="loading" />
      <EmptyState v-else-if="loadError" kind="error" @retry="loadAnnouncements" />
      <EmptyState v-else-if="announcements.length === 0" text="暂无公告" />

      <template v-else>
        <div class="notice-list">
          <RouterLink
            v-for="notice in announcements"
            :key="notice.id"
            class="notice"
            :to="{ name: 'announcement-detail', params: { id: notice.id } }"
          >
            <div class="n-ico">
              <BaseIcon name="bell" :size="17" />
            </div>
            <div class="n-body">
              <div class="n-t">{{ notice.title }}</div>
              <div class="n-s">{{ notice.summary || '—' }}</div>
              <div class="n-time">{{ formatDateTime(notice.publishedAt) }}</div>
            </div>
            <span class="n-arrow" aria-hidden="true">›</span>
          </RouterLink>
        </div>

        <div v-if="totalPages > 1" class="pager">
          <button
            class="btn btn-sm"
            type="button"
            :disabled="page <= 1"
            @click="changePage(page - 1)"
          >
            上一页
          </button>
          <span class="pager-text">第 {{ page }} / {{ totalPages }} 页</span>
          <button
            class="btn btn-sm"
            type="button"
            :disabled="page >= totalPages"
            @click="changePage(page + 1)"
          >
            下一页
          </button>
        </div>
      </template>
    </section>
  </DefaultLayout>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue';
import DefaultLayout from '../layouts/DefaultLayout.vue';
import BaseIcon from '../components/BaseIcon.vue';
import EmptyState from '../components/EmptyState.vue';
import StatusBadge from '../components/StatusBadge.vue';
import { getAnnouncements } from '../api/announcement';
import { formatDateTime } from '../utils/time';

const PAGE_SIZE = 10;

const loading = ref(true);
const loadError = ref(false);
const announcements = ref([]);
const total = ref(0);
const page = ref(1);

let loadSeq = 0;

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / PAGE_SIZE)));

async function loadAnnouncements() {
  const seq = ++loadSeq;
  loading.value = true;
  loadError.value = false;

  try {
    const data = await getAnnouncements({ page: page.value, pageSize: PAGE_SIZE });

    if (seq !== loadSeq) {
      return;
    }

    announcements.value = Array.isArray(data?.list) ? data.list : [];
    total.value = Number(data?.total ?? 0);
  } catch {
    if (seq !== loadSeq) {
      return;
    }
    loadError.value = true;
    announcements.value = [];
    total.value = 0;
  } finally {
    if (seq === loadSeq) {
      loading.value = false;
    }
  }
}

function changePage(nextPage) {
  const target = Math.min(Math.max(1, nextPage), totalPages.value);
  if (target === page.value) {
    return;
  }
  page.value = target;
  loadAnnouncements();
}

onMounted(loadAnnouncements);
</script>

<style scoped>
.announcement-card {
  max-width: 1080px;
  padding: 24px 28px;
}

.notice-list {
  display: flex;
  flex-direction: column;
}

.notice {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  padding: 16px 6px;
  border-bottom: 1px solid rgba(255, 154, 69, 0.07);
  color: inherit;
  text-decoration: none;
  transition: 0.15s;
}

.notice:last-child {
  border-bottom: 0;
}

.notice:hover {
  background: rgba(255, 154, 69, 0.03);
}

.notice:hover .n-t,
.notice:hover .n-arrow {
  color: var(--cyan);
}

.n-ico {
  display: grid;
  flex: 0 0 36px;
  place-items: center;
  width: 36px;
  height: 36px;
  margin-top: 1px;
  border-radius: 9px;
  background: rgba(255, 154, 69, 0.09);
  color: var(--blue);
}

.n-body {
  flex: 1;
  min-width: 0;
}

.n-t {
  font-size: 14.5px;
  font-weight: 500;
  color: var(--text);
  transition: 0.15s;
}

.n-s {
  margin-top: 3px;
  font-size: 13px;
  color: var(--text2);
}

.n-time {
  margin-top: 4px;
  font-family: var(--tech);
  font-size: 11.5px;
  color: var(--text3);
}

.n-arrow {
  align-self: center;
  font-size: 22px;
  line-height: 1;
  color: var(--text3);
  transition: 0.15s;
}

.pager {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 20px;
}

.pager-text {
  font-size: 12.5px;
  color: var(--text2);
}

.btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  padding: 9px 20px;
  border: 1px solid var(--line2);
  border-radius: 8px;
  background: var(--panel2);
  color: var(--text);
  font-size: 13.5px;
  font-weight: 500;
  cursor: pointer;
  transition: 0.15s;
}

.btn:hover:not(:disabled) {
  border-color: rgba(255, 154, 69, 0.5);
  background: #fff1e3;
}

.btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.btn-sm {
  padding: 7px 14px;
  font-size: 13px;
}

@media (max-width: 760px) {
  .announcement-card {
    padding: 20px 18px;
  }

  .notice {
    padding: 14px 2px;
  }

  .pager {
    justify-content: center;
  }
}
</style>
