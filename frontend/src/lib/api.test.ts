import { beforeEach, describe, expect, it, vi } from 'vitest';
import { api, ApiError, tokenStore } from './api';

/** Minimal Response stand-in for the bits api.ts reads. */
function json(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'content-type': 'application/json' },
  });
}

const fetchMock = vi.fn<typeof fetch>();

beforeEach(() => {
  fetchMock.mockReset();
  vi.stubGlobal('fetch', fetchMock);
});

function authHeader(callIndex: number): string | undefined {
  const init = fetchMock.mock.calls[callIndex][1] as RequestInit;
  return (init.headers as Record<string, string>)['Authorization'];
}

describe('api()', () => {
  it('sends the stored access token as a Bearer header', async () => {
    tokenStore.set('access-1', 'refresh-1');
    fetchMock.mockResolvedValueOnce(json(200, { ok: true }));

    await api('/api/v1/assets');

    expect(authHeader(0)).toBe('Bearer access-1');
  });

  it('does not send a Bearer header for noAuth calls (login)', async () => {
    tokenStore.set('access-1', 'refresh-1');
    fetchMock.mockResolvedValueOnce(json(200, {}));

    await api('/api/v1/auth/login', { method: 'POST', body: {}, noAuth: true });

    expect(authHeader(0)).toBeUndefined();
  });

  it('drops empty query params instead of sending ?q=', async () => {
    fetchMock.mockResolvedValueOnce(json(200, {}));

    await api('/api/v1/assets', { query: { q: '', status: 'ASSIGNED', page: 0, x: undefined } });

    const url = new URL(fetchMock.mock.calls[0][0] as string);
    expect(url.searchParams.has('q')).toBe(false);
    expect(url.searchParams.has('x')).toBe(false);
    expect(url.searchParams.get('status')).toBe('ASSIGNED');
    expect(url.searchParams.get('page')).toBe('0');
  });

  it('on 401 refreshes once, stores the rotated pair, and retries with the new token', async () => {
    tokenStore.set('expired', 'refresh-1');
    fetchMock
      .mockResolvedValueOnce(json(401, { code: 'auth.unauthenticated', message: 'x', status: 401 }))
      .mockResolvedValueOnce(json(200, { accessToken: 'access-2', refreshToken: 'refresh-2' }))
      .mockResolvedValueOnce(json(200, { id: 7 }));

    const result = await api<{ id: number }>('/api/v1/assets/7');

    expect(result).toEqual({ id: 7 });
    expect(fetchMock).toHaveBeenCalledTimes(3);
    expect(fetchMock.mock.calls[1][0]).toBe('/api/v1/auth/refresh');
    expect(authHeader(2)).toBe('Bearer access-2');
    expect(tokenStore.getRefresh()).toBe('refresh-2');   // rotation persisted
  });

  it('when refresh fails: clears tokens, signals auth-lost, throws ApiError', async () => {
    tokenStore.set('expired', 'stale-refresh');
    const lost = vi.fn();
    window.addEventListener('itams:auth-lost', lost);
    fetchMock
      .mockResolvedValueOnce(json(401, { code: 'auth.unauthenticated', message: 'Authentication failed.', status: 401 }))
      .mockResolvedValueOnce(json(401, { code: 'auth.bad_credentials', message: 'x', status: 401 }));

    await expect(api('/api/v1/assets')).rejects.toMatchObject({ status: 401, code: 'auth.unauthenticated' });

    expect(tokenStore.getAccess()).toBeNull();
    expect(tokenStore.getRefresh()).toBeNull();
    expect(lost).toHaveBeenCalledTimes(1);
    window.removeEventListener('itams:auth-lost', lost);
  });

  it('collapses concurrent 401s into ONE refresh call (single-flight)', async () => {
    // Matters with server-side reuse detection: two parallel refreshes with the same
    // token would look like a replay and revoke the whole session.
    tokenStore.set('expired', 'refresh-1');
    fetchMock.mockImplementation(async (input, init) => {
      const url = String(input);
      if (url === '/api/v1/auth/refresh') return json(200, { accessToken: 'access-2', refreshToken: 'refresh-2' });
      const auth = (init?.headers as Record<string, string>)['Authorization'];
      return auth === 'Bearer access-2' ? json(200, { url }) : json(401, { code: 'x', message: 'x', status: 401 });
    });

    await Promise.all([api('/api/v1/a'), api('/api/v1/b'), api('/api/v1/c')]);

    const refreshCalls = fetchMock.mock.calls.filter(c => c[0] === '/api/v1/auth/refresh');
    expect(refreshCalls).toHaveLength(1);
  });

  it('maps a JSON error body to a typed ApiError with field details', async () => {
    fetchMock.mockResolvedValueOnce(json(400, {
      code: 'validation.failed', message: 'Validation failed', status: 400,
      details: [{ field: 'serialNumber', message: 'must not be blank' }],
    }));

    const err = (await api('/api/v1/assets', { method: 'POST', body: {} }).catch((e: unknown) => e)) as ApiError;

    expect(err).toBeInstanceOf(ApiError);
    expect(err.code).toBe('validation.failed');
    expect(err.details).toEqual([{ field: 'serialNumber', message: 'must not be blank' }]);
  });

  it('returns undefined for 204 No Content', async () => {
    fetchMock.mockResolvedValueOnce(new Response(null, { status: 204 }));
    await expect(api('/api/v1/auth/logout', { method: 'POST', body: {} })).resolves.toBeUndefined();
  });
});
