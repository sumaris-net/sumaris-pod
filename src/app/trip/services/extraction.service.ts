import {Injectable} from "@angular/core";
import gql from "graphql-tag";
import {Observable} from "rxjs";
import {BaseDataService, EntityUtils, environment, isNil, isNotNil, StatusIds} from "../../core/core.module";
import {map} from "rxjs/operators";

import {ErrorCodes} from "./trip.errors";
import {AccountService} from "../../core/services/account.service";
import {
  AggregationType,
  ExtractionColumn,
  ExtractionFilter,
  ExtractionFilterCriterion,
  ExtractionResult,
  ExtractionType,
  StrataAreaType,
  StrataTimeType
} from "./extraction.model";
import {FetchPolicy} from "apollo-client";
import {isNotNilOrBlank, trimEmptyToNull} from "../../shared/functions";
import {GraphqlService} from "../../core/services/graphql.service";
import {FeatureCollection} from "geojson";
import {Fragments} from "./trip.queries";
import {SAVE_AS_OBJECT_OPTIONS} from "./model/base.model";


export const ExtractionFragments = {
  extractionType: gql`fragment ExtractionTypeFragment on ExtractionTypeVO {
    id
    category
    label
    name
    sheetNames
    isSpatial
    statusId
    recorderDepartment {
      ...LightDepartmentFragment
    }
  }
  ${Fragments.lightDepartment}
  `,
  aggregationType: gql`fragment AggregationTypeFragment on AggregationTypeVO {
    id
    category
    label
    name
    sheetNames
    description
    updateDate
    comments
    isSpatial
    statusId
    stratum {
      id
      label
      name
      updateDate
      isDefault
      sheetName
      spaceColumnName
      timeColumnName
      aggColumnName
      techColumnName
    }
    recorderDepartment {
      ...LightDepartmentFragment
    }
    recorderPerson {
      ...LightPersonFragment
    }
  }
  ${Fragments.lightDepartment}
  ${Fragments.lightPerson}
  `,
  column: gql`fragment ExtractionColumnFragment on ExtractionProductColumnVO {
    label
    name
    columnName
    type
    description
    rankOrder
  }`
}

export declare interface CustomAggregationStrata {
  spaceColumnName: StrataAreaType;
  timeColumnName: StrataTimeType;
  techColumnName?: string;
  aggColumnName?: string;
  aggFunction?: string;
}
//
// export declare class ExtractionFilterCriterion {
//   sheetName?: string;
//   name?: string;
//   operator: string;
//   value?: string;
//   values?: string[];
//   endValue?: string;
// }

const LoadTypes: any = gql`
  query ExtractionTypes{
    extractionTypes {
      ...ExtractionTypeFragment
    }
  }
  ${ExtractionFragments.extractionType}
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

const LoadAggregationColumnsQuery: any = gql`
  query AggregationColumns($type: AggregationTypeVOInput, $sheet: String){
    aggregationColumns(type: $type, sheet: $sheet){
      ...ExtractionColumnFragment
      values
    }
  }
  ${ExtractionFragments.column}
`;


const GetFileQuery: any = gql`
  query ExtractionFile($type: ExtractionTypeVOInput, $filter: ExtractionFilterVOInput){
    extractionFile(type: $type, filter: $filter)
  }
`;


const LoadAggregationType = gql`
  query AggregationType($id: Int) {
    aggregationType(id: $id) {
      ...AggregationTypeFragment
    }
  }
  ${ExtractionFragments.aggregationType}
`;

const LoadAggregationTypes = gql`
  query AggregationTypes($filter: AggregationTypeFilterVOInput) {
    aggregationTypes(filter: $filter) {
      ...AggregationTypeFragment
    }
  }
  ${ExtractionFragments.aggregationType}
  `;

const LoadAggregationGeoJsonQuery = gql`
  query AggregationGeoJson(
    $type: AggregationTypeVOInput,
    $filter: ExtractionFilterVOInput,
    $strata: AggregationStrataVOInput,
    $offset: Int, $size: Int, $sortBy: String, $sortDirection: String) {
      aggregationGeoJson(
        type: $type, filter: $filter, strata: $strata,
        offset: $offset, size: $size, sortBy: $sortBy, sortDirection: $sortDirection
      )
  }`;

const SaveAggregation: any = gql`
  mutation SaveAggregation($type: AggregationTypeVOInput, $filter: ExtractionFilterVOInput){
    saveAggregation(type: $type, filter: $filter){
      ...AggregationTypeFragment
    }
  }
  ${ExtractionFragments.aggregationType}
`;

const DeleteAggregations: any = gql`
  mutation DeleteAggregations($ids:[Int]){
    deleteAggregations(ids: $ids)
  }
`;

export interface AggregationTypeFilter {
  statusIds?: number[];
  isSpatial?: boolean;
}

@Injectable({providedIn: 'root'})
export class ExtractionService extends BaseDataService {

  constructor(
    protected graphql: GraphqlService,
    protected accountService: AccountService
  ) {
    super(graphql);

    // FOR DEV ONLY
    this._debug = !environment.production;
  }

  /**
   * Load extraction types
   */
  async loadTypes(): Promise<ExtractionType[]> {
    if (this._debug) console.debug("[extraction-service] Loading extraction types...");

    const variables = {};

    this._lastVariables.loadTypes = variables;

    const data = await this.graphql.query<{ extractionTypes: ExtractionType[] }>({
      query: LoadTypes,
      variables: variables,
      error: {code: ErrorCodes.LOAD_EXTRACTION_TYPES_ERROR, message: "EXTRACTION.ERROR.LOAD_TYPES_ERROR"}
    });

    return (data && data.extractionTypes || []).map(ExtractionType.fromObject);
  }

  /**
   * Load extraction types
   */
  watchTypes(): Observable<ExtractionType[]> {
    let now = Date.now();
    if (this._debug) console.debug("[extraction-service] Loading extraction types...");

    const variables = {

    };

    this._lastVariables.loadTypes = variables;

    return this.graphql.watchQuery<{ extractionTypes: ExtractionType[] }>({
      query: LoadTypes,
      variables: variables,
      error: {code: ErrorCodes.LOAD_EXTRACTION_TYPES_ERROR, message: "EXTRACTION.ERROR.LOAD_TYPES_ERROR"}
    })
      .pipe(
        map((data) => {
          const res = (data && data.extractionTypes || []).map(ExtractionType.fromObject);
          if (this._debug && now) {
            console.debug(`[extraction-service] Extraction types loaded in ${Date.now() - now}ms`, res);
            now = undefined;
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
      sortBy: sortBy || undefined,
      sortDirection: sortDirection || 'asc',
      filter: filter
    };

    this._lastVariables.loadAll = variables;

    const now = Date.now();
    if (this._debug) console.debug("[extraction-service] Loading rows... using options:", variables);
    const res = await this.graphql.query<{ extractionRows: ExtractionResult }>({
      query: LoadRowsQuery,
      variables: variables,
      error: {code: ErrorCodes.LOAD_EXTRACTION_ROWS_ERROR, message: "EXTRACTION.ERROR.LOAD_ROWS_ERROR"},
      fetchPolicy: options && options.fetchPolicy || 'network-only'
    });
    const data = res && res.extractionRows;
    if (!data) return null;

    // Compute column index
    (data.columns || []).forEach((c, index) => c.index = index);

    if (this._debug) console.debug(`[extraction-service] Rows ${type.category} ${type.label} loaded in ${Date.now() - now}ms`, data);
    return data;
  }

  /**
   * Load columns metadata
   * @param offset
   * @param size
   * @param sortBy
   * @param sortDirection
   * @param filter
   */
  async loadColumns(
    type: ExtractionType,
    sheetName?: string,
    options?: {
      fetchPolicy?: FetchPolicy
    }): Promise<ExtractionColumn[]> {

    const variables: any = {
      type: {
        category: type.category,
        label: type.label
      },
      sheet: sheetName
    };

    const now = Date.now();
    if (this._debug) console.debug("[extraction-service] Loading columns... using options:", variables);
    const res = await this.graphql.query<{ aggregationColumns: ExtractionColumn[] }>({
      query: LoadAggregationColumnsQuery,
      variables: variables,
      error: {code: ErrorCodes.LOAD_EXTRACTION_ROWS_ERROR, message: "EXTRACTION.ERROR.LOAD_ROWS_ERROR"},
      fetchPolicy: options && options.fetchPolicy || 'network-only'
    });
    const data = res && res.aggregationColumns;
    if (!data) return null;

    // Compute column index
    (data || []).forEach((c, index) => c.index = index);

    if (this._debug) console.debug(`[extraction-service] Columns ${type.category} ${type.label} loaded in ${Date.now() - now}ms`, data);
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

    this._lastVariables.loadAll = variables;

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

  /**
   * Load spatial types
   */
  loadAggregationTypes(filter?: AggregationTypeFilter,
                       options?: { fetchPolicy?: FetchPolicy }
  ): Observable<AggregationType[]> {
    if (this._debug) console.debug("[extraction-service] Loading geo types...");

    const variables = {
      filter: filter
    };

    // Remember variables, to be able to update the cache in saveAggregation()
    this._lastVariables.loadAggregationTypes = variables;

    return this.graphql.watchQuery<{ aggregationTypes: AggregationType[] }>({
      query: LoadAggregationTypes,
      variables: variables,
      error: {code: ErrorCodes.LOAD_EXTRACTION_GEO_TYPES_ERROR, message: "EXTRACTION.ERROR.LOAD_GEO_TYPES_ERROR"},
      fetchPolicy: options && options.fetchPolicy || 'network-only'
    })
      .pipe(
        map((data) => (data && data.aggregationTypes || []).map(AggregationType.fromObject))
      );
  }

  async loadAggregationType(id: number,  options?: {
    fetchPolicy?: FetchPolicy
  }): Promise<AggregationType> {
    const data = await this.graphql.query<{ aggregationType: AggregationType }>({
      query: LoadAggregationType,
      variables: {
        id
      },
      error: {code: ErrorCodes.LOAD_EXTRACTION_GEO_TYPES_ERROR, message: "EXTRACTION.ERROR.LOAD_GEO_TYPE_ERROR"},
      fetchPolicy: options && options.fetchPolicy || 'network-only'
    });

    return (data && data.aggregationType && AggregationType.fromObject(data.aggregationType));
  }

  /**
   * Load aggregation as GeoJson
   */
  async loadAggregationGeoJson(type: AggregationType,
                               strata: CustomAggregationStrata,
                               offset: number,
                               size: number,
                               sortBy?: string,
                               sortDirection?: string,
                               filter?: ExtractionFilter,
                               options?: {
                                 fetchPolicy?: FetchPolicy
                               }): Promise<FeatureCollection> {
    options = options || {};

    const variables: any = {
      type: {
        category: type.category,
        label: type.label
      },
      strata: strata,
      filter: filter,
      offset: offset || 0,
      size: size || 1000
    };

    const res = await this.graphql.query<{ aggregationGeoJson: any }>({
      query: LoadAggregationGeoJsonQuery,
      variables: variables,
      error: {code: ErrorCodes.LOAD_EXTRACTION_GEO_DATA_ERROR, message: "EXTRACTION.ERROR.LOAD_GEO_DATA_ERROR"},
      fetchPolicy: options && options.fetchPolicy || 'network-only'
    });

    return (res && res.aggregationGeoJson as FeatureCollection) || null;
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


  async saveAggregation(sourceType: AggregationType,
                        filter?: ExtractionFilter): Promise<AggregationType> {
    const now = Date.now();
    if (this._debug) console.debug("[extraction-service] Saving aggregation...");

    // Transform into entity
    const entity = AggregationType.fromObject(sourceType);

    this.fillDefaultProperties(entity);

    const isNew = isNil(sourceType.id);

    // Transform to json
    const json = entity.asObject(SAVE_AS_OBJECT_OPTIONS);
    if (this._debug) console.debug("[extraction-service] Using minify object, to send:", json);

    await this.graphql.mutate<{ saveAggregation: any }>({
      mutation: SaveAggregation,
      variables: {
        type: json,
        filter: filter
      },
      error: {code: ErrorCodes.SAVE_AGGREGATION_ERROR, message: "ERROR.SAVE_DATA_ERROR"},
      update: (proxy, {data}) => {
        const savedEntity = data && AggregationType.fromObject(data.saveAggregation);
        if (savedEntity) {
          if (savedEntity !== entity) {
            EntityUtils.copyIdAndUpdateDate(savedEntity, entity);

            if (this._debug) console.debug(`[extraction-service] Aggregation saved in ${Date.now() - now}ms`, savedEntity);
          }

          // Always force category, as sourceType could NOT be a live extraction
          savedEntity.category = 'product';
          savedEntity.isSpatial = entity.isSpatial;

          if (isNew) {
            // Add to cache (extraction types)
            let addToCache = this._lastVariables.loadTypes &&
              // Check if cache on the same statusId
              (isNil(this._lastVariables.loadTypes.statusIds) || this._lastVariables.loadTypes.statusIds.findIndex(s => s === entity.statusId) !== -1);
            if (addToCache) {
              this.graphql.addToQueryCache(proxy, {
                query: LoadTypes,
                variables: this._lastVariables.loadTypes
              }, 'extractionTypes', savedEntity);
            }

            // Add to cache (aggregation types)
            addToCache = this._lastVariables.loadAggregationTypes &&
              // Check if cache on the same statusId
              (this._lastVariables.loadAggregationTypes.statusIds || []).findIndex(s => s === entity.statusId) !== -1;
            if (addToCache) {
              this.graphql.addToQueryCache(proxy, {
                query: LoadAggregationTypes,
                variables: this._lastVariables.loadAggregationTypes
              }, 'aggregationTypes', savedEntity);
            }
          }
        }
      }
    });

    return entity;
  }

  async deleteAggregations(entities: AggregationType[]): Promise<any> {
    const ids = entities && entities
      .map(t => t.id)
      .filter(isNotNil);

    const now = Date.now();
    if (this._debug) console.debug("[extraction-service] Deleting aggregations... ids:", ids);

    await this.graphql.mutate<any>({
      mutation: DeleteAggregations,
      variables: {
        ids: ids
      },
      update: (proxy) => {

        // Remove from cache (extraction types)
        if (this._lastVariables.loadTypes) {
          this.graphql.removeToQueryCacheByIds(proxy,{
            query: LoadTypes,
            variables: this._lastVariables.loadTypes
          }, 'extractionTypes', ids);
        }

        // Remove from cache (aggregation types)
        if (this._lastVariables.loadAggregationTypes) {
          this.graphql.removeToQueryCacheByIds(proxy,{
            query: LoadAggregationTypes,
            variables: this._lastVariables.loadAggregationTypes
          }, 'aggregationTypes', ids);
        }

        if (this._debug) console.debug(`[extraction-service] Aggregations deleted in ${Date.now() - now}ms`);
      }
    });
  }

  /* -- protected methods  -- */

  protected fillDefaultProperties(entity: AggregationType) {

    // If new trip
    if (isNil(entity.id)) {

      // Compute label
      entity.label = `${entity.label}-${Date.now()}`;

      // Recorder department
      entity.recorderDepartment = EntityUtils.isNotEmpty(entity.recorderDepartment) ? entity.recorderDepartment : this.accountService.department;

      // Recorder person
      entity.recorderPerson = entity.recorderPerson || this.accountService.person;
    }

    entity.name = entity.name || `Aggregation on ${entity.label}`;
    entity.statusId = isNotNil(entity.statusId) ? entity.statusId : StatusIds.TEMPORARY;

    // Description
    if (!entity.description) {
      const account = this.accountService.account;
      entity.description = `Created by ${account.firstName} ${account.lastName}`;
    }
  }

  protected copyIdAndUpdateDate(source: AggregationType, target: AggregationType) {

    EntityUtils.copyIdAndUpdateDate(source, target);
  }
}
