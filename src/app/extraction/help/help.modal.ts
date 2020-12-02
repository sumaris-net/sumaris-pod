import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Injector, Input, OnInit} from "@angular/core";
import {BehaviorSubject} from "rxjs";
import {ModalController} from "@ionic/angular";
import {PlatformService} from "../../core/services/platform.service";
import {LocalSettingsService} from "../../core/services/local-settings.service";
import {TranslateService} from "@ngx-translate/core";
import {ExtractionType} from "../services/model/extraction.model";
import {isNotNilOrBlank} from "../../shared/functions";

@Component({
    selector: 'app-extraction-help-modal',
    templateUrl: './help.modal.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class ExtractionHelpModal implements OnInit {

  readonly debug: boolean;
  loading = true;
  error: string = undefined;

  markdownContent: string;
  docUrl: string;

  @Input()
  type: ExtractionType;

  @Input()
  showError: boolean = true;

  constructor(
      protected injector: Injector,
      protected viewCtrl: ModalController,
      protected platform: PlatformService,
      protected settings: LocalSettingsService,
      protected translate: TranslateService,
      protected cd: ChangeDetectorRef
  ) {
    // TODO: for DEV only
    //this.debug = !environment.production;
  }


  ngOnInit() {

    if (isNotNilOrBlank(this.type.description)) {
      const descriptionSubtitle = this.translate.instant('EXTRACTION.HELP.MODAL.DESCRIPTION');
      this.markdownContent = `# ${descriptionSubtitle}\n\n${this.type.description}\n\n`;
    }
    if (this.type.docUrl) {
      this.loading = true;
      this.docUrl = this.type.docUrl;
    }
    else {
      this.markAsLoaded(); // Nothing to load
    }
  }

  async close() {
      await this.viewCtrl.dismiss();
  }

  markAsLoaded() {
    this.loading = false;
    this.error = null;
  }

  onLoadError(error?: string) {
    console.error(error);

    this.error = error;
    this.loading = false;
  }

  /* -- protected methods -- */

  protected markForCheck() {
      this.cd.markForCheck();
  }

}
