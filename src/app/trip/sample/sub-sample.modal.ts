import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Injector, Input, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {Alerts, AppFormUtils, isNil, isNotEmptyArray, LocalSettingsService, toBoolean, UsageMode} from '@sumaris-net/ngx-components';
import {environment} from '../../../environments/environment';
import {AlertController, IonContent, ModalController} from '@ionic/angular';
import {BehaviorSubject, isObservable, Observable, Subscription, TeardownLogic} from 'rxjs';
import {TranslateService} from '@ngx-translate/core';
import {AcquisitionLevelCodes} from '@app/referential/services/model/model.enum';
import {DenormalizedPmfmStrategy} from '@app/referential/services/model/pmfm-strategy.model';
import {Sample} from '../services/model/sample.model';
import {IDataEntityModalOptions} from '@app/data/table/data-modal.class';
import {debounceTime, filter} from 'rxjs/operators';
import {IPmfm} from '@app/referential/services/model/pmfm.model';
import {SubSampleForm} from '@app/trip/sample/sub-sample.form';

export interface ISubSampleModalOptions extends IDataEntityModalOptions<Sample> {

  //Data
  availableParents: Sample[];

  // UI Fields show/hide
  enableParent: boolean;

  // UI Options
  maxVisibleButtons: number;
  mobile: boolean;
  i18nPrefix?: string;
  defaultLatitudeSign: '+' | '-';
  defaultLongitudeSign: '+' | '-';

}

@Component({
  selector: 'app-sub-sample-modal',
  templateUrl: 'sub-sample.modal.html',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SubSampleModal implements OnInit, OnDestroy, ISubSampleModalOptions {

  private _subscription = new Subscription();
  $pmfms = new BehaviorSubject<IPmfm[]>(undefined);
  $title = new BehaviorSubject<string>(undefined);
  debug = false;
  loading = false;

  @Input() isNew: boolean;
  @Input() data: Sample;
  @Input() disabled: boolean;
  @Input() acquisitionLevel: string;
  @Input() programLabel: string;
  @Input() usageMode: UsageMode;
  @Input() mobile: boolean;

  @Input() availableParents: Sample[];

  @Input() i18nPrefix: string;
  @Input() showLabel = false;
  @Input() enableParent = true;
  @Input() showComment: boolean;
  @Input() set pmfms(value: Observable<IPmfm[]> | IPmfm[]) {
    this.setPmfms(value);
  }

  @Input() mapPmfmFn: (pmfms: DenormalizedPmfmStrategy[]) => DenormalizedPmfmStrategy[]; // If PMFM are load from program: allow to override the list
  @Input() onDelete: (event: UIEvent, data: Sample) => Promise<boolean>;
  @Input() maxVisibleButtons: number;
  @Input() defaultLatitudeSign: '+' | '-';
  @Input() defaultLongitudeSign: '+' | '-';


  @ViewChild('form', { static: true }) form: SubSampleForm;
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
    protected modalCtrl: ModalController,
    protected alertCtrl: AlertController,
    protected settings: LocalSettingsService,
    protected translate: TranslateService,
    protected cd: ChangeDetectorRef
  ) {
    // Default value
    this.acquisitionLevel = AcquisitionLevelCodes.SAMPLE;

    // TODO: for DEV only
    this.debug = !environment.production;
    this.showComment = !this.mobile;
  }

  ngOnInit() {
    this.isNew = toBoolean(this.isNew, !this.data);
    this.usageMode = this.usageMode || this.settings.usageMode;
    this.disabled = toBoolean(this.disabled, false);

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
      this._subscription.add(
        this.form.valueChanges
          .pipe(debounceTime(250))
          .subscribe(json => this.computeTitle(json))
      );
    }
  }

  ngOnDestroy() {
    this._subscription.unsubscribe();
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
        await this.onSubmit(event);
        return;
      }
    }

    await this.modalCtrl.dismiss();
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

  toggleComment() {
    this.showComment = !this.showComment;
    this.markForCheck();
  }

  /* -- protected methods -- */

  private setPmfms(value: Observable<IPmfm[]> | IPmfm[]) {
    if (isObservable(value)) {
      this.registerSubscription(
        value
          .pipe(filter(pmfms => pmfms !== this.$pmfms.value))
          .subscribe(pmfms => this.$pmfms.next(pmfms))
      );
    }
    else if (value !== this.$pmfms.value){
      this.$pmfms.next(value);
    }
  }

  protected getDataToSave(opts?: { markAsLoading?: boolean; }): Sample {

    if (this.invalid) {
      if (this.debug) AppFormUtils.logFormErrors(this.form.form, "[sample-modal] ");
      this.form.error = "COMMON.FORM.HAS_ERROR";
      this.form.markAllAsTouched();
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

      // Compute the title
      this.computeTitle();
    }
    finally {
      this.markForCheck();
    }
  }

  protected async computeTitle(data?: Sample) {

    data = data || this.data;

    // Compute prefix
    let prefix = '';
    const prefixItems = [];
    if (isNotEmptyArray(prefixItems)) {
      prefix = await this.translate.get('TRIP.SAMPLE.NEW.TITLE_PREFIX',
        { prefix: prefixItems.join(' / ')})
        .toPromise();
    }

    if (this.isNew || !data) {
      this.$title.next(prefix + await this.translate.get('TRIP.INDIVIDUAL_RELEASE.NEW.TITLE').toPromise());
    }
    else {
      // Label can be optional (e.g. in auction control)
      const label = this.showLabel && data.label || ('#' + data.rankOrder);
      this.$title.next(prefix + await this.translate.get('TRIP.INDIVIDUAL_RELEASE.EDIT.TITLE', {label}).toPromise());
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
