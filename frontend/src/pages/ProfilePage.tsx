import { useAuth } from '../auth/AuthProvider';
import { PageHeader } from '../components/ui/PageHeader';
import { Card, CardBody, CardHeader } from '../components/ui/Card';
import { Badge } from '../components/ui/Badge';

export function ProfilePage() {
  const { user } = useAuth();
  if (!user) return null;

  return (
    <>
      <PageHeader title="My profile" description="Your account and roles. Contact an administrator to change your details." />

      <div className="grid gap-4 lg:grid-cols-3">
        <Card className="lg:col-span-2">
          <CardHeader><div className="text-sm font-semibold">Account</div></CardHeader>
          <CardBody>
            <dl className="grid grid-cols-2 gap-x-6 gap-y-3 text-sm">
              <Field label="Username">{user.username}</Field>
              <Field label="User id">#{user.userId}</Field>
              <Field label="First name">{user.firstName}</Field>
              <Field label="Last name">{user.lastName}</Field>
              <Field label="Email">{user.email}</Field>
              <Field label="Person id">#{user.personId}</Field>
            </dl>
          </CardBody>
        </Card>

        <Card>
          <CardHeader><div className="text-sm font-semibold">Roles</div></CardHeader>
          <CardBody>
            {user.roles.length === 0
              ? <div className="text-sm text-slate-500">No roles assigned.</div>
              : <div className="flex flex-wrap gap-2">
                  {user.roles.map(r => <Badge key={r} tone="slate">{r.replace('ROLE_', '')}</Badge>)}
                </div>}
          </CardBody>
        </Card>
      </div>
    </>
  );
}

function Field({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div>
      <dt className="text-xs font-medium uppercase tracking-wide text-slate-500">{label}</dt>
      <dd className="mt-0.5 text-slate-900">{children}</dd>
    </div>
  );
}
