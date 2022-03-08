import {ChangeDetectionStrategy, Component, Injector, OnInit, ViewChild} from '@angular/core';
import {ValidatorService} from '@e-is/ngx-material-table';
import {AbstractControl, FormGroup} from '@angular/forms';
import {ReferentialForm} from '../form/referential.form';
import {ParameterValidatorService} from '../services/validator/parameter.validator';
import {
  AccountService,
  AppEntityEditor,
  EntityServiceLoadOptions,
  fadeInOutAnimation,
  FormFieldDefinitionMap,
  HistoryPageReference,
  isNil, isNotNil,
  joinPropertiesPath,
  MatAutocompleteFieldConfig,
  referentialToString,
  ReferentialUtils
} from '@sumaris-net/ngx-components';
import {ReferentialRefService} from '../services/referential-ref.service';
import {TaxonName} from '../services/model/taxon-name.model';
import {TaxonNameService} from '../services/taxon-name.service';
import {TaxonNameValidatorService} from '../services/validator/taxon-name.validator';
import {environment} from '@environments/environment';
import { WeightLengthConversionTable } from '@app/referential/taxon/weight-length-conversion.table';

@Component({
  selector: 'app-taxon-name',
  templateUrl: 'taxon-name.page.html',
  styleUrls: ['taxon-name.page.scss'],
  providers: [
    {
      provide: ValidatorService,
      useExisting: ParameterValidatorService
    }
  ],
  animations: [fadeInOutAnimation],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class TaxonNamePage extends AppEntityEditor<TaxonName> {

  canEdit: boolean;
  mobile: boolean;
  form: FormGroup;
  fieldDefinitions: FormFieldDefinitionMap;

  get useExistingReferenceTaxon(): boolean {
    return this.form.controls.useExistingReferenceTaxon.value;
  }

  @ViewChild('referentialForm', {static: true}) referentialForm: ReferentialForm;

  @ViewChild('weightLengthConversionTable', {static: true}) weightLengthConversionTable: WeightLengthConversionTable;

  constructor(
    protected injector: Injector,
    protected accountService: AccountService,
    protected validatorService: TaxonNameValidatorService,
    protected TaxonNameService: TaxonNameService,
    protected referentialRefService: ReferentialRefService
  ) {
    super(injector,
      TaxonName,
      TaxonNameService,
      {
        tabCount: 2
      }
    );
    this.form = validatorService.getFormGroup();
    this.mobile = this.settings.mobile;

    // default values
    this.defaultBackHref = "/referential/list?entity=TaxonName";

    this.debug = !environment.production;

  }

  ngOnInit() {
    super.ngOnInit();

    // Set entity name (required for referential form validator)
    this.referentialForm.entityName = 'TaxonName';

    const autocompleteConfig: MatAutocompleteFieldConfig = {
      suggestFn: (value, opts) => this.referentialRefService.suggest(value, opts),
      displayWith: (value) => value && joinPropertiesPath(value, ['label', 'name']),
      attributes: ['label', 'name'],
      columnSizes: [6, 6]
    };

    this.fieldDefinitions = {
      parentTaxonName: {
        key: `parentTaxonName`,
        label: `REFERENTIAL.TAXON_NAME.PARENT`,
        type: 'entity',
        autocomplete: {
          ...autocompleteConfig,
          filter: {entityName: 'TaxonName', statusIds: [0, 1]}
        }
      },
      taxonomicLevel: {
        key: `taxonomicLevel`,
        label: `REFERENTIAL.TAXON_NAME.TAXONOMIC_LEVEL`,
        type: 'entity',
        autocomplete: {
          ...autocompleteConfig,
          filter: {entityName: 'TaxonomicLevel'}
        }
      },
      isReferent: {
        key: `isReferent`,
        label: `REFERENTIAL.TAXON_NAME.IS_REFERENT`,
        type: 'boolean'
      },
      isNaming: {
        key: `isNaming`,
        label: `REFERENTIAL.TAXON_NAME.IS_NAMING`,
        type: 'boolean'
      },
      isVirtual: {
        key: `isReferent`,
        label: `REFERENTIAL.TAXON_NAME.IS_VIRTUAL`,
        type: 'boolean'
      }
    };
  }

  enable() {
    //When reload after save new Taxon name, super.enable() set referenceTaxonId to null, that why we save the value before.
    const referenceTaxonId = this.form.get('referenceTaxonId').value;
    super.enable();

    if (!this.isNewData) {
      this.form.get('label').disable();
      this.form.get('referenceTaxonId').setValue(referenceTaxonId);
      this.form.get('referenceTaxonId').disable();
    }
  }

  canUserWrite(data: TaxonName, opts?: any): boolean {
    return this.accountService.isAdmin();
  }

  /* -- protected methods -- */

  protected registerForms() {
    this.addChildForms([
      this.referentialForm,
      this.weightLengthConversionTable
    ]);
  }

  protected setValue(data: TaxonName) {
    if (!data) return; // Skip

    const json = data.asObject();

    this.form.patchValue(json, {emitEvent: false});

    // Set filter
    if (isNotNil(json.referenceTaxonId)) {
      this.weightLengthConversionTable.setFilter({
        referenceTaxonId: json.referenceTaxonId
      });
      this.weightLengthConversionTable.markAsReady();
    }

    this.markAsPristine();
  }

  protected async getValue(): Promise<TaxonName> {
    const data = await super.getValue();

    // Re add label, because missing when field disable
    data.label = this.form.get('label').value;
    data.label = data.label && data.label.toUpperCase();

    data.referenceTaxonId = this.form.get('referenceTaxonId').value;

    return data;
  }

  protected computeTitle(data: TaxonName): Promise<string> {
    // new data
    if (!data || isNil(data.id)) {
      return this.translate.get('REFERENTIAL.TAXON_NAME.NEW.TITLE').toPromise();
    }

    // Existing data
    return this.translate.get('REFERENTIAL.TAXON_NAME.EDIT.TITLE', data).toPromise();
  }

  protected async computePageHistory(title: string): Promise<HistoryPageReference> {
    return {
      ...(await super.computePageHistory(title)),
      title: `${this.data.label} - ${this.data.name}`,
      subtitle: 'REFERENTIAL.ENTITY.TAXON_NAME',
      icon: 'list'
    };
  }

  protected getFirstInvalidTabIndex(): number {
    if (this.referentialForm.invalid) return 0;
    return -1;
  }

  protected async onNewEntity(data: TaxonName, options?: EntityServiceLoadOptions): Promise<void> {
    await super.onNewEntity(data, options);

    // Check label is unique
    this.form.get('label')
      .setAsyncValidators(async (control: AbstractControl) => {
        const label = control.enabled && control.value;
        return label && (await this.TaxonNameService.existsByLabel(label, {excludedId: this.data.id})) ? {unique: true} : null;
      });

    // Check Reference Taxon exists
    this.form.get('referenceTaxonId')
      .setAsyncValidators(async (control: AbstractControl) => {
        const useExistingReferenceTaxon = this.form.get('useExistingReferenceTaxon').value;
        if (this.isNewData && useExistingReferenceTaxon) {
          const referenceTaxon = control.enabled && control.value;
          if (!referenceTaxon) {
            return {required: true};
          } else if (!await this.TaxonNameService.referenceTaxonExists(referenceTaxon)) {
            return {not_exist: true};
          }
        }
        return null;
      });

    this.form.get('useExistingReferenceTaxon')
      .setAsyncValidators(async (control: AbstractControl) => {
        const useExistingReferenceTaxon = this.form.controls['useExistingReferenceTaxon'].value;
        if (useExistingReferenceTaxon) {
          this.form.get('referenceTaxonId').updateValueAndValidity();
        } else {
          this.form.get('referenceTaxonId').setValue(null);
        }
        return null;
      });

    this.markAsReady();
  }

  protected async onEntityLoaded(data: TaxonName, options?: EntityServiceLoadOptions): Promise<void> {
    await super.onEntityLoaded(data, options);

    this.markAsReady();
  }

  referentialToString = referentialToString;

  protected markForCheck() {
    this.cd.markForCheck();
  }
}

