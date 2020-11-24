/* -- Extraction -- */

import {Entity, EntityAsObjectOptions} from "../../../core/services/model/entity.model";
import {Department} from "../../../core/services/model/department.model";
import {Person} from "../../../core/services/model/person.model";
import {
  arraySize, fromDateISOString,
  isNil,
  isNotEmptyArray,
  isNotNil,
  toBoolean,
  toDateISOString,
  toNumber
} from "../../../shared/functions";
import {Moment} from "moment";
import {IWithRecorderDepartmentEntity, IWithRecorderPersonEntity} from "../../../data/services/model/model.utils";
import {tar} from "@ionic/cli/lib/utils/archive";

export declare type ExtractionCategoryType = 'PRODUCT' | 'LIVE';
export const ExtractionCategories = {
  PRODUCT: 'PRODUCT',
  LIVE: 'LIVE',
}

export class ExtractionType<T extends ExtractionType<any> = ExtractionType<any>> extends Entity<T> {


  static equals(o1: ExtractionType, o2: ExtractionType): boolean {
    return o1 && o2 ? o1.label === o2.label && o1.category === o2.category : o1 === o2;
  }

  static fromObject(source: any): ExtractionType {
    const res = new ExtractionType();
    res.fromObject(source);
    return res;
  }

  category: string;
  label: string;
  name?: string;
  version?: string;
  sheetNames: string[];
  statusId: number;
  isSpatial: boolean;
  docUrl: string;
  description: string;
  comments:  string;

  recorderPerson: Person;
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
    this.description = source.description;
    this.comments = source.comments;
    this.version = source.version;
    this.sheetNames = source.sheetNames;
    this.statusId = source.statusId;
    this.isSpatial = source.isSpatial;
    this.docUrl = source.docUrl;
    this.recorderPerson = source.recorderPerson && Person.fromObject(source.recorderPerson) || null;
    this.recorderDepartment = source.recorderDepartment && Department.fromObject(source.recorderDepartment);
    return this;
  }

  asObject(options?: EntityAsObjectOptions): any {
    const target = super.asObject(options);
    target.recorderPerson = this.recorderPerson && this.recorderPerson.asObject(options) || undefined;
    target.recorderDepartment = this.recorderDepartment && this.recorderDepartment.asObject(options) || undefined;
    return target;
  }

  get format(): string {
    return this.label && this.label.split('-')[0] || undefined;
  }
}

export class ExtractionResult {

  static fromObject(source: any): ExtractionResult {
    if (!source || source instanceof ExtractionResult) return source;
    const target = new ExtractionResult();
    target.fromObject(source);
    return target;
  }

  columns: ExtractionColumn[];
  rows: ExtractionRow[];
  total: number;

  fromObject(source: any): ExtractionResult {
    this.total = source.total;
    this.columns = source.columns && source.columns.map(ExtractionColumn.fromObject) || null;
    this.rows = source.rows && source.rows.slice() || null;
    return this;
  }
}


export class ExtractionColumn {
  static fromObject(source: any): ExtractionColumn {
    if (!source || source instanceof ExtractionColumn) return source;
    const target = new ExtractionColumn();
    target.fromObject(source);
    return target;
  }

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

  fromObject(source: any): ExtractionColumn {
    this.id = source.id;
    this.creationDate = source.creationDate;
    this.index = source.index;
    this.label = source.label;
    this.name = source.name;
    this.columnName = source.columnName;
    this.type = source.type;
    this.description = source.description;
    this.rankOrder = source.rankOrder;
    this.values = source.values && source.values.slice();
    return this;
  }
}

export class ExtractionRow extends Array<any> {
  constructor(...items: any[]) {
    super(...items);
  }
}

export type StrataAreaType = 'area' | 'statistical_rectangle' | 'sub_polygon' | 'square';
export type StrataTimeType = 'year' | 'quarter' | 'month';

export class AggregationType extends ExtractionType<AggregationType>
  implements IWithRecorderDepartmentEntity<AggregationType>,
             IWithRecorderPersonEntity<AggregationType> {

  static fromObject(source: any): AggregationType {
    const res = new AggregationType();
    res.fromObject(source);
    return res;
  }

  documentation: string;
  creationDate: Date | Moment;
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

    this.documentation = source.documentation;
    this.creationDate = fromDateISOString(source.creationDate);
    this.stratum = isNotEmptyArray(source.stratum) && source.stratum.map(AggregationStrata.fromObject) || [];

    return this;
  }

  asObject(options?: EntityAsObjectOptions): any {
    const target = super.asObject(options);

    target.creationDate = toDateISOString(this.creationDate);
    target.stratum = this.stratum && this.stratum.map(s => s.asObject(options)) || undefined;
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

  isDefault: boolean;
  sheetName: string;

  spatialColumnName: StrataAreaType;
  timeColumnName: StrataTimeType;
  aggColumnName: string;
  aggFunction: string;
  techColumnName: string;

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
    this.sheetName = source.sheetName;
    this.isDefault = toBoolean(source.isDefault, false);
    this.spatialColumnName = source.spatialColumnName;
    this.timeColumnName = source.timeColumnName;
    this.aggColumnName = source.aggColumnName;
    this.aggFunction = source.aggFunction;
    this.techColumnName = source.techColumnName;
    return this;
  }

  asObject(options?: EntityAsObjectOptions): any {
    const target = super.asObject(options);
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

  asObject(options?: EntityAsObjectOptions): any {
    return super.asObject(options);
  }
}

export const SPATIAL_COLUMNS: string[] = [
  'area',
  'statistical_rectangle',
  'sub_polygon',
  'square'];
export const TIME_COLUMNS:   string[] = ['year', 'quarter', 'month'];
export const IGNORED_COLUMNS:   string[] = ['record_type'];

export class ExtractionUtils {

  static dispatchColumns(columns: ExtractionColumn[]): {
    timeColumns: ExtractionColumn[];
    spatialColumns: ExtractionColumn[];
    aggColumns: ExtractionColumn[];
    techColumns: ExtractionColumn[];
    criteriaColumns: ExtractionColumn[];} {

    const timeColumns = columns.filter(c => TIME_COLUMNS.includes(c.columnName));
    const spatialColumns = columns.filter(c => SPATIAL_COLUMNS.includes(c.columnName));

    // Aggregation columns (numeric columns)
    const aggColumns = columns.filter(c =>
      (!c.type || c.type === 'integer' || c.type === 'double')
      && (c.columnName.endsWith('_count')
      || c.columnName.indexOf('_count_by_') != -1
      || c.columnName.endsWith('_time')
      || c.columnName.endsWith('_weight')
      || c.columnName.endsWith('_length')
      || c.columnName.endsWith('_value')));

    const excludedFilterColumns = spatialColumns
      .concat(timeColumns);

    const techColumns = columns.filter(c => !excludedFilterColumns.includes(c)
      && !IGNORED_COLUMNS.includes(c.columnName)
      && (c.type === 'string' || (c.columnName.endsWith('_class'))));
    const criteriaColumns = columns.filter(c => !excludedFilterColumns.includes(c)
      && !IGNORED_COLUMNS.includes(c.columnName));

    return {
      timeColumns,
      spatialColumns,
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

  static filterWithValues(columns: ExtractionColumn[]) {
    return this.filterValuesMinSize(columns, 1);
  }

  static filterValuesMinSize(columns: ExtractionColumn[], minSize: number) {
    return (columns || []).filter(c => arraySize(c.values) >= minSize);
  }
}

