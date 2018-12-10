import {Pipe, Injectable, PipeTransform} from '@angular/core';
import {Moment} from "moment/moment";
import {DateAdapter} from "@angular/material";
import {DATE_ISO_PATTERN} from '../constants';

let moment = require('moment');

@Pipe({
  name: 'dateDiffDuration'
})
@Injectable()
export class DateDiffDurationPipe implements PipeTransform {

  constructor(
    private dateAdapter: DateAdapter<Moment>) {
  }

  transform(value: { startValue: string | Moment; endValue: string | Moment }, args?: any): string | Promise<string> {
    let startDate = this.dateAdapter.parse(value.startValue, DATE_ISO_PATTERN);
    let endDate = this.dateAdapter.parse(value.endValue, DATE_ISO_PATTERN);
    let duration = moment.duration(endDate.diff(startDate));
    if (duration.asMinutes() < 0) return '';

    let days = Math.floor(duration.asDays());
    let result = days > 0 ? days.toString() + ' jours' : '';
    result += duration.hours() > 0 ? ' ' + duration.hours().toString() + ' heures' : '';
    result += duration.minutes() > 0 ? ' ' + duration.minutes().toString() + ' minutes' : '';
    return result;
  }
}
