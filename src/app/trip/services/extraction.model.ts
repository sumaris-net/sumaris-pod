/* -- Extraction -- */

import {Department, Entity, Person} from "../../core/services/model";
import {isNotNil, IWithRecorderDepartmentEntity, IWithRecorderPersonEntity, toDateISOString} from "./model/base.model";
import {arraySize, isNotEmptyArray, toBoolean} from "../../shared/functions";
import {Validators} from "@angular/forms";
import {Moment} from "moment";

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
  id: number;
  creationDate: Moment;
  index?: number;
  label: string;
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
  stratum: AggregationStrata[];
  columns: ExtractionColumn[];

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

    this.stratum = isNotEmptyArray(source.stratum) && source.stratum.map(AggregationStrata.fromObject) || [];

    return this;
  }

  asObject(minify?: boolean): any {
    const target = super.asObject(minify);

    target.recorderPerson = this.recorderPerson && this.recorderPerson.asObject(minify) || undefined;
    target.stratum = this.stratum && this.stratum.map(s => s.asObject(minify)) || undefined;
    target.columns = this.columns && this.columns.map((c: any) => {
      const json = Object.assign({}, c);
      delete json.index;
      delete json.__typename;
      return json;
    }) || undefined;
    return target;
  }
}

export class AggregationStrata extends Entity<AggregationStrata> {

  static fromObject(source: any): AggregationStrata {
    const res = new AggregationStrata();
    res.fromObject(source);
    return res;
  }

  label: string;
  isDefault: boolean;
  sheetName: string;

  spaceColumnName: StrataAreaType;
  timeColumnName: StrataTimeType;
  techColumnName?: string;
  aggColumnName?: string;
  aggFunction?: string;

  constructor() {
    super();
  }

  copy(target: AggregationStrata): AggregationStrata {
    target.fromObject(this);
    return target;
  }

  clone(): AggregationStrata {
    return this.copy(new AggregationStrata());
  }

  fromObject(source: any): AggregationStrata {
    super.fromObject(source);
    this.label = source.label;
    this.sheetName = source.sheetName;
    this.isDefault = isNotNil(source.isDefault) ? source.isDefault : ('default' === source.label);
    this.spaceColumnName = source.spaceColumnName;
    this.timeColumnName = source.timeColumnName;
    this.techColumnName = source.techColumnName;
    this.aggColumnName = source.aggColumnName;
    this.aggFunction = source.aggFunction;
    return this;
  }

  asObject(minify?: boolean): any {
    const target = super.asObject(minify);
    return target;
  }
}

export declare class ExtractionFilter {
  searchText?: string;
  criteria?: ExtractionFilterCriterion[];
  sheetName?: string;
}

export class ExtractionFilterCriterion extends Entity<ExtractionFilterCriterion> {

  static fromObject(source: any): ExtractionFilterCriterion {
    const res = new ExtractionFilterCriterion();
    res.fromObject(source);
    return res;
  }

  name?: string;
  operator: string;
  value?: string;
  values?: string[];
  endValue?: string;
  sheetName?: string;

  constructor() {
    super();
  }

  copy(target: ExtractionFilterCriterion): ExtractionFilterCriterion {
    target.fromObject(this);
    return target;
  }

  clone(): ExtractionFilterCriterion {
    return this.copy(new ExtractionFilterCriterion());
  }

  fromObject(source: any): ExtractionFilterCriterion {
    super.fromObject(source);
    this.name = source.name;
    this.operator = source.operator;
    this.value = source.value;
    this.endValue = source.endValue;
    this.sheetName = source.sheetName;
    return this;
  }

  asObject(minify?: boolean): any {
    const target = super.asObject(minify);
    return target;
  }
}

export const SPACE_STRATA_COLUMNS: string[] = ['rect', 'statistical_rectangle', 'square', 'area'];
export const TIME_STRATA_COLUMNS:   string[] = ['year', 'quarter', 'month'];

export class ExtractionUtils {

  static dispatchColumns(columns: ExtractionColumn[], opts?: {
    excludeIfNoValue?: boolean
  }): {
    timeColumns: ExtractionColumn[];
    spaceColumns: ExtractionColumn[];
    aggColumns: ExtractionColumn[];
    techColumns: ExtractionColumn[];
    criteriaColumns: ExtractionColumn[];} {

    const excludeIfNoValue = toBoolean(opts && opts.excludeIfNoValue, false);

    const timeColumns = columns.filter(c => TIME_STRATA_COLUMNS.includes(c.columnName));
    const spaceColumns = columns.filter(c => SPACE_STRATA_COLUMNS.includes(c.columnName)
      // Exclude column if no value
      && (!excludeIfNoValue || isNotEmptyArray(c.values)));

    // Aggregation columns (numeric columns)
    const aggColumns = columns.filter(c =>
      (!c.type || c.type === 'integer' || c.type === 'double')
      && (c.columnName.endsWith('_count')
      || c.columnName.endsWith('_time')
      || c.columnName.endsWith('_weight')));

    const excludedFilterColumns = spaceColumns.concat(timeColumns);
    const techColumns = columns.filter(c => !excludedFilterColumns.includes(c) && c.type === 'string');
    const criteriaColumns = columns.filter(c => !excludedFilterColumns.includes(c)
      // Exclude string column if only one value
      && (c.type !== 'string' || !excludeIfNoValue || arraySize(c.values) > 1));

    return {
      timeColumns,
      spaceColumns,
      aggColumns,
      techColumns,
      criteriaColumns
    };
  }

  static asQueryParams(type: ExtractionType|AggregationType, filter?: ExtractionFilter): any {
    const queryParams: any = {
      category: type && type.category,
      label: type && type.label
    };
    if (filter.sheetName) {
      queryParams.sheet = filter.sheetName;
    }
    if (isNotEmptyArray(filter.criteria)) {
      queryParams.q = filter.criteria.reduce((res, criterion) => {
        if (criterion.endValue) {
          return res.concat(`${criterion.name}${criterion.operator}${criterion.value}:${criterion.endValue}`);
        } else {
          return res.concat(`${criterion.name}${criterion.operator}${criterion.value}`);
        }
      }, []).join(";");
    }
    return queryParams;
  }
}

