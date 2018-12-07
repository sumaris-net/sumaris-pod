import { Component, OnInit, ViewChild } from '@angular/core';
import {Router, ActivatedRoute, Params} from "@angular/router";
import { OperationService } from '../services/operation.service';
import { OperationForm } from './operation.form';
import { Operation, Trip, Batch } from '../services/trip.model';
import { TripService } from '../services/trip.service';
import { MeasurementsForm } from '../measurement/measurements.form.component';
import { AppTabPage, AppFormUtils } from '../../core/core.module';
import { CatchForm } from '../catch/catch.form';
import { SamplesTable } from '../sample/samples.table';
import { SubSamplesTable } from '../sample/sub-samples.table';
import { AlertController } from "@ionic/angular";
import { TranslateService } from '@ngx-translate/core';
import { AcquisitionLevelCodes, isNotNil, isNil } from '../../core/services/model';
import { PmfmIds } from '../../referential/services/model';
import { Subject } from 'rxjs';
import { DateFormatPipe } from 'src/app/shared/pipes/date-format.pipe';
import { BatchesTable } from '../batch/batches.table';
import {BatchGroupsTable} from "../batch/batch-groups.table";
import {SubBatchesTable} from "../batch/sub-batches.table";
import {MatTabChangeEvent} from "@angular/material";


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
  selectedBatchTabIndex: number = 0;

  defaultBackHref: string;

  @ViewChild('opeForm') opeForm: OperationForm;

  @ViewChild('measurementsForm') measurementsForm: MeasurementsForm;

  @ViewChild('catchForm') catchForm: CatchForm;

  @ViewChild('survivalTestsTable') survivalTestsTable: SamplesTable;

  @ViewChild('individualMonitoringTable') individualMonitoringTable: SubSamplesTable;

  @ViewChild('individualReleaseTable') individualReleaseTable: SubSamplesTable;

  @ViewChild('batchGroupsTable') batchGroupsTable: BatchGroupsTable;

  @ViewChild('subBatchesTable') subBatchesTable: SubBatchesTable;


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

    // Listen route parameters
    this.route.queryParams.subscribe(res => {
      const subTabIndex = res["sub-tab"];
      if (subTabIndex !== undefined) {
        this.selectedBatchTabIndex = parseInt(subTabIndex) || 0;
      }
    });

    // FOR DEV ONLY ----
    //this.debug = !environment.production;
  }

  ngOnInit() {
    super.ngOnInit();

    // Register sub forms & table
    this.registerForms([this.opeForm, this.measurementsForm, this.catchForm])
      .registerTables([
        this.survivalTestsTable,
        this.individualMonitoringTable,
        this.individualReleaseTable,
        this.batchGroupsTable,
        this.subBatchesTable
      ]);

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
        // Compute the default back
        this.defaultBackHref = "/trips/" + tripId + "?tab=2";
      });
    });

    this.opeForm.form.controls['physicalGear'].valueChanges.subscribe((res) => {
      if (this.loading) return; // SKip during loading
      this.catchForm.gear = res && res.gear && res.gear.label || null;
    });

    // Update available parent on sub-sample table, when samples changes
    this.survivalTestsTable.listChange.debounceTime(400).subscribe(samples => {
      const availableParents = (samples || [])
        .filter(s => !!s.measurementValues[PmfmIds.TAG_ID]);
      // Will refresh the tables (inside the setter):
      this.individualMonitoringTable.availableParents = availableParents;
      this.individualReleaseTable.availableParents = availableParents;
    });

    // Update available parent on individual batch table, when batch group changes
    this.batchGroupsTable.listChange.debounceTime(400).subscribe(batchGroups => {
      // Will refresh the tables (inside the setter):
      this.subBatchesTable.availableParents = (batchGroups || []);
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
      if (trip.program && trip.program.label) {
        this.updateViewByProgram(trip.program.label);
      }
      this.enable();
      this.loading = false;
    }

    // New operation
    else if (options && options.tripId) {
      if (this.debug) console.debug("[page-operation] Creating new operation...");
      const trip = await this.tripService.load(options.tripId).first().toPromise();

      const operation = new Operation();

      // Use the default gear, if only one
      if (trip.gears.length == 1) {
        operation.physicalGear = Object.assign({}, trip.gears[0]);
      }

      this.updateView(operation, trip);
      if (trip.program && trip.program.label) {
        this.updateViewByProgram(trip.program.label);
      }
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
    this.catchForm.value = data && data.catchBatch || Batch.fromObject({ rankOrder: 1, label: AcquisitionLevelCodes.CATCH_BATCH });
    //this.catchForm.updateControls();

    // Get all samples (and children)
    const samples = (data && data.samples || []).reduce((res, sample) => !sample.children ? res.concat(sample) : res.concat(sample).concat(sample.children), [])

    // Set survival tests
    const survivalTestSamples = samples.filter(s => s.label && s.label.startsWith(this.survivalTestsTable.acquisitionLevel + "#"));
    this.survivalTestsTable.value = survivalTestSamples;

    // Set individual monitoring
    this.individualMonitoringTable.availableParents = survivalTestSamples.filter(s => s.measurementValues && isNotNil(s.measurementValues[PmfmIds.TAG_ID]));
    this.individualMonitoringTable.value = samples.filter(s => s.label && s.label.startsWith(this.individualMonitoringTable.acquisitionLevel + "#"));

    // Set individual release
    this.individualReleaseTable.availableParents = this.individualMonitoringTable.availableParents;
    this.individualReleaseTable.value = samples.filter(s => s.label && s.label.startsWith(this.individualReleaseTable.acquisitionLevel + "#"));

    // Get all batches (and children)
    const batches = (data && data.catchBatch && data.catchBatch.children) || [];

    // Set batch tables (if exists)
    if (isNotNil(this.batchGroupsTable) && isNotNil(this.subBatchesTable)) {
      const batchGroups = batches.filter(s => s.label && s.label.startsWith(this.batchGroupsTable.acquisitionLevel + "#"));

      this.batchGroupsTable.pmfms
        .filter(pmfms => (pmfms && pmfms.length > 0))
        .first()
        .subscribe(() => {
          const qvPmfm = this.batchGroupsTable.qvPmfm;
          this.subBatchesTable.availableParents = batchGroups;
          this.subBatchesTable.value = batchGroups.reduce((res, group) => {
            if (qvPmfm) {
              return res.concat(group.children.reduce((res, qvBatch) => {
                const children = this.getBatchChildrenByLevel(qvBatch, this.subBatchesTable.acquisitionLevel);
                return res.concat(children
                  .map(child => {
                    // Copy QV value from the group
                    child.measurementValues = child.measurementValues || {};
                    child.measurementValues[qvPmfm.pmfmId] = qvBatch.measurementValues[qvPmfm.pmfmId];
                    // Replace parent by the group (instead of the sampling batch)
                    child.parentId = group.id;
                    return child;
                  }));
                }, []));
            } else {
              return res.concat(this.getBatchChildrenByLevel(group, this.subBatchesTable.acquisitionLevel)
                .map(child => {
                  // Replace parent by the group (instead of the sampling batch)
                  child.parentId = group.id;
                  return child;
                }));
            }
          }, []);
        });

      this.batchGroupsTable.value = batchGroups;

    }


    this.updateViewExtension(data, trip);

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
      if (this.batchGroupsTable.invalid) {
        this.batchGroupsTable.markAsTouched();
        if (this.batchGroupsTable.selectedRow && this.batchGroupsTable.selectedRow.editing) {
          AppFormUtils.logFormErrors(this.batchGroupsTable.selectedRow.validator, "[batch-group-table]")
        }
      }
      if (this.subBatchesTable.invalid) {
        this.subBatchesTable.markAsTouched();
        if (this.subBatchesTable.selectedRow && this.subBatchesTable.selectedRow.editing) {
          AppFormUtils.logFormErrors(this.subBatchesTable.selectedRow.validator, "[batch-table]")
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
    const saved = await this.survivalTestsTable.save();
    await this.individualMonitoringTable.save();
    await this.individualReleaseTable.save();
    await this.batchGroupsTable.save();
    await this.subBatchesTable.save();

    // get sub-samples, from tables
    const subSamples = (this.individualMonitoringTable.value || [])
      .concat(this.individualReleaseTable.value || []);
    this.data.samples = (this.survivalTestsTable.value || [])
      .map(sample => {
        // Add children
        sample.children = subSamples.filter(childSample => childSample.parent && sample.equals(childSample.parent));
        return sample;
      });

    // get batches
    const batchGroups = (this.batchGroupsTable.value || []);

    const subBatches  = (this.subBatchesTable.value || []);
    const qvPmfm = this.batchGroupsTable.qvPmfm;
    batchGroups.forEach(batchGroup => {
        // Add children
        (batchGroup.children || []).forEach(b => {
          const children = subBatches.filter(childBatch => childBatch.parent && batchGroup.equals(childBatch.parent) &&
            (!qvPmfm || (childBatch.measurementValues[qvPmfm.pmfmId] == b.measurementValues[qvPmfm.pmfmId]))
          );
          // If has sampling batch, use it a parent
          if (b.children && b.children.length == 1) b.children[0].children = children;
          else b.children = children;
        });
    });
    this.data.catchBatch.children = batchGroups;

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


  protected getBatchChildrenByLevel(batch: Batch, acquisitionLevel: string): Batch[]{
    return (batch.children || []).reduce((res, child) => {
      if (child.label && child.label.startsWith(acquisitionLevel + "#")) return res.concat(child);
      return res.concat(this.getBatchChildrenByLevel(child, acquisitionLevel)); // recursive call
    }, []);
  }

  public onBatchTabChange(event: MatTabChangeEvent) {
    const queryParams: Params = Object.assign({}, this.route.snapshot.queryParams);
    queryParams['sub-tab'] = event.index;
    this.router.navigate(['.'], {
      relativeTo: this.route,
      queryParams: queryParams
    });

    // Confirm editing row
    this.batchGroupsTable.confirmEditCreateSelectedRow();
    this.subBatchesTable.confirmEditCreateSelectedRow();
  }

  /**
   * Configure specific behavior program
   * @param programLabel
   */
  protected updateViewExtension(data: Operation | null, trip?: Trip) {
    if (trip.program && trip.program.label == "SUMARiS") {
      //this.measurementsForm.form.controls[]
    }
  }
}
