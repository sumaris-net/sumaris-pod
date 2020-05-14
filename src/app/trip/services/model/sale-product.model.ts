import {EntityUtils, isNil, PmfmStrategy} from "../../../referential/services/model";
import {MeasurementValuesUtils} from "./measurement.model";
import {isNotNilOrNaN, round} from "../../../shared/functions";
import {DataEntityAsObjectOptions} from "./base.model";
import {Product} from "./product.model";
import {Packet, PacketUtils} from "./packet.model";

export class SaleProduct extends Product {

  static TYPENAME = 'SaleProductVO';

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
    this.__typename = Product.TYPENAME;
    this.saleProducts = [];
    this.productIdByTaxonGroup = {};
  }

  clone(): SaleProduct {
    return SaleProduct.fromObject(this.asObject());
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

  static isSaleOfProduct(product: Product, saleProduct: Product, pmfms: PmfmStrategy[]): boolean {
    return product && saleProduct
      && product.taxonGroup && saleProduct.taxonGroup && product.taxonGroup.equals(saleProduct.taxonGroup)
      && product.measurementValues && saleProduct.measurementValues
      && MeasurementValuesUtils.getValue(product.measurementValues, pmfms, 'PACKAGING') === MeasurementValuesUtils.getValue(saleProduct.measurementValues, pmfms, 'PACKAGING')
      && MeasurementValuesUtils.getValue(product.measurementValues, pmfms, 'SIZE_CATEGORY') === MeasurementValuesUtils.getValue(saleProduct.measurementValues, pmfms, 'SIZE_CATEGORY')
      ;
  }

  static isSaleOfPacket(packet: Packet, saleProduct: Product): boolean {
    return packet && saleProduct
      && packet.id === saleProduct.batchId;
  }

  static productToSaleProduct(product: Product, pmfms: PmfmStrategy[]): SaleProduct {
    const target = SaleProduct.fromObject(product);

    // parse measurements to sale properties
    if (product.measurementValues && pmfms) {
      const rankOrder = MeasurementValuesUtils.getValue(product.measurementValues, pmfms, 'SALE_RANK_ORDER', true);
      if (rankOrder) {
        // replace product rankOrder by saleRankOrder
        target.rankOrder = isNotNilOrNaN(rankOrder) ? +rankOrder : undefined;
      }
      const ratio = MeasurementValuesUtils.getValue(product.measurementValues, pmfms, 'SALE_RATIO', true);
      target.ratio = isNotNilOrNaN(ratio) ? +ratio : undefined;
      target.ratioCalculated = !target.ratio;
      const averageWeightPrice = MeasurementValuesUtils.getValue(product.measurementValues, pmfms, 'AVERAGE_PRICE_WEI', true);
      target.averageWeightPrice = isNotNilOrNaN(averageWeightPrice) ? +averageWeightPrice : undefined;
      target.averageWeightPriceCalculated = !target.averageWeightPrice;
      const averagePackagingPrice = MeasurementValuesUtils.getValue(product.measurementValues, pmfms, 'AVERAGE_PACKAGING_PRICE', true);
      target.averagePackagingPrice = isNotNilOrNaN(averagePackagingPrice) ? +averagePackagingPrice : undefined;
      target.averagePackagingPriceCalculated = !target.averagePackagingPrice;
      const totalPrice = MeasurementValuesUtils.getValue(product.measurementValues, pmfms, 'TOTAL_PRICE', true);
      target.totalPrice = isNotNilOrNaN(totalPrice) ? +totalPrice : undefined;
      target.totalPriceCalculated = !target.totalPrice;
    }

    return target;
  }

  static productsToAggregatedSaleProduct(products: Product[], pmfms: PmfmStrategy[]): SaleProduct[] {
    const target: SaleProduct[] = [];

    (products || []).forEach(product => {
      const saleProduct = this.productToSaleProduct(product, pmfms);

      // Valid saleProduct
      if (EntityUtils.isEmpty(saleProduct.taxonGroup))
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


  static saleProductToProduct(product: Product, saleProduct: SaleProduct, pmfms: PmfmStrategy[], options?: { keepId?: boolean }): Product {
    // merge product with sale product to initialize target product
    const target = {...product, ...saleProduct};
    delete target.saleProducts;

    // Don't copy id by default
    if (!options || !options.keepId)
      delete target.id;

    target.measurementValues = MeasurementValuesUtils.normalizeValuesToModel(target.measurementValues, pmfms);

    // even a calculated ratio need to be saved
    MeasurementValuesUtils.setValue(target.measurementValues, pmfms, 'SALE_RATIO', saleProduct.ratio);
    // if (saleProduct.batchId && saleProduct.rankOrder) {
    //   // for a packet sale, the rankOrder is stored in measurement (SIH)
    //   MeasurementValuesUtils.setValue(target.measurementValues, pmfms, 'SALE_RANK_ORDER', saleProduct.rankOrder);
    // }
    // add measurements for each non calculated values
    MeasurementValuesUtils.setValue(target.measurementValues, pmfms, 'AVERAGE_PRICE_WEI',
      isNotNilOrNaN(saleProduct.averageWeightPrice) && !saleProduct.averageWeightPriceCalculated ? saleProduct.averageWeightPrice : undefined);
    MeasurementValuesUtils.setValue(target.measurementValues, pmfms, 'AVERAGE_PACKAGING_PRICE',
      isNotNilOrNaN(saleProduct.averagePackagingPrice) && !saleProduct.averagePackagingPriceCalculated ? saleProduct.averagePackagingPrice : undefined);
    MeasurementValuesUtils.setValue(target.measurementValues, pmfms, 'TOTAL_PRICE',
      isNotNilOrNaN(saleProduct.totalPrice) && !saleProduct.totalPriceCalculated ? saleProduct.totalPrice : undefined);

    return Product.fromObject(target);
  }

  static aggregatedSaleProductsToProducts(packet: Packet, saleProducts: SaleProduct[], pmfms: PmfmStrategy[]): Product[] {
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
        const compositionAverageRatio = PacketUtils.getCompositionAverageRatio(composition);
        let averageWeight = composition.weight;
        if (!averageWeight) {
          averageWeight = compositionAverageRatio * packet.weight; // todo be sure packet.weight is defined
        }
        product.weight = round(averageWeight * saleProduct.subgroupCount / packet.number);
        product.weightCalculated = true;

        // sale rank order
        MeasurementValuesUtils.setValue(product.measurementValues, pmfms, 'SALE_RANK_ORDER', saleProduct.rankOrder);

        // average packaging converted to average weight price
        MeasurementValuesUtils.setValue(product.measurementValues, pmfms, 'AVERAGE_PRICE_WEI',
          isNotNilOrNaN(saleProduct.averagePackagingPrice) && !saleProduct.averagePackagingPriceCalculated
            ? round(compositionAverageRatio * saleProduct.averagePackagingPrice)
            : undefined);

        // total price
        MeasurementValuesUtils.setValue(product.measurementValues, pmfms, 'TOTAL_PRICE',
          isNotNilOrNaN(saleProduct.totalPrice) && !saleProduct.totalPriceCalculated
            ? round(compositionAverageRatio * saleProduct.totalPrice)
            : undefined);

        // add to target
        target.push(product);
      });


    });

    return target;
  }

  static isSaleProductEmpty(saleProduct: SaleProduct): boolean {
    return !saleProduct || isNil(saleProduct.saleType);
  }

  static isSaleProductEquals(saleProduct1: SaleProduct, saleProduct2: SaleProduct): boolean {
    return (saleProduct1 === saleProduct2) || (isNil(saleProduct1) && isNil(saleProduct2)) || (
      saleProduct1 && saleProduct2 && EntityUtils.equals(saleProduct1.saleType, saleProduct2.saleType)
      && saleProduct1.rankOrder === saleProduct2.rankOrder
    );
  }

}
