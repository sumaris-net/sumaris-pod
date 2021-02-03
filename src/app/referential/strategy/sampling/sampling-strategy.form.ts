import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnInit, ViewChild} from '@angular/core';
import {FormArray, FormBuilder, FormControl, ValidationErrors, ValidatorFn} from "@angular/forms";
import {DateAdapter} from "@angular/material/core";
import * as moment from "moment";
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
  ParameterLabelGroups,
  ProgramPrivilegeIds
} from '../../services/model/model.enum';
import {AppForm} from "../../../core/form/form.class";
import {FormArrayHelper} from "../../../core/form/form.utils";
import {EntityUtils} from "../../../core/services/model/entity.model";
import {PmfmUtils} from "../../services/model/pmfm.model";
import {
  firstArrayValue,
  isNil,
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
import {PmfmService} from "../../services/pmfm.service";
import {firstNotNilPromise} from "../../../shared/observables";
import {MatAutocompleteField} from "../../../shared/material/autocomplete/material.autocomplete";

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



  ngOnInit() {
    super.ngOnInit();

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
    // Add age and sex has controls

    // register year field changes
    this.registerSubscription(
      this.form.get('year').valueChanges.subscribe(date => this.onDateChange(date))
    );

    const idControl = this.form.get('id');
    this.form.get('label').setAsyncValidators([
      async (control) => {
        console.debug('[sampling-strategy-form] Checking of label is unique...');
        const exists = await this.strategyService.existLabel(control.value, {
          programId: this.program && this.program.id,
          excludedIds: isNotNil(idControl.value) ? [idControl.value] : undefined
        });
        if (exists) {
          console.warn('[sampling-strategy-form] Label not unique!');
          return <ValidationErrors>{ unique: true };
        }

        console.debug('[sampling-strategy-form] Checking of label is unique [OK]');
        SharedValidators.clearError(control, 'unique');
      }
    ]);

    // taxonName autocomplete
    this.registerAutocompleteField('taxonName', {
      suggestFn: (value, filter) => this.suggestTaxonName(value, {
        ...filter, statusIds: [StatusIds.ENABLE, StatusIds.TEMPORARY]
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
        ...filter, statusIds: [StatusIds.ENABLE, StatusIds.TEMPORARY] // TODO BLA why disable ??
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

    const weights = this.weightPmfmStrategiesTable.value.filter(p => p.pmfm || p.parameterId);
    const lengths = this.lengthPmfmStrategiesTable.value.filter(p => p.pmfm || p.parameterId);
    const maturities = this.maturityPmfmStrategiesTable.value.filter(p => p.pmfm || p.parameterId);

    pmfms.push(this.sexAndAgeHelper.at(0).value);
    pmfms.push(this.sexAndAgeHelper.at(1).value);
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
   * @param entityName - referential to request
   * @param filtered - boolean telling if we load prefilled data
   */
  protected async suggestLocations(value: string, filter: any): Promise<IReferentialRef[]> {
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
  protected async suggestAnalyticReferences(value: string, filter: any): Promise<IReferentialRef[]> {
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
   * @param entityName - referential to request
   * @param filtered - boolean telling if we load prefilled data
   */
  protected async suggestAgeFractions(value: string, filter: any): Promise<IReferentialRef[]> {
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
   * @param entityName - referential to request
   * @param filtered - boolean telling if we load prefilled data
   */
  protected async suggestDepartments(value: string, filter: any): Promise<IReferentialRef[]> {
    if (this.autocompleteFilters.department) {
      return suggestFromArray(this.departmentItems.getValue(), value, filter);
    } else {
      return this.referentialRefService.suggest(value, {
        ...filter,
        entityName: 'Department'
      });
    }
  }

  protected async suggestTaxonName(value: string, filter: any): Promise<TaxonNameRef[]> {
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

    // Resize strategy department array
    this.departmentsHelper.resize(Math.max(1, data.departments.length));

    // Resize strategy department array
    this.appliedStrategiesHelper.resize(Math.max(1, data.appliedStrategies.length));

    // Resize pmfm strategy array
    this.taxonNamesHelper.resize(Math.max(1, data.taxonNames.length));

    // Resize pmfm strategy array
    // this.pmfmStrategiesHelper.resize(Math.max(1, data.pmfmStrategies.length));

    // Resize strategy department array
    this.appliedPeriodsHelper.resize(4);

    // APPLIED_PERIODS
    // get model appliedPeriods which are stored in first applied strategy
    const appliedPeriodControl = this.appliedPeriodsForm;
    const appliedPeriods = data.appliedStrategies.length && data.appliedStrategies[0].appliedPeriods || [];
    const appliedStrategyId = data.appliedStrategies.length && data.appliedStrategies[0].strategyId || undefined;

    const year = moment().year();

    // format periods for applied conrol period in view and init default period by quarter if no set
    const quarter1 = appliedPeriods.find(period => (fromDateISOString(period.startDate).month() + 1) === 1) || {
      appliedStrategyId: appliedStrategyId,
      startDate: moment(`${year}-01-01`),
      endDate: moment(`${year}-03-31`),
      acquisitionNumber: undefined
    };

    const quarter2 = appliedPeriods.find(period => (fromDateISOString(period.startDate).month() + 1) === 4) || {
      appliedStrategyId: appliedStrategyId,
      startDate: moment(`${year}-04-01`),
      endDate: moment(`${year}-06-30`),
      acquisitionNumber: undefined
    };
    const quarter3 = appliedPeriods.find(period => (fromDateISOString(period.startDate).month() + 1) === 7) || {
      appliedStrategyId: appliedStrategyId,
      startDate: moment(`${year}-07-01`),
      endDate: moment(`${year}-09-30`),
      acquisitionNumber: undefined
    };
    const quarter4 = appliedPeriods.find(period => (fromDateISOString(period.startDate).month() + 1) === 10) || {
      appliedStrategyId: appliedStrategyId,
      startDate: moment(`${year}-10-01`),
      endDate: moment(`${year}-12-31`),
      acquisitionNumber: undefined
    };
    const formattedAppliedPeriods = [quarter1, quarter2, quarter3, quarter4];

    // patch the control value
    appliedPeriodControl.patchValue(formattedAppliedPeriods);

    super.setValue(data, opts);

    // Get fisrt period
    const period = appliedPeriods[0];
    this.form.get('year').patchValue(period ? period.startDate : moment());

    // fixme get eotp from referential by label = data.analyticReference
    this.form.patchValue({
      analyticReference: { label: data.analyticReference }
    });

    const pmfmStrategiesControl = this.pmfmStrategiesForm;
    let pmfmStrategies: any[];

    // If new
    if (!data.id) {
      pmfmStrategies = [null, null];
    } else {
      const hasAge = (data.pmfmStrategies || []).findIndex(p => PmfmUtils.hasParameterLabelIncludes(p.pmfm, ParameterLabelGroups.AGE)) !== -1;
      const hasSex = (data.pmfmStrategies || []).findIndex(p => PmfmUtils.hasParameterLabelIncludes(p.pmfm, ParameterLabelGroups.SEX)) !== -1;
      pmfmStrategies = [hasSex, hasAge];
    }

    //Weights
    // TODO BLA: revoir ces sélections
    // Dans le ngOnInit() :
    //   pmfmService.loadIdsGroupByParameterLabels(ParameterLabelGroups)
    //    .then(map => this._$pmfmGroups.next(map));
    //
    // ICI:
    // const pmfmGroups = await firstNotNilPromise(this._$pmfmGroups);
    // (data.pmfmStrategies || []).filter(p => {
    //   const pmfmId = toNumber(p.pmfmId, p.pmfm && p.pmfm.id);
    //   return pmfmGroups.WEIGHT.includes(pmfmId)
    // }
    const weightPmfmStrategy = (data.pmfmStrategies || []).filter(
      p =>
        (p.pmfm && p.pmfm.parameter && ParameterLabelGroups.WEIGHT.includes(p.pmfm.parameter.label)) ||
        (p['parameter'] && ParameterLabelGroups.WEIGHT.includes(p['parameter'].label))
    );
    pmfmStrategies.push(weightPmfmStrategy.length > 0 ? weightPmfmStrategy : []);
    this.weightPmfmStrategiesTable.value = weightPmfmStrategy.length > 0 ? weightPmfmStrategy : [new PmfmStrategy()];

    // Length
    const lengthPmfmStrategies = (data.pmfmStrategies || []).filter(
      p =>
        (p.pmfm && p.pmfm.parameter && ParameterLabelGroups.LENGTH.includes(p.pmfm.parameter.label)) ||
        (p['parameter'] && ParameterLabelGroups.LENGTH.includes(p['parameter'].label))
    );
    pmfmStrategies.push(lengthPmfmStrategies.length > 0 ? lengthPmfmStrategies : []);
    this.lengthPmfmStrategiesTable.value = lengthPmfmStrategies.length > 0 ? lengthPmfmStrategies : [new PmfmStrategy()];

    // Maturities
    const maturityPmfmStrategy = (data.pmfmStrategies || []).filter(
      p =>
        (p.pmfm && p.pmfm.parameter && ParameterLabelGroups.MATURITY.includes(p.pmfm.parameter.label)) ||
        (p['parameter'] && ParameterLabelGroups.MATURITY.includes(p['parameter'].label))
    );
    pmfmStrategies.push(maturityPmfmStrategy.length > 0 ? maturityPmfmStrategy : []);
    this.maturityPmfmStrategiesTable.value = maturityPmfmStrategy.length > 0 ? maturityPmfmStrategy : [new PmfmStrategy()];

    pmfmStrategiesControl.patchValue(pmfmStrategies);

    // TODO
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

  markAsUntouched(opts?: { onlySelf?: boolean }) {
    console.log("TODO NOE: markAsUntouched()");
    super.markAsUntouched(opts);
  }

  async getValue(): Promise<any> {
    const json = this.form.value;

    json.name = json.label || json.name;
    json.analyticReference = (typeof json.analyticReference === 'object') ? json.analyticReference.label : json.analyticReference;

    // FIXME : description is not nullable in database so we init it with an empty string if nothing provided in the
    json.description = json.description || ' ';

    // get taxonName and
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
    const year = isNotNil(json.year) ? moment(json.year).year() : moment().year();

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
    json.appliedStrategies = appliedStrategy ? [appliedStrategy] : [];

    // PMFM + Fractions -------------------------------------------------------------------------------------------------
    const [sex, age] = this.pmfmStrategiesForm.value;

    // Save before get PMFM values
    await this.weightPmfmStrategiesTable.save();
    await this.lengthPmfmStrategiesTable.save();

    let pmfmStrategies = [
      ...this.weightPmfmStrategiesTable.value,
      ...this.lengthPmfmStrategiesTable.value
    ];

    if (sex) {
      // Add sex pmfm
      const pmfmStrategySex = <PmfmStrategy>{
        pmfm: firstArrayValue(await this.getPmfmsByParameterLabels(ParameterLabelGroups.SEX))
      };
      pmfmStrategies.push(pmfmStrategySex);

      await this.maturityPmfmStrategiesTable.save();

      // Add maturity pmfms
      pmfmStrategies = pmfmStrategies.concat(
        ...this.maturityPmfmStrategiesTable.value
      );
    }

    if (age) {
      const pmfmStrategyAge = <PmfmStrategy>{
        pmfm: firstArrayValue(await this.getPmfmsByParameterLabels(ParameterLabelGroups.AGE))
      };
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

    let rankOrder = 1;
    json.pmfmStrategies = pmfmStrategies
      .map(pmfm => {
        // Set defaults attributes
        pmfm.strategyId = json.id;
        pmfm.acquisitionLevel = AcquisitionLevelCodes.SAMPLE;
        pmfm.pmfmId = toNumber(pmfm.pmfmId, pmfm.pmfm && pmfm.pmfm.id);
        pmfm.parameterId = toNumber(pmfm.parameterId, pmfm.parameter && pmfm.parameter.id);
        pmfm.matrixId = toNumber(pmfm.matrixId, pmfm.matrix && pmfm.matrix.id);
        pmfm.fractionId = toNumber(pmfm.fractionId, pmfm.fraction && pmfm.fraction.id);
        pmfm.methodId = toNumber(pmfm.methodId, pmfm.method && pmfm.method.id);
        pmfm.acquisitionNumber = 1;
        pmfm.isMandatory = false;
        pmfm.rankOrder = rankOrder++;

        // Minify entity
        pmfm.pmfm = pmfm.pmfm && pmfm.pmfm.asObject ? pmfm.pmfm.asObject({minify: false}) : undefined;

        delete pmfm.parameter; //  = pmfm.parameter && pmfm.parameter.asObject ? pmfm.parameter.asObject({minify: false}) : undefined;

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
        allowEmptyArray: false,
        validators: [
          this.requiredPmfmMinLength(2),
          this.requiredWeightOrSize()
        ]
      }
    );
    // Create at least one fishing Area
    if (this.sexAndAgeHelper.size() === 0) {
      this.sexAndAgeHelper.resize(5);
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
          this.requiredPeriodMinLength(1)
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

  requiredPmfmMinLength(minLength?: number): ValidatorFn {
    minLength = minLength || 2;
    return (array: FormArray): ValidationErrors | null => {
      //Check if sex parameter check
      const data = array.value;
      if (data[0] === false) {
        // Sex = false => remove maturity
        data[4] = [];
      }
      const values = data.flat().filter(pmfm => pmfm && pmfm !== false);
      if (!values || values.length < minLength) {
        return { minLength: { minLength: minLength } };
      }
      return null;
    };
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

  requiredWeightOrSize(): ValidatorFn {
    return (array: FormArray): ValidationErrors | null => {
      if (Array.isArray(array.value[2])) {
        const weight = (array.value[2] || []).filter(p => p.pmfm);
        if (weight && weight.length > 0) {
          return null;
        }
      }
      if (Array.isArray(array.value[3])) {
        const size = (array.value[3] || []).filter(p => p.pmfm);
        if (size && size.length > 0) {
          return null;
        }
      }
      return { weightOrSize: { weightOrSize: false } };
    };
  }

  ifSex(): boolean {
    const sex = this.pmfmStrategiesForm.value[0];
    return sex;
  }

  ifAge(): boolean {
    const sex = this.pmfmStrategiesForm.value[1];
    return sex;
  }


  /**
   * get pmfm
   * @param label
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
}
