import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnInit } from '@angular/core';
import { AsyncValidatorFn, FormArray, FormBuilder, FormControl, FormGroup, ValidationErrors, ValidatorFn } from '@angular/forms';
import { DateAdapter } from '@angular/material/core';
import * as momentImported from 'moment';
import { Moment } from 'moment';
import {
  AppForm,
  AppFormUtils,
  DEFAULT_PLACEHOLDER_CHAR,
  EntityUtils,
  firstArrayValue,
  FormArrayHelper,
  fromDateISOString,
  IReferentialRef,
  isEmptyArray,
  isNil,
  isNilOrBlank,
  isNotEmptyArray,
  isNotNil,
  LoadResult,
  LocalSettingsService,
  MatAutocompleteField,
  ObjectMap,
  ReferentialRef,
  ReferentialUtils,
  removeDuplicatesFromArray,
  SharedValidators,
  StatusIds,
  suggestFromArray,
  toNumber
} from '@sumaris-net/ngx-components';
import { PmfmStrategy } from '../../services/model/pmfm-strategy.model';
import { Program } from '../../services/model/program.model';
import { AppliedPeriod, AppliedStrategy, Strategy, StrategyDepartment, TaxonNameStrategy } from '../../services/model/strategy.model';
import { ReferentialRefService } from '../../services/referential-ref.service';
import { StrategyService } from '../../services/strategy.service';
import { StrategyValidatorService } from '../../services/validator/strategy.validator';
import {
  AcquisitionLevelCodes,
  autoCompleteFractions,
  FractionIdGroups,
  LocationLevelIds,
  ParameterLabelGroups,
  PmfmIds,
  ProgramPrivilegeIds,
  TaxonomicLevelIds
} from '../../services/model/model.enum';
import { ProgramProperties } from '../../services/config/program.config';
import {BehaviorSubject, merge, Subscription} from 'rxjs';
import { SamplingStrategyService } from '../../services/sampling-strategy.service';
import { PmfmService } from '../../services/pmfm.service';
import { SamplingStrategy, StrategyEffort } from '@app/referential/services/model/sampling-strategy.model';
import { TaxonName, TaxonNameRef, TaxonUtils } from '@app/referential/services/model/taxon-name.model';
import { TaxonNameService } from '@app/referential/services/taxon-name.service';
import { PmfmStrategyValidatorService } from '@app/referential/services/validator/pmfm-strategy.validator';
import { Pmfm } from '@app/referential/services/model/pmfm.model';
import { TaxonNameRefFilter } from '@app/referential/services/filter/taxon-name-ref.filter';
import { TaxonNameFilter } from '@app/referential/services/filter/taxon-name.filter';

const moment = momentImported;

type FilterableFieldName = 'analyticReference' | 'location' | 'taxonName' | 'department' | 'lengthPmfm' | 'weightPmfm' | 'maturityPmfm' | 'fractionPmfm';

const MIN_PMFM_COUNT = 2;

@Component({
  selector: 'app-sampling-strategy-form',
  templateUrl: './sampling-strategy.form.html',
  styleUrls: ['./sampling-strategy.form.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SamplingStrategyForm extends AppForm<Strategy> implements OnInit {

  private _started = false;
  private _startPromise: Promise<void>;
  private _$pmfmGroups: BehaviorSubject<ObjectMap<number[]>> = new BehaviorSubject(null);

  initJobs = [];
  mobile: boolean;
  $program = new BehaviorSubject<Program>(null);
  labelMask: (string | RegExp)[] = [/\d/, /\d/, ' ', /^[a-zA-Z]$/, /^[a-zA-Z]$/, /^[a-zA-Z]$/, /^[a-zA-Z]$/, /^[a-zA-Z]$/, /^[a-zA-Z]$/, /^[a-zA-Z]$/, ' ', /\d/, /\d/, /\d/];
  data: SamplingStrategy;

  hasEffort = false;
  hasLanding = false;

  taxonNamesHelper: FormArrayHelper<TaxonNameStrategy>;
  departmentsHelper: FormArrayHelper<StrategyDepartment>;
  appliedStrategiesHelper: FormArrayHelper<AppliedStrategy>;
  appliedPeriodsHelper: FormArrayHelper<AppliedPeriod>;
  pmfmsHelper: FormArrayHelper<PmfmStrategy>;

  lengthPmfmsHelper: FormArrayHelper<PmfmStrategy>;
  weightPmfmsHelper: FormArrayHelper<PmfmStrategy>;
  maturityPmfmsHelper: FormArrayHelper<PmfmStrategy>;
  fractionPmfmsHelper: FormArrayHelper<PmfmStrategy>;
  locationLevelIds: number[];

  disableEditionListeners: boolean;

  autocompleteFilters = {
    analyticReference: false,
    location: false,
    taxonName: false,
    department: false,

    // Pmfms
    lengthPmfm: false,
    weightPmfm: false,
    maturityPmfm: false,
    fractionPmfm: false
  };

  get value(): any {
    throw new Error("Not implemented! Please use getValue() instead, that is an async function");
  }

  get pmfmGroups(): ObjectMap<number[]> {
    return this._$pmfmGroups.getValue();
  }

  set pmfmGroups(value: ObjectMap<number[]>) {
    this._$pmfmGroups.next(value);
  }

  @Input() set program(value: Program) {
    this.setProgram(value);
  }

  get program(): Program {
    return this.$program.getValue();
  }

  get hasSex(): boolean {
    return this.form.get('sex').value;
  }

  get hasAge(): boolean {
    return this.form.get('age').value;
  }

  @Input() tabIndex: number;
  @Input() showError = true;
  @Input() i18nFieldPrefix = 'PROGRAM.STRATEGY.EDIT.';
  @Input() placeholderChar: string = DEFAULT_PLACEHOLDER_CHAR;

  get appliedStrategiesForm(): FormArray {
    return this.form.controls.appliedStrategies as FormArray;
  }

  get appliedStrategyForm(): FormGroup {
    return this.appliedStrategiesHelper && this.appliedStrategiesHelper.at(0) as FormGroup;
  }

  get appliedPeriodsForm(): FormArray {
    const appliedStrategyForm = this.appliedStrategyForm;
    return appliedStrategyForm && appliedStrategyForm.controls.appliedPeriods as FormArray;
  }

  get departmentsFormArray(): FormArray {
    return this.form.controls.departments as FormArray;
  }

  get taxonNamesFormArray(): FormArray {
    return this.form.controls.taxonNames as FormArray;
  }

  get pmfmsForm(): FormArray {
    return this.form.controls.pmfms as FormArray;
  }

  get minPmfmCount(): number {
    return MIN_PMFM_COUNT;
  }

  get lengthPmfmsForm(): FormArray {
    return this.form.controls.lengthPmfms as FormArray;
  }

  get weightPmfmsForm():  FormArray {
    return this.form.controls.weightPmfms as FormArray;
  }

  get maturityPmfmsForm():  FormArray {
    return this.form.controls.maturityPmfms as FormArray;
  }

  get fractionPmfmsForm(): FormArray {
    return this.form.controls.fractionPmfms as FormArray;
  }

  $filteredAnalyticsReferences: BehaviorSubject<ReferentialRef[]> = new BehaviorSubject(null);
  $filteredLocations: BehaviorSubject<ReferentialRef[]> = new BehaviorSubject(null);
  $filteredDepartments: BehaviorSubject<ReferentialRef[]> = new BehaviorSubject(null);
  $filteredTaxonNames: BehaviorSubject<TaxonNameRef[]> = new BehaviorSubject(null);
  $filteredLengthPmfms: BehaviorSubject<ReferentialRef[]> = new BehaviorSubject(null);
  $filteredWeightPmfms: BehaviorSubject<ReferentialRef[]> = new BehaviorSubject(null);
  $filteredMaturityPmfms: BehaviorSubject<ReferentialRef[]> = new BehaviorSubject(null);
  $filteredFractionPmfms: BehaviorSubject<ReferentialRef[]> = new BehaviorSubject(null);

  $allFractions: BehaviorSubject<ReferentialRef[]> = new BehaviorSubject(null);


  enable(opts?: { onlySelf?: boolean, emitEvent?: boolean; }) {
    super.enable(opts);

    // disable whole form or form part
    if (!this.canUserWrite()) {
      this.disable();
       // FIXME fractions not disabled
      this.fractionPmfmsHelper.disable();
    } else if (this.hasLanding) {
      this.taxonNamesFormArray.disable();
      this.appliedStrategiesForm.disable();
      this.lengthPmfmsForm.disable();
      this.weightPmfmsForm.disable();
      this.maturityPmfmsForm.disable();
      const form = this.form;
      form.get('analyticReference').disable();
      form.get('year').disable();
      form.get('label').disable();
      form.get('age').disable();
      form.get('sex').disable();

      // Allow user to update efforts for current quarter and after even when strategy already has samples (#IMAGINE-471)
      this.appliedPeriodsForm.controls.map(control => {
        const formGroupControl = control as FormGroup;
        if (moment().isAfter((formGroupControl.controls.endDate as FormControl).value)) {
          formGroupControl.disable();
        } else {
          formGroupControl.enable();
        }
      });
    }
  }

  constructor(
    protected dateAdapter: DateAdapter<Moment>,
    protected validatorService: StrategyValidatorService,
    protected referentialRefService: ReferentialRefService,
    protected samplingStrategyService: SamplingStrategyService,
    protected pmfmService: PmfmService,
    protected strategyService: StrategyService,
    protected settings: LocalSettingsService,
    protected taxonNameService: TaxonNameService,
    protected pmfmStrategyValidator: PmfmStrategyValidatorService,
    protected cd: ChangeDetectorRef,
    protected formBuilder: FormBuilder
  ) {
    super(dateAdapter, validatorService.getFormGroup(), settings);
    this.mobile = this.settings.mobile;

    // Add missing control
    this.form.addControl('year', new FormControl());
    this.form.addControl('sex', new FormControl());
    this.form.addControl('age', new FormControl());

    // Init array helpers
    this.initDepartmentsHelper();
    this.initTaxonNameHelper();
    this.initAppliedStrategiesHelper();
    this.initAppliedPeriodHelper();

    this.initPmfmStrategiesHelpers();

    // Start loading items
    this.start();
  }


  ngOnInit() {
    super.ngOnInit();

    this.registerSubscription(
      this.form.get('age').valueChanges.subscribe(hasAge => {
        if (hasAge) {
          this.loadFraction();
          this.fractionPmfmsForm.enable();
        }
        else {
          this.fractionPmfmsForm.disable();
        }
      }));
    this.taxonNamesFormArray.setAsyncValidators([async (_) => {
        this.loadFraction();
        return null;
      }
    ]);

    this.appliedPeriodsForm.setAsyncValidators([
      async (control: FormArray) => {
        const minLength = 1;
        const appliedPeriods = control.controls;
        if (!isEmptyArray(appliedPeriods)) {
          const values = appliedPeriods.filter(appliedPeriod => toNumber(appliedPeriod.value.acquisitionNumber, 0) >= 1);
          if (!isEmptyArray(values) && values.length >= minLength) {
            SharedValidators.clearError(control, 'minLength');
            return null;
          }
        }
        return <ValidationErrors>{ minLength: { minLength } };
      },
      // Check quarter acquisitionNumber is not
      async (control) => {
        const appliedPeriods = (control.value as any[]);
        const invalidQuarters = (appliedPeriods || [])
          .map(AppliedPeriod.fromObject)
          .filter(period => {
            const quarter = period.startDate.quarter();
            const quarterEffort: StrategyEffort = this.data && this.data.effortByQuarter && this.data.effortByQuarter[quarter];
            return quarterEffort && quarterEffort.hasRealizedEffort && (isNil(period.acquisitionNumber) || period.acquisitionNumber < 0);
          }).map(period => period.startDate.quarter());
        if (isNotEmptyArray(invalidQuarters)) {
          return <ValidationErrors>{ hasRealizedEffort: { quarters: invalidQuarters } };
        }
        SharedValidators.clearError(control, 'hasRealizedEffort');
        return null;
      }
    ]);

    const pmfmValidator: AsyncValidatorFn = (_) => this.validatePmfmsForm();
    this.pmfmsForm.setAsyncValidators(pmfmValidator);
    this.lengthPmfmsForm.setAsyncValidators(pmfmValidator);
    this.weightPmfmsForm.setAsyncValidators(pmfmValidator);
    this.maturityPmfmsForm.setAsyncValidators(pmfmValidator);

    // Force pmfms validation, when sex/age changes
    this.registerSubscription(
      merge(
        this.form.get('sex').valueChanges,
        this.form.get('age').valueChanges
      ).subscribe(() => {
        this.pmfmsForm.updateValueAndValidity();
        this.validatePmfmsForm();
      })
    );

    this.registerSubscription(this.form.get('label').valueChanges.subscribe(value => this.onEditLabel(value)));
    this.registerSubscription(this.form.get('year').valueChanges.subscribe(date => this.onDateChange(date)));
    this.registerSubscription(this.taxonNamesFormArray.valueChanges.subscribe(() => this.onTaxonChange()));

    const idControl = this.form.get('id');
    this.form.get('label').setAsyncValidators([
      async (control) => {
        const label = control.value;
        const parts = label.split(" ");
        if (parts.some(str => str.indexOf("_") !== -1)) {
          return <ValidationErrors>{ required: true };
        }
        if (label.includes('000')) {
          return <ValidationErrors>{ zero: true };
        }
        console.debug('[sampling-strategy-form] Checking of label is unique...');
        const exists = await this.strategyService.existsByLabel(label, {
          programId: this.program && this.program.id,
          excludedIds: isNotNil(idControl.value) ? [idControl.value] : undefined,
          fetchPolicy: 'network-only' // Force to check remotely
        });
        if (exists) {
          console.warn('[sampling-strategy-form] Label not unique!');
          return <ValidationErrors>{ unique: true };
        }

        console.debug('[sampling-strategy-form] Checking of label is unique [OK]');
        SharedValidators.clearError(control, 'unique');
        SharedValidators.clearError(control, 'zero');
      }
    ]);

    // taxonName autocomplete
    this.registerAutocompleteField('taxonName', {
      suggestFn: (value, filter) => this.suggestTaxonName(value, <Partial<TaxonNameRefFilter>>{
        ...filter,
        searchAttribute: 'name',
        statusIds: [StatusIds.ENABLE],
        levelIds: [TaxonomicLevelIds.SPECIES, TaxonomicLevelIds.SUBSPECIES]
      }),
      attributes: ['name'],
      columnNames: ['REFERENTIAL.NAME'],
      mobile: this.settings.mobile
    });

    // Department autocomplete
    this.registerAutocompleteField('department', {
      suggestFn: (value, filter) => this.suggestDepartments(value, {
        ...filter, statusIds: [StatusIds.ENABLE, StatusIds.TEMPORARY]
      }),
      columnSizes: [4, 8],
      mobile: this.settings.mobile
    });

    // appliedStrategy autocomplete
    this.registerAutocompleteField('location', {
      suggestFn: (value, filter) => this.suggestLocations(value, {
        ...filter,
        statusIds: [StatusIds.ENABLE, StatusIds.TEMPORARY],
        // TODO BLA: pourquoi utiliser une constante globale,
        //  et non pas un option de Program ?
        levelIds: LocationLevelIds.LOCATIONS_AREA
      }),
      mobile: this.settings.mobile
    });

    // Analytic reference autocomplete
    this.registerAutocompleteField('analyticReference', {
      suggestFn: (value, filter) => this.suggestAnalyticReferences(value, {
        ...filter, statusIds: [StatusIds.ENABLE, StatusIds.TEMPORARY]
      }),
      columnSizes: [4, 6],
      mobile: this.settings.mobile
    });

    // length PMFM autocomplete
    this.registerAutocompleteField('lengthPmfm', {
      // suggestFn: (value, filter) => this.suggestLengthPmfms(value, {
      suggestFn: (value, filter) => this.suggestLengthPmfms(value, {
        ...filter,
        statusIds: [StatusIds.ENABLE, StatusIds.TEMPORARY],
        levelLabels: ParameterLabelGroups.LENGTH
      }),
      attributes: ['name', 'unit.label', 'matrix.name', 'fraction.name', 'method.name'],
      columnNames: ['REFERENTIAL.NAME', 'REFERENTIAL.PMFM.UNIT', 'REFERENTIAL.PMFM.MATRIX', 'REFERENTIAL.PMFM.FRACTION', 'REFERENTIAL.PMFM.METHOD'],
      mobile: this.settings.mobile
    });

    // appliedStrategy autocomplete
    this.registerAutocompleteField('weightPmfm', {
      suggestFn: (value, filter) => this.suggestWeightPmfms(value, {
        ...filter,
        statusIds: [StatusIds.ENABLE, StatusIds.TEMPORARY],
        levelLabels: ParameterLabelGroups.WEIGHT
      }),
      attributes: ['name', 'unit.label', 'matrix.name', 'fraction.name', 'method.name'],
      columnNames: ['REFERENTIAL.NAME', 'REFERENTIAL.PMFM.UNIT', 'REFERENTIAL.PMFM.MATRIX', 'REFERENTIAL.PMFM.FRACTION', 'REFERENTIAL.PMFM.METHOD'],
      mobile: this.settings.mobile
    });

    // appliedStrategy autocomplete
    this.registerAutocompleteField('maturityPmfm', {
      suggestFn: (value, filter) => this.suggestMaturityPmfms(value, {
        ...filter,
        statusIds: [StatusIds.ENABLE, StatusIds.TEMPORARY],
        levelLabels: ParameterLabelGroups.MATURITY
      }),
      attributes: ['name', 'unit.label', 'matrix.name', 'fraction.name', 'method.name'],
      columnNames: ['REFERENTIAL.NAME', 'REFERENTIAL.PMFM.UNIT', 'REFERENTIAL.PMFM.MATRIX', 'REFERENTIAL.PMFM.FRACTION', 'REFERENTIAL.PMFM.METHOD'],
      mobile: this.settings.mobile
    });

    // Fraction autocomplete
    this.registerAutocompleteField('fractionPmfm', {
      suggestFn: (value, filter) => this.suggestFractionPmfms(value, {
        ...filter,
        statusIds: [StatusIds.ENABLE, StatusIds.TEMPORARY],
        includedIds: FractionIdGroups.CALCIFIED_STRUCTURE
      }),
      attributes: ['name'],
      columnNames: ['REFERENTIAL.NAME'],
      mobile: this.settings.mobile
    });
  }

  public setDisableEditionListeners (disable : boolean)
  {
    this.disableEditionListeners = disable;
  }

  async start(): Promise<void> {
    if (this._started) return;
    if (this._startPromise) return this._startPromise;

    const now = Date.now();
    console.debug('[sampling-strategy-form] Starting...');
    this._startPromise = Promise.all([

      this.referentialRefService.loadAll(0, 1000, null, null, {
        entityName: 'Fraction',
        statusIds: [StatusIds.ENABLE, StatusIds.TEMPORARY],
        includedIds: FractionIdGroups.CALCIFIED_STRUCTURE
      })
        .then(({data}) => this.$allFractions.next(data)),

      // Load pmfm by parameter groups
      this.pmfmService.loadIdsGroupByParameterLabels(ParameterLabelGroups)
        .then(pmfmGroups => this._$pmfmGroups.next(pmfmGroups))
    ]).then(() => {
      console.debug('[sampling-strategy-form] Started in ' + (Date.now() - now) + 'ms');
      this._started = true;
    });
    return this._startPromise;
  }

  protected async setProgram(program: Program, opts?: { emitEvent?: boolean; }) {
    if (program && this.program !== program) {
      this.i18nFieldPrefix = 'PROGRAM.STRATEGY.EDIT.';
      const i18nSuffix = program.getProperty(ProgramProperties.I18N_SUFFIX) || '';
      this.i18nFieldPrefix += i18nSuffix !== 'legacy' && i18nSuffix || '';

      // Get location level ids
      this.locationLevelIds = program.getPropertyAsNumbers(ProgramProperties.STRATEGY_EDITOR_LOCATION_LEVEL_IDS);

      // Load items from historical data
      this.loadFilteredItems(program);

      this.$program.next(program);

      if (!opts || opts.emitEvent !== false) {
        this.markForCheck();
      }
    }
  }

  async ready(): Promise<void> {
    if (!this._started) return this.start();
  }

  loadFraction(): void {
    const taxonNameStrategies = this.hasAge && this.taxonNamesFormArray.value
    if (isNotEmptyArray(taxonNameStrategies) && taxonNameStrategies[0]?.taxonName) {
      const taxonNameStrategy = taxonNameStrategies[0];
      const fractionName = autoCompleteFractions[taxonNameStrategy.taxonName.id];
      if (fractionName) {
        const fraction = this.$allFractions.value.find(f => f.label.toUpperCase() === fractionName.toUpperCase());
        this.fractionPmfmsForm.patchValue([{fraction}]);
      }
    }
  }

  loadFilteredItems(program: Program): Promise<void> {
    // Get load options, from program properties
    const autoEnableFilter = program.getPropertyAsBoolean(ProgramProperties.STRATEGY_EDITOR_PREDOC_ENABLE);
    const fetchSize = program.getPropertyAsInt(ProgramProperties.STRATEGY_EDITOR_PREDOC_FETCH_SIZE);

    return Promise.all([

      // Analytic References
      this.strategyService.loadStrategiesReferentials(program.id, 'AnalyticReference', undefined, 0, fetchSize)
        .then(analyticReferences => {
          analyticReferences = removeDuplicatesFromArray(analyticReferences, 'id');
          this.$filteredAnalyticsReferences.next(analyticReferences);
          this.autocompleteFilters.analyticReference = autoEnableFilter && isNotEmptyArray(analyticReferences); // Enable filtering, if need by program
        })
        .catch(err => {
          console.debug('Error on load AnalyticReferences: ', err);
          this.autocompleteFilters.analyticReference = false;
        }),

      // Departments
      this.strategyService.loadStrategiesReferentials(program.id, 'Department', undefined, 0, fetchSize)
        .then(departments => {
          this.$filteredDepartments.next(departments);
          this.autocompleteFilters.department = autoEnableFilter && isNotEmptyArray(departments); // Enable filtering, if need by program
        }),

      // Locations
      this.strategyService.loadStrategiesReferentials(program.id, 'Location', 'SEA', 0, fetchSize)
        .then(locations => {
          this.$filteredLocations.next(locations);
          this.autocompleteFilters.location = autoEnableFilter && isNotEmptyArray(locations); // Enable filtering, if need by program
        }),

      // Taxons
      this.strategyService.loadStrategiesReferentials(program.id, 'TaxonName', undefined, 0, fetchSize)
        .then(taxons => {
          this.$filteredTaxonNames.next(taxons as TaxonNameRef[]);
          this.autocompleteFilters.taxonName = autoEnableFilter && isNotEmptyArray(taxons); // Enable filtering, if need by program
        }),

      // Length pmfms
      /*this.strategyService.loadStrategiesReferentials(program.id, 'Pmfm', undefined, 0, fetchSize, 'name')
        .then(lengthPmfms => {
          this.$filteredLengthPmfms.next(lengthPmfms);
          this.autocompleteFilters.lengthPmfm = isNotEmptyArray(lengthPmfms) && autoEnableFilter; // Enable filtering, if need by program
        }),

      // Weight pmfms
      this.strategyService.loadStrategiesReferentials(program.id, 'Pmfm', undefined, 0, fetchSize, 'name')
        .then(weightPmfms => {
          this.$filteredWeightPmfms.next(weightPmfms);
          this.autocompleteFilters.weightPmfm = isNotEmptyArray(weightPmfms) && autoEnableFilter; // Enable filtering, if need by program
        }),

      // Maturity pmfms
      this.strategyService.loadStrategiesReferentials(program.id, 'Pmfm', undefined, 0, fetchSize, 'name')
        .then(maturityPmfms => {
          this.$filteredMaturityPmfms.next(maturityPmfms);
          this.autocompleteFilters.maturityPmfm = isNotEmptyArray(maturityPmfms) && autoEnableFilter; // Enable filtering, if need by program
        }),*/

      // Fractions pmfm
      this.strategyService.loadStrategiesReferentials(program.id, 'Fraction', undefined, 0, fetchSize)
        .then(fractions => {
          this.$filteredFractionPmfms.next(fractions);
          this.autocompleteFilters.fractionPmfm = autoEnableFilter && isNotEmptyArray(fractions); // Enable filtering, if need by program
        })
    ])
    .then(() => this.markForCheck());
  }


  async getAnalyticReferenceByLabel(label: string): Promise<ReferentialRef> {
    if (isNilOrBlank(label)) return undefined;
    try {
      const res = await this.strategyService.loadAllAnalyticReferences(0, 1, 'label', 'desc', { label });
      return firstArrayValue(res && res.data || []);
    } catch (err) {
      console.debug('Error on load AnalyticReference');
    }
  }

  removeAppliedStrategies(index: number) {
    // first element AND more than one element
    // this.appliedPeriodsForm.controls become empty array
    let appliedPeriodsFormControls: any = null;
    if (index === 0 && this.appliedStrategiesHelper.size() > 1) {
      appliedPeriodsFormControls = this.appliedPeriodsForm.controls;
    }
    this.appliedStrategiesHelper.removeAt(index);
    if (index === 0) {
      if (appliedPeriodsFormControls) {
        this.appliedPeriodsForm.controls = appliedPeriodsFormControls;
      }
    }
  }

  /**
   * Select text that can be changed, using the text mask
   * @param input
   */
  selectMask(input: HTMLInputElement) {
    if (!this.labelMask) input.select();
    const taxonNameControl = this.taxonNamesHelper.at(0);
    const endIndex = this.labelMask.length;
    if (taxonNameControl.hasError('cannotComputeTaxonCode') || taxonNameControl.hasError('uniqueTaxonCode')) {
      input.setSelectionRange(3, endIndex, "backward");
    }
    else
    {
      input.setSelectionRange(11, endIndex, "backward");
    }
  }

  toggleFilter(fieldName: FilterableFieldName, field?: MatAutocompleteField) {
    this.autocompleteFilters[fieldName] = !this.autocompleteFilters[fieldName];
    this.markForCheck();

    if (field) field.reloadItems();
  }


  /**
   * Suggest autocomplete values
   * @param value
   * @param filter - filters to apply
   */
  protected async suggestDepartments(value: any, filter: any): Promise<LoadResult<IReferentialRef>> {
    const currentControlValue = ReferentialUtils.isNotEmpty(value) ? value : null;
    const newValue = currentControlValue ? '*' : value;

    // Excluded existing locations, BUT keep the current control value
    const excludedIds = (this.departmentsFormArray.value || [])
      .map(pmfmDepartment => pmfmDepartment?.department)
      .filter(ReferentialUtils.isNotEmpty)
      .filter(item => !currentControlValue || currentControlValue !== item)
      .map(item => parseInt(item.id));

    if (this.autocompleteFilters.department) {
      return suggestFromArray(this.$filteredDepartments.getValue(), newValue, {
        ...filter,
        excludedIds
      });
    } else {
      return this.referentialRefService.suggest(newValue, {
        ...filter,
        excludedIds,
        entityName: 'Department'
      });
    }
  }

  protected async suggestTaxonName(value: any, filter: any): Promise<LoadResult<TaxonNameRef>> {
    if (this.autocompleteFilters.taxonName) {
      return suggestFromArray(this.$filteredTaxonNames.getValue(), value, filter);
    } else {
      return this.referentialRefService.suggestTaxonNames(value,
        {
          ...filter,
          entityName: 'TaxonName'
        },
      );
    }
  }

  /**
   * Suggest autocomplete values
   * @param value
   * @param filter - filters to apply
   */
  protected async suggestLocations(value: string, filter: any): Promise<LoadResult<IReferentialRef>> {
    filter = {
      levelIds: this.locationLevelIds || [LocationLevelIds.ICES_DIVISION],
      ...filter
    }
    // DEBUG
    //console.debug("Suggest locations: ", filter);
    if (this.autocompleteFilters.location) {
      return suggestFromArray(this.$filteredLocations.getValue(), value, filter);
    } else {
      return this.referentialRefService.suggest(value, {
        ...filter,
        entityName: 'Location'
      });
    }
  }

  /**
   * Suggest autocomplete values
   * @param value
   * @param filter - filters to apply
   */
  protected async suggestAnalyticReferences(value: string, filter: any): Promise<LoadResult<IReferentialRef>> {
    if (this.autocompleteFilters.analyticReference) {
      return suggestFromArray(this.$filteredAnalyticsReferences.getValue(), value, filter);
    } else {
      return this.strategyService.suggestAnalyticReferences(value, filter);
    }
  }

  /**
   * Suggest autocomplete values, for length pmfms
   * @param value
   * @param filter - filters to apply
   */
  protected async suggestLengthPmfms(value: any, filter: any): Promise<LoadResult<IReferentialRef>> {

    const currentControlValue = ReferentialUtils.isNotEmpty(value) ? value : null;
    const newValue = currentControlValue ? '*' : value;

    // Excluded existing locations, BUT keep the current control value
    const excludedIds = (this.lengthPmfmsForm.value || [])
      .map(ps => ps?.pmfm)
      .filter(ReferentialUtils.isNotEmpty)
      .filter(item => !currentControlValue || currentControlValue !== item)
      .map(item => parseInt(item.id));

    if (this.autocompleteFilters.lengthPmfm) {
      return suggestFromArray(this.$filteredLengthPmfms.value, value, {
        ...filter,
        excludedIds
      });
    } else {
      return this.pmfmService.suggest(newValue, {
        ...filter,
        excludedIds,
        entityName: Pmfm.ENTITY_NAME
      },
      'name');
    }
  }

  /**
   * Suggest autocomplete values, for weight pmfms
   * @param value
   * @param filter - filters to apply
   */
  protected async suggestWeightPmfms(value: string, filter: any): Promise<LoadResult<IReferentialRef>> {
    const currentControlValue = ReferentialUtils.isNotEmpty(value) ? value : null;
    const newValue = currentControlValue ? '*' : value;

    // Excluded existing locations, BUT keep the current control value
    const excludedIds = (this.weightPmfmsForm.value || [])
      .map(ps => ps?.pmfm)
      .filter(ReferentialUtils.isNotEmpty)
      .filter(item => !currentControlValue || currentControlValue !== item)
      .map(item => parseInt(item.id));

    if (this.autocompleteFilters.weightPmfm) {
      return suggestFromArray(this.$filteredWeightPmfms.value, value, {
        ...filter,
        excludedIds
      });
    } else {
      return this.pmfmService.suggest(newValue, {
        ...filter,
        excludedIds,
        entityName: Pmfm.ENTITY_NAME
      },
      'name');
    }
  }

  /**
   * Suggest autocomplete values, for maturity pmfms
   * @param value
   * @param filter - filters to apply
   */
  protected async suggestMaturityPmfms(value: string, filter: any): Promise<LoadResult<IReferentialRef>> {
    const currentControlValue = ReferentialUtils.isNotEmpty(value) ? value : null;
    const newValue = currentControlValue ? '*' : value;

    // Excluded existing locations, BUT keep the current control value
    const excludedIds = (this.maturityPmfmsForm.value || [])
      .map(ps => ps?.pmfm)
      .filter(ReferentialUtils.isNotEmpty)
      .filter(item => !currentControlValue || currentControlValue !== item)
      .map(item => parseInt(item.id));

    if (this.autocompleteFilters.maturityPmfm) {
      return suggestFromArray(this.$filteredMaturityPmfms.value, value, filter);
    } else {
      return this.pmfmService.suggest(newValue, {
        ...filter,
        excludedIds,
        entityName: Pmfm.ENTITY_NAME
      },
      'name');
    }
  }

  /**
   * Suggest autocomplete values, for age fraction
   * @param value
   * @param filter - filters to apply
   */
  protected async suggestFractionPmfms(value: string, filter: any): Promise<LoadResult<IReferentialRef>> {
    const currentControlValue = ReferentialUtils.isNotEmpty(value) ? value : null;
    const newValue = currentControlValue ? '*' : value;

    // Excluded existing locations, BUT keep the current control value
    const excludedIds = (this.fractionPmfmsForm.value || [])
      .map(ps => ps?.fraction)
      .filter(ReferentialUtils.isNotEmpty)
      .filter(item => !currentControlValue || currentControlValue !== item)
      .map(item => parseInt(item.id));

    if (this.autocompleteFilters.fractionPmfm) {
      return suggestFromArray(this.$filteredFractionPmfms.value, newValue, {
        ...filter,
        excludedIds
      });
    } else {
      return suggestFromArray(this.$allFractions.getValue(), newValue, {
        ...filter,
        excludedIds
      });
    }
  }


  setValue(data: SamplingStrategy, opts?: { emitEvent?: boolean; onlySelf?: boolean }) {
    console.debug("[sampling-strategy-form] Setting Strategy value", data);
    if (!data) return;

    const isNew = isNil(data.id);
    this.data = SamplingStrategy.fromObject(data);

    // Fill efforts (need by validator)
    this.hasEffort = this.data.hasRealizedEffort;
    this.hasLanding = this.data.hasLanding;

    // Make sure to have (at least) one department
    data.departments = data.departments && data.departments.length ? data.departments : [null];
    // Resize strategy department array
    this.departmentsHelper.resize(Math.max(1, data.departments.length));

    data.appliedStrategies = isNotEmptyArray(data.appliedStrategies) ? data.appliedStrategies : [new AppliedStrategy()];
    // Resize strategy department array
    this.appliedStrategiesHelper.resize(Math.max(1, data.appliedStrategies.length));

    data.taxonNames = data.taxonNames && data.taxonNames.length ? data.taxonNames : [null];
    // Resize pmfm strategy array
    this.taxonNamesHelper.resize(Math.max(1, data.taxonNames.length));

    // APPLIED_PERIODS
    // get model appliedPeriods which are stored in first applied strategy
    const appliedStrategyWithPeriods = firstArrayValue((data.appliedStrategies || []).filter(as => as && isNotEmptyArray(as.appliedPeriods)))
      || firstArrayValue(data.appliedStrategies || []);
    const appliedPeriods = appliedStrategyWithPeriods.appliedPeriods || [];

    // Find year, from applied period, or use current
    const year: number = firstArrayValue(appliedPeriods.map(ap => ap.startDate.year())) || moment().year();

    // format periods for applied period in view and init default period by quarter if no set
    appliedStrategyWithPeriods.appliedPeriods = [1, 2, 3, 4].map(quarter => {
      const startMonth = (quarter - 1) * 3 + 1;
      const startDate = fromDateISOString(`${year}-${startMonth.toString().padStart(2, '0')}-01T00:00:00.000Z`).utc();
      const endDate = startDate.clone().add(2, 'month').endOf('month').startOf('day');
      // Find the existing entity, or create a new one
      const appliedPeriod = appliedPeriods && appliedPeriods.find(period => period.startDate.month() === startDate.month())
        || AppliedPeriod.fromObject({ acquisitionNumber: undefined });
      appliedPeriod.startDate = startDate;
      appliedPeriod.endDate = endDate;

      return appliedPeriod;
    });

    // Resize applied periods array
    this.appliedPeriodsHelper.resize(4);

    // Get first period
    const firstAppliedPeriod = firstArrayValue(appliedStrategyWithPeriods.appliedPeriods);

    data.year = firstAppliedPeriod ? firstAppliedPeriod.startDate : moment();

    data.pmfms = data.pmfms || [];

    // If new
    if (isNew) {
      // pmfms = [null, null];
      data.sex = null;
      data.age = null;
    } else {
      data.age = data.pmfms.findIndex(p => p.pmfmId && p.pmfmId === PmfmIds.AGE) !== -1;
      data.sex = data.pmfms.findIndex(p => p.pmfmId && p.pmfmId === PmfmIds.SEX) !== -1;
      console.debug("[sampling-strategy-form] Has sex ?", data.sex, PmfmIds.SEX);
      data.label = data.label.substr(0, 2).concat(' ').concat(data.label.substr(2, 7)).concat(' ').concat(data.label.substr(9, 3));
    }

    data.lengthPmfms = this.getPmfmStrategiesByGroup(data.pmfms, this.pmfmGroups.LENGTH, ParameterLabelGroups.LENGTH);
    data.weightPmfms = this.getPmfmStrategiesByGroup(data.pmfms, this.pmfmGroups.WEIGHT, ParameterLabelGroups.WEIGHT);
    data.maturityPmfms = this.getPmfmStrategiesByGroup(data.pmfms, this.pmfmGroups.MATURITY, ParameterLabelGroups.MATURITY);
    data.fractionPmfms = (data.pmfms || [])
      .filter(p => p.fraction && !p.pmfm)
      .map(ps => {
        ps.fraction = this.$allFractions.value.find(fraction => fraction.id === ps.fraction.id);
        return ps;
      });

    // Min size = 1
    if (isEmptyArray(data.lengthPmfms)) data.lengthPmfms = [new PmfmStrategy()];
    if (isEmptyArray(data.weightPmfms)) data.weightPmfms = [new PmfmStrategy()];
    if (isEmptyArray(data.maturityPmfms)) data.maturityPmfms = [new PmfmStrategy()];
    if (isEmptyArray(data.fractionPmfms)) data.fractionPmfms = [new PmfmStrategy()];

    this.lengthPmfmsHelper.resize(Math.max(1, data.lengthPmfms.length));
    this.weightPmfmsHelper.resize(Math.max(1, data.weightPmfms.length));
    this.maturityPmfmsHelper.resize(Math.max(1, data.maturityPmfms.length));
    this.fractionPmfmsHelper.resize(Math.max(1, data.fractionPmfms.length));

    super.setValue(data, opts);

  }

  async getValue(): Promise<SamplingStrategy> {
    const json = this.form.getRawValue();
    const target = SamplingStrategy.fromObject(json);

    target.name = target.label || target.name;
    target.label = target.label || target.name;
    target.description = target.label || target.description;
    target.analyticReference = target.analyticReference && EntityUtils.isNotEmpty(target.analyticReference, 'label') ?
      target.analyticReference['label'] :
      EntityUtils.isNotEmpty(this.form.get('analyticReference').value, 'label') ?
        this.form.get('analyticReference').value.label :
        this.form.get('analyticReference').value;

    // get taxonName and
    target.taxonNames = (this.form.controls.taxonNames.value || []).map(TaxonNameStrategy.fromObject);
    target.taxonNames.forEach(taxonNameStrategy => {
      delete taxonNameStrategy.strategyId; // Not need when saved
      taxonNameStrategy.priorityLevel = taxonNameStrategy.priorityLevel || 1;
      taxonNameStrategy.taxonName = TaxonNameRef.fromObject({
        ...taxonNameStrategy.taxonName,
        taxonGroupIds: undefined
      });
    });

    // Apply observer privilege to departments
    const observerPrivilege = ReferentialRef.fromObject({ id: ProgramPrivilegeIds.OBSERVER, entityName: 'ProgramPrivilege' });
    target.departments = (target.departments || []).map(StrategyDepartment.fromObject);
    target.departments.forEach(department => {
      department.privilege = observerPrivilege;
    });

    // Compute year
    const year = isNotNil(this.form.controls.year.value) ? moment(this.form.controls.year.value).year() : moment().year();

    // Fishing Area + Efforts --------------------------------------------------------------------------------------------

    const appliedStrategyWithPeriods = firstArrayValue((target.appliedStrategies || []).filter(as => isNotEmptyArray(as.appliedPeriods)));
    if (appliedStrategyWithPeriods) {
      appliedStrategyWithPeriods.appliedPeriods = (appliedStrategyWithPeriods && appliedStrategyWithPeriods.appliedPeriods || [])
        // Exclude period without acquisition number
        .filter(period => isNotNil(period.acquisitionNumber))
        .map(ap => {
          // Set year (a quarter should be already set)
          ap.startDate.set('year', year);
          ap.endDate.set('year', year);
          ap.appliedStrategyId = appliedStrategyWithPeriods.id;
          return ap;
        });

      // Clean periods, on each other applied strategies
      (target.appliedStrategies || [])
        .filter(as => as !== appliedStrategyWithPeriods)
        .forEach(appliedStrategy => appliedStrategy.appliedPeriods = []);
    }

    // PMFM + Fractions -------------------------------------------------------------------------------------------------
    let pmfmStrategies: Partial<PmfmStrategy>[] = [
      // Add tag id Pmfm
      {pmfmId: PmfmIds.TAG_ID },
      // Add dressing Pmfm
      {pmfmId: PmfmIds.DRESSING },
      // Weight
      ...target.weightPmfms,
      // Length
      ...target.lengthPmfms
    ];

    // Add SEX Pmfm
    if (target.sex) {
      pmfmStrategies = pmfmStrategies.concat([
        { pmfmId: PmfmIds.SEX },
        ...target.maturityPmfms
      ]);
    }

    // Add AGE Pmfm
    if (target.age) {
      pmfmStrategies = pmfmStrategies.concat([
        { pmfmId: PmfmIds.AGE },
        ...target.fractionPmfms
      ]);
    }

    // Fill PmfmStrategy defaults
    let rankOrder = 1;
    target.pmfms = pmfmStrategies
      .filter(isNotNil)
      .map(PmfmStrategy.fromObject)
      .map(pmfmStrategy => {
        pmfmStrategy.strategyId = pmfmStrategy.id;
        pmfmStrategy.acquisitionLevel = AcquisitionLevelCodes.SAMPLE;
        pmfmStrategy.acquisitionNumber = 1;
        pmfmStrategy.isMandatory = false;
        pmfmStrategy.rankOrder = rankOrder++;
        return pmfmStrategy;
      })
      // Remove if empty
      .filter(p => isNotNil(p.pmfmId) || isNotNil(p.pmfm) || isNotNil(p.parameter) || isNotNil(p.matrix) || isNotNil(p.fraction) || isNotNil(p.method));


    return target;
  }

  protected async onEditLabel(value: string) {
    if (this.disableEditionListeners) return;
    const taxonNameControl = this.taxonNamesHelper.at(0);
    if (!value) return
    const expectedLabelFormatRegex = new RegExp(/^\d\d [a-zA-Z][a-zA-Z][a-zA-Z][a-zA-Z][a-zA-Z][a-zA-Z][a-zA-Z] ___$/);
    if (value.match(expectedLabelFormatRegex)) {
      const currentViewTaxon = taxonNameControl?.value?.taxonName;
      const isUnique = await this.isTaxonNameUnique(value.substring(3, 10), currentViewTaxon?.id);
      if (!isUnique) {
        taxonNameControl.setErrors({ uniqueTaxonCode: true });
      } else {
        SharedValidators.clearError(this.taxonNamesHelper.at(0), 'uniqueTaxonCode');
      }
    }
    const acceptedLabelFormatRegex = new RegExp(/^\d\d [a-zA-Z][a-zA-Z][a-zA-Z][a-zA-Z][a-zA-Z][a-zA-Z][a-zA-Z] \d\d\d$/);
    if (value.match(acceptedLabelFormatRegex)) {
      const currentViewTaxon = taxonNameControl?.value?.taxonName;
      const isUnique = await this.isTaxonNameUnique(value.substring(3, 10), currentViewTaxon?.id);
      if (!isUnique) {
        //taxonNameControl.setErrors({ uniqueTaxonCode: true });
      } else {
        SharedValidators.clearError(this.taxonNamesHelper.at(0), 'uniqueTaxonCode');
        const labelControl = this.form.get('label');
        labelControl.setValue(value?.replace(/\s/g, '').toUpperCase());
      }
    }
  }

  private async isTaxonNameUnique(label: string, currentViewTaxonId?: number): Promise<boolean> {
    if (isNilOrBlank(label)) return true;

    const taxonNameFilter: Partial<TaxonNameFilter> = {
      searchAttribute: 'name',
      excludedIds: [currentViewTaxonId],
      statusIds: [StatusIds.ENABLE],
      levelIds: [TaxonomicLevelIds.SPECIES, TaxonomicLevelIds.SUBSPECIES],
      withSynonyms: false
    };

    const [first, second] = await Promise.all([
      // Try without parenthesis
      this.taxonNameService.countAll({
        ...taxonNameFilter,
        searchText: TaxonUtils.generateNameSearchPatternFromLabel(label, false)
      }),
      // Try WITH parenthesis
      this.taxonNameService.countAll({
        ...taxonNameFilter,
        searchText: TaxonUtils.generateNameSearchPatternFromLabel(label, true)
      }),
    ])

    return (first + second) === 0;
  }

  protected async onDateChange(date?: Moment) {
    if (this.disableEditionListeners) return;
    let dateAsMoment: Moment;
    if (typeof date === 'string') {
      dateAsMoment = moment(date, 'YYYY-MM-DD');
    } else {
      dateAsMoment = date;
    }
    if (!dateAsMoment || dateAsMoment.isBefore(moment("1900-12-31T00:00:00.000Z", 'YYYY-MM-DD'))) return;

      await this.generateLabel(dateAsMoment);
  }

  protected async onTaxonChange() {
    if (this.disableEditionListeners) return;
    if (!this.program) return; // Skip if program is missing
    await this.generateLabel();
    // TODO try to limit pmfms, by loading previous sampling strategies ?
  }

  protected async generateLabel(date?: Moment) {

    date = fromDateISOString(date || this.form.get('year').value);
    if (!date || !this.program) return // Skip if year or program is missing
    const yearMask = date.format('YY');

    let errors: ValidationErrors;
    const taxonNameControl = this.taxonNamesHelper.at(0);
    const currentViewTaxon = taxonNameControl?.value?.taxonName;
    const currentViewTaxonName = taxonNameControl?.value?.taxonName?.name;
    const previousFormTaxonName = this.form.getRawValue().taxonNames[0]?.taxonName?.name?.clone;
    const storedDataTaxonName = this.data.taxonNames[0]?.taxonName?.name;
    const storedDataYear = this.data.appliedStrategies[0]?.appliedPeriods[0]?.startDate ? fromDateISOString(this.data.appliedStrategies[0].appliedPeriods[0].startDate).format('YY') : undefined;
    let previousFormYear = undefined;
    if (this.form.getRawValue().year && fromDateISOString(this.form.getRawValue().year)) {
      previousFormYear = fromDateISOString(this.form.getRawValue().year).format('YY');
    }
    const labelControl = this.form.get('label');

    const label = currentViewTaxonName && TaxonUtils.generateLabelFromName(currentViewTaxonName);
    const isUnique = await this.isTaxonNameUnique(label, currentViewTaxon?.id);

    if (!label) {
      errors = {cannotComputeTaxonCode: true};
    } else if (!isUnique) {
      errors = {uniqueTaxonCode: true};
    }

    // @ts-ignore
    const newMask = yearMask.split("")
      .concat([' ', /^[a-zA-Z]$/, /^[a-zA-Z]$/, /^[a-zA-Z]$/, /^[a-zA-Z]$/, /^[a-zA-Z]$/, /^[a-zA-Z]$/, /^[a-zA-Z]$/, ' ', /\d/, /\d/, /\d/]);

    if (currentViewTaxonName  && currentViewTaxonName === previousFormTaxonName && yearMask && yearMask === previousFormYear) return; // Skip generate label when there is no update on year or taxon
    this.labelMask = newMask;

    if (errors && taxonNameControl) {
      // if (this.data.label && this.data.label.substring(0, 2) === yearMask && this.data.label.substring(2, 9) === labelControl.value.toUpperCase().substring(2, 9)) {
      //   labelControl.setValue(this.data.label);
      // } else {
        const computedLabel = `${yearMask} `;
        if (!taxonNameControl.errors) {

          if ((this.data.label && this.data.label === labelControl.value) && (storedDataTaxonName && storedDataTaxonName === currentViewTaxonName)) {
            // When function is called back after save, we do nothing
          }
          else {
            labelControl.setValue(computedLabel);
          }
        }
        taxonNameControl.setErrors(errors);
      // }
    } else {
      //const computedLabel = this.program && (await this.strategyService.computeNextLabel(this.program.id, `${yearMask}${label}`, 3));
      SharedValidators.clearError(taxonNameControl, 'cannotComputeTaxonCode');
      //console.info('[sampling-strategy-form] Computed label: ' + computedLabel);
      //labelControl.setValue(computedLabel);
      // if current date and taxon code are same than stored data, set stored data
      const formTaxon = labelControl.value?.replace(/\s/g, '').toUpperCase().substring(2, 9);
      if (this.data.label && this.data.label.substring(0, 2) === yearMask && this.data.label.substring(2, 9) === formTaxon && formTaxon === label) {
        // Complete label with '___' when increment isn't set in order to throw a warning in validator
        if (this.data.label.length === 9) {
          labelControl.setValue(this.data.label + '___');
        }
        else {
          labelControl.setValue(this.data.label);
        }
      } else {
        // Complete label with '___' when increment isn't set in order to throw a warning in validator
        labelControl.setValue(`${yearMask} ${label} ___`);
      }
    }
  }

  async generateLabelIncrementButton() {
    let date: Moment;
    if (typeof this.form.get('year').value === 'string') {
      date = moment(this.form.get('year').value, 'YYYY-MM-DD');
    } else {
      date = this.form.get('year').value;
    }
    if (date.isBefore(moment("1900-12-31T00:00:00.000Z", 'YYYY-MM-DD'))) return;
    const year = date.format('YY');

    const inputLabel = this.form.get('label');
    const inputLabelValueWithoutSpaces = inputLabel.value?.replace(/\s/g, '').toUpperCase();
    let label = '';
    if (inputLabelValueWithoutSpaces.substring(2, 9))
    {
      // Label automatically or manually set
      label = inputLabelValueWithoutSpaces.substring(2, 9);
    }
    else {
      if (this.taxonNamesHelper.at(0).value?.taxonName) {
        label = TaxonUtils.generateLabelFromName(this.taxonNamesHelper.at(0).value?.taxonName?.name);
        if (!label) {
          this.taxonNamesHelper.at(0).setErrors({cannotComputeTaxonCode: true});
          return;
        }
        const isUnique = await this.isTaxonNameUnique(label, this.taxonNamesHelper.at(0).value?.taxonName?.id);
        if (!isUnique) {
          this.taxonNamesHelper.at(0).setErrors({uniqueTaxonCode: true});
          return;
        }
      }
    }
      // if current date and taxon code are same than stored data, set stored data
      // if (this.data.label && this.data.label.substring(0, 2) === year && this.data.label.substring(2, 9) === label.toUpperCase()) {
      //   this.form.get('label').setValue(this.data.label);
      // } else {
        this.form.get('label').setValue(await this.strategyService.computeNextLabel(this.program.id, year + label.toUpperCase(), 3));
      // }
      SharedValidators.clearError(this.taxonNamesHelper.at(0), 'uniqueTaxonCode');
      SharedValidators.clearError(this.taxonNamesHelper.at(0), 'cannotComputeTaxonCode');
  }

  // TaxonName Helper -----------------------------------------------------------------------------------------------
  protected initTaxonNameHelper() {
    // appliedStrategies => appliedStrategies.location ?
    this.taxonNamesHelper = new FormArrayHelper<TaxonNameStrategy>(
      FormArrayHelper.getOrCreateArray(this.formBuilder, this.form, 'taxonNames'),
      (ts) => this.validatorService.getTaxonNameStrategyControl(ts),
      (t1, t2) => EntityUtils.equals(t1.taxonName, t2.taxonName, 'name'),
      value => isNil(value) && isNil(value.taxonName),
      {
        allowEmptyArray: false
      }
    );
    // Create at least one fishing Area
    if (this.taxonNamesHelper.size() === 0) {
      this.taxonNamesHelper.resize(1);
    }
  }

  // appliedStrategies Helper -----------------------------------------------------------------------------------------------
  protected initAppliedStrategiesHelper() {
    // appliedStrategiesHelper formControl can't have common validator since quarters efforts are optional
    this.appliedStrategiesHelper = new FormArrayHelper<AppliedStrategy>(
      FormArrayHelper.getOrCreateArray(this.formBuilder, this.form, 'appliedStrategies'),
      (appliedStrategy) => this.validatorService.getAppliedStrategiesControl(appliedStrategy),
      (s1, s2) => EntityUtils.equals(s1.location, s2.location, 'label'),
      value => isNil(value) && isNil(value.location),
      {
        allowEmptyArray: false
      }
    );
    // Create at least one fishing Area
    if (this.appliedStrategiesHelper.size() === 0) {
      this.appliedStrategiesHelper.resize(1);
    }
  }

  addAppliedStrategy() {
    this.appliedStrategiesHelper.add(new AppliedStrategy());
  }

  addLengthPmfm() {
    this.lengthPmfmsHelper.add(new PmfmStrategy());
  }

  addWeightPmfm() {
    this.weightPmfmsHelper.add(new PmfmStrategy());
  }

  addMaturityPmfm() {
    this.maturityPmfmsHelper.add(new PmfmStrategy());
  }

  removeLengthPmfm(idx: number) {
    this.lengthPmfmsHelper.removeAt(idx);
    this.validatePmfmsForm();
  }

  removeWeightPmfm(idx: number) {
    this.weightPmfmsHelper.removeAt(idx);
    this.validatePmfmsForm();
  }

  removeMaturityPmfm(idx: number) {
    this.maturityPmfmsHelper.removeAt(idx);
    this.validatePmfmsForm();
  }

  // appliedStrategies Helper -----------------------------------------------------------------------------------------------
  protected initAppliedPeriodHelper() {
    // Use the first applied strategy form group (created just before)
    const appliedStrategyForm = this.appliedStrategiesHelper.at(0) as FormGroup;

    // appliedStrategiesHelper formControl can't have common validator since quarters efforts are optional
    this.appliedPeriodsHelper = new FormArrayHelper<AppliedPeriod>(
      FormArrayHelper.getOrCreateArray(this.formBuilder, appliedStrategyForm, 'appliedPeriods'),
      (appliedPeriod) => this.validatorService.getAppliedPeriodsControl(appliedPeriod),
      (p1, p2) => EntityUtils.equals(p1, p2, 'startDate'),
      value => isNil(value),
      {
        allowEmptyArray: false,
        validators: [
          // this.requiredPeriodMinLength(1)
        ]
      }
    );
    // Create at least one fishing Area
    if (this.appliedStrategiesHelper.size() === 0) {
      this.departmentsHelper.resize(1);
    }
  }

  // Laboratory Helper -----------------------------------------------------------------------------------------------
  protected initDepartmentsHelper() {
    this.departmentsHelper = new FormArrayHelper<StrategyDepartment>(
      FormArrayHelper.getOrCreateArray(this.formBuilder, this.form, 'departments'),
      (department) => this.validatorService.getStrategyDepartmentsControl(department),
      (d1, d2) => EntityUtils.equals(d1.department, d2.department, 'label'),
      value => isNil(value) && isNil(value.department),
      {
        allowEmptyArray: false
      }
    );
    // Create at least one laboratory
    if (this.departmentsHelper.size() === 0) {
      this.departmentsHelper.resize(1);
    }
  }

  addDepartment() {
    this.departmentsHelper.add(new StrategyDepartment());
  }

  protected initPmfmStrategiesHelpers() {
    this.pmfmsHelper = this.createPmfmStrategiesArrayHelper('pmfms', 0);

    this.lengthPmfmsHelper = this.createPmfmStrategiesArrayHelper('lengthPmfms', 1);
    this.weightPmfmsHelper = this.createPmfmStrategiesArrayHelper('weightPmfms', 1);
    this.maturityPmfmsHelper = this.createPmfmStrategiesArrayHelper('maturityPmfms', 1);
    this.fractionPmfmsHelper = this.createPmfmStrategiesArrayHelper('fractionPmfms', 1);
  }

  protected createPmfmStrategiesArrayHelper(arrayName: string, minSize?: number): FormArrayHelper<PmfmStrategy> {
    const helper = new FormArrayHelper<PmfmStrategy>(
      FormArrayHelper.getOrCreateArray(this.formBuilder, this.form, arrayName),
      (data) => this.pmfmStrategyValidator.getFormGroup(data, {withDetails: false, required: false}),
      PmfmStrategy.equals,
      PmfmStrategy.isEmpty,
      {
        allowEmptyArray: false
      }
    );
    // Create at least one fishing Area
    if (minSize && helper.size() < minSize) {
      helper.resize(minSize);
    }
    return helper;
  }

  addPmfmFraction() {
    this.fractionPmfmsHelper.add();
  }

  protected markForCheck() {
    if (this.cd) this.cd.markForCheck();
  }

  protected canUserWrite(): boolean {
    return this.strategyService.canUserWrite(this.data);
  }

  requiredPeriodMinLength(minLength?: number): ValidatorFn {
    minLength = minLength || 1;
    return (array: FormArray): ValidationErrors | null => {
      const values = array.value.flat().filter(period => period.acquisitionNumber !== undefined && period.acquisitionNumber !== null && period.acquisitionNumber >= 1);
      if (!values || values.length < minLength) {
        return { minLength: { minLength: minLength } };
      }
      return null;
    };
  }

  isGenerateLabelButtonDisable(): boolean {
    return this.hasLanding;
  }

  isDepartmentDisable(index: number): boolean {
    return this.departmentsHelper.at(index).status === "DISABLED";
  }

  isLocationDisable(index: number): boolean {
    return this.appliedStrategiesHelper.at(index).status === "DISABLED" || this.hasLanding;
  }

  isFractionDisable(index: number): boolean {
    return this.fractionPmfmsHelper.at(index).status === "DISABLED";
  }

  isLengthPmfmDisable(index: number): boolean {
    return this.lengthPmfmsHelper.at(index).status === "DISABLED";
  }

  isWeightPmfmDisable(index: number): boolean {
    return this.weightPmfmsHelper.at(index).status === "DISABLED";
  }

  isMaturityPmfmDisable(index: number): boolean {
    return this.maturityPmfmsHelper.at(index).status === "DISABLED";
  }

  markAsDirty() {
    this.form.markAsDirty();
  }

  /**
   * get pmfm by type
   * @param pmfms
   * @param pmfmIds
   * @param parameterLabels
   * @protected
   */
  protected getPmfmStrategiesByGroup(pmfms: PmfmStrategy[], pmfmIds: number[], parameterLabels: string[]) {
    return (pmfms || []).filter(p => {
      if (p) {
        const pmfmId = toNumber(p.pmfmId, p.pmfm && p.pmfm.id);
        const hasParameterId = p.parameter && p.parameter.label && parameterLabels.includes(p.parameter.label);
        return pmfmIds.includes(pmfmId) || hasParameterId;
      }
      return false;
    });
  }

  protected async validatePmfmsForm(): Promise<ValidationErrors | null> {
    // DEBUG
    //console.debug('DEV Call validatePmfmsForm()...');

    const weightPmfmsCount = (this.weightPmfmsForm.value || []).filter(PmfmStrategy.isNotEmpty).length;
    const lengthPmfmsCount = (this.lengthPmfmsForm.value || []).filter(PmfmStrategy.isNotEmpty).length;
    const maturityPmfmsCount = (this.maturityPmfmsForm.value || []).filter(PmfmStrategy.isNotEmpty).length;

    let errors: ValidationErrors;

    // Check weight OR length is present
    if (weightPmfmsCount === 0 && lengthPmfmsCount === 0 ) {
      errors = {
        weightOrSize: true
      };
    }
    else {
      SharedValidators.clearError(this.pmfmsForm, 'weightOrSize');
    }

    let length = (this.hasAge ? 1 : 0)
      + (this.hasSex ? (1 + maturityPmfmsCount) : 0)
      + weightPmfmsCount
      + lengthPmfmsCount;

    if (length < this.minPmfmCount) {
      errors = {
        ...errors,
        minLength: { minLength: this.minPmfmCount }
      };
    }
    else {
      SharedValidators.clearError(this.pmfmsForm, 'minLength');
    }
    this.pmfmsForm.setErrors(errors);
    if (errors) {
      this.pmfmsForm.markAllAsTouched();
      this.pmfmsForm.markAsDirty();
    }
    return null;
  }

  selectInputContent = AppFormUtils.selectInputContent;

}
