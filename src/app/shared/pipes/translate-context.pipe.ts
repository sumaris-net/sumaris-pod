import {Pipe, Injectable, PipeTransform, Injector, ChangeDetectorRef} from '@angular/core';
import {isMoment, Moment} from "moment";
import { DateAdapter } from "@angular/material/core";
import { DATE_ISO_PATTERN } from '../constants';
import {TranslateService} from "@ngx-translate/core";
import {getPmfmName, PmfmStrategy} from "../../referential/services/model/pmfm-strategy.model";
import {translateElement} from "@ionic/core/dist/types/components/refresher/refresher.utils";
import {ModalController} from "@ionic/angular";
import {PlatformService} from "../../core/services/platform.service";
import {LocalSettingsService} from "../../core/services/local-settings.service";
import {Subject} from "rxjs";
import {TranslateContextService} from "../services/translate-context.service";

@Pipe({
    name: 'translateContext'
})
@Injectable({providedIn: 'root'})
export class TranslateContextPipe implements PipeTransform {

  constructor(
    protected translateContext: TranslateContextService
  ) {
  }

  transform(key: string, context?: string): string {
    return this.translateContext.instant(key, context);
  }
}
