import {Injectable} from "@angular/core";
import {FetchPolicy, gql, WatchQueryFetchPolicy} from "@apollo/client/core";
import {ReferentialFragments} from "./referential.fragments";
import {GraphqlService} from "../../core/graphql/graphql.service";
import {CacheService} from "ionic-cache";
import {ErrorCodes} from "./errors";
import {AccountService} from "../../core/services/account.service";
import {NetworkService} from "../../core/services/network.service";
import {EntitiesStorage} from "../../core/services/storage/entities-storage.service";
import {ReferentialFilter} from "./referential.service";
import {Strategy} from "./model/strategy.model";
import {BaseEntityGraphqlQueries} from "./base-entity-service.class";
import {PlatformService} from "../../core/services/platform.service";
import {StrategyFragments} from "./strategy.fragments";
import {firstArrayValue, isNotNil, toNumber} from "../../shared/functions";
import {defer, Observable} from "rxjs";
import {filter, map} from "rxjs/operators";
import {BaseReferentialService} from "./base-referential-service.class";
import {firstNotNilPromise} from "../../shared/observables";


export class StrategyFilter extends ReferentialFilter {
  //entityName: 'Strategy';
}


const StrategyRefQueries: BaseEntityGraphqlQueries = {
  load: gql`query StrategyRef($id: Int!) {
    data: strategy(id: $id) {
      ...StrategyRefFragment
      appliedStrategies {
        ...AppliedStrategyFragment
      }
      departments {
        ...StrategyDepartmentFragment
      }
    }
  }
  ${StrategyFragments.strategyRef}
  ${StrategyFragments.appliedStrategy}
  ${StrategyFragments.appliedPeriod}
  ${StrategyFragments.strategyDepartment}
  ${StrategyFragments.strategyRef}
  ${StrategyFragments.denormalizedPmfmStrategy}
  ${StrategyFragments.taxonGroupStrategy}
  ${StrategyFragments.taxonNameStrategy}
  ${ReferentialFragments.referential}
  ${ReferentialFragments.taxonName}
  `,
  loadAll: gql`query StrategyRefs($filter: StrategyFilterVOInput!, $offset: Int, $size: Int, $sortBy: String, $sortDirection: String){
    data: strategies(filter: $filter, offset: $offset, size: $size, sortBy: $sortBy, sortDirection: $sortDirection){
      ...StrategyRefFragment
      appliedStrategies {
        ...AppliedStrategyFragment
      }
      departments {
        ...StrategyDepartmentFragment
      }
    }
  }
  ${StrategyFragments.strategyRef}
  ${StrategyFragments.appliedStrategy}
  ${StrategyFragments.appliedPeriod}
  ${StrategyFragments.strategyDepartment}
  ${StrategyFragments.strategyRef}
  ${StrategyFragments.denormalizedPmfmStrategy}
  ${StrategyFragments.taxonGroupStrategy}
  ${StrategyFragments.taxonNameStrategy}
  ${ReferentialFragments.referential}
  ${ReferentialFragments.taxonName}
  `,
  loadAllWithTotal: gql`query StrategyRefWithTotal($filter: StrategyFilterVOInput!, $offset: Int, $size: Int, $sortBy: String, $sortDirection: String){
    data: strategies(filter: $filter, offset: $offset, size: $size, sortBy: $sortBy, sortDirection: $sortDirection){
      ...StrategyRefFragment
      appliedStrategies {
        ...AppliedStrategyFragment
      }
      departments {
        ...StrategyDepartmentFragment
      }
    }
    total: strategiesCount(filter: $filter)
  }
  ${StrategyFragments.strategyRef}
  ${StrategyFragments.appliedStrategy}
  ${StrategyFragments.appliedPeriod}
  ${StrategyFragments.strategyDepartment}
  ${StrategyFragments.strategyRef}
  ${StrategyFragments.denormalizedPmfmStrategy}
  ${StrategyFragments.taxonGroupStrategy}
  ${StrategyFragments.taxonNameStrategy}
  ${ReferentialFragments.referential}
  ${ReferentialFragments.taxonName}
  `
};


const StrategyRefCacheKeys = {
  CACHE_GROUP: 'strategy',

  STRATEGY_BY_LABEL: 'strategyByLabel'
};


@Injectable({providedIn: 'root'})
export class StrategyRefService extends BaseReferentialService<Strategy, StrategyFilter> {

  constructor(
    graphql: GraphqlService,
    platform: PlatformService,
    protected network: NetworkService,
    protected accountService: AccountService,
    protected cache: CacheService,
    protected entities: EntitiesStorage
  ) {
    super(graphql, platform, Strategy,
      {
        queries: StrategyRefQueries,
        filterAsObjectFn: StrategyFilter.asPodObject,
        filterFnFactory: StrategyFilter.searchFilter
      });
  }

  /**
   * Watch strategy by label
   * @param label
   * @param opts
   */
  watchByLabel(label: string, opts?: {
    programId?: number;
    toEntity?: boolean;
    debug?: boolean;
    cache?: boolean;
    fetchPolicy?: WatchQueryFetchPolicy;
  }): Observable<Strategy> {

    if (!opts || opts.cache !== false) {
      const cacheKey = [StrategyRefCacheKeys.STRATEGY_BY_LABEL, label, opts && opts.programId].join('|');
      return this.cache.loadFromObservable(cacheKey,
        defer(() => this.watchByLabel(label, {...opts, toEntity: false, cache: false})),
        StrategyRefCacheKeys.CACHE_GROUP)
        .pipe(
          map(data => (!opts || opts.toEntity !== false) ? Strategy.fromObject(data) : data)
        );
    }

    let now;
    const debug = this._debug && (!opts || opts !== false);
    now = debug && Date.now();
    if (now) console.debug(`[strategy-ref-service] Watching strategy {${label}}...`);
    let res: Observable<any>;

    if (this.network.offline) {
      res = this.entities.watchAll<Strategy>(Strategy.TYPENAME, {
        offset: 0,
        size: 1,
        filter: (p) => p.label ===  label && (!opts.programId || p.programId === opts.programId)
      })
        .pipe(
          map(res => firstArrayValue(res && res.data))
        );
    }
    else {
      res = this.graphql.watchQuery<{data: any[]}>({
        query: this.queries.loadAll,
        variables: {
          offset: 0, size: 1,
          filter: {
            label,
            levelId: toNumber(opts && opts.programId, undefined)
          }
        },
        // Important: do NOT using cache here, as default (= 'no-cache')
        // because cache is manage by Ionic cache (easier to clean)
        fetchPolicy: opts && opts.fetchPolicy || 'no-cache',
        error: {code: ErrorCodes.LOAD_STRATEGY_ERROR, message: "ERROR.LOAD_ERROR"}
      }).pipe(map(res => firstArrayValue(res && res.data)));
    }

    return res.pipe(
      filter(isNotNil),
      map(data => {
        const entity = (!opts || opts.toEntity !== false) ? Strategy.fromObject(data) : data;
        if (now) {
          console.debug(`[strategy-service] Watching strategy {${label}} [OK] in ${Date.now() - now}ms`);
          now = undefined;
        }
        return entity;
      })
    );
  }

  /**
   *
   * @param label
   * @param opts
   */
  async loadByLabel(label: string, opts?: {
    programId?: number;
    fetchPolicy?: FetchPolicy;
    toEntity?: boolean;
  }): Promise<Strategy> {
    return firstNotNilPromise(this.watchByLabel(label, opts));
  }

  async clearCache() {
    console.info("[strategy-ref-service] Clearing strategy cache...");
    await this.cache.clearGroup(StrategyRefCacheKeys.CACHE_GROUP);
  }
}
