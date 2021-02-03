import {Observable} from "rxjs";
import {map} from "rxjs/operators";
import {ErrorCodes} from "./errors";

import {FetchPolicy, MutationUpdaterFn, WatchQueryFetchPolicy} from "@apollo/client/core";
import {SortDirection} from "@angular/material/sort";

import {ReferentialFilter} from "./referential.service";
import {Referential} from "../../core/services/model/referential.model";
import {BaseEntityService} from "../../core/services/base.data-service.class";
import {EntityServiceLoadOptions, IEntitiesService, LoadResult} from "../../shared/services/entity-service.class";
import {GraphqlService} from "../../core/graphql/graphql.service";
import {PlatformService} from "../../core/services/platform.service";
import {environment} from "../../../environments/environment";
import {EntityUtils} from "../../core/services/model/entity.model";
import {chainPromises} from "../../shared/observables";
import {isEmptyArray, isNil, isNotNil} from "../../shared/functions";
import {Directive} from "@angular/core";

export interface BaseReferentialEntityQueries {
  load: any;
}

export interface BaseReferentialEntitiesQueries {
  loadAll: any;
  loadAllWithTotal: any;
}

export interface BaseReferentialEntityMutations {
  save: any;
  delete: any;
}

export interface BaseReferentialEntitiesMutations {
  saveAll: any;
  deleteAll: any;
}

export interface BaseReferentialSubscriptions {
  listenChanges: any;
}
export interface BaseReferentialServiceOptions<E extends Referential, F extends ReferentialFilter> {
  queries: Partial<BaseReferentialEntitiesQueries & BaseReferentialEntityQueries>;
  mutations?: Partial<BaseReferentialEntityMutations & BaseReferentialEntitiesMutations>;
  subscriptions?: Partial<BaseReferentialSubscriptions>;
  filterAsObjectFn?: (filter: F) => any;
  createFilterFn?: (filter: F) => ((data: E) => boolean);
}

@Directive()
// tslint:disable-next-line:directive-class-suffix
export abstract class BaseReferentialService<E extends Referential, F extends ReferentialFilter>
  extends BaseEntityService<E>
  implements IEntitiesService<E, F> {

  private readonly _entityName: string;

  protected readonly queries: Partial<BaseReferentialEntitiesQueries & BaseReferentialEntityQueries>;
  protected readonly mutations: Partial<BaseReferentialEntityMutations & BaseReferentialEntitiesMutations>;
  protected readonly subscriptions: Partial<BaseReferentialSubscriptions>;
  protected readonly filterAsObjectFn: (filter: F) => any;
  protected readonly createFilterFn: (filter: F) => ((data: E) => boolean);

  protected constructor(
    protected graphql: GraphqlService,
    protected platform: PlatformService,
    protected dataType: new() => E,
    options: BaseReferentialServiceOptions<E, F>
  ) {
    super(graphql, environment);
    this.queries = options.queries;
    this.mutations = options.mutations || {};
    this.subscriptions = options.subscriptions || {};
    this.filterAsObjectFn = options.filterAsObjectFn || ReferentialFilter.asPodObject;
    this.createFilterFn = options.createFilterFn || ReferentialFilter.searchFilter;

    platform.ready().then(() => {
      // No limit for updatable watch queries, if desktop
      if (!platform.mobile) {
        this._mutableWatchQueriesMaxCount = -1;
      }
    });

    this._entityName = (new dataType()).constructor.name;
    this.initQueriesAndMutationsFallback();

    // For DEV only
    this._debug = !environment.production;
  }

  watch(id: number, opts?: EntityServiceLoadOptions & {
    query?: any;
    toEntity?: boolean;
  }): Observable<E> {

    if (this._debug) console.debug(`[referential-service] Watching ${this._entityName} {${id}}...`);

    const query = opts && opts.query || this.queries.load;
    return this.graphql.watchQuery<{data: any}>({
      query,
      variables: { id },
      fetchPolicy: opts && (opts.fetchPolicy as FetchPolicy) || undefined,
      error: {code: ErrorCodes.LOAD_REFERENTIAL_ERROR, message: "REFERENTIAL.ERROR.LOAD_REFERENTIAL_ERROR"}
    })
      .pipe(
        map(({data}) => {
          const entity = (!opts || opts.toEntity !== false)
            ? (data && this.fromObject(data))
            : (data as E);
          return entity;
        })
      );
  }

  async load(id: number, opts?: EntityServiceLoadOptions & {
    query?: any;
    toEntity?: boolean;
  }): Promise<E> {

    if (this._debug) console.debug(`[referential-service] Loading ${this._entityName} {${id}}...`);

    const query = opts && opts.query || this.queries.load;
    const { data } = await this.graphql.query<{data: any}>({
      query,
      variables: {
        id: id
      },
      fetchPolicy: opts && (opts.fetchPolicy as FetchPolicy) || undefined,
      error: {code: ErrorCodes.LOAD_REFERENTIAL_ERROR, message: "REFERENTIAL.ERROR.LOAD_REFERENTIAL_ERROR"}
    });
    const entity = (!opts || opts.toEntity !== false)
      ? (data && this.fromObject(data))
      : (data as E);
    return entity;
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
  ): Observable<LoadResult<E>> {

    const variables: any = {
      offset: offset || 0,
      size: size || 100,
      sortBy: sortBy,
      sortDirection: sortDirection || 'asc',
      filter: this.filterAsObjectFn(filter)
    };

    let now = this._debug && Date.now();
    if (this._debug) console.debug(`[referential-service] Watching ${this._entityName}...`, variables);

    const withTotal = (!opts || opts.withTotal !== false);
    const query = withTotal ? this.queries.loadAllWithTotal : this.queries.loadAll;
    return this.mutableWatchQuery<LoadResult<any>>({
      queryName: withTotal ? 'LoadAllWithTotal' : 'LoadAll',
      query,
      arrayFieldName: 'data',
      totalFieldName: withTotal ? 'total' : undefined,
      insertFilterFn: this.createFilterFn(filter),
      variables,
      error: {code: ErrorCodes.LOAD_REFERENTIAL_ERROR, message: "REFERENTIAL.ERROR.LOAD_REFERENTIAL_ERROR"},
      fetchPolicy: opts && opts.fetchPolicy || 'network-only'
    })
      .pipe(
        map(({data, total}) => {
          // Convert to entity (if need)
          const entities = (!opts || opts.toEntity !== false)
            ? (data || []).map(json => this.fromObject(json))
            : (data || []) as E[];

          if (now) {
            console.debug(`[referential-service] ${this._entityName} loaded in ${Date.now() - now}ms`, entities);
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
  ): Promise<LoadResult<E>> {

    const debug = this._debug && (!opts || opts.debug !== false);

    const variables: any = {
      offset: offset || 0,
      size: size || 100,
      sortBy: sortBy || filter.searchAttribute,
      sortDirection: sortDirection || 'asc',
      filter: this.filterAsObjectFn(filter)
    };

    const now = Date.now();
    if (debug) console.debug(`[referential-service] Loading ${this._entityName}...`, variables);

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
      (data || []) as E[];
    if (debug) console.debug(`[referential-service] ${this._entityName} items loaded in ${Date.now() - now}ms`);
    return {
      data: entities,
      total
    };
  }


  async saveAll(entities: E[], options?: any): Promise<E[]> {
    if (!this.mutations.saveAll) throw Error('Not implemented');

    if (!entities) return entities;
    // Nothing to save: skip
    if (!entities.length) return;

    const json = entities.map(t => t.asObject());
    const now = Date.now();
    if (this._debug) console.debug(`[referential-service] Saving all ${this._entityName}...`, json);

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
            const savedEntity = data.data.find(e => this.entityEquals(e, entity));
            this.copyIdAndUpdateDate(savedEntity, entity);
          });

          // Update the cache
          this.insertIntoMutableCachedQuery(proxy, {
            query: this.queries.loadAll,
            data: data.data
          });
        }

        if (this._debug) console.debug(`[referential-service] ${this._entityName} saved in ${Date.now() - now}ms`, entities);

      }
    });

    return entities;
  }

  /**
   * Save a referential entity
   * @param entity
   * @param options
   */
  async save(entity: E, options?: any): Promise<E> {
    if (!this.mutations.save) throw Error('Not implemented');

    // Fill default properties
    this.fillDefaultProperties(entity);

    // Transform into json
    const json = entity.asObject();
    const isNew = isNil(json.id);

    const now = Date.now();
    if (this._debug) console.debug(`[referential-service] Saving ${this._entityName}...`, json);

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

        if (this._debug) console.debug(`[referential-service] ${entity.entityName} saved in ${Date.now() - now}ms`, entity);

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
  async deleteAll(entities: E[], options?: Partial<{
    update: MutationUpdaterFn<any>;
  }> | any): Promise<any> {
    if (!this.mutations.deleteAll) throw Error('Not implemented');

    // Filter saved entities
    entities = entities && entities.filter(e => isNotNil(e.id));

    // Nothing to save: skip
    if (isEmptyArray(entities)) return;

    const ids = entities.map(t => t.id);
    const now = new Date();
    if (this._debug) console.debug(`[referential-service] Deleting ${this._entityName}...`, ids);

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

        if (this._debug) console.debug(`[referential-service] ${this._entityName} deleted in ${new Date().getTime() - now.getTime()}ms`);
      }
    });
  }

  /**
   * Delete a referential entity
   */
  async delete(entity: E, options?: Partial<{
    update: MutationUpdaterFn<any>;
  }> | any): Promise<any> {
    if (!this.mutations.delete) throw Error('Not implemented');

    // Nothing to save: skip
    if (!entity || isNil(entity.id)) return;

    const id = entity.id;
    const now = new Date();
    if (this._debug) console.debug(`[referential-service] Deleting ${this._entityName} {${id}}...`);

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

        if (this._debug) console.debug(`[referential-service] ${this._entityName} deleted in ${new Date().getTime() - now.getTime()}ms`);
      }
    });
  }

  listenChanges(id: number, opts?: {
    interval?: number
  }): Observable<E> {
    if (isNil(id)) throw Error("Missing argument 'id' ");
    if (!this.subscriptions.listenChanges) throw Error("Not implemented!");

    const variables = {
      id,
      interval: opts && opts.interval || 10 // seconds
    };
    if (this._debug) console.debug(`[referential-service] [WS] Listening for changes on ${this._entityName} {${id}}...`);

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
          if (entity && this._debug) console.debug(`[referential-service] [WS] Received changes on ${this._entityName} {${id}}`, entity);

          // TODO: when missing = deleted ?
          if (!entity) console.warn(`[referential-service] [WS] Received deletion on ${this._entityName} {${id}} - TODO check implementation`);

          return entity;
        })
      );
  }

  entityEquals(e1: E, e2: E): boolean {
    return e1 && e2 && (e1.id === e2.id || e1.label === e2.label || e1.name === e2.name);
  }


  copyIdAndUpdateDate(source: E | undefined, target: E) {
    if (!source) return;

    // Update (id and updateDate)
    EntityUtils.copyIdAndUpdateDate(source, target);
  }

  fromObject(source: any): E {
    if (!source) return source;
    const target = new this.dataType();
    target.fromObject(source);
    return target;
  }

  /* -- protected functions -- */

  protected fillDefaultProperties(source: E) {
    // Can be override by subclasses
  }


  /**
   * Workaround to enable delete() and deleteAll() even when some mutation are missing
   *
   * @protected
   */
  protected initQueriesAndMutationsFallback() {

    if (this.queries) {
      // load()
      if (!this.queries.load && this.queries.loadAll) {
        this.load = async (id, opts) => {
          const data = await this.loadAll(0, 1, null, null, { id: id } as F, opts);
          return data && data[0];
        };
      }
    }

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
