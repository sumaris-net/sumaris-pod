import {ChangeDetectionStrategy, Component, Injector, OnInit, ViewChild} from "@angular/core";
import {TableElement, ValidatorService} from "angular4-material-table";
import {AbstractControl, FormBuilder, FormGroup} from "@angular/forms";
import {AppEditorPage, EntityUtils, isNil, isNotNil, ReferentialRef} from "../../core/core.module";
import {
  PmfmStrategy,
  Program,
  ProgramProperties,
  referentialToString,
  Strategy,
  TaxonGroupStrategy, TaxonNameStrategy
} from "../services/model";
import {ProgramService} from "../services/program.service";
import {ReferentialForm} from "../form/referential.form";
import {ProgramValidatorService} from "../services/validator/program.validator";
import {StrategiesTable} from "../strategy/strategies.table";
import {changeCaseToUnderscore, fadeInOutAnimation} from "../../shared/shared.module";
import {AccountService} from "../../core/services/account.service";
import {ReferentialUtils} from "../../core/services/model";
import {PmfmStrategiesTable} from "../strategy/pmfm-strategies.table";
import {AppPropertiesForm} from "../../core/form/properties.form";
import {ReferentialRefFilter, ReferentialRefService} from "../services/referential-ref.service";
import {SelectReferentialModal} from "../list/select-referential.modal";
import {ModalController} from "@ionic/angular";
import {AppListForm} from "../../core/form/list.form";
import {FormFieldDefinition, FormFieldDefinitionMap} from "../../shared/form/field.model";
import {toNumber} from "../../shared/functions";

@Component({
  selector: 'app-program',
  templateUrl: 'program.page.html',
  providers: [
    {provide: ValidatorService, useExisting: ProgramValidatorService}
  ],
  animations: [fadeInOutAnimation],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ProgramPage extends AppEditorPage<Program> implements OnInit {

  propertyDefinitions = Object.getOwnPropertyNames(ProgramProperties).map(name => ProgramProperties[name]);
  fieldDefinitions: FormFieldDefinitionMap = {};
  form: FormGroup;
  editedStrategy: Strategy;
  i18nFieldPrefix = 'PROGRAM.';

  @ViewChild('referentialForm', { static: true }) referentialForm: ReferentialForm;
  @ViewChild('strategiesTable', { static: true }) strategiesTable: StrategiesTable;
  @ViewChild('pmfmStrategiesTable', { static: true }) pmfmStrategiesTable: PmfmStrategiesTable;
  @ViewChild('propertiesForm', { static: true }) propertiesForm: AppPropertiesForm;

  @ViewChild('locationsForm', { static: true }) locationsForm: AppListForm;
  @ViewChild('gearsForm', { static: true }) gearsForm: AppListForm;
  @ViewChild('taxonGroupsForm', { static: true }) taxonGroupsForm: AppListForm;
  @ViewChild('taxonNamesForm', { static: true }) taxonNamesForm: AppListForm;

  constructor(
    protected injector: Injector,
    protected formBuilder: FormBuilder,
    protected accountService: AccountService,
    protected validatorService: ProgramValidatorService,
    protected programService: ProgramService,
    protected referentialRefService: ReferentialRefService,
    protected modalCtrl: ModalController
  ) {
    super(injector,
      Program,
      programService);
    this.form = validatorService.getFormGroup();

    // default values
    this.defaultBackHref = "/referential/list?entity=Program";
    this._enabled = this.accountService.isAdmin();
    this.tabCount = 4;



    //this.debug = !environment.production;
  }

  ngOnInit() {
    super.ngOnInit();

    // Set entity name (required for referential form validator)
    this.referentialForm.entityName = 'Program';

    // Check label is unique
    this.form.get('label')
      .setAsyncValidators(async (control: AbstractControl) => {
        const label = control.enabled && control.value;
        return label && (await this.programService.existsByLabel(label)) ? {unique: true} : null;
      });

    this.registerFormField('gearClassification', {
      type: 'entity',
      autocomplete: {
        suggestFn: (value, filter) => this.referentialRefService.suggest(value, filter),
        filter: {
          entityName: 'GearClassification'
        }
      }
    });

    this.registerFormField('taxonGroupType', {
      key: 'taxonGroupType',
      type: 'entity',
      autocomplete: {
        suggestFn: (value, filter) => this.referentialRefService.suggest(value, filter),
        filter: {
          entityName: 'TaxonGroupType'
        }
      }
    });

    // Listen start editing strategy
    this.registerSubscription(this.strategiesTable.onStartEditingRow
      .subscribe(row => this.onStartEditStrategy(row)));
    this.registerSubscription(this.strategiesTable.onConfirmEditCreateRow
      .subscribe(row => this.onConfirmEditCreateStrategy(row)));
    this.registerSubscription(this.strategiesTable.onCancelOrDeleteRow
      .subscribe(row => this.onCancelOrDeleteStrategy(row)));
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

  removeFromArray(array: any[], item: any) {
    const index = array.findIndex(i => i === item);
    if (index !== -1) {
      array.splice(index, 1);
      this.markForCheck();
    }
  }

  async addInArray(array: any[], entityName: string) {
    const items = await this.openSelectReferentialModal({filter: {
        entityName
      }});
    (items || []).forEach(item => array.push(item))
    this.markForCheck();
  }

  async addLocation() {
    if (!this.editedStrategy) return; // Skip

    const items = await this.openSelectReferentialModal({
      filter: {
        entityName: 'Location',
        levelIds: (this.data.locationClassifications || []).map(item => item.id).filter(isNotNil)
      }
    });

    // Add to list
    (items || []).forEach(item => this.locationsForm.add(item))

    this.markForCheck();
  }

  async addGear() {
    if (!this.editedStrategy) return; // Skip

    const items = await this.openSelectReferentialModal({
      filter: {
        entityName: 'Gear',
        levelId: this.data.gearClassification ? toNumber(this.data.gearClassification.id, -1) : -1
      }
    });

    // Add to list
    (items || []).forEach(item => this.gearsForm.add(item))
    this.markForCheck();
  }

  async addTaxonGroup(priorityLevel?: number) {
    if (!this.editedStrategy) return; // Skip

    priorityLevel = priorityLevel && priorityLevel > 0 ? priorityLevel : 1;

    const items = await this.openSelectReferentialModal({
      filter: {
        entityName: 'TaxonGroup',
        levelId: this.data.taxonGroupType ? toNumber(this.data.taxonGroupType.id, -1) : -1
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
    if (!this.editedStrategy) return; // Skip

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
  /* -- protected methods -- */

  protected registerFormField(fieldName: string, def: Partial<FormFieldDefinition>) {
    const definition = <FormFieldDefinition>{
      key: fieldName,
      label: this.i18nFieldPrefix + changeCaseToUnderscore(fieldName).toUpperCase(),
      ...def
    }
    this.fieldDefinitions[fieldName] = definition;
  }

  protected canUserWrite(data: Program): boolean {
    // TODO : check user is in program managers
    return (this.isNewData && this.accountService.isAdmin())
      || (ReferentialUtils.isNotEmpty(data) && this.accountService.isSupervisor());

  }

  enable() {
    super.enable();

    if (!this.isNewData) {
      this.form.get('label').disable();
    }
  }

  protected registerFormsAndTables() {
    this.registerForms([this.referentialForm, this.propertiesForm])
      .registerTables([this.strategiesTable, this.pmfmStrategiesTable]);
  }

  protected setValue(data: Program) {
    if (!data) return; // Skip

    const json = data.asObject();
    const properties = EntityUtils.getObjectAsArray(json.properties);
    delete json.properties;

    this.form.patchValue(json, {emitEvent: false});

    this.propertiesForm.value = properties;

    // strategies
    this.strategiesTable.value = data.strategies && data.strategies.slice() || []; // force update

    this.markAsPristine();
  }

  protected async getJsonValueToSave(): Promise<any> {
    const data = await super.getJsonValueToSave();

    // Re add label, because missing when field disable
    data.label = this.form.get('label').value;
    data.properties = this.propertiesForm.value;
    data.strategies = this.data.strategies;

    return data;
  }

  protected computeTitle(data: Program): Promise<string> {
    // new data
    if (!data || isNil(data.id)) {
      return this.translate.get('PROGRAM.NEW.TITLE').toPromise();
    }

    // Existing data
    return this.translate.get('PROGRAM.EDIT.TITLE', data).toPromise();
  }

  protected getFirstInvalidTabIndex(): number {
    if (this.referentialForm.invalid) return 0;
    if (this.propertiesForm.invalid) return 1;
    if (this.strategiesTable.invalid) return 2;
    return 0;
  }

  protected async onStartEditStrategy(row: TableElement<Strategy>) {
    const strategy = this.getStrategy(row.currentData, false) || new Strategy();
    this.editedStrategy = strategy;

    console.debug("[program] Start editing strategy", strategy);

    this.locationsForm.value = this.data.locations;
    this.gearsForm.value = strategy.gears;
    this.taxonGroupsForm.value = strategy.taxonGroups;
    this.taxonNamesForm.value = strategy.taxonNames;
    this.pmfmStrategiesTable.value = strategy.pmfmStrategies || [];

    this.markForCheck();
  }
  protected async onCancelOrDeleteStrategy(row: TableElement<Strategy>) {
    if (!this.editedStrategy) return; // skip

    this.editedStrategy = null; // forget editing strategy
    this.markForCheck();
  }

  protected async onConfirmEditCreateStrategy(row: TableElement<Strategy>) {
    if (!this.editedStrategy) return; // skip

    const source = row.currentData;
    const target = this.getStrategy(source, true);

    // Update some properties
    target.label = source.label;
    target.name = source.name;
    target.description = source.description;
    target.statusId = source.statusId;
    target.comments = source.comments;

    target.gears = this.gearsForm.value;
    target.taxonGroups = this.taxonGroupsForm.value;
    target.taxonNames = this.taxonNamesForm.value;
    // TODO target.locations = this.locationssForm.value;

    // Update pmfm strategy
    await this.pmfmStrategiesTable.save();
    target.pmfmStrategies = (this.pmfmStrategiesTable.value || []).map(PmfmStrategy.fromObject);

    console.debug("[program] End editing strategy", target);

    this.editedStrategy = null; // forget editing strategy
    this.markForCheck();
  }

  protected getStrategy(lightStrategy: Strategy|any, createIfNotExists?: boolean) {
    let strategy = Strategy.fromObject(lightStrategy);
    const existingStrategy = this.data.strategies.find(s => ReferentialUtils.equals(s, strategy));
    if (existingStrategy) {

      return existingStrategy;
    }
    if (createIfNotExists) {
      this.data.strategies.push(strategy);
      return strategy;
    }

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

