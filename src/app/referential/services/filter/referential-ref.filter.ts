import {EntityAsObjectOptions, EntityUtils}  from "@sumaris-net/ngx-components";
import {BaseReferentialFilter} from "./referential.filter";
import {ReferentialRef}  from "@sumaris-net/ngx-components";
import {FilterFn} from "@sumaris-net/ngx-components";
import {isNotEmptyArray} from "@sumaris-net/ngx-components";
import {EntityClass}  from "@sumaris-net/ngx-components";

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

  fromObject(source: any, opts?: any) {
    super.fromObject(source, opts);

    this.searchAttributes = source.searchAttributes;
  }

  protected buildFilter(): FilterFn<ReferentialRef>[] {
    const filterFns = super.buildFilter();

    // Search on many attributes
    const searchTextFilter = EntityUtils.searchTextFilter(this.searchAttributes || this.searchAttribute || ['label', 'name'], this.searchText);
    if (searchTextFilter) filterFns.push(searchTextFilter);

    return filterFns;
  }
}
