import {
  AfterViewInit,
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  NgZone,
  OnInit,
  ViewChild
} from '@angular/core';
import {ActivatedRoute, Router} from "@angular/router";
import {OperationService} from '../services/operation.service';
import {OperationForm} from './operation.form';
import {Batch, EntityUtils, ObservedLocation, Operation, PhysicalGear, Trip} from '../services/trip.model';
import {TripService} from '../services/trip.service';
import {MeasurementsForm} from '../measurement/measurements.form.component';
import {AppFormUtils, AppTableUtils, AppTabPage, environment} from '../../core/core.module';
import {CatchBatchForm} from '../catch/catch.form';
import {AlertController} from "@ionic/angular";
import {TranslateService} from '@ngx-translate/core';
import {UsageMode} from '../../core/services/model';
import {fadeInOutAnimation, isNil, isNotNil} from '../../shared/shared.module';
import {AcquisitionLevelCodes, PmfmIds, ProgramService, QualitativeLabels} from '../../referential/referential.module';
import {BehaviorSubject, Subject} from 'rxjs';
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
import {BatchGroupsTable} from "../batch/batch-groups.table";
import {BatchUtils} from "../services/model/batch.model";
import {isNotNilOrBlank} from "../../shared/functions";
import {filterNotNil} from "../../shared/observables";
import {LocalSettingsService} from "../../core/services/local-settings.service";

@Component({
  selector: 'page-operation',
  templateUrl: './operation.page.html',
  styleUrls: ['./operation.page.scss'],
  animations: [fadeInOutAnimation],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class OperationPage extends AppTabPage<Operation, { tripId: number }> implements OnInit, AfterViewInit {

  protected _enableListenChanges = (environment.listenRemoteChanges === true);

  title = new Subject<string>();
  trip: Trip;
  programSubject = new BehaviorSubject<string>(undefined);
  saving = false;
  rankOrder: number;
  selectedBatchTabIndex = 0;
  selectedSampleTabIndex = 0;

  defaultBackHref: string;

  showSampleTables = false;
  showBatchTables = false;
  enableSubBatchesTable = false;

  usageMode: UsageMode;
  mobile: boolean;
  idAttribute: string; // TODO remove when inherit from class EditorPage

  @ViewChild('batchTabGroup', { static: true }) batchTabGroup: MatTabGroup;
  @ViewChild('sampleTabGroup', { static: true }) sampleTabGroup: MatTabGroup;
  @ViewChild('opeForm', { static: true }) opeForm: OperationForm;
  @ViewChild('measurementsForm', { static: true }) measurementsForm: MeasurementsForm;
  @ViewChild('catchBatchForm', { static: true }) catchBatchForm: CatchBatchForm;

  // Sample tables
  @ViewChild('samplesTable', { static: true }) samplesTable: SamplesTable;
  @ViewChild('individualMonitoringTable', { static: true }) individualMonitoringTable: IndividualMonitoringSubSamplesTable;
  @ViewChild('individualReleaseTable', { static: true }) individualReleaseTable: SubSamplesTable;

  @ViewChild('batchGroupsTable', { static: true }) batchGroupsTable: BatchGroupsTable;
  @ViewChild('subBatchesTable', { static: true }) subBatchesTable: SubBatchesTable;

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
    protected cd: ChangeDetectorRef,
    protected zone: NgZone,
  ) {
    super(route, router, alterCtrl, translate);

    this.idAttribute = 'operationId';

    // Init mobile (WARN
    this.mobile = this.settings.mobile;

    // FOR DEV ONLY ----
    this.debug = !environment.production;
  }

  ngOnInit() {
    super.ngOnInit();

    //await this.settings.ready();

    // Register sub forms & table
    this.registerForms([this.opeForm, this.measurementsForm, this.catchBatchForm])
      .registerTables([
        this.samplesTable,
        this.individualMonitoringTable,
        this.individualReleaseTable,
        this.batchGroupsTable,
        this.subBatchesTable
      ]);

    // Disable, during load
    this.disable();

    this.registerSubscription(
      this.opeForm.form.controls['physicalGear'].valueChanges
        .subscribe((res) => {
          if (this.loading) return; // SKip during loading
          const gearLabel = res && res.gear && res.gear.label || null;
          this.measurementsForm.gear = gearLabel;
          this.catchBatchForm.gear = gearLabel;
        })
    );

    // Update available parent on sub-sample table, when samples changes
    this.registerSubscription(
      this.samplesTable.dataSource.datasourceSubject
        .pipe(
          debounceTime(400),
          // skip if loading
          filter(() => !this.loading)
        )
        .subscribe(samples => {
          if (this.loading) return; // skip during loading
          // Get parents with a TAG_ID
          const availableParents = (samples || [])
            .filter(s => isNotNil(s.measurementValues[PmfmIds.TAG_ID.toString()]));

          // Will refresh the tables (inside the setter):
          this.individualMonitoringTable.availableParents = availableParents;
          this.individualReleaseTable.availableParents = availableParents;
        }));

    // Update available parent on individual batch table, when batch group changes
    this.registerSubscription(
      this.batchGroupsTable.dataSource.datasourceSubject
        .pipe(
          debounceTime(400),
          // skip if loading
          filter(() => !this.loading && this.enableSubBatchesTable)
        )
        // Will refresh the tables (inside the setter):
        .subscribe(rootBatches => this.subBatchesTable.availableParents = (rootBatches || []))
    );


    // Enable sub batches when table pmfms ready
    this.registerSubscription(
      filterNotNil(this.subBatchesTable.$pmfms)
        .subscribe(pmfms => {
          if (!this.enableSubBatchesTable && pmfms.length > 0) {
            this.enableSubBatchesTable = true;
            this.markForCheck();
          }
        }));

    // Link group table to individual
    this.batchGroupsTable.availableSubBatchesFn = async () => {
      if (this.subBatchesTable.dirty) await this.subBatchesTable.save();
      return this.subBatchesTable.value;
    };

    this.ngInitExtension();

    // Listen route query parameters
    const queryParams = this.route.snapshot.queryParams;
    const subTabIndex = queryParams["subtab"] && parseInt(queryParams["subtab"]) || 0;
    this.selectedBatchTabIndex = subTabIndex > 1 ? 1 : subTabIndex;
    this.selectedSampleTabIndex = subTabIndex;
    if (this.batchTabGroup) this.batchTabGroup.realignInkBar();
    if (this.sampleTabGroup) this.sampleTabGroup.realignInkBar();
  }

  async ngAfterViewInit(): Promise<void> {
    const {tripId, operationId} = this.route.snapshot.params;
    if (isNil(tripId)) return; // skip
    if (isNil(operationId) || operationId === "new") {
      await this.load(undefined, {tripId: tripId});
    } else {
      await this.load(+operationId, {tripId: tripId});
    }
  }

  async load(id?: number, options?: { tripId: number }): Promise<void> {
    this.error = null;

    // New operation
    if (isNil(id)) {
      if (this.debug) console.debug("[page-operation] Creating new operation...");

      if (!options || isNil(options.tripId)) throw new Error("Missing argument 'options.tripId'!");

      const trip = await this.tripService.load(options.tripId);

      this.usageMode = this.computeUsageMode(trip);

      const data = new Operation();
      data.tripId = trip.id;

      // If is on field mode, fill default values
      if (this.usageMode === 'FIELD') {
        data.startDateTime = moment();
      }

      // Use the default gear, if only one
      if (trip.gears.length === 1) {
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

      // Replace physical gear by the real entity
      data.physicalGear = (trip.gears || []).find(g => EntityUtils.equals(g, data.physicalGear)) || data.physicalGear;

      await this.updateView(data, trip);
      this.loading = false;
      this.startListenRemoteChanges();
    }
  }

  startListenRemoteChanges() {

    // Listen for changes on server
    if (isNotNil(this.data.id) && this._enableListenChanges) {

      const subscription = this.operationService.listenChanges(this.data.id)
        .subscribe((data: Operation | undefined) => {
          const newUpdateDate = data && (data.updateDate as Moment) || undefined;
          if (isNotNil(newUpdateDate) && newUpdateDate.isAfter(this.data.updateDate)) {
            if (this.debug) console.debug("[operation] Detected update on server at:", newUpdateDate);
            if (!this.dirty) {
              this.updateView(data, this.trip);
            }
            else {
              // TODO: warn the user, with : a reload button, a force button (to copy updateDate)
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

    const program = trip && trip.program && trip.program.label;
    const gearLabel = data && data.physicalGear && data.physicalGear.gear && data.physicalGear.gear.label || null;

    if (trip) {
      this.trip = trip;
      this.opeForm.trip = trip;
    }

    // Set measurements form
    this.measurementsForm.gear = gearLabel;
    this.measurementsForm.program = program;
    this.measurementsForm.value = data && data.measurements || [];

    // Get all batches (and children), and samples
    const batches = (data && data.catchBatch && data.catchBatch.children) || [];
    const samples = (data && data.samples || []).reduce((res, sample) => !sample.children ? res.concat(sample) : res.concat(sample).concat(sample.children), []);

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
    this.batchGroupsTable.value = batches.filter(s => s.label && s.label.startsWith(this.batchGroupsTable.acquisitionLevel + "#"));

    // make sure PMFMs are loaded (need the QV pmfm)
    this.batchGroupsTable.$pmfms
      .pipe(filter(isNotNil), first())
      .subscribe((_) => {
        this.subBatchesTable.setValueFromParent(this.batchGroupsTable.value, this.batchGroupsTable.qvPmfm);
      });

    // Applying program to tables (async)
    if (program) {
      setTimeout(() => {
        this.programSubject.next(program);
      }, 250);
    }

    // Update title
    this.updateTitle();

    // Compute the default back href
    if (data && isNotNil(data.tripId)) {
      this.defaultBackHref = `/trips/${this.data.tripId}?tab=2`;
    }
    else {
      this.defaultBackHref = null;
    }

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
      this.measurementsForm.$pmfms
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
                  startWith(samplingTypeControl.value),
                  filter(EntityUtils.isNotEmpty),
                  map(qv => qv.label),
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
                  if (this.batchTabGroup) this.batchTabGroup.realignInkBar();
                  if (this.sampleTabGroup) this.sampleTabGroup.realignInkBar();
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
                  startWith(isSamplingControl.value),
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
                  if (this.batchTabGroup) this.batchTabGroup.realignInkBar();
                  if (this.sampleTabGroup) this.sampleTabGroup.realignInkBar();
                  this.markForCheck();
                })
            );
          }

          // Default
          if (isNil(samplingTypeControl) && isNil(isSamplingControl)) {
            if (this.debug) console.debug("[operation] Enable default tables (Nor SUMARiS nor ADAP pmfms were found)");
            this.showSampleTables = false;
            this.showBatchTables = true;
            if (this.batchTabGroup) this.batchTabGroup.realignInkBar();
            if (this.sampleTabGroup) this.sampleTabGroup.realignInkBar();
            this.markForCheck();
          }

          // Abnormal trip => Set comment as required
          const tripProgressControl = formGroup && formGroup.controls[PmfmIds.TRIP_PROGRESS];
          if (isNotNil(samplingTypeControl)) {
            this.registerSubscription(
              tripProgressControl.valueChanges
                .pipe(
                  debounceTime(400),
                  startWith(tripProgressControl.value),
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
                  commentControl.updateValueAndValidity({emitEvent: false, onlySelf: true});
                })
            );
          }

        });
    }

    // Watch program, to configure tables from program properties
    this.registerSubscription(
      this.programSubject
        .pipe(
          filter(isNotNilOrBlank),
          switchMap(label => this.programService.watchByLabel(label, true))
        )
        .subscribe(program => {
          if (this.debug) console.debug(`[operation] Program ${program.label} loaded, with properties: `, program.properties);
          this.batchGroupsTable.showTaxonGroupColumn = program.getPropertyAsBoolean(ProgramProperties.TRIP_BATCH_TAXON_GROUP_ENABLE);
          this.batchGroupsTable.showTaxonNameColumn = program.getPropertyAsBoolean(ProgramProperties.TRIP_BATCH_TAXON_NAME_ENABLE);
          // Force taxon name in sub batches, if not filled in root batch
          if (this.subBatchesTable) {
            this.subBatchesTable.showTaxonNameColumn = !this.batchGroupsTable.showTaxonNameColumn;
            this.subBatchesTable.showIndividualCount = program.getPropertyAsBoolean(ProgramProperties.TRIP_BATCH_MEASURE_INDIVIDUAL_COUNT_ENABLE);
          }
        })
    );

  }

  async save(event): Promise<any> {
    if (this.loading || this.saving) return;

    // Not valid
    if (!this.valid) {
      this.markAsTouched({emitEvent: true});

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
    const json = this.opeForm.value;
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
      await this.batchGroupsTable.save();
      await this.subBatchesTable.save();

      // get batches
      const batches = BatchUtils.prepareRootBatchesForSaving(this.batchGroupsTable.value, this.subBatchesTable.value, this.batchGroupsTable.qvPmfm);
      this.data.catchBatch.children = batches;
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
        this.startListenRemoteChanges();
      }

      this.submitted = false;

      return updatedData;
    } catch (err) {
      console.error(err && err.message || err, err);
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

  addTableRow(event: UIEvent) {
    if (this.showBatchTables) {
      switch (this.selectedBatchTabIndex) {
        case 0:
          this.batchGroupsTable.addRow(event);
          break;
        case 1:
          this.subBatchesTable.addRow(event);
          break;
      }
    }
    else if (this.showSampleTables) {
      switch (this.selectedSampleTabIndex) {
        case 0:
          this.samplesTable.addRow(event);
          break;
        case 1:
          this.individualMonitoringTable.addRow(event);
          break;
        case 2:
          this.individualReleaseTable.addRow(event);
          break;
      }
    }
  }

  /**
   * Compute the title
   * @param data
   */
  protected async computeTitle(data: Operation): Promise<string> {

    // new ope
    if (!data || isNil(data.id)) {
      return await this.translate.get('TRIP.OPERATION.NEW.TITLE').toPromise();
    }

    // Existing ope
    const title = (await this.translate.get('TRIP.OPERATION.EDIT.TITLE', {
      vessel: this.trip && this.trip.vesselFeatures && (this.trip.vesselFeatures.exteriorMarking || this.trip.vesselFeatures.name) || '',
      departureDateTime: this.trip && this.trip.departureDateTime && this.dateFormat.transform(this.trip.departureDateTime) as string || '',
      startDateTime: data.startDateTime && this.dateFormat.transform(data.startDateTime, {time: true}) as string
    }).toPromise()) as string;

    return title;
  }

  async onSubBatchesChanges(subbatches: Batch[]) {
    if (isNil(subbatches)) return; // user cancelled

    // TODO: for mobile, hide sub-batches tables, and store data else where ?
    this.subBatchesTable.value = subbatches;
    await AppTableUtils.waitLoaded(this.subBatchesTable);

    this.subBatchesTable.markAsDirty();
  }


  protected getBatchChildrenByLevel(batch: Batch, acquisitionLevel: string): Batch[] {
    return (batch.children || []).reduce((res, child) => {
      if (child.label && child.label.startsWith(acquisitionLevel + "#")) return res.concat(child);
      return res.concat(this.getBatchChildrenByLevel(child, acquisitionLevel)); // recursive call
    }, []);
  }

  onTabChange(event: MatTabChangeEvent, queryParamName?: string): boolean {
    const changed = super.onTabChange(event, queryParamName);
    if (changed && this.selectedTabIndex === 1) {
      if (this.batchTabGroup) this.batchTabGroup.realignInkBar();
      if (this.sampleTabGroup) this.sampleTabGroup.realignInkBar();
      this.markForCheck();
    }
    return changed;
  }

  public onBatchTabChange(event: MatTabChangeEvent) {
    super.onSubTabChange(event);
    if (!this.loading) {
      // On each tables, confirm editing row
      this.batchGroupsTable.confirmEditCreate();
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
    if (this.batchGroupsTable.invalid) {
      if (this.batchGroupsTable.editedRow && this.batchGroupsTable.editedRow.editing) {
        AppFormUtils.logFormErrors(this.batchGroupsTable.editedRow.validator, "[batches-table]");
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
    const subTab0Invalid = (this.showBatchTables && this.batchGroupsTable.invalid) || (this.showSampleTables && this.samplesTable.invalid);
    const subTab1Invalid = (this.showBatchTables && this.subBatchesTable.invalid) || (this.showSampleTables && this.individualMonitoringTable.invalid);
    const subTab2Invalid = this.showBatchTables && this.individualReleaseTable.invalid || false;
    const tab1Invalid = this.catchBatchForm.invalid || subTab0Invalid || subTab1Invalid || subTab2Invalid;

    // Open the first invalid tab
    const invalidTabIndex = tab0Invalid ? 0 : (tab1Invalid ? 1 : this.selectedTabIndex);
    if (this.selectedTabIndex === 0 && !tab0Invalid) {
      this.selectedTabIndex = invalidTabIndex;
    } else if (this.selectedTabIndex === 1 && !tab1Invalid) {
      this.selectedTabIndex = invalidTabIndex;
    }

    // If tab 1, open the invalid sub tab
    if (invalidTabIndex === 1) {
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

  /**
   * Compute the title
   * @param data
   */
  protected async updateTitle(data?: Operation) {
    const title = await this.computeTitle(data || this.data);
    this.title.next(title);

    if (!this.isNewData) {
      // Add to page history
      this.settings.addToPageHistory({
        title: title,
        path: this.router.url,
        icon: 'locate'
      });
    }
  }

  protected markForCheck() {
    console.debug("[operation] markForCheck")
    this.cd.markForCheck();
  }
}
