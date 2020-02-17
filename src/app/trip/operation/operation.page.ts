import {AfterViewInit, ChangeDetectionStrategy, Component, Injector, OnInit, ViewChild} from '@angular/core';
import {OperationFilter, OperationSaveOptions, OperationService} from '../services/operation.service';
import {OperationForm} from './operation.form';
import {Batch, EntityUtils, Operation, Trip} from '../services/trip.model';
import {TripService} from '../services/trip.service';
import {MeasurementsForm} from '../measurement/measurements.form.component';
import {AppEditorPage, AppTableUtils, environment} from '../../core/core.module';
import {CatchBatchForm} from '../catch/catch.form';
import {HistoryPageReference, UsageMode} from '../../core/services/model';
import {EditorDataServiceLoadOptions, fadeInOutAnimation, isNil, isNotNil} from '../../shared/shared.module';
import {AcquisitionLevelCodes, PmfmIds, ProgramService, QualitativeLabels} from '../../referential/referential.module';
import {BehaviorSubject, Subject} from 'rxjs';
import {MatTabChangeEvent, MatTabGroup} from "@angular/material";
import {debounceTime, distinctUntilChanged, filter, first, map, startWith, switchMap} from "rxjs/operators";
import {FormGroup, Validators} from "@angular/forms";
import * as moment from "moment";
import {IndividualMonitoringSubSamplesTable} from "../sample/individualmonitoring/individual-monitoring-samples.table";
import {Program, ProgramProperties} from "../../referential/services/model";
import {SubBatchesTable} from "../batch/sub-batches.table";
import {SubSamplesTable} from "../sample/sub-samples.table";
import {SamplesTable} from "../sample/samples.table";
import {BatchGroupsTable} from "../batch/batch-groups.table";
import {BatchUtils} from "../services/model/batch.model";
import {isNotNilOrBlank} from "../../shared/functions";
import {filterNotNil, firstNotNil} from "../../shared/observables";

@Component({
  selector: 'app-operation-page',
  templateUrl: './operation.page.html',
  styleUrls: ['./operation.page.scss'],
  animations: [fadeInOutAnimation],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class OperationPage extends AppEditorPage<Operation, OperationFilter> implements OnInit, AfterViewInit {

  readonly acquisitionLevel = AcquisitionLevelCodes.OPERATION;

  trip: Trip;
  programSubject = new BehaviorSubject<string>(null);
  onProgramChanged = new Subject<Program>();
  saveOptions: OperationSaveOptions = {};

  rankOrder: number;
  selectedBatchTabIndex = 0;
  selectedSampleTabIndex = 0;

  showSampleTables = false;
  showBatchTables = false;
  enableSubBatchesTable = false;

  mobile: boolean;

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

  get isOnFieldMode(): boolean {
    return this.usageMode ? this.usageMode === 'FIELD' : this.settings.isUsageMode('FIELD');
  }

  get form(): FormGroup {
    return this.opeForm.form;
  }

  constructor(
    injector: Injector,
    protected dataService: OperationService,
    protected tripService: TripService,
    protected programService: ProgramService
  ) {
    super(injector, Operation, dataService);
    this.idAttribute = 'operationId';

    // Init mobile (WARN
    this.mobile = this.settings.mobile;

    // FOR DEV ONLY ----
    this.debug = !environment.production;
  }

  ngOnInit() {
    super.ngOnInit();

    // Watch program, to configure tables from program properties
    this.registerSubscription(
      this.programSubject.asObservable()
        .pipe(
          filter(isNotNilOrBlank),
          distinctUntilChanged(),
          switchMap(programLabel => this.programService.watchByLabel(programLabel))
        )
        .subscribe(program => this.onProgramChanged.next(program)));
  }

  async ngAfterViewInit(): Promise<void> {

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


    this.ngAfterViewInitExtension();

    // Manage tab group
    const queryParams = this.route.snapshot.queryParams;
    const subTabIndex = queryParams["subtab"] && parseInt(queryParams["subtab"]) || 0;
    this.selectedBatchTabIndex = subTabIndex > 1 ? 1 : subTabIndex;
    this.selectedSampleTabIndex = subTabIndex;
    if (this.batchTabGroup) this.batchTabGroup.realignInkBar();
    if (this.sampleTabGroup) this.sampleTabGroup.realignInkBar();
  }


  /**
   * Configure specific behavior
   */
  protected ngAfterViewInitExtension() {

    if (this.measurementsForm) {
      firstNotNil(this.measurementsForm.$pmfms)
        .pipe(
          debounceTime(400)
        )
        .subscribe(() => {
          const formGroup = this.measurementsForm.form;

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

    // Configure somes tables from program properties
    this.registerSubscription(
      this.onProgramChanged
        .subscribe(program => {
          if (this.debug) console.debug(`[operation] Program ${program.label} loaded, with properties: `, program.properties);
          this.batchGroupsTable.showTaxonGroupColumn = program.getPropertyAsBoolean(ProgramProperties.TRIP_BATCH_TAXON_GROUP_ENABLE);
          this.batchGroupsTable.showTaxonNameColumn = program.getPropertyAsBoolean(ProgramProperties.TRIP_BATCH_TAXON_NAME_ENABLE);
          // Force taxon name in sub batches, if not filled in root batch
          if (this.subBatchesTable) {
            this.subBatchesTable.showTaxonNameColumn = !this.batchGroupsTable.showTaxonNameColumn;
            this.subBatchesTable.showIndividualCount = program.getPropertyAsBoolean(ProgramProperties.TRIP_BATCH_MEASURE_INDIVIDUAL_COUNT_ENABLE);
          }
          this.saveOptions.computeBatchRankOrder = program.getPropertyAsBoolean(ProgramProperties.TRIP_BATCH_MEASURE_RANK_ORDER_COMPUTE);
          this.saveOptions.computeBatchIndividualCount = program.getPropertyAsBoolean(ProgramProperties.TRIP_BATCH_INDIVIDUAL_COUNT_COMPUTE);
        })
    );
  }

  async onNewEntity(data: Operation, options?: EditorDataServiceLoadOptions): Promise<void> {
    const tripId = options && isNotNil(options.tripId) ? options.tripId :
      isNotNil(this.trip && this.trip.id) ? this.trip.id : (data && data.tripId);
    if (isNil(tripId)) throw new Error("Missing argument 'options.tripId'!");
    data.tripId = tripId;

    const trip = await this.tripService.load(tripId);
    data.trip = trip;

    // If is on field mode, fill default values
    if (this.isOnFieldMode) {
      data.startDateTime = moment();
    }

    // Use the default gear, if only one
    if (trip && trip.gears && trip.gears.length === 1) {
      data.physicalGear = trip.gears[0];
    }

    this.defaultBackHref = trip ? '/trips/' + trip.id  + '?tab=2': undefined;
  }

  async onEntityLoaded(data: Operation, options?: EditorDataServiceLoadOptions): Promise<void> {
    const tripId = options && isNotNil(options.tripId) ? options.tripId :
      isNotNil(this.trip && this.trip.id) ? this.trip.id : (data && data.tripId);
    if (isNil(tripId)) throw new Error("Missing argument 'options.tripId'!");
    data.tripId = tripId;

    const trip = await this.tripService.load(tripId);
    data.trip = trip;

    // Replace physical gear by the real entity
    data.physicalGear = (trip.gears || []).find(g => EntityUtils.equals(g, data.physicalGear)) || data.physicalGear;

    this.defaultBackHref = trip ? '/trips/' + trip.id  + '?tab=2' : undefined;
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

    // Trip exists
    const titlePrefix = this.trip && (await this.translate.get('TRIP.OPERATION.TITLE_PREFIX', {
      vessel: this.trip && this.trip.vesselSnapshot && (this.trip.vesselSnapshot.exteriorMarking || this.trip.vesselSnapshot.name) || '',
      departureDateTime: this.trip && this.trip.departureDateTime && this.dateFormat.transform(this.trip.departureDateTime) as string || ''
    }).toPromise()) || '';

    // new ope
    if (!data || isNil(data.id)) {
      return titlePrefix + (await this.translate.get('TRIP.OPERATION.NEW.TITLE').toPromise());
    }

    // Existing operation
    return titlePrefix + (await this.translate.get('TRIP.OPERATION.EDIT.TITLE', {
      startDateTime: data.startDateTime && this.dateFormat.transform(data.startDateTime, {time: true}) as string
    }).toPromise()) as string;
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

  /**
   * Open the first tab that is invalid
   */
  protected getFirstInvalidTabIndex(): number {
    // tab 0
    const tab0Invalid = this.opeForm.invalid || this.measurementsForm.invalid;
    // tab 1
    const subTab0Invalid = (this.showBatchTables && this.batchGroupsTable.invalid) || (this.showSampleTables && this.samplesTable.invalid);
    const subTab1Invalid = (this.showBatchTables && this.subBatchesTable.invalid) || (this.showSampleTables && this.individualMonitoringTable.invalid);
    const subTab2Invalid = this.showBatchTables && this.individualReleaseTable.invalid || false;
    const tab1Invalid = this.catchBatchForm.invalid || subTab0Invalid || subTab1Invalid || subTab2Invalid;

    // Open the first invalid tab
    const invalidTabIndex = tab0Invalid ? 0 : (tab1Invalid ? 1 : -1);

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
    return invalidTabIndex;
  }

  protected computeUsageMode(operation: Operation): UsageMode {
    return this.settings.isUsageMode('FIELD') && (
      isNil(this.trip) || (
        isNotNil(this.trip.departureDateTime)
        && this.trip.departureDateTime.diff(moment(), "day") < 15))
    ? 'FIELD' : 'DESK';
  }

  setValue(data: Operation) {

    // set parent trip
    const trip = data.trip;
    delete data.trip;
    this.trip = trip || this.trip;

    this.opeForm.value = data;
    if (trip) {
      this.opeForm.trip = trip;
    }

    const program = trip && trip.program && trip.program.label;

    // Get gear
    const gearLabel = data && data.physicalGear && data.physicalGear.gear && data.physicalGear.gear.label || null;

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
  }

  protected registerFormsAndTables() {
    // Register sub forms & table
    this.registerForms([this.opeForm, this.measurementsForm, this.catchBatchForm])
      .registerTables([
        this.samplesTable,
        this.individualMonitoringTable,
        this.individualReleaseTable,
        this.batchGroupsTable,
        this.subBatchesTable
      ]);
  }

  protected async waitWhilePending(): Promise<void> {
    this.form.updateValueAndValidity();
    return super.waitWhilePending();
  }

  protected async getValue(): Promise<Operation> {
    const data = await super.getValue();

    data.catchBatch = this.catchBatchForm.value;

    // save survival tables
    if (this.showSampleTables) {
      await this.samplesTable.save();
      await this.individualMonitoringTable.save();
      await this.individualReleaseTable.save();
      // get sub-samples, from tables
      const subSamples = (this.individualMonitoringTable.value || [])
        .concat(this.individualReleaseTable.value || []);
      data.samples = (this.samplesTable.value || [])
        .map(sample => {
          // Add children
          sample.children = subSamples.filter(childSample => childSample.parent && sample.equals(childSample.parent));
          return sample;
        });

    } else {
      data.samples = undefined;
    }

    // Save batch sampling tables
    if (this.showBatchTables) {
      await this.batchGroupsTable.save();
      await this.subBatchesTable.save();

      // get batches
      const batches = BatchUtils.prepareRootBatchesForSaving(this.batchGroupsTable.value, this.subBatchesTable.value, this.batchGroupsTable.qvPmfm);
      data.catchBatch.children = batches;
    } else {
      data.catchBatch.children = undefined;
    }

    return data;
  }

  protected getJsonValueToSave(): Promise<any> {
    const json = this.opeForm.value;
    json.measurements = this.measurementsForm.value;
    json.tripId = this.trip.id;
    return json;
  }

  async save(event, options?: any): Promise<boolean> {
    return super.save(event, {...options, ...this.saveOptions});
  }

  protected canUserWrite(data: Operation): boolean {
    return !!data && this.trip && isNil(this.trip.validationDate)
      && this.tripService.canUserWrite(this.trip);
  }

  protected addToPageHistory(page: HistoryPageReference) {
    super.addToPageHistory({ ...page, icon: 'pin'});
  }

  protected markForCheck() {
    this.cd.markForCheck();
  }
}
