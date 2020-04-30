import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  EventEmitter,
  Inject,
  InjectionToken,
  Injector,
  Input,
  OnDestroy,
  OnInit,
  Optional,
  Output
} from "@angular/core";
import {of, Subject} from 'rxjs';
import {map, takeUntil} from "rxjs/operators";
import {TableElement, ValidatorService} from "angular4-material-table";
import {environment, IReferentialRef, isNil, ReferentialRef, referentialToString} from "../../core/core.module";
import {
  AcquisitionLevelCodes,
  getPmfmName,
  PmfmLabelPatterns,
  PmfmStrategy,
  PmfmUtils,
  ReferentialRefService
} from "../../referential/referential.module";
import {isNilOrBlank, isNotNil} from "../../shared/functions";
import {AppMeasurementsTable} from "../measurement/measurements.table.class";
import {InMemoryTableDataService} from "../../shared/services/memory-data-service.class";
import {UsageMode} from "../../core/services/model";
import {SubBatchesModal} from "./sub-batches.modal";
import {MeasurementValuesUtils} from "../services/model/measurement.model";
import {BatchModal} from "./batch.modal";
import {MatDialog} from '@angular/material/dialog';
import {TaxonNameRef} from "../../referential/services/model/taxon.model";
import {Batch} from "../services/model/batch.model";
import {Operation} from "../services/model/trip.model";
import {Landing} from "../services/model/landing.model";

export interface BatchFilter {
  operationId?: number;
  landingId?: number;
}

export const BATCH_RESERVED_START_COLUMNS: string[] = ['taxonGroup', 'taxonName'];
export const BATCH_RESERVED_END_COLUMNS: string[] = ['comments'];

export const DATA_TYPE_ACCESSOR = new InjectionToken<new() => Batch>('BatchesTableDataType');

@Component({
  selector: 'app-batches-table',
  templateUrl: 'batches.table.html',
  styleUrls: ['batches.table.scss'],
  providers: [
    {provide: ValidatorService, useValue: null},  // important: do NOT use validator, to be sure to keep all PMFMS, and not only displayed pmfms
    {
      provide: InMemoryTableDataService,
      useFactory: () => new InMemoryTableDataService<Batch, BatchFilter>(Batch, {
        equals: Batch.equals
      })
    },
    {
      provide: DATA_TYPE_ACCESSOR,
      useValue: Batch
    }
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class BatchesTable<T extends Batch = Batch, F extends BatchFilter = BatchFilter> extends AppMeasurementsTable<T, F>
  implements OnInit, OnDestroy {

  protected _initialPmfms: PmfmStrategy[];
  protected cd: ChangeDetectorRef;
  protected referentialRefService: ReferentialRefService;

  qvPmfm: PmfmStrategy;
  defaultWeightPmfm: PmfmStrategy;
  weightPmfmsByMethod: { [key: string]: PmfmStrategy };

  @Input()
  set value(data: T[]) {
    this.memoryDataService.value = data;
  }

  get value(): T[] {
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
  availableSubBatchesFn: () => Promise<Batch[]>;

  @Input() defaultTaxonGroup: ReferentialRef;
  @Input() defaultTaxonName: TaxonNameRef;

  @Output()
  onSubBatchesChanges = new EventEmitter<Batch[]>();

  private matDialog: MatDialog;

  constructor(
    injector: Injector,
    protected validatorService: ValidatorService,
    protected memoryDataService: InMemoryTableDataService<T, F>,
    @Inject(DATA_TYPE_ACCESSOR) dataType?: new() => T
  ) {
    super(injector,
      dataType || ((Batch as any) as (new() => T)),
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
    this.matDialog = injector.get(MatDialog);

    // Set default value
    this.showCommentsColumn = false;
    this.acquisitionLevel = AcquisitionLevelCodes.SORTING_BATCH;

    //this.debug = false;
    this.debug = !environment.production;
  }

  ngOnInit() {
    super.ngOnInit();

    // Taxon group combo
    this.registerAutocompleteField('taxonGroup', {
      suggestFn: (value: any, options?: any) => this.suggestTaxonGroups(value, options)
    });

    // Taxon name combo
    this.registerAutocompleteField('taxonName', {
      suggestFn: (value: any, options?: any) => this.suggestTaxonNames(value, options)
    });
  }

  setParent(data: Operation | Landing) {
    if (!data) {
      this.setFilter({} as F);
    } else if (data instanceof Operation) {
      this.setFilter({operationId: data.id} as F);
    } else if (data instanceof Landing) {
      this.setFilter({landingId: data.id} as F);
    }
  }

  protected async openNewRowDetail(): Promise<boolean> {
    const data = await this.openDetailModal(new this.dataType(), {isNew: true});
    if (data) {
      await this.addEntityToTable(data);
    }
    return true;
  }


  protected async openRow(id: number, row: TableElement<T>): Promise<boolean> {

    if (!this.allowRowDetail) return false;

    if (this.onOpenRow.observers.length) {
      this.onOpenRow.emit({id, row});
      return true;
    }

    const data = this.getRowEntity(row, true);

    // Prepare entity measurement values
    this.prepareEntityToSave(data);

    const updatedData = await this.openDetailModal(data);
    if (updatedData) {
      await this.updateEntityToTable(updatedData, row);
    }
    else {
      this.editedRow = null;
    }
    return true;
  }

  async openDetailModal(batch?: Batch, opts?: {isNew?: boolean}): Promise<T | undefined> {
    const modal = await this.modalCtrl.create({
      component: BatchModal,
      componentProps: {
        program: this.program,
        acquisitionLevel: this.acquisitionLevel,
        value: batch,
        isNew: opts && opts.isNew || false,
        disabled: this.disabled,
        qvPmfm: this.qvPmfm,
        showTaxonGroup: this.showTaxonGroupColumn,
        showTaxonName: this.showTaxonNameColumn,
        // Not need on a root species batch (fill in sub-batches)
        showTotalIndividualCount: false,
        showIndividualCount: false
      },
      keyboardClose: true
    });

    // Open the modal
    await modal.present();

    // Wait until closed
    const {data} = await modal.onDidDismiss();
    if (data && this.debug) console.debug("[batches-table] Batch modal result: ", data);
    return (data instanceof Batch) ? data as T : undefined;
  }

  async onSubBatchesClick(event: UIEvent, row: TableElement<T>, opts?: {
    showParent?: boolean;
  }): Promise<Batch[] | undefined> {
    if (event) event.preventDefault();
    const selectedParent = this.getRowEntity(row);
    return await this.openSubBatchesModal(selectedParent, opts);
  }

  async openSubBatchesModal(selectedParent?: T, opts?: {
    showParent?: boolean;
  }): Promise<Batch[] | undefined> {

    if (this.debug) console.debug("[batches-table] Open individual measures modal...");

    const showParent = !opts || opts.showParent !== false; // True by default

    // Define a function to add new parent
    const onNewParentClick = showParent ? async () => {
      const newBatch = await this.openDetailModal();
      if (!newBatch) return undefined;
      await this.addEntityToTable(newBatch);
      return newBatch;
    } : undefined;

    // Define available parent, as an observable (if new parent can added)
    const onModalDismiss = new Subject<any>();
    const availableParents =
      // If mobile, create an observable, linked to table rows
      this.mobile ?
        this.dataSource.connect()
        .pipe(
          takeUntil(onModalDismiss),
          map((res) => res.map((row) => row.currentData as T))
        ) :
      // else (if desktop), create a copy
      of((await this.dataSource.getRows()).map(row => this.getRowEntity(row)));

    const modal = await this.modalCtrl.create({
      component: SubBatchesModal,
      componentProps: {
        program: this.program,
        acquisitionLevel: AcquisitionLevelCodes.SORTING_BATCH_INDIVIDUAL,
        usageMode: this.usageMode,
        selectedParent: selectedParent,
        qvPmfm: this.qvPmfm,
        disabled: this.disabled,
        showParent,
        // Scientific species is required, if not set in root batches
        showTaxonNameColumn: !this.showTaxonNameColumn,
        // If on field mode: use individualCount=1 on each sub-batches
        showIndividualCount: !this.isOnFieldMode,
        availableParents: availableParents,
        availableSubBatchesFn: this.availableSubBatchesFn,
        onNewParentClick
      },
      keyboardClose: true,
      cssClass: 'modal-large'
    });

    // Open the modal
    await modal.present();

    // Wait until closed
    const {data} = await modal.onDidDismiss();

    onModalDismiss.next(); // disconnect to service

    // User cancelled
    if (isNil(data)) {
      if (this.debug) console.debug("[batches-table] Sub-batches modal: user cancelled");
    }
    else {
      if (this.debug) console.debug("[batches-table] Sub-batches modal result: ", data);
      this.onSubBatchesChanges.emit(data);
    }

    return data;
  }

  /* -- protected methods -- */

  protected async suggestTaxonGroups(value: any, options?: any): Promise<IReferentialRef[]> {
    //if (isNilOrBlank(value)) return [];
    return this.programService.suggestTaxonGroups(value,
      {
        program: this.program,
        searchAttribute: options && options.searchAttribute
      });
  }

  protected async suggestTaxonNames(value: any, options?: any): Promise<IReferentialRef[]> {
    const taxonGroup = this.editedRow && this.editedRow.validator.get('taxonGroup').value;

    // IF taxonGroup column exists: taxon group must be filled first
    if (this.showTaxonGroupColumn && isNilOrBlank(value) && isNil(taxonGroup)) return [];

    return this.programService.suggestTaxonNames(value,
      {
        program: this.program,
        searchAttribute: options && options.searchAttribute,
        taxonGroupId: taxonGroup && taxonGroup.id || undefined
      });
  }

  protected prepareEntityToSave(batch: T) {
    // Override by subclasses
  }

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
    this.qvPmfm = PmfmUtils.getFirstQualitativePmfm(pmfms);

    // Remove weight pmfms
    return pmfms.filter(p => !p.isWeight);
  }

  protected getSubBatches(batch: Batch): Batch[] {
    return batch.children;
  }

  protected async onNewEntity(data: T): Promise<void> {
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
  measurementValueToString = MeasurementValuesUtils.valueToString;

  protected markForCheck() {
    this.cd.markForCheck();
  }
}

