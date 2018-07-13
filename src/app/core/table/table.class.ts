import { Component, EventEmitter, OnInit, Output, ViewChild, OnDestroy } from "@angular/core";
import { MatPaginator, MatSort, MatTable } from "@angular/material";
import { merge } from "rxjs/observable/merge";
import { Observable } from 'rxjs-compat';
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

export abstract class AppTable<T extends Entity<T>, F> implements OnInit, OnDestroy {

    private _initialized = false;
    private _subscriptions: Subscription[] = [];

    inlineEdition: boolean = false;
    displayedColumns;
    resultsLength = 0;
    loading = true;
    focusFirstColumn = false;
    error: string;
    showFilter = false;
    dirty = false;
    isRateLimitReached = false;
    selection = new SelectionModel<TableElement<T>>(true, []);
    selectedRow: TableElement<T> = undefined;
    onRefresh: EventEmitter<any> = new EventEmitter<any>();
    i18nColumnPrefix = 'COMMON.';
    autoLoad = true;

    @ViewChild(MatTable) table: MatSort;
    @ViewChild(MatPaginator) paginator: MatPaginator;
    @ViewChild(MatSort) sort: MatSort;

    @Output()
    listChange = new EventEmitter<T[]>();

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

        this.displayedColumns = this.getDisplayColumns();

        // If the user changes the sort order, reset back to the first page.
        this.sort.sortChange.subscribe(() => this.paginator.pageIndex = 0);

        merge(
            this.sort.sortChange,
            this.paginator.page,
            this.onRefresh
        )
            .pipe(
                //map(() => { }),
                startWith(this.autoLoad ? {} : 'skip'),
                switchMap(
                    (any: any) => {
                        this.dirty = false;
                        this.selection.clear();
                        this.selectedRow = null;
                        if (any === 'skip') {
                            console.debug("[table] Skipping first load (autoload=false)");
                            return Observable.of(undefined);
                        }
                        console.debug("[table] Loading datasource...");
                        return this.dataSource.load(
                            this.paginator.pageIndex * this.paginator.pageSize,
                            this.paginator.pageSize || 10,
                            this.sort.active,
                            this.sort.direction,
                            this.filter
                        );
                    })
            )
            .subscribe(data => {
                if (data) {
                    this.isRateLimitReached = data.length < this.paginator.pageSize;
                    this.resultsLength = this.paginator.pageIndex * this.paginator.pageSize + data.length;
                    console.debug('[table] Loaded ' + data.length + ' rows');
                }
                else {
                    console.debug('[table] Loaded NO rows');
                    this.isRateLimitReached = true;
                    this.resultsLength = 0;
                }
            });

        // Subscriptions:
        this._subscriptions.push(this.dataSource.onLoading.subscribe(loading => this.loading = loading));
        this._subscriptions.push(this.dataSource.datasourceSubject.subscribe(data => this.listChange.emit(data)));
        this._subscriptions.push(this.listChange.subscribe(event => this.onDataChanged(event)));

        if (this.autoLoad) {
            //this.onRefresh.emit();
        }
    }

    ngOnDestroy() {
        this._subscriptions.forEach(s => s.unsubscribe());
        this._subscriptions = [];
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
        this.dataSource.createNew();
        this.resultsLength++;
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
        this.dataSource.createNew();
        this.dirty = true;
        this.resultsLength++;
    }

    editRow(row) {
        if (!row.editing) {
            console.log(row);
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
            if (res) this.dirty = false;
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
            this.dataSource.connect().subscribe(rows =>
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
        this.dirty = true;
        return true;
    }

    public onRowClick(event: MouseEvent, row: TableElement<T>): boolean {
        if (!row.currentData.id || row.editing || event.defaultPrevented) return false;

        // Open the detail page (if not editing)
        if (!this.dirty && !this.inlineEdition) {
            this.onOpenRowDetail(row.currentData.id);
            return true;
        }

        return this.onEditRow(event, row);
    }

    public onOpenRowDetail(id: number): Promise<boolean> {
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
        const fixedColumns = this.columns.slice(0, 2);
        var userColumns = this.accountService.getPageSettings(this.location.path(true), SETTINGS_DISPLAY_COLUMNS);
        userColumns = (userColumns || []).filter(c => c !== 'actions');
        return userColumns && fixedColumns.concat(userColumns).concat(['actions']) || this.columns;
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
        modal.onDidDismiss(res => {
            if (!res) return; // CANCELLED

            // Apply columns
            var userColumns = columns && columns.filter(c => c.visible).map(c => c.name) || [];
            this.displayedColumns = fixedColumns.concat(userColumns).concat(['actions']);

            // Update user settings
            this.accountService.savePageSetting(this.location.path(true), userColumns, SETTINGS_DISPLAY_COLUMNS);
        });
        return modal.present();
    }

    public trackByFn(index: number, row: TableElement<T>) {
        return row.id;
    }
}

