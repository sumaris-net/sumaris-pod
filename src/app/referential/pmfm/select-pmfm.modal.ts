import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Inject, Input, OnInit, ViewChild} from "@angular/core";
import {ModalController} from "@ionic/angular";
import {changeCaseToUnderscore, isNotNil, toBoolean} from "../../shared/functions";
import {ReferentialFilter} from "../services/referential.service";
import {Subject} from "rxjs";
import {ReferentialRefFilter, ReferentialRefService} from "../services/referential-ref.service";
import {EntitiesTableDataSource} from "../../core/table/entities-table-datasource.class";
import {Referential, ReferentialRef} from "../../core/services/model/referential.model";
import {environment} from "../../../environments/environment";
import {SelectReferentialModal} from "../list/select-referential.modal";
import {ReferentialRefTable} from "../list/referential-ref.table";
import {BaseEntityService} from "../../core/services/base.data-service.class";
import {IEntitiesService} from "../../shared/services/entity-service.class";
import {PmfmFilter, PmfmService} from "../services/pmfm.service";
import {PmfmRefTable} from "./pmfm-ref.table";
import {Pmfm} from "../services/model/pmfm.model";

@Component({
  selector: 'app-select-pmfm-modal',
  templateUrl: './select-pmfm.modal.html',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SelectPmfmModal extends SelectReferentialModal<Pmfm, PmfmFilter> implements OnInit {

  // @ViewChild(ReferentialRefTable, { static: true }) table: ReferentialRefTable;
  //@ViewChild(ReferentialRefTable, { static: true }) table: PmfmRefTable;
constructor(
    protected viewCtrl: ModalController,
    protected pmfmService: PmfmService,
    protected cd: ChangeDetectorRef,
) {
  super(viewCtrl, null, cd);
  console.log('TODO CLT SelectPmfmModal constructor');

  this.datasource = new EntitiesTableDataSource<Pmfm, PmfmFilter>(/*this.dataType*/Pmfm,
    this.pmfmService,
    null,
    {
      prependNewElements: false,
      suppressErrors: environment.production
    });
}

  // ngOnInit() {
  //   // Init table
  //   if (!this.filter || !this.filter.entityName) {
  //     throw new Error("Missing argument 'filter'");
  //   }
  //   this.table.setDatasource(new EntitiesTableDataSource<ReferentialRef, ReferentialRefFilter>(ReferentialRef,
  //     this.referentialRefService,
  //     null,
  //     {
  //       prependNewElements: false,
  //       suppressErrors: environment.production
  //     }));
  //   this.table.filter = this.filter;
  //
  //   // Compute title
  //   this.$title.next('REFERENTIAL.ENTITY.' + changeCaseToUnderscore(this.filter.entityName).toUpperCase());
  //
  //   // Set defaults
  //   this.allowMultiple = toBoolean(this.allowMultiple, false);
  //
  //   // Load data
  //   setTimeout(() => {
  //
  //     this.table.onRefresh.next("modal");
  //     this.markForCheck();
  //   }, 200);
  //
  // }


}
