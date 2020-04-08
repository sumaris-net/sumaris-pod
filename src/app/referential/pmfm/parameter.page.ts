import {ChangeDetectionStrategy, Component, Injector, OnInit, ViewChild} from "@angular/core";
import {ValidatorService} from "angular4-material-table";
import {AbstractControl, FormGroup} from "@angular/forms";
import {AppEditorPage, EntityUtils, environment, isNil} from "../../core/core.module";
import {referentialToString} from "../services/model";
import {ReferentialForm} from "../form/referential.form";
import {ParameterValidatorService} from "../services/validator/parameter.validator";
import {EditorDataServiceLoadOptions, fadeInOutAnimation} from "../../shared/shared.module";
import {AccountService} from "../../core/services/account.service";
import {Parameter} from "../services/model/pmfm.model";
import {ReferentialService} from "../services/referential.service";
import {ParameterService} from "../services/parameter.service";
import {FormFieldDefinitionMap} from "../../shared/form/field.model";
import {ReferentialRefService} from "../services/referential-ref.service";
import {ReferentialTable} from "../list/referential.table";

@Component({
  selector: 'app-parameter',
  templateUrl: 'parameter.page.html',
  providers: [
    {provide: ValidatorService, useExisting: ParameterValidatorService}
  ],
  animations: [fadeInOutAnimation],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ParameterPage extends AppEditorPage<Parameter> implements OnInit {

  canEdit: boolean;
  form: FormGroup;
  fieldDefinitions: FormFieldDefinitionMap;

  get type(): string {
    return this.form.controls.type.value;
  }

  get isQualitative(): boolean {
    return this.type === 'qualitative_value';
  }

  @ViewChild('referentialForm', { static: true }) referentialForm: ReferentialForm;
  @ViewChild('qualitativeValuesTable', { static: true }) qualitativeValuesTable: ReferentialTable;

  constructor(
    protected injector: Injector,
    protected accountService: AccountService,
    protected validatorService: ParameterValidatorService,
    protected parameterService: ParameterService,
    protected referentialService: ReferentialService,
    protected referentialRefService: ReferentialRefService
  ) {
    super(injector,
      Parameter,
      parameterService);
    this.form = validatorService.getFormGroup();

    // default values
    this.defaultBackHref = "/referential/list?entity=Parameter";
    this.canEdit = this.accountService.isAdmin();
    this.hasManyTabs = false;

    this.debug = !environment.production;

    this.fieldDefinitions = {
      type: {
        key: `type`,
        label: `REFERENTIAL.PARAMETER.TYPE`,
        type: 'enum',
        required: true,
        values: ['double', 'string', 'qualitative_value', 'date', 'boolean']
          .map(key => {
            return {key, value: ('REFERENTIAL.PARAMETER.TYPE_ENUM.' + key.toUpperCase()) };
          })
      }
    };
  }
  ngOnInit() {
    super.ngOnInit();

    // Set entity name (required for referential form validator)
    this.referentialForm.entityName = 'Parameter';

    // Check label is unique
    this.form.get('label')
      .setAsyncValidators(async (control: AbstractControl) => {
        const label = control.enabled && control.value;
        return label && (await this.parameterService.existsByLabel(label, {excludedId: this.data && this.data.id})) ?
          {unique: true} : null;
      });

  }

  /* -- protected methods -- */

  protected canUserWrite(data: Parameter): boolean {
    return (this.isNewData && this.accountService.isAdmin())
      || (EntityUtils.isNotEmpty(data) && this.accountService.isSupervisor());

  }

  enable() {
    super.enable();

    if (!this.isNewData) {
      this.form.get('label').disable();
    }
  }

  protected registerFormsAndTables() {
    this.registerTable(this.qualitativeValuesTable)
      .registerForm(this.referentialForm);
  }

  protected setValue(data: Parameter) {
    if (!data) return; // Skip

    const json = data.asObject();
    json.qualitativeValues = json.qualitativeValues || []; // Make sure to it array

    this.form.patchValue(json, {emitEvent: false});

    // QualitativeValues
    this.qualitativeValuesTable.value = data.qualitativeValues && data.qualitativeValues.slice() || []; // force update

    this.markAsPristine();
  }

  protected async getValue(): Promise<Parameter> {
    const data = await super.getValue();

    // Re add label, because missing when field disable
    data.label = this.form.get('label').value;
    data.label = data.label && data.label.toUpperCase();

    await this.qualitativeValuesTable.save();
    data.qualitativeValues = this.qualitativeValuesTable.value;

    return data;
  }

  protected computeTitle(data: Parameter): Promise<string> {
    // new data
    if (!data || isNil(data.id)) {
      return this.translate.get('REFERENTIAL.PARAMETER.NEW.TITLE').toPromise();
    }

    // Existing data
    return this.translate.get('REFERENTIAL.PARAMETER.EDIT.TITLE', data).toPromise();
  }

  protected getFirstInvalidTabIndex(): number {
    if (this.referentialForm.invalid) return 0;
    if (this.isQualitative && this.qualitativeValuesTable.invalid) return 1;
    return 0;
  }

  protected async onEntityLoaded(data: Parameter, options?: EditorDataServiceLoadOptions): Promise<void> {
    await super.onEntityLoaded(data, options);

    this.canEdit = this.canUserWrite(data);
  }

  referentialToString = referentialToString;

  protected markForCheck() {
    this.cd.markForCheck();
  }

}

