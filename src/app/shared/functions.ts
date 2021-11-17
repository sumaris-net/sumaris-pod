
// TODO: remove after then updating to last version of ngx-components

import { LoadResult } from '@sumaris-net/ngx-components';

export function isNilOrNaN<T>(obj: T | null | undefined): boolean {
  return obj === undefined || obj === null || (typeof obj === 'number' && isNaN(obj));
}


export function mergeLoadResult<T>(res1: LoadResult<T>, res2: LoadResult<T>): LoadResult<T> {
  return {
    data : (res1.data || []).concat(...res2.data),
    total: ((res1.total || res1.data?.length || 0) + (res2.total || res2.data?.length || 0))
  };
}
