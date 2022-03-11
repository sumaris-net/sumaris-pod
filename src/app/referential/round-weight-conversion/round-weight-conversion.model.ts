import {
  DateUtils,
  Entity,
  EntityAsObjectOptions,
  EntityClass,
  EntityUtils,
  fromDateISOString,
  IEntity,
  ReferentialRef,
  ReferentialUtils,
  toDateISOString,
  toFloat, toInt
} from '@sumaris-net/ngx-components';
import { Moment } from 'moment';
import { StoreObject } from '@apollo/client/core';
import { NOT_MINIFY_OPTIONS } from '@app/core/services/model/referential.utils';

export abstract class BaseRoundWeightConversion<T extends Entity<T, number, EntityAsObjectOptions>>
  extends Entity<T, number, EntityAsObjectOptions>
  implements IEntity<T> {

  startDate: Moment = null;
  endDate: Moment = null;
  conversionCoefficient: number = null;

  taxonGroupId: number = null;

  statusId: number = null;
  description: string = null;
  comments: string = null;
  creationDate: Moment = null;

  protected constructor(__typename: string) {
    super(__typename);
  }

  fromObject(source: any, opts?: any) {
    super.fromObject(source, opts);

    this.startDate = fromDateISOString(source.startDate);
    this.endDate = fromDateISOString(source.endDate);
    this.conversionCoefficient = toFloat(source.conversionCoefficient);
    this.taxonGroupId = toInt(source.taxonGroupId);
    this.description = source.description;
    this.comments = source.comments;

    this.statusId = source.statusId;
    this.creationDate = fromDateISOString(source.creationDate);
  }

  asObject(opts?: EntityAsObjectOptions): StoreObject {
    const target = super.asObject(opts);
    target.creationDate = toDateISOString(this.creationDate);
    return target;
  }
}


@EntityClass({typename: 'WeightLengthConversionVO'})
export class RoundWeightConversionRef
  extends BaseRoundWeightConversion<RoundWeightConversionRef> {

  static fromObject: (source: any, opts?: any) => RoundWeightConversionRef;

  locationId: number = null;
  dressingId: number = null;
  preservingId: number = null;

  constructor() {
    super(RoundWeightConversionRef.TYPENAME);
  }

  fromObject(source: any, opts?: any) {
    super.fromObject(source, opts);

    this.locationId = source.locationId;
    this.dressingId = source.dressingId;
    this.preservingId = source.preservingId;
  }
}

@EntityClass({typename: 'RoundWeightConversionVO'})
export class RoundWeightConversion
  extends BaseRoundWeightConversion<RoundWeightConversion> {

  static fromObject: (source: any, opts?: any) => RoundWeightConversion;

  location: ReferentialRef = null;
  dressing: ReferentialRef = null;
  preserving: ReferentialRef = null;

  constructor() {
    super(RoundWeightConversion.TYPENAME);
  }

  fromObject(source: any, opts?: any) {
    super.fromObject(source, opts);

    this.location = source.location && ReferentialRef.fromObject(source.location);
    this.dressing = source.dressing && ReferentialRef.fromObject(source.dressing);
    this.preserving = source.preserving && ReferentialRef.fromObject(source.preserving);
  }

  asObject(opts?: EntityAsObjectOptions): StoreObject {
    const target = super.asObject(opts);

    target.location = this.location?.asObject({...opts, ...NOT_MINIFY_OPTIONS}) || undefined;
    target.dressing = this.dressing?.asObject({...opts, ...NOT_MINIFY_OPTIONS}) || undefined;
    target.preserving = this.preserving?.asObject({...opts, ...NOT_MINIFY_OPTIONS}) || undefined;
    return target;
  }

  equals(other: RoundWeightConversion): boolean {
    console.log('TODO check equals', other);
    return super.equals(other)
      || (this.conversionCoefficient === other.conversionCoefficient
        && DateUtils.isSame(this.startDate, other.startDate)
        && DateUtils.isSame(this.endDate, other.endDate)
        && ReferentialUtils.equals(this.location, other.location)
        && ReferentialUtils.equals(this.dressing, other.dressing)
        && ReferentialUtils.equals(this.preserving, other.preserving)
      );
  }
}
