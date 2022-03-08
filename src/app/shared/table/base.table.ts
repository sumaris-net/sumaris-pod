import {Directive, ElementRef, Injector, Input, OnInit, ViewChild} from '@angular/core';
import {
  AppTable,
  AppTableDataSourceOptions,
  EntitiesServiceWatchOptions,
  EntitiesTableDataSource,
  Entity,
  EntityFilter,
  ENVIRONMENT,
  IEntitiesService,
  LocalSettingsService,
  PlatformService, RESERVED_END_COLUMNS, RESERVED_START_COLUMNS
} from '@sumaris-net/ngx-components';
import {ActivatedRoute, Router} from '@angular/router';
import {ModalController} from '@ionic/angular';
import {Location} from '@angular/common';
import {TableElement} from '@e-is/ngx-material-table';
import {PredefinedColors} from '@ionic/core';
import {FormGroup} from '@angular/forms';
import {BaseValidatorService} from '@app/shared/service/base.validator.service';
import {MatExpansionPanel} from '@angular/material/expansion';
import { environment } from '@environments/environment';


export const BASE_TABLE_SETTINGS_ENUM = {
  filterKey: 'filter',
  compactRowsKey: 'compactRows'
};

export class BaseTableOptions<
  T extends Entity<T, ID>,
  ID = number,
  O extends EntitiesServiceWatchOptions = EntitiesServiceWatchOptions>
  extends AppTableDataSourceOptions<T, ID, O> {

}

@Directive()
export abstract class AppBaseTable<E extends Entity<E, ID>,
  F extends EntityFilter<any, E, any>,
  V extends BaseValidatorService<E, ID> = any,
  ID = number>
  extends AppTable<E, F, ID> implements OnInit {


  @Input() canGoBack = false;
  @Input() showTitle = true;
  @Input() showToolbar = true;
  @Input() showPaginator = true;
  @Input() showFooter = true;
  @Input() showError = true;
  @Input() toolbarColor: PredefinedColors = 'primary';
  @Input() sticky = false;
  @Input() stickyEnd = false;
  @Input() compact = false;

  @ViewChild('tableContainer', { read: ElementRef }) tableContainerRef: ElementRef;
  @ViewChild(MatExpansionPanel, {static: true}) filterExpansionPanel: MatExpansionPanel;

  filterForm: FormGroup = null;
  filterCriteriaCount = 0;
  filterPanelFloating = true;

  get filterIsEmpty(): boolean {
    return this.filterCriteriaCount === 0;
  }

  protected logPrefix: string = null;

  constructor(
    protected injector: Injector,
    protected dataType: new () => E,
    protected filterType: new () => F,
    columnNames: string[],
    protected entityService: IEntitiesService<E, F>,
    protected validatorService?: V,
    options?: BaseTableOptions<E, ID>
  ) {
    super(
      injector,
      RESERVED_START_COLUMNS
        .concat(columnNames)
        .concat(RESERVED_END_COLUMNS),
      new EntitiesTableDataSource<E, F, ID>(dataType, entityService, validatorService, {
          prependNewElements: false,
          keepOriginalDataAfterConfirm: true,
          suppressErrors: injector.get(ENVIRONMENT).production,
          onRowCreated: (row) => this.onDefaultRowCreated(row),
          dataServiceOptions: {
            saveOnlyDirtyRows: true,
          },
          ...options,
        }),
        null
    );

    this.i18nColumnPrefix = options?.i18nColumnPrefix || '';
    this.logPrefix = '[base-table]';
    this.defaultSortBy = 'label';
    this.inlineEdition = !!this.validatorService;
    this.debug = !environment.production;
  }

  ngOnInit() {
    super.ngOnInit();

    this.restoreCompactMode();
  }

  scrollToBottom() {
    if (this.tableContainerRef) {
      // scroll to bottom
      this.tableContainerRef.nativeElement.scroll({
        top: this.tableContainerRef.nativeElement.scrollHeight,
      });
    }
  }

  setFilter(filter: Partial<F>, opts?: { emitEvent: boolean }) {

    filter = this.asFilter(filter);

    // Update criteria count
    const criteriaCount = filter.countNotEmptyCriteria();
    if (criteriaCount !== this.filterCriteriaCount) {
      this.filterCriteriaCount = criteriaCount;
      this.markForCheck();
    }

    // Update the form content
    if (!opts || opts.emitEvent !== false) {
      this.filterForm.patchValue(filter.asObject(), {emitEvent: false});
    }

    super.setFilter(filter as F, opts);
  }

  toggleFilterPanelFloating() {
    this.filterPanelFloating = !this.filterPanelFloating;
    this.markForCheck();
  }

  applyFilterAndClosePanel(event?: UIEvent) {
    this.onRefresh.emit(event);
    if (this.filterExpansionPanel && this.filterPanelFloating) this.filterExpansionPanel.close();
  }

  closeFilterPanel() {
    if (this.filterExpansionPanel) this.filterExpansionPanel.close();
  }

  resetFilter(event?: UIEvent) {
    this.filterForm.reset();
    this.setFilter(null, {emitEvent: true});
    this.filterCriteriaCount = 0;
    if (this.filterExpansionPanel && this.filterPanelFloating) this.filterExpansionPanel.close();
  }

  /* -- protected function -- */

  protected restoreFilterOrLoad(opts?: { emitEvent: boolean }) {
    this.markAsLoading();

    let json = this.settings.getPageSettings(this.settingsId, BASE_TABLE_SETTINGS_ENUM.filterKey);
    if (json) {
      console.debug(this.logPrefix + 'Restoring filter from settings...', json);
    }
    else {
      const {q} = this.route.snapshot.queryParams;
      if (q) {
        console.debug(this.logPrefix + 'Restoring filter from route query param: ', q);
        json = JSON.parse(q);
      }
    }

    if (json) {
      this.setFilter(json, opts);
    }
    else if (!opts || opts.emitEvent !== false){
      this.onRefresh.emit();
    }
  }

  restoreCompactMode() {
    if (!this.compact) {
      const compact = this.settings.getPageSettings(this.settingsId, BASE_TABLE_SETTINGS_ENUM.compactRowsKey) || false;
      if (this.compact !== compact) {
        this.compact = compact;
        this.markForCheck();
      }
    }
  }

  toggleCompactMode() {
    this.compact = !this.compact;
    this.markForCheck();
    this.settings.savePageSetting(this.settingsId, this.compact, BASE_TABLE_SETTINGS_ENUM.compactRowsKey);
  }

  /* -- protected functions -- */

  protected onDefaultRowCreated(row: TableElement<E>) {
    if (row.validator) {
      row.validator.patchValue(this.defaultNewRowValue());
    } else {
      Object.assign(row.currentData, this.defaultNewRowValue());
    }

    this.clickRow(undefined, row);
    this.scrollToBottom();
  }

  protected defaultNewRowValue(): any {
    return {};
  }

  protected asFilter(source: Partial<F>): F {
    const target = new this.filterType();
    if (source) target.fromObject(source);
    return target;
  }
}
