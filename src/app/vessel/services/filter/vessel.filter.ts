import {
  EntityAsObjectOptions,
  EntityClass,
  EntityFilter,
  EntityUtils,
  FilterFn,
  fromDateISOString,
  isNotEmptyArray,
  isNotNil,
  toDateISOString,
} from '@sumaris-net/ngx-components';
import { Vessel, VesselFeatures, VesselRegistration } from '../model/vessel.model';
import { RootDataEntityFilter } from '@app/data/services/model/root-data-filter.model';
import { Moment } from 'moment';

@EntityClass({ typename: 'VesselFilterVO' })
export class VesselFilter extends RootDataEntityFilter<VesselFilter, Vessel> {
  static fromObject: (source: any, opts?: any) => VesselFilter;

  searchText: string = null;
  date: Moment = null;
  vesselId: number = null;
  statusId: number = null;
  statusIds: number[] = null;

  fromObject(source: any, opts?: any) {
    super.fromObject(source, opts);
    this.searchText = source.searchText;
    this.date = fromDateISOString(source.date);
    this.statusId = source.statusId;
    this.statusIds = source.statusIds;
    this.vesselId = source.vesselId;
  }

  asObject(opts?: EntityAsObjectOptions): any {
    const target = super.asObject(opts);
    target.date = toDateISOString(this.date);
    if (opts && opts.minify) {
      target.statusIds = isNotNil(this.statusId) ? [this.statusId] : this.statusIds;
      delete target.statusId;
    }
    return target;
  }

  buildFilter(): FilterFn<Vessel>[] {
    const filterFns = super.buildFilter();

    // Vessel id
    if (isNotNil(this.vesselId)) {
      filterFns.push((t) => t.id === this.vesselId);
    }

    // Status
    const statusIds = isNotNil(this.statusId) ? [this.statusId] : this.statusIds;
    if (isNotEmptyArray(statusIds)) {
      filterFns.push((t) => statusIds.includes(t.statusId));
    }

    const searchTextFilter = EntityUtils.searchTextFilter(
      ['features.name', 'features.exteriorMarking', 'registration.registrationCode'],
      this.searchText
    );
    if (searchTextFilter) filterFns.push(searchTextFilter);

    return filterFns;
  }
}

@EntityClass({ typename: 'VesselFeaturesFilterVO' })
export class VesselFeaturesFilter extends EntityFilter<VesselFeaturesFilter, VesselFeatures> {
  static fromObject: (source: any, opts?: any) => VesselFeaturesFilter;

  vesselId: number;

  fromObject(source: any, opts?: any) {
    super.fromObject(source, opts);
    this.vesselId = source.vesselId;
  }

  protected buildFilter(): FilterFn<VesselFeatures>[] {
    const filterFns = super.buildFilter();
    if (isNotNil(this.vesselId)) {
      filterFns.push((e) => e.vesselId === this.vesselId);
    }

    return filterFns;
  }
}

@EntityClass({ typename: 'VesselRegistrationFilterVO' })
export class VesselRegistrationFilter extends EntityFilter<VesselRegistrationFilter, VesselRegistration> {
  static fromObject: (source: any, opts?: any) => VesselRegistrationFilter;

  vesselId: number;

  fromObject(source: any, opts?: any) {
    super.fromObject(source, opts);
    this.vesselId = source.vesselId;
  }

  asObject(opts?: EntityAsObjectOptions): any {
    return {
      vesselId: this.vesselId,
    };
  }

  protected buildFilter(): FilterFn<VesselRegistration>[] {
    const filterFns = super.buildFilter();

    if (isNotNil(this.vesselId)) {
      const vesselId = this.vesselId;
      filterFns.push((e) => e.vesselId === vesselId);
    }

    return filterFns;
  }
}
