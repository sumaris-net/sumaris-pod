import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnInit} from '@angular/core';
import {Moment} from 'moment/moment';
import {FormArrayHelper, isNil, isNotNil, Person, referentialToString} from '../../core/core.module';
import {DateAdapter} from "@angular/material/core";
import {debounceTime, distinctUntilChanged, filter, pluck} from 'rxjs/operators';
import {AcquisitionLevelCodes, LocationLevelIds} from '../../referential/services/model/model.enum';
import {LandingValidatorService} from "../services/validator/landing.validator";
import {PersonService} from "../../admin/services/person.service";
import {MeasurementValuesForm} from "../measurement/measurement-values.form.class";
import {MeasurementsValidatorService} from "../services/validator/measurement.validator";
import {FormArray, FormBuilder} from "@angular/forms";
import {ModalController} from "@ionic/angular";
import {ReferentialUtils} from "../../core/services/model/referential.model";
import {personToString, UserProfileLabel} from "../../core/services/model/person.model";
import {LocalSettingsService} from "../../core/services/local-settings.service";
import {MatAutocompleteFieldAddOptions, MatAutocompleteFieldConfig} from "../../shared/material/material.autocomplete";
import {VesselSnapshotService} from "../../referential/services/vessel-snapshot.service";
import {toBoolean} from "../../shared/functions";
import {Landing} from "../services/model/landing.model";
import {ReferentialRefService} from "../../referential/services/referential-ref.service";
import {ProgramService} from "../../referential/services/program.service";
import {StatusIds} from "../../core/services/model/model.enum";
import {VesselSnapshot} from "../../referential/services/model/vessel-snapshot.model";
import {VesselModal} from "../../referential/vessel/modal/modal-vessel";

@Component({
  selector: 'app-landing-form2',
  templateUrl: './landing2.form.html',
  styleUrls: ['./landing2.form.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class Landing2Form extends MeasurementValuesForm<Landing> implements OnInit {

  private _showObservers: boolean;
  observersHelper: FormArrayHelper<Person>;
  observerFocusIndex = -1;
  mobile: boolean;

  @Input() required = true;

  @Input() showProgram = true;
  @Input() showSampleRowCode = true;
  @Input() showVessel = true;
  @Input() showDateTime = true;
  @Input() showLocation = true;
  @Input() showFishingArea = true;
  @Input() showTargetSpecies = true;
  @Input() showComment = true;
  @Input() showMeasurements = true;
  @Input() showError = true;
  @Input() showButtons = true;
  @Input() locationLevelIds: number[];

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

  get empty(): any {
    const value = this.value;
    return ReferentialUtils.isEmpty(value.location)
      && (!value.dateTime)
      && (!value.comments || !value.comments.length);
  }

  get valid(): any {
    return this.form && (this.required ? this.form.valid : (this.form.valid || this.empty));
  }

  get observersForm(): FormArray {
    return this.form.controls.observers as FormArray;
  }

  constructor(
    protected dateAdapter: DateAdapter<Moment>,
    protected measurementValidatorService: MeasurementsValidatorService,
    protected formBuilder: FormBuilder,
    protected programService: ProgramService,
    protected validatorService: LandingValidatorService,
    protected referentialRefService: ReferentialRefService,
    protected personService: PersonService,
    protected vesselSnapshotService: VesselSnapshotService,
    protected settings: LocalSettingsService,
    protected modalCtrl: ModalController,
    protected cd: ChangeDetectorRef
  ) {
    super(dateAdapter, measurementValidatorService, formBuilder, programService, settings, cd, validatorService.getFormGroup());
    this._enable = false;
    this.mobile = this.settings.mobile;

    // Set default acquisition level
    this.acquisitionLevel = AcquisitionLevelCodes.LANDING;
  }

  ngOnInit() {
    super.ngOnInit();

    // Default values
    this.showObservers = toBoolean(this.showObservers, true); // Will init the observers helper
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
    const vesselField = this.registerAutocompleteField('vesselSnapshot', {
      service: this.vesselSnapshotService,
      attributes: this.settings.getFieldDisplayAttributes('vesselSnapshot', ['exteriorMarking', 'name']),
      filter: {
        statusIds: [StatusIds.ENABLE, StatusIds.TEMPORARY]
      }
    });
    // Add base port location
    vesselField.attributes = vesselField.attributes.concat(this.settings.getFieldDisplayAttributes('location').map(key => 'basePortLocation.' + key));

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
      if (res && res.data instanceof VesselSnapshot) {
        console.debug("[landing-form] New vessel added : updating form...", res.data);
        this.form.controls['vesselSnapshot'].setValue(res.data);
        this.markForCheck();
      }
      else {
        console.debug("[landing-form] No vessel added (user cancelled)");
      }
    });
    return modal.present();
  }

  public registerAutocompleteField(fieldName: string, options?: MatAutocompleteFieldAddOptions): MatAutocompleteFieldConfig {
    return super.registerAutocompleteField(fieldName, options);
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
      {allowEmptyArray: !this._showObservers}
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
