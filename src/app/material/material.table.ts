import { TableDataSource, ValidatorService } from "angular4-material-table";
import { Observable, Subject } from "rxjs";
import { DataService } from "../../services/data-service";
import { EventEmitter } from "@angular/core";
import { Trip, Entity } from "../../services/model";
import { FormGroup, AbstractControl } from "@angular/forms";
import { TableElement } from "angular4-material-table";

export class AppTableDataSource<T extends Entity<T>, F> extends TableDataSource<T> {

  public serviceOptions: any;
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
      serviceOptions?: any
    }) {
    super([], dataType, validatorService, config);
    this.serviceOptions = config && config.serviceOptions;
  };

  load(offset: number,
    size: number,
    sortBy?: string,
    sortDirection?: string,
    filter?: F): Observable<T[]> {

    this.onLoading.emit(true);
    return this.dataService.loadAll(offset, size, sortBy, sortDirection, filter, this.serviceOptions)
      .map(rows => {
        this.onLoading.emit(false);
        this.updateDatasource(rows);
        return rows
      },
        err => this.handleError(err, 'Unable to load rows'))
      ;
  }

  async save(): Promise<boolean> {

    console.debug("[material.table] Saving rows...");
    this.onLoading.emit(true);

    // Get row's currentData
    const rows = await this.getRows();
    const data: T[] = rows
      .map(r => {
        if (r.editing && !r.confirmEditCreate()) {
          this.logRowErrors(r);
          return undefined;
        }
        return r.currentData as T;
      });
    // Keep new or dirty rows
    const dataToSave = data.filter(t => (t && (t.id === undefined || t.dirty)));

    // Nothing to save
    if (!dataToSave.length) {
      console.debug("[material.table] No row to save");
      this.onLoading.emit(false);
      return false;
    }

    try {
      var savedData = await this.dataService.saveAll(dataToSave, this.serviceOptions);
      this.onLoading.emit(false);
      console.debug("[material.table] Saved data received after data service:", savedData);
      this.updateDatasource(data, { emitEvent: false });
      return true;
    }
    catch (error) {
      this.onLoading.emit(false);
      throw error;
    }
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

    this.dataService.deleteAll([row.currentData], this.serviceOptions)
      .then(() => {
        super.delete(id);
        this.onLoading.emit(false);
      })
      .catch(err => {
        console.error(err);
        this.onLoading.emit(false);
      });
  }

  /* -- private method -- */

  protected getRows(): Promise<TableElement<T>[]> {
    return new Promise<TableElement<T>[]>((resolve, reject) => {
      var subscription = this.connect().first().subscribe(rows => {
        resolve(rows);
        if (subscription) {
          subscription.unsubscribe();
        }
      });
    });
  }

  private logRowErrors(row: TableElement<T>): void {

    if (!row.validator.hasError) return;

    var errorsMessage = "";
    Object.getOwnPropertyNames(row.validator.controls)
      .forEach(key => {
        var control = row.validator.controls[key];
        if (control.invalid) {
          errorsMessage += "'" + key + "' (" + Object.getOwnPropertyNames(control.errors) + "),";
        }
      });

    if (errorsMessage.length) {
      console.debug("[material.table] Row (id=" + row.id + ") has errors: " + errorsMessage.slice(0, -1));
    }
  }
}
