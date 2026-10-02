import { forwardRef, type TextareaHTMLAttributes } from 'react';
import { cn } from '../../lib/cn';

export const Textarea = forwardRef<HTMLTextAreaElement, TextareaHTMLAttributes<HTMLTextAreaElement>>(
  function Textarea({ className, ...rest }, ref) {
    return (
      <textarea
        ref={ref}
        rows={4}
        className={cn(
          'block w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm',
          'text-slate-900 placeholder:text-slate-400',
          'focus:border-brand-600 focus:outline-none focus:ring-1 focus:ring-brand-600',
          className
        )}
        {...rest}
      />
    );
  }
);
