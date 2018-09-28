import { EventEmitter, OnInit, Output, ViewChild, OnDestroy, Input } from "@angular/core";
import { MatPaginator, MatSort, MatTable } from "@angular/material";
import { merge } from "rxjs/observable/merge";
import { Observable } from 'rxjs';
import { startWith, mergeMap } from "rxjs/operators";
import { TableElement } from "angular4-material-table";
import { AppTableDataSource } from "./table-datasource.class";
import { SelectionModel } from "@angular/cdk/collections";
import { Entity } from "../services/model";
import { Subscription } from "rxjs-compat";
import { ModalController, Platform } from "@ionic/angular";
import { Router, ActivatedRoute } from "@angular/router";
import { AccountService } from '../services/account.service';
import { TableSelectColumnsComponent } from './table-select-columns.component';
import { Location } from '@angular/common';
import { ErrorCodes } from "../services/errors";
import { AppFormUtils } from "../form/form.utils";

export const SETTINGS_DISPLAY_COLUMNS = "displayColumns";
export const DEFAULT_PAGE_SIZE = 20;
export const RESERVED_START_COLUMNS = ['select', 'id'];
export const RESERVED_END_COLUMNS = ['actions'];

export abstract class AppTable<T extends Entity<T>, F> implements OnInit, OnDestroy {

    private _initialized = false;
    private _subscriptions: Subscription[] = [];
    protected _dirty = false;
    private _columnValueChangesConfig: {
        [key: string]: {
            eventEmitter: EventEmitter<any>;
            subscription: Subscription
        }
    } = {};

    inlineEdition: boolean = false;
    displayedColumns: string[];
    resultsLength = 0;
    loading = true;
    focusFirstColumn = false;
    error: string;
    showFilter = false;
    isRateLimitReached = false;
    selection = new SelectionModel<TableElement<T>>(true, []);
    selectedRow: TableElement<T> = undefined;
    onRefresh = new EventEmitter<any>();
    i18nColumnPrefix = 'COMMON.';
    autoLoad = true;
    settingsId;
    mobile: boolean;

    @Input()
    debug = false;

    @ViewChild(MatTable) table: MatSort;
    @ViewChild(MatPaginator) paginator: MatPaginator;
    @ViewChild(MatSort) sort: MatSort;

    @Output()
    listChange = new EventEmitter<T[]>();

    get dirty(): boolean {
        return this._dirty;
    }

    get valid(): boolean {
        if (this.selectedRow && this.selectedRow.editing) {
            if (this.debug && !this.selectedRow.validator.valid) {
                this.logRowErrors(this.selectedRow);
            }
            return this.selectedRow.validator.valid;
        }
        return true;
    }

    get invalid(): boolean {
        return !this.valid;
    }

    disable() {
        if (!this._initialized || !this.table) return;
        this.table.disabled = true;
    }

    enable() {
        if (!this._initialized || !this.table) return;
        this.table.disabled = false;
    }

    markAsPristine() {
        this._dirty = false;
    }

    markAsUntouched() {
        this._dirty = false;
    }

    constructor(
        protected route: ActivatedRoute,
        protected router: Router,
        protected platform: Platform,
        protected location: Location,
        protected modalCtrl: ModalController,
        protected accountService: AccountService,
        protected columns: string[],
        public dataSource?: AppTableDataSource<T, F>,
        protected filter?: F
    ) {
        this.mobile = this.platform.is('mobile');
    };

    ngOnInit() {
        if (this._initialized) return; // Init only once
        this._initialized = true;

        if (!this.table) throw new Error("[table] Missing 'table' component in template !");

        // Defined unique id for settings
        this.settingsId = this.generateTableId();

        this.displayedColumns = this.getDisplayColumns();

        // If the user changes the sort order, reset back to the first page.
        this.sort && this.paginator && this.sort.sortChange.subscribe(() => this.paginator.pageIndex = 0);

        merge(
            this.sort && this.sort.sortChange || EventEmitter.empty(),
            this.paginator && this.paginator.page || EventEmitter.empty(),
            this.onRefresh
        )
            .pipe(
                startWith(this.autoLoad ? {} : 'skip'),
                mergeMap(
                    (any: any) => {
                        this._dirty = false;
                        this.selection.clear();
                        this.selectedRow = null;
                        if (any === 'skip' || !this.dataSource) {
                            return Observable.of(undefined);
                        }
                        if (!this.dataSource) {
                            if (this.debug) console.debug("[table] Skipping data load: no dataSource defined");
                            return Observable.of(undefined);
                        }
                        if (this.debug) console.debug("[table] Calling dataSource.load()...");
                        return this.dataSource.load(
                            this.paginator && this.paginator.pageIndex * this.paginator.pageSize,
                            this.paginator && this.paginator.pageSize || DEFAULT_PAGE_SIZE,
                            this.sort && this.sort.active,
                            this.sort && this.sort.direction,
                            this.filter
                        );
                    })
            )
            .catch(err => {
                this.error = err && err.message || err;
                return Observable.empty();
            })
            .subscribe(data => {
                if (data) {
                    this.isRateLimitReached = !this.paginator || (data.length < this.paginator.pageSize);
                    this.resultsLength = (this.paginator && this.paginator.pageIndex * (this.paginator.pageSize || DEFAULT_PAGE_SIZE) || 0) + data.length;
                    if (this.debug) console.debug('[table] ' + data.length + ' rows loaded');
                }
                else {
                    if (this.debug) console.debug('[table] NO rows loaded');
                    this.isRateLimitReached = true;
                    this.resultsLength = 0;
                }
                this.markAsUntouched();
                this.markAsPristine();
            });

        // Subscriptions:
        this._subscriptions.push(this.listChange.subscribe(event => this.onDataChanged(event)));

        // Listen datasource events
        if (this.dataSource) this.listenDatasource(this.dataSource);
    }

    ngOnDestroy() {
        this._subscriptions.forEach(s => s.unsubscribe());
        this._subscriptions = [];

        // Unsubcribe column value changes
        Object.getOwnPropertyNames(this._columnValueChangesConfig).forEach(columnName => {
            this.unsubscribeCellValueChanges(columnName);
        });
        this._columnValueChangesConfig = {};
    }

    setDatasource(datasource: AppTableDataSource<T, F>) {
        if (this.dataSource) throw new Error("[table] dataSource already set !");
        this.dataSource = datasource;
        this.listenDatasource(datasource);
    }

    listenDatasource(dataSource: AppTableDataSource<T, F>) {
        if (!dataSource) throw new Error("[table] dataSource not set !");
        this._subscriptions.push(dataSource.onLoading.subscribe(loading => this.loading = loading));
        this._subscriptions.push(dataSource.datasourceSubject.subscribe(data => this.listChange.emit(data)));
    }

    confirmAndAddRow(event?: any, row?: TableElement<T>): boolean {
        // create
        if (row && row.editing && !row.confirmEditCreate()) {
            if (this.debug) console.warn("[table] Row not valid: unable to add new row", row);
            return false;
        }

        // Add row
        return this.addRow(event);
    }

    cancelOrDelete(event: any, row: TableElement<T>) {
        this.selectedRow = null; // unselect row
        event.stopPropagation();
        row.cancelOrDelete();

        // If row never saved: this is a delete, else a cancel
        if (row.id == -1) {
            this.resultsLength--;
        }
    }

    addRow(event?: any): boolean {
        if (this.debug) console.debug("[table] Asking for new row...");

        // Use modal if not expert mode, or if small screen
        if (!this.inlineEdition) {
            this.openNewRowDetail();
            return false;
        }

        // Try to finish previous row first
        if (this.selectedRow && this.selectedRow.editing && !this.selectedRow.confirmEditCreate()) {
            if (this.debug) console.warn("[table] Selected row not valid: unable to add new row", this.selectedRow);
            return false;
        }

        // Add new row
        this.addRowToTable();
        return true;
    }

    protected addRowToTable() {
        this.focusFirstColumn = true;
        this.dataSource.createNew();
        this._dirty = true;
        this.resultsLength++;
    }

    onDataChanged(data: T[]) {
        this.error = undefined;
        data.forEach(t => {
            if (!t.id && !t.dirty) {
                t.dirty = true;
            }
        });
    }

    async save(): Promise<boolean> {
        this.error = undefined;
        if (this.selectedRow && this.selectedRow.editing && !this.selectedRow.confirmEditCreate()) {
            if (this.debug) console.warn("[table] Row not valid: unable to save", this.selectedRow);
            throw { code: ErrorCodes.TABLE_INVALID_ROW_ERROR, message: 'ERROR.TABLE_INVALID_ROW_ERROR' };
        }
        if (this.debug) console.debug("[table] Calling dataSource.save()...");
        try {
            const res = await this.dataSource.save();
            if (res) this._dirty = false;
            return res;
        }
        catch (err) {
            if (this.debug) console.debug("[table] dataSource.save() return an error:", err);
            this.error = err && err.message || err;
            throw err;
        };
    }

    cancel() {
        this.onRefresh.emit();
    }

    /** Whether the number of selected elements matches the total number of rows. */
    isAllSelected() {
        const numSelected = this.selection.selected.length;
        return numSelected == this.resultsLength;
    }

    /** Selects all rows if they are not all selected; otherwise clear selection. */
    masterToggle() {
        if (this.loading) return;
        this.isAllSelected() ?
            this.selection.clear() :
            this.dataSource.connect().first().subscribe(rows =>
                rows.forEach(row => this.selection.select(row))
            );
    }

    deleteSelection() {
        if (this.loading) return;
        this.selection.selected.forEach(row => {
            row.delete();
            this.selection.deselect(row);
            //if (row.currentData && row.currentData.id >= 0) {
            this.resultsLength--;
            //}
        });
        this.selection.clear();
        this.selectedRow = null;
    }

    public onEditRow(event: MouseEvent, row: TableElement<T>): boolean {
        if (this.selectedRow && this.selectedRow === row || event.defaultPrevented) return;
        if (this.selectedRow && this.selectedRow !== row) {
            if (this.selectedRow.editing && !this.selectedRow.confirmEditCreate()) {
                if (this.debug) console.warn("[table] selected row not valid: unable to edit another row", this.selectedRow);
                return false;
            }
        }
        if (!row.editing && !this.loading) {
            if (this.debug) console.warn("[table] Starting edition of row", row);
            row.startEdit();
            //AppFormUtils.copyEntity2Form(row.currentData, row.validator);
        }
        this.selectedRow = row;
        this._dirty = true;
        return true;
    }

    public onRowClick(event: MouseEvent, row: TableElement<T>): boolean {
        if (row.id == -1 || row.editing) return true;
        if (event.defaultPrevented) return false;

        // Open the detail page (if not editing)
        if (!this._dirty && !this.inlineEdition) {
            event.stopPropagation();
            this.openEditRowDetail(row.currentData.id, row);
            return true;
        }

        return this.onEditRow(event, row);
    }

    protected openEditRowDetail(id: number, row?: TableElement<T>): Promise<boolean> {
        return this.router.navigate([id], {
            relativeTo: this.route
        });
    }

    protected openNewRowDetail(): Promise<any> {
        return this.router.navigate(['new'], {
            relativeTo: this.route
        });
    }

    protected getDisplayColumns(): string[] {
        var userColumns = this.accountService.getPageSettings(this.settingsId, SETTINGS_DISPLAY_COLUMNS);
        // No user override: use defaults
        if (!userColumns) return this.columns;

        // Get fixed start columns
        const fixedStartColumns = this.columns.filter(value => RESERVED_START_COLUMNS.includes(value));

        // Remove end columns
        const fixedEndColumns = this.columns.filter(value => RESERVED_END_COLUMNS.includes(value));

        // Remove fixed columns from user columns
        userColumns = userColumns.filter(value => (!fixedStartColumns.includes(value) && !fixedEndColumns.includes(value)));
        return fixedStartColumns.concat(userColumns).concat(fixedEndColumns);
    }

    public async openSelectColumnsModal(event: any): Promise<any> {
        const fixedColumns = this.columns.slice(0, RESERVED_START_COLUMNS.length);
        var hiddenColumns = this.columns.slice(fixedColumns.length)
            .filter(name => this.displayedColumns.indexOf(name) == -1);
        let columns = this.displayedColumns.slice(fixedColumns.length)
            .concat(hiddenColumns)
            .filter(name => name != "actions")
            .map((name, index) => {
                return {
                    name,
                    label: this.getI18nColumnName(name),
                    visible: this.displayedColumns.indexOf(name) != -1
                }
            });

        const modal = await this.modalCtrl.create({ component: TableSelectColumnsComponent, componentProps: { columns: columns } });

        // On dismiss
        modal.onDidDismiss()
            .then(res => {
                if (!res) return; // CANCELLED

                // Apply columns
                var userColumns = columns && columns.filter(c => c.visible).map(c => c.name) || [];
                this.displayedColumns = RESERVED_START_COLUMNS.concat(userColumns).concat(RESERVED_END_COLUMNS);

                // Update user settings
                this.accountService.savePageSetting(this.settingsId, userColumns, SETTINGS_DISPLAY_COLUMNS);
            });
        return modal.present();
    }

    protected getI18nColumnName(columnName: string) {
        return this.i18nColumnPrefix + columnName.replace(/([a-z])([A-Z])/g, "$1_$2").toUpperCase()
    }

    public trackByFn(index: number, row: TableElement<T>) {
        return row.id;
    }

    private generateTableId() {
        return this.location.path(true).replace(/[?].*$/g, '') + "_" + this.constructor.name;
    }

    private logRowErrors(row: TableElement<T>): void {

        if (row.validator.valid) return;

        var errorsMessage = "";
        Object.getOwnPropertyNames(row.validator.controls)
            .forEach(key => {
                var control = row.validator.controls[key];
                if (control.invalid) {
                    errorsMessage += "'" + key + "' (" + (control.errors ? Object.getOwnPropertyNames(control.errors) : 'unkown error') + "),";
                }
            });

        if (errorsMessage.length) {
            console.error("[table] Row (id=" + row.id + ") has errors: " + errorsMessage.slice(0, -1));
        }
    }

    protected registerColumnValueChanges(columnName: string): Observable<any> {
        if (this.debug) console.debug("[table] Register column {" + columnName + "} for value changes");
        this._columnValueChangesConfig[columnName] = this._columnValueChangesConfig[columnName] || {
            eventEmitter: new EventEmitter<any>(),
            subscription: null
        };

        return this._columnValueChangesConfig[columnName].eventEmitter;
    }

    public subscribeCellValueChanges(columnName: string, row: TableElement<any>) {
        if (this._columnValueChangesConfig[columnName] && this._columnValueChangesConfig[columnName].subscription) {
            this._columnValueChangesConfig[columnName].subscription.unsubscribe();
            this._columnValueChangesConfig[columnName].subscription = null;
        }

        const columnConfig = this._columnValueChangesConfig[columnName];
        if (!columnConfig) {
            console.warn("[table] Column {" + columnName + "} not resgistered for value changes. Please call registerColumnValueChanges() first;");
            return;
        }

        if (this.debug) console.debug("[table] Subscribe to cell changes, on column {" + columnName + "}");

        // Listen value changes, and redirect to event emitter
        this._columnValueChangesConfig[columnName].subscription = row.validator.controls[columnName].valueChanges

            //TODO check if working 
            //.debounceTime(250)

            .subscribe((value) => {
                this._columnValueChangesConfig[columnName].eventEmitter.emit(value);
            });

        // Emit actual value
        this._columnValueChangesConfig[columnName].eventEmitter.emit(row.validator.controls[columnName].value);
    }

    public unsubscribeCellValueChanges(columnName: string) {
        if (this._columnValueChangesConfig[columnName] && this._columnValueChangesConfig[columnName].subscription) {
            if (this.debug) console.debug("[table] Unsubcribe cell changes, on column {" + columnName + "}");
            this._columnValueChangesConfig[columnName].subscription.unsubscribe();
            this._columnValueChangesConfig[columnName].subscription = null;
        }
    }
}

