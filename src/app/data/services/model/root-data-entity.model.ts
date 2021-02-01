import {ReferentialRef} from "../../../core/services/model/referential.model";
import {Person} from "../../../core/services/model/person.model";
import {Moment} from "moment";

import {NOT_MINIFY_OPTIONS, ReferentialAsObjectOptions} from "../../../core/services/model/referential.model";
import {DataEntity, DataEntityAsObjectOptions} from "./data-entity.model";
import {IWithProgramEntity, IWithRecorderPersonEntity} from "./model.utils";
import {EntityUtils} from "../../../core/services/model/entity.model";
import {fromDateISOString, toDateISOString} from "../../../shared/dates";
import {isNil} from "../../../shared/functions";

export type SynchronizationStatus = 'DIRTY' | 'READY_TO_SYNC' | 'SYNC' | 'DELETED';
export const SynchronizationStatusEnum = {
  DIRTY: <SynchronizationStatus>'DIRTY',
  READY_TO_SYNC: <SynchronizationStatus>'READY_TO_SYNC',
  SYNC: <SynchronizationStatus>'SYNC',
  DELETED: <SynchronizationStatus>'DELETED'
};

export abstract class RootDataEntity<T extends RootDataEntity<any>, O extends DataEntityAsObjectOptions = DataEntityAsObjectOptions, F = any>
  extends DataEntity<T, O, F>
  implements IWithRecorderPersonEntity<T>, IWithProgramEntity<T> {

  creationDate: Moment;
  validationDate: Moment;
  comments: string = null;
  recorderPerson: Person;
  program: ReferentialRef;
  synchronizationStatus?: SynchronizationStatus;

  protected constructor() {
    super();
    this.creationDate = null;
    this.validationDate = null;
    this.comments = null;
    this.recorderPerson = null;
    this.program = null;
  }

  asObject(options?: O): any {
    const target = super.asObject(options);
    target.creationDate = toDateISOString(this.creationDate);
    target.validationDate = toDateISOString(this.validationDate);
    target.recorderPerson = this.recorderPerson && this.recorderPerson.asObject({ ...options, ...NOT_MINIFY_OPTIONS /*always keep for table*/ } as ReferentialAsObjectOptions) || undefined;
    target.program = this.program && this.program.asObject({ ...options, ...NOT_MINIFY_OPTIONS /*always keep for table*/ } as ReferentialAsObjectOptions) || undefined;
    if (options && options.minify) {
      if (target.program) delete target.program.entityName;
      if (options.keepSynchronizationStatus !== true) {
        delete target.synchronizationStatus; // Remove by default, when minify, because not exists on pod's model
      }
    }
    return target;
  }


  fromObject(source: any, opts?: F) {
    super.fromObject(source, opts);
    this.comments = source.comments;
    this.creationDate = fromDateISOString(source.creationDate);
    this.validationDate = fromDateISOString(source.validationDate);
    this.recorderPerson = source.recorderPerson && Person.fromObject(source.recorderPerson);
    this.program = source.program && ReferentialRef.fromObject(source.program);
    this.synchronizationStatus = source.synchronizationStatus;
  }
}

export abstract class DataRootEntityUtils {

  static copyControlAndValidationDate(source: RootDataEntity<any> | undefined, target: RootDataEntity<any>) {
    if (!source) return;

    // Update (id and updateDate)
    target.controlDate = fromDateISOString(source.controlDate);
    target.validationDate = fromDateISOString(source.validationDate);

  }

  static copyQualificationDateAndFlag = EntityUtils.copyQualificationDateAndFlag;

  static isLocal(entity: RootDataEntity<any>): boolean {
    return entity && (isNil(entity.id) ? (entity.synchronizationStatus && entity.synchronizationStatus !== 'SYNC') : entity.id < 0);
  }

  static isRemote(entity: RootDataEntity<any>): boolean {
    return entity && !DataRootEntityUtils.isLocal(entity);
  }

}
