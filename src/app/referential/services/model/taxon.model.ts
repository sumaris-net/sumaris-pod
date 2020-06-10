import {Entity, IReferentialRef, isNil, Referential, ReferentialRef} from "../../../core/core.module";
import {ReferentialAsObjectOptions, ReferentialUtils} from "../../../core/services/model";
import {uncapitalizeFirstLetter} from "../../../shared/functions";


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

  static TYPENAME = 'TaxonNameVO';

  static fromObject(source: any): TaxonNameRef {
    if (isNil(source)) return null;
    const res = new TaxonNameRef();
    res.fromObject(source);
    return res;
  }

  static equalsOrSameReferenceTaxon(v1: TaxonNameRef, v2: TaxonNameRef): boolean {
    return ReferentialUtils.equals(v1, v2) || (v1 && v2 && v1.referenceTaxonId === v2.referenceTaxonId);
  }

  label: string;
  name: string;
  statusId: number;
  rankOrder: number;
  entityName: string;

  levelId: number;
  taxonGroupIds: number[];

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

  asObject(options?: ReferentialAsObjectOptions): any {
    if (options && options.minify) {
      return {
        id: this.id,
        __typename: options.keepTypename && this.__typename || undefined
      };
    }
    const target: any = super.asObject(options);
    if (options && options.keepEntityName !== true) delete target.entityName; // delete by default
    delete target.taxonGroupIds; // Not need
    return target;
  }

  fromObject(source: any): Entity<TaxonNameRef> {
    super.fromObject(source);
    this.label = source.label;
    this.name = source.name;
    this.statusId = source.statusId;
    this.entityName = source.entityName || TaxonNameRef.TYPENAME;
    this.levelId = source.levelId;
    this.referenceTaxonId = source.referenceTaxonId;
    this.taxonGroupIds = source.taxonGroupIds;
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
  rankOrder: number;
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

  asObject(options?: ReferentialAsObjectOptions): any {
    if (options && options.minify) {
      return {
        id: this.id,
        __typename: options.keepTypename && this.__typename || undefined
      };
    }
    const target: any = super.asObject(options);
    if (options && options.keepEntityName !== true) delete target.entityName; // delete by default
    delete target.taxonNames; // Not need
    return target;
  }

  fromObject(source: any): Entity<TaxonGroupRef> {
    super.fromObject(source);
    this.label = source.label;
    this.name = source.name;
    this.statusId = source.statusId;
    this.entityName = source.entityName || 'TaxonGroupVO';
    this.taxonNames = source.taxonNames && source.taxonNames.map(TaxonNameRef.fromObject) || [];
    return this;
  }
}

export interface MetierFromObjectOptions {
  useChildAttributes?: false | 'TaxonGroup' | 'Gear';
}

export class Metier extends Referential<Metier> {

  static fromObject(source: any, opts?: MetierFromObjectOptions): Metier {
    if (isNil(source) || source instanceof Metier) return source;
    const target = new Metier();
    target.fromObject(source, opts);
    return target;
  }

  gear: ReferentialRef;
  taxonGroup: ReferentialRef;

  constructor() {
    super();
    this.taxonGroup = null;
  }

  clone(): Metier {
    const target = new Metier();
    target.fromObject(this);
    return target;
  }

  asObject(opts?: ReferentialAsObjectOptions): any {
    const target = super.asObject(opts);
    if (!opts || opts.minify !== true) {
      target.gear = this.gear && this.gear.asObject(opts) || undefined;

      if (target.gear && !target.gear.entityName) {
        // Fixme gear entityName here
        console.warn('Missing gear.entityName in Metier instance', this);
        target.gear.entityName = 'Gear';
      }

      target.taxonGroup = this.taxonGroup && this.taxonGroup.asObject(opts) || undefined;
    }
    return target;
  }

  fromObject(source: any, opts?: MetierFromObjectOptions) {
    super.fromObject(source);
    this.entityName = source.entityName || 'Metier';
    this.gear = source.gear && ReferentialRef.fromObject(source.gear);
    this.taxonGroup = source.taxonGroup && ReferentialRef.fromObject(source.taxonGroup);

    // Copy label/name from child (TaxonGroup or Gear)
    if (opts && opts.useChildAttributes) {
      const childKey = uncapitalizeFirstLetter(opts.useChildAttributes);
      if (source[childKey]) {
        this.label = source[childKey].label || this.label;
        this.name = source[childKey].name || this.name;
      }
    }
  }
}
