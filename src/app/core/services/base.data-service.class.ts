import {GraphqlService, MutateQueryOptions, WatchQueryOptions} from "./graphql.service";
import {Page} from "../../shared/services/entity-service.class";
import {R} from "apollo-angular/types";
import {Observable} from "rxjs";
import {DataProxy} from "apollo-cache";
import {FetchResult} from "apollo-link";
import {environment} from "../../../environments/environment";
import {variable} from "@angular/compiler/src/output/output_ast";
import {EntityUtils} from "./model/entity.model";

const sha256 =  require('hash.js/lib/hash/sha/256');

export interface QueryVariables<T=any,F= any> extends Partial<Page<T>> {
  filter?: F;
  [key: string]: any;
}

export interface MutateQueryWithCacheUpdateOptions<T = any, V = R> extends MutateQueryOptions<T, V> {
  cacheInsert?: {
    query: any;
    fetchData: (variables: V, mutationResult: FetchResult<T>) => any;
  }[];

  cacheRemove?: {
    query: any;
    fetchIds: (variables: V) => number | number[];
  }[];
}

export interface MutableWatchQueryOptions<D, T = any, V = R> extends WatchQueryOptions<V> {
  queryName?: string,
  arrayFieldName: keyof D;
  totalFieldName?: keyof D ;
  insertFilterFn?: (data: T) => boolean
}

export interface MutableWatchQueryInfo<D, T = any, V = R> {
  id?: string;
  query: any;
  variables: V;
  arrayFieldName: keyof D;
  totalFieldName?: keyof D;
  insertFilterFn?: (data: T) => boolean;
  counter: number;
}

export abstract class BaseEntityService<T = any, F = any>{

  protected _debug = false;
  protected _mutableWatchQueries: MutableWatchQueryInfo<any>[] = [];
  protected _mutableWatchQueriesMaxCount: number;

  protected constructor(
    protected graphql: GraphqlService
  ) {

    this._mutableWatchQueriesMaxCount = 1;

    // for DEV only
    this._debug = !environment.production;
  }

  mutableWatchQuery<D, V = R>(opts: MutableWatchQueryOptions<D, T, V>): Observable<D> {

    if (!opts.arrayFieldName) {
      return this.graphql.watchQuery(opts);
    }
    else {
      // Create the query info to remember
      const queryName = opts.queryName || sha256().update(JSON.stringify(opts.query)).digest('hex').substring(0, 8);
      const variablesKey = sha256().update(JSON.stringify(opts.variables)).digest('hex').substring(0, 8);
      const queryId = [queryName, opts.arrayFieldName, variablesKey].join('|');

      // TODO Remove old queries
      /*if (this._mutableWatchQueries.length >= this._mutableWatchQueriesMaxCount) {
        const removedWatchQueries = this._mutableWatchQueries.splice(0, this._mutableWatchQueries.length - this._mutableWatchQueriesMaxCount);
        console.debug(`[base-data-service] Removing mutable watching queries (exceed max of ${this._mutableWatchQueriesMaxCount}): `,
          removedWatchQueries.map(q => q.id));
      }*/

      const existingQueries = opts.queryName ? this._mutableWatchQueries.filter(q => q.id === queryId) :
        this._mutableWatchQueries.filter(q => q.query === opts.query);
      let mutableQuery: MutableWatchQueryInfo<D, T, V>;

      if (existingQueries.length === 1) {
        mutableQuery = existingQueries[0] as MutableWatchQueryInfo<D, T, V>;
        mutableQuery.counter += 1;
        console.debug('[base-data-service] Find existing mutable watching query (same variables): ' + queryName);
        if (mutableQuery.counter > 3) {
          console.warn('[base-data-service] TODO: clean previous queries with name: ' + queryName);
        }
      }
      else {
        console.debug('[base-data-service] Adding new mutable watching query: ' + queryId);
        mutableQuery = {
          id: queryId,
          query: opts.query,
          variables: opts.variables,
          arrayFieldName: opts.arrayFieldName,
          insertFilterFn: opts.insertFilterFn,
          counter: 1
        };
        this._mutableWatchQueries.push(mutableQuery);
      }

      return this.graphql.watchQuery(opts);
    }
  }

  insertIntoMutableCachedQuery(proxy: DataProxy, opts: {
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

      const sortFn = watchQuery.variables && watchQuery.variables.sortBy
        && EntityUtils.sortComparator(watchQuery.variables.sortBy, watchQuery.variables.sortDirection || 'asc');

        if (opts.data instanceof Array) {
          // Filter values, if a filter function exists
          const data = watchQuery.insertFilterFn ? opts.data.filter(i => watchQuery.insertFilterFn(i)) : opts.data;
          if (this._debug && data.length) console.debug(`[base-data-service] Inserting data into watching query: `, watchQuery.id);
          this.graphql.addManyToQueryCache(proxy, {
            query: opts.query,
            variables: watchQuery.variables,
            arrayFieldName: watchQuery.arrayFieldName as string,
            sortFn,
            data
          });
        }
        else {
          // Filter value, if a filter function exists
          if (!watchQuery.insertFilterFn || watchQuery.insertFilterFn(opts.data)) {
            if (this._debug) console.debug(`[base-data-service] Inserting data into watching query: `, watchQuery.id);
            this.graphql.insertIntoQueryCache(proxy, {
              query: opts.query,
              variables: watchQuery.variables,
              arrayFieldName: watchQuery.arrayFieldName as string,
              data: opts.data
            });
          }
        }
      });
  }

  removeFromMutableCachedQueryByIds(proxy: DataProxy, opts: {
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
}
