import {EntityAsObjectOptions} from "./entity.model";
import {Referential} from "./referential.model";

export class Department extends Referential<Department> {

  static fromObject(source: any): Department {
    if (!source || source instanceof Department) return source;
    const res = new Department();
    res.fromObject(source);
    return res;
  }

  logo: string;
  siteUrl: string;

  constructor() {
    super();
    this.__typename = 'DepartmentVO';
  }

  clone(): Department {
    const target = new Department();
    target.fromObject(this);
    return target;
  }

  asObject(options?: EntityAsObjectOptions): any {
    const target: any = super.asObject(options);
    return target;
  }

  fromObject(source: any) {
    super.fromObject(source);
    this.logo = source.logo;
    this.siteUrl = source.siteUrl;
    delete this.entityName; // not need
  }
}
