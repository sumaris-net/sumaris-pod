import {Injectable} from "@angular/core";
import gql from "graphql-tag";
import {BehaviorSubject, Observable} from "rxjs";
import {map} from "rxjs/operators";
import {isNotEmptyArray, isNotNil, LoadResult} from "../../shared/shared.module";
import {BaseDataService, EntityUtils, environment, Referential, StatusIds} from "../../core/core.module";
import {ErrorCodes} from "./errors";
import {AccountService} from "../../core/services/account.service";
import {ReferentialRef} from "../../core/services/model";

import {FetchPolicy} from "apollo-client";
import {ReferentialFilter, ReferentialService} from "./referential.service";
import {fetchAllPagesWithProgress, SuggestionDataService} from "../../shared/services/data-service.class";
import {GraphqlService} from "../../core/services/graphql.service";
import {LocationLevelIds, TaxonGroupIds, TaxonomicLevelIds} from "./model";
import {TaxonNameRef} from "./model/taxon.model";
import {NetworkService} from "../../core/services/network.service";
import {EntityStorage} from "../../core/services/entities-storage.service";
import {ReferentialFragments} from "./referential.queries";

export type ReferentialRefFilter = ReferentialFilter & {
  searchAttributes?: string[];
};

export type TaxonNameRefFilter = Partial<ReferentialRefFilter> & {

  taxonGroupId?: number;
  taxonGroupIds?: number[];
};



const LoadAllQuery: any = gql`
  query ReferentialRefs($entityName: String, $offset: Int, $size: Int, $sortBy: String, $sortDirection: String, $filter: ReferentialFilterVOInput){
    referentials(entityName: $entityName, offset: $offset, size: $size, sortBy: $sortBy, sortDirection: $sortDirection, filter: $filter){
      ...ReferentialFragment
    }
  }
  ${ReferentialFragments.referential}
`;

const LoadAllWithCountQuery: any = gql`
  query ReferentialRefs($entityName: String, $offset: Int, $size: Int, $sortBy: String, $sortDirection: String, $filter: ReferentialFilterVOInput){
    referentials(entityName: $entityName, offset: $offset, size: $size, sortBy: $sortBy, sortDirection: $sortDirection, filter: $filter){
      ...ReferentialFragment
    }
    referentialsCount(entityName: $entityName, filter: $filter)
  }
  ${ReferentialFragments.referential}
`;

const LoadAllTaxonNamesQuery: any = gql`
  query TaxonNames($offset: Int, $size: Int, $sortBy: String, $sortDirection: String, $filter: TaxonNameFilterVOInput){
    taxonNames(offset: $offset, size: $size, sortBy: $sortBy, sortDirection: $sortDirection, filter: $filter){
      ...FullTaxonNameFragment
    }
  }
  ${ReferentialFragments.fullTaxonName}
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

    let now = this._debug && Date.now();
    if (this._debug) console.debug(`[referential-ref-service] Watching ${entityName} items...`, variables);
    let $loadResult: Observable<{referentials?: any[]; referentialsCount?: number}>;

    if (this.network.offline) {
      $loadResult = this.entities.watchAll(entityName,
        {
          ...variables,
          filter: this.createSearchFilterFn(filter)
        }).pipe(
          map(res => {
            return {
              referentials: res.data,
              referentialsCount: res.total
            };
          })
      );
    }

    else {
      const query = (!opts || opts.withCount !== false) ? LoadAllWithCountQuery : LoadAllQuery;
      $loadResult = this.graphql.watchQuery<{ referentials?: any[]; referentialsCount?: number }>({
        query,
        variables: variables,
        error: {code: ErrorCodes.LOAD_REFERENTIAL_ERROR, message: "REFERENTIAL.ERROR.LOAD_REFERENTIAL_ERROR"},
        fetchPolicy: opts && opts.fetchPolicy || "cache-first"
      });
    }

    return $loadResult
      .pipe(
        map(({referentials, referentialsCount}) => {
          const data = (referentials || []).map(ReferentialRef.fromObject);
          if (now) {
            console.debug(`[referential-ref-service] References on ${entityName} loaded in ${Date.now() - now}ms`);
            now = undefined;
          }
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

    const now = debug && Date.now();
    if (debug) console.debug(`[referential-ref-service] Loading ${entityName} items...`, variables);

    // Offline mode: read from the entities storage
    let loadResult: { referentials: any[]; referentialsCount: number };
    const offline = this.network.offline && (!opts || opts.fetchPolicy !== 'network-only');
    if (offline) {
      loadResult = await this.entities.loadAll(filter.entityName + 'VO',
        {
          ...variables,
          filter: this.createSearchFilterFn(filter)
        }
      ).then(res => {
        return {
          referentials: res && res.data,
          referentialsCount: res && res.total
        };
      });
    }

    // Online mode: use graphQL
    else {
      const query = (!opts || opts.withCount !== false) ? LoadAllWithCountQuery : LoadAllQuery;
      loadResult = await this.graphql.query<{ referentials: any[]; referentialsCount: number }>({
        query,
        variables,
        error: {code: ErrorCodes.LOAD_REFERENTIAL_ERROR, message: "REFERENTIAL.ERROR.LOAD_REFERENTIAL_ERROR"},
        fetchPolicy: opts && opts.fetchPolicy || 'cache-first'
      });
    }

    const data = (!opts || opts.transformToEntity !== false) ?
      (loadResult && loadResult.referentials || []).map(ReferentialRef.fromObject) :
      (loadResult && loadResult.referentials || []) as ReferentialRef[];
    if (debug) console.debug(`[referential-ref-service] ${entityName} items loaded in ${Date.now() - now}ms`);
    return {
      data: data,
      total: loadResult.referentialsCount
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
      { ...opts, searchText: value}, {
        withCount: false // not need
      });
    return res.data;
  }

  async loadAllTaxonNames(offset: number,
                          size: number,
                          sortBy?: string,
                          sortDirection?: string,
                          filter?: Partial<TaxonNameRefFilter>,
                          opts?: {
                            [key: string]: any;
                            fetchPolicy?: FetchPolicy;
                            debug?: boolean;
                            toEntity?: boolean;
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
        levelIds: filter.levelIds || (isNotNil(filter.levelId) && [filter.levelId]) || [TaxonomicLevelIds.SPECIES, TaxonomicLevelIds.SUBSPECIES],
        statusIds: filter.statusIds || (isNotNil(filter.statusId) && [filter.statusId]) || [StatusIds.ENABLE],
        taxonGroupIds: isNotNil(filter.taxonGroupId) ? [filter.taxonGroupId] : (filter.taxonGroupIds || undefined)
      }
    };

    const debug = this._debug && (!opts || opts.debug !== false);
    const now = debug && Date.now();
    if (debug) console.debug(`[referential-ref-service] Loading TaxonName items...`, variables);

    let taxonNames: any[];

    // Offline mode
    const offline = this.network.offline && (!opts || opts.fetchPolicy !== 'network-only');
    if (offline) {
      const res = await this.entities.loadAll('TaxonNameVO', {
        ...variables,
        filter: this.createSearchTaxonNameRefFilterFn(filter)
      });
      taxonNames = res && res.data;
    }

    // Online mode
    else {
      const res = await this.graphql.query<{ taxonNames: any[]}>({
        query: LoadAllTaxonNamesQuery,
        variables: variables,
        error: {code: ErrorCodes.LOAD_REFERENTIAL_ERROR, message: "REFERENTIAL.ERROR.LOAD_REFERENTIAL_ERROR"},
        fetchPolicy: opts && opts.fetchPolicy || "cache-first"
      });
      taxonNames = res && res.taxonNames;
    }

    const data = (!opts || opts.toEntity !== false) ?
      (taxonNames || []).map(TaxonNameRef.fromObject) :
      (taxonNames || []) as TaxonNameRef[];
    if (debug) console.debug(`[referential-ref-service] TaxonName items loaded in ${Date.now() - now}ms`, data);
    return data;
  }

  async suggestTaxonNames(value: any, options: {
    levelId?: number;
    levelIds?: number[];
    searchAttribute?: string;
    taxonGroupId?: number;
  }): Promise<TaxonNameRef[]> {
    if (EntityUtils.isNotEmpty(value)) return [value];
    value = (typeof value === "string" && value !== '*') && value || undefined;
    return await this.loadAllTaxonNames(0, !value ? 30 : 10, undefined, undefined,
      {
        ...options,
        searchText: value as string
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

    return progress.asObservable();
  }

  /* -- protected methods -- */

  protected async executeImportWithProgress(progression: BehaviorSubject<number>,
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
    if (this._debug) {
      console.info(`[referential-ref-service] Starting importation of ${entityNames.length} referential... (progressionStep=${progressionStep}, stepCount=${stepCount}, maxProgression=${maxProgression}`);
    }
    else {
      console.info(`[referential-ref-service] Starting importation of ${entityNames.length} referential...`);
    }

    const importedEntities = [];
    const jobs = entityNames.map(entityName => {
      let filter: ReferentialFilter;
      let promise: Promise<LoadResult<any>>;
      const logPrefix = this._debug && `[referential-ref-service] [${entityName}]`;
      switch (entityName) {
        case 'TaxonName':
          promise = fetchAllPagesWithProgress<any>((offset, size) =>
              this.loadAllTaxonNames(offset, size, 'id', null,  {
                statusIds: [StatusIds.ENABLE],
                levelIds: [TaxonomicLevelIds.SPECIES, TaxonomicLevelIds.SUBSPECIES]
              }, {
                fetchPolicy: 'network-only',
                debug: false,
                toEntity: false
              }).then(data => {
                return {data};
              }),
            progression,
            progressionStep,
          null,
            logPrefix);
          break;
        case 'Metier':
          filter = {entityName, statusIds, searchJoin: 'TaxonGroup' };
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
              fetchPolicy: 'network-only',
              withTotal: (offset === 0), // Compute total only once
              toEntity: false
            }),
          progression,
          progressionStep,
          null,
          logPrefix);
      }
      return promise
        .then((res) => {
          importedEntities.push(entityName);
          return this.entities.saveAll(res.data, {
            entityName: entityName + 'VO'
          });
        })
        .catch(err => {
          const detailMessage = err && err.details && (err.details.message || err.details) || undefined;
          console.error(`[referential-ref-service] Failed to import ${entityName}: ${detailMessage || err && err.message || err}`);
          throw err;
        });
    });

    // Import by filter
    await Promise.all(jobs);

    // Not all entity imported: error
    if (importedEntities.length < entityNames.length) {
      console.error(`[referential-ref-service] Importation failed in ${Date.now() - now}ms`);
      progression.error({code: ErrorCodes.IMPORT_REFERENTIAL_ERROR, message: 'ERROR.IMPORT_REFERENTIAL_ERROR'});
    }
    else {
      // Success
      console.info(`[referential-ref-service] Successfully import ${entityNames.length} entities in ${Date.now() - now}ms`);
      this._importedEntities = importedEntities;
    }

    // Fill the progression to max
    progression.next(maxProgression);
  }

  protected createSearchFilterFn<T extends Referential>(f: Partial<ReferentialRefFilter>): (T) => boolean {

    const filterFns: ((T) => boolean)[] = [];

    // Filter by levels ids
    const levelIds = f.levelIds || (isNotNil(f.levelId) && [f.levelId]) || undefined;
    if (levelIds) {
      filterFns.push((entity: T) => !!levelIds.find(v => entity.levelId === v));
    }

    // Filter by status
    const statusIds = f.statusIds || (isNotNil(f.statusId) && [f.statusId]) || undefined;
    if (statusIds) {
      filterFns.push((entity: T) => !!statusIds.find(v => entity.statusId === v));
    }

    const searchTextFilter = EntityUtils.searchTextFilter(f.searchAttribute || f.searchAttributes, f.searchText);
    if (searchTextFilter) filterFns.push(searchTextFilter);

    if (!filterFns.length) return undefined;

    return (entity) => {
      return !filterFns.find(fn => !fn(entity));
    };
  }

  protected createSearchTaxonNameRefFilterFn(f: TaxonNameRefFilter): (TaxonNameRef) => boolean {

    const filterFns: ((TaxonNameRef) => boolean)[] = [];

    // Filter by taxon group id, or list of id
    if (isNotNil(f.taxonGroupId)) {
      filterFns.push((entity: TaxonNameRef) =>  {
        const res = entity.taxonGroupIds && entity.taxonGroupIds.indexOf(f.taxonGroupId) !== -1;
        console.debug(`TODO TaxonName offline filter, by {taxonGroupId: ${f.taxonGroupId} => ${entity.label}:${res}`);
        return res;
      });
    }
    else if (isNotEmptyArray(f.taxonGroupIds)) {
      filterFns.push((entity: TaxonNameRef) => {
        const res = f.taxonGroupIds.findIndex(filterTgId =>
          entity.taxonGroupIds && entity.taxonGroupIds.indexOf(filterTgId) !== -1) !== -1;
        console.debug(`TODO TaxonName offline filter, by {taxonGroupIds: ${f.taxonGroupIds.join(',')} => ${entity.label}:${res}`);
        return res;
      });
    }

    const baseSearchFilter = this.createSearchFilterFn(f);
    if (baseSearchFilter) filterFns.push(baseSearchFilter);

    if (!filterFns.length) return undefined;

    return (entity: TaxonNameRef) => {
      return !filterFns.find(fn => !fn(entity));
    };
  }
}
