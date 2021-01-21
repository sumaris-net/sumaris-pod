import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnInit} from '@angular/core';
import {Moment} from 'moment/moment';
import {
  EntityUtils,
  FormArrayHelper,
  fromDateISOString,
  isNil,
  isNotNil,
  Person,
  referentialToString
} from '../../core/core.module';
import {DateAdapter} from "@angular/material/core";
import {debounceTime, distinctUntilChanged, filter, map, pluck} from 'rxjs/operators';
import {ObservedLocationValidatorService} from "../services/validator/observed-location.validator";
import {PersonService} from "../../admin/services/person.service";
import {MeasurementValuesForm} from "../measurement/measurement-values.form.class";
import {MeasurementsValidatorService} from "../services/validator/measurement.validator";
import {FormArray, FormBuilder, FormGroup} from "@angular/forms";

import {personToString, UserProfileLabel} from "../../core/services/model/person.model";
import {ReferentialUtils} from "../../core/services/model/referential.model";
import {LocalSettingsService} from "../../core/services/local-settings.service";
import {isNotEmptyArray, isNotNilOrBlank, toBoolean} from "../../shared/functions";
import {ObservedLocation} from "../services/model/observed-location.model";
import {AcquisitionLevelCodes, LocationLevelIds} from "../../referential/services/model/model.enum";
import {ReferentialRefService} from "../../referential/services/referential-ref.service";
import {ProgramService} from "../../referential/services/program.service";
import {StatusIds} from "../../core/services/model/model.enum";
import {start} from "repl";

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
    if (this.autocompleteFields.location) {
      this.autocompleteFields.location.filter = {
        ...this.autocompleteFields.location.filter,
        levelIds: value
      };
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
    return this.form.controls.observers as FormArray;
  }

  get measurementValuesForm(): FormGroup {
    return this.form.controls.measurementValues as FormGroup;
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
    super(dateAdapter, measurementValidatorService, formBuilder, programService, settings, cd,
      validatorService.getFormGroup());
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
        .subscribe(programLabel => this.program = programLabel as string));

    // Combo location
    this.registerAutocompleteField('location', {
      service: this.referentialRefService,
      filter: {
        entityName: 'Location',
        levelIds: this.locationLevelIds
      }
    });

    // Combo: observers
    this.registerAutocompleteField('observer', {
      suggestFn: (value, filter) => {
        const actualEntity = ReferentialUtils.isNotEmpty(value) ? value : null;
        const excludedIds = (this.observersForm.value || [])
          .filter(ReferentialUtils.isNotEmpty)
          .filter(person => !actualEntity || actualEntity !== person)
          .map(person => parseInt(person.id));
        return this.personService.suggest(actualEntity ? '*' : value, {
          ...filter,
          excludedIds
        });
      },
      filter: {
        statusIds: [StatusIds.TEMPORARY, StatusIds.ENABLE],
        userProfiles: ['SUPERVISOR', 'USER']
      },

      // Important, to get the focused control value, in the suggest fn. Otherwise suggestFn will received '*'.
      showAllOnFocus: false,
      attributes: ['lastName', 'firstName', 'department.name'],
      displayWith: personToString
    });

    // Copy startDateTime to endDateTime, when endDate is hidden
    this.registerSubscription(
      this.form.get('startDateTime').valueChanges
        .pipe(
          debounceTime(150),
          filter(v => isNotNil(v) && !this.showEndDateTime),
          map(fromDateISOString)
        )
        .subscribe(startDateTime => {
          this.form.patchValue({endDateTime: startDateTime.add(1, 'millisecond')}, {emitEvent: false})
        }
      )
    );
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

    // Force to show end date
    if (!this.showEndDateTime && isNotNil(value.endDateTime) && isNotNil(value.startDateTime)) {
      const diffInSeconds = fromDateISOString(value.endDateTime)
        .diff(fromDateISOString(value.startDateTime), 'second');
      if (diffInSeconds !== 0) {
        this.showEndDateTime = true;
        this.markForCheck();
      }
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
      FormArrayHelper.getOrCreateArray(this.formBuilder, this.form, 'observers'),
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
