import { describe, expect, it } from 'vitest';
import { fmtDate, fmtEUR } from './format';

describe('format helpers', () => {
  it('fmtEUR formats numbers and numeric strings the German way', () => {
    // Intl inserts a non-breaking space before €; normalise for the assertion.
    expect(fmtEUR(1234.5).replace(/\s/g, ' ')).toBe('1.234,50 €');
    expect(fmtEUR('99.9').replace(/\s/g, ' ')).toBe('99,90 €');   // BigDecimal arrives as string
  });

  it('fmtEUR shows a dash for null / undefined / non-numeric input', () => {
    expect(fmtEUR(null)).toBe('—');
    expect(fmtEUR(undefined)).toBe('—');
    expect(fmtEUR('abc')).toBe('—');
  });

  it('fmtDate shows a dash for missing dates', () => {
    expect(fmtDate(null)).toBe('—');
    expect(fmtDate('2026-10-02T08:00:00Z')).toMatch(/2026/);
  });
});
