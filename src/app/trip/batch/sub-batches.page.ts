import {ChangeDetectionStrategy, Component, Inject, Injector, Input, OnDestroy, OnInit, ViewChild} from "@angular/core";
import {TableElement, ValidatorService} from "angular4-material-table";
import {Batch} from "../services/model/batch.model";
import {AcquisitionLevelCodes} from "../../core/services/model";
import {LocalSettingsService} from "../../core/services/local-settings.service";
import {SubBatchForm} from "./sub-batch.form";
import {environment} from "../../../environments/environment";
import {SubBatchValidatorService} from "../services/sub-batch.validator";
import {SubBatchesTable, SubBatchesTableOptions} from "./sub-batches.table";
import {AppMeasurementsTableOptions} from "../measurement/measurements.table.class";
import {measurementValueToString} from "../services/model/measurement.model";
import {PmfmStrategy} from "../../referential/services/model";

@Component({
  selector: 'app-sub-batches',
  templateUrl: 'sub-batches.page.html',
  providers: [
    {provide: ValidatorService, useClass: SubBatchValidatorService},
    {
      provide: SubBatchesTableOptions,
      useFactory: () => {
        return {
          prependNewElements: true,
          suppressErrors: false,
          reservedStartColumns: SubBatchesPage.RESERVED_START_COLUMNS,
          reservedEndColumns: SubBatchesPage.RESERVED_END_COLUMNS
        };
      }
    },
    LocalSettingsService
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SubBatchesPage extends SubBatchesTable implements OnInit, OnDestroy {

  static RESERVED_START_COLUMNS: string[] = ['parent', 'taxonName'];
  static RESERVED_END_COLUMNS: string[] = ['comments'];


  debug = false;
  private _parent: Batch;

  @ViewChild('form') form: SubBatchForm;

  @Input()
  set parent(parent: Batch) {
    this._parent = parent;
    this.value = parent.children || [];
  }

  constructor(
    protected injector: Injector,
    protected settings: LocalSettingsService,
    @Inject(SubBatchesTableOptions) options: AppMeasurementsTableOptions<Batch>
  ) {
    super(injector, null, options);

    // Default value
    this.acquisitionLevel = AcquisitionLevelCodes.SORTING_BATCH_INDIVIDUAL;

    // TODO: for DEV only
    this.debug = !environment.production;
    this.inlineEdition = false;
    this.allowRowDetail = false;
  }

  ngOnInit(): void {
    super.ngOnInit();

    // Init the form
    this.resetForm();
  }

  ngOnDestroy(): void {
  }

  protected async onNewEntity(data: Batch): Promise<void> {
    await super.onNewEntity(data);

    data.parent = this._parent;

  }

  clickRow2(event: MouseEvent, row: TableElement<Batch>): boolean {

    const batch = row.validator ? Batch.fromObject(row.currentData) : row.currentData;
    this.form.value = batch;
    this.form.enable();
    this.form.markAsPristine();

    return true;
  }

  async resetForm() {
    // Create a new
    const newBatch = new Batch();
    await this.onNewEntity(newBatch);

    const formData = new Batch();


    this.form.value = newBatch;
    this.form.enable();
    this.form.markAsPristine();
  }

  async addBatch() {
    const subBatch = this.form.value;
    console.log("TODO: add to table", subBatch);

    this.resetForm();

    // Add the row
    this.memoryDataService.value = [subBatch].concat(this.memoryDataService.value);
    //this.onRefresh.emit();

    this.markForCheck();
  }

  test() {
    this.form.value = new Batch();
    this.form.enable();
  }

  /* -- protected methods -- */

  protected markForCheck() {
    this.cd.markForCheck();
  }

  measurementValueToString(value: any, pmfm: PmfmStrategy): string {
    console.log("Display pmfm: ", value);

    return measurementValueToString(value, pmfm);

  }

  //measurementValueToString = measurementValueToString;

}
