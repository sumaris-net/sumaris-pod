import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Inject, Input, OnInit, ViewChild} from "@angular/core";
import {ModalController} from "@ionic/angular";
import {changeCaseToUnderscore, isNotNil, toBoolean} from "../../shared/functions";
import {ReferentialFilter} from "../services/referential.service";
import {Subject} from "rxjs";
import {ReferentialRefFilter, ReferentialRefService} from "../services/referential-ref.service";
import {EntitiesTableDataSource} from "../../core/table/entities-table-datasource.class";
import {ReferentialRefTable} from "./referential-ref.table";
import {Referential, ReferentialAsObjectOptions, ReferentialRef} from "../../core/services/model/referential.model";
import {environment} from "../../../environments/environment";
import {Entity} from "../../core/services/model/entity.model";
import {EntitiesServiceWatchOptions, IEntitiesService} from "../../shared/services/entity-service.class";

@Component({
  selector: 'app-select-referential-modal',
  templateUrl: './select-referential.modal.html',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SelectReferentialModal <T extends Entity<T>, F extends ReferentialFilter> implements OnInit {
// export class SelectReferentialModal<T extends IEntitiesService<T, F extends ReferentialFilter, EntitiesServiceWatchOptions>> implements OnInit {

  selectedTabIndex = 0;
  $title = new Subject<string>();
  datasource :EntitiesTableDataSource<any, any>;

  // @ViewChild(ReferentialRefTable, { static: true }) table: ReferentialRefTable;
  // @ViewChild(ReferentialRefTable, { static: true }) table: ReferentialRefTable<T extends Entity<T>, F extends ReferentialFilter>;
  //@ViewChild(ReferentialRefTable, { static: true }) table: ReferentialRefTable<ReferentialRef, ReferentialFilter>;
  // @ViewChild(ReferentialRefTable, { static: true }) table: ReferentialRefTable<T extends Entity<T>, ReferentialFilter>;
  // @ViewChild(ReferentialRefTable, { static: true }) table: ReferentialRefTable<T extends Entity<T> = Entity<any>, ReferentialFilter>;
  // @ViewChild(ReferentialRefTable, { static: true }) table: ReferentialRefTable<T extends Entity<T>, F extends ReferentialFilter>;

  @ViewChild(ReferentialRefTable, { static: true }) table: ReferentialRefTable<ReferentialRef, ReferentialFilter>;

  @Input() filter: ReferentialFilter;
  //@Input() filter: F;
  @Input() entityName: string;
  @Input() allowMultiple: boolean;

  get loading(): boolean {
    return this.table && this.table.loading;
  }

  constructor(
    protected viewCtrl: ModalController,
    protected referentialRefService: ReferentialRefService,
    protected cd: ChangeDetectorRef,
  ) {
    this.datasource = new EntitiesTableDataSource<ReferentialRef, ReferentialFilter>(/*this.dataType*/Referential,
      this.referentialRefService,
      null,
      {
        prependNewElements: false,
        suppressErrors: environment.production
      });
  }

  ngOnInit() {
    // Init table
    if (!this.filter || !this.filter.entityName) {
      throw new Error("Missing argument 'filter'");
    }

    //Referential<T extends Referential<any> = Referential<any>, O extends ReferentialAsObjectOptions = ReferentialAsObjectOptions>
    // this.table.setDatasource(new EntitiesTableDataSource<ReferentialRef, ReferentialFilter>(/*this.dataType*/Referential,
    this.table.setDatasource(this.datasource);
    this.table.filter = this.filter;

    //this.table.setDatasource(new EntitiesTableDataSource<ReferentialRef, ReferentialFilter>(ReferentialRef,
    //   this.referentialRefService,
    //   null,
    //   {
    //     prependNewElements: false,
    //     suppressErrors: environment.production
    //   }));

    // this.table.setDatasource(new EntitiesTableDataSource<T, F>(this.dataType,
    //   this.referentialRefService,
    //   null,
    //   {
    //         prependNewElements: false,
    //         suppressErrors: environment.production
    //       }));
    // this.table.filter = this.filter;

    // Compute title
    this.$title.next('REFERENTIAL.ENTITY.' + changeCaseToUnderscore(this.filter.entityName).toUpperCase());

    // Set defaults
    this.allowMultiple = toBoolean(this.allowMultiple, false);

    // Load data
    setTimeout(() => {

      this.table.onRefresh.next("modal");
      this.markForCheck();
    }, 200);

  }

  loadData() {
    /*this.referentialRefService.loadAll()*/
  }

  async selectRow({id, row}) {
    const table = this.table;
    if (row && table) {
      if (!this.allowMultiple) {
        table.selection.clear();
        table.selection.select(row);
        await this.close();
      }
      else {
        table.selection.select(row);
      }
    }
  }

  async close(event?: any): Promise<boolean> {
    try {
      if (this.hasSelection()) {
        const items = (this.table.selection.selected || [])
            .map(row => row.currentData)
            .map(ReferentialRef.fromObject)
            .filter(isNotNil);
        this.viewCtrl.dismiss(items);
      }
      return true;
    } catch (err) {
      // nothing to do
      return false;
    }
  }

  async cancel() {
    await this.viewCtrl.dismiss();
  }

  hasSelection(): boolean {
    const table = this.table;
    return table && table.selection.hasValue() && (this.allowMultiple || table.selection.selected.length === 1);
  }

  protected markForCheck() {
    this.cd.markForCheck();
  }
}
