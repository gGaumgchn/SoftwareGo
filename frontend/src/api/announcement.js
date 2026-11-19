import request from './request';

export function getAnnouncements(params) {
  return request.get('/announcements', { params });
}

export function getAnnouncement(id) {
  return request.get(`/announcements/${id}`);
}
