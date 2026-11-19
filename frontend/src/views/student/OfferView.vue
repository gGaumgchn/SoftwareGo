<template>
  <DefaultLayout title="拟录取确认">
    <section class="card offer-card">
      <div class="card-head">
        <div class="card-title"><span class="dot"></span>拟录取确认</div>
        <StatusBadge :type="headBadgeType">{{ headBadgeText }}</StatusBadge>
      </div>

      <EmptyState v-if="loading" kind="loading" />
      <EmptyState v-else-if="loadError" kind="error" @retry="loadData" />

      <template v-else>
        <div v-if="finalAdmission" class="admission-panel">
          <div class="panel-head">
            <div class="panel-title">{{ isAdjustment ? '调剂录取结果' : '最终录取结果' }}</div>
            <StatusBadge :type="isAdjustment ? 'cyan' : 'green'">
              {{ isAdjustment ? '已录取 · 无需确认' : '已确认录取' }}
            </StatusBadge>
          </div>

          <div class="offer final-offer">
            <div class="o-ico o-ico--ok">
              <BaseIcon name="check" :size="22" />
            </div>
            <div class="o-body">
              <div class="o-lab">{{ finalAdmission.labName || '—' }}</div>
              <div class="o-meta">
                <span
                  >导师 <b>{{ finalAdmission.teacherName || '—' }}</b></span
                >
                <span
                  >录取方式 <b>{{ admissionTypeLabel }}</b></span
                >
                <span v-if="finalAdmission.roundNo">
                  轮次 <b>{{ roundText(finalAdmission.roundNo) }}</b>
                </span>
                <span
                  >录取时间 <b>{{ formatDateTime(finalAdmission.admittedAt) }}</b></span
                >
              </div>
              <div v-if="finalAdmission.location" class="o-extra">
                实训室位置：{{ finalAdmission.location }}
              </div>
            </div>
          </div>

          <div class="banner" :class="isAdjustment ? 'banner-info' : 'banner-ok'">
            <BaseIcon
              :name="isAdjustment ? 'alert-circle' : 'check'"
              :size="18"
              class="banner-icon"
              :class="isAdjustment ? 'banner-icon--info' : 'banner-icon--ok'"
            />
            <span v-if="isAdjustment">
              三轮结束后的统筹调剂直接形成录取结果，无需确认，仅展示最终去向。
            </span>
            <span v-else>已确认录取去向，其余拟录取已自动放弃，不可再更改。</span>
          </div>
        </div>

        <template v-if="offers.length > 0">
          <div class="section-head">
            <div class="section-title">{{ admitted ? '历史拟录取' : '本人拟录取' }}</div>
            <span v-if="actionableOffers.length > 0" class="section-note">
              待确认 {{ actionableOffers.length }} 条
            </span>
          </div>

          <div v-if="actionableOffers.length > 0" class="banner banner-warn">
            <BaseIcon name="alert-circle" :size="18" class="banner-icon banner-icon--warn" />
            <span>
              请在本轮确认截止时间前选择一条拟录取并主动确认；确认后不可更改，其他拟录取将自动放弃。
            </span>
          </div>

          <div class="offer-list">
            <article
              v-for="offer in offers"
              :key="offer.offerId"
              class="offer"
              :class="{ 'offer--final': displayStatus(offer) === 'CONFIRMED' }"
            >
              <div class="o-ico">
                <BaseIcon name="building" :size="22" />
              </div>

              <div class="o-body">
                <div class="o-lab">{{ offer.labName || '—' }}</div>
                <div class="o-meta">
                  <span
                    >导师 <b>{{ offer.teacherName || '—' }}</b></span
                  >
                  <span
                    >轮次 <b>{{ roundText(offer.roundNo) }}</b></span
                  >
                  <span>
                    确认截止
                    <b class="deadline">{{ formatDateTime(offer.confirmDeadline) }}</b>
                  </span>
                  <span v-if="displayStatus(offer) === 'PENDING'" class="remaining">
                    {{ remainingText(offer) }}
                  </span>
                  <span v-else-if="offer.status === 'CONFIRMED' && offer.confirmedAt">
                    确认时间 <b>{{ formatDateTime(offer.confirmedAt) }}</b>
                  </span>
                </div>
              </div>

              <div class="o-actions">
                <StatusBadge :type="statusType(displayStatus(offer))">
                  {{ statusText(displayStatus(offer)) }}
                </StatusBadge>
                <button
                  v-if="displayStatus(offer) === 'PENDING'"
                  class="btn btn-primary"
                  type="button"
                  :disabled="!isOfferActionable(offer)"
                  @click="openConfirm(offer)"
                >
                  {{ confirmActionText(offer) }}
                </button>
              </div>
            </article>
          </div>
        </template>

        <EmptyState v-else-if="!finalAdmission" text="暂无拟录取" />
      </template>
    </section>

    <BaseModal :open="Boolean(confirmTarget)" aria-label="确认拟录取" @close="closeConfirm">
      <div v-if="confirmTarget" class="confirm-body">
        <h3 class="confirm-title">确认拟录取</h3>
        <p class="confirm-text">
          你正在确认 <strong>{{ confirmTarget.labName }}</strong> · 导师
          <strong>{{ confirmTarget.teacherName }}</strong> 的拟录取。
        </p>
        <p class="confirm-warn">确认后不可更改，其他拟录取将自动放弃。</p>
        <p v-if="confirmTargetExpired" class="confirm-expired">
          确认时间已截止，无法确认该拟录取。
        </p>

        <div class="m-actions">
          <button class="btn" type="button" :disabled="confirming" @click="closeConfirm">
            取消
          </button>
          <button
            class="btn btn-primary"
            type="button"
            :disabled="confirming || confirmTargetExpired"
            @click="submitConfirm"
          >
            {{ confirming ? '确认中…' : '确认录取' }}
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
import { confirmOffer, getAdmission, getOffers } from '../../api/offer';
import { useAdmissionStore } from '../../stores/admission';
import { formatDateTime, getServerNow, parseServerDate, setServerTime } from '../../utils/time';
import { showToast } from '../../utils/toast';

const ROUND_LABELS = {
  1: '第一轮录取',
  2: '第二轮录取',
  3: '第三轮录取',
};

const ADMISSION_TYPE_LABELS = {
  VOLUNTEER: '志愿录取',
  ADJUST_INTRA_LAB: '实训室内调剂',
  ADJUST_COLLEGE: '学院内调剂',
};

const STATUS_META = {
  PENDING: { text: '待确认', type: 'cyan' },
  CONFIRMED: { text: '已确认', type: 'green' },
  ABANDONED: { text: '已放弃', type: 'red' },
};

const admissionStore = useAdmissionStore();

const loading = ref(true);
const loadError = ref(false);
const offers = ref([]);
const finalAdmission = ref(null);
const confirmTarget = ref(null);
const confirming = ref(false);
const nowTick = ref(Date.now());

let loadSeq = 0;
let clockTimer = null;

const admitted = computed(() => Boolean(finalAdmission.value));

const isAdjustment = computed(() => {
  const type = finalAdmission.value?.admissionType;
  return type === 'ADJUST_INTRA_LAB' || type === 'ADJUST_COLLEGE';
});

const admissionTypeLabel = computed(
  () => ADMISSION_TYPE_LABELS[finalAdmission.value?.admissionType] || '—',
);

const hasConfirmedOffer = computed(() =>
  offers.value.some((offer) => offer.status === 'CONFIRMED'),
);

const confirmBlocked = computed(() => admitted.value || hasConfirmedOffer.value);

const nowMs = computed(() => {
  const tick = nowTick.value;
  return tick + (getServerNow().getTime() - Date.now());
});

const actionableOffers = computed(() => offers.value.filter(isOfferActionable));

const actionableCount = computed(() => actionableOffers.value.length);

const headBadgeText = computed(() => {
  if (loading.value) return '加载中…';
  if (loadError.value) return '加载失败';
  if (admitted.value) return isAdjustment.value ? '已录取 · 调剂' : '已确认录取';
  if (actionableCount.value > 0) return `待确认 ${actionableCount.value} 条`;
  if (offers.value.length > 0) return '已结束';
  return '暂无拟录取';
});

const headBadgeType = computed(() => {
  if (loadError.value) return 'red';
  if (admitted.value) return isAdjustment.value ? 'cyan' : 'green';
  if (actionableCount.value > 0) return 'cyan';
  return 'gray';
});

const confirmTargetExpired = computed(
  () => Boolean(confirmTarget.value) && isOfferExpired(confirmTarget.value),
);

function pad(value) {
  return String(value).padStart(2, '0');
}

function roundText(roundNo) {
  if (!roundNo) return '—';
  return ROUND_LABELS[roundNo] || `第${roundNo}轮录取`;
}

function statusText(status) {
  return STATUS_META[status]?.text || '—';
}

function statusType(status) {
  return STATUS_META[status]?.type || 'gray';
}

function isOfferExpired(offer) {
  const deadline = parseServerDate(offer?.confirmDeadline);
  if (!deadline) {
    return false;
  }
  return nowMs.value >= deadline.getTime();
}

function displayStatus(offer) {
  // 截止或已有录取结果后，前端先行展示为已放弃；服务端为最终权威，刷新后以接口状态为准。
  if (offer?.status === 'PENDING' && (isOfferExpired(offer) || confirmBlocked.value)) {
    return 'ABANDONED';
  }
  return offer?.status || '';
}

function isOfferActionable(offer) {
  return (
    offer?.status === 'PENDING' &&
    Boolean(offer.canConfirm) &&
    !confirmBlocked.value &&
    !isOfferExpired(offer)
  );
}

function remainingText(offer) {
  const deadline = parseServerDate(offer?.confirmDeadline);
  if (!deadline) {
    return '';
  }

  const diff = deadline.getTime() - nowMs.value;
  if (diff <= 0) {
    return '已截止';
  }

  const totalSeconds = Math.floor(diff / 1000);
  const days = Math.floor(totalSeconds / 86400);
  const hours = Math.floor((totalSeconds % 86400) / 3600);
  const minutes = Math.floor((totalSeconds % 3600) / 60);
  const seconds = totalSeconds % 60;
  const dayText = days > 0 ? `${days}天 ` : '';

  return `剩余 ${dayText}${pad(hours)}:${pad(minutes)}:${pad(seconds)}`;
}

function confirmActionText(offer) {
  if (isOfferExpired(offer)) return '已截止';
  if (confirmBlocked.value) return '已结束';
  return offer.canConfirm ? '确认' : '不可确认';
}

async function loadData({ silent = false } = {}) {
  const seq = ++loadSeq;

  if (!silent) {
    loading.value = true;
    loadError.value = false;
  }

  try {
    const [offersData, admissionData] = await Promise.all([getOffers(), getAdmission()]);

    if (seq !== loadSeq) {
      return;
    }

    setServerTime(offersData?.serverTime);
    offers.value = Array.isArray(offersData?.list) ? offersData.list : [];
    finalAdmission.value = admissionData?.admitted ? admissionData.admission : null;
    admissionStore.setAdmission(finalAdmission.value);
    loadError.value = false;
  } catch {
    if (seq !== loadSeq) {
      return;
    }
    if (!silent) {
      loadError.value = true;
      offers.value = [];
      finalAdmission.value = null;
    }
  } finally {
    if (seq === loadSeq && !silent) {
      loading.value = false;
    }
  }
}

function openConfirm(offer) {
  if (confirming.value || !isOfferActionable(offer)) {
    return;
  }
  confirmTarget.value = offer;
}

function closeConfirm() {
  if (confirming.value) {
    return;
  }
  confirmTarget.value = null;
}

function applyConfirmResult(offerId, data) {
  const abandonedIds = new Set((data?.abandonedOfferIds || []).map((id) => Number(id)));

  offers.value = offers.value.map((offer) => {
    if (Number(offer.offerId) === Number(offerId)) {
      return {
        ...offer,
        status: data?.status || 'CONFIRMED',
        confirmedAt: data?.confirmedAt ?? offer.confirmedAt,
        canConfirm: false,
      };
    }

    if (abandonedIds.has(Number(offer.offerId)) || offer.status === 'PENDING') {
      return {
        ...offer,
        status: 'ABANDONED',
        abandonReason: 'CONFIRMED_OTHER',
        canConfirm: false,
      };
    }

    return { ...offer, canConfirm: false };
  });

  if (data?.finalAdmission) {
    finalAdmission.value = data.finalAdmission;
    admissionStore.setAdmission(data.finalAdmission);
  }
}

async function submitConfirm() {
  if (!confirmTarget.value || confirming.value || confirmTargetExpired.value) {
    return;
  }

  const offerId = confirmTarget.value.offerId;
  confirming.value = true;

  try {
    const data = await confirmOffer(offerId);
    applyConfirmResult(offerId, data);
    confirmTarget.value = null;
    showToast('确认成功', 'success');
    loadData({ silent: true });
  } catch {
    // 真实接口错误由 request.js 统一提示；随后刷新，以服务端保存的结果为准。
    confirmTarget.value = null;
    loadData({ silent: true });
  } finally {
    confirming.value = false;
  }
}

onMounted(() => {
  clockTimer = window.setInterval(() => {
    nowTick.value = Date.now();
  }, 1000);
  loadData();
});

onUnmounted(() => {
  if (clockTimer) {
    window.clearInterval(clockTimer);
  }
  loadSeq += 1;
});
</script>

<style scoped>
.offer-card {
  max-width: 1040px;
  padding: 24px 26px;
}

.admission-panel {
  margin-bottom: 24px;
}

.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}

.panel-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--text);
}

.final-offer {
  margin-bottom: 12px;
  border-color: rgba(52, 211, 153, 0.3);
  background: rgba(52, 211, 153, 0.04);
}

.o-ico--ok {
  background: rgba(52, 211, 153, 0.1);
  color: var(--green);
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

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text);
}

.section-note {
  font-size: 12.5px;
  color: var(--amber);
}

.offer-list {
  display: flex;
  flex-direction: column;
}

.offer {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 14px;
  padding: 18px 20px;
  border: 1px solid var(--line);
  border-radius: 12px;
  background: rgba(250, 250, 252, 0.92);
}

.o-ico {
  display: grid;
  flex: 0 0 44px;
  place-items: center;
  width: 44px;
  height: 44px;
  border-radius: 11px;
  background: rgba(255, 154, 69, 0.1);
  color: var(--blue);
}

.o-body {
  flex: 1;
  min-width: 0;
}

.o-lab {
  font-size: 15px;
  font-weight: 600;
  color: var(--text);
}

.o-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 5px 18px;
  margin-top: 5px;
  font-size: 12.5px;
  color: var(--text2);
}

.o-meta b {
  font-weight: 400;
  color: var(--text3);
}

.o-meta .deadline {
  font-family: var(--tech);
  font-size: 13px;
  color: var(--amber);
}

.o-meta .remaining {
  font-family: var(--tech);
  font-size: 13px;
  color: var(--amber);
}

.o-extra {
  margin-top: 6px;
  font-size: 12.5px;
  color: var(--text3);
}

.o-actions {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  gap: 12px;
}

.offer--final {
  border-color: rgba(52, 211, 153, 0.3);
}

.confirm-title {
  margin: 0 0 12px;
  font-size: 16px;
  font-weight: 600;
  color: var(--text);
}

.confirm-text {
  margin: 0 0 10px;
  font-size: 13.5px;
  line-height: 1.7;
  color: var(--text2);
}

.confirm-text strong {
  color: var(--text);
  font-weight: 600;
}

.confirm-warn {
  margin: 0 0 22px;
  font-size: 13px;
  color: var(--amber);
}

.confirm-expired {
  margin: -10px 0 18px;
  font-size: 12.5px;
  color: var(--red);
}

.m-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
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

.btn-primary {
  border-color: rgba(255, 122, 24, 0.5);
  background: linear-gradient(180deg, rgba(255, 122, 24, 0.92), rgba(255, 154, 69, 0.78));
  color: #ffffff;
  font-weight: 600;
}

.btn-primary:hover:not(:disabled) {
  filter: brightness(1.07);
}

@media (max-width: 760px) {
  .offer-card {
    padding: 20px 18px;
  }

  .offer {
    flex-wrap: wrap;
    align-items: flex-start;
  }

  .o-actions {
    width: 100%;
    justify-content: flex-end;
  }
}
</style>
