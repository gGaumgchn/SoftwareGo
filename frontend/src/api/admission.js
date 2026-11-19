import request from './request';

export function getTeacherApplications(params) {
  return request.get('/teacher/applications', { params });
}

export function updateApplicationScore(targetId, score) {
  return request.put(`/teacher/applications/${targetId}/score`, { score });
}

export function updateApplicationOrder(targetId, tieOrder) {
  return request.put(`/teacher/applications/${targetId}/order`, { tieOrder });
}

export function getTeacherAdmissions(params) {
  return request.get('/teacher/admissions', { params });
}

export function grantOffers(payload) {
  return request.post('/teacher/admissions/grant', payload);
}
