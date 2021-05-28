import {Referential, ReferentialRef} from "../../../core/services/model/referential.model";
import {EntityAsObjectOptions} from "../../../core/services/model/entity.model";
import {Moment} from "moment";

export class TaxonName extends Referential<TaxonName> {

  static TYPENAME = 'TaxonName';

  static fromObject(source: any): TaxonName {
    if (!source || source instanceof TaxonName) return source;
    const res = new TaxonName();
    res.fromObject(source);
    return res;
  }

  isReferent: boolean;
  isNaming: boolean;
  isVirtual: boolean;
  taxonGroupIds: number[];
  referenceTaxonId: number;
  taxonomicLevel: Referential;
  parentTaxonName: TaxonName;
  useExistingReferenceTaxon: boolean;
  startDate: Moment;
  endDate: Moment;

  constructor() {
    super();
    this.entityName = TaxonName.TYPENAME;
  }

  clone(): TaxonName {
    const target = new TaxonName();
    target.fromObject(this);
    return target;
  }

  asObject(options?: EntityAsObjectOptions): any {
    const target: any = super.asObject({
      ...options,
      minify: false // Do NOT minify itself
    });

    if (options && options.minify) {
      target.parentId = this.parentTaxonName && this.parentTaxonName.id;
      target.taxonomicLevelId = this.taxonomicLevel && this.taxonomicLevel.id;
      delete target.taxonomicLevel;
      delete target.parentTaxonName;
      delete target.useExistingReferenceTaxon;
    } else {
      target.parentTaxonName = this.parentTaxonName && this.parentTaxonName.asObject(options);

    }
    return target;
  }

  fromObject(source: any): TaxonName {
    super.fromObject(source);

    this.isReferent = source.isReferent;
    this.isNaming = source.isNaming;
    this.isVirtual = source.isVirtual;
    this.referenceTaxonId = source.referenceTaxonId;
    this.taxonomicLevel = source.taxonomicLevel && ReferentialRef.fromObject(source.taxonomicLevel);
    this.taxonGroupIds = source.taxonGroupIds;
    this.entityName = source.entityName || TaxonName.TYPENAME;
    this.parentTaxonName = source.parentTaxonName && ReferentialRef.fromObject(source.parentTaxonName);
    this.startDate = source.startDate;
    this.endDate = source.endDate;

    return this;
  }

  get taxonomicLevelId(): number {
    return this.taxonomicLevel && this.taxonomicLevel.id;
  }
}
