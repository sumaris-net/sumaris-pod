import {EntityFilter} from "../../../core/services/model/filter.model";
import {DataEntity} from "./data-entity.model";
import {Department} from "../../../core/services/model/department.model";
import {isNotNil} from "../../../shared/functions";
import {FilterFn} from "../../../shared/services/entity-service.class";
import {EntityAsObjectOptions} from "../../../core/services/model/entity.model";
import {NOT_MINIFY_OPTIONS} from "../../../core/services/model/referential.model";

export abstract class DataEntityFilter<
  T extends DataEntityFilter<T, E, EID, AO, FO>,
  E extends DataEntity<E, EID> = DataEntity<any, any>,
  EID = number,
  AO extends EntityAsObjectOptions = EntityAsObjectOptions,
  FO = any
  >
  extends EntityFilter<T, E, EID, AO, FO> {

  recorderDepartment: Department;

  fromObject(source: any, opts?: FO) {
    super.fromObject(source, opts);
    this.recorderDepartment = Department.fromObject(source.recorderDepartment)
      || isNotNil(source.recorderDepartmentId) && Department.fromObject({id: source.recorderDepartmentId})
      || undefined;
  }

  asObject(opts?: AO): any {
    const target = super.asObject(opts);
    if (opts && opts.minify) {
      target.recorderDepartmentId = this.recorderDepartment && isNotNil(this.recorderDepartment.id) ? this.recorderDepartment.id : undefined;
      delete target.recorderDepartment;
    }
    else {
      target.recorderDepartment = this.recorderDepartment && this.recorderDepartment.asObject({...opts, ...NOT_MINIFY_OPTIONS});
    }
    return target;
  }

  buildFilter(): FilterFn<E>[] {
    const filterFns = super.buildFilter();

    // Department
    if (this.recorderDepartment) {
      const recorderDepartmentId = this.recorderDepartment.id;
      if (isNotNil(recorderDepartmentId)) {
        filterFns.push(t => (t.recorderDepartment && t.recorderDepartment.id === recorderDepartmentId));
      }
    }

    return filterFns;
  }
}
