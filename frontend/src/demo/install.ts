// Demo build only: answers /api/v1/* requests inside the browser instead of
// sending them to a server. The data lives in sessionStorage, so it survives
// page reloads in the same tab and resets when the tab is closed.

import { createDemoDb, type Db } from './db';
import { handle } from './server';

const KEY = 'itams.demo.db.v1';

function load(): Db {
  try {
    const saved = sessionStorage.getItem(KEY);
    if (saved) return JSON.parse(saved) as Db;
  } catch { /* storage unavailable or corrupt: start fresh */ }
  return createDemoDb();
}

function save(db: Db) {
  try { sessionStorage.setItem(KEY, JSON.stringify(db)); } catch { /* ignore */ }
}

export function resetDemoData() {
  try { sessionStorage.removeItem(KEY); } catch { /* ignore */ }
}

export function installDemoBackend() {
  const db = load();
  const realFetch = window.fetch.bind(window);

  window.fetch = async (input: RequestInfo | URL, init?: RequestInit) => {
    const url = new URL(input instanceof Request ? input.url : String(input), window.location.origin);
    if (!url.pathname.includes('/api/v1/')) return realFetch(input, init);

    const method = (init?.method ?? 'GET').toUpperCase();
    const headers = new Headers(init?.headers);
    const body = typeof init?.body === 'string' && init.body ? JSON.parse(init.body) : {};

    // A short delay so loading states behave like they do against a real server.
    await new Promise(r => setTimeout(r, 120));
    const res = handle(db, method, url, body, headers.get('Authorization'));
    if (method !== 'GET') save(db);

    return res.body === undefined
      ? new Response(null, { status: res.status })
      : new Response(JSON.stringify(res.body), { status: res.status, headers: { 'Content-Type': 'application/json' } });
  };
}
