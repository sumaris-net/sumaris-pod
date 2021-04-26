import {Injectable} from "@angular/core";
import {FetchPolicy, gql, WatchQueryFetchPolicy} from "@apollo/client/core";
import {Observable} from "rxjs";
import {map} from "rxjs/operators";

import {ErrorCodes} from "../../trip/services/trip.errors";
import {AccountService} from "../../core/services/account.service";
import {ExtractionCategories, ExtractionColumn, ExtractionFilter, ExtractionType} from "./model/extraction.model";
import {GraphqlService} from "../../core/graphql/graphql.service";
import {FeatureCollection} from "geojson";
import {Fragments} from "../../trip/services/trip.queries";
import {SAVE_AS_OBJECT_OPTIONS} from "../../data/services/model/data-entity.model";
import {ReferentialUtils} from "../../core/services/model/referential.model";
import {SortDirection} from "@angular/material/sort";
import {FilterFn} from "../../shared/services/entity-service.class";
import {firstNotNilPromise} from "../../shared/observables";
import {AggregationType, IAggregationStrata} from "./model/aggregation-type.model";
import {ExtractionFragments, LoadExtractionTypesQuery} from "./extraction.service";
import {BaseGraphqlService} from "../../core/services/base-graphql-service.class";
import {isNil, isNotNil} from "../../shared/functions";
import {StatusIds} from "../../core/services/model/model.enum";
import {EntityUtils} from "../../core/services/model/entity.model";
import {environment} from "../../../environments/environment";


export const AggregationFragments = {
  aggregationType: gql`fragment AggregationTypeFragment on AggregationTypeVO {
    id
    category
    label
    name
    version
    sheetNames
    description
    documentation
    creationDate
    updateDate
    comments
    filter
    isSpatial
    statusId
    stratum {
      id
      updateDate
      isDefault
      sheetName
      spatialColumnName
      timeColumnName
      aggColumnName
      aggFunction
      techColumnName
    }
    recorderPerson {
      ...LightPersonFragment
    }
    recorderDepartment {
      ...LightDepartmentFragment
    }
  }
  ${Fragments.lightDepartment}
  ${Fragments.lightPerson}
  `
};

const LoadTypeQuery = gql`
  query AggregationType($id: Int!) {
    aggregationType(id: $id) {
      ...AggregationTypeFragment
    }
  }
  ${AggregationFragments.aggregationType}
`;

const LoadTypesQuery = gql`
  query AggregationTypes($filter: AggregationTypeFilterVOInput) {
    aggregationTypes(filter: $filter) {
      ...AggregationTypeFragment
    }
  }
  ${AggregationFragments.aggregationType}
`;

const LoadAggColumnsQuery: any = gql`
  query AggregationColumns($type: AggregationTypeVOInput, $sheet: String){
    aggregationColumns(type: $type, sheet: $sheet){
      ...ExtractionColumnFragment
      values
    }
  }
  ${ExtractionFragments.column}
`;

const LoadAggGeoJsonQuery = gql`
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

const LoadAggByTechQuery = gql`
  query AggregationTech(
    $type: AggregationTypeVOInput,
    $filter: ExtractionFilterVOInput,
    $strata: AggregationStrataVOInput,
    $sortBy: String, $sortDirection: String) {
    aggregationTech(type: $type, filter: $filter, strata: $strata, sortBy: $sortBy, sortDirection: $sortDirection) {
      data
    }
  }`;


const LoadAggMinMaxByTechQuery = gql`
  query AggregationTechMinMax(
    $type: AggregationTypeVOInput,
    $filter: ExtractionFilterVOInput,
    $strata: AggregationStrataVOInput) {
    aggregationTechMinMax(type: $type, filter: $filter, strata: $strata) {
      min
      max
    }
  }`;

const SaveAggregation: any = gql`
  mutation SaveAggregation($type: AggregationTypeVOInput, $filter: ExtractionFilterVOInput){
    saveAggregation(type: $type, filter: $filter){
      ...AggregationTypeFragment
    }
  }
  ${AggregationFragments.aggregationType}
`;

const DeleteAggregations: any = gql`
  mutation DeleteAggregations($ids:[Int]){
    deleteAggregations(ids: $ids)
  }
`;

export class AggregationTypeFilter {

  static searchFilter<T extends AggregationType>(f: AggregationTypeFilter): (T) => boolean {
    const filterFns: FilterFn<T>[] = [];

    // Filter by status
    if (f.statusIds) {
      filterFns.push((entity) => !!f.statusIds.find(v => entity.statusId === v));
    }

    // Filter by spatial
    if (isNotNil(f.isSpatial)) {
      filterFns.push((entity) => f.isSpatial === entity.isSpatial);
    }

    if (!filterFns.length) return undefined;

    return (entity) => !filterFns.find(fn => !fn(entity));
  }

  statusIds?: number[];
  isSpatial?: boolean;
}

@Injectable({providedIn: 'root'})
export class AggregationService extends BaseGraphqlService {

  constructor(
    protected graphql: GraphqlService,
    protected accountService: AccountService
  ) {
    super(graphql, environment);
  }

  /**
   * Load aggregated types
   */
  async loadAll(): Promise<AggregationType[]> {
    return await firstNotNilPromise(this.watchAll());
  }

  /**
   * Watch aggregated types
   */
  watchAll(dataFilter?: AggregationTypeFilter,
           options?: { fetchPolicy?: WatchQueryFetchPolicy }
  ): Observable<AggregationType[]> {
    if (this._debug) console.debug("[aggregation-service] Loading geo types...");

    const variables = {
      filter: dataFilter
    };

    return this.mutableWatchQuery<{ aggregationTypes: AggregationType[] }>({
      queryName: 'LoadAggregationTypes',
      query: LoadTypesQuery,
      arrayFieldName: 'aggregationTypes',
      insertFilterFn: AggregationTypeFilter.searchFilter(dataFilter),
      variables,
      error: {code: ErrorCodes.LOAD_EXTRACTION_GEO_TYPES_ERROR, message: "EXTRACTION.ERROR.LOAD_GEO_TYPES_ERROR"},
      fetchPolicy: options && options.fetchPolicy || 'network-only'
    })
      .pipe(
        map((data) => (data && data.aggregationTypes || []).map(AggregationType.fromObject))
      );
  }

  async load(id: number, options?: {
    fetchPolicy?: FetchPolicy
  }): Promise<AggregationType> {
    const data = await this.graphql.query<{ aggregationType: AggregationType }>({
      query: LoadTypeQuery,
      variables: {
        id
      },
      error: {code: ErrorCodes.LOAD_EXTRACTION_GEO_TYPES_ERROR, message: "EXTRACTION.ERROR.LOAD_GEO_TYPE_ERROR"},
      fetchPolicy: options && options.fetchPolicy || 'network-only'
    });

    return (data && data.aggregationType && AggregationType.fromObject(data.aggregationType)) || null;
  }

  /**
   * Load columns metadata
   * @param type
   * @param sheetName
   * @param options
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
    if (this._debug) console.debug("[aggregation-service] Loading columns... using options:", variables);
    const res = await this.graphql.query<{ aggregationColumns: ExtractionColumn[] }>({
      query: LoadAggColumnsQuery,
      variables: variables,
      error: {code: ErrorCodes.LOAD_EXTRACTION_ROWS_ERROR, message: "EXTRACTION.ERROR.LOAD_ROWS_ERROR"},
      fetchPolicy: options && options.fetchPolicy || 'network-only'
    });
    if (!res || !res.aggregationColumns) return null;

    const data = res.aggregationColumns.map(ExtractionColumn.fromObject);
    // Compute column index
    (data || []).forEach((c, index) => c.index = index);

    if (this._debug) console.debug(`[aggregation-service] Columns ${type.category} ${type.label} loaded in ${Date.now() - now}ms`, data);
    return data;
  }

  /**
   * Load aggregation as GeoJson
   */
  async loadGeoJson(type: AggregationType,
                    strata: IAggregationStrata,
                    offset: number,
                    size: number,
                    sortBy?: string,
                    sortDirection?: SortDirection,
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
      size: size >= 0 ? size : 1000
    };

    const res = await this.graphql.query<{ aggregationGeoJson: any }>({
      query: LoadAggGeoJsonQuery,
      variables: variables,
      error: {code: ErrorCodes.LOAD_EXTRACTION_GEO_JSON_ERROR, message: "EXTRACTION.ERROR.LOAD_GEO_JSON_ERROR"},
      fetchPolicy: options && options.fetchPolicy || 'network-only'
    });
    if (!res || !res.aggregationGeoJson) return null;

    return Object.assign({}, res.aggregationGeoJson);
  }

  async loadAggByTech(type: ExtractionType, strata: IAggregationStrata, filter: ExtractionFilter,
                      options?: { fetchPolicy?: FetchPolicy; }): Promise<Map<string, any>> {
    const variables: any = {
      type: {
        category: type.category,
        label: type.label
      },
      strata: strata,
      filter: filter
    };

    const res = await this.graphql.query<{ aggregationTech: {data: Map<string, any>} }>({
      query: LoadAggByTechQuery,
      variables: variables,
      error: {code: ErrorCodes.LOAD_EXTRACTION_TECH_ERROR, message: "EXTRACTION.ERROR.LOAD_TECH_ERROR"},
      fetchPolicy: options && options.fetchPolicy || 'network-only'
    });

    return (res && res.aggregationTech && res.aggregationTech.data) || null;
  }

  async loadAggMinMaxByTech(type: ExtractionType, strata: IAggregationStrata, filter: ExtractionFilter,
                            options?: { fetchPolicy?: FetchPolicy; }): Promise<{min: number; max: number; }> {
    const variables: any = {
      type: {
        category: type.category,
        label: type.label
      },
      strata: strata,
      filter: filter
    };

    const res = await this.graphql.query<{ aggregationTechMinMax: {min: number; max: number; } }>({
      query: LoadAggMinMaxByTechQuery,
      variables: variables,
      error: {code: ErrorCodes.LOAD_EXTRACTION_MIN_MAX_TECH_ERROR, message: "EXTRACTION.ERROR.LOAD_MIN_MAX_ERROR"},
      fetchPolicy: options && options.fetchPolicy || 'network-only'
    });

    return res && { min: 0, max: 0, ...res.aggregationTechMinMax} || null;
  }

  async save(entity: AggregationType,
             filter?: ExtractionFilter): Promise<AggregationType> {
    const now = Date.now();
    if (this._debug) console.debug("[aggregation-service] Saving aggregation...");

    // Make sure to have an entity
    entity = AggregationType.fromObject(entity);

    this.fillDefaultProperties(entity);

    const isNew = isNil(entity.id);

    // Transform to json
    const json = entity.asObject(SAVE_AS_OBJECT_OPTIONS);
    if (this._debug) console.debug("[aggregation-service] Using minify object, to send:", json);

    await this.graphql.mutate<{ saveAggregation: any }>({
      mutation: SaveAggregation,
      variables: {
        type: json,
        filter: filter
      },
      error: {code: ErrorCodes.SAVE_AGGREGATION_ERROR, message: "ERROR.SAVE_DATA_ERROR"},
      update: (cache, {data}) => {
        const savedEntity = data && data.saveAggregation;
        EntityUtils.copyIdAndUpdateDate(savedEntity, entity);
        //if (this._debug)
        console.debug(`[aggregation-service] Aggregation saved in ${Date.now() - now}ms`, savedEntity);

        // Convert into the extraction type
        const savedExtractionType = ExtractionType.fromObject(savedEntity).asObject({keepTypename: true});
        savedExtractionType.category = 'PRODUCT';

        // Insert into cached queries
        if (isNew) {
          // Insert as an extraction types
          this.insertIntoMutableCachedQuery(cache, {
            queryName: "LoadExtractionTypes",
            query: LoadExtractionTypesQuery,
            data: savedExtractionType
          });
        }

        // Update from cached queries
        else {
          // Remove, then insert, from extraction types
          this.removeFromMutableCachedQueryByIds(cache, {
            queryName: "LoadExtractionTypes",
            query: LoadExtractionTypesQuery,
            ids: savedEntity.id
          });
          this.insertIntoMutableCachedQuery(cache, {
            queryName: "LoadExtractionTypes",
            query: LoadExtractionTypesQuery,
            data: savedExtractionType
          });

          // Aggregation types
          this.insertIntoMutableCachedQuery(cache, {
            query: LoadTypesQuery,
            data: savedEntity
          });
        }


      }
    });

    return entity;
  }

  async delete(type: AggregationType): Promise<any> {
    if (!type || isNil(type.id)) throw Error('Missing type or type.id');

    const now = Date.now();
    if (this._debug) console.debug(`[aggregation-service] Deleting aggregation {id: ${type.id}'}`);

    await this.graphql.mutate<any>({
      mutation: DeleteAggregations,
      variables: {
        ids: [type.id]
      },
      update: (cache) => {

        // Remove from cache
        const cacheKey = {__typename: AggregationType.TYPENAME, id: type.id, label: type.label, category: ExtractionCategories.PRODUCT};
        cache.evict({ id: cache.identify(cacheKey)});
        cache.evict({ id: cache.identify({
            ...cacheKey,
            __typename: ExtractionType.TYPENAME
          })});

       if (this._debug) console.debug(`[aggregation-service] Aggregation deleted in ${Date.now() - now}ms`);
      }
    });
  }

  async deleteAll(entities: AggregationType[]): Promise<any> {
    await Promise.all((entities || [])
      .filter(t => t && isNotNil(t.id)).map(type => this.delete(type)));
  }

  /* -- protected methods  -- */

  protected fillDefaultProperties(entity: AggregationType) {

    // If new trip
    if (isNil(entity.id)) {

      // Compute label
      entity.label = `${entity.label}-${Date.now()}`;

      // Recorder department
      entity.recorderDepartment = ReferentialUtils.isNotEmpty(entity.recorderDepartment) ? entity.recorderDepartment : this.accountService.department;

      // Recorder person
      entity.recorderPerson = entity.recorderPerson || this.accountService.person;
    }

    entity.name = entity.name || `Aggregation on ${entity.label}`;
    entity.statusId = isNotNil(entity.statusId) ? entity.statusId : StatusIds.TEMPORARY;

    // Description
    if (!entity.description) {
      const person = this.accountService.person;
      entity.description = `Created by ${person.firstName} ${person.lastName}`;
    }
  }

  protected copyIdAndUpdateDate(source: AggregationType, target: AggregationType) {

    EntityUtils.copyIdAndUpdateDate(source, target);
  }
}
