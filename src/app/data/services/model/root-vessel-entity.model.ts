import {NOT_MINIFY_OPTIONS} from "../../../core/services/model/referential.model";
import {RootDataEntity} from "./root-data-entity.model";
import {DataEntityAsObjectOptions} from "./data-entity.model";
import {IWithVesselSnapshotEntity, VesselSnapshot} from "../../../referential/services/model/vessel-snapshot.model";


export abstract class DataRootVesselEntity<T extends DataRootVesselEntity<any>, O extends DataEntityAsObjectOptions = DataEntityAsObjectOptions, F = any>
  extends RootDataEntity<T, O, F> implements IWithVesselSnapshotEntity<T> {

  vesselSnapshot: VesselSnapshot;

  protected constructor() {
    super();
    this.vesselSnapshot = null;
  }

  asObject(options?: O): any {
    const target = super.asObject(options);
    target.vesselSnapshot = this.vesselSnapshot && this.vesselSnapshot.asObject({ ...options, ...NOT_MINIFY_OPTIONS }) || undefined;
    return target;
  }

  fromObject(source: any, opts?: F) {
    super.fromObject(source);
    this.vesselSnapshot = source.vesselSnapshot && VesselSnapshot.fromObject(source.vesselSnapshot);
  }
}
