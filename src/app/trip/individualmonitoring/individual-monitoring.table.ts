import { Component, OnInit, Input, OnDestroy, EventEmitter } from "@angular/core";
import { Observable } from 'rxjs';
import { ValidatorService, TableElement } from "angular4-material-table";
import { AppTableDataSource, AppTable, AccountService } from "../../core/core.module";
import { getPmfmName, PmfmStrategy, Sample, MeasurementUtils } from "../services/trip.model";
import { ModalController, Platform } from "@ionic/angular";
import { Router, ActivatedRoute } from "@angular/router";
import { Location } from '@angular/common';
import { ReferentialRefService, ProgramService } from "../../referential/referential.module";
import { IndividualMonitoringService } from "../services/individual-monitoring.validator";
import { RESERVED_START_COLUMNS, RESERVED_END_COLUMNS } from "../../core/table/table.class";
import { zip } from "rxjs/observable/zip";
import { merge } from "rxjs/observable/merge";
import { mergeMap, debounceTime } from "rxjs/operators";
import { FormBuilder, FormGroup, Validators } from "@angular/forms";
import { TranslateService } from '@ngx-translate/core';
import { environment } from '../../../environments/environment';
import { AcquisitionLevelCodes, EntityUtils } from "../../core/services/model";
import { MeasurementsValidatorService } from "../services/trip.validators";
import { PmfmIds } from "../../referential/services/model";

const PMFM_ID_REGEXP = new RegExp(/\d+/);
const RESERVED_SAMPLE_COLUMNS: string[] = ['parentSample'];

@Component({
    selector: 'table-individual-monitoring',
    templateUrl: 'individual-monitoring.table.html',
    styleUrls: ['individual-monitoring.table.scss'],
    providers: [
        { provide: ValidatorService, useClass: IndividualMonitoringService }
    ]
})
export class IndividualMonitoringTable extends AppTable<Sample, { operationId?: number }> implements OnInit, OnDestroy, ValidatorService {

    private _onDataChange = new EventEmitter<any>();
    private _onAvailableParentSamplesChange = new EventEmitter<any>();
    private _acquisitionLevel: string = AcquisitionLevelCodes.INDIVIDUAL_MONITORING;
    private _implicitParentSample: Sample;
    private _availableParentSamples: Sample[] = [];

    started: boolean = false;
    pmfms: Observable<PmfmStrategy[]>;
    displayParentPmfm: PmfmStrategy;
    cachedPmfms: PmfmStrategy[];
    measurementValuesFormGroupConfig: { [key: string]: any };
    data: Sample[];
    parentSamples: Observable<Sample[]>;

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

    set availableParentSamples(parentSamples: Sample[]) {
        if (this._availableParentSamples !== parentSamples) {
            this._availableParentSamples = parentSamples;
            this._onAvailableParentSamplesChange.emit();
        }
    }

    constructor(
        protected route: ActivatedRoute,
        protected router: Router,
        protected platform: Platform,
        protected location: Location,
        protected modalCtrl: ModalController,
        protected accountService: AccountService,
        protected validatorService: IndividualMonitoringService,
        protected measurementsValidatorService: MeasurementsValidatorService,
        protected referentialRefService: ReferentialRefService,
        protected programService: ProgramService,
        protected translate: TranslateService,
        protected formBuilder: FormBuilder
    ) {
        super(route, router, platform, location, modalCtrl, accountService,
            RESERVED_START_COLUMNS.concat(RESERVED_SAMPLE_COLUMNS).concat(RESERVED_END_COLUMNS)
        );
        this.i18nColumnPrefix = 'TRIP.INDIVIDUAL_MONITORING.TABLE.';
        this.autoLoad = false;
        this.inlineEdition = true;
        this.setDatasource(new AppTableDataSource<any, { operationId?: number }>(Sample, this, this))
        //this.debug = true;
    };

    ngOnInit() {
        super.ngOnInit();

        this.pmfms = this.programService.loadProgramPmfms(
            this.program,
            {
                acquisitionLevel: this._acquisitionLevel
            }).first();

        this.pmfms.subscribe(pmfms => {
            this.displayParentPmfm = (pmfms || []).find(p => p.pmfmId == PmfmIds.TAG_ID);
            this.cachedPmfms = (pmfms || []).filter(p => p.pmfmId != PmfmIds.TAG_ID);
            this.measurementValuesFormGroupConfig = this.measurementsValidatorService.getFormGroupConfig(this.cachedPmfms);
            let displayedColumns = this.cachedPmfms.map(p => p.pmfmId.toString());

            this.displayedColumns = RESERVED_START_COLUMNS
                .concat(RESERVED_SAMPLE_COLUMNS)
                .concat(displayedColumns)
                .concat(RESERVED_END_COLUMNS);
            this.started = true;
        });

        merge(
            zip(
                this.pmfms,
                this._onDataChange
            ),
            this._onAvailableParentSamplesChange
        )
            .subscribe(() => this.onRefresh.emit());

        // Tag IDs combo
        this.parentSamples = this.registerColumnValueChanges('parentSample')
            .pipe(
                debounceTime(250),
                mergeMap((value) => {
                    if (EntityUtils.isNotEmpty(value)) return Observable.of([value]);
                    value = (typeof value === "string") && value || undefined;
                    if (this.debug) console.debug("[monitoring-table] Searching tag id {" + (value || '*') + "}...");
                    // TODO filter
                    return Observable.of(this._availableParentSamples);
                })
            );

        // add implicit value
        this.parentSamples.subscribe(items => {
            this._implicitParentSample = (items.length === 1) && items[0];
        });
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

    loadAll(
        offset: number,
        size: number,
        sortBy?: string,
        sortDirection?: string,
        filter?: any,
        options?: any
    ): Observable<any[]> {
        if (!this.data || !this.started) {
            if (this.debug) console.debug("[monitoring-table] Unable to load row: value not set (or not started)");
            return Observable.empty(); // Not initialized
        }
        sortBy = (sortBy !== 'id') && sortBy || 'rankOrder'; // Replace id by rankOrder

        const now = Date.now();
        if (this.debug) console.debug("[monitoring-table] Preparing measurementValues to form...", this.data);

        // Fill samples measurement map
        this.data.forEach(sample => {
            sample.measurementValues = MeasurementUtils.toFormValues(sample.measurementValues, this.cachedPmfms);
        });

        // Sort by column
        const after = (!sortDirection || sortDirection === 'asc') ? 1 : -1;
        this.data.sort((a, b) =>
            a[sortBy] === b[sortBy] ?
                0 : (a[sortBy] > b[sortBy] ?
                    after : (-1 * after)
                )
        );
        if (this.debug) console.debug("[monitoring-table] Rows extracted in " + (Date.now() - now) + "ms", this.data);

        return Observable.of(this.data);
    }

    async saveAll(data: Sample[], options?: any): Promise<Sample[]> {
        if (!this.data || !this.started) throw new Error("[monitoring-table] Could not save table: value not set (or not started)");

        if (this.debug) console.debug("[monitoring-table] Updating data from rows...");

        const rows = await this.dataSource.getRows();
        this.data = rows.map(row => row.currentData);

        return this.data;
    }

    deleteAll(dataToRemove: Sample[], options?: any): Promise<any> {
        console.debug("[table-survival-tests] Remove data", dataToRemove);
        this.data = this.data.filter(item => !dataToRemove.find(g => g === item || g.id === item.id))
        return Promise.resolve();
    }

    addRow(): boolean {
        if (this.debug) console.debug("[monitoring-table] Calling addRow()");

        // Create new row
        const result = super.addRow();
        if (!result) return result;

        const row = this.dataSource.getRow(-1);
        this.data.push(row.currentData);
        this.selectedRow = row;
        row.validator.controls['rankOrder'].setValue(this.resultsLength);
        row.validator.controls['label'].setValue(this._acquisitionLevel + "#" + this.resultsLength);
        const measurementValuesControls = (row.validator.controls['measurementValues'] as FormGroup).controls;
        // Set default values to IS_DEAD pmfm
        measurementValuesControls[PmfmIds.IS_DEAD].setValue(false);
        measurementValuesControls[PmfmIds.DEATH_TIME].disable();
        measurementValuesControls[PmfmIds.VERTEBRAL_COLUMN_ANALYSIS].disable();

        // Enable some pmfms if IS_DEAD change
        measurementValuesControls[PmfmIds.IS_DEAD].valueChanges.subscribe((isDead) => {
            if (isDead) {
                if (row.validator.enabled) {
                    measurementValuesControls[PmfmIds.DEATH_TIME].enable();
                }
                measurementValuesControls[PmfmIds.DEATH_TIME].setValidators(Validators.required);
                if (row.validator.enabled) {
                    measurementValuesControls[PmfmIds.VERTEBRAL_COLUMN_ANALYSIS].enable();
                }
                measurementValuesControls[PmfmIds.VERTEBRAL_COLUMN_ANALYSIS].setValidators(Validators.required);
            }
            else {
                measurementValuesControls[PmfmIds.DEATH_TIME].disable();
                measurementValuesControls[PmfmIds.DEATH_TIME].setValue(null);
                measurementValuesControls[PmfmIds.DEATH_TIME].setValidators([]);
                measurementValuesControls[PmfmIds.VERTEBRAL_COLUMN_ANALYSIS].setValue(null);
                measurementValuesControls[PmfmIds.VERTEBRAL_COLUMN_ANALYSIS].setValidators([]);
                measurementValuesControls[PmfmIds.VERTEBRAL_COLUMN_ANALYSIS].disable();
            }
        });

        return true;
    }

    parentSampleToString(sample: Sample) {
        if (!sample) return null;
        return sample.measurementValues && sample.measurementValues[PmfmIds.TAG_ID] || `#${sample.rankOrder}`;
    }

    onParentSampleCellFocus(event: any, row: TableElement<any>) {
        this.subscribeCellValueChanges('parentSample', row);
    }

    onParentSampleCellBlur(event: FocusEvent, row: TableElement<any>) {
        this.unsubscribeCellValueChanges('parentSample');
        // Apply last implicit value
        if (row.validator.controls.parentSample.hasError('entity') && this._implicitParentSample) {
            row.validator.controls.parentSample.setValue(this._implicitParentSample);
        }
    }

    protected getI18nColumnName(columnName: string): string {

        // Try to resolve PMFM column, using the cached pmfm list
        if (PMFM_ID_REGEXP.test(columnName)) {
            const pmfmId = parseInt(columnName);
            const pmfm = this.cachedPmfms.find(p => p.pmfmId === pmfmId);
            if (pmfm) return pmfm.name;
        }

        return super.getI18nColumnName(columnName);
    }

    public trackByFn(index: number, row: TableElement<Sample>) {
        return row.currentData.rankOrder;
    }

    getPmfmColumnHeader = getPmfmName;
}

