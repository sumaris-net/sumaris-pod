import { Component, OnInit, Input, EventEmitter, Output, forwardRef } from '@angular/core';
import { FormGroup } from "@angular/forms";
import { Referential, PmfmStrategy, Measurement } from "../../services/model";
import { ModalController, Platform } from "@ionic/angular";
import { Moment } from 'moment/moment';
import { DateAdapter } from "@angular/material";
import { Observable, Subject } from 'rxjs';
import { startWith, switchMap, mergeMap, debounceTime, map } from 'rxjs/operators';
import { merge } from "rxjs/observable/merge";
import { forkJoin } from "rxjs/observable/forkJoin";
import { AppForm } from '../../../core/core.module';
import { VesselModal, ReferentialService, VesselService } from "../../../referential/referential.module";
import { referentialToString } from '../../../referential/services/model';
import { NG_VALUE_ACCESSOR, ControlValueAccessor, FormBuilder, Validators } from '@angular/forms';
import { MeasurementsValidatorService } from '../validator/validators';

import { environment } from '../../../../environments/environment';

const noop = () => {
};

@Component({
    selector: 'form-measurements',
    templateUrl: './form-measurements.html',
    styleUrls: ['./form-measurements.scss']
})
export class MeasurementsForm extends AppForm<Measurement[]> {

    private _onAcquisitionLevelChange: EventEmitter<any> = new EventEmitter<any>();
    private _onGearChange: EventEmitter<any> = new EventEmitter<any>();
    private _onMeasurementsChange: EventEmitter<any> = new EventEmitter<any>();
    private _onPmfmChange: EventEmitter<any> = new EventEmitter<any>();
    private _measurements: Measurement[];
    private _gear: string;
    private _acquisitionLevel: string;

    loading: boolean = true;
    pmfms: Observable<PmfmStrategy[]>;
    cachedPmfms: PmfmStrategy[];

    @Output()
    valueChanges: EventEmitter<any> = new EventEmitter<any>();

    @Input() program: string = environment.defaultProgram;

    get acquisitionLevel(): string {
        return this._acquisitionLevel;
    }

    @Input()
    set acquisitionLevel(value: string) {
        this._acquisitionLevel = value;
        if (!this.loading) this._onAcquisitionLevelChange.emit();
    }

    get gear(): string {
        return this._gear;
    }

    @Input()
    set gear(value: string) {
        this._gear = value;
        if (!this.loading) this._onGearChange.emit();
    }

    public set value(measurements: Measurement[]) {
        this._measurements = measurements;
        //this.loading = true;
        this._onMeasurementsChange.emit();
    }

    public get value(): Measurement[] {
        return this._measurements;
    }

    constructor(protected dateAdapter: DateAdapter<Moment>,
        protected platform: Platform,
        protected measurementValidatorService: MeasurementsValidatorService,
        protected formBuilder: FormBuilder,
        protected referentialService: ReferentialService
    ) {
        super(dateAdapter, platform, formBuilder.group({}));
    }

    ngOnInit() {

        // Load program pmfms
        this.pmfms = merge(
            this._onAcquisitionLevelChange,
            this._onGearChange
        )
            .pipe(
                startWith({}),
                switchMap((any: any) => {
                    console.debug("[measurements-" + this._acquisitionLevel + "] Loading PMFM for program {" + this.program + "}...");
                    //this.cachedPmfms = undefined;
                    return this.referentialService.loadProgramPmfms(
                        this.program,
                        {
                            acquisitionLevel: this._acquisitionLevel,
                            gear: this._gear
                        });
                })
            );

        // Store pmfms
        this.pmfms.subscribe(pmfms => {
            this.cachedPmfms = pmfms || [];
            if (!pmfms.length) {
                console.warn("[measurements-" + this._acquisitionLevel + "] No PMFM for program {" + this.program + "} and gear {" + this._gear + "}. Please check strategies in database.");
            }
            this._onPmfmChange.emit();
        });

        // update form
        merge(
            this._onPmfmChange,
            this._onMeasurementsChange
        )
            .subscribe(any => {
                if (!this.cachedPmfms) return; // Pmfm not yet loaded

                // Update the form group
                //console.debug("[measurements] Init form with PMFM: ", pmfms);
                this.measurementValidatorService.updateFormGroup(this.form, this.cachedPmfms);

                // Remove not filled measurements
                let measurements = (this._measurements || []).filter(m => !m.isEmpty());
                const formValues: any = {};

                // Init form values from PMFMs
                let rankOrder = 1;
                this._measurements = this.cachedPmfms.map(pmfm => {
                    const mIndex = (measurements || []).findIndex(m => m.pmfmId === pmfm.id);
                    let m = mIndex != -1 ? measurements.splice(mIndex, 1)[0] : new Measurement();
                    m.pmfmId = pmfm.id;
                    m.rankOrder = rankOrder++;

                    // Read value from measurement
                    let value;
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
                            console.error("[measurements-" + this._acquisitionLevel + "] Unknown Pmfm type for conversion into form value: " + pmfm.type);
                            value = null;
                    }
                    // Set the value (convert undefined into null)
                    formValues[pmfm.id.toString()] = (value !== undefined ? value : null);
                    return m;
                });

                // TODO: keep additionnal measurements, but need to retrieve corresponding PMFM
                if (measurements.length) {
                    console.warn("[measurements-" + this._acquisitionLevel + "] Measurements to remove (not in strategy):", measurements);
                }

                //console.debug("[measurements-" + this._acquisitionLevel + "]  Updating form with: ", formValues);

                // Appply to form
                this.form.setValue(formValues);

                this.markAsUntouched();
                this.markAsPristine();

                console.debug("[measurements-" + this._acquisitionLevel + "] Updating form finished !");
                this.loading = false;
            });

        // update value
        merge(
            this._onPmfmChange,
            this.form.valueChanges
        )
            // .pipe(
            //     debounceTime(300)
            // )
            .subscribe(any => {
                if (this.loading || !this.form.touched || !this.cachedPmfms) return;

                // Find dirty pmfms
                const pmfms = this.cachedPmfms.filter(pmfm => this.form.controls['' + pmfm.id].dirty);
                if (!pmfms.length) return;

                // Update measurements value
                this._measurements.forEach(m => {
                    let pmfm = pmfms.find(pmfm => pmfm.id === m.pmfmId);
                    if (pmfm) {
                        const value = this.form.controls['' + pmfm.id].value;
                        if (value === null || value === undefined) return null;
                        switch (pmfm.type) {
                            case "qualitative_value":
                                m.qualitativeValue = value;
                                break;
                            case "integer":
                            case "double":
                                m.numericalValue = value;
                                break;
                            case "string":
                                m.alphanumericalValue = value;
                                break;
                            case "boolean":
                                m.numericalValue = (value === true || value === "true") ? 1 : 0;
                                break;
                            default:
                                console.error("[measurements-" + this._acquisitionLevel + "]  Unknown Pmfm type, to fill measruement value: " + pmfm.type);
                                return null;
                        }
                    }
                });

                this.valueChanges.emit(this._measurements);
            })
    }

    referentialToString = referentialToString;

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
}
