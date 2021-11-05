import {EntityClass}  from "@sumaris-net/ngx-components";
import {BaseReferentialFilter} from "../../../referential/services/filter/referential.filter";
import {ExtractionProduct} from "../model/extraction-product.model";
import {FilterFn} from "@sumaris-net/ngx-components";
import {isNotEmptyArray, isNotNil} from "@sumaris-net/ngx-components";
import {EntityAsObjectOptions}  from "@sumaris-net/ngx-components";
import { ExtractionCategoryType, ExtractionType } from '@app/extraction/services/model/extraction-type.model';

@EntityClass({typename: 'ExtractionTypeFilterVO'})
export class ExtractionTypeFilter extends BaseReferentialFilter<ExtractionTypeFilter, ExtractionType> {

  static fromObject: (source: any, opts?: any) => ExtractionTypeFilter;

  category: ExtractionCategoryType
  isSpatial: boolean = null;

  fromObject(source: any, opts?: EntityAsObjectOptions) {
    super.fromObject(source, opts);
    this.isSpatial = source.isSpatial;
    this.category = source.category;
  }

  protected buildFilter(): FilterFn<ExtractionProduct>[] {
    const filterFns = super.buildFilter();
    // Filter by status
    if (isNotEmptyArray(this.statusIds)) {
        filterFns.push(entity => this.statusIds.includes(entity.statusId));
    }

    // Filter by spatial
    if (isNotNil(this.isSpatial)) {
        filterFns.push(entity => this.isSpatial === entity.isSpatial);
    }

    // Filter by category
    if (isNotNil(this.category)) {
      filterFns.push(entity => this.category === entity.category);
    }

    return filterFns;
  }
}
