import {Entity, isNil, ReferentialRef, IReferentialRef} from "../../../core/core.module";


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
    this.entityName = source.entityName;
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

  taxonNames: ReferentialRef[];

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
    this.taxonNames = source.taxonNames || source.taxonNames.map(TaxonNameRef.fromObject) || [];
    return this;
  }
}
