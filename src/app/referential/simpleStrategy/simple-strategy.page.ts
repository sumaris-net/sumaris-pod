import { animate, state, style, transition, trigger } from "@angular/animations";
import { ChangeDetectionStrategy, Component, Injector, OnInit, ViewChild } from "@angular/core";
import { FormBuilder, FormGroup } from "@angular/forms";
import { ActivatedRoute } from "@angular/router";
import { ValidatorService } from "@e-is/ngx-material-table";
import * as moment from "moment";
import { HistoryPageReference } from "src/app/core/services/model/settings.model";
import { PlatformService } from "src/app/core/services/platform.service";
import { AccountService } from "../../core/services/account.service";
import {ReferentialRef, ReferentialUtils} from "../../core/services/model/referential.model";
import { FormFieldDefinitionMap } from "../../shared/form/field.model";
import { ProgramProperties } from "../services/config/program.config";
import { PmfmStrategy } from "../services/model/pmfm-strategy.model";
import {
  Strategy,
  StrategyDepartment
} from "../services/model/strategy.model";
import { PmfmService } from "../services/pmfm.service";
import { StrategyService } from "../services/strategy.service";
import { ProgramValidatorService } from "../services/validator/program.validator";
import { StrategyValidatorService } from "../services/validator/strategy.validator";
import { SimpleStrategyForm } from "./simple-strategy.form";
import {AppEntityEditor} from "../../core/form/editor.class";
import {isNil, isNotNil} from "../../shared/functions";
import {EntityServiceLoadOptions} from "../../shared/services/entity-service.class";

export enum AnimationState {
  ENTER = 'enter',
  LEAVE = 'leave'
}

@Component({
  selector: 'app-simple-strategy',
  templateUrl: 'simple-strategy.page.html',
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
    protected validatorService: StrategyValidatorService,
    dataService: StrategyService,
    protected activatedRoute: ActivatedRoute,
    protected pmfmService: PmfmService,
    protected platform: PlatformService
  ) {
    super(injector, Strategy, dataService,
      {
        pathIdAttribute: 'strategyId',
        tabCount: 3,
        autoUpdateRoute: !platform.mobile,
        autoOpenNextTab: !platform.mobile
      });
    this.form = validatorService.getFormGroup();
    // default values
    this.defaultBackHref = "/referential?entity=Program";
    this._enabled = this.accountService.isAdmin();
    this.tabCount = 4;
  }

  ngOnInit() {
    //  Call editor routing
    super.ngOnInit();
    this.simpleStrategyForm.entityName = 'simpleStrategyForm';
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
  }

  //protected setValue(data: Strategy) {
  protected setValue(data: Strategy, opts?: { emitEvent?: boolean; onlySelf?: boolean }) {
    if (!data) return; // Skip
    this.simpleStrategyForm.value = data;
  }

  protected async getJsonValueToSave(): Promise<Strategy> {

    const data = this.simpleStrategyForm.value;

    data.name = data.label || data.name;

    // TODO BLA: usage non recommandé de isOwnProperty
    data.analyticReference = data.analyticReference && data.analyticReference.hasOwnProperty('label') ? data.analyticReference.label : data.analyticReference;

    // FIXME : description is not nullable in database so we init it with an empty string if nothing provided in the
    data.description = data.description || ' ';

    // get taxonName and delete useless attribute
    data.taxonNames[0].strategyId = data.taxonNames[0].strategyId || 30;
    delete data.taxonNames[0].taxonName.taxonGroupIds;


    // FIXME : how to get privilege previously ??
    data.departments.map((dpt: StrategyDepartment) => {
      const observer = new ReferentialRef();
      observer.id = 2;
      observer.label = "Observer";
      observer.name = "Observer privilege";
      observer.statusId = 1;
      observer.entityName = "ProgramPrivilege";
      dpt.privilege = observer;
    });

    //Fishig Area + Efforts --------------------------------------------------------------------------------------------
    const appliedStrategies = data.appliedStrategies;
    // append efforts (trick is that effots are added to the first appliedStrategy of the array)
    if (appliedStrategies.length) {
      const appliedPeriods = data.appliedPeriods;
      appliedStrategies[0].appliedPeriods = appliedPeriods.filter(period => isNotNil(period.acquisitionNumber));
      // Set selected year
      appliedStrategies[0].appliedPeriods.forEach(p => {
        p.startDate = moment(p.startDate).set('year', moment(data.creationDate).year());
        p.endDate = moment(p.endDate).set('year', moment(data.creationDate).year());
      });
    }
    data.appliedStrategies = appliedStrategies;

    //PMFM + Fractions -------------------------------------------------------------------------------------------------
    const pmfmStrategie = this.simpleStrategyForm.pmfmStrategiesForm.value;
    const sex = pmfmStrategie[0];
    const age = pmfmStrategie[1];

    let pmfmStrategies: PmfmStrategy[] = [];

    // Save before get PMFM values
    await this.simpleStrategyForm.weightPmfmStrategiesTable.save();
    await this.simpleStrategyForm.sizePmfmStrategiesTable.save();

    pmfmStrategies = pmfmStrategies
      .concat(this.simpleStrategyForm.weightPmfmStrategiesTable.value.filter(p => p.pmfm || p.parameterId))
      .concat(this.simpleStrategyForm.sizePmfmStrategiesTable.value.filter(p => p.pmfm || p.parameterId))

    if (sex) {
      const pmfmStrategySex = this.createNewPmfmStrategy(data);
      const pmfmSex = await this.getPmfms("SEX");
      pmfmStrategySex.pmfm = pmfmSex[0];
      pmfmStrategies.push(pmfmStrategySex);
      //If Sex is true
      await this.simpleStrategyForm.maturityPmfmStrategiesTable.save();
      pmfmStrategies = pmfmStrategies.concat(this.simpleStrategyForm.maturityPmfmStrategiesTable.value.filter(p => p.pmfm || p.parameterId))
    }
    if (age) {
      const pmfmStrategyAge = this.createNewPmfmStrategy(data);
      const pmfmAge = await this.getPmfms("AGE");
      pmfmStrategyAge.pmfm = pmfmAge[0];
      pmfmStrategies.push(pmfmStrategyAge);

      // If Age is true
      const pmfmStrategiesFractions = data.pmfmStrategiesFraction.filter(p => p !== null);
      // Pièces calcifiées
      for (let i = 0; i < pmfmStrategiesFractions.length; i++) {
        const pmfmStrategiesFraction = this.createNewPmfmStrategy(data);
        pmfmStrategiesFraction.fractionId = pmfmStrategiesFractions[i].id;
        pmfmStrategies.push(pmfmStrategiesFraction);
      }
      //
    }

    // Add all mandatory fields
    data.pmfmStrategies = pmfmStrategies.map(pmfm => {
      pmfm.acquisitionLevel = 'SAMPLE';
      pmfm.parameter = null;
      pmfm.acquisitionNumber = 1;
      pmfm.isMandatory = false;
      pmfm.rankOrder = 1;
      return pmfm;
    });

    //--------------------------------------------------------------------------------------------------------------------
    return data;
  }

  createNewPmfmStrategy(data: Strategy): PmfmStrategy {
    const pmfmStrategy = new PmfmStrategy();
    pmfmStrategy.strategyId = data.id;
    return pmfmStrategy;
  }

  /**
   * get pmfm
   * @param label
   * @protected
   */
  protected async getPmfms(label: string) {
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
    this.defaultBackHref = isNotNil(data.programId) ? `/referential/program/${data.programId}?tab=2` : this.defaultBackHref;
    this.markForCheck();
  }


  protected async onNewEntity(data: Strategy, options?: EntityServiceLoadOptions): Promise<void> {
    // Read options and query params
    console.info(options);
    if (options && options.id) {
      console.debug("[landedTrip-page] New entity: settings defaults...");

      // init new entity attributs
      data.programId = data.programId || this.activatedRoute.snapshot.params['id'];
      data.statusId = data.statusId || 1;
      data.creationDate = moment();

      this.defaultBackHref = `/referential/program/${data.programId}?tab=2`;

    } else {
      throw new Error("[landedTrip-page] the observedLocationId must be present");
    }
    const queryParams = this.route.snapshot.queryParams;
    // Load the vessel, if any
    if (isNotNil(queryParams['program'])) {
      const programId = +queryParams['program'];
      console.debug(`[landedTrip-page] Loading vessel {${programId}}...`);
      data.programId = programId;
    }
  }

  protected async computePageHistory(title: string): Promise<HistoryPageReference> {
    return {
      ...(await super.computePageHistory(title)),
      matIcon: 'date_range',
      title: `${this.data.label} - ${this.data.name}`,
      subtitle: 'REFERENTIAL.ENTITY.PROGRAM'
    };
  }
}

