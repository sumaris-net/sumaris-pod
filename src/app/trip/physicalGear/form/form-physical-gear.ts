import { Component, OnInit, Input, EventEmitter, Output } from '@angular/core';
import { PhysicalGearValidatorService } from "../validator/validators";
import { FormGroup } from "@angular/forms";
import { PhysicalGear, Referential, GearLevelIds } from "../../services/model";
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
    selector: 'form-physical-gear',
    templateUrl: './form-physical-gear.html',
    styleUrls: ['./form-physical-gear.scss']
})
export class PhysicalGearForm extends AppForm<PhysicalGear> implements OnInit {

    gears: Observable<Referential[]>;

    @Input() showComment: boolean = true;

    constructor(
        protected dateAdapter: DateAdapter<Moment>,
        protected platform: Platform,
        protected physicalGearValidatorService: PhysicalGearValidatorService,
        protected referentialService: ReferentialService
    ) {

        super(dateAdapter, platform, physicalGearValidatorService.getFormGroup());
        console.log("Creating gear form");
    }

    ngOnInit() {
        // Combo: gears
        this.gears = this.form.controls['gear']
            .valueChanges
            .pipe(
                mergeMap(value => {
                    if (!value) return Observable.empty();
                    if (typeof value == "object") return Observable.of([value]);
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
}
