import { Component, OnInit, Input } from '@angular/core';
import { PmfmStrategy, Batch } from "../services/trip.model";
import { Platform } from "@ionic/angular";
import { Moment } from 'moment/moment';
import { DateAdapter } from "@angular/material";
import { ProgramService } from "../../referential/referential.module";
import { FormBuilder } from '@angular/forms'
import { AcquisitionLevelCodes } from '../../core/services/model';
import { MeasurementsValidatorService } from '../services/measurement.validator';
import { MeasurementValuesForm } from '../measurement/measurement-values.form.class';
import { Subject } from 'rxjs';
import { BatchValidatorService } from '../services/batch.validator';

@Component({
    selector: 'form-catch',
    templateUrl: './catch.form.html',
    styleUrls: ['./catch.form.scss']
})
export class CatchForm extends MeasurementValuesForm<Batch> implements OnInit {

    onDeckPmfms = new Subject<PmfmStrategy[]>();
    sortingPmfms = new Subject<PmfmStrategy[]>();
    weightPmfms = new Subject<PmfmStrategy[]>();

    @Input() showError: boolean = true;

    constructor(
        protected dateAdapter: DateAdapter<Moment>,
        protected platform: Platform,
        protected measurementsValidatorService: MeasurementsValidatorService,
        protected formBuilder: FormBuilder,
        protected programService: ProgramService,
        protected validatorService: BatchValidatorService
    ) {

        super(dateAdapter, platform, measurementsValidatorService, formBuilder, programService, validatorService.getFormGroup());
        this._acquisitionLevel = AcquisitionLevelCodes.CATCH_BATCH;
    }

    ngOnInit() {
        super.ngOnInit();

        // pmfm
        this.registerSubscription(
            this.pmfms.subscribe(pmfms => {
                //this.logDebug("[catch-form] Received pmfms:", pmfms);
                this.onDeckPmfms.next(pmfms.filter(p => p.label.indexOf('ON_DECK_') === 0));
                this.sortingPmfms.next(pmfms.filter(p => p.label.indexOf('SORTING_') === 0));
                this.weightPmfms.next(pmfms.filter(p => p.label.indexOf('_WEIGHT') > 0));
            }));

        this.registerSubscription(
            this._onValueChange.subscribe(event => this.data.label = this._acquisitionLevel)
        );
    }
}
