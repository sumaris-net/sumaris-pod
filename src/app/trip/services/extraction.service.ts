import {Injectable} from "@angular/core";
import gql from "graphql-tag";
import {Observable} from "rxjs-compat";
import {BaseDataService, environment, isNil, isNotNil, StatusIds} from "../../core/core.module";
import {map} from "rxjs/operators";

import {ErrorCodes} from "./trip.errors";
import {AccountService} from "../../core/services/account.service";
import {AggregationStrata, AggregationType, ExtractionResult, ExtractionType} from "./extraction.model";
import {FetchPolicy} from "apollo-client";
import {isNilOrBlank, trimEmptyToNull} from "../../shared/functions";
import {GraphqlService} from "../../core/services/graphql.service";
import {FeatureCollection} from "geojson";
import {TripFragments} from "./trip.service";


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


const LoadAggregationTypes = gql`
  query AggregationTypes($filter: AggregationTypeFilterVOInput) {
    aggregationTypes(filter: $filter) {
      id
      label
      name
      description
      category
      sheetNames
      strata {
        space
        time
        tech
      }
      statusId
    }
  }`;

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
      id
      label
      category
      sheetNames
    }
  }
`;

export interface AggregationTypeFilter {
  statusIds?: number[];
  isSpatial?: boolean;
}

@Injectable()
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
  loadTypes(): Observable<ExtractionType[]> {
    if (this._debug) console.debug("[extraction-service] Loading extraction types...");
    return this.graphql.watchQuery<{ extractionTypes: ExtractionType[] }>({
      query: LoadTypes,
      variables: null,
      error: {code: ErrorCodes.LOAD_EXTRACTION_TYPES_ERROR, message: "EXTRACTION.ERROR.LOAD_TYPES_ERROR"}
    })
      .pipe(
        map((data) => (data && data.extractionTypes || []).map(ExtractionType.fromObject))
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
   * Load aggregation types
   */
  loadAggregationTypes(filter?: AggregationTypeFilter): Observable<AggregationType[]> {
    if (this._debug) console.debug("[extraction-service] Loading geo types...");

    const variables = {
      filter: filter
    };

    // Remember variables, to be able to update the cache in saveAggregation()
    this._lastVariables.loadAggregationTypes = variables;

    return this.graphql.watchQuery<{ aggregationTypes: AggregationType[] }>({
      query: LoadAggregationTypes,
      variables: variables,
      error: {code: ErrorCodes.LOAD_EXTRACTION_GEO_TYPES_ERROR, message: "EXTRACTION.ERROR.LOAD_GEO_TYPES_ERROR"}
    })
      .pipe(
        map((data) => (data && data.aggregationTypes || []).map(AggregationType.fromObject))
      );
  }

  /**
   * Load aggregation as GeoJson
   */
  async loadAggregationGeoJson(type: AggregationType,
                               strata: AggregationStrata,
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
      .filter(criterion => isNotNil(criterion.name) && isNotNil(trimEmptyToNull(criterion.value)))
      .map(criterion => {
        const isMulti = isNotNil(criterion.value) && criterion.value.indexOf(',') != -1;
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
      .filter(criterion => isNotNil(criterion.value) || (criterion.values && criterion.values.length));

    return target;
  }


  async save(type: ExtractionType | AggregationType,
             filter?: ExtractionFilter,
             options?: {
               aggregate: boolean;
             }): Promise<ExtractionType | AggregationType> {

    // Transform into json
    options = options || {aggregate: true};
    if (options.aggregate) {
      const entity = AggregationType.fromObject(type);

      this.fillDefaultProperties(entity);

      const json = entity.asObject(true/*minify*/);

      const isNew = isNil(type.id);

      const now = Date.now();
      if (this._debug) console.debug("[extraction-service] Saving aggregation...");

      const res = await this.graphql.mutate<{ saveAggregation: any }>({
        mutation: SaveAggregation,
        variables: {
          type: json,
          filter: filter
        },
        error: {code: ErrorCodes.SAVE_AGGREGATION_ERROR, message: "ERROR.SAVE_DATA_ERROR"}
      });

      const savedEntity = res && res.saveAggregation && res.saveAggregation[0];
      if (savedEntity) {
        this.copyIdAndUpdateDate(savedEntity, entity);

        // Add to cache
        const addToCache = isNew && this._lastVariables.loadAggregationTypes &&
          // Check if cache on the same statusId
          (this._lastVariables.loadAggregationTypes.statusIds || []).findIndex(s => s === entity.statusId) !== -1;
        if (addToCache) {
          this.addToQueryCache({
            query: LoadAggregationTypes,
            variables: this._lastVariables.loadAggregationTypes
          }, 'aggregationTypes', savedEntity);
        }
      }

      if (this._debug) console.debug(`[trip-service] Trip saved in ${Date.now() - now}ms`, entity);

      return entity;
    }

    throw new Error("Not aggregated extraction could not be saved yet");
  }

  /* -- protected methods  -- */

  protected fillDefaultProperties(source: AggregationType) {

    source.name = source.name || 'Aggregation for ' + source.label;

    source.statusId = isNotNil(source.statusId) ? source.statusId : StatusIds.TEMPORARY;
  }

  protected copyIdAndUpdateDate(source: AggregationType, target: AggregationType) {

    target.id = isNotNil(source.id) ? source.id : target.id;
    target.updateDate = isNotNil(source.updateDate) ? source.updateDate : target.updateDate;
  }
}
