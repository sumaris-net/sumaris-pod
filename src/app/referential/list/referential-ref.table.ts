import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Injector, Input} from "@angular/core";
import {
  AppTable,
  environment,
  ReferentialRef,
  RESERVED_END_COLUMNS,
  RESERVED_START_COLUMNS
} from "../../core/core.module";
import {ActivatedRoute, Router} from "@angular/router";
import {ModalController, Platform} from "@ionic/angular";
import {Location} from "@angular/common";
import {LocalSettingsService} from "../../core/services/local-settings.service";
import {DefaultStatusList} from "../../core/services/model/referential.model";
import {ReferentialRefFilter, ReferentialRefService} from "../services/referential-ref.service";


@Component({
  selector: 'app-referential-ref-table',
  templateUrl: './referential-ref.table.html',
  styleUrls: ['./referential-ref.table.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ReferentialRefTable extends AppTable<ReferentialRef, ReferentialRefFilter> {

  statusList = DefaultStatusList;
  statusById: any;

  @Input() set entityName(entityName: string) {
    this.setFilter({
      ...this.filter,
      entityName
    });
  }

  get entityName(): string {
    return this.filter.entityName;
  }

  /*@Input('datasource') set datasourceInput(datasource: AppTableDataSource<ReferentialRef, ReferentialRefFilter>) {
    super.setDatasource(datasource);
  }*/

  // get dirty(): boolean {
  //   return this._dirty || this.memoryDataService.dirty;
  // }

  constructor(
    protected injector: Injector,
    protected referentialRefService: ReferentialRefService,
    protected cd: ChangeDetectorRef
  ) {
    super(injector.get(ActivatedRoute),
      injector.get(Router),
      injector.get(Platform),
      injector.get(Location),
      injector.get(ModalController),
      injector.get(LocalSettingsService),
      // columns
      RESERVED_START_COLUMNS
        .concat([
          'label',
          'name',
          'description',
          'status',
          'comments'])
        .concat(RESERVED_END_COLUMNS),
      null,
      null,
      injector);

    this.i18nColumnPrefix = 'REFERENTIAL.';
    this.autoLoad = false; // waiting dataSource to be set
    this.inlineEdition = false;

    // Fill statusById
    this.statusById = {};
    this.statusList.forEach((status) => this.statusById[status.id] = status);

    this.debug = !environment.production;
  }

  protected markForCheck() {
    this.cd.markForCheck();
  }
}

