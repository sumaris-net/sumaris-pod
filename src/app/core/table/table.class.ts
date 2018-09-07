import { Component, EventEmitter, OnInit, Output, ViewChild, OnDestroy } from "@angular/core";
import { MatPaginator, MatSort, MatTable } from "@angular/material";
import { merge } from "rxjs/observable/merge";
import { Observable } from 'rxjs';
import { startWith, switchMap, mergeMap } from "rxjs/operators";
import { ValidatorService, TableElement } from "angular4-material-table";
import { AppTableDataSource } from "./table-datasource.class";
import { SelectionModel } from "@angular/cdk/collections";
import { Entity, Referential, joinProperties } from "../services/model";
import { Subscription } from "rxjs-compat";
import { ModalController, Platform } from "@ionic/angular";
import { Router, ActivatedRoute } from "@angular/router";
import { AccountService } from '../services/account.service';
import { TableSelectColumnsComponent } from './table-select-columns.component';
import { Location } from '@angular/common';
import { PopoverController } from '@ionic/angular';
import { map } from "rxjs/operators";
import { ErrorCodes } from "../services/errors";

export const SETTINGS_DISPLAY_COLUMNS = "displayColumns";
export const DEFAULT_PAGE_SIZE = 20;
export const RESERVED_START_COLUMNS = ['select', 'id'];
export const RESERVED_END_COLUMNS = ['actions'];

export abstract class AppTable<T extends Entity<T>, F> implements OnInit, OnDestroy {

    private _initialized = false;
    private _subscriptions: Subscription[] = [];
    protected _dirty = false;

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
    onRefresh: EventEmitter<any> = new EventEmitter<any>();
    i18nColumnPrefix = 'COMMON.';
    autoLoad = true;
    settingsId;

    @ViewChild(MatTable) table: MatSort;
    @ViewChild(MatPaginator) paginator: MatPaginator;
    @ViewChild(MatSort) sort: MatSort;

    @Output()
    listChange = new EventEmitter<T[]>();

    public get dirty(): boolean {
        return this._dirty;
    }

    public get valid(): boolean {
        if (this.selectedRow && this.selectedRow.editing) {
            return this.selectedRow.validator.valid;
        }
        return true;
    }

    disable() {
        this.table.disabled = true;
    }

    enable() {
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
        protected validatorService: ValidatorService,
        public dataSource: AppTableDataSource<T, F>,
        protected columns: string[],
        protected filter: F
    ) {
    };

    ngOnInit() {
        if (this._initialized) return; // Init only once
        this._initialized = true;

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
                //map(() => { }),
                startWith(this.autoLoad ? {} : 'skip'),
                switchMap(
                    (any: any) => {
                        this._dirty = false;
                        this.selection.clear();
                        this.selectedRow = null;
                        if (any === 'skip' || !this.dataSource) {
                            return Observable.of(undefined);
                        }
                        if (!this.dataSource) {
                            console.debug("[table] Skipping data load: no dataSource defined");
                            return Observable.of(undefined);
                        }
                        console.debug("[table] Loading datasource...");
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
                    console.debug('[table] Loaded ' + data.length + ' rows');
                }
                else {
                    //console.debug('[table] Loaded NO rows');
                    this.isRateLimitReached = true;
                    this.resultsLength = 0;
                }
            });

        // Subscriptions:
        this._subscriptions.push(this.listChange.subscribe(event => this.onDataChanged(event)));

        if (this.autoLoad) {
            //this.onRefresh.emit();
        }

        // Listen datasource events
        if (this.dataSource) this.listenDatasource(this.dataSource);
    }

    ngOnDestroy() {
        this._subscriptions.forEach(s => s.unsubscribe());
        this._subscriptions = [];
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

    confirmAndAddRow(row: TableElement<T>) {
        // create
        var valid = false;
        if (row.id < 0) {
            valid = this.dataSource.confirmCreate(row);
        }
        // update
        else {
            valid = this.dataSource.confirmEdit(row);
        }
        if (!valid) {
            console.warn("[table] Could NOT confirm row", row);
            return false;
        }

        // Add new row
        this.createNew();
        return true;
    }

    cancelOrDelete(event, row) {
        // cancel
        if (!row.currentData.id) {
            this.resultsLength--;
        }
        this.selectedRow = null;
        event.preventDefault();

        row.cancelOrDelete();
    }

    addRow() {
        // Use modal if not expert mode, or if small screen
        if (this.platform.is('mobile') || !this.inlineEdition) {
            this.onAddRowDetail();
            return;
        }

        // Add new row
        this.focusFirstColumn = true;
        this.createNew();
    }

    createNew() {
        this.dataSource.createNew();
        this._dirty = true;
        this.resultsLength++;
    }

    editRow(row) {
        if (!row.editing) {
            row.startEdit();
        }
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
        if (this.selectedRow && this.selectedRow.editing) {
            var confirm = this.selectedRow.confirmEditCreate();
            if (!confirm) throw { code: ErrorCodes.TABLE_INVALID_ROW_ERROR, message: 'ERROR.TABLE_INVALID_ROW_ERROR' };
        }
        console.log("[table] Saving...");
        try {
            const res = await this.dataSource.save();
            if (res) this._dirty = false;
            return res;
        }
        catch (err) {
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
            if (row.currentData && row.currentData.id >= 0) {
                this.resultsLength--;
            }
        });
        this.selection.clear();
        this.selectedRow = null;
    }

    public onEditRow(event: MouseEvent, row: TableElement<T>): boolean {
        if (this.selectedRow && this.selectedRow === row || event.defaultPrevented) return;
        if (this.selectedRow && this.selectedRow !== row && this.selectedRow.editing) {
            var confirm = this.selectedRow.confirmEditCreate();
            if (!confirm) {
                return false;
            }
        }
        if (!row.editing && !this.loading) {
            row.startEdit();
            row.currentData.dirty = true;
        }
        this.selectedRow = row;
        this._dirty = true;
        return true;
    }

    public onRowClick(event: MouseEvent, row: TableElement<T>): boolean {
        if (!row.currentData.id || row.editing || event.defaultPrevented) return false;

        event.stopPropagation();
        // Open the detail page (if not editing)
        if (!this._dirty && !this.inlineEdition) {
            this.onOpenRowDetail(row.currentData.id, row);
            return true;
        }

        return this.onEditRow(event, row);
    }

    public onOpenRowDetail(id: number, row?: TableElement<T>): Promise<boolean> {
        return this.router.navigate([id], {
            relativeTo: this.route
        });
    }

    public onAddRowDetail(): Promise<any> {
        return this.router.navigate(['new'], {
            relativeTo: this.route
        });
    }

    public getDisplayColumns(): string[] {
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
        const fixedColumns = this.columns.slice(0, 2);
        var hiddenColumns = this.columns.slice(fixedColumns.length)
            .filter(name => this.displayedColumns.indexOf(name) == -1);
        let columns = this.displayedColumns.slice(fixedColumns.length)
            .concat(hiddenColumns)
            .filter(name => name != "actions")
            .map((name, index) => {
                return {
                    name,
                    label: this.i18nColumnPrefix + name.replace(/([a-z])([A-Z])/g, "$1_$2").toUpperCase(),
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
                this.displayedColumns = fixedColumns.concat(userColumns).concat(['actions']);

                // Update user settings
                this.accountService.savePageSetting(this.settingsId, userColumns, SETTINGS_DISPLAY_COLUMNS);
            });
        return modal.present();
    }

    public trackByFn(index: number, row: TableElement<T>) {
        return row.id;
    }

    private generateTableId() {
        return this.location.path(true).replace(/[?].*$/g, '') + "_" + this.constructor.name;
    }
}

