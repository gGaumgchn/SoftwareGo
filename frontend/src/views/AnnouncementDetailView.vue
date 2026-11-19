<template>
  <DefaultLayout title="公告详情">
    <section class="card detail-card">
      <div class="detail-top">
        <RouterLink class="back-link" :to="{ name: 'announcements' }">← 返回公告列表</RouterLink>
        <StatusBadge type="gray">只读</StatusBadge>
      </div>

      <EmptyState v-if="loading" kind="loading" />
      <EmptyState v-else-if="notFound" kind="empty" text="公告不存在" />
      <EmptyState v-else-if="loadError" kind="error" @retry="loadAnnouncement" />

      <article v-else-if="announcement" class="announcement">
        <div class="title-row">
          <div class="title-icon">
            <BaseIcon name="bell" :size="20" />
          </div>
          <h1 class="detail-title">{{ announcement.title }}</h1>
        </div>

        <div class="detail-meta">
          <span>发布人：{{ announcement.publisherName || '—' }}</span>
          <span>发布时间：{{ formatDateTime(announcement.publishedAt) }}</span>
        </div>

        <div class="divider"></div>

        <div class="detail-content">{{ announcement.content || '—' }}</div>
      </article>
    </section>
  </DefaultLayout>
</template>

<script setup>
import { onUnmounted, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import DefaultLayout from '../layouts/DefaultLayout.vue';
import BaseIcon from '../components/BaseIcon.vue';
import EmptyState from '../components/EmptyState.vue';
import StatusBadge from '../components/StatusBadge.vue';
import { getAnnouncement } from '../api/announcement';
import { formatDateTime } from '../utils/time';

const route = useRoute();

const loading = ref(true);
const loadError = ref(false);
const notFound = ref(false);
const announcement = ref(null);

let loadSeq = 0;

async function loadAnnouncement() {
  const seq = ++loadSeq;
  loading.value = true;
  loadError.value = false;
  notFound.value = false;

  try {
    const data = await getAnnouncement(route.params.id);

    if (seq !== loadSeq) {
      return;
    }

    announcement.value = data || null;
    if (!data) {
      notFound.value = true;
    }
  } catch (error) {
    if (seq !== loadSeq) {
      return;
    }
    announcement.value = null;
    if (error?.response?.status === 404) {
      notFound.value = true;
    } else {
      loadError.value = true;
    }
  } finally {
    if (seq === loadSeq) {
      loading.value = false;
    }
  }
}

watch(() => route.params.id, loadAnnouncement, { immediate: true });

onUnmounted(() => {
  loadSeq += 1;
});
</script>

<style scoped>
.detail-card {
  max-width: 860px;
  padding: 26px 30px 32px;
}

.detail-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 22px;
}

.back-link {
  font-size: 13px;
  color: var(--cyan);
}

.back-link:hover {
  text-decoration: underline;
}

.title-row {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.title-icon {
  display: grid;
  flex: 0 0 40px;
  place-items: center;
  width: 40px;
  height: 40px;
  margin-top: 2px;
  border-radius: 10px;
  background: rgba(255, 154, 69, 0.09);
  color: var(--blue);
}

.detail-title {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  line-height: 1.5;
  color: var(--text);
}

.detail-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px 20px;
  margin-top: 12px;
  font-size: 12.5px;
  color: var(--text3);
}

.divider {
  height: 1px;
  margin: 20px 0;
  background: var(--line);
}

.detail-content {
  font-size: 14px;
  line-height: 1.9;
  color: var(--text2);
  white-space: pre-wrap;
  word-break: break-word;
}

@media (max-width: 760px) {
  .detail-card {
    padding: 20px 18px 26px;
  }

  .detail-title {
    font-size: 18px;
  }
}
</style>
