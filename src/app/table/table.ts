import { Component, EventEmitter, OnInit, Output, ViewChild, OnDestroy } from "@angular/core";
import { MatPaginator, MatSort } from "@angular/material";
import { merge } from "rxjs/observable/merge";
import { Observable } from 'rxjs';
import { startWith, switchMap, mergeMap } from "rxjs/operators";
import { ValidatorService, TableElement } from "angular4-material-table";
import { AppTableDataSource } from "../material/material.table";
import { SelectionModel } from "@angular/cdk/collections";
import { Entity, Referential, VesselFeatures } from "../../services/model";
import { Subscription } from "rxjs";
import { ModalController, Platform } from "ionic-angular";
import { Router, ActivatedRoute } from "@angular/router";
import { VesselService } from '../../services/vessel-service';
import { AccountService } from '../../services/account-service';
import { TableSelectColumnsComponent } from '../../components/table/table-select-columns';
import { Location } from '@angular/common';
import { ViewController } from "ionic-angular";
import { PopoverController } from 'ionic-angular';
import { displayEntity } from '../form/form';
import { map } from "rxjs/operators";

export abstract class AppTable<T extends Entity<T>, F> implements OnInit, OnDestroy {

    initialized = false;

    inlineEdition: boolean = false;
    subscriptions: Subscription[] = [];
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
        protected dataSource: AppTableDataSource<T, F>,
        protected columns: string[],
        protected filter: F
    ) {
    };

    ngOnInit() {
        if (this.initialized) return; // Init only once
        this.initialized = true;

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
                startWith({}),
                switchMap(
                    (any: any) => {
                        this.dirty = false;
                        this.selection.clear();
                        this.selectedRow = null;
                        //if (event.first && !this.autoLoad) return Observable.empty();

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
                    console.debug('[table] Loaded ' + data.length + ' rows: ', data);
                }
                else {
                    console.debug('[table] Loaded NO rows');
                    this.isRateLimitReached = true;
                    this.resultsLength = 0;
                }
            });

        // Subscriptions:
        this.subscriptions.push(this.dataSource.onLoading.subscribe(loading => this.loading = loading));
        this.subscriptions.push(this.dataSource.datasourceSubject.subscribe(data => this.listChange.emit(data)));
        this.subscriptions.push(this.listChange.subscribe(event => this.onDataChanged(event)));

        if (this.autoLoad) {
            this.onRefresh.emit();
        }
    }

    ngOnDestroy() {
        this.subscriptions.forEach(s => s.unsubscribe());
        this.subscriptions = [];
    }

    abstract addRowModal(): Promise<any>;

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
            return this.addRowModal();
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

    async save() {
        this.error = undefined;
        if (this.selectedRow && this.selectedRow.editing) {
            var confirm = this.selectedRow.confirmEditCreate();
            if (!confirm) return;
        }
        console.log("[table] Saving...");
        try {
            const res = await this.dataSource.save();
            if (res) this.dirty = false;
        }
        catch (err) {
            this.error = err && err.message || err;
        };
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
            if (row.currentData && row.currentData.id >= 0) {
                row.delete();
                this.selection.deselect(row);
                this.resultsLength--;
            }
        });
        this.selection.clear();
        this.selectedRow = null;
    }

    public onEditRow(event: MouseEvent, row: TableElement<T>) {
        if (this.selectedRow && this.selectedRow === row || event.defaultPrevented) return;
        if (this.selectedRow && this.selectedRow !== row && this.selectedRow.editing) {
            var confirm = this.selectedRow.confirmEditCreate();
            if (!confirm) {
                return;
            }
        }
        if (!row.editing && !this.loading) {
            row.startEdit();
            row.currentData.dirty = true;
        }
        this.selectedRow = row;
        this.dirty = true;
    }

    public onOpenRowDetail(event: MouseEvent, row: TableElement<T>) {
        if (!row.currentData.id || row.editing || event.defaultPrevented) return;

        // Open the detail page (if not editing)
        if (!this.dirty && !this.inlineEdition) {
            return this.router.navigate([row.currentData.id], {
                relativeTo: this.route
            });
        }

        this.onEditRow(event, row);
    }


    public getDisplayColumns(): string[] {
        const fixedColumns = this.columns.slice(0, 2);
        var userColumns = this.accountService.getDisplayColumns(this.location.path(true));
        return userColumns && fixedColumns.concat(userColumns) || this.columns;
    }

    public openSelectColumnsModal(event: any): Promise<any> {
        const fixedColumns = this.columns.slice(0, 2);
        var hiddenColumns = this.columns.slice(fixedColumns.length)
            .filter(name => this.displayedColumns.indexOf(name) == -1);
        let columns = this.displayedColumns.slice(fixedColumns.length)
            .concat(hiddenColumns)
            .map((name, index) => {
                return {
                    name,
                    label: this.i18nColumnPrefix + name.replace(/([a-z])([A-Z])/g, "$1_$2").toUpperCase(),
                    visible: this.displayedColumns.indexOf(name) != -1
                }
            });

        let modal = this.modalCtrl.create(TableSelectColumnsComponent, columns);

        // On dismiss
        modal.onDidDismiss(res => {
            // Apply columns
            var userColumns = columns && columns.filter(c => c.visible).map(c => c.name) || [];
            this.displayedColumns = fixedColumns.concat(userColumns);

            // Update user settings
            this.accountService.saveDisplayColumns(this.location.path(true), userColumns);
        });
        return modal.present();
    }

    public displayEntity(obj: Referential | any, properties?: String[]): string | undefined {
        return obj && obj.id && displayEntity(obj, properties || ['name']) || undefined;
    }

    public displayReferential(obj: Referential | any, properties?: String[]): string | undefined {
        return obj && obj.id && displayEntity(obj, properties || ['label', 'name']) || undefined;
    }

    public displayVessel(obj: VesselFeatures | any): string | undefined {
        return obj && obj.vesselId && displayEntity(obj, ['exteriorMarking', 'name']) || undefined;
    }
}

