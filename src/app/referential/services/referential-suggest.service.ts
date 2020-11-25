import {Injectable} from "@angular/core";
import gql from "graphql-tag";
import {Observable} from "rxjs";
import {map} from "rxjs/operators";
import {isNotNil, LoadResult, EntitiesService} from "../../shared/shared.module";
import {BaseEntityService, EntityUtils, Referential} from "../../core/core.module";
import {ErrorCodes} from "./errors";
import {AccountService} from "../../core/services/account.service";

import {FetchPolicy, MutationUpdaterFn} from "apollo-client";
import {GraphqlService} from "../../core/services/graphql.service";
import {ReferentialSuggestFragments} from "./referential-suggest.fragments";
import {environment} from "../../../environments/environment";
import {Beans, KeysEnum, toNumber} from "../../shared/functions";
import {ReferentialUtils} from "../../core/services/model/referential.model";
import {StatusIds} from "../../core/services/model/model.enum";
import {SortDirection} from "@angular/material/sort";
import {PlatformService} from "../../core/services/platform.service";

export class ReferentialSuggestFilter {
  entityName: string;

  id?: number;
  label?: string;
  name?: string;

  statusId?: number;
  statusIds?: number[];

  levelId?: number;
  levelIds?: number[];

  searchJoin?: string; // If search is on a sub entity (e.g. Metier can search on TaxonGroup)
  searchText?: string;
  searchAttribute?: string;

  static isEmpty(f: ReferentialSuggestFilter|any): boolean {
    return Beans.isEmpty<ReferentialSuggestFilter>(f, ReferentialSuggestFilterKeys, {
      blankStringLikeEmpty: true
    });
  }

  /**
   * Clean a filter, before sending to the pod (e.g remove 'levelId', 'statusId')
   * @param filter
   */
  static asPodObject(filter: ReferentialSuggestFilter): any {
    if (!filter) return filter;
    return {
      id: filter.id,
      label: filter.label,
      name: filter.name,
      searchText: filter.searchText,
      searchAttribute: filter.searchAttribute,
      searchJoin: filter.searchJoin,
      levelIds: isNotNil(filter.levelId) ? [filter.levelId] : filter.levelIds,
      statusIds: isNotNil(filter.statusId) ? [filter.statusId] : (filter.statusIds || [StatusIds.ENABLE])
    };
  }
}
export const ReferentialSuggestFilterKeys: KeysEnum<ReferentialSuggestFilter> = {
  entityName: true,
  id: true,
  label: true,
  name: true,
  statusId: true,
  statusIds: true,
  levelId: true,
  levelIds: true,
  searchJoin: true,
  searchText: true,
  searchAttribute: true
};

export interface ReferentialType {
  id: string;
  level?: string;
}

const LoadQuery: any = gql`
  query SuggestedStrategyNextLabelQuery($id: Int, $labelPrefix: String, $nbDigit: Int){
    suggestedStrategyNextLabel(programId: $programId, labelPrefix: $labelPrefix, nbDigit: $nbDigit){
      ...SuggestedStrategyNextLabelFragment
    }
  }
  ${ReferentialSuggestFragments.SuggestedStrategyNextLabelFragment}
`;

@Injectable({providedIn: 'root'})
export class ReferentialSuggestService extends BaseEntityService<Referential> implements EntitiesService<Referential, ReferentialSuggestFilter> {

  constructor(
    protected graphql: GraphqlService,
    protected accountService: AccountService,
    protected platform: PlatformService
  ) {
    super(graphql);

    platform.ready().then(() => {
      // No limit for updatable watch queries, if desktop
      if (!platform.mobile) {
        this._mutableWatchQueriesMaxCount = -1;
      }
    });

    // For DEV only
    this._debug = !environment.production;
  }

  /**
   * Load pmfms
   * @param offset
   * @param size
   * @param sortBy
   * @param sortDirection
   * @param filter
   */
  async findSuggestedStrategyNextLabel(programId: number, labelPrefix: string, nbDigit: number,options?: any): Promise<string> {
    opts = opts || {};
    const variables: any = {
      offset: offset || 0,
      size: size || 100,
      sortBy: sortBy || 'label',
      sortDirection: sortDirection || 'asc',
      filter: ReferentialSuggestFilter.asPodObject(filter)
    };
    const debug = this._debug && (opts.debug !== false);
    const now = debug && Date.now();
    if (debug) console.debug("[referentiel-suggest-service] find SuggestedStrategyNextLabel... using options:", variables);

    const data = await this.graphql.query<{ suggestedStrategyNextLabel: string}>({
      query: SuggestedStrategyNextLabelQuery,
      variables: {programId: programId, labelPrefix: labelPrefix, nbDigit: nbDigit},
      error: {code: ErrorCodes.LOAD_REFERENTIAL_ERROR, message: "REFERENTIAL.ERROR.LOAD_REFERENTIAL_ERROR"},
      fetchPolicy: opts && opts.fetchPolicy || undefined
    });

    if (data && data.suggestedStrategyNextLabel) {
          const suggestedStrategyNextLabel = data.suggestedStrategyNextLabel;
          if (suggestedStrategyNextLabel && this._debug) console.debug(`[referentiel-suggest-service] suggestedStrategyNextLabel loaded in ${Date.now() - now}ms`);
          return suggestedStrategyNextLabel;
        }
    return null;

  }

}
