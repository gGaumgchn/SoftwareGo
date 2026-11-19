<template>
  <DefaultLayout title="主页面">
    <div v-if="isStudent && admissionError" class="admit-banner admit-banner--error">
      <div class="a-main">
        <div class="a-ico">
          <BaseIcon name="alert-circle" :size="20" />
        </div>
        <div>
          <div class="a-t">录取状态加载失败</div>
          <div class="a-d">加载失败，请重试。</div>
        </div>
      </div>
      <button class="banner-link" type="button" @click="loadAdmission">重试</button>
    </div>

    <div v-else-if="isStudent && admission" class="admit-banner">
      <div class="a-main">
        <div class="a-ico">
          <BaseIcon name="check" :size="20" />
        </div>
        <div>
          <div class="a-t">已录取</div>
          <div class="a-d">{{ admissionDestination }}</div>
        </div>
      </div>
      <StatusBadge type="green">最终录取结果</StatusBadge>
    </div>

    <section class="card">
      <div class="card-head">
        <div class="card-title"><span class="dot"></span>选拔进度</div>
        <StatusBadge :type="stageBadgeType">{{ stageBadgeText }}</StatusBadge>
      </div>

      <EmptyState v-if="stageLoading" kind="loading" />
      <EmptyState v-else-if="stageError" kind="error" @retry="loadStage" />
      <template v-else>
        <StepBar
          :stages="stepStages"
          :current-stage-code="currentStageCode"
          :next-stage-code="nextStageCode"
        />

        <div class="stage-summary">
          <div class="stage-now">
            <div class="k">{{ stageNameLabel }}</div>
            <div class="v">
              {{ currentStageName }}
              <small v-if="currentSubStageLabel">· {{ currentSubStageLabel }}</small>
            </div>
          </div>

          <div class="stage-now">
            <div class="k">{{ stageTimeLabel }}</div>
            <div class="v stage-time">{{ stageTimeText }}</div>
          </div>

          <div v-if="countdownTarget" class="stage-now">
            <div class="k">截止倒计时</div>
            <Countdown :target="countdownTarget" :label="countdownLabel" />
          </div>
        </div>
      </template>
    </section>

    <div class="grid-2">
      <section class="card">
        <div class="card-head">
          <div class="card-title"><span class="dot"></span>公告</div>
          <RouterLink class="card-link" to="/announcements">查看全部</RouterLink>
        </div>

        <EmptyState v-if="announcementsLoading" kind="loading" />
        <EmptyState v-else-if="announcementsError" kind="error" @retry="loadAnnouncements" />
        <EmptyState v-else-if="announcements.length === 0" kind="empty" text="暂无公告" />

        <div v-else class="notice-list">
          <RouterLink
            v-for="notice in announcements"
            :key="notice.id"
            class="notice"
            :to="`/announcements/${notice.id}`"
          >
            <div class="n-ico">
              <BaseIcon name="bell" :size="15" />
            </div>
            <div class="n-body">
              <div class="n-t">{{ notice.title }}</div>
              <div class="n-s">{{ notice.summary }}</div>
              <div class="n-time">{{ formatDateTime(notice.publishedAt) }}</div>
            </div>
          </RouterLink>
        </div>
      </section>

      <section class="card">
        <div class="card-head">
          <div class="card-title"><span class="dot"></span>个人待办</div>
          <RouterLink class="card-link" to="/todos">查看全部</RouterLink>
        </div>

        <EmptyState v-if="todosLoading" kind="loading" />
        <EmptyState v-else-if="todosError" kind="error" @retry="loadTodos" />
        <EmptyState v-else-if="visibleTodos.length === 0" kind="empty" text="暂无待办" />

        <div v-else class="todo-list">
          <RouterLink
            v-for="(todo, index) in visibleTodos"
            :key="`${todo.type}-${index}`"
            class="todo"
            to="/todos"
          >
            <div class="t-ico">
              <BaseIcon :name="todoIcon(todo.type)" :size="17" />
            </div>
            <div class="t-body">
              <div class="t-t">{{ todo.title }}</div>
              <div class="t-d">{{ todoMeta(todo) }}</div>
            </div>
            <div class="t-go">{{ todo.actionLabel || '前往' }} ›</div>
          </RouterLink>
        </div>
      </section>
    </div>
  </DefaultLayout>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue';
import DefaultLayout from '../layouts/DefaultLayout.vue';
import BaseIcon from '../components/BaseIcon.vue';
import StatusBadge from '../components/StatusBadge.vue';
import EmptyState from '../components/EmptyState.vue';
import StepBar from '../components/StepBar.vue';
import Countdown from '../components/Countdown.vue';
import { getCurrentStage } from '../api/stage';
import { getAnnouncements } from '../api/announcement';
import { getTodos } from '../api/todo';
import { useUserStore } from '../stores/user';
import { useAdmissionStore } from '../stores/admission';
import { formatDateTime, setServerTime } from '../utils/time';

const STAGE_DEFINITIONS = [
  { stageCode: 'BRIEFING', stageName: '集中宣讲', sortNo: 1, startAt: null, endAt: null, state: 'UNCONFIGURED' },
  { stageCode: 'OPEN_DAY', stageName: '实训室开放日', sortNo: 2, startAt: null, endAt: null, state: 'UNCONFIGURED' },
  { stageCode: 'ROUND_1', stageName: '第一轮录取', sortNo: 3, startAt: null, endAt: null, state: 'UNCONFIGURED' },
  { stageCode: 'ROUND_2', stageName: '第二轮录取', sortNo: 4, startAt: null, endAt: null, state: 'UNCONFIGURED' },
  { stageCode: 'ROUND_3', stageName: '第三轮录取', sortNo: 5, startAt: null, endAt: null, state: 'UNCONFIGURED' },
  { stageCode: 'ADJUSTMENT', stageName: '统筹调剂', sortNo: 6, startAt: null, endAt: null, state: 'UNCONFIGURED' },
  { stageCode: 'ENROLLMENT', stageName: '正式入组', sortNo: 7, startAt: null, endAt: null, state: 'UNCONFIGURED' },
];

const STAGE_STATUS_LABELS = {
  ONGOING: '进行中',
  BETWEEN: '阶段间隙',
  ENDED: '已结束',
  UNCONFIGURED: '未配置',
};

const TODO_ICONS = {
  VOLUNTEER_SUBMIT: 'edit',
  VOLUNTEER_MAKEUP: 'edit',
  OFFER_CONFIRM: 'check',
  LAB_INFO: 'building',
  SCORE_ENTRY: 'users',
  GRANT_OFFER: 'users',
  STAGE_CONFIG: 'calendar',
};

const ADMISSION_TYPE_LABELS = {
  VOLUNTEER: '志愿录取',
  ADJUST_INTRA_LAB: '实训室内调剂',
  ADJUST_COLLEGE: '学院内调剂',
};

const userStore = useUserStore();
const admissionStore = useAdmissionStore();

const stageLoading = ref(false);
const stageError = ref(false);
const stageData = ref(null);

const announcementsLoading = ref(false);
const announcementsError = ref(false);
const announcements = ref([]);

const todosLoading = ref(false);
const todosError = ref(false);
const todos = ref([]);

const isStudent = computed(() => userStore.role === 'STUDENT');
const admission = computed(() => admissionStore.admission);
const admitted = computed(() => admissionStore.admitted);
const admissionError = computed(() => admissionStore.error);

const visibleTodos = computed(() => {
  if (!admitted.value) {
    return todos.value;
  }

  const blockedTypes = new Set(['VOLUNTEER_SUBMIT', 'VOLUNTEER_MAKEUP', 'OFFER_CONFIRM']);
  return todos.value.filter((todo) => !blockedTypes.has(todo.type));
});

const stageStatus = computed(() => stageData.value?.status || '');
const currentStageCode = computed(() => stageData.value?.currentStage?.stageCode || '');
const nextStageCode = computed(() => stageData.value?.nextStage?.stageCode || '');
const stepStages = computed(() =>
  stageData.value?.stages?.length ? stageData.value.stages : STAGE_DEFINITIONS,
);

const stageBadgeText = computed(() => {
  if (stageLoading.value) return '加载中';
  if (stageError.value) return '加载失败';
  return STAGE_STATUS_LABELS[stageStatus.value] || '—';
});

const stageBadgeType = computed(() => {
  if (stageError.value) return 'red';
  if (stageStatus.value === 'ONGOING') return 'cyan';
  if (stageStatus.value === 'BETWEEN') return 'amber';
  return 'gray';
});

const isRoundStage = computed(() => currentStageCode.value.startsWith('ROUND_'));

const currentStageName = computed(() => {
  if (stageStatus.value === 'ONGOING') return stageData.value?.currentStage?.stageName || '—';
  if (stageStatus.value === 'BETWEEN') return stageData.value?.nextStage?.stageName || '—';
  if (stageStatus.value === 'ENDED') return '选拔已结束';
  if (stageStatus.value === 'UNCONFIGURED') return '阶段暂未配置';
  return '—';
});

const stageNameLabel = computed(() => {
  if (stageStatus.value === 'BETWEEN') return '下一阶段';
  if (stageStatus.value === 'ENDED') return '选拔状态';
  if (stageStatus.value === 'UNCONFIGURED') return '阶段状态';
  return '当前阶段';
});

const stageTimeLabel = computed(() =>
  stageStatus.value === 'BETWEEN' ? '开始时间' : '起止时间',
);

const stageTimeText = computed(() => {
  if (stageStatus.value === 'ONGOING') {
    const stage = stageData.value?.currentStage;
    return stage ? `${formatDateTime(stage.startAt)} ~ ${formatDateTime(stage.endAt)}` : '—';
  }
  if (stageStatus.value === 'BETWEEN') {
    const stage = stageData.value?.nextStage;
    return stage ? `${formatDateTime(stage.startAt)} 开始` : '—';
  }
  return '—';
});

const countdownTarget = computed(() => {
  if (stageStatus.value === 'ONGOING') return stageData.value?.currentStage?.endAt || '';
  if (stageStatus.value === 'BETWEEN') return stageData.value?.nextStage?.startAt || '';
  return '';
});

const countdownLabel = computed(() => {
  if (stageStatus.value === 'ONGOING') return isRoundStage.value ? '距本轮结束' : '距本阶段结束';
  if (stageStatus.value === 'BETWEEN') return '距下一阶段开始';
  return '';
});

const currentSubStageLabel = computed(() => {
  if (!isRoundStage.value) return '';

  const types = new Set(visibleTodos.value.map((todo) => todo.type));
  if (types.has('OFFER_CONFIRM')) return '拟录取确认';
  if (types.has('VOLUNTEER_MAKEUP')) return '补录填报';
  if (types.has('VOLUNTEER_SUBMIT')) return '志愿填报';
  if (types.has('SCORE_ENTRY')) return '面试';
  if (types.has('GRANT_OFFER')) return '发放拟录取';
  return '';
});

const admissionTypeLabel = computed(
  () => ADMISSION_TYPE_LABELS[admission.value?.admissionType] || '录取',
);

const admissionRoundText = computed(() => {
  const roundNo = admission.value?.roundNo;
  if (!roundNo) return '';
  const labels = { 1: '第一轮录取', 2: '第二轮录取', 3: '第三轮录取' };
  return labels[roundNo] || `第 ${roundNo} 轮录取`;
});

const admissionDestination = computed(() => {
  const item = admission.value;
  if (!item) return '';

  return [
    `最终去向：${item.labName || '—'}`,
    `导师 ${item.teacherName || '—'}`,
    admissionTypeLabel.value,
    admissionRoundText.value,
  ]
    .filter(Boolean)
    .join(' · ');
});

function todoIcon(type) {
  return TODO_ICONS[type] || 'todo';
}

function todoMeta(todo) {
  const parts = [todo.stageName, todo.deadline ? `截止 ${formatDateTime(todo.deadline)}` : ''];
  return parts.filter(Boolean).join(' · ') || '当前可办理';
}

async function loadStage() {
  stageLoading.value = true;
  stageError.value = false;

  try {
    const data = await getCurrentStage();
    stageData.value = data;
    setServerTime(data?.serverTime);
  } catch {
    stageData.value = null;
    stageError.value = true;
  } finally {
    stageLoading.value = false;
  }
}

async function loadAnnouncements() {
  announcementsLoading.value = true;
  announcementsError.value = false;

  try {
    const data = await getAnnouncements({ page: 1, pageSize: 3 });
    announcements.value = data?.list || [];
  } catch {
    announcements.value = [];
    announcementsError.value = true;
  } finally {
    announcementsLoading.value = false;
  }
}

async function loadTodos() {
  todosLoading.value = true;
  todosError.value = false;

  try {
    const data = await getTodos();
    todos.value = data?.list || [];
    setServerTime(data?.serverTime);
  } catch {
    todos.value = [];
    todosError.value = true;
  } finally {
    todosLoading.value = false;
  }
}

async function loadAdmission() {
  if (!isStudent.value) {
    return;
  }

  try {
    await admissionStore.fetch();
  } catch {
    // 错误状态由 admissionStore.error 展示；不能把加载失败当作未录取。
    return;
  }
}

async function loadAll() {
  await Promise.allSettled([loadStage(), loadAnnouncements(), loadTodos(), loadAdmission()]);
}

onMounted(loadAll);
</script>

<style scoped>
.admit-banner {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
  margin-bottom: 20px;
  padding: 14px 18px;
  border: 1px solid rgba(52, 211, 153, 0.35);
  border-radius: 12px;
  background: rgba(52, 211, 153, 0.07);
}

.admit-banner--error {
  border-color: rgba(248, 113, 113, 0.35);
  background: rgba(248, 113, 113, 0.06);
}

.a-main {
  display: flex;
  flex: 1;
  align-items: center;
  gap: 12px;
  min-width: 240px;
}

.a-ico {
  display: grid;
  flex: 0 0 38px;
  place-items: center;
  width: 38px;
  height: 38px;
  border-radius: 10px;
  background: rgba(52, 211, 153, 0.16);
  color: var(--green);
}

.admit-banner--error .a-ico {
  background: rgba(248, 113, 113, 0.12);
  color: var(--red);
}

.a-t {
  font-size: 15px;
  font-weight: 600;
  color: var(--green);
}

.admit-banner--error .a-t {
  color: var(--red);
}

.a-d {
  margin-top: 3px;
  font-size: 12.5px;
  color: var(--text2);
}

.banner-link {
  padding: 7px 14px;
  border: 1px solid rgba(248, 113, 113, 0.35);
  border-radius: 8px;
  background: transparent;
  color: var(--red);
  font-size: 12.5px;
  cursor: pointer;
}

.banner-link:hover {
  background: rgba(248, 113, 113, 0.08);
}

.stage-summary {
  display: flex;
  align-items: center;
  gap: 34px;
  flex-wrap: wrap;
  margin-top: 20px;
  padding-top: 18px;
  border-top: 1px solid var(--line);
}

.stage-now .k {
  font-size: 12px;
  color: var(--text3);
}

.stage-now .v {
  margin-top: 2px;
  font-size: 16px;
  font-weight: 600;
  color: var(--cyan);
}

.stage-now .v small {
  font-size: 12px;
  font-weight: 400;
  color: var(--text2);
}

.stage-time {
  font-family: var(--tech);
  font-size: 13px !important;
  font-weight: 500 !important;
  color: var(--text) !important;
}

.grid-2 {
  display: grid;
  grid-template-columns: 1.5fr 1fr;
  gap: 20px;
  margin-top: 20px;
}

.notice-list {
  display: flex;
  flex-direction: column;
}

.notice {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 11px 4px;
  border-bottom: 1px solid rgba(255, 154, 69, 0.07);
  cursor: pointer;
}

.notice:last-child {
  border-bottom: 0;
}

.notice:hover .n-t {
  color: var(--cyan);
}

.n-ico {
  display: grid;
  flex: 0 0 30px;
  place-items: center;
  width: 30px;
  height: 30px;
  margin-top: 2px;
  border-radius: 8px;
  background: rgba(255, 154, 69, 0.09);
  color: var(--blue);
}

.n-body {
  flex: 1;
  min-width: 0;
}

.n-t {
  overflow: hidden;
  font-size: 13.5px;
  font-weight: 500;
  color: var(--text);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.n-s {
  display: -webkit-box;
  overflow: hidden;
  margin-top: 2px;
  font-size: 12.5px;
  color: var(--text2);
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.n-time {
  margin-top: 3px;
  font-family: var(--tech);
  font-size: 11px;
  color: var(--text3);
}

.todo-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.todo {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 13px 12px;
  border: 1px solid var(--line);
  border-radius: 10px;
  background: rgba(250, 250, 252, 0.92);
}

.todo:hover {
  border-color: rgba(255, 154, 69, 0.3);
}

.t-ico {
  display: grid;
  flex: 0 0 34px;
  place-items: center;
  width: 34px;
  height: 34px;
  border-radius: 9px;
  background: rgba(255, 122, 24, 0.1);
  color: var(--cyan);
}

.t-body {
  flex: 1;
  min-width: 0;
}

.t-t {
  font-size: 13.5px;
  font-weight: 500;
  color: var(--text);
}

.t-d {
  margin-top: 2px;
  font-family: var(--tech);
  font-size: 12px;
  color: var(--text3);
}

.t-go {
  font-size: 12px;
  color: var(--cyan);
  white-space: nowrap;
}

@media (max-width: 1100px) {
  .grid-2 {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 700px) {
  .stage-summary {
    flex-direction: column;
    align-items: flex-start;
    gap: 14px;
  }
}
</style>
