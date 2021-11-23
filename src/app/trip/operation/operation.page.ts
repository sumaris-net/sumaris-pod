import { ChangeDetectionStrategy, Component, Injector, ViewChild } from '@angular/core';
import { OperationSaveOptions, OperationService } from '../services/operation.service';
import { OperationForm } from './operation.form';
import { TripService } from '../services/trip.service';
import { MeasurementsForm } from '../measurement/measurements.form.component';
import {
  AppEntityEditor,
  EntityServiceLoadOptions,
  EntityUtils,
  fadeInOutAnimation,
  firstNotNilPromise,
  firstTruePromise,
  fromDateISOString,
  HistoryPageReference,
  IEntity,
  isNil,
  isNotEmptyArray,
  isNotNil,
  isNotNilOrBlank,
  PlatformService,
  ReferentialUtils,
  SharedValidators,
  toBoolean,
  toNumber,
  UsageMode,
} from '@sumaris-net/ngx-components';
import { MatTabChangeEvent, MatTabGroup } from '@angular/material/tabs';
import { debounceTime, distinctUntilChanged, filter, map, mergeMap, startWith, switchMap } from 'rxjs/operators';
import { FormGroup, Validators } from '@angular/forms';
import * as momentImported from 'moment';
import { IndividualMonitoringSubSamplesTable } from '../sample/individualmonitoring/individual-monitoring-samples.table';
import { Program } from '@app/referential/services/model/program.model';
import { SubSamplesTable } from '../sample/sub-samples.table';
import { SamplesTable } from '../sample/samples.table';
import { Operation, Trip } from '../services/model/trip.model';
import { ProgramProperties } from '@app/referential/services/config/program.config';
import { AcquisitionLevelCodes, AcquisitionLevelType, PmfmIds, QualitativeLabels } from '@app/referential/services/model/model.enum';
import { BatchTreeComponent } from '../batch/batch-tree.component';
import { environment } from '@environments/environment';
import { ProgramRefService } from '@app/referential/services/program-ref.service';
import { BehaviorSubject, Subject, Subscription } from 'rxjs';
import { Measurement, MeasurementUtils } from '@app/trip/services/model/measurement.model';

const moment = momentImported;

@Component({
  selector: 'app-operation-page',
  templateUrl: './operation.page.html',
  styleUrls: ['./operation.page.scss'],
  animations: [fadeInOutAnimation],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class OperationPage extends AppEntityEditor<Operation, OperationService> {

  private _lastOperationsTripId: number;
  private _measurementSubscription: Subscription;

  $acquisitionLevel = new BehaviorSubject<string>(AcquisitionLevelCodes.OPERATION);
  $ready = new BehaviorSubject(false);

  measurements: Measurement[];
  trip: Trip;
  $programLabel = new BehaviorSubject<string>(null);
  $program = new Subject<Program>();
  saveOptions: OperationSaveOptions = {};
  readonly dateTimePattern: string;

  $tripId = new BehaviorSubject<number>(null);
  $lastOperations = new BehaviorSubject<Operation[]>(null);

  rankOrder: number;
  selectedBatchTabIndex = 0;
  selectedSampleTabIndex = 0;

  // All second tabs components are disabled, by default
  // (waiting PMFM measurements to decide that to show)
  enableCatchTab = true;
  showSampleTables = false;
  showBatchTables = false;
  showBatchTablesByProgram = true;
  mobile: boolean;
  sampleAcquisitionLevel: AcquisitionLevelType = AcquisitionLevelCodes.SURVIVAL_TEST;

  @ViewChild('opeForm', {static: true}) opeForm: OperationForm;
  @ViewChild('measurementsForm', {static: true}) measurementsForm: MeasurementsForm;

  // Catch batch, sorting batches, individual measure
  @ViewChild('batchTree', {static: true}) batchTree: BatchTreeComponent;

  // Sample tables
  @ViewChild('sampleTabGroup', {static: true}) sampleTabGroup: MatTabGroup;
  @ViewChild('samplesTable', {static: true}) samplesTable: SamplesTable;
  @ViewChild('individualMonitoringTable', {static: true}) individualMonitoringTable: IndividualMonitoringSubSamplesTable;
  @ViewChild('individualReleaseTable', {static: true}) individualReleaseTable: SubSamplesTable;

  get form(): FormGroup {
    return this.opeForm.form;
  }

  constructor(
    injector: Injector,
    dataService: OperationService,
    protected tripService: TripService,
    protected programRefService: ProgramRefService,
    protected platform: PlatformService
  ) {
    super(injector, Operation, dataService, {
      pathIdAttribute: 'operationId',
      tabCount: 2,
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
      this.$programLabel
        .pipe(
          filter(isNotNilOrBlank),
          distinctUntilChanged(),
          switchMap(programLabel => this.programRefService.watchByLabel(programLabel))
        )
        .subscribe(program => this.$program.next(program)));

    // Watch trip, to load last operations
    this.registerSubscription(
      this.$tripId
        .pipe(
          // Filter on tripId changes
          filter(tripId => isNotNil(tripId) && this._lastOperationsTripId !== tripId),
          // Load last operations
          switchMap(tripId => {
            this._lastOperationsTripId = tripId; // Remember new trip id

            // Update back href
            this.defaultBackHref = `/trips/${tripId}?tab=2`;
            this.markForCheck();

            return this.dataService.watchAll(
              0, 5,
              'startDateTime', 'desc',
              {tripId}, {
                withBatchTree: false,
                withSamples: false,
                computeRankOrder: false,
                fetchPolicy: 'cache-and-network',
                withTotal: true
              });
          }),
          map(res => res && res.data || [])
        )
        .subscribe(data => this.$lastOperations.next(data))
    );

  }

  ngAfterViewInit() {
    super.ngAfterViewInit();

    this.registerSubscription(
      this.form.get('physicalGear').valueChanges
        .pipe(
          // skip if loading
          filter(() => !this.loading)
        )
        .subscribe((res) => {
          const gearId = res && res.gear && res.gear.id || null;
          this.measurementsForm.gearId = gearId;
          this.batchTree.gearId = gearId;
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

    if (this.measurementsForm) {
      this.registerSubscription(
        this.measurementsForm.$pmfms
          .pipe(
            debounceTime(400),
            filter(isNotNil),
            mergeMap(_ => this.measurementsForm.ready())
          )
          .subscribe(_ => this.onMeasurementsFormReady())
      );
    }

    // Configure page, from Program's properties
    this.registerSubscription(
      this.$program.subscribe(program => this.setProgram(program))
    );

    // Manage tab group
    {
      const queryParams = this.route.snapshot.queryParams;
      const subTabIndex = queryParams['subtab'] && parseInt(queryParams['subtab']) || 0;
      this.selectedBatchTabIndex = subTabIndex;
      this.selectedSampleTabIndex = subTabIndex;
    }
  }

  protected async ready() {
    if (this.$ready.value === true) return;
    await firstTruePromise(this.$ready);
  }

  /**
   * Configure specific behavior
   */
  protected async onMeasurementsFormReady() {

    // Wait program to be loaded
    await this.ready();

    // DEBUG
    //console.debug('[operation-page] Measurement form is ready');

    // Clean existing subscription (e.g. when acquisition level change, this function can= be called many times)
    this._measurementSubscription?.unsubscribe();
    this._measurementSubscription = new Subscription();

    const formGroup = this.measurementsForm.form as FormGroup;
    let defaultTableStates = true;

    // If PMFM "Sampling type" exists (e.g. SUMARiS), then use to enable/disable some tables
    const samplingTypeControl = formGroup?.controls[PmfmIds.SURVIVAL_SAMPLING_TYPE];
    if (isNotNil(samplingTypeControl)) {
      defaultTableStates = false;
      this.enableCatchTab = this.batchTree.showCatchForm;
      this._measurementSubscription.add(
        samplingTypeControl.valueChanges
          .pipe(
            debounceTime(400),
            startWith<any, any>(samplingTypeControl.value),
            filter(ReferentialUtils.isNotEmpty),
            map(qv => qv.label),
            distinctUntilChanged()
          )
          .subscribe(qvLabel => {

            switch (qvLabel as string) {
              case QualitativeLabels.SURVIVAL_SAMPLING_TYPE.SURVIVAL:
                if (this.debug) console.debug('[operation] Enable survival test tables');
                this.enableCatchTab = true;
                this.showSampleTables = true;
                this.showBatchTables = false;
                break;
              case QualitativeLabels.SURVIVAL_SAMPLING_TYPE.CATCH_HAUL:
                if (this.debug) console.debug('[operation] Enable batch sampling tables');
                this.enableCatchTab = true;
                this.showSampleTables = false;
                this.showBatchTables = true;
                break;
              case QualitativeLabels.SURVIVAL_SAMPLING_TYPE.UNSAMPLED:
                if (this.debug) console.debug('[operation] Disable survival test and batch sampling tables');
                this.enableCatchTab = true;
                this.showSampleTables = false;
                this.showBatchTables = false;
            }

            // Force first tab index
            this.selectedBatchTabIndex = 0;
            this.selectedSampleTabIndex = 0;
            this.updateTablesState();
            this.markForCheck();
          })
      );
    }

    // If PMFM "Has accidental catches ?" exists, then use to enable/disable sample tables
    const hasAccidentalCatchesControl = formGroup?.controls[PmfmIds.HAS_ACCIDENTAL_CATCHES];
    if (isNotNil(hasAccidentalCatchesControl)) {
      defaultTableStates = true; // Applying defaults (because will not manage the catch
      hasAccidentalCatchesControl.setValidators(Validators.compose([Validators.required, SharedValidators.entity]));
      this._measurementSubscription.add(
        hasAccidentalCatchesControl.valueChanges
          .pipe(
            debounceTime(400),
            startWith<any, any>(hasAccidentalCatchesControl.value),
            filter(isNotNil),
            distinctUntilChanged()
          )
          .subscribe(hasAccidentalCatches => {

            if (this.debug) console.debug('[operation] Enable/Disable samples table, because HAS_ACCIDENTAL_CATCHES=' + hasAccidentalCatches);

            // Enable samples, when has accidental catches
            this.showSampleTables = hasAccidentalCatches;
            this.enableCatchTab = this.showBatchTables || this.showSampleTables || this.batchTree.showCatchForm;
            this.tabCount = this.enableCatchTab ? 2 : 1;

            // Force first tab index
            this.selectedBatchTabIndex = 0;
            this.selectedSampleTabIndex = 0;
            this.updateTablesState();
            this.markForCheck();
          })
      );
    }

    if (this.opeForm.allowParentOperation) {
      defaultTableStates = false;
      this._measurementSubscription.add(
        this.opeForm.onParentChanges
          .pipe(
            startWith<Operation>(this.opeForm.parentControl.value as Operation),
            map(parent => !!parent), // into boolean
            distinctUntilChanged()
          )
          .subscribe((hasParent) => {
            let acquisitionLevel: AcquisitionLevelType;
            if (hasParent) {
              if (this.debug) console.debug('[operation] Enable batch tables');
              this.showBatchTables = this.showBatchTablesByProgram;
              this.showSampleTables = false;
              this.enableCatchTab = this.showBatchTables || this.showSampleTables || this.batchTree.showCatchForm;
              this.tabCount = this.enableCatchTab ? 2 : 1;
              acquisitionLevel = AcquisitionLevelCodes.CHILD_OPERATION;
            } else {
              if (this.debug) console.debug('[operation] Disable batch tables');
              this.showBatchTables = false;
              this.showSampleTables = false;
              this.enableCatchTab = false;
              this.tabCount = 1;
              acquisitionLevel = AcquisitionLevelCodes.OPERATION;
            }

            // Change acquisition level, if need
            if (this.$acquisitionLevel.value !== acquisitionLevel) {
              this.measurementsForm.setAcquisitionLevel(acquisitionLevel, []/* force cleaning previous values*/);
              this.$acquisitionLevel.next(acquisitionLevel);
            }

            // Force first tab index
            this.selectedBatchTabIndex = 0;
            this.selectedSampleTabIndex = 0;
            this.updateTablesState();
            this.markForCheck();
          })
      );
    }

    const hasIndividualMeasuresControl = formGroup?.controls[PmfmIds.HAS_INDIVIDUAL_MEASURES];
    if (isNotNil(hasIndividualMeasuresControl)) {
      defaultTableStates = false;
      this._measurementSubscription.add(
        hasIndividualMeasuresControl.valueChanges
          .pipe(
            debounceTime(400),
            startWith<any, any>(hasIndividualMeasuresControl.value),
            filter(isNotNil),
            distinctUntilChanged()
          )
          .subscribe(hasIndividualMeasures => {
            this.batchTree.allowSamplingBatches = hasIndividualMeasures;
            this.batchTree.defaultHasSubBatches = hasIndividualMeasures;
            this.batchTree.allowSubBatches = hasIndividualMeasures;
          })
      );
    }

    // Show default tables
    if (defaultTableStates) {
      if (this.debug) console.debug('[operation] Enable default tables (Nor SUMARiS nor ADAP pmfms were found)');
      this.showBatchTables = this.showBatchTablesByProgram;
      this.showSampleTables = false;
      this.enableCatchTab = this.showBatchTables || this.showSampleTables || this.batchTree.showCatchForm;
      this.tabCount = 2;
      this.updateTablesState();
      this.markForCheck();
    }

    // Abnormal trip => Change comments as required
    const tripProgressControl = formGroup?.controls[PmfmIds.TRIP_PROGRESS];
    if (isNotNil(samplingTypeControl)) {
      this._measurementSubscription.add(
        tripProgressControl.valueChanges
          .pipe(
            debounceTime(400),
            startWith<any, any>(tripProgressControl.value),
            filter(isNotNilOrBlank),
            distinctUntilChanged()
          )
          .subscribe(normalProgress => {
            if (!normalProgress) console.debug('[operation] Abnormal OPE: comment is now required');
            this.opeForm.requiredComment = !normalProgress;
          })
      );
    }
  }

  ngOnDestroy() {
    super.ngOnDestroy();
    this._measurementSubscription?.unsubscribe();
    this.$lastOperations.complete();
    this.$program.complete();
    this.$programLabel.complete();
    this.$tripId.complete();
    this.$tripId.complete();
  }

  protected async setProgram(program: Program) {
    if (!program) return; // Skip
    if (this.debug) console.debug(`[operation] Program ${program.label} loaded, with properties: `, program.properties);

    if (this.opeForm.showPosition) {
      // Activate position controls only if showPosition has default value (=true)
      this.opeForm.showPosition = program.getPropertyAsBoolean(ProgramProperties.TRIP_POSITION_ENABLE);
    }
    this.opeForm.fishingAreaLocationLevelIds = program.getPropertyAsNumbers(ProgramProperties.TRIP_FISHING_AREA_LOCATION_LEVEL_IDS);
    this.opeForm.defaultLatitudeSign = program.getProperty(ProgramProperties.TRIP_LATITUDE_SIGN);
    this.opeForm.defaultLongitudeSign = program.getProperty(ProgramProperties.TRIP_LONGITUDE_SIGN);
    this.opeForm.maxDistanceWarning = program.getPropertyAsInt(ProgramProperties.TRIP_DISTANCE_MAX_WARNING);
    this.opeForm.maxDistanceError = program.getPropertyAsInt(ProgramProperties.TRIP_DISTANCE_MAX_ERROR);
    this.opeForm.allowParentOperation = program.getPropertyAsBoolean(ProgramProperties.TRIP_ALLOW_PARENT_OPERATION);
    this.opeForm.startProgram = program.creationDate;
    this.opeForm.showMetierFilter = program.getPropertyAsBoolean(ProgramProperties.TRIP_FILTER_METIER);
    this.opeForm.copyTripDates = program.getPropertyAsBoolean(ProgramProperties.TRIP_APPLY_DATE_ON_NEW_OPERATION);

    this.saveOptions.computeBatchRankOrder = program.getPropertyAsBoolean(ProgramProperties.TRIP_BATCH_MEASURE_RANK_ORDER_COMPUTE);
    this.saveOptions.computeBatchIndividualCount = program.getPropertyAsBoolean(ProgramProperties.TRIP_BATCH_INDIVIDUAL_COUNT_COMPUTE);
    this.saveOptions.withChildOperation = this.opeForm.allowParentOperation;

    this.showBatchTablesByProgram = program.getPropertyAsBoolean(ProgramProperties.TRIP_BATCH_ENABLE);

    this.batchTree.batchGroupsTable.setModalOption('maxVisibleButtons', program.getPropertyAsInt(ProgramProperties.MEASUREMENTS_MAX_VISIBLE_BUTTONS));

    const hasBatchMeasure = program.getPropertyAsBoolean(ProgramProperties.TRIP_BATCH_MEASURE_ENABLE);
    this.batchTree.allowSamplingBatches = hasBatchMeasure;
    this.batchTree.allowSubBatches = hasBatchMeasure;

    // Autofill batch group table (e.g. with taxon groups found in strategies)
    const autoFillBatch = program.getPropertyAsBoolean(ProgramProperties.TRIP_BATCH_AUTO_FILL);
    await this.setDefaultTaxonGroups(autoFillBatch);

    this.$ready.next(true);
  }

  load(id?: number, opts?: EntityServiceLoadOptions & { emitEvent?: boolean; openTabIndex?: number; updateTabAndRoute?: boolean; [p: string]: any }): Promise<void> {
    return super.load(id, {...opts, withLinkedOperation: true});
  }

  async onNewEntity(data: Operation, options?: EntityServiceLoadOptions): Promise<void> {
    const tripId = options && isNotNil(options.tripId) ? +(options.tripId) :
      isNotNil(this.trip && this.trip.id) ? this.trip.id : (data && data.tripId);
    if (isNil(tripId)) throw new Error('Missing argument \'options.tripId\'!');
    data.tripId = tripId;

    // Update trip id
    this.$tripId.next(+tripId);

    // Load parent trip
    const trip = await this.loadTrip(tripId);

    // Use the default gear, if only one
    if (trip && trip.gears && trip.gears.length === 1) {
      data.physicalGear = trip.gears[0];
    }

    // Copy some trip's properties (need by filter)
    data.programLabel = trip.program?.label;
    data.vesselId = trip.vesselSnapshot?.id;

    // If is on field mode, fill default values
    if (this.isOnFieldMode) {
      data.startDateTime = moment();

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

  }

  async onEntityLoaded(data: Operation, options?: EntityServiceLoadOptions): Promise<void> {
    const tripId = options && isNotNil(options.tripId) ? +(options.tripId) :
      isNotNil(this.trip && this.trip.id) ? this.trip.id : (data && data.tripId);
    if (isNil(tripId)) throw new Error('Missing argument \'options.tripId\'!');
    data.tripId = tripId;

    // Update trip id (will cause last operations to be watched, if need)
    this.$tripId.next(+tripId);

    const trip = await this.loadTrip(tripId);

    // Replace physical gear by the real entity
    data.physicalGear = (trip.gears || []).find(g => EntityUtils.equals(g, data.physicalGear, 'id')) || data.physicalGear;
    data.programLabel = trip.program?.label;
    data.vesselId = trip.vesselSnapshot?.id;

    try {
      // Load child operation (need by validator)
      const childOperationId = toNumber(data.childOperationId, data.childOperation?.id);
      if (isNotNil(childOperationId)) {
        data.childOperation = await this.dataService.load(childOperationId, {fetchPolicy: 'cache-first'});
      }

      // Load parent operation
      else {
        const parentOperationId = toNumber(data.parentOperationId, data.parentOperation?.id);
        if (isNotNil(parentOperationId)) {
          data.parentOperation = await this.dataService.load(parentOperationId, {fetchPolicy: 'cache-first'});

          // Force copy

        }
      }
    } catch (err) {
      console.error("Cannot load child/parent operation", err);
      data.childOperation = undefined;
      data.parentOperation = undefined;
    }
  }

  onNewFabButtonClick(event: UIEvent) {
    if (this.showBatchTables) {
      this.batchTree.addRow(event);
    } else if (this.showSampleTables) {
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
   * @param opts
   */
  protected async computeTitle(data: Operation, opts?: {
    withPrefix?: boolean;
  }): Promise<string> {

    // Trip exists
    const titlePrefix = (!opts || opts.withPrefix !== false) && this.trip && (await this.translate.get('TRIP.OPERATION.TITLE_PREFIX', {
      vessel: this.trip && this.trip.vesselSnapshot && (this.trip.vesselSnapshot.exteriorMarking || this.trip.vesselSnapshot.name) || '',
      departureDateTime: this.trip && this.trip.departureDateTime && this.dateFormat.transform(this.trip.departureDateTime) as string || ''
    }).toPromise()) || '';

    // new ope
    if (!data || isNil(data.id)) {
      return titlePrefix + (await this.translate.get('TRIP.OPERATION.NEW.TITLE').toPromise());
    }

    // Existing operation
    if (this.platform.mobile) {
      return titlePrefix + (await this.translate.get('TRIP.OPERATION.EDIT.TITLE_NO_RANK', {
        startDateTime: data.startDateTime && this.dateFormat.transform(data.startDateTime, {time: true}) as string
      }).toPromise()) as string;
    } else {
      return titlePrefix + (await this.translate.get('TRIP.OPERATION.EDIT.TITLE', {
        startDateTime: data.startDateTime && this.dateFormat.transform(data.startDateTime, {time: true}) as string,
        rankOrder: await this.service.computeRankOrder(data, {fetchPolicy: 'cache-first'})
      }).toPromise()) as string;
    }
  }

  protected async computePageHistory(title: string): Promise<HistoryPageReference> {
    return {
      ...(await super.computePageHistory(title)),
      icon: 'navigate'
    };
  }

  onTabChange(event: MatTabChangeEvent, queryParamName?: string): boolean {
    const changed = super.onTabChange(event, queryParamName);
    if (changed && this.selectedTabIndex === 1) {
      if (this.showBatchTables && this.batchTree) this.batchTree.realignInkBar();
      if (this.showSampleTables && this.sampleTabGroup) this.sampleTabGroup.realignInkBar();
      this.markForCheck();
    }
    return changed;
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

  async onLastOperationClick(event: UIEvent, id: number): Promise<any> {
    if (event && event.defaultPrevented) return; // Skip

    if (isNil(id) || this.data.id === id) return; // skip

    const savePromise: Promise<boolean> = this.isOnFieldMode && this.dirty && this.valid
      // If on field mode: try to save silently
      ? this.save(event)
      // If desktop mode: ask before save
      : this.saveIfDirtyAndConfirm(null, {
        emitEvent: false /*do not update view*/
      });
    const canContinue = await savePromise;

    if (canContinue) {
      return this.load(+id, {tripId: this.data.tripId, updateTabAndRoute: true});
    }
  }

  async onNewOperationClick(event: UIEvent): Promise<any> {
    if (event && event.defaultPrevented) return Promise.resolve(); // Skip
    if (event) event.preventDefault(); // Avoid propagation to <ion-item>

    const savePromise: Promise<boolean> = (this.isOnFieldMode && this.dirty && this.valid)
      // If on field mode AND valid: save silently
      ? this.save(event)
      // Else If desktop mode: ask before save
      : this.saveIfDirtyAndConfirm(null, {
        emitEvent: false /*do not update view*/
      });
    const canContinue = await savePromise;
    if (canContinue) {
      return this.load(undefined, {tripId: this.data.tripId, updateTabAndRoute: true});
    }
  }

  async setValue(data: Operation) {

    this.opeForm.value = data;

    // set parent trip
    if (this.trip) {
      this.saveOptions.trip = this.trip;
      this.opeForm.trip = this.trip;
    }

    const programLabel = data.programLabel || this.trip?.program && this.trip.program?.label;

    // Get gear, from the physical gear
    const gearId = data && data.physicalGear && data.physicalGear.gear && data.physicalGear.gear.id || null;

    // Set measurements form
    this.measurementsForm.gearId = gearId;
    this.measurementsForm.programLabel = programLabel;
    if (isNotNil(data.parentOperationId)) {
      await this.measurementsForm.setAcquisitionLevel(AcquisitionLevelCodes.CHILD_OPERATION, data && data.measurements || []);
      this.$acquisitionLevel.next(AcquisitionLevelCodes.CHILD_OPERATION);
    } else {
      this.measurementsForm.value = data && data.measurements || [];
    }

    // Set batch tree
    this.batchTree.gearId = gearId;
    this.batchTree.value = data && data.catchBatch || null;

    // Get all samples
    const samples = (data && data.samples || []).reduce((res, sample) => !sample.children ? res.concat(sample) : res.concat(sample).concat(sample.children), []);

    // Set root samples
    this.samplesTable.value = samples.filter(s => s.label && s.label.startsWith(this.samplesTable.acquisitionLevel + '#'));

    // Set sub-samples (individual monitoring)
    this.individualMonitoringTable.availableParents = this.samplesTable.value.filter(s => s.measurementValues && isNotNil(s.measurementValues[PmfmIds.TAG_ID]));
    this.individualMonitoringTable.value = samples.filter(s => s.label && s.label.startsWith(this.individualMonitoringTable.acquisitionLevel + '#'));

    // Set sub-samples (individual release)
    this.individualReleaseTable.availableParents = this.individualMonitoringTable.availableParents;
    this.individualReleaseTable.value = samples.filter(s => s.label && s.label.startsWith(this.individualReleaseTable.acquisitionLevel + '#'));

    // Applying program to tables (async)
    if (programLabel) this.$programLabel.next(programLabel);
  }

  isCurrentData(other: IEntity<any>): boolean {
    return (this.isNewData && isNil(other.id))
      || (this.data && this.data.id === other.id);
  }

  /* -- protected method -- */

  protected async loadTrip(tripId: number): Promise<Trip> {
    const trip = await this.tripService.load(tripId);
    this.trip = trip;
    this.saveOptions.trip = trip;

    // Trip has gps in use, so active positions controls else active fishing area control
    const isGPSUsed = toBoolean(MeasurementUtils.toMeasurementValues(trip.measurements)?.[PmfmIds.GPS_USED] === 1, true);
    this.opeForm.showPosition = isGPSUsed;
    this.opeForm.showFishingArea = !isGPSUsed;

    return trip;
  }

  /**
   * Open the first tab that is invalid
   */
  protected getFirstInvalidTabIndex(): number {
    // tab 0
    const tab0Invalid = this.opeForm.invalid || this.measurementsForm.invalid;
    // tab 1
    const batchTreeInvalidSubTab = this.batchTree.getFirstInvalidTabIndex();
    const subTab0Invalid = (batchTreeInvalidSubTab === 0) || (this.showSampleTables && this.samplesTable.invalid);
    const subTab1Invalid = (batchTreeInvalidSubTab === 1) || (this.showSampleTables && this.individualMonitoringTable.invalid);
    const subTab2Invalid = (batchTreeInvalidSubTab === 2) || (this.showSampleTables && this.individualReleaseTable.invalid);
    const tab1Invalid = subTab0Invalid || subTab1Invalid || subTab2Invalid;

    // Open the first invalid tab
    const invalidTabIndex = tab0Invalid ? 0 : (tab1Invalid ? 1 : -1);

    // If tab 1, open the invalid sub tab
    if (invalidTabIndex === 1 && this.enableCatchTab) {
      if (this.showBatchTables) {
        this.selectedBatchTabIndex = batchTreeInvalidSubTab;
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
      this.updateTablesState();
    }
    return invalidTabIndex;
  }

  protected computeUsageMode(operation: Operation): UsageMode {
    return this.settings.isUsageMode('FIELD') && (
      isNil(this.trip) || (
      isNotNil(this.trip.departureDateTime)
      && fromDateISOString(this.trip.departureDateTime).diff(moment(), 'day') < 15))
      ? 'FIELD' : 'DESK';
  }

  protected registerForms() {
    // Register sub forms & table
    this.addChildForms([
      this.opeForm,
      this.measurementsForm,
      this.samplesTable,
      this.individualMonitoringTable,
      this.individualReleaseTable,
      this.batchTree
    ]);
  }

  protected async waitWhilePending(): Promise<boolean> {
    this.form.updateValueAndValidity();
    return super.waitWhilePending();
  }

  protected async getValue(): Promise<Operation> {
    const data = await super.getValue();

    // Batches
    if (this.enableCatchTab) {
      await this.batchTree.save();

      // Get batch tree,rom the batch tree component
      data.catchBatch = this.batchTree.value;

      // Make sure to clean species groups, if not batch enable
      if (!this.showBatchTables) {
        data.catchBatch.children = undefined;
      }
    }
    else {
      data.catchBatch = undefined;
    }

    // Samples
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

    // Apply updates on child operation if it exists
    if (data.childOperation && (data.startDateTime !== data.childOperation.startDateTime || data.fishingStartDateTime !== data.childOperation.fishingStartDateTime)) {
      data.childOperation.startDateTime = data.startDateTime;
      data.childOperation.fishingStartDateTime = data.fishingStartDateTime;
      data.childOperation.parentOperationId = data.id;
      data.childOperation.parentOperation = data;

      this.saveOptions.withChildOperation = true;
    }
    else {
      this.saveOptions.withChildOperation = false;
    }

    return data;
  }

  protected getJsonValueToSave(): Promise<any> {
    const json = this.opeForm.value;
    json.measurements = this.measurementsForm.value;
    json.tripId = this.trip.id;
    return json;
  }

  async save(event, opts?: OperationSaveOptions): Promise<boolean> {
    // Force to pass specific saved options to dataService.save()
    return await super.save(event, <OperationSaveOptions>{
      ...this.saveOptions,
      ...opts
    });
  }

  async saveIfDirtyAndConfirm(event?: UIEvent, opts?: { emitEvent: boolean }): Promise<boolean> {
    return super.saveIfDirtyAndConfirm(event, {...this.saveOptions, ...opts});
  }

  protected canUserWrite(data: Operation): boolean {
    return !!data && this.trip && isNil(this.trip.validationDate)
      && this.tripService.canUserWrite(this.trip);
  }


  protected async setDefaultTaxonGroups(enable: boolean) {
    if (!enable) {
      // Reset table's default taxon groups
      this.batchTree.defaultTaxonGroups = null;
      return; // Skip
    }

    if (this.debug) console.debug('[operation] Check if can auto fill species...');
    let defaultTaxonGroups: string[];

    // Retrieve the trip measurements on SELF_SAMPLING_PROGRAM, if any
    const qvMeasurement = (this.trip.measurements || []).find(m => m.pmfmId === PmfmIds.SELF_SAMPLING_PROGRAM);
    if (qvMeasurement && ReferentialUtils.isNotEmpty(qvMeasurement.qualitativeValue)) {

      // Retrieve QV from the program pmfm (because measurement's QV has only the 'id' attribute)
      const tripPmfms = await this.programRefService.loadProgramPmfms(this.$programLabel.getValue(), {acquisitionLevel: AcquisitionLevelCodes.TRIP});
      const pmfm = (tripPmfms || []).find(pmfm => pmfm.id === PmfmIds.SELF_SAMPLING_PROGRAM);
      const qualitativeValue = (pmfm && pmfm.qualitativeValues || []).find(qv => qv.id === qvMeasurement.qualitativeValue.id);

      // Transform QV.label has a list of TaxonGroup.label
      if (qualitativeValue && qualitativeValue.label) {
        defaultTaxonGroups = qualitativeValue.label
          .split(/[^\w]+/) // Split by separator (= not a word)
          .filter(isNotNilOrBlank)
          .map(label => label.trim().toUpperCase());
      }
    } else {
      const taxonGroupRefs = await this.programRefService.loadTaxonGroups(this.$programLabel.getValue());
      defaultTaxonGroups = taxonGroupRefs.map(taxonGroup => taxonGroup.label);
    }

    // Set table's default taxon groups
    this.batchTree.defaultTaxonGroups = defaultTaxonGroups;

    // If new data, auto fill the table
    if (this.isNewData) {
      await this.batchTree.autoFill({defaultTaxonGroups, forceIfDisabled: true});
    }
  }

  protected updateTablesState() {
    if (this.enabled) {
      if (this.showSampleTables) {
        if (this.samplesTable.disabled) this.samplesTable.enable();
        if (this.individualMonitoringTable.disabled) this.individualMonitoringTable.enable();
        if (this.individualReleaseTable.disabled) this.individualReleaseTable.enable();
        if (this.sampleTabGroup) this.sampleTabGroup.realignInkBar();
      }
      else {
        this.selectedSampleTabIndex = 0;
      }
      if (this.enableCatchTab) {
        if (this.batchTree.disabled) this.batchTree.enable();
        if (this.showBatchTables) this.batchTree.realignInkBar();
      }
      else {
        this.selectedBatchTabIndex = 0;
      }
    }
    else {
      if (this.showSampleTables) {
        if (this.samplesTable.enabled) this.samplesTable.disable();
        if (this.individualMonitoringTable.enabled) this.individualMonitoringTable.disable();
        if (this.individualReleaseTable.enabled) this.individualReleaseTable.disable();
      }
      if (this.enableCatchTab && this.batchTree.enabled) {
        this.batchTree.disable();
      }
    }
  }

  protected computePageUrl(id: number | 'new'): string | any[] {
    const parentUrl = this.getParentPageUrl();
    return parentUrl && `${parentUrl}/operation/${id}`;
  }

  protected markForCheck() {
    this.cd.markForCheck();
  }

  markAsLoaded(opts?: { emitEvent?: boolean }) {
    super.markAsLoaded(opts);
    this.children?.forEach(c => c.markAsLoaded(opts));
  }

}
