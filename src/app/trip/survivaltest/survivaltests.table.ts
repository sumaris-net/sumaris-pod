import { Component, OnInit, Input, OnDestroy, EventEmitter } from "@angular/core";
import { Observable, BehaviorSubject } from 'rxjs';
import { mergeMap, debounceTime } from "rxjs/operators";
import { ValidatorService, TableElement } from "angular4-material-table";
import { AppTableDataSource, AppTable, AccountService } from "../../core/core.module";
import { referentialToString, PmfmStrategy, Sample, TaxonGroupIds, MeasurementUtils, getPmfmName } from "../services/trip.model";
import { ModalController, Platform } from "@ionic/angular";
import { Router, ActivatedRoute } from "@angular/router";
import { Location } from '@angular/common';
import { ReferentialRefService, ProgramService } from "../../referential/referential.module";
import { SurvivalTestValidatorService } from "../services/survivaltest.validator";
import { FormBuilder } from "@angular/forms";
import { TranslateService } from '@ngx-translate/core';
import { environment } from '../../../environments/environment';
import { AcquisitionLevelCodes, EntityUtils, ReferentialRef, isNotNil } from "../../core/services/model";

const PMFM_ID_REGEXP = new RegExp(/\d+/);

import { FormGroup } from "@angular/forms";
import { MeasurementsValidatorService } from "../services/trip.validators";
import { RESERVED_START_COLUMNS, RESERVED_END_COLUMNS } from "../../core/table/table.class";

const RESERVED_COLUMNS: string[] = ['taxonGroup', 'sampleDate'];

@Component({
    selector: 'table-survival-tests',
    templateUrl: 'survivaltests.table.html',
    styleUrls: ['survivaltests.table.scss'],
    providers: [
        { provide: ValidatorService, useClass: SurvivalTestValidatorService }
    ]
})
export class SurvivalTestsTable extends AppTable<Sample, { operationId?: number }> implements OnInit, OnDestroy, ValidatorService {

    private _acquisitionLevel: string = AcquisitionLevelCodes.SURVIVAL_TEST;
    private _implicitTaxonGroup: ReferentialRef;
    private _dataSubject = new BehaviorSubject<Sample[]>([]);

    loading: boolean = true;
    loadingPmfms: boolean = true;
    pmfms = new BehaviorSubject<PmfmStrategy[]>(undefined);
    measurementValuesFormGroupConfig: { [key: string]: any };
    data: Sample[];
    taxonGroups: Observable<ReferentialRef[]>;

    @Input() program: string = environment.defaultProgram;


    set value(data: Sample[]) {
        if (this.data !== data) {
            this.data = data;
            if (!this.loading) this.onRefresh.emit();
        }
    }

    get value(): Sample[] {
        return this.data;
    }

    constructor(
        protected route: ActivatedRoute,
        protected router: Router,
        protected platform: Platform,
        protected location: Location,
        protected modalCtrl: ModalController,
        protected accountService: AccountService,
        protected validatorService: SurvivalTestValidatorService,
        protected measurementsValidatorService: MeasurementsValidatorService,
        protected referentialRefService: ReferentialRefService,
        protected programService: ProgramService,
        protected translate: TranslateService,
        protected formBuilder: FormBuilder
    ) {
        super(route, router, platform, location, modalCtrl, accountService,
            RESERVED_START_COLUMNS.concat(RESERVED_COLUMNS).concat(RESERVED_END_COLUMNS)
        );
        this.i18nColumnPrefix = 'TRIP.SURVIVAL_TEST.TABLE.';
        this.autoLoad = false;
        this.inlineEdition = true;
        this.setDatasource(new AppTableDataSource<any, { operationId?: number }>(
            Sample, this, this,
            {
                prependNewElements: false,
                onNewRow: (row) => this.onCreateNewSample(row)
            }));
        //this.debug = true;
    };

    async ngOnInit() {
        super.ngOnInit();

        this.pmfms
            .filter(pmfms => pmfms && pmfms.length > 0)
            .first()
            .subscribe(pmfms => {
                this.measurementValuesFormGroupConfig = this.measurementsValidatorService.getFormGroupConfig(pmfms);
                let displayedColumns = pmfms.map(p => p.pmfmId.toString());

                this.displayedColumns = RESERVED_START_COLUMNS
                    .concat(RESERVED_COLUMNS)
                    .concat(displayedColumns)
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
                    if (this.debug) console.debug("[survivaltests-table] Searching taxon group on {" + (value || '*') + "}...");
                    return this.referentialRefService.loadAll(0, 10, undefined, undefined,
                        {
                            entityName: 'TaxonGroup',
                            levelId: TaxonGroupIds.FAO,
                            searchText: value as string,
                            searchAttribute: 'label'
                        }).first();
                })
            );

        this.taxonGroups.subscribe(items => {
            this._implicitTaxonGroup = (items.length === 1) && items[0];
        });

        // Start loading Pmfms
        await this.refreshPmfms();
    }

    getRowValidator(): FormGroup {
        return this.getFormGroup();
    }

    getFormGroup(data?: any): FormGroup {
        let formGroup = this.validatorService.getFormGroup(data);
        if (this.measurementValuesFormGroupConfig) {
            formGroup.addControl('measurementValues', this.formBuilder.group(this.measurementValuesFormGroupConfig));
        }
        return formGroup;
    }

    async getMaxRankOrder(): Promise<number> {

        const rows = await this.dataSource.getRows();
        return rows.reduce((res, row) => Math.max(res, row.currentData.rankOrder || 0), 0);
    }

    async onCreateNewSample(row: TableElement<Sample>): Promise<void> {
        // Set computed values
        row.currentData.rankOrder = (await this.getMaxRankOrder()) + 1;
        row.currentData.label = this._acquisitionLevel + "#" + row.currentData.rankOrder;
    }

    loadAll(
        offset: number,
        size: number,
        sortBy?: string,
        sortDirection?: string,
        filter?: any,
        options?: any
    ): Observable<any[]> {
        if (!this.data) {
            if (this.debug) console.debug("[survivaltests-table] Unable to load row: value not set (or not started)");
            return Observable.empty(); // Not initialized
        }
        sortBy = (sortBy !== 'id') && sortBy || 'rankOrder'; // Replace id by rankOrder

        const now = Date.now();
        if (this.debug) console.debug("[survivaltests-table] Loading rows..", this.data);

        this.pmfms
            .filter(pmfms => pmfms && pmfms.length > 0)
            .first()
            .subscribe(pmfms => {
                // Fill samples measurement map
                const data = this.data.map(sample => {
                    const json = sample.asObject();
                    json.measurementValues = MeasurementUtils.normalizeFormValues(sample.measurementValues, pmfms);
                    return json;
                });

                // Sort 
                this.sortSamples(data, sortBy, sortDirection);
                if (this.debug) console.debug(`[survivaltests-table] Rows loaded in ${Date.now() - now}ms`, data);

                this._dataSubject.next(data);
            });

        return this._dataSubject.asObservable();
    }

    async saveAll(data: Sample[], options?: any): Promise<Sample[]> {
        if (!this.data) throw new Error("[survivaltests-table] Could not save table: value not set (or not started)");

        if (this.debug) console.debug("[survivaltests-table] Updating data from rows...");

        const pmfms = this.pmfms.getValue() || [];
        this.data = data.map(json => {
            const sample = Sample.fromObject(json);
            sample.measurementValues = MeasurementUtils.toEntityValues(json.measurementValues, pmfms);
            return sample;
        });

        return this.data;
    }

    async deleteAll(dataToRemove: Sample[], options?: any): Promise<any> {
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
        this.data.push(row.currentData);
        this.selectedRow = row;
        return true;
    }

    referentialToString = referentialToString;

    onTaxonGroupCellFocus(event: any, row: TableElement<any>) {
        this.startCellValueChanges('taxonGroup', row);
    }

    onTaxonGroupCellBlur(event: FocusEvent, row: TableElement<any>) {
        this.stopCellValueChanges('taxonGroup');
        // Apply last implicit value
        if (row.validator.controls.taxonGroup.hasError('entity') && this._implicitTaxonGroup) {
            row.validator.controls.taxonGroup.setValue(this._implicitTaxonGroup);
        }
        this._implicitTaxonGroup = undefined;
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

    getPmfmColumnHeader = getPmfmName;

    public trackByFn(index: number, row: TableElement<Sample>) {
        return row.currentData.rankOrder;
    }

    protected async refreshPmfms(): Promise<PmfmStrategy[]> {
        const candLoadPmfms = isNotNil(this.program) && isNotNil(this._acquisitionLevel);
        if (!candLoadPmfms) {
            return undefined;
        }

        this.loading = true;
        this.loadingPmfms = true;

        // Load pmfms
        const pmfms = (await this.programService.loadProgramPmfms(
            this.program,
            {
                acquisitionLevel: this._acquisitionLevel
            })) || [];

        if (!pmfms.length && this.debug) {
            console.debug(`[survivaltests-table] No pmfm found (program=${this.program}, acquisitionLevel=${this._acquisitionLevel}). Please fill program's strategies !`);
        }

        this.loadingPmfms = false;

        this.pmfms.next(pmfms);

        return pmfms;
    }

    protected sortSamples(data: Sample[], sortBy?: string, sortDirection?: string): Sample[] {
        sortBy = (!sortBy || sortBy === 'id') ? 'rankOrder' : sortBy; // Replace id with rankOrder
        const after = (!sortDirection || sortDirection === 'asc') ? 1 : -1;
        return data.sort((a, b) => {
            const valueA = EntityUtils.getPropertyByPath(a, sortBy);
            const valueB = EntityUtils.getPropertyByPath(b, sortBy);
            return valueA === valueB ? 0 : (valueA > valueB ? after : (-1 * after));
        });
    }
}

