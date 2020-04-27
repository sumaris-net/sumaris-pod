import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  Inject,
  Injector,
  Input,
  OnDestroy,
  OnInit
} from "@angular/core";
import {TableElement, ValidatorService} from "angular4-material-table";
import {environment, referentialToString, TableDataService} from "../../core/core.module";
import {PhysicalGearValidatorService} from "../services/physicalgear.validator";
import {AppMeasurementsTable} from "../measurement/measurements.table.class";
import {InMemoryTableDataService} from "../../shared/services/memory-data-service.class";
import {MeasurementValuesUtils} from "../services/model/measurement.model";
import {PhysicalGearModal} from "./physicalgear.modal";
import {AcquisitionLevelCodes} from "../services/model/base.model";
import {PhysicalGear, Trip} from "../services/model/trip.model";
import {PHYSICAL_GEAR_DATA_SERVICE, PhysicalGearFilter} from "../services/physicalgear.service";

export const GEAR_RESERVED_START_COLUMNS: string[] = ['gear'];
export const GEAR_RESERVED_END_COLUMNS: string[] = ['comments'];


@Component({
  selector: 'app-physicalgears-table',
  templateUrl: 'physicalgears.table.html',
  styleUrls: ['physicalgears.table.scss'],
  providers: [
    {provide: ValidatorService, useExisting: PhysicalGearValidatorService},
    {
      provide: PHYSICAL_GEAR_DATA_SERVICE,
      useFactory: () => new InMemoryTableDataService<PhysicalGear, PhysicalGearFilter>(PhysicalGear)
    }
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class PhysicalGearTable extends AppMeasurementsTable<PhysicalGear, PhysicalGearFilter> implements OnInit, OnDestroy {

  protected cd: ChangeDetectorRef;
  protected memoryDataService: InMemoryTableDataService<PhysicalGear>;

  set value(data: PhysicalGear[]) {
    this.memoryDataService.value = data;
  }

  get value(): PhysicalGear[] {
    return this.memoryDataService.value;
  }

  @Input() canEdit = true;
  @Input() canDelete = true;
  @Input() parent: Trip;

  constructor(
    injector: Injector,
    @Inject(PHYSICAL_GEAR_DATA_SERVICE) dataService?: TableDataService<PhysicalGear, PhysicalGearFilter>
  ) {
    super(injector,
      PhysicalGear,
      dataService,
      null, // No validator = no inline edition
      {
        prependNewElements: false,
        suppressErrors: environment.production,
        reservedStartColumns: GEAR_RESERVED_START_COLUMNS,
        reservedEndColumns: GEAR_RESERVED_END_COLUMNS,
        mapPmfms: (pmfms) => pmfms.filter(p => p.required)
      });
    this.cd = injector.get(ChangeDetectorRef);
    this.memoryDataService = (this.dataService as InMemoryTableDataService<PhysicalGear>);
    this.i18nColumnPrefix = 'TRIP.PHYSICAL_GEAR.LIST.';
    this.autoLoad = false; // waiting parent to be loaded

    // Set default acquisition level
    this.acquisitionLevel = AcquisitionLevelCodes.PHYSICAL_GEAR;

    // FOR DEV ONLY ----
    //this.debug = !environment.production;
  }

  ngOnInit() {
    super.ngOnInit();

    this._enable = this.canEdit;
  }

  protected async openNewRowDetail(): Promise<boolean> {

    const newGear = await this.openDetailModal();
    if (newGear) {
      await this.addGearToTable(newGear);
    }
    return true;
  }

  protected async openRow(id: number, row: TableElement<PhysicalGear>): Promise<boolean> {
    const gear = row.validator ? PhysicalGear.fromObject(row.currentData) : row.currentData;

    const updatedGear = await this.openDetailModal(gear);
    if (updatedGear) {
      await this.addGearToTable(updatedGear, row);
    }
    return true;
  }

  async openDetailModal(gear?: PhysicalGear): Promise<PhysicalGear | undefined> {

    const isNew = !gear;
    if (isNew) {
      gear = new PhysicalGear();
      await this.onNewEntity(gear);
    }

    const modal = await this.modalCtrl.create({
      component: PhysicalGearModal,
      componentProps: {
        program: this.program,
        acquisitionLevel: this.acquisitionLevel,
        disabled: this.disabled,
        value: gear.clone(), // Do a copy, because edition can be cancelled
        isNew: isNew,
        parent: this.parent
      },
      keyboardClose: true
    });

    // Open the modal
    modal.present();

    // Wait until closed
    const {data} = await modal.onDidDismiss();
    if (data && this.debug) console.debug("[physical-gear-table] Modal result: ", data);

    return (data instanceof PhysicalGear) ? data : undefined;
  }

  protected async addGearToTable(gear: PhysicalGear, row?: TableElement<PhysicalGear>): Promise<TableElement<PhysicalGear>> {
    if (this.debug) console.debug("[physical-gear-table] Adding new gear", gear);

    // Create a new row, if need
    if (!row) {
      row = await this.addRowToTable();
      if (!row) throw new Error("Could not add row t table");

      // Use the generated rankOrder
      gear.rankOrder = row.currentData.rankOrder;

    }

    // Adapt measurement values to row
    this.normalizeEntityToRow(gear, row);

    // Affect new row
    if (row.validator) {
      row.validator.patchValue(gear);
      row.validator.markAsDirty();
    }
    else {
      row.currentData = gear;
    }

    this.confirmEditCreate(null, row);
    this.markAsDirty();

    return row;
  }

  referentialToString = referentialToString;
  measurementValueToString = MeasurementValuesUtils.valueToString;

  /* -- protected methods -- */

  protected markForCheck() {
    this.cd.markForCheck();
  }
}


