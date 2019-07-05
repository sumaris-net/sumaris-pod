import {DataRootEntity, Department, Entity, EntityUtils, Person} from "../../trip/services/model/base.model";
import {Injector} from "@angular/core";
import {AccountService, BaseDataService, GraphqlService} from "../../core/core.module";
import {EditorDataService} from "../../shared/services/data-service.class";
import {ObservedLocation} from "./model/observed-location.model";

export abstract class RootDataService<T extends DataRootEntity<T>, F = any> extends BaseDataService
  //implements TableDataService<T, F>, EditorDataService<T, F>
    {
  protected accountService: AccountService;

  protected constructor(
    injector: Injector
  ) {
    super(injector.get(GraphqlService));

    this.accountService = injector && injector.get(AccountService) || undefined;
  }
  //
  // load(id: number, options?: any): Promise<T> {
  //
  // }

  protected asObject(entity: T): any {
    const copy: any = entity.asObject(true/*minify*/);

    // Keep id only, on person and department
    copy.recorderPerson = {id: entity.recorderPerson && entity.recorderPerson.id};
    copy.recorderDepartment = entity.recorderDepartment && {id: entity.recorderDepartment && entity.recorderDepartment.id} || undefined;

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
  }
}
