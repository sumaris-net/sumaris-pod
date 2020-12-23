import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnInit} from '@angular/core';
import {Moment} from 'moment/moment';
import {
  EntityUtils,
  FormArrayHelper,
  IReferentialRef,
  isNil,
  isNotNil,
  Person,
  ReferentialRef,
  referentialToString
} from '../../core/core.module';
import {DateAdapter} from "@angular/material/core";
import {debounceTime, distinctUntilChanged, filter, pluck} from 'rxjs/operators';
import {AcquisitionLevelCodes, LocationLevelIds} from '../../referential/services/model/model.enum';
import {Landing2ValidatorService} from "../services/validator/landing2.validator";
import {PersonService} from "../../admin/services/person.service";
import {MeasurementValuesForm} from "../measurement/measurement-values.form.class";
import {MeasurementsValidatorService} from "../services/validator/measurement.validator";
import {FormArray, FormBuilder, FormControl, Validators} from "@angular/forms";
import {ModalController} from "@ionic/angular";
import {ReferentialUtils} from "../../core/services/model/referential.model";
import {personToString, UserProfileLabel} from "../../core/services/model/person.model";
import {LocalSettingsService} from "../../core/services/local-settings.service";
import {MatAutocompleteFieldAddOptions, MatAutocompleteFieldConfig} from "../../shared/material/material.autocomplete";
import {VesselSnapshotService} from "../../referential/services/vessel-snapshot.service";
import {toBoolean} from "../../shared/functions";
import {Landing} from "../services/model/landing.model";
import {ReferentialRefFilter, ReferentialRefService} from "../../referential/services/referential-ref.service";
import {ProgramService} from "../../referential/services/program.service";
import {StatusIds} from "../../core/services/model/model.enum";
import {VesselSnapshot} from "../../referential/services/model/vessel-snapshot.model";
import {VesselModal} from "../../referential/vessel/modal/modal-vessel";
import {TaxonNameRef} from "../../referential/services/model/taxon.model";
import {TaxonNameStrategy} from "../../referential/services/model/strategy.model";

@Component({
  selector: 'app-landing2-form',
  templateUrl: './landing2.form.html',
  styleUrls: ['./landing2.form.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class Landing2Form extends MeasurementValuesForm<Landing> implements OnInit {

  private _showObservers: boolean;
  private _vessel: any;
  observersHelper: FormArrayHelper<Person>;
  observerFocusIndex = -1;
  mobile: boolean;

  enableTaxonNameFilter = false;
  canFilterTaxonName = true;

  @Input() required = true;

  referenceTaxon : ReferentialRef;
  fishingAreas : ReferentialRef[];
  fishingAreaHelper: FormArrayHelper<ReferentialRef>;

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

  getReferenceTaxonControl(): FormControl {
    return null; //this.form.controls.observers;
  }

  get fishingAreasFormArray(): FormArray {
    return this.form.controls.fishingAreas as FormArray;
  }
  taxonNameHelper: FormArrayHelper<TaxonNameStrategy>;
  get taxonNamesForm(): FormArray {
    return this.form.controls.taxonNames as FormArray;
  }

  constructor(
    protected dateAdapter: DateAdapter<Moment>,
    protected measurementValidatorService: MeasurementsValidatorService,
    protected formBuilder: FormBuilder,
    protected programService: ProgramService,
    protected validatorService: Landing2ValidatorService,
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

    // Combo: sampleRowCode
    // const sampleRowCodeField = this.registerAutocompleteField('sampleRowCode', {
    //   service: this.vesselSnapshotService,
    //   attributes: this.settings.getFieldDisplayAttributes('sampleRowCode', ['exteriorMarking', 'name']),
    //   filter: {
    //     statusIds: [StatusIds.ENABLE, StatusIds.TEMPORARY]
    //   }
    // });
// Combo: sampleRowCodes
    const sampleRowCodeAttributes = this.settings.getFieldDisplayAttributes('sampleRowCode');
    this.registerAutocompleteField('sampleRowCode', {
      service: this.referentialRefService,
      attributes: sampleRowCodeAttributes,
      // Increase default column size, for 'label'
      columnSizes: sampleRowCodeAttributes.map(a => a === 'label' ? 4 : undefined/*auto*/),
      filter: <ReferentialRefFilter>{
        entityName: 'Program'
      },
      mobile: this.mobile
    });

    // const programAttributes = this.settings.getFieldDisplayAttributes('program');
    // this.registerAutocompleteField('program', {
    //   service: this.referentialRefService,
    //   attributes: programAttributes,
    //   // Increase default column size, for 'label'
    //   columnSizes: programAttributes.map(a => a === 'label' ? 4 : undefined/*auto*/),
    //   filter: <ReferentialRefFilter>{
    //     entityName: 'Program'
    //   },
    //   mobile: this.mobile
    // });


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


    // Combo: fishingArea
    const fishingAreaAttributes = this.settings.getFieldDisplayAttributes('fishingArea');
    this.registerAutocompleteField('fishingArea', {
      service: this.referentialRefService,
      attributes: fishingAreaAttributes,
      // Increase default column size, for 'label'
      columnSizes: fishingAreaAttributes.map(a => a === 'label' ? 4 : undefined/*auto*/),
      filter: <ReferentialRefFilter>{
        entityName: 'Location',
        statusIds: [StatusIds.ENABLE],
        levelId: LocationLevelIds.PORT
      },
      mobile: this.mobile
    });

    // Combo: taxon / targetSpecies
    // const taxonNameAttributes = this.settings.getFieldDisplayAttributes('taxonName');
    // const taxonNameField = this.registerAutocompleteField('taxonName', {
    //   service: this.referentialRefService,
    //   attributes: taxonNameAttributes,
    //   // Increase default column size, for 'label'
    //   columnSizes: taxonNameAttributes.map(a => a === 'label' ? 4 : undefined/*auto*/),
    //   filter: <ReferentialRefFilter>{
    //     entityName: 'TaxonName'
    //   },
    //   mobile: this.mobile
    // });

    this.registerAutocompleteField('taxonName', {
      suggestFn: (value, filter) => this.suggest(value, {
          ...filter, statusId : 1
        },
        'TaxonName',
        this.enableTaxonNameFilter),
      attributes: ['name'],
      columnNames: [ 'REFERENTIAL.NAME'],
      columnSizes: [2,10],
      mobile: this.settings.mobile
    });

    this.initTaxonNameHelper();
  }

  // TaxonName Helper -----------------------------------------------------------------------------------------------
  protected initTaxonNameHelper() {
    // appliedStrategies => appliedStrategies.location ?
    this.taxonNameHelper = new FormArrayHelper<TaxonNameStrategy>(
      FormArrayHelper.getOrCreateArray(this.formBuilder, this.form, 'taxonNames'),
      (ts) => this.validatorService.getTaxonNameStrategyControl(ts),
      (t1, t2) => EntityUtils.equals(t1.taxonName, t2.taxonName, 'name'),
      value => isNil(value) && isNil(value.taxonName),
      {
        allowEmptyArray: false
      }
    );
    // Create at least one fishing Area
    if (this.taxonNameHelper.size() === 0) {
      this.taxonNameHelper.resize(1);
    }
  }

  // get value(): any {
  //   const json = this.form.value;
  //
  //   // Add sampleRowCode, because if control disabled the value is missing
  //   json.sampleRowCode = this.form.get('sampleRowCode').value;
  //
  //   return json;
  // }

  public setValue(value: Landing) {
    // FIXME CLT MOck object for Imagine - 119
    // value.program => initiaized
    // value.location => initiaized
    // value.observer => initiaized
    // value.dateTime => initiaized

    // samples = empty array
    // let sample1 = new Sample();
    // let taxon = new TaxonNameRef();
    // taxon.__typename = "TaxonNameVO";
    // taxon.label = "NEP";
    // taxon.name = "Nephrops norvegicus";
    // taxon.statusId=1;
    // taxon.id=1043;
    // taxon.referenceTaxonId=1043;
    //
    // let taxon2 = new TaxonNameRef();
    // taxon2.__typename = "TaxonNameVO";
    // taxon2.name = "Dipturus batis";
    // taxon2.statusId=1;
    // taxon2.id=17906;
    // taxon2.referenceTaxonId=17906;
    //
    //
    // sample1.taxonName = taxon;
    // sample1.rankOrder=1;
    // value.samples.push(sample1)
    //
    // value.comments = "Test PYC";
    // value.rankOrder=1;
    //
    // value.measurementValues = value.measurementValues || {} ;
    // MeasurementValuesUtils.normalizeValuesToForm(value.measurementValues as MeasurementModelValues, pmfms, {
    //   // Keep extra pmfm values (not need to remove, when no validator used)
    //   keepSourceObject: true,
    //   onlyExistingPmfms: opts && opts.onlyExistingPmfms
    // });


    if (!value) return;

    // Make sure to have (at least) one observer
    value.observers = value.observers && value.observers.length ? value.observers : [null];

    // Resize observers array
    if (this._showObservers) {
      this.observersHelper.resize(Math.max(1, value.observers.length));
    } else {
      this.observersHelper.removeAllEmpty();
    }

    // Propagate the program
    if (value.program && value.program.label) {
      this.program = value.program.label;
    }
//this.taxonNamesForm.value = value.samples[0].taxonName;
    const taxonNameControl = this.taxonNamesForm;
    if (value && value.samples[0] && value.samples[0].taxonName) {
      const taxonName: TaxonNameRef = value.samples[0].taxonName as TaxonNameRef;
      let taxonNameseStrategies: TaxonNameStrategy[] = [];
      let taxonNameStrategy = new TaxonNameStrategy();
      taxonNameStrategy.taxonName = taxonName;
      taxonNameseStrategies.push(taxonNameStrategy);
      taxonNameControl.patchValue(taxonNameseStrategies);
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


  toggleFilteredItems(fieldName: string){
    let value : boolean;
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
  protected async suggest(value: string, filter: any, entityName: string, filtered: boolean) : Promise<IReferentialRef[]> {

    if(filtered) {
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
}
