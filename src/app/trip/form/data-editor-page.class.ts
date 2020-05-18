import { Injector, OnInit, Directive } from '@angular/core';

import {EntityUtils, ReferentialRef} from '../../core/core.module';
import {BehaviorSubject, Subject} from 'rxjs';
import {isNil, isNotNil} from '../../shared/functions';
import {distinctUntilChanged, filter, switchMap} from "rxjs/operators";
import {Program} from "../../referential/services/model";
import {ProgramService} from "../../referential/services/program.service";
import {isNotNilOrBlank} from "../../shared/functions";
import {EditorDataService, EditorDataServiceLoadOptions} from "../../shared/services/data-service.class";
import {AppEditorPage} from "../../core/form/editor-page.class";
import {HistoryPageReference} from "../../core/services/model";
import {RootDataEntity} from "../services/model/base.model";


@Directive()
export abstract class AppDataEditorPage<T extends RootDataEntity<T>, S extends EditorDataService<T>>
  extends AppEditorPage<T>
  implements OnInit {

  protected programService: ProgramService;

  programSubject = new BehaviorSubject<string>(null);
  onProgramChanged = new Subject<Program>();

  get service(): S {
    return this.dataService;
  }

  protected constructor(
    injector: Injector,
    protected dataType: new() => T,
    protected dataService: S
  ) {
    super(injector,
      dataType,
      dataService);

    this.programService = injector.get(ProgramService);

    // FOR DEV ONLY ----
    //this.debug = !environment.production;
  }

  ngOnInit() {
    super.ngOnInit();

    // Watch program, to configure tables from program properties
    this.registerSubscription(
      this.programSubject.asObservable()
        .pipe(
          filter(isNotNilOrBlank),
          distinctUntilChanged(),
          switchMap(programLabel => this.programService.watchByLabel(programLabel))
        )
        .subscribe(program => this.onProgramChanged.next(program))
    );
  }

  async load(id?: number, options?: EditorDataServiceLoadOptions) {
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

  protected canUserWrite(data: T): boolean {
    return isNil(data.validationDate) && this.programService.canUserWrite(data);
  }

  protected startListenProgramChanges() {

    // If new entity
    if (this.isNewData) {

      // Listen program changes (only if new data)
      this.registerSubscription(this.form.controls['program'].valueChanges
        .subscribe(program => {
          if (EntityUtils.isNotEmpty(program)) {
            console.debug("[root-data-editor] Propagate program change: " + program.label);
            this.programSubject.next(program.label);
          }
        })
      );
    }
  }

  /**
   * Override default function, to add the entity program as subtitle)
   * @param page
   */
  protected addToPageHistory(page: HistoryPageReference) {
    page.subtitle = page.subtitle || this.data.program.label;
    super.addToPageHistory(page);
  }

  protected async updateRoute(data: T, queryParams: any): Promise<boolean> {
    let parentUrl = this.defaultBackHref;
    if (parentUrl && parentUrl.indexOf('?') !== -1) {
      parentUrl = parentUrl.substr(0, parentUrl.indexOf('?'));
    }
    return await this.router.navigateByUrl(`${parentUrl}/${data.id}`, {
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
