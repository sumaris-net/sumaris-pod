import {EntityUtils, FormArrayHelper, isNil, isNotNil, referentialToString} from "../../../core/core.module";
import {AcquisitionLevelCodes, PmfmStrategy, ReferentialRef} from "../../../referential/referential.module";
import {DataEntity, DataEntityAsObjectOptions, NOT_MINIFY_OPTIONS} from "./base.model";
import {IEntityWithMeasurement, IMeasurementValue, MeasurementUtils, MeasurementValuesUtils} from "./measurement.model";
import {isNilOrBlank, isNotNilOrBlank} from "../../../shared/functions";
import {AbstractControl, FormBuilder, FormGroup} from "@angular/forms";
import {TaxonNameRef} from "../../../referential/services/model/taxon.model";
import {ITreeItemEntity, ReferentialAsObjectOptions} from "../../../core/services/model";

export declare interface BatchWeight extends IMeasurementValue {
  unit?: 'kg';
}


export class Batch extends DataEntity<Batch> implements IEntityWithMeasurement<Batch>, ITreeItemEntity<Batch> {

  static TYPENAME = 'BatchVO';
  static SAMPLE_BATCH_SUFFIX = '.%';

  static fromObject(source: any, opts?: { withChildren: boolean; }): Batch {
    const target = new Batch();
    target.fromObject(source, opts);
    return target;
  }

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
    const target = source.asObject({...opts, withChildren: false});

    // Link target with the given parent
    const parent = opts && opts.parent;
    if (parent) {
      if (isNil(parent.id)) {
        throw new Error(`Cannot convert batch tree into array: No id found for batch ${parent.label}!`);
      }
      target.parentId = parent.id;
      delete target.parent; // not need
    }

    return (source.children || []).reduce((res, batch) => {
        return res.concat(this.treeAsObjectArray(batch, {...opts, parent: target}) || []);
      },
      [target]) || undefined;
  }

  public static equals(b1: Batch | any, b2: Batch | any): boolean {
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

  label: string;
  rankOrder: number;
  exhaustiveInventory: boolean;
  samplingRatio: number;
  samplingRatioText: string;
  individualCount: number;
  taxonGroup: ReferentialRef;
  taxonName: TaxonNameRef;
  comments: string;
  measurementValues: { [key: number]: any };
  weight: BatchWeight;

  operationId: number;
  parentId: number;
  parent: Batch;
  children: Batch[];

  constructor() {
    super();
    this.__typename = Batch.TYPENAME;
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

  asObject(opts?: DataEntityAsObjectOptions & {withChildren?: boolean}): any {
    const parent = this.parent;
    this.parent = null; // avoid parent conversion
    const target = super.asObject(opts);
    delete target.parentBatch;
    this.parent = parent;

    target.taxonGroup = this.taxonGroup && this.taxonGroup.asObject({ ...opts, ...NOT_MINIFY_OPTIONS, keepEntityName: true /*fix #32*/ } as ReferentialAsObjectOptions) || undefined;
    target.taxonName = this.taxonName && this.taxonName.asObject({ ...opts, ...NOT_MINIFY_OPTIONS, keepEntityName: true /*fix #32*/ } as ReferentialAsObjectOptions) || undefined;
    target.samplingRatio = isNotNil(this.samplingRatio) ? this.samplingRatio : null;
    target.individualCount = isNotNil(this.individualCount) ? this.individualCount : null;
    target.children = this.children && (!opts || opts.withChildren !== false) && this.children.map(c => c.asObject(opts)) || undefined;
    target.parentId = this.parentId || this.parent && this.parent.id || undefined;
    target.measurementValues = MeasurementValuesUtils.asObject(this.measurementValues, opts);

    if (opts && opts.minify) {
      // Parent Id not need, as the tree batch will be used by pod
      delete target.parent;
      delete target.parentId;
      // Remove computed properties
      delete target.weight;
    }

    return target;
  }

  fromObject(source: any, opts?: { withChildren: boolean; }): Batch {
    super.fromObject(source);
    this.label = source.label;
    this.rankOrder = +source.rankOrder;
    this.exhaustiveInventory = source.exhaustiveInventory;
    this.samplingRatio = isNotNilOrBlank(source.samplingRatio) ? parseFloat(source.samplingRatio) : null;
    this.samplingRatioText = source.samplingRatioText;
    this.individualCount = isNotNilOrBlank(source.individualCount) ? parseInt(source.individualCount) : null;
    this.taxonGroup = source.taxonGroup && ReferentialRef.fromObject(source.taxonGroup) || undefined;
    this.taxonName = source.taxonName && TaxonNameRef.fromObject(source.taxonName) || undefined;
    this.comments = source.comments;
    this.operationId = source.operationId;
    this.parentId = source.parentId;
    this.parent = source.parent;

    this.weight = source.weight || undefined;

    if (source.measurementValues) {
      this.measurementValues = source.measurementValues;
    }
    // Convert measurement lists to map
    else if (source.sortingMeasurements || source.quantificationMeasurements) {
      const measurements = (source.sortingMeasurements || []).concat(source.quantificationMeasurements);
      this.measurementValues = MeasurementUtils.toMeasurementValues(measurements);
    }

    if (source.children && opts && opts.withChildren) {
      this.children = source.children.map(child => Batch.fromObject(child, opts));
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

  static parentToString(parent: Batch, opts?: {
    pmfm?: PmfmStrategy,
    taxonGroupAttributes: string[];
    taxonNameAttributes: string[];
  }): string {
    if (!parent) return null;
    opts = opts || {taxonGroupAttributes: ['label', 'name'], taxonNameAttributes: ['label', 'name']};
    if (opts.pmfm && parent.measurementValues && isNotNil(parent.measurementValues[opts.pmfm.pmfmId])) {
      return MeasurementValuesUtils.valueToString(parent.measurementValues[opts.pmfm.pmfmId], opts.pmfm);
    }
    const hasTaxonGroup = EntityUtils.isNotEmpty(parent.taxonGroup);
    const hasTaxonName = EntityUtils.isNotEmpty(parent.taxonName);
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

  public static getAcquisitionLevelFromLabel(batch: Batch): string|undefined {
    if (!batch || !batch.label) return undefined;
    const parts = batch.label.split('#');
    return parts.length > 0 && parts[0];
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
        return res.concat((rootBatch.children || []).reduce((res, qvBatch) => {
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

  static hasChildrenWithLevel(batch: Batch, acquisitionLevel: string): boolean {
    return batch && (batch.children || []).findIndex(child => {
      return (child.label && child.label.startsWith(acquisitionLevel + "#")) || 
        // If children, recursive call
        (child.children && BatchUtils.hasChildrenWithLevel(child, acquisitionLevel));
    }) !== -1;
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

  static normalizeEntityToForm(batch: Batch,
                               pmfms: PmfmStrategy[],
                               opts?: {
                                 withChildren: boolean;
                                 form: FormGroup;
                                 formBuilder: FormBuilder;
                                 createForm: (value?: Batch) => AbstractControl;
                                 onlyExistingPmfms?: boolean;
                               }) {
    if (!batch) return;

    MeasurementValuesUtils.normalizeEntityToForm(batch, pmfms, opts && opts.form, {
      onlyExistingPmfms: opts && opts.onlyExistingPmfms
    });

    if (batch.children && opts && opts.withChildren) {
      const childrenFormHelper = new FormArrayHelper<Batch>(
        opts.formBuilder,
        opts.form,
        'children',
        (value) => opts.createForm(value),
        (v1, v2) => false /*comparision not need*/,
        (value) => isNil(value),
        {allowEmptyArray: true}
      );
      childrenFormHelper.resize(batch.children.length);

      batch.children.forEach((child, index) => {
        // Recursive call, on each children
        BatchUtils.normalizeEntityToForm(child, pmfms,
          Object.assign(opts, {form: childrenFormHelper.at(index) as FormGroup}));
      });
    }
  }

  static logTree(batch: Batch, opts?: {indent?: string; nextIndent?: string}) {
    const indent = opts && opts.indent || '';
    const nextIndent = opts && opts.nextIndent || indent;
    let message = indent + batch.label + ' id=' + batch.id;
    if (batch.parent || isNotNil(batch.parentId)) {
      message += ' - parentId=' + (batch.parent && batch.parent.id || batch.parentId);
    }
    console.debug(message);
    const childrenCount = batch.children && batch.children.length || 0;
    if (childrenCount) {
      batch.children.forEach((b, index, ) => {
        const opts = (index === childrenCount - 1) ? {
          indent: nextIndent + ' \\- ',
          nextIndent: nextIndent + '   '
        } : {
          indent: nextIndent + ' |- '
        };

        this.logTree(b, opts);
      });
    }
  }

}
