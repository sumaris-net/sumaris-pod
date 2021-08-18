import { MeasurementFormValues, MeasurementModelValues, MeasurementUtils, MeasurementValuesUtils } from './measurement.model';
import { Moment } from 'moment';
import { IWithVesselSnapshotEntity, VesselSnapshot } from '@app/referential/services/model/vessel-snapshot.model';
import { Entity, EntityAsObjectOptions, EntityClass, fromDateISOString, isEmptyArray, isNotNil, ReferentialRef, toDateISOString } from '@sumaris-net/ngx-components';
import { NOT_MINIFY_OPTIONS } from '@app/core/services/model/referential.model';

@EntityClass({ typename: 'VesselActivityVO' })
export class VesselActivity extends Entity<VesselActivity> {
  static fromObject: (source: any, opts?: any) => VesselActivity;

  date: Moment;
  rankOrder: number;
  comments: string;
  measurementValues: MeasurementModelValues | MeasurementFormValues;
  metiers: ReferentialRef[];
  observedLocationId: number;
  landingId: number;
  tripId: number;

  constructor() {
    super(VesselActivity.TYPENAME);
    this.date = null;
    this.rankOrder = null;
    this.comments = null;
    this.measurementValues = {};
    this.metiers = [];
    this.observedLocationId = null;
    this.landingId = null;
    this.tripId = null;
  }

  asObject(opts?: EntityAsObjectOptions): any {
    const target = super.asObject(opts);
    target.date = toDateISOString(this.date);
    target.measurementValues = MeasurementValuesUtils.asObject(this.measurementValues, opts);
    target.metiers = (this.metiers && this.metiers.filter(isNotNil).map((p) => p && p.asObject({ ...opts, ...NOT_MINIFY_OPTIONS }))) || undefined;
    return target;
  }

  fromObject(source: any) {
    super.fromObject(source);
    this.date = fromDateISOString(source.date);
    this.rankOrder = source.rankOrder;
    this.comments = source.comments;
    this.measurementValues =
      (source.measurementValues && { ...source.measurementValues }) || MeasurementUtils.toMeasurementValues(source.measurements);
    this.metiers = (source.metiers && source.metiers.map(ReferentialRef.fromObject)) || [];
    this.observedLocationId = source.observedLocationId;
    this.landingId = source.landingId;
    this.tripId = source.tripId;
  }

  static isEmpty(value: VesselActivity) {
    return !value || (MeasurementValuesUtils.isEmpty(value.measurementValues) && isEmptyArray(value.metiers));
  }
}

@EntityClass({ typename: 'AggregatedLandingVO' })
export class AggregatedLanding extends Entity<AggregatedLanding> implements IWithVesselSnapshotEntity<AggregatedLanding> {
  static fromObject: (source: any, opts?: any) => AggregatedLanding;

  vesselSnapshot: VesselSnapshot;
  vesselActivities: VesselActivity[];

  // parent (for entity cache use only)
  observedLocationId: number;

  constructor() {
    super(AggregatedLanding.TYPENAME);
    this.vesselSnapshot = null;
    this.vesselActivities = [];
  }

  asObject(opts?: EntityAsObjectOptions): any {
    const target = super.asObject(opts);
    target.vesselSnapshot = (this.vesselSnapshot && this.vesselSnapshot.asObject({ ...opts, ...NOT_MINIFY_OPTIONS })) || undefined;
    target.vesselActivities = this.vesselActivities && this.vesselActivities.map((value) => value.asObject(opts));
    return target;
  }

  fromObject(source: any) {
    super.fromObject(source);
    this.vesselSnapshot = source.vesselSnapshot && VesselSnapshot.fromObject(source.vesselSnapshot);
    this.id = this.vesselSnapshot.id;
    this.vesselActivities = (source.vesselActivities && source.vesselActivities.map(VesselActivity.fromObject)) || [];
  }
}
