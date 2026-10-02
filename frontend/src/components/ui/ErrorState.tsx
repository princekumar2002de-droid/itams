import { AlertTriangle } from 'lucide-react';
import { ApiError } from '../../lib/api';

export function ErrorState({ error, className }: { error: unknown; className?: string }) {
  const message = error instanceof ApiError ? error.message
                : error instanceof Error ? error.message
                : 'Something went wrong.';
  const code = error instanceof ApiError ? error.code : undefined;

  return (
    <div className={`flex items-start gap-3 rounded-md border border-red-200 bg-red-50 p-3 text-sm text-red-800 ${className ?? ''}`}>
      <AlertTriangle className="mt-0.5 h-4 w-4 flex-none" aria-hidden />
      <div>
        <div className="font-medium">{message}</div>
        {code && <div className="mt-0.5 text-xs opacity-75">code: {code}</div>}
      </div>
    </div>
  );
}
