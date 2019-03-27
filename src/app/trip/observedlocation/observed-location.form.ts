import {Component, Input, OnInit} from '@angular/core';
import {SaleValidatorService} from "../services/sale.validator";
import {
  entityToString,
  EntityUtils,
  LocationLevelIds, Person, personToString, Referential,
  ReferentialRef,
  referentialToString,
  Sale
} from "../services/trip.model";
import {Platform} from '@ionic/angular';
import {Moment} from 'moment/moment';
import {AppForm} from '../../core/core.module';
import {DateAdapter} from "@angular/material";
import {Observable} from 'rxjs';
import {debounceTime, mergeMap} from 'rxjs/operators';
import {ReferentialRefService} from '../../referential/referential.module';
import {ObservedLocationService} from "../services/observed-location.service";
import {ObservedLocationValidatorService} from "../services/observed-location.validator";
import {PersonService} from "../../admin/services/person.service";

@Component({
  selector: 'form-observed-location',
  templateUrl: './observed-location.form.html',
  styleUrls: ['./observed-location.form.scss']
})
export class ObservedLocationForm extends AppForm<Sale> implements OnInit {

  programs: Observable<ReferentialRef[]>;
  locations: Observable<ReferentialRef[]>;
  persons: Observable<Person[]>;

  @Input() required: boolean = true;
  @Input() showError: boolean = true;
  @Input() showEndDateTime: boolean = true;
  @Input() showComment: boolean = true;
  @Input() showButtons: boolean = true;

  get empty(): any {
    let value = this.value;
    return (!value.location || !value.location.id)
      && (!value.dateTime)
      && (!value.comments || !value.comments.length);
  }

  get valid(): any {
    return this.form && (this.required ? this.form.valid : (this.form.valid || this.empty));
  }

  constructor(
    protected dateAdapter: DateAdapter<Moment>,
    protected platform: Platform,
    protected validatorService: ObservedLocationValidatorService,
    protected referentialRefService: ReferentialRefService,
    protected personService: PersonService
  ) {
    super(dateAdapter, platform, validatorService.getFormGroup());
  }

  ngOnInit() {
    super.ngOnInit();

    // Combo: programs
    this.programs = this.form.controls['program']
      .valueChanges
      .startWith('')
      .pipe(
        debounceTime(250),
        mergeMap(value => {
          if (EntityUtils.isNotEmpty(value)) return Observable.of([value]);
          value = (typeof value === "string" && value !== "*") && value || undefined;
          return this.referentialRefService.loadAll(0, !value ? 50 : 10, undefined, undefined,
            {
              entityName: 'Program',
              searchText: value as string
            }).first().map(({data}) => data);
        }));

    // Combo: locations
    this.locations = this.form.controls['location']
      .valueChanges
      .pipe(
        debounceTime(250),
        mergeMap(value => {
          if (EntityUtils.isNotEmpty(value)) return Observable.of([value]);
          value = (typeof value === "string" && value !== '*') && value || undefined;
          return this.referentialRefService.loadAll(0, !value ? 30 : 10, undefined, undefined,
            {
              entityName: 'Location',
              levelId: LocationLevelIds.PORT,
              searchText: value as string
            }).first().map(({data}) => data);
        }));

    // Combo: observers
    this.persons = this.form.controls['recorderPerson']
      .valueChanges
      .pipe(
        debounceTime(250),
        mergeMap(value => {
          if (EntityUtils.isNotEmpty(value)) return Observable.of([value]);
          value = (typeof value === "string" && value !== '*') && value || undefined;
          return this.personService.loadAll(0, !value ? 30 : 10, undefined, undefined,
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
