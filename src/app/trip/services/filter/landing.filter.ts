import {RootDataEntityFilter} from "../../../data/services/model/root-data-filter.model";
import {Landing} from "../model/landing.model";
import {FilterFn} from "../../../shared/services/entity-service.class";
import {Moment} from "moment";
import {isNotEmptyArray, isNotNil} from "../../../shared/functions";
import {fromDateISOString, toDateISOString} from "../../../shared/dates";
import {EntityAsObjectOptions} from "../../../core/services/model/entity.model";
import {EntityClass} from "../../../core/services/model/entity.decorators";

@EntityClass()
export class LandingFilter extends RootDataEntityFilter<LandingFilter, Landing> {

  static fromObject: (source: any, opts?: any) => LandingFilter;

  vesselId?: number;
  locationId?: number;
  locationIds?: number[];
  startDate?: Moment;
  endDate?: Moment;
  groupByVessel?: boolean;
  excludeVesselIds?: number[];

  // Linked entities
  observedLocationId?: number;
  tripId?: number;

  fromObject(source: any, opts?: any) {
    super.fromObject(source, opts);
    this.vesselId = source.vesselId;
    this.locationId = source.locationId;
    this.locationIds = source.locationIds;
    this.startDate = fromDateISOString(source.startDate);
    this.endDate = fromDateISOString(source.startDate);
    this.groupByVessel = source.groupByVessel;
    this.excludeVesselIds = source.excludeVesselIds;
    this.observedLocationId = source.observedLocationId;
    this.tripId = source.tripId;
  }

  asObject(opts?: EntityAsObjectOptions): any {
    const target = super.asObject(opts);
    target.startDate = toDateISOString(this.startDate);
    target.endDate = toDateISOString(this.endDate);
    if (opts && opts.minify) {
      delete target.groupByVessel;
    }
    return target;
  }

  buildFilter(): FilterFn<Landing>[] {
    const filterFns = super.buildFilter();

    // observedLocationId
    if (isNotNil(this.observedLocationId)) {
      filterFns.push((entity) => entity.observedLocationId === this.observedLocationId);
    }

    // tripId
    if (isNotNil(this.tripId)) {
      filterFns.push((entity) => entity.tripId === this.tripId);
    }

    // Vessel
    if (isNotNil(this.vesselId)) {
      filterFns.push((entity) => entity.vesselSnapshot && entity.vesselSnapshot.id === this.vesselId);
    }

    // Vessel exclude
    if (isNotEmptyArray(this.excludeVesselIds)) {
      filterFns.push((entity) => entity.vesselSnapshot && !this.excludeVesselIds.includes(entity.vesselSnapshot.id));
    }

    // Location
    if (isNotNil(this.locationId)) {
      filterFns.push((entity) => entity.location && entity.location.id === this.locationId);
    }
    if (isNotEmptyArray(this.locationIds)) {
      filterFns.push((entity) => entity.location && this.locationIds.includes(entity.location.id));
    }

    // Start/end period
    if (this.startDate) {
      const startDate = this.startDate.clone();
      filterFns.push(t => t.dateTime && startDate.isSameOrBefore(t.dateTime));
    }
    if (this.endDate) {
      const endDate = this.endDate.clone().add(1, 'day').startOf('day');
      filterFns.push(t => t.dateTime && endDate.isAfter(t.dateTime));
    }
    return filterFns;
  }
}
