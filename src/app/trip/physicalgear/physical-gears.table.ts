import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Inject, Injector, Input, OnDestroy, OnInit, Output} from '@angular/core';
import {TableElement, ValidatorService} from '@e-is/ngx-material-table';
import {PhysicalGearValidatorService} from '../services/validator/physicalgear.validator';
import {AppMeasurementsTable} from '../measurement/measurements.table.class';
import {createPromiseEventEmitter, IEntitiesService, InMemoryEntitiesService} from '@sumaris-net/ngx-components';
import { PhysicalGearModal, PhysicalGearModalOptions } from './physical-gear.modal';
import {PhysicalGear} from '../services/model/trip.model';
import {PHYSICAL_GEAR_DATA_SERVICE} from '../services/physicalgear.service';
import {AcquisitionLevelCodes} from '../../referential/services/model/model.enum';
import {environment} from '../../../environments/environment';
import {PhysicalGearFilter} from '../services/filter/physical-gear.filter';
import { Subscription } from 'rxjs';

export const GEAR_RESERVED_START_COLUMNS: string[] = ['gear'];
export const GEAR_RESERVED_END_COLUMNS: string[] = ['lastUsed', 'comments'];


@Component({
  selector: 'app-physicalgears-table',
  templateUrl: 'physical-gears.table.html',
  styleUrls: ['physical-gears.table.scss'],
  providers: [
    {provide: ValidatorService, useExisting: PhysicalGearValidatorService},
    {
      provide: PHYSICAL_GEAR_DATA_SERVICE,
      useFactory: () => new InMemoryEntitiesService(PhysicalGear, PhysicalGearFilter, {
        equals: PhysicalGear.equals
      })
    }
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class PhysicalGearTable extends AppMeasurementsTable<PhysicalGear, PhysicalGearFilter> implements OnInit, OnDestroy {

  protected cd: ChangeDetectorRef;
  protected memoryDataService: InMemoryEntitiesService<PhysicalGear>;

  set value(data: PhysicalGear[]) {
    this.memoryDataService.value = data;
  }

  get value(): PhysicalGear[] {
    return this.memoryDataService.value;
  }

  @Input() canEdit = true;
  @Input() canDelete = true;
  @Input() canSelect = true;
  @Input() copyPreviousGears: (event: UIEvent) => Promise<PhysicalGear>;
  @Input() showToolbar = true;
  @Input() useSticky = false;

  @Input() set showSelectColumn(show: boolean) {
    this.setShowColumn('select', show);
  }

  @Input() set showLastUsedColumn(show: boolean) {
    this.setShowColumn('lastUsed', show);
  }

  @Output() onSelectPreviousGear = createPromiseEventEmitter();

  constructor(
    injector: Injector,
    @Inject(PHYSICAL_GEAR_DATA_SERVICE) dataService?: IEntitiesService<PhysicalGear, PhysicalGearFilter>
  ) {
    super(injector,
      PhysicalGear,
      dataService,
      null, // No validator = no inline edition
      {
        prependNewElements: false,
        suppressErrors: environment.production,
        reservedStartColumns: GEAR_RESERVED_START_COLUMNS,
        reservedEndColumns: GEAR_RESERVED_END_COLUMNS,
        mapPmfms: (pmfms) => pmfms.filter(p => p.required)
      });
    this.cd = injector.get(ChangeDetectorRef);
    this.memoryDataService = (this.dataService as InMemoryEntitiesService<PhysicalGear>);
    this.i18nColumnPrefix = 'TRIP.PHYSICAL_GEAR.LIST.';
    this.autoLoad = true;

    // Set default acquisition level
    this.acquisitionLevel = AcquisitionLevelCodes.PHYSICAL_GEAR;

    // Excluded columns, by default
    this.excludesColumns.push('lastUsed');

    // FOR DEV ONLY ----
    //this.debug = !environment.production;
  }

  ngOnInit() {
    super.ngOnInit();

    this._enabled = this.canEdit;
  }

  ngOnDestroy() {

    super.ngOnDestroy();
    this.onSelectPreviousGear.unsubscribe();
  }

  protected async openNewRowDetail(): Promise<boolean> {
    if (!this.allowRowDetail) return false;

    if (this.onNewRow.observers.length) {
      this.onNewRow.emit();
      return true;
    }

    const newGear = await this.openDetailModal();
    if (newGear) {
      if (this.debug) console.debug("Adding new gear:", newGear);
      await this.addEntityToTable(newGear);
    }
    return true;
  }

  protected async openRow(id: number, row: TableElement<PhysicalGear>): Promise<boolean> {
    if (!this.allowRowDetail) return false;

    if (this.onOpenRow.observers.length) {
      this.onOpenRow.emit({id, row});
      return true;
    }

    const gear = row.validator ? PhysicalGear.fromObject(row.currentData) : row.currentData;

    const updatedGear = await this.openDetailModal(gear);
    if (updatedGear) {
      await this.updateEntityToTable(updatedGear, row);
    }
    else {
      this.editedRow = null;
    }
    return true;
  }

  async openDetailModal(gear?: PhysicalGear): Promise<PhysicalGear | undefined> {

    const isNew = !gear && true;
    if (isNew) {
      gear = new PhysicalGear();
      await this.onNewEntity(gear);
    }
    const modalSubscription = new Subscription();

    const modal = await this.modalCtrl.create({
      component: PhysicalGearModal,
      componentProps: <PhysicalGearModalOptions>{
        programLabel: this.programLabel,
        acquisitionLevel: this.acquisitionLevel,
        disabled: this.disabled,
        value: gear.clone(), // Do a copy, because edition can be cancelled
        isNew,
        canEditRankOrder: this.canEditRankOrder,
        onInit: (modalComponent: PhysicalGearModal) => {
          // Subscribe to click on copy button, then redirect the event
          modalSubscription.add(
            modalComponent.onCopyPreviousGearClick.subscribe((event) => this.onSelectPreviousGear.emit(event))
          );
        },
        onDelete: (event, PhysicalGear) => this.deleteEntity(event, PhysicalGear)
      },
      keyboardClose: true,
      backdropDismiss: false
    });

    // Open the modal
    await modal.present();

    // Wait until closed
    const {data} = await modal.onDidDismiss();
    if (data && this.debug) console.debug("[physical-gear-table] Modal result: ", data);

    modalSubscription.unsubscribe();

    return (data instanceof PhysicalGear) ? data : undefined;
  }

  async deleteEntity(event: UIEvent, data: PhysicalGear): Promise<boolean> {
    const row = await this.findRowByEntity(data);

    // Row not exists: OK
    if (!row) return true;

    const canDeleteRow = await this.canDeleteRows([row]);
    if (canDeleteRow === true) {
      this.cancelOrDelete(event, row, {interactive: false /*already confirmed*/});
    }
    return canDeleteRow;
  }

  /* -- protected methods -- */

  protected markForCheck() {
    this.cd.markForCheck();
  }

  protected async findRowByEntity(physicalGear: PhysicalGear): Promise<TableElement<PhysicalGear>> {
    return PhysicalGear && (await this.dataSource.getRows()).find(r => r.currentData.equals(physicalGear));
  }

}


