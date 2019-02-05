import {Injectable} from "@angular/core";
import gql from "graphql-tag";
import {Apollo} from "apollo-angular";
import {Observable} from "rxjs-compat";
import {fillRankOrder, isNil, Person, Trip} from "./trip.model";
import {DataService, LoadResult, isNotNil} from "../../shared/shared.module";
import {BaseDataService, environment} from "../../core/core.module";
import {map} from "rxjs/operators";
import {Moment} from "moment";

import {ErrorCodes} from "./trip.errors";
import {AccountService} from "../../core/services/account.service";
import {Fragments} from "./trip.queries";
import {ExtractionRow, ExtractionResult, ExtractionType} from "./extraction.model";
import {FetchPolicy} from "apollo-client";


export declare class ExtractionFilter {
  searchText?: string;
  criteria?: ExtractionFilterCriterion[];
}

export declare class ExtractionFilterCriterion {
  name: string;
  value: string;
}
const LoadTypes: any = gql`
  query ExtractionTypes{
    extractionTypes {
      label
      category
    }
  }
`;

const LoadRowsQuery: any = gql`
  query ExtractionRows($type: ExtractionTypeVOInput, $filter: ExtractionFilterVOInput, $offset: Int, $size: Int, $sortBy: String, $sortDirection: String){
    extraction(type: $type, filter: $filter, offset: $offset, size: $size, sortBy: $sortBy, sortDirection: $sortDirection){
      columns {
        name
        type
        description
        rankOrder
      }
      rows
      total
    }    
  }
`;


@Injectable()
export class ExtractionService extends BaseDataService{

  constructor(
    protected apollo: Apollo,
    protected accountService: AccountService
  ) {
    super(apollo);

    // FOR DEV ONLY
    this._debug = !environment.production;
  }

  /**
   * Load extraction types
   */
  loadTypes(category?: string): Observable<ExtractionType[]> {
    if (this._debug) console.debug("[extraction-service] Loading extractions...");
    const now = Date.now();
    return this.watchQuery<{ extractionTypes: ExtractionType[] }>({
      query: LoadTypes,
      variables: null,
      error: { code: ErrorCodes.LOAD_EXTRACTION_TYPES_ERROR, message: "EXTRACTION.ERROR.LOAD_EXTRACTION_TYPES_ERROR" }
    })
      .pipe(
        map((data) => {
          const types = (data && data.extractionTypes || []);

          // Compute type name
          types.forEach(type => {
            type.name = `EXTRACTION.${type.category.toUpperCase()}.${type.label.toUpperCase()}.TITLE`;
          });

          if (this._debug) console.debug(`[extraction-service] Extraction types loaded in ${Date.now() - now}...`, types);

          return types;
        })
      );
  }

  /**
   * Load many trips
   * @param offset
   * @param size
   * @param sortBy
   * @param sortDirection
   * @param filter
   */
  loadRows(
          type: ExtractionType,
          offset: number,
          size: number,
          sortBy?: string,
          sortDirection?: string,
          filter?: ExtractionFilter,
          options?: {
            fetchPolicy?: FetchPolicy
          }): Promise<ExtractionResult> {

    const variables: any = {
      type: {
        category: type.category,
        label: type.label
      },
      offset: offset || 0,
      size: size || 100,
      sortBy: sortBy || 'id',
      sortDirection: sortDirection || 'asc',
      filter: {
        // TODO add filter
        criteria : filter && filter.criteria || undefined
      }
    };

    this._lastVariables.loadAll = variables;

    const now = Date.now();
    if (this._debug) console.debug("[extraction-service] Loading rows... using options:", variables);
    return this.query<{ extraction: ExtractionResult }>({
      query: LoadRowsQuery,
      variables: variables,
      error: { code: ErrorCodes.LOAD_EXTRACTION_ROWS_ERROR, message: "EXTRACTION.ERROR.LOAD_EXTRACTION_ROWS_ERROR" },
      fetchPolicy: options && options.fetchPolicy || 'network-only'
    })
      .then(data => {
          const res = data && data.extraction;
          if (res) {
            if (this._debug) console.debug(`[extraction-service] Rows ${type.category} ${type.label} loaded in ${Date.now() - now}ms`, res);

            // Compute column index
            res.columns.forEach( (c, index) =>  c.index = index );

            return res;
          }
          return null;
        });
  }

}
