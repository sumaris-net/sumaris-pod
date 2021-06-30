import {EntityClass, FilterFn, isNotNil} from "@sumaris-net/ngx-components";
import {DataEntityFilter} from "@app/data/services/model/data-filter.model";
import {Operation} from "@app/trip/services/model/trip.model";
import {DataEntityAsObjectOptions} from '@app/data/services/model/data-entity.model';

@EntityClass({typename: 'OperationFilterVO'})
export class OperationFilter extends DataEntityFilter<OperationFilter, Operation> {

    tripId?: number;
    excludeId?: number;

    static fromObject: (source: any, opts?: any) => OperationFilter;

    fromObject(source: any, opts?: any) {
        super.fromObject(source, opts);
        this.tripId = +source.tripId;
        this.excludeId = +source.excludeId;
    }

    asObject(opts?: DataEntityAsObjectOptions): any {
      const target = super.asObject(opts);
      if (opts && opts.minify) {
        delete target.excludeId; // Not include in Pod
      }
      return target;
    }

    buildFilter(): FilterFn<Operation>[] {
        const filterFns = super.buildFilter();

        // Exclude id
        if (isNotNil(this.excludeId)) {
            const excludeId = this.excludeId;
            filterFns.push(o => o.id !== excludeId);
        }

        // Trip
        if (isNotNil(this.tripId)) {
            const tripId = this.tripId;
            filterFns.push((o => (isNotNil(o.tripId) && o.tripId === tripId)
                || (o.trip && o.trip.id === tripId)));
        }

        return filterFns;
    }

}
