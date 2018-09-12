import { Component, OnInit, ViewChild } from '@angular/core';
import { Router, ActivatedRoute } from "@angular/router";
import { OperationService } from '../services/operation.service';
import { OperationForm } from './operation.form';
import { Operation, Trip } from '../services/model';
import { TripService } from '../services/trip.service';
import { MeasurementsForm } from '../measurement/measurements.form';
import { AppTabPage } from '../../core/core.module';
import { CatchForm } from '../catch/catch.form';

@Component({
  selector: 'page-operation',
  templateUrl: './operation.page.html',
  styleUrls: ['./operation.page.scss']
})
export class OperationPage extends AppTabPage<Operation, { tripId: number }> implements OnInit {

  trip: Trip;
  saving: boolean = false;

  @ViewChild('opeForm') opeForm: OperationForm;

  @ViewChild('measurementsForm') measurementsForm: MeasurementsForm;

  @ViewChild('catchForm') catchForm: CatchForm;

  constructor(
    protected route: ActivatedRoute,
    protected router: Router,
    protected operationService: OperationService,
    protected tripService: TripService
  ) {
    super(route, router);
  }


  ngOnInit() {
    // Register forms
    this.registerForm(this.opeForm);
    this.registerForm(this.measurementsForm);
    this.registerForm(this.catchForm);

    // Disable, during load
    this.disable();

    // Read route
    this.route.params.subscribe(res => {
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
      const gearLabel = res && res.gear && res.gear.label;
      if (gearLabel) {
        this.catchForm.gear = gearLabel;
      }
    })

  }

  async load(id?: number, opts?: { tripId: number }) {
    this.error = null;

    // Existing operation
    if (id) {
      console.debug("[page-operation] Loading operation...");
      this.operationService.load(id)
        .subscribe(data => {
          if (!data || !data.tripId) {
            console.error("Unable to load operation with id:" + id);
            this.error = "TRIP.OPERATION.ERROR.LOAD_OPERATION_ERROR";
            this.loading = false;
            return;
          }

          this.tripService.load(data.tripId)
            .subscribe(trip => {
              this.updateView(data, trip);
              this.enable();
              this.loading = false;
            });
        });
    }

    // New operation
    else if (opts && opts.tripId) {
      console.debug("[page-operation] Creating new operation...");
      this.tripService.load(opts.tripId)
        .subscribe(trip => {
          this.updateView(new Operation(), trip);
          this.enable();
          this.loading = false;
        });
    }
  }

  updateView(data: Operation | null, trip?: Trip) {
    this.data = data;
    this.opeForm.value = data;
    if (trip) {
      this.trip = trip;
      this.opeForm.setTrip(trip);
    }

    this.measurementsForm.value = data.measurements || [];

    const gearLabel = data && data.physicalGear && data.physicalGear.gear && data.physicalGear.gear.label;
    this.catchForm.value = [];
    this.catchForm.gear = gearLabel;

    this.markAsPristine();
    this.markAsUntouched();
  }

  async save(event): Promise<any> {
    if (this.loading || this.saving) return;

    // Not valid
    if (!this.valid) {
      console.debug("[page-operation] Could not save (invalid)");

      if (this.opeForm.invalid) this.opeForm.markAsTouched();
      if (this.measurementsForm.invalid) this.measurementsForm.markAsTouched();

      this.submitted = true;
      return;
    }

    if (this.loading || this.saving || !this.valid || !this.dirty) return;
    this.saving = true;

    console.debug("[page-operation] Saving...");

    // Update entity from JSON
    let json = this.opeForm.value;
    this.data.fromObject(json);
    this.data.tripId = this.trip.id;
    this.data.measurements = this.measurementsForm.value;

    // Catch: TODO
    //this.data.catch = this.catchForm.value;
    console.log("TODO: get catch", this.catchForm.value);

    this.disable();

    try {
      // Save trip form (with sale) 
      const updatedData = await this.operationService.save(this.data);

      // Update the view (e.g metadata)
      this.updateView(updatedData);
      return updatedData;
    }
    catch (err) {
      console.error(err);
      this.submitted = true;
      this.error = err && err.message || err;
    }
    finally {
      this.enable();
      this.submitted = false;
      this.saving = false;
    }
  }

  async cancel() {
    // reload
    this.loading = true;
    await this.load(this.data && this.data.id,
      { tripId: this.trip && this.trip.id });
  }

}
