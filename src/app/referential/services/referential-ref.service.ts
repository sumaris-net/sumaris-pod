import {Injectable} from "@angular/core";
import gql from "graphql-tag";
import {Observable} from "rxjs";
import {map} from "rxjs/operators";
import {isNotNil, LoadResult} from "../../shared/shared.module";
import {BaseDataService, EntityUtils, StatusIds} from "../../core/core.module";
import {ErrorCodes} from "./errors";
import {AccountService} from "../../core/services/account.service";
import {ReferentialRef} from "../../core/services/model";

import {FetchPolicy} from "apollo-client";
import {ReferentialFilter, TaxonNameFilter} from "./referential.service";
import {SuggestionDataService} from "../../shared/services/data-service.class";
import {GraphqlService} from "../../core/services/graphql.service";
import {TaxonomicLevelIds} from "./model";
import {TaxonNameRef} from "./model/taxon.model";

const LoadAllQuery: any = gql`
  query Referentials($entityName: String, $offset: Int, $size: Int, $sortBy: String, $sortDirection: String, $filter: ReferentialFilterVOInput){
    referentials(entityName: $entityName, offset: $offset, size: $size, sortBy: $sortBy, sortDirection: $sortDirection, filter: $filter){
      id
      label
      name
      statusId
      entityName
    }
  }
`;

const LoadAllTaxonNamesQuery: any = gql`
  query TaxonNames($offset: Int, $size: Int, $sortBy: String, $sortDirection: String, $filter: TaxonNameFilterVOInput){
    taxonNames(offset: $offset, size: $size, sortBy: $sortBy, sortDirection: $sortDirection, filter: $filter){
      id
      label
      name
      statusId
      referenceTaxonId
    }
  }
`;

@Injectable({providedIn: 'root'})
export class ReferentialRefService extends BaseDataService
  implements SuggestionDataService<ReferentialRef> {

  constructor(
    protected graphql: GraphqlService,
    protected accountService: AccountService
  ) {
    super(graphql);

    // -- For DEV only
    //this._debug = !environment.production;
  }

  /**
   * @param offset
   * @param size
   * @param sortBy
   * @param sortDirection
   * @param filter
   * @param options
   */
  watchAll(offset: number,
           size: number,
           sortBy?: string,
           sortDirection?: string,
           filter?: ReferentialFilter,
           options?: {
             [key: string]: any;
             fetchPolicy?: FetchPolicy;
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
      sortBy: sortBy || 'label',
      sortDirection: sortDirection || 'asc',
      filter: {
        label: filter.label,
        name: filter.name,
        searchText: filter.searchText,
        searchAttribute: filter.searchAttribute,
        levelId: filter.levelId,
        levelIds: filter.levelIds,
        statusIds: isNotNil(filter.statusId) ? [filter.statusId] : [StatusIds.ENABLE]
      }
    };

    const now = new Date();
    if (this._debug) console.debug(`[referential-ref-service] Watching references on ${entityName}...`, variables);

    return this.graphql.watchQuery<{ referentials: any[]; referentialsCount: number }>({
      query: LoadAllQuery,
      variables: variables,
      error: {code: ErrorCodes.LOAD_REFERENTIAL_ERROR, message: "REFERENTIAL.ERROR.LOAD_REFERENTIAL_ERROR"},
      fetchPolicy: options && options.fetchPolicy || "cache-first"
    })
      .pipe(
        map(({referentials, referentialsCount}) => {
          const data = (referentials || []).map(ReferentialRef.fromObject);
          if (this._debug) console.debug(`[referential-ref-service] References on ${entityName} loaded in ${new Date().getTime() - now.getTime()}ms`, data);
          return {
            data: data,
            total: referentialsCount
          };
        })
      );
  }

  async loadAll(offset: number,
                size: number,
                sortBy?: string,
                sortDirection?: string,
                filter?: ReferentialFilter,
                options?: {
                  [key: string]: any;
                  fetchPolicy?: FetchPolicy;
                }): Promise<LoadResult<ReferentialRef>> {

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
      sortDirection: sortDirection || 'asc',
      filter: {
        label: filter.label,
        name: filter.name,
        searchText: filter.searchText,
        searchAttribute: filter.searchAttribute,
        searchJoin: filter.searchJoin,
        levelIds: isNotNil(filter.levelId) ? [filter.levelId] : filter.levelIds,
        statusIds: isNotNil(filter.statusId) ? [filter.statusId] : (filter.statusIds || [StatusIds.ENABLE])
      }
    };

    const now = Date.now();
    if (this._debug) console.debug(`[referential-ref-service] Loading references on ${entityName}...`, variables);

    const res = await this.graphql.query<{ referentials: any[]; referentialsCount: number }>({
      query: LoadAllQuery,
      variables: variables,
      error: {code: ErrorCodes.LOAD_REFERENTIAL_ERROR, message: "REFERENTIAL.ERROR.LOAD_REFERENTIAL_ERROR"},
      fetchPolicy: options && options.fetchPolicy || "cache-first"
    });
    const data = (res && res.referentials || []).map(ReferentialRef.fromObject);
    if (this._debug) console.debug(`[referential-ref-service] References on ${entityName} loaded in ${Date.now() - now}ms`, data);
    return {
      data: data,
      total: res.referentialsCount
    };
  }

  async suggest(value: any, options: {
    entityName: string;
    levelId?: number;
    levelIds?: number[];
    searchAttribute?: string;
    statusId?: number;
    statusIds?: number[];
    searchJoin?: string;
  }): Promise<ReferentialRef[]> {
    if (EntityUtils.isNotEmpty(value)) return [value];
    value = (typeof value === "string" && value !== '*') && value || undefined;
    const res = await this.loadAll(0, !value ? 30 : 10, undefined, undefined,
      {
        entityName: options.entityName,
        levelId: options.levelId,
        levelIds: options.levelIds,
        searchText: value as string,
        searchJoin: options.searchJoin,
        searchAttribute: options.searchAttribute,
        statusId: options.statusId,
        statusIds: options.statusIds
      });
    return res.data;
  }

  async loadAllTaxonNames(offset: number,
               size: number,
               sortBy?: string,
               sortDirection?: string,
               filter?: TaxonNameFilter,
               options?: {
                 [key: string]: any;
                 fetchPolicy?: FetchPolicy;
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
        taxonomicLevelIds: isNotNil(filter.taxonomicLevelId) ? [filter.taxonomicLevelId] : (filter.taxonomicLevelIds || [TaxonomicLevelIds.SPECIES, TaxonomicLevelIds.SUBSPECIES]),
        statusIds: isNotNil(filter.statusId) ? [filter.statusId] : (filter.statusIds || [StatusIds.ENABLE]),
        taxonGroupIds: isNotNil(filter.taxonGroupId) ? [filter.taxonGroupId] : (filter.taxonGroupIds || undefined)
      }
    };

    const now = Date.now();
    if (this._debug) console.debug(`[referential-ref-service] Loading taxon names...`, variables);

    const res = await this.graphql.query<{ taxonNames: any[]}>({
      query: LoadAllTaxonNamesQuery,
      variables: variables,
      error: {code: ErrorCodes.LOAD_REFERENTIAL_ERROR, message: "REFERENTIAL.ERROR.LOAD_REFERENTIAL_ERROR"},
      fetchPolicy: options && options.fetchPolicy || "cache-first"
    });
    const data = (res && res.taxonNames || []).map(TaxonNameRef.fromObject);
    if (this._debug) console.debug(`[referential-ref-service] Taxon names loaded in ${Date.now() - now}ms`, data);
    return data;
  }

  async suggestTaxonNames(value: any, options: {
    taxonomicLevelId?: number;
    taxonomicLevelIds?: number[];
    searchAttribute?: string;
    taxonGroupId?: number;
  }): Promise<TaxonNameRef[]> {
    if (EntityUtils.isNotEmpty(value)) return [value];
    value = (typeof value === "string" && value !== '*') && value || undefined;
    return await this.loadAllTaxonNames(0, !value ? 30 : 10, undefined, undefined,
      {
        searchText: value as string,
        searchAttribute: options.searchAttribute,
        taxonomicLevelId: options.taxonomicLevelId,
        taxonomicLevelIds: options.taxonomicLevelIds,
        taxonGroupId: options.taxonGroupId
      });
  }

}
