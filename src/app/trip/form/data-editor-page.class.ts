import {Injector, OnInit, ViewChild} from '@angular/core';

import {EntityUtils, ReferentialRef} from '../../core/core.module';
import {Subject} from 'rxjs';
import {isNil, isNotNil} from '../../shared/shared.module';
import {DataRootEntity} from "../services/trip.model";
import {EntityQualityFormComponent} from "../quality/entity-quality-form.component";
import {filter, switchMap} from "rxjs/operators";
import {Program} from "../../referential/services/model";
import {ProgramService} from "../../referential/services/program.service";
import {isNotNilOrBlank} from "../../shared/functions";
import {EditorDataService, EditorDataServiceLoadOptions} from "../../shared/services/data-service.class";
import {AppEditorPage} from "../../core/form/editor-page.class";
import {HistoryPageReference} from "../../core/services/model";


export abstract class AppDataEditorPage<T extends DataRootEntity<T>, F = any> extends AppEditorPage<T, F> implements OnInit {

  protected programService: ProgramService;

  programSubject = new Subject<string>();
  onProgramChanged = new Subject<Program>();

  @ViewChild('qualityForm') qualityForm: EntityQualityFormComponent;

  protected constructor(
    injector: Injector,
    protected dataType: new() => T,
    protected dataService: EditorDataService<T, F>
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
          switchMap(programLabel => this.programService.watchByLabel(programLabel, true))
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

  updateViewState(data: T) {
    // Quality metadata
    if (this.qualityForm) {
      this.qualityForm.value = data;
    }

    if (isNotNil(data.validationDate)) {
      this.disable();
    } else {
      this.enable();
    }
  }

  enable() {
    if (!this.data || isNotNil(this.data.validationDate)) return false;

    // If not a new data, check user can write
    if (isNotNil(this.data.id) && !this.programService.canUserWrite(this.data)) {
      if (this.debug) console.warn("[root-data-editor] Leave form disable (User has NO write access)");
      return;
    }

    if (this.debug) console.debug("[root-data-editor] Enabling form (User has write access)");
    super.enable();
  }

  async onControl(event: Event) {
    // Stop if data is not valid
    if (!this.valid) {
      // Stop the control
      event && event.preventDefault();

      // Open the first tab in error
      this.openFirstInvalidTab();
    } else if (this.dirty) {

      // Stop the control
      event && event.preventDefault();

      console.debug("[root-data-editor] Saving data, before control...");
      const saved = await this.save(new Event('save'));
      if (saved) {
        // Loop
        await this.qualityForm.control(new Event('control'));
      }
    }
  }

  /* -- protected methods -- */

  /**
   * Override default function, to add the entity program as subtitle)
   * @param data
   */
  protected addToPageHistory(page: HistoryPageReference) {
    page.subtitle = page.subtitle || this.data.program.label;
    super.addToPageHistory(page);
  }

  protected async updateRoute(data: T, queryParams: any): Promise<boolean> {
    return await this.router.navigateByUrl(`${this.defaultBackHref}/${data.id}`, {
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
