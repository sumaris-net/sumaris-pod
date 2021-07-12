import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Injector, Input, OnInit} from "@angular/core";
import {TableElement} from "@e-is/ngx-material-table";
import {InMemoryEntitiesService} from "@sumaris-net/ngx-components";
import {IWithPacketsEntity, Packet, PacketFilter, PacketUtils} from "../services/model/packet.model";
import {AppTable, RESERVED_END_COLUMNS, RESERVED_START_COLUMNS}  from "@sumaris-net/ngx-components";
import {PacketValidatorService} from "../services/validator/packet.validator";
import {ModalController, Platform} from "@ionic/angular";
import {ActivatedRoute, Router} from "@angular/router";
import {Location} from "@angular/common";
import {LocalSettingsService}  from "@sumaris-net/ngx-components";
import {BehaviorSubject} from "rxjs";
import {DenormalizedPmfmStrategy} from "../../referential/services/model/pmfm-strategy.model";
import {PacketModal} from "./packet.modal";
import {PacketSaleModal} from "../sale/packet-sale.modal";
import {isNil, isNotEmptyArray} from "@sumaris-net/ngx-components";
import {SaleProductUtils} from "../services/model/sale-product.model";
import {AcquisitionLevelCodes} from "../../referential/services/model/model.enum";
import {EntitiesTableDataSource}  from "@sumaris-net/ngx-components";
import {environment} from "../../../environments/environment";
import {ProgramRefService} from "../../referential/services/program-ref.service";

@Component({
  selector: 'app-packets-table',
  templateUrl: 'packets.table.html',
  styleUrls: ['packets.table.scss'],
  providers: [
    {
      provide: InMemoryEntitiesService,
      useFactory: () => new InMemoryEntitiesService(Packet, PacketFilter)
    }
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class PacketsTable extends AppTable<Packet, PacketFilter> implements OnInit {

  @Input() $parents: BehaviorSubject<IWithPacketsEntity<any, any>[]>;
  @Input() parentAttributes: string[];

  @Input() set parentFilter(packetFilter: PacketFilter) {
    this.setFilter(packetFilter);
  }

  private _program: string;

  @Input()
  set program(value: string) {
    this._program = value;
    if (value) {
      this.loadPmfms();
    }
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

  private packetSalePmfms: DenormalizedPmfmStrategy[];

  constructor(
    protected injector: Injector,
    protected route: ActivatedRoute,
    protected router: Router,
    protected platform: Platform,
    protected location: Location,
    protected modalCtrl: ModalController,
    protected settings: LocalSettingsService,
    protected validatorService: PacketValidatorService,
    protected memoryDataService: InMemoryEntitiesService<Packet, PacketFilter>,
    protected programRefService: ProgramRefService,
    protected cd: ChangeDetectorRef,
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
      new EntitiesTableDataSource<Packet, PacketFilter>(Packet, memoryDataService, validatorService, {
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
    this.defaultPageSize = -1; // Do not use paginator

    // FOR DEV ONLY ----
    this.debug = !environment.production;
  }

  ngOnInit() {
    super.ngOnInit();

    this.registerAutocompleteField('parent', {
      items: this.$parents,
      attributes: this.parentAttributes,
      columnNames: ['REFERENTIAL.LABEL', 'REFERENTIAL.NAME'],
      columnSizes: this.parentAttributes.map(attr => attr === 'metier.label' ? 3 : undefined)
    });

    this.registerSubscription(this.onStartEditingRow.subscribe(row => this.onStartEditPacket(row)));
  }

  private loadPmfms() {
    this.programRefService.loadProgramPmfms(this.program, {acquisitionLevel: AcquisitionLevelCodes.PACKET_SALE})
      .then(packetSalePmfms => this.packetSalePmfms = packetSalePmfms);
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

    if (res && res.data) {
      row.validator.patchValue(res.data, {onlySelf: false, emitEvent: true});

      // update sales
      this.updateSaleProducts(row);

      this.markAsDirty();
    }

  }

  getComposition(row: TableElement<Packet>): string {
    return PacketUtils.getComposition(row.currentData);
  }

  updateSaleProducts(row: TableElement<Packet>) {
    if (row && row.currentData) {
      // update sales if any
      if (isNotEmptyArray(row.currentData.saleProducts)) {
        const updatedSaleProducts = SaleProductUtils.updateAggregatedSaleProducts(row.currentData, this.packetSalePmfms);
        row.validator.patchValue({saleProducts: updatedSaleProducts}, {emitEvent: true});
      }
    }
  }

  async openPacketSale(event: MouseEvent, row: TableElement<Packet>) {
    if (event) event.stopPropagation();

    const modal = await this.modalCtrl.create({
      component: PacketSaleModal,
      componentProps: {
        packet: row.currentData,
        packetSalePmfms: this.packetSalePmfms
      },
      backdropDismiss: false,
      cssClass: 'modal-large'
    });

    modal.present();
    const res = await modal.onDidDismiss();

    if (res && res.data) {
      // patch saleProducts only
      row.validator.patchValue({saleProducts: res.data.saleProducts}, {emitEvent: true});
      this.markAsDirty();
    }

  }

  private onStartEditPacket(row: TableElement<Packet>) {
    if (this.filter && this.filter.parent && row.currentData && !row.currentData.parent) {
      row.validator.patchValue({parent: this.filter.parent});
    }
  }
}
