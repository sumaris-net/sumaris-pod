import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Injector, Input, OnDestroy, OnInit} from "@angular/core";
import {Platform} from "@ionic/angular";
import {AcquisitionLevelCodes} from "../../referential/services/model/model.enum";
import {AppMeasurementsTable} from "../measurement/measurements.table.class";
import {OperationGroupValidatorService} from "../services/validator/operation-group.validator";
import {BehaviorSubject} from "rxjs";
import {TableElement, ValidatorService} from "@e-is/ngx-material-table";
import {InMemoryEntitiesService, isNil} from '@sumaris-net/ngx-components';
import {MetierService} from "../../referential/services/metier.service";
import {OperationGroup, PhysicalGear} from "../services/model/trip.model";
import {ReferentialRef, referentialToString}  from "@sumaris-net/ngx-components";
import {environment} from "../../../environments/environment";
import {OperationFilter} from "@app/trip/services/filter/operation.filter";
import {OperationGroupModal} from '@app/trip/operationgroup/operation-group.modal';
import {IPmfm} from '@app/referential/services/model/pmfm.model';

export const OPERATION_GROUP_RESERVED_START_COLUMNS: string[] = ['metier', 'physicalGear', 'targetSpecies'];
export const OPERATION_GROUP_RESERVED_END_COLUMNS: string[] = ['comments'];

@Component({
  selector: 'app-operation-group-table',
  templateUrl: 'operation-groups.table.html',
  styleUrls: ['operation-groups.table.scss'],
  providers: [
    {provide: ValidatorService, useExisting: OperationGroupValidatorService},
    {
      provide: InMemoryEntitiesService,
      useFactory: () => new InMemoryEntitiesService<OperationGroup, OperationFilter>(OperationGroup, OperationFilter)
    }
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class OperationGroupTable extends AppMeasurementsTable<OperationGroup, OperationFilter> implements OnInit, OnDestroy {

  displayAttributes: {
    [key: string]: string[]
  };

  @Input()
  set value(data: OperationGroup[]) {
    this.memoryDataService.value = data;
  }

  get value(): OperationGroup[] {
    return this.memoryDataService.value;
  }

  get dirty(): boolean {
    return super.dirty || this.memoryDataService.dirty;
  }

  @Input() showToolbar = true;
  @Input() useSticky = false;
  @Input() $metiers: BehaviorSubject<ReferentialRef[]>;

  constructor(
    injector: Injector,
    protected platform: Platform,
    protected validatorService: ValidatorService,
    protected memoryDataService: InMemoryEntitiesService<OperationGroup, OperationFilter>,
    protected metierService: MetierService,
    protected cd: ChangeDetectorRef,
  ) {
    super(injector,
      OperationGroup,
      memoryDataService,
      validatorService,
      {
        prependNewElements: false,
        suppressErrors: environment.production,
        reservedStartColumns: OPERATION_GROUP_RESERVED_START_COLUMNS,
        reservedEndColumns: platform.is('mobile') ? [] : OPERATION_GROUP_RESERVED_END_COLUMNS,
        mapPmfms: (pmfms) => this.mapPmfms(pmfms),
      });
    this.i18nColumnPrefix = 'TRIP.OPERATION.LIST.';
    this.autoLoad = false; // waiting parent to be loaded
    this.inlineEdition = this.validatorService && !this.mobile;
    this.confirmBeforeDelete = true;
    this.defaultPageSize = -1; // Do not use paginator

    // Set default acquisition level
    this.acquisitionLevel = AcquisitionLevelCodes.OPERATION;

    // FOR DEV ONLY ----
    this.debug = !environment.production;
  }

  ngOnInit() {
    super.ngOnInit();


      this.displayAttributes = {
      gear: this.settings.getFieldDisplayAttributes('gear'),
      taxonGroup: ['taxonGroup.label', 'taxonGroup.name']
    };

    // Metier combo
    const metierAttributes = this.settings.getFieldDisplayAttributes('metier');
    this.registerAutocompleteField('metier', {
      showAllOnFocus: true,
      items: this.$metiers,
      attributes: metierAttributes,
      columnSizes: metierAttributes.map(attr => attr === 'label' ? 3 : undefined),
      suggestFn: (value: any, options?: any) => this.metierService.suggest(value, options)
    });

  }

  async openDetailModal(operationGroup?: OperationGroup): Promise<OperationGroup | undefined> {
    const isNew = !operationGroup && true;
    if (isNew) {
      operationGroup = new this.dataType();
      await this.onNewEntity(operationGroup);
    }

    this.markAsLoading();

    const modal = await this.modalCtrl.create({
      component: OperationGroupModal,
      componentProps: {
        programLabel: this.programLabel,
        acquisitionLevel: this.acquisitionLevel,
        disabled: this.disabled,
        value: operationGroup,
        isNew,
        onDelete: (event, OperationGroup) => this.deleteOperationGroup(event, OperationGroup)
      },
      keyboardClose: true
    });

    // Open the modal
    await modal.present();

    // Wait until closed
    const {data} = await modal.onDidDismiss();
    if (data && this.debug) console.debug("[operation-groups-table] operation-groups modal result: ", data);
    this.markAsLoaded();

    if (data instanceof OperationGroup) {
      return data as OperationGroup;
    }

    // Exit if empty
    return undefined;
  }

  getNextRankOrderOnPeriod(): number {
    let next = 0;
    (this.value || []).forEach(v => {
      if (v.rankOrderOnPeriod && v.rankOrderOnPeriod > next) next = v.rankOrderOnPeriod;
    });
    return next + 1;
  }

  async onMetierChange($event: FocusEvent, row: TableElement<OperationGroup>) {
    if (row && row.currentData && row.currentData.metier) {
      console.debug('[operation-group.table] onMetierChange', $event, row.currentData.metier);
      const operationGroup: OperationGroup = row.currentData;

      if (!operationGroup.physicalGear || operationGroup.physicalGear.gear.id !== operationGroup.metier.gear.id) {

        // First, load the Metier (with children)
        const metier = await this.metierService.load(operationGroup.metier.id);

        // create new physical gear if missing
        const physicalGear = new PhysicalGear();
        physicalGear.gear = metier.gear;
        // affect same rank order than operation group
        physicalGear.rankOrder = operationGroup.rankOrderOnPeriod;

        // affect to current row
        row.validator.controls['metier'].setValue(metier);
        row.validator.controls['physicalGear'].setValue(physicalGear);
      }
    }
  }

  async deleteOperationGroup(event: UIEvent, data: OperationGroup): Promise<boolean> {
    const row = await this.findRowByOperationGroup(data);

    // Row not exists: OK
    if (!row) return true;

    const canDeleteRow = await this.canDeleteRows([row]);
    if (canDeleteRow === true) {
      this.cancelOrDelete(event, row, {interactive: false /*already confirmed*/});
    }
    return canDeleteRow;
  }


  referentialToString = referentialToString;

  /* -- protected methods -- */

  private mapPmfms(pmfms: IPmfm[]): IPmfm[] {

  if (this.mobile) {
    pmfms.forEach(pmfm => pmfm.hidden = true);
    // return [];
  }

    return pmfms;
  }

  protected markForCheck() {
    this.cd.markForCheck();
  }

  protected async openNewRowDetail(): Promise<boolean> {
    if (!this.allowRowDetail) return false;

    const data = await this.openDetailModal();
    if (data) {
      await this.addEntityToTable(data);
    }
    return true;
  }

  protected async openRow(id: number, row: TableElement<OperationGroup>): Promise<boolean> {
    if (!this.allowRowDetail) return false;

    const data = this.toEntity(row, true);

    const updatedData = await this.openDetailModal(data);
    if (updatedData) {
      await this.updateEntityToTable(updatedData, row, {confirmCreate: false});
    }
    else {
      this.editedRow = null;
    }
    return true;
  }

  protected async onNewEntity(data: OperationGroup): Promise<void> {
    if (isNil(data.rankOrderOnPeriod)) {
      data.rankOrderOnPeriod = await this.getNextRankOrderOnPeriod();
    }
    if (!this.inlineEdition && isNil(data.physicalGear)) {
      data.physicalGear = new PhysicalGear();
    }
  }

  protected async addRowToTable(): Promise<TableElement<OperationGroup>> {
    const row = await super.addRowToTable();

    // TODO BLA: a mettre dans onNewEntity() ?
    row.validator.controls['rankOrderOnPeriod'].setValue(this.getNextRankOrderOnPeriod());
    // row.validator.controls['rankOrderOnPeriod'].updateValueAndValidity();

    return row;
  }

  protected async findRowByOperationGroup(operationGroup: OperationGroup): Promise<TableElement<OperationGroup>> {
    console.debug(this.dataSource.getRows());
    return OperationGroup && (await this.dataSource.getRows()).find(r => operationGroup.equals(r.currentData));
  }
}

