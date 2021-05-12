import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Inject, Input, OnInit, Optional, ViewChild} from "@angular/core";
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
import {BaseEntityService} from "../services/base-entity-service.class";
import {BaseSelectEntityModal} from "./base-select-entity.modal";

@Component({
  selector: 'app-select-referential-modal',
  templateUrl: './select-referential.modal.html',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SelectReferentialModal extends BaseSelectEntityModal<ReferentialRef, ReferentialFilter> implements OnInit {

  @Input() entityName: string;

  constructor(
    protected viewCtrl: ModalController,
    protected dataService: ReferentialRefService,
    protected cd: ChangeDetectorRef
  ) {
    super(viewCtrl, ReferentialRef, dataService);
  }

  ngOnInit() {
    super.ngOnInit();

    // Copy the entityName to filter
    if (this.entityName) {
      this.filter.entityName = this.entityName;
    }
    if (!this.filter.entityName) {
      throw Error('Missing entityName');
    }
  }

  protected async computeTitle(): Promise<string> {
    return 'REFERENTIAL.ENTITY.' + changeCaseToUnderscore(this.filter.entityName).toUpperCase();
  }

  protected markForCheck() {
    this.cd.markForCheck();
  }
}
