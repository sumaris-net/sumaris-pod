import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Injector, Input, OnDestroy, OnInit} from "@angular/core";
import {Platform} from "@ionic/angular";
import {AcquisitionLevelCodes} from "../../referential/services/model/model.enum";
import {OperationFilter} from "../services/operation.service";
import {AppMeasurementsTable} from "../measurement/measurements.table.class";
import {OperationGroupValidatorService} from "../services/validator/operation-group.validator";
import {BehaviorSubject} from "rxjs";
import {TableElement, ValidatorService} from "@e-is/ngx-material-table";
import {InMemoryEntitiesService} from "../../shared/services/memory-entity-service.class";
import {MetierService} from "../../referential/services/metier.service";
import {OperationGroup, PhysicalGear} from "../services/model/trip.model";
import {DenormalizedPmfmStrategy} from "../../referential/services/model/pmfm-strategy.model";
import {ReferentialRef, referentialToString} from "../../core/services/model/referential.model";
import {environment} from "../../../environments/environment";
import {IPmfm} from "../../referential/services/model/pmfm.model";

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
    return this._dirty || this.memoryDataService.dirty;
  }

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
    this.inlineEdition = true;
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
      columnSizes: metierAttributes.map(attr => attr === 'label' ? 3 : undefined)
    });

  }

  referentialToString = referentialToString;

  /* -- protected methods -- */

  protected markForCheck() {
    this.cd.markForCheck();
  }

  private mapPmfms(pmfms: IPmfm[]): IPmfm[] {

    if (this.platform.is('mobile')) {
      // hide pmfms on mobile
      return [];
    }

    return pmfms;
  }

  protected async addRowToTable(): Promise<TableElement<OperationGroup>> {
    const row = await super.addRowToTable();

    // TODO BLA: a mettre dans onNewEntity() ?
    row.validator.controls['rankOrderOnPeriod'].setValue(this.getNextRankOrderOnPeriod());
    // row.validator.controls['rankOrderOnPeriod'].updateValueAndValidity();

    return row;
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
}

