import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Injector, Input, OnInit, ViewChild} from "@angular/core";
import {TableElement, ValidatorService} from "angular4-material-table";
import {FormBuilder} from "@angular/forms";
import {AppForm, AppTable, isNil, isNotNil, ReferentialRef} from "../../core/core.module";
import {Program, referentialToString, Strategy, TaxonGroupStrategy, TaxonNameStrategy} from "../services/model";
import {ProgramService} from "../services/program.service";
import {ReferentialForm} from "../form/referential.form";
import {ReferentialUtils} from "../../core/services/model";
import {PmfmStrategiesTable} from "../strategy/pmfm-strategies.table";
import {ReferentialRefFilter, ReferentialRefService} from "../services/referential-ref.service";
import {SelectReferentialModal} from "../list/select-referential.modal";
import {ModalController} from "@ionic/angular";
import {AppListForm} from "../../core/form/list.form";
import {toNumber} from "../../shared/functions";
import {DateAdapter} from "@angular/material/core";
import {Moment} from "moment";
import {LocalSettingsService} from "../../core/services/local-settings.service";
import {StrategyValidatorService} from "../services/validator/strategy.validator";

@Component({
  selector: 'app-strategy-form',
  templateUrl: 'strategy.form.html',
  providers: [
    {provide: ValidatorService, useExisting: StrategyValidatorService}
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class StrategyForm extends AppForm<Strategy> implements OnInit {

  data: Strategy;
  tablesAndForms: (AppForm<any>|AppTable<any>)[] = [];

  @ViewChild('referentialForm', { static: true }) referentialForm: ReferentialForm;
  @ViewChild('pmfmStrategiesTable', { static: true }) pmfmStrategiesTable: PmfmStrategiesTable;
  @ViewChild('locationsList', { static: true }) locationsForm: AppListForm;
  @ViewChild('gearsList', { static: true }) gearsForm: AppListForm;
  @ViewChild('taxonGroupsList', { static: true }) taxonGroupsForm: AppListForm;
  @ViewChild('taxonNamesList', { static: true }) taxonNamesForm: AppListForm;

  get isNewData(): boolean {
    return !this.data || isNil(this.data.id);
  }

  get value(): Strategy {
    return this.data;
  }

  @Input() set value(data: Strategy) {
    if (this.data !== data) {
      this.setValue(data);
    }
  }

  get firstError(): string {
    const errorComponent = this.tablesAndForms.find(item => isNotNil(item.error));
    return errorComponent && errorComponent.error;
  }

  get dirty(): boolean {
    return this.tablesAndForms.findIndex(component => component.dirty) !== -1;
  }

  get invalid(): boolean {
    return this.tablesAndForms.findIndex(component => component.invalid) !== -1;
  }

  get valid(): boolean {
    return !this._enable || this.tablesAndForms.findIndex(component => !component.valid) === -1;
  }

  get pending(): boolean {
    return this.tablesAndForms.findIndex(component => component.pending) !== -1;
  }

  markAsPristine(opts?: { onlySelf?: boolean; emitEvent?: boolean }) {
    super.markAsPristine(opts);
    this.tablesAndForms.forEach(component => component.markAsPristine(opts));
  }

  markAsTouched(opts?: { onlySelf?: boolean; emitEvent?: boolean }) {
    super.markAsTouched(opts);
    this.tablesAndForms.forEach(component => component.markAsTouched(opts));
  }

  markAsUntouched(opts?: { onlySelf?: boolean }) {
    super.markAsUntouched(opts);
    this.tablesAndForms.forEach(component => component.markAsUntouched(opts));
  }

  disable(opts?: {
    onlySelf?: boolean;
    emitEvent?: boolean;
  }): void {
    super.disable(opts);
    this.tablesAndForms.forEach(component => component.disable(opts));
  }

  enable(opts?: {
    onlySelf?: boolean;
    emitEvent?: boolean;
  }): void {
    super.enable(opts);
    if (!this.isNewData) {
      this.form.get('label').disable();
    }
    this.tablesAndForms.forEach(component => component.enable(opts));
  }

  @Input() program: Program;
  @Input() showBaseForm = true;

  constructor(
    protected injector: Injector,
    protected dateAdapter: DateAdapter<Moment>,
    protected settings: LocalSettingsService,
    protected validatorService: StrategyValidatorService,
    protected programService: ProgramService,
    protected modalCtrl: ModalController,
    protected cd: ChangeDetectorRef
  ) {
    super(dateAdapter,
      validatorService.getFormGroup(),
      settings);

    this._enable = false; // Waiting value to be set
    //this.debug = !environment.production;
  }

  ngOnInit() {
    super.ngOnInit();

    this.tablesAndForms = [
      this.referentialForm,
      this.pmfmStrategiesTable,
      this.locationsForm,
      this.gearsForm,
      this.taxonGroupsForm,
      this.taxonNamesForm
    ];

    // TODO: Check label is unique
    /*this.form.get('label')
      .setAsyncValidators(async (control: AbstractControl) => {
        const label = control.enabled && control.value;
        return label && (await this.programService.existsByLabel(label)) ? {unique: true} : null;
      });*/

  }

  async openSelectReferentialModal(opts: {
    filter: ReferentialRefFilter
  }): Promise<ReferentialRef[]> {

    const modal = await this.modalCtrl.create({ component: SelectReferentialModal,
      componentProps: {
        filter: opts.filter
      }
    });

    await modal.present();

    const {data} = await modal.onDidDismiss();

    return data;
  }

  async addLocation() {
    if (this.disabled) return; // Skip

    const items = await this.openSelectReferentialModal({
      filter: {
        entityName: 'Location',
        levelIds: (this.program && this.program.locationClassifications || []).map(item => item.id).filter(isNotNil)
      }
    });

    // Add to list
    (items || []).forEach(item => this.locationsForm.add(item))

    this.markForCheck();
  }

  async addGear() {
    if (this.disabled) return; // Skip

    const items = await this.openSelectReferentialModal({
      filter: {
        entityName: 'Gear',
        levelId: this.program && this.program.gearClassification ? toNumber(this.program.gearClassification.id, -1) : -1
      }
    });

    // Add to list
    (items || []).forEach(item => this.gearsForm.add(item))
    this.markForCheck();
  }

  async addTaxonGroup(priorityLevel?: number) {
    if (this.disabled) return; // Skip

    priorityLevel = priorityLevel && priorityLevel > 0 ? priorityLevel : 1;

    const items = await this.openSelectReferentialModal({
      filter: {
        entityName: 'TaxonGroup',
        levelId: this.program && this.program.taxonGroupType ? toNumber(this.program.taxonGroupType.id, -1) : -1
      }
    });

    // Add to list
    (items || []).map(taxonGroup => TaxonGroupStrategy.fromObject({
      priorityLevel,
      taxonGroup: taxonGroup.asObject()
    }))
      .forEach(item => this.taxonGroupsForm.add(item))
    this.markForCheck();
  }


  async addTaxonName(priorityLevel?: number) {
    if (this.disabled) return; // Skip

    priorityLevel = priorityLevel && priorityLevel > 0 ? priorityLevel : 1;

    const items = await this.openSelectReferentialModal({
      filter: {
        entityName: 'TaxonName'
      }
    });

    // Add to list
    (items || []).map(taxonName => TaxonNameStrategy.fromObject({
      priorityLevel,
      taxonName: taxonName.asObject()
    }))
      .forEach(item => this.taxonNamesForm.add(item))
    this.markForCheck();
  }

  async save(event?: UIEvent): Promise<boolean> {

    const json = await this.getJsonValueToSave();

    this.data = this.data || new Strategy();
    this.data.fromObject(json);

    return true;
  }

  /* -- protected methods -- */

  protected async setValue(data: Strategy) {
    data = data || new Strategy();

    console.debug("[strategy-form] Setting value", data);
    this.data = data;
    const json = data.asObject();

    this.form.patchValue(json, {emitEvent: false});

    // TODO get locations from AppliedStrategy
    this.locationsForm.value = []; //data.locations;

    this.gearsForm.value = data.gears;
    this.taxonGroupsForm.value = data.taxonGroups;
    this.taxonNamesForm.value = data.taxonNames;
    this.pmfmStrategiesTable.value = data.pmfmStrategies || [];

    this.markAsPristine();
  }

  protected async getJsonValueToSave(): Promise<any> {

    const json = this.form.value;

    // Re add label, because missing when field disable
    json.label = this.form.get('label').value;

    // TODO json.locations = this.locationssForm.value;
    json.gears = this.gearsForm.value;
    json.taxonGroups = this.taxonGroupsForm.value;
    json.taxonNames = this.taxonNamesForm.value;

    if (this.pmfmStrategiesTable.dirty) {
      const saved = await this.pmfmStrategiesTable.save();
      // TODO if (!saved)
    }
    json.pmfmStrategies = this.pmfmStrategiesTable.value;

    return json;
  }



  taxonGroupStrategyToString(data: TaxonGroupStrategy): string {
    return data && referentialToString(data.taxonGroup) || '';
  }

  taxonGroupStrategyEquals(v1: TaxonGroupStrategy, v2: TaxonGroupStrategy) {
    return ReferentialUtils.equals(v1.taxonGroup, v2.taxonGroup);
  }

  taxonNameStrategyToString(data: TaxonNameStrategy): string {
    return data && referentialToString(data.taxonName) || '';
  }

  taxonNameStrategyEquals(v1: TaxonNameStrategy, v2: TaxonNameStrategy) {
    return ReferentialUtils.equals(v1.taxonName, v2.taxonName);
  }

  referentialToString = referentialToString;
  referentialEquals = ReferentialUtils.equals;

  protected markForCheck() {
    this.cd.markForCheck();
  }

}

