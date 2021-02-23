import {Injectable, Pipe, PipeTransform} from '@angular/core';
import {getPmfmName, PmfmStrategy} from "../services/model/pmfm-strategy.model";
import {MethodIds} from "../services/model/model.enum";
import {PmfmValueUtils} from "../services/model/pmfm-value.model";
import {IPmfm} from "../services/model/pmfm.model";
import {isNil} from "../../shared/functions";

@Pipe({
    name: 'pmfmName'
})
@Injectable({providedIn: 'root'})
export class PmfmNamePipe implements PipeTransform {

    transform(val: IPmfm, opts?: {
      withUnit?: boolean;
      html?: boolean;
      withDetails?: boolean ;
    }): string {
      return getPmfmName(val, opts);
    }
}

@Pipe({
  name: 'pmfmValueToString'
})
@Injectable({providedIn: 'root'})
export class PmfmValueToStringPipe implements PipeTransform {

  transform(val: PmfmStrategy, opts: { pmfm: IPmfm; propertyNames?: string[]; html?: boolean; }): any {
    return PmfmValueUtils.valueToString(val, opts);
  }
}

@Pipe({
  name: 'isDatePmfm'
})
@Injectable({providedIn: 'root'})
export class IsDatePmfmPipe implements PipeTransform {

  transform(pmfm: IPmfm): any {
    return pmfm && pmfm.type === 'date';
  }
}

@Pipe({
  name: 'isComputedPmfm'
})
@Injectable({providedIn: 'root'})
export class IsComputedPmfmPipe implements PipeTransform {

  transform(pmfm: IPmfm): any {
    // DEBUG
    //if (isNil(pmfm && pmfm.methodId)) console.warn('TODO cannot check if computed - no method :', pmfm.name);

    return pmfm.type && (pmfm.methodId === MethodIds.CALCULATED);
  }
}
