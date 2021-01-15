import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Injector, Input, OnInit, ViewChild} from "@angular/core";
import {LocalSettingsService} from "../../core/services/local-settings.service";
import {environment} from "../../../environments/environment";
import {AppFormUtils, EntityUtils, isNil, referentialToString} from "../../core/core.module";
import {AlertController, IonContent, ModalController} from "@ionic/angular";
import {BehaviorSubject, Observable} from "rxjs";
import {TranslateService} from "@ngx-translate/core";
import {AcquisitionLevelCodes} from "../../referential/services/model/model.enum";
import {PmfmStrategy} from "../../referential/services/model/pmfm-strategy.model";
import {isNotEmptyArray, isNotNilOrBlank, toBoolean} from "../../shared/functions";
import {PlatformService} from "../../core/services/platform.service";
import {SampleForm} from "./sample.form";
import {Sample} from "../services/model/sample.model";
import {UsageMode} from "../../core/services/model/settings.model";
import {Alerts} from "../../shared/alerts";
import {TRIP_LOCAL_SETTINGS_OPTIONS} from "../services/config/trip.config";
import {IDataEntityModalOptions} from "../../data/table/data-modal.class";
import {debounceTime} from "rxjs/operators";

export interface ISampleModalOptions extends IDataEntityModalOptions<Sample> {
  // UI Fields show/hide
  showLabel: boolean;
  showDateTime: boolean;
  showTaxonGroup: boolean;
  showTaxonName: boolean;

  // UI Options
  maxVisibleButtons: number;
  enableBurstMode: boolean;

  // Callback actions
  onSaveAndNew: (data: Sample) => Promise<Sample>;
}

@Component({
  selector: 'app-sample-modal',
  templateUrl: 'sample.modal.html',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SampleModal implements OnInit, ISampleModalOptions {

  debug = false;
  loading = false;
  mobile: boolean;
  data: Sample;
  $title = new BehaviorSubject<string>(undefined);

  @Input() acquisitionLevel: string;
  @Input() program: string;
  @Input() pmfms: Observable<PmfmStrategy[]> | PmfmStrategy[]; // Avoid to load PMFM from program
  @Input() mapPmfmFn: (pmfms: PmfmStrategy[]) => PmfmStrategy[]; // If PMFM are load from program: allow to override the list

  @Input() disabled: boolean;
  @Input() isNew: boolean;

  @Input() usageMode: UsageMode;

  @Input() showLabel = false;
  @Input() showDateTime = true;
  @Input() showTaxonGroup = true;
  @Input() showTaxonName = true;


  @Input() onReady: (modal: SampleModal) => void;
  @Input() onSaveAndNew: (data: Sample) => Promise<Sample>;
  @Input() onDelete: (event: UIEvent, data: Sample) => Promise<boolean>;

  @Input() maxVisibleButtons: number;
  @Input() enableBurstMode: boolean;

  @Input()
  set value(value: Sample) {
    this.data = value;
  }

  @ViewChild('form', { static: true }) form: SampleForm;
  @ViewChild(IonContent) content: IonContent;

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
    protected modalCtrl: ModalController,
    protected alertCtrl: AlertController,
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
    this.isNew = toBoolean(this.isNew, !this.data);
    this.usageMode = this.usageMode || this.settings.usageMode;
    this.disabled = toBoolean(this.disabled, false);
    if (isNil(this.enableBurstMode)) {
      this.enableBurstMode = this.settings.getPropertyAsBoolean(TRIP_LOCAL_SETTINGS_OPTIONS.SAMPLE_BURST_MODE_ENABLE,
        this.usageMode === 'FIELD');
    }

    if (this.disabled) {
      this.form.disable();
    }
    else {
      // Change rankOrder validator, to optional
      this.form.form.get('rankOrder').setValidators(null);
    }

    this.form.value = this.data || new Sample();

    // Compute the title
    this.computeTitle();

    if (!this.isNew) {
      // Update title each time value changes
      this.form.valueChanges
        .pipe(debounceTime(250))
        .subscribe(json => this.computeTitle(json));
    }

    // Add callback
    this.ready().then(() => {
      if (this.onReady) this.onReady(this);
      this.markForCheck();
    });
  }

  async close(event?: UIEvent) {
    if (this.dirty) {
      const saveBeforeLeave = await Alerts.askSaveBeforeLeave(this.alertCtrl, this.translate, event);

      // User cancelled
      if (isNil(saveBeforeLeave) || event && event.defaultPrevented) {
        return;
      }

      // Is user confirm: close normally
      if (saveBeforeLeave === true) {
        this.enableBurstMode = false; // Force onSubmit to close
        await this.onSubmit(event);
        return;
      }
    }

    await this.modalCtrl.dismiss();
  }

  async ready(): Promise<void> {
    await this.form.ready();
  }

  async onSubmit(event?: UIEvent) {
    // Add and reset
    if (this.enableBurstMode) {
      if (this.loading) return undefined; // avoid many call
      const data = this.getDataToSave();
      if (!data) return; // invalid

      this.loading = true;

      try {
        const newData = await this.onSaveAndNew(data);
        this.reset(newData);

        await this.scrollToTop();
      }
      finally {
        this.loading = false;
      }
    }
    // Or leave
    else {
      if (this.loading) return undefined; // avoid many call
      const data = this.getDataToSave();
      if (!data) return; // invalid

      this.loading = true;

      await this.modalCtrl.dismiss(data);
    }
  }

  async delete(event?: UIEvent) {
    if (!this.onDelete) return; // Skip

    const result = await this.onDelete(event, this.data);
    if (isNil(result) || (event && event.defaultPrevented)) return; // User cancelled

    if (result) {
      await this.modalCtrl.dismiss();
    }
  }

  toggleBurstMode() {
    this.enableBurstMode = !this.enableBurstMode;

    this.settings.setProperty(TRIP_LOCAL_SETTINGS_OPTIONS.SAMPLE_BURST_MODE_ENABLE.key, this.enableBurstMode);
  }

  /* -- protected methods -- */

  protected getDataToSave(opts?: { markAsLoading?: boolean; }): Sample {

    if (this.invalid) {
      if (this.debug) AppFormUtils.logFormErrors(this.form.form, "[sample-modal] ");
      this.form.error = "COMMON.FORM.HAS_ERROR";
      this.form.markAsTouched({emitEvent: true});
      this.scrollToTop();
      return undefined;
    }

    this.loading = true;

    // To force to get computed values
    this.form.form.enable();

    try {
      // Get form value
      return this.form.value;
    }
    finally {
      this.form.form.disable();
    }
  }

  protected reset(data?: Sample) {

    this.data = data || new Sample();
    this.form.error = null;

    try {
      this.form.value = this.data;
      //this.form.markAsPristine();
      //this.form.markAsUntouched();

      this.form.enable();

      if (this.onReady) {
        this.onReady(this);
      }

      // Compute the title
      this.computeTitle();
    }
    finally {

    }
  }

  protected async computeTitle(data?: Sample) {

    data = data || this.data;

    // Compute prefix
    let prefix = '';
    const prefixItems = [];
    if (data && !this.showTaxonGroup && EntityUtils.isNotEmpty(data.taxonGroup, 'id')) {
      prefixItems.push(referentialToString(data.taxonGroup, this.settings.getFieldDisplayAttributes('taxonGroup')));
    }
    if (data && !this.showTaxonName && data && EntityUtils.isNotEmpty(data.taxonName, 'id')) {
      prefixItems.push(referentialToString(data.taxonName, this.settings.getFieldDisplayAttributes('taxonName')));
    }
    if (isNotEmptyArray(prefixItems)) {
      prefix = await this.translate.get('TRIP.SAMPLE.NEW.TITLE_PREFIX',
        { prefix: prefixItems.join(' / ')})
        .toPromise();
    }

    if (this.isNew || !data) {
      this.$title.next(prefix + await this.translate.get('TRIP.SAMPLE.NEW.TITLE').toPromise());
    }
    else {
      // Label can be optional (e.g. in auction control)
      const label = this.showLabel && data.label || ('#' + data.rankOrder);
      this.$title.next(prefix + await this.translate.get('TRIP.SAMPLE.EDIT.TITLE', {label}).toPromise());
    }
  }

  async scrollToTop() {
    return this.content.scrollToTop();
  }

  protected markForCheck() {
    this.cd.markForCheck();
  }
}
