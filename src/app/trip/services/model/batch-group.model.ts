import {Batch, BatchAsObjectOptions, BatchFromObjectOptions, BatchUtils} from "./batch.model";
import {AcquisitionLevelCodes} from "../../../referential/services/model/model.enum";
import { EntityClass, EntityUtils } from '@sumaris-net/ngx-components';

@EntityClass({typename: "BatchGroupVO", fromObjectReuseStrategy: "clone"})
export class BatchGroup extends Batch<BatchGroup> {

  static fromObject: (source: any, opts?: BatchFromObjectOptions) => BatchGroup;

  // Number of individual observed (by individual measure)
  observedIndividualCount: number;

  static fromBatch(batch: Batch): BatchGroup {
    const target = new BatchGroup();
    Object.assign(target, batch);
    // Compute observed indiv. count
    target.observedIndividualCount = BatchUtils.sumObservedIndividualCount(batch.children);
    return target;
  }

  constructor() {
    super(BatchGroup.TYPENAME);
  }

  asObject(opts?: BatchAsObjectOptions): any {
    const target = super.asObject(opts);
    if (opts && opts.minify === true) {
      delete target.observedIndividualCount;
    }
    return target;
  }

  fromObject(source: any, opts?: BatchFromObjectOptions) {
    super.fromObject(source, opts);
    this.observedIndividualCount = source.observedIndividualCount;
  }
}

export class BatchGroupUtils {

  static fromBatchTree(catchBatch: Batch): BatchGroup[] {

    // Retrieve batch group (make sure label start with acquisition level)
    // Then convert into batch group entities
    return (catchBatch.children || [])
      .filter(s => s.label && s.label.startsWith(AcquisitionLevelCodes.SORTING_BATCH + "#"))
      // Convert to Batch Group
      .map(BatchGroup.fromBatch);
  }

  /**
   * Count only individual count with measure
   * @param batch
   */
  static computeObservedIndividualCount(batch: BatchGroup) {

    // Compute observed indiv. count
    batch.observedIndividualCount = BatchUtils.sumObservedIndividualCount(batch.children);
  }

  /**
   * Check equality of BatchGroup
   * @param batchGroup1
   * @param batchGroup2
   */
  static equals(batchGroup1: BatchGroup, batchGroup2: BatchGroup) {
    return EntityUtils.equals(batchGroup1, batchGroup2, 'rankOrder')
      && EntityUtils.equals(batchGroup1, batchGroup2, 'parentId');
  }
}
