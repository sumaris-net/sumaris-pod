import {EntityUtils, fromDateISOString, isNotNil, toDateISOString} from "../../../core/core.module";
import {ReferentialRef} from "../../../referential/referential.module";
import {Moment} from "moment/moment";
import {DataEntity, DataRootEntity} from "./base.model";
import {MeasurementUtils} from "./measurement.model";


export class Sample extends DataRootEntity<Sample> {

  static fromObject(source: any): Sample {
    const res = new Sample();
    res.fromObject(source);
    return res;
  }

  label: string;
  rankOrder: number;
  sampleDate: Moment;
  individualCount: number;
  taxonGroup: ReferentialRef;
  taxonName: ReferentialRef;
  measurementValues: { [key: string]: any };
  matrixId: number;
  batchId: number;

  operationId: number;
  parentId: number;
  parent: Sample;
  children: Sample[];

  constructor() {
    super();
    this.label = null;
    this.rankOrder = null;
    this.taxonGroup = null;
    this.measurementValues = {};
    this.children = [];
    this.individualCount = null;
  }

  clone(): Sample {
    const target = new Sample();
    target.fromObject(this.asObject());
    return target;
  }

  asObject(minify?: boolean): any {
    const target = super.asObject(minify);
    target.sampleDate = toDateISOString(this.sampleDate);
    target.taxonGroup = this.taxonGroup && this.taxonGroup.asObject(false/*fix #32*/) || undefined;
    target.taxonName = this.taxonName && this.taxonName.asObject(false/*fix #32*/) || undefined;
    target.individualCount = isNotNil(this.individualCount) ? this.individualCount : null;
    target.parentId = this.parentId || this.parent && this.parent.id || undefined;
    delete target.parent; // Do not keep parent object
    target.children = this.children && this.children.map(c => c.asObject(minify)) || undefined;
    target.measurementValues = MeasurementUtils.measurementValuesAsObjectMap( this.measurementValues, minify);
    return target;
  }

  fromObject(source: any): Sample {
    super.fromObject(source);
    this.label = source.label;
    this.rankOrder = source.rankOrder;
    this.sampleDate = fromDateISOString(source.sampleDate);
    this.individualCount = isNotNil(source.individualCount) && source.individualCount !== "" ? source.individualCount : null;
    this.taxonGroup = source.taxonGroup && ReferentialRef.fromObject(source.taxonGroup) || undefined;
    this.taxonName = source.taxonName && ReferentialRef.fromObject(source.taxonName) || undefined;
    this.matrixId = source.matrixId;
    this.parentId = source.parentId;
    this.parent = source.parent;
    this.batchId = source.batchId;
    this.operationId = source.operationId;

    if (source.measurementValues) {
      this.measurementValues = source.measurementValues;
    }
    // Convert measurement to map
    else if (source.measurements) {
      this.measurementValues = MeasurementUtils.measurementsValuesFromObjectArray(source.measurements);
    }

    return this;
  }

  equals(other: Sample): boolean {
    return super.equals(other)
      || (this.rankOrder === other.rankOrder
        // same operation
        && ((!this.operationId && !other.operationId) || this.operationId === other.operationId)
        // same label
        && ((!this.label && !other.label) || this.label === other.label)
        // Warn: compare using the parent ID is too complicated
      );
  }
}
