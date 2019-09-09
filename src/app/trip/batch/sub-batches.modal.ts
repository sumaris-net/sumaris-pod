import {ChangeDetectionStrategy, Component, Inject, Injector, Input, OnInit, ViewChild} from "@angular/core";
import {ValidatorService} from "angular4-material-table";
import {Batch} from "../services/model/batch.model";
import {LocalSettingsService} from "../../core/services/local-settings.service";
import {SubBatchForm} from "./sub-batch.form";
import {environment} from "../../../environments/environment";
import {SubBatchValidatorService} from "../services/sub-batch.validator";
import {SubBatchesTable, SubBatchesTableOptions} from "./sub-batches.table";
import {AppMeasurementsTableOptions} from "../measurement/measurements.table.class";
import {measurementValueToString} from "../services/model/measurement.model";
import {AppFormUtils} from "../../core/core.module";
import {ModalController} from "@ionic/angular";


export const SUB_BATCH_MODAL_RESERVED_START_COLUMNS: string[] = ['parent', 'taxonName'];
export const SUB_BATCH_MODAL_RESERVED_END_COLUMNS: string[] = ['comments']; // do NOT use individual count

@Component({
  selector: 'app-sub-batches-modal',
  templateUrl: 'sub-batches.modal.html',
  providers: [
    {provide: ValidatorService, useExisting: SubBatchValidatorService},
    {
      provide: SubBatchesTableOptions,
      useFactory: () => {
        return {
          prependNewElements: true,
          suppressErrors: false,
          reservedStartColumns: SUB_BATCH_MODAL_RESERVED_START_COLUMNS,
          reservedEndColumns: SUB_BATCH_MODAL_RESERVED_END_COLUMNS
        };
      }
    }
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SubBatchesModal extends SubBatchesTable implements OnInit {

  private _defaultValue: Batch;
  private _parent: Batch;

  @Input()
  set parent(parent: Batch) {
    this._parent = parent;
  }

  @Input() onNewParentClick: () => Promise<Batch | undefined>;

  @ViewChild('form') form: SubBatchForm;

  get dirty(): boolean {
    return this._dirty || this.form.dirty;
  }

  get valid(): boolean {
    return this.form.valid;
  }

  get invalid(): boolean {
    return this.form.invalid;
  }

  set defaultValue(value: Batch) {
    this._defaultValue = value;
  }

  constructor(
    protected injector: Injector,
    protected viewCtrl: ModalController,
    protected settings: LocalSettingsService,
    @Inject(SubBatchesTableOptions) options: AppMeasurementsTableOptions<Batch>
  ) {
    super(injector, null, options);
    this.inlineEdition = false; // Disable row edition (readonly)
    this.allowRowDetail = false; // Disable click on a row
    this.showCommentsColumn = false;

    // TODO: for DEV only
    this.debug = !environment.production;

    this.showIndividualCount = false;
  }

  async ngOnInit() {
    await super.ngOnInit();

    await this.form.onReady();

    // Init the form
    this.setValueFromParent(this.availableParents as Batch[], this.qvPmfm);

    if (this._defaultValue) {
      const initBatch = (this._defaultValue instanceof Batch) ? this._defaultValue.clone() : Batch.fromObject(this._defaultValue);
      initBatch.parent = this._defaultValue.parent;
      await this.resetForm(initBatch);
    }
    else {
      await this.resetForm();
    }
  }

  async close(event?: UIEvent) {
    if (this.loading) return; // avoid many call

    if (this.debug && this.form.invalid) {
      AppFormUtils.logFormErrors(this.form.form, "[sub-batch-modal] ");
      // Continue
    }

    this.loading = true;
    this.error = undefined;

    // Save table content
    try {
      await this.save();
      const data = this.memoryDataService.value;
      await this.viewCtrl.dismiss(data);
    } catch (err) {
      console.error(err);
      this.error = err && err.message || err;
      this.loading = false;
    }
  }

  /* -- protected methods -- */

  measurementValueToString = measurementValueToString;

}
