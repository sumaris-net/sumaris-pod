/* -- Extraction -- */

import {Department, Entity, Person} from "../../core/services/model";
import {IWithRecorderDepartmentEntity, IWithRecorderPersonEntity, toDateISOString} from "./model/base.model";

export class ExtractionType<T extends ExtractionType<any> = ExtractionType<any>> extends Entity<T> {


  static equals(o1: ExtractionType, o2: ExtractionType): boolean {
    return o1 && o2 ? o1.label === o2.label && o1.category === o2.category : o1 === o2;
  }

  static fromObject(source: any): ExtractionType {
    const res = new ExtractionType();
    res.fromObject(source);
    return res;
  }

  category: 'product' | 'live';
  label: string;
  name?: string;
  sheetNames?: string[];
  statusId: number;
  isSpatial: boolean;

  recorderDepartment: Department;

  constructor() {
    super();
    this.recorderDepartment = null;
  }

  clone(): T {
    return this.copy(new ExtractionType() as T);
  }

  copy(target: T): T {
    target.fromObject(this);
    return target;
  }

  fromObject(source: any): ExtractionType<T> {
    super.fromObject(source);
    this.label = source.label;
    this.category = source.category;
    this.name = source.name;
    this.sheetNames = source.sheetNames;
    this.statusId = source.statusId;
    this.isSpatial = source.isSpatial;
    this.recorderDepartment = source.recorderDepartment && Department.fromObject(source.recorderDepartment);
    return this;
  }

  asObject(minify?: boolean): any {
    const target = super.asObject(minify);
    target.recorderDepartment = this.recorderDepartment && this.recorderDepartment.asObject(minify) || undefined;
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
  columnName: string;
  type: string;
  description?: String;
  rankOrder: number;
  values?: string[];
}

export class ExtractionRow extends Array<any> {
  constructor() {
    super();
  }
}

export type StrataAreaType = 'area' | 'rect' | 'square';
export type StrataTimeType = 'year' | 'quarter' | 'month';

export class AggregationType extends ExtractionType<AggregationType>
  implements IWithRecorderDepartmentEntity<AggregationType>,
             IWithRecorderPersonEntity<AggregationType> {

  static fromObject(source: any): AggregationType {
    const res = new AggregationType();
    res.fromObject(source);
    return res;
  }

  description: string;
  recorderPerson: Person;
  strata: {
    space: StrataAreaType[];
    time: StrataTimeType[];
    tech: string[];
  };

  constructor() {
    super();
    this.recorderPerson = null;
  }

  clone(): AggregationType {
    return this.copy(new AggregationType());
  }

  fromObject(source: any): AggregationType {
    super.fromObject(source);

    this.label = source.label;
    this.category = source.category;
    this.name = source.name;
    this.sheetNames = source.sheetNames;
    this.statusId = source.statusId;
    this.description = source.description;
    this.recorderPerson = source.recorderPerson && Person.fromObject(source.recorderPerson) || null;

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

    target.recorderPerson = this.recorderPerson && this.recorderPerson.asObject(minify) || undefined;
    return target;
  }
}

export class AggregationStrata {
  space: StrataAreaType;
  time: StrataTimeType;
  tech: string;
}
