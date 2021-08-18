import { Moment } from 'moment';
import { LandingFilter } from './landing.filter';
import { RootDataEntityFilter } from '@app/data/services/model/root-data-filter.model';
import { ObservedLocation } from '../model/observed-location.model';
import { EntityAsObjectOptions, EntityClass, FilterFn, ReferentialRef, ReferentialUtils } from '@sumaris-net/ngx-components';
import DurationConstructor = moment.unitOfTime.DurationConstructor;

@EntityClass({ typename: 'ObservedLocationFilterVO' })
export class ObservedLocationFilter extends RootDataEntityFilter<ObservedLocationFilter, ObservedLocation> {
  static fromObject: (source: any, opts?: any) => ObservedLocationFilter;

  location?: ReferentialRef;

  fromObject(source: any, opts?: any) {
    super.fromObject(source, opts);
    this.location = ReferentialRef.fromObject(source.location);
  }

  asObject(opts?: EntityAsObjectOptions): any {
    const target = super.asObject(opts);
    if (opts && opts.minify) {
      target.locationId = (this.location && this.location.id) || undefined;
      delete target.location;
    } else {
      target.location = (this.location && this.location.asObject(opts)) || undefined;
    }
    return target;
  }

  buildFilter(): FilterFn<ObservedLocation>[] {
    const filterFns = super.buildFilter();

    // Location
    if (ReferentialUtils.isNotEmpty(this.location)) {
      const locationId = this.location.id;
      filterFns.push((t) => t.location && t.location.id === locationId);
    }

    // Start/end period
    if (this.startDate) {
      const startDate = this.startDate.clone();
      filterFns.push((t) => (t.endDateTime ? startDate.isSameOrBefore(t.endDateTime) : startDate.isSameOrBefore(t.startDateTime)));
    }
    if (this.endDate) {
      const endDate = this.endDate.clone().add(1, 'day').startOf('day');
      filterFns.push((t) => t.startDateTime && endDate.isAfter(t.startDateTime));
    }

    return filterFns;
  }
}

export class ObservedLocationOfflineFilter {
  programLabel?: string;
  startDate?: Date | Moment;
  endDate?: Date | Moment;
  locationIds?: number[];
  periodDuration?: number;
  periodDurationUnit?: DurationConstructor;

  public static toLandingFilter(f: ObservedLocationOfflineFilter): LandingFilter {
    if (!f) return undefined;
    return LandingFilter.fromObject({
      program: { label: f.programLabel },
      startDate: f.startDate,
      endDate: f.endDate,
      locationIds: f.locationIds,
    });
  }
}
