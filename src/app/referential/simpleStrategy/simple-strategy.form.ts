import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnInit, ViewChild } from '@angular/core';
import { FormArray, FormBuilder, Validators } from "@angular/forms";
import { DateAdapter } from "@angular/material/core";
import * as moment from "moment";
import { Moment } from 'moment/moment';
import { DEFAULT_PLACEHOLDER_CHAR } from 'src/app/shared/constants';
import { SharedValidators } from 'src/app/shared/validator/validators';
import {AppForm,EntityUtils, FormArrayHelper, IReferentialRef, ReferentialRef} from '../../core/core.module';
import { LocalSettingsService } from "../../core/services/local-settings.service";
import { ReferentialUtils } from "../../core/services/model/referential.model";
import { fromDateISOString, isNil, isNotNil } from "../../shared/functions";
import { PmfmStrategy } from "../services/model/pmfm-strategy.model";
import { Program } from '../services/model/program.model';
import { AppliedPeriod, AppliedStrategy, Strategy, StrategyDepartment, TaxonNameStrategy } from "../services/model/strategy.model";
import { ReferentialRefService } from "../services/referential-ref.service";
import { StrategyService } from "../services/strategy.service";
import { StrategyValidatorService } from '../services/validator/strategy.validator';
import { PmfmStrategiesTable } from "../strategy/pmfm-strategies.table";


@Component({
  selector: 'form-simple-strategy',
  templateUrl: './simple-strategy.form.html',
  styleUrls: ['./simple-strategy.form.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    {provide: StrategyValidatorService}
  ],
})
export class SimpleStrategyForm extends AppForm<Strategy> implements OnInit {


  mobile: boolean;
  programId = -1;

  enableTaxonNameFilter = true;
  canFilterTaxonName = true;
  taxonNameHelper: FormArrayHelper<TaxonNameStrategy>;

  enableEotpFilter = true;
  canFilterEotp = true;

  enableStrategyDepartmentFilter = true;
  canFilterStrategyDepartment = true;
  strategyDepartmentHelper: FormArrayHelper<StrategyDepartment>;
  StrategyDepartmentFocusIndex = -1;

  enableAppliedStrategyFilter = true;
  canFilterAppliedStrategy = true;
  appliedStrategiesHelper: FormArrayHelper<AppliedStrategy>;
  appliedStrategiesIndex = -1;

  appliedPeriodHelper: FormArrayHelper<AppliedPeriod>;
  appliedPeriodIndex = -1;


  enablePmfmStrategiesFractionFilter = true;
  canFilterPmfmStrategiesFraction = true;
  PmfmStrategiesFractionHelper: FormArrayHelper<PmfmStrategy>;
  PmfmStrategiesFractionFocusIndex = -1;


  pmfmStrategiesHelper: FormArrayHelper<PmfmStrategy>;
  pmfmStrategiesFocusIndex = -1;

  @Input() program: Program;
  @Input() showError = true;
  @Input() entityName;
  label: string = '';

  @Input() placeholderChar: string = DEFAULT_PLACEHOLDER_CHAR;

  public sampleRowMask = ['2', '0', '2', '0', '_', 'B', 'I', '0', '_', /\d/, /\d/, /\d/, /\d/];


  get strategyDepartmentFormArray(): FormArray {
    return this.form.controls.strategyDepartments as FormArray;
  }

  get taxonNamesForm(): FormArray {
    return this.form.controls.taxonNames as FormArray;
  }

  get pmfmStrategiesForm(): FormArray {
    return this.form.controls.pmfmStrategies as FormArray;
  }

  get appliedStrategiesForm(): FormArray {
    return this.form.controls.appliedStrategies as FormArray;
  }

  get appliedPeriodsForm(): FormArray {
    return this.form.controls.appliedPeriods as FormArray;
  }

  get PmfmStrategiesFractionForm(): FormArray {
    return this.form.controls.PmfmStrategiesFraction as FormArray;
  }

  @ViewChild('weightPmfmStrategiesTable', { static: true }) weightPmfmStrategiesTable: PmfmStrategiesTable;
  @ViewChild('sizePmfmStrategiesTable', { static: true }) sizePmfmStrategiesTable: PmfmStrategiesTable;
  @ViewChild('maturityPmfmStrategiesTable', { static: true }) maturityPmfmStrategiesTable: PmfmStrategiesTable;

  constructor(
    protected dateAdapter: DateAdapter<Moment>,
    protected validatorService: StrategyValidatorService,
    protected referentialRefService: ReferentialRefService,
    protected strategyService: StrategyService,
    protected settings: LocalSettingsService,
    protected cd: ChangeDetectorRef,
    protected formBuilder: FormBuilder,
  ) {
    super(dateAdapter, validatorService.getFormGroup(), settings);
    this.mobile = this.settings.mobile;
  }
  tabIndex?: number;
  hidden?: boolean;
  appliedYear: string ='';

  
  setPmfmStrategies() {
    const pmfms = [];
    pmfms.push(this.pmfmStrategiesHelper.at(0).value);
    pmfms.push(this.pmfmStrategiesHelper.at(1).value);
    pmfms.push(this.weightPmfmStrategiesTable.value);
    pmfms.push(this.sizePmfmStrategiesTable.value);
    pmfms.push(this.maturityPmfmStrategiesTable.value);

    this.form.controls.pmfmStrategies.patchValue(pmfms);
    this.markAsDirty();
  }

  ngOnInit() {
    super.ngOnInit();

    this.weightPmfmStrategiesTable.onCancelOrDeleteRow.subscribe(() => this.setPmfmStrategies());
    this.sizePmfmStrategiesTable.onCancelOrDeleteRow.subscribe(() => this.setPmfmStrategies());
    this.maturityPmfmStrategiesTable.onCancelOrDeleteRow.subscribe(() => this.setPmfmStrategies());
    this.weightPmfmStrategiesTable.onConfirmEditCreateRow.subscribe(() => this.setPmfmStrategies());
    this.sizePmfmStrategiesTable.onConfirmEditCreateRow.subscribe(() => this.setPmfmStrategies());
    this.maturityPmfmStrategiesTable.onConfirmEditCreateRow.subscribe(() => this.setPmfmStrategies());

     // register year field changes
    this.registerSubscription(
      this.form.get('creationDate').valueChanges
        .subscribe(async (date : Moment) => this.onDateChange(date ? date : moment()))
    );

    // taxonName autocomplete
    this.registerAutocompleteField('taxonName', {
      suggestFn: (value, filter) => this.suggest(value, {
          ...filter, statusId : 1
        },
        'TaxonName',
        this.enableTaxonNameFilter),
      attributes: ['name'],
      columnNames: [ 'REFERENTIAL.NAME'],
      columnSizes: [2,10],
      mobile: this.settings.mobile
    });

    // laboratory autocomplete
    this.registerAutocompleteField('strategyDepartment', {
      suggestFn: (value, filter) => this.suggest(value, {
          ...filter, statusId : 1
        },
        'Department',
        this.enableStrategyDepartmentFilter),
      columnSizes : [4,6],
      mobile: this.settings.mobile
    });

    // appliedStrategy autocomplete
    this.registerAutocompleteField('appliedStrategy', {
      suggestFn: (value, filter) => this.suggest(value, {
          ...filter, statusId : 1, Id : 111
        },
        'Location',
        this.enableAppliedStrategyFilter),
      mobile: this.settings.mobile
    });


    // eotp combo -------------------------------------------------------------------
    this.registerAutocompleteField('analyticReference', {
      suggestFn: (value, filter) => this.suggest(value, {
          ...filter, statusIds : [0,1]
        },
        'AnalyticReference',
        this.enableEotpFilter),
      columnSizes : [4,6],
      mobile: this.settings.mobile
    });

    this.registerAutocompleteField('PmfmStrategiesFraction', {
        suggestFn: (value, filter) => this.suggest(value, {
            ...filter, statusId : 1
          },
          'Fraction',
          this.enablePmfmStrategiesFractionFilter),
      attributes: ['name'],
      columnNames: [ 'REFERENTIAL.NAME'],
        columnSizes: [2,10],
        mobile: this.settings.mobile
    });

    // set default mask
    this.sampleRowMask = [...moment().year().toString().split(''), '-', 'B', 'I', '0', '-', /\d/, /\d/, /\d/, /\d/];

    //init helpers
    this.initDepartmentHelper();
    this.initTaxonNameHelper();
    this.initPmfmStrategiesHelper();
    this.initAppliedStrategiesHelper();
    this.initAppliedPeriodHelper();
    this.initPmfmStrategiesFractionHelper();

  }

  /**
   * Suggest autocomplete values
   * @param value
   * @param filter - filters to apply
   * @param entityName - referential to request
   * @param filtered - boolean telling if we load prefilled data
   */
  protected async suggest(value: string, filter: any, entityName: string, filtered: boolean) : Promise<IReferentialRef[]> {

    // Special case: AnalyticReference
    if (entityName == "AnalyticReference") {
      if (filtered) {
        //TODO a remplacer par recuperation des donnees deja saisies
        return this.strategyService.LoadAllAnalyticReferences(0, 5, null, null, filter);
      } else {
        return this.strategyService.suggestAnalyticReferences(value, filter);
      }
    }

    if(filtered) {
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

  toggleFilteredItems(fieldName: string){
    let value : boolean;
    switch (fieldName) {
      case 'eotp':
        this.enableEotpFilter = value = !this.enableEotpFilter;
        break;
      case 'strategyDepartment':
        this.enableStrategyDepartmentFilter = value = !this.enableStrategyDepartmentFilter;
        break;
      case 'appliedStrategy':
        this.enableAppliedStrategyFilter = value = !this.enableAppliedStrategyFilter;
        break;
      case 'taxonName':
        this.enableTaxonNameFilter = value = !this.enableTaxonNameFilter;
        break;
      case 'PmfmStrategiesFraction':
        this.enablePmfmStrategiesFractionFilter = value = !this.enablePmfmStrategiesFractionFilter;
        //this.loadCalcifiedType();
        break;
      default:
        break;
    }
    this.markForCheck();
    console.debug(`[simpleStrategy] set enable filtered ${fieldName} items to ${value}`);
  }

  setValue(data: Strategy, opts?: { emitEvent?: boolean; onlySelf?: boolean }) {
    console.debug("[simpleStrategy-form] Setting Strategy value", data);
    if (!data) return;

    // QUICKFIX label to remove as soon as possible
    data.label = data.label?.replace(/_/g, "-");

    // Resize strategy department array
    this.strategyDepartmentHelper.resize(Math.max(1, data.strategyDepartments.length));

    // Resize strategy department array
    this.appliedStrategiesHelper.resize(Math.max(1, data.appliedStrategies.length));

    // Resize pmfm strategy array
    this.taxonNameHelper.resize(Math.max(1, data.taxonNames.length));

    // Resize pmfm strategy array
    this.pmfmStrategiesHelper.resize(Math.max(1, data.pmfmStrategies.length));

    // Resize strategy department array
    this.appliedPeriodHelper.resize(4);

    // APPLIED_PERIODS
    // get model appliedPeriods which are stored in first applied strategy
    const appliedPeriodControl = this.appliedPeriodsForm;
    const appliedPeriods = data.appliedStrategies.length && data.appliedStrategies[0].appliedPeriods || [];
    const appliedStrategyId = data.appliedStrategies.length && data.appliedStrategies[0].strategyId || undefined;

    // format periods for applied conrol period in view and init default period by quarter if no set
    const quarter1 = appliedPeriods.find(period => (fromDateISOString(period.startDate).month() + 1) === 1) || {
      appliedStrategyId: appliedStrategyId,
      startDate: moment("2020-01-01"),
      endDate: moment("2020-03-31"),
      acquisitionNumber: undefined
    };
    const quarter2 = appliedPeriods.find(period => (fromDateISOString(period.startDate).month() + 1) === 4) || {
      appliedStrategyId: appliedStrategyId,
      startDate: moment("2020-04-01"),
      endDate: moment("2020-06-30"),
      acquisitionNumber: undefined
    };
    const quarter3 = appliedPeriods.find(period => (fromDateISOString(period.startDate).month() + 1) === 7) || {
      appliedStrategyId: appliedStrategyId,
      startDate: moment("2020-07-01"),
      endDate: moment("2020-09-30"),
      acquisitionNumber: undefined
    };
    const quarter4 = appliedPeriods.find(period => (fromDateISOString(period.startDate).month() + 1) === 10) || {
      appliedStrategyId: appliedStrategyId,
      startDate: moment("2020-10-01"),
      endDate: moment("2020-12-31"),
      acquisitionNumber: undefined
    };
    const formattedAppliedPeriods = [quarter1,quarter2,quarter3,quarter4];

    // patch the control value
    appliedPeriodControl.patchValue(formattedAppliedPeriods);



    super.setValue(data, opts);

    // fixme get eotp from referential by label = data.analyticReference
    let  analyticReferenceToSet : IReferentialRef = new ReferentialRef();
    analyticReferenceToSet.label = data.analyticReference;
    this.form.get('analyticReference').setValue(analyticReferenceToSet);



  

      const pmfmStrategiesControl = this.pmfmStrategiesForm;


      let age = data.pmfmStrategies.filter(p => p.pmfm && p.pmfm.parameter && p.pmfm.parameter.label ===  "AGE");
      let sex = data.pmfmStrategies.filter(p => p.pmfm && p.pmfm.parameter && p.pmfm.parameter.label ===  "SEX");


    let pmfmStrategies : any[] = [ sex.length>0 ? true : false, age.length>0 ? true : false];


    let weightPmfmStrategy = (data.pmfmStrategies || []).filter(p => p.pmfm && p.pmfm.parameter && p.pmfm.parameter.label === 'WEIGHT');
    pmfmStrategies.push(weightPmfmStrategy.length > 0 ? weightPmfmStrategy : []);
    this.weightPmfmStrategiesTable.value = weightPmfmStrategy.length > 0 ? weightPmfmStrategy : [new PmfmStrategy()];


    //SIZES
    const sizeValues = ['LENGTH_PECTORAL_FORK', 'LENGTH_CLEITHRUM_KEEL_CURVE', 'LENGTH_PREPELVIC', 'LENGTH_FRONT_EYE_PREPELVIC', 'LENGTH_LM_FORK', 'LENGTH_PRE_SUPRA_CAUDAL', 'LENGTH_CLEITHRUM_KEEL', 'LENGTH_LM_FORK_CURVE', 'LENGTH_PECTORAL_FORK_CURVE', 'LENGTH_FORK_CURVE', 'STD_STRAIGTH_LENGTH', 'STD_CURVE_LENGTH', 'SEGMENT_LENGTH', 'LENGTH_MINIMUM_ALLOWED', 'LENGTH', 'LENGTH_TOTAL', 'LENGTH_STANDARD', 'LENGTH_PREANAL', 'LENGTH_PELVIC', 'LENGTH_CARAPACE', 'LENGTH_FORK', 'LENGTH_MANTLE'];
    let sizePmfmStrategy = (data.pmfmStrategies || []).filter(p => p.pmfm && p.pmfm.parameter && sizeValues.includes(p.pmfm.parameter.label));
    pmfmStrategies.push(sizePmfmStrategy.length > 0 ? sizePmfmStrategy : []);
    this.sizePmfmStrategiesTable.value = sizePmfmStrategy.length > 0 ? sizePmfmStrategy : [new PmfmStrategy()];



    const maturityValues = ['MATURITY_STAGE_3_VISUAL', 'MATURITY_STAGE_4_VISUAL', 'MATURITY_STAGE_5_VISUAL', 'MATURITY_STAGE_6_VISUAL', 'MATURITY_STAGE_7_VISUAL', 'MATURITY_STAGE_9_VISUAL'];
    let maturityPmfmStrategy = (data.pmfmStrategies || []).filter(p => p.pmfm && p.pmfm.parameter && maturityValues.includes(p.pmfm.parameter.label));
    pmfmStrategies.push(maturityPmfmStrategy.length > 0 ? maturityPmfmStrategy : []);
    this.maturityPmfmStrategiesTable.value = maturityPmfmStrategy.length > 0 ? maturityPmfmStrategy : [new PmfmStrategy()];


    this.pmfmStrategiesHelper.resize(Math.max(5, pmfmStrategies.length));
    pmfmStrategiesControl.patchValue(pmfmStrategies);




    this.referentialRefService.loadAll(0, 0, null, null,
      {
        entityName : 'Fraction'
      },
      { withTotal: false /* total not need */ }
    ).then(res => {
      const calcifiedTypeControl = this.PmfmStrategiesFractionForm;
      const PmfmStrategiesFraction =  (data.pmfmStrategies || []).filter(p => p.fractionId && !p.pmfm);
      const fractions = PmfmStrategiesFraction.map(cal => {
        return {
          id: cal.fractionId,
          name : res.data.find(fraction => fraction.id === cal.fractionId).name,
        }
      });

      this.PmfmStrategiesFractionHelper.resize(Math.max(1, PmfmStrategiesFraction.length))
      calcifiedTypeControl.patchValue(fractions);
    })

  }

  protected async onDateChange(date : Moment) {

    const labelControl = this.form.get('label');

    //update mask
    let year;
    if (date && (typeof date === 'object') && (date.year()))
    {
      year = date.year().toString();
    }
    else if (date && (typeof date === 'string'))
    {
      let dateAsString = date as string;
      year = dateAsString.split('-')[0];
    }
    this.sampleRowMask = [...year.split(''), '-', 'B', 'I', '0', '-', /\d/, /\d/, /\d/, /\d/];

    const label = labelControl.value;
    if(isNotNil(label)){
      const oldYear = label.split('-').shift();

      // get new label sample row code
      //TODO : replace 40 with this.program.id
      const updatedLabel = await this.strategyService.findStrategyNextLabel(40,`${year}-BIO-`, 4);

      // Update the label, if year change
      if (year && oldYear && year !== oldYear ) {
        labelControl.setValue(updatedLabel);
      }
    }
  }


  // TaxonName Helper -----------------------------------------------------------------------------------------------
  protected initTaxonNameHelper() {
    // appliedStrategies => appliedStrategies.location ?
      this.taxonNameHelper = new FormArrayHelper<TaxonNameStrategy>(
        FormArrayHelper.getOrCreateArray(this.formBuilder, this.form, 'taxonNames'),
        (ts) => this.validatorService.getTaxonNameStrategyControl(ts),
        (t1, t2) => EntityUtils.equals(t1.taxonName, t2.taxonName, 'name'),
        value => isNil(value) && isNil(value.taxonName),
        {
          allowEmptyArray: false
        }
      );
      // Create at least one fishing Area
      if (this.taxonNameHelper.size() === 0) {
        this.taxonNameHelper.resize(1);
      }
    }

  // pmfmStrategies Helper -----------------------------------------------------------------------------------------------
  protected initPmfmStrategiesHelper() {
  // appliedStrategies => appliedStrategies.location ?
    this.pmfmStrategiesHelper = new FormArrayHelper<PmfmStrategy>(
      FormArrayHelper.getOrCreateArray(this.formBuilder, this.form, 'pmfmStrategies'),
      (pmfmStrategy) => this.formBuilder.control(pmfmStrategy || null),
      ReferentialUtils.equals,
      ReferentialUtils.isEmpty,
      {
        allowEmptyArray: false
      }
    );
    // Create at least one fishing Area
    if (this.pmfmStrategiesHelper.size() === 0) {
      this.pmfmStrategiesHelper.resize(6);
    }
  }

  addPmfmStrategies() {
    this.pmfmStrategiesHelper.add();
    if (!this.mobile) {
      this.pmfmStrategiesFocusIndex = this.pmfmStrategiesHelper.size() - 1;
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
      this.strategyDepartmentHelper.resize(1);
    }
  }
  addAppliedStrategy() {
    this.appliedStrategiesHelper.add(new AppliedStrategy());
    if (!this.mobile) {
      this.appliedStrategiesIndex = this.appliedStrategiesHelper.size() - 1;
    }
  }

  // appliedStrategies Helper -----------------------------------------------------------------------------------------------
  protected initAppliedPeriodHelper() {
    // appliedStrategiesHelper formControl can't have common validator since quarters efforts are optional
    this.appliedPeriodHelper = new FormArrayHelper<AppliedPeriod>(
      FormArrayHelper.getOrCreateArray(this.formBuilder, this.form, 'appliedPeriods'),
      (appliedPeriod) => this.validatorService.getAppliedPeriodsControl(appliedPeriod),
      (p1, p2) => EntityUtils.equals(p1, p2, 'startDate'),
      value => isNil(value),
      {
        allowEmptyArray: false
      }
    );
    // Create at least one fishing Area
    if (this.appliedStrategiesHelper.size() === 0) {
      this.strategyDepartmentHelper.resize(1);
    }
  }

  // Laboratory Helper -----------------------------------------------------------------------------------------------
  protected initDepartmentHelper() {
    this.strategyDepartmentHelper = new FormArrayHelper<StrategyDepartment>(
      FormArrayHelper.getOrCreateArray(this.formBuilder, this.form, 'strategyDepartments'),
      (department) => this.validatorService.getStrategyDepartmentsControl(department),
      (d1, d2) => EntityUtils.equals(d1.department, d2.department, 'label'),
      value => isNil(value) && isNil(value.department),
      {
        allowEmptyArray: false
      }
    );
    // Create at least one laboratory
    if (this.strategyDepartmentHelper.size() === 0) {
      this.strategyDepartmentHelper.resize(1);
    }
  }
  addStrategyDepartment() {
    this.strategyDepartmentHelper.add(new StrategyDepartment());
    if (!this.mobile) {
      this.StrategyDepartmentFocusIndex = this.strategyDepartmentHelper.size() - 1;
    }
  }

  // PmfmStrategiesFractionHelper - Pièces calcifiées ------------------------------------------------------------------------------------------
  protected initPmfmStrategiesFractionHelper() {
    this.PmfmStrategiesFractionHelper = new FormArrayHelper<PmfmStrategy>(
      FormArrayHelper.getOrCreateArray(this.formBuilder, this.form, 'PmfmStrategiesFraction'),
      (pmfmStrategiesFraction) => this.formBuilder.control(pmfmStrategiesFraction || null, [Validators.required, SharedValidators.entity]),
      ReferentialUtils.equals,
      ReferentialUtils.isEmpty,
      {
        allowEmptyArray: false
      }
    );
    // Create at least one PmfmStrategiesFraction
    if (this.PmfmStrategiesFractionHelper.size() === 0) {
      this.PmfmStrategiesFractionHelper.resize(1);
    }
  }
  addPmfmStrategiesFraction() {
    this.PmfmStrategiesFractionHelper.add();
    if (!this.mobile) {
      this.PmfmStrategiesFractionFocusIndex = this.PmfmStrategiesFractionHelper.size() - 1;
    }
  }

  protected markForCheck() {
    if (this.cd) this.cd.markForCheck();
  }

}
