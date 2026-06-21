export function formatDate(value) {
  if (!value || value.length !== 8) return '-';

  return `${value.slice(0, 4)}.${value.slice(4, 6)}.${value.slice(6, 8)}`;
}

export function formatPeriod(startDate, endDate) {
  return `${formatDate(startDate)} ~ ${formatDate(endDate)}`;
}
