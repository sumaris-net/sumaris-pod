import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Injector, Input, OnDestroy, OnInit, ViewChild } from '@angular/core';
import {
  Alerts,
  AppFormUtils,
  EntityUtils,
  isNil,
  isNotEmptyArray,
  LocalSettingsService,
  PlatformService,
  referentialToString,
  toBoolean,
  TranslateContextService,
  UsageMode,
} from '@sumaris-net/ngx-components';
import { environment } from '../../../environments/environment';
import { AlertController, IonContent, ModalController } from '@ionic/angular';
import { BehaviorSubject, isObservable, Observable, Subscription, TeardownLogic } from 'rxjs';
import { TranslateService } from '@ngx-translate/core';
import { AcquisitionLevelCodes } from '@app/referential/services/model/model.enum';
import { SampleForm } from './sample.form';
import { Sample } from '../services/model/sample.model';
import { TRIP_LOCAL_SETTINGS_OPTIONS } from '../services/config/trip.config';
import { IDataEntityModalOptions } from '@app/data/table/data-modal.class';
import { debounceTime, filter } from 'rxjs/operators';
import { IPmfm } from '@app/referential/services/model/pmfm.model';
import { Moment } from 'moment';


export interface ISampleModalOptions<M = SampleModal> extends IDataEntityModalOptions<Sample> {

  // UI Fields show/hide
  showLabel: boolean;
  showSampleDate: boolean;
  showTaxonGroup: boolean;
  showTaxonName: boolean;
  showIndividualReleaseButton: boolean;

  defaultSampleDate?: Moment;

  // UI Options
  maxVisibleButtons: number;
  enableBurstMode: boolean;
  i18nSuffix?: string;

  // Callback actions
  onSaveAndNew: (data: Sample) => Promise<Sample>;
  onReady: (modal: M) => Promise<void> | void;
  openIndividualReleaseModal: (subSample: Sample) => Promise<Sample>;
}

@Component({
  selector: 'app-sample-modal',
  templateUrl: 'sample.modal.html',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SampleModal implements OnInit, OnDestroy, ISampleModalOptions {

  private _subscription = new Subscription();
  $title = new BehaviorSubject<string>(undefined);
  debug = false;
  loading = false;
  mobile: boolean;

  @Input() isNew: boolean;
  @Input() data: Sample;
  @Input() disabled: boolean;
  @Input() acquisitionLevel: string;
  @Input() programLabel: string;
  @Input() usageMode: UsageMode;
  @Input() pmfms: IPmfm[];

  // UI options
  @Input() i18nSuffix: string;
  @Input() showLabel = true;
  @Input() showSampleDate = true;
  @Input() showTaxonGroup = true;
  @Input() showTaxonName = true;
  @Input() showComment: boolean;
  @Input() showIndividualReleaseButton: boolean;
  @Input() maxVisibleButtons: number;
  @Input() enableBurstMode: boolean;

  @Input() onReady: (modal: SampleModal) => Promise<void> | void;
  @Input() onSaveAndNew: (data: Sample) => Promise<Sample>;
  @Input() onDelete: (event: UIEvent, data: Sample) => Promise<boolean>;
  @Input() openIndividualReleaseModal: (subSample: Sample) => Promise<Sample>;

  @ViewChild('form', {static: true}) form: SampleForm;
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

  constructor(
    protected injector: Injector,
    protected platform: PlatformService,
    protected modalCtrl: ModalController,
    protected alertCtrl: AlertController,
    protected settings: LocalSettingsService,
    protected translate: TranslateService,
    protected translateContext: TranslateContextService,
    protected cd: ChangeDetectorRef
  ) {
    // Default value
    this.mobile = platform.mobile;
    this.acquisitionLevel = AcquisitionLevelCodes.SAMPLE;

    // TODO: for DEV only
    this.debug = !environment.production;
    this.showComment = !this.mobile;
  }

  ngOnInit() {
    this.isNew = toBoolean(this.isNew, !this.data);
    this.usageMode = this.usageMode || this.settings.usageMode;
    this.disabled = toBoolean(this.disabled, false);
    this.i18nSuffix = this.i18nSuffix || '';
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


    // Compute the title
    this.computeTitle();

    // Update title each time value changes
    if (!this.isNew) {
      this._subscription.add(
        this.form.valueChanges
          .pipe(debounceTime(250))
          .subscribe(json => this.computeTitle(json))
      );
    }

    this.init();
  }

  ngOnDestroy() {
    this._subscription.unsubscribe();
  }

  private async init() {

    console.debug('[sample-modal] Applying value to form...', this.data);
    this.form.markAsReady();

    try {
      // Set form value
      this.data = this.data || new Sample();
      let promiseOrVoid = this.form.setValue(this.data);
      if (promiseOrVoid) await promiseOrVoid;

      // Call ready callback
      if (this.onReady) {
        promiseOrVoid = this.onReady(this);
        if (promiseOrVoid) await promiseOrVoid;
      }
    }
    finally {
      this.form.markAsUntouched();
      this.form.markAsPristine();
      this.markForCheck();
    }
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

  /**
   * Add and reset form
   */
  async onSubmitAndNext(event?: UIEvent) {
    if (this.loading) return undefined; // avoid many call
    // DEBUG
    //console.debug('[sample-modal] Calling onSubmitAndNext()');

    const data = this.getDataToSave();
    if (!data) return; // invalid

    this.loading = true;

    try {
      const newData = await this.onSaveAndNew(data);
      this.reset(newData);

      await this.scrollToTop();
    } finally {
      this.loading = false;
      this.markForCheck();
    }
  }

  /**
   * Validate and close
   * @param event
   */
  async onSubmit(event?: UIEvent) {
    if (this.loading) return undefined; // avoid many call

    // Leave without saving
    if (!this.dirty) {
      this.loading = true;
      await this.modalCtrl.dismiss();
    }
    // Convert and dismiss
    else {
      const data = this.dirty ? this.getDataToSave() : this.data;
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

  async showIndividualReleaseModal(event?: UIEvent) {
    if (!this.openIndividualReleaseModal) return; // Skip

    // Save
    const savedSample = await this.getDataToSave();
    if (!savedSample) return;

    try {

      // Execute the callback
      const updatedParent = await this.openIndividualReleaseModal(savedSample);

      if (!updatedParent) return; // User cancelled

      this.form.markAsDirty();
    } finally {
      this.loading = false;
      this.form.enable();
    }
  }

  toggleBurstMode() {
    this.enableBurstMode = !this.enableBurstMode;

    // Remember (store in local settings)
    this.settings.setProperty(TRIP_LOCAL_SETTINGS_OPTIONS.SAMPLE_BURST_MODE_ENABLE.key, this.enableBurstMode);
  }

  toggleComment() {
    this.showComment = !this.showComment;
    this.markForCheck();
  }

  /* -- protected methods -- */

  protected getDataToSave(): Sample {

    if (this.invalid) {
      if (this.debug) AppFormUtils.logFormErrors(this.form.form, '[sample-modal] ');
      this.form.error = 'COMMON.FORM.HAS_ERROR';
      this.form.markAllAsTouched();
      this.scrollToTop();
      return undefined;
    }

    this.loading = true;

    // To force enable, to get computed values
    this.form.form.enable();

    try {
      // Get form value
      return this.form.value;
    } finally {
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
    } finally {
      this.markForCheck();
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
      prefix = this.translateContext.instant('TRIP.SAMPLE.TITLE_PREFIX', this.i18nSuffix,
        {prefix: prefixItems.join(' / ')});
    }

    if (this.isNew || !data) {
      this.$title.next(prefix + this.translateContext.instant('TRIP.SAMPLE.NEW.TITLE', this.i18nSuffix));
    } else {
      // Label can be optional (e.g. in auction control)
      const label = this.showLabel && data.label || ('#' + data.rankOrder);
      this.$title.next(prefix + this.translateContext.instant('TRIP.SAMPLE.EDIT.TITLE', this.i18nSuffix, {label}));
    }
  }

  async scrollToTop() {
    return this.content.scrollToTop();
  }

  protected registerSubscription(teardown: TeardownLogic) {
    this._subscription.add(teardown);
  }

  protected markForCheck() {
    this.cd.markForCheck();
  }
}
