import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnInit, ViewChild} from "@angular/core";
import {LandingsTable} from "../../landing/landings.table";
import {LandingFilter} from "../../services/landing.service";
import {AcquisitionLevelCodes} from "../../../referential/services/model/model.enum";
import {ModalController} from "@ionic/angular";
import {Landing} from "../../services/model/landing.model";
import {VesselFilter} from "../../../referential/services/vessel-service";
import {VesselsTable} from "../../../referential/vessel/list/vessels.table";
import {AppTable} from "../../../core/table/table.class";
import {isNotNil, toBoolean} from "../../../shared/functions";
import {VesselSnapshot} from "../../../referential/services/model/vessel-snapshot.model";

@Component({
  selector: 'app-select-vessel-modal',
  templateUrl: './select-vessel.modal.html',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SelectVesselsModal implements OnInit {

  selectedTabIndex = 0;

  @ViewChild(LandingsTable, { static: true }) landingsTable: LandingsTable;
  @ViewChild(VesselsTable, { static: true }) vesselsTable: VesselsTable;

  @Input() landingFilter: LandingFilter = {};
  @Input() vesselFilter: VesselFilter = {};
  @Input() allowMultiple: boolean;

  get loading(): boolean {
    const table = this.table;
    return table && table.loading;
  }

  get table(): AppTable<any> {
    return (!this.showLandings && this.vesselsTable) || (this.showLandings && this.landingsTable);
  }

  get showLandings(): boolean {
    return this.selectedTabIndex === 0;
  }

  @Input() set showLandings(value: boolean) {
    if (this.showLandings !== value) {
      this.selectedTabIndex = value ? 0 : 1;
      this.markForCheck();
    }
  }

  constructor(
    protected viewCtrl: ModalController,
    protected cd: ChangeDetectorRef
  ) {
  }

  ngOnInit() {
    // Init landing table
    this.landingFilter = this.landingFilter || {};
    this.landingsTable.filter = this.landingFilter;
    this.landingsTable.program = this.landingFilter.programLabel;
    this.landingsTable.acquisitionLevel = AcquisitionLevelCodes.LANDING;

    // Set defaults
    this.allowMultiple = toBoolean(this.allowMultiple, false);

    // Load landings
    setTimeout(() => {
      this.landingsTable.onRefresh.next("modal");
      this.markForCheck();
    }, 200);

  }


  async selectRow({id, row}) {
    const table = this.table;
    if (row && table) {
      if (!this.allowMultiple) {
        table.selection.clear();
        table.selection.select(row);
        await this.close();
      }
      else {
        table.selection.select(row);
      }
    }
  }

  async close(event?: any): Promise<boolean> {
    try {
      if (this.hasSelection()) {
        let vessels: VesselSnapshot[];
        if (this.showLandings) {
          vessels = (this.landingsTable.selection.selected || [])
            .map(row => row.currentData)
            .map(Landing.fromObject)
            .filter(isNotNil)
            .map(l => l.vesselSnapshot);
        }
        else {
          vessels = (this.vesselsTable.selection.selected || [])
            .map(row => row.currentData)
            .map(VesselSnapshot.fromVessel)
            .filter(isNotNil);
        }
        this.viewCtrl.dismiss(vessels);
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
    const table = this.table;
    return table && table.selection.hasValue() && (this.allowMultiple || table.selection.selected.length === 1);
  }

  protected markForCheck() {
    this.cd.markForCheck();
  }
}
