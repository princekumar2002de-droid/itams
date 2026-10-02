/** Today as YYYY-MM-DD in the user's local time zone (what <input type="date"> uses). */
export function todayIso(): string {
  const d = new Date();
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
}

/** '' → undefined, so optional fields are omitted from the JSON body instead of sent as "". */
export function opt(v: string): string | undefined {
  const t = v.trim();
  return t === '' ? undefined : t;
}
