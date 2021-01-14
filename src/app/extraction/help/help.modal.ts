import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Injector, Input, OnInit} from "@angular/core";
import {BehaviorSubject} from "rxjs";
import {ModalController} from "@ionic/angular";
import {PlatformService} from "../../core/services/platform.service";
import {LocalSettingsService} from "../../core/services/local-settings.service";
import {TranslateService} from "@ngx-translate/core";
import {ExtractionType} from "../services/model/extraction.model";
import {isNotNilOrBlank} from "../../shared/functions";
import {AppHelpModal} from "../../shared/help/help.modal";

@Component({
    selector: 'app-extraction-help-modal',
    templateUrl: '../../shared/help/help.modal.html'
})
export class ExtractionHelpModal extends AppHelpModal implements OnInit {

  @Input()
  type: ExtractionType;

  constructor(
      protected injector: Injector,
      protected viewCtrl: ModalController,
      protected platform: PlatformService,
      protected settings: LocalSettingsService,
      protected translate: TranslateService,
      protected cd: ChangeDetectorRef
  ) {
    super(injector, viewCtrl, platform, settings, translate, cd);
  }


  ngOnInit() {
    if (!this.type) throw new Error("Missing 'type' input");
    this.title = this.type.name;

    console.debug('[extraction-help-modal] Show help modal for type:', this.type);
    if (isNotNilOrBlank(this.type.description)) {
      const subtitle = this.translate.instant('EXTRACTION.HELP.MODAL.DESCRIPTION');
      this.markdownContent = `# ${subtitle}\n\n${this.type.description}\n\n`;
    }
    if (this.type.docUrl) {
      this.loading = true;
      this.docUrl = this.type.docUrl;
    }
    else {
      this.markAsLoaded(); // Nothing to load
    }
  }
}
