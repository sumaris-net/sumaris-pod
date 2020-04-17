import {
  DataEntity,
  DataEntityAsObjectOptions,
  EntityUtils,
  isNil,
  isNotNil,
  NOT_MINIFY_OPTIONS,
  ReferentialRef, referentialToString
} from "./base.model";
import {ReferentialAsObjectOptions} from "../../../core/services/model";
import {OperationGroup} from "./trip.model";
import {getResponseURL} from "@angular/http/src/http_utils";

export class Packet extends DataEntity<Packet> {

  static TYPENAME = 'PacketVO';

  rankOrder: number;
  number: number;
  weight: number;
  // sampledWeights: number[];
  sampledWeight1: number;
  sampledWeight2: number;
  sampledWeight3: number;
  sampledWeight4: number;
  sampledWeight5: number;
  sampledWeight6: number;
  composition: PacketComposition[];

  // used to compute packet's ratio from composition
  sampledRatio1: number;
  sampledRatio2: number;
  sampledRatio3: number;
  sampledRatio4: number;
  sampledRatio5: number;
  sampledRatio6: number;

  parent: OperationGroup;
  parentId: number;

  constructor() {
    super();
    this.rankOrder = null;
    this.number = null;
    this.weight = null;
    // this.sampledWeights = [];
    this.composition = [];
    this.parent = null;
    this.parentId = null;
  }

  asObject(options?: DataEntityAsObjectOptions): any {
    const target = super.asObject(options);
    target.sampledWeights = [this.sampledWeight1, this.sampledWeight2, this.sampledWeight3, this.sampledWeight4, this.sampledWeight5, this.sampledWeight6];
    delete target.sampledWeight1;
    delete target.sampledWeight2;
    delete target.sampledWeight3;
    delete target.sampledWeight4;
    delete target.sampledWeight5;
    delete target.sampledWeight6;
    target.composition = this.composition && this.composition.map(c => c.asObject(options)) || undefined;
    target.operationId = this.parent && this.parent.id || this.parentId;
    return target;
  }

  fromObject(source: any): DataEntity<Packet> {
    super.fromObject(source);
    this.rankOrder = source.rankOrder;
    this.number = source.number;
    this.weight = source.weight;
    // this.sampledWeights = source.sampledWeights;
    this.sampledWeight1 = source.sampledWeights && source.sampledWeights[0] || source.sampledWeight1;
    this.sampledWeight2 = source.sampledWeights && source.sampledWeights[1] || source.sampledWeight2;
    this.sampledWeight3 = source.sampledWeights && source.sampledWeights[2] || source.sampledWeight3;
    this.sampledWeight4 = source.sampledWeights && source.sampledWeights[3] || source.sampledWeight4;
    this.sampledWeight5 = source.sampledWeights && source.sampledWeights[4] || source.sampledWeight5;
    this.sampledWeight6 = source.sampledWeights && source.sampledWeights[5] || source.sampledWeight6;
    this.composition = source.composition && source.composition.map(c => PacketComposition.fromObject(c));
    this.parentId = source.operationId;
    this.parent = source.parent;
    return this;
  }

  clone(): Packet {
    const target = new Packet();
    target.fromObject(this.asObject());
    return target;
  }

  static fromObject(source: any): Packet {
    const target = new Packet();
    target.fromObject(source);
    return target;
  }


}

export class PacketComposition extends DataEntity<PacketComposition> {

  static TYPENAME = 'PacketCompositionVO';

  rankOrder: number;
  taxonGroup: ReferentialRef;
  // ratios: number[];
  ratio1: number;
  ratio2: number;
  ratio3: number;
  ratio4: number;
  ratio5: number;
  ratio6: number;
  weight: number;

  constructor() {
    super();
    this.rankOrder = null;
    this.taxonGroup = null;
    this.weight = null;
    // this.ratios = [];
  }

  asObject(options?: DataEntityAsObjectOptions): any {
    const target = super.asObject(options);

    target.taxonGroup = this.taxonGroup && this.taxonGroup.asObject({...options, ...NOT_MINIFY_OPTIONS, keepEntityName: true} as ReferentialAsObjectOptions) || undefined;
    target.ratios = [this.ratio1, this.ratio2, this.ratio3, this.ratio4, this.ratio5, this.ratio6];
    delete target.ratio1;
    delete target.ratio2;
    delete target.ratio3;
    delete target.ratio4;
    delete target.ratio5;
    delete target.ratio6;
    delete target.weight;

    return target;
  }

  fromObject(source: any): PacketComposition {
    super.fromObject(source);
    this.rankOrder = source.rankOrder || undefined;
    this.taxonGroup = source.taxonGroup && ReferentialRef.fromObject(source.taxonGroup) || undefined;
    // this.ratios = source.ratios || undefined;
    this.ratio1 = source.ratios && source.ratios[0] || source.ratio1;
    this.ratio2 = source.ratios && source.ratios[1] || source.ratio2;
    this.ratio3 = source.ratios && source.ratios[2] || source.ratio3;
    this.ratio4 = source.ratios && source.ratios[3] || source.ratio4;
    this.ratio5 = source.ratios && source.ratios[4] || source.ratio5;
    this.ratio6 = source.ratios && source.ratios[5] || source.ratio6;
    return this;
  }

  clone(): PacketComposition {
    return PacketComposition.fromObject(this.asObject());
  }

  static fromObject(source: any) {
    if (isNil(source)) return null;
    const res = new PacketComposition();
    res.fromObject(source);
    return res;
  }

}

export class PacketUtils {

  static isPacketEmpty(packet: Packet): boolean {
    return !packet || isNil(packet.number);
  }

  static isPacketCompositionEmpty(composition: PacketComposition): boolean {
    return !composition || isNil(composition.taxonGroup);
  }

  static isPacketCompositionEquals(composition1: PacketComposition, composition2: PacketComposition): boolean {
    return (composition1 === composition2) || (isNil(composition1) && isNil(composition2)) || (
      composition1 && composition2 && EntityUtils.equals(composition1.taxonGroup, composition2.taxonGroup)
      && composition1.ratio1 === composition2.ratio1
      && composition1.ratio2 === composition2.ratio2
      && composition1.ratio3 === composition2.ratio3
      && composition1.ratio4 === composition2.ratio4
      && composition1.ratio5 === composition2.ratio5
      && composition1.ratio6 === composition2.ratio6
    );
  }

  static getComposition(packet: Packet) {
    return packet && packet.composition && packet.composition.map(composition => referentialToString(composition.taxonGroup)).join('\n') || "";
  }
}
