import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Injector, Input, OnInit, ViewChild} from "@angular/core";
import {Batch, BatchUtils} from "../../services/model/batch.model";
import {LocalSettingsService} from "../../../core/services/local-settings.service";
import {AppFormUtils} from "../../../core/core.module";
import {ModalController} from "@ionic/angular";
import {BehaviorSubject} from "rxjs";
import {TranslateService} from "@ngx-translate/core";
import {AcquisitionLevelCodes} from "../../../referential/services/model/model.enum";
import {PmfmStrategy} from "../../../referential/services/model/pmfm-strategy.model";
import {toBoolean} from "../../../shared/functions";
import {SubBatchForm} from "../form/sub-batch.form";
import {PlatformService} from "../../../core/services/platform.service";
import {SubBatch} from "../../services/model/subbatch.model";

@Component({
  selector: 'app-sub-batch-modal',
  templateUrl: 'sub-batch.modal.html',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SubBatchModal implements OnInit{

  debug = false;
  loading = false;
  mobile: boolean;
  data: SubBatch;
  $title = new BehaviorSubject<string>(undefined);

  @Input() acquisitionLevel: string;

  @Input() program: string;

  @Input() canEdit: boolean;

  @Input() disabled: boolean;

  @Input() isNew: boolean;

  @Input() showParent = true;

  @Input() showTaxonName = true;

  @Input() showIndividualCount = false;

  @Input() qvPmfm: PmfmStrategy;

  @Input() showSampleBatch = false;

  @Input() availableParents: Batch[];

  @Input()
  set value(value: SubBatch) {
    this.data = value;
  }

  @ViewChild('form', { static: true }) form: SubBatchForm;

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


  async ngOnInit() {
    this.canEdit = toBoolean(this.canEdit, !this.disabled);
    this.disabled = !this.canEdit || toBoolean(this.disabled, true);
    this.isNew = toBoolean(this.isNew, false);

    this.data = this.data || new SubBatch();

    // Compute the title
    this.computeTitle();

    await this.form.ready();
    this.form.value = this.data;

    if (!this.isNew) {
      // Update title each time value changes
      this.form.valueChanges.subscribe(batch => this.computeTitle(batch));
    }

    if (!this.disabled) {
      //setTimeout(() => {
        this.form.enable({emitEvent: false});
      //}, 500);
    }

  }

  async cancel() {
    await this.viewCtrl.dismiss();
  }

  async close(event?: UIEvent) {
    if (this.loading) return; // avoid many call

    if (this.invalid) {
      if (this.debug) AppFormUtils.logFormErrors(this.form.form, "[sub-batch-modal] ");
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

  protected async computeTitle(data?: SubBatch) {
    data = data || this.data;
    if (this.isNew || !data) {
      this.$title.next(await this.translate.get('TRIP.SUB_BATCH.NEW.TITLE').toPromise());
    }
    else {
      const label = BatchUtils.parentToString(data);
      this.$title.next(await this.translate.get('TRIP.SUB_BATCH.EDIT.TITLE', {label}).toPromise());
    }
  }
}
