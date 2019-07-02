import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Injector, Input, OnDestroy, OnInit} from "@angular/core";
import {Observable, Subject} from 'rxjs';
import {map, takeUntil} from "rxjs/operators";
import {TableElement, ValidatorService} from "angular4-material-table";
import {environment, isNil, ReferentialRef} from "../../core/core.module";
import {Batch, getPmfmName, Landing, Operation, PmfmStrategy, referentialToString} from "../services/trip.model";
import {PmfmLabelPatterns, ReferentialRefService} from "../../referential/referential.module";
import {isNotNil} from "../../shared/shared.module";
import {AppMeasurementsTable} from "../measurement/measurements.table.class";
import {InMemoryTableDataService} from "../../shared/services/memory-data-service.class";
import {UsageMode} from "../../core/services/model";
import {SubBatchesModal} from "./sub-batches.modal";
import {BatchModal} from "./batch.modal";
import {measurementValueToString} from "../services/model/measurement.model";
import {BatchUtils} from "../services/model/batch.model";
import {BatchesContext} from "./batches-context.class";


export interface BatchFilter {
  operationId?: number;
  landingId?: number;
}

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

  static RESERVED_START_COLUMNS: string[] = ['taxonGroup', 'taxonName'];
  static RESERVED_END_COLUMNS: string[] = ['comments'];

  protected _initialPmfms: PmfmStrategy[];
  protected cd: ChangeDetectorRef;
  protected referentialRefService: ReferentialRefService;

  qvPmfm: PmfmStrategy;
  defaultWeightPmfm: PmfmStrategy;
  weightPmfmsByMethod: { [key: string]: PmfmStrategy };

  @Input() context: BatchesContext;

  @Input()
  set value(data: Batch[]) {
    this.memoryDataService.value = data;
  }

  get value(): Batch[] {
    return this.memoryDataService.value;
  }

  get isOnFieldMode(): boolean {
    return this.usageMode ? this.usageMode === 'FIELD' : this.settingsService.isUsageMode('FIELD');
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

  @Input() defaultTaxonGroup: ReferentialRef;
  @Input() defaultTaxonName: ReferentialRef;

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
        reservedStartColumns: BatchesTable.RESERVED_START_COLUMNS,
        reservedEndColumns: BatchesTable.RESERVED_END_COLUMNS,
        mapPmfms: (pmfms) => this.mapPmfms(pmfms)
      }
    );
    this.referentialRefService = injector.get(ReferentialRefService);
    this.cd = injector.get(ChangeDetectorRef);
    this.i18nColumnPrefix = 'TRIP.BATCH.TABLE.';
    this.inlineEdition = !this.mobile;

    //this.debug = false;
    this.debug = !environment.production;
  }

  async ngOnInit() {
    super.ngOnInit();

    this.setShowColumn('comments', this.showCommentsColumn);

    if (!this.context) {
      console.warn("[sub-batches-table] No input context! Creating new...");
      this.context = new BatchesContext();
    }

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
    this.addRowToTable();
    return new Promise<TableElement<Batch>>((resolve) => {
      setTimeout(() => {
        const rankOrder = this.editedRow.currentData.rankOrder;
        newBatch.rankOrder = rankOrder;

        // Adapt measurement values to row
        this.normalizeRowMeasurementValues(newBatch, this.editedRow);

        this.editedRow.currentData = newBatch;
        this.confirmEditCreate(null, this.editedRow);
        this.markForCheck();
        resolve(this.editedRow);
      }, 100);
    });
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
    modal.present();

    // Wait until closed
    const {data} = await modal.onDidDismiss();
    if (data && this.debug) console.debug("[batches-table] Batch modal result: ", data);
    return (data instanceof Batch) ? data : undefined;
  }

  async openSubBatchesModal(event: UIEvent, row: TableElement<Batch>) {
    if (event) event.preventDefault();

    let parentBatch = row.validator ? Batch.fromObject(row.currentData) : row.currentData;

    // Use sampling batch (if exists)
    parentBatch = BatchUtils.getSamplingChild(parentBatch) || parentBatch;

    // Define a function to add new parent
    const onNewParentClick = this.mobile ? async () => {
      const newBatch = await this.openDetailModal();
      await this.addBatchToTable(newBatch);
      return newBatch;
    } : undefined;

    // Define available parent, as an observable (if new parent can added)
    const onModalDismiss = new Subject<any>();
    const availableParents = this.mobile ? this.dataSource.connect()
      .pipe(
        takeUntil(onModalDismiss),
        map((res) => res.map((row) => row.currentData as Batch))
      ) : Observable.of((await this.dataSource.getRows()).map((row) => row.currentData as Batch));

    // Define parent batch as default
    this.context.setDefault(parentBatch);

    const modal = await this.modalCtrl.create({
      component: SubBatchesModal,
      componentProps: {
        program: this.program,
        acquisitionLevel: this.acquisitionLevel,
        usageMode: this.usageMode,
        parent: parentBatch,
        value: this.getSubBatches(parentBatch),
        showTaxonNameColumn: this.showTaxonNameColumn,
        availableParents: availableParents,
        onNewParentClick: onNewParentClick,
        context: this.context
      }, keyboardClose: true
    });

    // Open the modal
    modal.present();

    // Wait until closed
    const {data} = await modal.onDidDismiss();
    onModalDismiss.next(); // disconnect to service
    if (data && this.debug) console.debug("[batches-table] Sub-batches modal result: ", data);
    return (data instanceof Array) ? data : undefined;
  }

  async getIndividualMeasureParent(): Promise<Batch[]> {
    if (this._dirty) await this.save();
    const batches = this.memoryDataService.value;

    console.log("getParentBatchForInidivualMeasure ", batches);

    return batches;
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
    this.qvPmfm = pmfms.find(p => p.type === 'qualitative_value');

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

