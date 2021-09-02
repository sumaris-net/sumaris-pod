import {RootDataEntityFilter} from '../../../data/services/model/root-data-filter.model';
import {EntityAsObjectOptions, EntityClass, FilterFn, fromDateISOString, isNotNil, ReferentialRef, ReferentialUtils, toDateISOString} from '@sumaris-net/ngx-components';
import {Moment} from 'moment';
import {Trip} from '../model/trip.model';
import {VesselSnapshot} from '../../../referential/services/model/vessel-snapshot.model';
import {NOT_MINIFY_OPTIONS} from '@app/core/services/model/referential.model';


@EntityClass({typename: 'TripFilterVO'})
export class TripFilter extends RootDataEntityFilter<TripFilter, Trip> {

  static fromObject: (source: any, opts?: any) => TripFilter;

  vesselSnapshot: VesselSnapshot = null;
  vesselId: number = null;
  location: ReferentialRef = null;
  startDate: Moment = null;
  endDate: Moment = null;
  includedIds: number[];

  fromObject(source: any, opts?: any) {
    super.fromObject(source, opts);
    this.vesselId = source.vesselId;
    this.vesselSnapshot = source.vesselSnapshot && VesselSnapshot.fromObject(source.vesselSnapshot);
    this.startDate = fromDateISOString(source.startDate);
    this.endDate = fromDateISOString(source.startDate);
    this.location = ReferentialRef.fromObject(source.location);
    this.includedIds = source.includedIds;
  }

  asObject(opts?: EntityAsObjectOptions): any {
    const target = super.asObject(opts);
    target.startDate = toDateISOString(this.startDate);
    target.endDate = toDateISOString(this.endDate);
    target.includedIds = this.includedIds;

    if (opts && opts.minify) {
      // Vessel
      target.vesselId = isNotNil(this.vesselId) ? this.vesselId : (this.vesselSnapshot && isNotNil(this.vesselSnapshot.id) ? this.vesselSnapshot.id : undefined);
      delete target.vesselSnapshot;

      // Location
      target.locationId = this.location && this.location.id || undefined;
      delete target.location;
    }
    else {
      target.vesselSnapshot = this.vesselSnapshot && this.vesselSnapshot.asObject({...opts, ...NOT_MINIFY_OPTIONS});
      target.location = this.location && this.location.asObject({...opts, ...NOT_MINIFY_OPTIONS});
    }
    return target;
  }

  buildFilter(): FilterFn<Trip>[] {
    const filterFns = super.buildFilter();

    // Vessel
    if (this.vesselId) {
      filterFns.push(t => (t.vesselSnapshot && t.vesselSnapshot.id === this.vesselId));
    }

    // Location
    if (ReferentialUtils.isNotEmpty(this.location)) {
      const locationId = this.location.id;
      filterFns.push(t => (
        (t.departureLocation && t.departureLocation.id === locationId)
        || (t.returnLocation && t.returnLocation.id === locationId))
      );
    }

    // Start/end period
    if (this.startDate) {
      const startDate = this.startDate.clone();
      filterFns.push(t => t.returnDateTime ? startDate.isSameOrBefore(t.returnDateTime) : startDate.isSameOrBefore(t.departureDateTime));
    }
    if (this.endDate) {
      const endDate = this.endDate.clone().add(1, 'day').startOf('day');
      filterFns.push(t => t.departureDateTime && endDate.isAfter(t.departureDateTime));
    }

    return filterFns;
  }
}
