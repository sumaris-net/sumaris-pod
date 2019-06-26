import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  Injector,
  Input,
  OnDestroy,
  OnInit,
  ViewChild
} from "@angular/core";

import {BehaviorSubject, Observable, Subject} from 'rxjs';
import {map} from 'rxjs/operators';
import {TableElement, ValidatorService} from "angular4-material-table";
import {
  AccountService, AcquisitionLevelCodes,
  AppTable,
  AppTableDataSource,
  environment,
  RESERVED_START_COLUMNS
} from "../../core/core.module";
import {PhysicalGearValidatorService} from "../services/physicalgear.validator";
import {Batch, EntityUtils, PhysicalGear, referentialToString, Sample} from "../services/trip.model";
import {ModalController, Platform} from "@ionic/angular";
import {ActivatedRoute, Router} from "@angular/router";
import {Location} from '@angular/common';
import {PhysicalGearForm} from "./physicalgear.form";
import {LoadResult, TableDataService} from "../../shared/shared.module";
import {AppMeasurementsTable} from "../measurement/measurements.table.class";
import {InMemoryTableDataService} from "../../shared/services/memory-data-service.class";
import {SampleFilter} from "../sample/samples.table";
import {measurementValueToString} from "../services/model/measurement.model";
import {SubBatchesPage} from "../batch/sub-batches.page";
import {PhysicalGearModal} from "./physicalgear.modal";
import {undefinedVarMessage} from "graphql/validation/rules/NoUndefinedVariables";


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

  static RESERVED_START_COLUMNS: string[] = ['gear'];
  static RESERVED_END_COLUMNS: string[] = ['comments'];

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
      null,
      {
        prependNewElements: false,
        suppressErrors: true,
        reservedStartColumns: PhysicalGearTable.RESERVED_START_COLUMNS,
        reservedEndColumns: PhysicalGearTable.RESERVED_END_COLUMNS,
        mapPmfms: (pmfms) => pmfms.filter(p => p.isMandatory)
      });
    this.cd = injector.get(ChangeDetectorRef);
    this.memoryDataService = (this.dataService as InMemoryTableDataService<PhysicalGear, any>);
    this.i18nColumnPrefix = 'TRIP.PHYSICAL_GEAR.LIST.';
    this.autoLoad = false;
    this.inlineEdition = false;

    // Default acquisition level
    this.acquisitionLevel = AcquisitionLevelCodes.PHYSICAL_GEAR;

    // FOR DEV ONLY ----
    this.debug = !environment.production;
  }

  ngOnInit() {
    super.ngOnInit();
  }

  protected async openNewRowDetail(): Promise<boolean> {

    const newGear = await this.openPhysicalGearModal();
    if (newGear) {
      console.debug("[physical-gear-table] Adding new physical gear", newGear);
      this.addRowToTable();
      setTimeout(() => {
        const rankOrder = this.editedRow.currentData.rankOrder;
        newGear.rankOrder = rankOrder;
        this.editedRow.currentData = newGear;
        this.confirmEditCreate(null, this.editedRow);
        this.markForCheck();
      }, 100);
    }
    return true;
  }

  protected async openRow(id: number, row: TableElement<PhysicalGear>): Promise<boolean> {
    const gear = row.validator ? PhysicalGear.fromObject(row.currentData) : row.currentData;

    const updatedGear = await this.openPhysicalGearModal(gear);
    if (updatedGear) {
      row.currentData = updatedGear;
      this._dirty = true;
      this.markForCheck();
      this.confirmEditCreate(null, row);
    }
    return true;
  }

  async openPhysicalGearModal(gear?: PhysicalGear): Promise<PhysicalGear | undefined> {

    let isNew = !gear;
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

    return (data instanceof PhysicalGear) ? data : undefined;
  }

  referentialToString = referentialToString;
  measurementValueToString = measurementValueToString;

  /* -- protected methods -- */


  protected markForCheck() {
    this.cd.markForCheck();
  }
}


