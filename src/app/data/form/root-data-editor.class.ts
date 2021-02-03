import {Directive, Injector, OnInit} from '@angular/core';

import {BehaviorSubject} from 'rxjs';
import {changeCaseToUnderscore, isNil, isNotNil, isNotNilOrBlank} from '../../shared/functions';
import {distinctUntilChanged, filter, switchMap, tap} from "rxjs/operators";
import {Program} from "../../referential/services/model/program.model";
import {ProgramService} from "../../referential/services/program.service";
import {EntityServiceLoadOptions, IEntityService} from "../../shared/services/entity-service.class";
import {AppEditorOptions, AppEntityEditor} from "../../core/form/editor.class";
import {ReferentialRef, ReferentialUtils} from "../../core/services/model/referential.model";
import {HistoryPageReference} from "../../core/services/model/settings.model";
import {RootDataEntity} from "../services/model/root-data-entity.model";
import {
  MatAutocompleteConfigHolder,
  MatAutocompleteFieldAddOptions,
  MatAutocompleteFieldConfig
} from "../../shared/material/autocomplete/material.autocomplete";
import {AddToPageHistoryOptions} from "../../core/services/local-settings.service";
import {Strategy} from "../../referential/services/model/strategy.model";
import {StrategyRefService} from "../../referential/services/strategy-ref.service";
import {ProgramRefService} from "../../referential/services/program-ref.service";


@Directive()
// tslint:disable-next-line:directive-class-suffix
export abstract class AppRootDataEditor<
    T extends RootDataEntity<T>,
    S extends IEntityService<T> = IEntityService<T>
  >
  extends AppEntityEditor<T, S>
  implements OnInit {

  protected programRefService: ProgramRefService;
  protected strategyRefService: StrategyRefService;
  protected autocompleteHelper: MatAutocompleteConfigHolder;

  autocompleteFields: { [key: string]: MatAutocompleteFieldConfig };

  programSubject = new BehaviorSubject<string>(undefined);
  $program = new BehaviorSubject<Program>(undefined);
  strategySubject = new BehaviorSubject<string>(undefined);
  $strategy = new BehaviorSubject<Strategy>(undefined);

  get program(): Program {
    return this.$program.getValue();
  }

  get strategy(): Strategy {
    return this.$strategy.getValue();
  }

  protected constructor(
    injector: Injector,
    dataType: new() => T,
    dataService: S,
    options?: AppEditorOptions
  ) {
    super(injector,
      dataType,
      dataService,
      options);

    this.programRefService = injector.get(ProgramRefService);
    this.strategyRefService = injector.get(StrategyRefService);

    // Create autocomplete fields registry
    this.autocompleteHelper = new MatAutocompleteConfigHolder(this.settings && {
      getUserAttributes: (a, b) => this.settings.getFieldDisplayAttributes(a, b)
    });
    this.autocompleteFields = this.autocompleteHelper.fields;

    // FOR DEV ONLY ----
    //this.debug = !environment.production;
  }

  ngOnInit() {
    super.ngOnInit();

    // Watch program, to configure tables from program properties
    this.registerSubscription(
      this.programSubject
        .pipe(
          filter(isNotNilOrBlank),
          distinctUntilChanged(),
          // DEBUG --
          //tap(programLabel => console.debug('DEV - Getting programLabel=' + programLabel)),
          switchMap(programLabel => this.programRefService.watchByLabel(programLabel, {debug: this.debug})),
          tap(program => this.$program.next(program))
        )
        .subscribe());

    // Watch strategy
    this.registerSubscription(
      this.strategySubject
        .pipe(
          distinctUntilChanged(),
          switchMap(strategyLabel => isNotNilOrBlank(strategyLabel)
            ? this.strategyRefService.watchByLabel(strategyLabel)
            : Promise.resolve(undefined) // Allow to have empty strategy (e.g. when user reset the strategy field)
          ),
          tap(strategy => this.$strategy.next(strategy))
        )
        .subscribe());
  }

  async load(id?: number, options?: EntityServiceLoadOptions) {
    await super.load(id, options);

    // New data
    if (isNil(id)) {
      this.startListenProgramChanges();
    }
  }

  enable(opts?: {onlySelf?: boolean, emitEvent?: boolean; }) {
    if (!this.data || isNotNil(this.data.validationDate)) return false;

    super.enable(opts);

    // Leave program disable once saved
    if (!this.isNewData) {
      this.form.controls['program'].disable(opts);
    }

    this.markForCheck();
  }

  setError(error: any) {

    if (error) {
      // Create a details message, from errors in forms (e.g. returned by control())
      const formErrors = error && error.details && error.details.errors;
      if (formErrors) {
        const messages = Object.keys(formErrors)
          .map(field => {
            const fieldErrors = formErrors[field];
            const fieldI18nKey = changeCaseToUnderscore(field).toUpperCase();
            const fieldName = this.translate.instant(fieldI18nKey);
            const errorMsg = Object.keys(fieldErrors).map(errorKey => {
              const key = 'ERROR.FIELD_' + errorKey.toUpperCase();
              return this.translate.instant(key, fieldErrors[key]);
            }).join(', ');
            return fieldName + ": " + errorMsg;
          }).filter(isNotNil);
        if (messages.length) {
          error.details.message = `<ul><li>${messages.join('</li><li>')}</li></ul>`;
        }
      }

    }

    super.setError(error);
  }

  /* -- protected methods -- */

  protected registerAutocompleteField<T = any, F = any>(fieldName: string,
                                                        opts?: MatAutocompleteFieldAddOptions<T, F>): MatAutocompleteFieldConfig<T, F> {
    return this.autocompleteHelper.add(fieldName, opts);
  }

  protected canUserWrite(data: T): boolean {
    return isNil(data.validationDate) && this.programRefService.canUserWrite(data);
  }

  /**
   * Listen program changes (only if new data)
   * @protected
   */
  protected startListenProgramChanges() {
    this.registerSubscription(
      this.form.controls.program.valueChanges
        .subscribe(program => {
          if (ReferentialUtils.isNotEmpty(program)) {
            console.debug("[root-data-editor] Propagate program change: " + program.label);
            this.programSubject.next(program.label);
          }
        }));
  }

  /**
   * Override default function, to add the entity program as subtitle)
   * @param page
   */
  protected async addToPageHistory(page: HistoryPageReference, opts?: AddToPageHistoryOptions) {
    page.subtitle = page.subtitle || this.data.program.label;
    return super.addToPageHistory(page, opts);
  }

  protected getParentPageUrl(withQueryParams?: boolean) {
    let parentUrl = this.defaultBackHref;

    // Remove query params
    if (withQueryParams !== true && parentUrl && parentUrl.indexOf('?') !== -1) {
      parentUrl = parentUrl.substr(0, parentUrl.indexOf('?'));
    }

    return parentUrl;
  }

  protected computePageUrl(id: number|'new') {
    const parentUrl = this.getParentPageUrl();
    return `${parentUrl}/${id}`;
  }

  protected async updateRoute(data: T, queryParams: any): Promise<boolean> {
    const pageUrl = this.computePageUrl(isNotNil(data.id) ? data.id : 'new');
    return await this.router.navigate(pageUrl.split('/') as any[], {
      replaceUrl: true,
      queryParams: this.queryParams
    });
  }

  protected async getValue(): Promise<T> {

    const data = await super.getValue();

    // Re add program, because program control can be disabled
    data.program = ReferentialRef.fromObject(this.form.controls['program'].value);

    return data;
  }

  protected computeStrategy(program: Program, data: T): Strategy {
    return null; // TODO BLA
  }
}
