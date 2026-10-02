const dateFormatter = new Intl.DateTimeFormat('en-GB', {
  year: 'numeric', month: 'short', day: '2-digit'
});

const dateTimeFormatter = new Intl.DateTimeFormat('en-GB', {
  year: 'numeric', month: 'short', day: '2-digit', hour: '2-digit', minute: '2-digit'
});

const eurFormatter = new Intl.NumberFormat('de-DE', {
  style: 'currency', currency: 'EUR'
});

export function fmtDate(iso?: string | null): string {
  if (!iso) return '—';
  return dateFormatter.format(new Date(iso));
}

export function fmtDateTime(iso?: string | null): string {
  if (!iso) return '—';
  return dateTimeFormatter.format(new Date(iso));
}

export function fmtEUR(amount?: number | string | null): string {
  if (amount == null) return '—';
  const n = typeof amount === 'string' ? Number(amount) : amount;
  return Number.isFinite(n) ? eurFormatter.format(n) : '—';
}
