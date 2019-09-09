import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Injector, Input, OnDestroy, OnInit} from "@angular/core";
import {Observable} from 'rxjs';
import {debounceTime, switchMap, tap} from "rxjs/operators";
import {ValidatorService} from "angular4-material-table";
import {environment, ReferentialRef} from "../../core/core.module";
import {getPmfmName, Landing, Operation, referentialToString, Sample, TaxonGroupIds} from "../services/trip.model";
import {ReferentialRefService, TaxonomicLevelIds} from "../../referential/referential.module";
import {SampleValidatorService} from "../services/sample.validator";
import {isNotNil} from "../../shared/shared.module";
import {UsageMode} from "../../core/services/model";
import * as moment from "moment";
import {Moment} from "moment";
import {AppMeasurementsTable} from "../measurement/measurements.table.class";
import {InMemoryTableDataService} from "../../shared/services/memory-data-service.class";

export const SAMPLE_RESERVED_START_COLUMNS: string[] = ['label', 'taxonGroup', 'taxonName', 'sampleDate'];
export const SAMPLE_RESERVED_END_COLUMNS: string[] = ['comments'];

export interface SampleFilter {
  operationId?: number;
  landingId?: number;
}

@Component({
  selector: 'app-samples-table',
  templateUrl: 'samples.table.html',
  styleUrls: ['samples.table.scss'],
  providers: [
    {provide: ValidatorService, useExisting: SampleValidatorService}
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SamplesTable extends AppMeasurementsTable<Sample, SampleFilter>
  implements OnInit, OnDestroy {

  protected cd: ChangeDetectorRef;
  protected referentialRefService: ReferentialRefService;
  protected memoryDataService: InMemoryTableDataService<Sample, SampleFilter>;

  $taxonGroups: Observable<ReferentialRef[]>;
  $taxonNames: Observable<ReferentialRef[]>;

  @Input()
  set value(data: Sample[]) {
    this.memoryDataService.value = data;
  }

  get value(): Sample[] {
    return this.memoryDataService.value;
  }

  get isOnFieldMode(): boolean {
    return this.usageMode ? this.usageMode === 'FIELD' : this.settings.isUsageMode('FIELD');
  }

  @Input() usageMode: UsageMode;

  @Input() showLabelColumn = false;
  @Input() showCommentsColumn = true;
  @Input() showDateTimeColumn = true;

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

  @Input() defaultSampleDate: Moment;
  @Input() defaultTaxonGroup: ReferentialRef;
  @Input() defaultTaxonName: ReferentialRef;

  constructor(
    injector: Injector
  ) {
    super(injector,
      Sample,
      new InMemoryTableDataService<Sample, SampleFilter>(Sample),
      injector.get(ValidatorService),
      {
        prependNewElements: false,
        suppressErrors: true,
        reservedStartColumns: SAMPLE_RESERVED_START_COLUMNS,
        reservedEndColumns: SAMPLE_RESERVED_END_COLUMNS
      }
    );
    this.cd = injector.get(ChangeDetectorRef);
    this.referentialRefService = injector.get(ReferentialRefService);
    this.memoryDataService = (this.dataService as InMemoryTableDataService<Sample, SampleFilter>);
    this.i18nColumnPrefix = 'TRIP.SAMPLE.TABLE.';
    this.inlineEdition = true;

    //this.debug = false;
    this.debug = !environment.production;
  }

  ngOnInit() {
    super.ngOnInit();

    this.setShowColumn('label', this.showLabelColumn);
    this.setShowColumn('sampleDate', this.showDateTimeColumn);
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

  protected async onNewEntity(data: Sample): Promise<void> {
    console.debug("[sample-table] Initializing new row data...");

    await super.onNewEntity(data);

    const isOnFieldMode = this.isOnFieldMode;

    // generate label
    if (!this.showLabelColumn) {
      data.label = `${this.acquisitionLevel}#${data.rankOrder}`;
    }

    // Default date
    if (isNotNil(this.defaultSampleDate)) {
      data.sampleDate = this.defaultSampleDate;
    } else if (isOnFieldMode) {
      data.sampleDate = moment();
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

