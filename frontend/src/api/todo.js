import request from './request';

export function getTodos() {
  return request.get('/todos');
}
