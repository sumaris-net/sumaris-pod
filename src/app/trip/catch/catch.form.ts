import { Component, OnInit, Input } from '@angular/core';
import { PmfmStrategy } from "../services/trip.model";
import { Platform } from "@ionic/angular";
import { Moment } from 'moment/moment';
import { DateAdapter } from "@angular/material";
import { ReferentialService } from "../../referential/referential.module";
import { FormBuilder } from '@angular/forms'
import { AcquisitionLevelCodes } from '../../core/services/model';
import { MeasurementsValidatorService } from '../services/measurement.validator';
import { MeasurementsForm } from '../measurement/measurements.form';
import { Subject, Observable } from 'rxjs';
import { map } from 'rxjs/operators';

import { TranslateService } from '@ngx-translate/core';

@Component({
    selector: 'form-catch',
    templateUrl: './catch.form.html',
    styleUrls: ['./catch.form.scss']
})
export class CatchForm extends MeasurementsForm implements OnInit {

    onDeckPmfms: Observable<PmfmStrategy[]>;
    sortingPmfms: Observable<PmfmStrategy[]>;
    weightPmfms: Observable<PmfmStrategy[]>;

    @Input() showError: boolean = true;

    constructor(
        protected dateAdapter: DateAdapter<Moment>,
        protected platform: Platform,
        protected measurementsValidatorService: MeasurementsValidatorService,
        protected formBuilder: FormBuilder,
        protected referentialService: ReferentialService,
        protected translate: TranslateService
    ) {

        super(dateAdapter, platform, measurementsValidatorService, formBuilder, referentialService, translate);
        this.acquisitionLevel = AcquisitionLevelCodes.CATCH_BATCH;
    }

    ngOnInit() {
        super.ngOnInit();

        const onDeckPmfms = new Subject<PmfmStrategy[]>();
        const sortingPmfms = new Subject<PmfmStrategy[]>();
        const weightPmfms = new Subject<PmfmStrategy[]>();

        this.onDeckPmfms = onDeckPmfms.asObservable();
        this.sortingPmfms = sortingPmfms.asObservable();
        this.weightPmfms = weightPmfms.asObservable();

        this.logDebug("[catch-form] Starting...");
        //console.log("[catch-form] Starting...");

        // pmfm
        this.pmfms.subscribe(pmfms => {
            this.logDebug("[catch-form] Received pmfms:", pmfms);
            this.measurementsValidatorService.updateFormGroup(this.form, pmfms);
            onDeckPmfms.next(pmfms.filter(p => p.label.indexOf('ON_DECK_') === 0));
            sortingPmfms.next(pmfms.filter(p => p.label.indexOf('SORTING_') === 0));
            weightPmfms.next(pmfms.filter(p => p.label.indexOf('_WEIGHT') > 0));
        });
    }
}
