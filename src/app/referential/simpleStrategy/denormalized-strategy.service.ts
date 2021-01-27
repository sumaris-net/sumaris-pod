import {Injectable} from "@angular/core";
import {FetchPolicy, gql} from "@apollo/client/core";
import {ReferentialFragments} from "../services/referential.fragments";
import {GraphqlService} from "../../core/graphql/graphql.service";
import {CacheService} from "ionic-cache";
import {AccountService} from "../../core/services/account.service";
import {NetworkService} from "../../core/services/network.service";
import {EntitiesStorage} from "../../core/services/storage/entities-storage.service";
import {AppliedPeriod, Strategy} from "../services/model/strategy.model";
import {BaseReferentialEntitiesQueries, BaseReferentialService} from "../services/base-referential.service";
import {PlatformService} from "../../core/services/platform.service";
import {SortDirection} from "@angular/material/sort";
import {StrategyFragments} from "../services/strategy.fragments";
import {LoadResult} from "../../shared/services/entity-service.class";
import {StrategyFilter} from "../services/strategy.service";
import {forkJoin, Observable, of} from "rxjs";
import {concatAll, map} from "rxjs/operators";
import {PmfmStrategy} from "../services/model/pmfm-strategy.model";
import {isEmptyArray, isNotNil} from "../../shared/functions";
import {PmfmUtils} from "../services/model/pmfm.model";
import {ParameterLabel, ParameterLabelGroups} from "../services/model/model.enum";
import {ConfigService} from "../../core/services/config.service";
import {PmfmService} from "../services/pmfm.service";
import {ReferentialRefService} from "../services/referential-ref.service";
import {firstNotNilPromise} from "../../shared/observables";
import {mergeMap} from "rxjs/internal/operators";
import {Moment} from "moment";
import {DateUtils, fromDateISOString} from "../../shared/dates";

export class DenormalizedStrategy extends Strategy<DenormalizedStrategy> {

  parameterGroups: string[];
  efforts: StrategyEffort[];
  effortByQuarter: {
    1?: StrategyEffort;
    2?: StrategyEffort;
    3?: StrategyEffort;
    4?: StrategyEffort;
  };

  constructor() {
    super();
  }

  clone(): DenormalizedStrategy {
    const target = new DenormalizedStrategy();
    target.fromObject(this);
    return target;
  }

}


export class StrategyEffort {

  static fromObject(value: any): StrategyEffort {
    if (!value || value instanceof StrategyEffort) return value;
    const target = new StrategyEffort();
    target.fromObject(value);
    return target;
  }

  strategyLabel: string;
  startDate: Moment;
  endDate: Moment;
  quarter: number;
  expectedEffort: number;
  realizedEffort: number;

  constructor() {
  }

  clone(): StrategyEffort {
    const target = new StrategyEffort();
    target.fromObject(this);
    return target;
  }

  fromObject(source: any) {
    if (!source) return;
    this.strategyLabel = source.strategy || source.strategyLabel;
    this.startDate = fromDateISOString(source.startDate);
    this.endDate = fromDateISOString(source.endDate);
    this.expectedEffort = source.expectedEffort;
    this.realizedEffort = source.realizedEffort;

    // Compute quarter (if possible = is same between start/end date)
    const startQuarter = this.startDate && this.startDate.quarter();
    const endQuarter = this.endDate && this.endDate.quarter();
    this.quarter = startQuarter === endQuarter ? startQuarter : undefined;
  }

  get realized(): boolean {
    return (!this.expectedEffort || (this.realizedEffort && this.realizedEffort >= this.expectedEffort));
  }

  get missingEffort(): number {
    return !this.expectedEffort ? undefined :
      // Avoid negative missing effort (when realized > expected)
      Math.max(0, this.expectedEffort - (this.realizedEffort || 0));
  }

}

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


const DenormalizedStrategyCacheKeys = {
  CACHE_GROUP: 'denormalizedStrategy',

  PMFM_IDS_BY_PARAMETER_GROUP: 'pmfmByGroups'
};

@Injectable({providedIn: 'root'})
export class DenormalizedStrategyService extends BaseReferentialService<DenormalizedStrategy, StrategyFilter> {

  constructor(
    graphql: GraphqlService,
    platform: PlatformService,
    protected network: NetworkService,
    protected accountService: AccountService,
    protected cache: CacheService,
    protected entities: EntitiesStorage,
    protected configService: ConfigService,
    protected pmfmService: PmfmService,
    protected referentialRefService: ReferentialRefService
  ) {
    super(graphql, platform, DenormalizedStrategy,
      DenormalizedStrategyQueries,
      null,
      null,
      StrategyFilter.asPodObject, StrategyFilter.searchFilter);
  }

  watchAll(offset: number, size: number, sortBy?: string, sortDirection?: SortDirection, filter?: StrategyFilter,
           opts?: { fetchPolicy?: FetchPolicy; withTotal: boolean }
           ): Observable<LoadResult<DenormalizedStrategy>> {
    return super.watchAll(offset, size, sortBy, sortDirection, filter, opts)
      .pipe(
        mergeMap(async (res) => Promise.all([
            // Fill parameters groups
            this.fillParameterGroups(res.data),
            // Fill strategy efforts
            this.fillEfforts(res.data)
          ]).then(() => res)
      ));
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

  async loadPmfmIdsByParameterLabels(parameterLabels: string[]): Promise<number[]> {
    const {data} = await this.referentialRefService.loadAll(0, 1000, 'id', 'asc', {
      entityName: "Pmfm",
      levelLabels: parameterLabels
    }, {
      withTotal: false
    });
    return (data || []).map(p => p.id);
  }

  //const parameterLabels = Object.values(ParameterLabelList)
  //  .reduce((res, labels) => res.concat(...labels), []);


  /**
   * Fill parameterGroups attribute, on each denormalized strategy
   * @param entities
   */
  protected async fillParameterGroups(entities: DenormalizedStrategy[]): Promise<void> {

    const parameterListKeys = Object.keys(ParameterLabelGroups); // AGE, SEX, MATURITY, etc
    const pmfmIdsMap = await this.pmfmService.loadIdsGroupByParameterLabels(ParameterLabelGroups);

    entities.forEach(s => {
      const pmfmStrategies = s.pmfmStrategies;
      s.parameterGroups = (pmfmStrategies && parameterListKeys || []).reduce((res, key) => {
        return pmfmStrategies.findIndex(p => pmfmIdsMap[key].includes(p.pmfmId)) !== -1 ? res.concat(key) : res;
      }, []);
    });
  }

  protected async fillEfforts(entities: DenormalizedStrategy[]): Promise<void> {
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

    console.log("TODO: entities: ", entities);
  }
}
