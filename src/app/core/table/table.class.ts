import { AfterViewInit, EventEmitter, Injector, Input, OnDestroy, OnInit, Output, ViewChild, Directive } from "@angular/core";
import {MatPaginator} from "@angular/material/paginator";
import {MatSort} from "@angular/material/sort";
import {MatTable} from "@angular/material/table";
import {EMPTY, merge, Observable, of, Subject, Subscription} from 'rxjs';
import {catchError, filter, mergeMap, startWith, switchMap, takeUntil} from "rxjs/operators";
import {TableElement} from "angular4-material-table";
import {AppTableDataSource} from "./table-datasource.class";
import {SelectionModel} from "@angular/cdk/collections";
import {Entity} from "../services/model";
import {AlertController, ModalController, Platform, ToastController} from "@ionic/angular";
import {ActivatedRoute, Router} from "@angular/router";
import {TableSelectColumnsComponent} from './table-select-columns.component';
import {Location} from '@angular/common';
import {ErrorCodes} from "../services/errors";
import {AppFormUtils} from "../form/form.utils";
import {isNil, isNotNil, toBoolean} from "../../shared/shared.module";
import {LocalSettingsService} from "../services/local-settings.service";
import {TranslateService} from "@ngx-translate/core";
import {PlatformService} from "../services/platform.service";
import {
  MatAutocompleteConfigHolder,
  MatAutocompleteFieldAddOptions,
  MatAutocompleteFieldConfig
} from "../../shared/material/material.autocomplete";
import {ToastOptions} from "@ionic/core";
import {Toasts} from "../../shared/toasts";
import {Alerts} from "../../shared/alerts";

export const SETTINGS_DISPLAY_COLUMNS = "displayColumns";
export const DEFAULT_PAGE_SIZE = 20;
export const RESERVED_START_COLUMNS = ['select', 'id'];
export const RESERVED_END_COLUMNS = ['actions'];

export class CellValueChangeListener {
  eventEmitter: EventEmitter<any>;
  subscription: Subscription;
  formPath?: string;
}

@Directive()
export abstract class AppTable<T extends Entity<T>, F = any> implements OnInit, OnDestroy, AfterViewInit {

  private _initialized = false;
  private _subscription = new Subscription();
  private _dataSourceSubscription: Subscription;

  private _cellValueChangesDefs: {
    [key: string]: CellValueChangeListener
  } = {};

  protected _enable = true;
  protected _dirty = false;
  protected allowRowDetail = true;
  protected _onDestroy = new Subject();
  protected _autocompleteHelper: MatAutocompleteConfigHolder;
  protected translate: TranslateService;
  protected alertCtrl: AlertController;
  protected toastController: ToastController;

  pageSize: number;
  excludesColumns = new Array<String>();
  displayedColumns: string[];
  resultsLength: number;
  loading = true;
  error: string;
  isRateLimitReached = false;
  selection = new SelectionModel<TableElement<T>>(true, []);
  editedRow: TableElement<T> = undefined;
  onRefresh = new EventEmitter<any>();
  i18nColumnPrefix = 'COMMON.';
  settingsId: string;
  autocompleteFields: {[key: string]: MatAutocompleteFieldConfig};

  mobile: boolean;

  // Table options
  autoLoad = true;
  @Input() readOnly = false;
  inlineEdition = false;
  focusFirstColumn = false;
  confirmBeforeDelete = false;
  saveBeforeDelete: boolean;
  saveBeforeSort: boolean;

  @Input()
  debug = false;

  @Input() set filter(value: F) {
    this.setFilter(value);
  }

  get filter(): F {
    return this._filter;
  }

  @ViewChild(MatTable, {static: true}) table: MatTable<T>;
  @ViewChild(MatPaginator, {static: true}) paginator: MatPaginator;
  @ViewChild(MatSort, {static: true}) sort: MatSort;

  @Output() onOpenRow = new EventEmitter<{ id?: number; row: TableElement<T> }>();

  @Output() onNewRow: EventEmitter<void> = new EventEmitter<void>();

  @Output() onStartEditingRow = new EventEmitter<TableElement<T>>();

  get $loading(): Observable<boolean> {
    return this.dataSource.loadingSubject.asObservable();
  }

  @Output()
  get dirty(): boolean {
    return this._dirty;
  }

  @Output()
  get valid(): boolean {
    return this.editedRow && this.editedRow.editing ? (!this.editedRow.validator || this.editedRow.validator.valid) : true;
  }

  @Output()
  get invalid(): boolean {
    return this.editedRow && this.editedRow.editing ? (this.editedRow.validator && this.editedRow.validator.invalid) : false;
  }

  @Output()
  get pending(): boolean {
    return this.editedRow && this.editedRow.editing ? (this.editedRow.validator && this.editedRow.validator.pending) : false;
  }


  disable(opts?: {onlySelf?: boolean, emitEvent?: boolean; }) {
    if (!this._initialized || !this.table) return;
    if (this.sort) this.sort.disabled = true;
    this._enable = false;
  }

  enable(opts?: {onlySelf?: boolean, emitEvent?: boolean; }) {
    if (!this._initialized || !this.table) return;
    if (this.sort) this.sort.disabled = false;
    this._enable = true;
  }

  get enabled(): boolean {
    return this._enable;
  }

  get disabled(): boolean {
    return !this._enable;
  }

  markAsDirty(opts?: {onlySelf?: boolean; emitEvent?: boolean; }) {
    this._dirty = true;
    if (!opts || opts.emitEvent !== false) {
      this.markForCheck();
    }
  }

  markAsPristine(opts?: {onlySelf?: boolean; emitEvent?: boolean; }) {
    this._dirty = false;
    if (!opts || opts.emitEvent !== false) {
      this.markForCheck();
    }
  }

  markAsUntouched(opts?: {onlySelf?: boolean; emitEvent?: boolean; }) {
    this._dirty = false;
    this.editedRow = null;
    if (!opts || opts.emitEvent !== false) {
      this.markForCheck();
    }
  }

  markAsTouched(opts?: {onlySelf?: boolean; emitEvent?: boolean; }) {
    if (this.editedRow && this.editedRow.editing) {
      AppFormUtils.markAsTouched(this.editedRow.validator, opts);
      if (!opts || opts.emitEvent !== false) {
        this.markForCheck();
      }
    }
  }

  enableSort() {
    if (this.sort) this.sort.disabled = false;
  }

  disableSort() {
    if (this.sort) this.sort.disabled = true;
  }

  protected constructor(
    protected route: ActivatedRoute,
    protected router: Router,
    protected platform: Platform | PlatformService,
    protected location: Location,
    protected modalCtrl: ModalController,
    protected settings: LocalSettingsService,
    protected columns: string[],
    public dataSource?: AppTableDataSource<T, F>,
    private _filter?: F,
    injector?: Injector
  ) {
    this.mobile = this.platform.is('mobile');
    this.translate = injector && injector.get(TranslateService);
    this.alertCtrl = injector && injector.get(AlertController);
    this.toastController = injector && injector.get(ToastController);
    this._autocompleteHelper = new MatAutocompleteConfigHolder({
      getUserAttributes: (a,b) => settings.getFieldDisplayAttributes(a, b)
    });
    this.autocompleteFields = this._autocompleteHelper.fields;
  }

  ngOnInit() {
    if (this._initialized) return; // Init only once
    this._initialized = true;

    // Set defaults
    this.readOnly = toBoolean(this.readOnly, false); // read/write by default
    this.inlineEdition = this.inlineEdition && !this.readOnly; // force to false when readonly
    this.saveBeforeDelete = toBoolean(this.saveBeforeDelete, !this.readOnly); // force to false when readonly
    this.saveBeforeSort = toBoolean(this.saveBeforeSort, !this.readOnly); // force to false when readonly

    // Check ask user confirmation is possible
    if (this.confirmBeforeDelete && !this.alertCtrl) throw Error("Missing 'alertCtrl' or 'injector' in component's constructor.");

    // Defined unique id for settings for the page
    this.settingsId = this.settingsId || this.generateTableId();

    this.displayedColumns = this.getDisplayColumns();

    // If the user changes the sort order, reset back to the first page.
    this.sort && this.paginator && this.sort.sortChange.subscribe(() => this.paginator.pageIndex = 0);

    merge(
      this.sort && this.sort.sortChange
        .pipe(
          mergeMap(async () => {
            if (this._dirty && this.saveBeforeSort) {
              const saved = await this.save();
              this.markAsDirty(); // restore dirty flag
              return saved;
            }
            return true;
          }),
          filter(res => res === true)
        )
      || EMPTY,
      this.paginator && this.paginator.page || EMPTY,
      this.onRefresh
    )
      .pipe(
        startWith<any, any>(this.autoLoad ? {} : 'skip'),
        switchMap(
          (any: any) => {
            this._dirty = false;
            this.selection.clear();
            this.editedRow = undefined;
            if (any === 'skip' || !this.dataSource) {
              return of(undefined);
            }
            if (!this.dataSource) {
              if (this.debug) console.debug("[table] Skipping data load: no dataSource defined");
              return of(undefined);
            }
            if (this.debug) console.debug("[table] Calling dataSource.watchAll()...");
            return this.dataSource.watchAll(
              this.paginator && this.paginator.pageIndex * this.paginator.pageSize || 0,
              this.paginator && this.paginator.pageSize || this.pageSize || DEFAULT_PAGE_SIZE,
              this.sort && this.sort.active,
              this.sort && this.sort.direction && (this.sort.direction === 'desc' ? 'desc' : 'asc') || undefined,
              this._filter
            );
          }),
        takeUntil(this._onDestroy),
        catchError(err => {
          this.error = err && err.message || err;
          return of(undefined);
        })
      )
      .subscribe(res => {
        if (res && res.data) {
          this.isRateLimitReached = !this.paginator || (res.data.length < this.paginator.pageSize);
          this.resultsLength = isNotNil(res.total) ? res.total : ((this.paginator && this.paginator.pageIndex * (this.paginator.pageSize || DEFAULT_PAGE_SIZE) || 0) + res.data.length);
          if (this.debug) console.debug(`[table] ${res.data.length} rows loaded`);
        } else {
          //if (this.debug) console.debug('[table] NO rows loaded');
          this.isRateLimitReached = true;
          this.resultsLength = 0;
        }
        this.markAsUntouched();
        this.markAsPristine();
        this.markForCheck();
      });

    // Listen datasource events
    if (this.dataSource) this.listenDatasource(this.dataSource);
  }

  ngAfterViewInit() {

    if (!this.table) console.warn(`[table] Missing <mat-table> in the HTML template! Component: ${this.constructor.name}`);

  }

  ngOnDestroy() {
    this._subscription.unsubscribe();

    // Unsubscribe column value changes
    Object.getOwnPropertyNames(this._cellValueChangesDefs)
      .forEach(col => this.stopCellValueChanges(col));
    this._cellValueChangesDefs = {};

    this._onDestroy.next();
  }

  setDatasource(datasource: AppTableDataSource<T, F>) {
    if (this.dataSource) throw new Error("[table] dataSource already set !");
    this.dataSource = datasource;
    if (this._initialized) this.listenDatasource(datasource);
  }

  setFilter(filter: F, opts?: { emitEvent: boolean; }) {
    opts = opts || {emitEvent: true};
    this._filter = filter;
    if (opts.emitEvent) {
      this.onRefresh.emit();
    }
  }

  protected listenDatasource(dataSource: AppTableDataSource<T, F>) {
    if (!dataSource) throw new Error("[table] dataSource not set !");

    // Cleaning previous subscription on datasource
    if (isNotNil(this._dataSourceSubscription)) {
      //if (this.debug)
      console.debug("[table] Many call to listenDatasource(): Cleaning previous subscriptions...");
      this._dataSourceSubscription.unsubscribe();
      this._subscription.remove(this._dataSourceSubscription);
    }

    this._dataSourceSubscription = new Subscription();
    this._dataSourceSubscription.add(this.$loading.subscribe(loading => {
      this.loading = loading;
      this.markForCheck();
    }));

    this._subscription.add(this._dataSourceSubscription);
  }

  confirmAndAddRow(event?: any, row?: TableElement<T>): boolean {
    if (!this.confirmEditCreate(event, row)) {
      return false;
    }

    // Add row
    return this.addRow(event);
  }

  /**
   * Confirm the creation of the given row, or if not specified the currently edited row
   * @param event
   * @param row
   */
  confirmEditCreate(event?: any, row?: TableElement<T>): boolean {
    row = row || this.editedRow;
    if (row && row.editing) {
      if (event) event.stopPropagation();
      // confirmation edition or creation
      if (!row.confirmEditCreate()) {
        // If pending, wait end of validation, then loop
        if (row.validator && row.validator.pending) {
          AppFormUtils.waitWhilePending(row.validator)
            .then(() => this.confirmEditCreate(event, row));
        }
        else {
          if (this.debug) {
            console.warn("[table] Row not valid: unable to confirm", row);
            AppFormUtils.logFormErrors(row.validator, '[table] ');
          }
        }
        return false;
      }
      // If edit finished, forget edited row
      if (row === this.editedRow) {
        this.editedRow = undefined;
        this.markAsDirty();
      }
    }
    return true;
  }

  cancelOrDelete(event: any, row: TableElement<T>) {
    this.editedRow = undefined; // unselect row
    event.stopPropagation();

    this.dataSource.cancelOrDelete(row);

    // If delete (if new row): update counter
    if (row.id === -1) {
      this.resultsLength--;
    }
    //this.markForCheck();
  }

  addRow(event?: any): boolean {
    if (!this._enable) return false;
    if (this.debug) console.debug("[table] Asking for new row...");

    // Use modal if inline edition is disabled
    if (!this.inlineEdition) {
      this.openNewRowDetail();
      return false;
    }

    // Try to finish edited row first
    if (!this.confirmEditCreate()) {
      return false;
    }

    // Add new row
    this.addRowToTable();
    return true;
  }

  async save(): Promise<boolean> {
    if (this.readOnly) {
      throw {code: ErrorCodes.TABLE_READ_ONLY, message: 'ERROR.TABLE_READ_ONLY'};
    }

    this.error = undefined;
    if (!this.confirmEditCreate()) {
      throw {code: ErrorCodes.TABLE_INVALID_ROW_ERROR, message: 'ERROR.TABLE_INVALID_ROW_ERROR'};
    }

    if (this.debug) console.debug("[table] Calling dataSource.save()...");
    try {
      const isOK = await this.dataSource.save();
      if (isOK) this._dirty = false;
      return isOK;
    } catch (err) {
      if (this.debug) console.debug("[table] dataSource.save() return an error:", err);
      this.error = err && err.message || err;
      this.markForCheck();
      throw err;
    }
  }

  cancel(event?: UIEvent) {
    this.onRefresh.emit();
  }

  /** Whether the number of selected elements matches the total number of rows. */
  isAllSelected() {
    return this.selection.selected.length === this.resultsLength;
  }

  /** Selects all rows if they are not all selected; otherwise clear selection. */
  async masterToggle() {
    if (this.loading) return;
    if (this.isAllSelected()) {
      this.selection.clear();
    } else {
      const rows = await this.dataSource.getRows();
      rows.forEach(row => this.selection.select(row));
    }
  }

  async deleteSelection(confirm?: boolean): Promise<void> {
    if (this.readOnly) {
      throw {code: ErrorCodes.TABLE_READ_ONLY, message: 'ERROR.TABLE_READ_ONLY'};
    }
    if (!this._enable) return;
    if (this.loading || this.selection.isEmpty()) return;

    if (this.confirmBeforeDelete && !confirm) {
      confirm = await this.askDeleteConfirmation();
      if (!confirm) return; // user cancelled
      return await this.deleteSelection(true); // Loop with confirmation
    }

    // If data need to be saved first: do it
    if (this._dirty && this.saveBeforeDelete) {
      if (this.debug) console.debug("[table] Saving (before deletion)...");
      const saved = await this.save();
      this.markAsDirty();
      if (!saved) return; // Stop if cannot save
    }

    if (this.debug) console.debug("[table] Delete selection...");

    const rowsToDelete = this.selection.selected.slice()
    // Reverse row order
    // This is a workaround, need because row.delete() has async execution
    // and index cache is updated with a delay)
      .sort((a, b) => a.id > b.id ? -1 : 1);

    try {
      await this.dataSource.deleteAll(rowsToDelete);
      this.resultsLength -= rowsToDelete.length;
      this.selection.clear();
      this.editedRow = undefined;
      this.markAsDirty();
    } catch (err) {
      this.error = err && err.message || err;
    }
  }

  onEditRow(event: MouseEvent, row: TableElement<T>): boolean {
    if (!this._enable) return false;
    if (this.editedRow === row || event.defaultPrevented) return;

    if (!this.confirmEditCreate()) {
      return false;
    }

    if (!row.editing && !this.loading) {
      this.dataSource.startEdit(row);
    }
    this.editedRow = row;
    this.onStartEditingRow.emit(row);
    this._dirty = true;
    return true;
  }

  clickRow(event: MouseEvent, row: TableElement<T>): boolean {
    if (row.id === -1 || row.editing) return true;
    if (event.defaultPrevented || this.loading) return false;

    // Open the detail page (if not inline editing)
    if (!this.inlineEdition) {
      if (this._dirty && this.debug) {
        console.warn("[table] Opening row details, but table has unsaved changes!");
      }

      event.stopPropagation();
      event.preventDefault();

      // No ID defined: unable to open details
      if (isNil(row.currentData.id)) {
        console.warn("[table] Opening row details, but missing currentData.id!");
        //return false;
      }

      this.markAsLoading();
      this.openRow(row.currentData.id, row)
        .then(() => {
          this.markAsLoaded();
        });

      return true;
    }


    return this.onEditRow(event, row);
  }

  protected async openRow(id: number, row: TableElement<T>): Promise<boolean> {
    if (!this.allowRowDetail) return false;

    if (this.onOpenRow.observers.length) {
      this.onOpenRow.emit({id, row});
      return true;
    }

    return await this.router.navigate([id], {
      relativeTo: this.route
    });
  }

  protected async openNewRowDetail(): Promise<boolean> {
    if (!this.allowRowDetail) return false;

    if (this.onNewRow.observers.length) {
      this.onNewRow.emit();
      return true;
    }

    return await this.router.navigate(['new'], {
      relativeTo: this.route
    });
  }

  protected getUserColumns(): string[] {
    return this.settings.getPageSettings(this.settingsId, SETTINGS_DISPLAY_COLUMNS);
  }

  protected getDisplayColumns(): string[] {
    let userColumns = this.getUserColumns();

    // No user override: use defaults
    if (!userColumns) return this.columns;

    // Get fixed start columns
    const fixedStartColumns = this.columns.filter(c => RESERVED_START_COLUMNS.includes(c));

    // Remove end columns
    const fixedEndColumns = this.columns.filter(c => RESERVED_END_COLUMNS.includes(c));

    // Remove fixed columns from user columns
    userColumns = userColumns.filter(c => (!fixedStartColumns.includes(c) && !fixedEndColumns.includes(c) && this.columns.includes(c)));

    return fixedStartColumns
      .concat(userColumns)
      .concat(fixedEndColumns);
  }

  public async openSelectColumnsModal(event?: UIEvent): Promise<any> {
    const fixedColumns = this.columns.slice(0, RESERVED_START_COLUMNS.length);
    const hiddenColumns = this.columns.slice(fixedColumns.length)
      .filter(name => this.displayedColumns.indexOf(name) == -1);
    const columns = this.displayedColumns.slice(fixedColumns.length)
      .concat(hiddenColumns)
      .filter(name => name !== "actions")
      .filter(name => !this.excludesColumns.includes(name))
      .map(name => {
        return {
          name,
          label: this.getI18nColumnName(name),
          visible: this.displayedColumns.indexOf(name) !== -1
        };
      });

    const modal = await this.modalCtrl.create({
      component: TableSelectColumnsComponent,
      componentProps: {columns: columns}
    });

    // Open the modal
    await modal.present();

    // On dismiss
    const res = await modal.onDidDismiss();
    if (!res) return; // CANCELLED

    // Apply columns
    const userColumns = columns && columns.filter(c => c.visible).map(c => c.name) || [];
    this.displayedColumns = RESERVED_START_COLUMNS.concat(userColumns).concat(RESERVED_END_COLUMNS);
    this.markForCheck();

    // Update user settings
    await this.settings.savePageSetting(this.settingsId, userColumns, SETTINGS_DISPLAY_COLUMNS);
  }

  public trackByFn(index: number, row: TableElement<T>) {
    return row.id;
  }

  /* -- protected method -- */

  protected registerSubscription(sub: Subscription) {
    this._subscription.add(sub);
  }

  protected registerAutocompleteField(fieldName: string, options?: MatAutocompleteFieldAddOptions): MatAutocompleteFieldConfig {
    return this._autocompleteHelper.add(fieldName, options);
  }

  protected getI18nColumnName(columnName: string) {
    return this.i18nColumnPrefix + columnName.replace(/([a-z])([A-Z])/g, "$1_$2").toUpperCase();
  }

  protected generateTableId() {
    const id = this.location.path(true).replace(/[?].*$/g, '').replace(/\/[\d]+/g, '_id') + "_" + this.constructor.name;
    //if (this.debug) console.debug("[table] id = " + id);
    return id;
  }

  protected async addRowToTable(): Promise<TableElement<T>> {
    this.focusFirstColumn = true;
    await this.dataSource.asyncCreateNew();
    this.editedRow = this.dataSource.getRow(-1);
    // Emit start editing event
    this.onStartEditingRow.emit(this.editedRow);
    this._dirty = true;
    this.resultsLength++;
    this.markForCheck();
    return this.editedRow;
  }

  protected registerCellValueChanges(name: string, formPath?: string): Observable<any> {
    formPath = formPath || name;
    if (this.debug) console.debug(`[table] New listener {${name}} for value changes on path ${formPath}`);
    this._cellValueChangesDefs[name] = this._cellValueChangesDefs[name] || {
      eventEmitter: new EventEmitter<any>(),
      subscription: null,
      formPath: formPath
    };

    // Start the listener, when editing starts
    this.registerSubscription(
      this.onStartEditingRow.subscribe(row => this.startCellValueChanges(name, row)));

    return this._cellValueChangesDefs[name].eventEmitter;
  }

  protected startCellValueChanges(name: string, row: TableElement<T>) {
    const def = this._cellValueChangesDefs[name];
    if (!def) {
      console.warn("[table] Listener with name {" + name + "} not registered! Please call registerCellValueChanges() before;");
      return;
    }
    // Stop previous subscription
    if (def.subscription) {
      def.subscription.unsubscribe();
      def.subscription = null;
    } else {
      if (this.debug) console.debug(`[table] Start values changes on row path {${def.formPath}}`);
    }

    // Listen value changes, and redirect to event emitter
    const control = row.validator && AppFormUtils.getControlFromPath(row.validator, def.formPath);
    if (!control) {
      console.warn(`[table] Could not listen cell changes: no validator or invalid form path {${def.formPath}}`);
    } else {
      def.subscription = control.valueChanges
        .subscribe((value) => {
          def.eventEmitter.emit(value);
        });

      // Emit the actual value
      def.eventEmitter.emit(control.value);
    }
  }

  protected stopCellValueChanges(name: string) {
    const def = this._cellValueChangesDefs[name];
    if (def && def.subscription) {
      if (this.debug) console.debug("[table] Stop value changes on row path {" + def.formPath + "}");
      def.subscription.unsubscribe();
      def.subscription = null;
    }
  }

  setShowColumn(columnName: string, show: boolean) {
    if (!this.excludesColumns.includes(columnName) !== show) {
      if (!show) {
        this.excludesColumns.push(columnName);
      } else {
        const index = this.excludesColumns.findIndex(value => value === columnName);
        if (index >= 0) this.excludesColumns.splice(index, 1);
      }
    }
  }

  getShowColumn(columnName: string): boolean {
    return !this.excludesColumns.includes(columnName);
  }

  protected startsWithUpperCase(input: string, search: string): boolean {
    return input && input.toUpperCase().startsWith(search);
  }

  protected markForCheck() {
    // Should be override by subclasses, depending on ChangeDetectionStrategy
  }

  protected markAsLoading() {
    if (this.dataSource) {
      this.dataSource.loadingSubject.next(true);
    }
    else {
      this.loading = true;
      this.markForCheck();
    }
  }

  protected markAsLoaded() {
    if (this.dataSource) {
      this.dataSource.loadingSubject.next(false);
    }
    else {
      this.loading = false;
      this.markForCheck();
    }
  }

  protected async askDeleteConfirmation(event?: UIEvent): Promise<boolean> {
    if (!this.alertCtrl) {
      console.warn("[table] Missing alertCtrl in component's constructor. Cannot ask user confirmation before deletion!")
      return true;
    }
    return Alerts.askActionConfirmation(this.alertCtrl, this.translate, true, event);
  }

  protected async showToast(opts: ToastOptions & { error?: boolean; showCloseButton?: boolean }) {
    if (!this.toastController) {
      console.warn("[table] Missing toastController in component's constructor. Cannot show toast");
      return;
    }
    return Toasts.show(this.toastController, this.translate, opts);
  }
}

