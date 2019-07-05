import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Injector, OnDestroy, OnInit} from "@angular/core";
import {TableElement, ValidatorService} from "angular4-material-table";
import {AcquisitionLevelCodes, environment} from "../../core/core.module";
import {PhysicalGearValidatorService} from "../services/physicalgear.validator";
import {PhysicalGear, referentialToString} from "../services/trip.model";
import {AppMeasurementsTable} from "../measurement/measurements.table.class";
import {InMemoryTableDataService} from "../../shared/services/memory-data-service.class";
import {measurementValueToString} from "../services/model/measurement.model";
import {PhysicalGearModal} from "./physicalgear.modal";

export const GEAR_RESERVED_START_COLUMNS: string[] = ['gear'];
export const GEAR_RESERVED_END_COLUMNS: string[] = ['comments'];

@Component({
  selector: 'table-physical-gears',
  templateUrl: 'physicalgears.table.html',
  styleUrls: ['physicalgears.table.scss'],
  providers: [
    {provide: ValidatorService, useClass: PhysicalGearValidatorService}
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class PhysicalGearTable extends AppMeasurementsTable<PhysicalGear, any> implements OnInit, OnDestroy {

  protected cd: ChangeDetectorRef;
  protected memoryDataService: InMemoryTableDataService<PhysicalGear, any>;

  set value(data: PhysicalGear[]) {
    this.memoryDataService.value = data;
    this.markForCheck();
  }

  get value(): PhysicalGear[] {
    return this.memoryDataService.value;
  }

  constructor(
    injector: Injector
  ) {
    super(injector, PhysicalGear,
      new InMemoryTableDataService<PhysicalGear, any>(PhysicalGear),
      null, // No validator = no inline edition
      {
        prependNewElements: false,
        suppressErrors: true,
        reservedStartColumns: GEAR_RESERVED_START_COLUMNS,
        reservedEndColumns: GEAR_RESERVED_END_COLUMNS,
        mapPmfms: (pmfms) => pmfms.filter(p => p.isMandatory)
      });
    this.cd = injector.get(ChangeDetectorRef);
    this.memoryDataService = (this.dataService as InMemoryTableDataService<PhysicalGear, any>);
    this.i18nColumnPrefix = 'TRIP.PHYSICAL_GEAR.LIST.';
    this.autoLoad = false;

    // Default acquisition level
    this.acquisitionLevel = AcquisitionLevelCodes.PHYSICAL_GEAR;

    // FOR DEV ONLY ----
    this.debug = !environment.production;
  }

  ngOnInit() {
    super.ngOnInit();
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
        isNew: isNew
      }, keyboardClose: true
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
    this.normalizeRowMeasurementValues(gear, row);

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
  measurementValueToString = measurementValueToString;

  /* -- protected methods -- */


  protected markForCheck() {
    this.cd.markForCheck();
  }
}


