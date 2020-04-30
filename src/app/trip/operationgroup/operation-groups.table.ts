import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  EventEmitter,
  Injector,
  Input,
  OnDestroy,
  OnInit, Output
} from "@angular/core";
import {environment, referentialToString} from "../../core/core.module";
import {Platform} from "@ionic/angular";
import {AcquisitionLevelCodes, PmfmStrategy} from "../../referential/services/model";
import {OperationFilter} from "../services/operation.service";
import {AppMeasurementsTable} from "../measurement/measurements.table.class";
import {OperationGroupValidatorService} from "../services/validator/operation-group.validator";
import {MetierRef} from "../../referential/services/model/taxon.model";
import {BehaviorSubject} from "rxjs";
import {MeasurementValuesUtils} from "../services/model/measurement.model";
import {TableElement, ValidatorService} from "angular4-material-table";
import {InMemoryTableDataService} from "../../shared/services/memory-data-service.class";
import {MetierRefService} from "../../referential/services/metier-ref.service";
import {OperationGroup, PhysicalGear} from "../services/model/trip.model";

export const OPERATION_GROUP_RESERVED_START_COLUMNS: string[] = ['metier', 'physicalGear', 'targetSpecies'];
export const OPERATION_GROUP_RESERVED_END_COLUMNS: string[] = ['comments'];

@Component({
  selector: 'app-operation-group-table',
  templateUrl: 'operation-groups.table.html',
  styleUrls: ['operation-groups.table.scss'],
  providers: [
    {provide: ValidatorService, useExisting: OperationGroupValidatorService},
    {
      provide: InMemoryTableDataService,
      useFactory: () => new InMemoryTableDataService<OperationGroup, OperationFilter>(OperationGroup)
    }
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class OperationGroupTable extends AppMeasurementsTable<OperationGroup, OperationFilter> implements OnInit, OnDestroy {

  displayAttributes: {
    [key: string]: string[]
  };

  @Input()
  set value(data: OperationGroup[]) {
    this.memoryDataService.value = data;
  }

  get value(): OperationGroup[] {
    return this.memoryDataService.value;
  }

  get dirty(): boolean {
    return this._dirty || this.memoryDataService.dirty;
  }

  @Input() $metiers: BehaviorSubject<MetierRef[]>;

  @Output() change: EventEmitter<void> = new EventEmitter<void>();

  constructor(
    injector: Injector,
    protected platform: Platform,
    protected validatorService: ValidatorService,
    protected memoryDataService: InMemoryTableDataService<OperationGroup, OperationFilter>,
    protected metierRefService: MetierRefService,
    protected cd: ChangeDetectorRef
  ) {
    super(injector,
      OperationGroup,
      memoryDataService,
      validatorService,
      {
        prependNewElements: false,
        suppressErrors: environment.production,
        reservedStartColumns: OPERATION_GROUP_RESERVED_START_COLUMNS,
        reservedEndColumns: platform.is('mobile') ? [] : OPERATION_GROUP_RESERVED_END_COLUMNS,
        mapPmfms: (pmfms) => this.mapPmfms(pmfms),
      });
    this.i18nColumnPrefix = 'TRIP.OPERATION.LIST.';
    this.autoLoad = false; // waiting parent to be loaded
    this.inlineEdition = true;
    this.confirmBeforeDelete = true;
    this.pageSize = 1000; // Do not use paginator

    // Set default acquisition level
    this.acquisitionLevel = AcquisitionLevelCodes.OPERATION;

    // FOR DEV ONLY ----
    this.debug = !environment.production;
  }

  ngOnInit() {
    super.ngOnInit();

    this.displayAttributes = {
      gear: this.settings.getFieldDisplayAttributes('gear'),
      taxonGroup: ['taxonGroup.label', 'taxonGroup.name']
    };

    // Metier combo
    this.registerAutocompleteField('metier', {
      showAllOnFocus: true,
      items: this.$metiers
    });

    // Apply trip id, if already set
    // if (isNotNil(this.tripId)) {
    //   this.setTripId(this.tripId);
    // }
  }

  // setTrip(data: Trip) {
  //   this.setTripId(data && data.id || undefined);
  // }
  //
  // setTripId(id: number) {
  //   this.tripId = id;
  //   const filter = this.filter || {};
  //   filter.tripId = id;
  //   this.dataSource.serviceOptions = this.dataSource.serviceOptions || {};
  //   this.dataSource.serviceOptions.tripId = id;
  //   this.setFilter(filter, {emitEvent: isNotNil(id)});
  // }

  referentialToString = referentialToString;

  /* -- protected methods -- */

  protected markForCheck() {
    this.cd.markForCheck();
    if (this.dirty) {
      this.change.emit();
    }
  }

  private mapPmfms(pmfms: PmfmStrategy[]): PmfmStrategy[] {

    if (this.platform.is('mobile')) {
      // hide pmfms on mobile
      return [];
    }

    return pmfms;
  }

  protected async addRowToTable(): Promise<TableElement<OperationGroup>> {
    const row = await super.addRowToTable();

    row.validator.controls['rankOrderOnPeriod'].setValue(this.getNextRankOrderOnPeriod());
    // row.validator.controls['rankOrderOnPeriod'].updateValueAndValidity();

    return row;
  }

  getNextRankOrderOnPeriod(): number {
    let next = 0;
    (this.value || []).forEach(v => {
      if (v.rankOrderOnPeriod && v.rankOrderOnPeriod > next) next = v.rankOrderOnPeriod;
    });
    return next + 1;
  }

  async onMetierChange($event: FocusEvent, row: TableElement<OperationGroup>) {
    if (row && row.currentData && row.currentData.metier) {
      console.debug('[operation-group.table] onMetierChange', $event, row.currentData.metier);
      const operationGroup: OperationGroup = row.currentData;

      if (!operationGroup.physicalGear || operationGroup.physicalGear.gear.id !== operationGroup.metier.gear.id) {

        // First, load the Metier (with children)
        const metier = await this.metierRefService.load(operationGroup.metier.id);

        // create new physical gear if missing
        const physicalGear = new PhysicalGear();
        physicalGear.gear = metier.gear;
        // affect same rank order than operation group
        physicalGear.rankOrder = operationGroup.rankOrderOnPeriod;

        // affect to current row
        row.validator.controls['metier'].setValue(metier);
        row.validator.controls['physicalGear'].setValue(physicalGear);
      }

    }
  }
}

