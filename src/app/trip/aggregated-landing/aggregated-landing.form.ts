import {ChangeDetectionStrategy, ChangeDetectorRef, Component, EventEmitter, Input, OnInit} from '@angular/core';
import {AppForm, environment, FormArrayHelper, isNil, isNotNil, referentialToString} from "../../core/core.module";
import {DateAdapter} from "@angular/material/core";
import {Moment} from "moment";
import {FormArray, FormBuilder, Validators} from "@angular/forms";
import {ReferentialRefService} from "../../referential/services/referential-ref.service";
import {ModalController} from "@ionic/angular";
import {LocalSettingsService} from "../../core/services/local-settings.service";
import {NetworkService} from "../../core/services/network.service";
import {AggregatedLandingValidatorService} from "../services/validator/aggregated-landing.validator";
import {BehaviorSubject, combineLatest, Observable} from "rxjs";
import {filterNotNil, firstNotNilPromise} from "../../shared/observables";
import {distinctUntilChanged, filter} from "rxjs/operators";
import * as moment from "moment";
import {ObservedLocation} from "../services/model/observed-location.model";
import {AggregatedLandingService} from "../services/aggregated-landing.service";
import {AcquisitionLevelCodes} from "../../referential/services/model/model.enum";
import {AggregatedLanding, VesselActivity} from "../services/model/aggregated-landing.model";
import {SharedFormArrayValidators, SharedValidators} from "../../shared/validator/validators";
import {DisplayFn} from "../../shared/form/field.model";
import {DateFormatPipe} from "../../shared/pipes/date-format.pipe";
import {VesselActivityValidatorService} from "../services/validator/vessel-activity.validator";
import {VesselActivityForm} from "./vessel-activity.form";
import {MeasurementUtils, MeasurementValuesUtils} from "../services/model/measurement.model";
import {getMaxRankOrder} from "../../data/services/model/model.utils";

export class AggregatedLandingFormOption {
  dates: Observable<Moment[]> | Moment[];
  initialDate: Moment | undefined;
  program: string;
  acquisitionLevel: string;
}

@Component({
  selector: 'app-aggregated-landings-form',
  templateUrl: './aggregated-landing.form.html',
  styleUrls: ['./aggregated-landing.form.scss']
})
export class AggregatedLandingForm extends AppForm<AggregatedLanding> implements OnInit {

  private _options: AggregatedLandingFormOption;
  private _activeDate: Moment;
  @Input() showError = true;

  private $data = new BehaviorSubject<AggregatedLanding>(undefined) ;
  get data(): AggregatedLanding {
    // Save active form before return data
    this.saveActivitiesAt(this._activeDate);
    return this.$data.getValue();
  }

  set data(data) {
    this.$data.next(data);
  }

  get value(): any {
    throw new Error('The aggregated landing form has no form value accessible from outside');
  }

  set value(value: any) {
    throw new Error('The aggregated landing form has no form value accessible from outside');
  }

  activitiesHelper: FormArrayHelper<VesselActivity>;
  activityFocusIndex = -1;
  get activitiesForm(): FormArray {
    return this.form.controls.activities as FormArray;
  }
  activities: VesselActivity[];

  @Input() set options(option: AggregatedLandingFormOption) {
    this._options = option;
  }

  get options(): AggregatedLandingFormOption {
    return this._options;
  }

  mobile: boolean;
  $loadingControls = new BehaviorSubject<boolean>(false);
  controlsLoaded = false;
  onRefresh = new EventEmitter<any>();
  program: string;
  acquisitionLevel: string;
  dates: Observable<Moment[]> | Moment[];

  constructor(
    protected dateAdapter: DateAdapter<Moment>,
    protected dateFormatPipe: DateFormatPipe,
    protected formBuilder: FormBuilder,
    protected dataService: AggregatedLandingService,
    protected vesselActivityValidatorService: VesselActivityValidatorService,
    protected referentialRefService: ReferentialRefService,
    protected modalCtrl: ModalController,
    protected settings: LocalSettingsService,
    public network: NetworkService,
    protected cd: ChangeDetectorRef
  ) {
    super(dateAdapter, null, settings);
    this.mobile = this.settings.mobile;

    this.acquisitionLevel = AcquisitionLevelCodes.LANDING; // default

    this.debug = !environment.production;
  }

  ngOnInit() {

    if (isNil(this._options)) {
      console.warn('[aggregated-landing-form] No option found, the form will be unusable');
    }

    this.dates = this._options && this._options.dates;
    this.program = this._options && this._options.program;
    this.acquisitionLevel = this._options && this._options.acquisitionLevel;

    const form = this.formBuilder.group({
      'date': [this._options && this._options.initialDate || undefined, Validators.compose([Validators.required, SharedValidators.validDate])],
      'activities': this.formBuilder.array([])
    });
    this.setForm(form);

    // this.form.valueChanges.subscribe(value => console.debug('aggregated-landing CHILD FORM VALUE', value));

    this.initActivitiesHelper();

    this.registerSubscription(
      combineLatest([
        this.form.get('date').valueChanges.pipe(distinctUntilChanged()),
        filterNotNil(this.$data)
      ])
      .subscribe(date => this.showAtDate(this.form.value.date))
    );

    super.ngOnInit();

  }

  private showAtDate(date: Moment) {
    if (!date)
      throw new Error('[aggregated-landing-form] No date provided');

    console.debug(`[aggregated-landing-form] Show vessel activity at ${date}`);

    if (this._activeDate && !date.isSame(this._activeDate)) {
      // Save activities into data
      this.saveActivitiesAt(this._activeDate);
    }

    // Load activities for this date
    this._activeDate = date;
    this.activities = this.$data.getValue().vesselActivities.filter(value => value.date.isSame(date)).slice() || [null];

    // remove all previous forms
    this.activitiesForm.clear();

    // Add each activity with helper.add()
    for (const activity of this.activities) {
      this.activitiesHelper.add(activity);
    }

    this.enable();

    // this.activitiesHelper.resize(activities.length);
    // this.form.controls.activities.setValue(activities);
  }

  addActivity() {
    if (this.debug) console.debug('[aggregated-landing-form] addActivity');
    this.activitiesHelper.add(this.newActivity());
    if (!this.mobile) {
      this.activityFocusIndex = this.activitiesHelper.size() - 1;
    }
  }

  private newActivity(): VesselActivity {
    const maxRankOrder = getMaxRankOrder(this.activities);
    const activity = new VesselActivity();
    activity.rankOrder = maxRankOrder + 1;
    activity.date = this.form.value.date;
    return activity;
  }

  removeActivity(index: number) {
    // TODO check data before remove
    this.activitiesHelper.removeAt(index);
  }

  async ready(): Promise<void> {
    // Wait pmfms load, and controls load
    if (this.$loadingControls.getValue() === true && this.controlsLoaded === false) {
      if (this.debug) console.debug(`[aggregated-landings-form] waiting form to be ready...`);
      await firstNotNilPromise(this.$loadingControls
        .pipe(
          filter((loadingControls) => loadingControls === false && this.controlsLoaded === true)
        ));
    }
  }


  protected markForCheck() {
    this.cd.markForCheck();
  }

  displayDate(): DisplayFn {
    return (obj: any) => this.dateFormatPipe.transform(obj, {pattern: 'dddd L'}).toString();
  }

  compareDate() {
    return (d1: Moment, d2: Moment) => d1 && d2 && d1.isSame(d2) || false;
  }

  private initActivitiesHelper() {
    this.activitiesHelper = new FormArrayHelper<VesselActivity>(
      FormArrayHelper.getOrCreateArray(this.formBuilder, this.form, 'activities'),
      (activity) => this.vesselActivityValidatorService.getFormGroup(activity),
      (v1, v2) => v1.rankOrder === v2.rankOrder,
      value => VesselActivity.isEmpty(value),
      {
        allowEmptyArray: true
      }
    )
  }

  private saveActivitiesAt(date: Moment) {
    if (isNil(date)) {
      console.warn('Try to save activities at undefined date');
      return;
    }
    if (this.debug) console.debug(`[aggregated-landing-form] save activities at ${date}`)
    const newActivities = this.$data.getValue().vesselActivities.filter(value => !value.date.isSame(date)).slice() || [];
    const activities = this.activitiesForm.value.map(v => VesselActivity.fromObject(v));
    newActivities.push(...activities);
    this.$data.getValue().vesselActivities = newActivities;
  }
}
