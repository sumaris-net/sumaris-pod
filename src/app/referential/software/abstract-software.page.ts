import {ChangeDetectorRef, Injector, OnInit, ViewChild} from "@angular/core";
import {AbstractControl, FormArray, FormGroup} from "@angular/forms";
import {EntityUtils} from '../../core/services/model/entity.model';
import {Software} from '../../core/services/model/config.model';
import {FormArrayHelper} from "../../core/form/form.utils";
import {FormFieldDefinition, FormFieldDefinitionMap, FormFieldValue} from "../../shared/form/field.model";
import {PlatformService} from "../../core/services/platform.service";
import {AppEntityEditor, isNil} from "../../core/core.module";
import {AccountService} from "../../core/services/account.service";
import {ReferentialForm} from "../form/referential.form";
import {SoftwareService} from "../services/software.service";
import {SoftwareValidatorService} from "../services/validator/software.validator";
import {AppEditorOptions} from "../../core/form/editor.class";
import {ConfigOptions} from "../../core/services/config/core.config";


export abstract class AbstractSoftwarePage<T extends Software<T>, S extends SoftwareService<T>>
  extends AppEntityEditor<T, S>
  implements OnInit {

  protected accountService: AccountService;
  protected platform: PlatformService;
  protected cd: ChangeDetectorRef;

  propertyDefinitions: FormFieldDefinition[];
  propertyDefinitionsByKey: FormFieldDefinitionMap = {};
  propertyDefinitionsByIndex: { [index: number]: FormFieldDefinition } = {};
  propertiesFormHelper: FormArrayHelper<FormFieldValue>;

  form: FormGroup;


  @ViewChild('referentialForm', { static: true }) referentialForm: ReferentialForm;

  get propertiesForm(): FormArray {
    return this.form.get('properties') as FormArray;
  }

  protected constructor(
    injector: Injector,
    dataType: new() => T,
    dataService: S,
    protected validatorService: SoftwareValidatorService,
    private configOptions: FormFieldDefinitionMap,
    options?: AppEditorOptions,
    ) {
    super(injector,
      dataType,
      dataService,
      options);
    this.platform = injector.get(PlatformService);
    this.accountService = injector.get(AccountService);
    this.cd = injector.get(ChangeDetectorRef);

    // Fill property definitions map
    this.propertyDefinitions = Object.keys({...ConfigOptions, ...configOptions}).map(name => configOptions[name]);
    this.propertyDefinitions.forEach(o => this.propertyDefinitionsByKey[o.key] = o);

    this.form = validatorService.getFormGroup();
    this.propertiesFormHelper = new FormArrayHelper<FormFieldValue>(
      this.form.get('properties') as FormArray,
      (value) => validatorService.getPropertyFormGroup(value),
      (v1, v2) => (!v1 && !v2) || v1.key === v2.key,
      (value) => isNil(value) || (isNil(value.key) && isNil(value.value)),
      {
        allowEmptyArray: true
      }
    );

  }

  ngOnInit() {
    super.ngOnInit();

    // Set entity name (required for referential form validator)
    this.referentialForm.entityName = 'Software';

    // Check label is unique
    this.form.get('label')
      .setAsyncValidators(async (control: AbstractControl) => {
        const label = control.enabled && control.value;
        return label && (await this.service.existsByLabel(label)) ? {unique: true} : null;
      });
  }

  getPropertyDefinition(index: number): FormFieldDefinition {
    let definition = this.propertyDefinitionsByIndex[index];
    if (!definition) {
      definition = this.updatePropertyDefinition(index);
      this.propertyDefinitionsByIndex[index] = definition;
    }
    return definition;
  }

  updatePropertyDefinition(index: number): FormFieldDefinition {
    const key = (this.propertiesForm.at(index) as FormGroup).controls.key.value;
    const definition = key && this.propertyDefinitionsByKey[key] || null;
    this.propertyDefinitionsByIndex[index] = definition; // update map by index
    this.markForCheck();
    return definition;
  }

  removePropertyAt(index: number) {
    this.propertiesFormHelper.removeAt(index);
    this.propertyDefinitionsByIndex = {}; // clear map by index
    this.markForCheck();
  }

  /* -- protected methods -- */

  protected canUserWrite(data: T): boolean {
    return this.accountService.isAdmin();
  }

  enable() {
    super.enable();

    if (!this.isNewData) {
      this.form.get('label').disable();
    }
  }

  protected registerForms() {
    this.addChildForm(this.referentialForm);
  }

  protected async loadFromRoute(): Promise<void> {

    // Make sure the platform is ready
    await this.platform.ready();


    return super.loadFromRoute();
  }

  protected setValue(data: T) {
    if (!data) return; // Skip

    const json = data.asObject();

    // Transform properties map into array
    json.properties = EntityUtils.getObjectAsArray(data.properties || {});
    this.propertiesFormHelper.resize(Math.max(json.properties.length, 1));

    this.form.patchValue(json, {emitEvent: false});


    this.markAsPristine();
  }

  protected async getJsonValueToSave(): Promise<any> {
    const json = await super.getJsonValueToSave();

    // Re add label, because missing when field disable
    json.label = this.form.get('label').value;

    return json;
  }

  protected computeTitle(data: T): Promise<string> {
    // new data
    if (!data || isNil(data.id)) {
      return this.translate.get('CONFIGURATION.NEW.TITLE').toPromise();
    }

    return this.translate.get('CONFIGURATION.EDIT.TITLE', data).toPromise();
  }

  protected getFirstInvalidTabIndex(): number {
    if (this.referentialForm.invalid) return 0;
    return 0;
  }


  protected markForCheck() {
    this.cd.markForCheck();
  }

}

