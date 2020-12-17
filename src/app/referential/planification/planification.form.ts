import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnInit, ViewChild} from '@angular/core';
import {Moment} from 'moment/moment';
import {DateAdapter} from "@angular/material/core";
import {LocalSettingsService} from "../../core/services/local-settings.service";
import {FormBuilder, FormArray, Validators} from "@angular/forms";
import {ReferentialRefService} from "../../referential/services/referential-ref.service";
import {StrategyService} from "../services/strategy.service";
import {
  AppForm,
  ReferentialRef,
  IReferentialRef,
  FormArrayHelper,
  EntityUtils
} from '../../core/core.module';
import {BehaviorSubject} from "rxjs";
import { Program } from '../services/model/program.model';
import { DEFAULT_PLACEHOLDER_CHAR } from 'src/app/shared/constants';
import { ReferentialUtils} from "../../core/services/model/referential.model";
import * as moment from "moment";
import {AppliedPeriod, AppliedStrategy, Strategy, StrategyDepartment} from "../services/model/strategy.model";
import {fromDateISOString, isNil, isNotNil} from "../../shared/functions";
import {PmfmStrategy} from "../services/model/pmfm-strategy.model";
import { StrategyValidatorService } from '../services/validator/strategy.validator';
import { SharedValidators } from 'src/app/shared/validator/validators';
import {PmfmStrategiesTable} from "../strategy/pmfm-strategies.table";



@Component({
  selector: 'form-planification',
  templateUrl: './planification.form.html',
  styleUrls: ['./planification.form.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    {provide: StrategyValidatorService}
  ],
})
export class PlanificationForm extends AppForm<Strategy> implements OnInit {

  // protected formBuilder: FormBuilder;
  private _eotpSubject = new BehaviorSubject<IReferentialRef[]>(undefined);
  private _calcifiedTypeSubject = new BehaviorSubject<IReferentialRef[]>(undefined);

  mobile: boolean;
  programId = -1;

  enableTaxonNameFilter = true;
  canFilterTaxonName = true;
  taxonNameHelper: FormArrayHelper<ReferentialRef>;

  enableEotpFilter = true;
  canFilterEotp = true;

  enableLaboratoryFilter = true;
  canFilterLaboratory = true;
  strategyDepartmentHelper: FormArrayHelper<StrategyDepartment>;
  laboratoryFocusIndex = -1;

  enableFishingAreaFilter = true;
  canFilterFishingArea = true;
  appliedStrategiesHelper: FormArrayHelper<AppliedStrategy>;
  appliedStrategiesIndex = -1;

  appliedPeriodHelper: FormArrayHelper<AppliedPeriod>;
  appliedPeriodIndex = -1;


  enableCalcifiedTypeFilter = true;
  canFilterCalcifiedType = true;
  calcifiedTypeHelper: FormArrayHelper<ReferentialRef>;
  calcifiedTypeFocusIndex = -1;

  @Input() program: Program;
  @Input() showError = true;
  @Input() entityName;
  label: string = '';

  @Input() placeholderChar: string = DEFAULT_PLACEHOLDER_CHAR;


  pmfmStrategiesHelper: FormArrayHelper<PmfmStrategy>;
  pmfmStrategiesFocusIndex = -1;

  public sampleRowMask = ['2', '0', '2', '0', '_', 'B', 'I', '0', '_', /\d/, /\d/, /\d/, /\d/];

  get calcifiedTypesForm(): FormArray {
    return this.form.controls.calcifiedTypes as FormArray;
  }

  get strategyDepartmentFormArray(): FormArray {
    return this.form.controls.strategyDepartments as FormArray;
  }

  get fishingAreasForm(): FormArray {
    // appliedStrategies.location à la place de appliedStrategies
    return this.form.controls.appliedStrategies as FormArray;
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

  focus() {
    throw new Error('Method not implemented.');
  }
  writeValue(value: any): void {
    //  if (value !== this.sampleRowCode) {
    //       this.sampleRowCode = value;
    //     }
  }
  registerOnChange(fn: any): void {
    throw new Error('Method not implemented.');
  }
  registerOnTouched(fn: any): void {
    throw new Error('Method not implemented.');
  }
  setDisabledState?(isDisabled: boolean): void {
    throw new Error('Method not implemented.');
  }

  setPmfmStrategies() {
    const pmfms = [];
    pmfms.push(this.pmfmStrategiesHelper.at(0).value);
    pmfms.push(this.pmfmStrategiesHelper.at(1).value);
    pmfms.push(this.weightPmfmStrategiesTable.value);
    pmfms.push(this.sizePmfmStrategiesTable.value);
    pmfms.push(this.maturityPmfmStrategiesTable.value);

    for (let i = 5; i < this.pmfmStrategiesForm.value.lenght; i++) {
      pmfms.push(this.pmfmStrategiesHelper.at(i).value);
    }
    return pmfms;
  }


  ngOnInit() {
    super.ngOnInit();

    this.weightPmfmStrategiesTable.onConfirmEditCreateRow.subscribe(res => {
      //this.form.controls.pmfmStrategies.setValue([res.currentData, res.currentData, res.currentData, res.currentData, res.currentData, res.currentData]);
      this.form.controls.pmfmStrategies.patchValue(this.setPmfmStrategies());

      // this.form.controls.updateDate.setValue(new Date());
      //this.form.markAsPristine();
      this.markAsDirty();
    });

    this.sizePmfmStrategiesTable.onConfirmEditCreateRow.subscribe(res => {
      //this.form.controls.pmfmStrategies.setValue([res.currentData, res.currentData, res.currentData, res.currentData, res.currentData, res.currentData]);
      this.form.controls.pmfmStrategies.patchValue(this.setPmfmStrategies());

      // this.form.controls.updateDate.setValue(new Date());
      //this.form.markAsPristine();
      this.markAsDirty();
    });

    this.maturityPmfmStrategiesTable.onConfirmEditCreateRow.subscribe(res => {
      //this.form.controls.pmfmStrategies.setValue([res.currentData, res.currentData, res.currentData, res.currentData, res.currentData, res.currentData]);
      this.form.controls.pmfmStrategies.patchValue(this.setPmfmStrategies());

      // this.form.controls.updateDate.setValue(new Date());
      //this.form.markAsPristine();
      this.markAsDirty();
    });

     // register year field changes
    this.registerSubscription(
      this.form.get('creationDate').valueChanges
        .subscribe(async (date : Moment) => this.onDateChange(date) )
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
    this.registerAutocompleteField('laboratory', {
      suggestFn: (value, filter) => this.suggest(value, {
          ...filter, statusId : 1
        },
        'Department',
        this.enableLaboratoryFilter),
      columnSizes : [4,6],
      mobile: this.settings.mobile
    });

    // fishingArea autocomplete
    this.registerAutocompleteField('fishingArea', {
      suggestFn: (value, filter) => this.suggest(value, {
          ...filter, statusId : 1, Id : 111
        },
        'Location',
        this.enableFishingAreaFilter),
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

    // Calcified type combo ------------------------------------------------------------
    // this.registerAutocompleteField('calcifiedType', {
    //   attributes: ['name'],
    //   columnNames: [ 'REFERENTIAL.NAME'],
    //   items: this._calcifiedTypeSubject,
    //   mobile: this.mobile
    // });
    // this.loadCalcifiedType();

    // const res = await this.referentialRefService.loadAll(0, 200, null,null,
    //   {
    //     entityName: 'Fraction',
    //     searchAttribute: "description",
    //     searchText: "individu"
    //   });
    // return res.data;

    this.registerAutocompleteField('calcifiedType', {
        suggestFn: (value, filter) => this.suggest(value, {
            ...filter, statusId : 1
          },
          'Fraction',
          this.enableCalcifiedTypeFilter),
      attributes: ['name'],
      columnNames: [ 'REFERENTIAL.NAME'],
        columnSizes: [2,10],
        mobile: this.settings.mobile
    });

    // set default mask
    this.sampleRowMask = [...moment().year().toString().split(''), '-', 'B', 'I', '0', '-', /\d/, /\d/, /\d/, /\d/];

    //init helpers
    // this.initCalcifiedTypeHelper();
    this.initDepartmentHelper();
    //this.initFishingAreaHelper();
    this.initTaxonNameHelper();
    this.initPmfmStrategiesHelper();
    this.initAppliedStrategiesHelper();
    this.initAppliedPeriodHelper();

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
      case 'laboratory':
        this.enableLaboratoryFilter = value = !this.enableLaboratoryFilter;
        break;
      case 'fishingArea':
        this.enableFishingAreaFilter = value = !this.enableFishingAreaFilter;
        break;
      case 'taxonName':
        this.enableTaxonNameFilter = value = !this.enableTaxonNameFilter;
        break;
      case 'calcifiedType':
        this.enableCalcifiedTypeFilter = value = !this.enableCalcifiedTypeFilter;
        //this.loadCalcifiedType();
        break;
      default:
        break;
    }
    this.markForCheck();
    console.debug(`[planification] set enable filtered ${fieldName} items to ${value}`);
  }

  setValueSimpleStrategy(data: Strategy, opts?: { emitEvent?: boolean; onlySelf?: boolean }) {
    console.debug("[planification-form] Setting Strategy value", data);
    if (!data) return;


    console.log(data);

    // QUICKFIX label to remove as soon as possible
    data.label = data.label.replace(/_/g, "-");

    // Resize strategy department array
    this.strategyDepartmentHelper.resize(Math.max(1, data.strategyDepartments.length));

    // Resize strategy department array
    this.appliedStrategiesHelper.resize(Math.max(1, data.appliedStrategies.length));

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
    console.log(this.form);

      // EOTP
      /*const eotpControl = this.form.get("analyticReference");
      let eotp = data.analyticReference;
      let eotpValues = this._eotpSubject.getValue();
      let eotpObject = eotpValues.find(e => e.label && e.label === eotp);
      eotpControl.patchValue(eotpObject);*/

      // fixme get eotp from referential by label = data.analyticReference
      let  analyticReferenceToSet : IReferentialRef = new ReferentialRef();
      analyticReferenceToSet.label = data.analyticReference;
      this.form.get('analyticReference').setValue(analyticReferenceToSet);


      // const laboratoriesControl = this.laboratoriesForm;
      // let strategyDepartments = data.strategyDepartments;
      // let laboratories = strategyDepartments.map(strategyDepartment => { return strategyDepartment.department;});
      // this.strategyDepartmentHelper.resize(Math.max(1, data.strategyDepartments.length));
      // laboratoriesControl.patchValue(laboratories);



    //   // TAXONS
    const taxonNamesControl = this.taxonNamesForm;
    let taxonsNames = data.taxonNames;
    let taxons = taxonsNames.map(taxonsNames => { return taxonsNames.taxonName;});
    this.taxonNameHelper.resize(Math.max(1, data.taxonNames.length));
    taxonNamesControl.patchValue(taxons);

    // let appliedStrategiesValues = [quarterEffort1, quarterEffort2, quarterEffort3, quarterEffort4];
    // // appliedStrategiesValues = appliedStrategiesValues.concat(fishingArea);
    // // this.appliedStrategiesHelper.resize(appliedStrategiesValues.length);
    // const appliedPeriodControl = this.appliedPeriodsForm;
    // appliedPeriodControl.patchValue(appliedStrategiesValues);


      // WEIGHT PMFMS
    //   const weightPmfmsControl = this.form.get("weightPmfmStrategies");
      //  let weightPmfmStrategy = (data.pmfmStrategies || []).filter(p => p.pmfm && p.pmfm.parameter && p.pmfm.parameter.label === 'WEIGHT');

      // if (weightPmfmStrategy)
      // {
      //   let weightPmfm = weightPmfmStrategy.map(pmfmStrategy =>  {return pmfmStrategy.pmfm;});
        //weightPmfmsControl.patchValue(weightPmfm);
    //     weightPmfmsControl.patchValue(weightPmfmStrategy);

        // this.weightPmfmStrategiesTable.value = weightPmfmStrategy || [];
      // }

      // Size
    //   const sizePmfmsControl = this.form.get("sizePmfmStrategies");
    //   const sizeValues = ['LENGTH_PECTORAL_FORK', 'LENGTH_CLEITHRUM_KEEL_CURVE', 'LENGTH_PREPELVIC', 'LENGTH_FRONT_EYE_PREPELVIC', 'LENGTH_LM_FORK', 'LENGTH_PRE_SUPRA_CAUDAL', 'LENGTH_CLEITHRUM_KEEL', 'LENGTH_LM_FORK_CURVE', 'LENGTH_PECTORAL_FORK_CURVE', 'LENGTH_FORK_CURVE', 'STD_STRAIGTH_LENGTH', 'STD_CURVE_LENGTH', 'SEGMENT_LENGTH', 'LENGTH_MINIMUM_ALLOWED', 'LENGTH', 'LENGTH_TOTAL', 'LENGTH_STANDARD', 'LENGTH_PREANAL', 'LENGTH_PELVIC', 'LENGTH_CARAPACE', 'LENGTH_FORK', 'LENGTH_MANTLE'];
    //   let sizePmfmStrategy = (data.pmfmStrategies || []).filter(p => p.pmfm && p.pmfm.parameter && sizeValues.includes(p.pmfm.parameter.label));
    //   if (sizePmfmStrategy)
    //   {
    // //     let sizePmfm = sizePmfmStrategy.map(pmfmStrategy =>  {return pmfmStrategy.pmfm;});
    // //     sizePmfmsControl.patchValue(sizePmfm);
    //     // this.sizePmfmStrategiesTable.value = sizePmfmStrategy || [];
    //   }

      // SEX

      const pmfmStrategiesControl = this.pmfmStrategiesForm;


      let age = data.pmfmStrategies.filter(p => p.pmfm && p.pmfm.parameter && p.pmfm.parameter.label ===  "AGE");
      let sex = data.pmfmStrategies.filter(p => p.pmfm && p.pmfm.parameter && p.pmfm.parameter.label ===  "SEX");


      // MATURITY PMFMS
    //   const maturityPmfmsControl = this.form.get("maturityPmfmStrategies");
    //   const maturityValues = ['MATURITY_STAGE_3_VISUAL', 'MATURITY_STAGE_4_VISUAL', 'MATURITY_STAGE_5_VISUAL', 'MATURITY_STAGE_6_VISUAL', 'MATURITY_STAGE_7_VISUAL', 'MATURITY_STAGE_9_VISUAL'];
    //   let maturityPmfmStrategy = (data.pmfmStrategies || []).filter(p => p.pmfm && p.pmfm.parameter && maturityValues.includes(p.pmfm.parameter.label));
    //   if (maturityPmfmStrategy)
    //   {
    // //     let maturityPmfm = maturityPmfmStrategy.map(pmfmStrategy =>  {return pmfmStrategy.pmfm;});
    // //     maturityPmfmsControl.patchValue(maturityPmfm);
    //     // this.maturityPmfmStrategiesTable.value = maturityPmfmStrategy || [];
    //   }

    // let pmfmStrategies = [ sex ? true : false, age ? true : false, weightPmfmStrategy, sizePmfmStrategy, maturityPmfmStrategy];
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


        // CALCIFIED TYPES
      // const calcifiedTypesControl = this.calcifiedTypesForm;
      // let calcifiedTypesPmfmStrategy = (data.pmfmStrategies || []).filter(p => p.fractionId && !p.pmfm);

    // return {
    //   id: pmfmStrategy.fractionId,
    //   entityName: "Fraction",
    //   __typename: "ReferentialVO" || undefined
    // };



      let calcifiedTypesPmfmStrategy = (data.pmfmStrategies || []).filter(p => p.fractionId && !p.pmfm);
      if (calcifiedTypesPmfmStrategy)
      {
        calcifiedTypesPmfmStrategy.forEach(pmfmStrategy => {
          pmfmStrategies.push({
            id:pmfmStrategy.fractionId,
            label:null,
            name:null,
            rankOrder:null
          })
        });
        // calcifiedTypesPmfmStrategy.map(pmfmStrategy =>  {
        //   return new ReferentialRef ({
        //     id:pmfmStrategy.fractionId,
        //     label:null, name:null,rankOrder:null}
        //     );
        // });

    //     // Not initialiezd since loadCalcifiedTypes ares loaded asynchronously
    //     //this._calcifiedTypeSubject.getValue();
    //     // @ts-ignore
    //     //for (let item of this._calcifiedTypeSubject.asObservable())
    //     //{
    //     //  item.toString();
    //    // }
        // this.calcifiedTypeHelper.resize(Math.max(1, calcifiedTypesPmfmStrategy.length));
        // calcifiedTypesControl.patchValue(calcifiedTypesFractionRefIds);
      }


      this.pmfmStrategiesHelper.resize(Math.max(6, pmfmStrategies.length));
      pmfmStrategiesControl.patchValue(pmfmStrategies);
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


  // save button
  save(){
    console.log("save work");

    console.log(this.form.get("comments"));

    /* console.log("comment : "+this.form.get("comment").value);*/
  }

  cancel(){
    console.log("cancel works");
  }

  add(){
    console.log("add works");
  }

  close(){
    console.log("close works");
  }

  // TaxonName Helper -----------------------------------------------------------------------------------------------
  protected initTaxonNameHelper() {
    // appliedStrategies => appliedStrategies.location ?
      this.taxonNameHelper = new FormArrayHelper<ReferentialRef>(
        FormArrayHelper.getOrCreateArray(this.formBuilder, this.form, 'taxonNames'),
        (taxonName) => this.formBuilder.control(taxonName && taxonName.name || null, [Validators.required, SharedValidators.entity]),
        ReferentialUtils.equals,
        ReferentialUtils.isEmpty,
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
      (pmfmStrategy) => this.formBuilder.control(pmfmStrategy || null, [Validators.required]),
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
      this.laboratoryFocusIndex = this.strategyDepartmentHelper.size() - 1;
    }
  }
  // CalcifiedTypeHelper -----------------------------------------------------------------------------------------------

  // protected initCalcifiedTypeHelper() {
  //   this.calcifiedTypeHelper = new FormArrayHelper<ReferentialRef>(
  //     FormArrayHelper.getOrCreateArray(this.formBuilder, this.form, 'calcifiedTypes'),
  //     (calcifiedType) => this.formBuilder.control(calcifiedType || null, [Validators.required, SharedValidators.entity]),
  //     ReferentialUtils.equals,
  //     ReferentialUtils.isEmpty,
  //     {
  //       allowEmptyArray: false
  //     }
  //   );
  //   // Create at least one calcifiedType
  //   if (this.calcifiedTypeHelper.size() === 0) {
  //     this.calcifiedTypeHelper.resize(1);
  //   }
  // }
  // addCalcifiedType() {
  //   this.calcifiedTypeHelper.add();
  //   if (!this.mobile) {
  //     this.calcifiedTypeFocusIndex = this.calcifiedTypeHelper.size() - 1;
  //   }
  // }

  // Calcified Type ---------------------------------------------------------------------------------------------
  protected async loadCalcifiedType() {
    // const calcifiedTypeControl = this.form.get('calcifiedTypes');
    // calcifiedTypeControl.enable();
    // // Refresh filtred departments
    // if (this.enableCalcifiedTypeFilter) {
    //   const allcalcifiedTypes = await this.loadFilteredCalcifiedTypesMethod();
    //   this._calcifiedTypeSubject.next(allcalcifiedTypes);
    // } else {
    //   // TODO Refresh filtred departments
    //   const filtredCalcifiedTypes = await this.loadCalcifiedTypesMethod();
    //   this._calcifiedTypeSubject.next(filtredCalcifiedTypes);
    // }
  }
  // Load CalcifiedTypes Service
  protected async loadCalcifiedTypesMethod(): Promise<ReferentialRef[]> {
    const res = await this.referentialRefService.loadAll(0, 200, null,null,
      {
        entityName: 'Fraction',
        searchAttribute: "description",
        searchText: "individu"
      });
    return res.data;
  }

  //TODO : Load filtred CalcifiedTypes Service : another service to implement
  protected async loadFilteredCalcifiedTypesMethod(): Promise<ReferentialRef[]> {
    const res = await this.referentialRefService.loadAll(0, 1, null,null,
      {
        entityName: 'Fraction',
        searchAttribute: "description",
        searchText: "individu"
      });
    return res.data;
  }

  protected markForCheck() {
    if (this.cd) this.cd.markForCheck();
  }

}
