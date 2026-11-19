import axios from 'axios';
import { showToast } from '../utils/toast';

const request = axios.create({
  baseURL: '/api/v1',
  timeout: 15000,
  withCredentials: true,
});

request.interceptors.response.use(
  (response) => {
    const body = response.data;

    if (body && body.code === 200) {
      return body.data;
    }

    const message = body?.message || '提交失败，请重试';
    showToast(message, 'error');
    return Promise.reject(new Error(message));
  },
  (error) => {
    const response = error.response;

    if (!response) {
      showToast('网络异常，请重试', 'error');
      return Promise.reject(error);
    }

    const message = response.data?.message;

    if (response.status === 401) {
      // 由 main.js 统一清理用户状态并跳转登录页，避免 request 反向依赖 router/store。
      window.dispatchEvent(new CustomEvent('auth:unauthorized'));
    } else if (response.status === 403) {
      showToast(message || '无权访问该数据', 'error');
    } else {
      showToast(message || '加载失败，请重试', 'error');
    }

    return Promise.reject(error);
  },
);

export default request;
