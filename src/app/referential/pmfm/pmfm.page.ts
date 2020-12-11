import {ChangeDetectionStrategy, Component, Inject, Injector, OnInit, ViewChild} from "@angular/core";
import {ValidatorService} from "@e-is/ngx-material-table";
import {AbstractControl, FormGroup} from "@angular/forms";
import {referentialToString, ReferentialUtils} from "../../core/services/model/referential.model";
import {ReferentialForm} from "../form/referential.form";
import {PmfmValidatorService} from "../services/validator/pmfm.validator";
import {AccountService} from "../../core/services/account.service";
import {Pmfm} from "../services/model/pmfm.model";
import {Parameter} from "../services/model/parameter.model";
import {PmfmService} from "../services/pmfm.service";
import {FormFieldDefinitionMap} from "../../shared/form/field.model";
import {ReferentialRefService} from "../services/referential-ref.service";
import {ParameterService} from "../services/parameter.service";
import {filter, mergeMap} from "rxjs/operators";
import {Observable} from "rxjs";
import {fadeInOutAnimation} from "../../shared/material/material.animations";
import {isNil, joinPropertiesPath} from "../../shared/functions";
import {EntityServiceLoadOptions} from "../../shared/services/entity-service.class";
import {AppEntityEditor} from "../../core/form/editor.class";
import {MatAutocompleteFieldConfig} from "../../shared/material/autocomplete/material.autocomplete";
import {EnvironmentService} from "../../../environments/environment.class";

@Component({
  selector: 'app-pmfm',
  templateUrl: 'pmfm.page.html',
  providers: [
    {provide: ValidatorService, useExisting: PmfmValidatorService}
  ],
  animations: [fadeInOutAnimation],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class PmfmPage extends AppEntityEditor<Pmfm> implements OnInit {

  canEdit: boolean;
  form: FormGroup;
  fieldDefinitions: FormFieldDefinitionMap;
  $parameter: Observable<Parameter>;

  get matrix(): any {
    return this.form.controls.matrix.value;
  }

  get hasMatrix(): boolean {
    return ReferentialUtils.isNotEmpty(this.matrix);
  }

  @ViewChild('referentialForm', { static: true }) referentialForm: ReferentialForm;
  //@ViewChild('strategiesTable', { static: true }) strategiesTable: StrategiesTable;

  constructor(
    protected injector: Injector,
    protected accountService: AccountService,
    protected validatorService: PmfmValidatorService,
    protected pmfmService: PmfmService,
    protected parameterService: ParameterService,
    protected referentialRefService: ReferentialRefService,
    @Inject(EnvironmentService) protected environment
  ) {
    super(injector,
      Pmfm,
      pmfmService);
    this.form = validatorService.getFormGroup();

    // default values
    this.defaultBackHref = "/referential/list?entity=Pmfm";
    this.canEdit = this.accountService.isAdmin();

    this.debug = !environment.production;


  }
  ngOnInit() {
    super.ngOnInit();

    // Set entity name (required for referential form validator)
    this.referentialForm.entityName = 'Pmfm';

    const autocompleteConfig: MatAutocompleteFieldConfig = {
      suggestFn: (value, opts) => this.referentialRefService.suggest(value, opts),
      displayWith: (value) => value && joinPropertiesPath(value, ['label', 'name']),
      attributes: ['label', 'name'],
      columnSizes: [6, 6]
    };
    this.fieldDefinitions = {

      parameter: {
        key: `parameter`,
        label: `REFERENTIAL.PMFM.PARAMETER`,
        type: 'entity',
        autocomplete: {
          ...autocompleteConfig,
          filter: {entityName: 'Parameter'}
        }
      },
      unit: {
        key: `unit`,
        label: `REFERENTIAL.PMFM.UNIT`,
        type: 'entity',
        autocomplete: {
          ...autocompleteConfig,
          attributes: ['label'],
          filter: {entityName: 'Unit'}
        }
      },

      // Numerical options
      minValue: {
        key: `minValue`,
        label: `REFERENTIAL.PMFM.MIN_VALUE`,
        type: 'double'
      },
      maxValue: {
        key: `maxValue`,
        label: `REFERENTIAL.PMFM.MAX_VALUE`,
        type: 'double'
      },
      defaultValue: {
        key: `defaultValue`,
        label: `REFERENTIAL.PMFM.DEFAULT_VALUE`,
        type: 'double'
      },
      maximumNumberDecimals: {
        key: `maximumNumberDecimals`,
        label: `REFERENTIAL.PMFM.MAXIMUM_NUMBER_DECIMALS`,
        type: 'integer',
        minValue: 0
      },
      signifFiguresNumber: {
        key: `signifFiguresNumber`,
        label: `REFERENTIAL.PMFM.SIGNIF_FIGURES_NUMBER`,
        type: 'integer',
        minValue: 0
      },
      matrix: {
        key: `matrix`,
        label: `REFERENTIAL.PMFM.MATRIX`,
        type: 'entity',
        autocomplete: {
          ...autocompleteConfig,
          filter: {entityName: 'Matrix'}
        }
      },
      fraction: {
        key: `fraction`,
        label: `REFERENTIAL.PMFM.FRACTION`,
        type: 'entity',
        autocomplete: {
          ...autocompleteConfig,
          filter: {entityName: 'Fraction', levelId: 1}
        }
      },
      method: {
        key: `method`,
        label: `REFERENTIAL.PMFM.METHOD`,
        type: 'entity',
        autocomplete: {
          ...autocompleteConfig,
          filter: {entityName: 'Method'}
        }
      }
    };

    // Check fraction
    this.form.get('fraction')
      .setAsyncValidators(async (control: AbstractControl) => {
        const value = control.enabled && control.value;
        return value && (!this.matrix || value.levelId !== this.matrix.id) ? {entity: true} : null;
      });

    // Check fraction
    this.$parameter = this.form.get('parameter').valueChanges
        .pipe(
          filter(ReferentialUtils.isNotEmpty),
          mergeMap(p => this.parameterService.load(p.id))
        );
  }

  async addNewParameter() {
    await this.router.navigateByUrl(
      '/referential/parameter/new'
    );
    return true;
  }

  /* -- protected methods -- */


  protected canUserWrite(data: Pmfm): boolean {
    // TODO : check user is in pmfm managers
    return (this.isNewData && this.accountService.isAdmin())
      || (ReferentialUtils.isNotEmpty(data) && this.accountService.isSupervisor());

  }

  enable() {
    super.enable();

    if (!this.isNewData) {
      this.form.get('label').disable();
    }
  }

  protected registerForms() {
    this // TODO QV .registerTable(this.strategiesTable)
      .addChildForm(this.referentialForm);
  }

  protected setValue(data: Pmfm) {
    if (!data) return; // Skip

    const json = data.asObject();
    json.entityName = Pmfm.TYPENAME;

    this.form.patchValue(json, {emitEvent: false});

    // qualitativeValues
    //this.qualitativeValuesTable.value = data.qualitativeValues.slice(); // force update

    this.markAsPristine();
  }

  protected async getValue(): Promise<Pmfm> {
    const data = await super.getValue();

    // Re add label, because missing when field disable
    data.label = this.form.get('label').value;
    data.label = data.label && data.label.toUpperCase();

    //await this.qualitativeValuesTable.save();
    //data.qualitativeValues = this.qualitativeValuesTable.value;

    return data;
  }

  protected computeTitle(data: Pmfm): Promise<string> {
    // new data
    if (!data || isNil(data.id)) {
      return this.translate.get('REFERENTIAL.PMFM.NEW.TITLE').toPromise();
    }

    // Existing data
    return this.translate.get('REFERENTIAL.PMFM.EDIT.TITLE', data).toPromise();
  }

  protected getFirstInvalidTabIndex(): number {
    if (this.referentialForm.invalid) return 0;
    return 0;
  }

  protected async onNewEntity(data: Pmfm, options?: EntityServiceLoadOptions): Promise<void> {
    await super.onNewEntity(data, options);

    // Check label is unique
    this.form.get('label')
      .setAsyncValidators(async (control: AbstractControl) => {
        const label = control.enabled && control.value;
        return label && (await this.pmfmService.existsByLabel(label, {excludedId: this.data.id})) ? {unique: true} : null;
      });
  }

  protected async onEntityLoaded(data: Pmfm, options?: EntityServiceLoadOptions): Promise<void> {
    await super.onEntityLoaded(data, options);

    this.canEdit = this.canUserWrite(data);
  }

  referentialToString = referentialToString;

  protected markForCheck() {
    this.cd.markForCheck();
  }

}

