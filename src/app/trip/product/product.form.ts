import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnInit} from '@angular/core';
import {Moment} from 'moment';
import {DateAdapter} from '@angular/material/core';
import {IReferentialRef, isNotNil, LoadResult, LocalSettingsService, referentialToString} from '@sumaris-net/ngx-components';
import {FormBuilder, FormGroup} from '@angular/forms';
import {BehaviorSubject} from 'rxjs';
import {MeasurementValuesForm} from '@app/trip/measurement/measurement-values.form.class';
import {ProgramRefService} from '@app/referential/services/program-ref.service';
import {MeasurementsValidatorService} from '@app/trip/services/validator/measurement.validator';
import {filter, first} from 'rxjs/operators';
import {AcquisitionLevelCodes} from '@app/referential/services/model/model.enum';
import {environment} from '@environments/environment';
import {IWithProductsEntity, Product} from '@app/trip/services/model/product.model';
import {ProductValidatorService} from '@app/trip/services/validator/product.validator';


@Component({
  selector: 'app-product-form',
  templateUrl: './product.form.html',
  styleUrls: ['./product.form.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ProductForm extends MeasurementValuesForm<Product> implements OnInit {

  protected $initialized = new BehaviorSubject<boolean>(false);
  displayAttributes: {
    [key: string]: string[]
  };

  mobile: boolean;

  @Input() tabindex: number;
  @Input() showComment = false;
  @Input() showError = true;
  @Input() parents: IWithProductsEntity<any>[];
  @Input() parentAttributes: string[];


  constructor(
    protected dateAdapter: DateAdapter<Moment>,
    protected measurementValidatorService: MeasurementsValidatorService,
    protected formBuilder: FormBuilder,
    protected programRefService: ProgramRefService,
    protected validatorService: ProductValidatorService,
    protected settings: LocalSettingsService,
    protected cd: ChangeDetectorRef
  ) {
    super(dateAdapter, measurementValidatorService, formBuilder, programRefService, settings, cd,
      validatorService.getFormGroup(null, {
        withMeasurements: false
      }),
      {
        onUpdateFormGroup: (form) => this.onUpdateFormGroup(form)
      }
    );

    // Set default acquisition level
    this._acquisitionLevel = AcquisitionLevelCodes.PRODUCT;

    this.debug = !environment.production;
  };

  ngOnInit() {
    super.ngOnInit();

    // Default values
    this.tabindex = isNotNil(this.tabindex) ? this.tabindex : 1;

    this.registerAutocompleteField('parent', {
      items: this.parents,
      attributes: this.parentAttributes,
      columnNames: ['RANK_ORDER', 'REFERENTIAL.LABEL', 'REFERENTIAL.NAME'],
      columnSizes: this.parentAttributes.map(attr => attr === 'metier.label' ? 3 : (attr === 'rankOrderOnPeriod' ? 1 : undefined)),
    });

    const taxonGroupAttributes = this.settings.getFieldDisplayAttributes('taxonGroup');
    this.registerAutocompleteField('taxonGroup', {
      suggestFn: (value: any, options?: any) => this.suggestTaxonGroups(value, options),
      columnSizes: taxonGroupAttributes.map(attr => attr === 'label' ? 3 : undefined)
    });

  }

  setValue(data: Product, opts?: { emitEvent?: boolean; onlySelf?: boolean; normalizeEntityToForm?: boolean; }) {
    super.setValue(data, opts);

    // This will cause update controls
    this.$initialized.next(true);
  }

  protected async onUpdateFormGroup(form?: FormGroup): Promise<void> {
    form = form || this.form;

    // Wait end of ngInit()
    await this.onInitialized();

    // Add pmfms to form
    const measFormGroup = form.get('measurementValuesForm') as FormGroup;
    if (measFormGroup) {
      this.measurementValidatorService.updateFormGroup(measFormGroup, {pmfms: this.$pmfms.getValue()});
    }
  }

  /* -- protected methods -- */

  protected async onInitialized(): Promise<void> {
    // Wait end of setValue()
    if (this.$initialized.getValue() !== true) {
      await this.$initialized
        .pipe(
          filter((initialized) => initialized === true),
          first()
        ).toPromise();
    }
  }

  protected async suggestTaxonGroups(value: any, options?: any): Promise<LoadResult<IReferentialRef>> {
    return this.programRefService.suggestTaxonGroups(value,
      {
        program: this.programLabel,
        searchAttribute: options && options.searchAttribute
      });
  }

  protected markForCheck() {
    this.cd.markForCheck();
  }

  referentialToString = referentialToString;
}
