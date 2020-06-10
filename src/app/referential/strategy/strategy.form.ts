import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Injector, Input, OnInit, ViewChild} from "@angular/core";
import {TableElement, ValidatorService} from "angular4-material-table";
import {AppForm, AppTable, isNil, isNotNil, ReferentialRef} from "../../core/core.module";
import {Program, referentialToString, Strategy, TaxonGroupStrategy, TaxonNameStrategy} from "../services/model";
import {ReferentialForm} from "../form/referential.form";
import {ReferentialUtils} from "../../core/services/model";
import {PmfmStrategiesTable, PmfmStrategyFilter} from "./pmfm-strategies.table";
import {ReferentialRefFilter, ReferentialRefService} from "../services/referential-ref.service";
import {SelectReferentialModal} from "../list/select-referential.modal";
import {ModalController} from "@ionic/angular";
import {AppListForm} from "../../core/form/list.form";
import {toNumber} from "../../shared/functions";
import {DateAdapter} from "@angular/material/core";
import {Moment} from "moment";
import {LocalSettingsService} from "../../core/services/local-settings.service";
import {StrategyValidatorService} from "../services/validator/strategy.validator";
import {SelectionModel} from "@angular/cdk/collections";

@Component({
  selector: 'app-strategy-form',
  templateUrl: 'strategy.form.html',
  styleUrls: ['strategy.form.scss'],
  providers: [
    {provide: ValidatorService, useExisting: StrategyValidatorService}
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class StrategyForm extends AppForm<Strategy> implements OnInit {

  data: Strategy;
  components: (AppForm<any>|AppTable<any>)[] = [];

  @ViewChild('referentialForm', { static: true }) referentialForm: ReferentialForm;
  @ViewChild('acquisitionLevelList', { static: true }) acquisitionLevelList: AppListForm;
  @ViewChild('locationsList', { static: true }) locationListForm: AppListForm;
  @ViewChild('gearsList', { static: true }) gearListForm: AppListForm;
  @ViewChild('taxonGroupsList', { static: true }) taxonGroupListForm: AppListForm;
  @ViewChild('taxonNamesList', { static: true }) taxonNameListForm: AppListForm;

  @ViewChild('pmfmStrategiesTable', { static: true }) pmfmStrategiesTable: PmfmStrategiesTable;

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
    const errorComponent = this.components.find(item => isNotNil(item.error));
    return errorComponent && errorComponent.error;
  }

  get dirty(): boolean {
    return this.components.findIndex(component => component.dirty) !== -1;
  }

  get invalid(): boolean {
    return this.components.findIndex(component => component.invalid) !== -1;
  }

  get valid(): boolean {
    return !this._enable || this.components.findIndex(component => !component.valid) === -1;
  }

  get pending(): boolean {
    return this.components.findIndex(component => component.pending) !== -1;
  }

  markAsPristine(opts?: { onlySelf?: boolean; emitEvent?: boolean }) {
    super.markAsPristine(opts);
    this.components.forEach(component => component.markAsPristine(opts));
  }

  markAsTouched(opts?: { onlySelf?: boolean; emitEvent?: boolean }) {
    super.markAsTouched(opts);
    this.components.forEach(component => component.markAsTouched(opts));
  }

  markAsUntouched(opts?: { onlySelf?: boolean }) {
    super.markAsUntouched(opts);
    this.components.forEach(component => component.markAsUntouched(opts));
  }

  disable(opts?: {
    onlySelf?: boolean;
    emitEvent?: boolean;
  }): void {
    super.disable(opts);
    this.components.forEach(component => component.disable(opts));
  }

  enable(opts?: {
    onlySelf?: boolean;
    emitEvent?: boolean;
  }): void {
    super.enable(opts);
    if (!this.isNewData) {
      this.form.get('label').disable();
    }
    this.components.forEach(component => component.enable(opts));
  }

  @Input() program: Program;
  @Input() showBaseForm = true;

  @Input() allowMultiple = false;

  constructor(
    protected injector: Injector,
    protected dateAdapter: DateAdapter<Moment>,
    protected settings: LocalSettingsService,
    protected validatorService: StrategyValidatorService,
    protected referentialRefService: ReferentialRefService,
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

    this.components = [
      this.referentialForm,
      this.pmfmStrategiesTable,
      this.acquisitionLevelList,
      this.locationListForm,
      this.gearListForm,
      this.taxonGroupListForm,
      this.taxonNameListForm
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

  async addAcquisitionLevel() {
    if (this.disabled) return; // Skip

    const items = await this.openSelectReferentialModal({
      filter: {
        entityName: 'AcquisitionLevel'
      }
    });

    // Add to list
    (items || []).forEach(item => this.acquisitionLevelList.add(item))

    this.markForCheck();
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
    (items || []).forEach(item => this.locationListForm.add(item))

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
    (items || []).forEach(item => this.gearListForm.add(item))
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
      .forEach(item => this.taxonGroupListForm.add(item))
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
      .forEach(item => this.taxonNameListForm.add(item))
    this.markForCheck();
  }

  async save(event?: UIEvent): Promise<boolean> {

    const json = await this.getJsonValueToSave();

    this.data = this.data || new Strategy();
    this.data.fromObject(json);

    return true;
  }

  reset(data?: Strategy, opts?: { emitEvent?: boolean; onlySelf?: boolean }) {

    this.components.forEach(component => {
      if (component instanceof AppTable) {
        component.markAsLoading(opts);
      }
      else if (component instanceof AppForm) {
        component.reset(null, opts);
      }
    })
  }

  async setValue(data: Strategy) {
    data = data || new Strategy();

    console.debug("[strategy-form] Setting value", data);
    this.data = data;
    const json = data.asObject();

    this.form.patchValue(json, {emitEvent: false});

    // TODO get locations from AppliedStrategy
    this.locationListForm.value = []; //data.locations;

    this.gearListForm.value = data.gears;
    this.taxonGroupListForm.value = data.taxonGroups;
    this.taxonNameListForm.value = data.taxonNames;

    const allAcquisitionLevels = (await this.referentialRefService.loadAll(0,1000, 'name', 'asc', {entityName: 'AcquisitionLevel'}, {fetchPolicy: 'cache-first'})).data;
    const existingAcquisitionLevels = (data.pmfmStrategies || []).reduce((res, item) => {
      if (typeof item.acquisitionLevel === "string" && res[item.acquisitionLevel] === undefined) {
        res[item.acquisitionLevel] = allAcquisitionLevels.find(al => al.label === item.acquisitionLevel) || null;
      }
      return res;
    }, <{[key: string]: ReferentialRef|null}>{});
    this.acquisitionLevelList.value = Object.values(existingAcquisitionLevels).filter(isNotNil) as ReferentialRef[];

    this.pmfmStrategiesTable.value = data.pmfmStrategies || [];

    this.markAsPristine();
  }

  /* -- protected methods -- */


  protected async getJsonValueToSave(): Promise<any> {

    const json = this.form.value;

    // Re add label, because missing when field disable
    json.label = this.form.get('label').value;

    // TODO json.locations = this.locationssForm.value;
    json.gears = this.gearListForm.value;
    json.taxonGroups = this.taxonGroupListForm.value;
    json.taxonNames = this.taxonNameListForm.value;

    if (this.pmfmStrategiesTable.dirty) {
      const saved = await this.pmfmStrategiesTable.save();
      // TODO if (!saved)
    }
    json.pmfmStrategies = this.pmfmStrategiesTable.value;

    return json;
  }

  updateFilterAcquisitionLevel(value: ReferentialRef|any) {
    const acquisitionLevel = value && (value as ReferentialRef).label || undefined;
    this.patchPmfmStrategyFilter({acquisitionLevel});
  }

  updateFilterLocations(value: ReferentialRef[]|any) {
    const locationIds = (value as ReferentialRef[]).map(item => item.id);
    this.patchPmfmStrategyFilter({locationIds});
  }

  updateFilterGears(value: ReferentialRef[]|any) {
    const gearIds = (value as ReferentialRef[]).map(item => item.id);
    this.patchPmfmStrategyFilter({gearIds});
  }

  updateFilterTaxonGroups(value: TaxonGroupStrategy[]|any) {
    const taxonGroupIds = value.map(tgs => tgs.taxonGroup && tgs.taxonGroup.id);
    this.patchPmfmStrategyFilter({taxonGroupIds});
  }

  updateFilterTaxonNames(value: TaxonNameStrategy[]|any) {
    if (value instanceof Array) {
      const taxonNameIds = value.map(tgs => tgs.taxonName && tgs.taxonName.id);
      this.patchPmfmStrategyFilter({taxonNameIds});
    }
  }

  protected patchPmfmStrategyFilter(filter: Partial<PmfmStrategyFilter>) {
    this.pmfmStrategiesTable.setFilter({
      ...this.pmfmStrategiesTable.filter,
      ...filter
    });
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

  getReferentialName(item: ReferentialRef) {
    return item && item.name || '';
  }

  referentialToString = referentialToString;
  referentialEquals = ReferentialUtils.equals;

  protected markForCheck() {
    this.cd.markForCheck();
  }

}

