import clsx, { type ClassValue } from 'clsx';

/** Tiny className concatenator. Wrapping clsx so we can swap in tailwind-merge later. */
export function cn(...inputs: ClassValue[]): string {
  return clsx(inputs);
}
