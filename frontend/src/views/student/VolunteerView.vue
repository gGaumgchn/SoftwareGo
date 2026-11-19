<template>
  <DefaultLayout :title="pageTitle">
    <section class="card volunteer-card">
      <div class="card-head">
        <div class="card-title"><span class="dot"></span>{{ formTitle }}</div>
        <div v-if="!loading && !loadError" class="head-badges">
          <StatusBadge v-if="application" type="green">已提交</StatusBadge>
          <StatusBadge :type="roundBadgeType">{{ roundBadgeText }}</StatusBadge>
        </div>
      </div>

      <EmptyState v-if="loading" kind="loading" />
      <EmptyState v-else-if="loadError" kind="error" @retry="loadPage" />

      <template v-else>
        <div v-if="blockedMessage" class="banner banner-warn" role="alert">
          <BaseIcon name="alert-circle" :size="18" class="banner-icon banner-icon--warn" />
          <span>{{ blockedMessage }}</span>
        </div>

        <div v-if="roundWindowText" class="banner banner-info">
          <BaseIcon name="calendar" :size="18" class="banner-icon banner-icon--info" />
          <span>{{ roundWindowText }}</span>
        </div>

        <div v-if="application" class="banner banner-ok">
          <BaseIcon name="check" :size="18" class="banner-icon banner-icon--ok" />
          <span>{{ applicationHint }}</span>
        </div>

        <div v-if="showAvailableTeachers" class="supp-panel">
          <div class="supp-head">
            <div class="supp-title">本轮仍有剩余名额的导师</div>
            <StatusBadge type="cyan">{{ roundLabel }}</StatusBadge>
          </div>
          <p class="supp-note">
            本轮为补录填报，以下导师仍剩余名额，供查看参考；填报对象须为以下仍有名额的导师。
          </p>

          <EmptyState v-if="teachersLoading" kind="loading" />
          <EmptyState
            v-else-if="teachersError"
            kind="error"
            error-text="导师名额加载失败，请重试"
            @retry="loadAvailableTeachers"
          />
          <EmptyState v-else-if="teachers.length === 0" text="本轮暂无可填报名额" />
          <div v-else class="table-wrap">
            <table>
              <thead>
                <tr>
                  <th class="col-name">导师姓名</th>
                  <th class="col-lab">所属实训室</th>
                  <th class="col-quota">剩余名额</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="teacher in teachers" :key="teacher.teacherId">
                  <td>
                    <strong>{{ teacher.teacherName }}</strong>
                  </td>
                  <td>{{ teacher.labName }}</td>
                  <td>
                    <StatusBadge type="cyan">{{ teacher.remainingQuota }}</StatusBadge>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
          <div class="divider"></div>
        </div>

        <div class="field">
          <label class="label">
            意向导师
            <span class="opt">最多填写三名，三个志愿平行、不分优先级</span>
          </label>
          <div class="teacher-grid">
            <input
              v-for="index in TEACHER_SLOTS"
              :key="index"
              v-model="form.teacherNames[index]"
              class="input"
              :class="{ err: duplicateIndexes.includes(index) }"
              type="text"
              maxlength="64"
              autocomplete="off"
              :disabled="formDisabled"
              placeholder="导师姓名（可选）"
            />
          </div>
          <div class="help">至少填写一名导师；仅输入空格视为未填写；同一导师姓名不能重复填写。</div>
          <div v-if="teacherError" class="error" role="alert">{{ teacherError }}</div>
        </div>

        <div class="form-grid">
          <div class="field">
            <label class="label">是否同意实训室内调剂 <span class="req">*</span></label>
            <div class="radio-group">
              <label class="radio" :class="{ disabled: formDisabled }">
                <input
                  v-model="form.intraLabAdjust"
                  type="radio"
                  value="agree"
                  :disabled="formDisabled"
                />
                <span class="rb"></span>
                <span class="rt">同意</span>
              </label>
              <label class="radio" :class="{ disabled: formDisabled }">
                <input
                  v-model="form.intraLabAdjust"
                  type="radio"
                  value="disagree"
                  :disabled="formDisabled"
                />
                <span class="rb"></span>
                <span class="rt">不同意</span>
              </label>
            </div>
          </div>

          <div class="field">
            <label class="label">是否同意学院内调剂 <span class="req">*</span></label>
            <div class="radio-group">
              <label class="radio" :class="{ disabled: formDisabled }">
                <input
                  v-model="form.collegeAdjust"
                  type="radio"
                  value="agree"
                  :disabled="formDisabled"
                />
                <span class="rb"></span>
                <span class="rt">同意</span>
              </label>
              <label class="radio" :class="{ disabled: formDisabled }">
                <input
                  v-model="form.collegeAdjust"
                  type="radio"
                  value="disagree"
                  :disabled="formDisabled"
                />
                <span class="rb"></span>
                <span class="rt">不同意</span>
              </label>
            </div>
          </div>
        </div>

        <div class="field">
          <label class="label">个人简历（PDF）<span class="req">*</span></label>
          <FileUpload
            :file-id="form.resumeFileId"
            :file-name="form.resumeFileName"
            :disabled="formDisabled"
            upload-text="点击上传个人简历，仅支持 PDF 格式，提交前可替换"
            @uploaded="handleResumeUploaded"
            @uploading="handleResumeUploading"
          />
        </div>

        <div class="actions">
          <button
            class="btn btn-primary"
            type="button"
            :disabled="!canSubmit"
            @click="handleSubmit"
          >
            {{ submitting ? '提交中…' : submitLabel }}
          </button>
          <span class="help action-hint">{{ actionHint }}</span>
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
import FileUpload from '../../components/FileUpload.vue';
import { getAvailableTeachers, getCurrentVolunteer, submitVolunteer } from '../../api/volunteer';
import { formatDateTime, setServerTime } from '../../utils/time';
import { showToast } from '../../utils/toast';

const ROUND_LABELS = {
  1: '第一轮录取',
  2: '第二轮录取',
  3: '第三轮录取',
};

const TEACHER_SLOTS = [0, 1, 2];

const loading = ref(true);
const loadError = ref(false);
const submitting = ref(false);
const resumeUploading = ref(false);

const current = ref(null);
const teachers = ref([]);
const teachersLoading = ref(false);
const teachersError = ref(false);

const form = reactive({
  teacherNames: ['', '', ''],
  intraLabAdjust: '',
  collegeAdjust: '',
  resumeFileId: null,
  resumeFileName: '',
});

const roundNo = computed(() => {
  const value = current.value?.roundNo;
  return value == null ? null : Number(value);
});

const roundState = computed(() => current.value?.roundState || 'UNCONFIGURED');
const roundLabel = computed(() => ROUND_LABELS[roundNo.value] || '');
const isSupplement = computed(() => (roundNo.value ?? 0) >= 2);
const admitted = computed(() => Boolean(current.value?.admitted));
const canSubmitByServer = computed(() => Boolean(current.value?.canSubmit));
const application = computed(() => current.value?.application || null);

const formTitle = computed(() =>
  isSupplement.value ? `${roundLabel.value} · 志愿填报（补录）` : '志愿填报',
);

const pageTitle = computed(() => (isSupplement.value ? '志愿填报 · 补录' : '志愿填报'));

const roundBadgeText = computed(() => {
  if (roundNo.value) {
    return roundLabel.value;
  }
  return roundState.value === 'UNCONFIGURED' ? '阶段未配置' : '当前无录取轮次';
});

const roundBadgeType = computed(() => {
  if (roundState.value === 'OPEN') return 'cyan';
  if (roundState.value === 'NOT_STARTED') return 'amber';
  return 'gray';
});

const blockedMessage = computed(() => {
  const reason = current.value?.blockedReason;
  const round = roundNo.value;

  const messages = {
    ADMITTED: round === 1 ? '已被录取！' : '你已确认录取，无需参加补录',
    NOT_OPEN: round === 1 ? '志愿填报尚未开始' : '补录填报尚未开始',
    ENDED: round === 1 ? '志愿填报已截止' : '补录填报已截止',
    NO_ROUND: '当前不在录取轮次',
    NO_QUOTA: '本轮暂无可填报名额',
    PROCESSED: '本轮志愿已进入处理阶段，不能再修改',
  };

  if (reason && messages[reason]) {
    return messages[reason];
  }

  if (!canSubmitByServer.value) {
    if (roundState.value === 'UNCONFIGURED') return '阶段暂未配置';
    if (roundState.value === 'NO_ROUND') return '当前不在录取轮次';
    return '本轮暂不可提交';
  }

  return '';
});

const roundWindowText = computed(() => {
  const start = current.value?.roundStart;
  const end = current.value?.roundEnd;
  if (!start || !end) {
    return '';
  }

  const label = roundLabel.value ? `${roundLabel.value} · ` : '';
  return `${label}填报时间：${formatDateTime(start)} — ${formatDateTime(end)}`;
});

const applicationHint = computed(() => {
  if (!application.value) {
    return '';
  }

  const submittedAt = application.value.submittedAt
    ? `本轮志愿已提交（${formatDateTime(application.value.submittedAt)}）。`
    : '本轮志愿已提交。';

  return canSubmitByServer.value
    ? `${submittedAt}截止前可重新提交，重新提交将覆盖本轮志愿内容。`
    : `${submittedAt}当前不可再次提交。`;
});

const submitLabel = computed(() => (application.value ? '重新提交志愿' : '提交志愿'));

const actionHint = computed(() => {
  if (!canSubmitByServer.value) {
    return blockedMessage.value || '当前不可提交';
  }
  if (application.value) {
    return '重新提交仅覆盖本轮志愿，不影响历轮记录。';
  }
  return '完成填写并上传 PDF 简历后可提交。';
});

const showAvailableTeachers = computed(
  () => isSupplement.value && roundState.value === 'OPEN' && !admitted.value,
);

function normalizeName(value) {
  return String(value ?? '')
    .trim()
    .replace(/\s+/g, ' ');
}

const normalizedNames = computed(() => form.teacherNames.map(normalizeName));
const filledNames = computed(() => normalizedNames.value.filter(Boolean));

const duplicateIndexes = computed(() => {
  const counts = new Map();
  normalizedNames.value.forEach((name) => {
    if (name) {
      counts.set(name, (counts.get(name) || 0) + 1);
    }
  });

  return normalizedNames.value.reduce((indexes, name, index) => {
    if (name && (counts.get(name) || 0) > 1) {
      indexes.push(index);
    }
    return indexes;
  }, []);
});

const teacherError = computed(() => (duplicateIndexes.value.length > 0 ? '导师姓名不能重复' : ''));

const formDisabled = computed(
  () => loading.value || loadError.value || !canSubmitByServer.value || submitting.value,
);

const canSubmit = computed(() => {
  if (formDisabled.value || resumeUploading.value) {
    return false;
  }

  return (
    filledNames.value.length >= 1 &&
    duplicateIndexes.value.length === 0 &&
    Boolean(form.intraLabAdjust) &&
    Boolean(form.collegeAdjust) &&
    Boolean(form.resumeFileId)
  );
});

function applyApplicationToForm(applicationData) {
  const targets = [...(applicationData?.targets || [])].sort(
    (a, b) => Number(a.preferenceNo) - Number(b.preferenceNo),
  );

  form.teacherNames = [0, 1, 2].map((index) => targets[index]?.teacherName || '');
  form.intraLabAdjust =
    typeof applicationData?.intraLabAdjust === 'boolean'
      ? applicationData.intraLabAdjust
        ? 'agree'
        : 'disagree'
      : '';
  form.collegeAdjust =
    typeof applicationData?.collegeAdjust === 'boolean'
      ? applicationData.collegeAdjust
        ? 'agree'
        : 'disagree'
      : '';
  form.resumeFileId = applicationData?.resumeFileId ?? null;
  form.resumeFileName = applicationData?.resumeFileName || '';
}

async function loadAvailableTeachers() {
  if (!isSupplement.value) {
    teachers.value = [];
    return;
  }

  teachersLoading.value = true;
  teachersError.value = false;

  try {
    const data = await getAvailableTeachers();
    setServerTime(data?.serverTime);
    teachers.value = Array.isArray(data?.list) ? data.list : [];
  } catch {
    teachersError.value = true;
    teachers.value = [];
  } finally {
    teachersLoading.value = false;
  }
}

async function loadPage() {
  loading.value = true;
  loadError.value = false;

  try {
    const data = await getCurrentVolunteer();
    current.value = data || {};
    setServerTime(data?.serverTime);
    applyApplicationToForm(data?.application);
  } catch {
    loadError.value = true;
  } finally {
    loading.value = false;
  }

  if (!loadError.value && showAvailableTeachers.value) {
    loadAvailableTeachers();
  } else {
    teachers.value = [];
    teachersError.value = false;
  }
}

function handleResumeUploaded(info) {
  form.resumeFileId = info?.fileId ?? null;
  form.resumeFileName = info?.originalName || '';
}

function handleResumeUploading(value) {
  resumeUploading.value = value;
}

function applySubmitResult(data) {
  const existing = application.value || {};
  const intraAgree = form.intraLabAdjust === 'agree';
  const collegeAgree = form.collegeAdjust === 'agree';

  current.value = {
    ...current.value,
    application: {
      applicationId: data?.applicationId ?? existing.applicationId ?? null,
      roundNo: data?.roundNo ?? roundNo.value,
      submittedAt: data?.submittedAt || existing.submittedAt || '',
      intraLabAdjust: typeof data?.intraLabAdjust === 'boolean' ? data.intraLabAdjust : intraAgree,
      collegeAdjust: typeof data?.collegeAdjust === 'boolean' ? data.collegeAdjust : collegeAgree,
      resumeFileId: form.resumeFileId,
      resumeFileName: form.resumeFileName,
      targets: Array.isArray(data?.targets) ? data.targets : existing.targets || [],
    },
  };
}

async function handleSubmit() {
  if (!canSubmit.value) {
    return;
  }

  submitting.value = true;

  try {
    const data = await submitVolunteer({
      teacherNames: filledNames.value,
      intraLabAdjust: form.intraLabAdjust === 'agree',
      collegeAdjust: form.collegeAdjust === 'agree',
      resumeFileId: form.resumeFileId,
    });

    applySubmitResult(data);
    showToast(data?.message || '提交成功', 'success');

    if (showAvailableTeachers.value) {
      loadAvailableTeachers();
    }
  } catch {
    // 真实接口错误由 request.js 统一提示；失败时保留填写内容与已上传简历。
  } finally {
    submitting.value = false;
  }
}

onMounted(loadPage);
</script>

<style scoped>
.volunteer-card {
  max-width: 1040px;
  padding: 24px 26px;
}

.head-badges {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.banner {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
  padding: 13px 16px;
  border: 1px solid var(--line);
  border-radius: 10px;
  font-size: 13px;
  color: var(--text2);
}

.banner-warn {
  border-color: rgba(251, 191, 36, 0.35);
  background: rgba(251, 191, 36, 0.08);
}

.banner-info {
  border-color: rgba(255, 154, 69, 0.25);
  background: rgba(255, 154, 69, 0.06);
}

.banner-ok {
  border-color: rgba(52, 211, 153, 0.35);
  background: rgba(52, 211, 153, 0.06);
}

.banner-icon {
  flex: 0 0 auto;
}

.banner-icon--warn {
  color: var(--amber);
}

.banner-icon--info {
  color: var(--blue);
}

.banner-icon--ok {
  color: var(--green);
}

.supp-panel {
  margin-bottom: 22px;
}

.supp-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.supp-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 600;
  color: var(--text);
}

.supp-note {
  margin: 6px 0 0;
  font-size: 12.5px;
  color: var(--text2);
}

.table-wrap {
  overflow-x: auto;
  margin-top: 14px;
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
}

tbody tr:hover {
  background: rgba(255, 154, 69, 0.04);
}

td strong {
  color: var(--text);
  font-weight: 500;
}

.col-name {
  width: 33%;
}

.col-lab {
  width: 34%;
}

.col-quota {
  width: 33%;
}

.divider {
  height: 1px;
  margin: 22px 0;
  background: var(--line);
}

.field {
  margin-bottom: 18px;
}

.label {
  display: block;
  margin-bottom: 7px;
  font-size: 13px;
  color: var(--text2);
}

.label .req {
  color: var(--cyan);
}

.label .opt {
  margin-left: 8px;
  font-size: 12px;
  font-weight: 400;
  color: var(--text3);
}

.teacher-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14px;
}

.input {
  width: 100%;
  padding: 10px 12px;
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

.input.err {
  border-color: rgba(248, 113, 113, 0.5);
}

.help {
  margin-top: 6px;
  font-size: 12px;
  color: var(--text3);
}

.error {
  margin-top: 6px;
  font-size: 12px;
  color: var(--red);
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0 28px;
}

.radio-group {
  display: flex;
  gap: 18px;
}

.radio {
  display: flex;
  align-items: center;
  gap: 7px;
  font-size: 14px;
  color: var(--text2);
  cursor: pointer;
}

.radio.disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.radio input {
  position: absolute;
  width: 1px;
  height: 1px;
  opacity: 0;
}

.radio .rb {
  position: relative;
  flex: 0 0 16px;
  width: 16px;
  height: 16px;
  border: 1px solid #cbd2de;
  border-radius: 50%;
  transition: 0.15s;
}

.radio input:checked ~ .rb {
  border-color: var(--cyan);
}

.radio input:checked ~ .rb::after {
  position: absolute;
  inset: 3px;
  border-radius: 50%;
  background: var(--cyan);
  content: '';
}

.radio input:checked ~ .rt {
  color: var(--text);
}

.actions {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
  margin-top: 6px;
}

.action-hint {
  margin: 0;
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

@media (max-width: 900px) {
  .volunteer-card {
    padding: 20px 18px;
  }

  .teacher-grid,
  .form-grid {
    grid-template-columns: 1fr;
  }

  .form-grid {
    gap: 0;
  }
}
</style>
