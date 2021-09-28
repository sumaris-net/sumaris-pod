import {EntityClass, FilterFn, fromDateISOString, isNil, isNotNil} from '@sumaris-net/ngx-components';
import {DataEntityFilter} from '@app/data/services/model/data-filter.model';
import {Operation} from '@app/trip/services/model/trip.model';
import {DataEntityAsObjectOptions} from '@app/data/services/model/data-entity.model';
import {Moment} from 'moment';

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
  startDate?: Date | Moment;
  endDate?: Date | Moment;
  gearIds?: number[];
  taxonGroupLabels?: string[];
  qualityFlagId?: number

  static fromObject: (source: any, opts?: any) => OperationFilter;

  fromObject(source: any, opts?: any) {
    super.fromObject(source, opts);
    this.tripId = source.tripId;
    this.vesselId = source.vesselId;
    this.excludeId = source.excludeId;
    this.includedIds = source.includedIds;
    this.excludedIds = source.excludedIds;
    this.programLabel = source.programLabel;
    this.excludeChildOperation = source.excludeChildOperation;
    this.hasNoChildOperation = source.hasNoChildOperation;
    this.startDate = source.startDate;
    this.endDate = source.endDate;
    this.gearIds = source.gearIds;
    this.taxonGroupLabels = source.taxonGroupLabels;
    this.qualityFlagId = source.qualityFlagId;

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

    const includedIds = this.includedIds;
    // Exclude id
    if (isNotNil(this.excludeId)) {
      const excludeId = this.excludeId;
      filterFns.push(o => o.id !== excludeId
        || (isNotNil(includedIds) && includedIds.indexOf(o.id) !== -1));
    }

    // Trip
    if (isNotNil(this.tripId)) {
      const tripId = this.tripId;
      filterFns.push((o => ((isNotNil(o.tripId) && o.tripId === tripId)
        || (o.trip && o.trip.id === tripId)) || (isNotNil(includedIds) && includedIds.indexOf(o.id) !== -1)));
    }

    // Vessel
    if (isNotNil(this.vesselId)) {
      const vesselId = this.vesselId;
      filterFns.push((o => ((isNotNil(o.trip) && isNotNil(o.trip.vesselSnapshot) && o.trip.vesselSnapshot.id === vesselId)
        || (isNotNil(includedIds) && includedIds.indexOf(o.id) !== -1))));
    }

    // ExcludedIds
    if (isNotNil(this.excludedIds)) {
      const excludedIds = this.excludedIds;
      filterFns.push((o => (excludedIds.indexOf(o.id) === -1)
        || (isNotNil(includedIds) && includedIds.indexOf(o.id) !== -1)));
    }

    // Program label
    if (isNotNil(this.programLabel)) {
      const programLabel = this.programLabel;
      filterFns.push(o => (isNotNil(o.trip) && (isNotNil(o.trip.program) && (o.trip.program.label === programLabel)))
        || (isNotNil(includedIds) && includedIds.indexOf(o.id) !== -1));
    }

    // Only operation with no parents
    if (isNotNil(this.excludeChildOperation) && this.excludeChildOperation) {
      filterFns.push((o => (isNil(o.parentOperationId) && isNil(o.parentOperation))
        || (isNotNil(includedIds) && includedIds.indexOf(o.id) !== -1)));
    }

    // Only operation with no child
    if (isNotNil(this.hasNoChildOperation) && this.hasNoChildOperation) {
      filterFns.push((o => (isNil(o.childOperationId) && isNil(o.childOperation))
        || (isNotNil(includedIds) && includedIds.indexOf(o.id) !== -1)));
    }

    // StartDate
    if (isNotNil(this.startDate)) {
      const startDate = this.startDate;
      filterFns.push((o => ((isNotNil(o.endDateTime) && fromDateISOString(o.endDateTime).isAfter(startDate))
        || (isNotNil(o.fishingStartDateTime) && fromDateISOString(o.fishingStartDateTime).isAfter(startDate)))
        || (isNotNil(includedIds) && includedIds.indexOf(o.id) !== -1)));
    }

    // EndDate
    if (isNotNil(this.endDate)) {
      const endDate = this.endDate;
      filterFns.push((o => ((isNotNil(o.endDateTime) && fromDateISOString(o.endDateTime).isBefore(endDate))
        || (isNotNil(o.fishingStartDateTime) && fromDateISOString(o.fishingStartDateTime).isBefore(endDate)))
        || (isNotNil(includedIds) && includedIds.indexOf(o.id) !== -1)));
    }

    // GearIds;
    if (isNotNil(this.gearIds)) {
      const gearIds = this.gearIds;
      filterFns.push((o => (isNotNil(o.physicalGear) && isNotNil(o.physicalGear.gear) && gearIds.indexOf(o.physicalGear.gear.id) !== -1)
        || (isNotNil(includedIds) && includedIds.indexOf(o.id) !== -1)));
    }

    // taxonGroupIds
    if (isNotNil(this.taxonGroupLabels)) {
      const targetSpecieLabels = this.taxonGroupLabels;
      filterFns.push((o => (isNotNil(o.metier) && isNotNil(o.metier.taxonGroup) && targetSpecieLabels.indexOf(o.metier.taxonGroup.label) !== -1)
        || (isNotNil(includedIds) && includedIds.indexOf(o.id) !== -1)));
    }

    if (isNotNil(this.qualityFlagId)){
      const qualityFlagId = this.qualityFlagId;
      filterFns.push((o => (isNotNil(o.qualityFlagId) && o.qualityFlagId === qualityFlagId)
        || (isNotNil(includedIds) && includedIds.indexOf(o.id) !== -1)));
    }
    return filterFns;
  }
}
