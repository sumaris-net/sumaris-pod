import {Injectable} from "@angular/core";
import {FetchPolicy, gql, WatchQueryFetchPolicy} from "@apollo/client/core";
import {ReferentialFragments} from "./referential.fragments";
import {BaseEntityGraphqlQueries, GraphqlService} from '@sumaris-net/ngx-components';
import {CacheService} from "ionic-cache";
import {ErrorCodes} from "./errors";
import {AccountService}  from "@sumaris-net/ngx-components";
import {NetworkService}  from "@sumaris-net/ngx-components";
import {EntitiesStorage}  from "@sumaris-net/ngx-components";
import {ReferentialFilter} from "./filter/referential.filter";
import {Strategy} from "./model/strategy.model";
import {PlatformService}  from "@sumaris-net/ngx-components";
import {StrategyFragments} from "./strategy.fragments";
import {firstArrayValue, isNil, isNotEmptyArray, isNotNil, toNumber} from "@sumaris-net/ngx-components";
import {defer, Observable, Subject, Subscription} from "rxjs";
import {filter, finalize, map, tap} from "rxjs/operators";
import {BaseReferentialService} from "./base-referential-service.class";
import {firstNotNilPromise} from "@sumaris-net/ngx-components";


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
  ${StrategyFragments.lightPmfmStrategy}
  ${ReferentialFragments.lightPmfm}
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
  ${StrategyFragments.lightPmfmStrategy}
  ${ReferentialFragments.lightPmfm}
  ${StrategyFragments.denormalizedPmfmStrategy}
  ${StrategyFragments.taxonGroupStrategy}
  ${StrategyFragments.taxonNameStrategy}
  ${ReferentialFragments.referential}
  ${ReferentialFragments.taxonName}`,

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
  ${StrategyFragments.lightPmfmStrategy}
  ${ReferentialFragments.lightPmfm}
  ${StrategyFragments.denormalizedPmfmStrategy}
  ${StrategyFragments.taxonGroupStrategy}
  ${StrategyFragments.taxonNameStrategy}
  ${ReferentialFragments.referential}
  ${ReferentialFragments.taxonName}`
};

const StrategySubscriptions = {
  listenChangesByProgram: gql`subscription UpdateProgramStrategies($programId: Int!, $interval: Int){
    data: updateProgramStrategies(programId: $programId, interval: $interval) {
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
  ${StrategyFragments.lightPmfmStrategy}
  ${ReferentialFragments.lightPmfm}
  ${StrategyFragments.denormalizedPmfmStrategy}
  ${StrategyFragments.taxonGroupStrategy}
  ${StrategyFragments.taxonNameStrategy}
  ${ReferentialFragments.referential}
  ${ReferentialFragments.taxonName}`
};

const StrategyRefCacheKeys = {
  CACHE_GROUP: 'strategy',

  STRATEGY_BY_LABEL: 'strategyByLabel',
  STRATEGIES_BY_PROGRAM_ID: 'strategiesByProgramId'
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
    super(graphql, platform, Strategy, StrategyFilter,
      {
        queries: StrategyRefQueries
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

  private _subscriptionCache: {[key: string]: {
      counter: number;
      subject: Subject<Strategy[]>;
      subscription: Subscription;
    }} = {};

  listenChangesByProgram(programId: number, opts?: {
    interval?: number
  }): Observable<Strategy[]> {
    if (isNil(programId)) throw Error("Missing argument 'programId' ");

    const cacheKey = [StrategyRefCacheKeys.STRATEGIES_BY_PROGRAM_ID, programId].join('|');
    let cache = this._subscriptionCache[cacheKey];
    if (!cache) {
      cache = {
        counter: 0,
        subject: new Subject<Strategy[]>(),
        subscription: undefined
      };
      this._subscriptionCache[cacheKey] = cache;
    }

    cache.counter++;

    const variables = {
      programId,
      interval: opts && opts.interval || 10 // seconds
    };
    if (this._debug) console.debug(`[strategy-ref-service] [WS] Listening for changes on strategies, from program {${programId}}...`);

    cache.subscription = cache.subscription || this.graphql.subscribe<{data: any}>({
      query: StrategySubscriptions.listenChangesByProgram,
      variables,
      error: {
        code: ErrorCodes.SUBSCRIBE_REFERENTIAL_ERROR,
        message: 'REFERENTIAL.ERROR.SUBSCRIBE_REFERENTIAL_ERROR'
      }
    })
      .pipe(
        map(({data}) => {
          const entities = (data || []).map(s => this.fromObject(s));
          if (isNotEmptyArray(entities) && this._debug) console.debug(`[strategy-ref-service] [WS] Received changes on strategies, from program {${programId}}`, entities);
          return entities;
        }),
        // DEBUG
        //tap(program => console.debug('[program-ref-service] Received program changes')),
        tap(program => cache.subject.next(program))
      ).subscribe();

    return cache.subject
      .pipe(
        finalize(() => {
          cache.counter--;
          if (cache.counter === 0) {
            // DEBUG
            //console.debug('[program-ref-service] Closing program changes listener'));
            cache.subscription.unsubscribe();
            cache.subscription = null;
          }
        })
      );
  }
}
