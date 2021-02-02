import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnInit} from '@angular/core';
import {Moment} from 'moment/moment';

import {DateAdapter} from "@angular/material/core";
import {debounceTime, distinctUntilChanged, filter, pluck} from "rxjs/operators";
import {AcquisitionLevelCodes, LocationLevelIds, PmfmIds} from "../../../referential/services/model/model.enum";
import {PersonService} from "../../../admin/services/person.service";
import {MeasurementValuesForm} from "../../measurement/measurement-values.form.class";
import {MeasurementsValidatorService} from "../../services/validator/measurement.validator";
import {FormArray, FormBuilder, FormControl, Validators} from "@angular/forms";
import {ModalController} from "@ionic/angular";
import {IReferentialRef, ReferentialRef, ReferentialUtils} from "../../../core/services/model/referential.model";
import {Person, personToString, UserProfileLabels} from "../../../core/services/model/person.model";
import {LocalSettingsService} from "../../../core/services/local-settings.service";
import {VesselSnapshotService} from "../../../referential/services/vessel-snapshot.service";
import {isNil, isNotNil, toBoolean} from "../../../shared/functions";
import {Landing} from "../../services/model/landing.model";
import {ReferentialRefService} from "../../../referential/services/referential-ref.service";
import {ProgramService} from "../../../referential/services/program.service";
import {StatusIds} from "../../../core/services/model/model.enum";
import {VesselSnapshot} from "../../../referential/services/model/vessel-snapshot.model";
import {VesselModal} from "../../../referential/vessel/modal/modal-vessel";
import {Strategy} from "../../../referential/services/model/strategy.model";
import {TranslateService} from "@ngx-translate/core";
import {FormArrayHelper} from "../../../core/form/form.utils";
import {
  MatAutocompleteFieldAddOptions,
  MatAutocompleteFieldConfig
} from "../../../shared/material/autocomplete/material.autocomplete";
import {LandingValidatorService} from "../../services/validator/landing.validator";
import {PmfmStrategy} from "../../../referential/services/model/pmfm-strategy.model";
import {EntityUtils} from "../../../core/services/model/entity.model";

const DEFAULT_I18N_PREFIX = 'LANDING.EDIT.';

@Component({
  selector: 'app-sampling-landing-form',
  templateUrl: './sampling-landing.form.html',
  styleUrls: ['./sampling-landing.form.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SamplingLandingForm extends MeasurementValuesForm<Landing> implements OnInit {

  private _showObservers: boolean;
  private _vessel: any;

  mobile: boolean;
  observersHelper: FormArrayHelper<Person>;
  observerFocusIndex = -1;
  enableTaxonNameFilter = false;
  canFilterTaxonName = true;
  strategyControl: FormControl;

  @Input() i18nPrefix = DEFAULT_I18N_PREFIX;

  @Input() canEditStrategy = true;
  @Input() required = true;

  @Input() showProgram = true;
  @Input() showVessel = true;
  @Input() showDateTime = true;
  @Input() showLocation = true;
  @Input() showComment = true;
  @Input() showMeasurements = true;
  @Input() showError = true;
  @Input() showButtons = true;
  @Input() showStrategy = true; // TODO BLA change to false
  @Input() locationLevelIds: number[];
  @Input() allowAddNewVessel: boolean;

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

  @Input()
  set vessel(value: string) {
    if (this._vessel !== value && isNotNil(value)) {
      this._vessel = value;
    }
  }

  get vessel(): string {
    return this._vessel;
  }

  get empty(): any {
    const value = this.value;
    return !this.strategy
      && ReferentialUtils.isEmpty(value.location)
      && (!value.dateTime)
      && (!value.comments || !value.comments.length);
  }

  get valid(): any {
    return this.form
      && (this.required && this.form.valid && this.strategyControl.valid || this.empty);
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
    protected translate: TranslateService,
    protected modalCtrl: ModalController,
    protected cd: ChangeDetectorRef
  ) {
    super(dateAdapter, measurementValidatorService, formBuilder, programService, settings, cd, validatorService.getFormGroup(), {
      mapPmfms: pmfms => this.mapPmfms(pmfms)
    });
    this._enable = false;
    this.mobile = this.settings.mobile;
    this.requiredStrategy = true; // Will force to wait to get strategy, to fill Pmfm

    // Set default acquisition level
    this.acquisitionLevel = AcquisitionLevelCodes.LANDING;

    // Add a strategy field (not in validator)
    this.strategyControl = formBuilder.control(null, [Validators.required]);

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

    // Combo: sampleRowCode
    this.registerAutocompleteField('strategy', {
      service: this.referentialRefService,
      filter: {
        entityName: 'Strategy',
        levelLabel: this.programSubject.getValue() // is empty, will be set in setProgram()
      },
      attributes: ['label'],
      columnSizes: [12]
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
          pluck<ReferentialRef, string>('label'),
          distinctUntilChanged()
        )
        .subscribe(programLabel => this.program = programLabel)
        );

    // Propagate strategy
    this.registerSubscription(
      this.strategyControl.valueChanges
        .pipe(
          debounceTime(250),
          filter(ReferentialUtils.isNotEmpty),
          pluck<ReferentialRef, string>('label'),
          distinctUntilChanged()
        )
        .subscribe(strategyLabel => this.strategy = strategyLabel)
    );

    // Combo location
    this.registerAutocompleteField('location', {
      service: this.referentialRefService,
      filter: {
        entityName: 'Location',
        levelIds: this.locationLevelIds
      }
    });

    // Combo: observers
    this.registerAutocompleteField('person', {
      service: this.personService,
      filter: {
        statusIds: [StatusIds.TEMPORARY, StatusIds.ENABLE],
        userProfiles: [UserProfileLabels.SUPERVISOR, UserProfileLabels.USER, UserProfileLabels.GUEST]
      },
      attributes: ['lastName', 'firstName', 'department.name'],
      displayWith: personToString
    });

    this.registerAutocompleteField('taxonName', {
      suggestFn: (value, filter) => this.suggest(value, {
          ...filter, statusId : 1
        },
        'TaxonName',
        this.enableTaxonNameFilter),
      attributes: ['name'],
      columnNames: [ 'REFERENTIAL.NAME'],
      columnSizes: [2, 10],
      mobile: this.settings.mobile
    });

  }

  // get value(): any {
  //   const json = this.form.value;
  //
  //   // Add sampleRowCode, because if control disabled the value is missing
  //   json.sampleRowCode = this.form.get('sampleRowCode').value;
  //
  //   return json;
  // }

  async safeSetValue(data: Landing, opts?: { emitEvent?: boolean; onlySelf?: boolean; normalizeEntityToForm?: boolean; [p: string]: any }) {
    if (!data) return;

    // Make sure to have (at least) one observer
    data.observers = data.observers && data.observers.length ? data.observers : [null];

    // Resize observers array
    if (this._showObservers) {
      this.observersHelper.resize(Math.max(1, data.observers.length));
    } else {
      this.observersHelper.removeAllEmpty();
    }

    // Propagate the program
    if (data.program && data.program.label) {
      this.program = data.program.label;
    }

    // Set strategy field
    const strategyLabel = Object.entries(data.measurementValues || {})
      .filter(([pmfmId, _]) => +pmfmId === PmfmIds.STRATEGY_LABEL)
      .map(([_, value]) => value)
      .find(isNotNil) as string;
    this.strategyControl.patchValue(ReferentialRef.fromObject({label: strategyLabel}));
    this.strategy = strategyLabel;

    await super.safeSetValue(data, opts);

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
    const isNew = !this.data || isNil(this.data.id);
    if (!isNew && !this.form.controls['program'].disabled) {
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

  registerAutocompleteField(fieldName: string, options?: MatAutocompleteFieldAddOptions): MatAutocompleteFieldConfig {
    return super.registerAutocompleteField(fieldName, options);
  }

  /* -- protected method -- */

  protected setProgram(program: string) {
    super.setProgram(program);

    // Update the strategy filter (if autocomplete field exists. If not, program will set later in ngOnInit())
    if (this.autocompleteFields.strategy) {
      this.autocompleteFields.strategy.filter.levelLabel = program;
    }
  }

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

  toggleFilteredItems(fieldName: string){
    let value: boolean;
    switch (fieldName) {
      case 'taxonName':
        this.enableTaxonNameFilter = value = !this.enableTaxonNameFilter;
        break;
      default:
        break;
    }
    this.markForCheck();
    console.debug(`[landing] set enable filtered ${fieldName} items to ${value}`);
  }

  /**
   * Suggest autocomplete values
   * @param value
   * @param filter - filters to apply
   * @param entityName - referential to request
   * @param filtered - boolean telling if we load prefilled data
   */
  protected async suggest(value: string, filter: any, entityName: string, filtered: boolean): Promise<IReferentialRef[]> {

    if (filtered) {
      //TODO a remplacer par recuperation des donnees deja saisies
      const res = await this.referentialRefService.loadAll(0, 5, null, null,
        { ...filter,
          entityName : entityName
        },
        { withTotal: false /* total not need */ }
      );
      return res.data;
    } else {
      return this.referentialRefService.suggest(value, {
        ...filter,
        entityName : entityName
      });
    }
  }

  protected getValue(): Landing {
    const data = super.getValue();

    if (this.showStrategy) {
      const strategyValue = this.strategyControl.value;
      const strategyLabel = EntityUtils.isNotEmpty(strategyValue, 'label') && strategyValue.label || strategyValue as string;
      data.measurementValues[PmfmIds.STRATEGY_LABEL.toString()] = strategyLabel;
    }

    return data;
  }

  protected async mapPmfms(pmfms: PmfmStrategy[]): Promise<PmfmStrategy[]> {

    return (pmfms || []).map(p => {
      if (p && p.pmfmId === PmfmIds.STRATEGY_LABEL) {
        const strategyLabelPmfm = p.clone(); // Copy, to leave original PMFM unchanged

        // Mark as optional AND hidden
        strategyLabelPmfm.hidden = true;
        strategyLabelPmfm.required = false;
        return strategyLabelPmfm;
      }
      return p;
    });
  }
}
