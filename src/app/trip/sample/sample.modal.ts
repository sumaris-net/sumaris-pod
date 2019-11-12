import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Injector, Input, OnInit, ViewChild} from "@angular/core";
import {Batch} from "../services/model/batch.model";
import {LocalSettingsService} from "../../core/services/local-settings.service";
import {environment} from "../../../environments/environment";
import {AppFormUtils} from "../../core/core.module";
import {ModalController} from "@ionic/angular";
import {BehaviorSubject, Observable} from "rxjs";
import {TranslateService} from "@ngx-translate/core";
import {AcquisitionLevelCodes, PmfmStrategy} from "../../referential/services/model";
import {isNotNilOrBlank, toBoolean} from "../../shared/functions";
import {PlatformService} from "../../core/services/platform.service";
import {SampleForm} from "./sample.form";
import {Sample} from "../services/model/sample.model";
import {FormGroup} from "@angular/forms";

@Component({
  selector: 'app-sample-modal',
  templateUrl: 'sample.modal.html',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SampleModal implements OnInit {

  debug = false;
  loading = false;
  mobile: boolean;
  data: Sample;
  $title = new BehaviorSubject<string>(undefined);

  @Input() acquisitionLevel: string;

  @Input() program: string;

  @Input() canEdit: boolean;

  @Input() disabled: boolean;

  @Input() isNew = false;

  @Input() showTaxonGroup = true;

  @Input() showTaxonName = true;

  @Input() title: string;

  @Input()
  set value(value: Sample) {
    this.data = value;
  }

  @Input() onReady: (modal: SampleModal) => void;

  @ViewChild('form', { static: true }) form: SampleForm;

  get dirty(): boolean {
    return this.form.dirty;
  }

  get invalid(): boolean {
    return this.form.invalid;
  }

  get valid(): boolean {
    return this.form.valid;
  }

  get $pmfms(): Observable<PmfmStrategy[]> {
    return this.form.$pmfms;
  }

  constructor(
    protected injector: Injector,
    protected viewCtrl: ModalController,
    protected platform: PlatformService,
    protected settings: LocalSettingsService,
    protected translate: TranslateService,
    protected cd: ChangeDetectorRef
  ) {
    // Default value
    this.acquisitionLevel = AcquisitionLevelCodes.SAMPLE;
    this.mobile = platform.mobile;

    // TODO: for DEV only
    this.debug = !environment.production;
  }


  ngOnInit() {
    this.canEdit = toBoolean(this.canEdit, !this.disabled);
    this.disabled = !this.canEdit || toBoolean(this.disabled, true);

    if (this.disabled) {
      this.form.disable();
    }

    this.form.value = this.data || new Sample();

    // Compute the title
    this.computeTitle();

    if (!this.isNew) {
      // Update title each time value changes
      this.form.valueChanges.subscribe(json => this.computeTitle(json));
    }

    // Add callback
    this.ready().then(() => {
      this.onReady && this.onReady(this);
      this.markForCheck();
    });
  }

  async cancel() {
    await this.viewCtrl.dismiss();
  }

  async ready(): Promise<void>{
    await this.form.ready();
  }

  async close(event?: UIEvent) {
    if (this.loading) return; // avoid many call

    if (this.invalid) {
      if (this.debug) AppFormUtils.logFormErrors(this.form.form, "[sample-modal] ");
      this.form.error = "COMMON.FORM.HAS_ERROR";
      this.form.markAsTouched({emitEvent: true});
      return;
    }

    this.loading = true;

    // Save table content
    const data = this.form.value;

    await this.viewCtrl.dismiss(data);
  }

  /* -- protected methods -- */

  protected markForCheck() {
    this.cd.markForCheck();
  }

  protected async computeTitle(data?: Sample) {
    if (isNotNilOrBlank(this.title)) return this.title;

    data = data || this.data;
    if (this.isNew || !data) {
      this.$title.next(await this.translate.get('TRIP.SAMPLE.NEW.TITLE').toPromise());
    }
    else {
      // Label can be optional (e.g. in auction control)
      const label = data.label || ('#' + data.rankOrder);
      this.$title.next(await this.translate.get('TRIP.SAMPLE.EDIT.TITLE', {label}).toPromise());
    }
  }
}
