import {Entity, isNil, ReferentialRef, IReferentialRef, toDateISOString, EntityUtils} from "../../../core/core.module";
import {MeasurementUtils} from "../../../trip/services/model/measurement.model";


export const TaxonGroupIds = {
  FAO: 2,
  METIER: 3
};

export const TaxonomicLevelIds = {
  ORDO: 13,
  FAMILY: 17,
  GENUS: 26,
  SUBGENUS: 27,
  SPECIES: 28,
  SUBSPECIES: 29
};


export class TaxonNameRef extends Entity<TaxonNameRef> implements IReferentialRef {

  static fromObject(source: any): TaxonNameRef {
    if (isNil(source)) return null;
    const res = new TaxonNameRef();
    res.fromObject(source);
    return res;
  }

  static equalsOrSameReferenceTaxon(v1: TaxonNameRef, v2: TaxonNameRef): boolean {
    return EntityUtils.equals(v1, v2) || (v1 && v2 && v1.referenceTaxonId === v2.referenceTaxonId);
  }

  label: string;
  name: string;
  statusId: number;
  entityName: string;

  referenceTaxonId: number;

  constructor() {
    super();
  }

  clone(): TaxonNameRef {
    return this.copy(new TaxonNameRef());
  }

  copy(target: TaxonNameRef): TaxonNameRef {
    target.fromObject(this);
    return target;
  }

  asObject(minify?: boolean): any {
    if (minify) return {id: this.id}; // minify=keep id only
    const target: any = super.asObject();
    delete target.entityName;
    return target;
  }

  fromObject(source: any): Entity<TaxonNameRef> {
    super.fromObject(source);
    this.label = source.label;
    this.name = source.name;
    this.statusId = source.statusId;
    this.entityName = source.entityName || 'TaxonName';
    this.referenceTaxonId = source.referenceTaxonId;
    return this;
  }
}


export class TaxonGroupRef extends Entity<TaxonGroupRef> implements IReferentialRef {

  static fromObject(source: any): TaxonGroupRef {
    if (isNil(source)) return null;
    const res = new TaxonGroupRef();
    res.fromObject(source);
    return res;
  }

  label: string;
  name: string;
  statusId: number;
  entityName: string;

  taxonNames: TaxonNameRef[];

  constructor(data?: {
    id?: number,
    label?: string,
    name?: string
  }) {
    super();
    this.id = data && data.id;
    this.label = data && data.label;
    this.name = data && data.name;
  }

  clone(): TaxonGroupRef {
    return this.copy(new TaxonGroupRef());
  }

  copy(target: TaxonGroupRef): TaxonGroupRef {
    target.fromObject(this);
    return target;
  }

  asObject(minify?: boolean): any {
    if (minify) return {id: this.id}; // minify=keep id only
    const target: any = super.asObject();
    delete target.entityName;
    return target;
  }

  fromObject(source: any): Entity<TaxonGroupRef> {
    super.fromObject(source);
    this.label = source.label;
    this.name = source.name;
    this.statusId = source.statusId;
    this.entityName = source.entityName;
    this.taxonNames = source.taxonNames && source.taxonNames.map(TaxonNameRef.fromObject) || [];
    return this;
  }
}

export class MetierRef extends ReferentialRef<MetierRef> {

  static fromObject(source: any, useTaxonGroupLabelAndName?: boolean): MetierRef {
    if (isNil(source)) return null;
    const res = new MetierRef();
    res.fromObject(source);

    // Copy some attributes from the target species
    if (useTaxonGroupLabelAndName && res.taxonGroup) {
      res.label = res.taxonGroup.label || res.label;
      res.name = res.taxonGroup.name || res.name;
    }
    return res;
  }

  gear: ReferentialRef;
  taxonGroup: ReferentialRef;

  constructor() {
    super();
    this.taxonGroup = null;
  }

  clone(): MetierRef {
    return this.copy(new MetierRef());
  }

  copy(target: MetierRef): MetierRef {
    target.fromObject(this);
    return target;
  }

  fromObject(source: any): Entity<MetierRef> {
    super.fromObject(source);
    this.gear = source.gear && ReferentialRef.fromObject(source.gear);
    this.taxonGroup = source.taxonGroup && ReferentialRef.fromObject(source.taxonGroup);
    return this;
  }

  asObject(minify?: boolean): any {
    const target = super.asObject(minify);
    target.gear = this.gear && this.gear.asObject(minify) || undefined;
    target.taxonGroup = this.taxonGroup && this.taxonGroup.asObject(minify) || undefined;
    return target;
  }
}
