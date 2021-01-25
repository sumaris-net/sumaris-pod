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
import {EntityServiceLoadOptions, LoadResult} from "../../shared/services/entity-service.class";


export class StrategyFilter extends ReferentialFilter {
  entityName: 'Strategy';
}

const FindStrategyNextLabel: any = gql`
  query StrategyNextLabelQuery($programId: Int!, $labelPrefix: String, $nbDigit: Int){
    strategyNextLabel(programId: $programId, labelPrefix: $labelPrefix, nbDigit: $nbDigit)
  }
`;

const LoadAllAnalyticReferencesQuery: any = gql`
  query AnalyticReferencesQuery($offset: Int, $size: Int, $sortBy: String, $sortDirection: String, $filter: ReferentialFilterVOInput){
    analyticReferences(offset: $offset, size: $size, sortBy: $sortBy, sortDirection: $sortDirection, filter: $filter){
      ...ReferentialFragment
    }
  }
  ${ReferentialFragments.referential}
`;

const StrategyQueries: BaseReferentialEntityQueries & BaseReferentialEntitiesQueries & { count: any; loadAllRef: any; } = {
  load: gql`query Strategy($id: Int!) {
    data: strategy(id: $id) {
      ...StrategyFragment
    }
  }
  ${StrategyFragments.strategy}
  ${StrategyFragments.appliedStrategy}
  ${StrategyFragments.appliedPeriod}
  ${StrategyFragments.strategyDepartment}
  ${StrategyFragments.pmfmStrategy}
  ${StrategyFragments.taxonGroupStrategy}
  ${StrategyFragments.taxonNameStrategy}
  ${ReferentialFragments.referential}
  ${ReferentialFragments.lightPmfm}
  ${ReferentialFragments.taxonName}
  `,
  loadAll: gql`query Strategies($filter: StrategyFilterVOInput!, $offset: Int, $size: Int, $sortBy: String, $sortDirection: String){
    data: strategies(filter: $filter, offset: $offset, size: $size, sortBy: $sortBy, sortDirection: $sortDirection){
      ...StrategyFragment
    }
  }
  ${StrategyFragments.strategy}
  ${StrategyFragments.appliedStrategy}
  ${StrategyFragments.appliedPeriod}
  ${StrategyFragments.pmfmStrategy}
  ${StrategyFragments.strategyDepartment}
  ${StrategyFragments.taxonGroupStrategy}
  ${StrategyFragments.taxonNameStrategy}
  ${ReferentialFragments.referential}
  ${ReferentialFragments.lightPmfm}
  ${ReferentialFragments.taxonName}
  `,
  loadAllWithTotal: gql`query StrategiesWithTotal($filter: StrategyFilterVOInput!, $offset: Int, $size: Int, $sortBy: String, $sortDirection: String){
    data: strategies(filter: $filter, offset: $offset, size: $size, sortBy: $sortBy, sortDirection: $sortDirection){
      ...StrategyFragment
    }
    total: strategiesCount(filter: $filter)
  }
  ${StrategyFragments.strategy}
  ${StrategyFragments.appliedStrategy}
  ${StrategyFragments.appliedPeriod}
  ${StrategyFragments.pmfmStrategy}
  ${StrategyFragments.strategyDepartment}
  ${StrategyFragments.taxonGroupStrategy}
  ${StrategyFragments.taxonNameStrategy}
  ${ReferentialFragments.referential}
  ${ReferentialFragments.lightPmfm}
  ${ReferentialFragments.taxonName}
  `,
  count: gql`query StrategyCount($filter: StrategyFilterVOInput!) {
      total: strategiesCount(filter: $filter)
    }
  `,
  loadAllRef: gql`query StrategiesRef($filter: StrategyFilterVOInput!, $offset: Int, $size: Int, $sortBy: String, $sortDirection: String) {
      data: strategies(filter: $filter) {
        ...StrategyRefFragment
        appliedStrategies {
          ...AppliedStrategyFragment
        }
        pmfmStrategies {
          ...PmfmStrategyFragment
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
    `
};

const StrategyMutations: BaseReferentialEntityMutations = {
  save: gql`mutation SaveStrategy($data:StrategyVOInput!){
    data: saveStrategy(strategy: $data){
      ...StrategyFragment
    }
  }
  ${StrategyFragments.strategy}
  ${StrategyFragments.appliedStrategy}
  ${StrategyFragments.appliedPeriod}
  ${StrategyFragments.pmfmStrategy}
  ${StrategyFragments.strategyDepartment}
  ${StrategyFragments.taxonGroupStrategy}
  ${StrategyFragments.taxonNameStrategy}
  ${ReferentialFragments.referential}
  ${ReferentialFragments.lightPmfm}
  ${ReferentialFragments.taxonName}
  `,
  delete: gql`mutation DeleteAllStrategies($id:Int!){
    deleteStrategy(id: $id)
  }
  `,
};

const strategySubscriptions: BaseReferentialSubscriptions = {
  listenChanges: gql`subscription UpdateReferential($entityName: String!, $id: Int!, $interval: Int){
    updateReferential(entityName: $entityName, id: $id, interval: $interval) {
      ...ReferentialFragment
    }
  }
  ${ReferentialFragments.referential}
  `
};

@Injectable({providedIn: 'root'})
export class StrategyService extends BaseReferentialService<Strategy, ReferentialFilter> {

  constructor(
    graphql: GraphqlService,
    platform: PlatformService,
    protected network: NetworkService,
    protected accountService: AccountService,
    protected cache: CacheService,
    protected entities: EntitiesStorage
  ) {
    super(graphql, platform, Strategy,
      StrategyQueries,
      StrategyMutations,
      strategySubscriptions,
      ReferentialFilter.asPodObject, StrategyFilter.searchFilter);
  }

  async existLabel(label: string, programId?: number): Promise<boolean> {
    if (isNilOrBlank(label)) throw new Error("Missing argument 'label' ");

    const filter: ReferentialFilter = {
      label,
      levelId: isNotNil(programId) ? programId : undefined
    };
    // Load from pod
    const {total} = await this.graphql.query<{ total: number }>({
      query: StrategyQueries.count,
      variables: { filter },
      error: {code: ErrorCodes.LOAD_STRATEGY_ERROR, message: "ERROR.LOAD_ERROR"},
      fetchPolicy: undefined,

    });
    return toNumber(total, 0) > 0;
  }

  /**
   *
   * @param label
   * @param options
   */
  async loadAllRef(offset: number,
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
                   }): Promise<LoadResult<Strategy>> {

    return super.loadAll(offset, size, sortBy, sortDirection, filter, {
      ...opts,
      query: StrategyQueries.loadAllRef
    });
  }

  /**
   *
   * @param label
   * @param options
   */
  async loadRefByLabel(label: string, opts?: {
    programId?: number;
    fetchPolicy?: FetchPolicy;
  }): Promise<Strategy> {

    const res = await this.loadAllRef(0, 1, null,null, {
      label,
      levelId: toNumber(opts && opts.programId, undefined)
    }, {
      ...opts
    });
    return res && firstArrayValue(res.data);
  }

  async computeNextLabel(programId: number, labelPrefix?: string, nbDigit?: number): Promise<string> {
    if (this._debug) console.debug(`[strategy-service] Loading strategy next label...`);

    const res = await this.graphql.query<{ strategyNextLabel: string }>({
      query: FindStrategyNextLabel,
      variables: {
        programId: programId,
        labelPrefix: labelPrefix,
        nbDigit: nbDigit
      },
      error: {code: ErrorCodes.LOAD_PROGRAM_ERROR, message: "PROGRAM.STRATEGY.ERROR.LOAD_STRATEGY_LABEL_ERROR"},
      fetchPolicy: 'network-only'
    });
    return res && res.strategyNextLabel;
  }

  async loadAllAnalyticReferences(
    offset: number,
    size: number,
    sortBy?: string,
    sortDirection?: SortDirection,
    filter?: ReferentialRefFilter): Promise<ReferentialRef[]> {

    const variables: any = {
      offset: offset || 0,
      size: size || 100,
      sortBy: sortBy || 'label',
      sortDirection: sortDirection || 'asc',
      filter: ReferentialFilter.asPodObject(filter)
    };

    const now = this._debug && Date.now();
    if (this._debug) console.debug(`[strategy-service] Loading analytic references...`, variables);

    const res = await this.graphql.query<{ analyticReferences: Referential[] }>({
      query: LoadAllAnalyticReferencesQuery,
      variables: variables,
      error: { code: ErrorCodes.LOAD_STRATEGY_ANALYTIC_REFERENCES_ERROR, message: "PROGRAM.STRATEGY.ERROR.LOAD_STRATEGY_ANALYTIC_REFERENCES_ERROR" },
      fetchPolicy: 'cache-first'
    });

    if (this._debug) console.debug(`[strategy-service] Analytic references loaded in ${Date.now() - now}ms`);
    return (res && res.analyticReferences || []) as ReferentialRef[];
  }

  async suggestAnalyticReferences(value: any, filter?: ReferentialRefFilter, sortBy?: keyof Referential, sortDirection?: SortDirection): Promise<ReferentialRef[]> {
    if (ReferentialUtils.isNotEmpty(value)) return [value];
    value = (typeof value === "string" && value !== '*') && value || undefined;
    return await this.loadAllAnalyticReferences(0, !value ? 30 : 10, sortBy, sortDirection,
      { ...filter, searchText: value}
    );
  }

  copyIdAndUpdateDate(source: Strategy, target: Strategy) {

    EntityUtils.copyIdAndUpdateDate(source, target);

    // Update strategies

    // Make sure tp copy programId (need by equals)
    target.programId = source.programId;

    const savedStrategy = source;
    EntityUtils.copyIdAndUpdateDate(savedStrategy, target);

    // Update pmfm strategy (id)
    if (savedStrategy.pmfmStrategies && savedStrategy.pmfmStrategies.length > 0) {

      target.pmfmStrategies.forEach(targetPmfmStrategy => {

        // Make sure to copy strategyId (need by equals)
        targetPmfmStrategy.strategyId = savedStrategy.id;

        const savedPmfmStrategy = target.pmfmStrategies.find(srcPmfmStrategy => targetPmfmStrategy.equals(srcPmfmStrategy));
        targetPmfmStrategy.id = savedPmfmStrategy.id;
      });
    }
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
