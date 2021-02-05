import {Observable} from "rxjs";
import {FetchPolicy, WatchQueryFetchPolicy} from "@apollo/client/core";
import {SortDirection} from "@angular/material/sort";

export declare interface Page {
  offset: number;
  size: number;
  sortBy?: string;
  sortDirection?: SortDirection;
}

export declare interface LoadResult<T> {
  data: T[];
  total?: number;
  errors?: any[];
}

export declare type SuggestFn<T, F> = (value: any, filter?: F, sortBy?: string, sortDirection?: SortDirection) => Promise<T[] | LoadResult<T>>;

export declare interface SuggestService<T, F> {
  suggest: SuggestFn<T, F>;
}

export declare type FilterFn<T> = (data: T) => boolean;
export declare type FilterFnFactory<T, F> = (filter: F) => FilterFn<T>;

export declare interface EntityServiceLoadOptions {
  fetchPolicy?: FetchPolicy;
  trash?: boolean;
  [key: string]: any;
}

export declare interface IEntityService<T, O = EntityServiceLoadOptions> {

  load(
    id: number,
    opts?: O
  ): Promise<T>;

  save(data: T, opts?: any): Promise<T>;

  delete(data: T, opts?: any): Promise<any>;

  listenChanges(id: number, opts?: any): Observable<T | undefined>;
}

export declare interface EntitiesServiceWatchOptions {
  fetchPolicy?: WatchQueryFetchPolicy;
  trash?: boolean;
  withTotal?: boolean;
  toEntity?: boolean;
  [key: string]: any;
}

export declare interface IEntitiesService<T, F, O extends EntitiesServiceWatchOptions = EntitiesServiceWatchOptions> {

  watchAll(
    offset: number,
    size: number,
    sortBy?: string,
    sortDirection?: SortDirection,
    filter?: F,
    options?: O
  ): Observable<LoadResult<T>>;

  // TODO
  /*watchPage(
    page: Page<T>,
    filter?: F,
    options?: O
  ): Observable<LoadResult<T>>;*/

  saveAll(data: T[], options?: any): Promise<T[]>;

  deleteAll(data: T[], options?: any): Promise<any>;
}

export declare type LoadResultByPageFn<T> = (offset: number, size: number) => Promise<LoadResult<T>>;


export interface IEntityFullService<T, F, O extends EntitiesServiceWatchOptions & EntityServiceLoadOptions>
  extends IEntityService<T, O>, IEntitiesService<T, F, O>  {
}


