import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnInit, ViewChild } from '@angular/core';
import { FormArray, FormBuilder, FormControl, FormGroup, ValidationErrors, ValidatorFn } from '@angular/forms';
import { DateAdapter } from '@angular/material/core';
import * as momentImported from 'moment';
import { Moment } from 'moment';
import {
  AppForm,
  AppFormUtils,
  DEFAULT_PLACEHOLDER_CHAR,
  EntityUtils,
  firstArrayValue,
  firstNotNilPromise,
  FormArrayHelper,
  fromDateISOString,
  IAppForm,
  IReferentialRef,
  isEmptyArray,
  isNil,
  isNilOrBlank,
  isNotEmptyArray,
  isNotNil,
  isNotNilOrBlank,
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
import { TaxonNameRef, TaxonUtils } from '../../services/model/taxon.model';
import { ReferentialRefService } from '../../services/referential-ref.service';
import { StrategyService } from '../../services/strategy.service';
import { StrategyValidatorService } from '../../services/validator/strategy.validator';
import { PmfmStrategiesTable } from '../pmfm-strategies.table';
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
import { BehaviorSubject, combineLatest, merge } from 'rxjs';
import { SamplingStrategyService } from '../../services/sampling-strategy.service';
import { PmfmFilter, PmfmService } from '../../services/pmfm.service';
import { SamplingStrategy, StrategyEffort } from '@app/referential/services/model/sampling-strategy.model';
import { TaxonName } from '@app/referential/services/model/taxon-name.model';
import { TaxonNameService } from '@app/referential/services/taxon-name.service';
import { debounceTime, map } from 'rxjs/operators';
import { PmfmStrategyValidatorService } from '@app/referential/services/validator/pmfm-strategy.validator';
import {Pmfm} from '@app/referential/services/model/pmfm.model';

const moment = momentImported;

type FilterableFieldName = 'analyticReference' | 'location' | 'taxonName' | 'department' | 'fraction' | 'lengthPmfm' | 'weightPmfm' | 'maturityPmfm';

const MIN_PMFM_COUNT = 2;

@Component({
  selector: 'app-sampling-strategy-form',
  templateUrl: './sampling-strategy.form.html',
  styleUrls: ['./sampling-strategy.form.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SamplingStrategyForm extends AppForm<Strategy> implements OnInit {

  private _$pmfmGroups: BehaviorSubject<ObjectMap<number[]>> = new BehaviorSubject(null);

  mobile: boolean;
  $program = new BehaviorSubject<Program>(null);
  labelMask: (string | RegExp)[] = [/\d/, /\d/, ' ', /^[a-zA-Z]$/, /^[a-zA-Z]$/, /^[a-zA-Z]$/, /^[a-zA-Z]$/, /^[a-zA-Z]$/, /^[a-zA-Z]$/, /^[a-zA-Z]$/, ' ', /\d/, /\d/, /\d/];
  data: SamplingStrategy;

  hasEffort = false;
  hasLanding = false;

  taxonNamesHelper: FormArrayHelper<TaxonNameStrategy>;
  departmentsHelper: FormArrayHelper<StrategyDepartment>;
  appliedStrategiesHelper: FormArrayHelper<AppliedStrategy>;
  lengthPmfmsHelper: FormArrayHelper<PmfmStrategy>;
  weightPmfmsHelper: FormArrayHelper<PmfmStrategy>;
  maturityPmfmsHelper: FormArrayHelper<PmfmStrategy>;
  appliedPeriodsHelper: FormArrayHelper<AppliedPeriod>;
  pmfmsHelper: FormArrayHelper<PmfmStrategy>;
  calcifiedFractionsHelper: FormArrayHelper<PmfmStrategy>;
  pmfmStrategiesHelper: FormArrayHelper<PmfmStrategy>;
  locationLevelIds: number[];

  autocompleteFilters = {
    analyticReference: false,
    location: false,
    lengthPmfm: false,
    weightPmfm: false,
    maturityPmfm: false,
    taxonName: false,
    department: false,
    fraction: false
  };

  readonly pmfmFilters = {
    weight: <PmfmFilter>{
      levelLabels: ParameterLabelGroups.WEIGHT
    },
    length: <PmfmFilter>{
      levelLabels: ParameterLabelGroups.LENGTH
    },
    maturity: <PmfmFilter>{
      levelLabels: ParameterLabelGroups.MATURITY
    }
  };
  private analyticsReferencePatched: boolean;
  private fillEffortsCalled: boolean;

  get value(): any {
    throw new Error("Not implemented! Please use getValue() instead, that is an async function");
  }

  @Input() set program(value: Program) {
    this.setProgram(value);
  }

  get program(): Program {
    return this.$program.getValue();
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

  get pmfmsFractionForm(): FormArray {
    return this.form.controls.pmfmsFraction as FormArray;
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

  analyticsReferenceItems: BehaviorSubject<ReferentialRef[]> = new BehaviorSubject(null);
  locationItems: BehaviorSubject<ReferentialRef[]> = new BehaviorSubject(null);
  departmentItems: BehaviorSubject<ReferentialRef[]> = new BehaviorSubject(null);
  fractionItems: BehaviorSubject<ReferentialRef[]> = new BehaviorSubject(null);
  lengthPmfmsItems: BehaviorSubject<ReferentialRef[]> = new BehaviorSubject(null);
  weightPmfmsItems: BehaviorSubject<ReferentialRef[]> = new BehaviorSubject(null);
  maturityPmfmsItems: BehaviorSubject<ReferentialRef[]> = new BehaviorSubject(null);
  taxonNameItems: BehaviorSubject<TaxonNameRef[]> = new BehaviorSubject(null);

  allFractionItems: BehaviorSubject<ReferentialRef[]> = new BehaviorSubject(null);


  enable(opts?: { onlySelf?: boolean, emitEvent?: boolean; }) {
    super.enable(opts);

    // disable whole form or form part
    if (!this.canUserWrite()) {
      this.disable();
      // FIXME fractions not disabled
      this.calcifiedFractionsHelper.disable();
    } else if (this.hasLanding) {
      this.taxonNamesFormArray.disable();
      this.appliedStrategiesForm.disable();
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
    this.initPmfmStrategiesHelper();
    this.initAppliedStrategiesHelper();
    this.initAppliedPeriodHelper();
    this.initCalcifiedFractionsHelper();
    this.initLengthPmfmsHelper();
    this.initWeightPmfmsHelper();
    this.initMaturityPmfmsHelper();
  }


  ngOnInit() {
    super.ngOnInit();
    this.fillEffortsCalled = false;
    this.analyticsReferencePatched = false;

    this.referentialRefService.loadAll(0, 0, null, null, {
      entityName: 'Fraction',
      // TODO BLA: Ne faut-il pas filtrer sur les fractions sur Individu ?
      // levelId: MatrixIds.INDIVIDUAL
    })
      .then(({data}) => this.allFractionItems.next(data));

    this.pmfmService.loadIdsGroupByParameterLabels(ParameterLabelGroups)
      .then(map => this._$pmfmGroups.next(map));

    this.registerSubscription(this.form.get('age').valueChanges.subscribe(_ => this.loadFraction()));
    this.taxonNamesFormArray.setAsyncValidators([async (_) => {
        this.loadFraction();
        return null;
      }
    ]);

    this.appliedPeriodsForm.setAsyncValidators([
      async (control) => {
        const minLength = 1;
        const appliedPeriods = control.value;
        if (!isEmptyArray(appliedPeriods)) {
          const values = appliedPeriods.filter(appliedPeriod => toNumber(appliedPeriod.acquisitionNumber, 0) >= 1);
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

    this.pmfmsForm.setAsyncValidators(form => this.validatePmfmsForm(form as FormArray));
    this.lengthPmfmsForm.setAsyncValidators(form => this.validatePmfmsForm(form as FormArray));
    this.weightPmfmsForm.setAsyncValidators(form => this.validatePmfmsForm(form as FormArray));
    this.maturityPmfmsForm.setAsyncValidators(form => this.validatePmfmsForm(form as FormArray));

    // Force pmfms validation, when sex/age changes
    this.registerSubscription(
      merge(
        this.form.get('sex').valueChanges,
        this.form.get('age').valueChanges
      ).subscribe(() => {
        this.pmfmsForm.updateValueAndValidity();
        this.validatePmfmsForm(this.pmfmsForm);
      })
    );

    this.registerSubscription(this.form.get('label').valueChanges.subscribe(value => this.onEditLabel(value)));
    // register year field changes
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
      suggestFn: (value, filter) => this.suggestTaxonName(value, {
        ...filter,
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

    // Fraction autocomplete
    this.registerAutocompleteField('fraction', {
      suggestFn: (value, filter) => this.suggestAgeFractions(value, {
        ...filter,
        statusIds: [StatusIds.ENABLE, StatusIds.TEMPORARY],
        includedIds: FractionIdGroups.CALCIFIED_STRUCTURE
      }),
      attributes: ['name'],
      columnNames: ['REFERENTIAL.NAME'],
      mobile: this.settings.mobile
    });

    // appliedStrategy autocomplete
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
  }


  protected async setProgram(program: Program, opts?: { emitEvent?: boolean; }) {
    if (program && this.program !== program) {
      this.i18nFieldPrefix = 'PROGRAM.STRATEGY.EDIT.';
      const i18nSuffix = program.getProperty(ProgramProperties.I18N_SUFFIX) || '';
      this.i18nFieldPrefix += i18nSuffix !== 'legacy' && i18nSuffix || '';

      // Get location level ids
      this.locationLevelIds = program.getPropertyAsNumbers(ProgramProperties.STRATEGY_EDITOR_LOCATION_LEVEL_IDS);

      // Load items from historical data
      await this.loadFilteredItems(program);

      this.$program.next(program);

      if (!opts || opts.emitEvent !== false) {
        this.markForCheck();
      }
    }
  }

  loadFraction(): void {
    if (this.hasAge() && this.taxonNamesFormArray.value && this.taxonNamesFormArray.value[0]) {
      const taxon = this.taxonNamesFormArray.value[0];
      const fractionName = autoCompleteFractions[taxon.taxonName.id];
      if (fractionName) {
        const fraction = this.allFractionItems.value.find(f => f.label.toUpperCase() === fractionName.toUpperCase());
        this.pmfmsFractionForm.patchValue([fraction]);
      }
    }
  }

  async loadFilteredItems(program: Program): Promise<void> {
    // Get load options, from program properties
    const autoEnableFilter = program.getPropertyAsBoolean(ProgramProperties.STRATEGY_EDITOR_PREDOC_ENABLE);
    const fetchSize = program.getPropertyAsInt(ProgramProperties.STRATEGY_EDITOR_PREDOC_FETCH_SIZE);

    // Departments
    const departments = await this.strategyService.loadStrategiesReferentials(program.id, 'Department', undefined, 0, fetchSize);
    this.departmentItems.next(departments);
    this.autocompleteFilters.department = isNotEmptyArray(departments) && autoEnableFilter; // Enable filtering, if need by program

    // Locations
    const locations = await this.strategyService.loadStrategiesReferentials(program.id, 'Location', 'SEA', 0, fetchSize);
    this.locationItems.next(locations);
    this.autocompleteFilters.location = isNotEmptyArray(locations) && autoEnableFilter; // Enable filtering, if need by program

    // Taxons
    const taxons = await this.strategyService.loadStrategiesReferentials(program.id, 'TaxonName', undefined, 0, fetchSize) as TaxonNameRef[];
    this.taxonNameItems.next(taxons);
    this.autocompleteFilters.taxonName = isNotEmptyArray(taxons) && autoEnableFilter; // Enable filtering, if need by program

    // Fractions
    const fractions = await this.strategyService.loadStrategiesReferentials(program.id, 'Fraction', undefined, 0, fetchSize);
    this.fractionItems.next(fractions);
    this.autocompleteFilters.fraction = isNotEmptyArray(fractions) && autoEnableFilter; // Enable filtering, if need by program

    // Length pmfms
    const lengthPmfms = await this.strategyService.loadStrategiesReferentials(program.id, 'Pmfm', undefined, 0, fetchSize);
    this.lengthPmfmsItems.next(lengthPmfms);
    this.autocompleteFilters.lengthPmfm = isNotEmptyArray(lengthPmfms) && autoEnableFilter; // Enable filtering, if need by program

    // Weight pmfms
    const weightPmfms = await this.strategyService.loadStrategiesReferentials(program.id, 'Pmfm', undefined, 0, fetchSize);
    this.weightPmfmsItems.next(weightPmfms);
    this.autocompleteFilters.weightPmfm = isNotEmptyArray(weightPmfms) && autoEnableFilter; // Enable filtering, if need by program

    // Maturity pmfms
    const maturityPmfms = await this.strategyService.loadStrategiesReferentials(program.id, 'Pmfm', undefined, 0, fetchSize);
    this.maturityPmfmsItems.next(maturityPmfms);
    this.autocompleteFilters.maturityPmfm = isNotEmptyArray(maturityPmfms) && autoEnableFilter; // Enable filtering, if need by program

    // Analytic References
    try {
      const analyticReferences = await this.strategyService.loadStrategiesReferentials(program.id, 'AnalyticReference', undefined, 0, fetchSize);
      this.analyticsReferenceItems.next(removeDuplicatesFromArray(analyticReferences, 'id'));
      this.autocompleteFilters.analyticReference = isNotEmptyArray(analyticReferences) && autoEnableFilter; // Enable filtering, if need by program
    } catch (err) {
      console.debug('Error on load AnalyticReference');
    }
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
  protected async suggestLocations(value: string, filter: any): Promise<LoadResult<IReferentialRef>> {
    filter = {
      levelIds: this.locationLevelIds || [LocationLevelIds.ICES_DIVISION],
      ...filter
    }
    // DEBUG
    //console.debug("Suggest locations: ", filter);
    if (this.autocompleteFilters.location) {
      return suggestFromArray(this.locationItems.getValue(), value, filter);
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
      return suggestFromArray(this.analyticsReferenceItems.getValue(), value, filter);
    } else {
      return this.strategyService.suggestAnalyticReferences(value, filter);
    }
  }

  /**
   * Suggest autocomplete values, for age fraction
   * @param value
   * @param filter - filters to apply
   */
  protected async suggestAgeFractions(value: string, filter: any): Promise<LoadResult<IReferentialRef>> {
    if (this.autocompleteFilters.fraction) {
      return suggestFromArray(this.fractionItems.getValue(), value, filter);
    } else {
      return this.referentialRefService.suggest(value, {
        ...filter,
        entityName: 'Fraction'
      });
    }
  }

  /**
   * Suggest autocomplete values, for length pmfms
   * @param value
   * @param filter - filters to apply
   */
  protected async suggestLengthPmfms(value: string, filter: any): Promise<LoadResult<IReferentialRef>> {
    /*if (this.autocompleteFilters.lengthPmfm) {
      return suggestFromArray(this.lengthPmfmsItems.getValue(), value, filter);
    } else*/ {
      return this.pmfmService.suggest(value, {
        ...filter,
        entityName: 'Pmfm'
      });
    }
  }

  /**
   * Suggest autocomplete values, for weight pmfms
   * @param value
   * @param filter - filters to apply
   */
  protected async suggestWeightPmfms(value: string, filter: any): Promise<LoadResult<IReferentialRef>> {
    /*if (this.autocompleteFilters.weightPmfm) {
      return suggestFromArray(this.weightPmfmsItems.getValue(), value, filter);
    } else*/ {
      return this.pmfmService.suggest(value, {
        ...filter,
        entityName: 'Pmfm'
      });
    }
  }

  /**
   * Suggest autocomplete values, for maturity pmfms
   * @param value
   * @param filter - filters to apply
   */
  protected async suggestMaturityPmfms(value: string, filter: any): Promise<LoadResult<IReferentialRef>> {
    /*if (this.autocompleteFilters.maturityPmfm) {
      return suggestFromArray(this.maturityPmfmsItems.getValue(), value, filter);
    } else*/ {
      return this.pmfmService.suggest(value, {
        ...filter,
        entityName: 'Pmfm'
      });
    }
  }

  /**
   * Suggest autocomplete values
   * @param value
   * @param filter - filters to apply
   */
  protected async suggestDepartments(value: string, filter: any): Promise<LoadResult<IReferentialRef>> {
    if (this.autocompleteFilters.department) {
      return suggestFromArray(this.departmentItems.getValue(), value, filter);
    } else {
      return this.referentialRefService.suggest(value, {
        ...filter,
        entityName: 'Department'
      });
    }
  }

  protected async suggestTaxonName(value: string, filter: any): Promise<LoadResult<TaxonNameRef>> {
    if (this.autocompleteFilters.taxonName) {
      return suggestFromArray(this.taxonNameItems.getValue(), value, filter);
    } else {
      return this.referentialRefService.suggestTaxonNames(value,
        {
          ...filter,
          entityName: 'TaxonName'
        },
      );
    }
  }

  setValue(data: Strategy, opts?: { emitEvent?: boolean; onlySelf?: boolean }) {
    console.debug("[sampling-strategy-form] Setting Strategy value", data);
    if (!data) return;

    this.data = new SamplingStrategy();
    this.data.fromObject(data);

    // Fill efforts (need by validator)
    this.samplingStrategyService.fillEfforts([this.data]).then((test) => {
      this.hasEffort = this.data.hasRealizedEffort;
      this.hasLanding = this.data.hasLanding;
      this.enable();
      this.fillEffortsCalled = true;
    });

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

    super.setValue(data, opts);

    // Get fisrt period
    const firstAppliedPeriod = firstArrayValue(appliedStrategyWithPeriods.appliedPeriods);

    this.getAnalyticReferenceByLabel(data.analyticReference).then(data => {
      this.form.patchValue({
        year: firstAppliedPeriod ? firstAppliedPeriod.startDate : moment(),
        analyticReference: data && { label: data.label, name: data.name } || null
      });
      this.analyticsReferencePatched = true;
    });

    // If new
    if (isNil(data.id)) {
      // pmfms = [null, null];
      this.form.get('sex').patchValue(null);
      this.form.get('age').patchValue(null);
    } else {
      // pmfms = [hasSex, hasAge];
      this.form.get('age').patchValue((data.pmfms || []).findIndex(p => p.pmfmId && p.pmfmId === PmfmIds.AGE) !== -1);
      this.form.get('sex').patchValue((data.pmfms || []).findIndex(p => p.pmfmId && p.pmfmId === PmfmIds.SEX) !== -1);
      const displayedLabel = data.label.substr(0, 2).concat(' ').concat(data.label.substr(2, 7)).concat(' ').concat(data.label.substr(9, 3));
      this.form.get('label').patchValue(displayedLabel);
    }



    data.pmfms = isNotEmptyArray(data.pmfms) ? data.pmfms : [new PmfmStrategy()];

    firstNotNilPromise(this._$pmfmGroups).then((pmfmGroups) => {
      // LENGTH
      const lengthPmfmStrategies = this.getPmfmsByType(data.pmfms, pmfmGroups.LENGTH, ParameterLabelGroups.LENGTH);
      this.lengthPmfmsHelper.resize(Math.max(1, lengthPmfmStrategies.length));
      const lengths = lengthPmfmStrategies.map(pmfm => {
        return {
          id: pmfm.pmfm?.id,
          name: pmfm.pmfm?.name,
        };
      });
      this.lengthPmfmsForm.patchValue(lengths);

      //WEIGHT
      const weightPmfmStrategy = this.getPmfmsByType(data.pmfms, pmfmGroups.WEIGHT, ParameterLabelGroups.WEIGHT);
      this.weightPmfmsHelper.resize(Math.max(1, weightPmfmStrategy.length));
      const weights = weightPmfmStrategy.map(pmfm => {
        return {
          id: pmfm.pmfm?.id,
          name: pmfm.pmfm?.name,
        };
      });
      this.weightPmfmsForm.patchValue(weights);

      // MATURITY
      const maturityPmfmStrategies = this.getPmfmsByType(data.pmfms, pmfmGroups.MATURITY, ParameterLabelGroups.MATURITY);
      this.maturityPmfmsHelper.resize(Math.max(1, maturityPmfmStrategies.length));
      const maturities = maturityPmfmStrategies.map(pmfm => {
        return {
          id: pmfm.pmfm?.id,
          name: pmfm.pmfm?.name,
        };
      });
      this.maturityPmfmsForm.patchValue(maturities);
    });

    this.referentialRefService.loadAll(0, 1000, null, null,
      {
        entityName: 'Fraction'
      },
      { withTotal: false /* total not need */ }
    ).then(res => {
      const calcifiedTypeControl = this.pmfmsFractionForm;
      const pmfmStrategiesWithFraction = (data.pmfms || []).filter(p => p.fraction && !p.pmfm);
      const fractions = pmfmStrategiesWithFraction.map(cal => {
        return {
          id: cal.fraction.id,
          name: res.data.find(fraction => fraction.id === cal.fraction.id).name,
        };
      });
      calcifiedTypeControl.clear();
      this.calcifiedFractionsHelper.resize(Math.max(1, pmfmStrategiesWithFraction.length));
      calcifiedTypeControl.patchValue(fractions);
    });
  }

  async getValue(): Promise<Strategy> {
    const json = this.form.getRawValue();
    const target = Strategy.fromObject(json);

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
    let pmfmStrategies: any[] = [
      // Add tag id Pmfm
      PmfmStrategy.fromObject(<PmfmStrategy>{ pmfm: { id: PmfmIds.TAG_ID } }),
      // Add dressing Pmfm
      PmfmStrategy.fromObject(<PmfmStrategy>{ pmfm: { id: PmfmIds.DRESSING } }),
    ];
    const weightPmfms = (this.weightPmfmsForm.value.flat() as PmfmStrategy[] || []).filter(elt => elt);
    const lengthPmfms = (this.lengthPmfmsForm.value.flat() as PmfmStrategy[] || []).filter(elt => elt);
    const maturityPmfms = (this.maturityPmfmsForm.value.flat() as PmfmStrategy[] || []).filter(elt => elt);

    // Add weights Pmfm
    weightPmfms.map(pmfm => pmfmStrategies.push(PmfmStrategy.fromObject(<PmfmStrategy>{ pmfm: { id: pmfm.id } })));
    // Add length Pmfm
    lengthPmfms.map(pmfm => pmfmStrategies.push(PmfmStrategy.fromObject(<PmfmStrategy>{ pmfm: { id: pmfm.id } })));

    // Add SEX Pmfm
    if (this.hasSex()) {
      pmfmStrategies.push(PmfmStrategy.fromObject(<PmfmStrategy>{ pmfm: { id: PmfmIds.SEX } }));
      // Add maturity pmfms
      maturityPmfms.map(pmfm => pmfmStrategies.push(PmfmStrategy.fromObject(<PmfmStrategy>{ pmfm: { id: pmfm.id } })));
    }

    // Add AGE Pmfm
    if (this.hasAge()) {
      pmfmStrategies.push(PmfmStrategy.fromObject(<PmfmStrategy>{ pmfm: { id: PmfmIds.AGE } }));

      // Pièces calcifiées
      (json.pmfmsFraction || [])
        .map(fraction => <PmfmStrategy>{ fraction })
        .filter(isNotNil)
        .forEach(pmfm => pmfmStrategies.push(PmfmStrategy.fromObject(pmfm)));
    }

    // Fill PmfmStrategy defaults
    let rankOrder = 1;
    target.pmfms = pmfmStrategies
      // .map(PmfmStrategy.fromObject)
      .map(pmfmStrategy => {
        pmfmStrategy.strategyId = pmfmStrategy.id;
        pmfmStrategy.acquisitionLevel = AcquisitionLevelCodes.SAMPLE;
        pmfmStrategy.acquisitionNumber = 1;
        pmfmStrategy.isMandatory = false;
        pmfmStrategy.rankOrder = rankOrder++;
        return pmfmStrategy;
      })
      // Remove if empty
      .filter(p => isNotNil(p.pmfm) || isNotNil(p.parameter) || isNotNil(p.matrix) || isNotNil(p.fraction) || isNotNil(p.method));


    return target;
  }

  protected async onEditLabel(value: string) {
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

  private async isTaxonNameUnique(label: string, currentViewTaxonId?: number) {
    const taxonNamesItems: BehaviorSubject<ReferentialRef[]> = new BehaviorSubject(null);
    const taxonNamesWithParentheseItems: BehaviorSubject<ReferentialRef[]> = new BehaviorSubject(null);
    let isUnique = true;
    if (label) {
      await this.referentialRefService.loadAll(0, 1000, null, null, {
        entityName: TaxonName.ENTITY_NAME,
        searchText: TaxonUtils.generateNameSearchPatternFromLabel(label),
        searchAttribute: 'name',
        excludedIds: [currentViewTaxonId],
        statusIds: [StatusIds.ENABLE],
        levelIds: [TaxonomicLevelIds.SPECIES, TaxonomicLevelIds.SUBSPECIES]
      }).then(({data}) => taxonNamesItems.next(data));

      if (taxonNamesItems && taxonNamesItems.value)
      {
        const filteredReferentTaxons = (await Promise.all(taxonNamesItems.value.map(taxonRef => (this.taxonNameService.load(taxonRef.id))))).filter(taxon => taxon.isReferent);
        if (!(filteredReferentTaxons === null || filteredReferentTaxons.length === 0))
        {
          isUnique = false;
        }
        else {
          // IMAGINE-511 - add a control on taxon unicity searching in taxon with parentheses
          // should be replaced by generateNameSearchPatternFromLabel managing optional parentheses in searchText parameter
          await this.referentialRefService.loadAll(0, 1000, null, null, {
            entityName: TaxonName.ENTITY_NAME,
            searchText: TaxonUtils.generateNameSearchPatternFromLabel(label, true),
            searchAttribute: 'name',
            excludedIds: [currentViewTaxonId],
            statusIds: [StatusIds.ENABLE],
            levelIds: [TaxonomicLevelIds.SPECIES, TaxonomicLevelIds.SUBSPECIES]
          }).then(({data}) => taxonNamesWithParentheseItems.next(data));
          const filteredReferentTaxonsWithParenthese = (await Promise.all(taxonNamesWithParentheseItems.value.map(taxonRef => (this.taxonNameService.load(taxonRef.id))))).filter(taxon => taxon.isReferent);
          if (!(filteredReferentTaxonsWithParenthese === null || filteredReferentTaxonsWithParenthese.length === 0))
          {
            isUnique = false;
          }
        }
      }
    }
    return isUnique;
  }

  protected async onDateChange(date?: Moment) {
    let dateAsMoment: Moment;
    if (typeof date === 'string') {
      dateAsMoment = moment(date, 'YYYY-MM-DD');
    } else {
      dateAsMoment = date;
    }
    if (!dateAsMoment || dateAsMoment.isBefore(moment("1900-12-31T00:00:00.000Z", 'YYYY-MM-DD'))) return;
    const storedDataYear = this.data.appliedStrategies[0]?.appliedPeriods[0]?.startDate ? fromDateISOString(this.data.appliedStrategies[0].appliedPeriods[0].startDate).format('YY') : undefined;
    const formYear = dateAsMoment.format('YY');
    if (storedDataYear === formYear)
    {
      // Don't call label generation when year hasn't changed
    }
    else
    {
      await this.generateLabel(dateAsMoment);
    }
  }

  protected async onTaxonChange() {
    if (!this.program) return; // Skip if program is missing
    const taxonNameControl = this.taxonNamesHelper.at(0);
    const currentViewTaxonName = taxonNameControl?.value?.taxonName?.name;
    const storedDataTaxonName = this.data.taxonNames[0]?.taxonName?.name;
    if (currentViewTaxonName === storedDataTaxonName)
    {
      // Don't call label generation when taxon hasn't changed
    }
    else
    {
      await this.generateLabel();
    }
    // TODO try to limit pmfms, by loading previous sampling strategies ?
  }

  protected async generateLabel(date?: Moment) {
    // Wait for asynchronous functions to be completed.
    if (this.analyticsReferencePatched && this.fillEffortsCalled) {
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
          if (!taxonNameControl.errors)
          {

            if ((this.data.label && this.data.label === labelControl.value) && (storedDataTaxonName && storedDataTaxonName === currentViewTaxonName))
            {
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
          if (this.data.label.length === 9)
          {
            labelControl.setValue(this.data.label + '___');
          }
          else
          {
            labelControl.setValue(this.data.label);
          }
        } else {
          // Complete label with '___' when increment isn't set in order to throw a warning in validator
          labelControl.setValue(`${yearMask} ${label} ___`);
        }
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
    this.validatePmfmsForm(this.pmfmsForm);
  }

  removeWeightPmfm(idx: number) {
    this.weightPmfmsHelper.removeAt(idx);
    this.validatePmfmsForm(this.pmfmsForm);
  }

  removeMaturityPmfm(idx: number) {
    this.maturityPmfmsHelper.removeAt(idx);
    this.validatePmfmsForm(this.pmfmsForm);
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

  protected initPmfmStrategiesHelper(minSize?: number) {
    this.pmfmStrategiesHelper = new FormArrayHelper<PmfmStrategy>(
      FormArrayHelper.getOrCreateArray(this.formBuilder, this.form, 'pmfms'),
      (data) => this.pmfmStrategyValidator.getFormGroup(data),
      (o1, o2) => (isNil(o1) && isNil(o2)) || o1?.equals(o2),
      (o) => !o || (!o.pmfm && !o.parameter),
      {
        allowEmptyArray: false
      }
    );
    // Create at least one fishing Area
    if (minSize && this.pmfmStrategiesHelper.size() < minSize) {
      this.pmfmStrategiesHelper.resize(minSize);
    }
    // Create at least one fishing Area
    if (this.pmfmStrategiesHelper.size() === 0) {
      this.pmfmStrategiesHelper.resize(1);
    }
  }

  // Pièces calcifiées
  protected initCalcifiedFractionsHelper() {
    this.calcifiedFractionsHelper = new FormArrayHelper<PmfmStrategy>(
      FormArrayHelper.getOrCreateArray(this.formBuilder, this.form, 'pmfmsFraction'),
      (pmfmsFraction) => this.formBuilder.control(pmfmsFraction || null, [SharedValidators.entity]),
      ReferentialUtils.equals,
      ReferentialUtils.isEmpty,
      {
        allowEmptyArray: false
      }
    );
    // Create at least one PmfmStrategiesFraction
    if (this.calcifiedFractionsHelper.size() === 0) {
      this.calcifiedFractionsHelper.resize(1);
    }
  }

  // Length pmfms
  protected initLengthPmfmsHelper() {
    this.lengthPmfmsHelper = new FormArrayHelper<PmfmStrategy>(
      FormArrayHelper.getOrCreateArray(this.formBuilder, this.form, 'lengthPmfms'),
      (lengthPmfms) => this.formBuilder.control(lengthPmfms || null, [SharedValidators.entity]),
      ReferentialUtils.equals,
      ReferentialUtils.isEmpty,
      {
        allowEmptyArray: false
      }
    );
    // Create at least one PmfmStrategies
    if (this.lengthPmfmsHelper.size() === 0) {
      this.lengthPmfmsHelper.resize(1);
    }
  }
  // Weight pmfms
  protected initWeightPmfmsHelper() {
    this.weightPmfmsHelper = new FormArrayHelper<PmfmStrategy>(
      FormArrayHelper.getOrCreateArray(this.formBuilder, this.form, 'weightPmfms'),
      (weightPmfms) => this.formBuilder.control(weightPmfms || null, [SharedValidators.entity]),
      ReferentialUtils.equals,
      ReferentialUtils.isEmpty,
      {
        allowEmptyArray: false
      }
    );
    // Create at least one PmfmStrategies
    if (this.weightPmfmsHelper.size() === 0) {
      this.weightPmfmsHelper.resize(1);
    }
  }
  // Maurity pmfms
  protected initMaturityPmfmsHelper() {
    this.maturityPmfmsHelper = new FormArrayHelper<PmfmStrategy>(
      FormArrayHelper.getOrCreateArray(this.formBuilder, this.form, 'maturityPmfms'),
      (maturityPmfms) => this.formBuilder.control(maturityPmfms || null, [SharedValidators.entity]),
      ReferentialUtils.equals,
      ReferentialUtils.isEmpty,
      {
        allowEmptyArray: false
      }
    );
    // Create at least one PmfmStrategies
    if (this.maturityPmfmsHelper.size() === 0) {
      this.maturityPmfmsHelper.resize(1);
    }
  }

  addCalcifiedFraction() {
    this.calcifiedFractionsHelper.add();
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
    return this.calcifiedFractionsHelper.at(index).status === "DISABLED";
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

  hasSex(): boolean {
    return this.form.get('sex').value;
  }

  hasAge(): boolean {
    return this.form.get('age').value;
  }

  markAsDirty() {
    this.form.markAsDirty();
  }

  /**
   * get pmfm by type
   * @param pmfms
   * @param pmfmGroups
   * @param type
   * @protected
   */
  protected getPmfmsByType(pmfms: PmfmStrategy[], pmfmGroups: number[], type: any) {
    return (pmfms || []).filter(p => {
      if (p) {
        const pmfmId = toNumber(p.pmfmId, p.pmfm && p.pmfm.id);
        const hasParameterId = p.parameter && p.parameter.label && type.includes(p.parameter.label);
        return pmfmGroups.includes(pmfmId) || hasParameterId;
      }
      return false;
    });
  }

  protected async validatePmfmsForm(form?: FormArray): Promise<ValidationErrors | null> {
    const weightPmfms = (this.weightPmfmsForm.value.flat() as PmfmStrategy[] || []).filter(elt => elt);
    const lengthPmfms = (this.lengthPmfmsForm.value.flat() as PmfmStrategy[] || []).filter(elt => elt);
    const maturityPmfms = (this.maturityPmfmsForm.value.flat() as PmfmStrategy[] || []).filter(elt => elt);

    let errors: ValidationErrors;

    // Check weight OR length is present
    if (isEmptyArray(weightPmfms) && isEmptyArray(lengthPmfms)) {
      errors = {
        weightOrSize: true
      };
    }
    else {
      SharedValidators.clearError(this.pmfmsForm, 'weightOrSize');
    }

    let length = (this.hasAge() ? 1 : 0)
      + (this.hasSex() ? (1 + maturityPmfms.length) : 0)
      + weightPmfms.length
      + lengthPmfms.length;

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
    this.pmfmsForm.markAllAsTouched();
    this.pmfmsForm.markAsDirty();
    return null;
  }

  selectInputContent = AppFormUtils.selectInputContent;

}
