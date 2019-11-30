import {Injectable} from "@angular/core";
import gql from "graphql-tag";
import {BehaviorSubject, Observable} from "rxjs";
import {map} from "rxjs/operators";
import {isNil, isNotNil, LoadResult} from "../../shared/shared.module";
import {BaseDataService, EntityUtils, environment, StatusIds} from "../../core/core.module";
import {ErrorCodes} from "./errors";
import {AccountService} from "../../core/services/account.service";
import {ReferentialRef} from "../../core/services/model";

import {FetchPolicy} from "apollo-client";
import {ReferentialFilter, TaxonNameFilter} from "./referential.service";
import {SuggestionDataService} from "../../shared/services/data-service.class";
import {GraphqlService} from "../../core/services/graphql.service";
import {LocationLevelIds, TaxonomicLevelIds} from "./model";
import {TaxonNameRef} from "./model/taxon.model";

const LoadAllQuery: any = gql`
  query Referentials($entityName: String, $offset: Int, $size: Int, $sortBy: String, $sortDirection: String, $filter: ReferentialFilterVOInput){
    referentials(entityName: $entityName, offset: $offset, size: $size, sortBy: $sortBy, sortDirection: $sortDirection, filter: $filter){
      id
      label
      name
      statusId
      entityName
    }
  }
`;

const LoadAllTaxonNamesQuery: any = gql`
  query TaxonNames($offset: Int, $size: Int, $sortBy: String, $sortDirection: String, $filter: TaxonNameFilterVOInput){
    taxonNames(offset: $offset, size: $size, sortBy: $sortBy, sortDirection: $sortDirection, filter: $filter){
      id
      label
      name
      statusId
      referenceTaxonId
    }
  }
`;

@Injectable({providedIn: 'root'})
export class ReferentialRefService extends BaseDataService
  implements SuggestionDataService<ReferentialRef> {


  private _importProgress: BehaviorSubject<number>;
  private _importedEntities: string[];

  constructor(
    protected graphql: GraphqlService,
    protected accountService: AccountService
  ) {
    super(graphql);

    // -- For DEV only
    this._debug = !environment.production;
  }

  /**
   * @param offset
   * @param size
   * @param sortBy
   * @param sortDirection
   * @param filter
   * @param opts
   */
  watchAll(offset: number,
           size: number,
           sortBy?: string,
           sortDirection?: string,
           filter?: ReferentialFilter,
           opts?: {
             [key: string]: any;
             fetchPolicy?: FetchPolicy;
           }): Observable<LoadResult<ReferentialRef>> {

    if (!filter || !filter.entityName) {
      console.error("[referential-ref-service] Missing filter.entityName");
      throw {code: ErrorCodes.LOAD_REFERENTIAL_ERROR, message: "REFERENTIAL.ERROR.LOAD_REFERENTIAL_ERROR"};
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
        levelId: filter.levelId,
        levelIds: filter.levelIds,
        statusIds: isNotNil(filter.statusId) ? [filter.statusId] : [StatusIds.ENABLE]
      }
    };

    const now = Date.now();
    if (this._debug) console.debug(`[referential-ref-service] Watching references on ${entityName}...`, variables);

    return this.graphql.watchQuery<{ referentials: any[]; referentialsCount: number }>({
      query: LoadAllQuery,
      variables: variables,
      error: {code: ErrorCodes.LOAD_REFERENTIAL_ERROR, message: "REFERENTIAL.ERROR.LOAD_REFERENTIAL_ERROR"},
      fetchPolicy: opts && opts.fetchPolicy || "cache-first"
    })
      .pipe(
        map(({referentials, referentialsCount}) => {
          const data = (referentials || []).map(ReferentialRef.fromObject);
          if (this._debug) console.debug(`[referential-ref-service] References on ${entityName} loaded in ${Date.now() - now}ms`);
          return {
            data: data,
            total: referentialsCount
          };
        })
      );
  }

  async loadAll(offset: number,
                size: number,
                sortBy?: string,
                sortDirection?: string,
                filter?: ReferentialFilter,
                opts?: {
                  [key: string]: any;
                  fetchPolicy?: FetchPolicy;
                  debug?: boolean;
                }): Promise<LoadResult<ReferentialRef>> {

    if (!filter || !filter.entityName) {
      console.error("[referential-ref-service] Missing filter.entityName");
      throw {code: ErrorCodes.LOAD_REFERENTIAL_ERROR, message: "REFERENTIAL.ERROR.LOAD_REFERENTIAL_ERROR"};
    }

    const entityName = filter.entityName;

    const variables: any = {
      entityName: entityName,
      offset: offset || 0,
      size: size || 100,
      sortBy: sortBy || filter.searchAttribute || 'label',
      sortDirection: sortDirection || 'asc',
      filter: {
        label: filter.label,
        name: filter.name,
        searchText: filter.searchText,
        searchAttribute: filter.searchAttribute,
        searchJoin: filter.searchJoin,
        levelIds: isNotNil(filter.levelId) ? [filter.levelId] : filter.levelIds,
        statusIds: isNotNil(filter.statusId) ? [filter.statusId] : (filter.statusIds || [StatusIds.ENABLE])
      }
    };

    const now = Date.now();
    if (this._debug && (!opts || opts.debug !== false)) console.debug(`[referential-ref-service] Loading references on ${entityName}...`, variables);

    const res = await this.graphql.query<{ referentials: any[]; referentialsCount: number }>({
      query: LoadAllQuery,
      variables: variables,
      error: {code: ErrorCodes.LOAD_REFERENTIAL_ERROR, message: "REFERENTIAL.ERROR.LOAD_REFERENTIAL_ERROR"},
      fetchPolicy: opts && opts.fetchPolicy || 'cache-first'
    });
    const data = (res && res.referentials || []).map(ReferentialRef.fromObject);
    if (this._debug && (!opts || opts.debug !== false)) console.debug(`[referential-ref-service] References on ${entityName} loaded in ${Date.now() - now}ms`);
    return {
      data: data,
      total: res.referentialsCount
    };
  }

  async suggest(value: any, opts: {
    entityName: string;
    levelId?: number;
    levelIds?: number[];
    searchAttribute?: string;
    statusId?: number;
    statusIds?: number[];
    searchJoin?: string;
  }): Promise<ReferentialRef[]> {
    if (EntityUtils.isNotEmpty(value)) return [value];
    value = (typeof value === "string" && value !== '*') && value || undefined;
    const res = await this.loadAll(0, !value ? 30 : 10, undefined, undefined,
      {
        entityName: opts.entityName,
        levelId: opts.levelId,
        levelIds: opts.levelIds,
        searchText: value as string,
        searchJoin: opts.searchJoin,
        searchAttribute: opts.searchAttribute,
        statusId: opts.statusId,
        statusIds: opts.statusIds
      });
    return res.data;
  }

  async loadAllTaxonNames(offset: number,
               size: number,
               sortBy?: string,
               sortDirection?: string,
               filter?: TaxonNameFilter,
               options?: {
                 [key: string]: any;
                 fetchPolicy?: FetchPolicy;
               }): Promise<TaxonNameRef[]> {

    if (!filter) {
      console.error("[referential-ref-service] Missing filter");
      throw {code: ErrorCodes.LOAD_REFERENTIAL_ERROR, message: "REFERENTIAL.ERROR.LOAD_REFERENTIAL_ERROR"};
    }

    const variables: any = {
      offset: offset || 0,
      size: size || 100,
      sortBy: sortBy || filter.searchAttribute || 'label',
      sortDirection: sortDirection || 'asc',
      filter: {
        searchText: filter.searchText,
        searchAttribute: filter.searchAttribute,
        taxonomicLevelIds: isNotNil(filter.taxonomicLevelId) ? [filter.taxonomicLevelId] : (filter.taxonomicLevelIds || [TaxonomicLevelIds.SPECIES, TaxonomicLevelIds.SUBSPECIES]),
        statusIds: isNotNil(filter.statusId) ? [filter.statusId] : (filter.statusIds || [StatusIds.ENABLE]),
        taxonGroupIds: isNotNil(filter.taxonGroupId) ? [filter.taxonGroupId] : (filter.taxonGroupIds || undefined)
      }
    };

    const now = Date.now();
    if (this._debug) console.debug(`[referential-ref-service] Loading taxon names...`, variables);

    const res = await this.graphql.query<{ taxonNames: any[]}>({
      query: LoadAllTaxonNamesQuery,
      variables: variables,
      error: {code: ErrorCodes.LOAD_REFERENTIAL_ERROR, message: "REFERENTIAL.ERROR.LOAD_REFERENTIAL_ERROR"},
      fetchPolicy: options && options.fetchPolicy || "cache-first"
    });
    const data = (res && res.taxonNames || []).map(TaxonNameRef.fromObject);
    if (this._debug) console.debug(`[referential-ref-service] Taxon names loaded in ${Date.now() - now}ms`, data);
    return data;
  }

  async suggestTaxonNames(value: any, options: {
    taxonomicLevelId?: number;
    taxonomicLevelIds?: number[];
    searchAttribute?: string;
    taxonGroupId?: number;
  }): Promise<TaxonNameRef[]> {
    if (EntityUtils.isNotEmpty(value)) return [value];
    value = (typeof value === "string" && value !== '*') && value || undefined;
    return await this.loadAllTaxonNames(0, !value ? 30 : 10, undefined, undefined,
      {
        searchText: value as string,
        searchAttribute: options.searchAttribute,
        taxonomicLevelId: options.taxonomicLevelId,
        taxonomicLevelIds: options.taxonomicLevelIds,
        taxonGroupId: options.taxonGroupId
      });
  }

  importAll(opts?: {
    entityNames?: string[],
    maxProgression?: number;
  }): Observable<number>{

    if (this._importProgress) return this._importProgress; // Skip if already running

    this._importProgress = new BehaviorSubject<number>(0);

    this.importAllWithProgress(this._importProgress, opts)
      .then(() => {
        this._importProgress.complete();
        this._importProgress = null;
      })
      .catch((err: any) => {
        console.error("[referential-ref-service] [synchro] Error during importation: " + (err && err.message || err), err);
        //this._importProgress.complete();
        this._importProgress.error(err);
        this._importProgress = null;
      });
    return this._importProgress;
  }

  /* -- protected methods -- */

  protected async importAllWithProgress(progress: BehaviorSubject<number>,
    opts?: {
      entityNames?: string[],
      maxProgression?: number;
      statusIds?: number[];
    }) {

    const entityNames = opts && opts.entityNames || ['Location', 'Gear', 'Metier', 'TaxonGroup', 'TaxonName', 'Department'];
    const statusIds = opts && opts.statusIds || [StatusIds.ENABLE, StatusIds.TEMPORARY];

    const maxProgression = opts && opts.maxProgression || 100;
    const progressionStepCount = entityNames.length;
    const progressionStep = (maxProgression - progress.getValue()) / progressionStepCount;

    const now = Date.now();
    console.info("[referential-ref-service] [synchro] Starting to import...");

    const entityFilters = entityNames.map(entityName => {
      switch (entityName) {
        case 'Location':
          const locationLevelIds = Object.keys(LocationLevelIds).map(key => LocationLevelIds[key]);
          return {entityName: 'Location', levelIds: locationLevelIds, statusIds};
        default:
          return {entityName, statusIds};
      }
    });

    // Import by filter
    const importedEntities = [];
    await Promise.all(entityFilters.map(filter => this.importByFilter(filter, progress, progressionStep)
      .then(() => {
        importedEntities.push(filter.entityName);
      })
      .catch(err => {
        console.error(`[referential-ref-service] [synchro] Failed to import ${filter.entityName}: ${err && err.message || err}`, err);
      })
    ));

    // Not all entity imported: error
    if (importedEntities.length < entityNames.length) {
      console.error(`[referential-ref-service] [synchro] Importation failed in ${Date.now() - now}ms`);
      progress.error({code: ErrorCodes.IMPORT_REFERENTIAL_ERROR, message: 'ERROR.IMPORT_REFERENTIAL_ERROR'});
    }

    // Success
    console.info(`[referential-ref-service] [synchro] Successfully import ${entityNames.length} referential in ${Date.now() - now}ms`);
    this._importedEntities = importedEntities;
    progress.next(maxProgression);

  }

  async importByFilter(filter: ReferentialFilter, progression: BehaviorSubject<number>, stepSize: number) {

    if (this._debug) console.debug(`[referential-ref-service] [synchro] Loading ${filter.entityName}s...`);

    const now = Date.now();
    let offset = 0;
    let total: number = undefined;
    let size: number;
    const fetchSize = 1000;
    do {
      if (this._debug && offset > 0) {
        console.debug(`[referential-ref-service] [synchro] Loading ${filter.entityName}s... (${offset / fetchSize})`);
      }

      // Get some items, using paging
      const res = await this.loadAll(offset, fetchSize, null, null, filter, {
        fetchPolicy: "network-only",
        debug: false // avoid too many logs
      });
      size = res.data && res.data.length || 0;
      offset += size;

      // Set total count (only if not already set)
      if (isNil(total) && isNotNil(res.total)) {
        total = res.total;
      }
    } while ((isNil(total) && size === fetchSize) || (isNotNil(total) && offset < total));

    console.info(`[referential-ref-service] [synchro] ${offset} ${filter.entityName}s loaded in ${Date.now() - now}ms`);


    progression.next(progression.getValue() + stepSize);
  }
}
