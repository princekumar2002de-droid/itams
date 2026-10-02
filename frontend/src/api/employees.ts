import { api, type PageResponse } from '../lib/api';
import type { Employee, EmploymentStatus } from './types';

// `type`, not `interface`: only type aliases are assignable to api()'s Record<string, …> query param.
export type EmployeesListParams = {
  q?: string;
  departmentId?: number;
  page?: number;
  size?: number;
};

export const employeesApi = {
  list(params: EmployeesListParams = {}) {
    return api<PageResponse<Employee>>('/api/v1/employees', { query: params });
  },
  get(id: number) {
    return api<Employee>(`/api/v1/employees/${id}`);
  },
  create(body: { personId: number; employeeNumber: string; departmentId: number; jobTitle?: string; hireDate: string }) {
    return api<Employee>('/api/v1/employees', { method: 'POST', body });
  },
  update(id: number, body: { departmentId?: number; jobTitle?: string; endDate?: string; employmentStatus?: EmploymentStatus }) {
    return api<Employee>(`/api/v1/employees/${id}`, { method: 'PATCH', body });
  },
};
