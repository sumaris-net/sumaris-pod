import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Injector, Input, ViewChild} from "@angular/core";
import {Batch, BatchUtils} from "../services/model/batch.model";
import {AcquisitionLevelCodes} from "../../core/services/model";
import {LocalSettingsService} from "../../core/services/local-settings.service";
import {environment} from "../../../environments/environment";
import {PlatformService} from "../../core/core.module";
import {ModalController} from "@ionic/angular";
import {BehaviorSubject} from "rxjs";
import {TranslateService} from "@ngx-translate/core";
import {PmfmStrategy} from "../../referential/services/model";
import {BatchGroupForm} from "./batch-group.form";
import {toBoolean} from "../../shared/functions";
import {throttleTime} from "rxjs/operators";

@Component({
  selector: 'app-batch-group-modal',
  templateUrl: 'batch-group.modal.html',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class BatchGroupModal {

  debug = false;
  loading = false;
  mobile: boolean;
  data: Batch;
  $title = new BehaviorSubject<string>(undefined);

  @Input() acquisitionLevel: string;

  @Input() program: string;

  @Input() canEdit: boolean;

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

  @ViewChild('form') form: BatchGroupForm;

  get dirty(): boolean {
    return this.form.dirty;
  }

  get invalid(): boolean {
    return this.form.invalid;
  }

  get valid(): boolean {
    return this.form.valid;
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
    this.debug = !environment.production;
  }


  ngOnInit() {
    this.form.value = this.data || new Batch();

    this.canEdit = this.isNew || toBoolean(this.canEdit, !this.disabled);
    this.disabled = !this.canEdit || toBoolean(this.disabled, true);

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
      this.form.valueChanges
        .pipe(throttleTime(500))
        .subscribe(batch => this.computeTitle(batch));
    }

  }

  edit() {
    if (!this.canEdit) return;
    this.disabled = false;
    this.form.enable();
    this.markForCheck();
  }

  async cancel() {
    await this.viewCtrl.dismiss();
  }

  async close(event?: UIEvent) {
    if (this.loading) return; // avoid many call

    this.loading = true;

    // Force enable form, before use value
    if (!this.enabled) this.form.enable({emitEvent: false});

    if (this.invalid) {
      if (this.debug) this.form.logErrors("[batch-group-modal] ");
      this.form.error = "COMMON.FORM.HAS_ERROR";
      this.form.markAsTouched();
      this.loading = false;
      return;
    }

    // Save table content
    const data = this.form.value;

    await this.viewCtrl.dismiss(data);
  }

  async openSubBatchesModal(event: UIEvent) {
    await this.close(event);

  }

  /* -- protected methods -- */

  protected markForCheck() {
    this.cd.markForCheck();
  }

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
}
