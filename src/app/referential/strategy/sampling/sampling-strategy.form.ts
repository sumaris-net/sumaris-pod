import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnInit, ViewChild} from '@angular/core';
import {FormArray, FormBuilder, FormControl, ValidationErrors, ValidatorFn} from "@angular/forms";
import {DateAdapter} from "@angular/material/core";
import * as momentImported from "moment";
import {Moment} from 'moment';
import {DEFAULT_PLACEHOLDER_CHAR} from 'src/app/shared/constants';
import {SharedValidators} from 'src/app/shared/validator/validators';
import {LocalSettingsService} from "../../../core/services/local-settings.service";
import {IReferentialRef, ReferentialRef, ReferentialUtils} from "../../../core/services/model/referential.model";
import {fromDateISOString} from "../../../shared/dates";
import {PmfmStrategy} from "../../services/model/pmfm-strategy.model";
import {Program} from '../../services/model/program.model';
import {
  AppliedPeriod,
  AppliedStrategy,
  Strategy,
  StrategyDepartment,
  TaxonNameStrategy
} from "../../services/model/strategy.model";
import {TaxonNameRef} from "../../services/model/taxon.model";
import {ReferentialRefService} from "../../services/referential-ref.service";
import {StrategyService} from "../../services/strategy.service";
import {StrategyValidatorService} from '../../services/validator/strategy.validator';
import {PmfmStrategiesTable} from "../pmfm-strategies.table";
import {
  AcquisitionLevelCodes,
  LocationLevelIds,
  ParameterLabelGroups, PmfmIds,
  ProgramPrivilegeIds, TaxonomicLevelIds
} from '../../services/model/model.enum';
import {AppForm} from "../../../core/form/form.class";
import {AppFormUtils, FormArrayHelper} from "../../../core/form/form.utils";
import {EntityUtils} from "../../../core/services/model/entity.model";
import {PmfmUtils} from "../../services/model/pmfm.model";
import {
  firstArrayValue,
  isEmptyArray,
  isNil,
  isNotEmptyArray,
  isNotNil,
  isNotNilOrBlank,
  removeDuplicatesFromArray,
  suggestFromArray,
  toNumber
} from "../../../shared/functions";
import {StatusIds} from "../../../core/services/model/model.enum";
import {ProgramProperties} from "../../services/config/program.config";
import {BehaviorSubject, merge} from "rxjs";
import {SamplingStrategyService} from '../../services/sampling-strategy.service';
import {PmfmFilter, PmfmService} from "../../services/pmfm.service";
import {firstNotNilPromise} from "../../../shared/observables";
import {MatAutocompleteField} from "../../../shared/material/autocomplete/material.autocomplete";
import { ObjectMap } from 'src/app/shared/types';
import { SamplingStrategy } from '../../services/model/sampling-strategy.model';
import {LoadResult} from "../../../shared/services/entity-service.class";

const moment = momentImported;

@Component({
  selector: 'app-sampling-strategy-form',
  templateUrl: './sampling-strategy.form.html',
  styleUrls: ['./sampling-strategy.form.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SamplingStrategyForm extends AppForm<Strategy> implements OnInit {

  mobile: boolean;
  $program = new BehaviorSubject<Program>(null);
  labelMask: (string | RegExp)[];

  hasEffort = false;

  taxonNamesHelper: FormArrayHelper<TaxonNameStrategy>;
  departmentsHelper: FormArrayHelper<StrategyDepartment>;
  appliedStrategiesHelper: FormArrayHelper<AppliedStrategy>;
  appliedPeriodsHelper: FormArrayHelper<AppliedPeriod>;
  calcifiedFractionsHelper: FormArrayHelper<PmfmStrategy>;
  sexAndAgeHelper: FormArrayHelper<PmfmStrategy>;

  autocompleteFilters = {
    analyticReference: false,
    location: false,
    taxonName: false,
    department: false,
    fraction: false
  };


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

  get departmentsFormArray(): FormArray {
    return this.form.controls.departments as FormArray;
  }

  get taxonNamesFormArray(): FormArray {
    return this.form.controls.taxonNames as FormArray;
  }

  get appliedPeriodsForm(): FormArray {
    return this.form.controls.appliedPeriods as FormArray;
  }

  get pmfmStrategiesForm(): FormArray {
    return this.form.controls.pmfmStrategies as FormArray;
  }

  get pmfmStrategiesFractionForm(): FormArray {
    return this.form.controls.pmfmStrategiesFraction as FormArray;
  }

  @ViewChild('weightPmfmStrategiesTable', { static: true }) weightPmfmStrategiesTable: PmfmStrategiesTable;
  @ViewChild('lengthPmfmStrategiesTable', { static: true }) lengthPmfmStrategiesTable: PmfmStrategiesTable;
  @ViewChild('maturityPmfmStrategiesTable', { static: true }) maturityPmfmStrategiesTable: PmfmStrategiesTable;

  analyticsReferenceItems: BehaviorSubject<ReferentialRef[]> = new BehaviorSubject(null);
  locationItems: BehaviorSubject<ReferentialRef[]> = new BehaviorSubject(null);
  departmentItems: BehaviorSubject<ReferentialRef[]> = new BehaviorSubject(null);
  fractionItems: BehaviorSubject<ReferentialRef[]> = new BehaviorSubject(null);
  taxonNameItems: BehaviorSubject<TaxonNameRef[]> = new BehaviorSubject(null);

  _$pmfmGroups: BehaviorSubject<ObjectMap<number[]>> = new BehaviorSubject(null);

  constructor(
    protected dateAdapter: DateAdapter<Moment>,
    protected validatorService: StrategyValidatorService,
    protected referentialRefService: ReferentialRefService,
    protected samplingStrategyService: SamplingStrategyService,
    protected pmfmService: PmfmService,
    protected strategyService: StrategyService,
    protected settings: LocalSettingsService,
    protected cd: ChangeDetectorRef,
    protected formBuilder: FormBuilder
  ) {
    super(dateAdapter, validatorService.getFormGroup(), settings);
    this.mobile = this.settings.mobile;
  }

  enable(opts?: {onlySelf?: boolean, emitEvent?: boolean; }) {
    super.enable(opts);
    if (this.hasEffort) {
      this.weightPmfmStrategiesTable.disable();
      this.lengthPmfmStrategiesTable.disable();
      this.maturityPmfmStrategiesTable.disable();
      this.taxonNamesFormArray.disable();
      this.form.get('analyticReference').disable();
      this.form.get('year').disable();
      this.form.get('label').disable();
      this.form.get('age').disable();
      this.form.get('sex').disable();
    }
  }

  ngOnInit() {
    super.ngOnInit();

    this.pmfmService.loadIdsGroupByParameterLabels(ParameterLabelGroups).then(map => this._$pmfmGroups.next(map));

    this.registerSubscription(
      merge(
        // Delete a row
        this.weightPmfmStrategiesTable.onCancelOrDeleteRow,
        this.lengthPmfmStrategiesTable.onCancelOrDeleteRow,
        this.maturityPmfmStrategiesTable.onCancelOrDeleteRow,

        // Add a row
        this.weightPmfmStrategiesTable.onConfirmEditCreateRow,
        this.lengthPmfmStrategiesTable.onConfirmEditCreateRow,
        this.maturityPmfmStrategiesTable.onConfirmEditCreateRow
      )
        .subscribe(() => this.initPmfmStrategies())
    );


    this.form.addControl('year', new FormControl());
    this.form.addControl('sex', new FormControl());
    this.form.addControl('age', new FormControl());

    this.form.get('appliedPeriods').setAsyncValidators([
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
        return <ValidationErrors>{ minLength: {minLength} };
      }
    ]);

    this.form.get('pmfmStrategies').setAsyncValidators([
      //Check if WEIGHT or LENGTH
      async (control) => {
          const pmfms = control.value.flat();
          if (!isEmptyArray(pmfms)) {
            const pmfmGroups = await firstNotNilPromise(this._$pmfmGroups);
            const weight = this.getPmfmsByType(pmfms, pmfmGroups.WEIGHT, ParameterLabelGroups.WEIGHT);
            const length = this.getPmfmsByType(pmfms, pmfmGroups.LENGTH, ParameterLabelGroups.LENGTH);
            if (isEmptyArray(weight) && isEmptyArray(length)) {
              return <ValidationErrors>{ weightOrSize: true };
            }
            SharedValidators.clearError(control, 'weightOrSize');
          } else {
            return <ValidationErrors>{ weightOrSize: true };
          }
        }
    ]);

    this.form.setAsyncValidators([
      //Check number of selected pmfms
      async (control) => {
        const minLength = 2;
        const pmfms = control.get('pmfmStrategies').value.flat();
        const sex = control.get('sex').value;
        const age = control.get('age').value;
        let length = 0;
        if (age) length++;
        if (sex) length++;

        if (!isEmptyArray(pmfms)) {
          const pmfmGroups = await firstNotNilPromise(this._$pmfmGroups);
          length += this.getPmfmsByType(pmfms, pmfmGroups.WEIGHT, ParameterLabelGroups.WEIGHT).length;
          length += this.getPmfmsByType(pmfms, pmfmGroups.LENGTH, ParameterLabelGroups.LENGTH).length;
          if (sex) length += this.getPmfmsByType(pmfms, pmfmGroups.MATURITY, ParameterLabelGroups.MATURITY).length;
        }

        if (length < minLength) {
          return <ValidationErrors>{ minLength: {minLength} };
        }
        SharedValidators.clearError(control, 'minLength');
      }
    ]);

    // register year field changes
    this.registerSubscription(
      this.form.get('year').valueChanges.subscribe(date => this.onDateChange(date))
    );

    const idControl = this.form.get('id');
    this.form.get('label').setAsyncValidators([
      async (control) => {
        console.debug('[sampling-strategy-form] Checking of label is unique...');
        const exists = await this.strategyService.existsByLabel(control.value, {
          programId: this.program && this.program.id,
          excludedIds: isNotNil(idControl.value) ? [idControl.value] : undefined,
          fetchPolicy: 'network-only' // Force to check remotely
        });
        if (exists) {
          console.warn('[sampling-strategy-form] Label not unique!');
          return <ValidationErrors>{ unique: true };
        }
        if (control.value.includes('0000')) {
          return <ValidationErrors>{ zero: true };
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
        levelIds: [LocationLevelIds.ICES_DIVISION]
      }),
      mobile: this.settings.mobile
    });

    // eotp combo -------------------------------------------------------------------
    this.registerAutocompleteField('analyticReference', {
      suggestFn: (value, filter) => this.suggestAnalyticReferences(value, {
        ...filter, statusIds: [StatusIds.ENABLE, StatusIds.TEMPORARY]
      }),
      columnSizes: [4, 6],
      mobile: this.settings.mobile
    });

    this.registerAutocompleteField('fraction', {
      suggestFn: (value, filter) => this.suggestAgeFractions(value, {
        ...filter, statusIds: [StatusIds.ENABLE, StatusIds.TEMPORARY]
      }),
      attributes: ['name'],
      columnNames: ['REFERENTIAL.NAME'],
      mobile: this.settings.mobile
    });

    // Init array helpers
    this.initdepartmentHelper();
    this.initTaxonNameHelper();
    this.initPmfmStrategiesHelper();
    this.initAppliedStrategiesHelper();
    this.initAppliedPeriodHelper();
    this.initPmfmStrategiesFractionHelper();

  }


  protected setProgram(program: Program, opts?: { emitEvent?: boolean; }) {
    if (program && this.program !== program) {
      this.i18nFieldPrefix = 'PROGRAM.STRATEGY.EDIT.';
      const i18nSuffix = program.getProperty(ProgramProperties.I18N_SUFFIX) || '';
      this.i18nFieldPrefix += i18nSuffix !== 'legacy' && i18nSuffix || '';

      // Load items from historical data
      this.loadFilteredItems(program);

      this.$program.next(program);

      if (!opts || opts.emitEvent !== false) {
        this.markForCheck();
      }

      // When program is loaded, reload increment
      this.onDateChange();
    }
  }

  async loadFilteredItems(program: Program): Promise<void> {

    // Load historical data
    // TODO BLA: check if sort by label works fine
    const items = await this.samplingStrategyService.loadAll(0, 20, 'label', 'desc', {
      levelId: program.id
    });

    const data = (items.data || []);

    // Departments
    const departments: ReferentialRef[] = removeDuplicatesFromArray(
      data.reduce((res, strategy) =>
        res.concat(...strategy.departments), [])
        .reduce((res, department: StrategyDepartment) => res.concat([department.department]), []),
      'id');
    this.departmentItems.next(departments);

    // Locations
    const locations: ReferentialRef[] = removeDuplicatesFromArray(
      data.reduce((res, strategy) =>
        res.concat(...strategy.appliedStrategies), [])
        .reduce((res, appliedStrategy: AppliedStrategy) =>
          res.concat([appliedStrategy.location]), []),
      'id');
    this.locationItems.next(locations);

    // Taxons
    const taxons: TaxonNameRef[] = removeDuplicatesFromArray(
      data.reduce((res, strategy) =>
        res.concat(...strategy.taxonNames), [])
        .reduce((res, taxonName: TaxonNameStrategy): TaxonNameRef[] =>
          res.concat([taxonName.taxonName]), []),
      'id');
    this.taxonNameItems.next(taxons);

    // Fractions
    const fractionIds: number[] = removeDuplicatesFromArray(data
      .reduce((res, strategy) => res.concat(...strategy.pmfmStrategies), [])
      .reduce((res, pmfmStrategie) => res.concat(pmfmStrategie.fractionId), [])
    );

    const fractions = (
      await Promise.all(
        fractionIds.map(id => this.referentialRefService.loadAll(0, 1, null, null, { id, entityName: 'Fraction' })
          .then(res => res && firstArrayValue(res.data)))
      ))
      .filter(isNotNil);
    this.fractionItems.next(fractions);

    // Analytic References
    try {
      const analyticReferences: ReferentialRef[] = (
        await Promise.all(
          data
            .map(strategy => strategy.analyticReference)
            .filter(isNotNilOrBlank)

            .map(analyticReference =>
              this.strategyService.loadAllAnalyticReferences(0, 1, 'label', 'desc', { label: analyticReference  })
                .then(res => res && firstArrayValue(res.data)))
        ))
        .filter(isNotNil);
      this.analyticsReferenceItems.next(analyticReferences);
    } catch (err) {
      console.debug('Error on load AnalyticReference');
    }
  }

  async initPmfmStrategies() {
    const pmfms = [];

    // TODO BLA: review this code

    await this.weightPmfmStrategiesTable.save();
    await this.lengthPmfmStrategiesTable.save();
    await this.maturityPmfmStrategiesTable.save();

    const weights = this.weightPmfmStrategiesTable.value.filter(p => p.pmfm || toNumber(p.parameterId, null));
    const lengths = this.lengthPmfmStrategiesTable.value.filter(p => p.pmfm || toNumber(p.parameterId, null));
    const maturities = this.maturityPmfmStrategiesTable.value.filter(p => p.pmfm || toNumber(p.parameterId, null));

    // pmfms.push(this.sexAndAgeHelper.at(0).value);
    // pmfms.push(this.sexAndAgeHelper.at(1).value);
    pmfms.push(weights);
    pmfms.push(lengths);
    pmfms.push(maturities);

    if (weights.length <= 0) { this.weightPmfmStrategiesTable.value = [new PmfmStrategy()]; }
    if (lengths.length <= 0) { this.lengthPmfmStrategiesTable.value = [new PmfmStrategy()]; }
    if (maturities.length <= 0) { this.maturityPmfmStrategiesTable.value = [new PmfmStrategy()]; }

    this.form.controls.pmfmStrategies.patchValue(pmfms);
    this.pmfmStrategiesForm.markAsTouched();
    this.markAsDirty();
  }

  /**
   * Select text that can be changed, using the text mask
   * @param input
   */
  selectMask(input: HTMLInputElement) {
    if (!this.labelMask) input.select();
    const startIndex = this.labelMask.findIndex(c => c instanceof RegExp);
    let endIndex = this.labelMask.slice(startIndex).findIndex(c => !(c instanceof RegExp), startIndex);
    endIndex = (endIndex === -1)
      ? this.labelMask.length
      : startIndex + endIndex;
    input.setSelectionRange(startIndex, endIndex, "backward");
  }

  toggleFilter(fieldName: string, field?: MatAutocompleteField) {
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

    const samplingStrategy = new SamplingStrategy();
    samplingStrategy.fromObject(data);
    this.samplingStrategyService.hasEffort(samplingStrategy).then((hasEffort) => {
      this.hasEffort = hasEffort;
      this.enable();
    });


    // Make sure to have (at least) one department
    data.departments = data.departments && data.departments.length ? data.departments : [null];
    // Resize strategy department array
    this.departmentsHelper.resize(Math.max(1, data.departments.length));

    data.appliedStrategies = data.appliedStrategies && data.appliedStrategies.length ? data.appliedStrategies : [null];
    // Resize strategy department array
    this.appliedStrategiesHelper.resize(Math.max(1, data.appliedStrategies.length));

    data.taxonNames = data.taxonNames && data.taxonNames.length ? data.taxonNames : [null];
    // Resize pmfm strategy array
    this.taxonNamesHelper.resize(Math.max(1, data.taxonNames.length));

    // Resize pmfm strategy array
    // this.pmfmStrategiesHelper.resize(Math.max(1, data.pmfmStrategies.length));

    // Resize strategy department array
    this.appliedPeriodsHelper.resize(4);

    // APPLIED_PERIODS
    // get model appliedPeriods which are stored in first applied strategy
    const appliedPeriodControl = this.appliedPeriodsForm;
    const appliedPeriods = (isNotEmptyArray(data.appliedStrategies) && data.appliedStrategies[0] && data.appliedStrategies[0].appliedPeriods) || [];
    const appliedStrategyId = (isNotEmptyArray(data.appliedStrategies) && data.appliedStrategies[0] && data.appliedStrategies[0].strategyId) || undefined;

    const year = moment().year();

    // format periods for applied conrol period in view and init default period by quarter if no set
    // let quarter = 1;
    const formattedAppliedPeriods = [];
    for (let quarter = 1; quarter <= 10; quarter = quarter+3) {
      formattedAppliedPeriods.push(
        appliedPeriods.find(period => (period.startDate.month() + 1) === quarter) || {
          appliedStrategyId: appliedStrategyId,
          startDate: moment(`${year}-${quarter}-01`).startOf('month'),
          endDate: moment(`${year}-${quarter+2}-01`).endOf('month'),
          acquisitionNumber: undefined
        }
      );
    }

    // patch the control value
    appliedPeriodControl.patchValue(formattedAppliedPeriods);

    super.setValue(data, opts);

    // Get fisrt period
    const period = appliedPeriods[0];
    this.form.get('year').patchValue(period ? period.startDate : moment());

    this.form.patchValue({
      analyticReference: { label: data.analyticReference }
    });

    const pmfmStrategiesControl = this.pmfmStrategiesForm;

    // If new
    if (!data.id) {
      // pmfmStrategies = [null, null];
      this.form.get('sex').patchValue(null);
      this.form.get('age').patchValue(null);
    } else {
      this.form.get('age').patchValue((data.pmfmStrategies || []).findIndex(p => PmfmUtils.hasParameterLabelIncludes(p.pmfm, ParameterLabelGroups.AGE)) !== -1);
      this.form.get('sex').patchValue((data.pmfmStrategies || []).findIndex(p => PmfmUtils.hasParameterLabelIncludes(p.pmfm, ParameterLabelGroups.SEX)) !== -1);
      // pmfmStrategies = [hasSex, hasAge];
    }


    const pmfmStrategies = [];

    firstNotNilPromise(this._$pmfmGroups).then((pmfmGroups) => {

      //WEIGHT
      const weightPmfmStrategy = this.getPmfmsByType(data.pmfmStrategies, pmfmGroups.WEIGHT, ParameterLabelGroups.WEIGHT);
      pmfmStrategies.push(weightPmfmStrategy.length > 0 ? weightPmfmStrategy : []);
      this.weightPmfmStrategiesTable.value = weightPmfmStrategy.length > 0 ? weightPmfmStrategy : [new PmfmStrategy()];

      // LENGTH
      const lengthPmfmStrategies = this.getPmfmsByType(data.pmfmStrategies, pmfmGroups.LENGTH, ParameterLabelGroups.LENGTH);
      pmfmStrategies.push(lengthPmfmStrategies.length > 0 ? lengthPmfmStrategies : []);
      this.lengthPmfmStrategiesTable.value = lengthPmfmStrategies.length > 0 ? lengthPmfmStrategies : [new PmfmStrategy()];

      // MATURITY
      const maturityPmfmStrategies = this.getPmfmsByType(data.pmfmStrategies, pmfmGroups.MATURITY, ParameterLabelGroups.MATURITY);
      pmfmStrategies.push(maturityPmfmStrategies.length > 0 ? maturityPmfmStrategies : []);
      this.maturityPmfmStrategiesTable.value = maturityPmfmStrategies.length > 0 ? maturityPmfmStrategies : [new PmfmStrategy()];

      pmfmStrategiesControl.patchValue(pmfmStrategies);
    });


    this.referentialRefService.loadAll(0, 0, null, null,
      {
        entityName: 'Fraction'
      },
      { withTotal: false /* total not need */ }
    ).then(res => {
      const calcifiedTypeControl = this.pmfmStrategiesFractionForm;
      const PmfmStrategiesFraction = (data.pmfmStrategies || []).filter(p => p.fractionId && !p.pmfm);
      const fractions = PmfmStrategiesFraction.map(cal => {
        return {
          id: cal.fractionId,
          name: res.data.find(fraction => fraction.id === cal.fractionId).name,
        };
      });
      calcifiedTypeControl.clear();
      this.calcifiedFractionsHelper.resize(Math.max(1, PmfmStrategiesFraction.length));
      calcifiedTypeControl.patchValue(fractions);
    });
  }


  async getValue(): Promise<any> {
    const json = this.form.value;

    json.name = json.label || json.name;
    json.label = json.label || json.name;
    json.description = json.label || json.description;
    json.analyticReference = (typeof json.analyticReference === 'object') ? json.analyticReference.label : json.analyticReference;

    // get taxonName and
    json.taxonNames = (this.form.controls.taxonNames.value || []);
    (json.taxonNames || []).forEach(taxonNameStrategy => {
      delete taxonNameStrategy.strategyId; // Not need when saved
      taxonNameStrategy.priorityLevel = taxonNameStrategy.priorityLevel || 1;
      taxonNameStrategy.taxonName = {
        ...taxonNameStrategy.taxonName,
        taxonGroupIds: undefined
      } as TaxonNameRef;
    });

    // Apply observer privilege to departments
    const observerPrivilege = {id: ProgramPrivilegeIds.OBSERVER};
    json.departments.map(department => department.privilege = observerPrivilege);

    // Compute year
    const year = isNotNil(this.form.controls.year.value) ? moment(this.form.controls.year.value).year() : moment().year();

    // Fishing Area + Efforts --------------------------------------------------------------------------------------------

    const appliedStrategy: any = firstArrayValue(json.appliedStrategies);

    // append efforts (trick is that efforts are added to the first appliedStrategy of the array)
    if (appliedStrategy) {
      appliedStrategy.appliedPeriods = (json.appliedPeriods || [])
        .filter(period => isNotNil(period.acquisitionNumber))
        .map(p => {
          p.startDate.set('year', year);
          p.endDate.set('year', year);
          return p;
        });
    }
    if (appliedStrategy) {
      json.appliedStrategies[0] = appliedStrategy;
    }
    // json.appliedStrategies = appliedStrategy ? [appliedStrategy] : [];

    // PMFM + Fractions -------------------------------------------------------------------------------------------------
    const sex = this.form.get('sex').value;
    const age = this.form.get('age').value;

    // Save before get PMFM values
    await this.weightPmfmStrategiesTable.save();
    await this.lengthPmfmStrategiesTable.save();

    let pmfmStrategies = [
      ...this.weightPmfmStrategiesTable.value,
      ...this.lengthPmfmStrategiesTable.value
    ];

    if (sex) {
      const pmfmStrategySex = <PmfmStrategy>{ pmfmId: PmfmIds.SEX };
      pmfmStrategies.push(pmfmStrategySex);

      // Add maturity pmfms
      await this.maturityPmfmStrategiesTable.save();
      pmfmStrategies = pmfmStrategies.concat(
        ...this.maturityPmfmStrategiesTable.value
      );
    }

    if (age) {
      const pmfmStrategyAge = <PmfmStrategy>{ pmfmId: PmfmIds.AGE };
      pmfmStrategies.push(pmfmStrategyAge);

      // Pièces calcifiées
      (json.pmfmStrategiesFraction || []).filter(isNotNil)
        .forEach(fraction => {
          const pmfmStrategiesFraction = <PmfmStrategy>{
            fraction
          };
          pmfmStrategies.push(pmfmStrategiesFraction);
        });
    }

    // Add analytic reference pmfm strategy
    const pmfmStrategyAnalyticReference = <PmfmStrategy>{
      pmfmId: PmfmIds.MORSE_CODE
    };
    pmfmStrategies.push(pmfmStrategyAnalyticReference);

    let rankOrder = 1;
    json.pmfmStrategies = pmfmStrategies
      .map(pmfm => {
        // Set defaults attributes
        pmfm.strategyId = json.id;
        pmfm.acquisitionLevel = AcquisitionLevelCodes.SAMPLE;
        pmfm.pmfmId = toNumber(pmfm.pmfmId, toNumber((pmfm.pmfm && pmfm.pmfm.id) ? pmfm.pmfm.id : null, null));
        pmfm.parameter = pmfm.parameter ? pmfm.parameter : undefined;
        pmfm.parameterId = toNumber(pmfm.parameterId, pmfm.parameter && pmfm.parameter.id);
        pmfm.matrixId = toNumber(pmfm.matrixId, pmfm.matrix && pmfm.matrix.id);
        pmfm.fractionId = toNumber(pmfm.fractionId, pmfm.fraction && pmfm.fraction.id);
        pmfm.methodId = toNumber(pmfm.methodId, pmfm.method && pmfm.method.id);
        pmfm.acquisitionNumber = 1;
        pmfm.isMandatory = false;
        pmfm.rankOrder = rankOrder++;

        // Minify entity
        pmfm.pmfm = pmfm.pmfm && pmfm.pmfm.asObject ? pmfm.pmfm.asObject({minify: false}) : undefined;

        //FIX TODO
        if (pmfm.parameter) {
          const parameter = new ReferentialRef();
          parameter.id = pmfm.parameter.id;
          parameter.entityName = pmfm.parameter.entityName;
          parameter.label = pmfm.parameter.label;
          parameter.name = pmfm.parameter.name;
          pmfm.parameter = parameter;
        }

        return pmfm;
      })
      // Remove if empty
      .filter(p => isNotNil(p.pmfmId) || isNotNil(p.parameterId) || isNotNil(p.matrixId) || isNotNil(p.fractionId) || isNotNil(p.methodId));

    return json;
  }

  protected async onDateChange(date?: Moment) {
    date = fromDateISOString(date || this.form.get('year').value);

    if (!date || !this.program) return; // Skip if date or program are missing

    const labelControl = this.form.get('label');

    //update mask
    const year = date.year().toString();
    this.labelMask = [...year.split(''), '-', 'B', 'I', 'O', '-', /\d/, /\d/, /\d/, /\d/];

    // get new label sample row code
    const computedLabel = this.program && (await this.strategyService.computeNextLabel(this.program.id, `${year}-BIO-`, 4));
    console.info('[sampling-strategy-form] Computed label: ' + computedLabel);

    const label = labelControl.value;
    if (isNil(label)) {
      labelControl.setValue(computedLabel);
    } else {
      const oldYear = label.split('-').shift();
      // Update the label, if year change
      if (year && oldYear && year !== oldYear) {
        labelControl.setValue(computedLabel);
      } else {
        labelControl.setValue(label);
      }
    }
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

  // pmfmStrategies Helper -----------------------------------------------------------------------------------------------
  protected initPmfmStrategiesHelper() {
    // appliedStrategies => appliedStrategies.location ?
    this.sexAndAgeHelper = new FormArrayHelper<PmfmStrategy>(
      FormArrayHelper.getOrCreateArray(this.formBuilder, this.form, 'pmfmStrategies'),
      (pmfmStrategy) => this.formBuilder.control(pmfmStrategy || null),
      ReferentialUtils.equals,
      ReferentialUtils.isEmpty,
      {
        allowEmptyArray: false
      }
    );
    // Create at least one fishing Area
    if (this.sexAndAgeHelper.size() === 0) {
      this.sexAndAgeHelper.resize(3);
    }
  }

  addPmfmStrategies() {
    this.sexAndAgeHelper.add();
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

  // appliedStrategies Helper -----------------------------------------------------------------------------------------------
  protected initAppliedPeriodHelper() {
    // appliedStrategiesHelper formControl can't have common validator since quarters efforts are optional
    this.appliedPeriodsHelper = new FormArrayHelper<AppliedPeriod>(
      FormArrayHelper.getOrCreateArray(this.formBuilder, this.form, 'appliedPeriods'),
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
  protected initdepartmentHelper() {
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

  // PmfmStrategiesFractionHelper - Pièces calcifiées ------------------------------------------------------------------------------------------
  protected initPmfmStrategiesFractionHelper() {
    this.calcifiedFractionsHelper = new FormArrayHelper<PmfmStrategy>(
      FormArrayHelper.getOrCreateArray(this.formBuilder, this.form, 'pmfmStrategiesFraction'),
      (pmfmStrategiesFraction) => this.formBuilder.control(pmfmStrategiesFraction || null, [SharedValidators.entity]),
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
  addPmfmStrategiesFraction() {
    this.calcifiedFractionsHelper.add();
  }

  protected markForCheck() {
    if (this.cd) this.cd.markForCheck();
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


  ifSex(): boolean {
    return this.form.get('sex').value;
  }

  ifAge(): boolean {
    return this.form.get('age').value;
  }

  /**
   * get pmfms
   * @param parameterLabels
   * @protected
   */
  protected async getPmfmsByParameterLabels(parameterLabels: string[]) {
    const res = await this.pmfmService.loadAll(0, 1000, null, null, {
        levelLabels: parameterLabels
      },
      {
        withTotal: false,
        withDetails: true,
        fetchPolicy: "cache-first"
      });
    return res.data;
  }



  /**
   * get pmfm by type
   * @param pmfmStrategies
   * @param pmfmGroups
   * @param type
   * @protected
   */
  protected getPmfmsByType(pmfmStrategies: PmfmStrategy[], pmfmGroups: number[], type: any) {
    return (pmfmStrategies || []).filter(p => {
      if (p) {
        const pmfmId = toNumber(p.pmfmId, p.pmfm && p.pmfm.id);
        const hasParameterId = p.parameter && p.parameter.label && type.includes(p.parameter.label);
        return pmfmGroups.includes(pmfmId) || hasParameterId;
      }
      return false;
    });
  }

  selectInputContent = AppFormUtils.selectInputContent;

}
