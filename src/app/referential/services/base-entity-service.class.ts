import {Observable} from "rxjs";
import {map} from "rxjs/operators";
import {ErrorCodes} from "./errors";

import {FetchPolicy, MutationUpdaterFn, WatchQueryFetchPolicy} from "@apollo/client/core";
import {SortDirection} from "@angular/material/sort";

import {ReferentialFilter} from "./referential.service";
import {BaseGraphqlService} from "../../core/services/base-graphql-service.class";
import {EntityServiceLoadOptions, IEntitiesService, LoadResult} from "../../shared/services/entity-service.class";
import {GraphqlService} from "../../core/graphql/graphql.service";
import {PlatformService} from "../../core/services/platform.service";
import {environment} from "../../../environments/environment";
import {Entity, EntityAsObjectOptions, EntityUtils} from "../../core/services/model/entity.model";
import {chainPromises} from "../../shared/observables";
import {isEmptyArray, isNil, isNotNil} from "../../shared/functions";
import {Directive} from "@angular/core";


export interface BaseEntityGraphqlQueries {
  load?: any;
  loadAll: any;
  loadAllWithTotal: any;
}
export interface BaseEntityGraphqlMutations {
  save?: any;
  delete?: any;
  saveAll?: any;
  deleteAll?: any;
}

export interface BaseEntityGraphqlSubscriptions {
  listenChanges?: any;
}
export interface BaseEntityServiceOptions<
  E extends Entity<any>,
  F = any,
  Q extends BaseEntityGraphqlQueries = BaseEntityGraphqlQueries,
  M extends BaseEntityGraphqlMutations = BaseEntityGraphqlMutations,
  S extends BaseEntityGraphqlSubscriptions = BaseEntityGraphqlSubscriptions> {
  queries: Q;
  mutations?: Partial<M>;
  subscriptions?: Partial<S>;
  filterAsObjectFn?: (filter: F) => any;
  equalsFn?: (e1: E, e2: E) => boolean;
  filterFnFactory?: (filter: F) => ((data: E) => boolean);
}


// @dynamic
@Directive()
// tslint:disable-next-line:directive-class-suffix
export abstract class BaseEntityService<T extends Entity<any>,
  F = any, // TODO BLA use EntityFilter avec 'includedIds', 'excludedIds'
  Q extends BaseEntityGraphqlQueries = BaseEntityGraphqlQueries,
  M extends BaseEntityGraphqlMutations = BaseEntityGraphqlMutations,
  S extends BaseEntityGraphqlSubscriptions = BaseEntityGraphqlSubscriptions>
  extends BaseGraphqlService<T, F>
  implements IEntitiesService<T, F> {

  private readonly _entityName: string;

  protected readonly queries: Q;
  protected readonly mutations: Partial<M>;
  protected readonly subscriptions: Partial<S>;
  protected readonly filterAsObjectFn: (filter: F) => any;
  protected readonly equalsFn: (e1: T, e2: T) => boolean;
  protected readonly filterFnFactory: (filter: F) => ((data: T) => boolean);

  protected constructor(
    protected graphql: GraphqlService,
    protected platform: PlatformService,
    protected dataType: new() => T,
    options: BaseEntityServiceOptions<T, F, Q, M, S>
  ) {
    super(graphql, environment);
    this.queries = options.queries;
    this.mutations = options.mutations || {};
    this.subscriptions = options.subscriptions || {};
    this.filterAsObjectFn = options.filterAsObjectFn || ReferentialFilter.asPodObject;
    this.filterFnFactory = options.filterFnFactory || ReferentialFilter.searchFilter;
    this.equalsFn = options.equalsFn || ((e1, e2) => EntityUtils.equals(e1, e2, 'id'));

    platform.ready().then(() => {
      // No limit for updatable watch queries, if desktop
      if (!platform.mobile) {
        this._mutableWatchQueriesMaxCount = -1;
      }
    });

    this._entityName = (new dataType()).constructor.name;
    this.createQueriesAndMutationsFallback();

    // For DEV only
    this._debug = !environment.production;
  }

  watch(id: number, opts?: EntityServiceLoadOptions & {
    query?: any;
    toEntity?: boolean;
  }): Observable<T> {

    if (this._debug) console.debug(`[base-entity-service] Watching ${this._entityName} {${id}}...`);

    const query = opts && opts.query || this.queries.load;
    return this.graphql.watchQuery<{data: any}>({
      query,
      variables: { id },
      fetchPolicy: opts && (opts.fetchPolicy as FetchPolicy) || undefined,
      error: {code: ErrorCodes.LOAD_REFERENTIAL_ERROR, message: "REFERENTIAL.ERROR.LOAD_REFERENTIAL_ERROR"}
    })
      .pipe(
        map(({data}) => {
          return (!opts || opts.toEntity !== false)
            ? (data && this.fromObject(data))
            : (data as T);
        })
      );
  }

  async load(id: number, opts?: EntityServiceLoadOptions & {
    query?: any;
    toEntity?: boolean;
  }): Promise<T> {

    if (this._debug) console.debug(`[base-entity-service] Loading ${this._entityName} {${id}}...`);

    const query = opts && opts.query || this.queries.load;
    const { data } = await this.graphql.query<{data: any}>({
      query,
      variables: {
        id: id
      },
      fetchPolicy: opts && (opts.fetchPolicy as FetchPolicy) || undefined,
      error: {code: ErrorCodes.LOAD_REFERENTIAL_ERROR, message: "REFERENTIAL.ERROR.LOAD_REFERENTIAL_ERROR"}
    });

    // Convert to entity
    return (!opts || opts.toEntity !== false)
      ? (data && this.fromObject(data))
      : (data as T);
  }

  watchAll(offset: number,
           size: number,
           sortBy?: string,
           sortDirection?: SortDirection,
           filter?: F,
           opts?: {
             fetchPolicy?: WatchQueryFetchPolicy;
             withTotal: boolean;
             toEntity?: boolean;
           }
  ): Observable<LoadResult<T>> {

    const variables: any = {
      offset: offset || 0,
      size: size || 100,
      sortBy: sortBy || 'id',
      sortDirection: sortDirection || 'asc',
      filter: this.filterAsObjectFn(filter)
    };

    let now = this._debug && Date.now();
    if (this._debug) console.debug(`[base-entity-service] Watching ${this._entityName}...`, variables);

    const withTotal = (!opts || opts.withTotal !== false);
    const query = withTotal ? this.queries.loadAllWithTotal : this.queries.loadAll;
    return this.mutableWatchQuery<LoadResult<any>>({
      queryName: withTotal ? 'LoadAllWithTotal' : 'LoadAll',
      query,
      arrayFieldName: 'data',
      totalFieldName: withTotal ? 'total' : undefined,
      insertFilterFn: this.filterFnFactory(filter),
      variables,
      error: {code: ErrorCodes.LOAD_REFERENTIAL_ERROR, message: "REFERENTIAL.ERROR.LOAD_REFERENTIAL_ERROR"},
      fetchPolicy: opts && opts.fetchPolicy || 'network-only'
    })
      .pipe(
        map(({data, total}) => {
          // Convert to entity (if need)
          const entities = (!opts || opts.toEntity !== false)
            ? (data || []).map(json => this.fromObject(json))
            : (data || []) as T[];

          if (now) {
            console.debug(`[base-entity-service] ${this._entityName} loaded in ${Date.now() - now}ms`, entities);
            now = null;
          }
          return {
            data: entities,
            total
          };
        })
      );
  }

  async loadAll(offset: number,
                size: number,
                sortBy?: string,
                sortDirection?: SortDirection,
                filter?: F,
                opts?: {
                  [key: string]: any;
                  query?: any;
                  fetchPolicy?: FetchPolicy;
                  debug?: boolean;
                  withTotal?: boolean;
                  toEntity?: boolean;
                }
  ): Promise<LoadResult<T>> {

    const debug = this._debug && (!opts || opts.debug !== false);

    const variables: any = {
      offset: offset || 0,
      size: size || 100,
      sortBy: sortBy || 'id',
      sortDirection: sortDirection || 'asc',
      filter: this.filterAsObjectFn(filter)
    };

    const now = Date.now();
    if (debug) console.debug(`[base-entity-service] Loading ${this._entityName}...`, variables);

    const query = (opts && opts.query) // use given query
      // Or get loadAll or loadAllWithTotal query
      || ((!opts || opts.withTotal !== false) ? this.queries.loadAllWithTotal : this.queries.loadAll);
    const {data, total} = await this.graphql.query<LoadResult<any>>({
      query,
      variables,
      error: {code: ErrorCodes.LOAD_REFERENTIAL_ERROR, message: "ERROR.LOAD_ERROR"},
      fetchPolicy: opts && opts.fetchPolicy || 'network-only'
    });
    const entities = (!opts || opts.toEntity !== false) ?
      (data || []).map(json => this.fromObject(json)) :
      (data || []) as T[];
    if (debug) console.debug(`[base-entity-service] ${this._entityName} items loaded in ${Date.now() - now}ms`);
    return {
      data: entities,
      total
    };
  }


  async saveAll(entities: T[], options?: any): Promise<T[]> {
    if (!this.mutations.saveAll) throw Error('Not implemented');

    if (isEmptyArray(entities)) return entities; // Nothing to save: skip

    const json = entities.map(entity => this.asObject(entity));

    const now = Date.now();
    if (this._debug) console.debug(`[base-entity-service] Saving all ${this._entityName}...`, json);

    await this.graphql.mutate<LoadResult<any>>({
      mutation: this.mutations.saveAll,
      variables: {
        data: json
      },
      error: {code: ErrorCodes.SAVE_REFERENTIAL_ERROR, message: "REFERENTIAL.ERROR.SAVE_REFERENTIAL_ERROR"},
      update: (proxy, {data}) => {
        if (data && data.data) {
          // Update entities (id and update date)
          entities.forEach(entity => {
            const savedEntity = data.data.find(e => this.equalsFn(e, entity));
            this.copyIdAndUpdateDate(savedEntity, entity);
          });

          // Update the cache
          this.insertIntoMutableCachedQuery(proxy, {
            query: this.queries.loadAll,
            data: data.data
          });
        }

        if (this._debug) console.debug(`[base-entity-service] ${this._entityName} saved in ${Date.now() - now}ms`, entities);

      }
    });

    return entities;
  }

  /**
   * Save a referential entity
   * @param entity
   * @param options
   */
  async save(entity: T, options?: any): Promise<T> {
    if (!this.mutations.save) throw Error('Not implemented');

    // Fill default properties
    this.fillDefaultProperties(entity);

    // Transform into json
    const json = this.asObject(entity);

    const isNew = isNil(json.id);

    const now = Date.now();
    if (this._debug) console.debug(`[base-entity-service] Saving ${this._entityName}...`, json);

    await this.graphql.mutate< {data: any}>({
      mutation: this.mutations.save,
      variables: {
        data: json
      },
      error: {code: ErrorCodes.SAVE_REFERENTIAL_ERROR, message: "REFERENTIAL.ERROR.SAVE_REFERENTIAL_ERROR"},
      update: (proxy, {data}) => {
        // Update entity
        const savedEntity = data && data.data;
        this.copyIdAndUpdateDate(savedEntity, entity);

        if (this._debug) console.debug(`[base-entity-service] ${entity.__typename} saved in ${Date.now() - now}ms`, entity);

        // Update the cache
        if (isNew) {
          this.insertIntoMutableCachedQuery(proxy, {
            query: this.queries.loadAll,
            data: savedEntity
          });

          // TODO BLA: should also clean referential ref queries ?
          // How to clean
        }

      }
    });

    return entity;
  }

  /**
   * Delete referential entities
   */
  async deleteAll(entities: T[], options?: Partial<{
    update: MutationUpdaterFn<any>;
  }> | any): Promise<any> {
    if (!this.mutations.deleteAll) throw Error('Not implemented');

    // Filter saved entities
    entities = entities && entities.filter(e => isNotNil(e.id));

    // Nothing to save: skip
    if (isEmptyArray(entities)) return;

    const ids = entities.map(t => t.id);
    const now = new Date();
    if (this._debug) console.debug(`[base-entity-service] Deleting ${this._entityName}...`, ids);

    await this.graphql.mutate<any>({
      mutation: this.mutations.deleteAll,
      variables: {
        ids: ids
      },
      error: {code: ErrorCodes.DELETE_REFERENTIAL_ERROR, message: "REFERENTIAL.ERROR.DELETE_REFERENTIAL_ERROR"},
      update: (proxy) => {
        // Remove from cache
        this.removeFromMutableCachedQueryByIds(proxy, {
          query: this.queries.loadAll,
          ids
        });

        if (options && options.update) {
          options.update(proxy);
        }

        if (this._debug) console.debug(`[base-entity-service] ${this._entityName} deleted in ${new Date().getTime() - now.getTime()}ms`);
      }
    });
  }

  /**
   * Delete a referential entity
   */
  async delete(entity: T, options?: Partial<{
    update: MutationUpdaterFn<any>;
  }> | any): Promise<any> {
    if (!this.mutations.delete) throw Error('Not implemented');

    // Nothing to save: skip
    if (!entity || isNil(entity.id)) return;

    const id = entity.id;
    const now = new Date();
    if (this._debug) console.debug(`[base-entity-service] Deleting ${this._entityName} {${id}}...`);

    await this.graphql.mutate<any>({
      mutation: this.mutations.delete,
      variables: {
        id: id
      },
      error: {code: ErrorCodes.DELETE_REFERENTIAL_ERROR, message: "REFERENTIAL.ERROR.DELETE_REFERENTIAL_ERROR"},
      update: (proxy) => {
        // Remove from cache
        this.removeFromMutableCachedQueryByIds(proxy, {
          query: this.queries.loadAll,
          ids: [id]
        });

        if (options && options.update) {
          options.update(proxy);
        }

        if (this._debug) console.debug(`[base-entity-service] ${this._entityName} deleted in ${new Date().getTime() - now.getTime()}ms`);
      }
    });
  }

  listenChanges(id: number, opts?: {
    interval?: number
  }): Observable<T> {
    if (isNil(id)) throw Error("Missing argument 'id' ");
    if (!this.subscriptions.listenChanges) throw Error("Not implemented!");

    const variables = {
      id,
      interval: opts && opts.interval || 10 // seconds
    };
    if (this._debug) console.debug(`[base-entity-service] [WS] Listening for changes on ${this._entityName} {${id}}...`);

    return this.graphql.subscribe<LoadResult<any>>({
      query: this.subscriptions.listenChanges,
      variables,
      error: {
        code: ErrorCodes.SUBSCRIBE_REFERENTIAL_ERROR,
        message: 'REFERENTIAL.ERROR.SUBSCRIBE_REFERENTIAL_ERROR'
      }
    })
      .pipe(
        map(({data}) => {
          const entity = data && this.fromObject(data);
          if (entity && this._debug) console.debug(`[base-entity-service] [WS] Received changes on ${this._entityName} {${id}}`, entity);

          // TODO: when missing = deleted ?
          if (!entity) console.warn(`[base-entity-service] [WS] Received deletion on ${this._entityName} {${id}} - TODO check implementation`);

          return entity;
        })
      );
  }

  copyIdAndUpdateDate(source: T | undefined, target: T) {
    if (!source) return;

    // Update (id and updateDate)
    EntityUtils.copyIdAndUpdateDate(source, target);
  }


  fromObject(source: any): T {
    if (!source) return source;
    const target = new this.dataType();
    target.fromObject(source);
    return target;
  }

  /* -- protected functions -- */

  protected fillDefaultProperties(source: T) {
    // Can be override by subclasses
  }

  protected asObject(entity: T, opts?: EntityAsObjectOptions): any {
    // Can be override by subclasses
    return entity.asObject(opts);
  }

  /**
   * Workaround to enable delete() and deleteAll() even when some mutation are missing
   *
   * @protected
   */
  protected createQueriesAndMutationsFallback() {
    if (this.mutations) {
      // save() and saveAll()
      if (!this.mutations.save && this.mutations.saveAll) {
        this.save = async (entity, opts) => {
          const data = await this.saveAll([entity], opts);
          return data && data[0];
        };
      }
      else if (!this.mutations.deleteAll && this.mutations.delete) {
        // Save one by one
        this.saveAll = (entities, opts) => chainPromises((entities || [])
          .map(entity => (() => this.save(entity, opts))));
      }

      // delete and deleteAll()
      if (!this.mutations.delete && this.mutations.deleteAll) {
        this.delete = (entity, opts) => this.deleteAll([entity], opts);
      }
      else if (!this.mutations.deleteAll && this.mutations.delete) {
        // Delete one by one
        this.deleteAll = (entities, opts) => chainPromises((entities || [])
          .map(entity => (() => this.delete(entity, opts))));
      }
    }
  }
}
