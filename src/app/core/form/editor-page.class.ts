import {ChangeDetectorRef, EventEmitter, Injector, OnInit} from '@angular/core';
import {ActivatedRoute, Router} from "@angular/router";
import {AlertController} from "@ionic/angular";

import {TranslateService} from '@ngx-translate/core';
import {environment} from '../../../environments/environment';
import {Subject} from 'rxjs';
import {
  DateFormatPipe,
  EditorDataService,
  EditorDataServiceLoadOptions,
  isNil,
  isNotNil
} from '../../shared/shared.module';
import {Moment} from "moment";
import {LocalSettingsService} from "../services/local-settings.service";
import {filter, first} from "rxjs/operators";
import {ProgramService} from "../../referential/services/program.service";
import {Entity, UsageMode} from "../services/model";
import {FormGroup} from "@angular/forms";
import {AppTabPage} from "./tab-page.class";


export abstract class AppEditorPage<T extends Entity<T>, F = any> extends AppTabPage<T, F> implements OnInit {

  protected _enableListenChanges = (environment.listenRemoteChanges === true);
  protected dateFormat: DateFormatPipe;
  protected cd: ChangeDetectorRef;
  protected settings: LocalSettingsService;
  protected programService: ProgramService;

  title = new Subject<string>();
  saving = false;
  defaultBackHref: string;
  onRefresh = new EventEmitter<any>();
  usageMode: UsageMode;

  get isOnFieldMode(): boolean {
    return this.usageMode ? this.usageMode === 'FIELD' : this.settings.isUsageMode('FIELD');
  }

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

    // FOR DEV ONLY ----
    //this.debug = !environment.production;
  }

  ngOnInit() {
    super.ngOnInit();

    // Register forms & tables
    this.registerFormsAndTables();

    this.disable();

    this.route.params.pipe(first())
      .subscribe(async (params) => {
        const id = params["id"];
        if (!id || id === "new") {
          await this.load(undefined, params);
        } else {
          await this.load(+id, params);
        }
      });
  }

  async load(id?: number, options?: EditorDataServiceLoadOptions) {
    this.error = null;

    // New data
    if (isNil(id)) {

      // Create using default values
      const data = new this.dataType();
      this.usageMode = this.computeUsageMode(data);
      await this.onNewEntity(data, options);
      this.updateView(data);
      this.loading = false;
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
              } else {
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

    this.updateTitle(data);

    this.markAsPristine();
    this.markAsUntouched();

    this.updateViewState(data);

    this.onRefresh.emit();
  }

  /**
   * Enable or disable state
   */
  updateViewState(data: T) {
    this.enable();
  }

  /**
   * After first save, update the route location
   */
  async updateRouteAfterFirstSave(data: T) {

    this.queryParams = this.queryParams || {};

    // Open the next tab
    if (this.selectedTabIndex === 0) {
      this.selectedTabIndex = 1;
      Object.assign(this.queryParams, {tab: this.selectedTabIndex});
    }

    // Update route location
    await this.router.navigate(['.'], {
      relativeTo: this.route,
      queryParams: Object.assign(this.queryParams, {id: data.id})
    });

    setTimeout(async () => {
      await this.router.navigate(['../../' + data.id], {
        replaceUrl: true,
        relativeTo: this.route,
        queryParams: this.queryParams,
      });
    }, 100);

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
        // Update route location
        await this.updateRouteAfterFirstSave(updatedData);

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

  /* -- protected methods to override -- */

  protected abstract registerFormsAndTables();

  protected abstract computeTitle(data: T): Promise<string>;

  protected abstract setValue(data: T);

  protected abstract get form(): FormGroup;

  protected abstract getFirstInvalidTabIndex(): number;


  /* -- protected methods -- */

  protected async onNewEntity(data: T, options?: EditorDataServiceLoadOptions): Promise<void> {
    // can be overwrite by subclasses
  }

  protected async onEntityLoaded(data: T, options?: EditorDataServiceLoadOptions): Promise<void> {
    // can be overwrite by subclasses
  }

  protected computeUsageMode(data: T): UsageMode {
    return this.settings.isUsageMode('FIELD') ? 'FIELD' : 'DESK';
  }

  protected getValue(): Promise<T> {
    const json = this.form.value;

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

  protected markForCheck() {
    this.cd.markForCheck();
  }
}
