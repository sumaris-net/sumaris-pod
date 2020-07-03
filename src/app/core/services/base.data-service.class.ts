import {GraphqlService} from "./graphql.service";

export interface LoadAllVariables<F= any> {
  offset?: number;
  size?: number;
  sortBy?: string;
  sortDirection?: string;
  filter?: F;
  [key: string]: any;
}

export abstract class BaseDataService<T = any, F = any> {

  protected _debug = false;
  protected _lastVariables: {
    [key: string]: LoadAllVariables<F> | undefined
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
