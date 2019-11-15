import {
  DataEntityAsObjectOptions,
  DataRootEntity,
  DataRootEntityUtils,
  Department,
  EntityUtils, MINIFY_OPTIONS,
  Person
} from "../../trip/services/model/base.model";
import {Injector} from "@angular/core";
import {BaseDataService} from "../../core/core.module";
import {AccountService} from "../../core/services/account.service";
import {GraphqlService} from "../../core/services/graphql.service";

export abstract class RootDataService<T extends DataRootEntity<T>, F = any> extends BaseDataService<T, F> {

  protected accountService: AccountService;

  protected constructor(
    injector: Injector
  ) {
    super(injector.get(GraphqlService));

    this.accountService = injector && injector.get(AccountService) || undefined;
  }

  protected asObject(entity: T, options?: DataEntityAsObjectOptions): any {
    const copy: any = entity.asObject({ ...MINIFY_OPTIONS, options } as DataEntityAsObjectOptions);

    // Keep id only, on person and department
    //copy.recorderPerson = {id: entity.recorderPerson && entity.recorderPerson.id};
    //copy.recorderDepartment = entity.recorderDepartment && {id: entity.recorderDepartment && entity.recorderDepartment.id} || undefined;

    return copy;
  }

  protected fillDefaultProperties(entity: T) {
    // If new trip
    if (!entity.id || entity.id < 0) {

      const person: Person = this.accountService.account;

      // Recorder department
      if (person && person.department && !entity.recorderDepartment) {
        entity.recorderDepartment = Department.fromObject({id: person.department.id});
      }

      // Recorder person
      if (person && person.id && !entity.recorderPerson) {
        entity.recorderPerson = Person.fromObject({id: person.id});
      }
    }
  }

  protected copyIdAndUpdateDate(source: T | undefined, target: T) {

    EntityUtils.copyIdAndUpdateDate(source, target);

    // Copy control and validation date
    DataRootEntityUtils.copyControlAndValidationDate(source, target);

  }
}
