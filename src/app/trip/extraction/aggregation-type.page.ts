import {ChangeDetectionStrategy, Component, Injector, Input, OnInit, ViewChild} from "@angular/core";
import {
  AccountService,
  AppEditorPage,
  AppFormUtils,
  EntityUtils,
  isNil,
  LocalSettingsService, StatusIds
} from "../../core/core.module";
import {AggregationType} from "../services/extraction.model";
import {AbstractControl, FormGroup} from "@angular/forms";
import {ExtractionTypeValidatorService} from "../services/validator/extraction-type.validator";
import {ExtractionService} from "../services/extraction.service";
import {ReferentialForm, StatusValue} from "../../referential/form/referential.form";
import {Router} from "@angular/router";
import {ValidatorService} from "angular4-material-table";
import {ProgramValidatorService} from "../../referential/services/validator/program.validator";
import {Program} from "../../referential/services/model";
import {EditorDataServiceLoadOptions} from "../../shared/services/data-service.class";

@Component({
  selector: 'app-aggregation-type-page',
  templateUrl: './aggregation-type.page.html',
  providers: [
    {provide: ValidatorService, useExisting: ExtractionTypeValidatorService}
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AggregationTypePage extends AppEditorPage<AggregationType> implements OnInit {

  form: FormGroup;

  @ViewChild('referentialForm') referentialForm: ReferentialForm;

  constructor(protected injector: Injector,
              protected router: Router,
              protected extractionService: ExtractionService,
              protected accountService: AccountService,
              protected validatorService: ExtractionTypeValidatorService,
              protected settings: LocalSettingsService) {
    super(injector,
      AggregationType,
      {
        load: (id: number, options) => extractionService.loadAggregationType(id, options),
        delete: (type, options) => extractionService.deleteAggregations([type]),
        save: (type, options) => extractionService.saveAggregation(type),
        listenChanges: (id, options) => undefined
      });
    this.form = validatorService.getFormGroup();
    this.idAttribute = 'aggregationTypeId';
  }

  ngOnInit(): void {
    super.ngOnInit();

    // Set entity name (required for referential form validator)
    this.referentialForm.entityName = 'AggregationType';

    this.referentialForm.statusList = [
      {
        id: StatusIds.ENABLE,
        icon: 'eye',
        label: 'EXTRACTION.AGGREGATION.EDIT.STATUS_ENUM.PUBLIC'
      },
      {
        id: StatusIds.TEMPORARY,
        icon: 'eye-off',
        label: 'EXTRACTION.AGGREGATION.EDIT.STATUS_ENUM.PRIVATE'
      },
      {
        id: StatusIds.DISABLE,
        icon: 'close',
        label: 'EXTRACTION.AGGREGATION.EDIT.STATUS_ENUM.DISABLE'
      }
    ];

  }

  close(event: UIEvent) {

  }

  enable(opts?: { onlySelf?: boolean; emitEvent?: boolean }) {
    super.enable(opts);

    // Label always disable is saved
    if (!this.isNewData) {
      this.form.get('label').disable();
    }
  }

  /* -- protected -- */

  protected setValue(data: AggregationType) {

    const json = data.asObject();
    // Apply data to form
    this.form.patchValue(json, {emitEvent: false});
  }

  protected async getValue(): Promise<AggregationType> {
    const data = await super.getValue();

    // Re add label, because missing when field disable
    data.label = this.form.get('label').value;

    return data;
  }

  protected async computeTitle(data: AggregationType): Promise<string> {
    // new data
    if (!data || isNil(data.id)) {
      return await this.translate.get('EXTRACTION.AGGREGATION.NEW.TITLE').toPromise();
    }

    // Existing data
    return await this.translate.get('EXTRACTION.AGGREGATION.EDIT.TITLE', data).toPromise();
  }

  protected getFirstInvalidTabIndex(): number {
    return 0;
  }

  protected registerFormsAndTables() {
    this.registerForm(this.referentialForm)
  }

  protected canUserWrite(data: AggregationType): boolean {

    return this.accountService.isAdmin()
      // New date allow for supervisors
      || (this.isNewData && this.accountService.isSupervisor())
      // Supervisor on existing data, and the same recorder department
      || (EntityUtils.isNotEmpty(data && data.recorderDepartment) && this.accountService.canUserWriteDataForDepartment(data.recorderDepartment));
  }

  protected async onEntityLoaded(data: AggregationType, options?: EditorDataServiceLoadOptions): Promise<void> {
    super.onEntityLoaded(data, options);

    this.defaultBackHref = '/extraction?category=product&label=' + data.label;
  }

  protected async onEntityDeleted(data: AggregationType): Promise<void> {
    super.onEntityDeleted(data);

    // Change back href
    this.defaultBackHref = '/extraction';
  }
}
