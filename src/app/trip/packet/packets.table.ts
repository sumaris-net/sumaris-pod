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
import {AppTableDataSource, isNil} from "../../core/core.module";
import {BehaviorSubject, Observable} from "rxjs";
import {PacketModal} from "./packet.modal";
import {AcquisitionLevelCodes, IWithPacketsEntity} from "../services/model/base.model";
import {PacketSaleModal} from "../sale/packet-sale.modal";
import {ProgramService} from "../../referential/services/program.service";

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
  @Input() $parents: BehaviorSubject<IWithPacketsEntity<any>[]>;
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
    protected programService: ProgramService,
    protected cd: ChangeDetectorRef
  ) {
    super(route, router, platform, location, modalCtrl, settings,
      // columns
      RESERVED_START_COLUMNS
        .concat([
          'parent',
          'rankOrder',
          'number',
          'weight'
        ])
        .concat(RESERVED_END_COLUMNS),
      new AppTableDataSource<Packet, PacketFilter>(Packet, memoryDataService, validatorService, {
        prependNewElements: false,
        suppressErrors: environment.production,
        onRowCreated: (row) => this.onRowCreated(row)
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

  trackByFn(index: number, row: TableElement<Packet>): number {
    return row.currentData.rankOrder;
  }

  private async onRowCreated(row: TableElement<Packet>) {
    const data = row.currentData; // if validator enable, this will call a getter function

    if (isNil(data.rankOrder)) {
      data.rankOrder = (await this.getMaxRankOrder()) + 1;
    }

    // Set row data
    row.currentData = data; // if validator enable, this will call a setter function

    this.markForCheck();
  }

  protected async getMaxRankOrder(): Promise<number> {
    const rows = await this.dataSource.getRows();
    return rows.reduce((res, row) => Math.max(res, row.currentData.rankOrder || 0), 0);
  }


  protected markForCheck() {
    this.cd.markForCheck();
  }


  async onCompositionClick(event: MouseEvent, row: TableElement<Packet>) {
    if (event) event.stopPropagation();

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

  async openPacketSale(event: MouseEvent, row: TableElement<Packet>) {
    if (event) event.stopPropagation();

    const modal = await this.modalCtrl.create({
      component: PacketSaleModal,
      componentProps: {
        packet: row.currentData,
        packetSalePmfms: await this.programService.loadProgramPmfms(this.program, {acquisitionLevel: AcquisitionLevelCodes.PACKET_SALE})
      },
      backdropDismiss: false,
      cssClass: 'modal-large'
    });

    modal.present();
    const res = await modal.onDidDismiss();

    if (res && res.data) {
      // patch saleProducts only
      row.validator.patchValue({ saleProducts: res.data.saleProducts }, {emitEvent: true});
      this.markAsDirty();
    }

  }

}
