import { AcquisitionLevelCodes, PmfmIds, QualitativeValueIds, QualityFlagIds } from '../../../referential/services/model/model.enum';
import { DataEntity, DataEntityAsObjectOptions } from '../../../data/services/model/data-entity.model';
import { IEntityWithMeasurement, IMeasurementValue, MeasurementFormValues, MeasurementModelValues, MeasurementUtils, MeasurementValuesUtils } from './measurement.model';
import {
  EntityClass,
  EntityUtils,
  isNil,
  isNilOrBlank,
  isNotEmptyArray,
  isNotNil,
  isNotNilOrBlank,
  ITreeItemEntity,
  ReferentialAsObjectOptions,
  referentialToString,
  ReferentialUtils,
  toNumber,
} from '@sumaris-net/ngx-components';
import { TaxonGroupRef } from '../../../referential/services/model/taxon-group.model';
import { PmfmValueUtils } from '../../../referential/services/model/pmfm-value.model';
import { IPmfm } from '../../../referential/services/model/pmfm.model';
import { NOT_MINIFY_OPTIONS } from '@app/core/services/model/referential.model';
import { TaxonNameRef } from '@app/referential/services/model/taxon-name.model';

export declare interface BatchWeight extends IMeasurementValue {
  unit?: 'kg';
}

export interface BatchAsObjectOptions extends DataEntityAsObjectOptions {
  withChildren?: boolean;
}

export interface BatchFromObjectOptions {
  withChildren?: boolean;
}

// WARN: always recreate en entity, even if source is a Batch
// because options can have changed
@EntityClass({typename: 'BatchVO', fromObjectReuseStrategy: 'clone'})
export class Batch<T extends Batch<T, ID> = Batch<any, any>,
  ID = number,
  O extends BatchAsObjectOptions = BatchAsObjectOptions,
  FO extends BatchFromObjectOptions = BatchFromObjectOptions>
  extends DataEntity<T, ID, O, FO>
  implements IEntityWithMeasurement<T, ID>,
    ITreeItemEntity<Batch> {

  static SAMPLING_BATCH_SUFFIX = '.%';
  static fromObject: (source: any, opts?: { withChildren?: boolean; }) => Batch;

  static fromObjectArrayAsTree(source: any[]): Batch {
    if (!source) return null;
    const batches = (source || []).map((json) => Batch.fromObject(json));
    const catchBatch = batches.find(b => isNil(b.parentId) && (isNilOrBlank(b.label) || b.label === AcquisitionLevelCodes.CATCH_BATCH)) || undefined;
    if (catchBatch) {
      batches.forEach(s => {
        // Link to parent
        s.parent = isNotNil(s.parentId) && batches.find(p => p.id === s.parentId) || undefined;
        s.parentId = undefined; // Avoid redundant info on parent
      });
      // Link to children
      batches.forEach(s => s.children = batches.filter(p => p.parent && p.parent === s) || []);
      // Fill catch children
      if (!catchBatch.children || !catchBatch.children.length) {
        catchBatch.children = batches.filter(b => b.parent === catchBatch);
      }
    }

    //console.debug("[trip-model] Operation.catchBatch as tree:", this.catchBatch);
    return catchBatch;
  }

  /**
   * Transform a batch entity tree, into a array of object.
   * Parent/.children link are removed, to keep only a parentId/
   * @param source
   * @param opts
   * @throw Error if a batch has no id
   */
  static treeAsObjectArray(source: Batch,
                           opts?: DataEntityAsObjectOptions & {
                             parent?: any;
                           }): any[] {
    if (!source) return null;

    // Convert entity into object, WITHOUT children (will be add later)
    const target = source.asObject ? source.asObject({...opts, withChildren: false}) : {...source, children: undefined};

    // Link target with the given parent
    const parent = opts && opts.parent;
    if (parent) {
      if (isNil(parent.id)) {
        throw new Error(`Cannot convert batch tree into array: No id found for batch ${parent.label}!`);
      }
      target.parentId = parent.id;
      delete target.parent; // not need
    }

    return (source.children || []).reduce((res, batch) => {
        return res.concat(this.treeAsObjectArray(batch, {...opts, parent: target}) || []);
      },
      [target]) || undefined;
  }

  static equals(b1: Batch | any, b2: Batch | any): boolean {
    return b1 && b2 && ((isNotNil(b1.id) && b1.id === b2.id)
      // Or by functional attributes
      || (b1.rankOrder === b2.rankOrder
        // same operation
        && ((!b1.operationId && !b2.operationId) || b1.operationId === b2.operationId)
        // same label
        && ((!b1.label && !b2.label) || b1.label === b2.label)
        // Warn: compare using the parent ID is too complicated
      ));
  }

  label: string = null;
  rankOrder: number = null;
  exhaustiveInventory: boolean = null;
  samplingRatio: number = null;
  samplingRatioText: string = null;
  individualCount: number = null;
  taxonGroup: TaxonGroupRef = null;
  taxonName: TaxonNameRef = null;
  comments: string = null;
  measurementValues: MeasurementModelValues | MeasurementFormValues = {};
  weight: BatchWeight = null;

  operationId: number = null;
  parentId: number = null;
  parent: Batch = null;
  children: Batch[] = null;

  constructor(__typename?: string) {
    super(__typename || Batch.TYPENAME);
  }

  asObject(opts?: O): any {
    const parent = this.parent;
    this.parent = null; // avoid parent conversion
    const target = super.asObject(opts);
    delete target.parentBatch;
    this.parent = parent;

    target.taxonGroup = this.taxonGroup && this.taxonGroup.asObject({...opts, ...NOT_MINIFY_OPTIONS, keepEntityName: true /*fix #32*/} as ReferentialAsObjectOptions) || undefined;
    target.taxonName = this.taxonName && this.taxonName.asObject({...opts, ...NOT_MINIFY_OPTIONS, keepEntityName: true /*fix #32*/} as ReferentialAsObjectOptions) || undefined;
    target.samplingRatio = isNotNil(this.samplingRatio) ? this.samplingRatio : null;
    target.individualCount = isNotNil(this.individualCount) ? this.individualCount : null;
    target.children = this.children && (!opts || opts.withChildren !== false) && this.children.map(c => c.asObject && c.asObject(opts) || c) || undefined;
    target.parentId = this.parentId || this.parent && this.parent.id || undefined;
    target.measurementValues = MeasurementValuesUtils.asObject(this.measurementValues, opts);

    if (opts && opts.minify) {
      // Parent Id not need, as the tree batch will be used by pod
      delete target.parent;
      delete target.parentId;
      // Remove computed properties
      delete target.weight;
      if (target.measurementValues) delete target.measurementValues.__typename
    }

    return target;
  }

  fromObject(source: any, opts?: FO) {
    super.fromObject(source);
    this.label = source.label;
    this.rankOrder = +source.rankOrder;
    this.exhaustiveInventory = source.exhaustiveInventory;
    this.samplingRatio = isNotNilOrBlank(source.samplingRatio) ? parseFloat(source.samplingRatio) : null;
    this.samplingRatioText = source.samplingRatioText;
    this.individualCount = isNotNilOrBlank(source.individualCount) ? parseInt(source.individualCount) : null;
    this.taxonGroup = source.taxonGroup && TaxonGroupRef.fromObject(source.taxonGroup) || undefined;
    this.taxonName = source.taxonName && TaxonNameRef.fromObject(source.taxonName) || undefined;
    this.comments = source.comments;
    this.operationId = source.operationId;
    this.parentId = source.parentId;
    this.parent = source.parent;

    this.weight = source.weight || undefined;

    if (source.measurementValues) {
      this.measurementValues = {...source.measurementValues};
    }
    // Convert measurement lists to map
    else if (source.sortingMeasurements || source.quantificationMeasurements) {
      const measurements = (source.sortingMeasurements || []).concat(source.quantificationMeasurements);
      this.measurementValues = MeasurementUtils.toMeasurementValues(measurements);
    }

    if (source.children && (!opts || opts.withChildren !== false)) {
      this.children = source.children.map(child => Batch.fromObject(child, opts));
    }
  }

  equals(other: T): boolean {
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
    return (ReferentialUtils.isNotEmpty(this.taxonName) || ReferentialUtils.isNotEmpty(this.taxonGroup)) && true;
  }
}

// @dynamic
export class BatchUtils {

  static parentToString(parent: Batch, opts?: {
    pmfm?: IPmfm,
    taxonGroupAttributes: string[];
    taxonNameAttributes: string[];
  }): string {
    if (!parent) return null;
    opts = opts || {taxonGroupAttributes: ['label', 'name'], taxonNameAttributes: ['label', 'name']};
    if (opts.pmfm && parent.measurementValues && isNotNil(parent.measurementValues[opts.pmfm.id])) {
      return PmfmValueUtils.valueToString(parent.measurementValues[opts.pmfm.id], {pmfm: opts.pmfm});
    }
    const hasTaxonGroup = ReferentialUtils.isNotEmpty(parent.taxonGroup);
    const hasTaxonName = ReferentialUtils.isNotEmpty(parent.taxonName);
    // Display only taxon name, if no taxon group or same label
    if (hasTaxonName && (!hasTaxonGroup || parent.taxonGroup.label === parent.taxonName.label)) {
      return referentialToString(parent.taxonName, opts.taxonNameAttributes);
    }
    // Display both, if both exists
    if (hasTaxonName && hasTaxonGroup) {
      return referentialToString(parent.taxonGroup, opts.taxonGroupAttributes) + ' / '
        + referentialToString(parent.taxonName, opts.taxonNameAttributes);
    }
    // Display only taxon group
    if (hasTaxonGroup) {
      return referentialToString(parent.taxonGroup, opts.taxonGroupAttributes);
    }

    // Display rankOrder only (should never occur)
    return `#${parent.rankOrder}`;
  }

  static isSampleBatch(batch: Batch) {
    return batch && isNotNilOrBlank(batch.label) && batch.label.endsWith(Batch.SAMPLING_BATCH_SUFFIX);
  }

  static isSampleNotEmpty(sampleBatch: Batch): boolean {
    return isNotNil(sampleBatch.individualCount)
      || isNotNil(sampleBatch.samplingRatio)
      || Object.getOwnPropertyNames(sampleBatch.measurementValues || {})
        .filter(key => isNotNil(sampleBatch.measurementValues[key]))
        .length > 0;
  }

  public static canMergeSubBatch(b1: Batch, b2: Batch, pmfms: IPmfm[]): boolean {
    return EntityUtils.equals(b1.parent, b2.parent, 'label')
      && ReferentialUtils.equals(b1.taxonName, b2.taxonName)
      && MeasurementValuesUtils.equalsPmfms(b1.measurementValues, b2.measurementValues, pmfms);
  }

  public static getAcquisitionLevelFromLabel(batch: Batch): string | undefined {
    if (!batch || !batch.label) return undefined;
    const parts = batch.label.split('#');
    return parts.length > 0 && parts[0];
  }

  static getOrCreateSamplingChild(parent: Batch) {
    const samplingLabel = parent.label + Batch.SAMPLING_BATCH_SUFFIX;

    const samplingChild = (parent.children || []).find(b => b.label === samplingLabel) || new Batch();
    const isNew = !samplingChild.label && true;

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
    const samplingLabel = parent.label + Batch.SAMPLING_BATCH_SUFFIX;
    return (parent.children || []).find(b => b.label === samplingLabel);
  }

  /**
   * Will copy root (species) batch id into subBatch.parentId
   * AND copy the QV sorting measurement hold by direct parent
   * @param rootBatches
   * @param subAcquisitionLevel
   * @param qvPmfm
   */
  static prepareSubBatchesForTable(rootBatches: Batch[], subAcquisitionLevel: string, qvPmfm?: IPmfm): Batch[] {
    if (qvPmfm) {
      return rootBatches.reduce((res, rootBatch) => {
        return res.concat((rootBatch.children || []).reduce((res, qvBatch) => {
          const children = BatchUtils.getChildrenByLevel(qvBatch, subAcquisitionLevel);
          return res.concat(children
            .map(child => {
              // Copy QV value from the root batch
              child.measurementValues = child.measurementValues || {};
              child.measurementValues[qvPmfm.id] = qvBatch.measurementValues[qvPmfm.id];
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
      if (child.label && child.label.startsWith(acquisitionLevel + '#')) return res.concat(child);
      return res.concat(BatchUtils.getChildrenByLevel(child, acquisitionLevel)); // recursive call
    }, []);
  }

  static hasChildrenWithLevel(batch: Batch, acquisitionLevel: string): boolean {
    return batch && (batch.children || []).findIndex(child => {
      return (child.label && child.label.startsWith(acquisitionLevel + '#')) ||
        // If children, recursive call
        (child.children && BatchUtils.hasChildrenWithLevel(child, acquisitionLevel));
    }) !== -1;
  }


  /**
   * Compute individual count, from individual measures
   * @param source
   */
  static computeIndividualCount(source: Batch) {

    if (!source.label || !source.children) return; // skip

    let sumChildrenIndividualCount: number = null;

    source.children.forEach((b, index) => {

      this.computeIndividualCount(b); // Recursive call

      // Update sum of individual count
      if (b.label && b.label.startsWith(AcquisitionLevelCodes.SORTING_BATCH_INDIVIDUAL)) {
        sumChildrenIndividualCount = toNumber(sumChildrenIndividualCount, 0) + toNumber(b.individualCount, 1);
      }
    });

    // Parent batch is a sampling batch: update individual count
    if (BatchUtils.isSampleBatch(source)) {
      source.individualCount = sumChildrenIndividualCount || null;
    }

    // Parent is NOT a sampling batch
    else if (isNotNil(sumChildrenIndividualCount) && source.label.startsWith(AcquisitionLevelCodes.SORTING_BATCH)) {
      if (isNotNil(source.individualCount) && source.individualCount < sumChildrenIndividualCount) {
        console.warn(`[batch-utils] Fix batch {${source.label}} individual count  ${source.individualCount} => ${sumChildrenIndividualCount}`);
        //source.individualCount = childrenIndividualCount;
        source.qualityFlagId = QualityFlagIds.BAD;
      } else if (isNil(source.individualCount) || source.individualCount > sumChildrenIndividualCount) {
        // Create a sampling batch, to hold the sampling individual count
        const samplingBatch = new Batch();
        samplingBatch.label = source.label + Batch.SAMPLING_BATCH_SUFFIX;
        samplingBatch.rankOrder = 1;
        samplingBatch.individualCount = sumChildrenIndividualCount;
        samplingBatch.children = source.children;
        source.children = [samplingBatch];
      }
    }
  }

  static computeRankOrder(source: Batch) {

    if (!source.label || !source.children) return; // skip

    // Sort by id and rankOrder (new batch at the end)
    source.children = source.children
      .sort((b1, b2) => ((b1.id || 0) * 10000 + (b1.rankOrder || 0)) - ((b2.id || 0) * 10000 + (b2.rankOrder || 0)));

    source.children.forEach((b, index) => {
      b.rankOrder = index + 1;

      // Sampling batch
      if (b.label.endsWith(Batch.SAMPLING_BATCH_SUFFIX)) {
        b.label = source.label + Batch.SAMPLING_BATCH_SUFFIX;
      }
      // Individual measure batch
      else if (b.label.startsWith(AcquisitionLevelCodes.SORTING_BATCH_INDIVIDUAL)) {
        b.label = `${AcquisitionLevelCodes.SORTING_BATCH_INDIVIDUAL}#${b.rankOrder}`;
      }

      this.computeRankOrder(b); // Recursive call
    });
  }

  /**
   * Sum individual count, onlly on batch with measure
   * @param batches
   */
  static sumObservedIndividualCount(batches: Batch[]): number {
    return (batches || [])
      .map(b => isNotEmptyArray(b.children) ?
        // Recursive call
        BatchUtils.sumObservedIndividualCount(b.children) :
        // Or get value from individual batches
        b.label.startsWith(AcquisitionLevelCodes.SORTING_BATCH_INDIVIDUAL) ? toNumber(b.individualCount, 1) :
          // Default value, if not an individual batches
          // Use '0' because we want only observed batches count
          0)
      .reduce((sum, individualCount) => {
        return sum + individualCount;
      }, 0);
  }

  static logTree(batch: Batch, opts?: {
    println?: (message: string) => void;
    indent?: string;
    nextIndent?: string;
    showAll?: boolean;
    showParent?: boolean;
    showTaxon?: boolean;
    showMeasure?: boolean;
  }) {
    opts = opts || {};
    const indent = opts && opts.indent || '';
    const nextIndent = opts && opts.nextIndent || indent;
    let message = indent + (batch.label || 'NO_LABEL');

    if (opts.showAll) {
      const excludeKeys = ['label', 'parent', 'children', '__typename'];
      Object.keys(batch)
        .filter(key => !excludeKeys.includes(key) && isNotNil(batch[key]))
        .forEach(key => {
          let value = batch[key];
          if (value instanceof Object) {
            if (!(value instanceof Batch)) {
              value = JSON.stringify(value);
            }
          }
          message += ' ' + key + ':' + value;
        });
    } else {

      if (isNotNil(batch.id)) {
        message += ' id:' + batch.id;
      }

      // Parent
      if (opts.showParent !== false) {
        if (batch.parent) {
          if (isNotNil(batch.parent.id)) {
            message += ' parent.id:' + batch.parent.id;
          } else if (isNotNil(batch.parent.label)) {
            message += ' parent.label:' + batch.parent.label;
          }
        }
        if (isNotNil(batch.parentId)) {
          message += ' parentId:' + batch.parentId;
        }
      }
      // Taxon
      if (opts.showTaxon !== false) {
        if (batch.taxonGroup) {
          message += ' taxonGroup:' + (batch.taxonGroup && (batch.taxonGroup.label || batch.taxonGroup.id));
        }
        if (batch.taxonName) {
          message += ' taxonName:' + (batch.taxonName && (batch.taxonName.label || batch.taxonName.id));
        }
      }
      // Measurement
      if (opts.showMeasure !== false && batch.measurementValues) {
        if (batch.measurementValues[PmfmIds.DISCARD_OR_LANDING]) {
          message += ' discardOrLanding:' + (batch.measurementValues[PmfmIds.DISCARD_OR_LANDING] == QualitativeValueIds.DISCARD_OR_LANDING.LANDING ? 'LAN' : 'DIS');
        }
        if (isNotNil(batch.measurementValues[PmfmIds.LENGTH_TOTAL_CM])) {
          message += ' lengthTotal:' + batch.measurementValues[PmfmIds.LENGTH_TOTAL_CM] + 'cm';
        }
        const weight = batch.measurementValues[PmfmIds.BATCH_ESTIMATED_WEIGHT]
          || batch.measurementValues[PmfmIds.BATCH_ESTIMATED_WEIGHT];
        if (isNotNil(weight)) {
          message += ' weight:' + weight + 'kg';
        }
      }
    }

    // Print
    if (opts.println) opts.println(message);
    else console.debug(message);

    const childrenCount = batch.children && batch.children.length || 0;
    if (childrenCount > 0) {
      batch.children.forEach((b, index,) => {
        const childOpts = (index === childrenCount - 1) ? {
          println: opts.println,
          indent: nextIndent + ' \\- ',
          nextIndent: nextIndent + '\t'
        } : {
          println: opts.println,
          indent: nextIndent + ' |- '
        };

        this.logTree(b, childOpts); // Loop
      });
    }
  }


}
