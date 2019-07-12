import {Component, Injector, Input, OnInit, ViewChild} from "@angular/core";
import {ValidatorService} from "angular4-material-table";
import {AbstractControl, FormArray, FormBuilder, FormGroup} from "@angular/forms";
import {AppEditorPage, EntityUtils, environment, FormArrayHelper, isNil, AccountService} from "../../core/core.module";
import {Program, ProgramProperties, referentialToString} from "../services/model";
import {ProgramService} from "../services/program.service";
import {ReferentialForm} from "../form/referential.form";
import {ProgramValidatorService} from "../services/validator/program.validator";
import {ConfigOption} from "../../core/services/model";
import {StrategiesTable} from "./strategies.table";

@Component({
  selector: 'app-program',
  templateUrl: 'program.page.html',
  providers: [
    {provide: ValidatorService, useClass: ProgramValidatorService}
  ],
})
export class ProgramPage extends AppEditorPage<Program> implements OnInit {

  private _propertyOptionsCache: { [index: number]: ConfigOption } = {};

  options = Object.getOwnPropertyNames(ProgramProperties).map(name => ProgramProperties[name]);
  optionMap: { [key: string]: ConfigOption };
  propertiesFormHelper: FormArrayHelper<{ key: string; value: string }>;

  canEdit: boolean;

  @Input()
  form: FormGroup;

  @ViewChild('referentialForm') referentialForm: ReferentialForm;
  @ViewChild('strategiesTable') strategiesTable: StrategiesTable;


  get propertiesForm(): FormArray {
    return this.form.get('properties') as FormArray;
  }

  constructor(
    protected injector: Injector,
    protected accountService: AccountService,
    protected validatorService: ProgramValidatorService,
    protected programService: ProgramService
  ) {
    super(injector,
      Program,
      programService);
    this.form = validatorService.getRowValidator();
    this.propertiesFormHelper = new FormArrayHelper<{ key: string; value: string }>(
      injector.get(FormBuilder),
      this.form,
      'properties',
      (value) => validatorService.getPropertyFormGroup(value),
      (v1, v2) => (!v1 && !v2) || v1.key === v2.key,
      (value) => isNil(value) || (isNil(value.key) && isNil(value.value))
    );
    this.defaultBackHref = "/referential/list?entity=Program";
    this.canEdit = this.accountService.isSupervisor();

    // Fill options map
    this.optionMap = {};
    this.options.forEach(o => {
      this.optionMap[o.key] = o;
    });

    this.debug = !environment.production;
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
  }

  getPropertyOption(index: number): ConfigOption {
    let option = this._propertyOptionsCache[index];
    if (!option) {
      option = this.updatePropertyOption(index);
      this._propertyOptionsCache[index] = option;
    }
    return option;
  }

  updatePropertyOption(index: number): ConfigOption {
    const optionKey = (this.propertiesForm.at(index) as FormGroup).controls.key.value;
    const option = optionKey && this.optionMap[optionKey] || null;
    return option;
  }

  /* -- protected methods -- */

  enable() {
    super.enable();

    if (!this.isNewData) {
      this.form.get('label').disable();
    }
  }

  protected registerFormsAndTables() {
    this.registerTable(this.strategiesTable)
      .registerForm(this.referentialForm);
  }

  protected setValue(data: Program) {
    if (!data) return; // Skip

    const json = data.asObject();

    // Transform properties map into array
    json.properties = EntityUtils.getObjectAsArray(json.properties);
    this.propertiesFormHelper.resize(Math.max(json.properties.length, 1));

    this.form.patchValue(json, {emitEvent: false});

    // strategies
    this.strategiesTable.value = data.strategies;

    this.markAsPristine();
  }

  protected async getValue(): Promise<Program> {
    const data = await super.getValue();

    // Re add label, because missing when field disable
    data.label = this.form.get('label').value;

    await this.strategiesTable.save();
    data.strategies = this.strategiesTable.value;

    return data;
  }

  protected async computeTitle(data: Program): Promise<string> {
    // new data
    if (!data || isNil(data.id)) {
      return await this.translate.get('PROGRAM.NEW.TITLE').toPromise();
    }

    // Existing data
    return await this.translate.get('PROGRAM.EDIT.TITLE', data).toPromise();
  }

  protected getFirstInvalidTabIndex(): number {
    if (this.referentialForm.invalid) return 0;
    return 0;
  }

  referentialToString = referentialToString;
}

