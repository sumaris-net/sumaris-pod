import { AfterViewInit, Directive, Injector, Input, OnInit, ViewChild } from '@angular/core';
import {
  AppFormUtils,
  changeCaseToUnderscore,
  EntitiesServiceWatchOptions,
  Entity,
  EntityFilter,
  FileEvent, FileResponse, FilesUtils, firstNotNilPromise,
  FormFieldDefinition,
  FormFieldType,
  IEntitiesService, isEmptyArray, isNotNil, joinProperties, sleep,
  StartableService, SuggestFn, suggestFromArray, UploadFile
} from '@sumaris-net/ngx-components';
import { AppBaseTable, BASE_TABLE_SETTINGS_ENUM, BaseTableOptions } from '@app/shared/table/base.table';
import { FormBuilder } from '@angular/forms';
import { debounceTime, filter, flatMap, map, mergeMap, switchMap, tap } from 'rxjs/operators';
import { IonInfiniteScroll, PopoverController } from '@ionic/angular';
import { BaseValidatorService } from '@app/shared/service/base.validator.service';
import { ValidatorService } from '@e-is/ngx-material-table';
import { ReferentialRefService } from '@app/referential/services/referential-ref.service';
import { CsvUtils } from '@app/shared/csv.utils';
import { FormatPropertyPipe } from '@app/shared/pipes/format-property.pipe';
import { isObservable, Observable, of, Subject } from 'rxjs';
import { HttpEventType } from '@angular/common/http';
import { LoadResult } from '@sumaris-net/ngx-components';

export class BaseReferentialTableOptions<
  T extends Entity<T, ID>,
  ID = number,
  O extends EntitiesServiceWatchOptions = EntitiesServiceWatchOptions>
  extends BaseTableOptions<T, ID, O> {

  propertyNames?: string[];
  canUpload?: boolean;
}

export declare type ColumnType = 'number'|'string'|'date'|'uri';


export const IGNORED_ENTITY_COLUMNS = ['__typename', 'id', 'updateDate'];

@Directive()
export abstract class BaseReferentialTable<
  E extends Entity<E, ID>,
  F extends EntityFilter<any, E, any>,
  V extends BaseValidatorService<E, ID> = any,
  ID = number,
  O extends BaseReferentialTableOptions<E, ID> = BaseReferentialTableOptions<E, ID>
  >
  extends AppBaseTable<E, F, V, ID>
  implements OnInit, AfterViewInit {

  /**
   * Compute columns from entity
   * @param dataType
   * @param validatorService
   */
  static getEntityDisplayProperties<T>(dataType: new () => T, validatorService?: ValidatorService): string[] {
    return Object.keys(validatorService && validatorService.getRowValidator().controls || new dataType())
      .filter(key => !IGNORED_ENTITY_COLUMNS.includes(key));
  }
  static getFirstEntityColumn<T>(dataType: new () => T): string {
    return Object.keys(new dataType()).find(key => !IGNORED_ENTITY_COLUMNS.includes(key));
  }


  @Input() title: string;
  @Input() showIdColumn = false;
  @Input() canDownload = false;
  @Input() canUpload = false;

  @ViewChild(IonInfiniteScroll) infiniteScroll: IonInfiniteScroll;

  columnDefinitions: FormFieldDefinition[];

  protected popoverController: PopoverController;
  protected formatPropertyPipe: FormatPropertyPipe;
  protected referentialRefService: ReferentialRefService;

  protected constructor(
    injector: Injector,
    dataType: new () => E,
    filterType: new () => F,
    entityService: IEntitiesService<E, F>,
    validatorService?: V,
    protected options?: O
  ) {
    super(
      injector,
      dataType,
      filterType,
      (options?.propertyNames || BaseReferentialTable.getEntityDisplayProperties(dataType)),
      entityService,
      validatorService,
      options
    );

    this.referentialRefService = injector.get(ReferentialRefService);
    this.formatPropertyPipe = injector.get(FormatPropertyPipe);
    this.popoverController = injector.get(PopoverController);
    this.title = this.i18nColumnPrefix && (this.i18nColumnPrefix + 'TITLE') || '';
    this.logPrefix = '[base-referential-table] ';
    this.canUpload = options?.canUpload || false;

    const config = this.getFilterFormConfig();
    this.filterForm = config && injector.get(FormBuilder).group(config);
  }

  ngOnInit() {
    super.ngOnInit();

    // Register autocomplete fields, BEFORE loading column definitions
    this.registerAutocompleteFields();

    this.columnDefinitions = this.loadColumnDefinitions(this.options);
    this.defaultSortBy = this.columnDefinitions[0].key;

    this.registerSubscription(
      this.onRefresh.subscribe(() => {
        this.filterForm.markAsUntouched();
        this.filterForm.markAsPristine();
      }));

    // Update filter when changes
    this.registerSubscription(
      this.filterForm.valueChanges
        .pipe(
          debounceTime(250),
          filter((_) => {
            const valid = this.filterForm.valid;
            if (!valid && this.debug) AppFormUtils.logFormErrors(this.filterForm);
            return valid;
          }),
          // Update the filter, without reloading the content
          tap(json => this.setFilter(json, {emitEvent: false})),
          // Save filter in settings (after a debounce time)
          debounceTime(500),
          tap(json => this.settings.savePageSetting(this.settingsId, json, BASE_TABLE_SETTINGS_ENUM.filterKey))
        )
        .subscribe());

    this.ready().then(() => this.restoreFilterOrLoad());
  }

  async ready(): Promise<void> {
    await (this.entityService instanceof StartableService
      ? this.entityService.ready()
      : this.settings.ready());

    return super.ready();
  }

  applyFilterAndClosePanel(event?: UIEvent) {
    const filter = this.filterForm.value;
    this.setFilter(filter, {emitEvent: false});
    super.applyFilterAndClosePanel(event);
  }

  async exportToCsv(event: UIEvent) {
    const filename = this.getExportFileName();
    const separator = this.getExportSeparator();
    const encoding = this.getExportEncoding();
    const headers = this.columnDefinitions.map(def => def.key);
    const rows = (await this.dataSource.getRows())
      .map(element => element.currentData)
      .map(data => this.columnDefinitions.map(definition => this.formatPropertyPipe.transform(data, definition)));

    CsvUtils.exportToFile(rows, {filename, headers, separator, encoding});
  }

  async importFromCsv(event?: UIEvent) {
    const { data } = await FilesUtils.showUploadPopover(this.popoverController, event, {
        uniqueFile: true,
        fileExtension: '.csv',
        uploadFn: (file) => this.uploadFile(file)
      });

    const entities = (data || []).flatMap(file => file.response?.body || []);
    if (isEmptyArray(entities)) return; // No entities: skip

    console.info(this.logPrefix + `Importing ${entities.length} entities...`, entities);
  }

  /* -- protected functions -- */

  protected loadColumnDefinitions(options?: O): FormFieldDefinition[] {

    return (options?.propertyNames || BaseReferentialTable.getEntityDisplayProperties(this.dataType))
      .map(key => this.getColumnDefinition(key, options));
  }

  protected registerAutocompleteFields() {
    // Can be overwritten by subclasses
  }

  protected getColumnDefinition(key: string, options?: O): FormFieldDefinition{
    if (this.autocompleteFields[key]) {
      return <FormFieldDefinition>{
        key,
        type: 'entity',
        label: (this.i18nColumnPrefix) + changeCaseToUnderscore(key).toUpperCase(),
        autocomplete: this.autocompleteFields[key]
      };
    }

    return <FormFieldDefinition>{
      key,
      type: this.getColumnType(key),
      label: (this.i18nColumnPrefix) + changeCaseToUnderscore(key).toUpperCase()
    };
  }

  protected getColumnType(key: string): FormFieldType {
    key = key.toLowerCase();
    if (key.endsWith('date')) return 'date';
    if (key.endsWith('month') || key.endsWith('year')) return 'integer';
    if (key.startsWith('is')) return 'boolean';
    if (key.endsWith('label') || key.endsWith('name') || key.endsWith('code')
      || key.endsWith('description') || key.endsWith('comments')) return 'string';
    return 'string';
  }

  protected getFilterFormConfig(): any {
    console.debug(this.logPrefix + ' Creating filter form group...');
    return BaseReferentialTable.getEntityDisplayProperties(this.filterType, this.validatorService)
      .reduce((config, key) => {
        console.debug(this.logPrefix + ' Adding filter control: ' + key);
        config[key] = [null];
        return config;
      }, {});
  }

  protected getExportFileName(): string {
    const key = this.i18nColumnPrefix + 'EXPORT_CSV_FILENAME';
    const filename = this.translate.instant(key, this.filter?.asObject());
    if (filename !== key) return filename;
    return 'export.csv'; // Default filename
  }

  protected getExportSeparator(): string {
    const key = 'FILE.CSV.SEPARATOR';
    const separator = this.translate.instant(key);
    if (separator !== key) return separator;
    return ','; // Default separator
  }

  protected getExportEncoding(): string {
    const key = 'FILE.CSV.ENCODING';
    const encoding = this.translate.instant(key);
    if (encoding !== key) return encoding;
    return 'UTF-8'; // Default encoding
  }

  protected uploadFile(file: File): Observable<FileEvent<E[]>> {
    console.info(this.logPrefix + `Importing CSV file ${file.name}...`);

    const separator = this.getExportSeparator();
    const encoding = this.getExportEncoding();

    return CsvUtils.parseFile(file, {encoding, separator})
      .pipe(
        switchMap(event => {
          if (event.type === HttpEventType.UploadProgress) {
            const loaded = Math.round(event.loaded * 0.8);
            return of({...event, loaded});
          }
          else if (event instanceof FileResponse){
            return this.uploadCsvRows(event.body);
          }
          // Unknown event: skip
          else {
            return of<FileEvent<E[]>>();
          }
        }),
        filter(isNotNil)
      );
  }

  protected uploadCsvRows(rows: string[][]): Observable<FileEvent<E[]>> {
    if (!rows || rows.length <= 1) throw {message: 'FILE.CSV.ERROR.EMPTY_FILE'};

    const $progress = new Subject<FileEvent<E[]>>();

    const headerNames = rows.splice(0, 1)[0];
    const total = rows.length;
    console.debug(this.logPrefix + `Importing ${total} rows...`);

    // Check headers
    if (headerNames.length <= 1) {
      const message = this.translate.instant('FILE.CSV.ERROR.NO_HEADER_OR_INVALID_SEPARATOR', {
        separator: this.getExportSeparator()
      });
      throw {message};
    }

    // Check column names
    console.debug(this.logPrefix + `Checking headers: ${headerNames.join(',')}`);
    const expectedHeaders = this.columnDefinitions.map(def => def.key);
    const unknownHeaders = headerNames.filter(h => !expectedHeaders.includes(h));
    if (unknownHeaders.length) {
      const message = this.translate.instant('FILE.CSV.ERROR.UNKNOWN_HEADERS', {
        headers: unknownHeaders.join(', ')
      });
      throw {message};
    }

    $progress.next({type: HttpEventType.UploadProgress, loaded: -1});
    const headers = headerNames.map(key => this.columnDefinitions.find(def => def.key === key));

    this.parseCsvRowsToEntities(headers, rows)
      .then(entities => this.fillEntities(headers, entities))
      .then(entities => this.excludeExistingEntities(headers, entities))
      .then(entities => {
        $progress.next(new FileResponse({body: entities}));
        $progress.complete();
      });

    return $progress.asObservable();
  }

  protected async parseCsvRowsToEntities(headers: FormFieldDefinition[], rows: string[][]): Promise<E[]> {
    return rows.map(cells => {
      // Convert to object
      const source = headers.reduce((res, fieldDef, i) => {
        const value = cells[i];

        // Parse sub-object
        const attributes = fieldDef.autocomplete?.attributes;
        if (attributes?.length) {
          res[fieldDef.key] = value.split(' - ', attributes.length)
            .reduce((o, v, j) => {
              o[attributes[j]] = v;
              return o;
            }, {});
        }
        // Parse simple field
        else {
          res[fieldDef.key] = value
        }
        return res;
      }, {});
      // Convert to entity
      const target: E = new this.dataType();
      target.fromObject(source);
      return target;
    });
  }

  protected async fillEntities(headers: FormFieldDefinition[], entities: E[]): Promise<E[]> {

    const autocompleteFields = headers.filter(def => def.autocomplete && (!!def.autocomplete.suggestFn || def.autocomplete.items));
    if (isEmptyArray(autocompleteFields)) return entities;

    // Preapre resulve function
    const suggestFns = autocompleteFields
      .map(def => def.autocomplete)
      .map(autocomplete => {
      return autocomplete.suggestFn
        || (isObservable(autocomplete.items) && ((v, o) => firstNotNilPromise(autocomplete.items as Observable<any[]>)
            .then(items => suggestFromArray(items, v, o))
            ))
        || ((v, o) => suggestFromArray(autocomplete.items as any[], v, o));
    });

    const result: E[] = [];

    // For each entities
    for (let entity of entities) {
      let incomplete = false;

      // For each field to resolve
      for (let i = 0; i < autocompleteFields.length; i++) {
        const field = autocompleteFields[i];
        const suggestFn = suggestFns[i];
        const attributes = field.autocomplete.attributes || [];
        const obj = entity[field.key];
        let resolveObj: any;
        for (let searchAttribute of attributes) {
          const searchValue = obj[searchAttribute];
          const res = await suggestFn(searchValue, { ...field.autocomplete.filter, searchAttribute });
          const matches = res && (Array.isArray(res) ? res : (res as LoadResult<any>).data);
          if (matches.length === 1) {
            resolveObj = matches[0];
            break;
          }
        }

        // Replace existing object
        if (resolveObj) {
          entity[field.key] = resolveObj;
        }

        // Not resolved: warn
        else {
          incomplete = true;
          console.warn(this.logPrefix + `Cannot resolve field ${obj.__typename}: ${joinProperties(obj, attributes)}`);
        }

        if (incomplete) break; // Stop if incomplete
      }

      // If complete entity: add to result
      if (!incomplete) result.push(entity)
    }

    return result;
  }

  protected async excludeExistingEntities(headers: FormFieldDefinition[], entities: E[]): Promise<E[]> {
    return entities;
  }
}
