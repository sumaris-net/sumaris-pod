import {GraphqlService} from "./graphql.service";
import {FetchPolicy} from "apollo-client";
import {LoadResult} from "../../shared/services/data-service.class";
import {BehaviorSubject} from "rxjs";
import {isNil, isNotNil} from "../../shared/functions";


export abstract class BaseDataService<T = any, F = any> {

  protected _debug = false;
  protected _lastVariables: {
    [key: string]: {
      offset?: number;
      size?: number;
      sortBy?: string;
      sortDirection?: string;
      filter?: F;
      [key: string]: any;
    } | undefined
  } = {
    loadAll: undefined
  };

  protected constructor(
    protected graphql: GraphqlService
  ) {

    // for DEV only
   // this._debug = !environment.production;
  }


}
