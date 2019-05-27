import {fromDateISOString, isNotNil, toDateISOString} from "../../../core/core.module";
import {ReferentialRef} from "../../../referential/referential.module";
import {Moment} from "moment/moment";
import {DataRootEntity} from "./base.model";


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
    this.taxonGroup = null;
    this.measurementValues = {};
    this.children = [];
    this.individualCount = null;
    this.rankOrder = null;
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
    delete target.parent;

    target.children = this.children && this.children.map(c => c.asObject(minify)) || undefined;

    // Measurement: keep only the map
    if (minify) {
      target.measurementValues = this.measurementValues && Object.getOwnPropertyNames(this.measurementValues)
        .reduce((map, pmfmId) => {
          const value = this.measurementValues[pmfmId] && this.measurementValues[pmfmId].id || this.measurementValues[pmfmId];
          if (isNotNil(value)) map[pmfmId] = '' + value;
          return map;
        }, {}) || undefined;
    }
    return target;
  }

  fromObject(source: any): Sample {
    super.fromObject(source);
    this.label = source.label;
    this.rankOrder = source.rankOrder;
    this.sampleDate = fromDateISOString(source.sampleDate);
    this.individualCount = isNotNil(source.individualCount) && source.individualCount !== "" ? source.individualCount : null;
    this.comments = source.comments;
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
      this.measurementValues = source.measurements && source.measurements.reduce((map, m) => {
        const value = m && m.pmfmId && (m.alphanumericalValue || m.numericalValue || (m.qualitativeValue && m.qualitativeValue.id));
        if (value) map[m.pmfmId] = value;
        return map;
      }, {}) || undefined;
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
