<template>
  <DefaultLayout title="查看实训室信息">
    <section class="card labs-card">
      <div class="card-head">
        <div class="card-title"><span class="dot"></span>实训室一览</div>
        <StatusBadge type="cyan">{{ countText }}</StatusBadge>
      </div>

      <div class="toolbar">
        <input
          v-model="keyword"
          class="input"
          type="text"
          placeholder="搜索实训室 / 导师 / 研究方向"
          autocomplete="off"
          maxlength="64"
          @input="handleKeywordInput"
        />
        <select v-model="directionFilter" class="select" @change="handleDirectionChange">
          <option value="">全部研究方向</option>
          <option v-for="direction in directions" :key="direction" :value="direction">
            {{ direction }}
          </option>
        </select>
      </div>

      <EmptyState v-if="loading" kind="loading" />
      <EmptyState v-else-if="loadError" kind="error" @retry="loadLabs" />
      <EmptyState v-else-if="labs.length === 0" :text="emptyText" />

      <template v-else>
        <div class="lab-grid">
          <button
            v-for="lab in labs"
            :key="lab.id"
            class="lab-card"
            type="button"
            @click="openLab(lab)"
          >
            <div class="lab-top">
              <div class="lab-ico">
                <BaseIcon name="building" :size="20" />
              </div>
              <StatusBadge type="cyan">
                计划 {{ lab.plannedQuota != null ? lab.plannedQuota : '—' }} 人
              </StatusBadge>
            </div>
            <div class="lab-name">{{ lab.labName }}</div>
            <div class="lab-meta">
              导师 {{ lab.teacherName || '—' }} · {{ lab.researchDirection || '—' }}
            </div>
            <div class="lab-info">
              <div>
                <span>{{ lab.location || '—' }}</span>
              </div>
              <div>研究方向：{{ lab.researchDirection || '—' }}</div>
            </div>
          </button>
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

    <BaseModal :open="Boolean(activeLab)" aria-label="实训室详情" @close="closeDetail">
      <div v-if="modalLab" class="modal-content">
        <h3 class="modal-title">{{ modalLab.labName || '实训室详情' }}</h3>
        <div class="modal-sub">
          {{
            modalLab.researchDirection
              ? `研究方向：${modalLab.researchDirection}`
              : '实训室公开信息'
          }}
        </div>

        <p v-if="detailLoading" class="detail-tip">正在加载实训室详情…</p>
        <div v-else-if="detailError" class="detail-error">
          <span>实训室详情加载失败，当前展示列表信息。</span>
          <button class="link-btn" type="button" @click="retryDetail">重试</button>
        </div>

        <h4 class="m-section">实训室信息</h4>
        <dl class="dl">
          <dt>实训室位置</dt>
          <dd>{{ modalLab.location || '—' }}</dd>
          <dt>研究方向</dt>
          <dd>{{ modalLab.researchDirection || '—' }}</dd>
          <dt>计划招收</dt>
          <dd>{{ quotaText }}</dd>
        </dl>

        <h4 class="m-section">导师信息</h4>
        <dl class="dl">
          <dt>负责导师</dt>
          <dd>{{ modalLab.teacherName || '—' }}</dd>
          <dt>联系方式</dt>
          <dd>{{ modalLab.contact || '—' }}</dd>
          <dt>导师简历</dt>
          <dd>{{ resumeText }}</dd>
          <template v-if="modalLab.updatedAt">
            <dt>更新时间</dt>
            <dd>{{ formatDateTime(modalLab.updatedAt) }}</dd>
          </template>
        </dl>

        <div class="m-actions">
          <button class="btn" type="button" @click="closeDetail">关闭</button>
          <button
            class="btn btn-primary"
            type="button"
            :disabled="!modalLab.resumeFileId"
            @click="openResume"
          >
            查看导师简历（PDF）
          </button>
        </div>
      </div>
    </BaseModal>
  </DefaultLayout>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue';
import DefaultLayout from '../../layouts/DefaultLayout.vue';
import BaseIcon from '../../components/BaseIcon.vue';
import BaseModal from '../../components/BaseModal.vue';
import EmptyState from '../../components/EmptyState.vue';
import StatusBadge from '../../components/StatusBadge.vue';
import { getLab, getLabs } from '../../api/lab';
import { getFileUrl } from '../../api/file';
import { formatDateTime } from '../../utils/time';

const PAGE_SIZE = 9;
const SEARCH_DEBOUNCE = 300;

const loading = ref(true);
const loadError = ref(false);
const labs = ref([]);
const total = ref(0);
const directions = ref([]);
const keyword = ref('');
const directionFilter = ref('');
const page = ref(1);

const activeLab = ref(null);
const detail = ref(null);
const detailLoading = ref(false);
const detailError = ref(false);

let listSeq = 0;
let detailSeq = 0;
let keywordTimer = null;

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / PAGE_SIZE)));

const hasFilter = computed(() => Boolean(keyword.value.trim() || directionFilter.value));

const countText = computed(() => {
  if (loading.value) return '加载中…';
  if (loadError.value) return '加载失败';
  return `共 ${total.value} 个实训室`;
});

const emptyText = computed(() => (hasFilter.value ? '没有匹配的实训室' : '暂无实训室信息'));

const modalLab = computed(() => {
  if (!activeLab.value) {
    return null;
  }
  return { ...activeLab.value, ...(detail.value || {}) };
});

const quotaText = computed(() =>
  modalLab.value?.plannedQuota != null ? `${modalLab.value.plannedQuota} 人` : '—',
);

const resumeText = computed(() => {
  if (modalLab.value?.resumeFileName) {
    return modalLab.value.resumeFileName;
  }
  return modalLab.value?.resumeFileId ? '已上传' : '暂未上传';
});

async function loadLabs() {
  const seq = ++listSeq;
  loading.value = true;
  loadError.value = false;

  try {
    const data = await getLabs({
      keyword: keyword.value.trim(),
      researchDirection: directionFilter.value,
      page: page.value,
      pageSize: PAGE_SIZE,
    });

    if (seq !== listSeq) {
      return;
    }

    labs.value = Array.isArray(data?.list) ? data.list : [];
    total.value = Number(data?.total ?? 0);
    if (Array.isArray(data?.researchDirections)) {
      directions.value = data.researchDirections;
    }
  } catch {
    if (seq !== listSeq) {
      return;
    }
    loadError.value = true;
    labs.value = [];
    total.value = 0;
  } finally {
    if (seq === listSeq) {
      loading.value = false;
    }
  }
}

function clearKeywordTimer() {
  if (keywordTimer) {
    window.clearTimeout(keywordTimer);
    keywordTimer = null;
  }
}

function handleKeywordInput() {
  clearKeywordTimer();
  keywordTimer = window.setTimeout(() => {
    keywordTimer = null;
    page.value = 1;
    loadLabs();
  }, SEARCH_DEBOUNCE);
}

function handleDirectionChange() {
  clearKeywordTimer();
  page.value = 1;
  loadLabs();
}

function changePage(nextPage) {
  const target = Math.min(Math.max(1, nextPage), totalPages.value);
  if (target === page.value) {
    return;
  }
  page.value = target;
  loadLabs();
}

async function openLab(lab) {
  activeLab.value = lab;
  detail.value = null;
  detailLoading.value = true;
  detailError.value = false;

  const seq = ++detailSeq;

  try {
    const data = await getLab(lab.id);
    if (seq !== detailSeq) {
      return;
    }
    detail.value = data || null;
  } catch {
    if (seq !== detailSeq) {
      return;
    }
    detailError.value = true;
  } finally {
    if (seq === detailSeq) {
      detailLoading.value = false;
    }
  }
}

function closeDetail() {
  detailSeq += 1;
  activeLab.value = null;
  detail.value = null;
  detailLoading.value = false;
  detailError.value = false;
}

function retryDetail() {
  if (activeLab.value) {
    openLab(activeLab.value);
  }
}

function openResume() {
  if (!modalLab.value?.resumeFileId) {
    return;
  }
  window.open(getFileUrl(modalLab.value.resumeFileId), '_blank', 'noopener,noreferrer');
}

onMounted(loadLabs);

onUnmounted(() => {
  clearKeywordTimer();
  listSeq += 1;
  detailSeq += 1;
});
</script>

<style scoped>
.labs-card {
  padding: 24px 26px;
}

.toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 20px;
}

.input {
  width: 260px;
  padding: 9px 12px;
  border: 1px solid var(--line);
  border-radius: 8px;
  outline: none;
  background: rgba(255, 255, 255, 0.9);
  color: var(--text);
  font-size: 14px;
  transition: 0.15s;
}

.input:focus {
  border-color: var(--line2);
  box-shadow: 0 0 0 3px rgba(255, 122, 24, 0.12);
}

.select {
  width: 200px;
  padding: 9px 12px;
  border: 1px solid var(--line);
  border-radius: 8px;
  outline: none;
  background: rgba(255, 255, 255, 0.9);
  color: var(--text);
  font-size: 14px;
}

.select:focus {
  border-color: var(--line2);
}

.lab-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 18px;
}

.lab-card {
  display: block;
  width: 100%;
  padding: 18px;
  border: 1px solid var(--line);
  border-radius: 12px;
  background: rgba(250, 250, 252, 0.92);
  color: inherit;
  font-family: inherit;
  text-align: left;
  cursor: pointer;
  transition: 0.15s;
}

.lab-card:hover {
  border-color: rgba(255, 154, 69, 0.4);
  background: rgba(250, 250, 252, 0.96);
  transform: translateY(-2px);
}

.lab-card:focus-visible {
  border-color: var(--line2);
  outline: 2px solid rgba(255, 122, 24, 0.28);
  outline-offset: 2px;
}

.lab-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 12px;
}

.lab-ico {
  display: grid;
  flex: 0 0 40px;
  place-items: center;
  width: 40px;
  height: 40px;
  border-radius: 10px;
  background: rgba(255, 154, 69, 0.1);
  color: var(--cyan);
}

.lab-name {
  font-size: 15.5px;
  font-weight: 600;
  color: var(--text);
}

.lab-meta {
  margin-top: 3px;
  font-size: 12.5px;
  color: var(--text2);
}

.lab-info {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid rgba(255, 154, 69, 0.08);
  font-size: 12.5px;
  line-height: 1.9;
  color: var(--text3);
}

.lab-info span {
  color: var(--text2);
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

.modal-title {
  margin: 0 0 4px;
  font-size: 18px;
  font-weight: 600;
  color: var(--text);
}

.modal-sub {
  margin-bottom: 18px;
  font-size: 12.5px;
  color: var(--text3);
}

.detail-tip {
  margin: 0 0 14px;
  font-size: 12.5px;
  color: var(--text3);
}

.detail-error {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 14px;
  padding: 10px 12px;
  border: 1px solid rgba(248, 113, 113, 0.3);
  border-radius: 8px;
  background: rgba(248, 113, 113, 0.06);
  font-size: 12.5px;
  color: var(--red);
}

.link-btn {
  padding: 0;
  border: 0;
  background: transparent;
  color: var(--cyan);
  font-size: 12.5px;
  cursor: pointer;
}

.link-btn:hover {
  text-decoration: underline;
}

.m-section {
  margin: 18px 0 10px;
  font-size: 13px;
  font-weight: 600;
  color: var(--text);
}

.dl {
  display: grid;
  grid-template-columns: 92px 1fr;
  gap: 9px 14px;
  margin: 0;
  font-size: 13.5px;
}

.dl dt {
  color: var(--text3);
}

.dl dd {
  margin: 0;
  color: var(--text2);
  word-break: break-word;
}

.m-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 24px;
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

.btn-primary {
  border-color: rgba(255, 122, 24, 0.5);
  background: linear-gradient(180deg, rgba(255, 122, 24, 0.92), rgba(255, 154, 69, 0.78));
  color: #ffffff;
  font-weight: 600;
}

.btn-primary:hover:not(:disabled) {
  filter: brightness(1.07);
}

@media (max-width: 1100px) {
  .lab-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 760px) {
  .labs-card {
    padding: 20px 18px;
  }

  .input,
  .select {
    width: 100%;
  }

  .lab-grid {
    grid-template-columns: 1fr;
  }

  .pager {
    justify-content: center;
  }
}
</style>
