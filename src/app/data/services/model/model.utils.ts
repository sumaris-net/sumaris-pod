import {Department, IEntity, Person, Referential, ReferentialRef, StatusIds} from '@sumaris-net/ngx-components';
import {PredefinedColors} from '@ionic/core';
import {QualityFlagIds} from '@app/referential/services/model/model.enum';

/* -- Enumerations -- */

export type SynchronizationStatus = 'DIRTY' | 'READY_TO_SYNC' | 'SYNC' | 'DELETED';

export const SynchronizationStatusEnum = Object.freeze({
  DIRTY: <SynchronizationStatus>'DIRTY',
  READY_TO_SYNC: <SynchronizationStatus>'READY_TO_SYNC',
  SYNC: <SynchronizationStatus>'SYNC',
  DELETED: <SynchronizationStatus>'DELETED'
});

export type DataQualityStatus = 'MODIFIED' | 'CONTROLLED' | 'VALIDATED' | 'QUALIFIED';

export const DataQualityStatusEnum = Object.freeze({
  MODIFIED: <DataQualityStatus>'MODIFIED',
  CONTROLLED: <DataQualityStatus>'CONTROLLED',
  VALIDATED: <DataQualityStatus>'VALIDATED',
  QUALIFIED: <DataQualityStatus>'QUALIFIED',
})

export declare interface DataQualityStatusItem {
  id: DataQualityStatus;
  icon: string;
  label: string;
}

export const DataQualityStatusItemsMap = Object.freeze({
  MODIFIED: <DataQualityStatusItem>{
    id: DataQualityStatusEnum.MODIFIED,
    icon: 'pencil',
    label: 'QUALITY.MODIFIED'
  },
  CONTROLLED: <DataQualityStatusItem>{
    id: DataQualityStatusEnum.CONTROLLED,
    icon: 'checkmark',
    label: 'QUALITY.CONTROLLED'
  },
  VALIDATED: <DataQualityStatusItem>{
    id: DataQualityStatusEnum.VALIDATED,
    icon: 'checkmark-circle',
    label: 'QUALITY.VALIDATED'
  },
  QUALIFIED: <DataQualityStatusItem>{
    id: DataQualityStatusEnum.QUALIFIED,
    icon: 'flag',
    label: 'QUALITY.QUALIFIED'
  }
});

export const DataQualityStatusItems = Object.freeze([
  DataQualityStatusItemsMap.MODIFIED,
  DataQualityStatusItemsMap.CONTROLLED,
  DataQualityStatusItemsMap.VALIDATED,
  DataQualityStatusItemsMap.QUALIFIED
]);


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
