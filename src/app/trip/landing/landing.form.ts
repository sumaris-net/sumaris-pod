import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnInit} from '@angular/core';
import {Moment} from 'moment';
import {DateAdapter} from '@angular/material/core';
import {debounceTime, map} from 'rxjs/operators';
import {AcquisitionLevelCodes, LocationLevelIds, PmfmIds} from '@app/referential/services/model/model.enum';
import {LandingValidatorService} from '../services/validator/landing.validator';
import {MeasurementValuesForm} from '../measurement/measurement-values.form.class';
import {MeasurementsValidatorService} from '../services/validator/measurement.validator';
import {FormArray, FormBuilder, FormControl, Validators} from '@angular/forms';
import {ModalController} from '@ionic/angular';
import {
  ConfigService,
  EntityUtils,
  FormArrayHelper,
  IReferentialRef,
  isNil,
  isNotNil,
  isNotNilOrBlank,
  LoadResult,
  LocalSettingsService,
  MatAutocompleteField,
  Person,
  PersonService,
  PersonUtils,
  ReferentialRef,
  ReferentialUtils,
  StatusIds,
  suggestFromArray,
  toBoolean,
  UserProfileLabel
} from '@sumaris-net/ngx-components';
import {VesselSnapshotService} from '@app/referential/services/vessel-snapshot.service';
import {Landing} from '../services/model/landing.model';
import {ReferentialRefService} from '@app/referential/services/referential-ref.service';
import {VesselSnapshot} from '@app/referential/services/model/vessel-snapshot.model';
import {VesselModal} from '@app/vessel/modal/vessel-modal';
import {DenormalizedPmfmStrategy} from '@app/referential/services/model/pmfm-strategy.model';
import {ProgramRefService} from '@app/referential/services/program-ref.service';
import {SamplingStrategyService} from '@app/referential/services/sampling-strategy.service';
import {TranslateService} from '@ngx-translate/core';
import {IPmfm, PmfmType} from '@app/referential/services/model/pmfm.model';
import {ReferentialRefFilter} from '@app/referential/services/filter/referential-ref.filter';
import {Metier} from '@app/referential/services/model/taxon.model';
import {isNumeric} from 'rxjs/internal/util/isNumeric';
import {AppRootDataEditor} from '@app/data/form/root-data-editor.class';
import {BehaviorSubject} from 'rxjs';
import {Program} from '@app/referential/services/model/program.model';
import {FishingArea} from '@app/trip/services/model/fishing-area.model';
import {FishingAreaValidatorService} from '@app/trip/services/validator/fishing-area.validator';
import {LandingService} from '@app/trip/services/landing.service';
import {Trip} from '@app/trip/services/model/trip.model';

export const LANDING_DEFAULT_I18N_PREFIX = 'LANDING.EDIT.';

type FilterableFieldName = 'fishingArea';

@Component({
  selector: 'app-landing-form',
  templateUrl: './landing.form.html',
  styleUrls: ['./landing.form.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class LandingForm extends MeasurementValuesForm<Landing> implements OnInit {

  private _showObservers: boolean;
  private _canEditStrategy: boolean;

  observersHelper: FormArrayHelper<Person>;
  fishingAreasHelper: FormArrayHelper<FishingArea>;
  observerFocusIndex = -1;
  mobile: boolean;
  strategyControl: FormControl;

  autocompleteFilters = {
    fishingArea: false
  };
  get empty(): any {
    const value = this.value;
    return ReferentialUtils.isEmpty(value.location)
      && (!value.dateTime)
      && (!value.comments || !value.comments.length);
  }

  get valid(): boolean {
    return this.form && (this.required ? this.form.valid : (this.form.valid || this.empty))
      && (!this.showStrategy || this.strategyControl.valid);
  }

  get invalid(): boolean {
    return super.invalid
      // Check strategy
      || (this.showStrategy && this.strategyControl.invalid);
  }

  get pending(): boolean {
    return super.pending
      // Check strategy
      || (this.showStrategy && this.strategyControl.pending);
  }

  get dirty(): boolean {
    return super.dirty
      // Check strategy
      || (this.showStrategy && this.strategyControl.dirty);
  }

  markAsUntouched(opts?: { onlySelf?: boolean }) {
    super.markAsUntouched(opts);
    this.strategyControl.markAsUntouched(opts);
  }

  markAsTouched(opts?: { onlySelf?: boolean; emitEvent?: boolean }) {
    super.markAsTouched(opts);
    this.strategyControl.markAsTouched(opts);
  }

  get observersForm(): FormArray {
    return this.form.controls.observers as FormArray;
  }

  get fishingAreasForm(): FormArray {
    return this.form.controls.fishingAreas as FormArray;
  }

  @Input() i18nPrefix = LANDING_DEFAULT_I18N_PREFIX;
  @Input() required = true;
  @Input() showProgram = true;
  @Input() showVessel = true;
  @Input() showDateTime = true;
  @Input() showLocation = true;
  @Input() showComment = true;
  @Input() showMeasurements = true;
  @Input() showError = true;
  @Input() showButtons = true;
  @Input() showStrategy = false;
  @Input() locationLevelIds: number[];
  @Input() allowAddNewVessel: boolean;
  @Input() showMetier = false;
  @Input() appliedStrategyLocations: ReferentialRef[] = null;

  @Input() set canEditStrategy(value: boolean) {
    if (this._canEditStrategy !== value) {
      this._canEditStrategy = value;
      if (this._canEditStrategy && this.strategyControl.disabled) {
        this.strategyControl.enable();
      }
      else if (!this._canEditStrategy && this.strategyControl.enabled) {
        this.strategyControl.disable();
      }
    }
  }

  get canEditStrategy(): boolean {
    return this._canEditStrategy;
  }

  @Input() set showObservers(value: boolean) {
    if (this._showObservers !== value) {
      this._showObservers = value;
      this.initObserversHelper();
      this.markForCheck();
    }
  }

  get showObservers(): boolean {
    return this._showObservers;
  }

  constructor(
    protected dateAdapter: DateAdapter<Moment>,
    protected measurementValidatorService: MeasurementsValidatorService,
    protected formBuilder: FormBuilder,
    protected programRefService: ProgramRefService,
    protected validatorService: LandingValidatorService,
    protected referentialRefService: ReferentialRefService,
    protected personService: PersonService,
    protected vesselSnapshotService: VesselSnapshotService,
    protected settings: LocalSettingsService,
    protected samplingStrategyService: SamplingStrategyService,
    protected configService: ConfigService,
    protected translate: TranslateService,
    protected modalCtrl: ModalController,
    protected fishingAreaValidatorService: FishingAreaValidatorService,
    protected cd: ChangeDetectorRef
  ) {
    super(dateAdapter, measurementValidatorService, formBuilder, programRefService, settings, cd, validatorService.getFormGroup(), {
      mapPmfms: pmfms => this.mapPmfms(pmfms)
    });

    this._enable = false;
    this.mobile = this.settings.mobile;

    // Add some missing controls
    // Add a strategy field (not in validator)
    this.strategyControl = formBuilder.control(null, Validators.required);
    //this.form.addControl('strategy', this.strategyControl);
    this.form.addControl('metier', new FormControl());

    // Set default acquisition level
    this.acquisitionLevel = AcquisitionLevelCodes.LANDING;
  }

  ngOnInit() {
    super.ngOnInit();

    // Default values
    this.showObservers = toBoolean(this.showObservers, true); // Will init the observers helper
    this.tabindex = isNotNil(this.tabindex) ? this.tabindex : 1;
    if (isNil(this.locationLevelIds) && this.showLocation) {
      this.locationLevelIds = [LocationLevelIds.PORT];
      console.debug("[landing-form] Location level ids:", this.locationLevelIds);
    }

    // Combo: programs
    const programAttributes = this.settings.getFieldDisplayAttributes('program');
    this.registerAutocompleteField('program', {
      service: this.referentialRefService,
      attributes: programAttributes,
      // Increase default column size, for 'label'
      columnSizes: programAttributes.map(a => a === 'label' ? 4 : undefined/*auto*/),
      filter: <ReferentialRefFilter>{
        entityName: 'Program'
      },
      mobile: this.mobile
    });

    // Combo: strategy
    this.registerAutocompleteField('strategy', {
      suggestFn: (value, filter) => {
        // Force to show all
        value = (typeof value === 'object') ? '*' : value;
        return this.referentialRefService.suggest(value, filter, 'label', 'asc',
          {
            fetchPolicy: 'network-only' // Force network - fix IMAGINE 302
          });
      },
      filter: {
        entityName: 'Strategy',
        searchAttribute: 'label',
        levelLabel: this.$programLabel.value // if not loaded yet, will be set in setProgramLabel()
      },
      attributes: ['label'],
      columnSizes: [12],
      showAllOnFocus: false
    });

    // Combo: vessels
    this.registerAutocompleteField('vesselSnapshot', this.vesselSnapshotService.getAutocompleteAddOptions());

    // Combo location
    const locationAttributes = this.settings.getFieldDisplayAttributes('location');
    this.registerAutocompleteField('location', {
      service: this.referentialRefService,
      filter: {
        entityName: 'Location',
        levelIds: this.locationLevelIds
      },
      attributes: locationAttributes
    });

    // Combo: observers
    this.registerAutocompleteField('person', {
      // Important, to get the current (focused) control value, in suggestObservers() function (otherwise it will received '*').
      showAllOnFocus: false,
      suggestFn: (value, filter) => this.suggestObservers(value, filter),
      // Default filter. An excludedIds will be add dynamically
      filter: {
        statusIds: [StatusIds.TEMPORARY, StatusIds.ENABLE],
        userProfiles: <UserProfileLabel[]>['SUPERVISOR', 'USER', 'GUEST']
      },
      attributes: ['lastName', 'firstName', 'department.name'],
      displayWith: PersonUtils.personToString
    });

    // Combo: metier
    const metierAttributes = this.settings.getFieldDisplayAttributes('qualitativeValue');
    this.registerAutocompleteField('metier', {
      showAllOnFocus: false,
      suggestFn: (value, filter) => this.referentialRefService.suggest(value, filter),
      // Default filter. An excludedIds will be add dynamically
      filter: {
        entityName: 'Metier',
        statusIds: [StatusIds.TEMPORARY, StatusIds.ENABLE]
      },
      attributes: metierAttributes
    });

    // Combo: fishingAreas
    this.registerAutocompleteField('fishingAreas', {
      showAllOnFocus: false,
      suggestFn: (value, filter) => this.suggestFishingAreas(value, filter),
      // Default filter. An excludedIds will be add dynamically
      filter: {
        statusIds: [StatusIds.TEMPORARY, StatusIds.ENABLE],
        userProfiles: <UserProfileLabel[]>['SUPERVISOR', 'USER', 'GUEST'],
        levelIds: LocationLevelIds.LOCATIONS_AREA
      },
      attributes: locationAttributes
    });


    // Propagate program
    this.registerSubscription(
      this.form.get('program').valueChanges
        .pipe(
          debounceTime(250),
          map(value => (value && typeof value === 'string') ? value : (value && value.label || undefined))
        )
        .subscribe(programLabel => this.programLabel = programLabel));

    // Propagate strategy changes
    this.registerSubscription(
      this.strategyControl.valueChanges
        .pipe(
          debounceTime(250),
          map(value => (value && typeof value === 'string') ? value : (value && value.label || undefined)),
          // DEBUG
          //tap(strategyLabel => console.debug('[landing-form] Sending strategy label: ' + strategyLabel))
        )
        .subscribe( async (strategyLabel) => {
          this.strategyLabel = strategyLabel;

          // Propagate to measurement values

          // Wait while pmfms are loading
          // Wait form controls ready, if need
          if (!this._ready) await this.ready();
          const measControl = this.form.get('measurementValues.' + PmfmIds.STRATEGY_LABEL);
          if (measControl && measControl.value !== strategyLabel) {
            measControl.setValue(strategyLabel);
          }
        }));

    this.initFishingAreas();
  }

  toggleFilter(fieldName: FilterableFieldName, field?: MatAutocompleteField) {
    this.autocompleteFilters[fieldName] = !this.autocompleteFilters[fieldName];
    this.markForCheck();

    if (field) field.reloadItems();
  }

  async safeSetValue(data: Landing, opts?: { emitEvent?: boolean; onlySelf?: boolean; normalizeEntityToForm?: boolean; [p: string]: any }) {
    if (!data) return;

    // Make sure to have (at least) one observer
    data.observers = data.observers && data.observers.length ? data.observers : [null];

    // Resize observers array
    if (this._showObservers) {
      this.observersHelper.resize(Math.max(1, data.observers.length));
    } else {
      this.observersHelper.removeAllEmpty();
    }

    // Propagate the strategy
    const strategyLabel = data.measurementValues && data.measurementValues[PmfmIds.STRATEGY_LABEL.toString()];
    this.strategyControl.patchValue(ReferentialRef.fromObject({label: strategyLabel}));
    this.strategyLabel = strategyLabel;

    await super.safeSetValue(data, opts);
  }

  protected getValue(): Landing {
    console.debug('[landing-form] DEV get value');
    const data = super.getValue();

    // Re add the strategy label
    if (this.showStrategy) {
      const strategyValue = this.strategyControl.value;
      const strategyLabel = EntityUtils.isNotEmpty(strategyValue, 'label') ? strategyValue.label : strategyValue as string;
      data.measurementValues = data.measurementValues || {};
      data.measurementValues[PmfmIds.STRATEGY_LABEL.toString()] = strategyLabel;
    }

    if (this.showMetier) {
      data.trip = Trip.fromObject(<Trip>{
        program: data.program,
        vesselSnapshot: data.vesselSnapshot,
        departureDateTime: data.dateTime,
        returnDateTime: data.dateTime,
        departureLocation: data.location,
        returnLocation: data.location,
        observedLocationId: data.observedLocationId,
        metiers: [this.form.get("metier")?.value],
        fishingAreas: (this.form.get('fishingAreas').value || [])
      });
    }

    // DEBUG
    console.debug('[landing-form] DEV Get getValue() result:', data);

    return data;
  }

  addObserver() {
    this.observersHelper.add();
    if (!this.mobile) {
      this.observerFocusIndex = this.observersHelper.size() - 1;
    }
  }

  addFishingArea() {
    this.fishingAreasHelper.add();
  }

  enable(opts?: {
    onlySelf?: boolean;
    emitEvent?: boolean;
  }): void {
    super.enable(opts);

    // Leave program disable once data has been saved
    const isNew = !this.data || isNil(this.data.id);
    if (!isNew && !this.form.controls['program'].disabled) {
      this.form.controls['program'].disable({emitEvent: false});
      this.markForCheck();
    }

    // TODO BLA: same for strategy
  }

  async addVesselModal(): Promise<any> {
    const modal = await this.modalCtrl.create({ component: VesselModal });
    modal.onDidDismiss().then(res => {
      // if new vessel added, use it
      if (res &&  res.data instanceof VesselSnapshot) {
        console.debug("[landing-form] New vessel added : updating form...", res.data);
        this.form.get('vesselSnapshot').setValue(res.data);
        this.markForCheck();
      }
      else {
        console.debug("[landing-form] No vessel added (user cancelled)");
      }
    });
    return modal.present();
  }

  notHiddenPmfm(pmfm: IPmfm): boolean{
    return pmfm && pmfm.hidden !== true;
  }

  /* -- protected method -- */

  protected suggestObservers(value: any, filter?: any): Promise<LoadResult<Person>> {
    const currentControlValue = ReferentialUtils.isNotEmpty(value) ? value : null;
    const newValue = currentControlValue ? '*' : value;

    // Excluded existing observers, BUT keep the current control value
    const excludedIds = (this.observersForm.value || [])
      .filter(ReferentialUtils.isNotEmpty)
      .filter(person => !currentControlValue || currentControlValue !== person)
      .map(person => parseInt(person.id));

    return this.personService.suggest(newValue, {
      ...filter,
      excludedIds
    });
  }
  protected async suggestFishingAreas(value: string, filter: any): Promise<LoadResult<IReferentialRef>> {
    if (this.autocompleteFilters.fishingArea) {
      return suggestFromArray(this.appliedStrategyLocations, value, filter);
    } else {
      return this.referentialRefService.suggest(value, {
        ...filter,
        entityName: 'Location'
      });
    }
  }

  protected setProgramLabel(programLabel: string) {
    super.setProgramLabel(programLabel);

    // Update the strategy filter (if autocomplete field exists. If not, program will set later in ngOnInit())
    if (this.autocompleteFields.strategy) {
      this.autocompleteFields.strategy.filter.levelLabel = programLabel;
    }
  }

  protected initObserversHelper() {
    if (isNil(this._showObservers)) return; // skip if not loading yet
    this.observersHelper = new FormArrayHelper<Person>(
      FormArrayHelper.getOrCreateArray(this.formBuilder, this.form, 'observers'),
      (person) => this.validatorService.getObserverControl(person),
      ReferentialUtils.equals,
      ReferentialUtils.isEmpty,
      {allowEmptyArray: !this._showObservers}
    );

    if (this._showObservers) {
      // Create at least one observer
      if (this.observersHelper.size() === 0) {
        this.observersHelper.resize(1);
      }
    }
    else if (this.observersHelper.size() > 0) {
      this.observersHelper.resize(0);
    }
  }

  protected initFishingAreas() {
    this.fishingAreasHelper = new FormArrayHelper<FishingArea>(
      FormArrayHelper.getOrCreateArray(this.formBuilder, this.form, 'fishingAreas'),
      (fishingArea) => this.fishingAreaValidatorService.getFormGroup(fishingArea, {required: true}),
      (o1, o2) => isNil(o1) && isNil(o2) || (o1 && o1.equals(o2)),
      (fishingArea) => !fishingArea || ReferentialUtils.isEmpty(fishingArea.location),
    {allowEmptyArray: false}
    );
    if (this.fishingAreasHelper.size() === 0) {
      this.fishingAreasHelper.resize(1);
    }
  }

  protected markForCheck() {
    this.cd.markForCheck();
  }

  /**
   * Make sure a pmfmStrategy exists to store the Strategy.label
   */
  protected async mapPmfms(pmfms: IPmfm[]): Promise<IPmfm[]> {

    if (this.debug) console.debug(`${this.logPrefix} calling mapPmfms()`);

    // Create the missing Pmfm, to hold strategy (if need)
    if (this.showStrategy) {
      const existingIndex = (pmfms || []).findIndex(pmfm => pmfm.id === PmfmIds.STRATEGY_LABEL);
      let strategyPmfm: IPmfm;
      if (existingIndex !== -1) {
        // Remove existing, then copy it (to leave original unchanged)
        strategyPmfm = pmfms.splice(existingIndex, 1)[0].clone();
      }
      else {
        strategyPmfm = DenormalizedPmfmStrategy.fromObject({
          id: PmfmIds.STRATEGY_LABEL,
          type: 'string'
        });
      }

      strategyPmfm.hidden = true; // Do not display it in measurement
      strategyPmfm.required = false; // Not need to be required, because of strategyControl validator

      // Prepend to list
      pmfms = [strategyPmfm, ...pmfms];
    }

    return pmfms;
  }

}
