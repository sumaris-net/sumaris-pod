import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  InjectionToken,
  Injector,
  Input,
  OnDestroy,
  OnInit, Optional
} from "@angular/core";
import {Observable} from 'rxjs';
import {debounceTime, switchMap, tap} from "rxjs/operators";
import {ValidatorService} from "angular4-material-table";
import {environment, ReferentialRef} from "../../core/core.module";
import {
  Batch,
  getPmfmName,
  Landing,
  Operation,
  PmfmStrategy,
  referentialToString,
  TaxonGroupIds
} from "../services/trip.model";
import {ReferentialRefService} from "../../referential/referential.module";
import {BatchValidatorService} from "../services/batch.validator";
import {TaxonomicLevelIds} from "src/app/referential/services/model";
import {isNotNil} from "../../shared/shared.module";
import {AppMeasurementsTable, AppMeasurementsTableOptions} from "../measurement/measurements.table.class";
import {InMemoryTableDataService} from "../../shared/services/memory-data-service.class";
import {UsageMode} from "../../core/services/model";


export interface BatchFilter {
  operationId?: number;
  landingId?: number;
}

export declare const BATCH_TABLE_OPTIONS: InjectionToken<AppMeasurementsTableOptions<Batch>>;

export function createBatchInMemoryService(): InMemoryTableDataService<Batch, BatchFilter> {
  return new InMemoryTableDataService<Batch, BatchFilter>(Batch, {});
}

@Component({
  selector: 'app-batches-table',
  templateUrl: 'batches.table.html',
  styleUrls: ['batches.table.scss'],
  providers: [
    {provide: ValidatorService, useClass: BatchValidatorService},
    {
      provide: InMemoryTableDataService,
      useFactory: createBatchInMemoryService
    }
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class BatchesTable extends AppMeasurementsTable<Batch, BatchFilter>
  implements OnInit, OnDestroy {

  static RESERVED_START_COLUMNS: string[] = ['taxonGroup', 'taxonName'];
  static RESERVED_END_COLUMNS: string[] = ['comments'];

  protected cd: ChangeDetectorRef;
  protected referentialRefService: ReferentialRefService;

  $taxonGroups: Observable<ReferentialRef[]>;
  $taxonNames: Observable<ReferentialRef[]>;

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
    protected memoryDataService: InMemoryTableDataService<Batch, BatchFilter>
  ) {
    super(injector,
      Batch,
      memoryDataService,
      injector.get(ValidatorService),
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
    this.inlineEdition = true;

    //this.debug = false;
    this.debug = !environment.production;
  }

  async ngOnInit() {
    super.ngOnInit();

    this.setShowColumn('comments', this.showCommentsColumn);

    // Taxon group combo
    this.$taxonGroups = this.registerCellValueChanges('taxonGroup')
      .pipe(
        debounceTime(250),
        switchMap((value) => this.referentialRefService.suggest(value, {
          entityName: 'TaxonGroup',
          levelId: TaxonGroupIds.FAO,
          searchAttribute: 'label'
        })),
        // Remember implicit value
        tap(res => this.updateImplicitValue('taxonGroup', res))
      );

    // Taxon name combo
    this.$taxonNames = this.registerCellValueChanges('taxonName')
      .pipe(
        debounceTime(250),
        switchMap((value) => this.referentialRefService.suggest(value, {
          entityName: 'TaxonName',
          levelId: TaxonomicLevelIds.SPECIES,
          searchAttribute: 'label'
        })),
        // Remember implicit value
        tap(res => this.updateImplicitValue('taxonName', res))
      );
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

  /* -- protected methods -- */

  protected mapPmfms(pmfms: PmfmStrategy[]): PmfmStrategy[] {
    return pmfms;
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

  protected markForCheck() {
    this.cd.markForCheck();
  }
}

