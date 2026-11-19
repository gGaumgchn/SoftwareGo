import request from './request';

export function getOffers() {
  return request.get('/student/offers');
}

export function confirmOffer(offerId) {
  return request.post(`/student/offers/${offerId}/confirm`);
}

export function getAdmission() {
  return request.get('/student/admission');
}
