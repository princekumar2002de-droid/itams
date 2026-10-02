import { Search } from 'lucide-react';
import { useEffect, useState } from 'react';
import { cn } from '../../lib/cn';

interface Props {
  value: string;
  onChange: (v: string) => void;
  placeholder?: string;
  className?: string;
  /** Debounce delay in ms. 250 by default. Use 0 to disable. */
  debounceMs?: number;
}

/** Debounced search input — commits to `onChange` after the user stops typing. */
export function SearchInput({ value, onChange, placeholder = 'Search…', className, debounceMs = 250 }: Props) {
  const [local, setLocal] = useState(value);
  useEffect(() => { setLocal(value); }, [value]);
  useEffect(() => {
    if (local === value) return;
    if (debounceMs === 0) { onChange(local); return; }
    const t = setTimeout(() => onChange(local), debounceMs);
    return () => clearTimeout(t);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [local, debounceMs]);

  return (
    <div className={cn('relative', className)}>
      <Search className="pointer-events-none absolute left-2.5 top-2.5 h-4 w-4 text-slate-400" aria-hidden />
      <input
        value={local}
        onChange={e => setLocal(e.target.value)}
        placeholder={placeholder}
        className="block w-full rounded-md border border-slate-300 bg-white pl-8 pr-3 py-2 text-sm text-slate-900 placeholder:text-slate-400 focus:border-brand-600 focus:outline-none focus:ring-1 focus:ring-brand-600"
      />
    </div>
  );
}
