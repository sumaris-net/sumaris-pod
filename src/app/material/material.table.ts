import {TableDataSource, ValidatorService} from "angular4-material-table";
import {Observable, Subject} from "rxjs";
import {DataService} from "../../services/data-service";
import {EventEmitter} from "@angular/core";
import { Trip, Entity } from "../../services/model";

export class AppTableDataSource<T extends Entity<T>, F> extends TableDataSource<T> {

  public onLoading: EventEmitter<boolean> = new EventEmitter<boolean>();
  /**
   * Creates a new TableDataSource instance, that can be used as datasource of `@angular/cdk` data-table.
   * @param data Array containing the initial values for the TableDataSource. If not specified, then `dataType` must be specified.
   * @param dataService A service to load and save data
   * @param dataType Type of data contained by the Table. If not specified, then `data` with at least one element must be specified.
   * @param validatorService Service that create instances of the FormGroup used to validate row fields.
   * @param config Additional configuration for table.
   */
  constructor(dataType: new () => T,
              private dataService: DataService<T, F>,
              validatorService?: ValidatorService,
              config?: {
                prependNewElements: boolean;
              }) {
    super([], dataType, validatorService, config);
  };

  load(offset: number,
       size: number,
       sortBy?: string,
       sortDirection?: string,
       filter?: F): Observable<T[]> {

    this.onLoading.emit(true);
    return this.dataService.loadAll(offset, size, sortBy, sortDirection, filter)
      .map(rows => {
        this.onLoading.emit(false);
        this.updateDatasource(rows);
        return rows
      },
      err => this.handleError(err, 'Unable to load rows'))
      ;
  }

  save(): Promise<any> {

    this.onLoading.emit(true);

    return new Promise((resolve, reject) => {
      let subscription = this.connect().first().subscribe(rows => {
        if (subscription) {
          subscription.unsubscribe();
        }
        const data: T[] = rows
          // Get row's currentData
          .map(r => (!r.editing || r.confirmEditCreate()) ? r.currentData as T : undefined)
          // Keep new or dirty rows
          .filter(t => (t && (t.id === undefined || t.dirty)));

          // Nothing to save
        if (!data.length) {
          this.onLoading.emit(false);
          return resolve(false); 
        }
        
        this.dataService.saveAll(data)
          .then(savedData => {
            this.onLoading.emit(false);
            console.log("[material.table] Saved data received after data service:", savedData);
            this.updateDatasource(savedData, {emitEvent: false});
            resolve(true);
          })
          .catch(err => {
            this.onLoading.emit(false);
            reject(err);
          });
      });
    });
  }

  public handleError(error: any, message: string): Observable<T[]> {
    console.error(error && error.message || error);
    this.onLoading.emit(false);
    return Observable.throw(error && error.message && error || message || error);
  }

  public handleErrorPromise(error: any, message: string) {
    console.error(error && error.message || error);
    this.onLoading.emit(false);
    throw error; // (error && error.code) ? error : (message || error);
  }

  public delete(id: number): void {
    var row = this.getRow(id);
    this.onLoading.emit(true);

    this.dataService.deleteAll([row.currentData])
      .then(() => {
        super.delete(id);
        this.onLoading.emit(false);
      })
      .catch(err => {
        console.error(err);
        this.onLoading.emit(false);
      });
  }
}
