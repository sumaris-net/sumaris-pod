import { Component, OnInit, Input, OnDestroy, EventEmitter } from "@angular/core";
import { Observable } from 'rxjs';
import { zip } from "rxjs/observable/zip";
import { map } from "rxjs/operators";
import { ValidatorService } from "angular4-material-table";
import { AppTableDataSource, AppTable, AccountService } from "../../core/core.module";
import { Referential, Operation, Trip, referentialToString, PmfmStrategy, Sample } from "../services/trip.model";
import { ModalController, Platform } from "@ionic/angular";
import { Router, ActivatedRoute } from "@angular/router";
import { Location } from '@angular/common';
import { ReferentialService } from "../../referential/referential.module";
import { OperationService, OperationFilter } from "../services/operation.service";
import { SurvivalTestValidatorService } from "../services/survivaltest.validator";

import { TranslateService } from '@ngx-translate/core';
import { environment } from '../../../environments/environment';
import { AcquisitionLevelCodes } from "../../core/services/model";

const PMFM_NAME_REGEXP = new RegExp(/^(([A-Z]+)([0-9]+))\s*[/]\s*(.*)$/);
import { ValidatorService as ValidatorService2 } from "angular4-material-table";
import { FormGroup, Validators, FormBuilder } from "@angular/forms";
import { MeasurementsValidatorService } from "../services/trip.validators";

const SAMPLE_PROTECTED_COLUMNS: string[] = ['rankOrder', 'taxonGroup', 'sampleDate'];

@Component({
    selector: 'table-survival-tests',
    templateUrl: 'survivaltests.table.html',
    styleUrls: ['survivaltests.table.scss'],
    providers: [
        { provide: ValidatorService, useClass: SurvivalTestValidatorService }
    ]
})
export class SurvivalTestsTable extends AppTable<any, { operationId?: number }> implements OnInit, OnDestroy, ValidatorService {

    private _onGearChange: EventEmitter<any> = new EventEmitter<any>();
    private _onDataChange: EventEmitter<any> = new EventEmitter<any>();
    private _gear: string;
    private _acquisitionLevel: string = AcquisitionLevelCodes.SURVIVAL_TEST;

    started: boolean = false;
    displayedHeaderColumns: string[];
    cachedPmfms: PmfmStrategy[];
    pmfms: Observable<PmfmStrategy[]>;
    pmfmHeaders: Observable<{ id: number, count: number, name: string }[]>;
    data: Sample[];

    @Input() program: string = environment.defaultProgram;

    @Input()
    set gear(value: string) {
        if (this._gear === value) return; // Skip if same
        this._gear = value;
        if (!this.loading) this._onGearChange.emit();
    }

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
        protected translate: TranslateService
    ) {
        super(route, router, platform, location, modalCtrl, accountService,
            ['select'].concat(SAMPLE_PROTECTED_COLUMNS).concat('actions')
        );
        this.i18nColumnPrefix = 'TRIP.SURVIVAL_TEST.TABLE.';
        this.autoLoad = false;
        this.inlineEdition = true;
        this.setDatasource(new AppTableDataSource<any, { operationId?: number }>(Operation, this, this))
        this.debug = true;
    };


    ngOnInit() {
        super.ngOnInit();

        this.pmfms = this.referentialService.loadProgramPmfms(this.program,
            {
                acquisitionLevel: this._acquisitionLevel
            });

        this.pmfmHeaders = this.pmfms
            .pipe(
                map(pmfms => {

                    let mapByLetter = {};
                    let id = 0;
                    return pmfms.reduce((res, pmfm) => {
                        var matches = PMFM_NAME_REGEXP.exec(pmfm.name);
                        if (matches) {
                            var labelLetter = matches[2];
                            if (!mapByLetter[labelLetter]) {
                                mapByLetter[labelLetter] = {
                                    id: id++,
                                    count: 1,
                                    name: "TRIP.SURVIVAL_TEST.TABLE.HEADER_GROUP." + labelLetter
                                };
                                return res.concat(mapByLetter[labelLetter]);
                            }
                            mapByLetter[labelLetter].count++;
                            return res;
                        }
                        return res.concat({
                            id: id++,
                            count: 1,
                            name: undefined
                        });
                    }, []);
                })
            );
        this.pmfms.subscribe(pmfms => {
            this.cachedPmfms = pmfms;
            let displayedColumns = pmfms.reduce((res, pmfm) => {
                return res.concat('' + pmfm.id);
            }, []);

            displayedColumns = ['select']
                .concat(SAMPLE_PROTECTED_COLUMNS)
                .concat(displayedColumns)
                .concat('actions');
            this.displayedColumns = displayedColumns;
            this.started = true;
        });

        zip(
            this.pmfms,
            this._onDataChange
        )
            .subscribe(() => this.onRefresh.emit());
    }

    getRowValidator(): FormGroup {
        return this.getFormGroup();
    }

    getFormGroup(data?: any): FormGroup {
        let formGroup = this.validatorService.getFormGroup(data);
        if (this.cachedPmfms) {
            this.measurementsValidatorService.updateFormGroup(formGroup, this.cachedPmfms, { protectedColumns: SAMPLE_PROTECTED_COLUMNS });
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
        if (!this.data) return Observable.empty(); // Not initialized
        sortBy = sortBy || 'rankOrder';

        // Copy samples into json
        const res = this.data.map(sample => {
            let s: any = sample.asObject();
            console.log(s);

            // Transform measurements array to a map (by pmfm id)
            s.measurements = {};
            this.cachedPmfms.forEach(pmfm => {
                const mIndex = (sample.measurements || []).findIndex(m => m.pmfmId === pmfm.id);
                let m = mIndex != -1 ? sample.measurements.splice(mIndex, 1)[0] : undefined;

                // Read value from measurement
                let value = null;
                if (m) {
                    switch (pmfm.type) {
                        case "qualitative_value":
                            if (m.qualitativeValue && m.qualitativeValue.id) {
                                value = pmfm.qualitativeValues.find(qv => qv.id == m.qualitativeValue.id);
                            }
                            break;
                        case "integer":
                        case "double":
                            value = m.numericalValue;
                            break;
                        case "string":
                            value = m.alphanumericalValue;
                            break;
                        case "boolean":
                            value = m.numericalValue === 1 ? true : (m.numericalValue === 0 ? false : null);
                            break;
                        case "date":
                            value = m.alphanumericalValue;
                            break;
                        default:
                            console.error("[survivaltests-table-" + this._acquisitionLevel + "] Unknown Pmfm type for conversion into form value: " + pmfm.type);
                            value = null;
                    }
                }
                // Set the value (convert undefined into null)
                s.measurements[pmfm.id.toString()] = (value !== undefined ? value : null);
            });
            return s;
        });

        // Sort by column
        const after = (!sortDirection || sortDirection === 'asc') ? 1 : -1;
        res.sort((a, b) =>
            a[sortBy] === b[sortBy] ?
                0 : (a[sortBy] > b[sortBy] ?
                    after : (-1 * after)
                )
        );
        console.debug("[survivaltest-table] Getting data", res);
        return Observable.of(res);
    }

    saveAll(data: any[], options?: any): Promise<any[]> {
        if (!this.data) throw new Error("[table-physical-gears] Could not save table: value not set yet");

        this.data = data;
        return Promise.resolve(this.data);
    }

    deleteAll(dataToRemove: any[], options?: any): Promise<any> {
        console.debug("[table-survival-tests] Remove data", dataToRemove);
        this.data = this.data.filter(item => !dataToRemove.find(g => g === item || g.id === item.id))
        return Promise.resolve();
    }

    getPmfmColumnHeader(pmfm: PmfmStrategy): string {

        var matches = PMFM_NAME_REGEXP.exec(pmfm.name);
        if (matches) {
            return matches[1];
        }
        return pmfm.name;
    }

    addRow() {
        // Skip if error in previous row
        if (this.selectedRow && this.selectedRow.validator.invalid) return;

        // Create new row
        this.createNew();
        const row = this.dataSource.getRow(-1);
        this.data.push(row.currentData);
        this.selectedRow = row;
        row.currentData.rankOrder = this.resultsLength;
    }

    referentialToString = referentialToString;
}

