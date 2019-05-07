import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnInit} from '@angular/core';
import {Batch, isNotNil, PmfmStrategy} from "../services/trip.model";
import {Platform} from "@ionic/angular";
import {Moment} from 'moment/moment';
import {DateAdapter} from "@angular/material";
import {ProgramService} from "../../referential/referential.module";
import {FormBuilder} from '@angular/forms'
import {MeasurementsValidatorService} from '../services/measurement.validator';
import {MeasurementValuesForm} from '../measurement/measurement-values.form.class';
import {Subject} from 'rxjs';
import {BatchValidatorService} from '../services/batch.validator';
import {filter} from "rxjs/operators";

@Component({
  selector: 'form-catch-batch',
  templateUrl: './catch.form.html',
  styleUrls: ['./catch.form.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class CatchBatchForm extends MeasurementValuesForm<Batch> implements OnInit {

  onDeckPmfms = new Subject<PmfmStrategy[]>();
  sortingPmfms = new Subject<PmfmStrategy[]>();
  weightPmfms = new Subject<PmfmStrategy[]>();

  @Input() showError = true;

  constructor(
    protected dateAdapter: DateAdapter<Moment>,
    protected measurementsValidatorService: MeasurementsValidatorService,
    protected formBuilder: FormBuilder,
    protected programService: ProgramService,
    protected validatorService: BatchValidatorService,
    protected cd: ChangeDetectorRef
  ) {

    super(dateAdapter, measurementsValidatorService, formBuilder, programService, cd, validatorService.getFormGroup());
  }

  ngOnInit() {
    super.ngOnInit();

    // pmfm
    this.registerSubscription(
      this.pmfms
        .pipe(filter(isNotNil))
        .subscribe(pmfms => {
        this.onDeckPmfms.next(pmfms.filter(p => p.label.indexOf('ON_DECK_') === 0));
        this.sortingPmfms.next(pmfms.filter(p => p.label.indexOf('SORTING_') === 0));
        this.weightPmfms.next(pmfms.filter(p => p.label.indexOf('_WEIGHT') > 0));
      }));

    // Make sure to set the label
    this.registerSubscription(
      this._onValueChanged.subscribe((_) => this.data.label = this._acquisitionLevel)
    );
  }
}
