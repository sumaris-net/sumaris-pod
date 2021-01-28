import {ChangeDetectionStrategy, Component, Injector, OnInit, ViewChild} from "@angular/core";
import {FormBuilder, FormGroup} from "@angular/forms";
import {ActivatedRoute} from "@angular/router";
import * as moment from "moment";
import {HistoryPageReference} from "src/app/core/services/model/settings.model";
import {PlatformService} from "src/app/core/services/platform.service";
import {AccountService} from "../../core/services/account.service";
import {ReferentialRef, ReferentialUtils} from "../../core/services/model/referential.model";
import {FormFieldDefinitionMap} from "../../shared/form/field.model";
import {ProgramProperties} from "../services/config/program.config";
import {PmfmStrategy} from "../services/model/pmfm-strategy.model";
import {Strategy, StrategyDepartment} from "../services/model/strategy.model";
import {PmfmService} from "../services/pmfm.service";
import {StrategyService} from "../services/strategy.service";
import {StrategyValidatorService} from "../services/validator/strategy.validator";
import {SimpleStrategyForm} from "./simple-strategy.form";
import {AppEntityEditor} from "../../core/form/editor.class";
import {isNil, isNotNil, isNotNilOrBlank} from "../../shared/functions";
import {EntityServiceLoadOptions} from "../../shared/services/entity-service.class";
import {firstNotNilPromise} from "../../shared/observables";
import {BehaviorSubject} from "rxjs";
import {Program} from "../services/model/program.model";
import {ProgramService} from "../services/program.service";


@Component({
  selector: 'app-simple-strategy',
  templateUrl: 'simple-strategy.page.html',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SimpleStrategyPage extends AppEntityEditor<Strategy, StrategyService> implements OnInit {

  propertyDefinitions = Object.getOwnPropertyNames(ProgramProperties).map(name => ProgramProperties[name]);
  form: FormGroup;
  programSubject = new BehaviorSubject<Program>(null);

  @ViewChild('simpleStrategyForm', { static: true }) simpleStrategyForm: SimpleStrategyForm;

  constructor(
    protected injector: Injector,
    protected formBuilder: FormBuilder,
    protected accountService: AccountService,
    protected validatorService: StrategyValidatorService,
    dataService: StrategyService,
    protected programService: ProgramService,
    protected activatedRoute: ActivatedRoute,
    protected pmfmService: PmfmService,
    protected platform: PlatformService
  ) {
    super(injector, Strategy, dataService,
      {
        pathIdAttribute: 'strategyId',
        tabCount: 1,
        autoUpdateRoute: !platform.mobile,
        autoOpenNextTab: false
      });
    this.form = validatorService.getFormGroup();
    // default values
    this.defaultBackHref = "/referential/programs";
    this._enabled = this.accountService.isAdmin();
  }

  ngOnInit() {
    super.ngOnInit();

    // Update back href, when program changed
    this.registerSubscription(
      this.programSubject.subscribe(program => {
        if (program && isNotNil(program.id)) {
          this.defaultBackHref = `/referential/programs/${program.id}?tab=1`;
          this.markForCheck();
        }
      }));
  }

  async load(id?: number, opts?: EntityServiceLoadOptions): Promise<void> {
    // Force the load from network
    return super.load(id, {...opts, fetchPolicy: "network-only"});
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

    const program = await firstNotNilPromise(this.programSubject);
    let i18nSuffix = program.getProperty(ProgramProperties.I18N_SUFFIX);
    i18nSuffix = i18nSuffix !== 'legacy' && i18nSuffix || '';

    // new strategy
    if (!data || isNil(data.id)) {
      return await this.translate.get(`PROGRAM.STRATEGY.NEW.${i18nSuffix}TITLE`).toPromise();
    }

    // Existing strategy
    return await this.translate.get(`PROGRAM.STRATEGY.EDIT.${i18nSuffix}TITLE`, {
      program: program.label,
      label: data && data.label
    }).toPromise() as string;
  }

  protected getFirstInvalidTabIndex(): number {
    if (this.simpleStrategyForm.invalid) return 0;
    return 0;
  }

  protected registerForms() {
    this.addChildForm(this.simpleStrategyForm);
  }

  protected setValue(data: Strategy, opts?: { emitEvent?: boolean; onlySelf?: boolean }) {
    if (!data) return; // Skip
    this.simpleStrategyForm.value = data;
  }

  protected async getJsonValueToSave(): Promise<Strategy> {

    const data = this.simpleStrategyForm.value;

    data.name = data.label || data.name;
    data.analyticReference = data.analyticReference && data.analyticReference.label || data.analyticReference;

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


    const year = data.year ? moment(data.year).year() : moment().year();

    //Fishig Area + Efforts --------------------------------------------------------------------------------------------
    const appliedStrategies = data.appliedStrategies;
    // append efforts (trick is that effots are added to the first appliedStrategy of the array)
    if (appliedStrategies.length) {
      const appliedPeriods = data.appliedPeriods;
      appliedStrategies[0].appliedPeriods = appliedPeriods.filter(period => isNotNil(period.acquisitionNumber));
      // Set selected year
      appliedStrategies[0].appliedPeriods.forEach(p => {
        p.startDate = moment(p.startDate).set('year', year);
        p.endDate = moment(p.endDate).set('year', year);
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
      levelLabels: [label]
    },
      {
        withTotal: false,
        withDetails: true
      });
    return res.data;
  }


  protected async onNewEntity(data: Strategy, options?: EntityServiceLoadOptions): Promise<void> {
    await super.onNewEntity(data, options);

    const program = await this.programService.load(options.programId);
    this.programSubject.next(program);

    data.programId = program.id;

    data.programId = data.programId || this.activatedRoute.snapshot.params['id'];
    data.statusId = data.statusId || 1;
    data.creationDate = moment();
  }

  protected async onEntityLoaded(data: Strategy, options?: EntityServiceLoadOptions): Promise<void> {
    await super.onEntityLoaded(data, options);

    const program = await this.programService.load(data.programId);
    this.programSubject.next(program);
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

