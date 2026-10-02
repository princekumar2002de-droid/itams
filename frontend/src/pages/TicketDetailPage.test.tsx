import { describe, expect, it, vi } from 'vitest';
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { TicketDetailPage, TRANSITIONS } from './TicketDetailPage';
import { ticketsApi } from '../api/tickets';
import type { Ticket } from '../api/types';
import { asUser, renderWithProviders } from '../test/utils';

vi.mock('../auth/AuthProvider', () => ({ useAuth: vi.fn() }));
vi.mock('../api/tickets', () => ({ ticketsApi: { get: vi.fn(), changeStatus: vi.fn(), update: vi.fn(), addComment: vi.fn() } }));

const base: Ticket = {
  id: 5, ticketNumber: 'TCK-2026-000005', subject: 'VPN drops', description: 'Every hour',
  priority: 'MEDIUM', status: 'OPEN', reporterPersonId: 70, reporterName: 'Eve Employee',
  relatedAssetId: null, relatedAssetTag: null, assignedToUserId: null, resolvedAt: null, closedAt: null,
  createdAt: '2026-10-01T08:00:00Z', updatedAt: '2026-10-01T08:00:00Z',
  comments: [{ id: 1, authorUserId: 9, body: 'Looking into it', internal: false, createdAt: '2026-10-01T09:00:00Z' }],
};

function show(t: Partial<Ticket>) {
  vi.mocked(ticketsApi.get).mockResolvedValue({ ...base, ...t });
  return renderWithProviders(<TicketDetailPage />, { path: '/tickets/:id', route: '/tickets/5' });
}

describe('<TicketDetailPage>', () => {
  it('offers a manager exactly the legal next statuses (mirrors the backend state machine)', async () => {
    asUser(['IT_MANAGER']);
    show({ status: 'IN_PROGRESS' });
    expect(await screen.findByRole('button', { name: 'Resolve' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Waiting for reply' })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Start work' })).not.toBeInTheDocument();
    expect(TRANSITIONS.CLOSED).toEqual([]);
  });

  it('calls the status endpoint with the chosen transition', async () => {
    asUser(['IT_MANAGER']);
    vi.mocked(ticketsApi.changeStatus).mockResolvedValue({ ...base, status: 'IN_PROGRESS' });
    show({ status: 'OPEN' });
    await userEvent.click(await screen.findByRole('button', { name: 'Start work' }));
    expect(ticketsApi.changeStatus).toHaveBeenCalledWith(5, 'IN_PROGRESS');
  });

  it('gives an EMPLOYEE no workflow controls and no internal-note option', async () => {
    asUser(['EMPLOYEE']);
    show({ status: 'OPEN' });
    expect(await screen.findByText('Every hour')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Start work' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /assign to me/i })).not.toBeInTheDocument();
    expect(screen.queryByLabelText(/internal note/i)).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: /post comment/i })).toBeInTheDocument();
  });

  it('"Assign to me" sends the current user id', async () => {
    asUser(['IT_MANAGER'], { userId: 42 });
    vi.mocked(ticketsApi.update).mockResolvedValue({ ...base, assignedToUserId: 42 });
    show({ status: 'OPEN' });
    await userEvent.click(await screen.findByRole('button', { name: /assign to me/i }));
    expect(ticketsApi.update).toHaveBeenCalledWith(5, { assignedToUserId: 42 });
  });
});
