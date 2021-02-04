import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Inject, Input, OnInit, ViewChild} from "@angular/core";
import {ModalController} from "@ionic/angular";
import {PHYSICAL_GEAR_DATA_SERVICE, PhysicalGearFilter, PhysicalGearService} from "../services/physicalgear.service";
import {TableElement} from "@e-is/ngx-material-table";
import {PhysicalGear} from "../services/model/trip.model";
import {isNotNil, toBoolean} from "../../shared/functions";
import {AcquisitionLevelCodes, AcquisitionLevelType} from "../../referential/services/model/model.enum";
import {AppMeasurementsTable} from "../measurement/measurements.table.class";
import {IEntitiesService} from "../../shared/services/entity-service.class";
import {Observable} from "rxjs";

@Component({
  selector: 'app-select-physical-gear-modal',
  templateUrl: './select-physical-gear.modal.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    {
      provide: PHYSICAL_GEAR_DATA_SERVICE,
      useExisting: PhysicalGearService
    }
  ]
})
export class SelectPhysicalGearModal implements OnInit {

  selectedTabIndex = 0;

  @ViewChild('table', { static: true }) table: AppMeasurementsTable<PhysicalGear, PhysicalGearFilter>;

  @Input() allowMultiple: boolean;

  @Input() filter: PhysicalGearFilter = {};
  @Input() acquisitionLevel: AcquisitionLevelType;
  @Input() program: string;

  get loadingSubject(): Observable<boolean> {
    return this.table.loadingSubject;
  }

  constructor(
    protected viewCtrl: ModalController,
    protected cd: ChangeDetectorRef,
    @Inject(PHYSICAL_GEAR_DATA_SERVICE) protected dataService?: IEntitiesService<PhysicalGear, PhysicalGearFilter>
  ) {
  }

  ngOnInit() {

    // Init table
    this.table.dataService = this.dataService;
    this.filter = this.filter || {};
    this.table.filter = this.filter;
    this.table.acquisitionLevel = this.acquisitionLevel || AcquisitionLevelCodes.PHYSICAL_GEAR;
    this.table.programLabel = this.program;

    // Set defaults
    this.allowMultiple = toBoolean(this.allowMultiple, false);

    // Load landings
    setTimeout(() => {
      this.table.onRefresh.next("modal");
      this.markForCheck();
    }, 200);

  }

  async selectRow(event: { id?: number; row: TableElement<PhysicalGear> }) {
    if (event.row && this.table) {

      // Select the clicked row, then close
      if (!this.allowMultiple) {
        this.table.selection.clear();
        this.table.selection.select(event.row);
        await this.close();
      }

      // Add clicked row to selection
      else {
        this.table.selection.select(event.row);
      }
    }
  }

  async close(event?: any): Promise<boolean> {
    try {
      if (this.hasSelection()) {
        const gears = (this.table.selection.selected || [])
            .map(row => row.currentData)
            .map(PhysicalGear.fromObject)
            .filter(isNotNil);
        this.viewCtrl.dismiss(gears);
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
    return this.table && this.table.selection.hasValue() && (this.allowMultiple || this.table.selection.selected.length === 1);
  }

  protected markForCheck() {
    this.cd.markForCheck();
  }
}
