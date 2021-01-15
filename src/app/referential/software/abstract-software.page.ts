import {ChangeDetectorRef, Directive, Injector, OnInit, ViewChild} from "@angular/core";
import {AbstractControl, FormArray, FormGroup} from "@angular/forms";
import {EntityUtils} from '../../core/services/model/entity.model';
import {Software} from '../../core/services/model/config.model';
import {FormArrayHelper} from "../../core/form/form.utils";
import {FormFieldDefinition, FormFieldDefinitionMap} from "../../shared/form/field.model";
import {PlatformService} from "../../core/services/platform.service";
import {AppEntityEditor, isNil} from "../../core/core.module";
import {AccountService} from "../../core/services/account.service";
import {ReferentialForm} from "../form/referential.form";
import {SoftwareService} from "../services/software.service";
import {SoftwareValidatorService} from "../services/validator/software.validator";
import {AppEditorOptions} from "../../core/form/editor.class";
import {CORE_CONFIG_OPTIONS} from "../../core/services/config/core.config";
import {ReferentialRefService} from "../services/referential-ref.service";
import {EntityServiceLoadOptions} from "../../shared/services/entity-service.class";
import {ObjectMapEntry} from "../../shared/types";

@Directive()
export abstract class AbstractSoftwarePage<T extends Software<T>, S extends SoftwareService<T>>
  extends AppEntityEditor<T, S>
  implements OnInit {

  protected accountService: AccountService;
  protected platform: PlatformService;
  protected cd: ChangeDetectorRef;
  protected referentialRefService: ReferentialRefService;

  propertyDefinitions: FormFieldDefinition[];
  propertyDefinitionsByKey: FormFieldDefinitionMap = {};
  propertyDefinitionsByIndex: { [index: number]: FormFieldDefinition } = {};
  propertiesFormHelper: FormArrayHelper<ObjectMapEntry>;

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
    configOptions: FormFieldDefinitionMap,
    options?: AppEditorOptions,
    ) {
    super(injector,
      dataType,
      dataService,
      options);
    this.platform = injector.get(PlatformService);
    this.accountService = injector.get(AccountService);
    this.cd = injector.get(ChangeDetectorRef);
    this.referentialRefService = injector.get(ReferentialRefService);

    // Convert map to list of options
    this.propertyDefinitions = Object.values({...CORE_CONFIG_OPTIONS, ...configOptions})
      .map(o => o.type !== 'entity' ? o : <FormFieldDefinition>{
        ...o,
        autocomplete: {
          ...o.autocomplete,
          suggestFn: (value, options) => this.referentialRefService.suggest(value, options)
        }
      })
    ;
    // Fill property definitions map
    this.propertyDefinitions.forEach(o => this.propertyDefinitionsByKey[o.key] = o);

    this.form = validatorService.getFormGroup();
    this.propertiesFormHelper = new FormArrayHelper<ObjectMapEntry>(
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

  protected async onEntityLoaded(data: T, options?: EntityServiceLoadOptions): Promise<void> {
    await this.prepareDataPropertiesToForm(data);
    await super.onEntityLoaded(data, options);
  }

  protected async onEntitySaved(data: T): Promise<void> {
    await this.prepareDataPropertiesToForm(data);
    await super.onEntitySaved(data);
  }

  async prepareDataPropertiesToForm(data: T | null) {

    return Promise.all(Object.keys(data.properties)
      .map(key => this.propertyDefinitionsByKey[key])
      .filter(option => option && option.type === 'entity')
      .map(option => {
        let value = data.properties[option.key];
        const filter = {...option.autocomplete.filter};
        const joinAttribute = option.autocomplete.filter.joinAttribute || 'id';
        if (joinAttribute == 'id') {
          filter.id = parseInt(value);
          value = '*';
        }
        else {
          filter.searchAttribute = joinAttribute;
        }
        // Fetch entity, as a referential
        return this.referentialRefService.suggest(value, filter)
          .then(matches => {
            data.properties[option.key] = (matches && matches[0] || {id: value,  label: '??'}) as any;
          })
          // Cannot ch: display an error
          .catch(err => {
            console.error('Cannot fetch entity, from option: ' + option.key + '=' + value, err);
            data.properties[option.key] = ({id: value,  label: '??'}) as any;
          });
    }));
  }

  protected setValue(data: T) {
    if (!data) return; // Skip

    const json = data.asObject();

    // Transform properties map into array
    json.properties = EntityUtils.getMapAsArray(data.properties || {});
    this.propertiesFormHelper.resize(Math.max(json.properties.length, 1));

    this.form.patchValue(json, {emitEvent: false});

    this.markAsPristine();
  }

  protected async getJsonValueToSave(): Promise<any> {
    const json = await super.getJsonValueToSave();

    // Re add label, because missing when field disable
    json.label = this.form.get('label').value;

    // Convert entities to id
    json.properties.forEach(property => {
      const option = this.propertyDefinitionsByKey[property.key];
      if (option && option.type === 'entity' && EntityUtils.isNotEmpty(property.value, 'id')) {
        property.value = property.value.id;
      }
    });

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

