import {BehaviorSubject, Observable} from "rxjs";
import {FetchPolicy} from "apollo-client";
import {isNil, isNotNil} from "../functions";
import {SortDirection} from "@angular/material/sort";

export declare interface LoadPage<T> {
  offset: number;
  size: number;
  sortBy?: keyof T;
  sortDirection?: 'asc'|'desc';
}

export declare interface LoadResult<T> {
  data: T[];
  total?: number;
}

export declare type SuggestFn<T, F> = (value: any, filter?: F, sortBy?: keyof T, sortDirection?: SortDirection) => Promise<T[]>;

export declare interface SuggestionDataService<T, F> {
  suggest: SuggestFn<T, F>;
}

export declare type FilterFn<T> = (data: T) => boolean;
export declare type FilterFnFactory<T, F> = (filter: F) => FilterFn<T>;

export declare interface DataService<T, F> {

  loadAll(
    offset: number,
    size: number,
    sortBy?: string,
    sortDirection?: string,
    filter?: F,
    options?: any
  ): Promise<LoadResult<T>>;

  saveAll(data: T[], options?: any): Promise<T[]>;

  deleteAll(data: T[], options?: any): Promise<any>;
}

export declare interface EditorDataServiceLoadOptions {
  fetchPolicy?: FetchPolicy;
  [key: string]: any;
}

export declare interface EditorDataService<T, F = any, O = EditorDataServiceLoadOptions> {

  load(
    id: number,
    options?: O
  ): Promise<T>;

  save(data: T, options?: any): Promise<T>;

  delete(data: T, options?: any): Promise<any>;

  listenChanges(id: number, options?: any): Observable<T | undefined>;
}

export declare interface TableDataService<T, F> {

  watchAll(
    offset: number,
    size: number,
    sortBy?: string,
    sortDirection?: string,
    filter?: F,
    options?: any
  ): Observable<LoadResult<T>>;

  saveAll(data: T[], options?: any): Promise<T[]>;

  deleteAll(data: T[], options?: any): Promise<any>;
}

export declare interface ImportDataService<T, F> {
  executeImport(opts?: {
    maxProgression?: number;
  }): Observable<number>;
}

export declare type LoadResultByPageFn<T> = (offset: number, size: number) => Promise<LoadResult<T>>;

export async function fetchAllPagesWithProgress<T>(
  loadPageFn: LoadResultByPageFn<T>,
  progression: BehaviorSubject<number>,
  progressionStep?: number,
  onPageLoaded?: (pageResult: LoadResult<T>) => any,
  logPrefix?: string): Promise<LoadResult<T>> {

  let total, loopCount, loopProgressionStepSize, loopDataCount: number;
  const fetchSize = 1000;
  let offset = 0;
  let data: T[] = [];
  do {
    console.debug(`${logPrefix || '[??]'} Fetching page #${offset / fetchSize}... (progressionStep=${progressionStep}), loopProgressionStepSize=${loopProgressionStepSize}, loopCount=${loopCount})`);

    // Get some items, using paging
    const res = await loadPageFn(offset, fetchSize);
    data = data.concat(res.data);
    loopDataCount = res.data && res.data.length || 0;
    offset += loopDataCount;

    // Set total count (only if not already set)
    if (isNil(total) && isNotNil(res.total)) {
      total = res.total;
      loopCount = Math.round(res.total / fetchSize + 0.5); // Round to next integer
      loopProgressionStepSize = progressionStep ? progressionStep / loopCount : undefined /*if 0 reset to undefined*/;
    }

    // Increment progression on each iteration (if possible)
    if (isNotNil(loopProgressionStepSize)) {
      progression.next(progression.getValue() + loopProgressionStepSize);
    }

    if (onPageLoaded) onPageLoaded(res);

  } while (loopDataCount > 0 && ((isNil(total) && loopDataCount === fetchSize) || (isNotNil(total) && (offset < total))));

  // Complete progression to reach progressionStep value (because of round)
  if (isNotNil(loopProgressionStepSize)) {

    // If total return by loadAll was bad !
    if (isNotNil(total) && data.length < total) {
      console.warn(`A function loadAll() returned a bad total (less than all page's data)! Expected ${total} but fetch ${data.length}`);
      total = data.length;
      loopCount = Math.round(data.length / fetchSize + 0.5);
    }

    const lastProgressionStep = progressionStep - (loopProgressionStepSize * loopCount);
    if (lastProgressionStep !== 0) {
      progression.next(progression.getValue() + progressionStep);
    }
  }
  // Or increment once
  else if (progressionStep) {
    progression.next(progression.getValue() + progressionStep);
  }

  return {
    data,
    total
  };
}
