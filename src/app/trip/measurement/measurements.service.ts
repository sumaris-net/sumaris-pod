import {BehaviorSubject, isObservable, Observable} from "rxjs";
import {isNil, isNotNil, LoadResult, IEntitiesService} from "../../core/core.module";
import {filter, first, map, switchMap} from "rxjs/operators";
import {
  IEntityWithMeasurement,
  MeasurementValuesUtils
} from "../services/model/measurement.model";
import {EntityUtils} from "../../core/services/model/entity.model";
import {PmfmStrategy} from "../../referential/services/model/pmfm-strategy.model";
import {Directive, EventEmitter, Injector, Input, OnDestroy} from "@angular/core";
import {ProgramService} from "../../referential/services/program.service";
import {firstNotNilPromise} from "../../shared/observables";
import {PMFM_ID_REGEXP} from "../../referential/services/model/pmfm.model";
import {SortDirection} from "@angular/material/sort";

@Directive()
export class MeasurementsDataService<T extends IEntityWithMeasurement<T>, F>
    implements IEntitiesService<T, F> {

  private _program: string;
  private _acquisitionLevel: string;
  private _onRefreshPmfms = new EventEmitter<any>();
  private _delegate: IEntitiesService<T, F>;

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

  @Input()
  set pmfms(pmfms: Observable<PmfmStrategy[]> | PmfmStrategy[]) {
    this.setPmfms(pmfms);
  }

  @Input() set delegate(value: IEntitiesService<T, F>) {
    this._delegate = value;
  }

  get delegate(): IEntitiesService<T, F> {
    return this._delegate;
  }

  constructor(
    injector: Injector,
    protected dataType: new() => T,
    delegate?: IEntitiesService<T, F>,
    protected options?: {
      mapPmfms: (pmfms: PmfmStrategy[]) => PmfmStrategy[] | Promise<PmfmStrategy[]>;
    }) {

    this._delegate = delegate;
    this.programService = injector.get(ProgramService);

    // Detect rankOrder on the entity class
    this.hasRankOrder = Object.getOwnPropertyNames(new dataType()).findIndex(key => key === 'rankOrder') !== -1;

    this._onRefreshPmfms.subscribe(() => this.refreshPmfms());
  }

  close() {
    this.$pmfms.unsubscribe();
    this._onRefreshPmfms.unsubscribe();
  }

  watchAll(
    offset: number,
    size: number,
    sortBy?: string,
    sortDirection?: SortDirection,
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
                res.data = (res.data || []).slice();
                res.data.forEach(entity => MeasurementValuesUtils.normalizeEntityToForm(entity, pmfms));

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
      const entity = new this.dataType() as T;
      entity.fromObject(json);
      // Adapt measurementValues to entity, but :
      // - keep the original JSON object measurementValues, because may be still used (e.g. in table without validator, in row.currentData)
      // - keep extra pmfm's values, because table can have filtered pmfms, to display only mandatory PMFM (e.g. physical gear table)
      entity.measurementValues = Object.assign({}, json.measurementValues, MeasurementValuesUtils.normalizeValuesToModel(json.measurementValues, pmfms));
      return entity;
    });

    return this.delegate.saveAll(dataToSaved, options);
  }

  deleteAll(data: T[], options?: any): Promise<any> {
    return this.delegate.deleteAll(data, options);
  }



  /* -- protected methods -- */

  protected async refreshPmfms(): Promise<PmfmStrategy[]> {
    if (isNil(this._program) || isNil(this._acquisitionLevel)) return undefined;

    this.loadingPmfms = true;

    // Load pmfms
    let pmfms = (await this.programService.loadProgramPmfms(
      this._program,
      {
        acquisitionLevel: this._acquisitionLevel
      })) || [];

    if (!pmfms.length && this.debug) {
      console.debug(`[meas-service] No pmfm found (program=${this.program}, acquisitionLevel=${this._acquisitionLevel}). Please fill program's strategies !`);
    }

    pmfms = await this.setPmfms(pmfms);

    return pmfms;
  }

  protected async setPmfms(pmfms: PmfmStrategy[] | Observable<PmfmStrategy[]>): Promise<PmfmStrategy[]> {
    if (!pmfms) return undefined; // skip

    // Wait loaded
    if (isObservable<PmfmStrategy[]>(pmfms)) {
      if (this.debug) console.debug("[meas-service] setPmfms(): waiting pmfms observable to emit...");
      pmfms = await firstNotNilPromise(pmfms);
    }

    // Map
    if (this.options && this.options.mapPmfms) {
      const res = this.options.mapPmfms(pmfms);
      pmfms = (res instanceof Promise) ? await res : res;
    }

    if (pmfms instanceof Array && pmfms !== this.$pmfms.getValue()) {

      // Apply
      this.loadingPmfms = false;
      this.$pmfms.next(pmfms);
    }

    return pmfms;
  }
}

