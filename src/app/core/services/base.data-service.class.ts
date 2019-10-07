import {Observable} from "rxjs";
import {R} from "apollo-angular/types";
import {ServiceError} from "./errors";
import {GraphqlService} from "./graphql.service";
import {environment} from "../../../environments/environment.prod";

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

  /**
   * @deprecated
   */
  protected mutate<T, V = R>(opts: {
    mutation: any,
    variables: V,
    error?: ServiceError
  }): Promise<T> {

    return this.graphql.mutate(opts);
  }

  protected subscribe<T, V = R>(opts: {
    query: any,
    variables: V,
    error?: ServiceError
  }): Observable<T> {
    return this.graphql.subscribe(opts);
  }

  protected addToQueryCache<V = R>(opts: {
    query: any,
    variables: V
  }, propertyName: string, newValue: any) {
    return this.graphql.addToQueryCache(opts, propertyName, newValue);
  }


  protected updateToQueryCache<V = R>(opts: {
    query: any,
    variables: V
  }, propertyName: string, newValue: any) {
    return this.graphql.updateToQueryCache(opts, propertyName, newValue);
  }

  protected addManyToQueryCache<V = R>(opts: {
    query: any,
    variables: V
  }, propertyName: string, newValues: any[]) {
    return this.graphql.addManyToQueryCache(opts, propertyName, newValues);
  }

  protected removeToQueryCacheById<V = R>(opts: {
    query: any,
    variables: V
  }, propertyName: string, idToRemove: number) {
    return this.graphql.removeToQueryCacheById(opts, propertyName, idToRemove);
  }

  protected removeToQueryCacheByIds<V = R>(opts: {
    query: any,
    variables: V
  }, propertyName: string, idsToRemove: number[]) {
    return this.graphql.removeToQueryCacheByIds(opts, propertyName, idsToRemove);
  }

}
