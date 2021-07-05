import {BaseReferential, IReferentialRef, ReferentialAsObjectOptions, ReferentialRef, ReferentialUtils}  from "@sumaris-net/ngx-components";
import {isNil, isNotNil, uncapitalizeFirstLetter} from "@sumaris-net/ngx-components";
import {Entity}  from "@sumaris-net/ngx-components";
import {EntityClass}  from "@sumaris-net/ngx-components";


export const TaxonGroupTypeIds = {
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

export const TaxonGroupLabels = {
  FISH: 'MZZ'
};

@EntityClass({typename: "TaxonNameVO"})
export class TaxonNameRef
  extends Entity<TaxonNameRef, number, ReferentialAsObjectOptions>
  implements IReferentialRef<TaxonNameRef> {

  static ENTITY_NAME = 'TaxonName';
  static fromObject: (source: any, opts?: any) => TaxonNameRef;

  static equalsOrSameReferenceTaxon(v1: TaxonNameRef, v2: TaxonNameRef): boolean {
    return ReferentialUtils.equals(v1, v2) || (v1 && v2 && isNotNil(v1.referenceTaxonId) && v1.referenceTaxonId === v2.referenceTaxonId);
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
    super(TaxonNameRef.TYPENAME);
    this.entityName = TaxonNameRef.ENTITY_NAME;
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

  fromObject(source: any) {
    super.fromObject(source);
    this.label = source.label;
    this.name = source.name;
    this.statusId = source.statusId;
    this.entityName = source.entityName || TaxonNameRef.ENTITY_NAME;
    this.levelId = source.levelId;
    this.referenceTaxonId = source.referenceTaxonId;
    this.taxonGroupIds = source.taxonGroupIds;
  }
}

@EntityClass({typename: 'TaxonGroupVO'})
export class TaxonGroupRef extends Entity<TaxonGroupRef, number, ReferentialAsObjectOptions>
  implements IReferentialRef<TaxonGroupRef> {

  static ENTITY_NAME = 'TaxonGroup';
  static fromObject: (source: any, opts?: any) => TaxonGroupRef;

  entityName: string;
  label: string;
  name: string;
  statusId: number;
  rankOrder: number;
  taxonNames: TaxonNameRef[];

  constructor() {
    super(TaxonGroupRef.TYPENAME);
    this.entityName = TaxonGroupRef.ENTITY_NAME;
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

  fromObject(source: any) {
    super.fromObject(source);
    this.label = source.label;
    this.name = source.name;
    this.statusId = source.statusId;
    this.entityName = source.entityName || TaxonGroupRef.ENTITY_NAME;
    this.taxonNames = source.taxonNames && source.taxonNames.map(TaxonNameRef.fromObject) || [];
  }
}

export interface MetierFromObjectOptions {
  useChildAttributes?: false | 'TaxonGroup' | 'Gear';
}

@EntityClass({typename: "MetierVO"})
export class Metier extends BaseReferential<Metier, number, ReferentialAsObjectOptions,  MetierFromObjectOptions> {
  static ENTITY_NAME = 'Metier';
  static fromObject: (source: any, opts?: MetierFromObjectOptions) => Metier;

  gear: ReferentialRef = null;
  taxonGroup: ReferentialRef = null;

  constructor() {
    super(Metier.TYPENAME);
    this.entityName = Metier.ENTITY_NAME;
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
    this.entityName = source.entityName || Metier.ENTITY_NAME;
    this.gear = source.gear && ReferentialRef.fromObject(source.gear);
    this.taxonGroup = source.taxonGroup && ReferentialRef.fromObject(source.taxonGroup);

    // Copy label/name from child (TaxonGroup or Gear)
    if (opts && opts.useChildAttributes) {
      const childKey = uncapitalizeFirstLetter(opts.useChildAttributes);
      if (source[childKey]) {
        this.label = source[childKey].label || this.label;
        this.name = source[childKey].name || this.name;
      }
    }
  }
}

export class TaxonUtils {

  static generateLabel(taxonName: string) {
    if (isNil(taxonName)) return undefined;
    let label = undefined;
    const genusWord = /^[a-zA-Z]{4,}$/;
    const speciesWord = /^[a-zA-Z]{3,}$/;

    // Rubin code for "Leucoraja circularis": LEUC CIR
    const parts = taxonName.split(" ");
    if (parts.length === 2 && parts[0].match(genusWord) && parts[1].match(speciesWord)) {
      label = parts[0].slice(0, 4).toUpperCase() + parts[1].slice(0, 3).toUpperCase();
    }

    return label;
  }

}
