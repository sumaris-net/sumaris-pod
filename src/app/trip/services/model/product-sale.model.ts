import {EntityUtils, ReferentialRef} from "../../../core/services/model";
import {isNil, PmfmStrategy} from "../../../referential/services/model";
import {MeasurementFormValues, MeasurementValuesUtils} from "./measurement.model";
import {Product} from "./product.model";
import {isNotNilOrNaN} from "../../../shared/functions";

export class ProductSale {

  id: number;
  rankOrder: number;
  saleType: ReferentialRef;
  number: number;
  ratio: number;
  ratioCalculated: boolean;
  weight: number;
  weightCalculated: boolean;
  averageWeightPrice: number;
  averageWeightPriceCalculated: boolean;
  averagePackagingPrice: number;
  averagePackagingPriceCalculated: boolean;
  totalPrice: number;
  totalPriceCalculated: boolean;

  public static isSaleOfProduct(product: Product, sale: Product, pmfms: PmfmStrategy[]): boolean {
    return product && sale
      && product.taxonGroup && sale.taxonGroup && product.taxonGroup.equals(sale.taxonGroup)
      && product.measurementValues && sale.measurementValues
      && MeasurementValuesUtils.getValue(product.measurementValues, pmfms, 'PACKAGING') === MeasurementValuesUtils.getValue(sale.measurementValues, pmfms, 'PACKAGING')
      && MeasurementValuesUtils.getValue(product.measurementValues, pmfms, 'SIZE_CATEGORY') === MeasurementValuesUtils.getValue(sale.measurementValues, pmfms, 'SIZE_CATEGORY')
      ;
  }

  static toProductSale(product: Product, pmfms: PmfmStrategy[]): ProductSale {
    const result = new ProductSale;

    result.id = product.id;
    result.saleType = product.saleType;
    result.number = product.individualCount;
    result.weight = product.weight;
    result.weightCalculated = product.weightCalculated;
    if (product.measurementValues && pmfms) {
      const ratio = MeasurementValuesUtils.getValue(product.measurementValues, pmfms, 'SALE_RATIO');
      result.ratio = isNotNilOrNaN(ratio) ? +ratio : undefined;
      result.ratioCalculated = !result.ratio;
      const averageWeightPrice = MeasurementValuesUtils.getValue(product.measurementValues, pmfms, 'AVERAGE_PRICE_WEI');
      result.averageWeightPrice = isNotNilOrNaN(averageWeightPrice) ? +averageWeightPrice : undefined;
      result.averageWeightPriceCalculated = !result.averageWeightPrice;
      const averagePackagingPrice = MeasurementValuesUtils.getValue(product.measurementValues, pmfms, 'AVERAGE_PACKAGING_PRICE');
      result.averagePackagingPrice = isNotNilOrNaN(averagePackagingPrice) ? +averagePackagingPrice : undefined;
      result.averagePackagingPriceCalculated = !result.averagePackagingPrice;
      const totalPrice = MeasurementValuesUtils.getValue(product.measurementValues, pmfms, 'TOTAL_PRICE');
      result.totalPrice = isNotNilOrNaN(totalPrice) ? +totalPrice : undefined;
      result.totalPriceCalculated = !result.totalPrice;
    }

    return result;
  }

  asObject(pmfms: PmfmStrategy[]): any {
    const target: any = {...this};

    const measurementValues: MeasurementFormValues = {};
    if (this.ratio)
      MeasurementValuesUtils.setValue(measurementValues, pmfms, 'SALE_RATIO', this.ratio);


    target.measurementValues = measurementValues;
    return target;
  }
}

export class ProductSaleUtils {

  static isProductSaleEmpty(productSale: ProductSale): boolean {
    return !productSale || isNil(productSale.saleType);
  }

  static isProductSaleEquals(productSale1: ProductSale, productSale2: ProductSale): boolean {
    return (productSale1 === productSale2) || (isNil(productSale1) && isNil(productSale2)) || (
      productSale1 && productSale2 && EntityUtils.equals(productSale1.saleType, productSale2.saleType)
      && productSale1.rankOrder === productSale2.rankOrder
    );
  }

}

