/**
 * Tiny fetch wrapper that:
 *   1. Adds Authorization: Bearer <accessToken> to every request
 *   2. On 401, tries ONE refresh with the stored refresh token; on success
 *      transparently retries the original request.
 *   3. Throws a typed ApiError shaped like the backend's ErrorResponse.
 */

const STORAGE = {
  access:  'itams.accessToken',
  refresh: 'itams.refreshToken',
};

export interface ApiErrorBody {
  code: string;
  message: string;
  status: number;
  path?: string;
  timestamp?: string;
  details?: Array<{ field: string; message: string }>;
}

export class ApiError extends Error {
  readonly status: number;
  readonly code: string;
  readonly details?: ApiErrorBody['details'];
  constructor(body: ApiErrorBody) {
    super(body.message || `HTTP ${body.status}`);
    this.status = body.status;
    this.code = body.code;
    this.details = body.details;
  }
}

// ── token storage ──────────────────────────────────────────────────────────

export const tokenStore = {
  getAccess():  string | null { try { return localStorage.getItem(STORAGE.access); }  catch { return null; } },
  getRefresh(): string | null { try { return localStorage.getItem(STORAGE.refresh); } catch { return null; } },
  set(access: string, refresh: string) {
    try { localStorage.setItem(STORAGE.access, access); localStorage.setItem(STORAGE.refresh, refresh); } catch { /* ignore */ }
  },
  clear() {
    try { localStorage.removeItem(STORAGE.access); localStorage.removeItem(STORAGE.refresh); } catch { /* ignore */ }
  },
};

// ── refresh flow ───────────────────────────────────────────────────────────

let refreshInFlight: Promise<boolean> | null = null;

async function tryRefresh(): Promise<boolean> {
  const refreshToken = tokenStore.getRefresh();
  if (!refreshToken) return false;
  if (refreshInFlight) return refreshInFlight;

  refreshInFlight = (async () => {
    try {
      const res = await fetch('/api/v1/auth/refresh', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ refreshToken }),
      });
      if (!res.ok) return false;
      const body = await res.json();
      tokenStore.set(body.accessToken, body.refreshToken);
      return true;
    } catch { return false; }
    finally { refreshInFlight = null; }
  })();
  return refreshInFlight;
}

// ── main request wrapper ───────────────────────────────────────────────────

interface RequestOptions {
  method?: 'GET' | 'POST' | 'PATCH' | 'DELETE' | 'PUT';
  body?: unknown;
  query?: Record<string, string | number | boolean | undefined | null>;
  signal?: AbortSignal;
  /** Skip Bearer header (login / refresh only). */
  noAuth?: boolean;
  /** Internal — set true when this call IS the retry after refresh. */
  _retried?: boolean;
}

export async function api<T>(path: string, opts: RequestOptions = {}): Promise<T> {
  const url = new URL(path, window.location.origin);
  if (opts.query) {
    for (const [k, v] of Object.entries(opts.query)) {
      if (v !== undefined && v !== null && v !== '') url.searchParams.set(k, String(v));
    }
  }

  const headers: Record<string, string> = {};
  const access = tokenStore.getAccess();
  if (!opts.noAuth && access) headers['Authorization'] = `Bearer ${access}`;
  if (opts.body !== undefined) headers['Content-Type'] = 'application/json';

  const res = await fetch(url.toString(), {
    method: opts.method ?? 'GET',
    headers,
    body: opts.body !== undefined ? JSON.stringify(opts.body) : undefined,
    signal: opts.signal,
  });

  // 401 → try one refresh, then retry the request once.
  if (res.status === 401 && !opts.noAuth && !opts._retried) {
    const ok = await tryRefresh();
    if (ok) return api<T>(path, { ...opts, _retried: true });
    tokenStore.clear();
    // Nudge the app to the login page; components will react to the auth event.
    window.dispatchEvent(new CustomEvent('itams:auth-lost'));
  }

  if (res.status === 204) return undefined as T;

  const contentType = res.headers.get('content-type') || '';
  const parsed = contentType.includes('application/json') ? await res.json() : null;

  if (!res.ok) {
    const body: ApiErrorBody = parsed ?? {
      code: `http.${res.status}`,
      message: res.statusText || 'Request failed',
      status: res.status,
    };
    throw new ApiError(body);
  }
  return parsed as T;
}

// ── shared response shapes ─────────────────────────────────────────────────

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}
