import {ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit, ViewChild} from '@angular/core';
import {ActivatedRoute, Params, Router} from "@angular/router";
import {OperationService} from '../services/operation.service';
import {OperationForm} from './operation.form';
import {Batch, EntityUtils, Operation, Trip} from '../services/trip.model';
import {TripService} from '../services/trip.service';
import {MeasurementsForm} from '../measurement/measurements.form.component';
import {AppFormUtils, AppTabPage, environment, LocalSettingsService} from '../../core/core.module';
import {CatchBatchForm} from '../catch/catch.form';
import {AlertController} from "@ionic/angular";
import {TranslateService} from '@ngx-translate/core';
import {AcquisitionLevelCodes, UsageMode} from '../../core/services/model';
import {isNil, isNotNil} from '../../shared/shared.module';
import {PmfmIds, ProgramService, QualitativeLabels} from '../../referential/referential.module';
import {Subject} from 'rxjs';
import {DateFormatPipe} from 'src/app/shared/pipes/date-format.pipe';
import {MatTabChangeEvent, MatTabGroup} from "@angular/material";
import {debounceTime, distinctUntilChanged, filter, first, map, startWith, switchMap} from "rxjs/operators";
import {Validators} from "@angular/forms";
import * as moment from "moment";
import {Moment} from "moment";
import {IndividualMonitoringSubSamplesTable} from "../sample/individualmonitoring/individual-monitoring-samples.table";
import {ProgramProperties} from "../../referential/services/model";
import {SubBatchesTable} from "../batch/sub-batches.table";
import {SubSamplesTable} from "../sample/sub-samples.table";
import {SamplesTable} from "../sample/samples.table";
import {BatchesTable} from "../batch/batches.table";
import {BatchesContext} from "../batch/batches-context.class";
import {BatchGroupsTable} from "../batch/batch-groups.table";
import {BatchUtils} from "../services/model/batch.model";

@Component({
  selector: 'page-operation',
  templateUrl: './operation.page.html',
  styleUrls: ['./operation.page.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class OperationPage extends AppTabPage<Operation, { tripId: number }> implements OnInit {

  protected _enableListenChanges = (environment.listenRemoteChanges === true);

  title = new Subject<string>();
  trip: Trip;
  programSubject = new Subject<string>();
  saving = false;
  rankOrder: number;
  selectedBatchTabIndex = 0;
  selectedSampleTabIndex = 0;

  defaultBackHref: string;

  showSampleTables = false;
  showBatchTables = false;
  enableSubBatchesTable = false;
  batchesContext = new BatchesContext();

  usageMode: UsageMode;
  mobile: boolean;

  @ViewChild('matTabGroup') matTabGroup: MatTabGroup;
  @ViewChild('batchTabGroup') batchTabGroup: MatTabGroup;
  @ViewChild('sampleTabGroup') sampleTabGroup: MatTabGroup;
  @ViewChild('opeForm') opeForm: OperationForm;
  @ViewChild('measurementsForm') measurementsForm: MeasurementsForm;
  @ViewChild('catchBatchForm') catchBatchForm: CatchBatchForm;

  // Sample tables
  @ViewChild('samplesTable') samplesTable: SamplesTable;
  @ViewChild('individualMonitoringTable') individualMonitoringTable: IndividualMonitoringSubSamplesTable;
  @ViewChild('individualReleaseTable') individualReleaseTable: SubSamplesTable;

  // Batch tables (= simpleBatchesTable or batchGroupsTable)
  batchesTable: BatchesTable;
  @ViewChild('simpleBatchesTable') simpleBatchesTable: BatchesTable;
  @ViewChild('batchGroupsTable') batchGroupsTable: BatchGroupsTable;

  @ViewChild('subBatchesTable') subBatchesTable: SubBatchesTable;

  constructor(
    route: ActivatedRoute,
    router: Router,
    alterCtrl: AlertController,
    translate: TranslateService,
    protected dateFormat: DateFormatPipe,
    protected settings: LocalSettingsService,
    protected operationService: OperationService,
    protected tripService: TripService,
    protected programService: ProgramService,
    protected cd: ChangeDetectorRef
  ) {
    super(route, router, alterCtrl, translate);

    this.mobile = settings.mobile;

    // Listen route parameters
    this.route.queryParams.pipe(first())
      .subscribe(queryParams => {
        const subTabIndex = queryParams["subtab"] && parseInt(queryParams["subtab"]);
        if (isNotNil(subTabIndex)) {
          this.selectedBatchTabIndex = subTabIndex > 1 ? 1 : subTabIndex;
          this.selectedSampleTabIndex = subTabIndex;
        } else {
          this.selectedBatchTabIndex = 0;
          this.selectedSampleTabIndex = 0;
        }
        if (this.batchTabGroup) this.batchTabGroup.realignInkBar();
        if (this.sampleTabGroup) this.sampleTabGroup.realignInkBar();
        this.markForCheck();
      });

    // FOR DEV ONLY ----
    this.debug = !environment.production;
  }

  ngOnInit() {
    super.ngOnInit();
    this.batchesTable = this.mobile ? this.simpleBatchesTable : this.batchGroupsTable;

    // Register sub forms & table
    this.registerForms([this.opeForm, this.measurementsForm, this.catchBatchForm])
      .registerTables([
        this.samplesTable,
        this.individualMonitoringTable,
        this.individualReleaseTable,
        this.batchesTable,
        this.subBatchesTable
      ]);

    // Disable, during load
    this.disable();

    // Read route
    this.route.params.pipe(first())
      .subscribe(async ({tripId, id}) => {
        if (isNil(tripId)) return; // skip
        if (isNil(id) || id === "new") {
          await this.load(undefined, {tripId: tripId});
        } else {
          await this.load(+id, {tripId: tripId});
        }
      });

    this.registerSubscription(
      this.opeForm.form.controls['physicalGear'].valueChanges.subscribe((res) => {
        if (this.loading) return; // SKip during loading
        this.catchBatchForm.gear = res && res.gear && res.gear.label || null;
      })
    );

    // Update available parent on sub-sample table, when samples changes
    this.registerSubscription(
      this.samplesTable.listChange
        .pipe(debounceTime(400))
        .subscribe(samples => {
          if (this.loading) return; // skip during loading
          const availableParents = (samples || [])
            .filter(s => !!s.measurementValues[PmfmIds.TAG_ID]);
          // Will refresh the tables (inside the setter):
          this.individualMonitoringTable.availableParents = availableParents;
          this.individualReleaseTable.availableParents = availableParents;
        }));

    // Update available parent on individual batch table, when batch group changes
    this.registerSubscription(
      this.batchesTable.listChange
        .pipe(debounceTime(400))
        .subscribe(rootBatches => {
          if (this.loading || !this.enableSubBatchesTable) return; // skip during loading
          // Will refresh the tables (inside the setter):
          this.subBatchesTable.availableParents = (rootBatches || []);
        })
    );

    // Enable sub batches when table pmfms ready
    this.subBatchesTable.pmfms
      .pipe(filter(isNotNil), first())
      .subscribe(pmfms => {
        if (!this.enableSubBatchesTable && pmfms.length > 0) {
          this.enableSubBatchesTable = true;
          this.markForCheck();
        }
      });

    this.ngInitExtension();
  }


  async load(id?: number, options?: { tripId: number }) {
    this.error = null;

    // New operation
    if (isNil(id)) {
      if (this.debug) console.debug("[page-operation] Creating new operation...");

      if (!options || isNil(options.tripId)) throw new Error("Missing argument 'options.tripId'!");

      const trip = await this.tripService.load(options.tripId);

      this.usageMode = this.computeUsageMode(trip);

      const data = new Operation();

      // If is on field mode, fill default values
      if (this.usageMode === 'FIELD') {
        data.startDateTime = moment();
      }

      // Use the default gear, if only one
      if (trip.gears.length == 1) {
        data.physicalGear = Object.assign({}, trip.gears[0]);
      }

      await this.updateView(data, trip);
      this.loading = false;
    }

    // Load existing operation
    else {
      if (this.debug) console.debug(`[page-operation] Loading operation with id {${id}}...`);

      const data = await this.operationService.load(id);
      if (!data || !data.tripId) {
        console.error(`[page-operation] Unable to load operation with id {${id}}`);
        this.error = "TRIP.OPERATION.ERROR.LOAD_OPERATION_ERROR";
        this.loading = false;
        return;
      }

      if (this.debug) console.debug("[page-operation] Operation loaded", data);

      const trip = await this.tripService.load(data.tripId);
      this.usageMode = this.computeUsageMode(trip);

      await this.updateView(data, trip);
      this.loading = false;
      this.startListenChanges();
    }
  }

  startListenChanges() {
    if (isNotNil(this.data.id) && this._enableListenChanges) {

      const subscription = this.operationService.listenChanges(this.data.id)
        .subscribe((data: Operation | undefined) => {
          const newUpdateDate = data && (data.updateDate as Moment) || undefined;
          if (isNotNil(newUpdateDate) && newUpdateDate.isAfter(this.data.updateDate)) {
            if (this.debug) console.debug("[operation] Detected update on server", newUpdateDate);
            if (!this.dirty) {
              this.updateView(data, this.trip);
            }
          }
        });

      // Add log when closing
      if (this.debug) subscription.add(() => console.debug('[operation] [WS] Stop to listen changes'));

      this.registerSubscription(subscription);
    }
  }

  async updateView(data: Operation | null, trip?: Trip) {
    this.data = data;
    this.opeForm.value = data;

    if (trip) {
      this.trip = trip;
      this.opeForm.trip = trip;

      // Set program
      const program = trip && trip.program && trip.program.label;
      this.programSubject.next(program);

      this.batchesTable.program = program;
      if (this.subBatchesTable) this.subBatchesTable.program = program;
    }

    const gearLabel = data && data.physicalGear && data.physicalGear.gear && data.physicalGear.gear.label;

    // Get all batches (and children), and samples
    const batches = (data && data.catchBatch && data.catchBatch.children) || [];
    const samples = (data && data.samples || []).reduce((res, sample) => !sample.children ? res.concat(sample) : res.concat(sample).concat(sample.children), []);

    // Set measurements
    this.measurementsForm.gear = gearLabel;
    this.measurementsForm.value = data && data.measurements || [];
    this.measurementsForm.updateControls();

    // Set catch batch
    this.catchBatchForm.gear = gearLabel;
    this.catchBatchForm.value = data && data.catchBatch || Batch.fromObject({
      rankOrder: 1,
      label: AcquisitionLevelCodes.CATCH_BATCH
    });

    // Set samples
    this.samplesTable.value = samples.filter(s => s.label && s.label.startsWith(this.samplesTable.acquisitionLevel + "#"));

    // Set individual monitoring
    this.individualMonitoringTable.availableParents = this.samplesTable.value.filter(s => s.measurementValues && isNotNil(s.measurementValues[PmfmIds.TAG_ID]));
    this.individualMonitoringTable.value = samples.filter(s => s.label && s.label.startsWith(this.individualMonitoringTable.acquisitionLevel + "#"));

    // Set individual release tables
    this.individualReleaseTable.availableParents = this.individualMonitoringTable.availableParents;
    this.individualReleaseTable.value = samples.filter(s => s.label && s.label.startsWith(this.individualReleaseTable.acquisitionLevel + "#"));

    // Set batches table (with root batches)
    this.batchesTable.value = batches.filter(s => s.label && s.label.startsWith(this.batchesTable.acquisitionLevel + "#"));

    // make sure PMFMs are loaded (need the QV pmfm)
    this.batchesTable.pmfms.pipe(filter(isNotNil), first())
      .subscribe((_) => {
        this.subBatchesTable.setValueFromParent(this.batchesTable.value, this.batchesTable.qvPmfm);
      });

    // Update title
    await this.updateTitle();

    // Compute the default back href
    this.defaultBackHref = `/trips/${data.tripId}?tab=2`;

    this.markAsPristine();
    this.markAsUntouched();

    if (!this.trip || isNotNil(this.trip.validationDate)) {
      this.disable();
    } else {
      this.enable();
    }
  }

  /**
   * Configure specific behavior
   */
  protected ngInitExtension() {
    if (this.measurementsForm) {
      this.measurementsForm.pmfms
        .pipe(
          filter(isNotNil),
          debounceTime(400),
          first(),
          map(() => this.measurementsForm.form)
        )
        .subscribe((formGroup) => {

          // If PMFM "Sampling type" exists (e.g. SUMARiS), then use to enable/disable some tables
          const samplingTypeControl = formGroup && formGroup.controls[PmfmIds.SURVIVAL_SAMPLING_TYPE];
          if (isNotNil(samplingTypeControl)) {
            this.registerSubscription(
              samplingTypeControl.valueChanges
                .pipe(
                  debounceTime(400),
                  startWith(() => samplingTypeControl.value),
                  filter(EntityUtils.isNotEmpty),
                  map(value => value.label),
                  distinctUntilChanged()
                )
                .subscribe(qvLabel => {
                  // Force first tab index
                  this.selectedBatchTabIndex = 0;
                  this.selectedSampleTabIndex = 0;

                  switch (qvLabel as string) {
                    case QualitativeLabels.SURVIVAL_SAMPLING_TYPE.SURVIVAL:
                      if (this.debug) console.debug("[operation] Enable survival test tables");
                      this.showSampleTables = true;
                      this.showBatchTables = false;
                      break;
                    case QualitativeLabels.SURVIVAL_SAMPLING_TYPE.CATCH_HAUL:
                      if (this.debug) console.debug("[operation] Enable batch sampling tables");
                      this.showSampleTables = false;
                      this.showBatchTables = true;
                      break;
                    case QualitativeLabels.SURVIVAL_SAMPLING_TYPE.UNSAMPLED:
                      if (this.debug) console.debug("[operation] Disable survival test and batch sampling tables");
                      this.showSampleTables = false;
                      this.showBatchTables = false;
                  }
                  this.markForCheck();
                })
            );
          }

          // If PMFM "Is Sampling ?" exists, then use to enable/disable some tables
          const isSamplingControl = formGroup && formGroup.controls[PmfmIds.IS_SAMPLING];
          if (isNotNil(isSamplingControl)) {
            this.registerSubscription(
              isSamplingControl.valueChanges
                .pipe(
                  debounceTime(400),
                  startWith(() => isSamplingControl.value),
                  filter(isNotNil),
                  distinctUntilChanged()
                )
                .subscribe(isSampling => {
                  // Force first tab index
                  this.selectedBatchTabIndex = 0;
                  this.selectedSampleTabIndex = 0;
                  if (this.debug) console.debug("[operation] Detected PMFM changes value for IS_SAMPLING: ", isSampling);

                  if (isSampling) {
                    if (this.debug) console.debug("[operation] Enable batch sampling tables");
                    this.showSampleTables = false;
                    this.showBatchTables = true;
                  } else {
                    if (this.debug) console.debug("[operation] Disable batch sampling tables");
                    this.showSampleTables = false;
                    this.showBatchTables = false;
                  }
                  this.markForCheck();
                })
            );
          }
          console.log("[operation] Enable default tables");

          // Default
          if (isNil(samplingTypeControl) && isNil(isSamplingControl)) {
            if (this.debug) console.debug("[operation] Enable default tables (Nor SUMARiS nor ADAP pmfms were found)");
            this.showSampleTables = false;
            this.showBatchTables = true;
          }

          // Abnormal trip => Set comment as required
          const tripProgressControl = formGroup && formGroup.controls[PmfmIds.TRIP_PROGRESS];
          if (isNotNil(samplingTypeControl)) {
            this.registerSubscription(
              tripProgressControl.valueChanges
                .debounceTime(400)
                .pipe(
                  startWith(() => tripProgressControl.value),
                  filter(isNotNil),
                  distinctUntilChanged()
                )
                .subscribe(value => {
                  const commentControl = this.opeForm.form.get('comments');
                  if (!value) {
                    commentControl.setValidators(Validators.required);
                    commentControl.markAsTouched({onlySelf: true});
                  } else {
                    commentControl.setValidators([]);
                  }
                  commentControl.updateValueAndValidity({emitEvent: false});
                })
            );
          }

        });
    }

    // Watch program, to configure tables from program properties
    this.registerSubscription(
      this.programSubject.asObservable()
        .pipe(switchMap(label => this.programService.watchByLabel(label)))
        .subscribe(program => {
          if (this.debug) console.debug(`[operation] Program ${program.label} loaded, with properties: `, program.properties);
          if (this.batchesTable) {
            this.batchesTable.showTaxonGroupColumn = program.getPropertyAsBoolean(ProgramProperties.BATCH_TAXON_GROUP_ENABLE, true);
            this.batchesTable.showTaxonNameColumn = program.getPropertyAsBoolean(ProgramProperties.BATCH_TAXON_NAME_ENABLE, true);
            // Force taxon name in sub batches, if not filled in root batch
            if (this.subBatchesTable) {
              this.subBatchesTable.showTaxonNameColumn = !this.batchesTable.showTaxonNameColumn;
            }
          }
        })
    );

  }

  async save(event): Promise<any> {
    if (this.loading || this.saving) return;

    // Not valid
    if (!this.valid) {
      this.markAsTouched();

      this.logFormErrors();

      this.openFirstInvalidTab();

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
    this.data.catchBatch = this.catchBatchForm.value;

    // save survival tables
    if (this.showSampleTables) {
      await this.samplesTable.save();
      await this.individualMonitoringTable.save();
      await this.individualReleaseTable.save();
      // get sub-samples, from tables
      const subSamples = (this.individualMonitoringTable.value || [])
        .concat(this.individualReleaseTable.value || []);
      this.data.samples = (this.samplesTable.value || [])
        .map(sample => {
          // Add children
          sample.children = subSamples.filter(childSample => childSample.parent && sample.equals(childSample.parent));
          return sample;
        });
    } else {
      this.data.samples = undefined;
    }

    // Save batch sampling tables
    if (this.showBatchTables) {
      await this.batchesTable.save();
      await this.subBatchesTable.save();

      // get batches
      const batches = BatchUtils.prepareRootBatchesForSaving(this.batchesTable.value, this.subBatchesTable.value, this.batchesTable.qvPmfm);
      this.data.catchBatch.children = batches;
      console.log("TODO: check batches to save: ", batches);
    } else {
      this.data.catchBatch.children = undefined;
    }

    const isNew = this.isNewData;
    this.disable();

    try {

      // Save trip form (with sale)
      const updatedData = await this.operationService.save(this.data);

      // Update the view (e.g metadata)
      await this.updateView(updatedData);

      // Update route location
      if (isNew) {
        // Update route location (add 'id' on query params, to force page resuse, in router strategy)
        await this.router.navigate(['.'], {
          relativeTo: this.route,
          queryParams: Object.assign(this.queryParams, {id: updatedData.id})
        });

        setTimeout(async () => {
          // Change the location (replace /new by /:id)
          await this.router.navigateByUrl(`/trips/${this.data.tripId}/operations/${updatedData.id}`, {
            replaceUrl: true,
            queryParams: this.queryParams,
          });
        }, 100);

        // Subscription to changes
        this.startListenChanges();
      }

      this.submitted = false;

      return updatedData;
    } catch (err) {
      console.error(err && err.message || err);
      this.error = err && err.message || err;
      this.submitted = true;
      this.enable();
    } finally {
      this.saving = false;
    }
  }

  // Override default function
  async doReload() {
    this.loading = true;
    await this.load(this.data && this.data.id,
      {tripId: this.trip && this.trip.id});
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
        vessel: this.trip && this.trip.vesselFeatures && (this.trip.vesselFeatures.exteriorMarking || this.trip.vesselFeatures.name) || '',
        departureDateTime: this.trip && this.trip.departureDateTime && this.dateFormat.transform(this.trip.departureDateTime) as string || '',
        startDateTime: data.startDateTime && this.dateFormat.transform(data.startDateTime, {time: true}) as string
      }).toPromise();
    }

    // Emit the title
    this.title.next(title);

  }


  protected getBatchChildrenByLevel(batch: Batch, acquisitionLevel: string): Batch[] {
    return (batch.children || []).reduce((res, child) => {
      if (child.label && child.label.startsWith(acquisitionLevel + "#")) return res.concat(child);
      return res.concat(this.getBatchChildrenByLevel(child, acquisitionLevel)); // recursive call
    }, []);
  }

  public onBatchTabChange(event: MatTabChangeEvent) {
    super.onSubTabChange(event);
    if (!this.loading) {
      // On each tables, confirm editing row
      this.batchesTable.confirmEditCreate();
      this.subBatchesTable.confirmEditCreate();
    }
  }

  public onSampleTabChange(event: MatTabChangeEvent) {
    super.onSubTabChange(event);
    if (!this.loading) {
      // On each tables, confirm editing row
      this.samplesTable.confirmEditCreate();
      this.individualMonitoringTable.confirmEditCreate();
      this.individualReleaseTable.confirmEditCreate();
    }
  }

  enable() {
    if (!this.data || !this.trip || isNotNil(this.trip.validationDate)) return false;
    // If not a new trip, check user can write
    if ((this.data.id || this.data.id === 0) && !this.tripService.canUserWrite(this.trip)) {
      if (this.debug) console.warn("[operation] Leave form disable (User has NO write access)");
      return;
    }
    if (this.debug) console.debug("[operation] Enabling form (User has write access)");
    super.enable();
  }

  protected logFormErrors() {
    if (this.debug) console.warn("[page-operation] Validation errors !");
    if (this.opeForm.invalid) {
      AppFormUtils.logFormErrors(this.opeForm.form, "[operation-form]");
    }
    if (this.measurementsForm.invalid) {
      AppFormUtils.logFormErrors(this.measurementsForm.form, "[operation-meas-form]");
    }
    if (this.catchBatchForm.invalid) {
      AppFormUtils.logFormErrors(this.catchBatchForm.form, "[catch-form]");
    }
    if (this.samplesTable.invalid) {
      if (this.samplesTable.editedRow && this.samplesTable.editedRow.editing) {
        AppFormUtils.logFormErrors(this.samplesTable.editedRow.validator, "[samples-table]");
      }
    }
    if (this.individualMonitoringTable.invalid) {
      if (this.individualMonitoringTable.editedRow && this.individualMonitoringTable.editedRow.editing) {
        AppFormUtils.logFormErrors(this.individualMonitoringTable.editedRow.validator, "[monitoring-table]");
      }
    }
    if (this.batchesTable.invalid) {
      if (this.batchesTable.editedRow && this.batchesTable.editedRow.editing) {
        AppFormUtils.logFormErrors(this.batchesTable.editedRow.validator, "[batches-table]");
      }
    }
    if (this.subBatchesTable.invalid) {
      if (this.subBatchesTable.editedRow && this.subBatchesTable.editedRow.editing) {
        AppFormUtils.logFormErrors(this.subBatchesTable.editedRow.validator, "[sub-batches-table]");
      }
    }
    if (this.individualReleaseTable.invalid) {
      if (this.individualReleaseTable.editedRow && this.individualReleaseTable.editedRow.editing) {
        AppFormUtils.logFormErrors(this.individualReleaseTable.editedRow.validator, "[release-table]");
      }
    }
  }

  /**
   * Open the first tab that is invalid
   */
  protected openFirstInvalidTab() {
    // tab 0
    const tab0Invalid = this.opeForm.invalid || this.measurementsForm.invalid;
    // tab 1
    const subTab0Invalid = (this.showBatchTables && this.batchesTable.invalid) || (this.showSampleTables && this.samplesTable.invalid);
    const subTab1Invalid = (this.showBatchTables && this.subBatchesTable.invalid) || (this.showSampleTables && this.individualMonitoringTable.invalid);
    const subTab2Invalid = this.showBatchTables && this.individualReleaseTable.invalid || false;
    const tab1Invalid = this.catchBatchForm.invalid || subTab0Invalid || subTab1Invalid || subTab2Invalid;

    // Open the first invalid tab
    let invalidTabIndex = tab0Invalid ? 0 : (tab1Invalid ? 1 : this.selectedTabIndex);
    if (this.selectedTabIndex === 0 && !tab0Invalid) {
      this.selectedTabIndex = invalidTabIndex;
    } else if (this.selectedTabIndex === 1 && !tab1Invalid) {
      this.selectedTabIndex = invalidTabIndex;
    }

    // If tab 1, open the invalid sub tab
    if (invalidTabIndex == 1) {
      if (this.showBatchTables) {
        const invalidSubTabIndex = subTab0Invalid ? 0 : (subTab1Invalid ? 1 : (subTab2Invalid ? 2 : this.selectedBatchTabIndex));
        if (this.selectedBatchTabIndex === 0 && !subTab0Invalid) {
          this.selectedBatchTabIndex = invalidSubTabIndex;
        } else if (this.selectedBatchTabIndex === 1 && !subTab1Invalid) {
          this.selectedBatchTabIndex = invalidSubTabIndex;
        } else if (this.selectedBatchTabIndex === 2 && !subTab2Invalid) {
          this.selectedBatchTabIndex = invalidSubTabIndex;
        }
      } else if (this.showSampleTables) {
        const invalidSubTabIndex = subTab0Invalid ? 0 : (subTab1Invalid ? 1 : (subTab2Invalid ? 2 : this.selectedSampleTabIndex));
        if (this.selectedSampleTabIndex === 0 && !subTab0Invalid) {
          this.selectedSampleTabIndex = invalidSubTabIndex;
        } else if (this.selectedSampleTabIndex === 1 && !subTab1Invalid) {
          this.selectedSampleTabIndex = invalidSubTabIndex;
        } else if (this.selectedSampleTabIndex === 2 && !subTab2Invalid) {
          this.selectedSampleTabIndex = invalidSubTabIndex;
        }
      }
    }
  }

  protected computeUsageMode(trip: Trip): UsageMode {
    return this.settings.isUsageMode('FIELD')
    && isNotNil(trip && trip.departureDateTime)
    && trip.departureDateTime.diff(moment(), "day") < 15 ? 'FIELD' : 'DESK';
  }

  protected markForCheck() {
    this.cd.markForCheck();
  }
}
