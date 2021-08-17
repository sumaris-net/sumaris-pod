import {Department, IEntity, Person, Referential, ReferentialRef, StatusIds} from '@sumaris-net/ngx-components';
import {PredefinedColors} from '@ionic/core';
import {QualityFlagIds} from '@app/referential/services/model/model.enum';

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

export const getMaxRankOrder = (values: { rankOrder: number }[]): number => {
  let maxRankOrder = 0;
  (values || []).forEach(m => {
    if (m.rankOrder && m.rankOrder > maxRankOrder) maxRankOrder = m.rankOrder;
  });
  return maxRankOrder;
};

export const fillRankOrder = (values: { rankOrder: number }[]) => {
  // Compute rankOrder
  let maxRankOrder = getMaxRankOrder(values);
  (values || []).forEach(m => {
    m.rankOrder = m.rankOrder || ++maxRankOrder;
  });
};

/**
 * Compare unique rankOrder from values with values count
 *
 * @param values
 * @return true if all rankOrder are unique
 */
export const isRankOrderValid = (values: { rankOrder: number }[]): boolean => (values || []).length ===
  (values || []).filter((v1, i, array) => array.findIndex(v2 => v2.rankOrder === v1.rankOrder) === i).length;

export const qualityFlagToColor = (qualityFlagId: number): PredefinedColors => {
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
};

export const statusToColor = (statusId: number): PredefinedColors => {
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
};

