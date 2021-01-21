import {Injectable} from "@angular/core";
import {gql} from "@apollo/client/core";
import {ReferentialFragments} from "./referential.fragments";
import {GraphqlService} from "../../core/graphql/graphql.service";
import {CacheService} from "ionic-cache";
import {ReferentialRefService} from "./referential-ref.service";
import {AccountService} from "../../core/services/account.service";
import {NetworkService} from "../../core/services/network.service";
import {EntitiesStorage} from "../../core/services/storage/entities-storage.service";
import {ReferentialFilter} from "./referential.service";
import {Strategy} from "./model/strategy.model";
import {
  BaseReferentialEntitiesQueries,
  BaseReferentialEntityMutations, BaseReferentialEntityQueries,
  BaseReferentialService, BaseReferentialSubscriptions
} from "./base-referential.service";
import {PlatformService} from "../../core/services/platform.service";


export class StrategyFilter extends ReferentialFilter {
}


export const StrategyFragments = {
  strategy: gql`
    fragment StrategyFragment on StrategyVO {
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
    }
  `,
  pmfmStrategy: gql`
    fragment PmfmStrategyFragment on PmfmStrategyVO {
      id
      acquisitionLevel
      rankOrder
      isMandatory
      acquisitionNumber
      defaultValue
      pmfmId
      pmfm {
        ...FullPmfmFragment  # TODO BLA rename as PmfmFragment ?
      }
      parameterId
      matrixId
      fractionId
      methodId
      gearIds
      taxonGroupIds
      referenceTaxonIds
      strategyId
      __typename
  }`,
  taxonGroupStrategy: gql`
    fragment TaxonGroupStrategyFragment on TaxonGroupStrategyVO {
      strategyId
      priorityLevel
      taxonGroup {
          id
          label
          name
          entityName
          taxonNames {
              ...TaxonNameFragment
          }
      }
      __typename
    }
  `,
  taxonNameStrategy: gql`
    fragment TaxonNameStrategyFragment on TaxonNameStrategyVO {
      strategyId
      priorityLevel
      taxonName {
          ...TaxonNameFragment
      }
      __typename
    }
  `
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

const LoadQuery: any = gql`
  query Strategy($id: Int!) {
    strategy(id: $id) {
      ...StrategyFragment
    }
  }
  ${StrategyFragments.strategy}
  ${StrategyFragments.pmfmStrategy}
  ${StrategyFragments.taxonGroupStrategy}
  ${StrategyFragments.taxonNameStrategy}
  ${ReferentialFragments.referential}
  ${ReferentialFragments.pmfm}
  ${ReferentialFragments.taxonName}
  `,
  loadAll: gql`query Strategies($filter: StrategyFilterVOInput!, $offset: Int, $size: Int, $sortBy: String, $sortDirection: String){
    data: strategies(filter: $filter, offset: $offset, size: $size, sortBy: $sortBy, sortDirection: $sortDirection){
      ...StrategyFragment
    }
  }
  ${StrategyFragments.strategy}
  ${StrategyFragments.pmfmStrategy}
  ${StrategyFragments.taxonGroupStrategy}
  ${StrategyFragments.taxonNameStrategy}
  ${ReferentialFragments.referential}
  ${ReferentialFragments.pmfm}
  ${ReferentialFragments.taxonName}
  `,
  loadAllWithTotal: gql`query StrategiesWithTotal($filter: StrategyFilterVOInput!, $offset: Int, $size: Int, $sortBy: String, $sortDirection: String){
    data: strategies(filter: $filter, offset: $offset, size: $size, sortBy: $sortBy, sortDirection: $sortDirection){
      ...StrategyFragment
    }
    total: strategiesCount(filter: $filter)
  }
  ${StrategyFragments.strategy}
  ${StrategyFragments.pmfmStrategy}
  ${StrategyFragments.taxonGroupStrategy}
  ${StrategyFragments.taxonNameStrategy}
  ${ReferentialFragments.referential}
  ${ReferentialFragments.pmfm}
  ${ReferentialFragments.taxonName}
  `
};


const StrategyMutations: BaseReferentialEntityMutations = {
  save: gql`mutation SaveStrategy($data:StrategyVOInput!){
    saveStrategy(strategy: $data){
      ...StrategyFragment
    }
  }
  ${StrategyFragments.strategy}
  ${StrategyFragments.pmfmStrategy}
  ${StrategyFragments.taxonGroupStrategy}
  ${StrategyFragments.taxonNameStrategy}
  ${ReferentialFragments.referential}
  ${ReferentialFragments.pmfm}
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


  async findStrategyNextLabel(programId: number, labelPrefix?: string, nbDigit?: number): Promise<string> {
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

  // TODO BLA rename (L majuscule)
  async LoadAllAnalyticReferences(
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
    if (this._debug) console.debug(`[strategy-service] Loading analytic references items...`, variables);

    const res = await this.graphql.query<{ analyticReferences: Referential[] }>({
      query: LoadAllAnalyticReferencesQuery,
      variables: variables,
      error: { code: ErrorCodes.LOAD_PROGRAM_ERROR, message: "PROGRAM.STRATEGY.ERROR.LOAD_STRATEGY_REFERENCES_ERROR" },
      fetchPolicy: 'cache-first'
    });

    if (this._debug) console.debug(`[strategy-service] Analytic references items loaded in ${Date.now() - now}ms`);
    return (res && res.analyticReferences || []) as ReferentialRef[];
  }

  async suggestAnalyticReferences(value: any, filter?: ReferentialRefFilter, sortBy?: keyof Referential, sortDirection?: SortDirection): Promise<ReferentialRef[]> {
    if (ReferentialUtils.isNotEmpty(value)) return [value];
    value = (typeof value === "string" && value !== '*') && value || undefined;
    const res = await this.LoadAllAnalyticReferences(0, !value ? 30 : 10, sortBy, sortDirection,
      { ...filter, searchText: value}
    );
    return res;
  }

  protected copyIdAndUpdateDate(source: Strategy, target: Strategy) {

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
}
