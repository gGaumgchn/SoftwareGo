<template>
  <DefaultLayout title="实训室信息">
    <section class="card lab-card">
      <div class="card-head">
        <div class="card-title"><span class="dot"></span>提交实训室信息</div>
        <StatusBadge :type="statusType">{{ statusText }}</StatusBadge>
      </div>

      <EmptyState v-if="loading" kind="loading" />
      <EmptyState v-else-if="loadError" kind="error" @retry="loadLabInfo" />

      <template v-else>
        <div v-if="submitted" class="banner-ok">
          <BaseIcon name="check" :size="18" class="banner-icon" />
          <span>{{ submittedHint }}</span>
        </div>
        <div v-else class="banner-info">
          <BaseIcon name="building" :size="18" class="banner-icon" />
          <span>尚未提交实训室信息，填写并提交后学生即可查看。</span>
        </div>

        <div class="form-grid">
          <div class="field">
            <label class="label" for="labName">实训室名称 <span class="req">*</span></label>
            <input
              id="labName"
              v-model.trim="form.labName"
              class="input"
              type="text"
              maxlength="100"
              :disabled="submitting"
              placeholder="例如：云计算与大数据实验室"
            />
          </div>

          <div class="field">
            <label class="label" for="location">
              实训室位置（校区、楼栋及房间号）<span class="req">*</span>
            </label>
            <input
              id="location"
              v-model.trim="form.location"
              class="input"
              type="text"
              maxlength="200"
              :disabled="submitting"
              placeholder="例如：主校区 · 软件学院楼 · 2-204"
            />
          </div>

          <div class="field">
            <label class="label" for="plannedQuota">计划招收人数 <span class="req">*</span></label>
            <input
              id="plannedQuota"
              v-model.trim="form.plannedQuota"
              class="input"
              :class="{ err: Boolean(plannedQuotaError) }"
              type="text"
              inputmode="numeric"
              maxlength="6"
              :disabled="submitting"
              placeholder="请输入正整数"
            />
            <div class="help">
              须为正整数；修改后不得小于当前已占用名额（当前占用 {{ occupiedQuota }} 人<template
                v-if="remainingQuota !== null"
                >，剩余 {{ remainingQuota }} 人</template
              >）。
            </div>
            <div v-if="plannedQuotaError" class="error">{{ plannedQuotaError }}</div>
          </div>

          <div class="field">
            <label class="label" for="teacherName">导师姓名 <span class="req">*</span></label>
            <input
              id="teacherName"
              v-model.trim="form.teacherName"
              class="input"
              type="text"
              maxlength="64"
              :disabled="submitting"
              placeholder="请输入导师姓名"
            />
          </div>

          <div class="field full">
            <label class="label" for="researchDirection">研究方向 <span class="req">*</span></label>
            <input
              id="researchDirection"
              v-model.trim="form.researchDirection"
              class="input"
              type="text"
              maxlength="200"
              :disabled="submitting"
              placeholder="例如：云计算与大数据"
            />
          </div>

          <div class="field">
            <label class="label" for="contact">联系方式 <span class="req">*</span></label>
            <input
              id="contact"
              v-model.trim="form.contact"
              class="input"
              type="text"
              maxlength="100"
              :disabled="submitting"
              placeholder="例如：138-0000-2222"
            />
          </div>

          <div class="field">
            <label class="label">导师简历（PDF）<span class="req">*</span></label>
            <FileUpload
              :file-id="form.resumeFileId"
              :file-name="form.resumeFileName"
              :disabled="submitting"
              upload-text="点击上传导师简历"
              @uploaded="handleResumeUploaded"
              @uploading="handleResumeUploading"
            />
          </div>
        </div>

        <div class="actions">
          <button class="btn btn-primary" type="button" :disabled="!canSubmit" @click="handleSubmit">
            {{ submitting ? '提交中…' : '提交' }}
          </button>
          <span class="help">提交成功后学生可查看；可随时修改并重新提交。</span>
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
import { getTeacherLab, saveTeacherLab } from '../../api/lab';
import { useUserStore } from '../../stores/user';
import { formatDateTime } from '../../utils/time';
import { showToast } from '../../utils/toast';

const userStore = useUserStore();

const loading = ref(true);
const loadError = ref(false);
const submitting = ref(false);
const resumeUploading = ref(false);

const submitted = ref(false);
const occupiedQuota = ref(0);
const remainingQuota = ref(null);
const updatedAt = ref('');

const form = reactive({
  labName: '',
  location: '',
  plannedQuota: '',
  teacherName: '',
  researchDirection: '',
  contact: '',
  resumeFileId: null,
  resumeFileName: '',
});

const statusText = computed(() => {
  if (loading.value) return '加载中';
  if (loadError.value) return '加载失败';
  return submitted.value ? '已提交 · 可修改' : '未提交';
});

const statusType = computed(() => {
  if (loadError.value) return 'red';
  return submitted.value ? 'green' : 'gray';
});

const submittedHint = computed(() => {
  if (!submitted.value) return '';
  const updated = updatedAt.value ? `最近更新：${formatDateTime(updatedAt.value)}。` : '';
  return `信息已提交，学生可查看。${updated}再次修改后提交将更新本人原记录，不影响其他导师。`;
});

const plannedQuotaError = computed(() => {
  const value = form.plannedQuota.trim();
  if (!value) return '';
  if (!/^[1-9]\d*$/.test(value)) return '计划招收人数须为正整数';
  if (Number(value) < occupiedQuota.value) return '计划招收人数不能小于当前已占用名额';
  return '';
});

const canSubmit = computed(() => {
  if (loading.value || submitting.value || resumeUploading.value) {
    return false;
  }

  const requiredFilled = [
    form.labName,
    form.location,
    form.teacherName,
    form.researchDirection,
    form.contact,
  ].every((value) => value.trim() !== '');

  return Boolean(
    requiredFilled &&
      form.resumeFileId &&
      !plannedQuotaError.value &&
      /^[1-9]\d*$/.test(form.plannedQuota.trim()),
  );
});

function resetFormForNew() {
  form.labName = '';
  form.location = '';
  form.plannedQuota = '';
  form.teacherName = userStore.user?.name || '';
  form.researchDirection = '';
  form.contact = '';
  form.resumeFileId = null;
  form.resumeFileName = '';
  updatedAt.value = '';
}

function applyLab(lab) {
  if (!lab) {
    return;
  }

  form.labName = lab.labName ?? form.labName;
  form.location = lab.location ?? form.location;
  form.plannedQuota = lab.plannedQuota != null ? String(lab.plannedQuota) : form.plannedQuota;
  form.teacherName = lab.teacherName ?? form.teacherName;
  form.researchDirection = lab.researchDirection ?? form.researchDirection;
  form.contact = lab.contact ?? form.contact;
  form.resumeFileId = lab.resumeFileId ?? form.resumeFileId;
  form.resumeFileName = lab.resumeFileName || form.resumeFileName;
  updatedAt.value = lab.updatedAt || updatedAt.value;
}

async function loadLabInfo() {
  loading.value = true;
  loadError.value = false;

  try {
    const data = await getTeacherLab();
    submitted.value = Boolean(data?.submitted && data?.lab);
    occupiedQuota.value = Number(data?.occupiedQuota ?? 0);
    remainingQuota.value = data?.remainingQuota ?? null;

    if (data?.lab) {
      applyLab(data.lab);
    } else {
      resetFormForNew();
    }
  } catch {
    loadError.value = true;
  } finally {
    loading.value = false;
  }
}

function handleResumeUploaded(info) {
  form.resumeFileId = info?.fileId ?? null;
  form.resumeFileName = info?.originalName || '';
}

function handleResumeUploading(value) {
  resumeUploading.value = value;
}

function applyLabData(data) {
  submitted.value = Boolean(data?.submitted && data?.lab);
  occupiedQuota.value = Number(data?.occupiedQuota ?? occupiedQuota.value);
  remainingQuota.value = data?.remainingQuota ?? remainingQuota.value;

  if (data?.lab) {
    applyLab(data.lab);
  }
}

async function handleSubmit() {
  if (!canSubmit.value) {
    return;
  }

  submitting.value = true;

  try {
    const data = await saveTeacherLab({
      labName: form.labName.trim(),
      location: form.location.trim(),
      plannedQuota: Number(form.plannedQuota),
      teacherName: form.teacherName.trim(),
      researchDirection: form.researchDirection.trim(),
      contact: form.contact.trim(),
      resumeFileId: form.resumeFileId,
    });

    applyLabData(data);
    showToast(data?.message || '提交成功', 'success');
  } catch {
    // 真实接口错误由 request.js 统一提示；保留当前填写内容。
    return;
  } finally {
    submitting.value = false;
  }
}

onMounted(loadLabInfo);
</script>

<style scoped>
.lab-card {
  max-width: 1040px;
  padding: 24px 26px;
}

.banner-ok,
.banner-info {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
  padding: 13px 16px;
  border-radius: 10px;
  font-size: 13px;
  color: var(--text2);
}

.banner-ok {
  border: 1px solid rgba(52, 211, 153, 0.35);
  background: rgba(52, 211, 153, 0.06);
}

.banner-info {
  border: 1px solid var(--line);
  background: rgba(255, 154, 69, 0.05);
}

.banner-icon {
  flex: 0 0 auto;
  color: var(--green);
}

.banner-info .banner-icon {
  color: var(--blue);
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0 28px;
}

.form-grid .full {
  grid-column: 1 / -1;
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

.actions {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
  margin-top: 6px;
}

.actions .help {
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
  .lab-card {
    padding: 20px 18px;
  }

  .form-grid {
    grid-template-columns: 1fr;
    gap: 0;
  }
}
</style>
