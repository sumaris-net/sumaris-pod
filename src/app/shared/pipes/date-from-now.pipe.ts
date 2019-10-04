import {Pipe, Injectable, PipeTransform} from '@angular/core';
import {Moment} from "moment/moment";
import {DateAdapter} from "@angular/material";
import {DATE_ISO_PATTERN} from '../constants';
import {TranslateService} from "@ngx-translate/core";
import {first} from "rxjs/operators";
import {firstNotNilPromise} from "../observables";

@Pipe({
  name: 'dateFromNow'
})
@Injectable({providedIn: 'root'})
export class DateFromNowPipe implements PipeTransform {

  private dayUnit: string;

  constructor(
    private dateAdapter: DateAdapter<Moment>,
    private translate: TranslateService) {

    this.dayUnit = translate.instant('COMMON.DAY_UNIT');
  }

  transform(value: string | Moment, args?: any): string | Promise<string> {
    const date = this.dateAdapter.parse(value, DATE_ISO_PATTERN);
    return date ? date.fromNow(args && args.withoutSuffix) : '';
  }
}
