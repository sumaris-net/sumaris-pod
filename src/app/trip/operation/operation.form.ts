import { Component, OnInit, Input, EventEmitter } from '@angular/core';
import { OperationValidatorService } from "../services/operation.validator";
import { Operation, Referential, Trip, PhysicalGear } from "../services/trip.model";
import { Platform } from "@ionic/angular";
import { Moment } from 'moment/moment';
import { DateAdapter } from "@angular/material";
import { Observable } from 'rxjs';
import { debounceTime, mergeMap, map } from 'rxjs/operators';
import { merge } from "rxjs/observable/merge";
import { AppForm } from '../../core/core.module';
import { ReferentialService } from "../../referential/referential.module";
import { referentialToString } from '../../referential/services/model';

@Component({
    selector: 'form-operation',
    templateUrl: './operation.form.html',
    styleUrls: ['./operation.form.scss']
})
export class OperationForm extends AppForm<Operation> implements OnInit {

    labelColSize = 3;

    trip: Trip;
    metiers: Observable<Referential[]>;
    physicalGears: Observable<PhysicalGear[]>;

    onFocusPhysicalGear: EventEmitter<any> = new EventEmitter<any>();
    onFocusMetier: EventEmitter<any> = new EventEmitter<any>();

    @Input() showComment: boolean = true;
    @Input() showError: boolean = true;

    constructor(
        protected dateAdapter: DateAdapter<Moment>,
        protected platform: Platform,
        protected physicalGearValidatorService: OperationValidatorService,
        protected referentialService: ReferentialService
    ) {

        super(dateAdapter, platform, physicalGearValidatorService.getFormGroup());

    }

    ngOnInit() {
        // Combo: physicalGears
        this.physicalGears =
            merge(
                this.form.get('physicalGear').valueChanges.pipe(debounceTime(300)),
                this.onFocusPhysicalGear.pipe(map(any => this.form.get('physicalGear').value))
            )
                .pipe(
                    mergeMap(value => {
                        // Display the selected object
                        if (value && typeof value == "object") {
                            this.form.controls["metier"].enable();
                            return Observable.of([value]);
                        }
                        this.form.controls["metier"].disable();
                        // Skip if no trip (or no physical gears)
                        if (!this.trip || !this.trip.gears || !this.trip.gears.length) return Observable.empty();
                        // Display all trip gears
                        if (!value || typeof value != "string" || value.length < 2) return Observable.of(this.trip.gears || []);

                        const ucValue = value.toUpperCase();
                        return Observable.of((this.trip.gears || [])
                            .filter(g => !!g.gear &&
                                (g.gear.label && g.gear.label.toUpperCase().indexOf(ucValue) != -1)
                                || (g.gear.name && g.gear.name.toUpperCase().indexOf(ucValue) != -1)
                            ));
                    }));

        // Combo: metiers
        this.metiers = merge(
            this.form.get('metier').valueChanges.pipe(debounceTime(300)),
            this.onFocusMetier.pipe(map(any => this.form.get('metier').value))
        )
            .pipe(
                mergeMap(value => {
                    if (typeof value == "object") return Observable.of([value]);
                    const physicalGear = this.form.get('physicalGear').value;
                    return this.referentialService.loadAll(0, 10, undefined, undefined,
                        {
                            levelId: physicalGear && physicalGear.gear && physicalGear.gear.id || null,
                            searchText: value as string
                        },
                        { entityName: 'Metier' });
                }));
    }

    setTrip(trip: Trip) {
        this.trip = trip;

        // Use trip physical gear Object (if possible)
        let physicalGear = this.form.get("physicalGear").value;
        if (physicalGear && physicalGear.id) {
            physicalGear = (this.trip.gears || [physicalGear])
                .find(g => g.id == physicalGear.id)
            if (physicalGear) {
                this.form.controls["physicalGear"].setValue(physicalGear);
            }
        }
    }


    physicalGearToString(physicalGear: PhysicalGear) {
        return physicalGear && physicalGear.id ? ("#" + physicalGear.rankOrder + " - " + referentialToString(physicalGear.gear)) : undefined;
    }

    referentialToString = referentialToString;
}
