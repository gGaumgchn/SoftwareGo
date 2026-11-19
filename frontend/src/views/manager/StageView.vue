<template>
  <DefaultLayout title="阶段管理">
    <section class="card stage-card">
      <div class="card-head">
        <div class="card-title"><span class="dot"></span>配置选拔阶段时间</div>
        <div class="head-meta">
          <span v-if="currentStageName" class="current-stage">当前阶段：{{ currentStageName }}</span>
          <StatusBadge type="cyan">保存后立即生效</StatusBadge>
        </div>
      </div>

      <EmptyState v-if="loading" kind="loading" />
      <EmptyState v-else-if="loadError" kind="error" @retry="loadPage" />

      <template v-else>
        <div v-if="failedChangeNo" class="retry-banner">
          <div class="retry-icon">
            <BaseIcon name="alert-circle" :size="18" />
          </div>
          <div class="retry-text">
            <strong>公告发布失败</strong>
            <span>阶段已保存，可仅重试发布公告，不会重复保存阶段。</span>
          </div>
          <button
            class="btn btn-retry"
            type="button"
            :disabled="retrying"
            @click="handleRetryAnnouncement"
          >
            {{ retrying ? '发布中…' : '重试发布公告' }}
          </button>
        </div>

        <div class="stage-list">
          <template v-for="(stage, index) in formStages" :key="stage.stageCode">
            <div v-if="stage.group" class="group-label">三轮录取（展开为三轮，分别配置时间）</div>

            <div class="stage-row" :class="{ conflict: conflictIndexes.includes(index) }">
              <div class="stage-name">
                {{ stage.stageName }}
                <small>{{ stage.sub }}</small>
              </div>

              <div>
                <input
                  v-model="stage.startAt"
                  class="input"
                  :class="{ err: isFieldError(index, 'startAt') }"
                  type="datetime-local"
                  step="60"
                  :disabled="saving"
                  :aria-label="`${stage.stageName}开始时间`"
                  @input="handleStageInput"
                />
              </div>

              <div>
                <input
                  v-model="stage.endAt"
                  class="input"
                  :class="{ err: isFieldError(index, 'endAt') }"
                  type="datetime-local"
                  step="60"
                  :disabled="saving"
                  :aria-label="`${stage.stageName}结束时间`"
                  @input="handleStageInput"
                />
              </div>
            </div>
          </template>
        </div>

        <div class="switch-row">
          <button
            class="switch"
            :class="{ on: publishAnnouncement }"
            type="button"
            :disabled="saving"
            :aria-pressed="publishAnnouncement"
            @click="togglePublish"
          >
            <span class="track" aria-hidden="true"></span>
            <span>本次调整发布公告</span>
          </button>
          <span class="help">不发布仅更新阶段时间；发布后公告面向全体用户，本次最多生成一条。</span>
        </div>

        <div v-if="publishAnnouncement" class="notice-editor">
          <div class="field">
            <label class="label" for="announcementTitle">公告标题</label>
            <input
              id="announcementTitle"
              v-model="announcement.title"
              class="input"
              :class="{ err: Boolean(announcementError) }"
              type="text"
              maxlength="200"
              :disabled="saving"
              @input="markTitleEdited"
            />
          </div>

          <div class="field">
            <label class="label" for="announcementContent">公告正文（可编辑）</label>
            <textarea
              id="announcementContent"
              v-model="announcement.content"
              class="textarea"
              :class="{ err: Boolean(announcementError) }"
              :disabled="saving"
              @input="markContentEdited"
            ></textarea>
            <p v-if="announcementError" class="error" role="alert">{{ announcementError }}</p>
          </div>
        </div>

        <div class="actions">
          <button class="btn btn-primary" type="button" :disabled="saving" @click="handleSave">
            {{ saving ? '保存中…' : '保存' }}
          </button>
          <span class="help">
            阶段名称与顺序固定，仅配置起止时间；每轮录取内「志愿填报 → 面试 →
            拟录取确认」共用该轮时间。
          </span>
        </div>
      </template>
    </section>
  </DefaultLayout>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue';
import DefaultLayout from '../../layouts/DefaultLayout.vue';
import BaseIcon from '../../components/BaseIcon.vue';
import StatusBadge from '../../components/StatusBadge.vue';
import EmptyState from '../../components/EmptyState.vue';
import {
  getManagerStages,
  getStageChanges,
  retryStageAnnouncement,
  saveStages,
} from '../../api/stage';
import { showToast } from '../../utils/toast';

const STAGE_DEFINITIONS = [
  { stageCode: 'BRIEFING', stageName: '集中宣讲', sub: '宣讲会（线下）' },
  { stageCode: 'OPEN_DAY', stageName: '实训室开放日', sub: '开放参观（线下）' },
  {
    stageCode: 'ROUND_1',
    stageName: '第一轮录取',
    sub: '志愿填报 → 面试 → 拟录取确认',
    group: true,
  },
  { stageCode: 'ROUND_2', stageName: '第二轮录取', sub: '志愿填报 → 面试 → 拟录取确认' },
  { stageCode: 'ROUND_3', stageName: '第三轮录取', sub: '志愿填报 → 面试 → 拟录取确认' },
  { stageCode: 'ADJUSTMENT', stageName: '统筹调剂', sub: '线下统筹调剂' },
  { stageCode: 'ENROLLMENT', stageName: '正式入组', sub: '确定最终去向' },
];

const loading = ref(false);
const loadError = ref(false);
const saving = ref(false);
const retrying = ref(false);

const configured = ref(false);
const currentStageCode = ref('');
const formStages = ref([]);
const originalStages = ref([]);

const publishAnnouncement = ref(false);
const announcement = reactive({
  title: '',
  content: '',
});
const announcementError = ref('');

const showValidation = ref(false);
const conflictIndexes = ref([]);
const fieldErrors = ref([]);

const failedChangeNo = ref('');

let currentChangeNo = '';

const announcementTitleEdited = ref(false);
const announcementContentEdited = ref(false);

const currentStageName = computed(() => {
  const stage = formStages.value.find((item) => item.stageCode === currentStageCode.value);
  return stage?.stageName || '';
});

function toInputDateTime(value) {
  return value ? String(value).replace(' ', 'T').slice(0, 16) : '';
}

function toApiDateTime(value) {
  return value ? `${String(value).replace(' ', 'T').slice(0, 16)}:00` : null;
}

function formatDisplay(value) {
  return value ? String(value).replace('T', ' ') : '—';
}

function normalizeStages(apiStages) {
  const stageMap = new Map(
    (Array.isArray(apiStages) ? apiStages : []).map((stage) => [stage.stageCode, stage]),
  );

  return STAGE_DEFINITIONS.map((definition) => {
    const stage = stageMap.get(definition.stageCode);
    return {
      ...definition,
      startAt: toInputDateTime(stage?.startAt),
      endAt: toInputDateTime(stage?.endAt),
    };
  });
}

async function loadStages() {
  loading.value = true;
  loadError.value = false;
  showValidation.value = false;
  conflictIndexes.value = [];
  fieldErrors.value = [];
  announcementError.value = '';
  publishAnnouncement.value = false;
  announcement.title = '';
  announcement.content = '';
  announcementTitleEdited.value = false;
  announcementContentEdited.value = false;
  currentChangeNo = '';

  try {
    const data = await getManagerStages();
    configured.value = Boolean(data?.configured);
    currentStageCode.value = data?.currentStageCode || '';

    const normalized = normalizeStages(data?.stages);
    formStages.value = normalized.map((stage) => ({ ...stage }));
    originalStages.value = normalized.map((stage) => ({ ...stage }));
  } catch {
    formStages.value = [];
    originalStages.value = [];
    loadError.value = true;
  } finally {
    loading.value = false;
  }
}

async function loadFailedChange() {
  try {
    const data = await getStageChanges({ status: 'FAILED', page: 1, pageSize: 1 });
    const item = data?.list?.[0];
    failedChangeNo.value = item?.changeNo || '';
  } catch {
    // 失败公告恢复不是主流程，不阻塞阶段配置。
    return;
  }
}

async function loadPage() {
  await Promise.allSettled([loadStages(), loadFailedChange()]);
}

function createChangeNo() {
  if (window.crypto?.randomUUID) {
    return window.crypto.randomUUID();
  }
  return `stage-${Date.now()}-${Math.random().toString(16).slice(2, 10)}`;
}

function generateAnnouncement() {
  const isFirstConfig =
    !configured.value || originalStages.value.every((stage) => !stage.startAt && !stage.endAt);
  const lines = [];

  formStages.value.forEach((stage, index) => {
    const original = originalStages.value[index] || {};
    const changed = original.startAt !== stage.startAt || original.endAt !== stage.endAt;

    if (isFirstConfig) {
      lines.push(
        `${stage.stageName}：${formatDisplay(stage.startAt)} ~ ${formatDisplay(stage.endAt)}`,
      );
      return;
    }

    if (!changed) {
      return;
    }

    const oldRange =
      original.startAt || original.endAt
        ? `${formatDisplay(original.startAt)} ~ ${formatDisplay(original.endAt)}`
        : '未配置';
    const newRange = `${formatDisplay(stage.startAt)} ~ ${formatDisplay(stage.endAt)}`;
    lines.push(`${stage.stageName}：${oldRange} → ${newRange}`);
  });

  if (lines.length === 0) {
    lines.push('本次未修改阶段时间。');
  }

  announcement.title = '关于调整选拔阶段时间的通知';
  announcement.content =
    `${isFirstConfig ? '各阶段时间配置如下：' : '本次调整涉及以下阶段：'}\n\n` +
    `${lines.join('\n')}\n\n` +
    '请各位同学、导师关注各阶段起止时间，按时完成对应操作。';

  announcementTitleEdited.value = false;
  announcementContentEdited.value = false;
}

function togglePublish() {
  if (saving.value) {
    return;
  }

  publishAnnouncement.value = !publishAnnouncement.value;
  announcementError.value = '';

  if (publishAnnouncement.value) {
    generateAnnouncement();
  }
}

function markTitleEdited() {
  announcementTitleEdited.value = true;
  announcementError.value = '';
}

function markContentEdited() {
  announcementContentEdited.value = true;
  announcementError.value = '';
}

function handleStageInput() {
  showValidation.value = false;
  conflictIndexes.value = [];
  fieldErrors.value = [];
  announcementError.value = '';

  if (
    publishAnnouncement.value &&
    !announcementTitleEdited.value &&
    !announcementContentEdited.value
  ) {
    generateAnnouncement();
  }
}

function isFieldError(index, field) {
  return showValidation.value && fieldErrors.value.includes(`${index}-${field}`);
}

function validateForm() {
  const missingIndexes = [];
  const missingFields = [];

  formStages.value.forEach((stage, index) => {
    if (!stage.startAt) {
      missingIndexes.push(index);
      missingFields.push(`${index}-startAt`);
    }
    if (!stage.endAt) {
      missingIndexes.push(index);
      missingFields.push(`${index}-endAt`);
    }
  });

  if (missingIndexes.length > 0) {
    return {
      valid: false,
      message: '请填写完整的阶段时间',
      indexes: [...new Set(missingIndexes)],
      fields: missingFields,
      announcementError: '',
    };
  }

  for (let index = 0; index < formStages.value.length; index += 1) {
    const stage = formStages.value[index];
    if (stage.startAt >= stage.endAt) {
      return {
        valid: false,
        message: '阶段结束时间必须晚于开始时间',
        indexes: [index],
        fields: [`${index}-startAt`, `${index}-endAt`],
        announcementError: '',
      };
    }
  }

  for (let index = 1; index < formStages.value.length; index += 1) {
    if (formStages.value[index - 1].endAt > formStages.value[index].startAt) {
      return {
        valid: false,
        message: '阶段时间必须按流程顺序设置且不能重叠',
        indexes: [index - 1, index],
        fields: [
          `${index - 1}-startAt`,
          `${index - 1}-endAt`,
          `${index}-startAt`,
          `${index}-endAt`,
        ],
        announcementError: '',
      };
    }
  }

  if (publishAnnouncement.value) {
    if (!announcement.title.trim() || !announcement.content.trim()) {
      return {
        valid: false,
        message: '公告内容不能为空',
        indexes: [],
        fields: [],
        announcementError: '公告内容不能为空',
      };
    }
  }

  return {
    valid: true,
    message: '',
    indexes: [],
    fields: [],
    announcementError: '',
  };
}

function syncOriginalStages() {
  originalStages.value = formStages.value.map((stage) => ({ ...stage }));
  configured.value = true;
}

function resetAnnouncement() {
  publishAnnouncement.value = false;
  announcement.title = '';
  announcement.content = '';
  announcementTitleEdited.value = false;
  announcementContentEdited.value = false;
  announcementError.value = '';
}

function handleSaveSuccess(data) {
  syncOriginalStages();
  currentChangeNo = '';

  const status = data?.announcementStatus;

  if (status === 'FAILED') {
    failedChangeNo.value = data?.changeNo || '';
    publishAnnouncement.value = false;
    announcementError.value = '';
    showToast('阶段已保存，公告发布失败，请重试', 'error');
    return;
  }

  if (status === 'PUBLISHED') {
    showToast('保存成功，公告已发布', 'success');
  } else {
    showToast(data?.message || '保存成功', 'success');
  }

  resetAnnouncement();
}

async function handleSave() {
  if (saving.value) {
    return;
  }

  showValidation.value = true;
  announcementError.value = '';
  const result = validateForm();
  conflictIndexes.value = result.indexes;
  fieldErrors.value = result.fields;
  announcementError.value = result.announcementError || '';

  if (!result.valid) {
    showToast(result.message, 'error');
    return;
  }

  saving.value = true;

  if (!currentChangeNo) {
    currentChangeNo = createChangeNo();
  }

  const payload = {
    changeNo: currentChangeNo,
    stages: formStages.value.map((stage) => ({
      stageCode: stage.stageCode,
      startAt: toApiDateTime(stage.startAt),
      endAt: toApiDateTime(stage.endAt),
    })),
    publishAnnouncement: publishAnnouncement.value,
  };

  if (publishAnnouncement.value) {
    payload.announcement = {
      title: announcement.title.trim(),
      content: announcement.content.trim(),
    };
  }

  try {
    const data = await saveStages(payload);

    if (!data?.stageSaved) {
      showToast(data?.message || '提交失败，请重试', 'error');
      return;
    }

    handleSaveSuccess(data);
  } catch {
    // request.js 已统一提示；保留 changeNo，网络重试可复用同一幂等键。
    return;
  } finally {
    saving.value = false;
  }
}

async function handleRetryAnnouncement() {
  if (!failedChangeNo.value || retrying.value) {
    return;
  }

  retrying.value = true;

  try {
    const data = await retryStageAnnouncement(failedChangeNo.value);
    failedChangeNo.value = '';
    showToast(data?.message || '公告已发布', 'success');
  } catch {
    // request.js 已统一提示；保留 changeNo，允许再次重试。
    return;
  } finally {
    retrying.value = false;
  }
}

onMounted(loadPage);
</script>

<style scoped>
.stage-card {
  max-width: 1120px;
  padding: 24px 26px;
}

.head-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.current-stage {
  font-size: 12px;
  color: var(--text3);
}

.retry-banner {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 20px;
  padding: 12px 16px;
  border: 1px solid rgba(248, 113, 113, 0.3);
  border-radius: 10px;
  background: rgba(248, 113, 113, 0.06);
}

.retry-icon {
  display: grid;
  place-items: center;
  color: var(--red);
}

.retry-text {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 2px;
  min-width: 220px;
  font-size: 12.5px;
  color: var(--text2);
}

.retry-text strong {
  font-size: 13.5px;
  color: var(--red);
}

.group-label {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 20px 0 10px;
  font-size: 12px;
  letter-spacing: 0.5px;
  color: var(--amber);
}

.group-label::before {
  content: '';
  width: 10px;
  height: 2px;
  background: var(--amber);
}

.stage-row {
  display: grid;
  grid-template-columns: 180px 1fr 1fr;
  gap: 16px;
  align-items: center;
  padding: 10px 0;
  border-bottom: 1px solid rgba(255, 154, 69, 0.06);
}

.stage-row.conflict {
  padding: 10px 8px;
  border-radius: 8px;
  background: rgba(248, 113, 113, 0.05);
}

.stage-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--text);
}

.stage-name small {
  display: block;
  margin-top: 1px;
  font-size: 11px;
  font-weight: 400;
  color: var(--text3);
}

.input {
  width: 100%;
  padding: 9px 12px;
  border: 1px solid var(--line);
  border-radius: 8px;
  outline: none;
  background: rgba(255, 255, 255, 0.9);
  color: var(--text);
  color-scheme: light;
  font-size: 13.5px;
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

.input.err {
  border-color: rgba(248, 113, 113, 0.55);
}

.switch-row {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  margin: 24px 0 6px;
  padding-top: 20px;
  border-top: 1px solid var(--line);
}

.switch {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  padding: 0;
  border: 0;
  background: transparent;
  color: var(--text2);
  font-size: 14px;
  cursor: pointer;
}

.switch:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.switch .track {
  position: relative;
  width: 40px;
  height: 22px;
  border: 1px solid var(--line);
  border-radius: 20px;
  background: #e7eaf1;
  transition: 0.15s;
}

.switch .track::after {
  content: '';
  position: absolute;
  top: 2px;
  left: 2px;
  width: 16px;
  height: 16px;
  border-radius: 50%;
  background: #94a3b8;
  transition: 0.15s;
}

.switch.on {
  color: var(--text);
}

.switch.on .track {
  border-color: var(--line2);
  background: rgba(255, 122, 24, 0.25);
}

.switch.on .track::after {
  left: 20px;
  background: var(--cyan);
}

.notice-editor {
  margin-top: 16px;
  padding: 18px;
  border: 1px solid var(--line);
  border-radius: 11px;
  background: rgba(250, 250, 252, 0.92);
}

.field {
  margin-bottom: 16px;
}

.field:last-child {
  margin-bottom: 0;
}

.label {
  display: block;
  margin-bottom: 7px;
  font-size: 13px;
  color: var(--text2);
}

.textarea {
  width: 100%;
  min-height: 120px;
  padding: 10px 12px;
  border: 1px solid var(--line);
  border-radius: 8px;
  outline: none;
  background: rgba(255, 255, 255, 0.9);
  color: var(--text);
  font-size: 13.5px;
  line-height: 1.8;
  resize: vertical;
  transition: 0.15s;
}

.textarea:focus {
  border-color: var(--line2);
  box-shadow: 0 0 0 3px rgba(255, 122, 24, 0.12);
}

.textarea:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.textarea.err {
  border-color: rgba(248, 113, 113, 0.55);
}

.error {
  margin: 6px 0 0;
  font-size: 12px;
  color: var(--red);
}

.actions {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
  margin-top: 22px;
}

.btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  padding: 10px 24px;
  border: 1px solid var(--line2);
  border-radius: 8px;
  background: var(--panel2);
  color: var(--text);
  font-size: 14px;
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

.btn-retry {
  padding: 7px 14px;
  border-color: rgba(248, 113, 113, 0.35);
  background: transparent;
  color: var(--red);
  font-size: 12.5px;
}

.btn-retry:hover:not(:disabled) {
  background: rgba(248, 113, 113, 0.08);
}

.help {
  margin-top: 6px;
  font-size: 12px;
  color: var(--text3);
}

@media (max-width: 900px) {
  .stage-card {
    padding: 20px 18px;
  }

  .stage-row {
    grid-template-columns: 1fr;
    gap: 8px;
  }
}
</style>
