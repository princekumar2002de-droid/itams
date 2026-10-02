import { Button } from './Button';

interface Props {
  page: number;        // zero-based
  totalPages: number;
  first: boolean;
  last: boolean;
  totalElements: number;
  size: number;
  onChange: (nextPage: number) => void;
}

export function Pagination({ page, totalPages, first, last, totalElements, size, onChange }: Props) {
  if (totalElements === 0) return null;
  const from = page * size + 1;
  const to = Math.min((page + 1) * size, totalElements);

  return (
    <div className="flex items-center justify-between border-t border-slate-200 px-4 py-3 text-sm text-slate-600">
      <span>
        Showing <span className="font-medium">{from}</span>–<span className="font-medium">{to}</span> of{' '}
        <span className="font-medium">{totalElements}</span>
      </span>
      <div className="flex items-center gap-2">
        <Button size="sm" variant="secondary" disabled={first} onClick={() => onChange(page - 1)}>Previous</Button>
        <span className="tabular-nums">
          Page {page + 1} of {Math.max(totalPages, 1)}
        </span>
        <Button size="sm" variant="secondary" disabled={last} onClick={() => onChange(page + 1)}>Next</Button>
      </div>
    </div>
  );
}
