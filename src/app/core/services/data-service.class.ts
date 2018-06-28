import { Observable, Subscription } from "rxjs";
import { Apollo } from "apollo-angular";
import { GraphQLError, DocumentNode } from "graphql";
import { ApolloQueryResult } from "apollo-client";
import { R } from "apollo-angular/types";
import { ErrorCodes, ServiceError } from "./errors";
import { map } from "rxjs/operators";
import { Entity } from "./model";

export declare interface DataService<T, F> {

  loadAll(
    offset: number,
    size: number,
    sortBy?: string,
    sortDirection?: string,
    filter?: F,
    options?: any
  ): Observable<T[]>;

  saveAll(data: T[], options?: any): Promise<T[]>;

  deleteAll(data: T[], options?: any): Promise<any>;
}


export class BaseDataService {

  constructor(
    protected apollo: Apollo
  ) {

  }

  protected query<T, V = R>(opts: {
    query: DocumentNode,
    variables: V,
    error?: ServiceError
  }): Promise<T> {
    //this.apollo.getClient().cache.reset();
    return new Promise<T>((resolve, reject) => {
      const subscription: Subscription = this.apollo.query<ApolloQueryResult<T>, V>({
        query: opts.query,
        variables: opts.variables
      })
        .catch(this.onApolloError)
        .subscribe(({ data, errors }) => {
          subscription.unsubscribe();

          if (errors) {
            if (errors[0].message == "ERROR.UNKNOWN_NETWORK_ERROR") {
              reject({
                code: ErrorCodes.UNKNOWN_NETWORK_ERROR,
                message: "ERROR.UNKNOWN_NETWORK_ERROR"
              });
              return;
            }
            console.error("[account] " + errors[0].message);
            reject(opts.error ? opts.error : errors[0].message);
            return;
          }
          resolve(data as T);
        });
    });
  }

  protected watchQuery<T, V = R>(opts: {
    query: DocumentNode,
    variables: V,
    error?: ServiceError
  }): Observable<T> {
    //this.apollo.getClient().cache.reset();
    return this.apollo.watchQuery<T, V>({
      query: opts.query,
      variables: opts.variables
    })
      .valueChanges
      .pipe(
        map(({ data, errors }) => {
          if (errors) {
            if (errors[0].message == "ERROR.UNKNOWN_NETWORK_ERROR") {
              throw {
                code: ErrorCodes.UNKNOWN_NETWORK_ERROR,
                message: "ERROR.UNKNOWN_NETWORK_ERROR"
              };
            }
            console.error("[data] " + errors[0].message);
            throw opts.error ? opts.error : errors[0].message;
          }
          return data;
        })
      )
      ;
  }

  protected mutate<T, V = R>(opts: { mutation: DocumentNode, variables: V, error?: ServiceError }): Promise<T> {
    return new Promise<T>((resolve, reject) => {
      let subscription = this.apollo.mutate<ApolloQueryResult<T>, V>({
        mutation: opts.mutation,
        variables: opts.variables
      })
        .catch(this.onApolloError)
        .subscribe(({ data, errors }) => {
          if (errors) {
            if (errors[0].message == "ERROR.UNKNOWN_NETWORK_ERROR") {
              reject(errors[0]);
            }
            else {
              console.error("[base-service] " + errors[0].message);
              reject(opts.error ? opts.error : errors[0].message);
            }
          }
          else {
            resolve(data as T);
          }
          subscription.unsubscribe();
        });
    });
  }

  protected addToQueryCache<V = R>(opts: {
    query: DocumentNode,
    variables: V
  }, propertyName: string, newValue: any) {
    const values = this.apollo.getClient().readQuery(opts);

    if (values)
      if (values && values[propertyName]) {
        values[propertyName].push(newValue);

        this.apollo.getClient().writeQuery({
          query: opts.query,
          variables: opts.variables,
          data: values
        });
      }
  }

  protected removeToQueryCacheById<V = R>(opts: {
    query: DocumentNode,
    variables: V
  }, propertyName: string, idToRemove: number) {

    const values = this.apollo.getClient().readQuery(opts);

    if (values && values[propertyName]) {

      values[propertyName] = (values[propertyName] || []).reduce((result: any[], item: any) => {
        return item['id'] == idToRemove ? result : result.concat(item);
      }, []);
      this.apollo.getClient().writeQuery({
        query: opts.query,
        variables: opts.variables,
        data: values
      });
    }
    else {
      console.warn("[data-service] Unable to remove id from cache. Please check {" + propertyName + "} exists in the result:", opts.query);
    }
  }

  protected removeToQueryCacheByIds<V = R>(opts: {
    query: DocumentNode,
    variables: V
  }, propertyName: string, idsToRemove: number[]) {

    const values = this.apollo.getClient().readQuery(opts);

    if (values && values[propertyName]) {

      values[propertyName] = (values[propertyName] || []).reduce((result: any[], item: any) => {
        return idsToRemove.indexOf(item['id']) === -1 ? result.concat(item) : result;
      }, []);
      this.apollo.getClient().writeQuery({
        query: opts.query,
        variables: opts.variables,
        data: values
      });
    }
    else {
      console.warn("[data-service] Unable to remove ids from cache. Please check {" + propertyName + "} exists in the result:", opts.query);
    }
  }

  private onApolloError<T>(err: any): Observable<ApolloQueryResult<T>> {
    let result: ApolloQueryResult<T>;
    if (err && err.networkError) {
      console.error("[base-service] " + err.networkError.message);
      result = {
        data: null,
        errors: [new GraphQLError("ERROR.UNKNOWN_NETWORK_ERROR")],
        loading: false,
        networkStatus: err.networkStatus,
        stale: err.stale
      };
    }
    else {
      result = {
        data: null,
        errors: [err as GraphQLError],
        loading: false,
        networkStatus: err.networkStatus,
        stale: err.stale
      };
    }
    return Observable.of(result);
  }

  private onApolloError2<T>(err: any): Observable<T> {
    let result: T;
    if (err && err.networkError) {
      console.error("[base-service] " + err.networkError.message);
      throw new GraphQLError("ERROR.UNKNOWN_NETWORK_ERROR");
    }
    else {
      throw err as GraphQLError;
    }
  }
}