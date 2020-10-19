import { Pipe, Injectable, PipeTransform } from '@angular/core';
import {isMoment, Moment} from "moment/moment";
import { DateAdapter } from "@angular/material/core";
import { DATE_ISO_PATTERN } from '../constants';
import {TranslateService} from "@ngx-translate/core";

@Pipe({
    name: 'dateFormat'
})
@Injectable({providedIn: 'root'})
export class DateFormatPipe implements PipeTransform {

  private datePattern: string;
  private dateTimePattern: string;

  constructor(
      private dateAdapter: DateAdapter<Moment>,
      private translate: TranslateService
      ) {

    this.datePattern = 'L';
    this.dateTimePattern = 'L LT';

    translate.get(['COMMON.DATE_PATTERN', 'COMMON.DATE_TIME_PATTERN'])
      .subscribe(translations => {
        this.datePattern = translations['COMMON.DATE_PATTERN'];
        this.dateTimePattern = translations['COMMON.DATE_TIME_PATTERN'];
      });
  }

  transform(value: string | Moment | Date, args?: any): string | Promise<string> {
      args = args || {};
      const pattern = args.pattern || (args.time ? this.dateTimePattern : this.datePattern);
      // Keep original moment object, if possible (to avoid a conversion)
      const date: Moment = value && (isMoment(value) ? value : this.dateAdapter.parse(value, DATE_ISO_PATTERN));
      return date && this.dateAdapter.format(date, pattern) || '';
  }

  format(date: Moment, displayFormat: any): string {
    return this.dateAdapter.format(date, displayFormat);
  }
}
