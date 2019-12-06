import {Injectable} from "@angular/core";
import gql from "graphql-tag";
import {BehaviorSubject, Observable} from "rxjs";
import {map} from "rxjs/operators";
import {isNotNil, LoadResult} from "../../shared/shared.module";
import {BaseDataService, EntityUtils, environment, Referential, StatusIds} from "../../core/core.module";
import {ErrorCodes} from "./errors";
import {AccountService} from "../../core/services/account.service";
import {ReferentialRef} from "../../core/services/model";

import {FetchPolicy} from "apollo-client";
import {ReferentialFilter, ReferentialService, TaxonNameFilter} from "./referential.service";
import {fetchAllPagesWithProgress, SuggestionDataService} from "../../shared/services/data-service.class";
import {GraphqlService} from "../../core/services/graphql.service";
import {LocationLevelIds, TaxonGroupIds, TaxonomicLevelIds} from "./model";
import {TaxonNameRef} from "./model/taxon.model";
import {NetworkService} from "../../core/services/network.service";
import {EntityStorage} from "../../core/services/entities-storage.service";

export class ReferentialRefFilter extends ReferentialFilter {

  static searchFilter<T extends Referential>(f: ReferentialRefFilter): (T) => boolean {

    const filterFns: ((T) => boolean)[] = [];

    // Filter by levels ids
    const levelIds = f.levelIds || (isNotNil(f.levelId) && [f.levelId]) || undefined;
    if (levelIds) {
      filterFns.push((entity) => !!levelIds.find(v => entity.levelId === v));
    }

    // Filter by status
    const statusIds = f.statusIds || (isNotNil(f.statusId) && [f.statusId]) || undefined;
    if (statusIds) {
      filterFns.push((entity) => !!statusIds.find(v => entity.statusId === v));
    }

    const searchTextFilter = EntityUtils.searchTextFilter(f.searchAttribute || f.searchAttributes, f.searchText)
    if (searchTextFilter) filterFns.push(searchTextFilter);

    if (!filterFns.length) return undefined;

    return (entity) => {
      return !filterFns.find(fn => !fn(entity));
    };
  }

  searchAttributes?: string[];
}


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

const LoadAllWithCountQuery: any = gql`
  query Referentials($entityName: String, $offset: Int, $size: Int, $sortBy: String, $sortDirection: String, $filter: ReferentialFilterVOInput){
    referentials(entityName: $entityName, offset: $offset, size: $size, sortBy: $sortBy, sortDirection: $sortDirection, filter: $filter){
      id
      label
      name
      statusId
      entityName
    }
    referentialsCount(entityName: $entityName, filter: $filter)
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
      entityName
    }
  }
`;

@Injectable({providedIn: 'root'})
export class ReferentialRefService extends BaseDataService
  implements SuggestionDataService<ReferentialRef> {

  private _importedEntities: string[];

  constructor(
    protected graphql: GraphqlService,
    protected referentialService: ReferentialService,
    protected accountService: AccountService,
    protected network: NetworkService,
    protected entities: EntityStorage
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
             withCount?: boolean;
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
      sortBy: sortBy || filter.searchAttribute || 'label',
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
    if (this._debug) console.debug(`[referential-ref-service] Watching ${entityName} items...`, variables);

    const query = (!opts || opts.withCount !== false) ? LoadAllWithCountQuery : LoadAllQuery;
    return this.graphql.watchQuery<{ referentials: any[]; referentialsCount: number }>({
      query,
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
                filter?: ReferentialRefFilter,
                opts?: {
                  [key: string]: any;
                  fetchPolicy?: FetchPolicy;
                  debug?: boolean;
                  withCount?: boolean;
                  transformToEntity?: boolean;
                }): Promise<LoadResult<ReferentialRef>> {

    if (!filter || !filter.entityName) {
      console.error("[referential-ref-service] Missing filter.entityName");
      throw {code: ErrorCodes.LOAD_REFERENTIAL_ERROR, message: "REFERENTIAL.ERROR.LOAD_REFERENTIAL_ERROR"};
    }

    const entityName = filter.entityName;
    const debug = this._debug && (!opts || opts.debug !== false);

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
    if (debug) console.debug(`[referential-ref-service] Loading ${entityName} items...`, variables);

    // Offline mode: read from the entities storage
    const offline = this.network.offline && (!opts || opts.fetchPolicy !== 'network-only');
    if (offline) {
      const filterFn = ReferentialRefFilter.searchFilter(filter);
      const res = await this.entities.loadAll(filter.entityName + 'VO',
        {
          ...variables,
          filter: filterFn
        }
      );
      const data = (!opts || opts.transformToEntity !== false) ?
        (res && res.data || []).map(ReferentialRef.fromObject) :
        (res && res.data || []) as ReferentialRef[];
      if (debug) console.debug(`[referential-ref-service] ${entityName} items loaded (from offline storage) in ${Date.now() - now}ms`);
      return {
        data: data,
        total: res.total
      };
    }

    // Online mode: use graphQL
    else {
      const query = (!opts || opts.withCount !== false) ? LoadAllWithCountQuery : LoadAllQuery;
      const res = await this.graphql.query<{ referentials: any[]; referentialsCount: number }>({
        query,
        variables,
        error: {code: ErrorCodes.LOAD_REFERENTIAL_ERROR, message: "REFERENTIAL.ERROR.LOAD_REFERENTIAL_ERROR"},
        fetchPolicy: opts && opts.fetchPolicy || 'cache-first'
      });
      const data = (!opts || opts.transformToEntity !== false) ?
        (res && res.referentials || []).map(ReferentialRef.fromObject) :
        (res && res.referentials || []) as ReferentialRef[];
      if (debug) console.debug(`[referential-ref-service] ${entityName} items loaded in ${Date.now() - now}ms`);
      return {
        data: data,
        total: res.referentialsCount
      };
    }

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
      { ...opts, searchText: value}, {
        withCount: false // not need
      });
    return res.data;
  }

  async loadAllTaxonNames(offset: number,
                          size: number,
                          sortBy?: string,
                          sortDirection?: string,
                          filter?: TaxonNameFilter,
                          opts?: {
                            [key: string]: any;
                            fetchPolicy?: FetchPolicy;
                            debug?: boolean;
                            transformToEntity?: boolean;
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
    const debug = this._debug && (!opts || opts.debug !== false);
    if (debug) console.debug(`[referential-ref-service] Loading TaxonName items...`, variables);

    const res = await this.graphql.query<{ taxonNames: any[]}>({
      query: LoadAllTaxonNamesQuery,
      variables: variables,
      error: {code: ErrorCodes.LOAD_REFERENTIAL_ERROR, message: "REFERENTIAL.ERROR.LOAD_REFERENTIAL_ERROR"},
      fetchPolicy: opts && opts.fetchPolicy || "cache-first"
    });
    const data = (!opts || opts.transformToEntity !== false) ?
      (res && res.taxonNames || []).map(TaxonNameRef.fromObject) :
      (res && res.taxonNames || []) as TaxonNameRef[];
    if (debug) console.debug(`[referential-ref-service] TaxonName items loaded in ${Date.now() - now}ms`, data);
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

  executeImport(opts?: {
    entityNames?: string[],
    statusIds?: number[];
    maxProgression?: number;
  }): Observable<number>{

    const progress = new BehaviorSubject(0);
    this.executeImportWithProgress(progress, opts)
      .then(() => progress.complete())
      .catch((err: any) => {
        console.error("[referential-ref-service] Error during importation: " + (err && err.message || err), err);
        progress.error(err);
      });

    return progress;
  }

  /* -- protected methods -- */

  protected async executeImportWithProgress(progress: BehaviorSubject<number>,
                                            opts?: {
                                              entityNames?: string[],
                                              maxProgression?: number;
                                              statusIds?: number[];
                                            }) {

    const entityNames = opts && opts.entityNames || ['Location', 'Gear', 'Metier', 'TaxonGroup', 'TaxonName', 'Department', 'QualityFlag', 'SaleType', 'VesselType'];

    const statusIds = opts && opts.statusIds || [StatusIds.ENABLE, StatusIds.TEMPORARY];

    const maxProgression = opts && opts.maxProgression || 100;
    const stepCount = entityNames.length;
    const progressionStep = maxProgression ? (maxProgression / (stepCount + 1)) : undefined;

    const now = Date.now();
    console.info(`[referential-ref-service] Importing ${entityNames.length} referential...`);

    const importedEntities = [];
    const jobs = entityNames.map(entityName => {
      let filter: ReferentialFilter;
      let promise: Promise<LoadResult<any>>;
      switch (entityName) {
        case 'TaxonName':
          promise = fetchAllPagesWithProgress<any>((offset, size) =>
              this.loadAllTaxonNames(offset, size, 'id', null,  {
                statusIds: [StatusIds.ENABLE],
                taxonomicLevelIds: [TaxonomicLevelIds.SPECIES, TaxonomicLevelIds.SUBSPECIES]
              }, {
                fetchPolicy: "network-only",
                debug: false
              }).
              then(data => {
                return {data};
              }),
            progress,
            progressionStep);
          break;
        case 'Metier':
          filter = {entityName, statusIds, searchJoin: "TaxonGroup" };
          break;
        case 'TaxonGroup':
          filter = {entityName, statusIds, levelIds: [TaxonGroupIds.FAO] };
          break;
        case 'Location':
          filter = {entityName, statusIds, levelIds: Object.keys(LocationLevelIds).map(key => LocationLevelIds[key]) };
          break;
        default:
          filter = {entityName, statusIds};
          break;
      }
      if (!promise) {
        promise = fetchAllPagesWithProgress<any>((offset, size) =>
            this.referentialService.loadAll(offset, size, 'id', null, filter, {
              debug: false,
              fetchPolicy: "network-only",
              withTotal: (offset === 0), // Compute total only once
              toEntity: false
            }),
          progress,
          progressionStep);
      }
      return promise
        .then((res) => {
          importedEntities.push(entityName);
          return this.entities.saveAll(res.data, {
            entityName: entityName + 'VO'
          });
        })
        .catch(err => {
          console.error(`[referential-ref-service] Failed to import ${entityName}: ${err && err.message || err}`, err);
          throw err;
        });
    });

    // Import by filter
    await Promise.all(jobs);

    // Not all entity imported: error
    if (importedEntities.length < entityNames.length) {
      console.error(`[referential-ref-service] Importation failed in ${Date.now() - now}ms`);
      progress.error({code: ErrorCodes.IMPORT_REFERENTIAL_ERROR, message: 'ERROR.IMPORT_REFERENTIAL_ERROR'});
    }
    else {
      // Success
      console.info(`[referential-ref-service] Successfully import ${entityNames.length} entities in ${Date.now() - now}ms`);
      this._importedEntities = importedEntities;
    }

    // Fill the progression to max
    progress.next(maxProgression);
  }

}
