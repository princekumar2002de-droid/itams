import { Badge } from './Badge';
import type { AssetStatus } from '../../api/types';

const map: Record<AssetStatus, { tone: 'gray' | 'green' | 'amber' | 'red' | 'blue' | 'slate'; label: string }> = {
  IN_STOCK:          { tone: 'blue',  label: 'In Stock' },
  ASSIGNED:          { tone: 'green', label: 'Assigned' },
  UNDER_MAINTENANCE: { tone: 'amber', label: 'Maintenance' },
  RETIRED:           { tone: 'slate', label: 'Retired' },
  LOST:              { tone: 'red',   label: 'Lost' },
};

export function AssetStatusBadge({ status }: { status: AssetStatus }) {
  const { tone, label } = map[status];
  return <Badge tone={tone}>{label}</Badge>;
}
