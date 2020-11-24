import {Injectable} from "@angular/core";
import gql from "graphql-tag";
import {isNil, isNotNil, LoadResult} from "../../shared/shared.module";
import {BaseEntityService, EntityUtils, environment} from "../../core/core.module";
import {ErrorCodes} from "./errors";
import {AccountService} from "../../core/services/account.service";
import {FetchPolicy} from "@apollo/client/core";
import {FilterFn, SuggestService} from "../../shared/services/entity-service.class";
import {GraphqlService} from "../../core/graphql/graphql.service";
import {Metier} from "./model/taxon.model";
import {NetworkService} from "../../core/services/network.service";
import {EntitiesStorage} from "../../core/services/storage/entities-storage.service";
import {ReferentialFragments} from "./referential.fragments";
import {ReferentialRefFilter} from "./referential-ref.service";
import {Moment} from "moment";
import {ReferentialUtils} from "../../core/services/model/referential.model";
import {StatusIds} from "../../core/services/model/model.enum";
import {SortDirection} from "@angular/material/sort";

export class MetierFilter extends ReferentialRefFilter {

  static  searchFilter<T extends Metier>(f: MetierFilter): FilterFn<T>{
    const filterFns: FilterFn<T>[] = [];

    // Filter by levels ids FIXME entity.levelId doesn't exists
    // const levelIds = f.levelIds || (isNotNil(f.levelId) && [f.levelId]) || undefined;
    // if (levelIds) {
    //   filterFns.push((entity: T) => !!levelIds.find(v => entity.levelId === v));
    // }

    // Filter by status
    const statusIds = f.statusIds || (isNotNil(f.statusId) && [f.statusId]) || undefined;
    if (statusIds) {
      filterFns.push((entity) => !!statusIds.find(v => entity.statusId === v));
    }

    const searchTextFilter = EntityUtils.searchTextFilter(f.searchAttribute || f.searchAttributes, f.searchText);
    if (searchTextFilter) filterFns.push(searchTextFilter);

    if (!filterFns.length) return undefined;

    return (entity) => !filterFns.find(fn => !fn(entity));
  }

  constructor() {
    super();
    this.entityName = ''
  }

  programLabel?: string;
  date?: Date | Moment;
  vesselId?: number;
  tripId?: number;
}

export const METIER_DEFAULT_FILTER: MetierFilter = {
  entityName: 'Metier',
  statusId: StatusIds.ENABLE
};

const LoadAllQuery: any = gql`
  query Metiers($offset: Int, $size: Int, $sortBy: String, $sortDirection: String, $filter: MetierFilterVOInput){
    metiers(offset: $offset, size: $size, sortBy: $sortBy, sortDirection: $sortDirection, filter: $filter){
      ...ReferentialFragment
    }
  }
  ${ReferentialFragments.referential}
`;
const LoadQuery: any = gql`
  query Metier($id: Int!){
    metier(id: $id){
      ...MetierFragment
    }
  }
  ${ReferentialFragments.metier}
`;

@Injectable({providedIn: 'root'})
export class MetierService extends BaseEntityService
  implements SuggestService<Metier, MetierFilter> {

  constructor(
    protected graphql: GraphqlService,
    protected accountService: AccountService,
    protected network: NetworkService,
    protected entities: EntitiesStorage
  ) {
    super(graphql);

    // -- For DEV only
    this._debug = !environment.production;
  }

  async load(id: number, options?: any): Promise<Metier> {
    if (isNil(id)) throw new Error("Missing argument 'id'");
    const now = this._debug && Date.now();
    if (this._debug) console.debug(`[metier-ref-service] Loading Metier #${id}...`);

    const data = await this.graphql.query<{ metier: Metier }>({
      query: LoadQuery,
      variables: {id: id},
      fetchPolicy: options && options.fetchPolicy || undefined
    });

    if (data && data.metier) {
      const metier = Metier.fromObject(data.metier, {useChildAttributes: false});
      if (metier && this._debug) console.debug(`[metier-ref-service] Metier #${id} loaded in ${Date.now() - now}ms`, metier);
      return metier;
    }
    return null;
  }

  async loadAll(offset: number,
                size: number,
                sortBy?: string,
                sortDirection?: SortDirection,
                filter?: MetierFilter,
                opts?: {
                  [key: string]: any;
                  fetchPolicy?: FetchPolicy;
                  debug?: boolean;
                  transformToEntity?: boolean;
                }): Promise<LoadResult<Metier>> {

    if (!filter) {
      console.error("[metier-ref-service] Missing filter");
      throw {code: ErrorCodes.LOAD_REFERENTIAL_ERROR, message: "REFERENTIAL.ERROR.LOAD_REFERENTIAL_ERROR"};
    }

    const debug = this._debug && (!opts || opts.debug !== false);

    const variables: any = {
      offset: offset || 0,
      size: size || 100,
      sortBy: sortBy || filter.searchAttribute || 'label',
      sortDirection: sortDirection || 'asc',
      filter: {
        label: filter.label,
        name: filter.name,
        searchText: filter.searchText,
        searchAttribute: filter.searchAttribute,
        levelIds: isNotNil(filter.levelId) ? [filter.levelId] : filter.levelIds,
        statusIds: isNotNil(filter.statusId) ? [filter.statusId] : (filter.statusIds || [StatusIds.ENABLE]),
        // Predoc filter
        programLabel: filter.programLabel,
        date: filter.date,
        vesselId: filter.vesselId,
        tripId: filter.tripId
      }
    };

    const now = debug && Date.now();
    if (debug) console.debug(`[metier-ref-service] Loading Metier items...`, variables);

    // Offline mode: read from the entities storage
    let loadResult: { metiers: any[]; metiersCount: number };
    const offline = this.network.offline && (!opts || opts.fetchPolicy !== 'network-only');
    if (offline) {
      loadResult = await this.entities.loadAll('MetierVO',
        {
          ...variables,
          filter: MetierFilter.searchFilter(filter)
        }
      ).then(res => {
        return {
          metiers: res && res.data,
          metiersCount: res && res.total
        };
      });
    }

    // Online mode: use graphQL
    else {
      const query = LoadAllQuery; // Without count
      loadResult = await this.graphql.query<{ metiers: any[]; metiersCount: number }>({
        query,
        variables,
        error: {code: ErrorCodes.LOAD_REFERENTIAL_ERROR, message: "REFERENTIAL.ERROR.LOAD_REFERENTIAL_ERROR"},
        fetchPolicy: opts && opts.fetchPolicy || 'cache-first'
      });
    }

    const data = (!opts || opts.transformToEntity !== false) ?
      (loadResult && loadResult.metiers || []).map(value => Metier.fromObject(value, {useChildAttributes: false})) :
      (loadResult && loadResult.metiers || []) as Metier[];
    if (debug) console.debug(`[metier-ref-service] Metiers loaded in ${Date.now() - now}ms`);
    return {
      data: data,
      total: loadResult.metiersCount
    };
  }

  async suggest(value: any, filter?: MetierFilter): Promise<Metier[]> {
    if (ReferentialUtils.isNotEmpty(value)) return [value];
    value = (typeof value === "string" && value !== '*') && value || undefined;
    const res = await this.loadAll(0, !value ? 30 : 10, undefined, undefined,
      {...filter, searchText: value},
      {withTotal: false /* total not need */}
    );
    return res.data;
  }
}
