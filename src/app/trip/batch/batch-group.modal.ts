import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  Injector,
  Input,
  OnDestroy,
  OnInit,
  ViewChild
} from "@angular/core";
import {Batch, BatchUtils} from "../services/model/batch.model";
import {LocalSettingsService} from "../../core/services/local-settings.service";
import {AppFormUtils} from "../../core/core.module";
import {ModalController} from "@ionic/angular";
import {BehaviorSubject, Subscription} from "rxjs";
import {TranslateService} from "@ngx-translate/core";
import {AcquisitionLevelCodes, PmfmStrategy} from "../../referential/services/model";
import {BatchGroupForm} from "./batch-group.form";
import {toBoolean} from "../../shared/functions";
import {throttleTime} from "rxjs/operators";
import {PlatformService} from "../../core/services/platform.service";

@Component({
  selector: 'app-batch-group-modal',
  templateUrl: 'batch-group.modal.html',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class BatchGroupModal implements OnInit, OnDestroy {

  private _subscription = new Subscription();

  debug = false;
  loading = false;
  mobile: boolean;
  data: Batch;
  $title = new BehaviorSubject<string>(undefined);

  @Input() acquisitionLevel: string;

  @Input() program: string;

  @Input() disabled: boolean;

  @Input() isNew = false;

  @Input() showTaxonGroup = true;

  @Input() showTaxonName = true;

  @Input() showIndividualCount = false;

  @Input() showTotalIndividualCount = false;

  @Input() qvPmfm: PmfmStrategy;

  @Input()
  set value(value: Batch) {
    this.data = value;
  }

  @Input() showSubBatchesCallback: (batch) => void;

  @ViewChild('form', { static: true }) form: BatchGroupForm;

  get dirty(): boolean {
    return this.form.dirty;
  }

  get invalid(): boolean {
    return this.form.invalid;
  }

  get valid(): boolean {
    return this.form.valid;
  }

  get pending(): boolean {
    return this.form.pending;
  }

  get enabled(): boolean {
    return !this.disabled;
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
    this.acquisitionLevel = AcquisitionLevelCodes.SORTING_BATCH;
    this.mobile = platform.mobile;

    // TODO: for DEV only
    //this.debug = !environment.production;
  }

  ngOnInit() {

    this.form.setValue(this.data || new Batch());

    this.disabled = toBoolean(this.disabled, false);

    if (this.disabled) {
      this.form.disable();
    }
    else {
      this.form.enable();
    }

    // Compute the title
    this.computeTitle();

    if (!this.isNew) {
      // Update title each time value changes
      this._subscription.add(
        this.form.valueChanges
          .pipe(
            throttleTime(500)
          )
          .subscribe(batch => this.computeTitle(batch))
      );
    }
  }

  ngOnDestroy(): void {
    this._subscription.unsubscribe();
  }

  async cancel() {
    await this.viewCtrl.dismiss();
  }

  async close(event?: UIEvent): Promise<Batch | undefined> {
    if (this.loading) return; // avoid many call

    this.loading = true;

    // Force enable form, before use value
    if (!this.enabled) this.form.enable({emitEvent: false});

    // Wait end of async validation
    await AppFormUtils.waitWhilePending(this);

    if (this.invalid) {
      this.form.error = "COMMON.FORM.HAS_ERROR";
      this.form.logFormErrors("[batch-group-modal] ");
      this.form.markAsTouched({emitEvent: true});
      this.loading = false;
      return;
    }

    // Save table content
    const data = this.form.value;

    await this.viewCtrl.dismiss(data);

    return data;
  }

  async onShowSubBatchesButtonClick(event: UIEvent) {

    // Close
    const batch = await this.close(event);

    // If closed succeed, execute the callback
    if (batch && this.showSubBatchesCallback) {
      this.showSubBatchesCallback(batch);
    }
  }

  /* -- protected methods -- */

  protected async computeTitle(data?: Batch) {
    data = data || this.data;
    if (this.isNew || !data) {
      this.$title.next(await this.translate.get('TRIP.BATCH.NEW.TITLE').toPromise());
    }
    else {
      const label = BatchUtils.parentToString(data);
      this.$title.next(await this.translate.get('TRIP.BATCH.EDIT.TITLE', {label}).toPromise());
    }
  }

  protected markForCheck() {
    this.cd.markForCheck();
  }
}
