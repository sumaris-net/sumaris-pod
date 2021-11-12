import {
  EntityClass,
  fromDateISOString,
  isNil,
  isNotEmptyArray,
  isNotNil,
  ITreeItemEntity,
  ReferentialAsObjectOptions,
  ReferentialRef,
  referentialToString,
  ReferentialUtils,
  toDateISOString
} from '@sumaris-net/ngx-components';
import {Moment} from 'moment';
import {DataEntityAsObjectOptions} from '@app/data/services/model/data-entity.model';
import { IEntityWithMeasurement, MeasurementFormValues, MeasurementModelValues, MeasurementUtils, MeasurementValuesUtils } from './measurement.model';
import {TaxonGroupRef} from '@app/referential/services/model/taxon-group.model';
import {RootDataEntity} from '@app/data/services/model/root-data-entity.model';
import {IPmfm} from '@app/referential/services/model/pmfm.model';
import {NOT_MINIFY_OPTIONS} from '@app/core/services/model/referential.model';
import { TaxonNameRef } from "@app/referential/services/model/taxon-name.model";

export interface SampleAsObjectOptions extends DataEntityAsObjectOptions {
  withChildren?: boolean;
}
export interface SampleFromObjectOptions {
  withChildren?: boolean;
}
@EntityClass({typename: 'SampleVO'})
export class Sample extends RootDataEntity<Sample, number, SampleAsObjectOptions, SampleFromObjectOptions>
  implements IEntityWithMeasurement<Sample>, ITreeItemEntity<Sample>{

  static fromObject: (source, opts?: SampleFromObjectOptions) => Sample;

  /**
   * Transform a samples tree, into a array of object.
   * Parent & children links are removed, to keep only a parentId
   * @param sources
   * @param opts
   * @throw Error if a sample has no id
   */
  static treeAsObjectArray(sources: Sample[],
                           opts?: DataEntityAsObjectOptions & {
                             parent?: any;
                           }): any[] {
    return sources && sources
      // Reduce to array
      .reduce((res, source) => {
        // Convert entity into object, WITHOUT children (will be add later)
        const target = source.asObject ? source.asObject({...opts, withChildren: false}) : {...source, children: undefined};

        // Link target with the given parent
        const parent = opts && opts.parent;
        if (parent) {
          if (isNil(parent.id)) {
            throw new Error(`Cannot convert sample tree into array: No id found for sample ${parent.label}!`);
          }
          target.parentId = parent.id;
          delete target.parent; // not need
        }

        if (isNotEmptyArray(source.children)) {
          return res.concat(target)
            .concat(...this.treeAsObjectArray(source.children, {...opts, parent: target}));
        }
        return res.concat(target);
      }, []) || undefined;
  }

  static equals(s1: Sample | any, s2: Sample | any): boolean {
    return s1 && s2 && s1.id === s2.id
      || (s1.rankOrder === s2.rankOrder
        // same operation
        && ((!s1.operationId && !s2.operationId) || s1.operationId === s2.operationId)
        // same label
        && ((!s1.label && !s2.label) || s1.label === s2.label)
        // Warn: compare using the parent ID is too complicated
      );
  }

  label: string = null;
  rankOrder: number = null;
  sampleDate: Moment = null;
  individualCount: number = null;
  taxonGroup: TaxonGroupRef  = null;
  taxonName: ReferentialRef = null;
  measurementValues: MeasurementModelValues | MeasurementFormValues = {};
  matrixId: number = null;
  batchId: number = null;
  size: number = null;
  sizeUnit: string = null;

  operationId: number = null;
  landingId: number = null;

  parentId: number = null;
  parent: Sample = null;
  children: Sample[] = null;

  constructor() {
    super(Sample.TYPENAME);
  }

  asObject(opts?: SampleAsObjectOptions): any {
    const target = super.asObject(opts);
    target.sampleDate = toDateISOString(this.sampleDate);
    target.taxonGroup = this.taxonGroup && this.taxonGroup.asObject({ ...opts, ...NOT_MINIFY_OPTIONS, keepEntityName: true /*fix #32*/} as ReferentialAsObjectOptions) || undefined;
    target.taxonName = this.taxonName && this.taxonName.asObject({ ...opts, ...NOT_MINIFY_OPTIONS, keepEntityName: true /*fix #32*/} as ReferentialAsObjectOptions) || undefined;
    target.individualCount = isNotNil(this.individualCount) ? this.individualCount : null;
    target.parentId = this.parentId || this.parent && this.parent.id || undefined;
    target.children = this.children && (!opts || opts.withChildren !== false) && this.children.map(c => c.asObject(opts)) || undefined;
    target.measurementValues = MeasurementValuesUtils.asObject( this.measurementValues, opts);
    target.landingId = this.landingId;
    target.operationId = this.operationId;

    if (opts && opts.minify) {
      // Parent not need, as the tree will be used by pod
      delete target.parent;
      delete target.parentId;
    }

    return target;
  }

  fromObject(source: any, opts?: SampleFromObjectOptions): Sample {
    super.fromObject(source);
    this.label = source.label;
    this.rankOrder = source.rankOrder;
    this.sampleDate = fromDateISOString(source.sampleDate);
    this.individualCount = isNotNil(source.individualCount) && source.individualCount !== "" ? source.individualCount : null;
    this.taxonGroup = source.taxonGroup && TaxonGroupRef.fromObject(source.taxonGroup) || undefined;
    this.taxonName = source.taxonName && TaxonNameRef.fromObject(source.taxonName) || undefined;
    this.size = source.size;
    this.sizeUnit = source.sizeUnit;
    this.matrixId = source.matrixId;
    this.parentId = source.parentId;
    this.parent = source.parent;
    this.batchId = source.batchId;
    this.operationId = source.operationId;
    this.landingId = source.landingId;
    this.measurementValues = source.measurementValues && { ...source.measurementValues } || MeasurementUtils.toMeasurementValues(source.measurements);

    if (source.children && (!opts || opts.withChildren !== false)) {
      this.children = source.children.map(child => Sample.fromObject(child, opts));
    }

    return this;
  }

  equals(other: Sample): boolean {
    return super.equals(other)
      || (this.rankOrder === other.rankOrder
        // same operation
        && ((!this.operationId && !other.operationId) || this.operationId === other.operationId)
        // same landing
        && ((!this.landingId && !other.landingId) || this.landingId === other.landingId)
        // same label
        && ((!this.label && !other.label) || this.label === other.label)
        // Warn: compare using the parent ID is too complicated
      );
  }
}

export class SampleUtils {

  static parentToString(parent: Sample, opts?: {
    pmfm?: IPmfm,
    taxonGroupAttributes: string[];
    taxonNameAttributes: string[];
  }) {
    if (!parent) return null;
    opts = opts || {taxonGroupAttributes: ['label', 'name'], taxonNameAttributes: ['label', 'name']};
    if (opts.pmfm && parent.measurementValues && isNotNil(parent.measurementValues[opts.pmfm.id])) {
      return parent.measurementValues[opts.pmfm.id];
    }

    const hasTaxonGroup = ReferentialUtils.isNotEmpty(parent.taxonGroup) ;
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
}
