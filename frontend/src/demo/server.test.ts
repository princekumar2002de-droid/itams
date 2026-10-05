import { describe, expect, it } from 'vitest';
import { createDemoDb } from './db';
import { handle } from './server';

// The demo backend has to enforce the same rules as the real API, otherwise the
// public demo would show behaviour the real system does not have.

const call = (db: ReturnType<typeof createDemoDb>, method: string, path: string, user?: string, body = {}) => {
  const token = user ? `Bearer demo.${db.users.find(u => u.username === user)!.id}` : null;
  return handle(db, method, new URL(`http://x/itams/api/v1${path}`), body, token);
};

describe('demo backend', () => {
  it('rejects requests without a token and wrong passwords', () => {
    const db = createDemoDb();
    expect(call(db, 'GET', '/assets').status).toBe(401);
    expect(call(db, 'POST', '/auth/login', undefined, { username: 'admin', password: 'nope' }).status).toBe(401);
    expect(call(db, 'POST', '/auth/login', undefined, { username: 'admin', password: 'changeme' }).status).toBe(200);
  });

  it('applies role checks like @PreAuthorize', () => {
    const db = createDemoDb();
    expect(call(db, 'GET', '/departments', 'employee').status).toBe(403);
    expect(call(db, 'POST', '/departments', 'itmanager', { code: 'X', name: 'X' }).status).toBe(403);
    expect(call(db, 'POST', '/departments', 'admin', { code: 'X', name: 'X' }).status).toBe(201);
  });

  it('shows an employee only their own tickets and never internal notes', () => {
    const db = createDemoDb();
    const list = call(db, 'GET', '/tickets', 'employee').body as { totalElements: number };
    expect(list.totalElements).toBe(2);
    expect(call(db, 'GET', '/tickets/3', 'employee').status).toBe(404);
    const t1 = call(db, 'GET', '/tickets/1', 'employee').body as { comments: { internal: boolean }[] };
    expect(t1.comments.some(c => c.internal)).toBe(false);
    const t1Staff = call(db, 'GET', '/tickets/1', 'itmanager').body as { comments: { internal: boolean }[] };
    expect(t1Staff.comments.some(c => c.internal)).toBe(true);
  });

  it('enforces the asset and ticket state machines', () => {
    const db = createDemoDb();
    // NB-2024-002 (id 2) is assigned: a second assignment and retirement are refused.
    expect(call(db, 'POST', '/assignments', 'admin', { assetId: 2, assigneePersonId: 4, outCondition: 'GOOD' }).status).toBe(409);
    expect(call(db, 'POST', '/assets/2/retire', 'admin', { reason: 'x' }).status).toBe(409);
    // Ticket 1 is IN_PROGRESS: OPEN is not a legal next state.
    expect(call(db, 'POST', '/tickets/1/status', 'admin', { to: 'OPEN' }).status).toBe(409);
    expect(call(db, 'POST', '/tickets/1/status', 'admin', { to: 'RESOLVED' }).status).toBe(200);
  });

  it('does not hand out more licence seats than were bought', () => {
    const db = createDemoDb();
    // JetBrains licence (id 3): 2 seats, both in use.
    const res = call(db, 'POST', '/licenses/3/assignments', 'admin', { personId: 6 });
    expect(res.status).toBe(409);
    expect((res.body as { code: string }).code).toBe('license.out_of_seats');
  });
});
