import {RootDataEntityFilter} from "../../../data/services/model/root-data-filter.model";
import {PhysicalGear} from "../model/trip.model";
import {Moment} from "moment";
import {fromDateISOString, toDateISOString} from "../../../shared/dates";
import {EntityAsObjectOptions} from "../../../core/services/model/entity.model";
import {EntityClass} from "../../../core/services/model/entity.decorators";

@EntityClass()
export class PhysicalGearFilter extends RootDataEntityFilter<PhysicalGearFilter, PhysicalGear> {

  static fromObject: (source: any, opts?: any) => PhysicalGearFilter;

  tripId?: number;
  vesselId?: number;
  startDate?: Moment;
  endDate?: Moment;
  excludeTripId?: number;

  fromObject(source: any, opts?: any) {
    super.fromObject(source, opts);
    this.tripId = source.tripId;
    this.vesselId = source.vesselId;
    this.startDate = fromDateISOString(source.startDate);
    this.endDate = fromDateISOString(source.endDate);
    this.excludeTripId = source.excludeTripId;
  }

  asObject(opts?: EntityAsObjectOptions): any {
    const target = super.asObject(opts);
    target.startDate = toDateISOString(this.startDate);
    target.endDate = toDateISOString(this.endDate);

    if (opts && opts.minify) {
      // NOT exists on pod:
      delete target.excludeTripId;
    }

    return target;
  }

}
