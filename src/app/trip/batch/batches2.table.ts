import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Injector, Input, OnDestroy, OnInit} from "@angular/core";
import {Observable} from 'rxjs';
import {debounceTime, filter, first, switchMap, tap} from "rxjs/operators";
import {TableElement, ValidatorService} from "angular4-material-table";
import {
  EntityUtils,
  environment,
  isNil,
  ReferentialRef,
  RESERVED_END_COLUMNS,
  RESERVED_START_COLUMNS
} from "../../core/core.module";
import {
  Batch,
  getPmfmName, Landing,
  MeasurementUtils, Operation,
  PmfmStrategy,
  referentialToString,
  Sample,
  TaxonGroupIds
} from "../services/trip.model";
import {ReferentialRefService} from "../../referential/referential.module";
import {BatchValidatorService} from "../services/batch.validator";
import {FormGroup} from "@angular/forms";
import {TaxonomicLevelIds} from "src/app/referential/services/model";
import {isNotNil, LoadResult} from "../../shared/shared.module";
import {AppMeasurementsTable} from "../measurement/measurements.table.class";
import {InMemoryTableDataService} from "../../shared/services/memory-data-service.class";
import {UsageMode} from "../../core/services/model";


export interface BatchFilter {
  operationId?: number;
  landingId?: number;
}

@Component({
  selector: 'app-batches-table',
  templateUrl: 'batches.table.html',
  styleUrls: ['batches.table.scss'],
  providers: [
    {provide: ValidatorService, useClass: BatchValidatorService}
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class Batches2Table extends AppMeasurementsTable<Batch, BatchFilter>
  implements OnInit, OnDestroy {

  static RESERVED_START_COLUMNS: string[] = ['label', 'taxonGroup', 'taxonName'];
  static RESERVED_END_COLUMNS: string[] = ['comments'];

  protected cd: ChangeDetectorRef;
  protected referentialRefService: ReferentialRefService;
  protected memoryDataService: InMemoryTableDataService<Batch, BatchFilter>;

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

  @Input() showLabelColumn = false;
  @Input() showCommentsColumn = true;

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
    injector: Injector
  ) {
    super(injector,
      Batch,
      new InMemoryTableDataService<Batch, BatchFilter>(Batch),
      injector.get(ValidatorService),
      {
        prependNewElements: false,
        suppressErrors: true,
        reservedStartColumns: Batches2Table.RESERVED_START_COLUMNS,
        reservedEndColumns: Batches2Table.RESERVED_END_COLUMNS
      }
    );
    this.referentialRefService = injector.get(ReferentialRefService);
    this.cd = injector.get(ChangeDetectorRef);
    this.memoryDataService = (this.dataService as InMemoryTableDataService<Batch, BatchFilter>);
    this.i18nColumnPrefix = 'TRIP.BATCH.TABLE.';
    this.inlineEdition = true;

    //this.debug = false;
    this.debug = !environment.production;
  };

  async ngOnInit() {
    super.ngOnInit();

    this.setShowColumn('label', this.showLabelColumn);
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

  protected async onNewEntity(data: Batch): Promise<void> {
    console.debug("[sample-table] Initializing new row data...");
    const isOnFieldMode = this.isOnFieldMode;

    // generate label
    if (!this.showLabelColumn) {
      data.label = this.acquisitionLevel + "#" + data.rankOrder;
    }

    // Taxon group
    if (isNotNil(this.defaultTaxonName)) {
      data.taxonName = this.defaultTaxonName;
    }

    // Default taxon group
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

