import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnInit} from '@angular/core';
import {Moment} from 'moment/moment';
import {
  EntityUtils,
  FormArrayHelper,
  isNil,
  isNotNil,
  Person,
  personToString,
  referentialToString,
  StatusIds
} from '../../core/core.module';
import {DateAdapter} from "@angular/material/core";
import {debounceTime, distinctUntilChanged, filter, pluck} from 'rxjs/operators';
import {
  AcquisitionLevelCodes,
  LocationLevelIds,
  ProgramService,
  ReferentialRefService
} from '../../referential/referential.module';
import {ObservedLocationValidatorService} from "../services/observed-location.validator";
import {PersonService} from "../../admin/services/person.service";
import {MeasurementValuesForm} from "../measurement/measurement-values.form.class";
import {MeasurementsValidatorService} from "../services/measurement.validator";
import {FormArray, FormBuilder} from "@angular/forms";
import {ReferentialUtils, UserProfileLabel} from "../../core/services/model";
import {LocalSettingsService} from "../../core/services/local-settings.service";
import {toBoolean} from "../../shared/functions";
import {ObservedLocation} from "../services/model/observed-location.model";

@Component({
  selector: 'form-observed-location',
  templateUrl: './observed-location.form.html',
  styleUrls: ['./observed-location.form.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ObservedLocationForm extends MeasurementValuesForm<ObservedLocation> implements OnInit {

  _showObservers: boolean;
  _locationLevelIds: number[];
  observersHelper: FormArrayHelper<Person>;
  observerFocusIndex = -1;
  mobile: boolean;

  @Input() required = true;
  @Input() showError = true;
  @Input() showEndDateTime = true;
  @Input() showComment = true;
  @Input() showButtons = true;

  @Input() set locationLevelIds(value: number[]) {
    this._locationLevelIds = value;
    // Update location complete field
    if (this.autocompleteFields['location']) {
      this.autocompleteFields['location'].filter.levelIds = this.locationLevelIds;
    }
  }

  get locationLevelIds(): number[] {
    return this._locationLevelIds;
  }

  @Input()
  set showObservers(value: boolean) {
    if (this._showObservers !== value) {
      this._showObservers = value;
      this.initObserversHelper();
      this.markForCheck();
    }
  }

  get showObservers(): boolean {
    return this._showObservers;
  }

  get empty(): any {
    const value = this.value;
    return (!value.location || !value.location.id)
      && (!value.startDateTime)
      && (!value.comments || !value.comments.length);
  }

  get valid(): any {
    return this.form && (this.required ? this.form.valid : (this.form.valid || this.empty));
  }

  get observersForm(): FormArray {
    return this.form.get('observers') as FormArray;
  }

  constructor(
    protected dateAdapter: DateAdapter<Moment>,
    protected measurementValidatorService: MeasurementsValidatorService,
    protected formBuilder: FormBuilder,
    protected programService: ProgramService,
    protected validatorService: ObservedLocationValidatorService,
    protected referentialRefService: ReferentialRefService,
    protected personService: PersonService,
    protected settings: LocalSettingsService,
    protected cd: ChangeDetectorRef
  ) {
    super(dateAdapter, measurementValidatorService, formBuilder, programService, settings, cd, validatorService.getFormGroup());
    this._enable = false;
    this.mobile = this.settings.mobile;

    // Set default acquisition level
    this.acquisitionLevel = AcquisitionLevelCodes.OBSERVED_LOCATION;

    // FOR DEV ONLY ----
    //this.debug = !environment.production;
  }

  ngOnInit() {
    super.ngOnInit();

    // Default values
    this.showObservers = toBoolean(this.showObservers, true); // Will init the observers helper
    this.tabindex = isNotNil(this.tabindex) ? this.tabindex : 1;
    if (isNil(this.locationLevelIds)) {
      this.locationLevelIds = [LocationLevelIds.PORT];
    }
    console.debug("[observed-location-form] Location level ids:", this.locationLevelIds);

    // Combo: programs
    this.registerAutocompleteField('program', {
      service: this.referentialRefService,
      filter: {
        entityName: 'Program'
      }
    });

    // Propagate program
    this.registerSubscription(
      this.form.get('program').valueChanges
        .pipe(
          debounceTime(250),
          filter(ReferentialUtils.isNotEmpty),
          pluck('label'),
          distinctUntilChanged()
        )
        .subscribe(programLabel => this.program = programLabel));

    // Combo location
    this.registerAutocompleteField('location', {
      service: this.referentialRefService,
      filter: {
        entityName: 'Location',
        levelIds: this.locationLevelIds
      }
    });

    // Combo: observers
    const profileLabels: UserProfileLabel[] = ['SUPERVISOR', 'USER', 'GUEST'];
    this.registerAutocompleteField('person', {
      service: this.personService,
      filter: {
        statusIds: [StatusIds.TEMPORARY, StatusIds.ENABLE],
        userProfiles: profileLabels
      },
      attributes: ['lastName', 'firstName', 'department.name'],
      displayWith: personToString
    });
  }

  setValue(value: ObservedLocation) {
    if (!value) return;

    // Make sure to have (at least) one observer
    value.observers = value.observers && value.observers.length ? value.observers : [null];

    // Resize observers array
    if (this._showObservers) {
      this.observersHelper.resize(Math.max(1, value.observers.length));
    }
    else {
      this.observersHelper.removeAllEmpty();
    }

    // Propagate the program
    if (value.program && value.program.label) {
      this.program = value.program.label;
    }
    // New data: copy the program into json value
    else if (isNil(value.id)){
      value.program = this.form.get('program').value;
    }

    // Send value for form
    super.setValue(value);
  }

  addObserver() {
    this.observersHelper.add();
    if (!this.mobile) {
      this.observerFocusIndex = this.observersHelper.size() - 1;
    }
  }

  enable(opts?: {
    onlySelf?: boolean;
    emitEvent?: boolean;
  }): void {
    super.enable(opts);

    // Leave program disable once data has been saved
    if (isNotNil(this.data.id) && !this.form.controls['program'].disabled) {
      this.form.controls['program'].disable({emitEvent: false});
      this.markForCheck();
    }
  }

  referentialToString = referentialToString;

  /* -- protected method -- */

  protected initObserversHelper() {
    if (isNil(this._showObservers)) return; // skip if not loading yet

    this.observersHelper = new FormArrayHelper<Person>(
      this.formBuilder,
      this.form,
      'observers',
      (person) => this.validatorService.getObserverControl(person),
      ReferentialUtils.equals,
      ReferentialUtils.isEmpty,
      {
        allowEmptyArray: !this._showObservers
      }
    );

    if (this._showObservers) {
      // Create at least one observer
      if (this.observersHelper.size() === 0) {
        this.observersHelper.resize(1);
      }
    }
    else if (this.observersHelper.size() > 0) {
      this.observersHelper.resize(0);
    }
  }

  protected markForCheck() {
    this.cd.markForCheck();
  }
}
