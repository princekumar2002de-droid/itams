import type { ReactNode } from 'react';
import { cn } from '../../lib/cn';

type Tone = 'gray' | 'green' | 'amber' | 'red' | 'blue' | 'slate';

const tones: Record<Tone, string> = {
  gray:  'bg-slate-100 text-slate-700 ring-slate-200',
  green: 'bg-green-100 text-green-800 ring-green-200',
  amber: 'bg-amber-100 text-amber-800 ring-amber-200',
  red:   'bg-red-100   text-red-800   ring-red-200',
  blue:  'bg-blue-100  text-blue-800  ring-blue-200',
  slate: 'bg-slate-200 text-slate-800 ring-slate-300',
};

export function Badge({ tone = 'gray', children, className }:
  { tone?: Tone; children: ReactNode; className?: string }) {
  return (
    <span className={cn(
      'inline-flex items-center rounded-md px-2 py-0.5 text-xs font-medium ring-1 ring-inset',
      tones[tone], className
    )}>{children}</span>
  );
}
