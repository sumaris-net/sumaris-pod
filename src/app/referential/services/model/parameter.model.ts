import {BaseReferential, Referential, ReferentialUtils} from "../../../core/services/model/referential.model";
import {EntityAsObjectOptions} from "../../../core/services/model/entity.model";
import {isNotNil} from "../../../shared/functions";
import {EntityClass} from "../../../core/services/model/entity.decorators";

export declare type ParameterType = 'double' | 'string' | 'qualitative_value' | 'date' | 'boolean' ;

@EntityClass()
export class Parameter extends BaseReferential<Parameter> {

  static ENTITY_NAME = 'Parameter';
  static fromObject: (source: any, opts?: any) => Parameter;

  type: string | ParameterType;
  qualitativeValues: Referential[];

  constructor() {
    super();
    this.entityName = Parameter.ENTITY_NAME;
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
    this.entityName = source.entityName || Parameter.ENTITY_NAME;
    this.type = source.type;
    this.qualitativeValues = source.qualitativeValues && source.qualitativeValues.map(Referential.fromObject) || [];
    return this;
  }

  get isNumeric(): boolean {
    return isNotNil(this.type) && (this.type === 'double');
  }

  get isQualitative(): boolean {
    return isNotNil(this.type) && (this.type === 'qualitative_value');
  }
}
