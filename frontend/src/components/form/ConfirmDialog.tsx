import { FormDialog } from './FormDialog';

/** In-app confirmation (no window.confirm: it blocks the page and can't be styled or tested). */
export function ConfirmDialog({ open, onClose, onConfirm, title, message, confirmLabel = 'Delete', busy, error }: {
  open: boolean;
  onClose: () => void;
  onConfirm: () => void;
  title: string;
  message: string;
  confirmLabel?: string;
  busy: boolean;
  error: unknown;
}) {
  return (
    <FormDialog open={open} onClose={onClose} title={title} submitLabel={confirmLabel}
      onSubmit={onConfirm} busy={busy} error={error} danger>
      <p className="text-sm text-slate-700">{message}</p>
    </FormDialog>
  );
}
