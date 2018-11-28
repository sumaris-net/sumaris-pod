import {
  Referential, ReferentialRef, EntityUtils, Department, Person,
  toDateISOString, fromDateISOString,
  vesselFeaturesToString, entityToString, referentialToString,
  StatusIds, Cloneable, Entity, LocationLevelIds, VesselFeatures, GearLevelIds, TaxonGroupIds,
  PmfmStrategy, getPmfmName
} from "../../referential/services/model";
import { Moment } from "moment/moment";
import { isNotNil, isNil, AcquisitionLevelCodes } from "../../core/services/model";

export {
  Referential, ReferentialRef, EntityUtils, Person, Department,
  toDateISOString, fromDateISOString,
  vesselFeaturesToString, entityToString, referentialToString, getPmfmName,
  StatusIds, Cloneable, Entity, VesselFeatures, LocationLevelIds, GearLevelIds, TaxonGroupIds,
  PmfmStrategy
};

/* -- Helper function -- */

export function fillRankOrder(values: { rankOrder: number }[]) {
  // Compute rankOrder
  let maxRankOrder = 0;
  (values || []).forEach(m => {
    if (m.rankOrder && m.rankOrder > maxRankOrder) maxRankOrder = m.rankOrder;
  });
  (values || []).forEach(m => {
    m.rankOrder = m.rankOrder || maxRankOrder++;
  });
}

const sortByDateTimeFn = (n1: VesselPosition, n2: VesselPosition) => { return n1.dateTime.isSame(n2.dateTime) ? 0 : (n1.dateTime.isAfter(n2.dateTime) ? 1 : -1); };


/* -- DATA -- */
export abstract class DataEntity<T> extends Entity<T> {
  recorderDepartment: Department;

  constructor() {
    super();
    this.recorderDepartment = new Department();
  }

  asObject(minify?: boolean): any {
    const target = super.asObject(minify);
    target.recorderDepartment = this.recorderDepartment && this.recorderDepartment.asObject(minify) || undefined;
    return target;
  }

  fromObject(source: any): DataEntity<T> {
    super.fromObject(source);
    source.recorderDepartment && this.recorderDepartment.fromObject(source.recorderDepartment);
    return this;
  }

}

export abstract class DataRootEntity<T> extends DataEntity<T> {
  comments: string;
  creationDate: Moment;
  recorderPerson: Person;

  constructor() {
    super();
    this.recorderPerson = new Person();
  }

  asObject(minify?: boolean): any {
    const target = super.asObject(minify);
    target.creationDate = toDateISOString(this.creationDate);
    target.recorderPerson = this.recorderPerson && this.recorderPerson.asObject(minify) || undefined;
    return target;
  }

  fromObject(source: any): DataRootEntity<T> {
    super.fromObject(source);
    this.comments = source.comments;
    this.creationDate = fromDateISOString(source.creationDate);
    source.recorderPerson && this.recorderPerson.fromObject(source.recorderPerson);
    return this;
  }
}

export abstract class DataRootVesselEntity<T> extends DataRootEntity<T> {
  vesselFeatures: VesselFeatures;
  // TODO: program: string;

  constructor() {
    super();
    this.vesselFeatures = new VesselFeatures();
  }

  asObject(minify?: boolean): any {
    const target = super.asObject();
    target.vesselFeatures = this.vesselFeatures && this.vesselFeatures.asObject(minify) || undefined;
    return target;
  }

  fromObject(source: any): DataRootVesselEntity<T> {
    super.fromObject(source);
    source.vesselFeatures && this.vesselFeatures.fromObject(source.vesselFeatures);
    // TODO: source.program && this.program;
    return this;
  }
}

export class Trip extends DataRootVesselEntity<Trip> {

  static fromObject(source: any): Trip {
    const res = new Trip();
    res.fromObject(source);
    return res;
  }

  program: ReferentialRef;
  departureDateTime: Moment;
  returnDateTime: Moment;
  departureLocation: ReferentialRef;
  returnLocation: ReferentialRef;
  sale: Sale;
  gears: PhysicalGear[];
  measurements: Measurement[];

  constructor() {
    super();
    this.program = new ReferentialRef();
    this.departureLocation = new ReferentialRef();
    this.returnLocation = null;
    this.measurements = [];
  }

  clone(): Trip {
    const target = new Trip();
    target.fromObject(this.asObject);
    return target;
  }

  copy(target: Trip) {
    target.fromObject(this);
  }

  asObject(minify?: boolean): any {
    const target = super.asObject(minify);
    target.program = this.program && this.program.asObject(minify) || undefined;
    target.departureDateTime = toDateISOString(this.departureDateTime);
    target.returnDateTime = toDateISOString(this.returnDateTime);
    target.departureLocation = this.departureLocation && this.departureLocation.asObject(minify) || undefined;
    target.returnLocation = this.returnLocation && this.returnLocation.asObject(minify) || undefined;
    target.sale = this.sale && this.sale.asObject(minify) || undefined;
    target.gears = this.gears && this.gears.map(p => p && p.asObject(minify)) || undefined;
    target.measurements = this.measurements && this.measurements.filter(MeasurementUtils.isNotEmpty).map(m => m.asObject(minify)) || undefined;
    return target;
  }

  fromObject(source: any): Trip {
    super.fromObject(source);
    source.program && this.program.fromObject(source.program);
    this.departureDateTime = fromDateISOString(source.departureDateTime);
    this.returnDateTime = fromDateISOString(source.returnDateTime);
    source.departureLocation && this.departureLocation.fromObject(source.departureLocation);
    this.returnLocation = source.returnLocation && ReferentialRef.fromObject(source.returnLocation);
    if (source.sale) {
      this.sale = new Sale();
      this.sale.fromObject(source.sale);
    };
    this.gears = source.gears && source.gears.filter(g => !!g).map(PhysicalGear.fromObject) || undefined;
    this.measurements = source.measurements && source.measurements.map(Measurement.fromObject) || [];
    return this;
  }

  equals(other: Trip): boolean {
    return super.equals(other)
      || (
        // Same vessel
        (this.vesselFeatures && other.vesselFeatures && this.vesselFeatures.vesselId === other.vesselFeatures.vesselId)
        // Same departure date (or, if not set, same return date)
        && ((this.departureDateTime === other.departureDateTime)
          || (!this.departureDateTime && !other.departureDateTime && this.returnDateTime === other.returnDateTime))
      );
  }
}

export class PhysicalGear extends DataRootEntity<PhysicalGear> {

  static fromObject(source: any): PhysicalGear {
    const res = new PhysicalGear();
    res.fromObject(source);
    return res;
  }

  rankOrder: number;
  gear: ReferentialRef;
  measurements: Measurement[];

  constructor() {
    super();
    this.gear = new ReferentialRef();
    this.measurements = [];
  }

  clone(): PhysicalGear {
    const target = new PhysicalGear();
    target.fromObject(this.asObject());
    return target;
  }

  copy(target: PhysicalGear) {
    target.fromObject(this);
  }

  asObject(minify?: boolean): any {
    const target = super.asObject(minify);
    target.gear = this.gear && this.gear.asObject(minify) || undefined;

    // Measurements
    target.measurements = this.measurements && this.measurements.filter(MeasurementUtils.isNotEmpty).map(m => m.asObject(minify)) || undefined;

    return target;
  }

  fromObject(source: any): PhysicalGear {
    super.fromObject(source);
    this.rankOrder = source.rankOrder;
    source.gear && this.gear.fromObject(source.gear);
    this.measurements = source.measurements && source.measurements.map(Measurement.fromObject) || [];
    return this;
  }

  equals(other: PhysicalGear): boolean {
    return super.equals(other)
      || (
        // Same gear
        (this.gear && other.gear && this.gear.id === other.gear.id)
        // Same rankOrder
        && (this.rankOrder === other.rankOrder)
      );
  }
}

export class Measurement extends DataEntity<Measurement> {
  pmfmId: number;
  alphanumericalValue: string;
  numericalValue: number;
  qualitativeValue: ReferentialRef;
  digitCount: number;
  rankOrder: number;

  static fromObject(source: any): Measurement {
    const res = new Measurement();
    res.fromObject(source);
    return res;
  }

  constructor() {
    super();
  }

  clone(): Measurement {
    const target = new Measurement();
    target.fromObject(this.asObject());
    return target;
  }

  copy(target: Measurement) {
    target.fromObject(this);
  }

  asObject(minify?: boolean): any {
    const target = super.asObject(minify);
    target.qualitativeValue = this.qualitativeValue && this.qualitativeValue.asObject(minify) || undefined;
    return target;
  }

  fromObject(source: any): Measurement {
    super.fromObject(source);
    this.pmfmId = source.pmfmId;
    this.alphanumericalValue = source.alphanumericalValue;
    this.numericalValue = source.numericalValue;
    this.digitCount = source.digitCount;
    this.rankOrder = source.rankOrder;
    this.qualitativeValue = source.qualitativeValue && ReferentialRef.fromObject(source.qualitativeValue);

    return this;
  }

  equals(other: Measurement): boolean {
    return super.equals(other)
      || (
        // Same [pmfmId, rankOrder]
        (this.pmfmId && other.pmfmId && this.rankOrder === other.rankOrder)
      );
  }

}

export class MeasurementUtils {

  static toFormValues(source: Measurement[], pmfms: PmfmStrategy[]): any {
    const res: any = {};
    pmfms.forEach(p => {
      const m = source && source.find(m => m.pmfmId === p.pmfmId);
      if (m) {
        res[p.pmfmId] = MeasurementUtils.normalizeFormValue(MeasurementUtils.getMeasurementEntityValue(m, p), p);
      }
      else {
        res[p.pmfmId] = null;
      }
    });
    return res;
  }

  static initAllMeasurements(source: Measurement[], pmfms: PmfmStrategy[]): Measurement[] {
    // Work on a copy, to be able to reduce the array
    let rankOrder = 1;
    return (pmfms || []).map(pmfm => {
      const m = (source || []).find(m => m.pmfmId === pmfm.pmfmId) || new Measurement();
      m.pmfmId = pmfm.pmfmId; // apply the pmfm (need for new object)
      m.rankOrder = rankOrder++;
      return m;
    });
  }

  // Update measurement values
  static updateMeasurementValues(valuesMap: { [key: number]: any }, measurements: Measurement[], pmfms: PmfmStrategy[]) {
    (measurements || []).forEach(m => {
      const pmfm = pmfms && pmfms.find(pmfm => pmfm.pmfmId === m.pmfmId);
      if (pmfm) MeasurementUtils.setMeasurementValue(valuesMap[pmfm.pmfmId], m, pmfm);
    });
  }

  static getMeasurementEntityValue(source: Measurement, pmfm: PmfmStrategy): any {
    switch (pmfm.type) {
      case "qualitative_value":
        if (source.qualitativeValue && source.qualitativeValue.id) {
          return pmfm.qualitativeValues.find(qv => qv.id == source.qualitativeValue.id);
        }
        return null;
      case "integer":
      case "double":
        return source.numericalValue;
      case "string":
        return source.alphanumericalValue;
      case "boolean":
        return source.numericalValue === 1 ? true : (source.numericalValue === 0 ? false : undefined);
      case "date":
        return fromDateISOString(source.alphanumericalValue);
      default:
        throw new Error("Unknown pmfm.type for getting value of measurement: " + pmfm.type);
    }
  }


  static setMeasurementValue(value: any, target: Measurement, pmfm: PmfmStrategy) {
    value = (value === null || value === undefined) ? undefined : value;
    switch (pmfm.type) {
      case "qualitative_value":
        target.qualitativeValue = value;
        break;
      case "integer":
      case "double":
        target.numericalValue = value;
        break;
      case "string":
        target.alphanumericalValue = value;
        break;
      case "boolean":
        target.numericalValue = (value === true || value === "true") ? 1 : ((value === false || value === "false") ? 0 : undefined);
        break;
      case "date":
        target.alphanumericalValue = toDateISOString(value);
        break;
      default:
        throw new Error("Unknown pmfm.type: " + pmfm.type);
    }
  }

  static normalizeFormValue(value: any, pmfm: PmfmStrategy): any {
    if (!pmfm) return value;
    switch (pmfm.type) {
      case "qualitative_value":
        if (value && typeof value != "object") {
          const qvId = parseInt(value);
          return pmfm.qualitativeValues && pmfm.qualitativeValues.find(qv => qv.id == qvId) || null;
        }
        return value || null;
      case "integer":
        return isNotNil(value) ? parseInt(value) : null;
      case "double":
        return isNotNil(value) ? parseFloat(value) : null;
      case "string":
        return value || null;
      case "boolean":
        return (value === "true" || value === true || value === 1) ? true : ((value === "false" || value === false || value === 0) ? false : null);
      case "date":
        return fromDateISOString(value) || null;
      default:
        throw new Error("Unknown pmfm.type: " + pmfm.type);
    }
  }

  static normalizeFormValues(source: { [key: number]: any }, pmfms: PmfmStrategy[]): any {
    const target = {};
    (pmfms || []).forEach(pmfm => {
      target[pmfm.pmfmId] = MeasurementUtils.normalizeFormValue(source[pmfm.pmfmId], pmfm);
    });
    return target;
  }

  static toEntityValue(value: any, pmfm: PmfmStrategy): string {
    if (value === null || value === undefined || !pmfm) return;
    switch (pmfm.type) {
      case "qualitative_value":
        return value && value.id && value.id.toString() || undefined;
      case "integer":
      case "double":
        return value && value.toString() || undefined;
      case "string":
        return value;
      case "boolean":
        return (value === true || value === "true") ? "true" : ((value === false || value === "false") ? "false" : undefined);
      case "date":
        return toDateISOString(value);
      default:
        throw new Error("Unknown pmfm.type: " + pmfm.type);
    }
  }

  static toEntityValues(source: { [key: number]: any }, pmfms: PmfmStrategy[]): { [key: string]: any } {
    const target = {};
    pmfms.forEach(pmfm => {
      target[pmfm.pmfmId] = MeasurementUtils.toEntityValue(source[pmfm.pmfmId], pmfm);
    });
    return target;
  }

  static isEmpty(source: Measurement | any): boolean {
    if (!source) return true;
    return isNil(source.alphanumericalValue)
      && isNil(source.numericalValue)
      && (!source.qualitativeValue || isNil(source.qualitativeValue.id))
  }

  static isNotEmpty(source: Measurement | any): boolean {
    return !MeasurementUtils.isEmpty(source)
  }
}

export class Sale extends DataRootVesselEntity<Sale> {
  startDateTime: Moment;
  endDateTime: Moment;
  saleLocation: ReferentialRef;
  saleType: ReferentialRef;

  constructor() {
    super();
    this.saleLocation = new ReferentialRef();
    this.saleType = new ReferentialRef();
  }

  clone(): Sale {
    const target = new Sale();
    target.fromObject(this.asObject());
    return target;
  }

  copy(target: Sale) {
    target.fromObject(this);
  }

  asObject(minify?: boolean): any {
    const target = super.asObject(minify);
    target.startDateTime = toDateISOString(this.startDateTime);
    target.endDateTime = toDateISOString(this.endDateTime);
    target.saleLocation = this.saleLocation && this.saleLocation.asObject(minify) || undefined;
    target.saleType = this.saleType && this.saleType.asObject(minify) || undefined;
    return target;
  }

  fromObject(source: any): Sale {
    super.fromObject(source);
    this.startDateTime = fromDateISOString(source.startDateTime);
    this.endDateTime = fromDateISOString(source.endDateTime);
    source.saleLocation && this.saleLocation.fromObject(source.saleLocation);
    source.saleType && this.saleType.fromObject(source.saleType);
    return this;
  }
}

export class Operation extends DataEntity<Operation> {

  static fromObject(source: any): Operation {
    const res = new Operation();
    res.fromObject(source);
    return res;
  }

  startDateTime: Moment;
  endDateTime: Moment;
  fishingStartDateTime: Moment;
  fishingEndDateTime: Moment;
  comments: string;
  rankOrderOnPeriod: number;
  hasCatch: boolean;
  positions: VesselPosition[];
  startPosition: VesselPosition;
  endPosition: VesselPosition;

  metier: ReferentialRef;
  physicalGear: PhysicalGear;
  tripId: number;

  measurements: Measurement[];
  samples: Sample[];
  catchBatch: Batch;

  constructor() {
    super();
    this.metier = new ReferentialRef();
    this.startPosition = new VesselPosition();
    this.endPosition = new VesselPosition();
    this.physicalGear = new PhysicalGear();
    this.measurements = [];
    this.samples = [];
    this.catchBatch = null;
  }

  clone(): Operation {
    const target = new Operation();
    target.fromObject(this.asObject());
    return target;
  }

  asObject(minify?: boolean): any {
    const target = super.asObject(minify);
    target.startDateTime = toDateISOString(this.startDateTime);
    target.endDateTime = toDateISOString(this.endDateTime);
    target.fishingStartDateTime = toDateISOString(this.fishingStartDateTime);
    target.fishingEndDateTime = toDateISOString(this.fishingEndDateTime);
    target.metier = this.metier && this.metier.asObject(false/*Always minify=false, because of operations tables cache*/) || undefined;

    // Create an array of position, instead of start/end
    target.positions = [this.startPosition, this.endPosition].map(p => p && p.asObject(minify)) || undefined;
    delete target.startPosition;
    delete target.endPosition;

    // Physical gear: keep id
    target.physicalGearId = this.physicalGear && this.physicalGear.id;
    delete target.physicalGear;

    // Measurements
    target.measurements = this.measurements && this.measurements.filter(MeasurementUtils.isNotEmpty).map(m => m.asObject(minify)) || undefined;

    // Samples
    target.samples = this.samples && this.samples.map(s => s.asObject(minify)) || undefined;

    // Batch
    target.catchBatch = this.catchBatch && this.catchBatch.asObject(minify) || undefined;

    return target;
  }

  fromObject(source: any): Operation {
    super.fromObject(source);
    this.hasCatch = source.hasCatch;
    this.comments = source.comments;
    this.tripId = source.tripId;
    this.physicalGear = source.physicalGear && PhysicalGear.fromObject(source.physicalGear) || new PhysicalGear();
    this.physicalGear.id = this.physicalGear.id || source.physicalGearId;
    this.startDateTime = fromDateISOString(source.startDateTime);
    this.endDateTime = fromDateISOString(source.endDateTime);
    this.fishingStartDateTime = fromDateISOString(source.fishingStartDateTime);
    this.fishingEndDateTime = fromDateISOString(source.fishingEndDateTime);
    this.rankOrderOnPeriod = source.rankOrderOnPeriod;
    source.metier && this.metier.fromObject(source.metier);
    if (source.startPosition || source.endPosition) {
      this.startPosition = source.startPosition && VesselPosition.fromObject(source.startPosition);
      this.endPosition = source.endPosition && VesselPosition.fromObject(source.endPosition);
      this.positions = undefined;
    }
    else if (source.positions) {
      const positions = source.positions.map(VesselPosition.fromObject).sort(sortByDateTimeFn) || undefined;
      if (positions.length == 2) {
        this.startPosition = positions[0];
        this.endPosition = positions[1];
        this.positions = undefined;
      }
      else {
        this.startPosition = undefined;
        this.endPosition = undefined;
        this.positions = positions;
      }
    }
    this.measurements = source.measurements && source.measurements.map(Measurement.fromObject) || [];

    // Samples
    this.samples = source.samples && source.samples.map(Sample.fromObject) || undefined;

    // Batches
    if (source.batches) {
      let batches = (source.batches || []).map(Batch.fromObject);
      this.catchBatch = batches.find(b => isNil(b.parentId) && (isNil(b.label) || b.label === AcquisitionLevelCodes.CATCH_BATCH)) || undefined;
      if (this.catchBatch) {
        batches.forEach(s => {
          // Link to parent
          s.parent = isNotNil(s.parentId) && batches.find(p => p.id === s.parentId) || undefined;
          s.parentId = undefined; // Avoid redundant info on parent
        });
        // Link to children
        batches.forEach(s => s.children = batches.filter(p => p.parent && p.parent === s) || []);
        this.catchBatch.children = batches.filter(b => b.parent === this.catchBatch);
        //console.log("[trip-model] Operation.catchBatch:", this.catchBatch);
      }
    }
    else {
      this.catchBatch = null;
    }
    return this;
  }

  equals(other: Operation): boolean {
    return super.equals(other)
      || ((this.startDateTime === other.startDateTime
        || (!this.startDateTime && !other.startDateTime && this.fishingStartDateTime === other.fishingStartDateTime))
        && ((!this.rankOrderOnPeriod && !other.rankOrderOnPeriod) || (this.rankOrderOnPeriod === other.rankOrderOnPeriod))
      );
  }
}

export class VesselPosition extends DataEntity<Operation> {

  static fromObject(source: any): VesselPosition {
    const res = new VesselPosition();
    res.fromObject(source);
    return res;
  }

  dateTime: Moment;
  latitude: number;
  longitude: number;
  operationId: number;

  constructor() {
    super();
  }

  clone(): Operation {
    const target = new Operation();
    target.fromObject(this.asObject());
    return target;
  }

  asObject(minify?: boolean): any {
    const target = super.asObject(minify);
    target.dateTime = toDateISOString(this.dateTime);
    return target;
  }

  fromObject(source: any): VesselPosition {
    super.fromObject(source);
    this.latitude = source.latitude;
    this.longitude = source.longitude;
    this.operationId = source.operationId;
    this.dateTime = fromDateISOString(source.dateTime);
    return this;
  }

  equals(other: VesselPosition): boolean {
    return super.equals(other)
      || (this.dateTime === other.dateTime
        && (!this.operationId && !other.operationId || this.operationId === other.operationId));
  }
}

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
  }

  clone(): Sample {
    const target = new Sample();
    target.fromObject(this.asObject());
    return target;
  }

  asObject(minify?: boolean): any {
    const target = super.asObject(minify);
    target.sampleDate = toDateISOString(this.sampleDate);
    target.taxonGroup = this.taxonGroup && this.taxonGroup.asObject(minify) || undefined;

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
    this.individualCount = source.individualCount;
    this.comments = source.comments;
    this.taxonGroup = source.taxonGroup && ReferentialRef.fromObject(source.taxonGroup) || undefined;
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

export class Batch extends DataEntity<Batch> {

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

    target.taxonGroup = this.taxonGroup && this.taxonGroup.asObject(minify) || undefined;
    target.taxonName = this.taxonName && this.taxonName.asObject(minify) || undefined;

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

  fromObject(source: any): Batch {
    super.fromObject(source);
    this.label = source.label;
    this.rankOrder = source.rankOrder;
    this.exhaustiveInventory = source.exhaustiveInventory;
    this.samplingRatio = source.samplingRatio;
    this.samplingRatioText = source.samplingRatioText;
    this.individualCount = source.individualCount;
    this.taxonGroup = source.taxonGroup && ReferentialRef.fromObject(source.taxonGroup) || undefined;
    this.taxonName = source.taxonName && ReferentialRef.fromObject(source.taxonName) || undefined;
    this.comments = source.comments;
    this.operationId = source.operationId;
    this.parentId = source.parentId;

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
