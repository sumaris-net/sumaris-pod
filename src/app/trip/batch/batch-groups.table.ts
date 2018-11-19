import { Component, OnInit, Input, OnDestroy, EventEmitter } from "@angular/core";
import { Observable, BehaviorSubject } from 'rxjs';
import { mergeMap, debounceTime, startWith } from "rxjs/operators";
import { ValidatorService, TableElement } from "angular4-material-table";
import { AppTableDataSource, AppTable, AccountService } from "../../core/core.module";
import { referentialToString, PmfmStrategy, Batch, TaxonGroupIds, MeasurementUtils, getPmfmName } from "../services/trip.model";
import { ModalController, Platform } from "@ionic/angular";
import { Router, ActivatedRoute } from "@angular/router";
import { Location } from '@angular/common';
import { ReferentialRefService, ProgramService } from "../../referential/referential.module";
import { TranslateService } from '@ngx-translate/core';
import { environment } from '../../../environments/environment';
import { EntityUtils, ReferentialRef, isNotNil, Entity, isNil } from "../../core/services/model";
import { MeasurementsValidatorService, BatchGroupsValidatorService } from "../services/trip.validators";
import { RESERVED_START_COLUMNS, RESERVED_END_COLUMNS } from "../../core/table/table.class";
import { TaxonomicLevelIds, PmfmLabelPatterns, MethodIds } from "src/app/referential/services/model";
import { FormGroup, Validators, FormBuilder } from "@angular/forms";
import { SharedValidators } from "../../shared/validator/validators";
import { getControlFromPath } from "src/app/core/form/form.utils";

const PMFM_ID_REGEXP = /\d+/;
const BATCH_GROUP_RESERVED_START_COLUMNS: string[] = ['taxonGroup', 'taxonName'];
const BATCH_GROUP_RESERVED_END_COLUMNS: string[] = [
    //'comments'
];
const BATCH_SUBGROUP_RESERVED_COLUMNS: string[] = ['totalIndividualCount', 'totalWeight', 'samplingRatio', 'samplingIndividualCount', 'samplingWeight'];

class BatchGroup extends Entity<BatchGroup>{
    taxonGroup: ReferentialRef;
    taxonName: ReferentialRef;
    rankOrder: number;
    label: string;
    children: {
        [key: string]: BatchSubGroup
    }

    pmfm: PmfmStrategy;

    clone(): BatchGroup {
        const target = new BatchGroup();
        console.warn("TODO: implement BatchGroup.clone()")
        return target;
    }
}
class BatchSubGroup {
    rankOrder: number;
    label: string;
    totalWeight: number;
    totalIndividualCount: number;
    samplingRatio: number;
    samplingRatioText: string;
    samplingWeight: number;
    isEstimatedWeight: boolean;
}

@Component({
    selector: 'table-batch-groups',
    templateUrl: 'batch-groups.table.html',
    styleUrls: ['batch-groups.table.scss'],
    providers: [
        { provide: ValidatorService, useClass: BatchGroupsValidatorService }
    ]
})
export class BatchGroupsTable extends AppTable<BatchGroup, { operationId?: number }> implements OnInit, OnDestroy, ValidatorService {

    private _program: string = environment.defaultProgram;
    private _acquisitionLevel: string;
    private _implicitValues: { [key: string]: any } = {};
    private _dataSubject = new BehaviorSubject<BatchGroup[]>([]);
    private _onRefreshPmfms = new EventEmitter<any>();

    loading = true;
    loadingPmfms = true;
    pmfms = new BehaviorSubject<PmfmStrategy[]>(undefined);
    qvPmfm: PmfmStrategy;
    defaultWeightPmfm: PmfmStrategy;
    rowGroupFormConfig: { [key: string]: any };
    rowSubGroupFormConfig: { [key: string]: any };
    data: Batch[];
    taxonGroups: Observable<ReferentialRef[]>;
    taxonNames: Observable<ReferentialRef[]>;

    set value(data: Batch[]) {
        if (this.data !== data) {
            this.data = data;
            if (!this.loading) this.onRefresh.emit();
        }
    }

    get value(): Batch[] {
        return this.data;
    }

    @Input()
    set program(value: string) {
        if (this._program === value) return; // Skip if same
        this._program = value;
        if (!this.loading) {
            this._onRefreshPmfms.emit('set program');
        }
    }

    get program(): string {
        return this._program;
    }

    @Input()
    set acquisitionLevel(value: string) {
        if (this._acquisitionLevel !== value) {
            this._acquisitionLevel = value;
            if (!this.loading) this.onRefresh.emit();
        }
    }

    get acquisitionLevel(): string {
        return this._acquisitionLevel;
    }

    constructor(
        protected route: ActivatedRoute,
        protected router: Router,
        protected platform: Platform,
        protected location: Location,
        protected modalCtrl: ModalController,
        protected accountService: AccountService,
        protected measurementsValidatorService: MeasurementsValidatorService,
        protected referentialRefService: ReferentialRefService,
        protected programService: ProgramService,
        protected translate: TranslateService,
        protected formBuilder: FormBuilder
    ) {
        super(route, router, platform, location, modalCtrl, accountService,
            RESERVED_START_COLUMNS.concat(BATCH_GROUP_RESERVED_START_COLUMNS).concat(BATCH_GROUP_RESERVED_END_COLUMNS).concat(RESERVED_END_COLUMNS)
        );
        this.i18nColumnPrefix = 'TRIP.BATCH.TABLE.';
        this.autoLoad = false;
        this.inlineEdition = true;
        this.setDatasource(new AppTableDataSource<any, { operationId?: number }>(
            BatchGroup, this, this, {
                prependNewElements: false,
                onNewRow: (row) => this.onNewBatchGroup(row.currentData)
            }));

        // -- For DEV only
        this.debug = true;
    };

    async ngOnInit() {
        super.ngOnInit();

        this._onRefreshPmfms
            .pipe(
                startWith('ngOnInit')
            )
            .subscribe((event) => {
                this.refreshPmfms(event)
            });

        this.pmfms
            .filter(pmfms => pmfms && pmfms.length > 0)
            .first()
            .subscribe(pmfms => {

                this.refreshRowFormGroupConfig(pmfms);

                let displayedColumns = this.qvPmfm.qualitativeValues.reduce((res, qv) => {
                    return res.concat(BATCH_SUBGROUP_RESERVED_COLUMNS.map(column => (qv.label + '_' + column)));
                }, []);

                console.log(displayedColumns);

                this.displayedColumns = RESERVED_START_COLUMNS
                    .concat(BATCH_GROUP_RESERVED_START_COLUMNS)
                    .concat(displayedColumns)
                    .concat(BATCH_GROUP_RESERVED_END_COLUMNS)
                    .concat(RESERVED_END_COLUMNS);

                this.loading = false;

                if (this.data) this.onRefresh.emit();
            });

        // Taxon group combo
        this.taxonGroups = this.registerCellValueChanges('taxonGroup')
            .pipe(
                debounceTime(250),
                mergeMap((value) => {
                    if (EntityUtils.isNotEmpty(value)) return Observable.of([value]);
                    value = (typeof value === "string") && value || undefined;
                    if (this.debug) console.debug("[batch-table] Searching taxon group on {" + (value || '*') + "}...");
                    return this.referentialRefService.loadAll(0, 20, undefined, undefined,
                        {
                            entityName: 'TaxonGroup',
                            levelId: TaxonGroupIds.FAO,
                            searchText: value as string,
                            searchAttribute: 'label'
                        }).first();
                })
            );

        this.taxonGroups.subscribe(items => {
            this._implicitValues['taxonGroup'] = (items.length === 1) && items[0] || undefined;
        });

        // Taxon name combo
        this.taxonNames = this.registerCellValueChanges('taxonName')
            .pipe(
                debounceTime(250),
                mergeMap((value) => {
                    if (EntityUtils.isNotEmpty(value)) return Observable.of([value]);
                    value = (typeof value === "string") && value || undefined;
                    if (this.debug) console.debug("[batch-table] Searching taxon name on {" + (value || '*') + "}...");
                    return this.referentialRefService.loadAll(0, 20, undefined, undefined,
                        {
                            entityName: 'TaxonName',
                            levelId: TaxonomicLevelIds.SPECIES,
                            searchText: value as string,
                            searchAttribute: 'label'
                        }).first();
                })
            );

        this.taxonNames.subscribe(items => {
            this._implicitValues['taxonName'] = (items.length === 1) && items[0] || undefined;
        });

    }

    getRowValidator(): FormGroup {

        const formGroup = this.formBuilder.group(this.rowGroupFormConfig);
        if (this.rowSubGroupFormConfig) {
            formGroup.addControl('children', this.formBuilder.group(
                this.qvPmfm.qualitativeValues.reduce((res, qv) => {
                    res[qv.label] = this.formBuilder.group(this.rowSubGroupFormConfig[qv.label]);
                    return res;
                }, {})
            ));
        }
        console.log(formGroup);
        return formGroup;
    }

    loadAll(
        offset: number,
        size: number,
        sortBy?: string,
        sortDirection?: string,
        filter?: any,
        options?: any
    ): Observable<BatchGroup[]> {
        if (!this.data) {
            if (this.debug) console.debug("[batch-table] Unable to load row: value not set (or not started)");
            return Observable.empty(); // Not initialized
        }
        sortBy = (sortBy !== 'id') && sortBy || 'rankOrder'; // Replace id by rankOrder

        const now = Date.now();
        if (this.debug) console.debug("[batch-table] Loading rows..", this.data);

        this.pmfms
            .filter(pmfms => pmfms && pmfms.length > 0)
            .first()
            .subscribe(pmfms => {
                const data: BatchGroup[] = [];
                // Transform entities into object array
                /*const data = this.data.map(batch => {
                    const json = batch.asObject();
                    json.measurementValues = MeasurementUtils.normalizeFormValues(batch.measurementValues, pmfms);
                    return json;
                });*/

                // Sort
                //this.sortBatches(data, sortBy, sortDirection);
                if (this.debug) console.debug(`[batch-table] Rows loaded in ${Date.now() - now}ms`, data);

                this._dataSubject.next(data);
            });

        return this._dataSubject.asObservable();
    }

    async saveAll(data: BatchGroup[], options?: any): Promise<BatchGroup[]> {
        if (!this.data) throw new Error("[batch-table] Could not save table: value not set (or not started)");

        if (this.debug) console.debug("[batch-table] Updating data from rows...");

        const pmfms = this.pmfms.getValue() || [];
        /*this.data = data.map(json => {
            const batch = Batch.fromObject(json);
            batch.measurementValues = MeasurementUtils.toEntityValues(json.measurementValues, pmfms);
            return batch;
        });*/

        return data;
    }

    deleteAll(dataToRemove: BatchGroup[], options?: any): Promise<any> {
        this._dirty = true;
        // Noting else to do (make no sense to delete in this.data, will be done in saveAll())
        return Promise.resolve();
    }

    addRow(): boolean {
        if (this.debug) console.debug("[survivaltest-table] Calling addRow()");

        // Create new row
        const result = super.addRow();
        if (!result) return result;

        const row = this.dataSource.getRow(-1);

        //this.data.push(row.currentData);
        this.selectedRow = row;
        return true;
    }

    onCellFocus(event: any, row: TableElement<any>, columnName: string) {
        this.startCellValueChanges(columnName, row);
    }

    onCellBlur(event: FocusEvent, row: TableElement<any>, columnName: string) {
        this.stopCellValueChanges(columnName);
        // Apply last implicit value
        if (row.validator.controls[columnName].hasError('entity') && isNotNil(this._implicitValues[columnName])) {
            row.validator.controls[columnName].setValue(this._implicitValues[columnName]);
        }
        this._implicitValues[columnName] = undefined;
    }

    public trackByFn(index: number, row: TableElement<BatchGroup>) {
        return row.currentData.rankOrder;
    }

    /* -- protected methods -- */

    protected async getMaxRankOrder(): Promise<number> {
        const rows = await this.dataSource.getRows();
        return rows.reduce((res, row) => Math.max(res, row.currentData.rankOrder || 0), 0);
    }

    protected async onNewBatchGroup(batchGroup: BatchGroup, rankOrder?: number): Promise<void> {
        // Set computed values
        batchGroup.rankOrder = isNotNil(rankOrder) ? rankOrder : ((await this.getMaxRankOrder()) + 1);
        batchGroup.label = this._acquisitionLevel + "#" + batchGroup.rankOrder;

        if (isNotNil(this.qvPmfm)) {
            let childCount = 1;
            batchGroup.children = this.qvPmfm.qualitativeValues.reduce((res, qv) => {
                const child = new BatchSubGroup();
                child.rankOrder = childCount++;
                child.label = batchGroup.label + '.' + qv.label + '.' + child.rankOrder;
                res[qv.label] = child;
                return res;
            }, {});
        }
    }

    protected getI18nColumnName(columnName: string): string {

        // Try to resolve PMFM column, using the cached pmfm list
        if (PMFM_ID_REGEXP.test(columnName)) {
            const pmfmId = parseInt(columnName);
            const pmfm = (this.pmfms.getValue() || []).find(p => p.pmfmId === pmfmId);
            if (pmfm) return pmfm.name;
        }

        return super.getI18nColumnName(columnName);
    }

    protected sortBatches(data: Batch[], sortBy?: string, sortDirection?: string): Batch[] {
        sortBy = (!sortBy || sortBy === 'id') ? 'rankOrder' : sortBy; // Replace id with rankOrder
        const after = (!sortDirection || sortDirection === 'asc') ? 1 : -1;
        return data.sort((a, b) => {
            const valueA = EntityUtils.getPropertyByPath(a, sortBy);
            const valueB = EntityUtils.getPropertyByPath(b, sortBy);
            return valueA === valueB ? 0 : (valueA > valueB ? after : (-1 * after));
        });
    }

    protected async refreshPmfms(event?: any): Promise<PmfmStrategy[]> {
        const candLoadPmfms = isNotNil(this._program) && isNotNil(this._acquisitionLevel);
        if (!candLoadPmfms) {
            return undefined;
        }

        this.loading = true;
        this.loadingPmfms = true;

        // Load pmfms
        const pmfms = (await this.programService.loadProgramPmfms(
            this._program,
            {
                acquisitionLevel: this._acquisitionLevel
            })) || [];

        if (!pmfms.length && this.debug) {
            console.debug(`[batch-table] No pmfm found (program=${this.program}, acquisitionLevel=${this._acquisitionLevel}). Please fill program's strategies !`);
        }

        let weightMinRankOrder;
        let defaultWeightPmfm;
        const weightPmfmsByMethod = pmfms.reduce((res, p) => {
            const matches = PmfmLabelPatterns.BATCH_WEIGHT.exec(p.label);
            if (matches) {
                const methodId = p.methodId;
                res[methodId] = p;
                if (isNil(weightMinRankOrder)) weightMinRankOrder = p.rankOrder;
                if (isNil(defaultWeightPmfm)) defaultWeightPmfm = p;
            }
            return res;
        }, {});

        this.defaultWeightPmfm = defaultWeightPmfm;

        this.qvPmfm = pmfms.find(p => p.type == 'qualitative_value');
        if (isNil(weightMinRankOrder) || weightMinRankOrder < this.qvPmfm.rankOrder) {
            throw new Error('Unable to construct table');
            // TODO: No QV - test if HTML code is OK
        }

        this.loadingPmfms = false;

        this.pmfms.next(pmfms);

        return pmfms;
    }

    protected refreshRowFormGroupConfig(pmfms: PmfmStrategy[]) {

        // Compute group form config
        this.rowGroupFormConfig = {
            id: [''],
            rankOrder: ['1', Validators.required],
            label: [''],
            taxonGroup: ['', SharedValidators.entity],
            taxonName: ['', SharedValidators.entity],
            comments: [''],
            parent: ['', SharedValidators.entity],
            children: ['']
        };
        let childCount = 0;
        const decimalValidator = Validators.compose([Validators.min(0), Validators.pattern('^[0-9]+$')]);
        const pctValidator = Validators.compose([Validators.min(0), Validators.max(100)]);

        // Compute sub group form config
        this.rowSubGroupFormConfig = this.qvPmfm.qualitativeValues
            .reduce((res, qv) => {
                childCount++;
                res[qv.label] = {
                    id: [''],
                    rankOrder: [childCount, Validators.required],
                    label: [''],
                    totalIndividualCount: ['', decimalValidator],
                    totalWeight: [''],
                    samplingRatio: ['', pctValidator],
                    samplingRatioText: [''],
                    samplingIndividualCount: ['', decimalValidator],
                    samplingWeight: [''],
                    isEstimatedWeight: [''],
                    comments: ['']
                };

                // TODO: add pmfms where rankOrder > weight and qv
                return res;
            }, {});
    }

    referentialToString = referentialToString;
    getPmfmColumnHeader = getPmfmName;
    getControlFromPath = getControlFromPath;
}

