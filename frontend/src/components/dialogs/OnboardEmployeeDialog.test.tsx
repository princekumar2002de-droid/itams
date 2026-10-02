import { describe, expect, it, vi } from 'vitest';
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { OnboardEmployeeDialog } from './OnboardEmployeeDialog';
import { peopleApi } from '../../api/people';
import { employeesApi } from '../../api/employees';
import { departmentsApi } from '../../api/departments';
import { ApiError } from '../../lib/api';
import { renderWithProviders } from '../../test/utils';

vi.mock('../../api/people', () => ({ peopleApi: { create: vi.fn(), list: vi.fn() } }));
vi.mock('../../api/employees', () => ({ employeesApi: { create: vi.fn() } }));
vi.mock('../../api/departments', () => ({ departmentsApi: { list: vi.fn() } }));

describe('<OnboardEmployeeDialog>', () => {
  it('creates the person only once when the employment step fails and is retried', async () => {
    vi.mocked(departmentsApi.list).mockResolvedValue({
      content: [{ id: 2, code: 'FIN', name: 'Finance', parentDepartmentId: null, managerPersonId: null, createdAt: '', updatedAt: '' }],
      page: 0, size: 200, totalElements: 1, totalPages: 1, first: true, last: true,
    });
    vi.mocked(peopleApi.create).mockResolvedValue({ id: 99 } as never);
    vi.mocked(employeesApi.create)
      .mockRejectedValueOnce(new ApiError({ code: 'employee.number_taken', message: 'Employee number already used', status: 409 }))
      .mockResolvedValueOnce({} as never);

    renderWithProviders(<OnboardEmployeeDialog open onClose={() => {}} />);
    await userEvent.type(screen.getByLabelText(/first name/i), 'Grace');
    await userEvent.type(screen.getByLabelText(/last name/i), 'Hopper');
    await userEvent.type(screen.getByLabelText(/email/i), 'grace@example.com');
    await userEvent.type(screen.getByLabelText(/employee number/i), 'emp-1');
    await screen.findByRole('option', { name: /FIN/ });
    await userEvent.selectOptions(screen.getByLabelText(/department/i), '2');
    await userEvent.click(screen.getByRole('button', { name: /onboard/i }));

    expect(await screen.findByText(/employee number already used/i)).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: /onboard/i }));

    expect(peopleApi.create).toHaveBeenCalledTimes(1);                       // no duplicate person
    expect(employeesApi.create).toHaveBeenLastCalledWith(expect.objectContaining({ personId: 99, employeeNumber: 'EMP-1' }));
  });
});
