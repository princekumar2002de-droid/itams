// Response shapes that mirror the backend DTOs.

export type AssetStatus = 'IN_STOCK' | 'ASSIGNED' | 'UNDER_MAINTENANCE' | 'RETIRED' | 'LOST';
export type EmploymentStatus = 'ACTIVE' | 'ON_LEAVE' | 'LEFT';
export type AssetCondition = 'NEW' | 'GOOD' | 'FAIR' | 'POOR';
export type LicenseType = 'PER_SEAT' | 'PER_DEVICE' | 'SITE' | 'SUBSCRIPTION';
export type TicketStatus = 'OPEN' | 'IN_PROGRESS' | 'WAITING' | 'RESOLVED' | 'CLOSED';
export type TicketPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';

export interface Department {
  id: number;
  code: string;
  name: string;
  parentDepartmentId: number | null;
  managerPersonId: number | null;
  createdAt: string;
  updatedAt: string;
}

export interface Person {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  phone: string | null;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface Employee {
  id: number;
  personId: number;
  firstName: string;
  lastName: string;
  email: string;
  employeeNumber: string;
  departmentId: number;
  departmentCode: string;
  jobTitle: string | null;
  hireDate: string;
  endDate: string | null;
  employmentStatus: EmploymentStatus;
  createdAt: string;
  updatedAt: string;
}

export interface AssetCategory {
  id: number;
  code: string;
  name: string;
  description: string | null;
}

export interface AssetModel {
  id: number;
  categoryId: number;
  categoryCode: string;
  manufacturer: string;
  modelName: string;
  specs: string | null;
}

export interface Asset {
  id: number;
  assetTag: string;
  modelId: number;
  modelManufacturer: string;
  modelName: string;
  categoryCode: string;
  serialNumber: string;
  status: AssetStatus;
  purchaseDate: string;
  purchasePrice: string;
  warrantyEndsOn: string | null;
  notes: string | null;
  retiredAt: string | null;
  retiredReason: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface AssetAssignment {
  id: number;
  assetId: number;
  assetTag: string;
  assigneePersonId: number;
  assigneeName: string;
  assignedByUserId: number;
  assignedAt: string;
  expectedReturnOn: string | null;
  actualReturnAt: string | null;
  returnedByUserId: number | null;
  outCondition: AssetCondition;
  inCondition: AssetCondition | null;
  notes: string | null;
  open: boolean;
}

// ── licenses ────────────────────────────────────────────────────────────────

export interface License {
  id: number;
  productId: number;
  productVendor: string;
  productName: string;
  productVersion: string | null;
  licenseReference: string;
  licenseType: LicenseType;
  seatsTotal: number;
  seatsUsed: number;
  seatsAvailable: number;
  purchaseDate: string;
  expiresOn: string | null;
  cost: string;
  procurementRef: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface LicenseAssignment {
  id: number;
  licenseId: number;
  personId: number | null;
  personName: string | null;
  assetId: number | null;
  assetTag: string | null;
  assignedAt: string;
  releasedAt: string | null;
  notes: string | null;
  open: boolean;
}

// ── maintenance ─────────────────────────────────────────────────────────────

export interface MaintenanceRecord {
  id: number;
  assetId: number;
  assetTag: string;
  performedOn: string;
  performedByUserId: number | null;
  providerName: string | null;
  description: string;
  cost: string | null;
  nextScheduledOn: string | null;
  createdAt: string;
  updatedAt: string;
}

// ── tickets ─────────────────────────────────────────────────────────────────

export interface TicketComment {
  id: number;
  authorUserId: number;
  body: string;
  internal: boolean;
  createdAt: string;
}

export interface Ticket {
  id: number;
  ticketNumber: string;
  subject: string;
  description: string;
  priority: TicketPriority;
  status: TicketStatus;
  reporterPersonId: number;
  reporterName: string;
  relatedAssetId: number | null;
  relatedAssetTag: string | null;
  assignedToUserId: number | null;
  resolvedAt: string | null;
  closedAt: string | null;
  createdAt: string;
  updatedAt: string;
  comments: TicketComment[] | null;
}

// ── stats ───────────────────────────────────────────────────────────────────

export interface CountByLabel { label: string; count: number; }

export interface DashboardStats {
  totalAssets: number;
  availableAssets: number;
  assignedAssets: number;
  assetsUnderMaintenance: number;
  retiredAssets: number;
  totalEmployees: number;
  openTickets: number;
  openAssignments: number;
  licensesExpiringSoon: number;
  assetsByStatus:     CountByLabel[];
  assetsByCategory:   CountByLabel[];
  assetsByDepartment: CountByLabel[];
  ticketsByPriority:  CountByLabel[];
  ticketsByStatus:    CountByLabel[];
}
