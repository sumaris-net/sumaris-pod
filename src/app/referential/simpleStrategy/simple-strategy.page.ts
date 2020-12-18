import {ChangeDetectionStrategy, Component, Injector, Input, OnInit, ViewChild} from "@angular/core";
import {ValidatorService} from "@e-is/ngx-material-table";
import {FormBuilder, FormGroup} from "@angular/forms";
import {
  AppEntityEditor, IReferentialRef,
  isNil, ReferentialRef,
} from "../../core/core.module";
import {
  AppliedPeriod,
  AppliedStrategy,
  Strategy,
  StrategyDepartment,
  TaxonNameStrategy
} from "../services/model/strategy.model";
import {ProgramValidatorService} from "../services/validator/program.validator";
import {
  EntityServiceLoadOptions,
  fadeInOutAnimation, isNotNil
} from "../../shared/shared.module";
import {AccountService} from "../../core/services/account.service";
import {ReferentialUtils} from "../../core/services/model/referential.model";
import {ReferentialRefService} from "../services/referential-ref.service";
import {ModalController} from "@ionic/angular";
import {FormFieldDefinitionMap} from "../../shared/form/field.model";
import {animate, state, style, transition, trigger} from "@angular/animations";
import {ProgramProperties} from "../services/config/program.config";
import {StrategyService} from "../services/strategy.service";
import {SimpleStrategyForm} from "./simple-strategy.form";
import {ActivatedRoute} from "@angular/router";
import {PmfmStrategy} from "../services/model/pmfm-strategy.model";
import * as moment from 'moment'
import {PmfmService} from "../services/pmfm.service";
import { HistoryPageReference } from "src/app/core/services/model/settings.model";

export enum AnimationState {
  ENTER = 'enter',
  LEAVE = 'leave'
}

@Component({
  selector: 'app-simple-strategy',
  templateUrl: 'simple-strategy.page.html',
  providers: [
    {provide: ValidatorService, useExisting: ProgramValidatorService}
  ],
  animations: [fadeInOutAnimation,
    // Fade in
    trigger('fadeIn', [
      state('*', style({opacity: 0, display: 'none', visibility: 'hidden'})),
      state(AnimationState.ENTER, style({opacity: 1, display: 'inherit', visibility: 'inherit'})),
      state(AnimationState.LEAVE, style({opacity: 0, display: 'none', visibility: 'hidden'})),
      // Modal
      transition(`* => ${AnimationState.ENTER}`, [
        style({display: 'inherit',  visibility: 'inherit', transform: 'translateX(50%)'}),
        animate('0.4s ease-out', style({opacity: 1, transform: 'translateX(0)'}))
      ]),
      transition(`${AnimationState.ENTER} => ${AnimationState.LEAVE}`, [
        animate('0.2s ease-out', style({opacity: 0, transform: 'translateX(50%)'})),
        style({display: 'none',  visibility: 'hidden'})
      ]) ])
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SimpleStrategyPage extends AppEntityEditor<Strategy, StrategyService> implements OnInit {

  propertyDefinitions = Object.getOwnPropertyNames(ProgramProperties).map(name => ProgramProperties[name]);
  fieldDefinitions: FormFieldDefinitionMap = {};
  form: FormGroup;
  i18nFieldPrefix = 'STRATEGY.';
  strategyFormState: AnimationState;
  programId: number;

  @ViewChild('simpleStrategyForm', { static: true }) simpleStrategyForm: SimpleStrategyForm;


  constructor(
    protected injector: Injector,
    protected formBuilder: FormBuilder,
    protected accountService: AccountService,
    protected validatorService: ProgramValidatorService,
    dataService: StrategyService,
    protected referentialRefService: ReferentialRefService,
    protected modalCtrl: ModalController,
    protected activatedRoute : ActivatedRoute,
    protected pmfmService: PmfmService,

  ) {
    super(injector,
      Strategy,
      dataService);
    this.form = validatorService.getFormGroup();
    // default values
    this.defaultBackHref = "/referential?entity=Program";
    this._enabled = this.accountService.isAdmin();
    this.tabCount = 4;

  }

  ngOnInit() {
    //  Call editor routing
    super.ngOnInit();
    // Set entity name (required for referential form validator)


    // get program id from route
    this.programId =  40 || this.activatedRoute.snapshot.params['id'];

    this.simpleStrategyForm.entityName = 'simpleStrategyForm';
    this.defaultBackHref = `/referential/program/${this.programId}?tab=2`

  }

   protected canUserWrite(data: Strategy): boolean {
    // TODO : check user is in program managers
    return (this.isNewData && this.accountService.isAdmin())
      || (ReferentialUtils.isNotEmpty(data) && this.accountService.isSupervisor());

  }

    /**
   * Compute the title
   * @param data
   */
  protected async computeTitle(data: Strategy, opts?: {
    withPrefix?: boolean;
  }): Promise<string> {

    // new strategy
    if (!data || isNil(data.id)) {
      return await this.translate.get('PROGRAM.STRATEGY.NEW.SAMPLING_TITLE').toPromise();
    }

    // Existing strategy
    return await this.translate.get('PROGRAM.STRATEGY.EDIT.SAMPLING_TITLE', {
      label: data && data.label
    }).toPromise() as string;

  }

  protected getFirstInvalidTabIndex(): number {
    if (this.simpleStrategyForm.invalid) return 0;
   // TODO
    return 0;
  }

  protected registerForms() {
    this.addChildForms([
      this.simpleStrategyForm
    ]);
  }

  updateView(data: Strategy | null, opts?: { emitEvent?: boolean; openTabIndex?: number; updateRoute?: boolean }) {
    super.updateView(data, opts);

    //if (this.isNewData && this.showBatchTables && isNotEmptyArray(this.batchTree.defaultTaxonGroups)) {
    //  this.batchTree.autoFill();
    //}
  }

  //protected setValue(data: Strategy) {
  protected setValue(data: Strategy, opts?: { emitEvent?: boolean; onlySelf?: boolean }) {

    if (!data) return; // Skip

    // FIXME : must be set in onNewEntity
    data.programId = this.programId;//data.programId ||
    data.statusId= data.statusId || 1;
    this.simpleStrategyForm.value = data;

  }

  protected async getJsonValueToSave(): Promise<Strategy> {

    const data = this.simpleStrategyForm.value;

    data.name = data.name || data.label;

    // FIXME : how to load referenceTaxonId previously ??
    data.taxonNames[0].strategyId = data.taxonNames[0].strategyId || 30;
    data.taxonNames[0].taxonName.referenceTaxonId = 1006;

    // FIXME : how to get privilege previously ??
    data.strategyDepartments.map((dpt : StrategyDepartment) =>{
      let observer : ReferentialRef = new ReferentialRef();
      observer.id =2;
      observer.label ="Observer";
      observer.name ="Observer privilege";
      observer.statusId =1;
      observer.entityName ="ProgramPrivilege";
      dpt.privilege = observer;
    });

    //Fishig Area + Efforts --------------------------------------------------------------------------------------------
    const appliedStrategies = data.appliedStrategies;
    // append efforts (trick is that effots are added to the first appliedStrategy of the array)
    if(appliedStrategies.length){
      const appliedPeriods = data.appliedPeriods;
      appliedStrategies[0].appliedPeriods = appliedPeriods.filter(period => isNotNil(period.acquisitionNumber));
    }
    data.appliedStrategies = appliedStrategies;
    // delete data.appliedPeriods;

    //PMFM + Fractions -------------------------------------------------------------------------------------------------
    let pmfmStrategie = this.simpleStrategyForm.pmfmStrategiesForm.value;
    let pmfmStrategies : PmfmStrategy [] = [];

    let sex = pmfmStrategie[0];
    let age = pmfmStrategie[1];

    // i == 0 age
    // i == 1 sex

    await this.simpleStrategyForm.weightPmfmStrategiesTable.save();
    await this.simpleStrategyForm.sizePmfmStrategiesTable.save();
    await this.simpleStrategyForm.maturityPmfmStrategiesTable.save();


    let lengthList = this.simpleStrategyForm.weightPmfmStrategiesTable.value;
    let sizeList = this.simpleStrategyForm.sizePmfmStrategiesTable.value;
    let maturityList = this.simpleStrategyForm.maturityPmfmStrategiesTable.value;

    for( let  i =0; i<lengthList.length;i++){
      pmfmStrategies.push(lengthList[i]);
    }
    for( let  i =0; i<sizeList.length;i++){
      pmfmStrategies.push(sizeList[i]);
    }
    for( let  i =0; i<maturityList.length;i++){
      pmfmStrategies.push(maturityList[i]);
    }


    let calcifiedTypes = this.simpleStrategyForm.calcifiedTypesForm.value;

    for( let i = 0; i < calcifiedTypes.length; i++){

        let calcifiedType : PmfmStrategy = new PmfmStrategy();
        calcifiedType.strategyId = data.id;
        calcifiedType.pmfm = null;
        calcifiedType.fractionId = calcifiedTypes[i].id;
        calcifiedType.qualitativeValues =undefined;
        calcifiedType.acquisitionLevel='SAMPLE'
        calcifiedType.acquisitionNumber=1;
        calcifiedType.isMandatory = false;
        calcifiedType.rankOrder = 1;

        pmfmStrategies.push(calcifiedType);

    }

    if(sex){
      let pmfmStrategySex : PmfmStrategy = new PmfmStrategy();
      let pmfmSex = await this.getPmfms("SEX");

      pmfmStrategySex.strategyId = data.id;
      pmfmStrategySex.pmfm = pmfmSex[0];
      pmfmStrategySex.fractionId = null;
      pmfmStrategySex.qualitativeValues =undefined;
      pmfmStrategySex.acquisitionLevel='SAMPLE'
      pmfmStrategySex.acquisitionNumber=1;
      pmfmStrategySex.isMandatory = false;
      pmfmStrategySex.rankOrder = 1;

      pmfmStrategies.push(pmfmStrategySex);
    }
    if(age){
      let pmfmStrategyAge : PmfmStrategy = new PmfmStrategy();
      let pmfmAge = await this.getPmfms("AGE");

      pmfmStrategyAge.strategyId = data.id;
      pmfmStrategyAge.pmfm = pmfmAge[0];
      pmfmStrategyAge.fractionId = null;
      pmfmStrategyAge.qualitativeValues =undefined;
      pmfmStrategyAge.acquisitionLevel='SAMPLE'
      pmfmStrategyAge.acquisitionNumber=1;
      pmfmStrategyAge.isMandatory = false;
      pmfmStrategyAge.rankOrder = 1;

      pmfmStrategies.push(pmfmStrategyAge);

    }

    data.pmfmStrategies= pmfmStrategies.map(p => {
      p.acquisitionLevel = 'SAMPLE';
      p.acquisitionNumber = 1;
      p.isMandatory = false;
      p.rankOrder =1;
      return p}).filter(p => p.pmfm || p.fractionId);

  //--------------------------------------------------------------------------------------------------------------------
    console.log(data);
    return data;
  }


  /**
   * get pmfm
   * @param label
   * @protected
   */
   protected async getPmfms(label : string){
     const res = await this.pmfmService.loadAll(0, 1000, null, null, {
         entityName: 'Pmfm',
         levelLabels: [label]
         // searchJoin: "Parameter" is implied in pod filter
       },
       {
         withTotal: false,
         withDetails: true
       });
     return res.data;
   }


   protected async onEntityLoaded(data: Strategy, options?: EntityServiceLoadOptions): Promise<void> {

    // Update back href
    this.defaultBackHref = isNotNil(data.programId) ? `/referential/program/${this.programId}?tab=2` : undefined;

    // data.id = 30;
    this.markForCheck();

  }


  // protected async onNewEntity(data: Strategy, options?: EntityServiceLoadOptions): Promise<void> {

  //   // Read options and query params
  //   console.info(options);
  //   if (options && options.observedLocationId) {

  //     console.debug("[landedTrip-page] New entity: settings defaults...");

  //   } else {
  //     throw new Error("[landedTrip-page] the observedLocationId must be present");
  //   }

  //   const queryParams = this.route.snapshot.queryParams;
  //   // Load the vessel, if any
  //   if (isNotNil(queryParams['strategy'])) {
  //     const strategyId = +queryParams['strategy'];
  //     console.debug(`[landedTrip-page] Loading vessel {${strategyId}}...`);
  //     data.id = strategyId;
  //   }

  // }

  protected addToPageHistory(page: HistoryPageReference) {
    super.addToPageHistory({ ...page, icon: 'list-outline'});
  }
}

