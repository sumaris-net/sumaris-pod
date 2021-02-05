import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Injector} from "@angular/core";
import {ReferentialRefService} from "../services/referential-ref.service";
import {FormBuilder} from "@angular/forms";
import {RESERVED_END_COLUMNS, RESERVED_START_COLUMNS} from "../../core/table/table.class";
import {ReferentialRefTable} from "../list/referential-ref.table";
import {InMemoryEntitiesService} from "../../shared/services/memory-entity-service.class";
import {PmfmStrategy} from "../services/model/pmfm-strategy.model";
import {PmfmStrategyFilter} from "../strategy/pmfm-strategies.table";
import {PmfmService} from "../services/pmfm.service";
import {BaseEntityService} from "../../core/services/base.data-service.class";
import {Entity} from "../../core/services/model/entity.model";
import {ReferentialFilter} from "../services/referential.service";


@Component({
  selector: 'app-pmfm-ref-table',
  templateUrl: './pmfm-ref.table.html',
  styleUrls: ['./pmfm-ref.table.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class PmfmRefTable extends ReferentialRefTable<any, any> {

  constructor(
    protected injector: Injector,
    protected referentialRefService: ReferentialRefService,
    formBuilder: FormBuilder,
    protected cd: ChangeDetectorRef,
  ) {
    super(injector, referentialRefService, formBuilder, cd);
    console.log('TODO CLT PmfmRefTable constructor');
    // super(injector, new ReferentialRefService, formBuilder, cd);

    this.columns = RESERVED_START_COLUMNS
      .concat([
        'name',
        'unit',
        'matrix',
        'fraction',
        'method'])
      .concat(RESERVED_END_COLUMNS);
  }

}

