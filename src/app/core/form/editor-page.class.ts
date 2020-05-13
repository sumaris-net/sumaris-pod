import { ChangeDetectorRef, EventEmitter, Injector, OnDestroy, OnInit, Directive } from '@angular/core';
import {ActivatedRoute, ActivatedRouteSnapshot, Router} from "@angular/router";
import {AlertController, ToastController} from "@ionic/angular";

import {TranslateService} from '@ngx-translate/core';
import {environment} from '../../../environments/environment';
import {Subject} from 'rxjs';
import {
  DateFormatPipe,
  EditorDataService,
  EditorDataServiceLoadOptions,
  isNil,
  isNotNil,
  toBoolean
} from '../../shared/shared.module';
import {Moment} from "moment";
import {LocalSettingsService} from "../services/local-settings.service";
import {filter} from "rxjs/operators";
import {Entity, HistoryPageReference, UsageMode} from "../services/model";
import {FormGroup} from "@angular/forms";
import {AppTabPage} from "./tab-page.class";
import {AppFormUtils} from "./form.utils";
import {Alerts} from "../../shared/alerts";
import {ServerErrorCodes} from "../services/errors";

@Directive()
export abstract class AppEditorPage<T extends Entity<T>, F = any> extends AppTabPage<T, F> implements OnInit, OnDestroy {

  protected idAttribute = 'id';
  protected _enableListenChanges = (environment.listenRemoteChanges === true);
  protected dateFormat: DateFormatPipe;
  protected cd: ChangeDetectorRef;
  protected settings: LocalSettingsService;
  protected _usageMode: UsageMode;

  $title = new Subject<string>();
  saving = false;
  hasRemoteListener = false;
  defaultBackHref: string;
  onUpdateView = new EventEmitter<T>();

  get usageMode(): UsageMode {
    return this._usageMode;
  }

  set usageMode(value: UsageMode) {
    if (this._usageMode !== value) {
      this._usageMode = value;
      // TODO: Force refresh of the form
      this.markForCheck();
    }
  }

  get isOnFieldMode(): boolean {
    return this.usageMode ? this.usageMode === 'FIELD' : this.settings.isUsageMode('FIELD');
  }

  get service(): EditorDataService<T> {
    return this.dataService;
  }

  protected constructor(
    injector: Injector,
    protected dataType?: new() => T,
    protected dataService?: EditorDataService<T, F>
  ) {
    super(injector.get(ActivatedRoute),
      injector.get(Router),
      injector.get(AlertController),
      injector.get(TranslateService));

    this.settings = injector.get(LocalSettingsService);
    this.cd = injector.get(ChangeDetectorRef);
    this.dateFormat = injector.get(DateFormatPipe);
    this.toastController = injector.get(ToastController);

    // FOR DEV ONLY ----
    //this.debug = !environment.production;
  }

  ngOnInit() {
    super.ngOnInit();

    // Register forms & tables
    this.registerFormsAndTables();

    // Disable page, during load
    this.disable();

    // Load data from the snapshot route
    this.loadFromRoute(this.route.snapshot);
  }

  ngOnDestroy() {
    super.ngOnDestroy();
  }

  protected async loadFromRoute(route: ActivatedRouteSnapshot) {
    const id = route.params[this.idAttribute];
    if (isNil(id) || id === "new") {
      this.load(undefined, route.params);
    } else {
      this.load(+id, route.params);
    }
  }

  async load(id?: number, opts?: EditorDataServiceLoadOptions) {
    this.error = null;

    // New data
    if (isNil(id)) {

      // Create using default values
      const data = new this.dataType();
      this._usageMode = this.computeUsageMode(data);
      await this.onNewEntity(data, opts);
      this.updateView(data);
      this.loading = false;
    }

    // Load existing data
    else {
      try {
        const data = await this.dataService.load(id, opts);
        this._usageMode = this.computeUsageMode(data);
        await this.onEntityLoaded(data, opts);
        this.updateView(data);
        this.loading = false;
        this.startListenRemoteChanges();
      }
      catch (err) {
        this.setError(err);
        this.selectedTabIndex = 0;
        this.loading = false;
      }
    }
  }

  startListenRemoteChanges() {
    if (this.hasRemoteListener) return; // Skip, if already listening

    // Listen for changes on server
    if (isNotNil(this.data.id) && this._enableListenChanges) {
      this.hasRemoteListener = true;

      this.registerSubscription(
        this.dataService.listenChanges(this.data.id)
          .pipe(filter(isNotNil))
          .subscribe((data: T) => {
            if (data.updateDate && (data.updateDate as Moment).isAfter(this.data.updateDate)) {
              if (!this.dirty) {
                if (this.debug) console.debug(`[data-editor] Changes detected on server, at {${data.updateDate}} : reloading page...`);
                this.updateView(data);
              } else {
                if (this.debug) console.debug(`[data-editor] Changes detected on server, at {${data.updateDate}}, but page is dirty: skip reloading.`);
              }
            }
          })
          .add(() => this.hasRemoteListener = false)
      );
    }
  }

  updateView(data: T | null, opts?: {
    openTabIndex?: number;
    updateTabAndRoute?: boolean;
  }) {
    const idChanged = isNotNil(data.id) && (isNil(this.previousDataId) || this.previousDataId !== data.id) || false;

    opts = opts || {};
    opts.updateTabAndRoute = toBoolean(opts.updateTabAndRoute, idChanged && !this.loading);
    opts.openTabIndex = this.tabCount > 1 ?
      ((isNotNil(opts.openTabIndex) && opts.openTabIndex < this.tabCount) ? opts.openTabIndex :
      // If new data: open the second tab (if it's not the select index)
      (idChanged && isNil(this.previousDataId) && this.selectedTabIndex < this.tabCount - 1 ? this.selectedTabIndex + 1 : undefined)) : undefined;

    this.data = data;
    this.previousDataId = data.id;
    this.setValue(data);

    this.markAsPristine();
    this.markAsUntouched();

    this.updateViewState(data);

    // Need to update route
    if (opts.updateTabAndRoute === true) {
      this.updateTabAndRoute(data, opts)
        // Update the title - should be executed AFTER updateTabAndRoute because of path change - fix #185
        .then(() => this.updateTitle(data));
    }
    else {
      // Update the title.
      this.updateTitle(data);
    }

    this.onUpdateView.emit(data);
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

      // Allow to sort table
      this.tables.forEach(t => t.enableSort());
    }

  }

  /**
   * Update the route location, and open the next tab
   */
  async updateTabAndRoute(data: T, opts?: {
    openTabIndex?: number;
  }): Promise<boolean> {

    this.queryParams = this.queryParams || {};

    // Open the second tab
    if (opts && isNotNil(opts.openTabIndex)) {
      if (this.selectedTabIndex < opts.openTabIndex) {
        this.selectedTabIndex = opts.openTabIndex;
        Object.assign(this.queryParams, {tab: this.selectedTabIndex});
        this.markForCheck();
      }
    }

    // Update route location
    const forcedQueryParams = {};
    forcedQueryParams[this.idAttribute] = data.id;
    await this.router.navigate(['.'], {
      relativeTo: this.route,
      queryParams: Object.assign(this.queryParams, forcedQueryParams)
    });

    return this.updateRoute(data, this.queryParams);
  }

  async saveAndClose(event: Event, options?: any): Promise<boolean> {
    const saved = await this.save(event);
    if (saved) {
      await this.close(event);
    }
    return saved;
  }

  async close(event: Event) {
    if (this.appToolbar && this.appToolbar.canGoBack) {
      await this.appToolbar.goBack();
    }
    else if (this.defaultBackHref) {
      await this.router.navigateByUrl(this.defaultBackHref);
    }
  }

  async save(event: Event, options?: any): Promise<boolean> {
    console.log("TODO call save");
    if (this.loading || this.saving) {
      console.debug("[data-editor] Skip save: editor is busy (loading or saving)");
      return false;
    }
    if (!this.dirty) {
      console.debug("[data-editor] Skip save: editor not dirty");
      return true;
    }

    // Wait end of async validation
    await this.waitWhilePending();

    // If invalid
    if (this.invalid) {
      this.markAsTouched({emitEvent: true});
      this.logFormErrors();
      this.openFirstInvalidTab();

      this.submitted = true;
      return false;
    }
    this.saving = true;
    this.error = undefined;

    if (this.debug) console.debug("[data-editor] Saving data...");

    // Get data
    const data = await this.getValue();

    this.disable();

    try {
      // Save form
      const updatedData = await this.dataService.save(data, options);

      // Update the view (e.g metadata)
      this.updateView(updatedData);

      // Subscribe to remote changes
      if (!this.hasRemoteListener) this.startListenRemoteChanges();

      this.submitted = false;

      return true;
    } catch (err) {
      this.submitted = true;
      this.setError(err);
      this.selectedTabIndex = 0;
      this.markAsDirty();
      this.enable();

      // Concurrent change on pod
      if (err.code === ServerErrorCodes.BAD_UPDATE_DATE && isNotNil(this.data.id)) {
        // Call a data reload (in background), to update the GraphQL cache, and allow to cancel changes
        this.dataService.load(this.data.id, {fetchPolicy: "network-only"}).then(() => {
          console.debug('[data-editor] Data cache reloaded. User can reload page');
        });
      }

      return false;
    } finally {
      this.saving = false;
    }
  }

  /**
   * Save data (if dirty and valid), and return it. Otherwise, return nil value.
   */
  async saveAndGetDataIfValid(): Promise<T | undefined> {
    // Form is not valid
    if (!this.valid) {

      // Make sure validation is finished
      await AppFormUtils.waitWhilePending(this);

      // If invalid: Open the first tab in error
      if (this.invalid) {
        this.openFirstInvalidTab();
        return undefined;
      }

      // Continue (valid)
    }

    // Form is valid, but not saved
    if (this.dirty) {
      const saved = await this.save(new Event('save'));
      if (!saved) return undefined;
    }

    // Valid and saved data
    return this.data;
  }

  async delete(event?: UIEvent): Promise<boolean> {
    if (this.loading || this.saving) return false;

    // Ask user confirmation
    const confirmation = await Alerts.askDeleteConfirmation(this.alertCtrl, this.translate);
    if (!confirmation) return;

    console.debug("[data-editor] Asking to delete...");

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
      this.submitted = true;
      this.setError(err);
      this.selectedTabIndex = 0;
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

  async unload(): Promise<void> {
    this.form.reset();
    this.registerFormsAndTables();
    this._dirty = false;
  }

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
    return false;
  }

  protected computeUsageMode(data: T): UsageMode {
    return this.settings.isUsageMode('FIELD') ? 'FIELD' : 'DESK';
  }

  protected async waitWhilePending(): Promise<void> {
    return await AppFormUtils.waitWhilePending(this);
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

  protected addToPageHistory(page: HistoryPageReference, opts?: {removePathQueryParams?: boolean; removeTitleSmallTag?: boolean; }) {
    this.settings.addToPageHistory(page, {
      removePathQueryParams: true,
      removeTitleSmallTag: true,
      ...opts
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

  public setError(err: any) {
    console.error("[data-editor] " + err && err.message || err);
    let userMessage = err && err.message && this.translate.instant(err.message) || err;

    // Add details error (if any) under the main message
    const detailMessage = err && err.details && (err.details.message || err.details) || undefined;
    if (detailMessage) {
      userMessage += `<br/><small class="hidden-xs hidden-sm" title="${detailMessage}">`;
      userMessage += detailMessage.length < 70 ? detailMessage : detailMessage.substring(0, 67) + '...';
      userMessage += "</small>";
    }
    this.error = userMessage;
  }
}
