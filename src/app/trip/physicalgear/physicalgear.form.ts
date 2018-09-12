import { Component, OnInit, Input, EventEmitter, Output, ViewChild } from '@angular/core';
import { PhysicalGearValidatorService } from "../services/physicalgear.validator";
import { PhysicalGear, Referential, GearLevelIds, Trip, Measurement } from "../services/model";
import { Platform } from "@ionic/angular";
import { Moment } from 'moment/moment';
import { DateAdapter } from "@angular/material";
import { Observable } from 'rxjs';
import { mergeMap } from 'rxjs/operators';
import { AppForm } from '../../core/core.module';
import { ReferentialService } from "../../referential/referential.module";
import { referentialToString } from '../../referential/services/model';
import { MeasurementsForm } from '../measurement/measurements.form';

@Component({
    selector: 'form-physical-gear',
    templateUrl: './physicalgear.form.html',
    styleUrls: ['./physicalgear.form.scss']
})
export class PhysicalGearForm extends AppForm<PhysicalGear> implements OnInit {

    data: PhysicalGear;
    gears: Observable<Referential[]>;
    measurements: Measurement[];
    gear: string;

    @Input() showComment: boolean = true;

    @ViewChild('measurementsForm') measurementsForm: MeasurementsForm;

    @Output()
    valueChanges: EventEmitter<any> = new EventEmitter<any>();


    get dirty(): boolean {
        return this.form.dirty || this.measurementsForm.dirty;
    }
    get invalid(): boolean {
        return this.form.invalid || this.measurementsForm.invalid;
    }
    get valid(): boolean {
        return this.form.valid && this.measurementsForm.valid;
    }

    constructor(
        protected dateAdapter: DateAdapter<Moment>,
        protected platform: Platform,
        protected physicalGearValidatorService: PhysicalGearValidatorService,
        protected referentialService: ReferentialService
    ) {

        super(dateAdapter, platform, physicalGearValidatorService.getFormGroup());
    }

    ngOnInit() {
        // Combo: gears
        this.gears = this.form.controls['gear']
            .valueChanges
            .pipe(
                mergeMap(value => {
                    if (value && typeof value == "object") {
                        if (!value.id) return Observable.empty();
                        this.gear = value.label;
                        this.measurementsForm.gear = this.gear;
                        this.measurementsForm.value = this.measurements;
                        return Observable.of([value]);
                    }

                    this.gear = null;
                    if (!value) return Observable.empty();
                    if (typeof value != "string" || value.length < 2) return Observable.of([]);
                    return this.referentialService.loadAll(0, 10, undefined, undefined,
                        {
                            levelId: GearLevelIds.FAO,
                            searchText: value as string
                        },
                        { entityName: 'Gear' });
                }));

        this.measurementsForm.valueChanges
            .debounceTime(300)
            .subscribe(measurements => {
                var json = this.form.value;
                json.measurements = (measurements || []).filter(m => !m.isEmpty());
                this.valueChanges.emit(json);
            });

        this.form.valueChanges
            .debounceTime(300)
            .subscribe(json => {
                this.data.fromObject(json);
                this.data.measurements = (this.measurementsForm.value || []).filter(m => !m.isEmpty());
                this.valueChanges.emit(this.data);
            });
    }

    referentialToString = referentialToString;

    set value(data: PhysicalGear) {

        this.data = data;

        super.setValue(data);

        this.measurements = data && data.measurements || [];
        this.gear = data && data.gear && data.gear.label;
        this.measurementsForm.value = this.measurements;
    }

    get value(): PhysicalGear {
        let json = this.form.value;
        this.data.gear.fromObject(json.gear);
        this.data.measurements = this.measurementsForm.value;
        return this.data;
    }

    public disable(opts?: {
        onlySelf?: boolean;
        emitEvent?: boolean;
    }): void {
        super.disable(opts);
        if (!opts || !opts.onlySelf) {
            this.measurementsForm.disable(opts);
        }
    }

    public enable(opts?: {
        onlySelf?: boolean;
        emitEvent?: boolean;
    }): void {
        super.enable(opts);
        if (!opts || !opts.onlySelf) {
            this.measurementsForm.enable(opts);
        }
    }

    public markAsPristine() {
        super.markAsPristine();
        this.measurementsForm.markAsPristine();
    }

    public markAsUntouched() {
        super.markAsUntouched();
        this.measurementsForm.markAsUntouched();
    }
}
