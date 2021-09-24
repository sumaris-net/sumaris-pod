import {ChangeDetectionStrategy, Component, Injector, OnInit, ViewChild} from "@angular/core";
import {ExtractionCategories, ExtractionColumn} from "../../services/model/extraction-type.model";
import {FormBuilder, FormGroup} from "@angular/forms";
import {AggregationTypeValidatorService} from "../../services/validator/aggregation-type.validator";
import {ExtractionService} from "../../services/extraction.service";
import {Router} from "@angular/router";
import {ValidatorService} from "@e-is/ngx-material-table";
import {EntityServiceLoadOptions} from "@sumaris-net/ngx-components";
import {ProductForm} from "../form/product.form";
import {AccountService}  from "@sumaris-net/ngx-components";
import {LocalSettingsService}  from "@sumaris-net/ngx-components";
import {ReferentialUtils}  from "@sumaris-net/ngx-components";
import {ExtractionProduct} from "../../services/model/extraction-product.model";
import {Alerts} from "@sumaris-net/ngx-components";
import {isEmptyArray, isNil} from "@sumaris-net/ngx-components";
import {ExtractionProductService} from "../../services/extraction-product.service";
import {AppEntityEditor}  from "@sumaris-net/ngx-components";

@Component({
  selector: 'app-product-page',
  templateUrl: './product.page.html',
  styleUrls: ['./product.page.scss'],
  providers: [
    {provide: ValidatorService, useExisting: AggregationTypeValidatorService}
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ProductPage extends AppEntityEditor<ExtractionProduct> {

  columns: ExtractionColumn[];

  @ViewChild('typeForm', {static: true}) typeForm: ProductForm;

  get form(): FormGroup {
    return this.typeForm.form;
  }

  constructor(protected injector: Injector,
              protected router: Router,
              protected formBuilder: FormBuilder,
              protected extractionService: ExtractionService,
              protected productService: ExtractionProductService,
              protected accountService: AccountService,
              protected validatorService: AggregationTypeValidatorService,
              protected settings: LocalSettingsService) {
    super(injector,
      ExtractionProduct,
      // Data service
      {
        load: (id: number, options) => productService.load(id, options),
        delete: (type, _) => productService.deleteAll([type]),
        save: (type, _) => productService.save(type),
        listenChanges: (id, _) => undefined
      },
      // Editor options
      {
        pathIdAttribute: 'productId'
      });
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

  protected setValue(data: ExtractionProduct) {
    // Apply data to form
    this.typeForm.value = data.asObject();
  }

  protected async getValue(): Promise<ExtractionProduct> {
    const data = await super.getValue();

    // Re add label, because missing when field disable
    data.label = this.form.get('label').value;

    // Re add columns
    data.columns = this.columns;

    return data;
  }

  protected async computeTitle(data: ExtractionProduct): Promise<string> {
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

  protected canUserWrite(data: ExtractionProduct): boolean {

    return this.accountService.isAdmin()
      // New date allow for supervisors
      || (this.isNewData && this.accountService.isSupervisor())
      // Supervisor on existing data, and the same recorder department
      || (ReferentialUtils.isNotEmpty(data && data.recorderDepartment) && this.accountService.canUserWriteDataForDepartment(data.recorderDepartment));
  }

  protected async onEntityLoaded(data: ExtractionProduct, options?: EntityServiceLoadOptions): Promise<void> {
    await this.typeForm.updateLists(data);

    // Define default back link
    this.defaultBackHref = `̀/extraction/data?category=${ExtractionCategories.PRODUCT}&label=${data.label}`;
  }

  protected async onEntityDeleted(data: ExtractionProduct): Promise<void> {
    // Change back href
    this.defaultBackHref = '/extraction/data';
  }
}
