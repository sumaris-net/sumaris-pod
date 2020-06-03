import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Injector, OnInit, ViewChild} from "@angular/core";
import {ActivatedRouteSnapshot} from "@angular/router";
import {AbstractControl, FormArray, FormBuilder, FormGroup} from "@angular/forms";
import {EntityUtils, Software} from '../../core/services/model';
import {FormArrayHelper} from "../../core/form/form.utils";
import {FormFieldDefinition, FormFieldDefinitionMap, FormFieldValue} from "../../shared/form/field.model";
import {PlatformService} from "../../core/services/platform.service";
import {AppEditorPage, isNil} from "../../core/core.module";
import {AccountService} from "../../core/services/account.service";
import {ReferentialForm} from "../form/referential.form";
import {SoftwareService} from "../services/software.service";
import {SoftwareValidatorService} from "../services/software.validator";
import {ConfigService} from "../../core/services/config.service";


@Component({
  selector: 'app-software-page',
  templateUrl: 'software.page.html',
  styleUrls: ['./software.page.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SoftwarePage<T extends Software<T> = Software<any>> extends AppEditorPage<Software<T>> implements OnInit {

  protected configService: ConfigService;

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

  constructor(
    protected injector: Injector,
    protected validatorService: SoftwareValidatorService,
      ) {
    super(injector,
      Software,
      injector.get(SoftwareService));
    this.configService = injector.get(ConfigService);
    this.platform = injector.get(PlatformService);
    this.accountService = injector.get(AccountService);
    this.cd = injector.get(ChangeDetectorRef);

    this.form = validatorService.getFormGroup();
    this.propertiesFormHelper = new FormArrayHelper<FormFieldValue>(
      injector.get(FormBuilder),
      this.form,
      'properties',
      (value) => validatorService.getPropertyFormGroup(value),
      (v1, v2) => (!v1 && !v2) || v1.key === v2.key,
      (value) => isNil(value) || (isNil(value.key) && isNil(value.value)),
      {
        allowEmptyArray: true
      }
    );

    // default values
    this.defaultBackHref = "/referential/list?entity=Software";

    //this.debug = !environment.production;
  }

  ngOnInit() {
    super.ngOnInit();

    // Set entity name (required for referential form validator)
    this.referentialForm.entityName = 'Software';

    // Check label is unique
    this.form.get('label')
      .setAsyncValidators(async (control: AbstractControl) => {
        const label = control.enabled && control.value;
        return label && (await this.configService.existsByLabel(label)) ? {unique: true} : null;
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

  protected registerFormsAndTables() {
    this.registerForm(this.referentialForm);
  }

  protected async loadFromRoute(route: ActivatedRouteSnapshot) {

    // Wait platform is ready
    await this.platform.ready();

    // Fill property definitions map
    this.propertyDefinitions = this.configService.optionDefs;
    this.propertyDefinitions.forEach(o => this.propertyDefinitionsByKey[o.key] = o);

    super.loadFromRoute(route);
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

