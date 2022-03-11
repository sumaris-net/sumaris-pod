import { Moment } from 'moment';
import { LandingFilter } from './landing.filter';
import { RootDataEntityFilter } from '../../../data/services/model/root-data-filter.model';
import { ObservedLocation } from '../model/observed-location.model';
import { EntityAsObjectOptions, EntityClass, FilterFn, isNotEmptyArray, isNotNil, Person, ReferentialRef, ReferentialUtils } from '@sumaris-net/ngx-components';
import DurationConstructor = moment.unitOfTime.DurationConstructor;

@EntityClass({typename: 'ObservedLocationFilterVO'})
export class ObservedLocationFilter extends RootDataEntityFilter<ObservedLocationFilter, ObservedLocation> {

  static fromObject: (source: any, opts?: any) => ObservedLocationFilter;

  location?: ReferentialRef;
  observers?: Person[];

  fromObject(source: any, opts?: any) {
    super.fromObject(source, opts);
    this.location = ReferentialRef.fromObject(source.location);
    this.observers = source.observers && source.observers.map(Person.fromObject) || [];
  }

  asObject(opts?: EntityAsObjectOptions): any {
    const target = super.asObject(opts);
    if (opts && opts.minify) {
      // Location
      target.locationId = this.location && this.location.id || undefined;
      delete target.location;

      // Observers
      target.observerPersonIds = this.observers && this.observers.map(o => o && o.id).filter(isNotNil) || undefined;
      delete target.observers;
    } else {
      target.location = this.location && this.location.asObject(opts) || undefined;
      target.observers = this.observers && this.observers.map(o => o && o.asObject(opts)).filter(isNotNil) || undefined;
    }
    return target;
  }

  buildFilter(): FilterFn<ObservedLocation>[] {
    const filterFns = super.buildFilter();

    // Location
    if (ReferentialUtils.isNotEmpty(this.location)) {
      const locationId = this.location.id;
      filterFns.push(t => (t.location && t.location.id === locationId));
    }

    // Start/end period
    if (this.startDate) {
      const startDate = this.startDate.clone();
      filterFns.push(t => t.endDateTime ? startDate.isSameOrBefore(t.endDateTime)
        : startDate.isSameOrBefore(t.startDateTime));
    }
    if (this.endDate) {
      const endDate = this.endDate.clone().add(1, 'day').startOf('day');
      filterFns.push(t => t.startDateTime && endDate.isAfter(t.startDateTime));
    }

    // Recorder department and person
    // Already defined in super classes root-data-filter.model.ts et data-filter.model.ts

    // Observers
    const observerIds = this.observers && this.observers.map(o => o && o.id).filter(isNotNil);
    if (isNotEmptyArray(observerIds)) {
      filterFns.push(t => t.observers && t.observers.findIndex(o => o && observerIds.includes(o.id)) != -1);
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
      program: {label: f.programLabel},
      startDate: f.startDate,
      endDate: f.endDate,
      locationIds: f.locationIds
    });
  }
}
