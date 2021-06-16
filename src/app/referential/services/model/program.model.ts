import {EntityUtils}  from "@sumaris-net/ngx-components";
import {BaseReferential, ReferentialAsObjectOptions, ReferentialRef}  from "@sumaris-net/ngx-components";
import {FormFieldDefinition} from "@sumaris-net/ngx-components";
import {Strategy} from "./strategy.model";
import {PropertiesMap} from "@sumaris-net/ngx-components";
import {isNotNil} from "@sumaris-net/ngx-components";
import {EntityClass}  from "@sumaris-net/ngx-components";

@EntityClass({typename: 'ProgramVO'})
export class Program extends BaseReferential<Program> {

  static fromObject: (source: any, opts?: any) => Program;

  properties: PropertiesMap = {};
  gearClassification: ReferentialRef = null;
  taxonGroupType: ReferentialRef = null;
  locationClassifications: ReferentialRef[] = null;
  locations: ReferentialRef[] = null;
  strategies: Strategy[] = null;

  constructor() {
    super();
    this.__typename = Program.TYPENAME;
  }

  asObject(opts?: ReferentialAsObjectOptions): any {
    if (opts && opts.minify) {
      return {
        id: this.id,
        __typename: opts.keepTypename && this.__typename || undefined
      };
    }
    const target: any = super.asObject(opts);
    target.properties = {...this.properties};
    target.gearClassification = this.gearClassification && this.gearClassification.asObject(opts);
    target.taxonGroupType = this.taxonGroupType && this.taxonGroupType.asObject(opts);
    target.locationClassifications = this.locationClassifications  && this.locationClassifications.map(item => item.asObject(opts)) || [];
    target.locations = this.locations && this.locations.map(item => item.asObject(opts)) || [];

    target.strategies = this.strategies && this.strategies.map(s => s.asObject(opts));
    return target;
  }

  fromObject(source: any) {
    super.fromObject(source);
    if (source.properties && source.properties instanceof Array) {
      this.properties = EntityUtils.getPropertyArrayAsObject(source.properties);
    } else {
      this.properties = {...source.properties};
    }
    this.gearClassification = source.gearClassification && ReferentialRef.fromObject(source.gearClassification);
    this.taxonGroupType = (source.taxonGroupType && ReferentialRef.fromObject(source.taxonGroupType)) ||
      (isNotNil(source.taxonGroupTypeId) ? ReferentialRef.fromObject({id: source.taxonGroupTypeId}) : undefined);
    this.locationClassifications = source.locationClassifications  && source.locationClassifications.map(ReferentialRef.fromObject) || [];
    this.locations = source.locations && source.locations.map(ReferentialRef.fromObject) || [];

    this.strategies = source.strategies && source.strategies.map(Strategy.fromObject) || [];
  }

  equals(other: Program): boolean {
    return super.equals(other) || this.label === other.label;
  }

  getPropertyAsBoolean(definition: FormFieldDefinition): boolean {
    const value = this.getProperty(definition);
    return isNotNil(value) ? (value && value !== "false") : undefined;
  }

  getPropertyAsInt(definition: FormFieldDefinition): number {
    const value = this.getProperty(definition);
    return isNotNil(value) ? parseInt(value) : undefined;
  }

  getPropertyAsNumbers(definition: FormFieldDefinition): number[] {
    const value = this.getProperty(definition);
    if (typeof value === 'string') return value.split(',').map(parseFloat) || undefined;
    return isNotNil(value) ? [parseFloat(value)] : undefined;
  }

  getPropertyAsStrings(definition: FormFieldDefinition): string[] {
    const value = this.getProperty(definition);
    return value && value.split(',') || undefined;
  }

  getProperty<T = string>(definition: FormFieldDefinition): T {
    if (!definition) throw new Error("Missing 'definition' argument");
    return isNotNil(this.properties[definition.key]) ? this.properties[definition.key] : (definition.defaultValue || undefined);
  }
}
