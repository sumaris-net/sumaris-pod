import {Injectable, Pipe, PipeTransform} from '@angular/core';
import {Moment} from "moment/moment";
import {DateAdapter} from "@angular/material/core";
import {DATE_ISO_PATTERN} from '../constants';
import {TranslateService} from "@ngx-translate/core";

const moment = require('moment');

@Pipe({
  name: 'dateDiffDuration'
})
@Injectable({providedIn: 'root'})
export class DateDiffDurationPipe implements PipeTransform {

  protected dayUnit: string;

  constructor(
    private dateAdapter: DateAdapter<Moment>,
    private translate: TranslateService) {

    this.dayUnit = translate.instant('COMMON.DAY_UNIT');
  }

  transform(value: { startValue: string | Moment; endValue: string | Moment }, args?: any): string | Promise<string> {
    if (!value.startValue || !value.endValue) return '';

    const startDate = this.dateAdapter.parse(value.startValue, DATE_ISO_PATTERN);
    const endDate = this.dateAdapter.parse(value.endValue, DATE_ISO_PATTERN);
    const duration = moment.duration(endDate.diff(startDate));
    if (duration.asMinutes() < 0) return '';

    const timeDuration = moment(0)
      .hour(duration.hours())
      .minute(duration.minutes());

    const days = Math.floor(duration.asDays());
    return (days > 0 ? days.toString() + (this.dayUnit + ' ') : '') + timeDuration.format('HH:mm');
  }
}
