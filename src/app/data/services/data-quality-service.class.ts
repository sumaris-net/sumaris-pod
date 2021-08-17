import {DataEntity} from './model/data-entity.model';
import {FormErrors} from '@sumaris-net/ngx-components';

export interface IDataEntityQualityService<
  T extends DataEntity<T, ID>,
  ID = number,
  O = any> {

  control(data: T, opts?: O): Promise<FormErrors>;
  terminate(data: T): Promise<T>;
  validate(data: T): Promise<T>;
  unvalidate(data: T): Promise<T>;
  qualify(data: T, qualityFlagId: number): Promise<T>;

  canUserWrite(data: T): boolean;
}

const DataQualityServiceFnName: (keyof IDataEntityQualityService<any>)[] = ['control', 'terminate', 'validate', 'unvalidate', 'qualify', 'canUserWrite'];
export function isDataQualityService(object: any): object is IDataEntityQualityService<any> {
  return object && DataQualityServiceFnName.filter(fnName => (typeof object[fnName] === 'function'))
    .length === DataQualityServiceFnName.length || false;
}

