import {BehaviorSubject, Observable} from "rxjs-compat";
import {isNil, isNotNil, LoadResult, TableDataService} from "../../core/core.module";
import {filter, first} from "rxjs/operators";
import {IEntityWithMeasurement, MeasurementUtils, PMFM_ID_REGEXP} from "../../trip/services/model/measurement.model";
import {EntityUtils} from "../../core/services/model";
import {PmfmStrategy} from "../../referential/services/model";
import {EventEmitter, Injector, Input} from "@angular/core";
import {ProgramService} from "../../referential/referential.module";

export class MeasurementsTableDataService<T extends IEntityWithMeasurement<T>, F> implements TableDataService<T, F> {

  private _program: string;
  private _acquisitionLevel: string;
  private _dataSubject = new BehaviorSubject<LoadResult<T>>({data: [], total: 0});
  private _onRefreshPmfms = new EventEmitter<any>();
  private _dirty = false;

  protected programService: ProgramService;

  loadingPmfms = true;
  pmfms = new BehaviorSubject<PmfmStrategy[]>(undefined);
  hasRankOrder = false;
  debug = false;
  data: T[];

  set value(data: T[]) {
    if (this.data !== data) {
      this.data = data;
      this._dirty = false;
      this.dataSubject.next({data: data || [], total: data && data.length || 0});
    }
  }

  get value(): T[] {
    return this.data;
  }

  @Input()
  set program(value: string) {
    if (this._program !== value && isNotNil(value)) {
      this._program = value;
      if (!this.loadingPmfms) this._onRefreshPmfms.emit('set program');
    }
  }

  get program(): string {
    return this._program;
  }

  @Input()
  set acquisitionLevel(value: string) {
    if (this._acquisitionLevel !== value && isNotNil(value)) {
      this._acquisitionLevel = value;
      if (!this.loadingPmfms) this._onRefreshPmfms.emit();
    }
  }

  get acquisitionLevel(): string {
    return this._acquisitionLevel;
  }

  get dataSubject(): BehaviorSubject<LoadResult<T>> {
    return this._dataSubject;
  }

  constructor(
    injector: Injector,
    protected dataType: new() => T,
    protected delegate: TableDataService<T, F>
  ) {

    this.programService = injector.get(ProgramService);

    // Detect rankOrder on the entity class
    this.hasRankOrder = Object.getOwnPropertyNames(new dataType()).findIndex(key => key === 'rankOrder') !== -1;

    this._onRefreshPmfms.asObservable().subscribe(() => this.refreshPmfms());
  }

  watchAll(
    offset: number,
    size: number,
    sortBy?: string,
    sortDirection?: string,
    selectionFilter?: any,
    options?: any
  ): Observable<LoadResult<T>> {

    // If dirty: save first
    if (this._dirty) {
      this.saveAll(this.value)
        .then(saved => {
          if (saved) {
            this.watchAll(offset, size, sortBy, sortDirection, selectionFilter, options);
            this._dirty = true; // restore previous state
          }
        });
    } else {
      this.pmfms
        .pipe(
          filter(isNotNil),
          first()
        )
        .subscribe(async (pmfms) => {

          // Replace sort in Pmfm by a valid path
          if (sortBy && PMFM_ID_REGEXP.test(sortBy)) {
            sortBy = 'measurementValues.' + sortBy;
          }

          const res = await this.delegate.watchAll(offset, size, sortBy, sortDirection, selectionFilter, options).toPromise();

          // Transform entities into object array
          const data = (res.data || []).map(t => {
            const json = t.asObject();
            json.measurementValues = MeasurementUtils.normalizeFormValues(t.measurementValues, pmfms);
            return json;
          });

          // Sort
          this.sort(data, sortBy, sortDirection);

          this._dataSubject.next({
            data: data,
            total: res && res.total || data.length
          });
        });
    }

    return this._dataSubject;
  }

  async saveAll(data: T[], options?: any): Promise<T[]> {

    if (this.debug) console.debug("[meas-service] converting measurement values before saving...");
    const pmfms = this.pmfms.getValue() || [];
    const dataToSaved = data.map(json => {
      const entity = new this.dataType();
      entity.fromObject(json);
      entity.measurementValues = MeasurementUtils.toEntityValues(json.measurementValues, pmfms);
      return entity;
    });

    return this.delegate.saveAll(dataToSaved);
  }

  deleteAll(data: T[], options?: any): Promise<any> {
    return this.delegate.deleteAll(data);
  }

  /* -- protected methods -- */

  protected async refreshPmfms(): Promise<PmfmStrategy[]> {
    if (isNil(this._program) || isNil(this._acquisitionLevel)) return undefined;

    this.loadingPmfms = true;

    // Load pmfms
    const pmfms = (await this.programService.loadProgramPmfms(
      this._program,
      {
        acquisitionLevel: this._acquisitionLevel
      })) || [];

    if (!pmfms.length && this.debug) {
      console.debug(`[meas-table] No pmfm found (program=${this.program}, acquisitionLevel=${this._acquisitionLevel}). Please fill program's strategies !`);
    }

    this.loadingPmfms = false;

    this.pmfms.next(pmfms);

    return pmfms;
  }

  protected sort(data: T[], sortBy?: string, sortDirection?: string): T[] {
    if (sortBy && PMFM_ID_REGEXP.test(sortBy)) {
      sortBy = 'measurementValues.' + sortBy;
    }
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

