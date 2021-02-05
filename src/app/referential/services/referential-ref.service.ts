import {Injectable} from "@angular/core";
import {FetchPolicy, gql} from "@apollo/client/core";
import {BehaviorSubject, Observable} from "rxjs";
import {map} from "rxjs/operators";
import {ErrorCodes} from "./errors";
import {AccountService} from "../../core/services/account.service";
import {Referential, ReferentialRef, ReferentialUtils} from "../../core/services/model/referential.model";
import {ReferentialFilter, ReferentialService} from "./referential.service";
import {FilterFn, IEntitiesService, LoadResult, SuggestService} from "../../shared/services/entity-service.class";
import {GraphqlService} from "../../core/graphql/graphql.service";
import {LocationLevelIds, TaxonGroupIds, TaxonomicLevelIds} from "./model/model.enum";
import {TaxonNameRef} from "./model/taxon.model";
import {NetworkService} from "../../core/services/network.service";
import {EntitiesStorage} from "../../core/services/storage/entities-storage.service";
import {ReferentialFragments} from "./referential.fragments";
import {SortDirection} from "@angular/material/sort";
import {Moment} from "moment";
import {isEmptyArray, isNotEmptyArray, isNotNil} from "../../shared/functions";
import {JobUtils} from "../../shared/services/job.utils";
import {chainPromises} from "../../shared/observables";
import {BaseGraphqlService} from "../../core/services/base-graphql-service.class";
import {StatusIds} from "../../core/services/model/model.enum";
import {environment} from "../../../environments/environment";
import {fromDateISOString} from "../../shared/dates";
import {ObjectMap} from "../../shared/types";
import {BaseEntityGraphqlQueries} from "./base-entity-service.class";

export class ReferentialRefFilter extends ReferentialFilter {
  searchAttributes?: string[];
}

export class TaxonNameRefFilter extends ReferentialRefFilter {

  taxonGroupId?: number;
  taxonGroupIds?: number[];

  static searchFilter(f: TaxonNameRefFilter): FilterFn<TaxonNameRef> {

    const filterFns: FilterFn<TaxonNameRef>[] = [];

    // Filter by taxon group id, or list of id
    if (isNotNil(f.taxonGroupId)) {
      filterFns.push((entity: TaxonNameRef) =>  {
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

    // Base referential filter fn
    const baseSearchFilter = ReferentialRefFilter.searchFilter(f);
    if (baseSearchFilter) filterFns.push(baseSearchFilter);

    if (!filterFns.length) return undefined;

    return (entity: TaxonNameRef) => !filterFns.find(fn => !fn(entity));
  }
}

const LastUpdateDate: any = gql`
  query LastUpdateDate{
    lastUpdateDate
  }
`;

const LoadAllQuery: any = gql`
  query ReferentialRefs($entityName: String, $offset: Int, $size: Int, $sortBy: String, $sortDirection: String, $filter: ReferentialFilterVOInput){
    data: referentials(entityName: $entityName, offset: $offset, size: $size, sortBy: $sortBy, sortDirection: $sortDirection, filter: $filter){
      ...ReferentialFragment
    }
  }
  ${ReferentialFragments.referential}
`;

const LoadAllWithTotalQuery: any = gql`
  query ReferentialRefsWithTotal($entityName: String, $offset: Int, $size: Int, $sortBy: String, $sortDirection: String, $filter: ReferentialFilterVOInput){
    data: referentials(entityName: $entityName, offset: $offset, size: $size, sortBy: $sortBy, sortDirection: $sortDirection, filter: $filter){
      ...ReferentialFragment
    }
    total: referentialsCount(entityName: $entityName, filter: $filter)
  }
  ${ReferentialFragments.referential}
`;

const LoadAllTaxonNamesQuery: any = gql`
  query TaxonNames($offset: Int, $size: Int, $sortBy: String, $sortDirection: String, $filter: TaxonNameFilterVOInput){
    data: taxonNames(offset: $offset, size: $size, sortBy: $sortBy, sortDirection: $sortDirection, filter: $filter){
      ...FullTaxonNameFragment
    }
  }
  ${ReferentialFragments.fullTaxonName}
`;

export const ReferentialRefQueries: BaseEntityGraphqlQueries = {
  loadAll: LoadAllQuery,
  loadAllWithTotal: LoadAllWithTotalQuery,
};

@Injectable({providedIn: 'root'})
export class ReferentialRefService extends BaseGraphqlService<ReferentialRef, ReferentialRefFilter>
  implements SuggestService<ReferentialRef, ReferentialRefFilter>,
      IEntitiesService<ReferentialRef, ReferentialRefFilter> {

  private _importedEntities: string[];

  constructor(
    protected graphql: GraphqlService,
    protected referentialService: ReferentialService,
    protected accountService: AccountService,
    protected network: NetworkService,
    protected entities: EntitiesStorage
  ) {
    super(graphql, environment);
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
           sortDirection?: SortDirection,
           filter?: ReferentialRefFilter,
           opts?: {
             [key: string]: any;
             fetchPolicy?: FetchPolicy;
             withTotal?: boolean;
             toEntity?: boolean;
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
      sortBy: sortBy || filter.searchAttribute || 'label',
      sortDirection: sortDirection || 'asc'
    };

    let now = this._debug && Date.now();
    if (this._debug) console.debug(`[referential-ref-service] Watching ${entityName} items...`, variables);
    let res: Observable<LoadResult<any>>;

    if (this.network.offline) {
      res = this.entities.watchAll(entityName,
        {
          ...variables,
          filter: ReferentialRefFilter.searchFilter(filter)
        });
    }

    else {
      const query = (!opts || opts.withTotal !== false) ? LoadAllWithTotalQuery : LoadAllQuery;
      res = this.graphql.watchQuery<LoadResult<any>>({
        query,
        variables: {
          ...variables,
          filter: ReferentialRefFilter.asPodObject(filter)
        },
        error: {code: ErrorCodes.LOAD_REFERENTIAL_ERROR, message: "REFERENTIAL.ERROR.LOAD_REFERENTIAL_ERROR"},
        fetchPolicy: opts && opts.fetchPolicy || "cache-first"
      });
    }

    return res
      .pipe(
        map(({data, total}) => {
          const entities = (!opts || opts.toEntity !== false)
            ? (data || []).map(ReferentialRef.fromObject)
            : (data || []) as ReferentialRef[];
          if (now) {
            console.debug(`[referential-ref-service] References on ${entityName} loaded in ${Date.now() - now}ms`);
            now = undefined;
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
                filter?: ReferentialRefFilter,
                opts?: {
                  [key: string]: any;
                  fetchPolicy?: FetchPolicy;
                  debug?: boolean;
                  withTotal?: boolean;
                  toEntity?: boolean;
                }): Promise<LoadResult<ReferentialRef>> {


    const offline = this.network.offline && (!opts || opts.fetchPolicy !== 'network-only');
    if (offline) {
      return this.loadAllLocally(offset, size, sortBy, sortDirection, filter, opts);
    }

    const entityName = filter && filter.entityName;
    if (!entityName) {
      console.error("[referential-ref-service] Missing filter.entityName");
      throw {code: ErrorCodes.LOAD_REFERENTIAL_ERROR, message: "REFERENTIAL.ERROR.LOAD_REFERENTIAL_ERROR"};
    }
    const uniqueEntityName = filter.entityName + (filter.searchJoin || '');

    const debug = this._debug && (!opts || opts.debug !== false);

    const variables: any = {
      entityName: entityName,
      offset: offset || 0,
      size: size || 100,
      sortBy: sortBy || filter.searchAttribute
        || filter.searchAttributes && filter.searchAttributes.length && filter.searchAttributes[0]
        || 'label',
      sortDirection: sortDirection || 'asc',
      filter: ReferentialRefFilter.asPodObject(filter)
    };
    const now = debug && Date.now();
    if (debug) console.debug(`[referential-ref-service] Loading ${uniqueEntityName} references...`, variables);


    // Online mode: use graphQL
    const query = (!opts || opts.withTotal !== false) ? LoadAllWithTotalQuery : LoadAllQuery;
    const { data, total } = await this.graphql.query<LoadResult<any>>({
      query,
      variables,
      error: {code: ErrorCodes.LOAD_REFERENTIAL_ERROR, message: "REFERENTIAL.ERROR.LOAD_REFERENTIAL_ERROR"},
      fetchPolicy: opts && opts.fetchPolicy || 'cache-first'
    });

    const entities = (!opts || opts.toEntity !== false) ?
      (data || []).map(ReferentialRef.fromObject) :
      (data || []) as ReferentialRef[];

    // Force entity name (if searchJoin)
    if (filter.entityName !== uniqueEntityName) {
      entities.forEach(item => item.entityName = uniqueEntityName);
    }

    if (debug) console.debug(`[referential-ref-service] Loading ${uniqueEntityName} references [OK] ${entities.length} items, in ${Date.now() - now}ms`);
    return {
      data: entities,
      total
    };
  }

  async loadAllLocally(offset: number,
                size: number,
                sortBy?: string,
                sortDirection?: SortDirection,
                filter?: ReferentialRefFilter,
                opts?: {
                  [key: string]: any;
                  toEntity?: boolean;
                }): Promise<LoadResult<ReferentialRef>> {

    const entityName = filter && filter.entityName;
    if (!entityName) {
      console.error("[referential-ref-service] Missing filter.entityName");
      throw {code: ErrorCodes.LOAD_REFERENTIAL_ERROR, message: "REFERENTIAL.ERROR.LOAD_REFERENTIAL_ERROR"};
    }
    const uniqueEntityName = filter.entityName + (filter.searchJoin || '');

    const variables: any = {
      entityName: entityName,
      offset: offset || 0,
      size: size || 100,
      sortBy: sortBy || filter.searchAttribute
        || filter.searchAttributes && filter.searchAttributes.length && filter.searchAttributes[0]
        || 'label',
      sortDirection: sortDirection || 'asc',
      filter: ReferentialRefFilter.searchFilter(filter)
    };

    const {data, total} = await this.entities.loadAll(uniqueEntityName + 'VO', variables);

    const entities = (!opts || opts.toEntity !== false) ?
      (data || []).map(ReferentialRef.fromObject) :
      (data || []) as ReferentialRef[];

    // Force entity name (if searchJoin)
    if (filter.entityName !== uniqueEntityName) {
      entities.forEach(item => item.entityName = uniqueEntityName);
    }
    return {
      data: entities,
      total: total || entities.length
    };
  }

  async suggest(value: any, filter?: ReferentialRefFilter, sortBy?: keyof Referential, sortDirection?: SortDirection): Promise<ReferentialRef[]> {
    if (ReferentialUtils.isNotEmpty(value)) return [value];
    value = (typeof value === "string" && value !== '*') && value || undefined;
    const res = await this.loadAll(0, !value ? 30 : 10, sortBy, sortDirection,
      { ...filter, searchText: value},
      { withTotal: false /* total not need */ }
    );
    return res.data;
  }

  async loadAllTaxonNames(offset: number,
                          size: number,
                          sortBy?: string,
                          sortDirection?: SortDirection,
                          filter?: TaxonNameRefFilter,
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
        levelIds: filter.levelIds || (isNotNil(filter.levelId) && [filter.levelId]) || [TaxonomicLevelIds.SPECIES, TaxonomicLevelIds.SUBSPECIES],
        statusIds: filter.statusIds || (isNotNil(filter.statusId) && [filter.statusId]) || [StatusIds.ENABLE],
        taxonGroupIds: isNotNil(filter.taxonGroupId) ? [filter.taxonGroupId] : (filter.taxonGroupIds || undefined)
      }
    };

    const debug = this._debug && (!opts || opts.debug !== false);
    const now = debug && Date.now();
    if (debug) console.debug(`[referential-ref-service] Loading TaxonName items...`, variables);

    let res: LoadResult<any>;

    // Offline mode
    const offline = this.network.offline && (!opts || opts.fetchPolicy !== 'network-only');
    if (offline) {
      res = await this.entities.loadAll('TaxonNameVO', {
        ...variables,
        filter: TaxonNameRefFilter.searchFilter(filter)
      });
    }

    // Online mode
    else {
      res = await this.graphql.query<LoadResult<any>>({
        query: LoadAllTaxonNamesQuery,
        variables,
        error: {code: ErrorCodes.LOAD_REFERENTIAL_ERROR, message: "REFERENTIAL.ERROR.LOAD_REFERENTIAL_ERROR"},
        fetchPolicy: opts && opts.fetchPolicy || "cache-first"
      });
    }

    const entities = (!opts || opts.toEntity !== false) ?
      (res && res.data || []).map(TaxonNameRef.fromObject) :
      (res && res.data || []) as TaxonNameRef[];
    if (debug) console.debug(`[referential-ref-service] TaxonName items loaded in ${Date.now() - now}ms`, entities);
    return entities;
  }

  async suggestTaxonNames(value: any, options: {
    levelId?: number;
    levelIds?: number[];
    searchAttribute?: string;
    taxonGroupId?: number;
  }): Promise<TaxonNameRef[]> {
    if (ReferentialUtils.isNotEmpty(value)) return [value];
    value = (typeof value === "string" && value !== '*') && value || undefined;
    return await this.loadAllTaxonNames(0, !value ? 30 : 10, undefined, undefined,
      {
        entityName: 'TaxonName',
        ...options,
        searchText: value as string
      });
  }

  saveAll(data: ReferentialRef[], options?: any): Promise<ReferentialRef[]> {
    throw new Error('Not implemented yet');
  }

  deleteAll(data: ReferentialRef[], options?: any): Promise<any> {
    throw new Error('Not implemented yet');
  }

  async lastUpdateDate(opts?: {fetchPolicy?: FetchPolicy}): Promise<Moment> {
    try {
      const {lastUpdateDate} = await this.graphql.query<{lastUpdateDate: string}>({
        query: LastUpdateDate,
        variables: {},
        fetchPolicy: opts && opts.fetchPolicy || 'network-only'
      });

      return fromDateISOString(lastUpdateDate);
    }
    catch (err) {
      console.error('[referential-ref] Cannot get remote lastUpdateDate: ' + (err && err.message || err), err);
      return undefined;
    }
  }

  /**
   * Get referential references, group by level
   * @param filter
   * @param groupBy
   * @param opts
   */
  async loadAllGroupByLevels(filter: ReferentialFilter,
                             groupBy: {
                               levelIds?: ObjectMap<number[]>
                               levelLabels?: ObjectMap<string[]>,
                             },
                             opts?: {
                               [key: string]: any;
                               fetchPolicy?: FetchPolicy;
                               debug?: boolean;
                               withTotal?: boolean;
                               toEntity?: boolean;
                             }): Promise<{[key: string]: ReferentialRef[]}> {
    const entityName = filter && filter.entityName;
    const groupKeys = Object.keys(groupBy.levelIds || groupBy.levelLabels); // AGE, SEX, MATURITY, etc

    // Check arguments
    if (!entityName) throw new Error("Missing 'filter.entityName' argument");
    if (isEmptyArray(groupKeys)) throw new Error("Missing 'levelLabelsMap' argument");
    if ((groupBy.levelIds && groupBy.levelLabels) || (!groupBy.levelIds && !groupBy.levelLabels)) {
      throw new Error("Invalid groupBy value: one (and only one) required: 'levelIds' or 'levelLabels'");
    }

    const debug = this._debug || (opts && opts.debug);
    const now = debug && Date.now();
    if (debug) console.debug(`[referential-ref-service] Loading grouped ${entityName}...`);

    const result: { [key: string]: ReferentialRef[]; } = {};
    await Promise.all(groupKeys.map(key => this.loadAll(0, 1000, 'id', 'asc', {
        ...filter,
        levelIds: groupBy.levelIds && groupBy.levelIds[key],
        levelLabels: groupBy.levelLabels && groupBy.levelLabels[key]
      }, {
        withTotal: false,
        ...opts
      })
      .then(({data}) => {
        result[key] = data || [];
      })
    ));

    if (debug) console.debug(`[referential-ref-service] Grouped ${entityName} loaded in ${Date.now() - now}ms`, result);

    return result;
  }

  async executeImport(progression: BehaviorSubject<number>,
                      opts?: {
                        maxProgression?: number;
                        entityNames?: string[],
                        statusIds?: number[];
                      }) {

    const entityNames = opts && opts.entityNames || ['Location', 'Gear', 'Metier', 'MetierTaxonGroup', 'TaxonGroup', 'TaxonName', 'Department', 'QualityFlag', 'SaleType', 'VesselType'];

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
    await chainPromises(entityNames.map(entityName =>
      () => this.executeImportEntity(progression, {
          ...opts,
          entityName,
          maxProgression: progressionStep
          })
          .then(() => importedEntities.push(entityName))
      )
    );

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
  }

  async executeImportEntity(progression: BehaviorSubject<number>,
                            opts: {
                              entityName: string;
                              maxProgression?: number;
                              statusIds?: number[];
                            }) {
    const entityName = opts && opts.entityName;
    if (!entityName) throw new Error("Missing 'opts.entityName'");

    const maxProgression = opts.maxProgression || 100;
    const logPrefix = this._debug && `[referential-ref-service] [${entityName}]`;
    const statusIds = opts && opts.statusIds || [StatusIds.ENABLE, StatusIds.TEMPORARY];

    try {
      let res: LoadResult<any>;
      let filter: ReferentialFilter;

      switch (entityName) {
        case 'TaxonName':
          res = await JobUtils.fetchAllPages<any>((offset, size) =>
              this.loadAllTaxonNames(offset, size, 'id', null, {
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
            {maxProgression, logPrefix}
            );
          break;
        case 'MetierTaxonGroup':
          filter = {entityName: 'Metier', statusIds, searchJoin: 'TaxonGroup'};
          break;
        case 'TaxonGroup':
          filter = {entityName, statusIds, levelIds: [TaxonGroupIds.FAO]};
          break;
        case 'Location':
          filter = {
            entityName, statusIds, levelIds: Object.values(LocationLevelIds)
              // Exclude rectangles (because more than 7200 rect exists !)
              // => Maybe find a way to add it, depending on the program properties ?
              .filter(id => id !== LocationLevelIds.ICES_RECTANGLE)
          };
          break;
        default:
          filter = {entityName, statusIds};
          break;
      }

      if (!res) {
        res = await JobUtils.fetchAllPages<any>((offset, size) =>
            this.referentialService.loadAll(offset, size, 'id', null, filter, {
              debug: false,
              fetchPolicy: 'network-only',
              withTotal: (offset === 0), // Compute total only once
              toEntity: false
            }),
          progression,
          {
            maxProgression,
            logPrefix
          });
      }

      // Save locally
      await this.entities.saveAll(res.data, {
          entityName: entityName + 'VO',
          reset: true
        });

    }
    catch (err) {
      const detailMessage = err && err.details && (err.details.message || err.details) || undefined;
      console.error(`[referential-ref-service] Failed to import ${entityName}: ${detailMessage || err && err.message || err}`);
      throw err;
    }
  }
}
