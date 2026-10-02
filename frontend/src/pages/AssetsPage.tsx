import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { Plus } from 'lucide-react';
import { usePermissions } from '../auth/permissions';
import { Button } from '../components/ui/Button';
import { CreateAssetDialog } from '../components/dialogs/CreateAssetDialog';
import { assetsApi } from '../api/assets';
import { PageHeader } from '../components/ui/PageHeader';
import { SearchInput } from '../components/ui/SearchInput';
import { Select } from '../components/ui/Select';
import { Card } from '../components/ui/Card';
import { EmptyState } from '../components/ui/EmptyState';
import { ErrorState } from '../components/ui/ErrorState';
import { TableSkeleton } from '../components/ui/Skeleton';
import { Pagination } from '../components/ui/Pagination';
import { AssetStatusBadge } from '../components/ui/StatusBadge';
import { fmtDate, fmtEUR } from '../lib/format';
import type { AssetStatus } from '../api/types';

const SIZE = 20;
const STATUSES: AssetStatus[] = ['IN_STOCK', 'ASSIGNED', 'UNDER_MAINTENANCE', 'RETIRED', 'LOST'];

export function AssetsPage() {
  const [q, setQ] = useState('');
  const [status, setStatus] = useState<AssetStatus | ''>('');
  const [categoryId, setCategoryId] = useState<number | ''>('');
  const [page, setPage] = useState(0);
  const [createOpen, setCreateOpen] = useState(false);
  const { canManage } = usePermissions();

  const categories = useQuery({
    queryKey: ['asset-categories'],
    queryFn: () => assetsApi.categories(),
  });

  const assets = useQuery({
    queryKey: ['assets', { q, status, categoryId, page }],
    queryFn: () => assetsApi.list({
      q: q || undefined,
      status: status === '' ? undefined : status,
      categoryId: categoryId === '' ? undefined : categoryId,
      page, size: SIZE,
    }),
  });

  return (
    <>
      <PageHeader title="Assets" description="Every physical asset tracked in the system."
        actions={canManage && <Button onClick={() => setCreateOpen(true)}><Plus className="h-4 w-4" /> New asset</Button>} />

      <Card>
        <div className="flex flex-wrap items-center gap-3 border-b border-slate-200 p-3">
          <SearchInput
            value={q}
            onChange={v => { setQ(v); setPage(0); }}
            placeholder="Search tag, serial, model…"
            className="w-full sm:w-72"
          />
          <Select
            value={status}
            onChange={e => { setStatus(e.target.value as AssetStatus | ''); setPage(0); }}
            className="w-full sm:w-52"
          >
            <option value="">All statuses</option>
            {STATUSES.map(s => <option key={s} value={s}>{s.replaceAll('_', ' ')}</option>)}
          </Select>
          <Select
            value={categoryId}
            onChange={e => { setCategoryId(e.target.value === '' ? '' : Number(e.target.value)); setPage(0); }}
            className="w-full sm:w-52"
          >
            <option value="">All categories</option>
            {categories.data?.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}
          </Select>
        </div>

        {assets.isLoading ? <TableSkeleton /> :
         assets.isError ? <div className="p-4"><ErrorState error={assets.error} /></div> :
         assets.data && assets.data.content.length > 0 ? (
          <>
            <div className="overflow-x-auto">
              <table className="min-w-full divide-y divide-slate-200 text-sm">
                <thead className="bg-slate-50 text-left text-xs font-medium uppercase tracking-wide text-slate-500">
                  <tr>
                    <th className="px-4 py-2.5">Tag</th>
                    <th className="px-4 py-2.5">Category</th>
                    <th className="px-4 py-2.5">Model</th>
                    <th className="px-4 py-2.5">Serial</th>
                    <th className="px-4 py-2.5">Status</th>
                    <th className="px-4 py-2.5">Purchased</th>
                    <th className="px-4 py-2.5 text-right">Price</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {assets.data.content.map(a => (
                    <tr key={a.id} className="hover:bg-slate-50">
                      <td className="px-4 py-2.5 font-mono text-xs">
                        <Link to={`/assets/${a.id}`} className="text-brand-700 hover:underline">{a.assetTag}</Link>
                      </td>
                      <td className="px-4 py-2.5">{a.categoryCode}</td>
                      <td className="px-4 py-2.5">
                        <div className="font-medium text-slate-900">{a.modelManufacturer}</div>
                        <div className="text-xs text-slate-500">{a.modelName}</div>
                      </td>
                      <td className="px-4 py-2.5 font-mono text-xs text-slate-600">{a.serialNumber}</td>
                      <td className="px-4 py-2.5"><AssetStatusBadge status={a.status} /></td>
                      <td className="px-4 py-2.5 text-slate-700">{fmtDate(a.purchaseDate)}</td>
                      <td className="px-4 py-2.5 text-right tabular-nums">{fmtEUR(a.purchasePrice)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <Pagination
              page={assets.data.page}
              totalPages={assets.data.totalPages}
              first={assets.data.first}
              last={assets.data.last}
              totalElements={assets.data.totalElements}
              size={assets.data.size}
              onChange={setPage}
            />
          </>
        ) : (
          <EmptyState
            title="No assets match your filters"
            description="Try clearing search or filters, or register an asset from the backend API."
          />
        )}
      </Card>
      <CreateAssetDialog open={createOpen} onClose={() => setCreateOpen(false)} />
    </>
  );
}
