import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  Inject,
  InjectionToken,
  Injector,
  Input,
  OnDestroy,
  OnInit
} from "@angular/core";
import {TableElement, ValidatorService} from "@e-is/ngx-material-table";
import {isNil, isNilOrBlank, isNotNil} from "../../../shared/functions";
import {AppMeasurementsTable} from "../../measurement/measurements.table.class";
import {InMemoryEntitiesService} from "../../../shared/services/memory-entity-service.class";
import {UsageMode} from "../../../core/services/model/settings.model";
import {MeasurementValuesUtils} from "../../services/model/measurement.model";
import {TaxonNameRef} from "../../../referential/services/model/taxon.model";
import {Batch} from "../../services/model/batch.model";
import {Operation} from "../../services/model/trip.model";
import {Landing} from "../../services/model/landing.model";
import {AcquisitionLevelCodes, PmfmLabelPatterns} from "../../../referential/services/model/model.enum";
import {PmfmUtils} from "../../../referential/services/model/pmfm.model";
import {getPmfmName, PmfmStrategy} from "../../../referential/services/model/pmfm-strategy.model";
import {ReferentialRefService} from "../../../referential/services/referential-ref.service";
import {BatchModal} from "../modal/batch.modal";
import {IReferentialRef, ReferentialRef, referentialToString} from "../../../core/services/model/referential.model";
import {environment} from "../../../../environments/environment";

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
      provide: InMemoryEntitiesService,
      useFactory: () => new InMemoryEntitiesService<Batch, BatchFilter>(Batch, {
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
export class BatchesTable<T extends Batch<any> = Batch<any>, F extends BatchFilter = BatchFilter>
  extends AppMeasurementsTable<T, F>
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

  @Input() defaultTaxonGroup: ReferentialRef;
  @Input() defaultTaxonName: TaxonNameRef;


  constructor(
    injector: Injector,
    validatorService: ValidatorService,
    protected memoryDataService: InMemoryEntitiesService<T, F>,
    @Inject(DATA_TYPE_ACCESSOR) dataType?: new() => T
  ) {
    super(injector,
      dataType || ((Batch as any) as (new() => T)),
      memoryDataService,
      validatorService,
      {
        prependNewElements: false,
        suppressErrors: environment.production,
        reservedStartColumns: BATCH_RESERVED_START_COLUMNS,
        reservedEndColumns: BATCH_RESERVED_END_COLUMNS,
        mapPmfms: (pmfms) => this.mapPmfms(pmfms)
      }
    );
    this.cd = injector.get(ChangeDetectorRef);
    this.referentialRefService = injector.get(ReferentialRefService);
    this.i18nColumnPrefix = 'TRIP.BATCH.TABLE.';
    this.inlineEdition = this.validatorService && !this.mobile;

    // Set default value
    this.showCommentsColumn = false;
    this.acquisitionLevel = AcquisitionLevelCodes.SORTING_BATCH;

    //this.debug = false;
    //this.debug = !environment.production;
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
    if (!this.allowRowDetail) return false;

    const data = await this.openDetailModal();
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

    const data = this.toEntity(row, true);

    // Prepare entity measurement values
    this.prepareEntityToSave(data);

    const updatedData = await this.openDetailModal(data);
    if (updatedData) {
      await this.updateEntityToTable(updatedData, row, {confirmCreate: false});
    }
    else {
      this.editedRow = null;
    }
    return true;
  }

  async openDetailModal(batch?: T): Promise<T | undefined> {
    const isNew = !batch && true;
    if (isNew) {
      batch = new this.dataType();
      await this.onNewEntity(batch);
    }

    this.markAsLoading();

    const modal = await this.modalCtrl.create({
      component: BatchModal,
      componentProps: {
        program: this.program,
        acquisitionLevel: this.acquisitionLevel,
        disabled: this.disabled,
        value: batch,
        isNew,
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
    this.markAsLoaded();

    // Exit if empty
    if (!(data instanceof Batch)) {
      return undefined;
    }

    return data as T;
  }


  /* -- protected methods -- */

  protected async suggestTaxonGroups(value: any, options?: any): Promise<IReferentialRef[]> {
    //if (isNilOrBlank(value)) return [];
    return this.programRefService.suggestTaxonGroups(value,
      {
        program: this.program,
        searchAttribute: options && options.searchAttribute
      });
  }

  protected async suggestTaxonNames(value: any, options?: any): Promise<IReferentialRef[]> {
    const taxonGroup = this.editedRow && this.editedRow.validator.get('taxonGroup').value;

    // IF taxonGroup column exists: taxon group must be filled first
    if (this.showTaxonGroupColumn && isNilOrBlank(value) && isNil(taxonGroup)) return [];

    return this.programRefService.suggestTaxonNames(value,
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

