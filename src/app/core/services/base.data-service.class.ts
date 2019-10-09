import {GraphqlService} from "./graphql.service";

export abstract class BaseDataService<T = any> {

  protected _debug = false;
  protected _lastVariables: any = {
    loadAll: undefined,
    load: undefined
  };

  protected constructor(
    protected graphql: GraphqlService
  ) {

    // for DEV only
   // this._debug = !environment.production;
  }
}
