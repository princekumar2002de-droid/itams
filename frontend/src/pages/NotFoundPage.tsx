import { Link } from 'react-router-dom';

export function NotFoundPage() {
  return (
    <div className="grid h-full place-items-center bg-slate-50">
      <div className="text-center">
        <div className="text-6xl font-semibold text-slate-300">404</div>
        <div className="mt-2 text-slate-600">Page not found.</div>
        <Link to="/dashboard" className="mt-4 inline-block text-sm font-medium text-brand-700 hover:underline">
          Back to dashboard
        </Link>
      </div>
    </div>
  );
}
