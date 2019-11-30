import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnInit} from '@angular/core';
import {TripValidatorService} from "../services/trip.validator";
import {
  EntityUtils,
  isNil,
  LocationLevelIds,
  Person,
  personToString,
  StatusIds,
  Trip,
  VesselSnapshot,
} from "../services/trip.model";
import {ModalController} from "@ionic/angular";
import {Moment} from 'moment/moment';
import {DateAdapter} from "@angular/material";
import {AppForm, FormArrayHelper} from '../../core/core.module';
import {ReferentialRefService, referentialToString, VesselModal} from "../../referential/referential.module";
import {UsageMode, UserProfileLabel} from "../../core/services/model";
import {LocalSettingsService} from "../../core/services/local-settings.service";
import {VesselSnapshotService} from "../../referential/services/vessel-snapshot.service";
import {FormArray, FormBuilder} from "@angular/forms";
import {PersonService} from "../../admin/services/person.service";
import {toBoolean} from "../../shared/functions";

@Component({
  selector: 'form-trip',
  templateUrl: './trip.form.html',
  styleUrls: ['./trip.form.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class TripForm extends AppForm<Trip> implements OnInit {

  private _showObservers: boolean;

  observersHelper: FormArrayHelper<Person>;
  observerFocusIndex = -1;
  mobile: boolean;

  @Input() showComment = true;
  @Input() showError = true;
  @Input() usageMode: UsageMode;

  @Input() set showObservers(value: boolean) {
    if (this._showObservers !== value) {
      this._showObservers = value;
      this.initObserversHelper();
      this.markForCheck();
    }
  }

  get showObservers(): boolean {
    return this._showObservers;
  }

  get value(): any {
    const json = this.form.value;

    // Add program, because if control disabled the value is missing
    json.program = this.form.get('program').value;

    return json;
  }

  set value(json: any) {
    this.setValue(json);
  }

  get isOnFieldMode(): boolean {
    return this.usageMode ? this.usageMode === 'FIELD' : this.settings.isUsageMode('FIELD');
  }

  get observersForm(): FormArray {
    return this.form.controls.observers as FormArray;
  }

  constructor(
    protected dateAdapter: DateAdapter<Moment>,
    protected formBuilder: FormBuilder,
    protected validatorService: TripValidatorService,
    protected vesselSnapshotService: VesselSnapshotService,
    protected referentialRefService: ReferentialRefService,
    protected personService: PersonService,
    protected modalCtrl: ModalController,
    protected settings: LocalSettingsService,
    protected cd: ChangeDetectorRef
  ) {

    super(dateAdapter, validatorService.getFormGroup(), settings);
    this.mobile = this.settings.mobile;
  }

  ngOnInit() {
    super.ngOnInit();

    // Default values
    this.showObservers = toBoolean(this.showObservers, true); // Will init the observers helper
    this.usageMode = this.usageMode || this.settings.usageMode;

    // Combo: programs
    this.registerAutocompleteField('program', {
      service: this.referentialRefService,
      filter: {
        entityName: 'Program'
      }
    });

    // Combo: vessels
    const vesselField = this.registerAutocompleteField('vesselSnapshot', {
      service: this.vesselSnapshotService,
      attributes: this.settings.getFieldDisplayAttributes('vesselSnapshot', ['exteriorMarking', 'name']),
      filter: {
        statusIds: [StatusIds.ENABLE, StatusIds.TEMPORARY]
      }
    });
    // Add base port location
    vesselField.attributes = vesselField.attributes.concat(this.settings.getFieldDisplayAttributes('location').map(key => 'basePortLocation.' + key));

    // Combo location
    this.registerAutocompleteField('location', {
      service: this.referentialRefService,
      filter: {
        entityName: 'Location',
        levelId: LocationLevelIds.PORT
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

  setValue(value: Trip, opts?: { emitEvent?: boolean; onlySelf?: boolean }) {

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

    // Send value for form
    super.setValue(value, opts);
  }

  async addVesselModal(): Promise<any> {
    const modal = await this.modalCtrl.create({ component: VesselModal });
    modal.onDidDismiss().then(res => {
      // if new vessel added, use it
      if (res && res.data instanceof VesselSnapshot) {
        console.debug("[trip-form] New vessel added : updating form...", res.data);
        this.form.controls['vesselSnapshot'].setValue(res.data);
        this.markForCheck();
      }
      else {
        console.debug("[trip-form] No vessel added (user cancelled)");
      }
    });
    return modal.present();
  }

  addObserver() {
    this.observersHelper.add();
    if (!this.mobile) {
      this.observerFocusIndex = this.observersHelper.size() - 1;
    }
  }

  referentialToString = referentialToString;

  /* -- protected methods-- */

  protected initObserversHelper() {
    if (isNil(this._showObservers)) return; // skip if not loading yet

    this.observersHelper = new FormArrayHelper<Person>(
      this.formBuilder,
      this.form,
      'observers',
      (person) => this.validatorService.getObserverControl(person),
      EntityUtils.equals,
      EntityUtils.isEmpty,
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
