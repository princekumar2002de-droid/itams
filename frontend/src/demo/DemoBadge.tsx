import { resetDemoData } from './install';

/** Small fixed badge so nobody mistakes the browser-only demo for the real deployment. */
export function DemoBadge() {
  return (
    <div className="fixed bottom-3 right-3 z-50 flex max-w-xs items-center gap-3 rounded-lg border border-amber-300 bg-amber-50 px-3 py-2 text-xs text-amber-900 shadow-sm">
      <span>
        <strong>Demo:</strong> runs in your browser with sample data. The real app uses Spring Boot and PostgreSQL.
      </span>
      <button
        type="button"
        className="shrink-0 rounded border border-amber-400 px-2 py-0.5 font-medium hover:bg-amber-100"
        onClick={() => { resetDemoData(); window.location.reload(); }}
      >
        Reset
      </button>
    </div>
  );
}
