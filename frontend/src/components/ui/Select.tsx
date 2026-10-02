import { forwardRef, type SelectHTMLAttributes } from 'react';
import { cn } from '../../lib/cn';

export const Select = forwardRef<HTMLSelectElement, SelectHTMLAttributes<HTMLSelectElement>>(
  function Select({ className, children, ...rest }, ref) {
    return (
      <select
        ref={ref}
        className={cn(
          'block w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm',
          'text-slate-900 focus:border-brand-600 focus:outline-none focus:ring-1 focus:ring-brand-600',
          className
        )}
        {...rest}
      >
        {children}
      </select>
    );
  }
);
