import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnInit} from '@angular/core';
import {Moment} from 'moment/moment';

import {DateAdapter} from "@angular/material/core";
import {debounceTime, distinctUntilChanged, filter, map, pluck, tap} from 'rxjs/operators';
import {AcquisitionLevelCodes, LocationLevelIds, PmfmIds} from '../../referential/services/model/model.enum';
import {Landing2ValidatorService} from "../services/validator/landing2.validator";
import {PersonService} from "../../admin/services/person.service";
import {MeasurementValuesForm} from "../measurement/measurement-values.form.class";
import {MeasurementsValidatorService} from "../services/validator/measurement.validator";
import {FormArray, FormBuilder, FormControl, Validators} from "@angular/forms";
import {ModalController} from "@ionic/angular";
import {IReferentialRef, ReferentialRef, ReferentialUtils} from "../../core/services/model/referential.model";
import {Person, personToString, UserProfileLabels} from "../../core/services/model/person.model";
import {LocalSettingsService} from "../../core/services/local-settings.service";
import {VesselSnapshotService} from "../../referential/services/vessel-snapshot.service";
import {isNil, isNotNil, toBoolean} from "../../shared/functions";
import {Landing} from "../services/model/landing.model";
import {ReferentialRefService} from "../../referential/services/referential-ref.service";
import {ProgramService} from "../../referential/services/program.service";
import {StatusIds} from "../../core/services/model/model.enum";
import {VesselSnapshot} from "../../referential/services/model/vessel-snapshot.model";
import {VesselModal} from "../../referential/vessel/modal/modal-vessel";
import {Strategy} from "../../referential/services/model/strategy.model";
import {StrategyService} from "../../referential/services/strategy.service";
import {PmfmStrategy} from "../../referential/services/model/pmfm-strategy.model";
import {Pmfm} from "../../referential/services/model/pmfm.model";
import {SharedValidators} from "../../shared/validator/validators";
import {TranslateService} from "@ngx-translate/core";
import {FormArrayHelper} from "../../core/form/form.utils";
import {
  MatAutocompleteFieldAddOptions,
  MatAutocompleteFieldConfig
} from "../../shared/material/autocomplete/material.autocomplete";
import {mergeMap} from "rxjs/internal/operators";
import {ProgramProperties} from "../../referential/services/config/program.config";

const DEFAULT_I18N_PREFIX = 'LANDING.EDIT.';

@Component({
  selector: 'app-landing2-form',
  templateUrl: './landing2.form.html',
  styleUrls: ['./landing2.form.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class Landing2Form extends MeasurementValuesForm<Landing> implements OnInit {

  private _showObservers: boolean;
  private _vessel: any;
  private _strategy: Strategy;

  mobile: boolean;
  observersHelper: FormArrayHelper<Person>;
  observerFocusIndex = -1;
  enableTaxonNameFilter = false;
  canFilterTaxonName = true;

  @Input() i18nPrefix = DEFAULT_I18N_PREFIX;

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

  @Input()
  set strategy(value: Strategy) {
    if (this._strategy !== value && isNotNil(value)) {
      this._strategy = value;
      if (this.strategyControl.value !== value) {
        this.strategyControl.setValue(value);
      }
    }
  }

  get strategy(): Strategy {
    return this._strategy;
  }

  get strategyControl(): FormControl {
    return this.form.controls.strategy as FormControl;
  }

  @Input()
  set vessel(value: string) {
    if (this._vessel !== value && isNotNil(value)) {
      this._vessel = value;
    }
  }

  get vessel(): string {
    return this._vessel;
  }

  get empty(): any {
    const value = this.value;
    return ReferentialUtils.isEmpty(value.location)
      && (!value.dateTime)
      && (!value.comments || !value.comments.length);
  }

  get valid(): any {
    return this.form && (this.required ? this.form.valid : (this.form.valid || this.empty));
  }

  get observersForm(): FormArray {
    return this.form.controls.observers as FormArray;
  }

  constructor(
    protected dateAdapter: DateAdapter<Moment>,
    protected measurementValidatorService: MeasurementsValidatorService,
    protected formBuilder: FormBuilder,
    protected programService: ProgramService,
    protected validatorService: Landing2ValidatorService,
    protected referentialRefService: ReferentialRefService,
    protected personService: PersonService,
    protected vesselSnapshotService: VesselSnapshotService,
    protected settings: LocalSettingsService,
    protected translate: TranslateService,
    protected modalCtrl: ModalController,
    protected cd: ChangeDetectorRef,
    protected strategyService: StrategyService
  ) {
    super(dateAdapter, measurementValidatorService, formBuilder, programService, settings, cd, validatorService.getFormGroup());
    this._enable = false;
    this.mobile = this.settings.mobile;

    // Set default acquisition level
    this.acquisitionLevel = AcquisitionLevelCodes.LANDING;
    this.strategyService = strategyService;

    // Add a strategy field (not in validator)
    this.form.addControl('strategy', formBuilder.control(null, [Validators.required, SharedValidators.entity]));

    this.registerSubscription(
      this.strategyControl.valueChanges
        .pipe(
          map((value) => (typeof value === 'string') ? value : (value && value.label || null)),
          filter(isNotNil),
          tap(strategyLabel => console.info('[landing-form] Strategy changed to: ' + strategyLabel)),
          distinctUntilChanged(),
          mergeMap(strategyLabel => strategyService.loadRefByLabel(strategyLabel, {
            programId: this.data && this.data.program && this.data.program.id
          }))
        )
        .subscribe(strategy => this.strategy = strategy));
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

    // Combo: sampleRowCode
    this.registerAutocompleteField('strategy', {
      service: this.referentialRefService,
      filter: {
        entityName: 'Strategy',
        levelLabel: this.programSubject.getValue() // is empty, will be set in setProgram()
      },
      attributes: ['label', 'name'],
      columnSizes: [6, 6]
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

    // Propagate program
    this.registerSubscription(
      this.form.get('program').valueChanges
        .pipe(
          debounceTime(250),
          filter(ReferentialUtils.isNotEmpty),
          pluck<ReferentialRef, string>('label'),
          distinctUntilChanged()
        )
        .subscribe(programLabel => this.program = programLabel)
        );

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

    this.registerAutocompleteField('taxonName', {
      suggestFn: (value, filter) => this.suggest(value, {
          ...filter, statusId : 1
        },
        'TaxonName',
        this.enableTaxonNameFilter),
      attributes: ['name'],
      columnNames: [ 'REFERENTIAL.NAME'],
      columnSizes: [2, 10],
      mobile: this.settings.mobile
    });
  }

  // get value(): any {
  //   const json = this.form.value;
  //
  //   // Add sampleRowCode, because if control disabled the value is missing
  //   json.sampleRowCode = this.form.get('sampleRowCode').value;
  //
  //   return json;
  // }

  setValue(value: Landing) {
    if (!value) return;

    // Make sure to have (at least) one observer
    value.observers = value.observers && value.observers.length ? value.observers : [null];

    // Resize observers array
    if (this._showObservers) {
      this.observersHelper.resize(Math.max(1, value.observers.length));
    } else {
      this.observersHelper.removeAllEmpty();
    }

    // Propagate the program
    if (value.program && value.program.label) {
      this.program = value.program.label;
    }

    // Send value for form
    super.setValue(value);

    // Set strategy field
    const strategyLabel = Object.entries(value.measurementValues || {})
      .filter(([pmfmId, _]) => +pmfmId === PmfmIds.STRATEGY_LABEL)
      .map(([_, value]) => value)
      .find(isNotNil);
    this.form.patchValue({strategy: strategyLabel});
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
    if (isNotNil(this.data.id) && !this.form.controls['program'].disabled) {
      this.form.controls['program'].disable({emitEvent: false});
      this.markForCheck();
    }
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

  registerAutocompleteField(fieldName: string, options?: MatAutocompleteFieldAddOptions): MatAutocompleteFieldConfig {
    return super.registerAutocompleteField(fieldName, options);
  }


  /* -- protected method -- */

  protected setProgram(program: string) {
    super.setProgram(program);

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

  toggleFilteredItems(fieldName: string){
    let value: boolean;
    switch (fieldName) {
      case 'taxonName':
        this.enableTaxonNameFilter = value = !this.enableTaxonNameFilter;
        break;
      default:
        break;
    }
    this.markForCheck();
    console.debug(`[landing] set enable filtered ${fieldName} items to ${value}`);
  }

  /**
   * Suggest autocomplete values
   * @param value
   * @param filter - filters to apply
   * @param entityName - referential to request
   * @param filtered - boolean telling if we load prefilled data
   */
  protected async suggest(value: string, filter: any, entityName: string, filtered: boolean): Promise<IReferentialRef[]> {

    if (filtered) {
      //TODO a remplacer par recuperation des donnees deja saisies
      const res = await this.referentialRefService.loadAll(0, 5, null, null,
        { ...filter,
          entityName : entityName
        },
        { withTotal: false /* total not need */ }
      );
      return res.data;
    } else {
      return this.referentialRefService.suggest(value, {
        ...filter,
        entityName : entityName
      });
    }
  }

  /**
   * Override refreshPmfms in order to keep sampleRowCode pmfmStrategy in measurement values even if it doesn't belong to strategy.pmfmStrategies
   */
  protected async refreshPmfms(event?: any) {
    // Skip if missing: program, acquisition (or gear, if required)
    if (isNil(this._program) || isNil(this._acquisitionLevel) || (this.requiredGear && isNil(this._gearId))) {
      return;
    }

    if (this.debug) console.debug(`${this.logPrefix} refreshPmfms(${event})`);

    this.loading = true;
    this.loadingPmfms = true;

    this.$pmfms.next(null);

    try {
      // Load pmfms
      let pmfms = (await this.programService.loadProgramPmfms(
        this._program,
        {
          acquisitionLevel: this._acquisitionLevel,
          gearId: this._gearId
        })) || [];
      pmfms = pmfms.filter(pmfm => (pmfm.pmfmId && pmfm.type));

      const sampleRowPmfmStrategy = new PmfmStrategy();
      const sampleRowPmfm = new Pmfm();
      sampleRowPmfm.id = PmfmIds.STRATEGY_LABEL;
      sampleRowPmfm.type = 'string';
      sampleRowPmfmStrategy.pmfm = sampleRowPmfm;
      sampleRowPmfmStrategy.pmfmId = PmfmIds.STRATEGY_LABEL;
      sampleRowPmfmStrategy.type = 'string';
      pmfms.push(sampleRowPmfmStrategy);

      if (!pmfms.length && this.debug) {
        console.warn(`${this.logPrefix} No pmfm found, for {program: ${this._program}, acquisitionLevel: ${this._acquisitionLevel}, gear: ${this._gearId}}. Make sure programs/strategies are filled`);
      }
      else {

        // If force to optional, create a copy of each pmfms that should be forced
        if (this._forceOptional) {
          pmfms = pmfms.map(pmfm => {
            if (pmfm.required) {
              pmfm = pmfm.clone(); // Keep original entity
              pmfm.required = false;
              return pmfm;
            }
            // Return original pmfm, as not need to be overrided
            return pmfm;
          });
        }
      }

      // Apply
      await this.setPmfms(pmfms.slice());
    }
    catch (err) {
      console.error(`${this.logPrefix} Error while loading pmfms: ${err && err.message || err}`, err);
      this.loadingPmfms = false;
      this.$pmfms.next(null); // Reset pmfms
    }
    finally {
      if (this.enabled) this.loading = false;
      this.markForCheck();
    }
  }
}
