import { Injectable, Pipe, PipeTransform } from '@angular/core';
import { MethodIds } from '../services/model/model.enum';
import { PmfmValueUtils } from '../services/model/pmfm-value.model';
import { IPmfm, PmfmUtils } from '../services/model/pmfm.model';
import { isNotNilOrBlank, TranslateContextService } from '@sumaris-net/ngx-components';
import { TranslateService } from '@ngx-translate/core';

@Pipe({
    name: 'pmfmName'
})
@Injectable({providedIn: 'root'})
export class PmfmNamePipe implements PipeTransform {

  constructor(
    protected translate: TranslateService,
    protected translateContext: TranslateContextService
  ) {

  }

  transform(pmfm: IPmfm, opts?: {
    withUnit?: boolean;
    html?: boolean;
    withDetails?: boolean;
    i18nPrefix?: string;
    i18nContext?: string;
  }): string {
    // Try to resolve PMFM using prefix + label
    if (opts && isNotNilOrBlank(opts.i18nPrefix)) {
      const i18nKey = opts.i18nPrefix + pmfm.label;

      // I18n translation WITH context, if any
      if (opts && opts.i18nContext) {
        const contextualKey = this.translateContext.contextualKey(i18nKey, opts.i18nContext);
        const contextualTranslation = this.translate.instant(contextualKey);
        if (contextualTranslation !== contextualKey) return contextualTranslation;
      }

      // I18n translation without context
      const translation = this.translate.instant(i18nKey);
      if (translation !== i18nKey) return translation;
    }

    // Default name, computed from the PMFM object
    return PmfmUtils.getPmfmName(pmfm, opts);
  }
}

@Pipe({
  name: 'pmfmValue'
})
@Injectable({providedIn: 'root'})
export class PmfmValuePipe implements PipeTransform {

  transform(val: any, opts: { pmfm: IPmfm; propertyNames?: string[]; html?: boolean; hideIfDefaultValue?: boolean; showLabelForPmfmIds?: number[] }): any {
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


@Pipe({
  name: 'isMultiplePmfm'
})
@Injectable({providedIn: 'root'})
export class IsMultiplePmfmPipe implements PipeTransform {

  transform(pmfm: IPmfm): any {
    return pmfm && pmfm.isMultiple;
  }
}
