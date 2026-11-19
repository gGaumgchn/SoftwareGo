import request from './request';

export function getCurrentVolunteer() {
  return request.get('/student/volunteers/current');
}

export function getAvailableTeachers() {
  return request.get('/student/available-teachers');
}

export function submitVolunteer(payload) {
  return request.post('/student/volunteers', payload);
}
