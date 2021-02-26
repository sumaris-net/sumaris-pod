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

@Pipe({
    name: 'translateOrDefault'
})
@Injectable({providedIn: 'root'})
export class TranslateOrDefaultPipe implements PipeTransform {

  constructor(
    protected injector: Injector,
    protected translate: TranslateService,
    protected cd: ChangeDetectorRef
  ) {

  }

  async transform(columnName: string, defaultVal: string):  Promise<string> {
    const translatedColumnName = await this.translate.get(columnName).toPromise();
    return (translatedColumnName == columnName) ? defaultVal : translatedColumnName;
  }
}
