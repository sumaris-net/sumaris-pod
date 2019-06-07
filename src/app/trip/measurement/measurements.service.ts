import {BehaviorSubject, Observable} from "rxjs-compat";
import {isNil, isNotNil, LoadResult, TableDataService} from "../../core/core.module";
import {filter, first, map, switchMap} from "rxjs/operators";
import {IEntityWithMeasurement, MeasurementUtils, PMFM_ID_REGEXP} from "../../trip/services/model/measurement.model";
import {EntityUtils} from "../../core/services/model";
import {PmfmStrategy} from "../../referential/services/model";
import {EventEmitter, Injector, Input} from "@angular/core";
import {ProgramService} from "../../referential/referential.module";

export class MeasurementsDataService<T extends IEntityWithMeasurement<T>, F> implements TableDataService<T, F> {

  private _program: string;
  private _acquisitionLevel: string;
  private _onRefreshPmfms = new EventEmitter<any>();

  protected programService: ProgramService;

  loadingPmfms = false;
  $pmfms = new BehaviorSubject<PmfmStrategy[]>(undefined);
  hasRankOrder = false;
  debug = false;

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

  return this.$pmfms
    .pipe(
      filter(isNotNil),
      first(),
      switchMap((pmfms) => {
        let cleanSortBy = sortBy;

        // Do not apply sortBy to delegated service, when sort on a pmfm
        let sortPmfm: PmfmStrategy;
        if (cleanSortBy && PMFM_ID_REGEXP.test(cleanSortBy)) {
          sortPmfm = pmfms.find(pmfm => pmfm.pmfmId === parseInt(sortBy));
          // A pmfm was found, do not apply the sort here
          if (sortPmfm) cleanSortBy = undefined;
        }

        return this.delegate.watchAll(offset, size, cleanSortBy, sortDirection, selectionFilter, options)
          .pipe(
            map((res) => {

              // Prepare measurement values for reactive form
              (res.data || []).forEach(entity => {
                entity.measurementValues = MeasurementUtils.normalizeFormValues(entity.measurementValues, pmfms);
              });

              // Apply sort on pmfm
              if (sortPmfm) {
                // Compute attributes path
                cleanSortBy = 'measurementValues.' + sortBy;
                if (sortPmfm.type === 'qualitative_value') {
                  cleanSortBy += '.label';
                }
                // Execute a simple sort
                res.data = EntityUtils.sort(res.data, cleanSortBy, sortDirection);
              }

              return res;
            })
          );
      })
    );
  }

  async saveAll(data: T[], options?: any): Promise<T[]> {

    if (this.debug) console.debug("[meas-service] converting measurement values before saving...");
    const pmfms = this.$pmfms.getValue() || [];
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

    this.$pmfms.next(pmfms);

    return pmfms;
  }

  // public sort(data: T[], sortBy?: string, sortDirection?: string): T[] {
  //   if (sortBy && PMFM_ID_REGEXP.test(sortBy)) {
  //     sortBy = 'measurementValues.' + sortBy;
  //   }
  //
  //   // Replace id with rankOrder
  //   sortBy = this.hasRankOrder && (!sortBy || sortBy === 'id') ? 'rankOrder' : sortBy || 'id';
  //
  //   // Execute the sort
  //   return EntityUtils.sort(data, sortBy, sortDirection);
  // }
}

