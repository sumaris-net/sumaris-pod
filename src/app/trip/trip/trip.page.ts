import {ChangeDetectionStrategy, Component, Injector, OnInit, ViewChild} from '@angular/core';

import {TripService} from '../services/trip.service';
import {TripForm} from './trip.form';
import {Trip} from '../services/trip.model';
import {SaleForm} from '../sale/sale.form';
import {OperationTable} from '../operation/operations.table';
import {MeasurementsForm} from '../measurement/measurements.form.component';
import {NetworkService} from '../../core/core.module';
import {PhysicalGearTable} from '../physicalgear/physicalgears.table';
import {EditorDataServiceLoadOptions, fadeInOutAnimation, isNil, isNotNil} from '../../shared/shared.module';
import {EntityQualityFormComponent} from "../quality/entity-quality-form.component";
import * as moment from "moment";
import {ProgramProperties} from "../../referential/services/model";
import {AppDataEditorPage} from "../form/data-editor-page.class";
import {FormGroup} from "@angular/forms";

@Component({
  selector: 'app-trip-page',
  templateUrl: './trip.page.html',
  styleUrls: ['./trip.page.scss'],
  animations: [fadeInOutAnimation],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class TripPage extends AppDataEditorPage<Trip, TripService> implements OnInit {


  showSaleForm = false;
  showGearTable = false;
  showOperationTable = false;

  @ViewChild('tripForm') tripForm: TripForm;

  @ViewChild('saleForm') saleForm: SaleForm;

  @ViewChild('physicalGearTable') physicalGearTable: PhysicalGearTable;

  @ViewChild('measurementsForm') measurementsForm: MeasurementsForm;

  @ViewChild('operationTable') operationTable: OperationTable;

  @ViewChild('qualityForm') qualityForm: EntityQualityFormComponent;

  constructor(
    injector: Injector,
    public network: NetworkService // Used for DEV (to debug OFFLINE mode)
  ) {
    super(injector,
      Trip,
      injector.get(TripService));
    this.idAttribute = 'tripId';
    this.defaultBackHref = "/trips";

    // FOR DEV ONLY ----
    //this.debug = !environment.production;
  }

  ngOnInit() {
    super.ngOnInit();

    // Watch program, to configure tables from program properties
    this.registerSubscription(
      this.onProgramChanged
        .subscribe(program => {
          if (this.debug) console.debug(`[trip] Program ${program.label} loaded, with properties: `, program.properties);
          this.showSaleForm = program.getPropertyAsBoolean(ProgramProperties.TRIP_SALE_ENABLE);
        })
    );
  }

  protected registerFormsAndTables() {
    this.registerForms([this.tripForm, this.saleForm, this.measurementsForm])
      .registerTables([this.physicalGearTable, this.operationTable]);
  }

  protected async onNewEntity(data: Trip, options?: EditorDataServiceLoadOptions): Promise<void> {
    if (this.isOnFieldMode) {
      data.departureDateTime = moment();

      // TODO : get the default program from local settings ?
      //data.program = ...
    }

    this.showGearTable = false;
    this.showOperationTable = false;

  }

  updateViewState(data: Trip) {
    super.updateViewState(data);

    if (this.isNewData) {
      this.showGearTable = false;
      this.showOperationTable = false;
    }
    else {
      this.showGearTable = true;
      this.showOperationTable = true;
    }
  }

  protected async setValue(data: Trip): Promise<void> {

    this.tripForm.value = data;
    const isNew = isNil(data.id);
    if (!isNew) {
      this.programSubject.next(data.program.label);
    }
    this.saleForm.value = data && data.sale;
    this.measurementsForm.value = data && data.measurements || [];
    //this.measurementsForm.updateControls();

    // Physical gear table
    this.physicalGearTable.value = data && data.gears || [];

    // Operations table
    if (!isNew && this.operationTable) {
      this.operationTable.setTrip(data);
    }
  }


  enable() {
    if (!this.data || isNotNil(this.data.validationDate)) return false;
    const isNew = this.isNewData;
    // If not a new data, check user can write
    if (!isNew && !this.dataService.canUserWrite(this.data)) {
      if (this.debug) console.warn("[trip] Leave form disable (User has NO write access)");
      return;
    }
    if (this.debug) console.debug("[trip] Enabling form (User has write access)");

    super.enable();

    // Leave program disable once saved
    if (!isNew) {
      this.tripForm.form.controls['program'].disable();
    }
  }

  async onOpenOperation({id, row}) {
    const savedOrContinue = await this.saveIfDirtyAndConfirm();
    if (savedOrContinue) {
      await this.router.navigateByUrl(`/trips/${this.data.id}/operations/${id}`);
    }
  }

  async onNewOperation(event?: any) {
    const savedOrContinue = await this.saveIfDirtyAndConfirm();
    if (savedOrContinue) {
      await this.router.navigateByUrl(`/trips/${this.data.id}/operations/new`);
    }
  }

  /* -- protected methods -- */

  protected get form(): FormGroup {
    return this.tripForm.form;
  }

  /**
   * Compute the title
   * @param data
   */
  protected async computeTitle(data: Trip) {

    // new data
    if (!data || isNil(data.id)) {
      return await this.translate.get('TRIP.NEW.TITLE').toPromise();
    }

    // Existing data
    return await this.translate.get('TRIP.EDIT.TITLE', {
      vessel: data.vesselFeatures && (data.vesselFeatures.exteriorMarking || data.vesselFeatures.name),
      departureDateTime: data.departureDateTime && this.dateFormat.transform(data.departureDateTime) as string
    }).toPromise();
  }

  protected async getValue(): Promise<Trip> {
    const data = await super.getValue();

    // Update gears, from table
    if (this.physicalGearTable.dirty) {
      await this.physicalGearTable.save();
    }
    data.gears = this.physicalGearTable.value;

    return data;
  }


  protected async getJsonForm(): Promise<any> {
    const json = await super.getJsonForm();

    json.sale = !this.saleForm.empty ? this.saleForm.value : null;
    json.measurements = this.measurementsForm.value;

    return json;
  }

  /**
   * Get the first invalid tab
   */
  protected getFirstInvalidTabIndex(): number {
    const tab0Invalid = this.tripForm.invalid || this.measurementsForm.invalid;
    const tab1Invalid = !tab0Invalid && this.physicalGearTable.invalid;
    const tab2Invalid = !tab1Invalid && this.operationTable.invalid;

    return tab0Invalid ? 0 : (tab1Invalid ? 1 : (tab2Invalid ? 2 : this.selectedTabIndex));
  }

  protected markForCheck() {
    this.cd.markForCheck();
  }
}
