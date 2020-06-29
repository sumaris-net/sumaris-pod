import {Referential, ReferentialUtils} from "../../../core/services/model/referential.model";
import {EntityAsObjectOptions} from "../../../core/services/model/entity.model";
import {isNotNil} from "../../../shared/functions";

export declare type ParameterType = 'double' | 'string' | 'qualitative_value' | 'date' | 'boolean' ;

export class Parameter extends Referential<Parameter> {

  static TYPENAME = 'Parameter';

  static fromObject(source: any): Parameter {
    if (!source || source instanceof Parameter) return source;
    const res = new Parameter();
    res.fromObject(source);
    return res;
  }

  type: string | ParameterType;
  qualitativeValues: Referential[];

  constructor() {
    super();
    this.entityName = Parameter.TYPENAME;
  }

  clone(): Parameter {
    const target = new Parameter();
    target.fromObject(this);
    target.qualitativeValues = this.qualitativeValues && this.qualitativeValues.map(qv => qv.clone()) || undefined;
    return target;
  }

  asObject(options?: EntityAsObjectOptions): any {
    const target: any = super.asObject(options);
    target.qualitativeValues = this.qualitativeValues && this.qualitativeValues.map(qv => qv.asObject(options)) || undefined;
    return target;
  }

  fromObject(source: any): Parameter {
    super.fromObject(source);

    this.type = source.type;
    this.entityName = source.entityName || Parameter.TYPENAME;

    this.qualitativeValues = source.qualitativeValues && source.qualitativeValues.map(ReferentialUtils.fromObject) || [];
    return this;
  }

  get isNumeric(): boolean {
    return isNotNil(this.type) && (this.type === 'double');
  }

  get isQualitative(): boolean {
    return isNotNil(this.type) && (this.type === 'qualitative_value');
  }
}
