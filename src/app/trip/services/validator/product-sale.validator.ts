import {Injectable} from "@angular/core";
import {ValidatorService} from "angular4-material-table";
import {FormBuilder, Validators} from "@angular/forms";
import {SharedValidators} from "../../../shared/validator/validators";
import {LocalSettingsService} from "../../../core/services/local-settings.service";
import {MeasurementsValidatorService} from "../measurement.validator";
import {Product} from "../model/product.model";
import {ProductValidatorOptions, ProductValidatorService} from "./product.validator";

@Injectable()
export class ProductSaleValidatorService
  extends ProductValidatorService implements ValidatorService {

  constructor(
    formBuilder: FormBuilder,
    settings: LocalSettingsService,
    measurementsValidatorService: MeasurementsValidatorService
  ) {
    super(formBuilder, settings, measurementsValidatorService);
  }

  getFormGroupConfig(data?: Product, opts?: ProductValidatorOptions): { [key: string]: any } {

    return Object.assign(
      super.getFormGroupConfig(data, opts),
      {
        saleType: [data && data.saleType || null, Validators.compose([Validators.required, SharedValidators.entity])]
      });
  }


}
