import {ReferentialRef} from "../../../core/services/model";
import {PmfmStrategy} from "../../../referential/services/model";
import {MeasurementValuesUtils} from "./measurement.model";
import {Product} from "./product.model";
import {Packet} from "./packet.model";
import {isNotNilOrNaN} from "../../../shared/functions";

export class PacketSale {

  saleType: ReferentialRef;
  rankOrder: number;
  number: number;
  weight: number;
  averagePackagingPrice: number;
  totalPrice: number;

  static isSaleOfPacket(packet: Packet, productSale: Product): boolean {
    return packet && productSale
      && packet.id === productSale.batchId;
  }

  static toPacketSale(product: Product, pmfms: PmfmStrategy[]): PacketSale {
    const result = new PacketSale();

    result.saleType = product.saleType;
    result.rankOrder = product.rankOrder;
    result.number = product.subgroupCount;
    result.weight = product.weight;
    if (product.measurementValues && pmfms) {
      const rankOrder = MeasurementValuesUtils.getValue(product.measurementValues, pmfms, 'SALE_RANK_ORDER');
      result.rankOrder = isNotNilOrNaN(rankOrder) ? +rankOrder : undefined;
      const averagePackagingPrice = MeasurementValuesUtils.getValue(product.measurementValues, pmfms, 'AVERAGE_PRICE_WEI');
      result.averagePackagingPrice = isNotNilOrNaN(averagePackagingPrice) ? +averagePackagingPrice : undefined;
      const totalPrice = MeasurementValuesUtils.getValue(product.measurementValues, pmfms, 'TOTAL_PRICE');
      result.totalPrice = isNotNilOrNaN(totalPrice) ? +totalPrice : undefined;
    }

    return result;
  }

}

