import {Entity, EntityAsObjectOptions, fromDateISOString, isNotNil, ReferentialRef, toDateISOString} from "../../../core/core.module";
import {Metier} from "../../../referential/services/model/taxon.model";
import {MeasurementFormValues, MeasurementModelValues, MeasurementUtils, MeasurementValuesUtils} from "./measurement.model";
import {Moment} from "moment";
import {IWithVesselSnapshotEntity, VesselSnapshot} from "../../../referential/services/model/vessel-snapshot.model";
import {NOT_MINIFY_OPTIONS} from "../../../core/services/model/referential.model";
import {isEmptyArray} from "../../../shared/functions";

export class VesselActivity extends Entity<VesselActivity> {

  static TYPENAME = 'VesselActivityVO';

  static fromObject(source: any): VesselActivity {
    const target = new VesselActivity();
    target.fromObject(source);
    return target;
  }

  date: Moment;
  rankOrder: number;
  comments: string;
  measurementValues: MeasurementModelValues | MeasurementFormValues;
  metiers: ReferentialRef[];
  observedLocationId: number;
  landingId: number;
  tripId: number;

  constructor() {
    super();
    this.__typename = VesselActivity.TYPENAME;
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
    target.metiers = this.metiers && this.metiers.filter(isNotNil).map(p => p && p.asObject({...opts, ...NOT_MINIFY_OPTIONS})) || undefined;
    return target;
  }

  fromObject(source: any) {
    super.fromObject(source);
    this.date = fromDateISOString(source.date);
    this.rankOrder = source.rankOrder;
    this.comments = source.comments;
    this.measurementValues = source.measurementValues || MeasurementUtils.toMeasurementValues(source.measurements);
    this.metiers = source.metiers && source.metiers.map(ReferentialRef.fromObject) || [];
    this.observedLocationId = source.observedLocationId;
    this.landingId = source.landingId;
    this.tripId = source.tripId;
  }

  clone(): VesselActivity {
    return VesselActivity.fromObject(this.asObject());
  }

  static isEmpty(value: VesselActivity) {
    return !value || (
      MeasurementValuesUtils.isEmpty(value.measurementValues)
      && isEmptyArray(value.metiers)
    );
  }
}

export class AggregatedLanding extends Entity<AggregatedLanding> implements IWithVesselSnapshotEntity<AggregatedLanding> {

  static TYPENAME = 'AggregatedLandingVO';

  static fromObject(source: any): AggregatedLanding {
    const target = new AggregatedLanding();
    target.fromObject(source);
    return target;
  }

  vesselSnapshot: VesselSnapshot;
  vesselActivities: VesselActivity[];

  // parent (for entity cache use only)
  observedLocationId: number;

  constructor() {
    super();
    this.__typename = AggregatedLanding.TYPENAME;
    this.vesselSnapshot = null;
    this.vesselActivities = [];
  }

  asObject(opts?: EntityAsObjectOptions): any {
    const target = super.asObject(opts);
    target.vesselSnapshot = this.vesselSnapshot && this.vesselSnapshot.asObject({...opts, ...NOT_MINIFY_OPTIONS}) || undefined;
    target.vesselActivities = this.vesselActivities && this.vesselActivities.map(value => value.asObject(opts));
    return target;
  }

  fromObject(source: any) {
    super.fromObject(source);
    this.vesselSnapshot = source.vesselSnapshot && VesselSnapshot.fromObject(source.vesselSnapshot);
    this.id = this.vesselSnapshot.id;
    this.vesselActivities = source.vesselActivities && source.vesselActivities.map(VesselActivity.fromObject) || [];
  }

  clone(): AggregatedLanding {
    return AggregatedLanding.fromObject(this.asObject());
  }
}
