// The demo build's stand-in for the Spring Boot API. It answers the same URLs
// with the same JSON shapes and applies the same role checks and business
// rules (state machines, one open assignment per asset, seat limits, employee
// ticket scoping), so the UI behaves as it does against the real backend.

import type { Db, DbAsset, DbTicket, DbUser } from './db';
import { day, nextId, nowIso } from './db';
import type { TicketStatus } from '../api/types';

export interface DemoResponse { status: number; body?: unknown }
type Query = URLSearchParams;
type Body = Record<string, unknown>;

class HttpError extends Error {
  constructor(readonly status: number, readonly code: string, message: string) { super(message); }
}
const notFound = (what: string, id: number | string) => new HttpError(404, 'resource.not_found', `${what} with id ${id} not found`);
const conflict = (code: string, message: string) => new HttpError(409, code, message);
const badRequest = (field: string, message: string) => new HttpError(400, 'validation.failed', `${field}: ${message}`);

const TICKET_TRANSITIONS: Record<TicketStatus, TicketStatus[]> = {
  OPEN: ['IN_PROGRESS', 'CLOSED'],
  IN_PROGRESS: ['WAITING', 'RESOLVED', 'CLOSED'],
  WAITING: ['IN_PROGRESS', 'RESOLVED', 'CLOSED'],
  RESOLVED: ['CLOSED', 'IN_PROGRESS'],
  CLOSED: [],
};

export function handle(db: Db, method: string, url: URL, body: Body, authHeader: string | null): DemoResponse {
  try {
    return route(db, method, url.pathname.replace(/^.*?\/api\/v1/, ''), url.searchParams, body ?? {}, authHeader);
  } catch (e) {
    if (e instanceof HttpError) {
      return { status: e.status, body: { code: e.code, message: e.message, status: e.status, path: url.pathname, timestamp: nowIso() } };
    }
    throw e;
  }
}

function route(db: Db, method: string, path: string, q: Query, b: Body, auth: string | null): DemoResponse {
  const ok = (body: unknown, status = 200): DemoResponse => ({ status, body });

  // ── auth (no token needed) ───────────────────────────────────────────────
  if (method === 'POST' && path === '/auth/login') {
    const user = db.users.find(u => u.username === b.username);
    if (!user || b.password !== 'changeme') throw new HttpError(401, 'auth.bad_credentials', 'Invalid username or password.');
    return ok(tokens(user));
  }
  if (method === 'POST' && path === '/auth/refresh') {
    const user = userFromToken(db, String(b.refreshToken ?? ''));
    if (!user) throw new HttpError(401, 'auth.unauthenticated', 'Authentication failed.');
    return ok(tokens(user));
  }
  if (method === 'POST' && path === '/auth/logout') return { status: 204 };

  const me = userFromToken(db, (auth ?? '').replace(/^Bearer /, ''));
  if (!me) throw new HttpError(401, 'auth.unauthenticated', 'Authentication failed.');
  const isAdmin = me.role === 'ADMIN';
  const canManage = me.role !== 'EMPLOYEE';
  const require = (allowed: boolean) => {
    if (!allowed) throw new HttpError(403, 'auth.forbidden', 'You do not have permission to perform this action.');
  };

  const seg = path.split('/').filter(Boolean);
  const id = (i: number) => Number(seg[i]);
  const is = (m: string, pattern: string) => {
    if (m !== method) return false;
    const parts = pattern.split('/').filter(Boolean);
    return parts.length === seg.length && parts.every((p, i) => p === '*' || p === seg[i]);
  };

  // ── me / stats ───────────────────────────────────────────────────────────
  if (is('GET', 'auth/me')) {
    const p = person(db, me.personId);
    return ok({ userId: me.id, username: me.username, personId: me.personId, firstName: p.firstName, lastName: p.lastName, email: p.email, roles: [`ROLE_${me.role}`] });
  }
  if (is('GET', 'stats/dashboard')) return ok(stats(db));

  // ── people / employees / departments ─────────────────────────────────────
  if (is('GET', 'people')) {
    require(canManage);
    return ok(page(db.people.filter(p => matches(q, p.firstName, p.lastName, p.email)), q));
  }
  if (is('POST', 'people')) {
    require(isAdmin);
    const email = String(b.email ?? '').trim();
    if (!b.firstName || !b.lastName || !email.includes('@')) throw badRequest('email', 'must be a well-formed email address');
    if (db.people.some(p => p.email.toLowerCase() === email.toLowerCase())) throw conflict('person.email_already_used', `A person with email '${email}' already exists.`);
    const p = { id: nextId(db, 'people'), firstName: String(b.firstName), lastName: String(b.lastName), email, phone: (b.phone as string) ?? null, active: true, createdAt: nowIso(), updatedAt: nowIso() };
    db.people.push(p);
    return ok(p, 201);
  }
  if (is('PATCH', 'people/*')) {
    require(isAdmin);
    const p = person(db, id(1));
    Object.assign(p, Object.fromEntries((['firstName', 'lastName', 'email', 'phone', 'active'] as const).filter(k => b[k] !== undefined).map(k => [k, b[k]])));
    p.updatedAt = nowIso();
    return ok(p);
  }

  if (is('GET', 'employees')) {
    require(canManage);
    const dept = q.get('departmentId');
    const rows = db.employees
      .filter(e => !dept || e.departmentId === Number(dept))
      .map(e => employeeDto(db, e))
      .filter(e => matches(q, e.firstName, e.lastName, e.email, e.employeeNumber));
    return ok(page(rows, q));
  }
  if (is('POST', 'employees')) {
    require(isAdmin);
    const personId = Number(b.personId);
    person(db, personId);
    const number = String(b.employeeNumber ?? '');
    if (!/^[A-Z0-9-]+$/.test(number)) throw badRequest('employeeNumber', 'must be uppercase alphanumeric or dashes');
    if (db.employees.some(e => e.employeeNumber === number)) throw conflict('employee.number_already_used', `Employee number '${number}' is already in use.`);
    if (db.employees.some(e => e.personId === personId)) throw conflict('employee.person_already_has_record', `Person ${personId} already has an employee record.`);
    department(db, Number(b.departmentId));
    const e = { id: nextId(db, 'employees'), personId, employeeNumber: number, departmentId: Number(b.departmentId), jobTitle: (b.jobTitle as string) || null, hireDate: String(b.hireDate), endDate: null, employmentStatus: 'ACTIVE' as const, createdAt: nowIso(), updatedAt: nowIso() };
    db.employees.push(e);
    return ok(employeeDto(db, e), 201);
  }
  if (is('PATCH', 'employees/*')) {
    require(isAdmin);
    const e = db.employees.find(x => x.id === id(1));
    if (!e) throw notFound('Employee', id(1));
    if (b.departmentId != null) e.departmentId = department(db, Number(b.departmentId)).id;
    if (b.jobTitle !== undefined) e.jobTitle = (b.jobTitle as string) || null;
    if (b.endDate) {
      if (String(b.endDate) < e.hireDate) throw conflict('employee.end_before_hire', 'endDate cannot be before hireDate.');
      e.endDate = String(b.endDate);
    }
    if (b.employmentStatus) e.employmentStatus = b.employmentStatus as typeof e.employmentStatus;
    e.updatedAt = nowIso();
    return ok(employeeDto(db, e));
  }

  if (is('GET', 'departments')) {
    require(canManage);
    const rows = db.departments.filter(d => !d.deleted && matches(q, d.code, d.name)).sort((x, y) => x.code.localeCompare(y.code));
    return ok(page(rows.map(departmentDto), q));
  }
  if (is('POST', 'departments')) {
    require(isAdmin);
    const code = String(b.code ?? '');
    if (!/^[A-Z0-9][A-Z0-9_-]*$/.test(code)) throw badRequest('code', "must be uppercase alphanumeric with '-' or '_' and start with a letter/digit");
    if (db.departments.some(d => !d.deleted && d.code === code)) throw conflict('department.code_already_used', `A department with code '${code}' already exists.`);
    const d = { id: nextId(db, 'departments'), code, name: String(b.name ?? ''), parentDepartmentId: (b.parentDepartmentId as number) ?? null, managerPersonId: (b.managerPersonId as number) ?? null, deleted: false, createdAt: nowIso(), updatedAt: nowIso() };
    db.departments.push(d);
    return ok(departmentDto(d), 201);
  }
  if (is('PATCH', 'departments/*')) {
    require(isAdmin);
    const d = department(db, id(1));
    if (b.name) d.name = String(b.name);
    if (b.parentDepartmentId != null) {
      if (Number(b.parentDepartmentId) === d.id) throw conflict('department.self_parent', 'A department cannot be its own parent.');
      d.parentDepartmentId = department(db, Number(b.parentDepartmentId)).id;
    }
    if (b.managerPersonId != null) d.managerPersonId = Number(b.managerPersonId);
    d.updatedAt = nowIso();
    return ok(departmentDto(d));
  }
  if (is('DELETE', 'departments/*')) {
    require(isAdmin);
    department(db, id(1)).deleted = true;
    return { status: 204 };
  }

  // ── asset catalogue ──────────────────────────────────────────────────────
  if (is('GET', 'asset-categories')) return ok(db.categories);
  if (is('GET', 'asset-models')) {
    const cat = q.get('categoryId');
    const rows = db.models.filter(m => (!cat || m.categoryId === Number(cat)) && matches(q, m.manufacturer, m.modelName));
    return ok(page(rows.map(m => modelDto(db, m.id)), q));
  }
  if (is('POST', 'asset-models')) {
    require(canManage);
    const manufacturer = String(b.manufacturer ?? '').trim();
    const modelName = String(b.modelName ?? '').trim();
    if (!manufacturer || !modelName) throw badRequest('modelName', 'must not be blank');
    if (db.models.some(m => m.manufacturer.toLowerCase() === manufacturer.toLowerCase() && m.modelName.toLowerCase() === modelName.toLowerCase())) {
      throw conflict('asset_model.duplicate', `Asset model '${manufacturer} ${modelName}' already exists.`);
    }
    const m = { id: nextId(db, 'models'), categoryId: Number(b.categoryId), manufacturer, modelName, specs: (b.specs as string) ?? null };
    db.models.push(m);
    return ok(modelDto(db, m.id), 201);
  }

  if (is('GET', 'assets')) {
    const status = q.get('status');
    const cat = q.get('categoryId');
    const rows = db.assets
      .filter(a => (!status || a.status === status) && (!cat || model(db, a.modelId).categoryId === Number(cat)))
      .map(a => assetDto(db, a))
      .filter(a => matches(q, a.assetTag, a.serialNumber, a.modelManufacturer, a.modelName));
    return ok(page(rows, q));
  }
  if (is('GET', 'assets/*')) return ok(assetDto(db, asset(db, id(1))));
  if (is('POST', 'assets')) {
    require(canManage);
    const tag = String(b.assetTag ?? '');
    if (!/^[A-Z0-9-]+$/.test(tag)) throw badRequest('assetTag', 'assetTag must be uppercase alphanumeric or dashes');
    if (db.assets.some(a => a.assetTag === tag)) throw conflict('asset.tag_already_used', `Asset tag '${tag}' is already in use.`);
    if (String(b.purchaseDate) > day(0)) throw badRequest('purchaseDate', 'must be a date in the past or in the present');
    model(db, Number(b.modelId));
    const a: DbAsset = {
      id: nextId(db, 'assets'), assetTag: tag, modelId: Number(b.modelId), serialNumber: String(b.serialNumber ?? ''), status: 'IN_STOCK',
      purchaseDate: String(b.purchaseDate), purchasePrice: Number(b.purchasePrice ?? 0), warrantyEndsOn: (b.warrantyEndsOn as string) || null,
      notes: (b.notes as string) || null, retiredAt: null, retiredReason: null, createdAt: nowIso(), updatedAt: nowIso(),
    };
    db.assets.push(a);
    return ok(assetDto(db, a), 201);
  }
  if (is('PATCH', 'assets/*')) {
    require(canManage);
    const a = asset(db, id(1));
    if (a.status === 'RETIRED') throw conflict('asset.retired_immutable', 'Retired assets cannot be edited.');
    if (b.warrantyEndsOn !== undefined) a.warrantyEndsOn = (b.warrantyEndsOn as string) || null;
    if (b.notes !== undefined) a.notes = (b.notes as string) || null;
    a.updatedAt = nowIso();
    return ok(assetDto(db, a));
  }
  if (is('POST', 'assets/*/retire')) {
    require(canManage);
    const a = asset(db, id(1));
    if (['RETIRED', 'LOST', 'ASSIGNED'].includes(a.status)) throw conflict('asset.retire_illegal_state', `Cannot retire asset with status ${a.status}`);
    Object.assign(a, { status: 'RETIRED', retiredAt: nowIso(), retiredReason: String(b.reason ?? ''), updatedAt: nowIso() });
    return ok(assetDto(db, a));
  }
  if (is('POST', 'assets/*/maintenance-complete')) {
    require(canManage);
    const a = asset(db, id(1));
    if (a.status !== 'UNDER_MAINTENANCE') throw conflict('asset.not_under_maintenance', `Asset is ${a.status}, not under maintenance.`);
    Object.assign(a, { status: 'IN_STOCK', updatedAt: nowIso() });
    return ok(assetDto(db, a));
  }

  // ── assignments ──────────────────────────────────────────────────────────
  if (is('GET', 'assignments')) {
    const assetId = q.get('assetId'); const personId = q.get('personId'); const onlyOpen = q.get('onlyOpen') === 'true';
    const rows = db.assignments
      .filter(x => (!assetId || x.assetId === Number(assetId)) && (!personId || x.assigneePersonId === Number(personId)) && (!onlyOpen || !x.actualReturnAt))
      .sort((x, y) => y.id - x.id);
    return ok(page(rows.map(x => assignmentDto(db, x.id)), q));
  }
  if (is('POST', 'assignments')) {
    require(canManage);
    const a = asset(db, Number(b.assetId));
    const p = person(db, Number(b.assigneePersonId));
    if (!p.active) throw conflict('assignment.assignee_inactive', 'Assignee is not active.');
    const open = db.assignments.find(x => x.assetId === a.id && !x.actualReturnAt);
    if (open) throw conflict('assignment.already_open', `Asset already has an open assignment (id=${open.id}).`);
    if (a.status !== 'IN_STOCK') throw conflict('assignment.asset_not_in_stock', `Asset is ${a.status} and cannot be assigned.`);
    const x = {
      id: nextId(db, 'assignments'), assetId: a.id, assigneePersonId: p.id, assignedByUserId: me.id, assignedAt: nowIso(),
      expectedReturnOn: (b.expectedReturnOn as string) || null, actualReturnAt: null, returnedByUserId: null,
      outCondition: b.outCondition as 'GOOD', inCondition: null, notes: (b.notes as string) || null,
    };
    db.assignments.push(x);
    Object.assign(a, { status: 'ASSIGNED', updatedAt: nowIso() });
    return ok(assignmentDto(db, x.id), 201);
  }
  if (is('POST', 'assignments/*/return')) {
    require(canManage);
    const x = db.assignments.find(r => r.id === id(1));
    if (!x) throw notFound('AssetAssignment', id(1));
    if (x.actualReturnAt) throw conflict('assignment.already_returned', 'Assignment was already returned.');
    Object.assign(x, { actualReturnAt: nowIso(), returnedByUserId: me.id, inCondition: b.inCondition, notes: (b.notes as string) || x.notes });
    Object.assign(asset(db, x.assetId), { status: b.sendForMaintenance ? 'UNDER_MAINTENANCE' : 'IN_STOCK', updatedAt: nowIso() });
    return ok(assignmentDto(db, x.id));
  }

  // ── licences ─────────────────────────────────────────────────────────────
  if (is('GET', 'licenses')) {
    return ok(page(db.licenses.map(l => licenseDto(db, l.id)).filter(l => matches(q, l.productVendor, l.productName, l.licenseReference)), q));
  }
  if (is('GET', 'licenses/*')) return ok(licenseDto(db, license(db, id(1)).id));
  if (is('POST', 'licenses')) {
    require(canManage);
    if (!(Number(b.seatsTotal) > 0)) throw badRequest('seatsTotal', 'must be greater than 0');
    if (b.expiresOn && String(b.expiresOn) < day(0)) throw badRequest('expiresOn', 'must be a date in the present or in the future');
    const l = {
      id: nextId(db, 'licenses'), vendor: String(b.vendor ?? ''), productName: String(b.productName ?? ''), productVersion: (b.productVersion as string) || null,
      licenseReference: String(b.licenseReference ?? ''), licenseType: b.licenseType as 'PER_SEAT', seatsTotal: Number(b.seatsTotal),
      purchaseDate: String(b.purchaseDate), expiresOn: (b.expiresOn as string) || null, cost: Number(b.cost ?? 0),
      procurementRef: (b.procurementRef as string) || null, createdAt: nowIso(), updatedAt: nowIso(),
    };
    db.licenses.push(l);
    return ok(licenseDto(db, l.id), 201);
  }
  if (is('GET', 'licenses/*/assignments')) {
    license(db, id(1));
    return ok(db.seats.filter(s => s.licenseId === id(1)).sort((x, y) => y.id - x.id).map(s => seatDto(db, s.id)));
  }
  if (is('POST', 'licenses/*/assignments')) {
    require(canManage);
    const l = license(db, id(1));
    if ((b.personId == null) === (b.assetId == null)) throw conflict('license.assignee_xor', 'Assign to a person OR an asset — not both, not neither.');
    const used = db.seats.filter(s => s.licenseId === l.id && !s.releasedAt).length;
    if (used >= l.seatsTotal) throw conflict('license.out_of_seats', `License has ${used}/${l.seatsTotal} seats in use.`);
    if (b.personId != null) person(db, Number(b.personId)); else asset(db, Number(b.assetId));
    const s = { id: nextId(db, 'seats'), licenseId: l.id, personId: b.personId != null ? Number(b.personId) : null, assetId: b.assetId != null ? Number(b.assetId) : null, assignedAt: nowIso(), releasedAt: null, notes: (b.notes as string) || null };
    db.seats.push(s);
    return ok(seatDto(db, s.id), 201);
  }
  if (is('POST', 'licenses/assignments/*/release')) {
    require(canManage);
    const s = db.seats.find(x => x.id === id(2));
    if (!s) throw notFound('LicenseAssignment', id(2));
    if (s.releasedAt) throw conflict('license.already_released', 'That seat was already released.');
    s.releasedAt = nowIso();
    return ok(seatDto(db, s.id));
  }

  // ── maintenance ──────────────────────────────────────────────────────────
  if (is('GET', 'maintenance')) {
    const assetId = q.get('assetId');
    const rows = db.maintenance
      .filter(m => !assetId || m.assetId === Number(assetId))
      .sort((x, y) => y.performedOn.localeCompare(x.performedOn) || y.id - x.id)
      .map(m => maintenanceDto(db, m.id))
      .filter(m => matches(q, m.assetTag, m.description, m.providerName ?? ''));
    return ok(page(rows, q));
  }
  if (is('POST', 'maintenance')) {
    require(canManage);
    if (b.performedByUserId == null && !String(b.providerName ?? '').trim()) {
      throw conflict('maintenance.performer_required', 'Must specify either performedByUserId (internal) or providerName (external).');
    }
    if (!String(b.description ?? '').trim()) throw badRequest('description', 'must not be blank');
    asset(db, Number(b.assetId));
    const m = {
      id: nextId(db, 'maintenance'), assetId: Number(b.assetId), performedOn: String(b.performedOn), performedByUserId: (b.performedByUserId as number) ?? null,
      providerName: (b.providerName as string) || null, description: String(b.description), cost: b.cost != null ? Number(b.cost) : null,
      nextScheduledOn: (b.nextScheduledOn as string) || null, createdAt: nowIso(), updatedAt: nowIso(),
    };
    db.maintenance.push(m);
    return ok(maintenanceDto(db, m.id), 201);
  }
  if (is('DELETE', 'maintenance/*')) {
    require(canManage);
    db.maintenance = db.maintenance.filter(m => m.id !== id(1));
    return { status: 204 };
  }

  // ── tickets ──────────────────────────────────────────────────────────────
  const employeeOnly = me.role === 'EMPLOYEE';
  const visible = (t: DbTicket) => !employeeOnly || t.reporterPersonId === me.personId || t.assignedToUserId === me.id;
  const ticketById = (ticketId: number) => {
    const t = db.tickets.find(x => x.id === ticketId);
    if (!t || !visible(t)) throw notFound('Ticket', ticketId);   // 404, not 403: don't reveal other people's tickets
    return t;
  };

  if (is('GET', 'tickets')) {
    const f = (k: string) => q.get(k);
    const rows = db.tickets
      .filter(t => visible(t)
        && (!f('status') || t.status === f('status')) && (!f('priority') || t.priority === f('priority'))
        && (!f('assignedToUserId') || t.assignedToUserId === Number(f('assignedToUserId')))
        && (employeeOnly || !f('reporterPersonId') || t.reporterPersonId === Number(f('reporterPersonId')))
        && matches(q, t.subject, t.ticketNumber))
      .sort((x, y) => y.id - x.id);
    return ok(page(rows.map(t => ticketDto(db, t, null)), q));
  }
  if (is('GET', 'tickets/*')) {
    const t = ticketById(id(1));
    return ok(ticketDto(db, t, comments(db, t.id, employeeOnly)));
  }
  if (is('POST', 'tickets')) {
    if (!String(b.subject ?? '').trim() || !String(b.description ?? '').trim()) throw badRequest('subject', 'must not be blank');
    const reporter = employeeOnly ? me.personId : Number(b.reporterPersonId);   // employees always report for themselves
    person(db, reporter);
    const tid = nextId(db, 'tickets');
    const t: DbTicket = {
      id: tid, ticketNumber: `TCK-${new Date().getFullYear()}-${String(tid).padStart(6, '0')}`, subject: String(b.subject), description: String(b.description),
      priority: b.priority as 'LOW', status: 'OPEN', reporterPersonId: reporter, relatedAssetId: b.relatedAssetId != null ? asset(db, Number(b.relatedAssetId)).id : null,
      assignedToUserId: null, resolvedAt: null, closedAt: null, createdAt: nowIso(), updatedAt: nowIso(),
    };
    db.tickets.push(t);
    return ok(ticketDto(db, t, []), 201);
  }
  if (is('PATCH', 'tickets/*')) {
    require(canManage);
    const t = ticketById(id(1));
    if (b.priority) t.priority = b.priority as 'LOW';
    if (b.assignedToUserId != null) t.assignedToUserId = Number(b.assignedToUserId);
    t.updatedAt = nowIso();
    return ok(ticketDto(db, t, null));
  }
  if (is('POST', 'tickets/*/status')) {
    require(canManage);
    const t = ticketById(id(1));
    const to = b.to as TicketStatus;
    if (!TICKET_TRANSITIONS[t.status].includes(to)) throw conflict('ticket.illegal_transition', `Cannot move ticket from ${t.status} to ${to}`);
    t.status = to;
    if (to === 'RESOLVED') t.resolvedAt = nowIso();
    if (to === 'CLOSED') t.closedAt = nowIso();
    t.updatedAt = nowIso();
    return ok(ticketDto(db, t, null));
  }
  if (is('POST', 'tickets/*/comments')) {
    const t = ticketById(id(1));
    if (!String(b.body ?? '').trim()) throw badRequest('body', 'must not be blank');
    const c = { id: nextId(db, 'comments'), ticketId: t.id, authorUserId: me.id, body: String(b.body), internal: employeeOnly ? false : Boolean(b.internal), createdAt: nowIso() };
    db.comments.push(c);
    return ok(commentDto(db, c));
  }

  throw new HttpError(404, 'resource.not_found', `No demo handler for ${method} /api/v1${path}`);
}

// ── tokens ──────────────────────────────────────────────────────────────────

function tokens(user: DbUser) {
  return { accessToken: `demo.${user.id}`, refreshToken: `demo-refresh.${user.id}`, tokenType: 'Bearer', expiresIn: 900 };
}
function userFromToken(db: Db, token: string): DbUser | undefined {
  const m = /^demo(?:-refresh)?\.(\d+)$/.exec(token);
  return m ? db.users.find(u => u.id === Number(m[1])) : undefined;
}

// ── lookups ─────────────────────────────────────────────────────────────────

function person(db: Db, id: number) { const p = db.people.find(x => x.id === id); if (!p) throw notFound('Person', id); return p; }
function department(db: Db, id: number) { const d = db.departments.find(x => x.id === id && !x.deleted); if (!d) throw notFound('Department', id); return d; }
function model(db: Db, id: number) { const m = db.models.find(x => x.id === id); if (!m) throw notFound('AssetModel', id); return m; }
function asset(db: Db, id: number) { const a = db.assets.find(x => x.id === id); if (!a) throw notFound('Asset', id); return a; }
function license(db: Db, id: number) { const l = db.licenses.find(x => x.id === id); if (!l) throw notFound('SoftwareLicense', id); return l; }
const fullName = (db: Db, personId: number) => { const p = person(db, personId); return `${p.firstName} ${p.lastName}`; };
const userName = (db: Db, userId: number | null) => {
  const u = userId == null ? undefined : db.users.find(x => x.id === userId);
  return u ? fullName(db, u.personId) : null;
};

function matches(q: Query, ...fields: string[]) {
  const term = (q.get('q') ?? '').trim().toLowerCase();
  return !term || fields.some(f => f.toLowerCase().includes(term));
}

function page<T>(rows: T[], q: Query) {
  const size = Math.max(1, Number(q.get('size') ?? 20));
  const pageNo = Math.max(0, Number(q.get('page') ?? 0));
  const totalPages = Math.max(1, Math.ceil(rows.length / size));
  return {
    content: rows.slice(pageNo * size, pageNo * size + size),
    page: pageNo, size, totalElements: rows.length, totalPages,
    first: pageNo === 0, last: pageNo >= totalPages - 1,
  };
}

// ── DTOs (same shapes as the backend responses) ─────────────────────────────

function departmentDto(d: Db['departments'][number]) {
  const { deleted: _deleted, ...rest } = d;
  return rest;
}
function employeeDto(db: Db, e: Db['employees'][number]) {
  const p = person(db, e.personId);
  const d = db.departments.find(x => x.id === e.departmentId);
  return { ...e, firstName: p.firstName, lastName: p.lastName, email: p.email, departmentCode: d?.code ?? '' };
}
function modelDto(db: Db, id: number) {
  const m = model(db, id);
  return { ...m, categoryCode: db.categories.find(c => c.id === m.categoryId)?.code ?? '' };
}
function assetDto(db: Db, a: DbAsset) {
  const m = modelDto(db, a.modelId);
  return { ...a, modelManufacturer: m.manufacturer, modelName: m.modelName, categoryCode: m.categoryCode };
}
function assignmentDto(db: Db, id: number) {
  const x = db.assignments.find(r => r.id === id)!;
  return { ...x, assetTag: asset(db, x.assetId).assetTag, assigneeName: fullName(db, x.assigneePersonId), open: !x.actualReturnAt };
}
function licenseDto(db: Db, id: number) {
  const l = license(db, id);
  const used = db.seats.filter(s => s.licenseId === id && !s.releasedAt).length;
  return {
    id: l.id, productId: l.id, productVendor: l.vendor, productName: l.productName, productVersion: l.productVersion,
    licenseReference: l.licenseReference, licenseType: l.licenseType, seatsTotal: l.seatsTotal, seatsUsed: used,
    seatsAvailable: l.seatsTotal - used, purchaseDate: l.purchaseDate, expiresOn: l.expiresOn, cost: l.cost,
    procurementRef: l.procurementRef, createdAt: l.createdAt, updatedAt: l.updatedAt,
  };
}
function seatDto(db: Db, id: number) {
  const s = db.seats.find(x => x.id === id)!;
  return {
    ...s, personName: s.personId != null ? fullName(db, s.personId) : null,
    assetTag: s.assetId != null ? asset(db, s.assetId).assetTag : null, open: !s.releasedAt,
  };
}
function maintenanceDto(db: Db, id: number) {
  const m = db.maintenance.find(x => x.id === id)!;
  return { ...m, assetTag: asset(db, m.assetId).assetTag, performedByName: userName(db, m.performedByUserId) };
}
function commentDto(db: Db, c: Db['comments'][number]) {
  return { id: c.id, authorUserId: c.authorUserId, authorName: userName(db, c.authorUserId), body: c.body, internal: c.internal, createdAt: c.createdAt };
}
function comments(db: Db, ticketId: number, hideInternal: boolean) {
  return db.comments.filter(c => c.ticketId === ticketId && !(hideInternal && c.internal)).map(c => commentDto(db, c));
}
function ticketDto(db: Db, t: DbTicket, commentList: ReturnType<typeof commentDto>[] | null) {
  return {
    ...t, reporterName: fullName(db, t.reporterPersonId),
    relatedAssetTag: t.relatedAssetId != null ? asset(db, t.relatedAssetId).assetTag : null,
    assignedToName: userName(db, t.assignedToUserId), comments: commentList,
  };
}

function stats(db: Db) {
  const count = <T,>(rows: T[], label: (r: T) => string) => {
    const m = new Map<string, number>();
    for (const r of rows) m.set(label(r), (m.get(label(r)) ?? 0) + 1);
    return [...m].map(([l, c]) => ({ label: l, count: c })).sort((x, y) => x.label.localeCompare(y.label));
  };
  const byStatus = count(db.assets, a => a.status);
  const pick = (s: string) => byStatus.find(x => x.label === s)?.count ?? 0;
  const byCategory = db.categories.map(c => ({
    label: c.name,
    count: db.assets.filter(a => model(db, a.modelId).categoryId === c.id).length,
  })).sort((x, y) => x.label.localeCompare(y.label));
  const openAssignments = db.assignments.filter(x => !x.actualReturnAt);
  const deptOf = (personId: number) => {
    const e = db.employees.find(x => x.personId === personId);
    return e ? db.departments.find(d => d.id === e.departmentId)?.code : undefined;
  };
  const byDept = count(openAssignments.filter(x => deptOf(x.assigneePersonId)), x => deptOf(x.assigneePersonId)!);
  const openTickets = db.tickets.filter(t => t.status !== 'CLOSED');
  const cutoff = day(60);
  return {
    totalAssets: db.assets.length, availableAssets: pick('IN_STOCK'), assignedAssets: pick('ASSIGNED'),
    assetsUnderMaintenance: pick('UNDER_MAINTENANCE'), retiredAssets: pick('RETIRED'),
    totalEmployees: db.employees.length, openTickets: openTickets.length, openAssignments: openAssignments.length,
    licensesExpiringSoon: db.licenses.filter(l => l.expiresOn && l.expiresOn <= cutoff).length,
    assetsByStatus: byStatus, assetsByCategory: byCategory, assetsByDepartment: byDept,
    ticketsByPriority: count(openTickets, t => t.priority), ticketsByStatus: count(db.tickets, t => t.status),
  };
}
