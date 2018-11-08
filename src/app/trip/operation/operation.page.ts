import { Component, OnInit, ViewChild } from '@angular/core';
import { Router, ActivatedRoute } from "@angular/router";
import { OperationService } from '../services/operation.service';
import { OperationForm } from './operation.form';
import { Operation, Trip, Batch } from '../services/trip.model';
import { TripService } from '../services/trip.service';
import { MeasurementsForm } from '../measurement/measurements.form';
import { AppTabPage, AppFormUtils } from '../../core/core.module';
import { CatchForm } from '../catch/catch.form';
import { SamplesTable } from '../sample/samples.table';
import { SubSamplesTable } from '../sample/sub-samples.table';
import { AlertController } from "@ionic/angular";
import { TranslateService } from '@ngx-translate/core';
import { AcquisitionLevelCodes, isNotNil, isNil } from '../../core/services/model';
import { PmfmIds } from '../../referential/services/model';
import { environment } from 'src/environments/environment';
import { Subject } from 'rxjs';
import { DateFormatPipe } from 'src/app/shared/pipes/date-format.pipe';
@Component({
  selector: 'page-operation',
  templateUrl: './operation.page.html',
  styleUrls: ['./operation.page.scss']
})
export class OperationPage extends AppTabPage<Operation, { tripId: number }> implements OnInit {

  title = new Subject<string>();
  trip: Trip;
  saving: boolean = false;
  rankOrder: number;

  @ViewChild('opeForm') opeForm: OperationForm;

  @ViewChild('measurementsForm') measurementsForm: MeasurementsForm;

  @ViewChild('catchForm') catchForm: CatchForm;

  @ViewChild('survivalTestsTable') survivalTestsTable: SamplesTable;

  @ViewChild('individualMonitoringTable') individualMonitoringTable: SubSamplesTable;

  @ViewChild('individualReleaseTable') individualReleaseTable: SubSamplesTable;

  constructor(
    route: ActivatedRoute,
    router: Router,
    alterCtrl: AlertController,
    translate: TranslateService,
    protected dateFormat: DateFormatPipe,
    protected operationService: OperationService,
    protected tripService: TripService
  ) {
    super(route, router, alterCtrl, translate);

    // FOR DEV ONLY ----
    this.debug = !environment.production;
  }

  ngOnInit() {
    // Register sub forms & table
    this.registerForms([this.opeForm, this.measurementsForm, this.catchForm])
      .registerTables([this.survivalTestsTable, this.individualMonitoringTable, this.individualReleaseTable]);

    // Disable, during load
    this.disable();

    // Read route
    this.route.params.first().subscribe(res => {
      const tripId = res && res["tripId"];
      const id = res && res["opeId"];
      setTimeout(() => {
        if (!id || id === "new") {
          this.load(undefined, { tripId: tripId });
        }
        else {
          this.load(parseInt(id), { tripId: tripId });
        }
      });
    });

    this.opeForm.form.controls['physicalGear'].valueChanges.subscribe((res) => {
      if (this.loading) return; // SKip during loading
      this.catchForm.gear = res && res.gear && res.gear.label || null;
    })

    // Update available parent on individual table, when survival tests changes
    this.survivalTestsTable.listChange.debounceTime(400).subscribe(samples => {
      const availableParents = (samples || [])
        .filter(s => !!s.measurementValues[PmfmIds.TAG_ID]);
      // Will refresh the tables (inside the setter):
      this.individualMonitoringTable.availableParents = availableParents;
      this.individualReleaseTable.availableParents = availableParents;
    });
  }

  async load(id?: number, options?: { tripId: number }) {
    this.error = null;

    // Existing operation
    if (id) {

      if (this.debug) console.debug("[page-operation] Loading operation...");

      const data = await this.operationService.load(id).first().toPromise();
      if (!data || !data.tripId) {
        console.error("Unable to load operation with id:" + id);
        this.error = "TRIP.OPERATION.ERROR.LOAD_OPERATION_ERROR";
        this.loading = false;
        return;
      }

      if (this.debug) console.debug("[page-operation] Operation loaded", data);

      const trip = await this.tripService.load(data.tripId).first().toPromise();
      this.updateView(data, trip);
      this.enable();
      this.loading = false;
    }

    // New operation
    else if (options && options.tripId) {
      if (this.debug) console.debug("[page-operation] Creating new operation...");
      const trip = await this.tripService.load(options.tripId).toPromise();

      const operation = new Operation();

      // Use the default gear, if only one
      if (trip.gears.length == 1) {
        operation.physicalGear = Object.assign({}, trip.gears[0]);
      }

      this.updateView(operation, trip);
      this.enable();
      this.loading = false;
    }
    else {
      throw new Error("Missing argument 'id' or 'options.tripId'!");
    }
  }

  updateView(data: Operation | null, trip?: Trip) {
    this.data = data;
    this.opeForm.value = data;
    if (trip) {
      this.trip = trip;
      this.opeForm.setTrip(trip);
    }

    const gearLabel = data && data.physicalGear && data.physicalGear.gear && data.physicalGear.gear.label;

    // Set measurements
    this.measurementsForm.gear = gearLabel;
    this.measurementsForm.value = data && data.measurements || [];
    this.measurementsForm.updateControls();

    // Set catch bacth
    this.catchForm.gear = gearLabel;
    this.catchForm.value = data && data.catchBatch || Batch.fromObject({ rankOrder: 1 });
    //this.catchForm.updateControls();

    // Get all samples (and children)
    const samples = (data && data.samples || []).reduce((res, sample) => !sample.children ? res.concat(sample) : res.concat(sample).concat(sample.children), [])

    // Set survival tests
    const survivalTestSamples = samples.filter(s => s.label.startsWith(this.survivalTestsTable.acquisitionLevel + "#"));
    this.survivalTestsTable.value = survivalTestSamples;

    // Set individual monitoring
    this.individualMonitoringTable.availableParents = survivalTestSamples.filter(s => s.measurementValues && isNotNil(s.measurementValues[PmfmIds.TAG_ID]));
    this.individualMonitoringTable.value = samples.filter(s => s.label.startsWith(AcquisitionLevelCodes.INDIVIDUAL_MONITORING + "#"));

    // Set individual release
    this.individualReleaseTable.availableParents = this.individualMonitoringTable.availableParents;
    this.individualReleaseTable.value = samples.filter(s => s.label.startsWith(AcquisitionLevelCodes.INDIVIDUAL_RELEASE + "#"));

    // Update title
    this.updateTitle();

    this.markAsPristine();
    this.markAsUntouched();
  }

  async save(event): Promise<any> {
    if (this.loading || this.saving) return;

    // Not valid
    if (!this.valid) {
      if (this.debug) console.warn("[page-operation] Validation errors !");

      if (this.opeForm.invalid) this.opeForm.markAsTouched();
      if (this.measurementsForm.invalid) this.measurementsForm.markAsTouched();
      if (this.catchForm.invalid) {
        this.catchForm.markAsTouched();
        AppFormUtils.logFormErrors(this.catchForm.form, "[catch-form]");
      }
      if (this.survivalTestsTable.invalid) {
        this.survivalTestsTable.markAsTouched();
        if (this.survivalTestsTable.selectedRow && this.survivalTestsTable.selectedRow.editing) {
          AppFormUtils.logFormErrors(this.survivalTestsTable.selectedRow.validator, "[survivaltests-table]")
        }
      }
      if (this.individualMonitoringTable.invalid) {
        this.individualMonitoringTable.markAsTouched();
        if (this.individualMonitoringTable.selectedRow && this.individualMonitoringTable.selectedRow.editing) {
          AppFormUtils.logFormErrors(this.individualMonitoringTable.selectedRow.validator, "[monitoring-table]")
        }
      }

      this.submitted = true;
      return;
    }

    if (this.loading || this.saving || !this.valid || !this.dirty) return;
    this.saving = true;
    this.error = undefined;

    if (this.debug) console.debug("[page-operation] Saving...");

    // Update entity from JSON
    let json = this.opeForm.value;
    json.measurements = this.measurementsForm.value;
    this.data.fromObject(json);
    this.data.tripId = this.trip.id;

    // get catch batch
    this.data.catchBatch = this.catchForm.value;

    // update tables 'value'
    await this.survivalTestsTable.save();
    await this.individualMonitoringTable.save();
    await this.individualReleaseTable.save();

    // get sub-samples, from tables
    const subSamples = (this.individualMonitoringTable.value || [])
      .concat(this.individualReleaseTable.value || []);
    this.data.samples = (this.survivalTestsTable.value || [])
      .map(sample => {
        // Add children
        sample.children = subSamples.filter(childSample => sample.equals(childSample.parent));
        return sample;
      });

    const isNew = this.isNewData();
    this.disable();

    try {

      // Save trip form (with sale) 
      const updatedData = await this.operationService.save(this.data);

      // Update the view (e.g metadata)
      this.updateView(updatedData);

      // Update route location
      if (isNew) {
        this.router.navigate(['../' + updatedData.id], {
          relativeTo: this.route,
          queryParams: this.route.snapshot.queryParams,
          replaceUrl: true // replace the current satte in history
        });

        // Subscription to changes
        //this.startListenChanges();
      }

      this.submitted = false;

      return updatedData;
    }
    catch (err) {
      console.error(err && err.message || err);
      this.error = err && err.message || err;
      this.submitted = true;
    }
    finally {
      this.enable();
      this.saving = false;
    }
  }

  // Override default function
  async doReload() {
    this.loading = true;
    await this.load(this.data && this.data.id,
      { tripId: this.trip && this.trip.id });
  }

  /**
   * Compute the title
   * @param data 
   */
  async updateTitle(data?: Operation) {
    data = data || this.data;

    // new ope
    let title;
    if (!data || isNil(data.id)) {
      title = await this.translate.get('TRIP.OPERATION.NEW.TITLE').toPromise();
    }
    // Existing ope
    else {
      title = await this.translate.get('TRIP.OPERATION.EDIT.TITLE', {
        startDateTime: data.startDateTime && this.dateFormat.transform(data.startDateTime, { time: true }) as string
      }).toPromise();
    }

    // Emit the title
    this.title.next(title);
  }

}
