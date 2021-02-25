import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnInit, Output} from '@angular/core';
import {Moment} from 'moment';
import {DateAdapter} from "@angular/material/core";
import {debounceTime, map, tap} from 'rxjs/operators';
import {AcquisitionLevelCodes, LocationLevelIds, PmfmIds} from '../../referential/services/model/model.enum';
import {LandingValidatorService} from "../services/validator/landing.validator";
import {PersonService} from "../../admin/services/person.service";
import {MeasurementValuesForm} from "../measurement/measurement-values.form.class";
import {MeasurementsValidatorService} from "../services/validator/measurement.validator";
import {FormArray, FormBuilder, FormControl, ValidationErrors, Validators} from "@angular/forms";
import {ModalController} from "@ionic/angular";
import {ReferentialRef, ReferentialUtils} from "../../core/services/model/referential.model";
import {Person, personToString, UserProfileLabels} from "../../core/services/model/person.model";
import {LocalSettingsService} from "../../core/services/local-settings.service";
import {VesselSnapshotService} from "../../referential/services/vessel-snapshot.service";
import {isNil, isNotNil, toBoolean} from "../../shared/functions";
import {Landing} from "../services/model/landing.model";
import {ReferentialRefService} from "../../referential/services/referential-ref.service";
import {StatusIds} from "../../core/services/model/model.enum";
import {VesselSnapshot} from "../../referential/services/model/vessel-snapshot.model";
import {VesselModal} from "../../referential/vessel/modal/modal-vessel";
import {FormArrayHelper} from "../../core/form/form.utils";
import {DenormalizedPmfmStrategy, PmfmStrategy} from "../../referential/services/model/pmfm-strategy.model";
import {SharedValidators} from "../../shared/validator/validators";
import {EntityUtils} from "../../core/services/model/entity.model";
import {ProgramRefService} from "../../referential/services/program-ref.service";
import {SamplingStrategyService} from "../../referential/services/sampling-strategy.service";
import {TranslateService} from "@ngx-translate/core";
import {IPmfm} from "../../referential/services/model/pmfm.model";
import {Observable} from "rxjs";

export const LANDING_DEFAULT_I18N_PREFIX = 'LANDING.EDIT.';

@Component({
  selector: 'app-landing-form',
  templateUrl: './landing.form.html',
  styleUrls: ['./landing.form.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class LandingForm extends MeasurementValuesForm<Landing> implements OnInit {

  private _showObservers: boolean;
  observersHelper: FormArrayHelper<Person>;
  observerFocusIndex = -1;
  mobile: boolean;
  strategyControl: FormControl;

  get empty(): any {
    const value = this.value;
    return ReferentialUtils.isEmpty(value.location)
      && (!value.dateTime)
      && (!value.comments || !value.comments.length);
  }

  get valid(): boolean {
    return this.form && (this.required ? this.form.valid : (this.form.valid || this.empty));
  }

  get value(): any {
    return this.getValue();
  }

  set value(value: any) {
    this.safeSetValue(value);
  }

  get observersForm(): FormArray {
    return this.form.controls.observers as FormArray;
  }

  @Input() i18nPrefix = LANDING_DEFAULT_I18N_PREFIX;
  @Input() canEditStrategy = true;
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

  }

  ngOnInit() {
    super.ngOnInit();

    // Default values
    this.showObservers = toBoolean(this.showObservers, true); // Will init the observers helper
    this.tabindex = isNotNil(this.tabindex) ? this.tabindex : 1;
    if (isNil(this.locationLevelIds)) {
      this.locationLevelIds = [LocationLevelIds.PORT];
    }

    // Combo: programs
    this.registerAutocompleteField('program', {
      service: this.referentialRefService,
      filter: {
        entityName: 'Program'
      }
    });

    // Combo: strategy
    this.registerAutocompleteField('strategy', {
      service: this.referentialRefService,
      filter: {
        entityName: 'Strategy',
        levelLabel: this.$programLabel.getValue() // is empty, will be set in setProgram()
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
      service: this.personService,
      filter: {
        statusIds: [StatusIds.TEMPORARY, StatusIds.ENABLE],
        userProfiles: [UserProfileLabels.SUPERVISOR, UserProfileLabels.USER, UserProfileLabels.GUEST]
      },
      attributes: ['lastName', 'firstName', 'department.name'],
      displayWith: personToString
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

    // Propagate the program
    if (data.program && data.program.label) {
      this.programLabel = data.program.label;
    }

    // Propagate the strategy
    const strategyLabel = Object.entries(data.measurementValues || {})
      .filter(([pmfmId, _]) => +pmfmId === PmfmIds.STRATEGY_LABEL)
      .map(([_, value]) => value)
      .find(isNotNil) as string;
    this.strategyControl.patchValue(ReferentialRef.fromObject({label: strategyLabel}));
    this.strategyLabel = strategyLabel;

    await super.safeSetValue(data, opts);
  }

  protected getValue(): Landing {
    const data = super.getValue();

    // Re add the strategy label
    if (this.showStrategy) {
      const strategyValue = this.strategyControl.value;
      const strategyLabel = EntityUtils.isNotEmpty(strategyValue, 'label') ? strategyValue.label : strategyValue as string;
      data.measurementValues = data.measurementValues || {};
      data.measurementValues[PmfmIds.STRATEGY_LABEL.toString()] = strategyLabel;
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
      if (res && res.data instanceof VesselSnapshot) {
        console.debug("[landing-form] New vessel added : updating form...", res.data);
        this.form.controls['vesselSnapshot'].setValue(res.data);
        this.markForCheck();
      }
      else {
        console.debug("[landing-form] No vessel added (user cancelled)");
      }
    });
    return modal.present();
  }

  /* -- protected method -- */

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

    if (this.showStrategy) {
      // Create the missing Pmfm, to hold strategy (if need)
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

  get invalid(): boolean {
    return super.invalid
      // Check strategy
      || (this.showStrategy && (this.strategyControl.invalid

      // TODO BLA: ne sert à rien, car strategyControl.invalid devrait suffir
      || (this.strategyControl.hasError('required') || this.strategyControl.hasError('noEffort'))));
  }
}
