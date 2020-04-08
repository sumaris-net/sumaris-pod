import {EntityUtils, FormArrayHelper, isNil, isNotNil, referentialToString} from "../../../core/core.module";
import {AcquisitionLevelCodes, PmfmStrategy, ReferentialRef} from "../../../referential/referential.module";
import {DataEntity, DataEntityAsObjectOptions, NOT_MINIFY_OPTIONS} from "./base.model";
import {IEntityWithMeasurement, IMeasurementValue, MeasurementUtils, MeasurementValuesUtils} from "./measurement.model";
import {isNilOrBlank, isNotNilOrBlank} from "../../../shared/functions";
import {AbstractControl, FormBuilder, FormGroup} from "@angular/forms";
import {TaxonNameRef} from "../../../referential/services/model/taxon.model";
import {ITreeItemEntity, ReferentialAsObjectOptions} from "../../../core/services/model";
import {Batch} from "./batch.model";

export class BatchGroup extends Batch {

  // Number of individual observed (by individual measure)
  observedIndividualCount: number;

  static fromBatch(batch: Batch): BatchGroup {
    const target = new BatchGroup();
    Object.assign(target, batch);
    // Compute observed indiv. count
    target.observedIndividualCount = BatchGroupUtils.getObservedIndividualCount(target);
    return target;
  }

  static fromObject(source: any, opts?: { withChildren: boolean; }): BatchGroup {
    const target = new BatchGroup();
    target.fromObject(source, opts);

    // Compute observed indiv. count
    target.observedIndividualCount = BatchGroupUtils.getObservedIndividualCount(target);

    return target;
  }

  constructor() {
    super();
  }

  clone(): BatchGroup {
    const target = new BatchGroup();
    target.fromObject(this.asObject());
    return target;
  }

  fromObject(source: any, opts?: { withChildren: boolean; }): BatchGroup {
    super.fromObject(source);
    this.observedIndividualCount = source.indirectIndividualCount;
    return this;
  }

  asObject(opts?: DataEntityAsObjectOptions & { withChildren?: boolean }): any {
    const target = super.asObject(opts);
    if (opts && opts.minify === true) {
      delete target.indirectIndividualCount;
    }
    return target;
  }
}

export class BatchGroupUtils {
  /**
   * Count only individual count with measure
   * @param batch
   */
  static computeObservedIndividualCount(batch: BatchGroup) {

    // Compute observed indiv. count
    batch.observedIndividualCount = BatchGroupUtils.getObservedIndividualCount(batch);
  }

  /**
   * Count only individual count with measure
   * @param batch
   */
  static getObservedIndividualCount(batch: BatchGroup|Batch): number {

    let count: number = undefined;

    if (batch.children && batch.children.length ) {
      // Reset counter
      count = 0;
      batch.children
        .map(BatchGroupUtils.getObservedIndividualCount) // recursive call
        .forEach(childCount => {
          count += (childCount || 0);
        });
    }

    return count;
  }
}
