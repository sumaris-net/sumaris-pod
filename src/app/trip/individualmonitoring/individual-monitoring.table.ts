import { Component, OnInit, Input, OnDestroy, EventEmitter } from "@angular/core";
import { Observable, Subject, BehaviorSubject } from 'rxjs';
import { ValidatorService, TableElement } from "angular4-material-table";
import { AppTableDataSource, AppTable, AccountService, AppFormUtils } from "../../core/core.module";
import { getPmfmName, PmfmStrategy, Sample, MeasurementUtils } from "../services/trip.model";
import { ModalController, Platform } from "@ionic/angular";
import { Router, ActivatedRoute } from "@angular/router";
import { Location } from '@angular/common';
import { ReferentialRefService, ProgramService } from "../../referential/referential.module";
import { IndividualMonitoringService } from "../services/individual-monitoring.validator";
import { RESERVED_START_COLUMNS, RESERVED_END_COLUMNS } from "../../core/table/table.class";
import { debounceTime, map } from "rxjs/operators";
import { FormBuilder, FormGroup, Validators } from "@angular/forms";
import { TranslateService } from '@ngx-translate/core';
import { environment } from '../../../environments/environment';
import { AcquisitionLevelCodes, EntityUtils, isNil, isNotNil } from "../../core/services/model";
import { MeasurementsValidatorService } from "../services/trip.validators";
import { PmfmIds } from "../../referential/services/model";

const PMFM_ID_REGEXP = new RegExp(/\d+/);
const RESERVED_COLUMNS: string[] = ['parent'];

@Component({
    selector: 'table-individual-monitoring',
    templateUrl: 'individual-monitoring.table.html',
    styleUrls: ['individual-monitoring.table.scss'],
    providers: [
        { provide: ValidatorService, useClass: IndividualMonitoringService }
    ]
})
export class IndividualMonitoringTable extends AppTable<Sample, { operationId?: number }> implements OnInit, OnDestroy, ValidatorService {

    private _acquisitionLevel: string = AcquisitionLevelCodes.INDIVIDUAL_MONITORING;
    private _implicitParent: Sample;
    private _availableParents: Sample[] = [];
    private _dataSubject = new BehaviorSubject<Sample[]>([]);

    started: boolean = false;
    pmfms: Observable<PmfmStrategy[]>;
    displayParentPmfm: PmfmStrategy;
    cachedPmfms: PmfmStrategy[];
    measurementValuesFormGroupConfig: { [key: string]: any };
    data: Sample[];
    filteredParents: Observable<Sample[]>;

    @Input() program: string = environment.defaultProgram;


    set value(data: Sample[]) {
        if (this.data !== data) {
            this.data = data;
            if (this.started) this.onRefresh.emit();
        }
    }

    get value(): Sample[] {
        return this.data;
    }

    set availableParents(parents: Sample[]) {
        if (this._availableParents !== parents) {
            // Sort parents by by Tag-ID
            this._availableParents = this.sortSamples(parents, PmfmIds.TAG_ID.toString());

            // Link samples to parent, and delete orphan
            this.linkSamplesToParentAndDeleteOrphan();
        }
    }

    get availableParents(): Sample[] {
        return this._availableParents;
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
            RESERVED_START_COLUMNS.concat(RESERVED_COLUMNS).concat(RESERVED_END_COLUMNS)
        );
        this.i18nColumnPrefix = 'TRIP.INDIVIDUAL_MONITORING.TABLE.';
        this.autoLoad = false;
        this.inlineEdition = true;
        this.setDatasource(new AppTableDataSource<any, { operationId?: number }>(
            Sample, this, this, {
                prependNewElements: false,
                onNewRow: (row) => this.onNewSample(row.currentData)
            }
        ));
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
                .concat(RESERVED_COLUMNS)
                .concat(displayedColumns)
                .concat(RESERVED_END_COLUMNS);
            this.started = true;
            if (this.data) this.onRefresh.emit();
        });

        // Parent combo
        this.filteredParents = this.registerCellValueChanges('parent')
            .pipe(
                debounceTime(250),
                map((value) => {
                    if (EntityUtils.isNotEmpty(value)) return [value];
                    value = (typeof value === "string" && value !== "*") && value || undefined;
                    if (this.debug) console.debug("[monitoring-table] Searching parent {" + (value || '*') + "}...");
                    if (!value) return this._availableParents; // All
                    if (this.displayParentPmfm) { // Search on a specific Pmfm (e.g Tag-ID)
                        return this._availableParents.filter(p => p.measurementValues && (p.measurementValues[this.displayParentPmfm.pmfmId] || '').startsWith(value))
                    }
                    // Search on rankOrder
                    return this._availableParents.filter(p => p.rankOrder && p.rankOrder.toString().startsWith(value));
                })
            )
            ;

        // add implicit value
        this.filteredParents.subscribe(items => {
            this._implicitParent = (items.length === 1) && items[0];
        });

        // Listenning on column 'IS_DEAD' value changes
        this.registerCellValueChanges('isDead', "measurementValues." + PmfmIds.IS_DEAD.toString())
            .subscribe((isDead) => {
                if (!this.selectedRow) return; // Should never occur
                const row = this.selectedRow
                const controls = (row.validator.controls['measurementValues'] as FormGroup).controls;
                if (isDead) {
                    if (row.validator.enabled) {
                        controls[PmfmIds.DEATH_TIME].enable();
                    }
                    controls[PmfmIds.DEATH_TIME].setValidators(Validators.required);
                    if (row.validator.enabled) {
                        controls[PmfmIds.VERTEBRAL_COLUMN_ANALYSIS].enable();
                    }
                    controls[PmfmIds.VERTEBRAL_COLUMN_ANALYSIS].setValidators(Validators.required);
                }
                else {
                    controls[PmfmIds.DEATH_TIME].disable();
                    controls[PmfmIds.DEATH_TIME].setValue(null);
                    controls[PmfmIds.DEATH_TIME].setValidators([]);
                    controls[PmfmIds.VERTEBRAL_COLUMN_ANALYSIS].setValue(null);
                    controls[PmfmIds.VERTEBRAL_COLUMN_ANALYSIS].setValidators([]);
                    controls[PmfmIds.VERTEBRAL_COLUMN_ANALYSIS].disable();
                }
            });
    }

    async getMaxRankOrder(): Promise<number> {

        const rows = await this.dataSource.getRows();
        return rows.reduce((res, row) => Math.max(res, row.currentData.rankOrder || 0), 0);
    }

    async onNewSample(sample: Sample, rankOrder?: number): Promise<void> {
        // Set computed values
        sample.rankOrder = rankOrder || ((await this.getMaxRankOrder()) + 1);
        sample.label = this._acquisitionLevel + "#" + sample.rankOrder;

        // Set default values
        sample.measurementValues[PmfmIds.IS_DEAD] = false;
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
    ): Observable<Sample[]> {
        if (!this.data || !this.started) {
            if (this.debug) console.debug("[monitoring-table] Unable to load row: value not set (or not started)");
            return Observable.empty(); // Not initialized
        }

        const now = Date.now();
        if (this.debug) console.debug("[monitoring-table] Loading rows...", this.data);

        setTimeout(() => {
            // Fill samples measurement map
            const data = this.data.map(data => {
                const sample = data.asObject();
                sample.measurementValues = MeasurementUtils.normalizeFormValues(data.measurementValues, this.cachedPmfms);
                return sample;
            });

            // Link to parent
            this.linkSamplesToParent(data);

            // Sort 
            this.sortSamples(data, sortBy, sortDirection);
            if (this.debug) console.debug(`[monitoring-table] Rows loaded in ${Date.now() - now}ms`, this.data);

            this._dataSubject.next(data);
        });

        return this._dataSubject.asObservable();
    }

    async saveAll(data: Sample[], options?: any): Promise<Sample[]> {
        if (!this.data || !this.started) throw new Error("[monitoring-table] Could not save table: value not set (or not started)");

        if (this.debug) console.debug("[monitoring-table] Updating data from rows...");

        this.data = data.map(json => {
            const sample = Sample.fromObject(json);
            sample.measurementValues = MeasurementUtils.toEntityValues(json.measurementValues, this.cachedPmfms);
            sample.parentId = json.parent && json.parent.id;
            return sample;
        });

        return this.data;
    }

    deleteAll(dataToRemove: Sample[], options?: any): Promise<any> {
        //console.debug("[table-survival-tests] Remove data", dataToRemove);
        //this.data = this.data.filter(item => !dataToRemove.find(g => g === item || g.id === item.id))
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

        // Listen row value changes
        this.startListenRow(row);

        return true;
    }

    onRowClick(event: MouseEvent, row: TableElement<Sample>): boolean {
        const canEdit = super.onRowClick(event, row)
        if (canEdit) this.startListenRow(row);
        return canEdit;
    }

    startListenRow(row: TableElement<Sample>) {
        this.startCellValueChanges('isDead', row);
    }

    parentSampleToString(sample: Sample) {
        if (!sample) return null;
        return sample.measurementValues && sample.measurementValues[PmfmIds.TAG_ID] || `#${sample.rankOrder}`;
    }

    onParentCellFocus(event: any, row: TableElement<any>) {
        this.startCellValueChanges('parent', row);
    }

    onParentCellBlur(event: FocusEvent, row: TableElement<any>) {
        this.stopCellValueChanges('parent');
        // Apply last implicit value
        if (row.validator.controls.parent.hasError('entity') && this._implicitParent) {
            row.validator.controls.parent.setValue(this._implicitParent);
        }
        this._implicitParent = undefined;
    }

    async autoFillTable() {
        if (!this.started) return;
        if (!this.confirmEditCreateSelectedRow()) return;

        const rows = await this.dataSource.getRows();
        const data = rows.map(r => r.currentData);

        let rankOrder = await this.getMaxRankOrder();
        await this._availableParents
            .filter(p => !data.find(s => s.parent && s.parent.id === p.id))
            .map(async p => {
                const sample = new Sample();
                sample.parent = p;
                await this.onNewSample(sample, ++rankOrder);
                data.push(sample);
            });

        this._dataSubject.next(data);
    }

    /* -- protected methods -- */


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

    protected linkSamplesToParent(data: Sample[]) {
        if (!this._availableParents || !data) return;

        data.forEach(s => {
            const parentId = s.parentId || (s.parent && s.parent.id);
            s.parent = isNotNil(parentId) ? this.availableParents.find(p => p.id === parentId) : null;
        });
    }

    /**
     * Remove samples in table, if there have no more parent
     */
    protected async linkSamplesToParentAndDeleteOrphan() {

        const rows = await this.dataSource.getRows();

        // Check if need to delete some rows
        let hasRemovedSample = false;
        const data = rows
            .filter(row => {
                const s = row.currentData;
                const parentId = s.parentId || (s.parent && s.parent.id);

                if (isNil(parentId)) {
                    const parentTagId = s.parent && s.parent.measurementValues && s.parent.measurementValues[PmfmIds.TAG_ID];
                    if (isNil(parentTagId)) {
                        s.parent = undefined; // remove link to parent
                        return true; // not yet a parent: keep (.e.g new row)
                    }
                    // Update the parent, by tagId
                    s.parent = this.availableParents.find(p => (p && p.measurementValues && p.measurementValues[PmfmIds.TAG_ID]) === parentTagId);

                }
                else {
                    // Update the parent, by id
                    s.parent = this.availableParents.find(p => p.id == s.parent.id);
                }

                // Could not found the parent anymore (parent has been delete)
                if (!s.parent) {
                    hasRemovedSample = true;
                    return false;
                }

                if (!row.editing) this.dataSource.refreshValidator(row);

                return true; // Keep only if sample still have a parent
            })
            .map(r => r.currentData);

        if (hasRemovedSample) this._dataSubject.next(data);
    }

    protected sortSamples(data: Sample[], sortBy?: string, sortDirection?: string): Sample[] {
        if (sortBy === "parent") {
            sortBy = 'parent.measurementvalues.' + PmfmIds.TAG_ID;
        }
        sortBy = (!sortBy || sortBy === 'id') ? 'rankOrder' : sortBy; // Replace id with rankOrder
        const after = (!sortDirection || sortDirection === 'asc') ? 1 : -1;
        return data.sort((a, b) => {
            const valueA = EntityUtils.getPropertyByPath(a, sortBy);
            const valueB = EntityUtils.getPropertyByPath(b, sortBy);
            return valueA === valueB ? 0 : (valueA > valueB ? after : (-1 * after));
        }
        );
    }

    getPmfmColumnHeader = getPmfmName;
}

