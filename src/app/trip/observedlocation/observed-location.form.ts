import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnInit} from '@angular/core';
import {
  entityToString,
  EntityUtils, isNotNil,
  LocationLevelIds,
  ObservedLocation,
  Person,
  personToString,
  Referential,
  ReferentialRef,
  referentialToString
} from "../services/trip.model";
import {Moment} from 'moment/moment';
import {AcquisitionLevelCodes} from '../../core/core.module';
import {DateAdapter} from "@angular/material";
import {Observable} from 'rxjs';
import {debounceTime, mergeMap, startWith, switchMap, tap} from 'rxjs/operators';
import {ProgramService, ReferentialRefService} from '../../referential/referential.module';
import {ObservedLocationValidatorService} from "../services/observed-location.validator";
import {PersonService} from "../../admin/services/person.service";
import {MeasurementValuesForm} from "../measurement/measurement-values.form.class";
import {MeasurementsValidatorService} from "../services/measurement.validator";
import {FormBuilder} from "@angular/forms";

@Component({
  selector: 'form-observed-location',
  templateUrl: './observed-location.form.html',
  styleUrls: ['./observed-location.form.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ObservedLocationForm extends MeasurementValuesForm<ObservedLocation> implements OnInit {

  programs: Observable<ReferentialRef[]>;
  locations: Observable<ReferentialRef[]>;
  persons: Observable<Person[]>;

  @Input() required = true;
  @Input() showError = true;
  @Input() showEndDateTime = true;
  @Input() showComment = true;
  @Input() showButtons = true;

  get empty(): any {
    const value = this.value;
    return (!value.location || !value.location.id)
      && (!value.startDateTime)
      && (!value.comments || !value.comments.length);
  }

  get valid(): any {
    return this.form && (this.required ? this.form.valid : (this.form.valid || this.empty));
  }

  constructor(
    protected dateAdapter: DateAdapter<Moment>,
    protected measurementValidatorService: MeasurementsValidatorService,
    protected formBuilder: FormBuilder,
    protected programService: ProgramService,
    protected cd: ChangeDetectorRef,
    protected validatorService: ObservedLocationValidatorService,
    protected referentialRefService: ReferentialRefService,
    protected personService: PersonService
  ) {
    super(dateAdapter, measurementValidatorService, formBuilder, programService, cd, validatorService.getFormGroup());

    console.log("Creating OBS LOC form");
    this._enable = false;
    this.acquisitionLevel = AcquisitionLevelCodes.OBSERVED_LOCATION;
  }

  ngOnInit() {
    super.ngOnInit();

    // Combo: programs
    this.programs = this.form.controls['program']
      .valueChanges
      .pipe(
        startWith('*'),
        debounceTime(250),
        switchMap(value => this.referentialRefService.suggest(value, {
          entityName: 'Program'
        })),
        tap(res => {
          if (res && res.length === 1) {
            this.program = res[0].label;
          }
        })
      );

    // Combo: locations
    this.locations = this.form.controls['location']
      .valueChanges
      .pipe(
        debounceTime(250),
        mergeMap(value => this.referentialRefService.suggest(value, {
              entityName: 'Location',
              levelId: LocationLevelIds.PORT
          }))
      );

    // Combo: observers
    this.persons = this.form.controls['recorderPerson']
      .valueChanges
      .pipe(
        debounceTime(250),
        mergeMap(value => {
          if (EntityUtils.isNotEmpty(value)) return Observable.of([value]);
          value = (typeof value === "string" && value !== '*') && value || undefined;
          return this.personService.watchAll(0, !value ? 30 : 10, undefined, undefined,
            {
              searchText: value as string
            }).first().map(({data}) => data);
        }));

    this._onValueChanged.subscribe((value) => {
      if (value && value.program && value.program.label) {
        this.program = value.program.label;
      }
    });
  }

  addObserver() {
    console.log('TODO: add observer');
  }

  enable(opts?: {
    onlySelf?: boolean;
    emitEvent?: boolean;
  }): void {
    console.log('Enabling form');
    super.enable(opts);

    // Leave program disable once data has been saved
    if (isNotNil(this.data.id) && !this.form.controls['program'].disabled) {
      this.form.controls['program'].disable({emitEvent: false});
      this.markForCheck();
    }
  }

  entityToString = entityToString;
  referentialToString = referentialToString;
  personToString = personToString;

  programToString(value: Referential): string {
    return value && value.label || undefined;
  }

  protected markForCheck() {
    this.cd.markForCheck();
  }
}
