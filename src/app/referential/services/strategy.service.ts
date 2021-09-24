import {Injectable} from '@angular/core';
import {FetchPolicy, gql, StoreObject} from '@apollo/client/core';
import {ReferentialFragments} from './referential.fragments';
import {
  AccountService,
  BaseEntityGraphqlMutations,
  BaseEntityGraphqlQueries,
  BaseEntityGraphqlSubscriptions,
  EntitiesStorage,
  EntityAsObjectOptions,
  EntityUtils,
  GraphqlService,
  isNilOrBlank,
  isNotNil,
  LoadResult,
  NetworkService,
  PlatformService,
  Referential,
  ReferentialRef,
  ReferentialUtils,
  toNumber
} from '@sumaris-net/ngx-components';
import {CacheService} from 'ionic-cache';
import {ErrorCodes} from './errors';

import {Strategy} from './model/strategy.model';
import {SortDirection} from '@angular/material/sort';
import {ReferentialRefService} from './referential-ref.service';
import {StrategyFragments} from './strategy.fragments';
import {BaseReferentialService} from './base-referential-service.class';
import {Pmfm} from './model/pmfm.model';
import {ProgramRefService} from './program-ref.service';
import {StrategyRefService} from './strategy-ref.service';
import {ReferentialRefFilter} from './filter/referential-ref.filter';
import {StrategyFilter} from '@app/referential/services/filter/strategy.filter';


const FindStrategyNextLabel: any = gql`
  query StrategyNextLabelQuery($programId: Int!, $labelPrefix: String, $nbDigit: Int){
    data: strategyNextLabel(programId: $programId, labelPrefix: $labelPrefix, nbDigit: $nbDigit)
  }
`;

const FindStrategyNextSampleLabel: any = gql`
  query StrategyNextSampleLabelQuery($strategyLabel: String!, $labelSeparator: String, $nbDigit: Int){
    data: strategyNextSampleLabel(strategyLabel: $strategyLabel, labelSeparator: $labelSeparator, nbDigit: $nbDigit)
  }
`;

const LoadAllAnalyticReferencesQuery: any = gql`query AnalyticReferencesQuery($offset: Int, $size: Int, $sortBy: String, $sortDirection: String, $filter: ReferentialFilterVOInput){
    data: analyticReferences(offset: $offset, size: $size, sortBy: $sortBy, sortDirection: $sortDirection, filter: $filter){
      ...ReferentialFragment
    }
  }
  ${ReferentialFragments.referential}`;
const LoadAllAnalyticReferencesWithTotalQuery: any = gql`query AnalyticReferencesQuery($offset: Int, $size: Int, $sortBy: String, $sortDirection: String, $filter: ReferentialFilterVOInput){
  data: analyticReferences(offset: $offset, size: $size, sortBy: $sortBy, sortDirection: $sortDirection, filter: $filter){
    ...ReferentialFragment
  }
  total: analyticReferencesCount(filter: $filter)
}
${ReferentialFragments.referential}`;

const FindStrategiesReferentials: any = gql`
  query StrategiesReferentials($programId: Int!, $locationClassification: LocationClassificationEnum, $entityName: String, $offset: Int, $size: Int, $sortBy: String, $sortDirection: String){
    data: strategiesReferentials(programId: $programId, locationClassification: $locationClassification, entityName: $entityName, offset: $offset, size: $size, sortBy: $sortBy, sortDirection: $sortDirection){
      ...ReferentialFragment
    }
  }
  ${ReferentialFragments.referential}
`;

const StrategyQueries: BaseEntityGraphqlQueries & { count: any; } = {
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
  ${ReferentialFragments.pmfm}
  ${ReferentialFragments.parameter}
  ${ReferentialFragments.fullReferential}
  ${ReferentialFragments.taxonName}`,

  loadAll: gql`query Strategies($filter: StrategyFilterVOInput!, $offset: Int, $size: Int, $sortBy: String, $sortDirection: String){
    data: strategies(filter: $filter, offset: $offset, size: $size, sortBy: $sortBy, sortDirection: $sortDirection){
      ...LightStrategyFragment
    }
  }
  ${StrategyFragments.lightStrategy}
  ${StrategyFragments.appliedStrategy}
  ${StrategyFragments.appliedPeriod}
  ${StrategyFragments.lightPmfmStrategy}
  ${StrategyFragments.strategyDepartment}
  ${StrategyFragments.taxonGroupStrategy}
  ${StrategyFragments.taxonNameStrategy}
  ${ReferentialFragments.referential}
  ${ReferentialFragments.lightPmfm}
  ${ReferentialFragments.taxonName}`,

  loadAllWithTotal: gql`query StrategiesWithTotal($filter: StrategyFilterVOInput!, $offset: Int, $size: Int, $sortBy: String, $sortDirection: String){
    data: strategies(filter: $filter, offset: $offset, size: $size, sortBy: $sortBy, sortDirection: $sortDirection){
      ...LightStrategyFragment
    }
    total: strategiesCount(filter: $filter)
  }
  ${StrategyFragments.lightStrategy}
  ${StrategyFragments.appliedStrategy}
  ${StrategyFragments.appliedPeriod}
  ${StrategyFragments.lightPmfmStrategy}
  ${StrategyFragments.strategyDepartment}
  ${StrategyFragments.taxonGroupStrategy}
  ${StrategyFragments.taxonNameStrategy}
  ${ReferentialFragments.referential}
  ${ReferentialFragments.lightPmfm}
  ${ReferentialFragments.taxonName}`,

  count: gql`query StrategyCount($filter: StrategyFilterVOInput!) {
      total: strategiesCount(filter: $filter)
    }`
};

const StrategyMutations: BaseEntityGraphqlMutations = {
  save: gql`mutation SaveStrategy($data: StrategyVOInput!){
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
  ${ReferentialFragments.pmfm}
  ${ReferentialFragments.parameter}
  ${ReferentialFragments.fullReferential}
  ${ReferentialFragments.taxonName}`,

  delete: gql`mutation DeleteAllStrategies($id:Int!){
    deleteStrategy(id: $id)
  }`,
};

const strategySubscriptions: BaseEntityGraphqlSubscriptions = {
  listenChanges: gql`subscription UpdateReferential($entityName: String!, $id: Int!, $interval: Int){
    updateReferential(entityName: $entityName, id: $id, interval: $interval) {
      ...ReferentialFragment
    }
  }
  ${ReferentialFragments.referential}`
};

@Injectable({providedIn: 'root'})
export class StrategyService extends BaseReferentialService<Strategy, StrategyFilter> {

  constructor(
    graphql: GraphqlService,
    platform: PlatformService,
    protected network: NetworkService,
    protected accountService: AccountService,
    protected cache: CacheService,
    protected entities: EntitiesStorage,
    protected programRefService: ProgramRefService,
    protected strategyRefService: StrategyRefService,
    protected referentialRefService: ReferentialRefService
  ) {
    super(graphql, platform, Strategy, StrategyFilter,
      {
        queries: StrategyQueries,
        mutations: StrategyMutations,
        subscriptions: strategySubscriptions
      });
  }

  async existsByLabel(label: string, opts?: {
    programId?: number;
    excludedIds?: number[];
    fetchPolicy?: FetchPolicy
  }): Promise<boolean> {
    if (isNilOrBlank(label)) throw new Error("Missing argument 'label' ");

    const filter: Partial<StrategyFilter> = {
      label,
      levelId: opts && isNotNil(opts.programId) ? opts.programId : undefined,
      excludedIds: opts && isNotNil(opts.excludedIds) ? opts.excludedIds : undefined,
    };
    const {total} = await this.graphql.query<{ total: number }>({
      query: StrategyQueries.count,
      variables: { filter },
      error: {code: ErrorCodes.LOAD_STRATEGY_ERROR, message: "ERROR.LOAD_ERROR"},
      fetchPolicy: opts && opts.fetchPolicy || undefined
    });
    return toNumber(total, 0) > 0;
  }

  async computeNextLabel(programId: number, labelPrefix?: string, nbDigit?: number): Promise<string> {
    if (this._debug) console.debug(`[strategy-service] Loading strategy next label for prefix ${labelPrefix}...`);

    const res = await this.graphql.query<{ data: string }>({
      query: FindStrategyNextLabel,
      variables: {
        programId: programId,
        labelPrefix: labelPrefix,
        nbDigit: nbDigit
      },
      error: {code: ErrorCodes.LOAD_PROGRAM_ERROR, message: "PROGRAM.STRATEGY.ERROR.LOAD_STRATEGY_LABEL_ERROR"},
      fetchPolicy: 'network-only'
    });
    return res && res.data;
  }

  async computeNextSampleTagId(strategyLabel: string, separator?: string, nbDigit?: number): Promise<string> {
    if (this._debug) console.debug(`[strategy-service] Loading strategy next sample label...`);

    const res = await this.graphql.query<{ data: string }>({
      query: FindStrategyNextSampleLabel,
      variables: {
        strategyLabel: strategyLabel,
        labelSeparator: separator,
        nbDigit: nbDigit
      },
      error: {code: ErrorCodes.LOAD_PROGRAM_ERROR, message: "PROGRAM.STRATEGY.ERROR.LOAD_STRATEGY_SAMPLE_LABEL_ERROR"},
      fetchPolicy: 'network-only'
    });
    return res && res.data;
  }

  async loadStrategiesReferentials(programId: number,
                               entityName: string,
                               locationClassification?: string,
                               offset?: number,
                               size?: number,
                               sortBy?: string,
                               sortDirection?: SortDirection
                               ): Promise<ReferentialRef[]> {
    //if (this._debug)
      console.debug(`[strategy-service] Loading strategies referentials (predoc) for ${entityName}...`);

    const res = await this.graphql.query<LoadResult<ReferentialRef>>({
      query: FindStrategiesReferentials,
      variables: {
        programId: programId,
        locationClassification: locationClassification,
        entityName: entityName,
        offset: offset || 0,
        size: size || 5,
        sortBy: sortBy || 'label',
        sortDirection: sortDirection || 'asc'
      },
      error: {code: ErrorCodes.LOAD_PROGRAM_ERROR, message: "PROGRAM.STRATEGY.ERROR.LOAD_STRATEGY_SAMPLE_LABEL_ERROR"},
      fetchPolicy: 'network-only'
    });

    return (res && res.data || []) as ReferentialRef[];
  }

  async loadAllAnalyticReferences(
    offset: number,
    size: number,
    sortBy?: string,
    sortDirection?: SortDirection,
    filter?: Partial<ReferentialRefFilter>,
    opts?: {
      withTotal?: boolean;
      toEntity?: boolean;
    }): Promise<LoadResult<ReferentialRef>> {

    filter = ReferentialRefFilter.fromObject(filter);
    const variables: any = {
      offset: offset || 0,
      size: size || 100,
      sortBy: sortBy || 'label',
      sortDirection: sortDirection || 'asc',
      filter: filter && filter.asPodObject()
    };

    const now = this._debug && Date.now();
    //if (this._debug)
      console.debug(`[strategy-service] Loading analytic references...`, variables);

    const withTotal = (!opts || opts.withTotal !== false);
    const query = withTotal ? LoadAllAnalyticReferencesWithTotalQuery : LoadAllAnalyticReferencesQuery;
    const { data, total } = await this.graphql.query<LoadResult<any>>({
      query,
      variables,
      error: { code: ErrorCodes.LOAD_STRATEGY_ANALYTIC_REFERENCES_ERROR, message: "PROGRAM.STRATEGY.ERROR.LOAD_STRATEGY_ANALYTIC_REFERENCES_ERROR" },
      fetchPolicy: 'cache-first'
    });

    const entities = (!opts || opts.toEntity !== false)
      ? data && data.map(ReferentialRef.fromObject)
      : data as ReferentialRef[];

    const res: LoadResult<ReferentialRef> = {
      data: entities,
      total
    };

    // Add fetch more capability, if total was fetched
    if (withTotal) {
      const nextOffset = offset + entities.length;
      if (nextOffset < total) {
        res.fetchMore = () => this.loadAllAnalyticReferences(nextOffset, size, sortBy, sortDirection, filter, opts);
      }
    }

    if (this._debug) console.debug(`[strategy-service] Analytic references loaded in ${Date.now() - now}ms`);

    return res;
  }

  async suggestAnalyticReferences(value: any, filter?: ReferentialRefFilter, sortBy?: keyof Referential, sortDirection?: SortDirection): Promise<LoadResult<ReferentialRef>> {
    if (ReferentialUtils.isNotEmpty(value)) return {data: [value]};
    value = (typeof value === "string" && value !== '*') && value || undefined;
    return this.loadAllAnalyticReferences(0, !value ? 30 : 10, sortBy, sortDirection,
      { ...filter, searchText: value},
      {withTotal: true}
    );
  }

  canUserWrite(data?: Strategy): boolean {

    // user is admin: ok
    if (this.accountService.isAdmin()) return true;

    // TODO check if program managers
    //const isNew = (!data || isNil(data.id);
    return this.accountService.isSupervisor();
  }

  copyIdAndUpdateDate(source: Strategy, target: Strategy) {

    EntityUtils.copyIdAndUpdateDate(source, target);

    // Make sure tp copy programId (need by equals)
    target.programId = source.programId;

    // Applied strategies
    if (source.appliedStrategies && source.appliedStrategies.length > 0) {
      target.appliedStrategies.forEach(targetAppliedStrategy => {
        // Make sure to copy strategyId (need by equals)
        targetAppliedStrategy.strategyId = source.id;

        // Copy id and update date
        const savedAppliedStrategy = (source.appliedStrategies || []).find(as => targetAppliedStrategy.equals(as));
        EntityUtils.copyIdAndUpdateDate(savedAppliedStrategy, targetAppliedStrategy);
      });
    }

    // Pmfm strategies
    if (source.pmfms && source.pmfms.length > 0) {
      target.pmfms.forEach(targetPmfmStrategy => {
        // Make sure to copy strategyId (need by equals)
        targetPmfmStrategy.strategyId = source.id;

        // Copy id and update date
        const savedPmfmStrategy = source.pmfms.find(srcPmfmStrategy => targetPmfmStrategy.equals(srcPmfmStrategy));
        EntityUtils.copyIdAndUpdateDate(savedPmfmStrategy, targetPmfmStrategy);

        // Copy pmfm
        targetPmfmStrategy.pmfm = savedPmfmStrategy && savedPmfmStrategy.pmfm && Pmfm.fromObject(savedPmfmStrategy.pmfm) || targetPmfmStrategy.pmfm;
      });
    }
  }

  async save(entity: Strategy, options?: any): Promise<Strategy> {
   await this.clearCache();

    return super.save(entity, {
      ...options,
      refetchQueries: this._mutableWatchQueries
        .filter(query => query.query === this.queries.loadAllWithTotal || query.query === this.queries.loadAllWithTotal),
      awaitRefetchQueries: true
    });
  }

  /* -- protected functions -- */

  protected asObject(entity: Strategy, opts?: EntityAsObjectOptions): StoreObject {
    const target: any = super.asObject(entity, opts);

    (target.pmfms || []).forEach(pmfmStrategy => {
      pmfmStrategy.pmfmId = toNumber(pmfmStrategy.pmfm && pmfmStrategy.pmfm.id, pmfmStrategy.pmfmId);
      delete pmfmStrategy.pmfm;
    });

    return target;
  }

  protected async clearCache() {

    // Make sure to clean all strategy references (.e.g Pmfm cache, etc)
    await Promise.all([
      this.programRefService.clearCache(),
      this.strategyRefService.clearCache()
    ]);
  }
}
