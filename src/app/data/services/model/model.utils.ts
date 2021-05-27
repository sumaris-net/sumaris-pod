import {IEntity} from "../../../core/services/model/entity.model";
import {Department} from "../../../core/services/model/department.model";
import {Referential, ReferentialRef} from "../../../core/services/model/referential.model";
import {Person} from "../../../core/services/model/person.model";
import {PredefinedColors} from "@ionic/core";
import {QualityFlagIds} from "../../../referential/services/model/model.enum";
import {StatusIds} from "../../../core/services/model/model.enum";

/* -- Interface -- */

export interface IWithRecorderDepartmentEntity<T, ID = number> extends IEntity<T, ID> {
  recorderDepartment: Department|ReferentialRef|Referential;
}
export interface IWithRecorderPersonEntity<T, ID = number> extends IEntity<T, ID> {
  recorderPerson: Person;
}
export interface IWithObserversEntity<T, ID = number> extends IEntity<T, ID> {
  observers: Person[];
}
export interface IWithProgramEntity<T, ID = number> extends IEntity<T, ID> {
  program: Referential | any;
  recorderPerson?: Person;
  recorderDepartment: Referential | any;
}

/* -- Helper function -- */

export function getMaxRankOrder(values: { rankOrder: number }[]): number {
  let maxRankOrder = 0;
  (values || []).forEach(m => {
    if (m.rankOrder && m.rankOrder > maxRankOrder) maxRankOrder = m.rankOrder;
  });
  return maxRankOrder;
}

export function fillRankOrder(values: { rankOrder: number }[]) {
  // Compute rankOrder
  let maxRankOrder = getMaxRankOrder(values);
  (values || []).forEach(m => {
    m.rankOrder = m.rankOrder || ++maxRankOrder;
  });
}

/**
 * Compare unique rankOrder from values with values count
 * @param values
 * @return true if all rankOrder are unique
 */
export function isRankOrderValid(values: { rankOrder: number }[]): boolean {
  return (values || []).length ===
    (values || []).filter((v1, i, array) => array.findIndex(v2 => v2.rankOrder === v1.rankOrder) === i).length;
}

export function qualityFlagToColor(qualityFlagId: number): PredefinedColors {
  switch (qualityFlagId) {
    case QualityFlagIds.NOT_QUALIFIED:
      return 'secondary';
    case QualityFlagIds.GOOD:
    case QualityFlagIds.FIXED:
      return 'success';
    case QualityFlagIds.OUT_STATS:
    case QualityFlagIds.DOUBTFUL:
      return 'warning';
    case QualityFlagIds.BAD:
    case QualityFlagIds.MISSING:
    case QualityFlagIds.NOT_COMPLETED:
      return 'danger';
    default:
      return 'secondary';
  }
}

export function statusToColor(statusId: number): PredefinedColors {
  switch (statusId) {
    case StatusIds.ENABLE:
      return 'tertiary';
    case StatusIds.TEMPORARY:
      return 'secondary';
    case StatusIds.DISABLE:
      return 'danger';
    default:
      return 'secondary';
  }
}

