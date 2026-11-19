<template>
  <div class="login-page">
    <div class="grid-bg" aria-hidden="true"></div>

    <main class="login-wrap">
      <header class="brand">
        <div class="brand-mark" aria-hidden="true">
          <svg
            width="28"
            height="28"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="1.6"
            stroke-linecap="round"
            stroke-linejoin="round"
          >
            <path d="M12 3 21 19 H3 Z" />
            <circle cx="12" cy="14.5" r="2.4" />
          </svg>
        </div>
        <div class="brand-t">
          <h1>实训室选拔系统</h1>
          <p>Software Lab Selection System</p>
        </div>
      </header>

      <section class="card" aria-labelledby="loginTitle">
        <h2 id="loginTitle" class="sec-title">选择登录身份</h2>

        <div class="roles" role="radiogroup" aria-label="登录身份">
          <button
            class="role"
            :class="{ on: selectedRole === 'STUDENT' }"
            type="button"
            role="radio"
            :aria-checked="selectedRole === 'STUDENT'"
            :disabled="submitting"
            @click="selectRole('STUDENT')"
          >
            <svg
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="1.7"
              stroke-linecap="round"
              stroke-linejoin="round"
              aria-hidden="true"
            >
              <circle cx="12" cy="8" r="4" />
              <path d="M4 21c0-4 3.6-6.5 8-6.5s8 2.5 8 6.5" />
            </svg>
            <span>学生</span>
          </button>

          <button
            class="role"
            :class="{ on: selectedRole === 'TEACHER' }"
            type="button"
            role="radio"
            :aria-checked="selectedRole === 'TEACHER'"
            :disabled="submitting"
            @click="selectRole('TEACHER')"
          >
            <svg
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="1.7"
              stroke-linecap="round"
              stroke-linejoin="round"
              aria-hidden="true"
            >
              <path d="M4 20 8 6l8-2 4 14" />
              <path d="m8 6 12 6-8 4-4-6" />
            </svg>
            <span>导师</span>
          </button>

          <button
            class="role"
            :class="{ on: selectedRole === 'MANAGER' }"
            type="button"
            role="radio"
            :aria-checked="selectedRole === 'MANAGER'"
            :disabled="submitting"
            @click="selectRole('MANAGER')"
          >
            <svg
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="1.7"
              stroke-linecap="round"
              stroke-linejoin="round"
              aria-hidden="true"
            >
              <path d="M3 8l4-4 4 4M3 16l4 4 4-4" />
              <path d="M7 4v8M7 12v8" />
              <circle cx="16" cy="12" r="3" />
              <path d="M14.5 12H12m8 0h-2.5" />
            </svg>
            <span>负责人</span>
          </button>
        </div>

        <div class="field">
          <label class="label" for="staffNo">学工号 <span class="req">*</span></label>
          <input
            id="staffNo"
            v-model.trim="staffNo"
            class="input"
            :class="{ err: Boolean(errorMessage) }"
            type="text"
            inputmode="numeric"
            maxlength="10"
            placeholder="请输入 10 位学工号"
            autocomplete="off"
            :disabled="verified || submitting"
            :aria-invalid="Boolean(errorMessage)"
            aria-describedby="staffNoError"
            @input="handleStaffNoInput"
            @keyup.enter="handleLogin"
          />
          <p id="staffNoError" class="error" role="alert" aria-live="polite">
            {{ errorMessage }}
          </p>
        </div>

        <div class="face-row">
          <button
            class="btn btn-face"
            type="button"
            :disabled="verified || submitting"
            @click="handleFaceVerify"
          >
            人脸比对
          </button>

          <div class="face-state" :class="{ ok: verified }">
            <svg
              v-if="verified"
              width="15"
              height="15"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
              stroke-linecap="round"
              stroke-linejoin="round"
              aria-hidden="true"
            >
              <path d="m5 12 4 4L19 6" />
            </svg>
            <span>{{ verified ? '比对成功' : '模拟比对，不采集人脸数据' }}</span>
          </div>
        </div>

        <button class="btn btn-primary" type="button" :disabled="!canLogin" @click="handleLogin">
          <span v-if="submitting" class="spinner" aria-hidden="true"></span>
          {{ submitting ? '登录中...' : '进入系统' }}
        </button>

        <p class="foot">
          无需注册、无需密码。身份以所选为准，学工号格式为 10 位数字（四位年份 + 三位代码 +
          三位序号）。<br />
          人脸比对仅为模拟操作，不调用摄像头，不采集、上传或存储人脸数据。
        </p>
      </section>

      <p class="copyright">软件学院 · 实训室选拔系统</p>
    </main>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useUserStore } from '../stores/user';
import { isValidStaffNo } from '../utils/validate';

const route = useRoute();
const router = useRouter();
const userStore = useUserStore();

const selectedRole = ref(null);
const staffNo = ref('');
const verified = ref(false);
const verifiedIdentity = ref('');
const errorMessage = ref('');
const submitting = ref(false);

const identityKey = computed(() => `${selectedRole.value ?? ''}|${staffNo.value.trim()}`);
const canLogin = computed(() =>
  Boolean(
    selectedRole.value &&
      verified.value &&
      verifiedIdentity.value === identityKey.value &&
      !submitting.value,
  ),
);

function selectRole(role) {
  if (submitting.value) {
    return;
  }
  if (selectedRole.value !== role) {
    // 人脸比对结果只对“当前身份 + 当前学工号”有效，换身份后必须重新比对。
    selectedRole.value = role;
    resetFaceVerification();
  }
  if (errorMessage.value) {
    errorMessage.value = '';
  }
}

function handleStaffNoInput() {
  if (errorMessage.value) {
    errorMessage.value = '';
  }
  if (verified.value) {
    resetFaceVerification();
  }
}

function handleFaceVerify() {
  if (verified.value || submitting.value) {
    return;
  }

  if (!selectedRole.value) {
    errorMessage.value = '请先选择登录身份';
    return;
  }

  if (!isValidStaffNo(staffNo.value)) {
    errorMessage.value = '学工号异常';
    return;
  }

  errorMessage.value = '';
  verified.value = true;
  verifiedIdentity.value = identityKey.value;
}

function resetFaceVerification() {
  verified.value = false;
  verifiedIdentity.value = '';
}

async function handleLogin() {
  if (!canLogin.value) {
    return;
  }

  submitting.value = true;
  try {
    await userStore.login({
      role: selectedRole.value,
      staffNo: staffNo.value.trim(),
    });

    await router.replace(getSafeRedirect(route.query.redirect));
  } catch {
    // request.js 已统一弹出错误提示；保留当前填写内容，允许重试。
    return;
  } finally {
    submitting.value = false;
  }
}

function getSafeRedirect(value) {
  if (typeof value === 'string' && value.startsWith('/') && !value.startsWith('//')) {
    return value;
  }
  return '/home';
}
</script>

<style scoped>
.login-page {
  position: relative;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  overflow: auto;
  background-color: var(--bg);
  background-image:
    radial-gradient(1100px 520px at 82% -8%, rgba(255, 122, 24, 0.1), transparent 62%),
    radial-gradient(900px 520px at 8% 108%, rgba(255, 154, 69, 0.08), transparent 60%),
    linear-gradient(180deg, var(--bg2), var(--bg));
}

.grid-bg {
  position: fixed;
  inset: 0;
  pointer-events: none;
  background-image:
    linear-gradient(rgba(255, 154, 69, 0.05) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 154, 69, 0.05) 1px, transparent 1px);
  background-size: 52px 52px;
  -webkit-mask-image: radial-gradient(circle at 50% 42%, #000 0%, transparent 74%);
  mask-image: radial-gradient(circle at 50% 42%, #000 0%, transparent 74%);
}

.login-wrap {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 26px;
}

.brand {
  display: flex;
  align-items: center;
  gap: 14px;
}

.brand-mark {
  width: 52px;
  height: 52px;
  border-radius: 12px;
  display: grid;
  place-items: center;
  background: linear-gradient(135deg, rgba(255, 122, 24, 0.24), rgba(255, 154, 69, 0.07));
  border: 1px solid var(--line2);
  color: var(--cyan);
  box-shadow: 0 0 34px rgba(255, 122, 24, 0.22);
}

.brand-t h1 {
  margin: 0;
  font-size: 26px;
  font-weight: 700;
  letter-spacing: 1px;
}

.brand-t p {
  margin: 2px 0 0;
  font-family: var(--tech);
  font-size: 12px;
  letter-spacing: 3px;
  color: var(--text3);
  text-transform: uppercase;
}

.card {
  width: 472px;
  max-width: 94vw;
  padding: 30px 32px;
  border: 1px solid var(--line);
  border-radius: 16px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.94), rgba(255, 255, 255, 0.96));
  box-shadow:
    0 30px 80px rgba(15, 23, 42, 0.16),
    inset 0 1px 0 rgba(255, 255, 255, 0.03);
}

.sec-title {
  margin: 0 0 10px;
  font-size: 13px;
  font-weight: 400;
  color: var(--text2);
}

.roles {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
  margin-bottom: 24px;
}

.role {
  position: relative;
  padding: 13px 8px;
  border: 1px solid var(--line);
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.88);
  color: var(--text2);
  text-align: center;
  cursor: pointer;
  transition: 0.15s;
}

.role svg {
  width: 22px;
  height: 22px;
  display: block;
  margin: 0 auto 6px;
  color: var(--text2);
}

.role span {
  font-size: 13.5px;
}

.role:hover:not(:disabled) {
  border-color: rgba(255, 154, 69, 0.4);
  color: var(--text);
}

.role.on {
  border-color: var(--line2);
  background: rgba(255, 122, 24, 0.1);
  color: var(--cyan);
  box-shadow: 0 0 0 3px rgba(255, 122, 24, 0.1);
}

.role.on svg {
  color: var(--cyan);
}

.role:disabled {
  opacity: 0.6;
  cursor: not-allowed;
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
  padding: 11px 13px;
  border: 1px solid var(--line);
  border-radius: 9px;
  outline: none;
  background: rgba(255, 255, 255, 0.9);
  color: var(--text);
  font-family: var(--tech);
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 1.5px;
  transition: 0.15s;
}

.input:focus {
  border-color: var(--line2);
  box-shadow: 0 0 0 3px rgba(255, 122, 24, 0.12);
}

.input:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.input.err {
  border-color: rgba(248, 113, 113, 0.5);
}

.error {
  min-height: 18px;
  margin: 6px 0 0;
  font-size: 12px;
  color: var(--red);
}

.face-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
}

.face-state {
  display: flex;
  align-items: center;
  gap: 7px;
  font-size: 13px;
  color: var(--text3);
}

.face-state.ok {
  color: var(--green);
}

.btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  width: 100%;
  padding: 12px 18px;
  border: 1px solid var(--line2);
  border-radius: 9px;
  background: var(--panel2);
  color: var(--text);
  font-family: inherit;
  font-size: 15px;
  font-weight: 600;
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
}

.btn-primary:hover:not(:disabled) {
  filter: brightness(1.07);
}

.btn-face {
  width: auto;
  padding: 11px 22px;
  border-color: var(--line);
  background: transparent;
  font-weight: 500;
}

.btn-face:hover:not(:disabled) {
  border-color: var(--line2);
  color: var(--cyan);
}

.spinner {
  width: 15px;
  height: 15px;
  border: 2px solid rgba(255, 255, 255, 0.45);
  border-top-color: #ffffff;
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
}

.foot {
  margin: 18px 0 0;
  padding-top: 16px;
  border-top: 1px solid var(--line);
  font-size: 12px;
  line-height: 1.8;
  color: var(--text3);
}

.copyright {
  margin: 0;
  font-size: 12px;
  color: var(--text3);
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

@media (max-width: 520px) {
  .card {
    padding: 24px 20px;
  }

  .brand-t h1 {
    font-size: 22px;
  }

  .face-row {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
