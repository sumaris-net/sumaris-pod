import { Component, Input, EventEmitter, Output } from '@angular/core';
import { PmfmStrategy, Measurement, MeasurementUtils } from "../services/trip.model";
import { Platform } from "@ionic/angular";
import { Moment } from 'moment/moment';
import { DateAdapter, FloatLabelType } from "@angular/material";
import { Subject } from 'rxjs';
import { switchMap } from "rxjs/operators";
import { zip } from "rxjs/observable/zip";
import { AppForm } from '../../core/core.module';
import { ReferentialService } from "../../referential/referential.module";
import { FormBuilder } from '@angular/forms';
import { MeasurementsValidatorService } from '../services/measurement.validator';
import { TranslateService } from '@ngx-translate/core';
import { environment } from '../../../environments/environment';

@Component({
    selector: 'form-measurements',
    templateUrl: './measurements.form.html',
    styleUrls: ['./measurements.form.scss']
})
export class MeasurementsForm extends AppForm<Measurement[]> {

    private _onMeasurementsChange = new EventEmitter<any>();
    private _onRefreshPmfms = new EventEmitter<any>();
    private _program: string = environment.defaultProgram;
    private _gear: string = null;
    private _acquisitionLevel: string;
    protected _measurements: Measurement[];
    protected _debugAcquisitionLevel: string;

    loading: boolean = true;
    pmfms = new Subject<PmfmStrategy[]>();
    cachedPmfms: PmfmStrategy[];

    @Input() showError: boolean = false;

    @Input() compact: boolean = false;

    @Input() floatLabel: FloatLabelType = "auto";

    @Output()
    valueChanges: EventEmitter<any> = new EventEmitter<any>();


    get program(): string {
        return this._program;
    }

    @Input()
    set program(value: string) {
        if (this._program === value) return; // Skip if same
        this._program = value;
        if (!this.loading && this.canRefreshPmfms()) {
            this._onRefreshPmfms.emit('set program');
        }
    }


    get acquisitionLevel(): string {
        return this._acquisitionLevel;
    }

    @Input()
    set acquisitionLevel(value: string) {
        if (this._acquisitionLevel == value) return; // Skip if same
        this._acquisitionLevel = value;
        if (!this.loading && this.canRefreshPmfms()) {
            this._onRefreshPmfms.emit('set acquisitionLevel');
        }
    }

    get gear(): string {
        return this._gear;
    }

    @Input()
    set gear(value: string) {
        if (this._gear == value) return; // Skip if same
        this._gear = value;
        if (!this.loading && this.canRefreshPmfms()) {
            this._onRefreshPmfms.emit('set gear');
        }
    }

    public set value(value: Measurement[]) {
        this.logDebug("Set form value", value);
        if (this._measurements === value) return;
        this._measurements = value;
        //if (!this.loading) {
        this._onMeasurementsChange.emit('set value');
        //}
    }

    public get value(): Measurement[] {

        // Find dirty pmfms, to avoid full update
        const dirtyPmfms = (this.cachedPmfms || []).filter(pmfm => this.form.controls[pmfm.id.toString()].dirty);
        if (dirtyPmfms.length) {

            // Update measurements value
            this.logDebug("Updating measurements from the form value, on dirty pmfms: ", dirtyPmfms);
            MeasurementUtils.updateMeasurementValues(this.form.value, this._measurements, dirtyPmfms);
            this.logDebug("Measurements updated !", this._measurements);
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

        // TODO: DEV only
        //this.debug = true;
        //this._debugAcquisitionLevel = 'TRIP';
    }

    ngOnInit() {
        super.ngOnInit();

        this.logDebug("ngOnInit");

        let now;
        this._onRefreshPmfms.asObservable()
            // refresh only if program + acquisition has been set
            .filter(() => !!this._program && !!this._acquisitionLevel)
            .pipe(
                switchMap((event: any) => {
                    //this.loading = true;
                    if (event) this.logDebug(`call _onRefreshPmfms.emit('${event}')`);
                    now = new Date();
                    this.logDebug(`Loading pmfms for '${this._program}' and gear '${this._gear}'...`);
                    return this.referentialService.loadProgramPmfms(
                        this._program,
                        {
                            acquisitionLevel: this._acquisitionLevel,
                            gear: this._gear
                        });
                })
            )
            .subscribe(pmfms => {
                pmfms = pmfms || [];

                if (!pmfms.length) {
                    const acquisitionLevel = this._acquisitionLevel.toLowerCase().replace(/[_]/g, '-');
                    console.warn(`[meas-form-${acquisitionLevel}] No pmfm found for '${this.program}' and gear '${this._gear}'. Please fill program's strategies for acquisition level '${this._acquisitionLevel}'.`);
                    this.loading = false; // mark as loaded
                }
                else {
                    this.logDebug(`Pmfms for '${this._program}' loaded in ${new Date().getTime() - now.getTime()}ms`, pmfms);
                    this.pmfms.next(pmfms);
                    // Emit measurement because of zip(), that wait for measurement event
                    if (this._measurements && this.cachedPmfms) {
                        this._onMeasurementsChange.emit('_onRefreshPmfms.subscribe');
                    }

                }
                this.cachedPmfms = pmfms;
            });

        // Update the form group
        zip(
            this._onMeasurementsChange,
            this.pmfms
        )
            .subscribe(([event, pmfms]) => {

                this.loading = true;

                if (event) this.logDebug(`call _onMeasurementsChange.emit('${event}')`);
                const now = new Date();
                this.logDebug("Updating form, using pmfms:", pmfms);

                this.measurementValidatorService.updateFormGroup(this.form, pmfms);
                this._measurements = MeasurementUtils.getMeasurements(this._measurements, pmfms);
                const formValue = MeasurementUtils.getMeasurementValuesMap(this._measurements, pmfms);
                this.form.setValue(formValue, {
                    onlySelf: true,
                    emitEvent: false
                });

                this.logDebug(`Form updated in ${new Date().getTime() - now.getTime()}ms`, formValue);

                this.markAsUntouched();
                this.markAsPristine();
                this.cachedPmfms = pmfms;
                this.loading = false;
            });

        if (this.canRefreshPmfms()) {
            this._onRefreshPmfms.next('ngOnInit');
        }
        if (this._measurements) {
            this._onMeasurementsChange.next('ngOnInit');
        }

    }

    public markAsTouched() {
        this.form.markAsTouched();
        this._measurements.forEach(m => {
            this.form.controls[m.pmfmId].markAsTouched();
        });
    }

    /* -- protected methods -- */

    protected logDebug(message: string, args?: any) {
        if (this.debug && (!this._debugAcquisitionLevel || (!this._acquisitionLevel || this._acquisitionLevel == this._debugAcquisitionLevel))) {
            const acquisitionLevel = this._acquisitionLevel && this._acquisitionLevel.toLowerCase().replace(/[_]/g, '-') || '?';
            if (!args) console.debug(`[meas-form-${acquisitionLevel}] ${message}`)
            else console.debug(`[meas-form-${acquisitionLevel}] ${message}`, args);
        }
    }

    protected canRefreshPmfms(): boolean {
        return true; //!!this._program && !!this._acquisitionLevel;
    }
}
