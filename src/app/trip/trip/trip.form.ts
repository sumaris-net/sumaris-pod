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
import {NetworkService} from "../../core/services/network.service";
import {Vessel} from "../../referential/services/model";
import {MetierRef} from "../../referential/services/model/taxon.model";

@Component({
  selector: 'form-trip',
  templateUrl: './trip.form.html',
  styleUrls: ['./trip.form.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class TripForm extends AppForm<Trip> implements OnInit {

  private _showObservers: boolean;
  private _showMetiers: boolean;

  observersHelper: FormArrayHelper<Person>;
  observerFocusIndex = -1;
  metiersHelper: FormArrayHelper<MetierRef>;
  metierFocusIndex = -1;
  mobile: boolean;

  @Input() showComment = true;
  @Input() showAddVessel = true;
  @Input() showError = true;
  @Input() usageMode: UsageMode;
  @Input() vesselDefaultStatus = StatusIds.TEMPORARY;


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

  @Input() set showMetiers(value: boolean) {
    if (this._showMetiers !== value) {
      this._showMetiers = value;
      this.initMetiersHelper();
      this.markForCheck();
    }
  }

  get showMetiers(): boolean {
    return this._showMetiers;
  }


  get value(): any {
    const json = this.form.value;

    // Add program, because if control disabled the value is missing
    json.program = this.form.get('program').value;

    // Force remove all observers
    if (!this._showObservers) {
      json.observers = [];
    }

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

  get metiersForm(): FormArray {
    return this.form.controls.metiers as FormArray;
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
    public network: NetworkService,
    protected cd: ChangeDetectorRef
  ) {

    super(dateAdapter, validatorService.getFormGroup(), settings);
    this.mobile = this.settings.mobile;
  }

  ngOnInit() {
    super.ngOnInit();

    // Default values
    this.showObservers = toBoolean(this.showObservers, true); // Will init the observers helper
    this.showMetiers = toBoolean(this.showMetiers, true); // Will init the metiers helper

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

    // Combo: metiers
    this.registerAutocompleteField('metier', {
      service: this.referentialRefService,
      filter: {
        entityName: 'Metier',
        statusId: StatusIds.ENABLE
      }
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

    // Make sure to have (at least) one metier
    value.metiers = value.metiers && value.metiers.length ? value.metiers : [null];
    // Resize metiers array
    if (this._showMetiers) {
      this.metiersHelper.resize(Math.max(1, value.metiers.length));
    }
    else {
      this.metiersHelper.removeAllEmpty();
    }

    // Send value for form
    super.setValue(value, opts);
  }

  async addVesselModal(): Promise<any> {
    const modal = await this.modalCtrl.create({
      component: VesselModal,
      componentProps: {
        defaultStatus: this.vesselDefaultStatus
      }
    });

    await modal.present();

    const res = await modal.onDidDismiss();

    // if new vessel added, use it
    const vessel = res && res.data;
    if (vessel) {
      const vesselSnapshot = (vessel instanceof VesselSnapshot) ? vessel :
        (vessel instanceof Vessel ? VesselSnapshot.fromVessel(vessel) : VesselSnapshot.fromObject(vessel));
      console.debug("[trip-form] New vessel added : updating form...", vesselSnapshot);
      this.form.controls['vesselSnapshot'].setValue(vesselSnapshot);
      this.markForCheck();
    }

    else {
      console.debug("[trip-form] No vessel added (user cancelled)");
    }
  }

  addObserver() {
    this.observersHelper.add();
    if (!this.mobile) {
      this.observerFocusIndex = this.observersHelper.size() - 1;
    }
  }

  addMetier() {
    this.metiersHelper.add();
    if (!this.mobile) {
      this.metierFocusIndex = this.metiersHelper.size() - 1;
    }
  }

  referentialToString = referentialToString;

  /* -- protected methods-- */

  protected initObserversHelper() {
    if (isNil(this._showObservers)) return; // skip if not loading yet

    // Create helper, if need
    if (!this.observersHelper) {
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
    }

    // Helper exists: update options
    else {
      this.observersHelper.allowEmptyArray = !this._showObservers;
    }

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

  protected initMetiersHelper() {
    if (isNil(this._showMetiers)) return; // skip if not loading yet

    this.metiersHelper = new FormArrayHelper<MetierRef>(
      this.formBuilder,
      this.form,
      'metiers',
      (metier) => this.validatorService.getMetierControl(metier),
      EntityUtils.equals,
      EntityUtils.isEmpty,
      {
        allowEmptyArray: !this._showMetiers
      }
    );

    if (this._showMetiers) {
      // Create at least one metier
      if (this.metiersHelper.size() === 0) {
        this.metiersHelper.resize(1);

      }
    }
    else if (this.metiersHelper.size() > 0) {
      this.metiersHelper.resize(0);
    }
  }

  protected markForCheck() {
    this.cd.markForCheck();
  }
}
