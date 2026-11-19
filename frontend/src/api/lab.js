import request from './request';

export function getLabs(params) {
  return request.get('/labs', { params });
}

export function getLab(labId) {
  return request.get(`/labs/${labId}`);
}

export function getTeacherLab() {
  return request.get('/teacher/lab');
}

export function saveTeacherLab(payload) {
  return request.put('/teacher/lab', payload);
}
