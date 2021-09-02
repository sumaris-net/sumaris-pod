import {EntityClass, FilterFn, isNotNil} from "@sumaris-net/ngx-components";
import {DataEntityFilter} from "@app/data/services/model/data-filter.model";
import {Operation} from "@app/trip/services/model/trip.model";
import {DataEntityAsObjectOptions} from '@app/data/services/model/data-entity.model';

@EntityClass({typename: 'OperationFilterVO'})
export class OperationFilter extends DataEntityFilter<OperationFilter, Operation> {

    tripId?: number;
    vesselId?: number;
    excludeId?: number;
    includedIds?: number[];
    excludedIds?: number[];
    programLabel?: string;
    excludeChildOperation?: boolean;
    hasNoChildOperation?: boolean;
    startDate?: Date;
    endDate?: Date;

    static fromObject: (source: any, opts?: any) => OperationFilter;

    fromObject(source: any, opts?: any) {
        super.fromObject(source, opts);
        this.tripId = +source.tripId;
        this.vesselId = +source.vesselId;
        this.excludeId = +source.excludeId;
        this.includedIds = source.includedIds;
        this.excludedIds = source.excludedIds;
        this.programLabel = source.programLabel;
        this.excludeChildOperation = source.excludeChildOperation;
        this.hasNoChildOperation = source.hasNoChildOperation;
        this.startDate = source.startDate;
        this.endDate = source.endDate;
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
