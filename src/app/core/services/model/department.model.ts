import {EntityAsObjectOptions, EntityUtils} from "./entity.model";
import {BaseReferential, ReferentialAsObjectOptions} from "./referential.model";
import {EntityClass} from "./entity.decorators";
import {tar} from "@ionic/cli/lib/utils/archive";

@EntityClass({typename: 'DepartmentVO'})
export class Department extends BaseReferential<Department> {

  static ENTITY_NAME = "Department";
  static fromObject: (source: any, opts?: any) => Department;

  logo: string;
  siteUrl: string;

  constructor() {
    super(Department.TYPENAME);
    this.entityName = Department.ENTITY_NAME;
  }

  fromObject(source: any) {
    super.fromObject(source);
    this.entityName = source.entityName || Department.ENTITY_NAME;
    this.logo = source.logo;
    this.siteUrl = source.siteUrl;
    delete this.entityName; // not need
  }

  asObject(opts?: ReferentialAsObjectOptions): any {
    const target = super.asObject(opts);
    delete target.entityName;
    return target;
  }
}

export function departmentToString(obj: Department): string {
  return obj && obj.id && (obj.name) || undefined;
}

export function departmentsToString(data: Department[], separator?: string): string {
  return (data || []).map(departmentToString).join(separator || ", ");
}
