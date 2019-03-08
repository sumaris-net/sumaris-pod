import {Injectable} from "@angular/core";
import gql from "graphql-tag";
import {Apollo} from "apollo-angular";
import {Observable} from "rxjs-compat";
import {BaseDataService, environment, isNil, isNotNil} from "../../core/core.module";
import {map} from "rxjs/operators";

import {ErrorCodes} from "./trip.errors";
import {AccountService} from "../../core/services/account.service";
import {ExtractionResult, ExtractionType} from "./extraction.model";
import {FetchPolicy} from "apollo-client";
import {trimEmptyToNull} from "../../shared/functions";


export declare class ExtractionFilter {
  searchText?: string;
  criteria?: ExtractionFilterCriterion[];
  sheetName?: string;
}

export declare class ExtractionFilterCriterion {
  sheetName?: string;
  name?: string;
  operator: string;
  value?: string;
  values?: string[];
  endValue?: string;
}
const LoadTypes: any = gql`
  query ExtractionTypes{
    extractionTypes {
      label
      category
      sheetNames
    }
  }
`;

const LoadRowsQuery: any = gql`
  query ExtractionRows($type: ExtractionTypeVOInput, $filter: ExtractionFilterVOInput, $offset: Int, $size: Int, $sortBy: String, $sortDirection: String){
    extractionRows(type: $type, filter: $filter, offset: $offset, size: $size, sortBy: $sortBy, sortDirection: $sortDirection){
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

const GetFileQuery: any = gql`
  query ExtractionFile($type: ExtractionTypeVOInput, $filter: ExtractionFilterVOInput){
    extractionFile(type: $type, filter: $filter)
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
  async loadRows(
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
      filter: this.prepareFilter(filter)
    };

    this._lastVariables.loadAll = variables;

    const now = Date.now();
    if (this._debug) console.debug("[extraction-service] Loading rows... using options:", variables);
    const res = await this.query<{ extractionRows: ExtractionResult }>({
      query: LoadRowsQuery,
      variables: variables,
      error: { code: ErrorCodes.LOAD_EXTRACTION_ROWS_ERROR, message: "EXTRACTION.ERROR.LOAD_EXTRACTION_ROWS_ERROR" },
      fetchPolicy: options && options.fetchPolicy || 'network-only'
    });
    const data = res && res.extractionRows;
    if (!data) return null;

    // Compute column index
    (data.columns || []).forEach( (c, index) =>  c.index = index );
    if (this._debug) console.debug(`[extraction-service] Rows ${type.category} ${type.label} loaded in ${Date.now() - now}ms`, res);

    return data;
  }

  /**
   * Download extraction to file
   * @param type
   * @param filter
   * @param options
   */
  async downloadFile(
    type: ExtractionType,
    filter?: ExtractionFilter,
    options?: {
      fetchPolicy?: FetchPolicy
    }): Promise<string|undefined> {

    const variables: any = {
      type: {
        category: type.category,
        label: type.label
      },
      filter: this.prepareFilter(filter)
    };

    this._lastVariables.loadAll = variables;

    const now = Date.now();
    if (this._debug) console.debug("[extraction-service] Download extraction file... using options:", variables);
    const res = await this.query<{ extractionFile: string }>({
      query: GetFileQuery,
      variables: variables,
      error: { code: ErrorCodes.DOWNLOAD_EXTRACTION_FILE_ERROR, message: "EXTRACTION.ERROR.DOWNLOAD_EXTRACTION_FILE_ERROR" },
      fetchPolicy: options && options.fetchPolicy || 'network-only'
    });
    const fileUrl = res && res.extractionFile;
    if (!fileUrl) return undefined;

    if (this._debug) console.debug(`[extraction-service] Extraction ${type.category} ${type.label} done in ${Date.now() - now}ms: ${fileUrl}`, res);

    return fileUrl;
  }

  protected prepareFilter(source?: ExtractionFilter): ExtractionFilter {
    if (isNil(source)) return undefined;

    const target:ExtractionFilter = {
      sheetName: source.sheetName
    };

    target.criteria = (source.criteria || [])
      .filter(criterion => isNotNil(criterion.name) && isNotNil(trimEmptyToNull(criterion.value)))
      .map(criterion => {
        const isMulti = isNotNil(criterion.value) && criterion.value.indexOf(',') != -1;
        switch(criterion.operator) {
          case '=':
            if (isMulti) {
              criterion.operator = 'IN';
              criterion.values = (criterion.value as string)
                .split(',')
                .map(trimEmptyToNull)
                .filter(isNotNil);
              delete criterion.value;
            }
            break;
          case '!=':
            if (isMulti) {
              criterion.operator = 'NOT IN';
              criterion.values = (criterion.value as string)
                .split(',')
                .map(trimEmptyToNull)
                .filter(isNotNil);
              delete criterion.value;
            }
            break;
          case 'BETWEEN':
            if (isNotNil(trimEmptyToNull(criterion.endValue))) {
              criterion.values = [criterion.value.trim(), criterion.endValue.trim()];
            }
            delete criterion.value;
            break;
        }

        return {
          name: criterion.name,
          operator: criterion.operator,
          value: criterion.value,
          values: criterion.values,
          sheetName: criterion.sheetName
        } as ExtractionFilterCriterion;
      })
      .filter(criterion => isNotNil(criterion.value) || (criterion.values && criterion.values.length))

    return target;
  }

}
