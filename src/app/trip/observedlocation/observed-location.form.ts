import {Component, Input, OnInit} from '@angular/core';
import {
  entityToString,
  EntityUtils,
  LocationLevelIds,
  Person,
  personToString,
  Referential,
  ReferentialRef,
  referentialToString
} from "../services/trip.model";
import {Moment} from 'moment/moment';
import {AppForm} from '../../core/core.module';
import {DateAdapter} from "@angular/material";
import {Observable} from 'rxjs';
import {debounceTime, mergeMap, startWith, switchMap} from 'rxjs/operators';
import {ReferentialRefService} from '../../referential/referential.module';
import {ObservedLocationValidatorService} from "../services/observed-location.validator";
import {PersonService} from "../../admin/services/person.service";
import {ObservedLocation} from "../services/observed-location.model";

@Component({
  selector: 'form-observed-location',
  templateUrl: './observed-location.form.html',
  styleUrls: ['./observed-location.form.scss']
})
export class ObservedLocationForm extends AppForm<ObservedLocation> implements OnInit {

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
    protected validatorService: ObservedLocationValidatorService,
    protected referentialRefService: ReferentialRefService,
    protected personService: PersonService
  ) {
    super(dateAdapter, validatorService.getFormGroup());
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
        }))
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
  }

  addObserver() {
    console.log('TODO: add observer');
  }

  entityToString = entityToString;
  referentialToString = referentialToString;
  personToString = personToString;

  programToString(value: Referential): string {
    return value && value.label || undefined;
  }
}
