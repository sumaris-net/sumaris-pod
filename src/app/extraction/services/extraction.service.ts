import {Injectable} from "@angular/core";
import {FetchPolicy, gql, WatchQueryFetchPolicy} from "@apollo/client/core";
import {Observable} from "rxjs";
import {BaseEntityService, isNil, isNotNil} from "../../core/core.module";
import {map} from "rxjs/operators";

import {ErrorCodes} from "../../trip/services/trip.errors";
import {AccountService} from "../../core/services/account.service";
import {ExtractionFilter, ExtractionFilterCriterion, ExtractionResult, ExtractionType} from "./model/extraction.model";
import {isNotNilOrBlank, trimEmptyToNull} from "../../shared/functions";
import {GraphqlService} from "../../core/graphql/graphql.service";
import {Fragments} from "../../trip/services/trip.queries";
import {SortDirection} from "@angular/material/sort";
import {firstNotNilPromise} from "../../shared/observables";


export const ExtractionFragments = {
  type: gql`fragment ExtractionTypeFragment on ExtractionTypeVO {
    id
    category
    label
    name
    description
    comments
    version
    sheetNames
    isSpatial
    statusId
    recorderDepartment {
      ...LightDepartmentFragment
    }
  }
  ${Fragments.lightDepartment}`,
  column: gql`fragment ExtractionColumnFragment on ExtractionTableColumnVO {
    label
    name
    columnName
    type
    description
    rankOrder
  }`
}


const LoadTypesQuery: any = gql`
  query ExtractionTypes {
    extractionTypes {
      ...ExtractionTypeFragment
    }
  }
  ${ExtractionFragments.type}
`;

const LoadRowsQuery: any = gql`
  query ExtractionRows($type: ExtractionTypeVOInput, $filter: ExtractionFilterVOInput, $offset: Int, $size: Int, $sortBy: String, $sortDirection: String){
    extractionRows(type: $type, filter: $filter, offset: $offset, size: $size, sortBy: $sortBy, sortDirection: $sortDirection){
      columns {
        ...ExtractionColumnFragment
      }
      rows
      total
    }
  }
  ${ExtractionFragments.column}
`;


const GetFileQuery: any = gql`
  query ExtractionFile($type: ExtractionTypeVOInput, $filter: ExtractionFilterVOInput){
    extractionFile(type: $type, filter: $filter)
  }
`;


@Injectable({providedIn: 'root'})
export class ExtractionService extends BaseEntityService {

  constructor(
    protected graphql: GraphqlService,
    protected accountService: AccountService
  ) {
    super(graphql);
  }

  /**
   * Load extraction types
   */
  async loadAll(): Promise<ExtractionType[]> {
    return await firstNotNilPromise(this.watchAll());
  }

  /**
   * Watch extraction types
   */
  watchAll(opts?: { fetchPolicy?: WatchQueryFetchPolicy }): Observable<ExtractionType[]> {
    let now = Date.now();
    if (this._debug) console.debug("[extraction-service] Loading extraction types...");

    return this.mutableWatchQuery<{ extractionTypes: ExtractionType[] }>({
      queryName: 'LoadExtractionTypes',
      query: LoadTypesQuery,
      arrayFieldName: 'extractionTypes',
      variables: {},
      error: {code: ErrorCodes.LOAD_EXTRACTION_TYPES_ERROR, message: "EXTRACTION.ERROR.LOAD_TYPES_ERROR"},
      ...opts
    })
      .pipe(
        map((data) => {
          const res = (data && data.extractionTypes || [])
            .filter(json => {
              // Workaround because saveAggregation() doest not add NEW extraction type correctly
              if (!json || isNil(json.label)) {
                console.warn('[extraction-service] FIXME: Invalid extraction type (no label)... bad cache insertion in saveAggregation() ?')
                return false;
              }
              return true;
            })
            .map(ExtractionType.fromObject);
          if (this._debug && now) {
            console.debug(`[extraction-service] Extraction types loaded in ${Date.now() - now}ms`, res);
            now = undefined;
          }
          else {
            console.debug(`[extraction-service] Extraction types updated (probably by cache)`, res);
          }
          return res;
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
    sortDirection?: SortDirection,
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
      sortBy: sortBy || undefined,
      sortDirection: sortDirection || 'asc',
      filter: filter
    };

    const now = Date.now();
    if (this._debug) console.debug("[extraction-service] Loading rows... using options:", variables);
    const res = await this.graphql.query<{ extractionRows: ExtractionResult }>({
      query: LoadRowsQuery,
      variables: variables,
      error: {code: ErrorCodes.LOAD_EXTRACTION_ROWS_ERROR, message: "EXTRACTION.ERROR.LOAD_ROWS_ERROR"},
      fetchPolicy: options && options.fetchPolicy || 'network-only'
    });
    if (!res || !res.extractionRows) return null;
    const data = ExtractionResult.fromObject(res.extractionRows);

    // Compute column index
    (data.columns || []).forEach((c, index) => c.index = index);

    if (this._debug) console.debug(`[extraction-service] Rows ${type.category} ${type.label} loaded in ${Date.now() - now}ms`, data);
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
    }): Promise<string | undefined> {

    const variables: any = {
      type: {
        category: type.category,
        label: type.label
      },
      filter: filter
    };

    const now = Date.now();
    if (this._debug) console.debug("[extraction-service] Download extraction file... using options:", variables);
    const res = await this.graphql.query<{ extractionFile: string }>({
      query: GetFileQuery,
      variables: variables,
      error: {code: ErrorCodes.DOWNLOAD_EXTRACTION_FILE_ERROR, message: "EXTRACTION.ERROR.DOWNLOAD_FILE_ERROR"},
      fetchPolicy: options && options.fetchPolicy || 'network-only'
    });
    const fileUrl = res && res.extractionFile;
    if (!fileUrl) return undefined;

    if (this._debug) console.debug(`[extraction-service] Extraction ${type.category} ${type.label} done in ${Date.now() - now}ms: ${fileUrl}`, res);

    return fileUrl;
  }


  prepareFilter(source?: ExtractionFilter | any): ExtractionFilter {
    if (isNil(source)) return undefined;

    const target: ExtractionFilter = {
      sheetName: source.sheetName
    };

    target.criteria = (source.criteria || [])
      .filter(criterion => isNotNil(criterion.name) && isNotNilOrBlank(criterion.value))
      .map(criterion => {
        const isMulti = (typeof criterion.value === 'string' && criterion.value.indexOf(',') !== -1);
        switch (criterion.operator) {
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
            if (isNotNilOrBlank(criterion.endValue)) {
              if (typeof criterion.value === 'string') {
                criterion.values = [criterion.value.trim(), criterion.endValue.trim()];
              }
              else {
                criterion.values = [criterion.value, criterion.endValue];
              }
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
      .filter(criterion => isNotNil(criterion.value) || (criterion.values && criterion.values.length));

    return target;
  }


}
