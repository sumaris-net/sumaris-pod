import {BehaviorSubject, Observable, Subject} from "rxjs";
import {EntitiesService, isNotNil, LoadResult} from "../../core/core.module";
import {EntityUtils, IEntity} from "../../core/services/model/entity.model";
import {filter, mergeMap} from "rxjs/operators";
import {isNotEmptyArray} from "../functions";
import {FilterFnFactory} from "./entity-service.class";
import {SortDirection} from "@angular/material/sort";

export interface InMemoryEntitiesServiceOptions<T, F> {
  onSort?: (data: T[], sortBy?: string, sortDirection?: SortDirection) => T[];
  onLoad?: (data: T[]) => T[] | Promise<T[]>;
  onSave?: (data: T[]) => T[] | Promise<T[]>;
  equals?: (d1: T, d2: T) => boolean;
  filterFnFactory?: FilterFnFactory<T, F>;
  onFilter?: (data: T[], filter: F) => T[] | Promise<T[]>;
}

export class InMemoryEntitiesService<T extends IEntity<T>, F = any> implements EntitiesService<T, F> {

  private _dataSubject = new BehaviorSubject<LoadResult<T>>(undefined);

  private readonly _sortFn: (data: T[], sortBy?: string, sortDirection?: SortDirection) => T[];
  private readonly _onLoad: (data: T[]) => T[] | Promise<T[]>;
  private readonly _onSaveFn: (data: T[]) => T[] | Promise<T[]>;
  private readonly _equalsFn: (d1: T, d2: T) => boolean;
  private readonly _onFilterFn: (data: T[], filter: F) => T[] | Promise<T[]>;
  private readonly _filterFnFactory: FilterFnFactory<T, F>;

  protected data: T[];
  private _hiddenData: T[];

  // TODO add test  see sub-batches.modal.ts onLoadData & _hiddenData

  hasRankOrder = false;
  debug = false;
  dirty = false;

  set value(data: T[]) {
    if (this.data !== data) {
      this._hiddenData = [];
      this.data = data;
      if (this._dataSubject.observers.length) {
        this._dataSubject.next({data: data || [], total: data && data.length || 0});
      }
    }
    this.dirty = false;
  }

  get value(): T[] {
    return this.data;
  }

  constructor(
    protected dataType: new() => T,
    options?: InMemoryEntitiesServiceOptions<T, F>
  ) {
    options = {
      onSort: this.sort,
      onFilter: this.filter,
      equals: this.equals,
      ...options
    };

    this._sortFn = options.onSort;
    this._onLoad = options.onLoad;
    this._onSaveFn = options.onSave;
    this._equalsFn = options.equals;
    this._onFilterFn = options.onFilter;
    this._filterFnFactory = options.filterFnFactory;

    // Detect rankOrder on the entity class
    this.hasRankOrder = Object.getOwnPropertyNames(new dataType()).findIndex(key => key === 'rankOrder') !== -1;
  }

  watchAll(
    offset: number,
    size: number,
    sortBy?: string,
    sortDirection?: SortDirection,
    filterData?: F,
    options?: any
  ): Observable<LoadResult<T>> {

    if (!this.data) {
      console.warn("[memory-data-service] Waiting value to be set...");
    } else {
      // /!\ If already observed, then always create a copy of the original array
      // Because datasource will only update if the array changed
      this.data = this.data.slice(0);
      this._dataSubject.next({
        data: this.data,
        total: this.data.length
      });
    }

    return this._dataSubject
      .pipe(
        filter(isNotNil),
        mergeMap(async (res) => {

          // Apply sort
          let data = this._sortFn(res.data || [], sortBy, sortDirection) ;

          if (this._onLoad) {
            const promiseOrData = this._onLoad(data);
            data = ((promiseOrData instanceof Promise)) ? await promiseOrData : promiseOrData;
          }

          // Apply filter
          {
            const promiseOrData = this._onFilterFn(data, filterData);
            data = ((promiseOrData instanceof Promise)) ? await promiseOrData : promiseOrData;
          }

          return {
            data,
            total: res && res.total || data.length
          };
        })
      );
  }

  async saveAll(data: T[], options?: any): Promise<T[]> {
    if (!this.data) throw new Error("[memory-service] Could not save, because value not set");

    // Restore hidden data
    if (isNotEmptyArray(this._hiddenData))
      data = data.concat(this._hiddenData);

    if (this._onSaveFn) {
      const res = this._onSaveFn(data);
      data = ((res instanceof Promise)) ? await res : res;
    }

    this.data = data;
    this.dirty = true;
    return this.data;
  }

  async deleteAll(dataToRemove: T[], options?: any): Promise<any> {
    if (!this.data) throw new Error("[memory-service] Could not delete, because value not set");

    // Remove deleted item, from data
    const updatedData = this.data.filter(entity => {
      const shouldRemoved = dataToRemove.findIndex(entityToRemove => this._equalsFn(entityToRemove, entity)) !== -1;
      return !shouldRemoved;
    });
    const deleteCount = this.data.length - updatedData.length;
    if (deleteCount > 0) {
      const updatedTotal = this._dataSubject.getValue().total - deleteCount;
      this.data = updatedData;
      this.dirty = true;
      this._dataSubject.next({
        data: updatedData,
        total: updatedTotal
      });
    }

  }

  sort(data: T[], sortBy?: string, sortDirection?: SortDirection): T[] {
    // Replace id with rankOrder
    sortBy = this.hasRankOrder && (!sortBy || sortBy === 'id') ? 'rankOrder' : sortBy || 'id';

    // Execute the sort
    return EntityUtils.sort(data, sortBy, sortDirection);
  }

  filter(data: T[], _filter: F): T[] {

    // if filter is DataFilter instance, use its test function
    const testFn = this._filterFnFactory && this._filterFnFactory(_filter);
    if (testFn) {
      this._hiddenData = [];
      const filteredData = [];

      data.forEach(value => {
        if (testFn(value))
          filteredData.push(value);
        else
          this._hiddenData.push(value);
      });

      return filteredData;
    }

    // default, no filter
    return data;
  }

  connect(): Observable<LoadResult<T>> {
    return this._dataSubject;
  }

  protected equals(d1: T, d2: T): boolean {
    return d1 && d1.equals ? d1.equals(d2) : EntityUtils.equals(d1, d2, 'id');
  }
}

