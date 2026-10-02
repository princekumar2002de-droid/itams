import type { FormEvent, ReactNode } from 'react';
import { Modal } from '../ui/Modal';
import { Button } from '../ui/Button';
import { ErrorState } from '../ui/ErrorState';

/**
 * The shared shell of every create/edit dialog: modal + form + server error + footer.
 * The caller owns field state and the mutation; this component only wires them up,
 * so each dialog file contains just its fields and its API call.
 */
export function FormDialog({
  open, onClose, title, description, submitLabel, onSubmit,
  busy, error, canSubmit = true, danger, children,
}: {
  open: boolean;
  onClose: () => void;
  title: string;
  description?: string;
  submitLabel: string;
  onSubmit: () => void;
  busy: boolean;
  error: unknown;
  canSubmit?: boolean;
  danger?: boolean;
  children: ReactNode;
}) {
  function submit(e: FormEvent) {
    e.preventDefault();
    if (canSubmit && !busy) onSubmit();
  }
  return (
    <Modal open={open} onClose={onClose} title={title} description={description} busy={busy}>
      <form className="space-y-4" onSubmit={submit} noValidate={false}>
        {children}
        {error != null && <ErrorState error={error} />}
        <div className="flex justify-end gap-2 pt-1">
          <Button type="button" variant="secondary" onClick={onClose} disabled={busy}>Cancel</Button>
          <Button type="submit" variant={danger ? 'danger' : 'primary'} loading={busy} disabled={!canSubmit}>
            {submitLabel}
          </Button>
        </div>
      </form>
    </Modal>
  );
}
