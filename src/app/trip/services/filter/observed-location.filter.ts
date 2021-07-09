import {Moment} from "moment";
import {LandingFilter} from "./landing.filter";
import {RootDataEntityFilter} from "../../../data/services/model/root-data-filter.model";
import {ObservedLocation} from "../model/observed-location.model";
import {EntityClass, EntityFilter, isNotEmptyArray, isNotNil, isNotNilOrBlank, Person, ReferentialRef, ReferentialUtils} from '@sumaris-net/ngx-components';
import {fromDateISOString, toDateISOString} from "@sumaris-net/ngx-components";
import {EntityAsObjectOptions}  from "@sumaris-net/ngx-components";
import {FilterFn} from "@sumaris-net/ngx-components";
import DurationConstructor = moment.unitOfTime.DurationConstructor;
import {NOT_MINIFY_OPTIONS} from '@app/core/services/model/referential.model';

@EntityClass({typename: 'ObservedLocationFilterVO'})
export class ObservedLocationFilter extends RootDataEntityFilter<ObservedLocationFilter, ObservedLocation> {

    static fromObject: (source: any, opts?: any) => ObservedLocationFilter

    location?: ReferentialRef;
    startDate?: Moment;
    endDate?: Moment;
    observerPerson?: Person;

    fromObject(source: any, opts?: any) {
        super.fromObject(source, opts);
        this.location = ReferentialRef.fromObject(source.location);
        this.startDate = fromDateISOString(source.startDate);
        this.endDate = fromDateISOString(source.endDate);
        this.observerPerson = Person.fromObject(source.observers)
        || isNotNil(source.observers) && Person.fromObject({id: source.observers.id}) || undefined;
    }

    asObject(opts?: EntityAsObjectOptions): any {
        const target = super.asObject(opts);
        target.startDate = toDateISOString(this.startDate);
        target.endDate = toDateISOString(this.endDate);
        if (opts && opts.minify) {
            target.locationId = this.location && this.location.id || undefined;
            delete target.location;
          target.observerPersonIds = this.observerPerson && this.observerPerson.id || undefined;
          delete target.observerPerson;
        } else {
            target.location = this.location && this.location.asObject(opts) || undefined;
            target.observerPersonIds = this.observerPerson && this.observerPerson.asObject({...opts, ...NOT_MINIFY_OPTIONS}) || undefined;
        }

        return target;
    }

    buildFilter(): FilterFn<ObservedLocation>[] {
        const filterFns = super.buildFilter();

      // Program
      if (isNotNilOrBlank(this.program?.label)) {
        const programLabel = this.program?.label;
        filterFns.push(t => (t.program && t.program.label === programLabel));
      }

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
      if (ReferentialUtils.isNotEmpty(this.observerPerson)) {
        const observerPersonIds = this.observerPerson.id;
        filterFns.push(t => (t.observerPerson && t.observerPerson.id === observerPersonIds));
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
