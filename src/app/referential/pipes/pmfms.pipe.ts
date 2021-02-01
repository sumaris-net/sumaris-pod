import {Injectable, Pipe, PipeTransform} from '@angular/core';
import {getPmfmName, PmfmStrategy} from "../services/model/pmfm-strategy.model";
import {MethodIds, PmfmIds} from "../services/model/model.enum";
import {PmfmValueUtils} from "../services/model/pmfm-value.model";
import {Pmfm} from "../services/model/pmfm.model";

@Pipe({
    name: 'pmfmName'
})
@Injectable({providedIn: 'root'})
export class PmfmNamePipe implements PipeTransform {

    transform(val: PmfmStrategy, opts?: {
      withUnit?: boolean;
      html?: boolean;
      withDetails?: boolean ;
    }): any {
      return getPmfmName(val, opts);
    }
}

@Pipe({
  name: 'pmfmValueToString'
})
@Injectable({providedIn: 'root'})
export class PmfmValueToStringPipe implements PipeTransform {

  transform(val: PmfmStrategy, opts: { pmfm: PmfmStrategy | Pmfm; propertyNames?: string[]; html?: boolean; }): any {
    return PmfmValueUtils.valueToString(val, opts);
  }
}

@Pipe({
  name: 'isDatePmfm'
})
@Injectable({providedIn: 'root'})
export class IsDatePmfmPipe implements PipeTransform {

  transform(pmfm: PmfmStrategy): any {
    return pmfm && pmfm.type === 'date';
  }
}

@Pipe({
  name: 'isComputedPmfm'
})
@Injectable({providedIn: 'root'})
export class IsComputedPmfmPipe implements PipeTransform {

  transform(pmfm: PmfmStrategy): any {
    return pmfm.type && (pmfm.methodId === MethodIds.CALCULATED);
  }
}
