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
  sampleRowCode : string;
  //update with correct type ---
  eotp: TaxonNameRef;
  landingArea: TaxonNameRef;
  //----------------------------
  laboratories: ReferentialRef [];
  fishingAreas: ReferentialRef [];
  sex : boolean;
  age : boolean;
  calcifiedTypes: ReferentialRef[];
  //-------------------------------


  constructor() {
    super();
    this.__typename = Planification.TYPENAME;
    this.year=null;
    this.comment=null;
    this.taxonName = null;
    this.eotp = null;
    this.sampleRowCode = null;
    this.laboratories =  [];
    this.fishingAreas = [];
    this.landingArea = null;
    this.sex = null;
    this.age =null;
    this.calcifiedTypes = [];
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
    target.sampleRowCode = this.sampleRowCode;

    target.laboratories = this.laboratories && this.laboratories.filter(isNotNil).map(p => p && p.asObject({...options, ...NOT_MINIFY_OPTIONS})) || undefined;
    if (isEmptyArray(target.laboratories)) delete target.laboratories; // Clean is empty, for compat with previous version

    target.fishingAreas = this.fishingAreas && this.fishingAreas.filter(isNotNil).map(p => p && p.asObject({...options, ...NOT_MINIFY_OPTIONS})) || undefined;
    if (isEmptyArray(target.fishingAreas)) delete target.fishingAreas; // Clean is empty, for compat with previous version

    target.landingArea = this.landingArea;

    target.calcifiedTypes = this.calcifiedTypes && this.calcifiedTypes.filter(isNotNil).map(p => p && p.asObject({...options, ...NOT_MINIFY_OPTIONS})) || undefined;
    if (isEmptyArray(target.calcifiedTypes)) delete target.calcifiedTypes; // Clean is empty, for compat with previous version

    target.sex = this.sex;
    target.age = this.age;

    return target;
  }

  fromObject(source: any): Planification {
    super.fromObject(source);
    this.year = fromDateISOString(source.year);
    this.comment = source.comment;
    this.sampleRowCode = source.sampleRowCode;
    this.taxonName = source.taxonName && TaxonNameRef.fromObject(source.taxonName) || undefined;

   // TODO : update correct types-------------------------------------------------------------------------
    this.eotp = source.eotp && TaxonNameRef.fromObject(source.eotp) || undefined;
    this.laboratories = source.laboratories && source.laboratories.map(ReferentialRef.fromObject) || [];
    this.fishingAreas = source.fishingAreas && source.fishingAreas.map(ReferentialRef.fromObject) || [];
    this.landingArea = source.landingArea && TaxonNameRef.fromObject(source.landingArea) || undefined;
    this.calcifiedTypes = source.calcifiedTypes && source.calcifiedTypes.map(ReferentialRef.fromObject) || [];
    //----------------------------------------------------------------------------------------------------
    this.sex = source.sex  || undefined;
    this.age = source.age  || undefined;

    return this;
  }

  }
