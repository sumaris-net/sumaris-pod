import { AfterViewInit, Directive, Injector, Input, OnInit, ViewChild } from '@angular/core';
import {
  AppFormUtils,
  changeCaseToUnderscore,
  EntitiesServiceWatchOptions,
  Entity,
  EntityFilter, firstNotNil, firstNotNilPromise, FormFieldDefinition,
  FormFieldType,
  IEntitiesService, isNotNil,
  RESERVED_END_COLUMNS,
  RESERVED_START_COLUMNS,
  StartableService, waitIdle
} from '@sumaris-net/ngx-components';
import { AppBaseTable, BASE_TABLE_SETTINGS_ENUM, BaseTableOptions } from '@app/shared/table/base.table';
import { environment } from '@environments/environment';
import { FormBuilder } from '@angular/forms';
import { debounceTime, filter, tap } from 'rxjs/operators';
import { IonInfiniteScroll } from '@ionic/angular';
import { BaseValidatorService } from '@app/shared/service/base.validator.service';
import { ValidatorService } from '@e-is/ngx-material-table';
import { BehaviorSubject, Subject } from 'rxjs';
import { ReferentialRefService } from '@app/referential/services/referential-ref.service';
import { CsvUtils } from '@app/shared/csv.utils';
import { FormatPropertyPipe } from '@app/shared/pipes/format-property.pipe';

export class BaseReferentialTableOptions<
  T extends Entity<T, ID>,
  ID = number,
  O extends EntitiesServiceWatchOptions = EntitiesServiceWatchOptions>
  extends BaseTableOptions<T, ID, O> {

  propertyNames?: string[];
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

  @ViewChild(IonInfiniteScroll) infiniteScroll: IonInfiniteScroll;

  columnDefinitions: FormFieldDefinition[];

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
    this.title = this.i18nColumnPrefix && (this.i18nColumnPrefix + 'TITLE') || '';
    this.logPrefix = '[base-referential-table] ';

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
    const headers = this.columnDefinitions.map(def => def.key);
    const rows = (await this.dataSource.getRows())
      .map(element => element.currentData)
      .map(data => this.columnDefinitions.map(definition => this.formatPropertyPipe.transform(data, definition)));

    CsvUtils.exportToFile(rows, {filename, headers, separator});
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
    const key = 'COMMON.CSV_SEPARATOR';
    const filename = this.translate.instant(key);
    if (filename !== key) return filename;
    return ','; // Default separator
  }
}
