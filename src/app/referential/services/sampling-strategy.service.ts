import {Injectable} from "@angular/core";
import {FetchPolicy, gql} from "@apollo/client/core";
import {ReferentialFragments} from "../services/referential.fragments";
import {GraphqlService} from "../../core/graphql/graphql.service";
import {CacheService} from "ionic-cache";
import {AccountService} from "../../core/services/account.service";
import {NetworkService} from "../../core/services/network.service";
import {EntitiesStorage} from "../../core/services/storage/entities-storage.service";
import {BaseReferentialService} from "../services/base-referential.service";
import {PlatformService} from "../../core/services/platform.service";
import {SortDirection} from "@angular/material/sort";
import {StrategyFragments} from "../services/strategy.fragments";
import {LoadResult} from "../../shared/services/entity-service.class";
import {StrategyFilter, StrategyService} from "../services/strategy.service";
import {Observable} from "rxjs";
import {map} from "rxjs/operators";
import {isEmptyArray, isNotNil} from "../../shared/functions";
import {ParameterLabelGroups} from "../services/model/model.enum";
import {ConfigService} from "../../core/services/config.service";
import {PmfmService} from "../services/pmfm.service";
import {ReferentialRefService} from "../services/referential-ref.service";
import {mergeMap} from "rxjs/internal/operators";
import {DateUtils} from "../../shared/dates";
import {SamplingStrategy, StrategyEffort} from "./model/sampling-strategy.model";

const DenormalizedStrategyFragments = {
  denormalizedStrategy: gql`fragment DenormalizedStrategyFragment on StrategyVO {
    id
    label
    name
    description
    comments
    analyticReference
    updateDate
    creationDate
    statusId
    programId
    gears {
      ...ReferentialFragment
    }
    taxonGroups {
      ...TaxonGroupStrategyFragment
    }
    taxonNames {
      ...TaxonNameStrategyFragment
    }
    appliedStrategies {
      ...AppliedStrategyFragment
    }
    pmfmStrategies {
      ...PmfmStrategyFragment
    }
    departments {
      ...StrategyDepartmentFragment
    }
  }`
}
;
const DenormalizedStrategyQueries = {
  loadAll: gql`query DenormalizedStrategies($filter: StrategyFilterVOInput!, $offset: Int, $size: Int, $sortBy: String, $sortDirection: String){
    data: strategies(filter: $filter, offset: $offset, size: $size, sortBy: $sortBy, sortDirection: $sortDirection){
      ...DenormalizedStrategyFragment
    }
    total: strategiesCount(filter: $filter)
  }
  ${DenormalizedStrategyFragments.denormalizedStrategy}
  ${StrategyFragments.appliedStrategy}
  ${StrategyFragments.appliedPeriod}
  ${StrategyFragments.pmfmStrategy}
  ${StrategyFragments.strategyDepartment}
  ${StrategyFragments.taxonGroupStrategy}
  ${StrategyFragments.taxonNameStrategy}
  ${ReferentialFragments.referential}
  ${ReferentialFragments.pmfm}
  ${ReferentialFragments.fullReferential}
  ${ReferentialFragments.parameter}
  ${ReferentialFragments.taxonName}`,

  loadAllWithTotal: gql`query DenormalizedStrategiesWithTotal($filter: StrategyFilterVOInput!, $offset: Int, $size: Int, $sortBy: String, $sortDirection: String){
    data: strategies(filter: $filter, offset: $offset, size: $size, sortBy: $sortBy, sortDirection: $sortDirection){
      ...DenormalizedStrategyFragment
    }
    total: strategiesCount(filter: $filter)
  }
  ${DenormalizedStrategyFragments.denormalizedStrategy}
  ${StrategyFragments.appliedStrategy}
  ${StrategyFragments.appliedPeriod}
  ${StrategyFragments.pmfmStrategy}
  ${StrategyFragments.strategyDepartment}
  ${StrategyFragments.taxonGroupStrategy}
  ${StrategyFragments.taxonNameStrategy}
  ${ReferentialFragments.referential}
  ${ReferentialFragments.pmfm}
  ${ReferentialFragments.fullReferential}
  ${ReferentialFragments.parameter}
  ${ReferentialFragments.taxonName}
  `,
  loadEffort: gql`query StrategyEffort($extractionType: String!,
    $offset: Int, $size: Int, $sortBy: String, $sortDirection: String,
    $viewSheetName: String!, $filterSheetName: String!,
    $columnName: String!, $operator: String!, $values: [String!]!) {
    data: extraction(
      type: {label: $extractionType},
      offset: $offset,
      size: $size,
      sortBy: $sortBy,
      sortDirection: $sortDirection,
      filter: {
        sheetName: $viewSheetName,
        criteria: [
          {sheetName: $filterSheetName, name: $columnName, operator: $operator, values: $values}
        ]
      }
    )
  }`
};

// TODO BLA: use cache to get strategy ?
const DenormalizedStrategyCacheKeys = {
  CACHE_GROUP: 'denormalizedStrategy',

  PMFM_IDS_BY_PARAMETER_GROUP: 'pmfmByGroups'
};

@Injectable({providedIn: 'root'})
export class SamplingStrategyService extends BaseReferentialService<SamplingStrategy, StrategyFilter> {

  constructor(
    graphql: GraphqlService,
    platform: PlatformService,
    protected network: NetworkService,
    protected accountService: AccountService,
    protected cache: CacheService,
    protected entities: EntitiesStorage,
    protected configService: ConfigService,
    protected strategyService: StrategyService,
    protected pmfmService: PmfmService,
    protected referentialRefService: ReferentialRefService
  ) {
    super(graphql, platform, SamplingStrategy,
      DenormalizedStrategyQueries,
      null,
      null,
      StrategyFilter.asPodObject, StrategyFilter.searchFilter);
  }

  watchAll(offset: number, size: number, sortBy?: string, sortDirection?: SortDirection, filter?: StrategyFilter,
           opts?: { fetchPolicy?: FetchPolicy; withTotal: boolean; withEffort?: boolean; }
           ): Observable<LoadResult<SamplingStrategy>> {
    return super.watchAll(offset, size, sortBy, sortDirection, filter, opts)
      .pipe(
        // Fill entities (parameter groups, effort, etc)
        mergeMap(res => this.fillEntities(res, opts)
          .then(() => res)
      ));
  }

  async loadAll(offset: number, size: number, sortBy?: string, sortDirection?: SortDirection, filter?: StrategyFilter,
           opts?: { fetchPolicy?: FetchPolicy; withTotal: boolean; withEffort?: boolean; withParameterGroups?: boolean; }
  ): Promise<LoadResult<SamplingStrategy>> {
    const res = await super.loadAll(offset, size, sortBy, sortDirection, filter, opts);

    // Fill entities (parameter groups, effort, etc)
    return this.fillEntities(res, opts);
  }

  async deleteAll(entities: SamplingStrategy[], options?: any): Promise<any> {
    return this.strategyService.deleteAll(entities, options);
  }

  /* -- protected -- */

  watchPmfmIdsByParameterLabels(parameterLabels: string[]): Observable<number[]> {
    return this.referentialRefService.watchAll(0, 1000, 'id', 'asc', {
      entityName: "Pmfm",
      levelLabels: parameterLabels
    }, {
      withTotal: false
    }).pipe(
      map((res) => {
        return (res.data || []).map(p => p.id);
      }));
  }

  async fillEntities(res: LoadResult<SamplingStrategy>, opts?: {
    withEffort?: boolean; withParameterGroups?: boolean;
  }): Promise<LoadResult<SamplingStrategy>> {
    const jobs: Promise<void>[] = [];
    // Fill parameters groups
    if (!opts || opts.withParameterGroups !== false) {
      jobs.push(this.fillParameterGroups(res.data));
    }
    // Fill strategy efforts
    if (!opts || opts.withEffort !== false) {
      jobs.push(this.fillEfforts(res.data)
        .catch(err => {
          console.error("Error while computing effort: " + err && err.message || err, err);
          res.errors = (res.errors || []).concat(err);
        })
      );
    }

    // Wait jobs end
    await Promise.all(jobs);

    return res;
  }

  /**
   * Fill parameterGroups attribute, on each denormalized strategy
   * @param entities
   */
  protected async fillParameterGroups(entities: SamplingStrategy[]): Promise<void> {

    const parameterListKeys = Object.keys(ParameterLabelGroups); // AGE, SEX, MATURITY, etc
    const pmfmIdsMap = await this.pmfmService.loadIdsGroupByParameterLabels(ParameterLabelGroups);

    entities.forEach(s => {
      const pmfmStrategies = s.pmfmStrategies;
      s.parameterGroups = (pmfmStrategies && parameterListKeys || []).reduce((res, key) => {
        return pmfmStrategies.findIndex(p => pmfmIdsMap[key].includes(p.pmfmId)) !== -1 ? res.concat(key) : res;
      }, []);
    });
  }

  protected async fillEfforts(entities: SamplingStrategy[]): Promise<void> {
    if (isEmptyArray(entities)) return; // Skip is empty

    console.debug(`[denormalized-strategy-service] Loading effort of ${entities.length} strategies...`);
    const {data} = await this.graphql.query<{data: { strategy: string; startDate: string; endDate: string; expectedEffort}[]}>({
      query: DenormalizedStrategyQueries.loadEffort,
      variables: {
        extractionType: "prog",
        viewSheetName: "SM",
        offset: 0,
        size: 1000, // All rows
        sortBy: "start_date",
        sortDirection: "asc",
        filterSheetName: "ST",
        columnName: "strategy_id",
        operator: "IN",
        values: entities.map(s => s.id.toString())
      }
    });

    // Add effort to entities
    (data || []).map(StrategyEffort.fromObject).forEach(effort => {
      const strategy = entities.find(s => s.label === effort.strategyLabel);
      if (strategy) {
        strategy.efforts = strategy.efforts || [];
        strategy.efforts.push(effort);
        if (isNotNil(effort.quarter)) {
          strategy.effortByQuarter = strategy.effortByQuarter || {};
          const existingEffort = strategy.effortByQuarter[effort.quarter];
          // Set the quarter's effort
          if (!existingEffort) {
            // Do a copy, to be able to increment if more than one effort by quarter
            strategy.effortByQuarter[effort.quarter] = effort.clone();
          }
          // More than one effort, on this quarter
          else {
            // Merge properties
            existingEffort.startDate = DateUtils.min(existingEffort.startDate, effort.startDate);
            existingEffort.endDate = DateUtils.max(existingEffort.endDate, effort.endDate);
            existingEffort.expectedEffort += effort.expectedEffort;
            existingEffort.realizedEffort += effort.realizedEffort;
          }
        }
      }
      else {
        console.warn(`[denormalized-strategy-service] An effort has unknown strategy '${effort.strategyLabel}'. Skipping. Please check GraphQL query 'extraction' of type 'prog'.`);
      }
    });

  }
}
