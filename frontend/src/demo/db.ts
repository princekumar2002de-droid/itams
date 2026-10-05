// In-browser stand-in for the PostgreSQL database, used only by the demo build
// (VITE_DEMO=true). It holds the same demo company that the screenshot script
// creates through the real API.

import type {
  AssetCondition, AssetStatus, EmploymentStatus, LicenseType, TicketPriority, TicketStatus,
} from '../api/types';

export interface DbUser { id: number; username: string; personId: number; role: 'ADMIN' | 'IT_MANAGER' | 'EMPLOYEE' }
export interface DbPerson { id: number; firstName: string; lastName: string; email: string; phone: string | null; active: boolean; createdAt: string; updatedAt: string }
export interface DbDepartment { id: number; code: string; name: string; parentDepartmentId: number | null; managerPersonId: number | null; deleted: boolean; createdAt: string; updatedAt: string }
export interface DbEmployee { id: number; personId: number; employeeNumber: string; departmentId: number; jobTitle: string | null; hireDate: string; endDate: string | null; employmentStatus: EmploymentStatus; createdAt: string; updatedAt: string }
export interface DbCategory { id: number; code: string; name: string; description: string | null }
export interface DbModel { id: number; categoryId: number; manufacturer: string; modelName: string; specs: string | null }
export interface DbAsset { id: number; assetTag: string; modelId: number; serialNumber: string; status: AssetStatus; purchaseDate: string; purchasePrice: number; warrantyEndsOn: string | null; notes: string | null; retiredAt: string | null; retiredReason: string | null; createdAt: string; updatedAt: string }
export interface DbAssignment { id: number; assetId: number; assigneePersonId: number; assignedByUserId: number; assignedAt: string; expectedReturnOn: string | null; actualReturnAt: string | null; returnedByUserId: number | null; outCondition: AssetCondition; inCondition: AssetCondition | null; notes: string | null }
export interface DbLicense { id: number; vendor: string; productName: string; productVersion: string | null; licenseReference: string; licenseType: LicenseType; seatsTotal: number; purchaseDate: string; expiresOn: string | null; cost: number; procurementRef: string | null; createdAt: string; updatedAt: string }
export interface DbSeat { id: number; licenseId: number; personId: number | null; assetId: number | null; assignedAt: string; releasedAt: string | null; notes: string | null }
export interface DbMaintenance { id: number; assetId: number; performedOn: string; performedByUserId: number | null; providerName: string | null; description: string; cost: number | null; nextScheduledOn: string | null; createdAt: string; updatedAt: string }
export interface DbTicket { id: number; ticketNumber: string; subject: string; description: string; priority: TicketPriority; status: TicketStatus; reporterPersonId: number; relatedAssetId: number | null; assignedToUserId: number | null; resolvedAt: string | null; closedAt: string | null; createdAt: string; updatedAt: string }
export interface DbComment { id: number; ticketId: number; authorUserId: number; body: string; internal: boolean; createdAt: string }

export interface Db {
  seq: Record<string, number>;
  users: DbUser[];
  people: DbPerson[];
  departments: DbDepartment[];
  employees: DbEmployee[];
  categories: DbCategory[];
  models: DbModel[];
  assets: DbAsset[];
  assignments: DbAssignment[];
  licenses: DbLicense[];
  seats: DbSeat[];
  maintenance: DbMaintenance[];
  tickets: DbTicket[];
  comments: DbComment[];
}

export const nowIso = () => new Date().toISOString();
export const day = (offset: number) => new Date(Date.now() + offset * 864e5).toISOString().slice(0, 10);

export function nextId(db: Db, table: string): number {
  db.seq[table] = (db.seq[table] ?? 0) + 1;
  return db.seq[table];
}

/** Builds the demo company. Mirrors the business rules, so the history it produces is a legal one. */
export function createDemoDb(): Db {
  const db: Db = {
    seq: {}, users: [], people: [], departments: [], employees: [], categories: [], models: [],
    assets: [], assignments: [], licenses: [], seats: [], maintenance: [], tickets: [], comments: [],
  };
  const ts = nowIso();

  const person = (firstName: string, lastName: string) => {
    const p: DbPerson = {
      id: nextId(db, 'people'), firstName, lastName,
      email: `${firstName}.${lastName}@example-company.de`.toLowerCase().replace(/\s/g, ''),
      phone: null, active: true, createdAt: ts, updatedAt: ts,
    };
    db.people.push(p);
    return p.id;
  };

  // Login accounts, as seeded by DevBootstrap in the real app.
  for (const [username, first, last, role] of [
    ['admin', 'Admin', 'User', 'ADMIN'],
    ['itmanager', 'IT', 'Manager', 'IT_MANAGER'],
    ['employee', 'Regular', 'Employee', 'EMPLOYEE'],
  ] as const) {
    db.users.push({ id: nextId(db, 'users'), username, personId: person(first, last), role });
  }
  const [, itm, emp] = db.users;

  // Reference data from Flyway migration V2.
  for (const [code, name, description] of [
    ['DESKTOP', 'Desktop', 'Non-portable workstation'], ['DOCK', 'Dock', 'Docking station / hub'],
    ['LAPTOP', 'Laptop', 'Portable computer'], ['MONITOR', 'Monitor', 'External display'],
    ['OTHER', 'Other', 'Anything not covered above'], ['PERIPHERAL', 'Peripheral', 'Keyboard, mouse, headset, webcam, etc.'],
    ['PHONE', 'Phone', 'Company-issued smartphone'], ['TABLET', 'Tablet', 'Company-issued tablet'],
  ]) {
    db.categories.push({ id: nextId(db, 'categories'), code, name, description });
  }
  const cat = (code: string) => db.categories.find(c => c.code === code)!.id;

  const dep: Record<string, number> = {};
  for (const [code, name, parent] of [
    ['IT', 'IT Services'], ['FIN', 'Finance & Controlling'], ['SALES', 'Sales'],
    ['HR', 'Human Resources'], ['OPS', 'Operations & Logistics'], ['FIN-ACC', 'Accounting', 'FIN'],
  ]) {
    const id = nextId(db, 'departments');
    db.departments.push({ id, code, name, parentDepartmentId: parent ? dep[parent] : null, managerPersonId: null, deleted: false, createdAt: ts, updatedAt: ts });
    dep[code] = id;
  }

  const p: Record<string, number> = {};
  let empNo = 1001;
  const hire = (personId: number, d: string, jobTitle: string, daysAgo: number) => {
    db.employees.push({
      id: nextId(db, 'employees'), personId, employeeNumber: `E-${empNo++}`, departmentId: dep[d], jobTitle,
      hireDate: day(-daysAgo), endDate: null, employmentStatus: 'ACTIVE', createdAt: ts, updatedAt: ts,
    });
  };
  for (const [first, last, d, title, daysAgo] of [
    ['Lena', 'Wagner', 'IT', 'IT Service Lead', 1400], ['Jonas', 'Becker', 'IT', 'System Administrator', 900],
    ['Sophie', 'Hoffmann', 'FIN', 'Head of Finance', 2100], ['Mehmet', 'Yilmaz', 'FIN-ACC', 'Accountant', 600],
    ['Anna', 'Schulz', 'SALES', 'Sales Manager', 1300], ['Lukas', 'Fischer', 'SALES', 'Account Executive', 420],
    ['Marie', 'Klein', 'SALES', 'Inside Sales', 210], ['Felix', 'Wolf', 'HR', 'HR Business Partner', 800],
    ['Julia', 'Neumann', 'OPS', 'Logistics Coordinator', 1050], ['Tobias', 'Schwarz', 'OPS', 'Warehouse Supervisor', 1700],
    ['Laura', 'Zimmermann', 'FIN', 'Controller', 350],
  ] as const) {
    p[first] = person(first, last);
    hire(p[first], d, title, daysAgo);
  }
  hire(emp.personId, 'SALES', 'Sales Assistant', 150);
  db.departments.find(d => d.code === 'IT')!.managerPersonId = p.Lena;
  db.departments.find(d => d.code === 'FIN')!.managerPersonId = p.Sophie;
  db.departments.find(d => d.code === 'SALES')!.managerPersonId = p.Anna;

  const model: Record<string, number> = {};
  for (const [key, c, manufacturer, modelName] of [
    ['lat', 'LAPTOP', 'Dell', 'Latitude 7450'], ['t14', 'LAPTOP', 'Lenovo', 'ThinkPad T14 Gen 5'],
    ['mbp', 'LAPTOP', 'Apple', 'MacBook Pro 14" M4'], ['u27', 'MONITOR', 'Dell', 'UltraSharp U2724D'],
    ['iph', 'PHONE', 'Apple', 'iPhone 15'], ['dock', 'DOCK', 'Lenovo', 'ThinkPad Universal USB-C Dock'],
    ['opt', 'DESKTOP', 'Dell', 'OptiPlex 7020 Micro'], ['ipad', 'TABLET', 'Apple', 'iPad 10th gen'],
  ]) {
    const id = nextId(db, 'models');
    db.models.push({ id, categoryId: cat(c), manufacturer, modelName, specs: null });
    model[key] = id;
  }

  const a: Record<string, number> = {};
  for (const [tag, m, serial, age, price] of [
    ['NB-2024-001', 'lat', 'DL7450-8KQ2M14', 520, 1689], ['NB-2024-002', 'lat', 'DL7450-8KQ2M15', 520, 1689],
    ['NB-2024-003', 't14', 'PF4XK2LM', 470, 1349], ['NB-2024-004', 't14', 'PF4XK2LQ', 470, 1349],
    ['NB-2025-005', 'mbp', 'C02FX91JMD6R', 300, 2299], ['NB-2025-006', 't14', 'PF5AB7TR', 120, 1379],
    ['NB-2025-007', 'lat', 'DL7450-9MZ1A02', 60, 1649], ['MON-2024-001', 'u27', 'CN-0F2K7W-74261', 500, 429],
    ['MON-2024-002', 'u27', 'CN-0F2K7W-74262', 500, 429], ['MON-2024-003', 'u27', 'CN-0F2K7W-74263', 500, 429],
    ['MON-2025-004', 'u27', 'CN-0F2K7W-80911', 90, 409], ['PH-2024-001', 'iph', 'F2LZK1XQ0D', 410, 849],
    ['PH-2024-002', 'iph', 'F2LZK1XQ0F', 410, 849], ['PH-2025-003', 'iph', 'G7HPM2WN4A', 45, 799],
    ['DOCK-2024-001', 'dock', '40AY0090EU-31', 480, 259], ['DOCK-2024-002', 'dock', '40AY0090EU-32', 480, 259],
    ['PC-2023-001', 'opt', 'OPT7020-HX12', 900, 899], ['TAB-2024-001', 'ipad', 'DMPZ93KQ1L', 380, 429],
  ] as const) {
    const id = nextId(db, 'assets');
    db.assets.push({
      id, assetTag: tag, modelId: model[m], serialNumber: serial, status: 'IN_STOCK',
      purchaseDate: day(-age), purchasePrice: price, warrantyEndsOn: day(-age + 3 * 365 - 1),
      notes: null, retiredAt: null, retiredReason: null, createdAt: ts, updatedAt: ts,
    });
    a[tag] = id;
  }

  const assign = (tag: string, personId: number, out: AssetCondition = 'GOOD', notes: string | null = null) => {
    const id = nextId(db, 'assignments');
    db.assignments.push({
      id, assetId: a[tag], assigneePersonId: personId, assignedByUserId: itm.id, assignedAt: nowIso(),
      expectedReturnOn: null, actualReturnAt: null, returnedByUserId: null, outCondition: out, inCondition: null, notes,
    });
    db.assets.find(x => x.id === a[tag])!.status = 'ASSIGNED';
    return id;
  };
  const giveBack = (assignmentId: number, inCondition: AssetCondition, toMaintenance: boolean, notes: string) => {
    const as = db.assignments.find(x => x.id === assignmentId)!;
    Object.assign(as, { actualReturnAt: nowIso(), returnedByUserId: itm.id, inCondition, notes });
    db.assets.find(x => x.id === as.assetId)!.status = toMaintenance ? 'UNDER_MAINTENANCE' : 'IN_STOCK';
  };
  const repair = (tag: string, r: Partial<DbMaintenance>) => {
    db.maintenance.push({
      id: nextId(db, 'maintenance'), assetId: a[tag], performedOn: day(0), performedByUserId: null, providerName: null,
      description: '', cost: 0, nextScheduledOn: null, createdAt: ts, updatedAt: ts, ...r,
    });
  };

  // NB-2024-001: returned with a hinge problem, repaired, handed to the next person.
  const first = assign('NB-2024-001', p.Mehmet, 'NEW', 'Onboarding equipment');
  giveBack(first, 'FAIR', true, 'Left hinge loose, display flickers when opened');
  repair('NB-2024-001', { providerName: 'Dell ProSupport', description: 'Replaced display hinge and LCD cable under warranty', cost: 0 });
  db.assets.find(x => x.id === a['NB-2024-001'])!.status = 'IN_STOCK';
  assign('NB-2024-001', p.Lukas, 'GOOD', 'Replacement for his old ThinkPad');

  assign('NB-2024-002', p.Sophie); assign('NB-2024-003', p.Anna); assign('NB-2024-004', p.Jonas);
  assign('NB-2025-005', p.Lena, 'NEW'); assign('NB-2025-006', emp.personId, 'NEW', 'Onboarding equipment');
  assign('MON-2024-001', p.Sophie); assign('MON-2024-002', p.Anna); assign('PH-2024-001', p.Anna);
  assign('PH-2024-002', p.Lukas); assign('DOCK-2024-001', p.Sophie);
  assign('TAB-2024-001', p.Tobias, 'FAIR', 'Warehouse stock counts');

  const ph = assign('PH-2025-003', p.Julia, 'NEW');
  giveBack(ph, 'POOR', true, 'Cracked screen');
  repair('PH-2025-003', { providerName: 'Apple Authorized Service Provider', description: 'Screen replacement quoted, device sent in', cost: 289 });
  repair('MON-2024-003', { performedOn: day(-20), performedByUserId: itm.id, description: 'Firmware update and colour calibration', nextScheduledOn: day(345) });
  repair('PC-2023-001', { performedOn: day(-40), performedByUserId: itm.id, description: 'Fan cleaned, SSD health check' });
  Object.assign(db.assets.find(x => x.id === a['PC-2023-001'])!, {
    status: 'RETIRED', retiredAt: nowIso(), retiredReason: 'Replaced by laptop + dock setup, out of warranty',
  });

  const licence = (l: Omit<DbLicense, 'id' | 'createdAt' | 'updatedAt' | 'productVersion' | 'procurementRef'> & Partial<DbLicense>) => {
    const id = nextId(db, 'licenses');
    db.licenses.push({ productVersion: null, procurementRef: null, ...l, id, createdAt: ts, updatedAt: ts });
    return id;
  };
  const seat = (licenseId: number, who: { personId?: number; assetId?: number }) => {
    db.seats.push({ id: nextId(db, 'seats'), licenseId, personId: who.personId ?? null, assetId: who.assetId ?? null, assignedAt: nowIso(), releasedAt: null, notes: null });
  };
  const m365 = licence({ vendor: 'Microsoft', productName: '365 Business Premium', licenseReference: 'MS-CSP-2025-0412', licenseType: 'SUBSCRIPTION', seatsTotal: 15, purchaseDate: day(-280), expiresOn: day(85), cost: 3960, procurementRef: 'PO-2025-0188' });
  for (const who of ['Lena', 'Jonas', 'Sophie', 'Mehmet', 'Anna', 'Lukas', 'Marie', 'Felix', 'Julia', 'Tobias', 'Laura']) seat(m365, { personId: p[who] });
  seat(m365, { personId: emp.personId });
  const acrobat = licence({ vendor: 'Adobe', productName: 'Acrobat Pro', licenseReference: 'ADB-VIP-88213', licenseType: 'PER_SEAT', seatsTotal: 5, purchaseDate: day(-200), expiresOn: day(165), cost: 1080, procurementRef: 'PO-2025-0231' });
  for (const who of ['Sophie', 'Mehmet', 'Laura']) seat(acrobat, { personId: p[who] });
  const jb = licence({ vendor: 'JetBrains', productName: 'IntelliJ IDEA Ultimate', licenseReference: 'JB-2025-77Q1', licenseType: 'PER_SEAT', seatsTotal: 2, purchaseDate: day(-330), expiresOn: day(35), cost: 1338 });
  seat(jb, { personId: p.Jonas }); seat(jb, { personId: p.Lena });
  const win = licence({ vendor: 'Microsoft', productName: 'Windows 11 Pro', productVersion: '23H2', licenseReference: 'MS-OEM-W11P-2024', licenseType: 'PER_DEVICE', seatsTotal: 10, purchaseDate: day(-520), expiresOn: null, cost: 0 });
  for (const tag of ['NB-2024-001', 'NB-2024-002', 'NB-2024-003', 'NB-2024-004', 'NB-2025-006', 'NB-2025-007']) seat(win, { assetId: a[tag] });

  const ticket = (t: Pick<DbTicket, 'subject' | 'description' | 'priority' | 'reporterPersonId'> & Partial<DbTicket>, steps: TicketStatus[] = [], assignee: number | null = null) => {
    const id = nextId(db, 'tickets');
    const row: DbTicket = {
      relatedAssetId: null, ...t, id, ticketNumber: `TCK-${new Date().getFullYear()}-${String(id).padStart(6, '0')}`,
      status: 'OPEN', assignedToUserId: assignee, resolvedAt: null, closedAt: null, createdAt: nowIso(), updatedAt: nowIso(),
    };
    for (const s of steps) {
      row.status = s;
      if (s === 'RESOLVED') row.resolvedAt = nowIso();
      if (s === 'CLOSED') row.closedAt = nowIso();
    }
    db.tickets.push(row);
    return id;
  };
  const comment = (ticketId: number, authorUserId: number, body: string, internal = false) => {
    db.comments.push({ id: nextId(db, 'comments'), ticketId, authorUserId, body, internal, createdAt: nowIso() });
  };

  const t1 = ticket({
    subject: 'Laptop battery drains within two hours',
    description: 'Since last week my ThinkPad goes from 100 % to empty in about two hours, even with only Outlook and Teams open.',
    priority: 'HIGH', reporterPersonId: emp.personId, relatedAssetId: a['NB-2025-006'],
  }, ['IN_PROGRESS'], itm.id);
  comment(t1, itm.id, 'Thanks for reporting. Could you leave the laptop with IT tomorrow morning? We will run the Lenovo battery diagnostics.');
  comment(t1, emp.id, 'Sure, I will bring it at 9:00.');
  comment(t1, itm.id, 'Battery health at 61 % after 120 days, looks like a warranty case. NB-2024-002 can be lent out meanwhile.', true);
  ticket({ subject: 'Need access to the shared Sales mailbox', description: 'Please add me to sales@ so I can answer customer requests.', priority: 'MEDIUM', reporterPersonId: emp.personId }, ['IN_PROGRESS', 'RESOLVED']);
  ticket({ subject: 'Monitor flickers on docking station', description: 'External monitor goes black every few minutes when connected through the dock.', priority: 'MEDIUM', reporterPersonId: p.Sophie, relatedAssetId: a['DOCK-2024-001'] }, ['IN_PROGRESS', 'WAITING'], itm.id);
  ticket({ subject: 'VPN disconnects when working from home', description: 'VPN drops roughly every 30 minutes since the last update.', priority: 'HIGH', reporterPersonId: p.Anna }, ['IN_PROGRESS'], itm.id);
  ticket({ subject: 'Cracked phone screen', description: 'Dropped the phone in the warehouse, screen is cracked.', priority: 'MEDIUM', reporterPersonId: p.Julia, relatedAssetId: a['PH-2025-003'] }, ['IN_PROGRESS', 'WAITING'], itm.id);
  ticket({ subject: 'New starter needs laptop and licences', description: 'New account executive starts on the 1st, needs laptop, monitor and M365.', priority: 'MEDIUM', reporterPersonId: p.Felix });
  ticket({ subject: 'Printer on 2nd floor not reachable', description: 'Nobody on the 2nd floor can print since this morning.', priority: 'CRITICAL', reporterPersonId: p.Laura }, ['IN_PROGRESS', 'RESOLVED', 'CLOSED'], itm.id);
  ticket({ subject: 'Request: second monitor', description: 'Would like a second monitor for month-end closing.', priority: 'LOW', reporterPersonId: p.Mehmet });

  return db;
}
