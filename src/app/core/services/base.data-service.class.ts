import {GraphqlService, MutateQueryOptions, WatchQueryOptions} from "../graphql/graphql.service";
import {Page} from "../../shared/services/entity-service.class";
import {EmptyObject} from "apollo-angular/types";
import {Observable} from "rxjs";
import {DataProxy} from "apollo-cache";
import {FetchResult} from "apollo-link";
import {environment} from "../../../environments/environment";
import {EntityUtils} from "./model/entity.model";
import {ApolloCache} from "@apollo/client/core";

const sha256 =  require('hash.js/lib/hash/sha/256');

export interface QueryVariables<F= any> extends Partial<Page> {
  filter?: F;
  [key: string]: any;
}

export interface MutateQueryWithCacheUpdateOptions<T = any, V = EmptyObject> extends MutateQueryOptions<T, V> {
  cacheInsert?: {
    query: any;
    fetchData: (variables: V, mutationResult: FetchResult<T>) => any;
  }[];

  cacheRemove?: {
    query: any;
    fetchIds: (variables: V) => number | number[];
  }[];
}

export interface MutableWatchQueryOptions<D, T = any, V = EmptyObject> extends WatchQueryOptions<V> {
  queryName?: string,
  arrayFieldName: keyof D;
  totalFieldName?: keyof D ;
  insertFilterFn?: (data: T) => boolean
}

export interface MutableWatchQueryInfo<D, T = any, V = EmptyObject> {
  id?: string;
  query: any;
  variables: V;
  arrayFieldName: keyof D;
  totalFieldName?: keyof D;
  insertFilterFn?: (data: T) => boolean;
  sortFn?: (a: T, b: T) => number;
  counter: number;
}

export abstract class BaseEntityService<T = any, F = any>{

  protected _debug: boolean;
  protected _mutableWatchQueries: MutableWatchQueryInfo<any>[] = [];

  // Max updated queries, for this entity.
  // - Can be override by subclasses (in constructor)
  // - Use value -1 for no max size
  // - Default value to 3, because it usually enough (e.g. OperationService and LandingService need at least 2)
  protected _mutableWatchQueriesMaxCount: number = 3;

  protected constructor(
    protected graphql: GraphqlService
  ) {

    // for DEV only
    this._debug = !environment.production;
  }

  mutableWatchQuery<D, V = EmptyObject>(opts: MutableWatchQueryOptions<D, T, V>): Observable<D> {

    if (!opts.arrayFieldName) {
      return this.graphql.watchQuery(opts);
    }
    else {
      // Create the query info to remember
      const queryName = opts.queryName || sha256().update(JSON.stringify(opts.query)).digest('hex').substring(0, 8);
      const variablesKey = sha256().update(JSON.stringify(opts.variables)).digest('hex').substring(0, 8);
      const queryId = [queryName, opts.arrayFieldName, variablesKey].join('|');

      const existingQueries = opts.queryName ? this._mutableWatchQueries.filter(q => q.id === queryId) :
        this._mutableWatchQueries.filter(q => q.query === opts.query);
      let mutableQuery: MutableWatchQueryInfo<D, T, V>;

      if (existingQueries.length === 1) {
        mutableQuery = existingQueries[0] as MutableWatchQueryInfo<D, T, V>;
        mutableQuery.counter += 1;
        console.debug('[base-data-service] Find existing mutable watching query (same variables): ' + queryName);

        //if (mutableQuery.counter > 3) {
        //  console.warn('[base-data-service] TODO: clean previous queries with name: ' + queryName);
        //}
      }
      else {
        this.registerNewMutableWatchQuery({
          id: queryId,
          query: opts.query,
          variables: opts.variables,
          arrayFieldName: opts.arrayFieldName,
          insertFilterFn: opts.insertFilterFn,
          counter: 1
        });

      }

      return this.graphql.watchQuery(opts);
    }
  }

  insertIntoMutableCachedQuery(cache: ApolloCache<any>, opts: {
    query?: any;
    queryName?: string;
    data?: T[] | T;
  }) {
    if (!opts.query && !opts.queryName) throw Error("Missing one of 'query' or 'queryName' in the given options");
    const existingQueries = opts.queryName ?
      this._mutableWatchQueries.filter(q => q.id.startsWith(opts.queryName + '|')) :
      this._mutableWatchQueries.filter(q => q.query === opts.query);
    if (!existingQueries.length)  return;

    existingQueries.forEach(watchQuery => {

        if (opts.data instanceof Array) {
          // Filter values, if a filter function exists
          const data = watchQuery.insertFilterFn ? opts.data.filter(i => watchQuery.insertFilterFn(i)) : opts.data;
          if (this._debug && data.length) console.debug(`[base-data-service] Inserting data into watching query: `, watchQuery.id);
          this.graphql.addManyToQueryCache(cache, {
            query: opts.query,
            variables: watchQuery.variables,
            arrayFieldName: watchQuery.arrayFieldName as string,
            sortFn: watchQuery.sortFn,
            data
          });
        }
        else {
          // Filter value, if a filter function exists
          if (!watchQuery.insertFilterFn || watchQuery.insertFilterFn(opts.data)) {
            if (this._debug) console.debug(`[base-data-service] Inserting data into watching query: `, watchQuery.id);
            this.graphql.insertIntoQueryCache(cache, {
              query: opts.query,
              variables: watchQuery.variables,
              arrayFieldName: watchQuery.arrayFieldName as string,
              sortFn: watchQuery.sortFn,
              data: opts.data
            });
          }
        }
      });
  }

  removeFromMutableCachedQueryByIds(proxy: ApolloCache<any>, opts: {
    query?: any;
    queryName?: string;
    ids?: number|number[];
  }){
    if (!opts.query && !opts.queryName) throw Error("Missing one of 'query' or 'queryName' in the given options");
    const existingQueries = opts.queryName ?
      this._mutableWatchQueries.filter(q => q.id.startsWith(opts.queryName + '|')) :
      this._mutableWatchQueries.filter(q => q.query === opts.query);
    if (!existingQueries.length)  return;

    console.debug(`[base-data-service] Removing data from watching queries: `, existingQueries);
    existingQueries.forEach(watchQuery => {
      if (opts.ids instanceof Array) {
        this.graphql.removeFromCachedQueryByIds(proxy, {
          query: watchQuery.query,
          variables: watchQuery.variables,
          arrayFieldName: watchQuery.arrayFieldName as string,
          ids: opts.ids
        });
      }
      else {
        this.graphql.removeFromCachedQueryById(proxy, {
          query: watchQuery.query,
          variables: watchQuery.variables,
          arrayFieldName: watchQuery.arrayFieldName as string,
          id: opts.ids as number
        });
      }
    });
  }

  /* -- protected methods -- */

  registerNewMutableWatchQuery(mutableQuery: MutableWatchQueryInfo<any>) {

    if (this._debug) console.debug('[base-data-service] Adding new mutable watching query: ' + mutableQuery.id);

    // If exceed the max size of mutable queries: remove some
    if (this._mutableWatchQueriesMaxCount > 0 && this._mutableWatchQueries.length >= this._mutableWatchQueriesMaxCount) {
      const removedWatchQueries = this._mutableWatchQueries.splice(0, 1 + this._mutableWatchQueries.length - this._mutableWatchQueriesMaxCount);

      // Warn, as it shouldn't be happen often, when max is correctly set
      console.warn(`[base-data-service] Removing mutable watching queries (exceed max of ${this._mutableWatchQueriesMaxCount}): `,
        removedWatchQueries.map(q => q.id));
    }

    // Define the sort function (to be used in insertIntoMutableCachedQuery())
    if (!mutableQuery.sortFn && mutableQuery.variables && mutableQuery.variables.sortBy) {
      mutableQuery.sortFn = mutableQuery.variables && mutableQuery.variables.sortBy
      && EntityUtils.sortComparator(mutableQuery.variables.sortBy, mutableQuery.variables.sortDirection || 'asc');
    }


    // Add the new mutable query to array
    this._mutableWatchQueries.push(mutableQuery);
  }
}
