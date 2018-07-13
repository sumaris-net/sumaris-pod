import { Component, OnInit, Input, EventEmitter, Output } from '@angular/core';
import { OperationValidatorService } from "../validator/validators";
import { FormGroup } from "@angular/forms";
import { Operation, Referential, GearLevelIds, TaxonGroupIds, Trip, PhysicalGear } from "../../services/model";
import { ModalController, Platform } from "@ionic/angular";
import { Moment } from 'moment/moment';
import { DateAdapter } from "@angular/material";
import { Observable } from 'rxjs-compat';
import { mergeMap, startWith } from 'rxjs/operators';
import { merge } from "rxjs/observable/merge";
import { AppForm } from '../../../core/core.module';
import { VesselModal, ReferentialService, VesselService } from "../../../referential/referential.module";
import { referentialToString } from '../../../referential/services/model';

@Component({
    selector: 'form-operation',
    templateUrl: './form-operation.html',
    styleUrls: ['./form-operation.scss']
})
export class OperationForm extends AppForm<Operation> implements OnInit {

    trip: Trip;
    metiers: Observable<Referential[]>;
    physicalGears: Observable<PhysicalGear[]>;

    @Input() showComment: boolean = true;

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
        this.physicalGears = this.form.controls['physicalGear']
            .valueChanges
            .pipe(
                mergeMap(value => {
                    if (!value) return Observable.empty();
                    if (typeof value == "object") return Observable.of([value]);
                    // Skip if too short value
                    if (typeof value != "string" || value.length < 2) return Observable.of([]);
                    // Skip if no trip (or no physical gears)
                    console.log("Searching on gear ", this.trip);
                    if (!this.trip || !this.trip.gears || !this.trip.gears.length) return Observable.of([]);
                    const ucValue = value.toUpperCase();
                    return Observable.of((this.trip.gears || [])
                        .filter(g => !!g.gear &&
                            (g.gear.label && g.gear.label.toUpperCase().indexOf(ucValue) != -1)
                            || (g.gear.name && g.gear.name.toUpperCase().indexOf(ucValue) != -1)
                        )/*
                        .map(g => {
                            const gear = g.gear.clone();
                            ref.id = g.id;
                            return ref;
                        })*/);
                }));

        // Combo: metiers
        this.metiers = this.form.controls['metier']
            .valueChanges
            .pipe(
                mergeMap(value => {
                    if (!value) return Observable.empty();
                    if (typeof value == "object") return Observable.of([value]);
                    if (typeof value != "string" || value.length < 2) return Observable.of([]);
                    const physicalGear = this.form.controls['physicalGear'].value;
                    console.log("reduce metier by physicalGear gear: " + physicalGear && physicalGear.gear && physicalGear.gear.id);
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
    }


    physicalGearToString(physicalGear: PhysicalGear) {
        return physicalGear && referentialToString(physicalGear.gear) || "";
    }

    referentialToString = referentialToString;
}
