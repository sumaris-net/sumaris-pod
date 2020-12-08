import {Directive, Injector, OnInit} from '@angular/core';

import {ReferentialRef} from '../../core/core.module';
import {BehaviorSubject, Subject} from 'rxjs';
import {isNil, isNotNil, isNotNilOrBlank} from '../../shared/functions';
import {distinctUntilChanged, filter, switchMap, tap} from "rxjs/operators";
import {Program} from "../../referential/services/model/program.model";
import {ProgramService} from "../../referential/services/program.service";
import {EntityService, EntityServiceLoadOptions} from "../../shared/services/entity-service.class";
import {AppEntityEditor, AppEditorOptions} from "../../core/form/editor.class";
import {ReferentialUtils} from "../../core/services/model/referential.model";
import {HistoryPageReference} from "../../core/services/model/settings.model";
import {RootDataEntity} from "../services/model/root-data-entity.model";
import {
  MatAutocompleteConfigHolder, MatAutocompleteFieldAddOptions,
  MatAutocompleteFieldConfig
} from "../../shared/material/autocomplete/material.autocomplete";
import {AddToPageHistoryOptions} from "../../core/services/local-settings.service";


@Directive()
export abstract class AppRootDataEditor<
    T extends RootDataEntity<T>,
    S extends EntityService<T> = EntityService<T>
  >
  extends AppEntityEditor<T, S>
  implements OnInit {

  protected programService: ProgramService;
  protected autocompleteHelper: MatAutocompleteConfigHolder;

  autocompleteFields: { [key: string]: MatAutocompleteFieldConfig };

  programSubject = new BehaviorSubject<string>(null);
  onProgramChanged = new BehaviorSubject<Program>(null);

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

    this.programService = injector.get(ProgramService);

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
          switchMap(programLabel => this.programService.watchByLabel(programLabel)),
          tap(program => this.onProgramChanged.next(program))
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

  /* -- protected methods -- */

  protected registerAutocompleteField<T = any, F = any>(fieldName: string,
                                                        opts?: MatAutocompleteFieldAddOptions<T, F>): MatAutocompleteFieldConfig<T, F> {
    return this.autocompleteHelper.add(fieldName, opts);
  }

  protected canUserWrite(data: T): boolean {
    return isNil(data.validationDate) && this.programService.canUserWrite(data);
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
    let parentUrl = this.getParentPageUrl();
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

    const res = await super.getValue();

    // Re add program, because program control can be disabled
    res.program = ReferentialRef.fromObject(this.form.controls['program'].value);

    return res;
  }
}
