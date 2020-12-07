import {ChangeDetectionStrategy, Component, Injector, OnInit, ViewChild} from "@angular/core";
import {AppEntityEditor, isNil} from "../../../core/core.module";
import {ExtractionColumn} from "../../services/model/extraction.model";
import {FormBuilder, FormGroup} from "@angular/forms";
import {AggregationTypeValidatorService} from "../../services/validator/aggregation-type.validator";
import {ExtractionService} from "../../services/extraction.service";
import {Router} from "@angular/router";
import {ValidatorService} from "@e-is/ngx-material-table";
import {EntityServiceLoadOptions} from "../../../shared/services/entity-service.class";
import {AggregationTypeForm} from "../form/aggregation-type.form";
import {AccountService} from "../../../core/services/account.service";
import {LocalSettingsService} from "../../../core/services/local-settings.service";
import {ReferentialUtils} from "../../../core/services/model/referential.model";
import {AggregationType} from "../../services/model/aggregation-type.model";
import {Alerts} from "../../../shared/alerts";
import {isEmptyArray} from "../../../shared/functions";
import {AggregationService} from "../../services/aggregation.service";

@Component({
  selector: 'app-aggregation-type-page',
  templateUrl: './aggregation-type.page.html',
  styleUrls: ['./aggregation-type.page.scss'],
  providers: [
    {provide: ValidatorService, useExisting: AggregationTypeValidatorService}
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AggregationTypePage extends AppEntityEditor<AggregationType> implements OnInit {

  columns: ExtractionColumn[];

  @ViewChild('typeForm', {static: true}) typeForm: AggregationTypeForm;

  get form(): FormGroup {
    return this.typeForm.form;
  }

  constructor(protected injector: Injector,
              protected router: Router,
              protected formBuilder: FormBuilder,
              protected extractionService: ExtractionService,
              protected aggregationService: AggregationService,
              protected accountService: AccountService,
              protected validatorService: AggregationTypeValidatorService,
              protected settings: LocalSettingsService) {
    super(injector,
      AggregationType,
      // Data service
      {
        load: (id: number, options) => aggregationService.load(id, options),
        delete: (type, _) => aggregationService.deleteAll([type]),
        save: (type, _) => aggregationService.save(type),
        listenChanges: (id, _) => undefined
      },
      // Editor options
      {
        pathIdAttribute: 'aggregationTypeId'
      });
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

  async openMap(event?: UIEvent) {
    if (this.dirty) {
      // Ask user confirmation
      const confirmation = await Alerts.askSaveBeforeLeave(this.alertCtrl, this.translate);
      if (confirmation) {
        await this.save(event);
      }
    }

    if (!this.data || isEmptyArray(this.data.stratum)) return; // Unable to load the map

    return setTimeout(() => {
      // open the map
      return this.router.navigate(['../../map'],
        {
          relativeTo: this.route,
          queryParams: {
            category: this.data.category,
            label: this.data.label,
            sheet: this.data.stratum[0].sheetName
          }
        });
    }, 200); // Add a delay need by matTooltip to be hide
  }

  /* -- protected -- */

  protected setValue(data: AggregationType) {
    // Apply data to form
    this.typeForm.value = data.asObject();
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

  protected registerForms() {
    this.addChildForm(this.typeForm);
  }

  protected canUserWrite(data: AggregationType): boolean {

    return this.accountService.isAdmin()
      // New date allow for supervisors
      || (this.isNewData && this.accountService.isSupervisor())
      // Supervisor on existing data, and the same recorder department
      || (ReferentialUtils.isNotEmpty(data && data.recorderDepartment) && this.accountService.canUserWriteDataForDepartment(data.recorderDepartment));
  }

  protected async onEntityLoaded(data: AggregationType, options?: EntityServiceLoadOptions): Promise<void> {
    await this.typeForm.updateLists(data);

    // Define default back link
    this.defaultBackHref = '/extraction?category=product&label=' + data.label;
  }

  protected async onEntityDeleted(data: AggregationType): Promise<void> {
    // Change back href
    this.defaultBackHref = '/extraction';
  }
}
