import {EntityAsObjectOptions, Referential, ReferentialRef} from "../../../core/services/model";
import {isNotNil} from "../../../shared/functions";


export declare type ParameterType = 'double' | 'string' | 'qualitative_value' | 'date' | 'boolean' ;

export declare type PmfmType = ParameterType | 'integer';


export class Pmfm extends Referential<Pmfm> {

  static TYPENAME = 'Pmfm';

  static fromObject(source: any): Pmfm {
    if (!source || source instanceof Pmfm) return source;
    const res = new Pmfm();
    res.fromObject(source);
    return res;
  }

  type: string | PmfmType;
  minValue: number;
  maxValue: number;
  defaultValue: number;
  maximumNumberDecimals: number;
  signifFiguresNumber: number;

  parameter: Parameter;
  matrix: ReferentialRef;
  fraction: ReferentialRef;
  method: ReferentialRef;
  unit: ReferentialRef;

  qualitativeValues: ReferentialRef[];

  constructor() {
    super();
    this.entityName = Pmfm.TYPENAME;
  }

  clone(): Pmfm {
    const target = new Pmfm();
    this.copy(target);
    target.qualitativeValues = this.qualitativeValues && this.qualitativeValues.map(qv => qv.clone()) || undefined;
    return target;
  }

  copy(target: Pmfm): Pmfm {
    target.fromObject(this);
    return target;
  }

  asObject(options?: EntityAsObjectOptions): any {
    const target: any = super.asObject({
      ...options,
      minify: false // Do NOT minify itself
    });

    if (options && options.minify) {
      target.parameterId = this.parameter && this.parameter.id;
      target.matrixId = this.matrix && this.matrix.id;
      target.fractionId = this.fraction && this.fraction.id;
      target.methodId = this.method && this.method.id;
      target.unitId = this.unit && this.unit.id;
      delete target.parameter;
      delete target.matrix;
      delete target.fraction;
      delete target.method;
      delete target.unit;
    }
    else {
      target.parameter = this.parameter && this.parameter.asObject(options);
      target.matrix = this.matrix && this.matrix.asObject(options);
      target.fraction = this.fraction && this.fraction.asObject(options);
      target.method = this.method && this.method.asObject(options);
      target.unit = this.unit && this.unit.asObject(options);
    }

    target.qualitativeValues = this.qualitativeValues && this.qualitativeValues.map(qv => qv.asObject(options)) || undefined;
    return target;
  }

  fromObject(source: any): Pmfm {
    super.fromObject(source);

    this.entityName = source.entityName || Pmfm.TYPENAME;
    this.type = source.type;
    this.minValue = source.minValue;
    this.maxValue = source.maxValue;
    this.defaultValue = source.defaultValue;
    this.maximumNumberDecimals = source.maximumNumberDecimals;
    this.signifFiguresNumber = source.signifFiguresNumber;

    this.parameter = source.parameter && Parameter.fromObject(source.parameter);
    this.matrix = source.matrix && ReferentialRef.fromObject(source.matrix);
    this.fraction = source.fraction && ReferentialRef.fromObject(source.fraction);
    this.method = source.method && ReferentialRef.fromObject(source.method);
    this.unit = source.unit && ReferentialRef.fromObject(source.unit);

    this.qualitativeValues = source.qualitativeValues && source.qualitativeValues.map(ReferentialRef.fromObject) || [];
    return this;
  }
}


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
