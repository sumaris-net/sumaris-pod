import {Moment} from "moment";
import {ReferentialAsObjectOptions}  from "@sumaris-net/ngx-components";
import { IWithRecorderDepartmentEntity, SynchronizationStatus } from './model.utils';
import {Entity}  from "@sumaris-net/ngx-components";
import {Department}  from "@sumaris-net/ngx-components";
import {fromDateISOString, toDateISOString} from "@sumaris-net/ngx-components";
import {isNotNil} from "@sumaris-net/ngx-components";


export interface DataEntityAsObjectOptions extends ReferentialAsObjectOptions {
  keepSynchronizationStatus?: boolean;

  keepRemoteId?: boolean; // Allow to clean id (e.g. when restoring entities from trash)
  keepUpdateDate?: boolean; // Allow to clean updateDate (e.g. when restoring entities from trash)
}

export const SERIALIZE_FOR_OPTIMISTIC_RESPONSE = Object.freeze(<DataEntityAsObjectOptions>{
  minify: false,
  keepTypename: true,
  keepEntityName: true,
  keepLocalId: true,
  keepSynchronizationStatus: true
});
export const MINIFY_DATA_ENTITY_FOR_LOCAL_STORAGE = Object.freeze(<DataEntityAsObjectOptions>{
  minify: true,
  keepTypename: true,
  keepEntityName: true,
  keepLocalId: true,
  keepSynchronizationStatus: true
});

export const SAVE_AS_OBJECT_OPTIONS = Object.freeze(<DataEntityAsObjectOptions>{
  minify: true,
  keepTypename: false,
  keepEntityName: false,
  keepLocalId: false,
  keepSynchronizationStatus: false
});
export const COPY_LOCALLY_AS_OBJECT_OPTIONS = Object.freeze(<DataEntityAsObjectOptions>{
  ...MINIFY_DATA_ENTITY_FOR_LOCAL_STORAGE,
  keepLocalId: false,
  keepRemoteId: false,
  keepUpdateDate: false
});
export const CLONE_AS_OBJECT_OPTIONS = Object.freeze(<DataEntityAsObjectOptions>{
  ...MINIFY_DATA_ENTITY_FOR_LOCAL_STORAGE,
  minify: false
});


export abstract class DataEntity<
  T extends DataEntity<T, ID, O>,
  ID = number,
  O extends DataEntityAsObjectOptions = DataEntityAsObjectOptions,
  FO = any>
  extends Entity<T, ID, O>
  implements IWithRecorderDepartmentEntity<T, ID> {

  recorderDepartment: Department;
  controlDate: Moment;
  qualificationDate: Moment;
  qualificationComments: string;
  qualityFlagId: number;

  protected constructor(__typename?: string) {
    super(__typename);
    this.recorderDepartment = null;
  }

  asObject(opts?: O): any {
    const target = super.asObject(opts);
    if (opts && opts.keepRemoteId === false && target.id >= 0) delete target.id;
    if (opts && opts.keepUpdateDate === false && target.id >= 0) delete target.updateDate;
    target.recorderDepartment = this.recorderDepartment && this.recorderDepartment.asObject(opts) || undefined;
    target.controlDate = toDateISOString(this.controlDate);
    target.qualificationDate = toDateISOString(this.qualificationDate);
    target.qualificationComments = this.qualificationComments || undefined;
    target.qualityFlagId = isNotNil(this.qualityFlagId) ? this.qualityFlagId : undefined;
    return target;
  }

  fromObject(source: any, opts?: FO) {
    super.fromObject(source);
    this.recorderDepartment = source.recorderDepartment && Department.fromObject(source.recorderDepartment);
    this.controlDate = fromDateISOString(source.controlDate);
    this.qualificationDate = fromDateISOString(source.qualificationDate);
    this.qualificationComments = source.qualificationComments;
    this.qualityFlagId = source.qualityFlagId;
  }
}

