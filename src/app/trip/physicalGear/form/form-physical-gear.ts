import { Component, OnInit, Input, EventEmitter, Output, ViewChild } from '@angular/core';
import { PhysicalGearValidatorService } from "../validator/validators";
import { FormGroup } from "@angular/forms";
import { PhysicalGear, Referential, GearLevelIds, Trip, Measurement } from "../../services/model";
import { ModalController, Platform } from "@ionic/angular";
import { Moment } from 'moment/moment';
import { DateAdapter } from "@angular/material";
import { Observable } from 'rxjs';
import { mergeMap, startWith } from 'rxjs/operators';
import { merge } from "rxjs/observable/merge";
import { AppForm } from '../../../core/core.module';
import { VesselModal, ReferentialService, VesselService } from "../../../referential/referential.module";
import { referentialToString } from '../../../referential/services/model';
import { MeasurementsForm } from '../../measurement/form/form-measurements';

@Component({
    selector: 'form-physical-gear',
    templateUrl: './form-physical-gear.html',
    styleUrls: ['./form-physical-gear.scss']
})
export class PhysicalGearForm extends AppForm<PhysicalGear> implements OnInit {

    gears: Observable<Referential[]>;
    measurements: Measurement[];
    gear: string;

    @Input() showComment: boolean = true;

    @ViewChild('measurementsForm') measurementsForm: MeasurementsForm;

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
    }

    referentialToString = referentialToString;

    set value(data: PhysicalGear) {

        super.setValue(data);

        this.measurements = data && data.measurements || [];
        this.gear = data && data.gear && data.gear.label;
    }
}
