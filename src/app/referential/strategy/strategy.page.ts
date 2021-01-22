import {ChangeDetectionStrategy, Component, Injector, OnInit, ViewChild} from "@angular/core";
import {ValidatorService} from "@e-is/ngx-material-table";
import {FormBuilder, FormGroup} from "@angular/forms";
import {Strategy} from "../services/model/strategy.model";
import {AccountService} from "../../core/services/account.service";
import {ReferentialUtils} from "../../core/services/model/referential.model";
import {ReferentialRefService} from "../services/referential-ref.service";
import {ModalController} from "@ionic/angular";
import {StrategyForm} from "./strategy.form";
import {HistoryPageReference} from "../../core/services/model/history.model";
import {StrategyValidatorService} from "../services/validator/strategy.validator";
import {StrategyService} from "../services/strategy.service";
import {ProgramService} from "../services/program.service";
import {BehaviorSubject} from "rxjs";
import {Program} from "../services/model/program.model";
import {ReferentialForm} from "../form/referential.form";
import {firstNotNilPromise} from "../../shared/observables";
import {debounceTime, filter} from "rxjs/operators";
import {EntityServiceLoadOptions} from "../../shared/services/entity-service.class";
import {AppEntityEditor} from "../../core/form/editor.class";
import {isNil, isNotNil} from "../../shared/functions";
import {environment} from "../../../environments/environment";

export enum AnimationState {
  ENTER = 'enter',
  LEAVE = 'leave'
}

@Component({
  selector: 'app-strategy',
  templateUrl: 'strategy.page.html',
  providers: [
    {provide: ValidatorService, useExisting: StrategyValidatorService}
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class StrategyPage extends AppEntityEditor<Strategy, StrategyService> implements OnInit {

  programSubject = new BehaviorSubject<Program>(null);

  @ViewChild('referentialForm', { static: true }) referentialForm: ReferentialForm;
  @ViewChild('strategyForm', { static: true }) strategyForm: StrategyForm;

  get form(): FormGroup {
    return this.strategyForm.form;
  }

  constructor(
    protected injector: Injector,
    protected formBuilder: FormBuilder,
    protected accountService: AccountService,
    protected validatorService: StrategyValidatorService,
    dataService: StrategyService,
    protected programService: ProgramService,
    protected referentialRefService: ReferentialRefService,
    protected modalCtrl: ModalController
  ) {
    super(injector,
      Strategy,
      dataService,
      {
        pathIdAttribute: 'strategyId'
      });

    // default values
    this.defaultBackHref = "/referential/programs";
    this._enabled = this.accountService.isAdmin();
    this.tabCount = 4;

    this.debug = !environment.production;
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

    this.registerSubscription(
      this.referentialForm.form.valueChanges
        .pipe(debounceTime(100), filter(() => this.referentialForm.valid))
        .subscribe(value => this.strategyForm.form.patchValue({...value, entityName: undefined})));
  }

  /* -- protected methods -- */

  async load(id?: number, opts?: EntityServiceLoadOptions): Promise<void> {
    // Force the load from network
    return super.load(id, {...opts, fetchPolicy: "network-only"});
  }

  protected canUserWrite(data: Strategy): boolean {
    // TODO : check user is in strategy managers
    return (this.isNewData && this.accountService.isAdmin())
      || (ReferentialUtils.isNotEmpty(data) && this.accountService.isSupervisor());

  }

  enable(opts?: {onlySelf?: boolean, emitEvent?: boolean; }) {
    super.enable(opts);

    if (!this.isNewData) {
      this.form.get('label').disable();
    }
  }

  protected registerForms() {
    this.addChildForms([
      this.referentialForm,
      this.strategyForm
    ]);
  }

  protected async onNewEntity(data: Strategy, options?: EntityServiceLoadOptions): Promise<void> {
    await super.onNewEntity(data, options);

    const program = await this.programService.load(options.programId);
    this.programSubject.next(program);

    data.programId = program.id;
  }

  protected async onEntityLoaded(data: Strategy, options?: EntityServiceLoadOptions): Promise<void> {
    await super.onEntityLoaded(data, options);

    const program = await this.programService.load(data.programId);
    this.programSubject.next(program);
  }

  protected setValue(data: Strategy) {
    if (!data) return; // Skip

    this.referentialForm.setValue(data);

    this.strategyForm.updateView(data);

    this.markAsPristine();
  }

  protected async getJsonValueToSave(): Promise<any> {

    if (this.strategyForm.dirty) {
      const saved = await this.strategyForm.save();
      if (!saved) return; // Skip
    }
    const data = this.strategyForm.form.value;

    // Re add label, because missing when field disable
    data.label = this.referentialForm.form.get('label').value;

    return data;
  }

  protected async computeTitle(data: Strategy): Promise<string> {
    // new data
    if (!data || isNil(data.id)) {
      return this.translate.get('PROGRAM.STRATEGY.NEW.TITLE').toPromise();
    }

    // Existing data
    const program = await firstNotNilPromise(this.programSubject);
    return this.translate.get('PROGRAM.STRATEGY.EDIT.TITLE', {
      program: program.label,
      label: data.label
    }).toPromise();
  }

  protected async computePageHistory(title: string): Promise<HistoryPageReference> {
    return {
      ...(await super.computePageHistory(title)),
      matIcon: 'date_range',
      title: `${this.data.label} - ${this.data.name}`,
      subtitle: 'REFERENTIAL.ENTITY.PROGRAM'
    };
  }

  protected getFirstInvalidTabIndex(): number {
    if (this.referentialForm.invalid) return 0;
    if (this.strategyForm.invalid) return 1;
    return 0;
  }

  protected markForCheck() {
    this.cd.markForCheck();
  }

}

