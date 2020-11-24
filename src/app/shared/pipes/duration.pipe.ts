import {Pipe, Injectable, PipeTransform} from '@angular/core';
import {TranslateService} from "@ngx-translate/core";
import {toDuration} from "../functions";

@Pipe({
  name: 'duration'
})
@Injectable({providedIn: 'root'})
export class DurationPipe implements PipeTransform {

  protected dayUnit: string;

  constructor(
    private translate: TranslateService
  ) {
    this.dayUnit = translate.instant('COMMON.DAY_UNIT');
  }

  transform(value: number, args?: any): string {
    if (!value) return '';
    const unit = args && args.unit || "hours";

    // try with moment
    const duration = toDuration(value, unit);

    const days = duration.days();
    const hour = duration.hours();
    const minute = duration.minutes();

    return (days > 0 ? days.toString() + (this.dayUnit + ' ') : '') + (hour < 10 ? '0' : '') + hour + ':' + (minute < 10 ? '0' : '') + minute;

  }
}
