import { useState, type FormEvent } from 'react';
import { Navigate, useLocation, useNavigate } from 'react-router-dom';
import { Boxes } from 'lucide-react';
import { useAuth } from '../auth/AuthProvider';
import { Button } from '../components/ui/Button';
import { Input } from '../components/ui/Input';
import { ErrorState } from '../components/ui/ErrorState';

export function LoginPage() {
  const { login, status } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<unknown>(null);

  if (status === 'authenticated') {
    const from = (location.state as { from?: string } | null)?.from ?? '/dashboard';
    return <Navigate to={from} replace />;
  }

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      await login(username.trim(), password);
      const from = (location.state as { from?: string } | null)?.from ?? '/dashboard';
      navigate(from, { replace: true });
    } catch (err) {
      setError(err);
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="grid h-full place-items-center bg-slate-50">
      <div className="w-full max-w-sm rounded-xl border border-slate-200 bg-white p-8 shadow-sm">
        <div className="mb-6 flex items-center gap-2">
          <div className="grid h-9 w-9 place-items-center rounded-md bg-brand-600 text-white">
            <Boxes className="h-5 w-5" aria-hidden />
          </div>
          <div>
            <div className="text-base font-semibold text-slate-900">ITAMS</div>
            <div className="text-xs text-slate-500">IT Asset Management System</div>
          </div>
        </div>

        <h1 className="text-lg font-semibold text-slate-900">Sign in</h1>
        <p className="mt-1 text-sm text-slate-500">Use your ITAMS credentials.</p>

        <form className="mt-5 space-y-4" onSubmit={onSubmit}>
          <div>
            <label htmlFor="username" className="block text-xs font-medium text-slate-700">Username</label>
            <Input
              id="username"
              value={username}
              onChange={e => setUsername(e.target.value)}
              autoComplete="username"
              autoFocus
              required
              className="mt-1"
            />
          </div>
          <div>
            <label htmlFor="password" className="block text-xs font-medium text-slate-700">Password</label>
            <Input
              id="password"
              type="password"
              value={password}
              onChange={e => setPassword(e.target.value)}
              autoComplete="current-password"
              required
              className="mt-1"
            />
          </div>

          {error != null && <ErrorState error={error} />}

          <Button type="submit" loading={submitting} className="w-full">
            Sign in
          </Button>
        </form>

        <div className="mt-6 rounded-md bg-slate-50 p-3 text-xs text-slate-600">
          <div className="font-medium text-slate-700">Demo accounts</div>
          <ul className="mt-1 space-y-0.5 font-mono">
            <li>admin      / changeme</li>
            <li>itmanager  / changeme</li>
            <li>employee   / changeme</li>
          </ul>
        </div>
      </div>
    </div>
  );
}
