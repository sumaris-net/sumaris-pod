import {fromDateISOString, isNil, isNotNil, toDateISOString} from "../../../core/core.module";
import {AcquisitionLevelCodes, Person, ReferentialRef} from "../../../referential/referential.module";
import {Moment} from "moment/moment";
import {DataEntity, DataRootEntity, DataRootVesselEntity} from "./base.model";
import {IEntityWithMeasurement, Measurement, MeasurementUtils} from "./measurement.model";


export class Batch extends DataEntity<Batch> implements IEntityWithMeasurement<Batch> {

  static fromObject(source: any): Batch {
    const res = new Batch();
    res.fromObject(source);
    return res;
  }

  label: string;
  rankOrder: number;
  exhaustiveInventory: boolean;
  samplingRatio: number;
  samplingRatioText: string;
  individualCount: number;
  taxonGroup: ReferentialRef;
  taxonName: ReferentialRef;
  comments: string;
  measurementValues: { [key: number]: any };

  operationId: number;
  parentId: number;
  parent: Batch;
  children: Batch[];

  constructor() {
    super();
    this.parent = null;
    this.taxonGroup = null;
    this.taxonName = null;
    this.measurementValues = {};
    this.children = [];
    this.individualCount = null;
    this.samplingRatio = null;
    this.rankOrder = null;
  }

  clone(): Batch {
    const target = new Batch();
    target.fromObject(this.asObject());
    return target;
  }

  asObject(minify?: boolean): any {
    let parent = this.parent; // avoid parent conversion
    this.parent = null;
    const target = super.asObject(minify);
    delete target.parentBatch;
    this.parent = parent;

    target.taxonGroup = this.taxonGroup && this.taxonGroup.asObject(false /*fix #32*/ ) || undefined;
    target.taxonName = this.taxonName && this.taxonName.asObject(false /*fix #32*/) || undefined;
    target.individualCount = isNotNil(this.individualCount) ? this.individualCount : null;
    target.children = this.children && this.children.map(c => c.asObject(minify)) || undefined;
    target.parentId = this.parentId || this.parent && this.parent.id || undefined;

    if (minify) {
      // Parent Id not need, as the tree batch will be used by pod
      delete target.parent;
      delete target.parentId;

      // Measurement: keep only the map
      target.measurementValues = this.measurementValues && Object.getOwnPropertyNames(this.measurementValues)
        .reduce((map, pmfmId) => {
          const value = this.measurementValues[pmfmId] && this.measurementValues[pmfmId].id || this.measurementValues[pmfmId];
          if (isNotNil(value)) map[pmfmId] = '' + value;
          return map;
        }, {}) || undefined;
    }

    return target;
  }

  fromObject(source: any): Batch {
    super.fromObject(source);
    this.label = source.label;
    this.rankOrder = source.rankOrder;
    this.exhaustiveInventory = source.exhaustiveInventory;
    this.samplingRatio = source.samplingRatio;
    this.samplingRatioText = source.samplingRatioText;
    this.individualCount = isNotNil(source.individualCount) && source.individualCount !== "" ? source.individualCount : null;
    this.taxonGroup = source.taxonGroup && ReferentialRef.fromObject(source.taxonGroup) || undefined;
    this.taxonName = source.taxonName && ReferentialRef.fromObject(source.taxonName) || undefined;
    this.comments = source.comments;
    this.operationId = source.operationId;
    this.parentId = source.parentId;
    this.parent = source.parent;

    if (source.measurementValues) {
      this.measurementValues = source.measurementValues;
    }
    // Convert measurement to map
    else if (source.measurements) {
      this.measurementValues = source.measurements && source.measurements.reduce((map, m) => {
        const value = m && m.pmfmId && (m.alphanumericalValue || m.numericalValue || (m.qualitativeValue && m.qualitativeValue.id));
        if (value) map[m.pmfmId] = value;
        return map;
      }, {}) || undefined;
    }

    return this;
  }

  equals(other: Batch): boolean {
    // equals by ID
    return super.equals(other)
      // Or by functional attributes
      || (this.rankOrder === other.rankOrder
        // same operation
        && ((!this.operationId && !other.operationId) || this.operationId === other.operationId)
        // same label
        && ((!this.label && !other.label) || this.label === other.label)
        // Warn: compare using the parent ID is too complicated
      );
  }
}
