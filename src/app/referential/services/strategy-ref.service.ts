import {Injectable} from "@angular/core";
import {FetchPolicy, gql} from "@apollo/client/core";
import {ReferentialFragments} from "./referential.fragments";
import {GraphqlService} from "../../core/graphql/graphql.service";
import {CacheService} from "ionic-cache";
import {ErrorCodes} from "./errors";
import {AccountService} from "../../core/services/account.service";
import {NetworkService} from "../../core/services/network.service";
import {EntitiesStorage} from "../../core/services/storage/entities-storage.service";
import {ReferentialFilter} from "./referential.service";
import {Strategy} from "./model/strategy.model";
import {
  BaseReferentialEntitiesQueries,
  BaseReferentialEntityMutations,
  BaseReferentialEntityQueries,
  BaseReferentialService,
  BaseReferentialSubscriptions
} from "./base-referential.service";
import {PlatformService} from "../../core/services/platform.service";
import {EntityUtils} from "../../core/services/model/entity.model";
import {SortDirection} from "@angular/material/sort";
import {ReferentialRefFilter} from "./referential-ref.service";
import {Referential, ReferentialRef, ReferentialUtils} from "../../core/services/model/referential.model";
import {StrategyFragments} from "./strategy.fragments";
import {firstArrayValue, isNilOrBlank, isNotNil, toNumber} from "../../shared/functions";
import {LoadResult} from "../../shared/services/entity-service.class";
import {defer, Observable} from "rxjs";
import {Program} from "./model/program.model";
import {filter, map} from "rxjs/operators";


export class StrategyFilter extends ReferentialFilter {
  //entityName: 'Strategy';
}


const StrategyRefQueries: BaseReferentialEntityQueries & BaseReferentialEntitiesQueries = {
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
  ${StrategyFragments.pmfmStrategyRef}
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
  ${StrategyFragments.pmfmStrategyRef}
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
  ${StrategyFragments.pmfmStrategyRef}
  ${StrategyFragments.taxonGroupStrategy}
  ${StrategyFragments.taxonNameStrategy}
  ${ReferentialFragments.referential}
  ${ReferentialFragments.taxonName}
  `
};


const StrategyCacheKeys = {
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
      StrategyRefQueries,
      null, // no mutations
      null, // no subscription
      StrategyFilter.asPodObject, StrategyFilter.searchFilter);
  }

  /**
   *
   * @param label
   * @param opts
   */
  async loadByLabel(label: string, opts?: {
    programId?: number;
    fetchPolicy?: FetchPolicy;
  }): Promise<Strategy> {

    const res = await this.loadAll(0, 1, null, null, {
      label,
      levelId: toNumber(opts && opts.programId, undefined)
    }, {
      ...opts
    });
    return res && firstArrayValue(res.data);
  }

  /**
   * Watch strategy by label
   * @param label
   * @param toEntity
   */
  watchByLabel(label: string, opts?: {
    toEntity?: boolean;
    debug?: boolean;
    query?: any;
  }): Observable<Strategy> {

    let now;
    const cacheKey = [StrategyCacheKeys.STRATEGY_BY_LABEL, label].join('|');
    return this.cache.loadFromObservable(cacheKey,
      defer(() => {
        const debug = this._debug && (!opts || opts !== false);
        now = debug && Date.now();
        if (now) console.debug(`[strategy-ref-service] Loading strategy {${label}}...`);
        let res: Observable<{ data: any }>;

        if (this.network.offline) {
          res = this.entities.watchAll<Strategy>(Strategy.TYPENAME, {
            filter: (p) => p.label ===  label
          })
            .pipe(
              map(res => {
                const uniqueValue = res && res.data && res.data.length && res.data[0] || undefined;
                return {data: uniqueValue};
              })
            );
        }
        else {
          const query = opts && opts.query || StrategyRefQueries.load;
          res = this.graphql.watchQuery<{ data: any }>({
            query: query,
            variables: {
              label
            },
            error: {code: ErrorCodes.LOAD_STRATEGY_ERROR, message: "ERROR.LOAD_ERROR"}
          });
        }
        return res.pipe(filter(isNotNil));
      }),
      StrategyCacheKeys.CACHE_GROUP
    )
      .pipe(
        map(({data}) => {
          const entity = (!opts || opts.toEntity !== false) ? Strategy.fromObject(data) : data;
          if (now) {
            console.debug(`[strategy-service] Loading strategy {${label}} [OK] in ${Date.now() - now}ms`);
            now = undefined;
          }
          return entity;
        })
      );
  }

  async suggest(value: any, filter?: StrategyFilter): Promise<Strategy[]> {
    if (ReferentialUtils.isNotEmpty(value)) return [value];
    value = (typeof value === "string" && value !== '*') && value || undefined;
    const res = await this.loadAll(0, !value ? 30 : 10, undefined, undefined,
      {
        ...filter,
        searchText: value as string
      }
    );
    return res.data;
  }
}
