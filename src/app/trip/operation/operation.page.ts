import {AfterViewInit, ChangeDetectionStrategy, Component, Injector, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {OperationSaveOptions, OperationService} from '../services/operation.service';
import {OperationForm} from './operation.form';
import {TripService} from '../services/trip.service';
import {MeasurementsForm} from '../measurement/measurements.form.component';
import {AppEntityEditor, AppTableUtils, EntityUtils, environment} from '../../core/core.module';
import {CatchBatchForm} from '../catch/catch.form';
import {ReferentialUtils} from '../../core/services/model/referential.model';
import {HistoryPageReference, UsageMode} from '../../core/services/model/settings.model';
import {
  EntityServiceLoadOptions,
  fadeInOutAnimation,
  isNil,
  isNotEmptyArray,
  isNotNil
} from '../../shared/shared.module';
import {BehaviorSubject, Observable, Subject, Subscription} from 'rxjs';
import {MatTabChangeEvent, MatTabGroup} from "@angular/material/tabs";
import {debounceTime, distinctUntilChanged, filter, first, map, startWith, switchMap, tap} from "rxjs/operators";
import {FormGroup, Validators} from "@angular/forms";
import * as moment from "moment";
import {IndividualMonitoringSubSamplesTable} from "../sample/individualmonitoring/individual-monitoring-samples.table";
import {Program} from "../../referential/services/model/program.model";
import {SubBatchesTable} from "../batch/sub-batches.table";
import {SubSamplesTable} from "../sample/sub-samples.table";
import {SamplesTable} from "../sample/samples.table";
import {BatchGroupsTable} from "../batch/batch-groups.table";
import {Batch, BatchUtils} from "../services/model/batch.model";
import {isNotNilOrBlank} from "../../shared/functions";
import {filterNotNil, firstNotNil, firstNotNilPromise, firstTruePromise} from "../../shared/observables";
import {Operation, Trip} from "../services/model/trip.model";
import {BatchGroup} from "../services/model/batch-group.model";
import {ProgramProperties} from "../../referential/services/config/program.config";
import {AcquisitionLevelCodes, PmfmIds, QualitativeLabels} from "../../referential/services/model/model.enum";
import {ProgramService} from "../../referential/services/program.service";
import {TranslateService} from "@ngx-translate/core";
import {IEntity} from "../../core/services/model/entity.model";
import {PlatformService} from "../../core/services/platform.service";

@Component({
  selector: 'app-operation-page',
  templateUrl: './operation.page.html',
  styleUrls: ['./operation.page.scss'],
  animations: [fadeInOutAnimation],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class OperationPage extends AppEntityEditor<Operation, OperationService> implements OnInit, AfterViewInit, OnDestroy {

  readonly acquisitionLevel = AcquisitionLevelCodes.OPERATION;

  trip: Trip;
  programSubject = new BehaviorSubject<string>(null);
  onProgramChanged = new Subject<Program>();
  saveOptions: OperationSaveOptions = {};
  readonly dateTimePattern: string;
  $lastOperations = new BehaviorSubject<Operation[]>(null);
  private _lastOperationsSubscription: Subscription;

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
    protected programService: ProgramService,
    protected platform: PlatformService
  ) {
    super(injector, Operation, dataService, {
      pathIdAttribute: 'operationId',
      tabCount: 3,
      autoUpdateRoute: !platform.mobile,
      autoOpenNextTab: !platform.mobile
    });

    this.dateTimePattern = this.translate.instant('COMMON.DATE_TIME_PATTERN');

    // Init mobile
    this.mobile = this.settings.mobile;

    // FOR DEV ONLY ----
    this.debug = !environment.production;
  }

  ngOnInit() {
    super.ngOnInit();

    // Watch program, to configure tables from program properties
    this.registerSubscription(
      this.programSubject
        .pipe(
          filter(isNotNilOrBlank),
          distinctUntilChanged(),
          switchMap(programLabel => this.programService.watchByLabel(programLabel))
        )
        .subscribe(program => this.onProgramChanged.next(program)));

    this.registerSubscription(() => this.$lastOperations.complete());
  }

  async ngAfterViewInit(): Promise<void> {

    this.registerSubscription(
      this.opeForm.form.controls['physicalGear'].valueChanges
        .subscribe((res) => {
          if (this.loading) return; // SKip during loading
          const gearId = res && res.gear && res.gear.id || null;
          this.measurementsForm.gearId = gearId;
          this.catchBatchForm.gearId = gearId;
        })
    );

    // Update available parent on sub-sample table, when samples changes
    this.registerSubscription(
      this.samplesTable.dataSource.datasourceSubject
        .pipe(
          debounceTime(500),
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
                  startWith<any, any>(samplingTypeControl.value),
                  filter(ReferentialUtils.isNotEmpty),
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
                  startWith<any, any>(isSamplingControl.value),
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
                  startWith<any, any>(tripProgressControl.value),
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

    // Configure page, from Program's properties
    this.registerSubscription(
      this.onProgramChanged
        .subscribe(program => {
          if (this.debug) console.debug(`[operation] Program ${program.label} loaded, with properties: `, program.properties);
          this.opeForm.defaultLatitudeSign = program.getProperty(ProgramProperties.TRIP_LATITUDE_SIGN);
          this.opeForm.defaultLongitudeSign = program.getProperty(ProgramProperties.TRIP_LONGITUDE_SIGN);
          this.batchGroupsTable.showTaxonGroupColumn = program.getPropertyAsBoolean(ProgramProperties.TRIP_BATCH_TAXON_GROUP_ENABLE);
          this.batchGroupsTable.showTaxonNameColumn = program.getPropertyAsBoolean(ProgramProperties.TRIP_BATCH_TAXON_NAME_ENABLE);

          // Some specific taxon groups have no weight collected
          const taxonGroupsNoWeight = program.getProperty(ProgramProperties.TRIP_BATCH_TAXON_GROUPS_NO_WEIGHT);
          this.batchGroupsTable.taxonGroupsNoWeight = taxonGroupsNoWeight && taxonGroupsNoWeight.split(',')
            .map(label => label.trim().toUpperCase())
            .filter(isNotNilOrBlank) || undefined;

          // Force taxon name in sub batches, if not filled in root batch
          if (this.subBatchesTable) {
            this.subBatchesTable.showTaxonNameColumn = !this.batchGroupsTable.showTaxonNameColumn;
            this.subBatchesTable.showIndividualCount = program.getPropertyAsBoolean(ProgramProperties.TRIP_BATCH_MEASURE_INDIVIDUAL_COUNT_ENABLE);
          }
          this.saveOptions.computeBatchRankOrder = program.getPropertyAsBoolean(ProgramProperties.TRIP_BATCH_MEASURE_RANK_ORDER_COMPUTE);
          this.saveOptions.computeBatchIndividualCount = program.getPropertyAsBoolean(ProgramProperties.TRIP_BATCH_INDIVIDUAL_COUNT_COMPUTE);

          // Autofill batch group table (e.g. with taxon groups found in strategies)
          const autoFillBatch = program.getPropertyAsBoolean(ProgramProperties.TRIP_BATCH_AUTO_FILL);
          this.setAutoFillSpecies(autoFillBatch);
        })
    );
  }

  ngOnDestroy() {
    super.ngOnDestroy();
  }

  async onNewEntity(data: Operation, options?: EntityServiceLoadOptions): Promise<void> {
    const tripId = options && isNotNil(options.tripId) ? +(options.tripId) :
      isNotNil(this.trip && this.trip.id) ? this.trip.id : (data && data.tripId);
    if (isNil(tripId)) throw new Error("Missing argument 'options.tripId'!");
    data.tripId = tripId;

    // Load parent trip
    const trip = await this.tripService.load(tripId);
    data.trip = trip;

    // Use the default gear, if only one
    if (trip && trip.gears && trip.gears.length === 1) {
      data.physicalGear = trip.gears[0];
    }


    // If is on field mode, fill default values
    if (this.isOnFieldMode) {
      data.startDateTime = moment();

      // Load last trip's operations
      this.loadLastOperations(tripId, data);

      // Wait last operations to be loaded
      const previousOperations = await firstNotNilPromise(this.$lastOperations);

      // Copy from previous operation
      if (isNotEmptyArray(previousOperations)) {
        const previousOperation = previousOperations
          .find(ope => ope && ope !== data && ReferentialUtils.isNotEmpty(ope.metier));
        if (previousOperation) {
          data.physicalGear = (trip.gears || []).find(g => EntityUtils.equals(g, previousOperation.physicalGear, 'id')) || data.physicalGear;
          data.metier = previousOperation.metier;
          data.rankOrderOnPeriod = previousOperation.rankOrderOnPeriod + 1;
        }
      }
    }

    this.defaultBackHref = trip ? `/trips/${trip.id}?tab=2` : undefined;
  }

  async onEntityLoaded(data: Operation, options?: EntityServiceLoadOptions): Promise<void> {
    const tripId = options && isNotNil(options.tripId) ? +(options.tripId) :
      isNotNil(this.trip && this.trip.id) ? this.trip.id : (data && data.tripId);
    if (isNil(tripId)) throw new Error("Missing argument 'options.tripId'!");
    data.tripId = tripId;

    const trip = await this.tripService.load(tripId);
    data.trip = trip;

    // Replace physical gear by the real entity
    data.physicalGear = (trip.gears || []).find(g => EntityUtils.equals(g, data.physicalGear, 'id')) || data.physicalGear;

    if (this.isOnFieldMode) {
      // Load last trip's operations
      this.loadLastOperations(tripId, data);
    }

    this.defaultBackHref = trip ? `/trips/${trip.id}?tab=2` : undefined;
  }


  onNewFabButtonClick(event: UIEvent) {
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
  protected async computeTitle(data: Operation, opts?: {
    withPrefix?: boolean;
  }): Promise<string> {

    // Trip exists
    const titlePrefix = (!opts || opts.withPrefix !== false) && this.trip && (await this.translate.get('TRIP.OPERATION.TITLE_PREFIX', {
      vessel: this.trip && this.trip.vesselSnapshot && (this.trip.vesselSnapshot.exteriorMarking || this.trip.vesselSnapshot.name) || '',
      departureDateTime: this.trip && this.trip.departureDateTime && this.dateFormat.transform(this.trip.departureDateTime) as string || ''
    }).toPromise()) || '';

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
    await AppTableUtils.waitIdle(this.subBatchesTable);

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

  onBatchTabChange(event: MatTabChangeEvent) {
    super.onSubTabChange(event);
    if (!this.loading) {
      // On each tables, confirm editing row
      this.batchGroupsTable.confirmEditCreate();
      this.subBatchesTable.confirmEditCreate();
    }
  }

  onSampleTabChange(event: MatTabChangeEvent) {
    super.onSubTabChange(event);
    if (!this.loading) {
      // On each tables, confirm editing row
      this.samplesTable.confirmEditCreate();
      this.individualMonitoringTable.confirmEditCreate();
      this.individualReleaseTable.confirmEditCreate();
    }
  }

  async onOpenOperation(event: UIEvent, id: number): Promise<any> {
    if (event && event.defaultPrevented) return; // Skip

    if (isNil(id) || this.data.id === id) return; // skip

    const savePromise: Promise<boolean> = this.isOnFieldMode && this.dirty && this.valid
      // If on field mode: try to save silently
      ? this.save(event)
      // If desktop mode: ask before save
      : this.saveIfDirtyAndConfirm(null, {
        ...this.saveOptions,
        emitEvent: false /*do not update view*/
      });
    const canContinue = await savePromise;

    if (canContinue) {
      return this.load(+id, {tripId: this.data.tripId, updateTabAndRoute: true});
    }
  }

  async onNewOperation(event: UIEvent): Promise<any> {
    if (event && event.defaultPrevented) return Promise.resolve(); // Skip
    if (event) event.preventDefault();

    const savePromise: Promise<boolean> = this.isOnFieldMode && this.dirty && this.valid
      // If on field mode: try to save silently
      ? this.save(event)
      // If desktop mode: ask before save
      : this.saveIfDirtyAndConfirm(null, {
        ...this.saveOptions,
        emitEvent: false /*do not update view*/
      });
    const canContinue = await savePromise;
    if (canContinue) {
      return this.load(undefined, {tripId: this.data.tripId, updateTabAndRoute: true});
    }
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

    // Get gear, from the physical gear
    const gearId = data && data.physicalGear && data.physicalGear.gear && data.physicalGear.gear.id || null;

    // Set measurements form
    this.measurementsForm.gearId = gearId;
    this.measurementsForm.program = program;
    this.measurementsForm.value = data && data.measurements || [];

    // Get all batches (and children), and samples
    const batches = (data && data.catchBatch && data.catchBatch.children) || [];
    const samples = (data && data.samples || []).reduce((res, sample) => !sample.children ? res.concat(sample) : res.concat(sample).concat(sample.children), []);

    // Set catch batch
    this.catchBatchForm.gearId = gearId;
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
    this.batchGroupsTable.value = batches.filter(s => s.label && s.label.startsWith(this.batchGroupsTable.acquisitionLevel + "#"))
      .map(BatchGroup.fromBatch);

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

  isCurrentData(other: IEntity<any>): boolean {
    return (this.isNewData && isNil(other.id))
      || (this.data && this.data.id == other.id);
  }

  /* -- protected method -- */


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

  protected registerForms() {
    // Register sub forms & table
    this.addChildForms([
      this.opeForm,
      this.measurementsForm,
      this.catchBatchForm,
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
      let batches;
      if (this.mobile) {
        await this.batchGroupsTable.save();

        // get batches
        console.warn("TODO: When mobile=true, should use a better implementation (e.g. without using subBatchesTable)");
        await this.subBatchesTable.save();

        batches = BatchUtils.prepareRootBatchesForSaving(this.batchGroupsTable.value, this.subBatchesTable.value, this.batchGroupsTable.qvPmfm);
      }
      else {
        await this.batchGroupsTable.save();
        await this.subBatchesTable.save();

        batches = BatchUtils.prepareRootBatchesForSaving(this.batchGroupsTable.value, this.subBatchesTable.value, this.batchGroupsTable.qvPmfm);
      }
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
    // Force to pass specific saved options to dataService.save()
    return await super.save(event, {...options, ...this.saveOptions});
  }

  protected canUserWrite(data: Operation): boolean {
    return !!data && this.trip && isNil(this.trip.validationDate)
      && this.tripService.canUserWrite(this.trip);
  }

  protected addToPageHistory(page: HistoryPageReference) {
    super.addToPageHistory({ ...page, icon: 'navigate'});
  }

  protected async setAutoFillSpecies(enable: boolean) {
    if (!this.showBatchTables || !this.batchGroupsTable.showTaxonGroupColumn || !enable) {
      // Reset table's default taxon groups
      this.batchGroupsTable.defaultTaxonGroups = null;
      return; // Skip
    }

    if (this.debug) console.debug("[operation] Check if can auto fill species...");
    let includeTaxonGroups: string[];

    // Retrieve the trip measurements on SELF_SAMPLING_PROGRAM, if any
    const qvMeasurement = (this.trip.measurements || []).find(m => m.pmfmId === PmfmIds.SELF_SAMPLING_PROGRAM);
    if (qvMeasurement && ReferentialUtils.isNotEmpty(qvMeasurement.qualitativeValue)) {

      // Retrieve QV from the program pmfm (because measurement's QV has only the 'id' attribute)
      const tripPmfms = await this.programService.loadProgramPmfms(this.programSubject.getValue(), {acquisitionLevel: AcquisitionLevelCodes.TRIP});
      const pmfm = (tripPmfms || []).find(pmfm => pmfm.pmfmId === PmfmIds.SELF_SAMPLING_PROGRAM);
      const qualitativeValue = (pmfm && pmfm.qualitativeValues || []).find(qv => qv.id === qvMeasurement.qualitativeValue.id);

      // Transform QV.label has a list of TaxonGroup.label
      if (qualitativeValue && qualitativeValue.label) {
        includeTaxonGroups = qualitativeValue.label
          .split(/[^\w]+/) // Split by separator (= not a word)
          .filter(isNotNilOrBlank)
          .map(label => label.trim().toUpperCase());
      }
    }

    // Set table's default taxon groups
    this.batchGroupsTable.defaultTaxonGroups = includeTaxonGroups;
    this.markForCheck();

    // If new data, auto fill the table
    if (this.isNewData) {
      await this.batchGroupsTable.autoFillTable();
    }
  }

  protected async updateRoute(data: Operation, queryParams: any): Promise<boolean> {
    const id = data && isNotNil(data.id) ? data.id : 'new';
    return await this.router.navigate([`/trips/${this.trip.id}/operations/${id}`], {
      replaceUrl: true,
      queryParams: queryParams,
      queryParamsHandling: "preserve"
    });
  }

  protected loadLastOperations(tripId: number, data?: Operation): Observable<Operation[]> {
    if (this._lastOperationsSubscription) {
      this._lastOperationsSubscription.unsubscribe();
      this.unregisterSubscription(this._lastOperationsSubscription);
    }

    // Load last trip's operations
    this._lastOperationsSubscription = this.dataService.watchAll(0, 10, 'startDateTime', 'desc', {
        tripId
      }, {
        withBatchTree: false,
        withSamples: false,
        fetchPolicy: 'cache-first'
      }).pipe(
        // Ignore event due to save action
        filter(res => !this.saving),
        debounceTime(500),

        map(res => res && res.data || []),

        /*map(items => {
          items.sort(EntityUtils.sortComparator('startDateTime', 'desc'));
        })*/
      ).subscribe(data => this.$lastOperations.next(data));
    this._lastOperationsSubscription.add(() => {
      console.debug('[operation] Completed last operations observable');
    })
    this.registerSubscription(this._lastOperationsSubscription);


    return this.$lastOperations;
  }

  protected markForCheck() {
    this.cd.markForCheck();
  }
}
