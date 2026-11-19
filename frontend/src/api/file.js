import request from './request';

export function uploadResume(file) {
  const formData = new FormData();
  formData.append('file', file);
  return request.post('/files/resumes', formData);
}

export function getFileUrl(fileId) {
  return `/api/v1/files/${fileId}`;
}
