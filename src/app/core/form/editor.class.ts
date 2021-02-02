import {
  AfterViewInit,
  ChangeDetectorRef,
  Directive,
  EventEmitter,
  Injector,
  OnDestroy,
  OnInit,
  Optional
} from '@angular/core';
import {ActivatedRoute, Router} from "@angular/router";
import {AlertController, ToastController} from "@ionic/angular";

import {TranslateService} from '@ngx-translate/core';
import {Subject} from 'rxjs';
import {Moment} from "moment";
import {AddToPageHistoryOptions, LocalSettingsService} from "../services/local-settings.service";
import {filter} from "rxjs/operators";
import {Entity} from "../services/model/entity.model";
import {HistoryPageReference, UsageMode} from "../services/model/settings.model";
import {FormGroup} from "@angular/forms";
import {AppTabEditor, AppTabFormOptions} from "./tab-editor.class";
import {AppFormUtils} from "./form.utils";
import {Alerts} from "../../shared/alerts";
import {ErrorCodes, ServerErrorCodes} from "../services/errors";
import {toNumber} from "../../shared/functions";
import {EntityServiceLoadOptions, IEntityService} from "../../shared/services/entity-service.class";
import {isNil, isNilOrBlank, isNotNil, toBoolean} from "../../shared/functions";
import {DateFormatPipe} from "../../shared/pipes/date-format.pipe";
import {ENVIRONMENT} from "../../../environments/environment.class";

export class AppEditorOptions extends AppTabFormOptions {
  autoLoad?: boolean;
  autoLoadDelay?: number;
  pathIdAttribute?: string;
  enableListenChanges?: boolean;

  /**
   * Change page route (window URL) when saving for the first time
   */
  autoUpdateRoute?: boolean; // Default to true

  /**
   * Open the next tab, after saving for the first time
   */
  autoOpenNextTab?: boolean; // Default to true

}

// @dynamic
@Directive()
export abstract class AppEntityEditor<
  T extends Entity<T>,
  S extends IEntityService<T> = IEntityService<T>
  >
  extends AppTabEditor<T, EntityServiceLoadOptions>
  implements OnInit, OnDestroy, AfterViewInit {

  private _usageMode: UsageMode;
  private readonly _enableListenChanges: boolean;
  private readonly _pathIdAttribute: string;
  private readonly _autoLoad: boolean;
  private readonly _autoLoadDelay: number;
  private readonly _autoUpdateRoute: boolean;
  private _autoOpenNextTab: boolean;

  protected dateFormat: DateFormatPipe;
  protected cd: ChangeDetectorRef;
  protected settings: LocalSettingsService;

  data: T;
  title$ = new Subject<string>();
  saving = false;
  hasRemoteListener = false;
  defaultBackHref: string;
  historyIcon: {icon?: string; matIcon?: string; };
  onUpdateView = new EventEmitter<T>();

  get usageMode(): UsageMode {
    return this._usageMode;
  }

  set usageMode(value: UsageMode) {
    if (this._usageMode !== value) {
      this._usageMode = value;
      this.markForCheck();
    }
  }

  get isOnFieldMode(): boolean {
    return this.settings.isOnFieldMode(this._usageMode);
  }

  get isNewData(): boolean {
    return !this.data || this.data.id === undefined || this.data.id === null;
  }

  get service(): S {
    return this.dataService;
  }

  markAsSaving(opts?: { emitEvent?: boolean; }){
    if (!this.saving) {
      this.saving = true;
      if (!opts || opts.emitEvent !== false) this.markForCheck();
    }
  }

  markAsSaved(opts?: { emitEvent?: boolean; }){
    if (this.saving) {
      this.saving = false;
      if (!opts || opts.emitEvent !== false) this.markForCheck();
    }
  }

  protected constructor(
    injector: Injector,
    protected dataType: new() => T,
    protected dataService?: S,
    @Optional() options?: AppEditorOptions
  ) {
    super(injector.get(ActivatedRoute),
      injector.get(Router),
      injector.get(AlertController),
      injector.get(TranslateService),
      options);
    options = <AppEditorOptions>{
      // Default options
      enableListenChanges: (injector.get(ENVIRONMENT).listenRemoteChanges === true),
      pathIdAttribute: 'id',
      autoLoad: true,
      autoLoadDelay: 0,
      autoUpdateRoute: true,

      // Following options are override inside ngOnInit()
      // autoOpenNextTab: ...,

      // Override defaults
      ...options
    };

    this.settings = injector.get(LocalSettingsService);
    this.cd = injector.get(ChangeDetectorRef);
    this.dateFormat = injector.get(DateFormatPipe);
    this.toastController = injector.get(ToastController);
    this._enableListenChanges = options.enableListenChanges;
    this._pathIdAttribute = options.pathIdAttribute;
    this._autoLoad = options.autoLoad;
    this._autoLoadDelay = options.autoLoadDelay;
    this._autoUpdateRoute = options.autoUpdateRoute;
    this._autoOpenNextTab = options.autoOpenNextTab;

    // FOR DEV ONLY ----
    //this.debug = !environment.production;
  }

  ngOnInit() {
    super.ngOnInit();

    // Defaults
    this._autoOpenNextTab = toBoolean(this._autoOpenNextTab, !this.isOnFieldMode);
    this.historyIcon = this.historyIcon || {icon: 'list'};

    // Register forms
    this.registerForms();

    // Disable page, during load
    this.disable();
  }

  ngAfterViewInit() {
    // Load data
    if (this._autoLoad) {
      setTimeout(() => this.loadFromRoute(), this._autoLoadDelay);
    }
  }

  ngOnDestroy() {
    super.ngOnDestroy();

    this.title$.unsubscribe();
  }

  /**
   * Load data from the snapshot route
   * @param route
   * @protected
   */
  protected loadFromRoute(): Promise<void> {
    const route = this.route.snapshot;
    if (!route || isNilOrBlank(this._pathIdAttribute)) {
      throw new Error("Unable to load from route: missing 'route' or 'options.pathIdAttribute'.");
    }
    const id = route.params[this._pathIdAttribute];
    if (isNil(id) || id === "new") {
      return this.load(undefined, route.params);
    } else {
      return this.load(+id, route.params);
    }
  }

  /**
   * Load data from id, using the dataService
   * @param id
   * @param opts
   */
  async load(id?: number, opts?: EntityServiceLoadOptions & {
    emitEvent?: boolean;
    openTabIndex?: number;
    updateTabAndRoute?: boolean;
    [key: string]: any;
  }) {
    if (!this.dataService) throw new Error("Cannot load data: missing 'dataService'!");

    this.error = null;

    // New data
    if (isNil(id)) {

      // Create using default values
      const data = new this.dataType();
      this._usageMode = this.computeUsageMode(data);
      await this.onNewEntity(data, opts);
      this.updateView(data, {
        openTabIndex: 0,
        ...opts
      });
      this.loading = false;
    }

    // Load existing data
    else {
      try {
        const data = await this.dataService.load(id, opts);
        if (!data) throw {code: ErrorCodes.DATA_NOT_FOUND_ERROR, message: 'ERROR.DATA_NO_FOUND'};
        this._usageMode = this.computeUsageMode(data);
        await this.onEntityLoaded(data, opts);
        this.updateView(data, opts);
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
    // Skip if disable, or already listening
    if (this.hasRemoteListener || !this._enableListenChanges || this.isNewData) return;

    // Listen for changes on server
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

  updateView(data: T | null, opts?: {
    emitEvent?: boolean;
    openTabIndex?: number;
    updateRoute?: boolean;
  }) {
    const idChanged = isNotNil(data.id)
      && this.previousDataId !== undefined // Ignore if first loading (=undefined)
      && this.previousDataId !== data.id;

    opts = {
      updateRoute: this._autoUpdateRoute && idChanged && !this.loading,
      openTabIndex: this._autoOpenNextTab && idChanged && isNil(this.previousDataId) && this.selectedTabIndex < this.tabCount - 1 ? this.selectedTabIndex + 1 : undefined,
      ...opts
    };

    this.data = data;
    this.previousDataId = data.id || null;

    this.setValue(data);

    if (!opts || opts.emitEvent !== false) {
      this.markAsPristine();
      this.markAsUntouched();
      this.updateViewState(data);

      // Need to update route
      if (opts.updateRoute === true) {
        this.updateTabAndRoute(data, opts)
          // Update the title - should be executed AFTER updateRoute because of path change - fix #185
          .then(() => this.updateTitle(data));
      }
      else {
        // Update the tag group index
        this.updateTabIndex(opts.openTabIndex);

        // Update the title.
        this.updateTitle(data);
      }

      this.onUpdateView.emit(data);
    }
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
  }): Promise<void> {

    this.queryParams = this.queryParams || {};

    // Open the tab group
    this.updateTabIndex(opts && opts.openTabIndex);

    // Save the opened tab into the queryParams
    this.queryParams.tab = this.selectedTabIndex;

    const done = await this.updateRoute(data, this.queryParams);
    if (done) return;

    // Update route location
    const forcedQueryParams = {};
    forcedQueryParams[this._pathIdAttribute] = data && isNotNil(data.id) ? data.id : 'new';
    if (data && isNotNil(data.id)) {
      await this.router.navigate(['.'], {
        relativeTo: this.route,
        queryParams: {...this.queryParams, ...forcedQueryParams}
      });
    }
    else {
      await this.router.navigate(['..', 'new'], {
        relativeTo: this.route,
        queryParams: {...this.queryParams, ...forcedQueryParams}
      });
    }
  }

  /**
   * Update the route location, and open the next tab
   */
  updateTabIndex(tabIndex?: number) {

    // Open the second tab
    if (isNotNil(tabIndex)) {
      if (this.selectedTabIndex !== tabIndex) {
        this.selectedTabIndex = tabIndex;
        this.markForCheck();
      }
    }
  }

  async saveAndClose(event: Event, options?: any): Promise<boolean> {

    const saved = await this.save(event);
    if (saved) {
      await this.close(event);
    }
    return saved;
  }

  async close(event: Event) {
    if (event) {
      if (event.defaultPrevented) return;
      event.preventDefault();
      event.stopPropagation();
    }
    if (this.appToolbar && this.appToolbar.canGoBack) {
      await this.appToolbar.goBack();
    }
    else if (this.defaultBackHref) {
      await this.router.navigateByUrl(this.defaultBackHref);
    }
  }

  async save(event?: Event, options?: any): Promise<boolean> {
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

    this.markAsSaving();
    this.error = undefined;

    if (this.debug) console.debug("[data-editor] Saving data...");

    try {
      // Get data
      const data = await this.getValue();

      this.disable();

      // Save form
      const updatedData = await this.dataService.save(data, options);

      await this.onEntitySaved(updatedData);

      // Update the view (e.g metadata)
      this.updateView(updatedData, options);

      // Subscribe to remote changes
      if (!this.hasRemoteListener) this.startListenRemoteChanges();

      this.submitted = false;

      return true;
    } catch (err) {
      this.submitted = true;
      this.setError(err);
      this.selectedTabIndex = 0;
      this.scrollToTop(); // Scroll to top (to show error)
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
      this.markAsSaved();
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

    this.markAsSaving();
    this.error = undefined;

    try {
      // Get data
      const data = await this.getValue();
      const isNew = this.isNewData;

      this.disable();

      if (!isNew) {
        await this.dataService.delete(data);
      }

      this.onEntityDeleted(data);

      // Remove page history
      this.removePageHistory();

    } catch (err) {
      this.submitted = true;
      this.setError(err);
      this.selectedTabIndex = 0;
      this.markAsSaved();
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

  async reload() {
    this.loading = true;
    await this.load(this.data && this.data.id);
  }

  setError(err: any) {
    console.error("[data-editor] " + err && err.message || err, err);
    let userMessage = err && err.message && this.translate.instant(err.message) || err;

    // Add details error (if any) under the main message
    const detailMessage = err && err.details && (err.details.message || err.details) || undefined;
    if (detailMessage) {
      userMessage += `<br/><small class="hidden-xs hidden-sm" title="${detailMessage}">`;
      userMessage += detailMessage.length < 70 ? detailMessage : detailMessage.substring(0, 67) + '...';
      userMessage += "</small>";
    }
    this.error = userMessage;
  }

  /* -- protected methods to override -- */

  protected abstract registerForms();

  protected abstract computeTitle(data: T): Promise<string>;

  protected abstract setValue(data: T);

  protected abstract get form(): FormGroup;

  protected abstract getFirstInvalidTabIndex(): number;

  protected abstract canUserWrite(data: T): boolean;


  /* -- protected methods -- */

  async unload(): Promise<void> {
    this.form.reset();
    this.registerForms();
    this._dirty = false;
    this.data = null;
    this.saving = false;
  }

  protected async onNewEntity(data: T, options?: EntityServiceLoadOptions): Promise<void> {
    // can be overwrite by subclasses
  }

  protected async onEntityLoaded(data: T, options?: EntityServiceLoadOptions): Promise<void> {
    // can be overwrite by subclasses
  }

  protected async onEntitySaved(data: T): Promise<void> {
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
    return Promise.resolve(this.form.value);
  }

  /**
   * Compute the title
   * @param data
   */
  protected async updateTitle(data?: T) {
    data = data || this.data;
    const title = await this.computeTitle(data);
    this.title$.next(title);

    // If NOT data, then add to page history
    if (!this.isNewData) {
      const page = await this.computePageHistory(title);
      return this.addToPageHistory(page);
    }
  }

  protected async addToPageHistory(page: HistoryPageReference, opts?: AddToPageHistoryOptions) {
    if (!page) return; // Skip

    return this.settings.addToPageHistory(page, {
      removePathQueryParams: true,
      removeTitleSmallTag: true,
      emitEvent: false,
      ...opts
    });
  }

  protected async computePageHistory(title: string): Promise<HistoryPageReference> {
    return {
      title,
      path: this.router.url
    };
  }

  protected async removePageHistory(opts?: { emitEvent?: boolean; }) {
    return this.settings.removePageHistory(this.router.url, opts);
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


  protected async scrollToTop(duration?: number) {
    duration = toNumber(duration, 500);

    if (!this.content) {
      console.warn(`[root-data-editor] Cannot scroll to top. Missing a 'content' child in the page ${this.constructor.name}`);
      return;
    }

    return this.content.scrollToTop(duration);
  }

  protected markForCheck() {
    this.cd.markForCheck();
  }


}

