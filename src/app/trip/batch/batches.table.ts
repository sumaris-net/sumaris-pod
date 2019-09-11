import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  EventEmitter,
  Injector,
  Input,
  OnDestroy,
  OnInit,
  Output
} from "@angular/core";
import {Observable, Subject} from 'rxjs';
import {map, takeUntil} from "rxjs/operators";
import {TableElement, ValidatorService} from "angular4-material-table";
import {AcquisitionLevelCodes, environment, isNil, ReferentialRef} from "../../core/core.module";
import {Batch, getPmfmName, Landing, Operation, PmfmStrategy, referentialToString} from "../services/trip.model";
import {PmfmIds, PmfmLabelPatterns, ReferentialRefService} from "../../referential/referential.module";
import {isNotNil} from "../../shared/shared.module";
import {AppMeasurementsTable} from "../measurement/measurements.table.class";
import {InMemoryTableDataService} from "../../shared/services/memory-data-service.class";
import {UsageMode} from "../../core/services/model";
import {SubBatchesModal} from "./sub-batches.modal";
import {BatchModal} from "./batch.modal";
import {measurementValueToString} from "../services/model/measurement.model";
import {BatchUtils} from "../services/model/batch.model";
import {isNotEmptyArray} from "../../shared/functions";


export interface BatchFilter {
  operationId?: number;
  landingId?: number;
}

export const BATCH_RESERVED_START_COLUMNS: string[] = ['taxonGroup', 'taxonName'];
export const BATCH_RESERVED_END_COLUMNS: string[] = ['comments'];

@Component({
  selector: 'app-batches-table',
  templateUrl: 'batches.table.html',
  styleUrls: ['batches.table.scss'],
  providers: [
    {provide: ValidatorService, useValue: null},  // important: do NOT use validator, to be sure to keep all PMFMS, and not only displayed pmfms
    {
      provide: InMemoryTableDataService,
      useFactory: () => new InMemoryTableDataService<Batch, BatchFilter>(Batch, {})
    }
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class BatchesTable extends AppMeasurementsTable<Batch, BatchFilter>
  implements OnInit, OnDestroy {

  protected _initialPmfms: PmfmStrategy[];
  protected cd: ChangeDetectorRef;
  protected referentialRefService: ReferentialRefService;

  qvPmfm: PmfmStrategy;
  defaultWeightPmfm: PmfmStrategy;
  weightPmfmsByMethod: { [key: string]: PmfmStrategy };

  @Input()
  set value(data: Batch[]) {
    this.memoryDataService.value = data;
  }

  get value(): Batch[] {
    return this.memoryDataService.value;
  }

  get isOnFieldMode(): boolean {
    return this.usageMode ? this.usageMode === 'FIELD' : this.settings.isUsageMode('FIELD');
  }

  @Input() usageMode: UsageMode;

  @Input()
  set showTaxonGroupColumn(value: boolean) {
    this.setShowColumn('taxonGroup', value);
  }

  get showTaxonGroupColumn(): boolean {
    return this.getShowColumn('taxonGroup');
  }

  @Input()
  set showTaxonNameColumn(value: boolean) {
    this.setShowColumn('taxonName', value);
  }

  get showTaxonNameColumn(): boolean {
    return this.getShowColumn('taxonName');
  }

  get dirty(): boolean {
    return this._dirty || this.memoryDataService.dirty;
  }

  @Input()
  availableSubBatchesFn: (defaultBatch: Batch) => Promise<Batch[]>;

  @Input() defaultTaxonGroup: ReferentialRef;
  @Input() defaultTaxonName: ReferentialRef;

  @Output()
  onNewSubBatches = new EventEmitter<Batch[]>();

  constructor(
    injector: Injector,
    protected validatorService: ValidatorService,
    protected memoryDataService: InMemoryTableDataService<Batch, BatchFilter>
  ) {
    super(injector,
      Batch,
      memoryDataService,
      validatorService,
      {
        prependNewElements: false,
        suppressErrors: true,
        reservedStartColumns: BATCH_RESERVED_START_COLUMNS,
        reservedEndColumns: BATCH_RESERVED_END_COLUMNS,
        mapPmfms: (pmfms) => this.mapPmfms(pmfms)
      }
    );
    this.referentialRefService = injector.get(ReferentialRefService);
    this.cd = injector.get(ChangeDetectorRef);
    this.i18nColumnPrefix = 'TRIP.BATCH.TABLE.';
    this.inlineEdition = !this.mobile;

    // Set default value
    this.acquisitionLevel = AcquisitionLevelCodes.SORTING_BATCH;

    //this.debug = false;
    this.debug = !environment.production;
  }

  ngOnInit() {
    super.ngOnInit();

    this.setShowColumn('comments', this.showCommentsColumn);

    // Configuration autocomplete fields
    this.registerAutocompleteField('taxonGroup');
    this.registerAutocompleteField('taxonName');
  }

  setParent(data: Operation | Landing) {
    if (!data) {
      this.setFilter({});
    } else if (data instanceof Operation) {
      this.setFilter({operationId: data.id});
    } else if (data instanceof Landing) {
      this.setFilter({landingId: data.id});
    }
  }

  protected async openNewRowDetail(): Promise<boolean> {
    const newBatch = await this.openDetailModal();
    if (newBatch) {
      await this.addBatchToTable(newBatch);
    }
    return true;
  }

  protected async addBatchToTable(newBatch: Batch): Promise<TableElement<Batch>> {
    console.debug("[batches-table] Adding new batch", newBatch);

    const row = await this.addRowToTable();
    if (!row) throw new Error("Could not add row t table");

    // Adapt measurement values to row
    this.normalizeRowMeasurementValues(newBatch, row);

    // Override rankOrder (keep computed value)
    newBatch.rankOrder = row.currentData.rankOrder;

    // Affect new row
    if (row.validator) {
      row.validator.patchValue(newBatch);
      row.validator.markAsDirty();
    } else {
      row.currentData = newBatch;
    }

    this.confirmEditCreate(null, row);
    this.markAsDirty();
    return row;
  }

  protected async openRow(id: number, row: TableElement<Batch>): Promise<boolean> {
    const gear = row.validator ? Batch.fromObject(row.currentData) : row.currentData;

    const updatedBatch = await this.openDetailModal(gear);
    if (updatedBatch) {
      // Adapt measurement values to row
      this.normalizeRowMeasurementValues(updatedBatch, row);

      // Update the row
      row.currentData = updatedBatch;

      this.markAsDirty();
      this.markForCheck();
      this.confirmEditCreate(null, row);
    }
    return true;
  }

  async openDetailModal(batch?: Batch): Promise<Batch | undefined> {

    const isNew = !batch;
    if (isNew) {
      batch = new Batch();
      await this.onNewEntity(batch);
    }

    const modal = await this.modalCtrl.create({
      component: BatchModal,
      componentProps: {
        program: this.program,
        acquisitionLevel: this.acquisitionLevel,
        disabled: this.disabled,
        value: batch.clone(), // Do a copy, because edition can be cancelled
        isNew: isNew,
        showTaxonGroup: this.showTaxonGroupColumn,
        showTaxonName: this.showTaxonNameColumn,
        showIndividualCount: false // Not need on a root species batch (fill in sub-batches)
      }, keyboardClose: true
    });

    // Open the modal
    await modal.present();

    // Wait until closed
    const {data} = await modal.onDidDismiss();
    if (data && this.debug) console.debug("[batches-table] Batch modal result: ", data);
    return (data instanceof Batch) ? data : undefined;
  }

  async onSubBatchesClick(event: UIEvent, row: TableElement<Batch>) {
    if (event) event.preventDefault();

    let parentBatch = row.validator ? Batch.fromObject(row.currentData) : row.currentData;

    // Use sampling batch (if exists)
    parentBatch = BatchUtils.getSamplingChild(parentBatch) || parentBatch;

    const defaultBatch = new Batch();
    defaultBatch.parent = parentBatch;

    await this.openSubBatchesModal(defaultBatch);
  }

  async openSubBatchesModal(defaultValue?: Batch): Promise<Batch[]> {

    // Define a function to add new parent
    const onNewParentClick = this.mobile ? async () => {
      const newBatch = await this.openDetailModal();
      if (!newBatch) return undefined;
      await this.addBatchToTable(newBatch);
      return newBatch;
    } : undefined;

    // Define available parent, as an observable (if new parent can added)
    const onModalDismiss = new Subject<any>();
    const availableParents =
      // If mobile, create an observable, linked to table rows
      this.mobile ? this.dataSource.connect()
      .pipe(
        takeUntil(onModalDismiss),
        map((res) => res.map((row) => row.currentData as Batch))
      ) :
      // else, create a copy
      Observable.of((await this.dataSource.getRows()).map((row) => row.currentData as Batch));

    const modal = await this.modalCtrl.create({
      component: SubBatchesModal,
      componentProps: {
        program: this.program,
        acquisitionLevel: AcquisitionLevelCodes.SORTING_BATCH_INDIVIDUAL,
        usageMode: this.usageMode,
        defaultValue: defaultValue,
        qvPmfm: this.qvPmfm,
        // Scientific species is required, if not set in root batches
        showTaxonNameColumn: !this.showTaxonNameColumn,
        availableParents: availableParents,
        availableSubBatchesFn: this.availableSubBatchesFn,
        onNewParentClick: onNewParentClick
      }, keyboardClose: true
    });

    // Open the modal
    await modal.present();

    // Wait until closed
    const {data} = await modal.onDidDismiss();
    onModalDismiss.next(); // disconnect to service

    console.debug("[batches-table] Sub-batches modal result: ", data);

    if (isNotEmptyArray(data)) {

      //if (data && this.debug) console.debug("[batches-table] Sub-batches modal result: ", data);

      this.onNewSubBatches.emit(data);
    }

    return data;
  }

  /* -- protected methods -- */

  /**
   * Allow to remove/Add some pmfms. Can be oerrive by subclasses
   * @param pmfms
   */
  protected mapPmfms(pmfms: PmfmStrategy[]): PmfmStrategy[] {
    if (!pmfms || !pmfms.length) return pmfms; // Skip (no pmfms)

    this._initialPmfms = pmfms; // Copy original pmfms list

    this.defaultWeightPmfm = undefined;
    this.weightPmfmsByMethod = pmfms.reduce((res, p) => {
      const matches = PmfmLabelPatterns.BATCH_WEIGHT.exec(p.label);
      if (matches) {
        const methodId = p.methodId;
        res[methodId] = p;
        if (isNil(this.defaultWeightPmfm)) this.defaultWeightPmfm = p;
      }
      return res;
    }, {});

    // Find the first qualitative PMFM
    let qvPmfm = pmfms.find(p => p.type === 'qualitative_value');
    // If landing/discard: 'Landing' is always before 'Discard (see issue #122)
    if (qvPmfm && qvPmfm.pmfmId === PmfmIds.DISCARD_OR_LANDING) {
      qvPmfm = qvPmfm.clone(); // copy, to keep original array
      qvPmfm.qualitativeValues.sort((qv1, qv2) => qv1.label === 'LAN' ? -1 : 1);
    }
    this.qvPmfm = qvPmfm;

    // Remove weight pmfms
    return pmfms.filter(p => !p.isWeight);
  }

  protected getSubBatches(batch: Batch): Batch[] {
    return batch.children;
  }

  protected async onNewEntity(data: Batch): Promise<void> {
    console.debug("[sample-table] Initializing new row data...");

    await super.onNewEntity(data);

    // generate label
    data.label = this.acquisitionLevel + "#" + data.rankOrder;

    // Default values
    if (isNotNil(this.defaultTaxonName)) {
      data.taxonName = this.defaultTaxonName;
    }
    if (isNotNil(this.defaultTaxonGroup)) {
      data.taxonGroup = this.defaultTaxonGroup;
    }
  }

  referentialToString = referentialToString;
  getPmfmColumnHeader = getPmfmName;
  measurementValueToString = measurementValueToString;

  protected markForCheck() {
    this.cd.markForCheck();
  }
}

