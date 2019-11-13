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
import {Entity, HistoryPageReference, UsageMode} from "../services/model";
import {FormGroup} from "@angular/forms";
import {AppTabPage} from "./tab-page.class";
import {AppFormUtils} from "./form.utils";

export abstract class AppEditorPage<T extends Entity<T>, F = any> extends AppTabPage<T, F> implements OnInit {

  protected _enableListenChanges = (environment.listenRemoteChanges === true);
  protected dateFormat: DateFormatPipe;
  protected cd: ChangeDetectorRef;
  protected settings: LocalSettingsService;
  protected idAttribute = 'id';

  $title = new Subject<string>();
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

    const params = this.route.snapshot.params;
    const id = params[this.idAttribute];
    if (!id || id === "new") {
      this.load(undefined, params);
    } else {
      this.load(+id, params);
    }
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
  updateViewState(data: T, opts?: {onlySelf?: boolean, emitEvent?: boolean; }) {
    if (this.isNewData || this.canUserWrite(data)) {
      this.enable(opts);
    }
    else {
      this.disable(opts);
    }
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
    const forcedQueryParams = {};
    forcedQueryParams[this.idAttribute] = data.id;
    await this.router.navigate(['.'], {
      relativeTo: this.route,
      queryParams: Object.assign(this.queryParams, forcedQueryParams)
    });

    setTimeout(async () => {
      await this.updateRoute(data, this.queryParams);
    }, 400);

  }

  async save(event): Promise<boolean> {
    console.debug("[root-data-editor] Asking to save...");
    if (this.loading || this.saving || !this.dirty) return false;

    // Wait end of async validation
    await AppFormUtils.waitWhilePending(this);

    // Not valid
    if (!this.valid) {
      this.markAsTouched({emitEvent: true});
      this.logFormErrors();
      this.openFirstInvalidTab();

      this.submitted = true;
      return false;
    }
    this.saving = true;
    this.error = undefined;

    if (this.debug) console.debug("[root-data-editor] Saving data...");

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

  async delete(event): Promise<boolean> {
    console.debug("[root-data-editor] Asking to delete...");
    if (this.loading || this.saving) return false;

    this.saving = true;
    this.error = undefined;

    // Get data
    const data = await this.getValue();
    const isNew = this.isNewData;

    this.disable();

    try {
      if (!isNew) {
        await this.dataService.delete(data);
      }

      this.onEntityDeleted(data);

    } catch (err) {
      console.error(err);
      this.submitted = true;
      this.error = err && err.message || err;
      this.saving = false;
      this.enable();
      return false;
    }

    // Wait, then go back (wait is need in order to update back href is need)
    setTimeout(() => {
      // Go back
      if (this.appToolbar && this.appToolbar.canGoBack) {
        return this.appToolbar.goBack();
      } else {
        // Back to home
        return this.router.navigateByUrl('/');
      }
    }, 500);
  }

  /* -- protected methods to override -- */

  protected abstract registerFormsAndTables();

  protected abstract computeTitle(data: T): Promise<string>;

  protected abstract setValue(data: T);

  protected abstract get form(): FormGroup;

  protected abstract getFirstInvalidTabIndex(): number;

  protected abstract canUserWrite(data: T): boolean;


  /* -- protected methods -- */

  protected async onNewEntity(data: T, options?: EditorDataServiceLoadOptions): Promise<void> {
    // can be overwrite by subclasses
  }

  protected async onEntityLoaded(data: T, options?: EditorDataServiceLoadOptions): Promise<void> {
    // can be overwrite by subclasses
  }

  protected async onEntityDeleted(data: T): Promise<void> {
    // can be overwrite by subclasses
  }

  protected async updateRoute(data: T, queryParams: any): Promise<boolean> {
    // can be overwrite by subclasses
    console.warn(`${this.constructor.name} should implement fucntion updateRoute() to have correct URL, after saving entity`);
    return false;
  }

  protected computeUsageMode(data: T): UsageMode {
    return this.settings.isUsageMode('FIELD') ? 'FIELD' : 'DESK';
  }

  protected async getValue(): Promise<T> {
    const json = await this.getJsonValueToSave();

    const res = new this.dataType();
    res.fromObject(json);

    return res;
  }

  protected getJsonValueToSave(): Promise<any> {
    return this.form.value;
  }

  /**
   * Compute the title
   * @param data
   */
  protected async updateTitle(data?: T) {
    data = data || this.data;
    const title = await this.computeTitle(data);
    this.$title.next(title);

    // If NOT data, then add to page history
    if (!this.isNewData) {
      this.addToPageHistory({
        title,
        path: this.router.url
      });
    }
  }

  protected addToPageHistory(page: HistoryPageReference) {
    this.settings.addToPageHistory(page, {
      removePathQueryParams: true,
      removeTitleSmallTag: true
    });
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
