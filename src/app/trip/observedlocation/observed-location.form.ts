import { ChangeDetectionStrategy, Component, Injector, Input, OnInit } from '@angular/core';
import { Moment } from 'moment';
import { debounceTime, distinctUntilChanged, filter, map, tap } from 'rxjs/operators';
import { ObservedLocationValidatorService } from '../services/validator/observed-location.validator';
import { MeasurementValuesForm } from '../measurement/measurement-values.form.class';
import { MeasurementsValidatorService } from '../services/validator/measurement.validator';
import { FormArray, FormBuilder, FormControl, FormGroup } from '@angular/forms';
import {
  FormArrayHelper,
  fromDateISOString,
  isEmptyArray,
  isNil,
  isNotNil,
  LoadResult,
  Person,
  PersonService,
  PersonUtils,
  referentialToString,
  ReferentialUtils,
  StatusIds,
  toBoolean,
  UserProfileLabel,
} from '@sumaris-net/ngx-components';
import { ObservedLocation } from '../services/model/observed-location.model';
import { AcquisitionLevelCodes, LocationLevelIds } from '@app/referential/services/model/model.enum';
import { ReferentialRefService } from '@app/referential/services/referential-ref.service';
import { ProgramRefService } from '@app/referential/services/program-ref.service';
import { ReferentialRefFilter } from '@app/referential/services/filter/referential-ref.filter';
import { environment } from '@environments/environment';

@Component({
  selector: 'app-form-observed-location',
  templateUrl: './observed-location.form.html',
  styleUrls: ['./observed-location.form.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ObservedLocationForm extends MeasurementValuesForm<ObservedLocation> implements OnInit {

  @Input() required = true;
  @Input() showError = true;
  @Input() showEndDateTime = true;
  @Input() showStartTime = true;
  @Input() showEndTime = true;
  @Input() showComment = true;
  @Input() showButtons = true;
  @Input() filterStartDateDay: number;
  @Input() forceDurationDays: number;

  _showObservers: boolean;
  observersHelper: FormArrayHelper<Person>;
  observerFocusIndex = -1;
  locationFilter: Partial<ReferentialRefFilter> = {
    entityName: 'Location'
  };
  mobile: boolean;
  referentialToString = referentialToString;


  @Input() set locationLevelIds(value: number[]) {
    if (this.locationFilter.levelIds !== value) {

      console.debug('[observed-location-form] Location level ids:', value);
      this.locationFilter = {
        ...this.locationFilter,
        levelIds: value
      };
      this.markForCheck();
    }
  }

  get locationLevelIds(): number[] {
    return this.locationFilter && this.locationFilter.levelIds;
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

  get programControl(): FormControl {
    return this.form.get('program') as FormControl;
  }

  constructor(
    injector: Injector,
    protected measurementValidatorService: MeasurementsValidatorService,
    protected formBuilder: FormBuilder,
    protected programRefService: ProgramRefService,
    protected validatorService: ObservedLocationValidatorService,
    protected referentialRefService: ReferentialRefService,
    protected personService: PersonService
  ) {
    super(injector, measurementValidatorService, formBuilder, programRefService,
      validatorService.getFormGroup());
    this._enable = false;
    this.mobile = this.settings.mobile;

    // Set default acquisition level
    this.acquisitionLevel = AcquisitionLevelCodes.OBSERVED_LOCATION;

    // FOR DEV ONLY ----
    this.debug = !environment.production;
  }

  ngOnInit() {
    super.ngOnInit();

    // Default values
    this.showObservers = toBoolean(this.showObservers, true); // Will init the observers helper
    this.tabindex = isNotNil(this.tabindex) ? this.tabindex : 1;
    if (isEmptyArray(this.locationLevelIds)) this.locationLevelIds = [LocationLevelIds.PORT];

    // Combo: programs
    const programAttributes = this.settings.getFieldDisplayAttributes('program');
    this.registerAutocompleteField('program', {
      service: this.referentialRefService,
      attributes: programAttributes,
      // Increase default column size, for 'label'
      columnSizes: programAttributes.map(a => a === 'label' ? 4 : undefined/*auto*/),
      filter: <ReferentialRefFilter>{
        entityName: 'Program'
      },
      mobile: this.mobile
    });

    // Combo location
    this.registerAutocompleteField('location', {
      service: this.referentialRefService,
      filter: this.locationFilter
    });

    // Combo: observers
    this.registerAutocompleteField('person', {
      // Important, to get the current (focused) control value, in suggestObservers() function (otherwise it will received '*').
      showAllOnFocus: false,
      suggestFn: (value, filter) => this.suggestObservers(value, filter),
      // Default filter. An excludedIds will be add dynamically
      filter: {
        statusIds: [StatusIds.TEMPORARY, StatusIds.ENABLE],
        userProfiles: <UserProfileLabel[]>['SUPERVISOR', 'USER']
      },
      attributes: ['lastName', 'firstName', 'department.name'],
      displayWith: PersonUtils.personToString
    });

    // Propagate program
    this.registerSubscription(
      this.form.get('program').valueChanges
        .pipe(
          debounceTime(250),
          map(value => (value && typeof value === 'string') ? value : (value && value.label || undefined)),
          distinctUntilChanged()
        )
        .subscribe(programLabel => this.programLabel = programLabel));

    // Copy startDateTime to endDateTime, when endDate is hidden
    this.registerSubscription(
      this.form.get('startDateTime').valueChanges
        .pipe(
          debounceTime(150),
          filter(v => isNotNil(v)),
          map(fromDateISOString),
          // remove time if time control not shown
          tap((startDateTime: Moment) => {
            if (!this.showStartTime) {
              this.form.patchValue({
                startDateTime: startDateTime.startOf('day'),
              }, { emitEvent: false });
            }
          }),
          // update end date time
          tap((startDateTime: Moment) => {
            if (!this.showEndDateTime) {
              // copy start date time + 1ms
              this.form.patchValue({
                endDateTime: startDateTime.clone().add(1, 'millisecond'),
              }, { emitEvent: false });
            } else if (this.forceDurationDays > 0) {
              // add duration days
              this.form.patchValue({
                endDateTime: startDateTime.clone().add(this.forceDurationDays - 1, 'day'),
              }, { emitEvent: false });
            }
          })
        )
        .subscribe()
    );

    this.registerSubscription(
      this.form.get('endDateTime').valueChanges
        .pipe(
          debounceTime(150),
          filter(v => isNotNil(v) && this.showEndDateTime),
          map(fromDateISOString),
          // remove time if time control not shown
          tap((endDateTime: Moment) => {
            if (!this.showEndTime) {
              this.form.patchValue({
                endDateTime: endDateTime.startOf('day'),
              }, { emitEvent: false });
            }
          })
        )
        .subscribe()
    );
  }

  protected onApplyingEntity(data: ObservedLocation, opts?: { [p: string]: any }) {
    if (!data) return;

    super.onApplyingEntity(data, opts);

    // Make sure to have (at least) one observer
    data.observers = data.observers && data.observers.length ? data.observers : [null];

    // Resize observers array
    if (this._showObservers) {
      this.observersHelper.resize(Math.max(1, data.observers.length));
    } else {
      this.observersHelper.removeAllEmpty();
    }

    // Force to show end date
    if (!this.showEndDateTime && isNotNil(data.endDateTime) && isNotNil(data.startDateTime)) {
      const diffInSeconds = fromDateISOString(data.endDateTime)
        .diff(fromDateISOString(data.startDateTime), 'second');
      if (diffInSeconds !== 0) {
        this.showEndDateTime = true;
        this.markForCheck();
      }
    }

    // Update form group
    this.validatorService.updateFormGroup(this.form, {startDateDay: this.filterStartDateDay});

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
    if (!this.isNewData && !this.programControl.disabled) {
      this.programControl.disable({emitEvent: false});
      this.markForCheck();
    }
  }


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

  protected suggestObservers(value: any, filter?: any): Promise<LoadResult<Person>> {
    const currentControlValue = ReferentialUtils.isNotEmpty(value) ? value : null;
    const newValue = currentControlValue ? '*' : value;

    // Excluded existing observers, BUT keep the current control value
    const excludedIds = (this.observersForm.value || [])
    .filter(ReferentialUtils.isNotEmpty)
    .filter(person => !currentControlValue || currentControlValue !== person)
    .map(person => parseInt(person.id));

    return this.personService.suggest(newValue, {
      ...filter,
      excludedIds
    });
  }

  protected markForCheck() {
    this.cd.markForCheck();
  }
}
