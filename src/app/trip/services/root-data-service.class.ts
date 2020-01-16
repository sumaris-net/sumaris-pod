import {
  DataEntityAsObjectOptions,
  DataRootEntity,
  DataRootEntityUtils,
  Department,
  EntityUtils, isNil, MINIFY_OPTIONS,
  Person
} from "../../trip/services/model/base.model";
import {Injector} from "@angular/core";
import {BaseDataService} from "../../core/core.module";
import {AccountService} from "../../core/services/account.service";
import {GraphqlService} from "../../core/services/graphql.service";
import {DataQualityService} from "./base.service";
import {FormErrors} from "../../core/form/form.utils";
import {DataRootEntityValidatorService} from "./validator/base.validator";
import {Trip} from "./model/trip.model";
import {ValidatorService} from "angular4-material-table";


export abstract class RootDataService<T extends DataRootEntity<T>, F = any>
  extends BaseDataService<T, F>
  implements DataQualityService<T> {

  protected accountService: AccountService;

  protected constructor(
    injector: Injector
  ) {
    super(injector.get(GraphqlService));

    this.accountService = this.accountService || injector && injector.get(AccountService) || undefined;
  }

  canUserWrite(entity: T): boolean {
    if (!entity) return false;

    // If the user is the recorder: can write
    if (entity.recorderPerson && this.accountService.account.equals(entity.recorderPerson)) {
      return true;
    }

    // TODO: check rights on program (need model changes)
    return this.accountService.canUserWriteDataForDepartment(entity.recorderDepartment);
  }

  abstract control(entity: T, opts?: any): Promise<FormErrors>;
  abstract terminate(data: T): Promise<T>;
  abstract synchronize(data: T): Promise<T>;
  abstract validate(data: T): Promise<T>;
  abstract unvalidate(data: T): Promise<T>;
  abstract qualify(data: T, qualityFlagId: number): Promise<T>;

  /* -- protected methods -- */

  protected asObject(entity: T, opts?: DataEntityAsObjectOptions): any {
    const copy: any = entity.asObject({ ...MINIFY_OPTIONS, ...opts } as DataEntityAsObjectOptions);

    if (opts && opts.minify) {
      // Keep id only, on person and department
      copy.recorderPerson = {id: entity.recorderPerson && entity.recorderPerson.id};
      copy.recorderDepartment = entity.recorderDepartment && {id: entity.recorderDepartment && entity.recorderDepartment.id} || undefined;
    }

    return copy;
  }

  protected fillDefaultProperties(entity: T) {
    // If new entity
    const isNew = isNil(entity.id);
    if (isNew) {

      const person = this.accountService.person;

      // Recorder department
      if (person && person.department && !entity.recorderDepartment) {
        entity.recorderDepartment = person.department;
      }

      // Recorder person
      if (person && person.id && !entity.recorderPerson) {
        entity.recorderPerson = person;
      }
    }
  }

  protected copyIdAndUpdateDate(source: T | undefined, target: T) {

    EntityUtils.copyIdAndUpdateDate(source, target);

    // Copy control and validation date
    DataRootEntityUtils.copyControlAndValidationDate(source, target);

  }
}
