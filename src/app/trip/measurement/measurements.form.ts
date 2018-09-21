import { Component, OnInit, Input, EventEmitter, Output } from '@angular/core';
import { PmfmStrategy, Measurement, MeasurementUtils } from "../services/trip.model";
import { Platform } from "@ionic/angular";
import { Moment } from 'moment/moment';
import { DateAdapter, FloatLabelType } from "@angular/material";
import { Observable, Subject } from 'rxjs';
import { startWith, switchMap, mergeMap, map, concatMap, combineLatest } from "rxjs/operators";
import { merge } from "rxjs/observable/merge";
import { AppForm } from '../../core/core.module';
import { ReferentialService } from "../../referential/referential.module";
import { FormBuilder } from '@angular/forms';
import { MeasurementsValidatorService } from '../services/measurement.validator';

import { TranslateService } from '@ngx-translate/core';

import { environment } from '../../../environments/environment';

const noop = () => {
};

@Component({
    selector: 'form-measurements',
    templateUrl: './measurements.form.html',
    styleUrls: ['./measurements.form.scss']
})
export class MeasurementsForm extends AppForm<Measurement[]> {

    private _onProgramChange = new EventEmitter<string>();
    private _onAcquisitionLevelChange = new EventEmitter<string>();
    private _onGearChange = new EventEmitter<string>();
    private _onMeasurementsChange = new EventEmitter<Measurement[]>();

    private _program: string = environment.defaultProgram;
    private _gear: string;
    private _acquisitionLevel: string;
    protected _measurements: Measurement[];

    loading: boolean = true;
    pmfms: Observable<PmfmStrategy[]>;
    cachedPmfms: PmfmStrategy[];

    @Output()
    valueChanges: EventEmitter<any> = new EventEmitter<any>();

    @Input() compact: boolean = false;

    @Input() floatLabel: FloatLabelType = "auto";

    get program(): string {
        return this._program;
    }

    @Input()
    set program(value: string) {
        if (this._program === value) return; // Skip if same
        this._program = value;
        //if (!this.loading) this._onProgramChange.emit(value);
        this._onProgramChange.emit(value);
    }


    get acquisitionLevel(): string {
        return this._acquisitionLevel;
    }

    @Input()
    set acquisitionLevel(value: string) {
        if (this._acquisitionLevel === value) return; // Skip if same
        this._acquisitionLevel = value;
        //if (!this.loading) this._onAcquisitionLevelChange.emit(value);
        this._onAcquisitionLevelChange.emit(value);
    }

    get gear(): string {
        return this._gear;
    }

    @Input()
    set gear(value: string) {
        if (this._gear === value) return; // Skip if same
        this._gear = value;
        //if (!this.loading) this._onGearChange.emit(value);
        this._onGearChange.emit(value);
    }

    public set value(value: Measurement[]) {
        if (this._measurements === value) return;
        this._measurements = value;
        //if (!this.loading) this._onMeasurementsChange.emit(value);
        this._onMeasurementsChange.emit(value);
    }

    public get value(): Measurement[] {

        // Find dirty pmfms
        const pmfmsToSync = (this.cachedPmfms || []).filter(pmfm => this.form.controls[pmfm.id.toString()].dirty);
        if (pmfmsToSync.length) {

            // Update measurements value
            if (this.debug) console.debug("[meas-form] [" + this._acquisitionLevel + "] calling updateMeasurementValues...");
            MeasurementUtils.updateMeasurementValues(this.form.value, this._measurements, pmfmsToSync);
            if (this.debug) console.debug("[meas-form] [" + this._acquisitionLevel + "] now measuremnts is:", this._measurements);
        }

        return this._measurements;
    }

    constructor(protected dateAdapter: DateAdapter<Moment>,
        protected platform: Platform,
        protected measurementValidatorService: MeasurementsValidatorService,
        protected formBuilder: FormBuilder,
        protected referentialService: ReferentialService,
        protected translate: TranslateService
    ) {
        super(dateAdapter, platform, formBuilder.group({}));
        // TODO : DEV only
        //this.debug = true;
    }

    ngOnInit() {

        this.pmfms = merge(
            this._onGearChange,
            this._onAcquisitionLevelChange
        )
            .pipe(
                startWith({}),
                switchMap((any: any) => {
                    if (this.debug) console.debug("[meas-form] [" + this._acquisitionLevel + "] Getting PMFM for {" + this._program + "} on gear {" + this._gear + "}...");
                    return this.referentialService.loadProgramPmfms(
                        this._program,
                        {
                            acquisitionLevel: this._acquisitionLevel,
                            gear: this._gear
                        }).first();
                })
            );

        // update form
        this._onMeasurementsChange
            .concatMap(() => this.pmfms)
            .subscribe((pmfms) => {
                if (!pmfms || !pmfms.length) {
                    console.warn("[meas-form] [" + this._acquisitionLevel + "] No PMFM for program {" + this.program + "} and gear {" + this._gear + "}. Please check strategies in database.");
                    this.cachedPmfms = [];
                    return;
                }

                // Update the form group
                if (this.debug) console.debug("[meas-form] [" + this._acquisitionLevel + "] Update form for PMFM:", pmfms);
                this.measurementValidatorService.updateFormGroup(this.form, pmfms);
                this.cachedPmfms = pmfms || [];

                if (this.debug) console.debug("[meas-form] [" + this._acquisitionLevel + "] Input data changed:", this._measurements);
                this._measurements = MeasurementUtils.getMeasurements(this._measurements, this.cachedPmfms);

                // Appply to form
                const formValue = MeasurementUtils.getMeasurementValuesMap(this._measurements, this.cachedPmfms);
                if (this.debug) console.debug("[meas-form] [" + this._acquisitionLevel + "] Updating form with:", formValue);
                this.form.setValue(formValue); // TODO use options

                this.markAsUntouched();
                this.markAsPristine();

                this.loading = false;
            });
    }

    computeNumberInputStep(pmfm: PmfmStrategy): string {

        if (pmfm.maximumNumberDecimals > 0) {
            let step = "0.";
            if (pmfm.maximumNumberDecimals > 1) {
                for (let i = 0; i < pmfm.maximumNumberDecimals - 1; i++) {
                    step += "0"
                }
            }
            step += "1";
            return step;
        }
        else {
            return "1";
        }
    }

    public markAsTouched() {
        this.form.markAsTouched();
        this._measurements.forEach(m => {
            this.form.controls[m.pmfmId].markAsTouched();
        });
    }
}
