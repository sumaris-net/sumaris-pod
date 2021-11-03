import { Injectable } from '@angular/core';
import { ApolloCache, FetchPolicy, gql, WatchQueryFetchPolicy } from '@apollo/client/core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

import {
  AccountService, BaseEntityGraphqlMutations, BaseEntityGraphqlQueries, BaseEntityService,
  BaseGraphqlService, EntitySaveOptions, EntityServiceLoadOptions,
  EntityUtils,
  firstNotNilPromise,
  GraphqlService,
  isNil,
  isNotNil,
  isNotNilOrBlank,
  LoadResult,
  Person, PlatformService,
  trimEmptyToNull,
} from '@sumaris-net/ngx-components';
import { ExtractionFilter, ExtractionFilterCriterion, ExtractionResult, ExtractionType } from './model/extraction-type.model';
import { DataCommonFragments } from '../../trip/services/trip.queries';
import { SortDirection } from '@angular/material/sort';
import { environment } from '../../../environments/environment';
import { DataEntityAsObjectOptions } from '../../data/services/model/data-entity.model';
import { MINIFY_OPTIONS } from '@app/core/services/model/referential.model';
import { ExtractionErrorCodes } from '@app/extraction/services/extraction.errors';
import { ExtractionTypeFilter } from '@app/extraction/services/filter/extraction-type.filter';
import { DocumentNode } from 'graphql';
import { EntitiesServiceWatchOptions } from '@sumaris-net/ngx-components/src/app/shared/services/entity-service.class';
import { DataRootEntityUtils } from '@app/data/services/model/root-data-entity.model';


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
  ${DataCommonFragments.lightDepartment}`,

  column: gql`fragment ExtractionColumnFragment on ExtractionTableColumnVO {
    label
    name
    columnName
    type
    description
    rankOrder
  }`
};


export const ExtractionQueries: BaseEntityGraphqlQueries & {
  loadRows: any;
  getFile: any;
} = {
  loadAll: gql`query ExtractionTypes {
      data: extractionTypes {
        ...ExtractionTypeFragment
      }
    }
    ${ExtractionFragments.type}`,

  loadRows: gql`query ExtractionRows($type: ExtractionTypeVOInput, $filter: ExtractionFilterVOInput, $offset: Int, $size: Int, $sortBy: String, $sortDirection: String){
    data: extractionRows(type: $type, filter: $filter, offset: $offset, size: $size, sortBy: $sortBy, sortDirection: $sortDirection){
      columns {
        ...ExtractionColumnFragment
      }
      rows
      total
    }
  }
  ${ExtractionFragments.column}`,

  getFile: gql`query ExtractionFile($type: ExtractionTypeVOInput, $filter: ExtractionFilterVOInput){
    data: extractionFile(type: $type, filter: $filter)
  }`
}

export const ExtractionMutation: BaseEntityGraphqlMutations = {
  save: gql`mutation SaveExtraction($data: ExtractionTypeVOInput, $filter: ExtractionFilterVOInput) {
    data: saveExtraction(type: $data, filter: $filter) {
      ...ExtractionTypeFragment
    }
  }
  ${ExtractionFragments.type}`
};

const fixWorkaroundDataFn = ({data, total}) => {
  // Workaround because saveAggregation() doest not add NEW extraction type correctly
  data = (data || []).filter(e => {
    if (isNil(e?.label)) {
      console.warn('[extraction-service] FIXME: Invalid extraction type (no label)... bad cache insertion in saveAggregation() ?');
      return false;
    }
    return true;
  });
  return {data, total}
};

@Injectable({providedIn: 'root'})
export class ExtractionService extends BaseEntityService<ExtractionType, ExtractionTypeFilter> {



  constructor(
    protected graphql: GraphqlService,
    protected platformService: PlatformService,
    protected accountService: AccountService,
  ) {
    super(graphql, platformService,
      ExtractionType, ExtractionTypeFilter,
      {
        queries: ExtractionQueries,
        mutations: ExtractionMutation
      });
  }

  loadAll(offset: number, size: number, sortBy?: string, sortDirection?: SortDirection,
          filter?: Partial<ExtractionTypeFilter>,
          opts?: EntityServiceLoadOptions & { query?: any; debug?: boolean; withTotal?: boolean }): Promise<LoadResult<ExtractionType>> {
    return super.loadAll(offset, size, sortBy, sortDirection, filter, {
      ...opts,
      withTotal: false // Always false (loadAllWithTotal query not defined yet)
    })
      .then(fixWorkaroundDataFn);
  }

  /**
   * Watch extraction types
   */
  watchAll(
    offset: number, size: number,
    sortAttribute?: string,
    sortDirection?: SortDirection,
    filter?: ExtractionTypeFilter,
    opts?: EntitiesServiceWatchOptions & {query?: any}): Observable<LoadResult<ExtractionType>> {

    return super.watchAll(offset, size, sortAttribute, sortDirection, filter, {
      ...opts,
      withTotal: false // Always false (loadAllWithTotal query not defined yet)
    }).pipe(map(fixWorkaroundDataFn));
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
      filter
    };

    const now = Date.now();
    if (this._debug) console.debug("[extraction-service] Loading rows... using options:", variables);
    const res = await this.graphql.query<{ data: ExtractionResult }>({
      query: ExtractionQueries.loadRows,
      variables,
      error: {code: ExtractionErrorCodes.LOAD_EXTRACTION_ROWS_ERROR, message: "EXTRACTION.ERROR.LOAD_ROWS_ERROR"},
      fetchPolicy: options && options.fetchPolicy || 'no-cache'
    });
    if (!res || !res.data) return null;
    const data = ExtractionResult.fromObject(res.data);

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
    const res = await this.graphql.query<{ data: string }>({
      query: ExtractionQueries.getFile,
      variables: variables,
      error: {code: ExtractionErrorCodes.DOWNLOAD_EXTRACTION_FILE_ERROR, message: "EXTRACTION.ERROR.DOWNLOAD_FILE_ERROR"},
      fetchPolicy: options && options.fetchPolicy || 'network-only'
    });
    const fileUrl = res && res.data;
    if (!fileUrl) return undefined;

    if (this._debug) console.debug(`[extraction-service] Extraction ${type.category} ${type.label} done in ${Date.now() - now}ms: ${fileUrl}`, res);

    return fileUrl;
  }


  async save(entity: ExtractionType, opts?: EntitySaveOptions & {
    filter: ExtractionFilter
  }): Promise<ExtractionType> {
    const filter = opts && opts.filter;
    if (this._debug) console.debug("[extraction-service] Saving extraction...", entity, filter);

    this.fillDefaultProperties(entity);

    const json = this.asObject(entity);

    const isNew = isNil(entity.id) || entity.id < 0; // Exclude live types

    await this.graphql.mutate<{ data: any }>({
      mutation: ExtractionMutation.save,
      variables: { data: json, filter },
      error: {code: ExtractionErrorCodes.LOAD_EXTRACTION_ROWS_ERROR, message: "EXTRACTION.ERROR.LOAD_ROWS_ERROR"},
      update: (cache, {data}) => {
        const savedEntity = data && data.data;

        this.copyIdAndUpdateDate(savedEntity, entity);

        // Insert into cache
        if (isNew) {
          this.insertIntoCache(cache, savedEntity);
        }
      }
    });

    return entity;
  }

  insertIntoCache(cache: ApolloCache<{data: any}>, entity: ExtractionType) {
    if (!entity || isNil(entity.id)) throw new Error('Extraction type (with an id) is required, to insert into the cache.');

    console.info('[extraction-service] Inserting into cache:', entity);
    this.insertIntoMutableCachedQueries(cache, {
      queries: this.getLoadQueries(),
      data: entity
    });
  }

  updateCache(cache: ApolloCache<{data: any}>, entity: ExtractionType) {
    if (!entity || isNil(entity.id)) throw new Error('Extraction type (with an id) is required, to update the cache.');

    console.info('[extraction-service] Updating cache:', entity);

    // Remove, then insert, from extraction types
    const exists = this.removeFromMutableCachedQueriesByIds(cache, {
      queries: this.getLoadQueries(),
      ids: entity.id
    }) > 0;
    if (exists) {
      this.insertIntoMutableCachedQueries(cache, {
        queries: this.getLoadQueries(),
        data: entity
      });
    }
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


  copyIdAndUpdateDate(source: ExtractionType | undefined, target: ExtractionType) {
    if (!source) return;

    EntityUtils.copyIdAndUpdateDate(source, target);

    // Copy category and label
    target.label = source.label;
    target.category = source.category;

  }
}
