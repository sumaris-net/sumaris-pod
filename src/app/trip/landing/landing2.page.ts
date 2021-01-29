import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  Injector,
  OnInit,
  QueryList,
  ViewChild,
  ViewChildren
} from '@angular/core';
import {FormGroup} from "@angular/forms";
import * as moment from "moment";
import {BehaviorSubject, merge, Observable, Subscription} from "rxjs";
import {distinctUntilChanged, filter, map, throttleTime} from "rxjs/operators";
import {environment} from "../../../environments/environment";
import {AppEditorOptions} from "../../core/form/editor.class";
import {ReferentialRef, ReferentialUtils} from "../../core/services/model/referential.model";
import {UsageMode} from "../../core/services/model/settings.model";
import {PlatformService} from "../../core/services/platform.service";
import {AppRootDataEditor} from "../../data/form/root-data-editor.class";
import {ProgramProperties} from "../../referential/services/config/program.config";
import {PmfmStrategy} from "../../referential/services/model/pmfm-strategy.model";
import {ReferentialRefService} from "../../referential/services/referential-ref.service";
import {StrategyService} from "../../referential/services/strategy.service";
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
import {Landing2Form} from "./landing2.form";
import {SampleValidatorService} from "../services/validator/sample.validator";
import {fromDateISOString} from "../../shared/dates";
import {Program} from "../../referential/services/model/program.model";
import {firstNotNilPromise} from "../../shared/observables";
import {Strategy} from "../../referential/services/model/strategy.model";
import {StrategySummaryCardComponent} from "../../data/strategy/strategy-summary-card.component";


const DEFAULT_I18N_PREFIX = 'LANDING.EDIT.';

@Component({
  selector: 'app-landing2-page',
  templateUrl: './landing2.page.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    {
      provide: AppEditorOptions,
      useValue: {
        pathIdAttribute: 'landingId'
      }
    }
  ]
})
export class Landing2Page extends AppRootDataEditor<Landing, LandingService> implements OnInit {

  protected parent: Trip | ObservedLocation;
  protected dataService: LandingService;
  protected observedLocationService: ObservedLocationService;
  protected tripService: TripService;
  protected referentialRefService: ReferentialRefService;
  protected vesselService: VesselSnapshotService;
  protected platform: PlatformService;
  protected strategyService: StrategyService;
  private _rowValidatorSubscription: Subscription;

  mobile: boolean;
  showQualityForm = false;
  i18nPrefix = DEFAULT_I18N_PREFIX;
  forceOneTab = false;

  @ViewChild('landingForm', { static: true }) landingForm: Landing2Form;
  @ViewChild('samplesTable', { static: true }) samplesTable: Samples2Table;

  @ViewChild('firstTabInjection', {static: false}) firstTabInjection: ElementRef;
  @ViewChildren('tabContent') tabContents: QueryList<ElementRef>;

  @ViewChild('strategyCard', {static: true}) strategyCard: StrategySummaryCardComponent;


  get pmfms(): Observable<PmfmStrategy[]> {
    return this.landingForm.$pmfms.pipe(filter(isNotNil));
  }

  get form(): FormGroup {
    return this.landingForm.form;
  }

  constructor(
    injector: Injector,
    options: AppEditorOptions
  ) {
    super(injector, Landing, injector.get(LandingService), options);
    this.observedLocationService = injector.get(ObservedLocationService);
    this.tripService = injector.get(TripService);
    this.referentialRefService = injector.get(ReferentialRefService);
    this.vesselService = injector.get(VesselSnapshotService);
    this.platform = injector.get(PlatformService);
    this.strategyService = injector.get(StrategyService);


    this.mobile = this.platform.mobile;
    // FOR DEV ONLY ----
    this.debug = !environment.production;
  }

  ngOnInit() {
    super.ngOnInit();

    this.registerSubscription(
      this.$program.subscribe(program => this.setProgram(program))
    );
    this.registerSubscription(
      this.$strategy.subscribe(strategy => this.setStrategy(strategy))
    );
    // this.landing2Form.program = this.program.label;
    // Watch program, to configure tables from program properties


    // Use landing date as default dateTime for samples
    this.registerSubscription(
      this.landingForm.form.get('dateTime').valueChanges
        .pipe(throttleTime(200), filter(isNotNil))
        .subscribe((dateTime) => {
          this.samplesTable.defaultSampleDate = fromDateISOString(dateTime);
        })
    );

    this.registerSubscription(
      this.landingForm.form.get('strategy').valueChanges
        .pipe(
          map(value => value && value.label ? value.label : value)
        )
        .subscribe((strategy: string) => this.strategySubject.next(strategy))
    );

    this.registerSubscription(
      merge(
        this.samplesTable.onConfirmEditCreateRow,
        this.samplesTable.onCancelOrDeleteRow
      )
      .subscribe(() => {
        this.landingForm.canEditStrategy = this.samplesTable.resultsLength === 0;
      })
    );
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
      data.program = this.parent.program;
      data.observers = this.parent.observers;
      if (this.parent instanceof ObservedLocation) {
        data.location = this.parent.location;
        data.dateTime = this.parent.startDateTime || this.parent.endDateTime;
        data.tripId = undefined;

        // Load the vessel, if any
        if (isNotNil(options.vessel)) {
          const vesselId = +options.vessel;
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
      if (isNotNil(options.rankOrder)) {
        data.rankOrder = +options.rankOrder;
      }
      else {
        data.rankOrder = 1;
      }
    }

  }
/*
  // TODO BLA: pourquoi cette méthode ? Pas besoin
  // Logiquement, l'éditor recharge la page après sauvegarde
  protected async onEntitySaved(data: Landing) {
    this.setValue(data);
  }*/

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
        this.landingForm.showLocation = true;
        this.landingForm.showDateTime = false;
        this.landingForm.showObservers = true;

      } else if (this.parent instanceof Trip) {

        // Hide some fields
        this.landingForm.showProgram = false;
        this.landingForm.showVessel = false;
        this.landingForm.showLocation = true;
        this.landingForm.showDateTime = true;
        this.landingForm.showObservers = true;
      }
    } else {

      this.landingForm.showVessel = true;
      this.landingForm.showLocation = true;
      this.landingForm.showDateTime = true;
      this.landingForm.showObservers = true;

      this.showQualityForm = true;
    }
  }

  /* -- protected methods -- */


  protected setProgram(program: Program) {
    if (!program) return; // Skip
    if (this.debug) console.debug(`[landing] Program ${program.label} loaded, with properties: `, program.properties);

    // Customize the UI, using program options
    this.landingForm.locationLevelIds = program.getPropertyAsNumbers(ProgramProperties.OBSERVED_LOCATION_LOCATION_LEVEL_ID);

    // TODO: use program properties ?
    this.landingForm.showStrategy = true;
    const forceOneTab = true; // Get it from properties ?
    if (this.forceOneTab !== forceOneTab) {
      this.forceOneTab = forceOneTab;
      this.moveTabContent();
    }

    this.i18nPrefix = DEFAULT_I18N_PREFIX;
    const i18nSuffix = program.getProperty(ProgramProperties.I18N_SUFFIX);
    this.i18nPrefix += (i18nSuffix && i18nSuffix !== 'legacy') ? i18nSuffix : '';
    this.landingForm.i18nPrefix = this.i18nPrefix;

    this.samplesTable.program = program.label;

  }

  protected async setStrategy(strategy: Strategy) {

    this.landingForm.strategy = strategy;

    //this.strategyCard.value = strategy;

    // Set table defaults
    this.samplesTable.defaultTaxonName = firstArrayValue(strategy.taxonNames);
    this.samplesTable.pmfms = strategy.pmfmStrategies;
  }

  protected registerForms() {
    this.addChildForms([this.landingForm, this.samplesTable]);
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
    if (this.samplesTable) {
      this.samplesTable.value = data.samples || [];
    }
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
  //  return this.landing2Form.invalid ? 0 : (this.samples2Table.invalid ? 1 : -1);
    // return first invalid tabIndex
    return 0;
  }

  protected computeUsageMode(landing: Landing): UsageMode {
    return this.settings.isUsageMode('FIELD')
    && isNotNil(landing && landing.dateTime)
    && landing.dateTime.diff(moment(), "day") <= 1 ? 'FIELD' : 'DESK';
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
    data.samples.map(s => s.rankOrder = 1);

    return data;
  }

  protected moveTabContent() {
    // Inject content of tabs, into the first tab
    const injectionPoint = this.forceOneTab && this.firstTabInjection && this.firstTabInjection.nativeElement;
    if (injectionPoint) {
      this.tabContents.forEach(content => {
        if (!content.nativeElement) return; // Skip
        injectionPoint.append(content.nativeElement);
      });
    }
  }

}
