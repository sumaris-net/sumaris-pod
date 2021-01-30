import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  Injector,
  OnInit,
  Optional,
  QueryList,
  ViewChild,
  ViewChildren
} from '@angular/core';
import {FormGroup} from "@angular/forms";
import * as moment from "moment";
import {merge, Subscription} from "rxjs";
import {debounceTime, filter, throttleTime} from "rxjs/operators";
import {environment} from "../../../environments/environment";
import {AppEditorOptions} from "../../core/form/editor.class";
import {ReferentialUtils} from "../../core/services/model/referential.model";
import {UsageMode} from "../../core/services/model/settings.model";
import {PlatformService} from "../../core/services/platform.service";
import {AppRootDataEditor} from "../../data/form/root-data-editor.class";
import {ProgramProperties} from "../../referential/services/config/program.config";
import {ReferentialRefService} from "../../referential/services/referential-ref.service";
import {VesselSnapshotService} from "../../referential/services/vessel-snapshot.service";
import {firstArrayValue, isEmptyArray, isNil, isNotEmptyArray, isNotNil} from '../../shared/functions';
import {EntityServiceLoadOptions} from "../../shared/services/entity-service.class";
import {Samples2Table} from "../sample/samples2.table";
import {LandingService} from "../services/landing.service";
import {Landing} from "../services/model/landing.model";
import {ObservedLocation} from "../services/model/observed-location.model";
import {Trip} from "../services/model/trip.model";
import {ObservedLocationService} from "../services/observed-location.service";
import {TripService} from "../services/trip.service";
import {SampleValidatorService} from "../services/validator/sample.validator";
import {fromDateISOString} from "../../shared/dates";
import {Program} from "../../referential/services/model/program.model";
import {firstNotNilPromise} from "../../shared/observables";
import {Strategy} from "../../referential/services/model/strategy.model";
import {
  STRATEGY_SUMMARY_DEFAULT_I18N_PREFIX,
  StrategySummaryCardComponent
} from "../../data/strategy/strategy-summary-card.component";
import {LandingForm} from "./landing.form";


const DEFAULT_I18N_PREFIX = 'LANDING.EDIT.';

@Component({
  selector: 'app-landing2-page',
  templateUrl: './landing2.page.html',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class Landing2Page extends AppRootDataEditor<Landing, LandingService> implements OnInit {

  protected parent: Trip | ObservedLocation;
  protected observedLocationService: ObservedLocationService;
  protected tripService: TripService;
  protected referentialRefService: ReferentialRefService;
  protected vesselService: VesselSnapshotService;
  protected platform: PlatformService;
  private _rowValidatorSubscription: Subscription;

  mobile: boolean;
  showQualityForm = false;
  i18nPrefix = DEFAULT_I18N_PREFIX;
  oneTabMode = false;

  get form(): FormGroup {
    return this.landingForm.form;
  }

  @ViewChild('landingForm', { static: true }) landingForm: LandingForm;
  @ViewChild('samplesTable', { static: true }) samplesTable: Samples2Table;
  @ViewChild('strategyCard', {static: true}) strategyCard: StrategySummaryCardComponent;

  @ViewChild('firstTabInjection', {static: false}) firstTabInjection: ElementRef;
  @ViewChildren('tabContent') tabContents: QueryList<ElementRef>;

  constructor(
    injector: Injector,
    @Optional() options: AppEditorOptions
  ) {
    super(injector, Landing, injector.get(LandingService), {
        tabCount: 2,
        pathIdAttribute: 'landingId',
        ...options
      });
    this.observedLocationService = injector.get(ObservedLocationService);
    this.tripService = injector.get(TripService);
    this.referentialRefService = injector.get(ReferentialRefService);
    this.vesselService = injector.get(VesselSnapshotService);
    this.platform = injector.get(PlatformService);

    this.mobile = this.platform.mobile;
    // FOR DEV ONLY ----
    this.debug = !environment.production;
  }

  ngAfterViewInit() {
    super.ngAfterViewInit();

    // Watch program, to configure tables from program properties
    this.registerSubscription(
      this.$program.subscribe(program => this.setProgram(program))
    );

    // Use landing date as default dateTime for samples
    this.registerSubscription(
      this.landingForm.form.get('dateTime').valueChanges
        .pipe(throttleTime(200), filter(isNotNil))
        .subscribe((dateTime) => {
          this.samplesTable.defaultSampleDate = fromDateISOString(dateTime);
        })
    );

    this.registerSubscription(
      this.landingForm.onStrategyChanged
        .subscribe((strategy: string) => this.strategySubject.next(strategy))
    );

    // Watch strategy
    this.registerSubscription(
      this.$strategy.subscribe(strategy => this.setStrategy(strategy))
    );

    // Watch table events, to avoid strategy edition, when has sample rows
    this.registerSubscription(
      merge(
        this.samplesTable.onConfirmEditCreateRow,
        this.samplesTable.onCancelOrDeleteRow
      )
        .pipe(debounceTime(500))
        .subscribe(() => {
          this.landingForm.canEditStrategy = this.samplesTable.resultsLength === 0;
        })
    );
  }

  protected registerForms() {
    this.addChildForms([this.landingForm, this.samplesTable]);
  }

  protected async onNewEntity(data: Landing, options?: EntityServiceLoadOptions): Promise<void> {

    if (this.isOnFieldMode) {
      data.dateTime = moment();
    }

    // Fill parent ids
    data.observedLocationId = options && options.observedLocationId && parseInt(options.observedLocationId);
    data.tripId = options && options.tripId && parseInt(options.tripId);

    // Load parent
    this.parent = await this.loadParent(data);

    // Copy from parent into the new object
    if (this.parent) {
      const queryParams = this.route.snapshot.queryParams;
      data.program = this.parent.program;
      data.observers = this.parent.observers;
      if (this.parent instanceof ObservedLocation) {
        data.location = this.parent.location;
        data.dateTime = this.parent.startDateTime || this.parent.endDateTime;
        data.tripId = undefined;

        // Load the vessel, if any
        if (isNotNil(queryParams['vessel'])) {
          const vesselId = +queryParams['vessel'];
          console.debug(`[landing-page] Loading vessel {${vesselId}}...`);
          data.vesselSnapshot = await this.vesselService.load(vesselId, {fetchPolicy: 'cache-first'});
        }

        // Define back link
        this.defaultBackHref = `/observations/${this.parent.id}?tab=1`;
      }
      else if (this.parent instanceof Trip) {
        data.vesselSnapshot = this.parent.vesselSnapshot;
        data.location = this.parent.returnLocation || this.parent.departureLocation;
        data.dateTime = this.parent.returnDateTime || this.parent.departureDateTime;
        data.observedLocationId = undefined;

        // Define back link
        this.defaultBackHref = `/trips/${this.parent.id}?tab=2`;
      }

      // Set rankOrder
      if (isNotNil(queryParams['rankOrder'])) {
        data.rankOrder = +queryParams['rankOrder'];
      }
      else {
        data.rankOrder = 1;
      }
    }

    // Landing as root
    else {
      this.showQualityForm = true;
    }

  }

  protected async onEntityLoaded(data: Landing, options?: EntityServiceLoadOptions): Promise<void> {

    this.parent = await this.loadParent(data);

    // Copy not fetched data
    if (this.parent) {
      // Set program using parent's program, if not already set
      data.program = ReferentialUtils.isNotEmpty(data.program) ? data.program : this.parent.program;
      data.observers = isNotEmptyArray(data.observers) && data.observers || this.parent.observers;

      if (this.parent instanceof ObservedLocation) {
        data.location = data.location || this.parent.location;
        data.dateTime = data.dateTime || this.parent.startDateTime || this.parent.endDateTime;
        data.tripId = undefined;

        // Define back link
        this.defaultBackHref = `/observations/${this.parent.id}?tab=1`;
      }
      else if (this.parent instanceof Trip) {
        data.vesselSnapshot = this.parent.vesselSnapshot;
        data.location = data.location || this.parent.returnLocation || this.parent.departureLocation;
        data.dateTime = data.dateTime || this.parent.returnDateTime || this.parent.departureDateTime;
        data.observedLocationId = undefined;

        this.defaultBackHref = `/trips/${this.parent.id}?tab=2`;
      }
    }
    // Landing as root
    else {
      this.showQualityForm = true;
    }
  }

  onStartSampleEditingForm({form, pmfms}) {
    // Remove previous subscription
    if (this._rowValidatorSubscription) {
      this._rowValidatorSubscription.unsubscribe();
    }

    // Add computation and validation
    this._rowValidatorSubscription = SampleValidatorService.addSampleValidators(form, pmfms, {markForCheck: () => this.markForCheck()});
  }

  updateView(data: Landing | null, opts?: {
    emitEvent?: boolean;
    openTabIndex?: number;
    updateRoute?: boolean;
  }) {
    super.updateView(data, opts);

    if (this.parent) {
      if (this.parent instanceof ObservedLocation) {
        this.landingForm.showProgram = false;
        this.landingForm.showVessel = true;

      } else if (this.parent instanceof Trip) {

        // Hide some fields
        this.landingForm.showProgram = false;
        this.landingForm.showVessel = false;

      }
    } else {

      this.landingForm.showVessel = true;
      this.landingForm.showLocation = true;
      this.landingForm.showDateTime = true;

      this.showQualityForm = true;
    }
  }

  protected setProgram(program: Program) {
    if (!program) return; // Skip
    if (this.debug) console.debug(`[landing] Program ${program.label} loaded, with properties: `, program.properties);

    // Customize the UI, using program options
    this.landingForm.locationLevelIds = program.getPropertyAsNumbers(ProgramProperties.OBSERVED_LOCATION_LOCATION_LEVEL_ID);
    this.landingForm.allowAddNewVessel = program.getPropertyAsBoolean(ProgramProperties.OBSERVED_LOCATION_CREATE_VESSEL_ENABLE);
    this.landingForm.showStrategy = program.getPropertyAsBoolean(ProgramProperties.LANDING_STRATEGY_ENABLE);
    this.landingForm.showObservers = program.getPropertyAsBoolean(ProgramProperties.LANDING_OBSERVERS_ENABLE);
    this.landingForm.showDateTime = program.getPropertyAsBoolean(ProgramProperties.LANDING_DATE_TIME_ENABLE);
    this.landingForm.showLocation = program.getPropertyAsBoolean(ProgramProperties.LANDING_LOCATION_ENABLE);

    /*this.samplesTable.modalOptions = {
      ...this.samplesTable.modalOptions,
      maxVisibleButtons: program.getPropertyAsInt(ProgramProperties.MEASUREMENTS_MAX_VISIBLE_BUTTONS)
    };*/

    const oneTabMode = program.getPropertyAsBoolean(ProgramProperties.LANDING_ONE_TAB_ENABLE);
    if (this.oneTabMode !== oneTabMode) {
      this.oneTabMode = oneTabMode;
      this.refreshTabLayout();
    }

    let i18nSuffix = program.getProperty(ProgramProperties.I18N_SUFFIX);
    i18nSuffix = (i18nSuffix && i18nSuffix !== 'legacy') ? i18nSuffix : '';
    this.i18nPrefix = DEFAULT_I18N_PREFIX + i18nSuffix;
    this.landingForm.i18nPrefix = this.i18nPrefix;
    this.strategyCard.i18nPrefix = STRATEGY_SUMMARY_DEFAULT_I18N_PREFIX + i18nSuffix;

  }

  protected async setStrategy(strategy: Strategy) {
    if (!strategy) return; // Skip if empty

    console.debug('[landing-page] Received strategy: ', strategy);

    this.strategyCard.value = strategy;

    // Set table defaults
    const taxonNameStrategy = firstArrayValue(strategy.taxonNames);
    this.samplesTable.defaultTaxonName = taxonNameStrategy && taxonNameStrategy.taxonName;
    this.samplesTable.pmfms = strategy.pmfmStrategies;
  }



  protected async loadParent(data: Landing): Promise<Trip | ObservedLocation> {
    let parent: Trip|ObservedLocation;

    // Load parent observed location
    if (isNotNil(data.observedLocationId)) {
      console.debug('[landing-page] Loading parent observed location...');
      parent = await this.observedLocationService.load(data.observedLocationId, {fetchPolicy: 'cache-first'});
    }
    // Load parent trip
    else if (isNotNil(data.tripId)) {
      console.debug('[landing-page] Loading parent trip...');
      parent = await this.tripService.load(data.tripId, {fetchPolicy: 'cache-first'});
    }
    else {
      throw new Error('No parent found in path. Landing without parent not implemented yet !');
    }

    // Emit program
    if (parent.program && parent.program.label) {
      this.programSubject.next(parent.program.label);
    }

    return parent;
  }

  protected async setValue(data: Landing): Promise<void> {
    if (!data) return; // Skip

    this.landingForm.canEditStrategy = isEmptyArray(data.samples);
    this.landingForm.value = data;

    // Set samples to table
    this.samplesTable.value = data.samples || [];
  }

  protected async computeTitle(data: Landing): Promise<string> {

    const program = await firstNotNilPromise(this.$program);
    let i18nSuffix = program.getProperty(ProgramProperties.I18N_SUFFIX);
    i18nSuffix = i18nSuffix !== 'legacy' && i18nSuffix || '';

    const titlePrefix = this.parent && this.parent instanceof ObservedLocation &&
      await this.translate.get('LANDING.EDIT.TITLE_PREFIX', {
        location: (this.parent.location && (this.parent.location.name || this.parent.location.label)),
        date: this.parent.startDateTime && this.dateFormat.transform(this.parent.startDateTime) as string || ''
      }).toPromise() || '';

    // new data
    if (!data || isNil(data.id)) {
      return titlePrefix + (await this.translate.get(`LANDING.NEW.${i18nSuffix}TITLE`).toPromise());
    }

    // Existing data
    return titlePrefix + (await this.translate.get(`LANDING.EDIT.${i18nSuffix}TITLE`, {
      vessel: data.vesselSnapshot && (data.vesselSnapshot.exteriorMarking || data.vesselSnapshot.name)
    }).toPromise());
  }

  protected computePageUrl(id: number|'new') {
    const parentUrl = this.getParentPageUrl();
    return `${parentUrl}/landing/${id}`;
  }

  protected getFirstInvalidTabIndex(): number {
    if (this.oneTabMode || this.landingForm.invalid) return 0;
    if (!this.oneTabMode && this.samplesTable.invalid) return 1;
    return -1;
  }

  protected computeUsageMode(landing: Landing): UsageMode {
    return this.settings.isUsageMode('FIELD')
      // Force desktop mode if landing date/time is 1 day later than now
      && (isNil(landing && landing.dateTime) || landing.dateTime.diff(moment(), "day") <= 1) ? 'FIELD' : 'DESK';
  }

  protected async getValue(): Promise<Landing> {
    const data = await super.getValue();

    // Save samples table
    if (this.samplesTable.dirty) {
      await this.samplesTable.save();
    }
    data.samples = this.samplesTable.value;

    // Apply rank Order
    // TODO BLA: pourquoi fixer la rankOrder à 1 ? Cela empêche de retrouver l'ordre de saisie
    //data.samples.map(s => s.rankOrder = 1);

    return data;
  }

  protected refreshTabLayout() {
    // Inject content of tabs, into the first tab
    const injectionPoint = this.oneTabMode && this.firstTabInjection && this.firstTabInjection.nativeElement;
    if (injectionPoint) {
      this.tabContents.forEach(content => {
        if (!content.nativeElement) return; // Skip
        injectionPoint.append(content.nativeElement);
      });
    }
  }

}
