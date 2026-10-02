import { useId, type ReactNode } from 'react';

/**
 * Label + control + optional hint, with the label programmatically linked to the
 * control (htmlFor/id). Screen readers announce the label, clicking it focuses the
 * input, and tests can use getByLabelText.
 */
export function FormField({ label, hint, required, children, className }: {
  label: string;
  hint?: string;
  required?: boolean;
  children: (id: string) => ReactNode;
  className?: string;
}) {
  const id = useId();
  return (
    <div className={className}>
      <label htmlFor={id} className="block text-xs font-medium text-slate-700">
        {label}{required && <span className="ml-0.5 text-red-600" aria-hidden>*</span>}
      </label>
      <div className="mt-1">{children(id)}</div>
      {hint && <p className="mt-1 text-xs text-slate-500">{hint}</p>}
    </div>
  );
}
