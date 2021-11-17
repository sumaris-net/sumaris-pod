import { DenormalizedPmfmStrategy } from '@app/referential/services/model/pmfm-strategy.model';
import { MeasurementValuesUtils } from './measurement.model';
import { isNil, isNotEmptyArray, isNotNil, isNotNilOrNaN, ObjectMap, ReferentialUtils, round } from '@sumaris-net/ngx-components';
import { DataEntityAsObjectOptions } from '@app/data/services/model/data-entity.model';
import { Product } from './product.model';
import { Packet, PacketUtils } from './packet.model';
import { PmfmIds } from '@app/referential/services/model/model.enum';

export class SaleProduct extends Product {

  // static TYPENAME = 'SaleProductVO'; // fixme: This VO don't exists, keep its TYPENAME ?

  static fromObject(source: any): SaleProduct {
    const target = new SaleProduct();
    target.fromObject(source);
    return target;
  }

  // members
  ratio: number;
  ratioCalculated: boolean;
  averageWeightPrice: number;
  averageWeightPriceCalculated: boolean;
  averagePackagingPrice: number;
  averagePackagingPriceCalculated: boolean;
  totalPrice: number;
  totalPriceCalculated: boolean;
  // Map product.id with taxonGroup to be able to spread in into valid sale products
  productIdByTaxonGroup: any;

  constructor() {
    super();
    this.__typename = undefined; //Product.TYPENAME;
    this.saleProducts = [];
    this.productIdByTaxonGroup = {};
  }

  asObject(opts?: DataEntityAsObjectOptions): any {
    const target = super.asObject(opts);
    delete target.saleProducts;
    return target;
  }

  fromObject(source: any, opts?: { withChildren: boolean; }): Product {
    super.fromObject(source);
    this.ratio = source.ratio;
    this.ratioCalculated = source.ratioCalculated;
    this.averageWeightPrice = source.averageWeightPrice;
    this.averageWeightPriceCalculated = source.averageWeightPriceCalculated;
    this.averagePackagingPrice = source.averagePackagingPrice;
    this.averagePackagingPriceCalculated = source.averagePackagingPriceCalculated;
    this.totalPrice = source.totalPrice;
    this.totalPriceCalculated = source.totalPriceCalculated;
    this.saleProducts = [];
    this.productIdByTaxonGroup = source.productIdByTaxonGroup || {};
    return this;
  }
}

export class SaleProductUtils {

  static isSaleProductEmpty(saleProduct: SaleProduct): boolean {
    return !saleProduct || isNil(saleProduct.saleType);
  }

  static isSaleProductEquals(saleProduct1: SaleProduct, saleProduct2: SaleProduct): boolean {
    return (saleProduct1 === saleProduct2) || (isNil(saleProduct1) && isNil(saleProduct2)) || (
      saleProduct1 && saleProduct2 && ReferentialUtils.equals(saleProduct1.saleType, saleProduct2.saleType)
      && saleProduct1.rankOrder === saleProduct2.rankOrder
    );
  }

  static isSaleOfProduct(product: Product, saleProduct: Product, pmfms: DenormalizedPmfmStrategy[]): boolean {
    return product && saleProduct
      && product.taxonGroup && saleProduct.taxonGroup && product.taxonGroup.equals(saleProduct.taxonGroup)
      && product.measurementValues && saleProduct.measurementValues
      && MeasurementValuesUtils.getValue(product.measurementValues, pmfms, PmfmIds.PACKAGING) === MeasurementValuesUtils.getValue(saleProduct.measurementValues, pmfms, PmfmIds.PACKAGING)
      && MeasurementValuesUtils.getValue(product.measurementValues, pmfms, PmfmIds.SIZE_CATEGORY) === MeasurementValuesUtils.getValue(saleProduct.measurementValues, pmfms, PmfmIds.SIZE_CATEGORY)
      ;
  }

  static isSaleOfPacket(packet: Packet, saleProduct: Product): boolean {
    return packet && saleProduct
      && packet.id === saleProduct.batchId;
  }

  static productToSaleProduct(product: Product, pmfms: DenormalizedPmfmStrategy[]): SaleProduct {
    const target = SaleProduct.fromObject(product);

    // parse measurements to sale properties
    if (product.measurementValues && pmfms) {
      const rankOrder = MeasurementValuesUtils.getValue(product.measurementValues, pmfms, PmfmIds.SALE_RANK_ORDER);
      if (rankOrder) {
        // replace product rankOrder by saleRankOrder
        target.rankOrder = isNotNilOrNaN(rankOrder) ? +rankOrder : undefined;
      }
      const ratio = MeasurementValuesUtils.getValue(product.measurementValues, pmfms, PmfmIds.SALE_ESTIMATED_RATIO);
      target.ratio = isNotNilOrNaN(ratio) ? +ratio : undefined;
      target.ratioCalculated = !target.ratio;
      const averageWeightPrice = MeasurementValuesUtils.getValue(product.measurementValues, pmfms, PmfmIds.AVERAGE_WEIGHT_PRICE);
      target.averageWeightPrice = isNotNilOrNaN(averageWeightPrice) ? +averageWeightPrice : undefined;
      target.averageWeightPriceCalculated = !target.averageWeightPrice;
      const averagePackagingPrice = MeasurementValuesUtils.getValue(product.measurementValues, pmfms, PmfmIds.AVERAGE_PACKAGING_PRICE);
      target.averagePackagingPrice = isNotNilOrNaN(averagePackagingPrice) ? +averagePackagingPrice : undefined;
      target.averagePackagingPriceCalculated = !target.averagePackagingPrice;
      const totalPrice = MeasurementValuesUtils.getValue(product.measurementValues, pmfms, PmfmIds.TOTAL_PRICE);
      target.totalPrice = isNotNilOrNaN(totalPrice) ? +totalPrice : undefined;
      target.totalPriceCalculated = !target.totalPrice;
    }

    return target;
  }

  static productsToAggregatedSaleProduct(products: Product[], pmfms: DenormalizedPmfmStrategy[]): SaleProduct[] {
    const target: SaleProduct[] = [];

    (products || []).forEach(product => {
      const saleProduct = this.productToSaleProduct(product, pmfms);

      // Valid saleProduct
      if (ReferentialUtils.isEmpty(saleProduct.taxonGroup))
        throw new Error('this saleProduct has no taxonGroup');

      // aggregate weight price to packaging price
      saleProduct.averagePackagingPrice = saleProduct.averageWeightPrice;
      saleProduct.averagePackagingPriceCalculated = !saleProduct.averagePackagingPrice;
      saleProduct.averageWeightPrice = undefined;

      const aggregatedSaleProduct = target.find(a => a.rankOrder === saleProduct.rankOrder);
      if (aggregatedSaleProduct) {

        // Some assertions
        if (aggregatedSaleProduct.subgroupCount !== saleProduct.subgroupCount)
          throw new Error(`Invalid packet sale: different packet count found: ${aggregatedSaleProduct.subgroupCount} != ${saleProduct.subgroupCount}`);
        if (isNil(saleProduct.saleType) || !saleProduct.saleType.equals(aggregatedSaleProduct.saleType))
          throw new Error(`Invalid packet sale: different sale type found:
              ${aggregatedSaleProduct.saleType && aggregatedSaleProduct.saleType.name || null} != ${saleProduct.saleType && saleProduct.saleType.name || null}`);

        // Sum values
        if (aggregatedSaleProduct.weight && saleProduct.weight)
          aggregatedSaleProduct.weight += saleProduct.weight;
        if (aggregatedSaleProduct.averagePackagingPrice && saleProduct.averagePackagingPrice)
          aggregatedSaleProduct.averagePackagingPrice += saleProduct.averagePackagingPrice;
        if (aggregatedSaleProduct.totalPrice && saleProduct.totalPrice)
          aggregatedSaleProduct.totalPrice += saleProduct.totalPrice;

        // Keep id
        if (saleProduct.id) {
          if (aggregatedSaleProduct.productIdByTaxonGroup[saleProduct.taxonGroup.id])
            throw new Error(`The taxonGroup id:${saleProduct.taxonGroup.id} already present in this aggregated sale product`);
          aggregatedSaleProduct.productIdByTaxonGroup[saleProduct.taxonGroup.id] = saleProduct.id;
        }

      } else {
        // Keep id
        if (saleProduct.id)
          saleProduct.productIdByTaxonGroup[saleProduct.taxonGroup.id] = saleProduct.id;
        // just add to aggregation
        target.push(saleProduct);
      }
    });

    return target;
  }


  static saleProductToProduct(product: Product, saleProduct: SaleProduct, pmfms: DenormalizedPmfmStrategy[], options?: { keepId?: boolean }): Product {
    // merge product with sale product to initialize target product
    const target = {...product, ...saleProduct};
    delete target.saleProducts;

    // Don't copy id by default
    if (!options || !options.keepId)
      delete target.id;

    target.measurementValues = MeasurementValuesUtils.normalizeValuesToForm(target.measurementValues, pmfms);

    // even a calculated ratio need to be saved
    MeasurementValuesUtils.setValue(target.measurementValues, pmfms, PmfmIds.SALE_ESTIMATED_RATIO, saleProduct.ratio);
    // add measurements for each non calculated values
    MeasurementValuesUtils.setValue(target.measurementValues, pmfms, PmfmIds.AVERAGE_WEIGHT_PRICE,
      isNotNilOrNaN(saleProduct.averageWeightPrice) && !saleProduct.averageWeightPriceCalculated ? saleProduct.averageWeightPrice : undefined);
    MeasurementValuesUtils.setValue(target.measurementValues, pmfms, PmfmIds.AVERAGE_PACKAGING_PRICE,
      isNotNilOrNaN(saleProduct.averagePackagingPrice) && !saleProduct.averagePackagingPriceCalculated ? saleProduct.averagePackagingPrice : undefined);
    MeasurementValuesUtils.setValue(target.measurementValues, pmfms, PmfmIds.TOTAL_PRICE,
      isNotNilOrNaN(saleProduct.totalPrice) && !saleProduct.totalPriceCalculated ? saleProduct.totalPrice : undefined);

    return Product.fromObject(target);
  }

  static aggregatedSaleProductsToProducts(packet: Packet, saleProducts: SaleProduct[], pmfms: DenormalizedPmfmStrategy[]): Product[] {
    const target: Product[] = [];

    (saleProducts || []).forEach(saleProduct => {
      // dispatch each sale product with packet composition
      packet.composition.forEach(composition => {
        const existingProductId = saleProduct.productIdByTaxonGroup && saleProduct.productIdByTaxonGroup[composition.taxonGroup.id];
        let product: Product;
        if (existingProductId) {
          product = packet.saleProducts.find(p => p.id === existingProductId);
        }
        if (!product) {
          product = new Product();
          product.taxonGroup = composition.taxonGroup;
        }
        // update this product
        product.rankOrder = saleProduct.rankOrder;
        product.subgroupCount = saleProduct.subgroupCount;
        product.saleType = saleProduct.saleType;

        // get or calculate average weight
        const compositionAverageRatio = PacketUtils.getCompositionAverageRatio(packet, composition);
        let averageWeight = composition.weight;
        if (!averageWeight) {
          averageWeight = compositionAverageRatio * packet.weight;
        }
        product.weight = round(averageWeight * saleProduct.subgroupCount / packet.number);
        product.weightCalculated = true;

        // sale rank order
        MeasurementValuesUtils.setValue(product.measurementValues, pmfms, PmfmIds.SALE_RANK_ORDER, saleProduct.rankOrder);

        // average packaging converted to average weight price
        MeasurementValuesUtils.setValue(product.measurementValues, pmfms, PmfmIds.AVERAGE_WEIGHT_PRICE,
          isNotNilOrNaN(saleProduct.averagePackagingPrice) && !saleProduct.averagePackagingPriceCalculated
            ? round(compositionAverageRatio * saleProduct.averagePackagingPrice)
            : undefined);

        // total price
        MeasurementValuesUtils.setValue(product.measurementValues, pmfms, PmfmIds.TOTAL_PRICE,
          isNotNilOrNaN(saleProduct.totalPrice) && !saleProduct.totalPriceCalculated
            ? round(compositionAverageRatio * saleProduct.totalPrice)
            : undefined);

        // add to target
        target.push(product);
      });

    });

    return target;
  }

  static updateSaleProducts(product: Product, pmfms: DenormalizedPmfmStrategy[]): Product[] {

    // convert to SaleProduct
    const saleProducts = isNotEmptyArray(product.saleProducts) ? product.saleProducts.map(p => SaleProductUtils.productToSaleProduct(p, pmfms)) : [];

    // compute prices
    saleProducts.forEach(saleProduct =>
      SaleProductUtils.computeSaleProduct(
        product,
        saleProduct,
        (object, valueName) => !!object[valueName],
        (object, valueName) => object[valueName],
        (object, valueName, value) => object[valueName] = round(value),
        (object, valueName) => object[valueName] = undefined,
        true,
        'individualCount'
      )
    );

    // convert back to Product
    return saleProducts.map(saleProduct => SaleProductUtils.saleProductToProduct(product, saleProduct, pmfms, {keepId: true}));

  }

  static updateAggregatedSaleProducts(packet: Packet, pmfms: DenormalizedPmfmStrategy[]): Product[] {

    // convert to SaleProduct
    const saleProducts = isNotEmptyArray(packet.saleProducts) ? SaleProductUtils.productsToAggregatedSaleProduct(packet.saleProducts, pmfms) : [];

    // compute prices
    saleProducts.forEach(saleProduct =>
      SaleProductUtils.computeSaleProduct(
        packet,
        saleProduct,
        (object, valueName) => !!object[valueName],
        (object, valueName) => object[valueName],
        (object, valueName, value) => object[valueName] = round(value),
        (object, valueName) => object[valueName] = undefined,
        false,
        'subgroupCount',
        'number'
      )
    );

    // convert back to Product
    return SaleProductUtils.aggregatedSaleProductsToProducts(packet, saleProducts, pmfms);

  }

  static computeSaleProduct(
    parent: {},
    saleProduct: ObjectMap,
    hasValueFn: (object: ObjectMap, valueName: string) => boolean,
    getValueFn: (object: ObjectMap, valueName: string) => any | undefined,
    setValueFn: (object: ObjectMap, valueName: string, value: any) => void,
    resetValueFn: (object: ObjectMap, valueName: string) => void,
    useRatioAndWeight: boolean,
    countProperty: string,
    parentCountProperty?: string
  ) {

    const parentCount = parent[parentCountProperty || countProperty];
    const parentWeight = parent['weight'];

    if (isNotNil(parentCount)) {

      // with saleProduct count (should be < whole parent count)
      const count = getValueFn(saleProduct, countProperty);
      if (count) {
        if (hasValueFn(saleProduct, 'averagePackagingPrice')) {
          // compute total price
          setValueFn(saleProduct, 'totalPrice', getValueFn(saleProduct, 'averagePackagingPrice') * count);

        } else if (hasValueFn(saleProduct, 'totalPrice')) {
          // compute average packaging price
          setValueFn(saleProduct, 'averagePackagingPrice', getValueFn(saleProduct, 'totalPrice') / count);

        }

        if (useRatioAndWeight) {
          // compute ratio
          const ratio = count / parentCount * 100;
          setValueFn(saleProduct, 'ratio', ratio);

          if (isNotNil(parentWeight)) {
            // calculate weight
            setValueFn(saleProduct, 'weight', ratio * parentWeight / 100);

            // calculate average weight price (with calculated or input total price)
            if (isNotNil(getValueFn(saleProduct, 'totalPrice'))) {
              setValueFn(saleProduct, 'averageWeightPrice', getValueFn(saleProduct, 'totalPrice') / parentWeight);
            }
          } else {
            // reset weight part
            resetValueFn(saleProduct, 'weight');
            resetValueFn(saleProduct, 'averageWeightPrice');
          }
        } else {

          // compute weigh (always calculated)
          setValueFn(saleProduct, 'weight', count * parentWeight / parentCount);

        }

      } else {
        // reset all
        resetValueFn(saleProduct, 'averagePackagingPrice');
        resetValueFn(saleProduct, 'totalPrice');
        resetValueFn(saleProduct, 'weight');
        if (useRatioAndWeight) {
          resetValueFn(saleProduct, 'ratio');
          resetValueFn(saleProduct, 'averageWeightPrice');
        }
      }

    } else if (useRatioAndWeight && isNotNil(parentWeight)) {
      // with weight only
      if (hasValueFn(saleProduct, 'weight')) {
        // calculate ratio
        setValueFn(saleProduct, 'ratio', getValueFn(saleProduct, 'weight') / parentWeight * 100);

      } else if (hasValueFn(saleProduct, 'ratio')) {
        // calculate weight
        setValueFn(saleProduct, 'weight', getValueFn(saleProduct, 'ratio') * parentWeight / 100);

      } else {
        // reset all
        resetValueFn(saleProduct, 'ratio');
        resetValueFn(saleProduct, 'weight');
        resetValueFn(saleProduct, 'averageWeightPrice');
        resetValueFn(saleProduct, 'totalPrice');
      }

      const weight = getValueFn(saleProduct, 'weight');
      if (isNotNil(weight)) {

        if (hasValueFn(saleProduct, 'averageWeightPrice')) {
          // compute total price
          setValueFn(saleProduct, 'totalPrice', getValueFn(saleProduct, 'averageWeightPrice') * weight);

        } else if (hasValueFn(saleProduct, 'totalPrice')) {
          // compute average weight price
          setValueFn(saleProduct, 'averageWeightPrice', getValueFn(saleProduct, 'totalPrice') / weight);

        } else {
          // reset
          resetValueFn(saleProduct, 'averageWeightPrice');
          resetValueFn(saleProduct, 'totalPrice');
        }

      }

    }
  }

}
