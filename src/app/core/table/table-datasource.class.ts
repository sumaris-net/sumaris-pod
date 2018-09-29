import { TableDataSource, ValidatorService } from "angular4-material-table";
import { Observable } from "rxjs";
import { map } from "rxjs/operators";
import { DataService } from "../services/data-service.class";
import { EventEmitter } from "@angular/core";
import { Entity } from "../services/model";
import { TableElement } from "angular4-material-table";
import { ErrorCodes } from "../services/errors";
import { AppFormUtils } from "../form/form.utils";

export class AppTableDataSource<T extends Entity<T>, F> extends TableDataSource<T> {

  protected _debug = false;

  public serviceOptions: any;
  public onLoading = new EventEmitter<boolean>();

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
      serviceOptions?: {
        saveOnlyDirtyRows?: boolean;
      } | any
    }) {
    super([], dataType, validatorService, config);
    this.serviceOptions = config && config.serviceOptions;
    this._debug = true;
  };

  load(offset: number,
    size: number,
    sortBy?: string,
    sortDirection?: string,
    filter?: F): Observable<T[]> {

    this.onLoading.emit(true);
    return this.dataService.loadAll(offset, size, sortBy, sortDirection, filter, this.serviceOptions)
      .catch(err => this.handleError(err, 'Unable to load rows'))
      .map(data => {
        this.onLoading.emit(false);
        if (this._debug) console.debug("[table-datasource] Updating datasource...", data);
        this.updateDatasource(data);
        return data
      });
  }

  async save(): Promise<boolean> {

    if (this._debug) console.debug("[table-datasource] Saving rows...");
    this.onLoading.emit(true);

    try {
      // Get all rows
      const rows = await this.getRows();

      // Finish editing all rows, and log row in error
      const invalidRows = rows.filter(row => row.editing && !row.confirmEditCreate());
      if (invalidRows.length) {
        // log errors
        if (this._debug) invalidRows.forEach(this.logRowErrors);
        // Stop with an error
        throw { code: ErrorCodes.TABLE_INVALID_ROW_ERROR, message: 'ERROR.TABLE_INVALID_ROW_ERROR' };
      }

      let data: T[] = rows.map(row => row.currentData);
      if (this._debug) console.log("[table-datasource] Data to save:", data);

      // Filter to keep only dirty row
      const dataToSave = (this.serviceOptions && this.serviceOptions.saveOnlyDirtyRows) ?
        data.filter(t => (t && (t.id === undefined || t.dirty))) : data;

      // If no data to save: exit
      if (!dataToSave.length) {
        if (this._debug) console.debug("[table-datasource] No row to save");
        return false;
      }


      var savedData = await this.dataService.saveAll(dataToSave, this.serviceOptions);
      if (this._debug) console.debug("[table-datasource] Data saved. Updated data received by service:", savedData);
      if (this._debug) console.debug("[table-datasource] Updating datasource...", data);
      this.updateDatasource(data, { emitEvent: false });
      return true;
    }
    catch (error) {
      if (this._debug) console.error("[table-datasource] Error while saving: " + error && error.message || error);
      throw error;
    }
    finally {
      // Always update the loading indicator
      this.onLoading.emit(false);
    }
  }

  confirmCreate(row) {
    if (row.validator.valid && row.validator.dirty) {
      AppFormUtils.copyForm2Entity(row.validator, row.currentData);
      row.currentData.dirty = true;
    }
    return super.confirmCreate(row);
  };

  confirmEdit(row) {
    if (row.validator.valid && row.validator.dirty) {
      AppFormUtils.copyForm2Entity(row.validator, row.currentData);
      row.currentData.dirty = true;
    }
    return super.confirmEdit(row);
  };

  startEdit(row) {
    if (this._debug) console.warn("[table-datasource] Start to edit row", row);
    row.startEdit();
    AppFormUtils.copyEntity2Form(row.currentData, row.validator);
  };

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

  public getRows(): Promise<TableElement<T>[]> {
    return new Promise((resolve) => {
      this.connect().first().subscribe(rows => {
        resolve(rows);
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
          errorsMessage += "'" + key + "' (" + (control.errors ? Object.getOwnPropertyNames(control.errors) : 'unkown error') + "),";
        }
      });

    if (errorsMessage.length) {
      console.error("[table-datasource] Row (id=" + row.id + ") has errors: " + errorsMessage.slice(0, -1));
    }
  }
}
