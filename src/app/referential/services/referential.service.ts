import {Injectable} from '@angular/core';
import {DocumentNode, FetchPolicy, gql, MutationUpdaterFn} from '@apollo/client/core';
import {Observable} from 'rxjs';
import {map} from 'rxjs/operators';
import {ErrorCodes} from './errors';
import {
  AccountService,
  BaseEntityGraphqlMutations,
  BaseEntityGraphqlQueries,
  BaseGraphqlService, EntitySaveOptions,
  EntityUtils,
  GraphqlService,
  IEntitiesService,
  isNil,
  isNotNil,
  LoadResult, LocalSettingsService,
  PlatformService,
  Referential,
  ReferentialRef,
  StatusIds,
} from '@sumaris-net/ngx-components';
import {ReferentialFragments} from './referential.fragments';
import {environment} from '@environments/environment';
import {SortDirection} from '@angular/material/sort';
import {ReferentialFilter} from './filter/referential.filter';


export interface ReferentialType {
  id: string;
  level?: string;
}
const LoadAllWithTotalQuery: any = gql`
  query ReferentialsWithTotal($entityName: String, $offset: Int, $size: Int, $sortBy: String, $sortDirection: String, $filter: ReferentialFilterVOInput){
    data: referentials(entityName: $entityName, offset: $offset, size: $size, sortBy: $sortBy, sortDirection: $sortDirection, filter: $filter){
      ...FullReferentialFragment
    }
    total: referentialsCount(entityName: $entityName, filter: $filter)
  }
  ${ReferentialFragments.fullReferential}
`;
const LoadAllQuery: any = gql`
  query Referentials($entityName: String, $offset: Int, $size: Int, $sortBy: String, $sortDirection: String, $filter: ReferentialFilterVOInput){
    data: referentials(entityName: $entityName, offset: $offset, size: $size, sortBy: $sortBy, sortDirection: $sortDirection, filter: $filter){
      ...FullReferentialFragment
    }
  }
  ${ReferentialFragments.fullReferential}
`;
const CountQuery: any = gql`
  query ReferentialsCount($entityName: String, $filter: ReferentialFilterVOInput){
    total: referentialsCount(entityName: $entityName, filter: $filter)
  }
`;
const LoadReferentialTypes: any = gql`
  query ReferentialTypes{
    data: referentialTypes {
       id
       level
      __typename
    }
  }
`;

const LoadReferentialLevels: any = gql`
  query ReferentialLevels($entityName: String) {
    data: referentialLevels(entityName: $entityName){
      ...ReferentialFragment
    }
  }
  ${ReferentialFragments.referential}
`;

const SaveAllQuery: any = gql`
  mutation SaveReferentials($data:[ReferentialVOInput]){
    data: saveReferentials(referentials: $data){
      ...FullReferentialFragment
    }
  }
  ${ReferentialFragments.fullReferential}
`;

const DeleteAll: any = gql`
  mutation deleteReferentials($entityName: String, $ids:[Int]){
    deleteReferentials(entityName: $entityName, ids: $ids)
  }
`;

const REFERENTIAL_QUERIES: BaseEntityGraphqlQueries & {count: any} = {
  loadAll: LoadAllQuery,
  loadAllWithTotal: LoadAllWithTotalQuery,
  count: CountQuery
};
const REFERENTIAL_MUTATIONS: BaseEntityGraphqlMutations = {
  saveAll: SaveAllQuery,
  deleteAll: DeleteAll
};

@Injectable({providedIn: 'root'})
export class ReferentialService
  extends BaseGraphqlService<Referential, ReferentialFilter>
  implements IEntitiesService<Referential, ReferentialFilter> {

  private queries = REFERENTIAL_QUERIES;
  private mutations = REFERENTIAL_MUTATIONS;

  constructor(
    protected graphql: GraphqlService,
    protected accountService: AccountService,
    protected settings: LocalSettingsService
  ) {
    super(graphql, environment);

    this.settings.ready().then(() => {
      // No limit for updatable watch queries, if desktop. Limit to 3 when mobile
      this._mutableWatchQueriesMaxCount = this.settings.mobile ? 3 : -1;
    });

    // For DEV only
    this._debug = !environment.production;
  }

  watchAll(offset: number,
           size: number,
           sortBy?: string,
           sortDirection?: SortDirection,
           filter?: Partial<ReferentialFilter>,
           opts?: {
      fetchPolicy?: FetchPolicy;
      withTotal: boolean;
    }): Observable<LoadResult<Referential>> {

    if (!filter || !filter.entityName) {
      console.error("[referential-service] Missing filter.entityName");
      throw { code: ErrorCodes.LOAD_REFERENTIAL_ERROR, message: "REFERENTIAL.ERROR.LOAD_REFERENTIAL_ERROR" };
    }

    filter = this.asFilter(filter);
    const entityName = filter.entityName;
    const uniqueEntityName = filter.entityName + (filter.searchJoin || '');

    const variables: any = {
      entityName,
      offset: offset || 0,
      size: size || 100,
      sortBy: sortBy || 'label',
      sortDirection: sortDirection || 'asc',
      filter: filter && filter.asPodObject()
    };

    let now = this._debug && Date.now();
    if (this._debug) console.debug(`[referential-service] Loading ${uniqueEntityName}...`, variables);

    const withTotal = (!opts || opts.withTotal !== false);
    const query = withTotal ? this.queries.loadAllWithTotal : this.queries.loadAll;
    return this.mutableWatchQuery<LoadResult<any>>({
      queryName: withTotal ? 'LoadAllWithTotal' : 'LoadAll',
      query,
      arrayFieldName: 'data',
      totalFieldName: withTotal ? 'total' : undefined,
      insertFilterFn: (d: Referential) => d.entityName === entityName,
      variables,
      error: { code: ErrorCodes.LOAD_REFERENTIAL_ERROR, message: "REFERENTIAL.ERROR.LOAD_REFERENTIAL_ERROR" },
      fetchPolicy: opts && opts.fetchPolicy || 'network-only'
    })
      .pipe(
        map(({data, total}) => {
          const entities = (data || []).map(Referential.fromObject);
          entities.forEach(r => r.entityName = uniqueEntityName);
          if (now) {
            console.debug(`[referential-service] ${uniqueEntityName} loaded in ${Date.now() - now}ms`, entities);
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
                filter?: ReferentialFilter,
                opts?: {
                  [key: string]: any;
                  fetchPolicy?: FetchPolicy;
                  debug?: boolean;
                  withTotal?: boolean;
                  toEntity?: boolean;
                }): Promise<LoadResult<Referential>> {

    if (!filter || !filter.entityName) {
      console.error("[referential-service] Missing filter.entityName");
      throw {code: ErrorCodes.LOAD_REFERENTIAL_ERROR, message: "REFERENTIAL.ERROR.LOAD_REFERENTIAL_ERROR"};
    }

    filter = this.asFilter(filter);
    const entityName = filter.entityName;
    const uniqueEntityName = filter.entityName + (filter.searchJoin || '');
    const debug = this._debug && (!opts || opts.debug !== false);

    const variables: any = {
      entityName: entityName,
      offset: offset || 0,
      size: size || 100,
      sortBy: sortBy || filter.searchAttribute || 'label',
      sortDirection: sortDirection || 'asc',
      filter: filter && filter.asPodObject()
    };

    const now = Date.now();
    if (debug) console.debug(`[referential-service] Loading ${uniqueEntityName} items...`, variables);

    const query = (!opts || opts.withTotal !== false) ? this.queries.loadAllWithTotal : this.queries.loadAll;
    const res = await this.graphql.query<LoadResult<any>>({
      query,
      variables,
      error: {code: ErrorCodes.LOAD_REFERENTIAL_ERROR, message: "REFERENTIAL.ERROR.LOAD_REFERENTIAL_ERROR"},
      fetchPolicy: opts && opts.fetchPolicy || 'network-only'
    });
    let data = (res && res.data || []) as Referential[];

    // Always use unique entityName, if need
    if (filter.entityName !== uniqueEntityName) {
      data = data.map(r => <Referential>{...r, entityName: uniqueEntityName});
    }

    // Convert to entities
    if (!opts || opts.toEntity !== false) {
      data = data.map(Referential.fromObject);
    }

    if (debug) console.debug(`[referential-service] ${uniqueEntityName} items loaded in ${Date.now() - now}ms`);
    return {
      data,
      total: res.total
    };

  }

  async saveAll(entities: Referential[], options?: any): Promise<Referential[]> {
    if (!entities) return entities;

    // Nothing to save: skip
    if (!entities.length) return;

    const entityName = entities[0].entityName;
    if (!entityName) {
      console.error("[referential-service] Could not save referential: missing entityName");
      throw { code: ErrorCodes.SAVE_REFERENTIAL_ERROR, message: "REFERENTIAL.ERROR.SAVE_REFERENTIAL_ERROR" };
    }

    if (entities.length !== entities.filter(e => e.entityName === entityName).length) {
      console.error("[referential-service] Could not save referential: more than one entityName found in the array to save!");
      throw { code: ErrorCodes.SAVE_REFERENTIAL_ERROR, message: "REFERENTIAL.ERROR.SAVE_REFERENTIAL_ERROR" };
    }

    const json = entities.map(t => t.asObject());

    const now = Date.now();
    if (this._debug) console.debug(`[referential-service] Saving all ${entityName}...`, json);

    await this.graphql.mutate<LoadResult<Referential>>({
      mutation: this.mutations.saveAll,
      variables: {
        data: json
      },
      error: { code: ErrorCodes.SAVE_REFERENTIAL_ERROR, message: "REFERENTIAL.ERROR.SAVE_REFERENTIAL_ERROR" },
      update: (cache, {data}) => {
        if (data && data.data) {
          // Update entities (id and update date)
          entities.forEach(entity => {
            const savedEntity = data.data.find(e => (e.id === entity.id || e.label === entity.label));
            if (savedEntity !== entity) {
              EntityUtils.copyIdAndUpdateDate(savedEntity, entity);
            }
          });

          // Update the cache
          this.insertIntoMutableCachedQueries(cache, {
            queries: this.getLoadQueries(),
            data: data.data
          });
        }

        if (this._debug) console.debug(`[referential-service] ${entityName} saved in ${Date.now() - now}ms`, entities);

      }
    });


    return entities;
  }

  async existsByLabel(label: string,
                      filter?: Partial<ReferentialFilter>,
                      opts?: {
                        fetchPolicy: FetchPolicy;
                      }): Promise<boolean> {

    if (!filter || !filter.entityName || !label) {
      console.error("[referential-service] Missing 'filter.entityName' or 'label'");
      throw {code: ErrorCodes.LOAD_REFERENTIAL_ERROR, message: "REFERENTIAL.ERROR.LOAD_REFERENTIAL_ERROR"};
    }

    filter = this.asFilter(filter);
    filter.label = label;

    const {total} = await this.graphql.query<{ total: number; }>({
      query: this.queries.count,
      variables : {
        entityName: filter.entityName,
        filter: filter.asPodObject()
      },
      error: { code: ErrorCodes.LOAD_REFERENTIAL_ERROR, message: "REFERENTIAL.ERROR.LOAD_REFERENTIAL_ERROR" },
      fetchPolicy: opts && opts.fetchPolicy || 'network-only'
    });

    return total > 0;
  }

  /**
   * Save a referential entity
   * @param entity
   */
  async save(entity: Referential, options?: EntitySaveOptions): Promise<Referential> {

    if (!entity.entityName) {
      console.error("[referential-service] Missing entityName");
      throw { code: ErrorCodes.SAVE_REFERENTIAL_ERROR, message: "REFERENTIAL.ERROR.SAVE_REFERENTIAL_ERROR" };
    }

    // Transform into json
    const json = entity.asObject();
    const isNew = isNil(json.id);

    const now = Date.now();
    if (this._debug) console.debug(`[referential-service] Saving ${entity.entityName}...`, json);

    await this.graphql.mutate<{ data: any }>({
      mutation: this.mutations.saveAll,
      variables: {
        data: [json]
      },
      error: { code: ErrorCodes.SAVE_REFERENTIAL_ERROR, message: "REFERENTIAL.ERROR.SAVE_REFERENTIAL_ERROR" },
      update: (cache, {data}) => {
        // Update entity
        const savedEntity = data && data.data && data.data[0];
        if (savedEntity === entity) {
          if (this._debug) console.debug(`[referential-service] ${entity.entityName} saved in ${Date.now() - now}ms`, entity);
          EntityUtils.copyIdAndUpdateDate(savedEntity, entity);
        }

        // Update the cache
        if (isNew) {
          this.insertIntoMutableCachedQueries(cache, {
            queries: this.getLoadQueries(),
            data: savedEntity
          });
        }

        if (options?.update) {
          options.update(cache, {data});
        }

      }
    });

    return entity;
  }

  /**
   * Delete referential entities
   */
  async deleteAll(entities: Referential[], options?: Partial<{
    update: MutationUpdaterFn<any>;
  }> | any): Promise<any> {

    // Filter saved entities
    entities = entities && entities
      .filter(e => !!e.id && !!e.entityName) || [];

    // Nothing to save: skip
    if (!entities.length) return;

    const entityName = entities[0].entityName;
    const ids = entities.filter(e => e.entityName === entityName).map(t => t.id);

    // Check that all entities have the same entityName
    if (entities.length > ids.length) {
      console.error("[referential-service] Could not delete referentials: only one entityName is allowed");
      throw { code: ErrorCodes.DELETE_REFERENTIAL_ERROR, message: "REFERENTIAL.ERROR.DELETE_REFERENTIAL_ERROR" };
    }

    const now = new Date();
    if (this._debug) console.debug(`[referential-service] Deleting ${entityName}...`, ids);

    await this.graphql.mutate<any>({
      mutation: this.mutations.deleteAll,
      variables: {
        entityName: entityName,
        ids: ids
      },
      error: { code: ErrorCodes.DELETE_REFERENTIAL_ERROR, message: "REFERENTIAL.ERROR.DELETE_REFERENTIAL_ERROR" },
      update: (proxy) => {
        // Remove from cache
        this.removeFromMutableCachedQueriesByIds(proxy, {
          queries: this.getLoadQueries(),
          ids
        });

        if (options && options.update) {
          options.update(proxy);
        }

        if (this._debug) console.debug(`[referential-service] ${entityName} deleted in ${new Date().getTime() - now.getTime()}ms`);
      }
    });
  }

  /**
   * Load referential types
   */
  loadTypes(): Observable<ReferentialType[]> {
    if (this._debug) console.debug("[referential-service] Loading referential types...");
    return this.graphql.watchQuery<LoadResult<ReferentialType>>({
      query: LoadReferentialTypes,
      variables: null,
      error: { code: ErrorCodes.LOAD_REFERENTIAL_ERROR, message: "REFERENTIAL.ERROR.LOAD_REFERENTIAL_ERROR" }
    })
      .pipe(
        map(({data}) => {
          return (data || []);
        })
      );
  }

  /**
   * Load entity levels
   */
  async loadLevels(entityName: string, options?: {
    fetchPolicy?: FetchPolicy
  }): Promise<ReferentialRef[]> {
    const now = Date.now();
    if (this._debug) console.debug(`[referential-service] Loading levels for ${entityName}...`);

    const {data} = await this.graphql.query<LoadResult<Referential>>({
      query: LoadReferentialLevels,
      variables: {
        entityName
      },
      error: { code: ErrorCodes.LOAD_REFERENTIAL_LEVELS_ERROR, message: "REFERENTIAL.ERROR.LOAD_REFERENTIAL_LEVELS_ERROR" },
      fetchPolicy: options && options.fetchPolicy || 'cache-first'
    });

    const entities = (data || []).map(ReferentialRef.fromObject);

    if (this._debug) console.debug(`[referential-service] Levels for ${entityName} loading in ${Date.now() - now}`, entities);

    return entities;
  }

  asFilter(filter: Partial<ReferentialFilter>): ReferentialFilter {
    return ReferentialFilter.fromObject(filter);
  }

  /* -- protected methods -- */

  protected fillDefaultProperties(entity: Referential) {
    entity.statusId = isNotNil(entity.statusId) ? entity.statusId : StatusIds.ENABLE;
  }

  protected getLoadQueries(): DocumentNode[] {
    return [this.queries.loadAll, this.queries.loadAllWithTotal].filter(isNotNil);
  }
}
