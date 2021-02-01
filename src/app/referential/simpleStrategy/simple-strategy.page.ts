import {ChangeDetectionStrategy, Component, Injector, OnInit, ViewChild} from "@angular/core";
import {FormBuilder, FormGroup} from "@angular/forms";
import {ActivatedRoute} from "@angular/router";
import * as moment from "moment";
import {HistoryPageReference} from "src/app/core/services/model/settings.model";
import {PlatformService} from "src/app/core/services/platform.service";
import {AccountService} from "../../core/services/account.service";
import {ProgramProperties} from "../services/config/program.config";
import {PmfmStrategy} from "../services/model/pmfm-strategy.model";
import {Strategy} from "../services/model/strategy.model";
import {PmfmService} from "../services/pmfm.service";
import {StrategyService} from "../services/strategy.service";
import {SimpleStrategyForm} from "./simple-strategy.form";
import {AppEntityEditor} from "../../core/form/editor.class";
import {isNil, isNotNil, toNumber} from "../../shared/functions";
import {EntityServiceLoadOptions} from "../../shared/services/entity-service.class";
import {firstNotNilPromise} from "../../shared/observables";
import {BehaviorSubject} from "rxjs";
import {Program} from "../services/model/program.model";
import {ProgramService} from "../services/program.service";
import {AcquisitionLevelCodes, PmfmIds} from "../services/model/model.enum";
import {StatusIds} from "../../core/services/model/model.enum";


@Component({
  selector: 'app-simple-strategy',
  templateUrl: 'simple-strategy.page.html',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SimpleStrategyPage extends AppEntityEditor<Strategy, StrategyService> implements OnInit {

  propertyDefinitions = Object.getOwnPropertyNames(ProgramProperties).map(name => ProgramProperties[name]);
  $program = new BehaviorSubject<Program>(null);

  @ViewChild('form', { static: true }) simpleStrategyForm: SimpleStrategyForm;

  get form(): FormGroup {
    return this.simpleStrategyForm.form;
  }

  constructor(
    protected injector: Injector,
    protected formBuilder: FormBuilder,
    protected accountService: AccountService,
    protected strategyService: StrategyService,
    protected programService: ProgramService,
    protected activatedRoute: ActivatedRoute,
    protected pmfmService: PmfmService,
    protected platform: PlatformService
  ) {
    super(injector, Strategy, strategyService,
      {
        pathIdAttribute: 'strategyId',
        tabCount: 1,
        autoUpdateRoute: !platform.mobile,
        autoOpenNextTab: false
      });
    // default values
    this.defaultBackHref = "/referential/programs";
    this._enabled = this.accountService.isAdmin();
  }

  ngOnInit() {
    super.ngOnInit();

    // Update back href, when program changed
    this.registerSubscription(
      this.$program.subscribe(program => this.setProgram(program))
    );
  }

  async load(id?: number, opts?: EntityServiceLoadOptions): Promise<void> {
    // Force the load from network
    return super.load(id, {...opts, fetchPolicy: "network-only"});
  }

  protected async onNewEntity(data: Strategy, options?: EntityServiceLoadOptions): Promise<void> {
    await super.onNewEntity(data, options);

    // Load program, form the route path
    if (options && isNotNil(options.programId)) {
      const program = await this.programService.load(options.programId);
      this.$program.next(program);

      data.programId = program && program.id;
    }

    // Set defaults
    data.statusId = toNumber(data.statusId, StatusIds.ENABLE);
    data.creationDate = moment();

    // Fill default PmfmStrategy (e.g. the PMFM to store the strategy's label)
    this.fillPmfmStrategyDefaults(data);
  }

  protected async onEntityLoaded(data: Strategy, options?: EntityServiceLoadOptions): Promise<void> {
    await super.onEntityLoaded(data, options);

    // Load program, form the entity's program
    if (data && isNotNil(data.programId)) {
      const program = await this.programService.load(data.programId);
      this.$program.next(program);
    }

    // Fill default PmfmStrategy (if need)
    this.fillPmfmStrategyDefaults(data);
  }

  protected registerForms() {
    this.addChildForm(this.simpleStrategyForm);
  }

  protected canUserWrite(data: Strategy): boolean {
    return this.strategyService.canUserWrite(data);
  }

  protected setProgram(program: Program) {
    if (program && isNotNil(program.id)) {
      this.defaultBackHref = `/referential/programs/${program.id}?tab=1`;
      this.markForCheck();
    }
  }

  /**
   * Compute the title
   * @param data
   * @param opts
   */
  protected async computeTitle(data: Strategy, opts?: {
    withPrefix?: boolean;
  }): Promise<string> {

    const program = await firstNotNilPromise(this.$program);
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

  protected setValue(data: Strategy, opts?: { emitEvent?: boolean; onlySelf?: boolean }) {
    if (!data) return; // Skip
    this.simpleStrategyForm.value = data;
  }

  protected async getJsonValueToSave(): Promise<Strategy> {

    const json = await this.simpleStrategyForm.getValue();

    // Add default PmfmStrategy
    this.fillPmfmStrategyDefaults(json);

    console.log('TODO BLA json to save: ', json);
    return json;
  }


  /**
   * Fill default PmfmStrategy (e.g. the PMFM to store the strategy's label)
   * @param data
   */
  fillPmfmStrategyDefaults(data: Strategy) {
    data.pmfmStrategies = data.pmfmStrategies || [];

    // Find existing pmfm
    let pmfmStrategyLabel: PmfmStrategy = data.pmfmStrategies.find(pmfm =>
      toNumber(pmfm.pmfmId, pmfm.pmfm && pmfm.pmfm.id) === PmfmIds.STRATEGY_LABEL);

    // Create if not exists
    if (!pmfmStrategyLabel) {
      pmfmStrategyLabel = <PmfmStrategy>{};
      data.pmfmStrategies.push(pmfmStrategyLabel);
    }

    pmfmStrategyLabel.pmfmId = PmfmIds.STRATEGY_LABEL;
    pmfmStrategyLabel.strategyId = data.id;
    pmfmStrategyLabel.isMandatory = true;
    pmfmStrategyLabel.acquisitionNumber = 1;
    pmfmStrategyLabel.acquisitionLevel = AcquisitionLevelCodes.LANDING;
    pmfmStrategyLabel.rankOrder = 1; // Should be the only one PmfmStrategy on Landing
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

