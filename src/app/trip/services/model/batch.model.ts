import {EntityUtils, isNil, isNotNil, referentialToString} from "../../../core/core.module";
import {AcquisitionLevelCodes, PmfmStrategy, ReferentialRef} from "../../../referential/referential.module";
import {DataEntity} from "./base.model";
import {IEntityWithMeasurement, MeasurementUtils, MeasurementValuesUtils} from "./measurement.model";
import {isNilOrBlank, isNotNilOrBlank} from "../../../shared/functions";

export declare interface BatchWeight {
  methodId: number;
  estimated: boolean;
  calculated: boolean;
  value: any;
}

export class Batch extends DataEntity<Batch> implements IEntityWithMeasurement<Batch> {

  static SAMPLE_BATCH_SUFFIX = '.%';

  static fromObject(source: any): Batch {
    const res = new Batch();
    res.fromObject(source);
    return res;
  }

  static fromObjectArrayAsTree(source: any[]): Batch {
    if (!source) return null;
    const batches = (source || []).map(Batch.fromObject);
    const catchBatch = batches.find(b => isNil(b.parentId) && (isNilOrBlank(b.label) || b.label === AcquisitionLevelCodes.CATCH_BATCH)) || undefined;
    if (catchBatch) {
      batches.forEach(s => {
        // Link to parent
        s.parent = isNotNil(s.parentId) && batches.find(p => p.id === s.parentId) || undefined;
        s.parentId = undefined; // Avoid redundant info on parent
      });
      // Link to children
      batches.forEach(s => s.children = batches.filter(p => p.parent && p.parent === s) || []);
      if (catchBatch.children && catchBatch.children.length) {
        //console.log("TODO: not need to reset children of catch batch ?", this.catchBatch);
      } else {
        catchBatch.children = batches.filter(b => b.parent === catchBatch);
      }
    }

    //console.debug("[trip-model] Operation.catchBatch as tree:", this.catchBatch);
    return catchBatch;
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
  weight: BatchWeight;

  operationId: number;
  parentId: number;
  parent: Batch;
  children: Batch[];

  constructor() {
    super();
    this.label = null;
    this.rankOrder = null;
    this.exhaustiveInventory = null;
    this.samplingRatio = null;
    this.samplingRatioText = null;
    this.individualCount = null;
    this.taxonGroup = null;
    this.taxonName = null;
    this.comments = null;

    this.operationId = null;
    this.parentId = null;
    this.parent = null;
    this.measurementValues = {};
    this.children = [];

    this.weight = null;
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

    target.taxonGroup = this.taxonGroup && this.taxonGroup.asObject(false /*fix #32*/) || undefined;
    target.taxonName = this.taxonName && this.taxonName.asObject(false /*fix #32*/) || undefined;
    target.samplingRatio = isNotNil(this.samplingRatio) ? this.samplingRatio : null;
    target.individualCount = isNotNil(this.individualCount) ? this.individualCount : null;
    target.children = this.children && this.children.map(c => c.asObject(minify)) || undefined;
    target.parentId = this.parentId || this.parent && this.parent.id || undefined;
    target.measurementValues = MeasurementUtils.measurementValuesAsObjectMap(this.measurementValues, minify);

    if (minify) {
      // Parent Id not need, as the tree batch will be used by pod
      delete target.parent;
      delete target.parentId;

      delete target.weight;
    }

    return target;
  }

  fromObject(source: any): Batch {
    super.fromObject(source);
    this.label = source.label;
    this.rankOrder = +source.rankOrder;
    this.exhaustiveInventory = source.exhaustiveInventory;
    this.samplingRatio = isNotNilOrBlank(source.samplingRatio) ? parseFloat(source.samplingRatio) : null;
    this.samplingRatioText = source.samplingRatioText;
    this.individualCount = isNotNilOrBlank(source.individualCount) ? parseInt(source.individualCount) : null;
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

  get hasTaxonNameOrGroup(): boolean {
    return (EntityUtils.isNotEmpty(this.taxonName) || EntityUtils.isNotEmpty(this.taxonGroup)) && true;
  }
}

export class BatchUtils {

  static parentToString(batch: Batch, opts?: {
    pmfm?: PmfmStrategy,
    taxonGroupAttributes: string[];
    taxonNameAttributes: string[];
  }): string {
    if (!batch) return null;
    opts = opts || {taxonGroupAttributes: ['label', 'name'], taxonNameAttributes: ['label', 'name']};
    if (opts.pmfm) {
      console.log("TODO: check parent pmfm", opts.pmfm);
    }

    const hasTaxonGroup = EntityUtils.isNotEmpty(batch.taxonGroup);
    const hasTaxonName = EntityUtils.isNotEmpty(batch.taxonName);
    // Display only taxon name, if no taxon group or same label
    if (hasTaxonName && (!hasTaxonGroup || batch.taxonGroup.label === batch.taxonName.label)) {
      return referentialToString(batch.taxonName, opts.taxonNameAttributes);
    }
    // Display both, if both exists
    if (hasTaxonName && hasTaxonGroup) {
      return referentialToString(batch.taxonGroup, opts.taxonGroupAttributes) + ' / '
        + referentialToString(batch.taxonName, opts.taxonNameAttributes);
    }
    // Display only taxon group
    if (hasTaxonGroup) {
      return referentialToString(batch.taxonGroup, opts.taxonGroupAttributes);
    }

    // Display rankOrder only (should never occur)
    return `#${batch.rankOrder}`;
  }

  static isSampleBatch(batch: Batch) {
    return batch && isNotNilOrBlank(batch.label) && batch.label.endsWith(Batch.SAMPLE_BATCH_SUFFIX);
  }

  static isSampleNotEmpty(sampleBatch: Batch): boolean {
    return isNotNil(sampleBatch.individualCount)
      || isNotNil(sampleBatch.samplingRatio)
      || Object.getOwnPropertyNames(sampleBatch.measurementValues || {})
        .filter(key => isNotNil(sampleBatch.measurementValues[key]))
        .length > 0;
  }

  public static canMergeSubBatch(b1: Batch, b2: Batch, pmfms: PmfmStrategy[]): boolean {
    return EntityUtils.equals(b1.parent, b2.parent)
      && EntityUtils.equals(b1.taxonName, b2.taxonName)
      && MeasurementValuesUtils.equalsPmfms(b1.measurementValues, b2.measurementValues, pmfms);
  }

  static getOrCreateSamplingChild(parent: Batch) {
    const samplingLabel = parent.label + Batch.SAMPLE_BATCH_SUFFIX;

    const samplingChild = (parent.children || []).find(b => b.label === samplingLabel) || new Batch();
    const isNew = !samplingChild.label;

    if (isNew) {
      samplingChild.rankOrder = 1;
      samplingChild.label = samplingLabel;

      // Copy children into the sample batch
      samplingChild.children = parent.children || [];

      // Set sampling batch in parent's children
      parent.children = [samplingChild];
    }

    return samplingChild;
  }

  static getSamplingChild(parent: Batch): Batch | undefined {
    const samplingLabel = parent.label + Batch.SAMPLE_BATCH_SUFFIX;
    return (parent.children || []).find(b => b.label === samplingLabel);
  }

  static prepareSubBatchesForTable(rootBatches: Batch[], subAcquisitionLevel: string, qvPmfm?: PmfmStrategy): Batch[] {
    if (qvPmfm) {
      return rootBatches.reduce((res, rootBatch) => {
        return res.concat((rootBatch.children || []).reduce((res, qvBatch) => {
          const children = BatchUtils.getChildrenByLevel(qvBatch, subAcquisitionLevel);
          return res.concat(children
            .map(child => {
              // Copy QV value from the root batch
              child.measurementValues = child.measurementValues || {};
              child.measurementValues[qvPmfm.pmfmId] = qvBatch.measurementValues[qvPmfm.pmfmId];
              // Replace parent by the group (instead of the sampling batch)
              child.parentId = rootBatch.id;
              return child;
            }));
        }, []));
      }, []);
    }
    return rootBatches.reduce((res, rootBatch) => {
      return res.concat(BatchUtils.getChildrenByLevel(rootBatch, subAcquisitionLevel)
        .map(child => {
          // Replace parent by the group (instead of the sampling batch)
          child.parentId = rootBatch.id;
          return child;
        }));
    }, []);
  }

  static getChildrenByLevel(batch: Batch, acquisitionLevel: string): Batch[] {
    return (batch.children || []).reduce((res, child) => {
      if (child.label && child.label.startsWith(acquisitionLevel + "#")) return res.concat(child);
      return res.concat(BatchUtils.getChildrenByLevel(child, acquisitionLevel)); // recursive call
    }, []);
  }

  static prepareRootBatchesForSaving(rootBatches: Batch[], subBatches: Batch[], qvPmfm?: PmfmStrategy) {
    (rootBatches || []).forEach(rootBatch => {
      // Add children
      (rootBatch.children || []).forEach(b => {
        const children = subBatches.filter(childBatch => childBatch.parent && rootBatch.equals(childBatch.parent) &&
          (!qvPmfm || (childBatch.measurementValues[qvPmfm.pmfmId] == b.measurementValues[qvPmfm.pmfmId]))
        );
        // If has sampling batch, use it as parent
        if (b.children && b.children.length === 1 && BatchUtils.isSampleBatch(b.children[0])) {
          b.children[0].children = children;
          children.forEach(c => c.parentId = b.children[0].id);
        } else {
          b.children = children;
          children.forEach(c => {
            c.parentId = b.id;
          });
        }
      });
    });
    return rootBatches;
  }

  static prepareBatchesForSimpleTable(rootBatches: Batch[], qvPmfm?: PmfmStrategy): Batch[] {
    if (!qvPmfm) return rootBatches || [];

    return (rootBatches || []).reduce((res, rootBatch) => {
        const children = qvPmfm.qualitativeValues.map((qv, index) => {
          let child = (rootBatch.children || []).find(childBatch => childBatch.measurementValues[qvPmfm.pmfmId] == qv);
          if (!child) {
            console.log("Creating new batch");
            child = new Batch();
            child.parent = rootBatch;
            child.rankOrder = index + 1;
            child.measurementValues = {};
            child.measurementValues[qvPmfm.pmfmId] = qv;
          }
          return child;
        });
        rootBatch.children = children;
        return res.concat(children);
      }, []);
  }
}
