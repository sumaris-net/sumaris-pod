import {Observable} from "rxjs";
import {map} from "rxjs/operators";
import {ErrorCodes} from "./errors";

import {FetchPolicy, MutationUpdaterFn} from "@apollo/client/core";
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

export abstract class BaseReferentialService<E extends Referential, F extends ReferentialFilter>
  extends BaseEntityService<E>
  implements IEntitiesService<E, F> {

  private readonly _entityName: string;

  protected constructor(
    protected graphql: GraphqlService,
    protected platform: PlatformService,
    //protected environment: Environment, // TODO q3
    protected dataType: new() => E,
    protected queries: Partial<BaseReferentialEntitiesQueries & BaseReferentialEntityQueries>,
    protected mutations: Partial<BaseReferentialEntityMutations & BaseReferentialEntitiesMutations> = {},
    protected subscriptions: Partial<BaseReferentialSubscriptions> = {},
    protected filterAsObjectFn: (filter: F) => any = ReferentialFilter.asPodObject,
    protected createFilterFn: (filter: F) => ((data: E) => boolean) = ReferentialFilter.searchFilter
  ) {
    super(graphql, environment);

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

  async load(id: number, options?: EntityServiceLoadOptions): Promise<E> {

    if (this._debug) console.debug(`[referential-service] Loading ${this._entityName} {${id}}...`);

    const res = await this.graphql.query<{data: any}>({
      query: this.queries.load,
      variables: {
        id: id
      },
      fetchPolicy: options && (options.fetchPolicy as FetchPolicy) || undefined,
      error: {code: ErrorCodes.LOAD_REFERENTIAL_ERROR, message: "REFERENTIAL.ERROR.LOAD_REFERENTIAL_ERROR"}
    });
    return res && res.data && this.fromObject(res.data);
  }

  watchAll(offset: number,
           size: number,
           sortBy?: string,
           sortDirection?: SortDirection,
           filter?: F,
           opts?: {
             fetchPolicy?: FetchPolicy;
             withTotal: boolean;
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
          const entities = (data || []).map(json => this.fromObject(json));
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

    const query = (!opts || opts.withTotal !== false) ? this.queries.loadAllWithTotal : this.queries.loadAll;
    const {data, total} = await this.graphql.query<LoadResult<any>>({
      query,
      variables,
      error: {code: ErrorCodes.LOAD_REFERENTIAL_ERROR, message: "REFERENTIAL.ERROR.LOAD_REFERENTIAL_ERROR"},
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

    // Transform into json
    const json = entity.asObject();
    const isNew = !json.id;

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

  public listenChanges(id: number, opts?: {
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

  /**
   * Workaround to enable delete() and deleteAll() even when some mutation are missing
   *
   * @protected
   */
  protected initQueriesAndMutationsFallback() {

    // load()
    if (!this.queries.load && this.queries.loadAll) {
      this.load = async (id, opts) => {
        const data = await this.loadAll(0, 1, null, null, { id: id } as F, opts);
        return data && data[0];
      };
    }

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
