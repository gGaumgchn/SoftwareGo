const SERVER_TIME_ZONE = 'Asia/Shanghai';

let serverOffset = 0;

const dateTimeFormatter = new Intl.DateTimeFormat('zh-CN', {
  timeZone: SERVER_TIME_ZONE,
  year: 'numeric',
  month: '2-digit',
  day: '2-digit',
  hour: '2-digit',
  minute: '2-digit',
  hour12: false,
});

const dateFormatter = new Intl.DateTimeFormat('zh-CN', {
  timeZone: SERVER_TIME_ZONE,
  year: 'numeric',
  month: '2-digit',
  day: '2-digit',
});

/**
 * 接口时间格式为 yyyy-MM-dd'T'HH:mm:ss，固定 Asia/Shanghai，无时区后缀。
 * 解析时补 +08:00，避免跟随浏览器本地时区。
 */
export function parseServerDate(value) {
  if (!value) {
    return null;
  }

  const normalized = String(value).trim().replace(' ', 'T');
  if (!normalized) {
    return null;
  }

  const withTime = /^\d{4}-\d{2}-\d{2}$/.test(normalized)
    ? `${normalized}T00:00:00`
    : normalized;
  const hasZone = /(?:Z|[+-]\d{2}:?\d{2})$/i.test(withTime);
  const date = new Date(hasZone ? withTime : `${withTime}+08:00`);

  return Number.isNaN(date.getTime()) ? null : date;
}

export function setServerTime(serverTime) {
  const date = parseServerDate(serverTime);
  if (date) {
    serverOffset = date.getTime() - Date.now();
  }
}

export function getServerNow() {
  return new Date(Date.now() + serverOffset);
}

export function formatDateTime(value) {
  const date = parseServerDate(value);
  return date ? dateTimeFormatter.format(date).replace(/\//g, '-') : '—';
}

export function formatDate(value) {
  const date = parseServerDate(value);
  return date ? dateFormatter.format(date).replace(/\//g, '-') : '—';
}

export function getCountdown(target) {
  const date = parseServerDate(target);
  if (!date) {
    return null;
  }

  const diff = Math.max(0, date.getTime() - getServerNow().getTime());

  return {
    days: Math.floor(diff / 86400000),
    hours: Math.floor((diff % 86400000) / 3600000),
    minutes: Math.floor((diff % 3600000) / 60000),
    seconds: Math.floor((diff % 60000) / 1000),
    finished: diff <= 0,
  };
}
