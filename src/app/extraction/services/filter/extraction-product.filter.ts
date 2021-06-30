import {EntityClass}  from "@sumaris-net/ngx-components";
import {BaseReferentialFilter} from "../../../referential/services/filter/referential.filter";
import {ExtractionProduct} from "../model/extraction-product.model";
import {FilterFn} from "@sumaris-net/ngx-components";
import {isNotEmptyArray, isNotNil} from "@sumaris-net/ngx-components";
import {EntityAsObjectOptions}  from "@sumaris-net/ngx-components";

@EntityClass({typename: 'ExtractionProductFilterVO'})
export class ExtractionProductFilter extends BaseReferentialFilter<ExtractionProductFilter, ExtractionProduct> {

    static fromObject: (source: any, opts?: any) => ExtractionProductFilter;

    isSpatial: boolean = null;

    fromObject(source: any, opts?: EntityAsObjectOptions) {
      super.fromObject(source, opts);
      this.isSpatial = source.isSpatial;
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
        return filterFns;
    }
}
