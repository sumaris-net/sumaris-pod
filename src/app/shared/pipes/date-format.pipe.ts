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

  private readonly datePattern: string;
  private readonly dateTimePattern: string;
  private readonly dateTimeSecondsPattern: string;

  constructor(
      private dateAdapter: DateAdapter<Moment>,
      private translate: TranslateService
      ) {

    const translations = translate.instant(['COMMON.DATE_PATTERN', 'COMMON.DATE_TIME_PATTERN', 'COMMON.DATE_TIME_SECONDS_PATTERN']);
    this.datePattern = translations['COMMON.DATE_PATTERN'];
    this.dateTimePattern = translations['COMMON.DATE_TIME_PATTERN'];
    this.dateTimeSecondsPattern = translations['COMMON.DATE_TIME_SECONDS_PATTERN'];
  }

  transform(value: string | Moment | Date, args?: { pattern?: string; time?: boolean; seconds?: boolean; } ): string | Promise<string> {
      const pattern = args && args.pattern ||
        (args && args.time ? (args.seconds ? this.dateTimeSecondsPattern : this.dateTimePattern) : this.datePattern);
      // Keep original moment object, if possible (to avoid a conversion)
      const date: Moment = value && (isMoment(value) ? value : this.dateAdapter.parse(value, DATE_ISO_PATTERN));
      return date && this.dateAdapter.format(date, pattern) || '';
  }

  format(date: Moment, displayFormat: any): string {
    return this.dateAdapter.format(date, displayFormat);
  }
}
