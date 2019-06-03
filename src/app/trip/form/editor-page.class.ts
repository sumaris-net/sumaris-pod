import {ChangeDetectorRef, EventEmitter, Injector, OnInit, ViewChild} from '@angular/core';
import {ActivatedRoute, Router} from "@angular/router";
import {AlertController} from "@ionic/angular";

import {AppTabPage, EntityUtils} from '../../core/core.module';
import {TranslateService} from '@ngx-translate/core';
import {environment} from '../../../environments/environment';
import {Subject} from 'rxjs';
import {DateFormatPipe, isNil, isNotNil} from '../../shared/shared.module';
import {Moment} from "moment";
import {DataRootEntity} from "../services/trip.model";
import {EntityQualityFormComponent} from "../quality/entity-quality-form.component";
import {LocalSettingsService} from "../../core/services/local-settings.service";
import {filter, mergeMap} from "rxjs/operators";
import {Program} from "../../referential/services/model";
import {ProgramService} from "../../referential/services/program.service";
import {isNotNilOrBlank} from "../../shared/functions";
import {UsageMode} from "../../core/services/model";
import {FormGroup} from "@angular/forms";
import {EditorDataService, LoadEditorDataOptions} from "../../shared/services/data-service.class";


export abstract class AppEditorPage<T extends DataRootEntity<T>, F = any> extends AppTabPage<T, F> implements OnInit {

  protected _enableListenChanges = (environment.listenRemoteChanges === true);
  protected dateFormat: DateFormatPipe;
  protected cd: ChangeDetectorRef;
  protected settings: LocalSettingsService;
  protected programService: ProgramService;

  programSubject = new Subject<string>();
  onProgramChanged = new Subject<Program>();
  title = new Subject<string>();
  saving = false;
  defaultBackHref: string;
  onRefresh = new EventEmitter<any>();
  usageMode: UsageMode;

  get isOnFieldMode(): boolean {
    return this.usageMode ? this.usageMode === 'FIELD' : this.settings.isUsageMode('FIELD');
  }

  @ViewChild('qualityForm') qualityForm: EntityQualityFormComponent;

  protected constructor(
    injector: Injector,
    protected dataType: new() => T,
    protected dataService: EditorDataService<T, F>
  ) {
    super(injector.get(ActivatedRoute),
      injector.get(Router),
      injector.get(AlertController),
      injector.get(TranslateService));

    this.settings = injector.get(LocalSettingsService);
    this.cd = injector.get(ChangeDetectorRef);
    this.dateFormat = injector.get(DateFormatPipe);
    this.programService = injector.get(ProgramService);

    // FOR DEV ONLY ----
    //this.debug = !environment.production;
  }

  ngOnInit() {
    super.ngOnInit();

    // Register forms & tables
    this.registerFormsAndTables();

    this.disable();

    this.route.params.first().subscribe(async (params) => {
      const id = params["id"];
      if (!id || id === "new") {
        await this.load(undefined, params);
      } else {
        await this.load(+id, params);
      }
    });

    // Watch program, to configure tables from program properties
    this.registerSubscription(
      this.programSubject.asObservable()
        .pipe(
          filter(isNotNilOrBlank),
          mergeMap(label => this.programService.loadByLabel(label))
        )
        .subscribe(program => this.onProgramChanged.next(program))
    );
  }

  async load(id?: number, options?: LoadEditorDataOptions) {
    this.error = null;

    // New data
    if (isNil(id)) {

      // Create using default values
      const data = new this.dataType();
      this.usageMode = this.computeUsageMode(data);
      await this.onNewEntity(data, options);
      this.updateView(data);
      this.loading = false;
      this.startListenProgramChanges();
    }

    // Load existing data
    else {
      const data = await this.dataService.load(id, options);
      this.usageMode = this.computeUsageMode(data);
      await this.onEntityLoaded(data, options);
      this.updateView(data);
      this.loading = false;
      this.startListenRemoteChanges();
    }
  }

  startListenProgramChanges() {

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

  startListenRemoteChanges() {
    // Listen for changes on server
    if (isNotNil(this.data.id) && this._enableListenChanges) {
      this.registerSubscription(
        this.dataService.listenChanges(this.data.id)
          .pipe(filter(isNotNil))
          .subscribe((data: T) => {
            if (data.updateDate && (data.updateDate as Moment).isAfter(this.data.updateDate)) {
              if (!this.dirty) {
                if (this.debug) console.debug(`[root-data-editor] Changes detected on server, at {${data.updateDate}} : reloading page...`);
                this.updateView(data);
              }
              else {
                if (this.debug) console.debug(`[root-data-editor] Changes detected on server, at {${data.updateDate}}, but page is dirty: skip reloading.`);
              }
            }
          })
      );
    }
  }

  updateView(data: T | null) {
    this.data = data;
    this.setValue(data);
    if (!this.isNewData) {
      this.form.controls['program'].disable();
      this.programSubject.next(data.program.label);
    }

    // Quality metadata
    if (this.qualityForm) {
      this.qualityForm.value = data;
    }

    this.updateTitle(data);

    this.markAsPristine();
    this.markAsUntouched();

    if (isNotNil(this.data.validationDate)) {
      this.disable();
    } else {
      this.enable();
    }

    this.onRefresh.emit();
  }

  async save(event): Promise<boolean> {
    if (this.loading || this.saving || !this.dirty) return false;

    // Not valid
    if (!this.valid) {
      this.markAsTouched();
      this.logFormErrors();
      this.openFirstInvalidTab();

      this.submitted = true;
      return false;
    }
    this.saving = true;
    this.error = undefined;

    if (this.debug) console.debug("[root-data-editor] Saving control...");

    // Get data
    const data = await this.getValue();
    const isNew = this.isNewData;

    this.disable();

    try {
      // Save saleControl form (with sale)
      const updatedData = await this.dataService.save(data);
      this.markAsPristine();
      this.markAsUntouched();

      // Update the view (e.g metadata)
      this.updateView(updatedData);

      // Is first save
      if (isNew) {

        // Open the gear tab
        this.selectedTabIndex = 1;
        const queryParams = Object.assign({}, this.route.snapshot.queryParams, {tab: this.selectedTabIndex});

        // Update route location
        this.router.navigate(['../' + updatedData.id], {
          relativeTo: this.route,
          queryParams: queryParams,
          replaceUrl: true // replace the current state in history
        });

        // Subscription to remote changes
        this.startListenRemoteChanges();
      }

      this.submitted = false;
      return true;
    } catch (err) {
      console.error(err);
      this.submitted = true;
      this.error = err && err.message || err;
      this.enable();
      return false;
    } finally {
      this.saving = false;
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

  public async onControl(event: Event) {
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

  /* -- protected methods to override -- */

  protected abstract registerFormsAndTables();

  protected abstract onNewEntity(data: T, options?: LoadEditorDataOptions): Promise<void>;

  protected abstract onEntityLoaded(data: T, options?: LoadEditorDataOptions): Promise<void>;

  protected abstract computeTitle(data: T): Promise<string>;

  protected abstract setValue(data: T);

  protected abstract get form(): FormGroup;

  protected abstract getFirstInvalidTabIndex(): number;


  /* -- protected methods -- */


  protected computeUsageMode(data: T): UsageMode {
    return this.settings.isUsageMode('FIELD') ? 'FIELD' : 'DESK';
  }

  protected getValue(): Promise<T> {
    const json = this.form.value;

    // Re add program, because program control can be disabled
    json.program = this.form.controls['program'].value;

    const res = new this.dataType();
    res.fromObject(json);

    return Promise.resolve(res);
  }

  /**
   * Compute the title
   * @param data
   */
  protected async updateTitle(data?: T) {
    const title = await this.computeTitle(data || this.data);
    this.title.next(title);
  }

  /**
   * Open the first tab that is invalid
   */
  protected openFirstInvalidTab() {
    const invalidTabIndex = this.getFirstInvalidTabIndex();
    if (invalidTabIndex !== -1 && this.selectedTabIndex !== invalidTabIndex) {
      this.selectedTabIndex = invalidTabIndex;
    }
  }

  /**
   *
   */
  protected markForCheck() {
    this.cd.markForCheck();
  }
}
