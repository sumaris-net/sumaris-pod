import {ChangeDetectionStrategy, Component, Injector, Input, OnInit, ViewChild} from "@angular/core";
import {
  AccountService,
  AppEditorPage,
  AppFormUtils,
  EntityUtils, FormArrayHelper,
  isNil,
  LocalSettingsService, Person, StatusIds
} from "../../core/core.module";
import {AggregationStrata, AggregationType, ExtractionColumn, ExtractionUtils} from "../services/extraction.model";
import {AbstractControl, FormArray, FormBuilder, FormGroup} from "@angular/forms";
import {AggregationTypeValidatorService} from "../services/validator/aggregation-type.validator";
import {ExtractionService} from "../services/extraction.service";
import {ReferentialForm, StatusValue} from "../../referential/form/referential.form";
import {Router} from "@angular/router";
import {ValidatorService} from "angular4-material-table";
import {ProgramValidatorService} from "../../referential/services/validator/program.validator";
import {Program} from "../../referential/services/model";
import {EditorDataServiceLoadOptions} from "../../shared/services/data-service.class";
import {BehaviorSubject} from "rxjs";
import {arraySize} from "../../shared/functions";
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

  $timeColumns = new BehaviorSubject<ExtractionColumn[]>(undefined);
  $spaceColumns = new BehaviorSubject<ExtractionColumn[]>(undefined);
  $aggColumns = new BehaviorSubject<ExtractionColumn[]>(undefined);
  $techColumns = new BehaviorSubject<ExtractionColumn[]>(undefined);
  aggFunctions = [
    {
      value: 'SUM',
      name: 'EXTRACTION.AGGREGATION.EDIT.AGG_FUNCTION.SUM'
    },
    {
      value: 'AVG',
      name: 'EXTRACTION.AGGREGATION.EDIT.AGG_FUNCTION.AVG'
    }
  ];

  stratumHelper: FormArrayHelper<AggregationStrata>;

  stratumFormArray: FormArray;

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
      const columns = await this.extractionService.loadColumns(data);

      const map = ExtractionUtils.dispatchColumns(columns);
      console.debug('[aggregation-type] Columns repartition:', map);

      this.$timeColumns.next(map.timeColumns);
      this.$spaceColumns.next(map.spaceColumns);
      this.$aggColumns.next(map.aggColumns);
      this.$techColumns.next(map.techColumns);
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
