import {GraphqlService, MutateQueryOptions, WatchQueryOptions} from "../graphql/graphql.service";
import {Page} from "../../shared/services/entity-service.class";
import {EmptyObject} from "apollo-angular/types";
import {Observable} from "rxjs";
import {FetchResult} from "@apollo/client/link/core";
import {EntityUtils} from "./model/entity.model";
import {ApolloCache} from "@apollo/client/core";
import {Environment} from "../../../environments/environment.class";
import {changeCaseToUnderscore, isNotEmptyArray, toBoolean} from "../../shared/functions";
import {environment} from "../../../environments/environment";
import {Optional} from "@angular/core";

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
  queryName?: string;
  arrayFieldName: keyof D;
  totalFieldName?: keyof D ;
  insertFilterFn?: (data: T) => boolean;
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

export class BaseGraphqlServiceOptions {
  production?: boolean;
}

export abstract class BaseGraphqlService<T = any, F = any> {

  protected _debug: boolean;
  protected _debugPrefix: string;
  protected _mutableWatchQueries: MutableWatchQueryInfo<any>[] = [];

  // Max updated queries, for this entity.
  // - Can be override by subclasses (in constructor)
  // - Use value -1 for no max size
  // - Default value to 3, because it usually enough (e.g. OperationService and LandingService need at least 2)
  protected _mutableWatchQueriesMaxCount = 3;

  protected constructor(
    protected graphql: GraphqlService,
    @Optional() options?: BaseGraphqlServiceOptions
  ) {

    // for DEV only
    this._debug = toBoolean(!options.production, !environment.production);
    this._debugPrefix = this._debug && `[${changeCaseToUnderscore(this.constructor.name).replace(/_/g, '-' )}]`;
  }

  mutableWatchQuery<D, V = EmptyObject>(opts: MutableWatchQueryOptions<D, T, V>): Observable<D> {

    if (!opts.arrayFieldName) {
      return this.graphql.watchQuery(opts);
    }
    else {
      // Create the query id
      const queryId = this.computeMutableWatchQueryId(opts);

      const exactMatchQueries = opts.queryName ? this._mutableWatchQueries.filter(q => q.id === queryId) :
        this._mutableWatchQueries.filter(q => q.query === opts.query);
      let mutableQuery: MutableWatchQueryInfo<D, T, V>;

      if (exactMatchQueries.length === 1) {
        mutableQuery = exactMatchQueries[0] as MutableWatchQueryInfo<D, T, V>;
        mutableQuery.counter += 1;
        if (this._debug) console.debug(this._debugPrefix + `Find same mutable watch query (same variables) {${queryId}}. Skip register`);

        //if (mutableQuery.counter > 3) {
        //  console.warn(this._debugPrefix + 'TODO: clean previous queries with name: ' + queryName);
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
    const queries = this.findMutableWatchQueries(opts);
    if (!queries.length)  return;

    queries.forEach(query => {

        if (opts.data instanceof Array) {
          // Filter values, if a filter function exists
          const data = query.insertFilterFn ? opts.data.filter(i => query.insertFilterFn(i)) : opts.data;
          if (isNotEmptyArray(data)) {
            if (this._debug) console.debug(`[base-data-service] Inserting data into watching query: `, query.id);
            this.graphql.addManyToQueryCache(cache, {
              query: query.query,
              variables: query.variables,
              arrayFieldName: query.arrayFieldName as string,
              sortFn: query.sortFn,
              data
            });
          }
        }
        // Filter value, if a filter function exists
        else if (!query.insertFilterFn || query.insertFilterFn(opts.data)) {
          if (this._debug) console.debug(`[base-data-service] Inserting data into watching query: `, query.id);
          this.graphql.insertIntoQueryCache(cache, {
            query: query.query,
            variables: query.variables,
            arrayFieldName: query.arrayFieldName as string,
            sortFn: query.sortFn,
            data: opts.data
          });
        }
      });
  }

  removeFromMutableCachedQueryByIds(cache: ApolloCache<any>, opts: {
    query?: any;
    queryName?: string;
    ids: number|number[];
  }){
    const queries = this.findMutableWatchQueries(opts);
    if (!queries.length)  return;

    console.debug(`[base-data-service] Removing data from watching queries: `, queries);
    queries.forEach(query => {
      if (opts.ids instanceof Array) {
        this.graphql.removeFromCachedQueryByIds(cache, {
          query: query.query,
          variables: query.variables,
          arrayFieldName: query.arrayFieldName as string,
          ids: opts.ids
        });
      }
      else {
        this.graphql.removeFromCachedQueryById(cache, {
          query: query.query,
          variables: query.variables,
          arrayFieldName: query.arrayFieldName as string,
          id: opts.ids.toString()
        });
      }
    });
  }

  /* -- protected methods -- */

  protected computeMutableWatchQueryId<D, T, V = EmptyObject>(opts: MutableWatchQueryOptions<D, T, V>) {
    const queryName = opts.queryName || sha256().update(JSON.stringify(opts.query)).digest('hex').substring(0, 8);
    const variablesKey = opts.variables && sha256().update(JSON.stringify(opts.variables)).digest('hex').substring(0, 8) || '';
    return [queryName, opts.arrayFieldName, variablesKey].join('|');
  }

  protected findMutableWatchQueries(opts?: {query?: any; queryName?: string; }) {
    if (!opts.query && !opts.queryName) throw Error("Missing one of 'query' or 'queryName' in the given options");
    // Search by queryName (if any) or by query
    return opts.queryName ?
      this._mutableWatchQueries.filter(q => q.id.startsWith(opts.queryName + '|')) :
      this._mutableWatchQueries.filter(q => q.query === opts.query);
  }

  protected registerNewMutableWatchQuery(mutableQuery: MutableWatchQueryInfo<any>) {

    if (this._debug) console.debug(this._debugPrefix + `Register new mutable watch query {${mutableQuery.id}}`);

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
