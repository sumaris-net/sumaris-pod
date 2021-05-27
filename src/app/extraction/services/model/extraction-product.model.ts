/* -- Extraction -- */

import {Entity, EntityAsObjectOptions} from "../../../core/services/model/entity.model";
import {isNotEmptyArray, toBoolean} from "../../../shared/functions";
import {Moment} from "moment";
import {IWithRecorderDepartmentEntity, IWithRecorderPersonEntity} from "../../../data/services/model/model.utils";
import {ExtractionColumn, ExtractionFilter, ExtractionType} from "./extraction-type.model";
import {fromDateISOString, toDateISOString} from "../../../shared/dates";
import {EntityClass} from "../../../core/services/model/entity.decorators";

export type StrataAreaType = 'area' | 'statistical_rectangle' | 'sub_polygon' | 'square';
export type StrataTimeType = 'year' | 'quarter' | 'month';

export const ProcessingFrequencyIds = {
  NEVER: 0,
  MANUALLY: 1,
  DAILY: 2,
  WEEKLY: 3,
  MONTHLY: 4
};

export declare interface ProcessingFrequency {
  id: number;
  label: string;
}
export const ProcessingFrequencyList: ProcessingFrequency[] = [
  {
    id: ProcessingFrequencyIds.NEVER,
    label: 'EXTRACTION.AGGREGATION.EDIT.PROCESSING_FREQUENCY_ENUM.NEVER'
  },
  {
    id: ProcessingFrequencyIds.MANUALLY,
    label: 'EXTRACTION.AGGREGATION.EDIT.PROCESSING_FREQUENCY_ENUM.MANUALLY'
  },
  {
    id: ProcessingFrequencyIds.DAILY,
    label: 'EXTRACTION.AGGREGATION.EDIT.PROCESSING_FREQUENCY_ENUM.DAILY'
  },
  {
    id: ProcessingFrequencyIds.WEEKLY,
    label: 'EXTRACTION.AGGREGATION.EDIT.PROCESSING_FREQUENCY_ENUM.WEEKLY'
  },
  {
    id: ProcessingFrequencyIds.MONTHLY,
    label: 'EXTRACTION.AGGREGATION.EDIT.PROCESSING_FREQUENCY_ENUM.MONTHLY'
  }
];


@EntityClass({typename: 'AggregationTypeVO', fromObjectStrategy: "recreate"})
export class ExtractionProduct extends ExtractionType<ExtractionProduct>
  implements IWithRecorderDepartmentEntity<ExtractionProduct>,
             IWithRecorderPersonEntity<ExtractionProduct> {

  static fromObject: (source: any, opts?: any) => ExtractionProduct;

  category: 'PRODUCT' = null;
  filter: ExtractionFilter = null;
  documentation: string = null;
  processingFrequencyId: number = null;
  creationDate: Date | Moment = null;
  stratum: AggregationStrata[] = null;

  columns: ExtractionColumn[] = null;

  constructor() {
    super();
  }

  fromObject(source: any, opts?: EntityAsObjectOptions) {
    super.fromObject(source, opts);

    this.processingFrequencyId = source.processingFrequencyId;
    this.documentation = source.documentation;
    this.creationDate = fromDateISOString(source.creationDate);
    this.stratum = isNotEmptyArray(source.stratum) && source.stratum.map(AggregationStrata.fromObject) || [];
    this.filter = source.filter && (typeof source.filter === 'string') ? JSON.parse(source.filter) as ExtractionFilter : source.filter;
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
}
