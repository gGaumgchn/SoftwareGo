import request from './request';

export function getCurrentStage() {
  return request.get('/stages/current');
}

export function getManagerStages() {
  return request.get('/manager/stages');
}

export function saveStages(payload) {
  return request.put('/manager/stages', payload);
}

export function retryStageAnnouncement(changeNo) {
  return request.post('/manager/stages/announcement', { changeNo });
}

export function getStageChanges(params) {
  return request.get('/manager/stage-changes', { params });
}
