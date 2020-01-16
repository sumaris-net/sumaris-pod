import {DataEntity} from "./model/base.model";
import {FormErrors} from "../../core/form/form.utils";

export interface DataQualityService<T extends DataEntity<T>, O = any> {

  synchronize(data: T): Promise<T>;

  control(data: T, opts?: O): Promise<FormErrors>;
  terminate(data: T): Promise<T>;
  validate(data: T): Promise<T>;
  unvalidate(data: T): Promise<T>;
  qualify(data: T, qualityFlagId: number): Promise<T>;

  canUserWrite(data: T): boolean;
}

const DataQualityServiceFnName: string[] = ['synchronize', 'control', 'terminate', 'validate', 'unvalidate', 'qualify', 'canUserWrite'];

export function isDataQualityService(object: any): object is DataQualityService<any> {
    return object && DataQualityServiceFnName.reduce((res, fnName) => {
      if (typeof object[fnName] === 'function') return res + 1;
      return res;
    }, 0) === DataQualityServiceFnName.length || false;
}
