import { Component, OnInit, Input, OnDestroy, EventEmitter } from "@angular/core";
import { Observable } from 'rxjs';
import { zip } from "rxjs/observable/zip";
import { mergeMap, debounceTime } from "rxjs/operators";
import { ValidatorService, TableElement } from "angular4-material-table";
import { AppTableDataSource, AppTable, AccountService, AppFormUtils } from "../../core/core.module";
import { referentialToString, PmfmStrategy, Sample, MeasurementUtils, TaxonGroupIds } from "../services/trip.model";
import { ModalController, Platform } from "@ionic/angular";
import { Router, ActivatedRoute } from "@angular/router";
import { Location } from '@angular/common';
import { ReferentialService } from "../../referential/referential.module";
import { SurvivalTestValidatorService } from "../services/survivaltest.validator";
import { FormBuilder } from "@angular/forms";
import { TranslateService } from '@ngx-translate/core';
import { environment } from '../../../environments/environment';
import { AcquisitionLevelCodes, Referential, EntityUtils, ReferentialRef } from "../../core/services/model";

const PMFM_NAME_REGEXP = new RegExp(/^(([A-Z]+)([0-9]+))\s*[/]\s*(.*)$/);

const PMFM_ID_REGEXP = new RegExp(/\d+/);

import { FormGroup } from "@angular/forms";
import { MeasurementsValidatorService } from "../services/trip.validators";
import { RESERVED_START_COLUMNS, RESERVED_END_COLUMNS } from "../../core/table/table.class";

const RESERVED_SAMPLE_COLUMNS: string[] = ['taxonGroup', 'sampleDate'];

@Component({
    selector: 'table-survival-tests',
    templateUrl: 'survivaltests.table.html',
    styleUrls: ['survivaltests.table.scss'],
    providers: [
        { provide: ValidatorService, useClass: SurvivalTestValidatorService }
    ]
})
export class SurvivalTestsTable extends AppTable<Sample, { operationId?: number }> implements OnInit, OnDestroy, ValidatorService {

    private _onDataChange = new EventEmitter<any>();
    private _acquisitionLevel: string = AcquisitionLevelCodes.SURVIVAL_TEST;
    private _implicitTaxonGroup: ReferentialRef;

    started: boolean = false;
    pmfms: Observable<PmfmStrategy[]>;
    cachedPmfms: PmfmStrategy[];
    data: Sample[];
    taxonGroups: Observable<ReferentialRef[]>;

    @Input() program: string = environment.defaultProgram;


    set value(data: Sample[]) {
        if (this.data !== data) {
            this.data = data;
            this._onDataChange.emit();
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
        protected referentialService: ReferentialService,
        protected translate: TranslateService,
        protected formBuilder: FormBuilder
    ) {
        super(route, router, platform, location, modalCtrl, accountService,
            RESERVED_START_COLUMNS.concat(RESERVED_SAMPLE_COLUMNS).concat(RESERVED_END_COLUMNS)
        );
        this.i18nColumnPrefix = 'TRIP.SURVIVAL_TEST.TABLE.';
        this.autoLoad = false;
        this.inlineEdition = true;
        this.setDatasource(new AppTableDataSource<any, { operationId?: number }>(Sample, this, this))
        //this.debug = true;
    };

    ngOnInit() {
        super.ngOnInit();

        this.pmfms = this.referentialService.loadProgramPmfms(
            this.program,
            {
                acquisitionLevel: this._acquisitionLevel
            }).first();

        this.pmfms.subscribe(pmfms => {
            this.cachedPmfms = pmfms;
            let displayedColumns = pmfms.reduce((res, pmfm) => {
                return res.concat('' + pmfm.id);
            }, []);

            this.displayedColumns = RESERVED_START_COLUMNS
                .concat(RESERVED_SAMPLE_COLUMNS)
                .concat(displayedColumns)
                .concat(RESERVED_END_COLUMNS);
            this.started = true;
        });

        zip(
            this.pmfms,
            this._onDataChange
        )
            .subscribe(() => this.onRefresh.emit());

        // Taxon group combo
        this.taxonGroups = this.registerColumnValueChanges('taxonGroup')
            .pipe(
                debounceTime(250),
                mergeMap((value) => {
                    if (EntityUtils.isNotEmpty(value)) return Observable.of([value]);
                    value = (typeof value === "string") && value || undefined;
                    if (this.debug) console.debug("[survivaltests-table] Searching taxon group on {" + (value || '*') + "}...");
                    return this.referentialService.loadAllRef(0, 10, undefined, undefined,
                        {
                            entityName: 'TaxonGroup',
                            levelId: TaxonGroupIds.FAO,
                            searchText: value as string,
                            searchAttribute: 'label'
                        });
                })
            );

        this.taxonGroups.subscribe(items => {
            this._implicitTaxonGroup = (items.length === 1) && items[0];
        });

        // Copy data to validator
        this.dataSource.connect().subscribe(rows => {
            rows.forEach(row => AppFormUtils.copyEntity2Form(row.currentData, row.validator));
        });
    }

    getRowValidator(): FormGroup {
        return this.getFormGroup();
    }

    getFormGroup(data?: any): FormGroup {
        let formGroup = this.validatorService.getFormGroup(data);
        if (this.cachedPmfms) {
            let measForm = formGroup.get('measurementsMap') as FormGroup;
            if (!measForm) {
                measForm = this.formBuilder.group({});
                formGroup.addControl('measurementsMap', measForm);
            }
            this.measurementsValidatorService.updateFormGroup(measForm, this.cachedPmfms);
        }
        return formGroup;
    }

    loadAll(
        offset: number,
        size: number,
        sortBy?: string,
        sortDirection?: string,
        filter?: any,
        options?: any
    ): Observable<any[]> {
        if (!this.data || !this.started) {
            if (this.debug) console.debug("[survivaltests-table] Unable to extracting rows from samples (no data)");
            return Observable.empty(); // Not initialized
        }
        sortBy = (sortBy !== 'id') && sortBy || 'rankOrder'; // Replace id by rankOrder

        const now = new Date();
        if (this.debug) console.debug("[survivaltests-table] Extracting rows from samples:", this.data);

        // Fill samples measurement map
        const res = this.data.slice(0); // copy
        res.forEach(sample => {

            const res = {};
            this.cachedPmfms.forEach(pmfm => {
                let value = sample.measurementsMap[pmfm.id.toString()];
                if (value && pmfm.type === "qualitative_value") {
                    value = pmfm.qualitativeValues.find(qv => qv.id === value);
                }
                res[pmfm.id.toString()] = value || (value === 0 ? 0 : null);
            })
            sample.measurementsMap = res;
        });

        // Sort by column
        const after = (!sortDirection || sortDirection === 'asc') ? 1 : -1;
        res.sort((a, b) =>
            a[sortBy] === b[sortBy] ?
                0 : (a[sortBy] > b[sortBy] ?
                    after : (-1 * after)
                )
        );
        if (this.debug) console.debug("[survivaltests-table] Rows extracted in " + (new Date().getTime() - now.getTime()) + "ms", res);

        return Observable.of(res);
    }

    async saveAll(data: Sample[], options?: any): Promise<Sample[]> {
        if (!this.data) throw new Error("[survivaltests-table] Could not save table: value not set yet");

        const rows = await this.dataSource.getRows();
        if (this.debug) console.debug("[survivaltests-table] Saving data...");

        this.data = rows.map(row => row.currentData);

        return Promise.resolve(this.data);
    }

    deleteAll(dataToRemove: Sample[], options?: any): Promise<any> {
        console.debug("[table-survival-tests] Remove data", dataToRemove);
        this.data = this.data.filter(item => !dataToRemove.find(g => g === item || g.id === item.id))
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
        row.validator.controls['rankOrder'].setValue(this.resultsLength);
        row.validator.controls['label'].setValue(this._acquisitionLevel + "#" + this.resultsLength);
        return true;
    }

    referentialToString = referentialToString;

    onTaxonGroupCellFocus(event: any, row: TableElement<any>) {
        this.subscribeCellValueChanges('taxonGroup', row);
    }

    onTaxonGroupCellBlur(event: FocusEvent, row: TableElement<any>) {
        this.unsubscribeCellValueChanges('taxonGroup');
        // Apply last implicit value
        if (row.validator.controls.taxonGroup.hasError('entity') && this._implicitTaxonGroup) {
            row.validator.controls.taxonGroup.setValue(this._implicitTaxonGroup);
        }
    }

    protected getI18nColumnName(columnName: string): string {

        // Try to resolve PMFM column, using the cached pmfm list
        if (PMFM_ID_REGEXP.test(columnName)) {
            const pmfmId = parseInt(columnName);
            const pmfm = this.cachedPmfms.find(p => p.id === pmfmId);
            if (pmfm) return pmfm.name;
        }

        return super.getI18nColumnName(columnName);
    }

    getPmfmColumnHeader(pmfm: PmfmStrategy): string {

        var matches = PMFM_NAME_REGEXP.exec(pmfm.name);
        if (matches) {
            return matches[1];
        }
        return pmfm.name;
    }

    public trackByFn(index: number, row: TableElement<Sample>) {
        return row.currentData.rankOrder;
    }
}

