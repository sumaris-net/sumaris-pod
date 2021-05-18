import {Injectable} from "@angular/core";
import {ApolloCache, FetchPolicy, gql, WatchQueryFetchPolicy} from "@apollo/client/core";
import {Observable} from "rxjs";
import {map} from "rxjs/operators";

import {ErrorCodes} from "../../trip/services/trip.errors";
import {AccountService} from "../../core/services/account.service";
import {ExtractionFilter, ExtractionFilterCriterion, ExtractionResult, ExtractionType} from "./model/extraction.model";
import {isNil, isNotNil, isNotNilOrBlank, trimEmptyToNull} from "../../shared/functions";
import {GraphqlService} from "../../core/graphql/graphql.service";
import {Fragments} from "../../trip/services/trip.queries";
import {SortDirection} from "@angular/material/sort";
import {firstNotNilPromise} from "../../shared/observables";
import {BaseGraphqlService} from "../../core/services/base-graphql-service.class";
import {environment} from "../../../environments/environment";
import {DataEntityAsObjectOptions} from "../../data/services/model/data-entity.model";
import {MINIFY_OPTIONS} from "../../core/services/model/referential.model";
import {Person} from "../../core/services/model/person.model";
import {EntityUtils} from "../../core/services/model/entity.model";


export const ExtractionFragments = {
  type: gql`fragment ExtractionTypeFragment on ExtractionTypeVO {
    id
    category
    label
    name
    description
    docUrl
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
};


export const LoadTypesQuery: any = gql`
  query ExtractionTypes {
    data: extractionTypes {
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


const GetFileQuery = gql`
  query ExtractionFile($type: ExtractionTypeVOInput, $filter: ExtractionFilterVOInput){
    extractionFile(type: $type, filter: $filter)
  }
`;

export const SaveExtractionMutation = gql`
  mutation SaveExtraction($type: ExtractionTypeVOInput, $filter: ExtractionFilterVOInput) {
    data: saveExtraction(type: $type, filter: $filter) {
      ...ExtractionTypeFragment
    }
  }
  ${ExtractionFragments.type}
`;

@Injectable({providedIn: 'root'})
export class ExtractionService extends BaseGraphqlService {

  constructor(
    protected graphql: GraphqlService,
    protected accountService: AccountService,
  ) {
    super(graphql, environment);
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

    return this.mutableWatchQuery<{ data: ExtractionType[] }>({
      queryName: 'LoadExtractionTypes',
      query: LoadTypesQuery,
      arrayFieldName: 'data',
      error: {code: ErrorCodes.LOAD_EXTRACTION_TYPES_ERROR, message: "EXTRACTION.ERROR.LOAD_TYPES_ERROR"},
      ...opts
    })
      .pipe(
        map((data) => {
          const res = (data && data.data || [])
            .filter(json => {
              // Workaround because saveAggregation() doest not add NEW extraction type correctly
              if (!json || isNil(json.label)) {
                console.warn('[extraction-service] FIXME: Invalid extraction type (no label)... bad cache insertion in saveAggregation() ?');
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
   * @param type
   * @param offset
   * @param size
   * @param sortBy
   * @param sortDirection
   * @param filter
   * @param options
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
      fetchPolicy: options && options.fetchPolicy || 'no-cache'
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
      .filter(criterion => isNotNil(criterion.name))
      .map(criterion => {
        const isMulti = typeof criterion.value === 'string' && criterion.value.indexOf(',') !== -1;
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

        delete criterion.endValue;

        return criterion as ExtractionFilterCriterion;
      })
      .filter(ExtractionFilterCriterion.isNotEmpty);

    return target;
  }

  async save(entity: ExtractionType, filter: ExtractionFilter): Promise<ExtractionType> {
    const now = Date.now();
    if (this._debug) console.debug("[extraction-service] Saving extraction...", entity, filter);

    this.fillDefaultProperties(entity);

    const json = this.asObject(entity);

    const isNew = isNil(entity.id) || entity.id < 0; // Exclude live types

    await this.graphql.mutate<{ data: any }>({
      mutation: SaveExtractionMutation,
      variables: { type: json, filter },
      // TODO : change error code
      error: {code: ErrorCodes.LOAD_EXTRACTION_ROWS_ERROR, message: "EXTRACTION.ERROR.LOAD_ROWS_ERROR"},
      update: (cache, {data}) => {
        const savedEntity = data && data.data;
        EntityUtils.copyIdAndUpdateDate(savedEntity, entity);

        // Insert into cache
        if (isNew) {
          this.insertIntoCache(cache, savedEntity);
        }
      }
    });

    return entity;
  }

  insertIntoCache(cache: ApolloCache<{data: any}>, type: ExtractionType) {
    if (!type || !type.label) throw new Error('Invalid type for cache: ' + type);

    console.info('[extraction-service] Inserting into cache:', type);
    this.insertIntoMutableCachedQuery(cache, {
      queryName: "LoadExtractionTypes",
      query: LoadTypesQuery,
      data: type
    });
  }

  updateCache(cache: ApolloCache<{data: any}>, type: ExtractionType) {
    if (!type || !type.label) throw new Error('Invalid type for cache: ' + type);
    console.info('[extraction-service] Updating cache:', type);
    // Remove, then insert, from extraction types
    const exists = this.removeFromMutableCachedQueryByIds(cache, {
      queryName: "LoadExtractionTypes",
      query: LoadTypesQuery,
      ids: type.id
    }) > 0;
    if (exists) {
      this.insertIntoMutableCachedQuery(cache, {
        queryName: "LoadExtractionTypes",
        query: LoadTypesQuery,
        data: type
      });
    }
  }

  /* -- protected functions -- */

  protected fillDefaultProperties(entity: ExtractionType) {
    // If new entity
    const isNew = isNil(entity.id) || entity.id < 0;
    if (isNew) {

      const person = this.accountService.person;

      // Recorder department
      if (person && person.department && !entity.recorderDepartment) {
        entity.recorderDepartment = person.department;
      }

      // Recorder person
      if (person && person.id && !entity.recorderPerson) {
        entity.recorderPerson = person;
      }
    }
  }

  protected asObject(entity: ExtractionType, opts?: DataEntityAsObjectOptions): any {
    opts = { ...MINIFY_OPTIONS, ...opts };
    const copy = entity.asObject(opts);

    if (opts && opts.minify) {

      // Comment because need to keep recorder person
      copy.recorderPerson = entity.recorderPerson && <Person>{
        id: entity.recorderPerson.id,
        firstName: entity.recorderPerson.firstName,
        lastName: entity.recorderPerson.lastName
      };

      // Keep id only, on department
      copy.recorderDepartment = entity.recorderDepartment && {id: entity.recorderDepartment && entity.recorderDepartment.id} || undefined;
    }

    return copy;
  }
}
