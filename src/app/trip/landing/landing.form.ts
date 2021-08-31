import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnInit} from '@angular/core';
import {Moment} from 'moment';
import {DateAdapter} from '@angular/material/core';
import {debounceTime, map} from 'rxjs/operators';
import {AcquisitionLevelCodes, LocationLevelIds, PmfmIds} from '../../referential/services/model/model.enum';
import {LandingValidatorService} from '../services/validator/landing.validator';
import {MeasurementValuesForm} from '../measurement/measurement-values.form.class';
import {MeasurementsValidatorService} from '../services/validator/measurement.validator';
import {FormArray, FormBuilder, FormControl, Validators} from '@angular/forms';
import {ModalController} from '@ionic/angular';
import {
  ConfigService,
  EntityUtils,
  FormArrayHelper,
  isNil,
  isNotNil,
  isNotNilOrBlank,
  LoadResult,
  LocalSettingsService,
  Person,
  PersonService,
  PersonUtils,
  ReferentialRef,
  ReferentialUtils,
  StatusIds,
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
import {FishingArea} from '@app/trip/services/model/fishing-area.model';

export const LANDING_DEFAULT_I18N_PREFIX = 'LANDING.EDIT.';

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
  observerFocusIndex = -1;
  mobile: boolean;
  strategyControl: FormControl;
  mainMetierPmfmId: number;
  mainMetiers: ReferentialRef[];
  fishingAreas: FishingArea[];

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
    protected cd: ChangeDetectorRef
  ) {
    super(dateAdapter, measurementValidatorService, formBuilder, programRefService, settings, cd, validatorService.getFormGroup(), {
      mapPmfms: pmfms => this.mapPmfms(pmfms)
    });
    // Add a strategy field (not in validator)
    this.strategyControl = formBuilder.control(null, Validators.required);

    this._enable = false;
    this.mobile = this.settings.mobile;

    // Set default acquisition level
    this.acquisitionLevel = AcquisitionLevelCodes.LANDING;
    this.mainMetierPmfmId = PmfmIds.MAIN_METIER;
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
        value = typeof value === 'object' ? '*' : value;
        return this.referentialRefService.suggest(value, {
          entityName: 'Strategy',
          searchAttribute: 'label',
          levelLabel: this.$programLabel.getValue() // if empty, will be set in setProgram()
        }, 'label', 'asc',
          {
            fetchPolicy: 'network-only' // Force network - fix IMAGINE 302
          });
      },
      attributes: ['label'],
      columnSizes: [12],
      showAllOnFocus: false
    });

    // Combo: vessels
    const vesselField = this.registerAutocompleteField('vesselSnapshot', {
      service: this.vesselSnapshotService,
      attributes: this.settings.getFieldDisplayAttributes('vesselSnapshot', ['exteriorMarking', 'name']),
      filter: {
        statusIds: [StatusIds.ENABLE, StatusIds.TEMPORARY]
      }
    });
    // Add base port location
    vesselField.attributes = vesselField.attributes.concat(this.settings.getFieldDisplayAttributes('location').map(key => 'basePortLocation.' + key));

    // Combo location
    this.registerAutocompleteField('location', {
      service: this.referentialRefService,
      filter: {
        entityName: 'Location',
        levelIds: this.locationLevelIds
      }
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

    // Combo: observers
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

    // Load metier
    const metierId = data.measurementValues && data.measurementValues[PmfmIds.MAIN_METIER.toString()];
    if (isNotNilOrBlank(metierId) && isNumeric(metierId)) {
      const metierRef = await this.referentialRefService.loadById(+metierId, Metier.ENTITY_NAME);
      (data.measurementValues as any)[PmfmIds.MAIN_METIER.toString()] = metierRef.asObject();
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
      data.measurementValues = data.measurementValues || {};
      const metier = data.measurementValues[PmfmIds.MAIN_METIER.toString()] as any;
      if (ReferentialUtils.isNotEmpty(metier)) {
        data.measurementValues[PmfmIds.MAIN_METIER.toString()] = metier.id;
      }
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

  protected setProgramLabel(program: string) {
    super.setProgramLabel(program);

    // Update the strategy filter (if autocomplete field exists. If not, program will set later in ngOnInit())
    if (this.autocompleteFields.strategy) {
      this.autocompleteFields.strategy.filter.levelLabel = program;
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

    // Create the missing Pmfm, to hold metier (if need)
    if (this.showMetier) {
      const existingIndex = (pmfms || []).findIndex(pmfm => pmfm.id === PmfmIds.MAIN_METIER);
      let metierPmfm: IPmfm;
      if (existingIndex !== -1) {
        // Remove existing, then copy it (to leave original unchanged)
        metierPmfm = pmfms.splice(existingIndex, 1)[0].clone();
      }
      else {
        metierPmfm = DenormalizedPmfmStrategy.fromObject({
          id: PmfmIds.MAIN_METIER,
          name: this.translate.instant('TRIP.METIERS'),
          type: <PmfmType>'string'
        });
      }

      metierPmfm.hidden = false; // Always display in measurement
      metierPmfm.required = true; // Required

      // Prepend to list
      pmfms = [metierPmfm, ...pmfms];

    }

    return pmfms;
  }


}
