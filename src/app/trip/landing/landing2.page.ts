import {ChangeDetectionStrategy, Component, Injector, OnInit, ViewChild} from '@angular/core';
import {FormGroup} from "@angular/forms";
import {MatTabGroup} from "@angular/material/tabs";
import * as moment from "moment";
import {Observable, Subscription} from "rxjs";
import {filter, throttleTime} from "rxjs/operators";
import {environment} from "../../../environments/environment";
import {AppEditorOptions} from "../../core/form/editor.class";
import {ReferentialUtils} from "../../core/services/model/referential.model";
import {UsageMode} from "../../core/services/model/settings.model";
import {PlatformService} from "../../core/services/platform.service";
import {AppRootDataEditor} from "../../data/form/root-data-editor.class";
import {ProgramProperties} from "../../referential/services/config/program.config";
import {PmfmIds} from "../../referential/services/model/model.enum";
import {PmfmStrategy} from "../../referential/services/model/pmfm-strategy.model";
import {Strategy} from "../../referential/services/model/strategy.model";
import {ReferentialRefService} from "../../referential/services/referential-ref.service";
import {StrategyService} from "../../referential/services/strategy.service";
import {VesselSnapshotService} from "../../referential/services/vessel-snapshot.service";
import {firstArrayValue, isNil, isNotEmptyArray, isNotNil, removeDuplicatesFromArray} from '../../shared/functions';
import {EntityServiceLoadOptions} from "../../shared/services/entity-service.class";
import {Samples2Table} from "../sample/samples2.table";
import {LandingService} from "../services/landing.service";
import {Landing} from "../services/model/landing.model";
import {MeasurementModelValues} from "../services/model/measurement.model";
import {ObservedLocation} from "../services/model/observed-location.model";
import {Sample} from "../services/model/sample.model";
import {Trip} from "../services/model/trip.model";
import {ObservedLocationService} from "../services/observed-location.service";
import {TripService} from "../services/trip.service";
import {Landing2Form} from "./landing2.form";
import {SampleValidatorService} from "../services/validator/sample.validator";
import {fromDateISOString} from "../../shared/dates";
import {Program} from "../../referential/services/model/program.model";


@Component({
  selector: 'app-landing2-page',
  templateUrl: './landing2.page.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    {
      provide: AppEditorOptions,
      useValue: {
        pathIdAttribute: 'landing2Id'
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

  @ViewChild('landing2Form', { static: true }) landingForm: Landing2Form;
  @ViewChild('samples2Table', { static: true }) samples2Table: Samples2Table;

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
      this.onProgramChanged.subscribe(program => this.setProgram(program))
    );

    // this.landing2Form.program = this.program.label;
    // Watch program, to configure tables from program properties


    // Use landing date as default dateTime for samples
    this.registerSubscription(
      this.landingForm.form.get('dateTime').valueChanges
        .pipe(throttleTime(200), filter(isNotNil))
        .subscribe((dateTime) => {
          this.samples2Table.defaultSampleDate = fromDateISOString(dateTime);
        })
    );

    this.registerSubscription(
      this.landingForm.form.get('sampleRowCode').valueChanges
        .pipe(throttleTime(200), filter(isNotNil))
        .subscribe((sampleRowCode: Strategy) => this.onSampleRowCodeChange(sampleRowCode))
    );

    this.samples2Table.onConfirmEditCreateRow.subscribe(() => {
      this.landingForm.hasSamples = true;
    });
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
        this.landingForm.showSampleRowCode = true;
        this.landingForm.showVessel = true;
        this.landingForm.showLocation = true;
        this.landingForm.showDateTime = false;
        this.landingForm.showFishingArea = true;
        this.landingForm.showTargetSpecies = true;
        this.landingForm.showComment = true;
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
    }
  }

  /* -- protected methods -- */



  protected async onSampleRowCodeChange(value: Strategy) {

    if (value && value.label) {
      const strategyLabel = value.label;

      if (strategyLabel !== this.landingForm.strategy) {
        this.landingForm.strategy = strategyLabel;

        this.landingForm.sampleRowCodeControl.patchValue(value);

        // TODO BLA: add opts  { programId }
        const strategy = await this.strategyService.loadRefByLabel(strategyLabel);
        // IMAGINE-201 : Existing bug in PMFM_STRATEGY storage => some duplicates exists
        const pmfmStrategies = strategy && removeDuplicatesFromArray(strategy.pmfmStrategies || [], 'id')
          .filter(pmfmStrategies => isNotNil(pmfmStrategies.pmfmId));

        // Refresh fishing areas from landing2Form according to selected sampleRowCode
        this.landingForm.appliedStrategies = strategy.appliedStrategies;
        // FIXME CLT : Obsolete - we use PmfmIds.SAMPLE_ROW_CODE to store specific sampleRowCode PMFM
        // this.landing2Form.pmfms = pmfmStrategies;

        let sampleRowCodeFound = false;
        const measurementValues = this.landingForm.value.measurementValues;
        if (measurementValues) {
          // update mode
          const measurementValuesAsKeyValues = Object.entries(measurementValues).map(([key, value]) => {
            return {
              key,
              value
            };
          });
          const newMeasurementValues: MeasurementModelValues = {};
          measurementValuesAsKeyValues.forEach((measurementValue) => {
            if (measurementValue.key === PmfmIds.SAMPLE_ROW_CODE.toString()) {
              newMeasurementValues[measurementValue.key] = strategy.label;
              sampleRowCodeFound = true;
            } else {
              newMeasurementValues[measurementValue.key] = measurementValue.value;
            }
          });
          // If there is no previous SAMPLE_ROW_CODE PMFM
          if (!sampleRowCodeFound && strategy)
          {
            newMeasurementValues[PmfmIds.SAMPLE_ROW_CODE.toString()] = strategy.label;
            sampleRowCodeFound = true;
          }
          Object.assign(measurementValues, newMeasurementValues);
        }
        if (strategy) {
          // Create mode
          // let target = {}
          // target[PmfmIds.SAMPLE_ROW_CODE.toString()] = sampleRowCode.label;
          // this.landing2Form.value.measurementValues = target;
          // sampleRowCodeFound = true;
          // this.landing2Form.value.measurementValues = this.landing2Form.value.measurementValues || target;
          // let sampleRowPmfmStrategy = new PmfmStrategy();
          // let sampleRowPmfm = new Pmfm();
          // sampleRowPmfm.id = PmfmIds.SAMPLE_ROW_CODE;
          // sampleRowPmfmStrategy.pmfm = sampleRowPmfm;
          // pmfmStrategies.push(sampleRowPmfmStrategy)
        }
        if (!sampleRowCodeFound) {
          this.landingForm.appliedStrategies = [];
          this.landingForm._defaultTaxonNameFromStrategy = null;
          Object.assign(this.landingForm.appliedStrategies, []);
        }
        this.landingForm.value.samples = [];
        this.landingForm.value.samples.length = 0;

        const landing2FormValueClone = this.landingForm.value.clone();
        landing2FormValueClone.samples = [];

        this.landingForm.setValue(landing2FormValueClone);


        const taxonNames = [];
        if (strategy.taxonNames && strategy.taxonNames[0]) {
          const defaultTaxonName = strategy.taxonNames[0];
          //propagation of taxonNames by strategy on sampleRowCode change
          this.landingForm.defaultTaxonNameFromStrategy = defaultTaxonName;
          this.samples2Table.defaultTaxonName = defaultTaxonName;

          if (this.landingForm._defaultTaxonNameFromStrategy) {
            const emptySampleWithTaxon = new Sample();
            emptySampleWithTaxon.taxonName = this.landingForm._defaultTaxonNameFromStrategy.taxonName;
            taxonNames.push(emptySampleWithTaxon);
          }
        } else {
          this.landingForm._defaultTaxonNameFromStrategy = null;
        }
        this.landingForm.value.samples = [];
        this.landingForm.taxonNamesForm.patchValue(taxonNames);

        // Refresh samples
        this.samples2Table.appliedPmfmStrategy = pmfmStrategies;
        this.samples2Table.pmfms = pmfmStrategies;
      }
    }
  }

  protected registerForms() {
    this.addChildForms([this.landingForm, this.samples2Table]);
  }

  protected setProgram(program: Program) {
    if (!program) return; // Skip
    if (this.debug) console.debug(`[landing] Program ${program.label} loaded, with properties: `, program.properties);

    // Customize the UI, using program options
    this.landingForm.locationLevelIds = program.getPropertyAsNumbers(ProgramProperties.OBSERVED_LOCATION_LOCATION_LEVEL_ID);

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

    // Load program
    if (parent.program && parent.program.label) {
      this.programSubject.next(parent.program.label);
    }

    return parent;
  }

  protected async setValue(data: Landing): Promise<void> {
    if (!data) return; // Skip

    const isNew = isNil(data.id);

    // Restrieve strategy
    const strategyLabel = Object.entries(data.measurementValues)
      .filter(([pmfmId, value]) => pmfmId === PmfmIds.SAMPLE_ROW_CODE.toString())
      .map(([pmfmId, value]) => value).find(isNotNil);


    this.landingForm.hasSamples = isNotEmptyArray(data.samples);
    this.landingForm.strategy = strategyLabel;
    this.landingForm.value = data;

    // Set table rows
    this.samples2Table.value = data.samples || [];

    // Use strategy defaults
    if (!isNew && isNotNil(strategyLabel)) {
      // Load strategy by label
      const strategy = await this.strategyService.loadRefByLabel(strategyLabel);

      // IMAGINE-201 : Existing bug in PMFM_STRATEGY storage => some duplicates exists
      const pmfmStrategies = strategy && removeDuplicatesFromArray(strategy.pmfmStrategies || [], 'id')
        .filter(pmfmStrategies => isNotNil(pmfmStrategies.pmfmId));

      this.landingForm.appliedStrategies = strategy.appliedStrategies;

      //propagation of taxonNames by strategy
      const defaultTaxonName = strategy && firstArrayValue(strategy.taxonNames);

      // TODO BLA: pourquoi 'FromStrategy' ?
      this.landingForm.defaultTaxonNameFromStrategy = defaultTaxonName;
      this.samples2Table.defaultTaxonName = defaultTaxonName;

      this.landingForm.pmfms = pmfmStrategies;
      this.samples2Table.appliedPmfmStrategy = pmfmStrategies;
      this.samples2Table.pmfms = pmfmStrategies;
    }
  }

  protected async computeTitle(data: Landing): Promise<string> {
    const titlePrefix = this.parent && this.parent instanceof ObservedLocation &&
      await this.translate.get('LANDING.TITLE_PREFIX', {
        location: (this.parent.location && (this.parent.location.name || this.parent.location.label)),
        date: this.parent.startDateTime && this.dateFormat.transform(this.parent.startDateTime) as string || ''
      }).toPromise() || '';

    // new data
    if (!data || isNil(data.id)) {
      return titlePrefix + (await this.translate.get('LANDING.NEW.SAMPLE_TITLE').toPromise());
    }

    // Existing data
    return titlePrefix + (await this.translate.get('LANDING.EDIT.TITLE', {
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
    if (this.samples2Table.dirty) {
      await this.samples2Table.save();
    }
    data.samples = this.samples2Table.value;

    // Apply rank Order
    // TODO BLA: pourquoi fixer la rankOrder à 1 ? Cela empêche de retrouver l'ordre de saisie
    data.samples.map(s => s.rankOrder = 1);

    return data;
  }


}
