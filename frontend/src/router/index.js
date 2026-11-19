import { createRouter, createWebHistory } from 'vue-router';
import { useUserStore } from '../stores/user';
import { showToast } from '../utils/toast';

const ALL_ROLES = ['STUDENT', 'TEACHER', 'MANAGER'];

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      redirect: '/home',
    },
    {
      path: '/login',
      name: 'login',
      component: () => import('../views/LoginView.vue'),
      meta: { public: true },
    },
    {
      path: '/home',
      name: 'home',
      component: () => import('../views/HomeView.vue'),
      meta: { title: '主页面', roles: ALL_ROLES },
    },
    {
      path: '/announcements',
      name: 'announcements',
      component: () => import('../views/AnnouncementView.vue'),
      meta: { title: '公告', roles: ALL_ROLES },
    },
    {
      path: '/announcements/:id',
      name: 'announcement-detail',
      component: () => import('../views/AnnouncementDetailView.vue'),
      meta: { title: '公告详情', roles: ALL_ROLES },
    },
    {
      path: '/todos',
      name: 'todos',
      component: () => import('../views/TodoView.vue'),
      meta: { title: '个人待办', roles: ALL_ROLES },
    },
    {
      path: '/student/labs',
      name: 'student-labs',
      component: () => import('../views/student/LabsView.vue'),
      meta: { title: '查看实训室信息', roles: ['STUDENT'] },
    },
    {
      path: '/student/volunteer',
      name: 'student-volunteer',
      component: () => import('../views/student/VolunteerView.vue'),
      meta: { title: '志愿填报', roles: ['STUDENT'] },
    },
    {
      path: '/student/offers',
      name: 'student-offers',
      component: () => import('../views/student/OfferView.vue'),
      meta: { title: '拟录取确认', roles: ['STUDENT'] },
    },
    {
      path: '/teacher/lab',
      name: 'teacher-lab',
      component: () => import('../views/teacher/LabInfoView.vue'),
      meta: { title: '实训室信息', roles: ['TEACHER'] },
    },
    {
      path: '/teacher/admissions',
      name: 'teacher-admissions',
      component: () => import('../views/teacher/AdmissionView.vue'),
      meta: { title: '申请与录取管理', roles: ['TEACHER'] },
    },
    {
      path: '/manager/stages',
      name: 'manager-stages',
      component: () => import('../views/manager/StageView.vue'),
      meta: { title: '阶段管理', roles: ['MANAGER'] },
    },
    {
      path: '/:pathMatch(.*)*',
      redirect: '/home',
    },
  ],
});

router.beforeEach(async (to) => {
  const userStore = useUserStore();

  if (to.meta.public) {
    return userStore.isLoggedIn ? { name: 'home' } : true;
  }

  if (!userStore.isLoggedIn) {
    try {
      await userStore.fetchMe();
    } catch {
      return {
        name: 'login',
        query: { redirect: to.fullPath },
      };
    }
  }

  if (to.meta.roles && !to.meta.roles.includes(userStore.role)) {
    if (to.name === 'manager-stages') {
      showToast('仅负责人可配置阶段', 'error');
    } else if (to.name === 'teacher-lab' || to.name === 'teacher-admissions') {
      showToast('无权访问该数据', 'error');
    }
    return { name: 'home' };
  }

  return true;
});

export default router;
