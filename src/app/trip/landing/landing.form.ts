import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnInit} from '@angular/core';
import {
  entityToString,
  EntityUtils, isNil,
  isNotNil,
  LocationLevelIds,
  Landing,
  Person,
  personToString,
  Referential,
  ReferentialRef,
  referentialToString, vesselFeaturesToString, VesselFeatures, StatusIds
} from "../services/trip.model";
import {Moment} from 'moment/moment';
import {FormArrayHelper, LocalSettingsService} from '../../core/core.module';
import {DateAdapter} from "@angular/material";
import {BehaviorSubject, Observable, Subject} from 'rxjs';
import {
  debounceTime,
  distinctUntilChanged,
  filter,
  map,
  mergeMap,
  pluck,
  startWith,
  switchMap,
  tap
} from 'rxjs/operators';
import {
  AcquisitionLevelCodes,
  ProgramService,
  ReferentialRefService, VesselModal,
  VesselService
} from '../../referential/referential.module';
import {LandingValidatorService} from "../services/landing.validator";
import {PersonService} from "../../admin/services/person.service";
import {MeasurementValuesForm} from "../measurement/measurement-values.form.class";
import {MeasurementsValidatorService} from "../services/measurement.validator";
import {FormArray, FormBuilder, FormControl} from "@angular/forms";
import {ModalController} from "@ionic/angular";
import {UserProfileLabel} from "../../core/services/model";

@Component({
  selector: 'app-landing-form',
  templateUrl: './landing.form.html',
  styleUrls: ['./landing.form.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class LandingForm extends MeasurementValuesForm<Landing> implements OnInit {

  observersHelper: FormArrayHelper<Person>;
  observerFocusIndex = -1;
  mobile: boolean;

  @Input() required = true;

  @Input() showProgram = true;
  @Input() showVessel = true;
  @Input() showDateTime = true;
  @Input() showLocation = true;
  @Input() showObservers = true;
  @Input() showComment = true;
  @Input() showError = true;

  @Input() showButtons = true;

  @Input() locationLevelIds: number[];

  get empty(): any {
    const value = this.value;
    return EntityUtils.isEmpty(value.location)
      && (!value.dateTime)
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
    protected validatorService: LandingValidatorService,
    protected referentialRefService: ReferentialRefService,
    protected personService: PersonService,
    protected vesselService: VesselService,
    protected settings: LocalSettingsService,
    protected modalCtrl: ModalController,
    protected cd: ChangeDetectorRef
  ) {
    super(dateAdapter, measurementValidatorService, formBuilder, programService, settings, cd, validatorService.getFormGroup());
    this._enable = false;
    this.mobile = this.settings.mobile;

    this.observersHelper = new FormArrayHelper<Person>(
      this.formBuilder,
      this.form,
      'observers',
      (person) => validatorService.getObserverControl(person),
      EntityUtils.equals,
      EntityUtils.isEmpty,
      {
        allowEmptyArray: false
      }
    );

    // Set default acquisition level
    this.acquisitionLevel = AcquisitionLevelCodes.LANDING;

    // Create at least one observer
    this.observersHelper.resize(1);
  }

  ngOnInit() {
    super.ngOnInit();

    // Default values
    this.tabindex = isNotNil(this.tabindex) ? this.tabindex : 1;
    if (isNil(this.locationLevelIds)) {
      this.locationLevelIds = [LocationLevelIds.PORT];
    }

    // Combo: programs
    this.registerAutocompleteField('program', {
      service: this.referentialRefService,
      filter: {
        entityName: 'Program'
      }
    });

    // Combo: vessels
    this.registerAutocompleteField('vesselFeatures', {
      service: this.vesselService,
      attributes: ['exteriorMarking', 'name'].concat(this.settings.getFieldDisplayAttributes('location').map(key => 'basePortLocation.' + key))
    });

    // Propagate program
    this.registerSubscription(
      this.form.get('program').valueChanges
        .pipe(
          debounceTime(250),
          filter(EntityUtils.isNotEmpty),
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

  public setValue(value: Landing) {
    if (!value) return;

    // Make sure to have (at least) one observer
    value.observers = value.observers && value.observers.length ? value.observers : [null];

    // Resize observers array
    this.observersHelper.resize(Math.min(1, value.observers.length));

    // Propagate the program
    if (value.program && value.program.label) {
      this.program = value.program.label;
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

  async addVesselModal(): Promise<any> {
    const modal = await this.modalCtrl.create({ component: VesselModal });
    modal.onDidDismiss().then(res => {
      // if new vessel added, use it
      if (res && res.data instanceof VesselFeatures) {
        console.debug("[landing-form] New vessel added : updating form...", res.data);
        this.form.controls['vesselFeatures'].setValue(res.data);
        this.markForCheck();
      }
      else {
        console.debug("[landing-form] No vessel added (user cancelled)");
      }
    });
    return modal.present();
  }

  /* -- protected method -- */

  entityToString = entityToString;
  referentialToString = referentialToString;
  vesselFeaturesToString = vesselFeaturesToString;


  protected markForCheck() {
    this.cd.markForCheck();
  }
}
