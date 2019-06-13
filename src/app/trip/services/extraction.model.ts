/* -- Extraction -- */

import {Entity} from "../../core/services/model";

export class ExtractionType extends Entity<ExtractionType> {


  static equals(o1: ExtractionType, o2: ExtractionType): boolean {
    return o1 && o2 ? o1.label === o2.label && o1.category === o2.category : o1 === o2;
  }

  static fromObject(source: any): ExtractionType {
    const res = new ExtractionType();
    res.fromObject(source);
    return res;
  }

  label: string;
  category: string;
  name?: string;
  sheetNames?: string[];
  statusId: number;

  clone() {
    return this.copy(new ExtractionType());
  }

  copy(target: ExtractionType): ExtractionType {
    target.fromObject(this);
    return target;
  }

  fromObject(source: any): ExtractionType {
    super.fromObject(source);
    this.label = source.label;
    this.category = source.category;
    this.name = source.name;
    this.sheetNames = source.sheetNames;
    this.statusId = source.statusId;
    return this;
  }

  asObject(minify?: boolean): any {
    const target = super.asObject(minify);
    return target;
  }

  get format(): string {
    return this.label && this.label.split('-')[0] || undefined;
  }
}

export class ExtractionResult {
  columns: ExtractionColumn[];
  rows: ExtractionRow[];
  total: number;
}


export class ExtractionColumn {
  index?: number;
  name: string;
  type: string;
  description?: String;
  rankOrder: number;
}

export class ExtractionRow extends Array<any> {
  constructor() {
    super();
  }
}

export type StrataAreaType = 'area' | 'rect' | 'square';
export type StrataTimeType = 'year' | 'quarter' | 'month';

export class AggregationType extends ExtractionType {

  static fromObject(source: any): AggregationType {
    const res = new AggregationType();
    res.fromObject(source);
    return res;
  }

  strata: {
    space: StrataAreaType[];
    time: StrataTimeType[];
    tech: string[];
  };

  constructor() {
    super();
  }

  fromObject(source: any): AggregationType {
    super.fromObject(source);

    if (source.strata) {
      this.strata = this.strata || {
        space: source.space || [],
        time: source.time || [],
        tech: source.tech || [],
      };
    }
    return this;
  }

  asObject(minify?: boolean): any {
    const target = super.asObject(minify);
    return target;
  }
}

export class AggregationStrata {
  space: StrataAreaType;
  time: StrataTimeType;
  tech: string;
}
