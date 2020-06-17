import {
  MINIFY_OPTIONS,
  NOT_MINIFY_OPTIONS,
  Referential,
  ReferentialAsObjectOptions,
  ReferentialRef
} from "../../../core/services/model/referential.model";
import {Moment} from "moment";
import {EntityAsObjectOptions} from "../../../core/services/model/entity.model";
import {fromDateISOString, toDateISOString} from "../../../shared/functions";
import {TaxonGroupRef, TaxonNameRef} from "./taxon.model";
import {PmfmStrategy} from "./pmfm-strategy.model";

export class Strategy extends Referential<Strategy> {

  static fromObject(source: any): Strategy {
    if (!source || source instanceof Strategy) return source;
    const res = new Strategy();
    res.fromObject(source);
    return res;
  }

  label: string;
  name: string;
  description: string;
  comments: string;
  creationDate: Moment;
  statusId: number;
  pmfmStrategies: PmfmStrategy[];

  gears: any[];
  taxonGroups: TaxonGroupStrategy[];
  taxonNames: TaxonNameStrategy[];

  programId: number;

  constructor(data?: {
    id?: number,
    label?: string,
    name?: string
  }) {
    super();
    this.id = data && data.id;
    this.label = data && data.label;
    this.name = data && data.name;
    this.pmfmStrategies = [];
    this.gears = [];
    this.taxonGroups = [];
    this.taxonNames = [];
  }

  clone(): Strategy {
    const target = new Strategy();
    target.fromObject(this);
    return target;
  }

  asObject(opts?: EntityAsObjectOptions): any {
    const target: any = super.asObject(opts);
    target.programId = this.programId;
    target.creationDate = toDateISOString(this.creationDate);
    target.pmfmStrategies = this.pmfmStrategies && this.pmfmStrategies.map(s => s.asObject({ ...opts, ...NOT_MINIFY_OPTIONS }));
    target.gears = this.gears && this.gears.map(s => s.asObject(opts));
    target.taxonGroups = this.taxonGroups && this.taxonGroups.map(s => s.asObject({ ...opts, ...NOT_MINIFY_OPTIONS }));
    target.taxonNames = this.taxonNames && this.taxonNames.map(s => s.asObject({ ...opts, ...NOT_MINIFY_OPTIONS }));
    return target;
  }

  fromObject(source: any) {
    super.fromObject(source);
    this.label = source.label;
    this.name = source.name;
    this.description = source.description;
    this.comments = source.comments;
    this.statusId = source.statusId;
    this.creationDate = fromDateISOString(source.creationDate);
    this.pmfmStrategies = source.pmfmStrategies && source.pmfmStrategies.map(PmfmStrategy.fromObject) || [];
    this.gears = source.gears && source.gears.map(ReferentialRef.fromObject) || [];
    // Taxon groups, sorted by priority level
    this.taxonGroups = source.taxonGroups && source.taxonGroups.map(TaxonGroupStrategy.fromObject) || [];
    this.taxonNames = source.taxonNames && source.taxonNames.map(TaxonNameStrategy.fromObject) || [];
  }

  equals(other: Strategy): boolean {
    return super.equals(other)
      // Or by functional attributes
      || (
        // Same label
        this.label === other.label
        // Same program
        && ((!this.programId && !other.programId) || this.programId === other.programId)
      );
  }
}

export class TaxonGroupStrategy {

  strategyId: number;
  priorityLevel: number;
  taxonGroup: TaxonGroupRef;

  static fromObject(source: any): TaxonGroupStrategy {
    if (!source || source instanceof TaxonGroupStrategy) return source;
    const res = new TaxonGroupStrategy();
    res.fromObject(source);
    return res;
  }

  asObject(opts?: ReferentialAsObjectOptions): any {
    const target: any = Object.assign({}, this); //= {...this};
    if (!opts || opts.keepTypename !== true) delete target.__typename;
    target.taxonGroup = this.taxonGroup && this.taxonGroup.asObject({ ...opts, ...MINIFY_OPTIONS });
    return target;
  }

  fromObject(source: any) {
    this.strategyId = source.strategyId;
    this.priorityLevel = source.priorityLevel;
    this.taxonGroup = source.taxonGroup && TaxonGroupRef.fromObject(source.taxonGroup);
  }
}

export class TaxonNameStrategy {

  strategyId: number;
  priorityLevel: number;
  taxonName: TaxonNameRef;

  static fromObject(source: any): TaxonNameStrategy {
    if (!source || source instanceof TaxonNameStrategy) return source;
    const res = new TaxonNameStrategy();
    res.fromObject(source);
    return res;
  }

  asObject(opts?: ReferentialAsObjectOptions): any {
    const target: any = Object.assign({}, this); //= {...this};
    if (!opts || opts.keepTypename !== true) delete target.__typename;
    target.taxonGroup = this.taxonName && this.taxonName.asObject({ ...opts, ...MINIFY_OPTIONS });
    return target;
  }

  fromObject(source: any) {
    this.strategyId = source.strategyId;
    this.priorityLevel = source.priorityLevel;
    this.taxonName = source.taxonName && TaxonNameRef.fromObject(source.taxonName);
  }
}

