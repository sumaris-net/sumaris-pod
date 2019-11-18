import {GraphqlService} from "./graphql.service";

export const OPTIMISTIC_AS_OBJECT_OPTIONS = {

}

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
