import {Injectable} from "@angular/core";
import gql from "graphql-tag";
import {Observable} from "rxjs";
import {map} from "rxjs/operators";
import {EntityUtils, isNil, isNotNil, Referential, StatusIds} from "./model";
import {LoadResult, TableDataService} from "../../shared/shared.module";
import {BaseDataService} from "../../core/core.module";
import {ErrorCodes} from "./errors";
import {AccountService} from "../../core/services/account.service";

import {FetchPolicy} from "apollo-client";
import {GraphqlService} from "../../core/services/graphql.service";

export declare class ReferentialFilter {
  entityName: string;
  label?: string;
  name?: string;

  statusId?: number;
  statusIds?: number[];

  levelId?: number;
  levelIds?: number[];

  searchJoin?: string; // If search is on a sub entity (e.g. Metier can esearch on TaxonGroup)
  searchText?: string;
  searchAttribute?: string;
}
export declare class TaxonNameFilter {

  taxonomicLevelId?: number;
  taxonomicLevelIds?: number[];
  searchText?: string;
  searchAttribute?: string;
  statusId?: number;
  statusIds?: number[];

  taxonGroupId: number;
  taxonGroupIds?: number[];
}

export declare class ReferentialType {
  id: string;
  level?: string;
}

const LoadAllQuery: any = gql`
  query Referentials($entityName: String, $offset: Int, $size: Int, $sortBy: String, $sortDirection: String, $filter: ReferentialFilterVOInput){
    referentials(entityName: $entityName, offset: $offset, size: $size, sortBy: $sortBy, sortDirection: $sortDirection, filter: $filter){
      id
      label
      name
      description
      comments
      updateDate
      creationDate
      statusId
      levelId
      entityName
    }
    referentialsCount(entityName: $entityName)
  }
`;

const LoadReferentialTypes: any = gql`
  query ReferentialTypes{
    referentialTypes {
       id
       level
    }
  }
`;

const LoadReferentialLevels: any = gql`
  query ReferentialLevels($entityName: String) {
    referentialLevels(entityName: $entityName){
      id
      label
      name
      entityName
    }
  }
`;

const SaveAllQuery: any = gql`
  mutation SaveReferentials($referentials:[ReferentialVOInput]){
    saveReferentials(referentials: $referentials){
      id
      label
      name
      description
      comments
      updateDate
      creationDate
      statusId
      levelId
      entityName
    }
  }
`;

const DeleteAll: any = gql`
  mutation deleteReferentials($entityName: String, $ids:[Int]){
    deleteReferentials(entityName: $entityName, ids: $ids)
  }
`;

@Injectable({providedIn: 'root'})
export class ReferentialService extends BaseDataService implements TableDataService<Referential, ReferentialFilter> {

  constructor(
    protected graphql: GraphqlService,
    protected accountService: AccountService
  ) {
    super(graphql);
    this._debug = true;
  }

  watchAll(offset: number,
           size: number,
           sortBy?: string,
           sortDirection?: string,
           filter?: ReferentialFilter,
           options?: {
      fetchPolicy?: FetchPolicy
    }): Observable<LoadResult<Referential>> {

    if (!filter || !filter.entityName) {
      console.error("[referential-service] Missing filter.entityName");
      throw { code: ErrorCodes.LOAD_REFERENTIAL_ERROR, message: "REFERENTIAL.ERROR.LOAD_REFERENTIAL_ERROR" };
    }

    const entityName = filter.entityName;

    const variables: any = {
      entityName: entityName,
      offset: offset || 0,
      size: size || 100,
      sortBy: sortBy || 'label',
      sortDirection: sortDirection || 'asc',
      filter: {
        label: filter.label,
        name: filter.name,
        searchText: filter.searchText,
        searchAttribute: filter.searchAttribute,
        levelIds: isNotNil(filter.levelId) ? [filter.levelId] : filter.levelIds,
        statusIds: isNotNil(filter.statusId) ?  [filter.statusId] : (filter.statusIds || [StatusIds.ENABLE])
      }
    };

    const now = new Date();
    if (this._debug) console.debug(`[referential-service] Loading ${entityName}...`, variables);

    // Saving variables, to be able to update the cache when saving or deleting
    this._lastVariables.loadAll = variables;

    return this.graphql.watchQuery<{ referentials: any[]; referentialsCount: number }>({
        query: LoadAllQuery,
        variables: variables,
        error: { code: ErrorCodes.LOAD_REFERENTIAL_ERROR, message: "REFERENTIAL.ERROR.LOAD_REFERENTIAL_ERROR" },
        fetchPolicy: options && options.fetchPolicy || 'network-only'
      })
      .pipe(
        map(({referentials, referentialsCount}) => {
          const data = (referentials || []).map(Referential.fromObject);
          data.forEach(r => r.entityName = entityName);
          if (this._debug) console.debug(`[referential-service] ${entityName} loaded in ${new Date().getTime() - now.getTime()}ms`, data);
          return {
            data: data,
            total: referentialsCount
          };
        })
      );
  }

  // loadAll(offset: number, size: number, sortBy?: string, sortDirection?: string, filter?: ReferentialFilter, options?: any): Promise<LoadResult<Referential>> {
  //   console.warn("TODO: implement loadAll");
  //   // TODO
  //   return this.watchAll(offset, size, sortBy, sortDirection, filter, options)
  //     .pipe(first()).toPromise();
  // }

  async saveAll(entities: Referential[], options?: any): Promise<Referential[]> {
    if (!entities) return entities;

    // Nothing to save: skip
    if (!entities.length) return;

    const entityName = entities[0].entityName;
    if (!entityName) {
      console.error("[referential-service] Could not save referentials: missing entityName");
      throw { code: ErrorCodes.SAVE_REFERENTIALS_ERROR, message: "REFERENTIAL.ERROR.SAVE_REFERENTIALS_ERROR" };
    }

    if (entities.length != entities.filter(e => e.entityName === entityName).length) {
      console.error("[referential-service] Could not save referentials: only one entityName is allowed");
      throw { code: ErrorCodes.SAVE_REFERENTIALS_ERROR, message: "REFERENTIAL.ERROR.SAVE_REFERENTIALS_ERROR" };
    }

    const json = entities.map(t => t.asObject());

    const now = new Date();
    if (this._debug) console.debug(`[referential-service] Saving all ${entityName}...`, json);

    const res = await this.graphql.mutate<{ saveReferentials: Referential[] }>({
      mutation: SaveAllQuery,
      variables: {
        referentials: json
      },
      error: { code: ErrorCodes.SAVE_REFERENTIALS_ERROR, message: "REFERENTIAL.ERROR.SAVE_REFERENTIALS_ERROR" }
    });

    if (res && res.saveReferentials) {
      // Update entites (id and update date)
      entities.forEach(entity => {
        const data = res.saveReferentials.find(res => (res.id === entity.id || res.label === entity.label));
        entity.id = data && data.id || entity.id;
        entity.updateDate = data && data.updateDate || entity.updateDate;
      });

      // Update the cache
      if (this._lastVariables.loadAll) {
        if (this._debug) console.debug(`[referential-service] Updating cache with saved ${entityName}...`);
        this.addManyToQueryCache({
          query: LoadAllQuery,
          variables: this._lastVariables.loadAll
        }, 'referentials', res.saveReferentials);
      }
    }

    if (this._debug) console.debug(`[referential-service] ${entityName} saved in ${new Date().getTime() - now.getTime()}ms`, entities);

    return entities;
  }

  /**
   * Save a referential entity
   * @param entity
   */
  async save(entity: Referential, options?: any): Promise<Referential> {

    if (!entity.entityName) {
      console.error("[referential-service] Missing entityName");
      throw { code: ErrorCodes.SAVE_REFERENTIAL_ERROR, message: "REFERENTIAL.ERROR.SAVE_REFERENTIAL_ERROR" };
    }

    // Transform into json
    const json = entity.asObject();
    const isNew = !json.id;

    const now = new Date();
    if (this._debug) console.debug(`[referential-service] Saving ${entity.entityName}...`, json);

    const data = await this.graphql.mutate<{ saveReferentials: any }>({
      mutation: SaveAllQuery,
      variables: {
        referentials: [json]
      },
      error: { code: ErrorCodes.SAVE_REFERENTIAL_ERROR, message: "REFERENTIAL.ERROR.SAVE_REFERENTIAL_ERROR" }
    });

    // Update entity
    const savedEntity = data && data.saveReferentials && data.saveReferentials[0];
    EntityUtils.copyIdAndUpdateDate(savedEntity, entity);

    // Update the cache
    if (isNew && this._lastVariables.loadAll) {
      if (this._debug) console.debug(`[referential-service] Updating cache with saved ${entity.entityName}...`);
      this.addToQueryCache({
        query: LoadAllQuery,
        variables: this._lastVariables.loadAll
      }, 'referentials', entity.asObject());
    }

    if (this._debug) console.debug(`[referential-service] ${entity.entityName} saved in ${new Date().getTime() - now.getTime()}ms`, entity);

    return entity;
  }

  /**
   * Delete referential entities
   */
  async deleteAll(entities: Referential[], options?: any): Promise<any> {

    // Filter saved entities
    entities = entities && entities
      .filter(e => !!e.id && !!e.entityName) || [];

    // Nothing to save: skip
    if (!entities.length) return;

    const entityName = entities[0].entityName;
    const ids = entities.filter(e => e.entityName == entityName).map(t => t.id);

    // Check that all entities have the same entityName
    if (entities.length > ids.length) {
      console.error("[referential-service] Could not delete referentials: only one entityName is allowed");
      throw { code: ErrorCodes.DELETE_REFERENTIALS_ERROR, message: "REFERENTIAL.ERROR.DELETE_REFERENTIALS_ERROR" };
    }

    const now = new Date();
    if (this._debug) console.debug(`[referential-service] Deleting ${entityName}...`, ids);

    const res = await this.graphql.mutate<any>({
      mutation: DeleteAll,
      variables: {
        entityName: entityName,
        ids: ids
      },
      error: { code: ErrorCodes.DELETE_REFERENTIALS_ERROR, message: "REFERENTIAL.ERROR.DELETE_REFERENTIALS_ERROR" }
    });

    // Remove from cache
    if (this._lastVariables.loadAll) {
      this.removeToQueryCacheByIds({
        query: LoadAllQuery,
        variables: this._lastVariables.loadAll
      }, 'referentials', ids);
    }

    if (this._debug) console.debug(`[referential-service] ${entityName} deleted in ${new Date().getTime() - now.getTime()}ms`);

    return res;
  }

  /**
   * Load referential types
   */
  loadTypes(): Observable<ReferentialType[]> {
    if (this._debug) console.debug("[referential-service] Loading referential types...");
    return this.graphql.watchQuery<{ referentialTypes: ReferentialType[] }>({
      query: LoadReferentialTypes,
      variables: null,
      error: { code: ErrorCodes.LOAD_REFERENTIAL_ENTITIES_ERROR, message: "REFERENTIAL.ERROR.LOAD_REFERENTIAL_ENTITIES_ERROR" }
    })
      .pipe(
        map((data) => {
          const res = (data && data.referentialTypes || []);
          return res;
        })
      );
  }

  /**
   * Load entity levels
   */
  async loadLevels(entityName: string, options?: {
    fetchPolicy?: FetchPolicy
  }): Promise<Referential[]> {
    const now = Date.now();
    if (this._debug) console.debug(`[referential-service] Loading levels for ${entityName}...`);

    const data = await this.graphql.query<{ referentialLevels: Referential[] }>({
      query: LoadReferentialLevels,
      variables: {
        entityName: entityName
      },
      error: { code: ErrorCodes.LOAD_REFERENTIAL_LEVELS_ERROR, message: "REFERENTIAL.ERROR.LOAD_REFERENTIAL_LEVELS_ERROR" },
      fetchPolicy: options && options.fetchPolicy || 'cache-first'
    });

    const res = (data && data.referentialLevels || []).map(Referential.fromObject);

    if (this._debug) console.debug(`[referential-service] Levels for ${entityName} loading in ${Date.now() - now}`, res);

    return res;
  }

  /* -- protected methods -- */


  protected fillDefaultProperties(entity: Referential) {

    entity.statusId = isNotNil(entity.statusId) ? entity.statusId : StatusIds.ENABLE;
  }
}
