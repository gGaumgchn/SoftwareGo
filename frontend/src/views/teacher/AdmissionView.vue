<template>
  <DefaultLayout title="申请与录取管理 · 发放拟录取">
    <section class="card admission-card">
      <div class="card-head">
        <div class="card-title"><span class="dot"></span>本轮申请 · {{ roundTitle }}</div>
        <StatusBadge :type="roundBadgeType">{{ roundBadgeText }}</StatusBadge>
      </div>

      <EmptyState v-if="loading" kind="loading" />
      <EmptyState
        v-else-if="loadError"
        kind="error"
        :error-text="loadErrorMessage"
        @retry="loadPage"
      />

      <template v-else>
        <div v-if="summary && !summary.labSubmitted" class="warning-banner">
          <BaseIcon name="alert-circle" :size="18" class="warning-icon" />
          <span>请先提交实训室信息，提交后才能发放拟录取。</span>
          <RouterLink class="warning-link" to="/teacher/lab">去提交</RouterLink>
        </div>

        <div class="stat-row">
          <div class="stat">
            <div class="s-k">计划招生名额</div>
            <div class="s-v cyan">{{ displayNumber(summary?.plannedQuota) }}</div>
          </div>
          <div class="stat">
            <div class="s-k">已确认</div>
            <div class="s-v green">{{ displayNumber(summary?.admittedCount) }}</div>
          </div>
          <div class="stat">
            <div class="s-k">待确认</div>
            <div class="s-v amber">{{ displayNumber(summary?.pendingCount) }}</div>
          </div>
          <div class="stat">
            <div class="s-k">剩余可发放</div>
            <div class="s-v gray">{{ displayNumber(summary?.remainingQuota) }}</div>
          </div>
        </div>

        <div class="toolbar">
          <div class="field">
            <label class="label" for="cutoffScore">录取分数线</label>
            <input
              id="cutoffScore"
              v-model.trim="cutoffScore"
              class="input"
              type="number"
              min="0"
              max="100"
              step="0.01"
              placeholder="请输入分数线"
              :disabled="granting"
            />
          </div>
          <button class="btn" type="button" @click="sortRowsWithToast">按分数排序</button>
          <button
            class="btn btn-primary"
            type="button"
            :disabled="grantButtonDisabled"
            @click="handleGrant"
          >
            {{ granting ? '发放中…' : '发放拟录取' }}
          </button>
        </div>

        <EmptyState v-if="rows.length === 0" kind="empty" text="本轮暂无申请" />

        <div v-else class="table-wrap">
          <table>
            <thead>
              <tr>
                <th style="width: 40px">序</th>
                <th style="width: 150px">学工号</th>
                <th style="width: 110px">姓名</th>
                <th style="width: 140px">面试分数</th>
                <th style="width: 120px">同分顺序</th>
                <th style="width: 120px">状态</th>
                <th style="width: 100px">简历</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(row, index) in rows" :key="row.targetId">
                <td class="index-cell">{{ index + 1 }}</td>
                <td><strong>{{ row.staffNo }}</strong></td>
                <td>{{ row.studentName || row.staffNo }}</td>
                <td>
                  <div v-if="row.scoreEditable" class="score-editor">
                    <input
                      v-model="row.scoreInput"
                      class="input input-sm"
                      type="number"
                      min="0"
                      max="100"
                      step="0.01"
                      placeholder="未评分"
                      :disabled="!canScore || row.scoreSaving"
                      @change="saveScore(row)"
                    />
                    <span v-if="row.scoreSaving" class="inline-hint">保存中…</span>
                  </div>
                  <strong v-else>{{ formatScore(row.score) || '—' }}</strong>
                </td>
                <td>
                  <div v-if="isTieRow(row)" class="order-editor">
                    <input
                      v-if="row.tieOrderEditable"
                      v-model="row.tieOrderInput"
                      class="input input-order"
                      type="number"
                      min="1"
                      step="1"
                      placeholder="顺序"
                      :disabled="!canScore || row.orderSaving"
                      @change="saveTieOrder(row)"
                    />
                    <strong v-else>{{ row.tieOrder ?? '—' }}</strong>
                    <span v-if="row.orderSaving" class="inline-hint">保存中…</span>
                  </div>
                  <span v-else>—</span>
                </td>
                <td>
                  <StatusBadge :type="offerBadge(row).type">{{ offerBadge(row).text }}</StatusBadge>
                </td>
                <td>
                  <button
                    class="btn btn-sm"
                    type="button"
                    :disabled="!row.resumeFileId"
                    @click="openResume(row)"
                  >
                    查看
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <div class="help">
          规则：分数从高到低排序，分数大于等于分数线者按顺序发放，直至名额占满；同分学生需手动确定先后顺序；未评分学生不参与发放。已确认与待确认会占用名额，放弃/超时后未截止前可继续递补。
        </div>
      </template>
    </section>
  </DefaultLayout>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue';
import DefaultLayout from '../../layouts/DefaultLayout.vue';
import BaseIcon from '../../components/BaseIcon.vue';
import StatusBadge from '../../components/StatusBadge.vue';
import EmptyState from '../../components/EmptyState.vue';
import {
  getTeacherAdmissions,
  getTeacherApplications,
  grantOffers,
  updateApplicationOrder,
  updateApplicationScore,
} from '../../api/admission';
import { getFileUrl } from '../../api/file';
import { formatDateTime } from '../../utils/time';
import { showToast } from '../../utils/toast';

const loading = ref(true);
const loadError = ref(false);
const loadErrorMessage = ref('');
const granting = ref(false);
const canScore = ref(false);
const cutoffScore = ref('');
const summary = ref(null);
const rows = ref([]);

const roundTitle = computed(() => {
  const labels = {
    1: '第一轮录取',
    2: '第二轮录取',
    3: '第三轮录取',
  };
  return labels[summary.value?.roundNo] || '当前轮次';
});

const roundBadgeText = computed(() => {
  if (loading.value) return '加载中';
  if (loadError.value) return '加载失败';

  const data = summary.value;
  if (!data) return '—';
  if (data.roundState === 'OPEN') return `发放中 · 截止 ${formatDateTime(data.roundEnd)}`;
  if (data.roundState === 'NOT_STARTED') return '尚未开始';
  if (data.roundState === 'ENDED') return '已结束';
  return '当前不在录取轮次';
});

const roundBadgeType = computed(() => {
  if (loadError.value) return 'red';
  if (summary.value?.roundState === 'OPEN') return 'cyan';
  if (summary.value?.roundState === 'NOT_STARTED') return 'amber';
  return 'gray';
});

const rowSaving = computed(() =>
  rows.value.some((row) => row.scoreSaving || row.orderSaving),
);

const grantButtonDisabled = computed(
  () => granting.value || rowSaving.value || !summary.value?.canGrant,
);

const scoreGroupSizes = computed(() => {
  const counts = new Map();
  rows.value.forEach((row) => {
    if (row.score == null) return;
    const key = String(Number(row.score));
    counts.set(key, (counts.get(key) || 0) + 1);
  });
  return counts;
});

function parseScore(value) {
  const text = String(value ?? '').trim();
  if (!text) return null;
  if (!/^\d{1,3}(\.\d{1,2})?$/.test(text)) return null;

  const number = Number(text);
  if (!Number.isFinite(number) || number < 0 || number > 100) return null;
  return number;
}

function parseTieOrder(value) {
  const number = Number(value);
  return Number.isInteger(number) && number > 0 ? number : null;
}

function formatScore(value) {
  if (value == null || value === '') return '';
  return String(Number(value));
}

function displayNumber(value) {
  return value == null ? '—' : value;
}

function offerBadge(row) {
  if (row.offerStatus === 'CONFIRMED') return { text: '已确认', type: 'green' };
  if (row.studentAdmitted) return { text: '已有录取', type: 'gray' };
  if (row.offerStatus === 'PENDING') return { text: '待确认', type: 'amber' };
  if (row.offerStatus === 'ABANDONED') return { text: '已放弃', type: 'red' };
  return { text: '未发放', type: 'gray' };
}

function isTieRow(row) {
  if (row.score == null) return false;
  return (scoreGroupSizes.value.get(String(Number(row.score))) || 0) > 1;
}

function normalizeRow(item) {
  return {
    ...item,
    scoreInput: formatScore(item.score),
    tieOrderInput: item.tieOrder != null ? String(item.tieOrder) : '',
    scoreSaving: false,
    orderSaving: false,
  };
}

function compareRows(a, b) {
  const aScore = a.score == null ? -1 : Number(a.score);
  const bScore = b.score == null ? -1 : Number(b.score);
  if (aScore !== bScore) return bScore - aScore;

  const aOrder = a.tieOrder ?? Number.MAX_SAFE_INTEGER;
  const bOrder = b.tieOrder ?? Number.MAX_SAFE_INTEGER;
  if (aOrder !== bOrder) return aOrder - bOrder;

  return a.targetId - b.targetId;
}

function sortRows() {
  rows.value = [...rows.value].sort(compareRows);
}

function sortRowsWithToast() {
  sortRows();
  showToast('已按面试分数从高到低排序', 'success');
}

function getErrorMessage(error, fallback) {
  const message = error?.message;
  return typeof message === 'string' && /[一-龥]/.test(message) ? message : fallback;
}

async function loadPage() {
  loading.value = true;
  loadError.value = false;
  loadErrorMessage.value = '';

  try {
    const [summaryData, applicationsData] = await Promise.all([
      getTeacherAdmissions(),
      getTeacherApplications(),
    ]);

    summary.value = summaryData || null;
    canScore.value = Boolean(applicationsData?.canScore);
    rows.value = (applicationsData?.list || []).map(normalizeRow);
    sortRows();
  } catch (error) {
    summary.value = null;
    rows.value = [];
    canScore.value = false;
    loadError.value = true;
    loadErrorMessage.value = getErrorMessage(error, '加载失败，请重试');
  } finally {
    loading.value = false;
  }
}

async function saveScore(row) {
  if (row.scoreSaving || !row.scoreEditable) {
    return;
  }

  const score = parseScore(row.scoreInput);
  if (score == null) {
    showToast('请填写有效分数', 'error');
    row.scoreInput = formatScore(row.score);
    return;
  }

  if (row.score != null && Number(row.score) === score) {
    row.scoreInput = formatScore(score);
    return;
  }

  row.scoreSaving = true;

  try {
    const data = await updateApplicationScore(row.targetId, score);
    row.score = data?.score ?? score;
    row.tieOrder = data?.tieOrder ?? null;
    row.tieOrderInput = data?.tieOrder != null ? String(data.tieOrder) : '';
    row.scoredAt = data?.scoredAt || row.scoredAt;
    row.tieOrderEditable = true;
    row.scoreInput = formatScore(row.score);
    sortRows();
  } catch {
    row.scoreInput = formatScore(row.score);
    // 真实接口错误由 request.js 统一提示。
  } finally {
    row.scoreSaving = false;
  }
}

async function saveTieOrder(row) {
  if (row.orderSaving || !row.tieOrderEditable) {
    return;
  }

  const order = parseTieOrder(row.tieOrderInput);
  if (order == null) {
    showToast('同分顺序不能重复', 'error');
    row.tieOrderInput = row.tieOrder != null ? String(row.tieOrder) : '';
    return;
  }

  const duplicate = rows.value.some(
    (item) =>
      item.targetId !== row.targetId &&
      item.score != null &&
      Number(item.score) === Number(row.score) &&
      parseTieOrder(item.tieOrderInput || item.tieOrder) === order,
  );

  if (duplicate) {
    showToast('同分顺序不能重复', 'error');
    row.tieOrderInput = row.tieOrder != null ? String(row.tieOrder) : '';
    return;
  }

  row.orderSaving = true;

  try {
    const data = await updateApplicationOrder(row.targetId, order);
    row.tieOrder = data?.tieOrder ?? order;
    row.tieOrderInput = String(row.tieOrder);
    sortRows();
  } catch {
    row.tieOrderInput = row.tieOrder != null ? String(row.tieOrder) : '';
    // 真实接口错误由 request.js 统一提示。
  } finally {
    row.orderSaving = false;
  }
}

async function handleGrant() {
  if (granting.value) {
    return;
  }

  const cutoff = parseScore(cutoffScore.value);
  if (cutoff == null) {
    showToast('请填写有效分数', 'error');
    return;
  }

  if (!summary.value?.canGrant) {
    return;
  }

  if (rowSaving.value) {
    showToast('正在保存分数或顺序，请稍候', 'info');
    return;
  }

  granting.value = true;

  try {
    const data = await grantOffers({ cutoffScore: cutoff });
    const count = data?.grantedCount ?? 0;
    const message = data?.message || '发放成功';
    showToast(count > 0 ? `${message}，共 ${count} 名` : message, 'success');
    await loadPage();
  } catch {
    // 真实接口错误由 request.js 统一提示。
  } finally {
    granting.value = false;
  }
}

function openResume(row) {
  if (!row.resumeFileId) {
    return;
  }

  // 走鉴权下载接口；浏览器携带当前会话 Cookie。
  window.open(getFileUrl(row.resumeFileId), '_blank', 'noopener,noreferrer');
}

onMounted(loadPage);
</script>

<style scoped>
.admission-card {
  padding: 22px 24px;
}

.warning-banner {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 20px;
  padding: 12px 16px;
  border: 1px solid rgba(251, 191, 36, 0.35);
  border-radius: 10px;
  background: rgba(251, 191, 36, 0.06);
  font-size: 13px;
  color: var(--text2);
}

.warning-icon {
  flex: 0 0 auto;
  color: var(--amber);
}

.warning-link {
  margin-left: auto;
  color: var(--cyan);
  font-size: 12.5px;
}

.warning-link:hover {
  text-decoration: underline;
}

.stat-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
  margin-bottom: 20px;
}

.stat {
  padding: 14px 16px;
  border: 1px solid var(--line);
  border-radius: 11px;
  background: rgba(250, 250, 252, 0.92);
}

.s-k {
  font-size: 12px;
  color: var(--text3);
}

.s-v {
  margin-top: 3px;
  font-family: var(--tech);
  font-size: 24px;
  font-weight: 700;
}

.s-v.cyan {
  color: var(--cyan);
}

.s-v.green {
  color: var(--green);
}

.s-v.amber {
  color: var(--amber);
}

.s-v.gray {
  color: var(--text2);
}

.toolbar {
  display: flex;
  align-items: flex-end;
  gap: 14px;
  flex-wrap: wrap;
  margin-bottom: 20px;
}

.field {
  margin-bottom: 0;
}

.label {
  display: block;
  margin-bottom: 7px;
  font-size: 12.5px;
  color: var(--text2);
}

.input {
  width: 150px;
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

.input:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.input-sm {
  width: 96px;
  padding: 8px 10px;
  text-align: center;
}

.input-order {
  width: 88px;
  padding: 8px 10px;
  text-align: center;
}

.btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  padding: 10px 20px;
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

.btn-primary {
  border-color: rgba(255, 122, 24, 0.5);
  background: linear-gradient(180deg, rgba(255, 122, 24, 0.92), rgba(255, 154, 69, 0.78));
  color: #ffffff;
  font-weight: 600;
}

.btn-primary:hover:not(:disabled) {
  filter: brightness(1.07);
}

.btn-sm {
  padding: 6px 12px;
  font-size: 12.5px;
}

.table-wrap {
  overflow-x: auto;
}

table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13.5px;
}

th {
  padding: 10px 12px;
  border-bottom: 1px solid var(--line);
  color: var(--text3);
  font-size: 12px;
  font-weight: 500;
  letter-spacing: 0.4px;
  text-align: left;
  white-space: nowrap;
}

td {
  padding: 12px;
  border-bottom: 1px solid rgba(255, 154, 69, 0.06);
  color: var(--text2);
  vertical-align: middle;
}

tbody tr:hover {
  background: rgba(255, 154, 69, 0.04);
}

td strong {
  color: var(--text);
  font-weight: 500;
}

.index-cell {
  color: var(--text3);
}

.score-editor,
.order-editor {
  display: flex;
  align-items: center;
  gap: 6px;
}

.inline-hint {
  color: var(--text3);
  font-size: 11px;
  white-space: nowrap;
}

.help {
  margin-top: 12px;
  font-size: 12px;
  color: var(--text3);
}

@media (max-width: 1000px) {
  .stat-row {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 700px) {
  .admission-card {
    padding: 18px 16px;
  }

  .stat-row {
    grid-template-columns: 1fr;
  }

  .toolbar .input {
    width: 100%;
  }

  .toolbar {
    align-items: stretch;
  }
}
</style>
