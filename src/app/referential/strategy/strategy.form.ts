import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Injector, Input, OnInit, ViewChild} from "@angular/core";
import {AppEditor, isNotNil, ReferentialRef, referentialToString} from "../../core/core.module";
import {ReferentialForm} from "../form/referential.form";
import {ReferentialUtils} from "../../core/services/model/referential.model";
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
import {BehaviorSubject} from "rxjs";
import {debounceTime} from "rxjs/operators";
import {EditorDataServiceLoadOptions} from "../../shared/services/data-service.class";
import {FormBuilder, FormGroup} from "@angular/forms";
import {AccountService} from "../../core/services/account.service";
import {ReferentialValidatorService} from "../services/validator/referential.validator";
import {Strategy, TaxonGroupStrategy, TaxonNameStrategy} from "../services/model/strategy.model";
import {Program} from "../services/model/program.model";

@Component({
  selector: 'app-strategy-form',
  templateUrl: 'strategy.form.html',
  styleUrls: ['strategy.form.scss'],
  providers: [
    {provide: ReferentialValidatorService, useExisting: StrategyValidatorService}
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class StrategyForm extends AppEditor<Strategy> implements OnInit {


  filterForm: FormGroup;
  $filter = new BehaviorSubject<Partial<PmfmStrategyFilter>>({});
  $allAcquisitionLevels = new BehaviorSubject<ReferentialRef[]>(undefined);
  taxonGroupListOptions = {
    allowEmptyArray: true,
    allowMultipleSelection: true,
    buttons: [{
      // TODO title:
      icon: 'arrow-forward-circle-outline',
      click: (event, item) => this.applyTaxonGroupToPmfm(event, item)
    }]};
  taxonNameListOptions = {
    allowEmptyArray: true,
    allowMultipleSelection: true,
    buttons: [{
      // TODO title:
      icon: 'arrow-forward-circle-outline',
      click: (event, item) => this.applyTaxonNameToPmfm(event, item)
    }]};

  @ViewChild('referentialForm', { static: true }) referentialForm: ReferentialForm;
  @ViewChild('acquisitionLevelList', { static: true }) acquisitionLevelList: AppListForm;
  @ViewChild('locationsList', { static: true }) locationListForm: AppListForm;
  @ViewChild('gearsList', { static: true }) gearListForm: AppListForm;

  @ViewChild('taxonGroupsList', { static: true }) taxonGroupListForm: AppListForm;


  @ViewChild('taxonNamesList', { static: true }) taxonNameListForm: AppListForm;


  @ViewChild('pmfmStrategiesTable', { static: true }) pmfmStrategiesTable: PmfmStrategiesTable;

  get form(): FormGroup {
    return this.referentialForm.form;
  }

  get firstError(): string {
    const firstChildWithError = this.children.find(item => isNotNil(item.error));
    return firstChildWithError && firstChildWithError.error;
  }

  @Input() program: Program;
  @Input() showBaseForm = true;

  @Input() allowMultiple = false;

  constructor(
    protected injector: Injector,
    protected formBuilder: FormBuilder,
    protected dateAdapter: DateAdapter<Moment>,
    protected settings: LocalSettingsService,
    protected validatorService: StrategyValidatorService,
    protected referentialRefService: ReferentialRefService,
    protected modalCtrl: ModalController,
    protected accountService: AccountService,
    protected cd: ChangeDetectorRef
  ) {
    super(injector, Strategy, null, {
      pathIdAttribute: null, // Do not load from route
      autoLoad: false
    });

    this.filterForm = formBuilder.group({
      acquisitionLevels: [null],
      locations: formBuilder.array([])
    });

    //this.debug = !environment.production;
  }

  ngOnInit() {
    //this.referentialForm.setForm(this.validatorService.getFormGroup());

    super.ngOnInit();


    this.registerSubscription(
      this.$filter
        .pipe(
          debounceTime(450)
        )
        .subscribe(filter => this.pmfmStrategiesTable.setFilter(filter))
    );

    // Load acquisition levels
    this.registerSubscription(
      this.referentialRefService.watchAll(0,1000, 'name', 'asc', {entityName: 'AcquisitionLevel'}, {fetchPolicy: 'cache-first', withTotal: false})
        .subscribe(res => this.$allAcquisitionLevels.next(res && res.data || []))
    );

    // TODO: Check label is unique
    /*this.form.get('label')
      .setAsyncValidators(async (control: AbstractControl) => {
        const label = control.enabled && control.value;
        return label && (await this.programService.existsByLabel(label)) ? {unique: true} : null;
      });*/

  }

  protected registerForms() {
    this.addChildForms([
      this.referentialForm,
      this.pmfmStrategiesTable,
      this.acquisitionLevelList,
      this.locationListForm,
      this.gearListForm,
      this.taxonGroupListForm,
      this.taxonNameListForm
    ]);
  }

  protected getFirstInvalidTabIndex(): number {
    return 0;
  }

  protected async computeTitle(data: Strategy): Promise<string> {
    return data && referentialToString(data) || 'PROGRAM.STRATEGY.NEW.TITLE';
  }

  protected canUserWrite(data: Strategy): boolean {
    return this.enabled && this.accountService.isAdmin(); // TODO test user is a program's manager
  }

  async save(event?: Event, options?: any): Promise<boolean> {
    if (this.dirty) {
      if (!this.valid) {
        await this.waitWhilePending();
        if (this.invalid) {
          this.logFormErrors();
          return false;
        }
      }

      const json = await this.getJsonValueToSave();
      const data = Strategy.fromObject(json);
      this.updateView(data, {openTabIndex: -1, updateTabAndRoute: false});
    }

    return true;
  }

  updateView(data: Strategy | null, opts?: { openTabIndex?: number; updateTabAndRoute?: boolean }) {
    super.updateView(data, {...opts, updateTabAndRoute: false});
  }

  async openSelectReferentialModal(opts: {
    filter: ReferentialRefFilter
  }): Promise<ReferentialRef[]> {

    const modal = await this.modalCtrl.create({ component: SelectReferentialModal,
      componentProps: {
        filter: opts.filter
      },
      keyboardClose: true,
      cssClass: 'modal-large'
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
    (items || []).forEach(item => this.acquisitionLevelList.add(item))

    this.markForCheck();
  }

  async addLocation() {
    if (this.disabled) return; // Skip

    const items = await this.openSelectReferentialModal({
      filter: {
        entityName: 'Location',
        levelIds: (this.program && this.program.locationClassifications || []).map(item => item.id).filter(isNotNil)
      }
    });

    // Add to list
    (items || []).forEach(item => this.locationListForm.add(item))

    this.markForCheck();
  }

  async addGear() {
    if (this.disabled) return; // Skip

    const items = await this.openSelectReferentialModal({
      filter: {
        entityName: 'Gear',
        levelId: this.program && this.program.gearClassification ? toNumber(this.program.gearClassification.id, -1) : -1
      }
    });

    // Add to list
    (items || []).forEach(item => this.gearListForm.add(item))
    this.markForCheck();
  }

  async addTaxonGroup(priorityLevel?: number) {
    if (this.disabled) return; // Skip

    priorityLevel = priorityLevel && priorityLevel > 0 ? priorityLevel : 1;

    const items = await this.openSelectReferentialModal({
      filter: {
        entityName: 'TaxonGroup',
        levelId: this.program && this.program.taxonGroupType ? toNumber(this.program.taxonGroupType.id, -1) : -1
      }
    });

    // Add to list
    (items || []).map(taxonGroup => TaxonGroupStrategy.fromObject({
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
    (items || []).map(taxonName => TaxonNameStrategy.fromObject({
      priorityLevel,
      taxonName: taxonName.asObject()
    }))
      .forEach(item => this.taxonNameListForm.add(item))
    this.markForCheck();
  }

  async load(id?: number, opts?: EditorDataServiceLoadOptions): Promise<void> {

  }


  setValue(data: Strategy) {

    console.debug("[strategy-form] Setting value", data);
    //const json = data.asObject();

    this.referentialForm.setForm(this.validatorService.getFormGroup(data));
    //AppFormUtils.copyEntity2Form(data, this.form, {emitEvent: false});

    // TODO get locations from AppliedStrategy
    this.locationListForm.value = []; //data.locations;

    this.gearListForm.value = data.gears;
    this.taxonGroupListForm.value = data.taxonGroups;
    this.taxonNameListForm.value = data.taxonNames;


    const allAcquisitionLevels = this.$allAcquisitionLevels.getValue();
    const collectedAcquisitionLevels = (data.pmfmStrategies || []).reduce((res, item) => {
      if (typeof item.acquisitionLevel === "string" && res[item.acquisitionLevel] === undefined) {
        res[item.acquisitionLevel] = allAcquisitionLevels.find(al => al.label === item.acquisitionLevel) || null;
      }
      return res;
    }, <{[key: string]: ReferentialRef|null}>{});
    this.acquisitionLevelList.value = Object.values(collectedAcquisitionLevels).filter(isNotNil) as ReferentialRef[];

    this.pmfmStrategiesTable.value = data.pmfmStrategies || [];


  }

  /* -- protected methods -- */


  protected async getJsonValueToSave(): Promise<any> {

    const json = this.form.value as Partial<Strategy>;

    // Re add label, because missing when field disable
    json.label = this.form.get('label').value;

    // TODO json.locations = this.locationssForm.value;
    json.gears = this.gearListForm.value;
    json.taxonGroups = this.taxonGroupListForm.value;
    json.taxonNames = this.taxonNameListForm.value;

    if (this.pmfmStrategiesTable.dirty) {
      const saved = await this.pmfmStrategiesTable.save();
      if (!saved) throw Error('Failed to save pmfmStrategiesTable');
    }
    json.pmfmStrategies = this.pmfmStrategiesTable.value;

    return json;
  }

  updateFilterAcquisitionLevel(value: ReferentialRef|any) {
    const acquisitionLevel = value && (value as ReferentialRef).label || undefined;
    this.patchPmfmStrategyFilter({acquisitionLevel});
  }

  updateFilterLocations(value: ReferentialRef[]|any) {
    const locationIds = value && (value as ReferentialRef[]).map(item => item.id) || undefined;
    this.patchPmfmStrategyFilter({locationIds});
  }

  updateFilterGears(value: ReferentialRef[]|any) {
    const gearIds = value && (value as ReferentialRef[]).map(item => item.id) || undefined;
    this.patchPmfmStrategyFilter({gearIds});
  }

  updateFilterTaxonGroups(value: TaxonGroupStrategy[]|any) {
    const taxonGroupIds = value && value.map(tgs => tgs.taxonGroup && tgs.taxonGroup.id) || undefined;
    this.patchPmfmStrategyFilter({taxonGroupIds});
  }

  updateFilterTaxonNames(value: TaxonNameStrategy[]|any) {
    const referenceTaxonIds = value && (value as TaxonNameStrategy[]).map(tgs => tgs.taxonName && tgs.taxonName.referenceTaxonId) || undefined;
    this.patchPmfmStrategyFilter({referenceTaxonIds});
  }

  /**
   * Applying a taxonName to selected PmfmSTrategy
   * @param event
   */
  applyTaxonGroupToPmfm(event: UIEvent, item: TaxonGroupStrategy) {
    console.debug("TODO: applying taxon group", item);
    event.preventDefault();

    const taxonGroupId = item.taxonGroup && item.taxonGroup.id;
    (this.pmfmStrategiesTable.selection.selected || [])
      .forEach(row => {
        const control = row.validator.get('taxonGroupIds');
        const taxonGroupIds = (control.value || []) as number[];
        if (!taxonGroupIds.includes(taxonGroupId)) {
          //row.startEdit();
          taxonGroupIds.push(taxonGroupId);
          control.setValue(taxonGroupIds);
          //row.confirmEditCreate();
          row.validator.markAsDirty();
        }
      });
    this.markForCheck();
  }


  applyTaxonNameToPmfm(event: UIEvent, item: TaxonNameStrategy) {
    console.debug("TODO: applying taxon name", item);
    event.preventDefault();
  }

  protected patchPmfmStrategyFilter(filter: Partial<PmfmStrategyFilter>) {
    this.$filter.next({
      ...this.$filter.getValue(),
      ...filter
    });
  }

  taxonGroupStrategyToString(data: TaxonGroupStrategy): string {
    return data && referentialToString(data.taxonGroup) || '';
  }

  taxonGroupStrategyEquals(v1: TaxonGroupStrategy, v2: TaxonGroupStrategy) {
    return ReferentialUtils.equals(v1.taxonGroup, v2.taxonGroup);
  }

  taxonNameStrategyToString(data: TaxonNameStrategy): string {
    return data && referentialToString(data.taxonName) || '';
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

