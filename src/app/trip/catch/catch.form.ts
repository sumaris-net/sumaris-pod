import { Component, OnInit, Input } from '@angular/core';
import { Operation, PmfmStrategy, Measurement } from "../services/trip.model";
import { Platform } from "@ionic/angular";
import { Moment } from 'moment/moment';
import { DateAdapter } from "@angular/material";
import { ReferentialService } from "../../referential/referential.module";
import { FormBuilder } from '@angular/forms'
import { AcquisitionLevelCodes } from '../../core/services/model';
import { MeasurementsValidatorService } from '../services/measurement.validator';
import { MeasurementsForm } from '../measurement/measurements.form';

import { TranslateService } from '@ngx-translate/core';

@Component({
    selector: 'form-catch',
    templateUrl: './catch.form.html',
    styleUrls: ['./catch.form.scss']
})
export class CatchForm extends MeasurementsForm implements OnInit {

    data: any;
    pmfmMap: { [key: string]: PmfmStrategy[] };
    loading: boolean = true;

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

        // pmfm
        this.pmfms
            .subscribe(pmfms => {

                this.measurementsValidatorService.updateFormGroup(this.form, pmfms);

                this.pmfmMap = {
                    ondeck: pmfms.filter(p => p.label.indexOf('ON_DECK_') === 0),
                    sorting: pmfms.filter(p => p.label.indexOf('SORTING_') === 0),
                    weight: pmfms.filter(p => p.label.indexOf('_WEIGHT') != -1)
                };
                console.log("Result of Pmfm:", this.pmfmMap);
                this.loading = false;
            });
    }
}
