import { EntityAsObjectOptions, EntityClass, EntityFilter, FilterFn, fromDateISOString, IEntityFilter, isNotNil } from '@sumaris-net/ngx-components';
import { Moment } from 'moment';
import { WeightLengthConversionRef } from '@app/referential/weight-length-conversion/weight-length-conversion.model';
import { isNonEmptyArray } from '@apollo/client/utilities';
import { StoreObject } from '@apollo/client/core';

@EntityClass({typename: 'WeightLengthConversionFilterVO'})
export class WeightLengthConversionFilter
  extends EntityFilter<WeightLengthConversionFilter, WeightLengthConversionRef, number, EntityAsObjectOptions>
  implements IEntityFilter<WeightLengthConversionFilter, WeightLengthConversionRef> {

  static fromObject: (source: any, opts?: any) => WeightLengthConversionFilter;

  date: Moment = null;
  statusIds: number[];

  referenceTaxonId: number = null;
  referenceTaxonIds: number[];

  locationId: number = null;
  locationIds: number[];

  sexId: number = null;
  sexIds: number[];

  lengthParameterId: number = null;
  lengthParameterIds: number[];
  lengthUnitId: number = null;
  lengthUnitIds: number[];


  fromObject(source: any, opts?: any) {
    super.fromObject(source, opts);

    this.date = fromDateISOString(source.date);
    this.statusIds = source.statusIds;
    this.referenceTaxonId = source.referenceTaxonId;
    this.referenceTaxonIds = source.referenceTaxonIds;
    this.locationId = source.locationId;
    this.locationIds = source.locationIds;
    this.sexId = source.sexId;
    this.sexIds = source.sexIds;
    this.lengthParameterId = source.lengthParameterId;
    this.lengthParameterIds = source.lengthParameterIds;
    this.lengthUnitId = source.lengthUnitId;
    this.lengthUnitIds = source.lengthUnitIds;
  }

  asObject(opts?: EntityAsObjectOptions): StoreObject {
    const target = super.asObject(opts);
    if (opts && opts.minify) {
      target.referenceTaxonIds = isNotNil(this.referenceTaxonId) ? [this.referenceTaxonId] : this.referenceTaxonIds;
      delete target.referenceTaxonId;
      target.locationIds = isNotNil(this.locationId) ? [this.locationId] : this.locationIds;
      delete target.locationId;
      target.sexIds = isNotNil(this.sexId) ? [this.sexId] : this.sexIds;
      delete target.sexId;
      target.lengthParameterIds = isNotNil(this.lengthParameterId) ? [this.lengthParameterId] : this.lengthParameterIds;
      delete target.lengthParameterId;
      target.lengthUnitIds = isNotNil(this.lengthUnitId) ? [this.lengthUnitId] : this.lengthUnitIds;
      delete target.lengthUnitId;

    } else {
      target.referenceTaxonId = this.referenceTaxonId;
      target.referenceTaxonIds = this.referenceTaxonIds;
      target.locationId = this.locationId;
      target.locationIds = this.locationIds;
      target.sexId = this.sexId;
      target.sexIds = this.sexIds;
      target.lengthParameterId = this.lengthParameterId;
      target.lengthParameterIds = this.lengthParameterIds;
      target.lengthUnitId = this.lengthUnitId;
      target.lengthUnitIds = this.lengthUnitIds;
    }
    return target;
  }



  public buildFilter(): FilterFn<WeightLengthConversionRef>[] {
    const filterFns = super.buildFilter();

    // Sex
    const sexId = this.sexId;
    if (isNotNil(sexId)) {
      filterFns.push(t => t.id === sexId);
    }

    // Status
    const statusIds = this.statusIds;
    if (isNonEmptyArray(statusIds)) {
      filterFns.push(t => statusIds.includes(t.statusId));
    }

    // Location
    const locationId = this.locationId;
    if (isNotNil(locationId)) {
      filterFns.push(t => (t.locationId === locationId));
    }

    // Reference Taxon
    const referenceTaxonId = this.referenceTaxonId;
    if (isNotNil(referenceTaxonId)) {
      filterFns.push(t => (t.referenceTaxonId === referenceTaxonId));
    }

    // Date
    const month = this.date?.get('month');
    if (isNotNil(month)) {
      console.warn('TODO check date month = ' + month);
      filterFns.push(t => (t.startMonth <= month) && (month <= t.endMonth));
    }

    return filterFns;
  }
}
