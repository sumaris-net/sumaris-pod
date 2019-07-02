import {ChangeDetectionStrategy, Component, Inject, Injector, Input, OnInit, ViewChild} from "@angular/core";
import {ValidatorService} from "angular4-material-table";
import {Batch} from "../services/model/batch.model";
import {AcquisitionLevelCodes} from "../../core/services/model";
import {LocalSettingsService} from "../../core/services/local-settings.service";
import {SubBatchForm} from "./sub-batch.form";
import {environment} from "../../../environments/environment";
import {SubBatchValidatorService} from "../services/sub-batch.validator";
import {SubBatchesTable, SubBatchesTableOptions} from "./sub-batches.table";
import {AppMeasurementsTableOptions} from "../measurement/measurements.table.class";
import {measurementValueToString} from "../services/model/measurement.model";
import {AppFormUtils} from "../../core/core.module";
import {ModalController} from "@ionic/angular";
import {BehaviorSubject} from "rxjs";

@Component({
  selector: 'app-sub-batches-modal',
  templateUrl: 'sub-batches.modal.html',
  providers: [
    {provide: ValidatorService, useClass: SubBatchValidatorService},
    {
      provide: SubBatchesTableOptions,
      useFactory: () => {
        return {
          prependNewElements: true,
          suppressErrors: false,
          reservedStartColumns: SubBatchesModal.RESERVED_START_COLUMNS,
          reservedEndColumns: SubBatchesModal.RESERVED_END_COLUMNS
        };
      }
    }
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SubBatchesModal extends SubBatchesTable implements OnInit {

  static RESERVED_START_COLUMNS: string[] = ['parent', 'taxonName'];
  static RESERVED_END_COLUMNS: string[] = ['comments'];

  private _parent: Batch;

  debug = false;
  $availableParents = new BehaviorSubject<Batch[]>([]);

  @Input()
  set parent(parent: Batch) {
    this._parent = parent;
    this.value = parent.children || [];
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

  constructor(
    protected injector: Injector,
    protected viewCtrl: ModalController,
    protected settings: LocalSettingsService,
    @Inject(SubBatchesTableOptions) options: AppMeasurementsTableOptions<Batch>
  ) {
    super(injector, null, options);

    // Default value
    this.acquisitionLevel = AcquisitionLevelCodes.SORTING_BATCH_INDIVIDUAL;

    this.inlineEdition = false; // Disable row edition (readonly)
    this.allowRowDetail = false; // Disable click on a row
    this.showCommentsColumn = false;

    // TODO: for DEV only
    this.debug = !environment.production;
  }

  ngOnInit(): void {
    super.ngOnInit();

    // Init the form
    this.resetForm();

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
