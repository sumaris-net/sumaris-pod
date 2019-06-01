import {BehaviorSubject, Observable} from "rxjs-compat";
import {Entity, isNil, isNotNil, LoadResult, TableDataService} from "../../core/core.module";
import {filter, first} from "rxjs/operators";
import {IEntityWithMeasurement, MeasurementUtils, PMFM_ID_REGEXP} from "../../trip/services/model/measurement.model";
import {EntityUtils} from "../../core/services/model";
import {ValidatorService} from "angular4-material-table";
import {PmfmStrategy} from "../../referential/services/model";
import {Injector, Input} from "@angular/core";

export class InMemoryTableDataService<T extends IEntityWithMeasurement<T>, F> implements TableDataService<T, F> {

  private _dataSubject = new BehaviorSubject<LoadResult<T>>({data: [], total: 0});
  private _dirty = false;

  hasRankOrder = false;
  debug = false;
  data: T[];

  set value(data: T[]) {
    if (this.data !== data) {
      this.data = data;
      this._dirty = false;
      this._dataSubject.next({data: data || [], total: data && data.length || 0});
    }
  }

  get value(): T[] {
    return this.data;
  }

  get dataSubject(): BehaviorSubject<LoadResult<T>> {
    return this._dataSubject;
  }

  constructor(
    protected dataType: new() => T
  ) {
    // Detect rankOrder on the entity class
    this.hasRankOrder = Object.getOwnPropertyNames(new dataType()).findIndex(key => key === 'rankOrder') !== -1;
  }

  watchAll(
    offset: number,
    size: number,
    sortBy?: string,
    sortDirection?: string,
    selectionFilter?: any,
    options?: any
  ): Observable<LoadResult<T>> {

    if (!this.data){
      console.warn("[memory-service] Waiting value to be set, to be able to send rows...");
    }

    // If dirty: save first
    else if (this._dirty) {
      this.saveAll(this.value)
        .then(saved => {
          if (saved) {
            this.watchAll(offset, size, sortBy, sortDirection, selectionFilter, options);
            this._dirty = true; // restore previous state
          }
        });
    }

    else if (this.data) {
      // Apply sort
      this.sort(this.data, sortBy, sortDirection);

      this._dataSubject.next({
        data: this.data,
        total: this.data.length
      });
    }

    return this._dataSubject;
  }

  async saveAll(data: T[], options?: any): Promise<T[]> {
    if (!this.data) throw new Error("[memory-service] Could not save table: value not set (or not started)");
    this._dirty = false;
    return this.data;
  }

  async deleteAll(data: T[], options?: any): Promise<any> {
    this._dirty = true;

    if (this.data) {
      // Remove deleted item, from data
      this.data = this.data.reduce((res, item) => {
        const keep = data.findIndex(i => i.equals(item)) === -1;
        return keep ? res.concat(item) : res;
      }, []);
    }
  }

  protected sort(data: T[], sortBy?: string, sortDirection?: string): T[] {

    // Replace id with rankOrder
    sortBy = this.hasRankOrder && (!sortBy || sortBy === 'id') ? 'rankOrder' : sortBy || 'id';
    const after = (!sortDirection || sortDirection === 'asc') ? 1 : -1;
    return data.sort((a, b) => {
      const valueA = EntityUtils.getPropertyByPath(a, sortBy);
      const valueB = EntityUtils.getPropertyByPath(b, sortBy);
      return valueA === valueB ? 0 : (valueA > valueB ? after : (-1 * after));
    });
  }
}

