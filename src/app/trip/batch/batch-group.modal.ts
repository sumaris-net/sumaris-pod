import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Injector, Input, ViewChild} from "@angular/core";
import {ValidatorService} from "angular4-material-table";
import {Batch, BatchUtils} from "../services/model/batch.model";
import {AcquisitionLevelCodes} from "../../core/services/model";
import {LocalSettingsService} from "../../core/services/local-settings.service";
import {environment} from "../../../environments/environment";
import {AppFormUtils, PlatformService} from "../../core/core.module";
import {ModalController} from "@ionic/angular";
import {BatchForm} from "./batch.form";
import {BatchValidatorService} from "../services/batch.validator";
import {PhysicalGear} from "../services/model/trip.model";
import {BehaviorSubject} from "rxjs";
import {TranslateService} from "@ngx-translate/core";
import {SpeciesBatchValidatorService} from "../services/validator/species-batch.validator";
import {PmfmStrategy} from "../../referential/services/model";
import {BatchGroupForm} from "./batch-group.form";

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

  @Input() disabled = false;

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
    if (this.disabled) {
      this.form.disable();
    }

    this.form.value = this.data || new Batch();

    // Compute the title
    this.computeTitle();

    if (!this.isNew) {
      // Update title each time value changes
      this.form.valueChanges.subscribe(batch => this.computeTitle(batch));
    }

  }

  async cancel() {
    await this.viewCtrl.dismiss();
  }

  async close(event?: UIEvent) {
    if (this.loading) return; // avoid many call

    if (this.invalid) {
      if (this.debug) this.form.logErrors("[batch-modal] ");
      this.form.error = "COMMON.FORM.HAS_ERROR";
      this.form.markAsTouched();
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
