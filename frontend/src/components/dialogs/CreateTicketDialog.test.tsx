import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { CreateTicketDialog } from './CreateTicketDialog';
import { ticketsApi } from '../../api/tickets';
import { useAuth } from '../../auth/AuthProvider';

vi.mock('../../auth/AuthProvider', () => ({ useAuth: vi.fn() }));
vi.mock('../../api/tickets', () => ({ ticketsApi: { create: vi.fn() } }));

function setup() {
  vi.mocked(useAuth).mockReturnValue({
    status: 'authenticated',
    user: { userId: 3, username: 'emp', personId: 42, firstName: 'E', lastName: 'M', email: 'e@x', roles: ['ROLE_EMPLOYEE'] },
    login: vi.fn(), logout: vi.fn(), hasRole: () => false,
  });
  const onClose = vi.fn();
  const qc = new QueryClient({ defaultOptions: { mutations: { retry: false } } });
  render(
    <QueryClientProvider client={qc}>
      <CreateTicketDialog open onClose={onClose} />
    </QueryClientProvider>,
  );
  return { onClose, user: userEvent.setup() };
}

describe('<CreateTicketDialog>', () => {
  it('submits trimmed fields with the logged-in person as reporter, then closes', async () => {
    vi.mocked(ticketsApi.create).mockResolvedValueOnce({} as never);
    const { onClose, user } = setup();

    await user.type(screen.getByPlaceholderText('Short summary'), '  Laptop will not boot  ');
    await user.type(screen.getByPlaceholderText(/what's happening/i), 'Black screen after update');
    await user.selectOptions(screen.getByRole('combobox'), 'HIGH');
    await user.click(screen.getByRole('button', { name: /create ticket/i }));

    expect(ticketsApi.create).toHaveBeenCalledWith({
      subject: 'Laptop will not boot',
      description: 'Black screen after update',
      priority: 'HIGH',
      reporterPersonId: 42,
      relatedAssetId: undefined,      // blank optional field is omitted, not sent as 0
    });
    expect(onClose).toHaveBeenCalled();
  });

  it('does not submit while required fields are empty', async () => {
    const { user } = setup();
    await user.click(screen.getByRole('button', { name: /create ticket/i }));
    expect(ticketsApi.create).not.toHaveBeenCalled();
  });

  it('keeps the dialog open and shows the server error when creation fails', async () => {
    vi.mocked(ticketsApi.create).mockRejectedValueOnce(new Error('Asset 999 not found'));
    const { onClose, user } = setup();

    await user.type(screen.getByPlaceholderText('Short summary'), 'x');
    await user.type(screen.getByPlaceholderText(/what's happening/i), 'y');
    await user.click(screen.getByRole('button', { name: /create ticket/i }));

    expect(await screen.findByText(/asset 999 not found/i)).toBeInTheDocument();
    expect(onClose).not.toHaveBeenCalled();
  });
});
