/* -- Extraction -- */

import {Entity, EntityAsObjectOptions} from "../../../core/services/model/entity.model";
import {fromDateISOString, isNotEmptyArray, toBoolean, toDateISOString} from "../../../shared/functions";
import {Moment} from "moment";
import {IWithRecorderDepartmentEntity, IWithRecorderPersonEntity} from "../../../data/services/model/model.utils";
import {ExtractionCategories, ExtractionColumn, ExtractionFilter, ExtractionType} from "./extraction.model";

export type StrataAreaType = 'area' | 'statistical_rectangle' | 'sub_polygon' | 'square';
export type StrataTimeType = 'year' | 'quarter' | 'month';

export class AggregationType extends ExtractionType<AggregationType>
  implements IWithRecorderDepartmentEntity<AggregationType>,
             IWithRecorderPersonEntity<AggregationType> {

  static TYPENAME = 'AggregationTypeVO';

  static fromObject(source: any): AggregationType {
    if (!source) return source;
    const res = new AggregationType();
    res.fromObject(source);
    return res;
  }

  category: 'PRODUCT';
  filter: ExtractionFilter;
  documentation: string;
  creationDate: Date | Moment;
  stratum: AggregationStrata[];

  columns: ExtractionColumn[];

  constructor() {
    super();
    this.__typename = AggregationType.TYPENAME;
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
    this.filter = source.filter && (typeof source.filter === 'string') ? JSON.parse(source.filter) as ExtractionFilter : source.filter;

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
    target.filter = this.filter && (typeof this.filter === 'object') ? JSON.stringify(this.filter) : this.filter;
    return target;
  }
}

export declare interface IAggregationStrata {
  spatialColumnName: StrataAreaType;
  timeColumnName: StrataTimeType;
  techColumnName?: string;
  aggColumnName?: string;
  aggFunction?: string;
}

export class AggregationStrata extends Entity<AggregationStrata> implements IAggregationStrata {

  static TYPENAME = 'AggregationStrataVO';

  static fromObject(source: any): AggregationStrata {
    if (!source) return source;
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
    this.__typename = AggregationStrata.TYPENAME;
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
