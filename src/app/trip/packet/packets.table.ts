import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Injector, Input, OnInit} from "@angular/core";
import {TableElement, ValidatorService} from "angular4-material-table";
import {InMemoryTableDataService} from "../../shared/services/memory-data-service.class";
import {Packet, PacketFilter, PacketUtils} from "../services/model/packet.model";
import {AppTable, RESERVED_END_COLUMNS, RESERVED_START_COLUMNS} from "../../core/table/table.class";
import {PacketValidatorService} from "../services/validator/packet.validator";
import {ModalController, Platform} from "@ionic/angular";
import {environment} from "../../../environments/environment";
import {ActivatedRoute, Router} from "@angular/router";
import {Location} from "@angular/common";
import {LocalSettingsService} from "../../core/services/local-settings.service";
import {AppTableDataSource} from "../../core/core.module";
import {BehaviorSubject, Observable} from "rxjs";
import {IWithProductsEntity} from "../services/model/base.model";
import {PacketModal} from "./packet.modal";
import {ProductFilter} from "../services/model/product.model";

@Component({
  selector: 'app-packets-table',
  templateUrl: 'packets.table.html',
  styleUrls: ['packets.table.scss'],
  providers: [
    {
      provide: InMemoryTableDataService,
      useFactory: () => new InMemoryTableDataService<Packet, PacketFilter>(Packet)
    }
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class PacketsTable extends AppTable<Packet, PacketFilter> implements OnInit {

  @Input() $parentFilter: Observable<any>;
  @Input() $parents: BehaviorSubject<IWithProductsEntity<any>[]>;
  @Input() parentAttributes: string[];

  private _program: string;

  @Input()
  set program(value: string) {
    this._program = value;
  }

  get program(): string {
    return this._program;
  }

  @Input()
  set value(data: Packet[]) {
    this.memoryDataService.value = data;
  }

  get value(): Packet[] {
    return this.memoryDataService.value;
  }

  get dirty(): boolean {
    return this._dirty || this.memoryDataService.dirty;
  }

  constructor(
    protected injector: Injector,
    protected route: ActivatedRoute,
    protected router: Router,
    protected platform: Platform,
    protected location: Location,
    protected modalCtrl: ModalController,
    protected settings: LocalSettingsService,
    protected validatorService: PacketValidatorService,
    protected memoryDataService: InMemoryTableDataService<Packet, PacketFilter>,
    protected cd: ChangeDetectorRef
  ) {
    super(route, router, platform, location, modalCtrl, settings,
      // columns
      RESERVED_START_COLUMNS
        .concat([
          'parent',
          'number',
          'weight',
          'composition'
        ])
        .concat(RESERVED_END_COLUMNS),
      new AppTableDataSource<Packet, PacketFilter>(Packet, memoryDataService, validatorService, {
        prependNewElements: false,
        suppressErrors: environment.production
      }),
      null,
      injector
      );

    this.i18nColumnPrefix = 'PACKET.LIST.';
    this.autoLoad = false; // waiting parent to be loaded
    this.inlineEdition = true;
    this.confirmBeforeDelete = true;
    this.pageSize = 1000; // Do not use paginator

    // FOR DEV ONLY ----
    this.debug = !environment.production;
  }

  ngOnInit() {
    super.ngOnInit();

    this.registerAutocompleteField('parent', {
      items: this.$parents,
      attributes: this.parentAttributes
    });

    this.registerSubscription(this.$parentFilter.subscribe(parentFilter => {
      // console.debug('parent test change', parentFilter);
      this.setFilter(new PacketFilter(parentFilter));
    }));

  }


  protected markForCheck() {
    this.cd.markForCheck();
  }


  async onCompositionClick(row: TableElement<Packet>) {
    // console.debug('onCompositionClick', row);

    const modal = await this.modalCtrl.create({
      component: PacketModal,
      componentProps: {
        packet: row.currentData
      },
      backdropDismiss: false,
      cssClass: 'modal-large'
    });

    modal.present();
    const res = await modal.onDidDismiss();

    // console.debug( 'onCompositionClick dismiss:',  res);

    if (res && res.data) {
      row.validator.patchValue(res.data, {onlySelf: false, emitEvent: true});
      this.markAsDirty();
    }

  }

  getComposition(row: TableElement<Packet>): string {
    return PacketUtils.getComposition(row.currentData);
  }
}
