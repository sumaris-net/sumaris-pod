import {Observable} from "rxjs-compat";
import {Trip} from "../../trip/services/model/trip.model";

export declare interface LoadResult<T> {
  data: T[];
  total?: number;
}
export declare interface SuggestionDataService<T> {
  suggest(value: any, options?: any): Promise<T[]>;
}
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

export declare interface EditorDataService<T, F> {

  load(
    id: number,
    options?: any
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

