import {
  EntityUtils,
  fromDateISOString,
  isNotNil,
  Person,
  ReferentialRef,
  toDateISOString
} from "../../../core/core.module";
import {DataEntity, DataEntityAsObjectOptions} from "../../../data/services/model/data-entity.model";
import {NOT_MINIFY_OPTIONS} from "../../../core/services/model/referential.model";
import {Moment} from "moment";
import {isEmptyArray} from "../../../shared/functions";
import {TaxonNameRef} from "../../../referential/services/model/taxon.model";


export class Planification extends DataEntity<Planification>  {

  //TODO : créer PlanificationVO requête
  static TYPENAME = 'PlanificationVO';

  static fromObject(source: any): Planification {
    const res = new Planification();
    res.fromObject(source);

    return res;
  }

  year : Moment;
  comment : string;
  taxonName: TaxonNameRef;

  // TODO : update correct types--
  eotp: TaxonNameRef;
  laboratory: TaxonNameRef;
  fishingArea: TaxonNameRef;
  landingArea: TaxonNameRef;
  sex : boolean;
  age : boolean;
  //-------------------------------


  constructor() {
    super();
    this.__typename = Planification.TYPENAME;
    this.year=null;
    this.comment=null;
    this.taxonName = null;
    this.eotp = null;
    this.laboratory = null;
    this.fishingArea = null;
    this.landingArea = null;
    this.sex = null;
    this.age =null;
  }

  clone(): Planification {
    return Planification.fromObject(this.asObject());
  }

  asObject(options?: DataEntityAsObjectOptions): any {
    const target = super.asObject(options);

    target.year = toDateISOString(this.year);
    target.comment = this.comment;
    target.taxonName = this.taxonName;

    target.eotp = this.eotp;
    target.laboratory = this.laboratory;
    target.fishingArea = this.fishingArea;
    target.landingArea = this.landingArea;

    target.sex = this.sex;
    target.age = this.age;

    return target;
  }

  fromObject(source: any): Planification {
    super.fromObject(source);
    this.year = fromDateISOString(source.year);
    this.comment = source.comment;
    this.taxonName = source.taxonName && TaxonNameRef.fromObject(source.taxonName) || undefined;

   // TODO : update correct types-------------------------------------------------------------------------
    this.eotp = source.eotp && TaxonNameRef.fromObject(source.eotp) || undefined;
    this.laboratory = source.laboratory && TaxonNameRef.fromObject(source.laboratory) || undefined;
    this.fishingArea = source.fishingArea && TaxonNameRef.fromObject(source.fishingArea) || undefined;
    this.landingArea = source.landingArea && TaxonNameRef.fromObject(source.landingArea) || undefined;
    //----------------------------------------------------------------------------------------------------
    this.sex = source.sex  || undefined;
    this.age = source.age  || undefined;

    return this;
  }

  }
