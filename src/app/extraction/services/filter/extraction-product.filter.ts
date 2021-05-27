import {EntityClass} from "../../../core/services/model/entity.decorators";
import {BaseReferentialFilter} from "../../../referential/services/filter/referential.filter";
import {ExtractionProduct} from "../model/extraction-product.model";
import {FilterFn} from "../../../shared/services/entity-service.class";
import {isNotEmptyArray, isNotNil} from "../../../shared/functions";
import {EntityAsObjectOptions} from "../../../core/services/model/entity.model";

@EntityClass()
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
