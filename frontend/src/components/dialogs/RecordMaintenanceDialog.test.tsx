import { describe, expect, it, vi } from 'vitest';
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { RecordMaintenanceDialog } from './RecordMaintenanceDialog';
import { maintenanceApi } from '../../api/maintenance';
import { asUser, renderWithProviders } from '../../test/utils';

vi.mock('../../auth/AuthProvider', () => ({ useAuth: vi.fn() }));
vi.mock('../../api/maintenance', () => ({ maintenanceApi: { create: vi.fn() } }));

function open() {
  renderWithProviders(<RecordMaintenanceDialog open onClose={() => {}} assetId={3} assetTag="LAP-3" />);
}

describe('<RecordMaintenanceDialog> performer rule (internal XOR external)', () => {
  it('internal work is recorded against the current user, without a provider', async () => {
    asUser(['IT_MANAGER'], { userId: 42 });
    vi.mocked(maintenanceApi.create).mockResolvedValue({} as never);
    open();
    await userEvent.type(screen.getByLabelText(/description/i), 'Replaced battery');
    await userEvent.click(screen.getByRole('button', { name: /save record/i }));
    expect(maintenanceApi.create).toHaveBeenCalledWith(expect.objectContaining({
      assetId: 3, performedByUserId: 42, providerName: undefined, description: 'Replaced battery',
    }));
  });

  it('external work requires a provider name and does not send a user id', async () => {
    asUser(['IT_MANAGER'], { userId: 42 });
    vi.mocked(maintenanceApi.create).mockResolvedValue({} as never);
    open();
    await userEvent.selectOptions(screen.getByLabelText(/performed by/i), 'external');
    await userEvent.type(screen.getByLabelText(/description/i), 'Screen swap');
    expect(screen.getByRole('button', { name: /save record/i })).toBeDisabled();   // provider still empty
    await userEvent.type(screen.getByLabelText(/provider name/i), 'Dell ProSupport');
    await userEvent.click(screen.getByRole('button', { name: /save record/i }));
    expect(maintenanceApi.create).toHaveBeenCalledWith(expect.objectContaining({
      performedByUserId: undefined, providerName: 'Dell ProSupport',
    }));
  });
});
