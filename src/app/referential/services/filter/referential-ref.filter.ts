import {EntityAsObjectOptions, EntityClass, EntityUtils, FilterFn, isNotEmptyArray, ReferentialRef} from '@sumaris-net/ngx-components';
import {BaseReferentialFilter} from './referential.filter';

@EntityClass({typename: 'ReferentialFilterVO'})
export class ReferentialRefFilter
    extends BaseReferentialFilter<ReferentialRefFilter, ReferentialRef> {

  static fromObject: (source, opts?: any) => ReferentialRefFilter;

  searchAttributes: string[] = null;

  asObject(opts?: EntityAsObjectOptions): any {
    const target = super.asObject(opts);
    if (opts && opts.minify) {
      target.searchAttribute = target.searchAttribute ||
        (isNotEmptyArray(this.searchAttributes) ? this.searchAttributes[0] : undefined);
      delete target.searchAttributes;
    }
    return target;
  }

  protected buildFilter(): FilterFn<ReferentialRef>[] {
    const filterFns = super.buildFilter();

    // Search on many attributes
    const searchTextFilter = EntityUtils.searchTextFilter(this.searchAttributes, this.searchText);
    if (searchTextFilter) filterFns.push(searchTextFilter);

    return filterFns;
  }
}
