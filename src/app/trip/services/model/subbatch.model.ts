import {Batch, BatchAsObjectOptions, BatchFromObjectOptions, BatchUtils} from "./batch.model";
import {BatchGroup} from "./batch-group.model";
import {AcquisitionLevelCodes} from "../../../referential/services/model/model.enum";
import {PmfmStrategy} from "../../../referential/services/model/pmfm-strategy.model";
import {ReferentialRef} from "../../../core/services/model/referential.model";

export class SubBatch extends Batch<SubBatch> {

  // The parent group (can be != parent)
  parentGroup: BatchGroup;

  static fromBatch(source: Batch, parentGroup: BatchGroup): SubBatch {
    if (!source || !parentGroup) throw new Error("Missing argument 'source' or 'parentGroup'");
    const target = new SubBatch();
    Object.assign(target, source);
    // Find the group
    target.parentGroup = parentGroup;

    return target;
  }

  static fromObject(source: any, opts?: BatchFromObjectOptions): BatchGroup {
    const target = new BatchGroup();
    target.fromObject(source, opts);
    return target;
  }

  constructor() {
    super();
  }

  clone(): SubBatch {
    const target = new SubBatch();
    target.fromObject(this.asObject());
    return target;
  }

  asObject(opts?: BatchAsObjectOptions): any {
    const target = super.asObject(opts);
    if (opts && opts.minify === true) {
      delete target.parentGroup;
    }
    return target;
  }

  fromObject(source: any, opts?: BatchFromObjectOptions) {
    super.fromObject(source, opts);
    this.parentGroup = source.parentGroup;
  }
}

export class SubBatchUtils {

  static fromBatchGroups(
    groups: BatchGroup[],
    opts?: {
      groupQvPmfm?: PmfmStrategy
    }
  ): SubBatch[] {
    opts = opts || {};

    if (!opts.groupQvPmfm) {
      return groups.reduce((res, group) => {
        return res.concat(BatchUtils.getChildrenByLevel(group, AcquisitionLevelCodes.SORTING_BATCH_INDIVIDUAL)
          .map(child => SubBatch.fromBatch(child, group)));
      }, []);
    }
    // if need to copy QV pmfm's value
    else {
      return groups.reduce((res, group) => {
        return res.concat((group.children || []).reduce((res, qvBatch) => {
          const children = BatchUtils.getChildrenByLevel(qvBatch, AcquisitionLevelCodes.SORTING_BATCH_INDIVIDUAL);
          return res.concat(children
            .map(child => {
              const target = SubBatch.fromBatch(child, group);
              // Copy QV value
              target.measurementValues = { ...target.measurementValues };
              target.measurementValues[opts.groupQvPmfm.pmfmId] = qvBatch.measurementValues[opts.groupQvPmfm.pmfmId];

              return target;
            }));
        }, []));
      }, []);
    }
  }

  /**
   * Make sure each subbatch.parentGroup use a reference found inside the groups arrays
   * @param groups
   * @param subBatches
   */
  static linkSubBatchesToGroup(groups: BatchGroup[], subBatches: SubBatch[]) {
    if (!groups || !subBatches) return;

    subBatches.forEach(s => {
      s.parentGroup = s.parentGroup && groups.find(p => Batch.equals(p, s.parentGroup)) || null;
      if (!s.parentGroup) console.warn("linkSubBatchesToGroup() - Could not found parent group, for sub-batch:", s);
    });
  }

  /**
   * Prepare subbatches for model (set the subbatch.parent)
   * @param batchGroups
   * @param subBatches
   * @param opts
   */
  static linkSubBatchesToParent(batchGroups: BatchGroup[], subBatches: SubBatch[], opts?: {
    qvPmfm?: PmfmStrategy;
  }) {
    opts = opts || {};

    if (!opts.qvPmfm) {
      (batchGroups || []).forEach(parent => {
        // Find subbatches, from parentGroup
        const children = subBatches.filter(sb => sb.parentGroup && Batch.equals(parent, sb.parentGroup));

        // If has sampling batch, use it as parent
        if (parent.children && parent.children.length === 1 && BatchUtils.isSampleBatch(parent.children[0])) {
          parent = parent.children[0] as BatchGroup;
        }

        parent.children = children;
        children.forEach(c => {
          c.parentId = parent.id;
          c.parent = undefined;
        });
      });
    }

    else {
      const qvPmfmId = opts.qvPmfm.pmfmId;
      (batchGroups || []).forEach(batchGroup => {
        // Get group's sub batches
        const groupSubBatches = (subBatches || []).filter(sb => sb.parentGroup && Batch.equals(batchGroup, sb.parentGroup));

        // Get group's children (that should hold a QV pmfm's value)
        (batchGroup.children || []).forEach(parent => {
          // Find sub batches for this QV pmfm's value
          const children = groupSubBatches.filter(sb => {
            let qvValue = sb.measurementValues[qvPmfmId];
            if (qvValue instanceof ReferentialRef) qvValue = qvValue.id;
            return qvValue == parent.measurementValues[qvPmfmId];
          });

          // If has sampling batch, use it as parent
          if (parent.children && parent.children.length === 1 && BatchUtils.isSampleBatch(parent.children[0])) {
            parent = parent.children[0];
          }

          // Link to parent
          parent.children = children;
          children.forEach(c => {
            c.parentId = parent.id;
            c.parent = undefined; // Not need for model serialization
          });
        });
      });
    }

    return batchGroups;
  }

}
