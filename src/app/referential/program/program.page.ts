import {ChangeDetectionStrategy, Component, Injector, OnInit, ViewChild} from "@angular/core";
import {TableElement, ValidatorService} from "@e-is/ngx-material-table";
import {AbstractControl, FormBuilder, FormGroup} from "@angular/forms";
import {Program} from "../services/model/program.model";
import {ProgramService} from "../services/program.service";
import {ReferentialForm} from "../form/referential.form";
import {ProgramValidatorService} from "../services/validator/program.validator";
import {StrategiesTable} from "../strategy/strategies.table";
import {AccountService} from "../../core/services/account.service";
import {ReferentialRef, referentialToString, ReferentialUtils} from "../../core/services/model/referential.model";
import {AppPropertiesForm} from "../../core/form/properties.form";
import {ReferentialRefFilter, ReferentialRefService} from "../services/referential-ref.service";
import {ModalController} from "@ionic/angular";
import {FormFieldDefinition, FormFieldDefinitionMap} from "../../shared/form/field.model";
import {ProgramProperties} from "../services/config/program.config";
import {AppEntityEditor} from "../../core/form/editor.class";
import {EntityServiceLoadOptions} from "../../shared/services/entity-service.class";
import {changeCaseToUnderscore, isNil} from "../../shared/functions";
import {EntityUtils} from "../../core/services/model/entity.model";
import {HistoryPageReference} from "../../core/services/model/history.model";
import {SelectReferentialModal} from "../list/select-referential.modal";
import {AppListForm} from "../../core/form/list.form";
import {environment} from "../../../environments/environment";

export enum AnimationState {
  ENTER = 'enter',
  LEAVE = 'leave'
}

@Component({
  selector: 'app-program',
  templateUrl: 'program.page.html',
  providers: [
    {provide: ValidatorService, useExisting: ProgramValidatorService}
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ProgramPage extends AppEntityEditor<Program, ProgramService> implements OnInit {

  propertyDefinitions = Object.getOwnPropertyNames(ProgramProperties).map(name => ProgramProperties[name]);
  fieldDefinitions: FormFieldDefinitionMap = {};
  form: FormGroup;
  i18nFieldPrefix = 'PROGRAM.';

  @ViewChild('referentialForm', { static: true }) referentialForm: ReferentialForm;
  @ViewChild('propertiesForm', { static: true }) propertiesForm: AppPropertiesForm;
  @ViewChild('strategiesTable', { static: true }) strategiesTable: StrategiesTable;
  @ViewChild('locationClassificationList', { static: true }) locationClassificationList: AppListForm;

  constructor(
    protected injector: Injector,
    protected formBuilder: FormBuilder,
    protected accountService: AccountService,
    protected validatorService: ProgramValidatorService,
    dataService: ProgramService,
    protected referentialRefService: ReferentialRefService,
    protected modalCtrl: ModalController
  ) {
    super(injector,
      Program,
      dataService, {
        autoOpenNextTab: false,
        pathIdAttribute: 'programId'
      });
    this.form = validatorService.getFormGroup();

    // default values
    this.defaultBackHref = "/referential/list?entity=Program";
    this._enabled = this.accountService.isAdmin();
    this.tabCount = 4;

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
        return label && (await this.service.existsByLabel(label)) ? {unique: true} : null;
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


  }

  /* -- protected methods -- */

  async load(id?: number, opts?: EntityServiceLoadOptions): Promise<void> {
    // Force the load from network
    return super.load(id, {...opts, fetchPolicy: "network-only"});
  }

  protected registerFormField(fieldName: string, def: Partial<FormFieldDefinition>) {
    const definition = <FormFieldDefinition>{
      key: fieldName,
      label: this.i18nFieldPrefix + changeCaseToUnderscore(fieldName).toUpperCase(),
      ...def
    };
    this.fieldDefinitions[fieldName] = definition;
  }

  protected canUserWrite(data: Program): boolean {
    // TODO : check user is in program managers
    return (this.isNewData && this.accountService.isAdmin())
      || (ReferentialUtils.isNotEmpty(data) && this.accountService.isSupervisor());

  }

  enable(opts?: {onlySelf?: boolean, emitEvent?: boolean; }) {
    super.enable(opts);

    this.locationClassificationList.enable(opts);

    if (!this.isNewData) {
      this.form.get('label').disable();
    }
  }

  protected registerForms() {
    this.addChildForms([
      this.referentialForm,
      this.propertiesForm,
      this.locationClassificationList,
      this.strategiesTable
    ]);
  }

  protected setValue(data: Program) {
    if (!data) return; // Skip

    this.form.patchValue({...data,
      properties: [],
      locationClassifications: [],
      strategies: []}, {emitEvent: false});

    // Program properties
    this.propertiesForm.value = EntityUtils.getMapAsArray(data.properties);

    // Location classification
    this.locationClassificationList.setValue(data.locationClassifications);

    // strategies
    this.strategiesTable.setProgram(data); // Will load strategies

    this.markAsPristine();
  }

  async save(event?: Event, options?: any): Promise<boolean> {
    //console.debug('TODO saving program...');
    return super.save(event, options);
  }

  protected async getJsonValueToSave(): Promise<any> {

    const data = await super.getJsonValueToSave();

    // Re add label, because missing when field disable
    data.label = this.form.get('label').value;
    data.properties = this.propertiesForm.value;

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

  protected async computePageHistory(title: string): Promise<HistoryPageReference> {
    return {
      ...(await super.computePageHistory(title)),
      icon: 'contract',
      title: `${this.data.label} - ${this.data.name}`,
      subtitle: 'REFERENTIAL.ENTITY.PROGRAM'
    };
  }

  protected getFirstInvalidTabIndex(): number {
    if (this.referentialForm.invalid) return 0;
    if (this.strategiesTable.invalid) return 1;
    if (this.propertiesForm.invalid) return 2;
    // TODO users rights
    return 0;
  }

  async addLocationClassification() {
    if (this.disabled) return; // Skip

    const items = await this.openSelectReferentialModal({
      filter: {
        entityName: 'LocationClassification'
      }
    });

    // Add to list
    (items || []).forEach(item => this.locationClassificationList.add(item));

    this.markForCheck();
  }

  async onOpenStrategy({id, row}: { id?: number; row: TableElement<any>; }) {
    const savedOrContinue = await this.saveIfDirtyAndConfirm();
    if (savedOrContinue) {
      this.markAsLoading();

      setTimeout(async () => {
        await this.router.navigate(['referential', 'program',  this.data.id, 'strategies', id], {
          queryParams: {}
        });
        this.markAsLoaded();
      });
    }
  }

  async onNewStrategy(event?: any) {
    const savedOrContinue = await this.saveIfDirtyAndConfirm();
    if (savedOrContinue) {
      this.markAsLoading();

      setTimeout(async () => {
        await this.router.navigate(['referential', 'program', this.data.id, 'strategies', 'new'], {
          queryParams: {}
        });
        this.markAsLoaded();
      });
    }
  }

  protected async openSelectReferentialModal(opts: {
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

  referentialToString = referentialToString;
  referentialEquals = ReferentialUtils.equals;

  protected markForCheck() {
    this.cd.markForCheck();
  }

}

