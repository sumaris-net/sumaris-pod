import {ChangeDetectionStrategy, Component, Injector, OnInit, ViewChild} from "@angular/core";
import {
  AccountService,
  AppEditorPage,
  EntityUtils,
  FormArrayHelper,
  isNil,
  LocalSettingsService
} from "../../core/core.module";
import {AggregationStrata, AggregationType, ExtractionColumn, ExtractionUtils} from "../services/extraction.model";
import {FormArray, FormBuilder, FormGroup} from "@angular/forms";
import {AggregationTypeValidatorService} from "../services/validator/aggregation-type.validator";
import {ExtractionService} from "../services/extraction.service";
import {Router} from "@angular/router";
import {ValidatorService} from "angular4-material-table";
import {EditorDataServiceLoadOptions} from "../../shared/services/data-service.class";
import {BehaviorSubject} from "rxjs";
import {AggregationTypeForm} from "./aggregation-type.form";

@Component({
  selector: 'app-aggregation-type-page',
  templateUrl: './aggregation-type.page.html',
  providers: [
    {provide: ValidatorService, useExisting: AggregationTypeValidatorService}
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AggregationTypePage extends AppEditorPage<AggregationType> implements OnInit {

  columns: ExtractionColumn[];

  @ViewChild('typeForm') typeForm: AggregationTypeForm;


  get form(): FormGroup {
    return this.typeForm.form;
  }

  constructor(protected injector: Injector,
              protected router: Router,
              protected formBuilder: FormBuilder,
              protected extractionService: ExtractionService,
              protected accountService: AccountService,
              protected validatorService: AggregationTypeValidatorService,
              protected settings: LocalSettingsService) {
    super(injector,
      AggregationType,
      {
        load: (id: number, options) => extractionService.loadAggregationType(id, options),
        delete: (type, options) => extractionService.deleteAggregations([type]),
        save: (type, options) => extractionService.saveAggregation(type),
        listenChanges: (id, options) => undefined
      });
    this.idAttribute = 'aggregationTypeId';
  }

  ngOnInit() {
    super.ngOnInit();

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
    this.typeForm.value = json;
  }

  protected async getValue(): Promise<AggregationType> {
    const data = await super.getValue();

    // Re add label, because missing when field disable
    data.label = this.form.get('label').value;

    // Re add columns
    data.columns = this.columns;

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
    this.registerForm(this.typeForm);
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

    // If spatial, load columns
    if (data.isSpatial) {
      this.columns = await this.extractionService.loadColumns(data) || [];

      const map = ExtractionUtils.dispatchColumns(this.columns);
      console.debug('[aggregation-type] Columns repartition:', map);

      this.typeForm.$timeColumns.next(map.timeColumns);
      this.typeForm.$spaceColumns.next(map.spaceColumns);
      this.typeForm.$aggColumns.next(map.aggColumns);
      this.typeForm.$techColumns.next(map.techColumns);
    }

    // Define default back link
    this.defaultBackHref = '/extraction?category=product&label=' + data.label;
  }

  protected async onEntityDeleted(data: AggregationType): Promise<void> {
    super.onEntityDeleted(data);

    // Change back href
    this.defaultBackHref = '/extraction';
  }
}
