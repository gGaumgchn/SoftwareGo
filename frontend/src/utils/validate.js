export const STAFF_NO_PATTERN = /^\d{10}$/;

export function isValidStaffNo(value) {
  return STAFF_NO_PATTERN.test(String(value ?? '').trim());
}
