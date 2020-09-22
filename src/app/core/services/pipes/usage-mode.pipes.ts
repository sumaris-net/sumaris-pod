import {Injectable, Pipe, PipeTransform} from '@angular/core';

import {UsageMode} from "../model/settings.model";
import {LocalSettingsService} from "../local-settings.service";

@Pipe({
  name: 'isOnField'
})
@Injectable({providedIn: 'root'})
export class IsOnFieldModePipe implements PipeTransform {

  constructor(
    private settings: LocalSettingsService
  ) {
  }

  transform(value: UsageMode): boolean {
    return this.settings.isOnFieldMode(value);
  }
}


@Pipe({
  name: 'isNotOnField'
})
@Injectable({providedIn: 'root'})
export class IsNotOnFieldModePipe implements PipeTransform {

  constructor(
    private settings: LocalSettingsService
  ) {
  }

  transform(value: UsageMode): boolean {
    return !this.settings.isOnFieldMode(value);
  }
}
