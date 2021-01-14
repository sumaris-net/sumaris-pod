import {Department, Entity, fromDateISOString, isNotNil, toDateISOString} from "../../../core/core.module";
import {Moment} from "moment/moment";
import {ReferentialAsObjectOptions} from "../../../core/services/model/referential.model";
import {IWithRecorderDepartmentEntity} from "./model.utils";


export interface DataEntityAsObjectOptions extends ReferentialAsObjectOptions {
  keepSynchronizationStatus?: boolean;

  keepRemoteId?: boolean; // Allow to clean id (e.g. when restoring entities from trash)
  keepUpdateDate?: boolean; // Allow to clean updateDate (e.g. when restoring entities from trash)
}

export const SAVE_OPTIMISTIC_AS_OBJECT_OPTIONS = Object.freeze(<DataEntityAsObjectOptions>{
  minify: false,
  keepTypename: true,
  keepEntityName: true,
  keepLocalId: true,
  keepSynchronizationStatus: true
});
export const SAVE_LOCALLY_AS_OBJECT_OPTIONS = Object.freeze(<DataEntityAsObjectOptions>{
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
  ...SAVE_LOCALLY_AS_OBJECT_OPTIONS,
  keepLocalId: false,
  keepRemoteId: false,
  keepUpdateDate: false
});
export const CLONE_AS_OBJECT_OPTIONS = Object.freeze(<DataEntityAsObjectOptions>{
  ...SAVE_LOCALLY_AS_OBJECT_OPTIONS,
  minify: false
});

export abstract class DataEntity<T extends DataEntity<any>, O extends DataEntityAsObjectOptions = DataEntityAsObjectOptions, F = any>
  extends Entity<T, O>
  implements IWithRecorderDepartmentEntity<T> {

  recorderDepartment: Department;
  controlDate: Moment;
  qualificationDate: Moment;
  qualificationComments: string;
  qualityFlagId: number;

  protected constructor() {
    super();
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

  fromObject(source: any, opts?: F) {
    super.fromObject(source);
    this.recorderDepartment = source.recorderDepartment && Department.fromObject(source.recorderDepartment);
    this.controlDate = fromDateISOString(source.controlDate);
    this.qualificationDate = fromDateISOString(source.qualificationDate);
    this.qualificationComments = source.qualificationComments;
    this.qualityFlagId = source.qualityFlagId;
  }
}

