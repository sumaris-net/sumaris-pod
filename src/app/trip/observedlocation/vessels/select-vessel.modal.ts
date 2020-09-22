import {
  AfterViewInit,
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  Input,
  OnDestroy,
  OnInit,
  ViewChild
} from "@angular/core";
import {LandingsTable} from "../../landing/landings.table";
import {LandingFilter} from "../../services/landing.service";
import {AcquisitionLevelCodes} from "../../../referential/services/model/model.enum";
import {ModalController} from "@ionic/angular";
import {Landing} from "../../services/model/landing.model";
import {VesselFilter, VesselService} from "../../../referential/services/vessel-service";
import {VesselsTable} from "../../../referential/vessel/list/vessels.table";
import {AppTable} from "../../../core/table/table.class";
import {isNotNil, toBoolean} from "../../../shared/functions";
import {VesselSnapshot} from "../../../referential/services/model/vessel-snapshot.model";
import {VesselForm} from "../../../referential/vessel/form/form-vessel";
import {AppFormUtils} from "../../../core/form/form.utils";
import {Vessel} from "../../../referential/services/model/vessel.model";
import {Subscription} from "rxjs";
import {ConfigOptions} from "../../../core/services/config/core.config";
import {ConfigService} from "../../../core/services/config.service";

@Component({
  selector: 'app-select-vessel-modal',
  templateUrl: './select-vessel.modal.html',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SelectVesselsModal implements OnInit, AfterViewInit, OnDestroy {

  selectedTabIndex = 0;
  subscription = new Subscription();

  @ViewChild(LandingsTable, { static: true }) landingsTable: LandingsTable;
  @ViewChild(VesselsTable, { static: true }) vesselsTable: VesselsTable;
  @ViewChild(VesselForm, { static: false }) vesselForm: VesselForm;

  @Input() landingFilter: LandingFilter = {};
  @Input() vesselFilter: VesselFilter = {};
  @Input() allowMultiple: boolean;
  @Input() allowAddNewVessel: boolean;

  get loading(): boolean {
    const table = this.table;
    return table && table.loading;
  }

  get table(): AppTable<any> {
    return (this.showVessels && this.vesselsTable) || (this.showLandings && this.landingsTable);
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

  get showVessels(): boolean {
    return this.selectedTabIndex === 1;
  }

  @Input() set showVessels(value: boolean) {
    if (this.showVessels !== value) {
      this.selectedTabIndex = value ? 1 : 0;
      this.markForCheck();
    }
  }

  get isNewVessel(): boolean {
    return this.selectedTabIndex === 2;
  }

  constructor(
    private vesselService: VesselService,
    private configService: ConfigService,
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
    this.allowAddNewVessel = toBoolean(this.allowAddNewVessel, true);

    // Load landings
    setTimeout(() => {
      this.landingsTable.onRefresh.next("modal");
      this.markForCheck();
    }, 200);

  }

  ngAfterViewInit() {

    // Get default status by config
    if (this.allowAddNewVessel && this.vesselForm) {
      this.subscription.add(
        this.configService.config.subscribe(config => {
          this.vesselForm.defaultStatus = config.getPropertyAsInt(ConfigOptions.VESSEL_DEFAULT_STATUS);
        })
      );

      this.vesselForm.enable();
    }
  }

  ngOnDestroy() {
    this.subscription.unsubscribe();
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
        } else if (this.showVessels) {
          vessels = (this.vesselsTable.selection.selected || [])
            .map(row => row.currentData)
            .map(VesselSnapshot.fromVessel)
            .filter(isNotNil);
        } else if (this.isNewVessel) {
          vessels = [await this.createVessel()];
        } else {
          console.warn("[select-vessel-modal] no selection");
        }
        this.viewCtrl.dismiss(vessels);
      }
      return true;
    } catch (err) {
      // nothing to do
      return false;
    }
  }

  async createVessel(): Promise<VesselSnapshot> {

    if (!this.vesselForm)
      throw Error('No Vessel Form');

    console.debug("[select-vessel-modal] Saving new vessel...");

    // Avoid multiple call
    if (this.vesselForm.disabled) return;

    await AppFormUtils.waitWhilePending(this.vesselForm);

    if (this.vesselForm.invalid) {
      this.vesselForm.markAsTouched({emitEvent: true});

      AppFormUtils.logFormErrors(this.vesselForm.form);
      return;
    }

    let result: VesselSnapshot;
    try {
      const json = this.vesselForm.value;
      const data = Vessel.fromObject(json);

      this.vesselForm.disable();

      const savedData = await this.vesselService.save(data);
      result = VesselSnapshot.fromVessel(savedData);
      this.vesselForm.error = null;
    }
    catch (err) {
      this.vesselForm.error = err && err.message || err;
      this.vesselForm.enable();
    }

    return result;
  }

  async cancel() {
    await this.viewCtrl.dismiss();
  }

  hasSelection(): boolean {
    if (this.allowAddNewVessel && this.isNewVessel && this.vesselForm) {
      return this.vesselForm.valid;
    }
    const table = this.table;
    return table && table.selection.hasValue() && (this.allowMultiple || table.selection.selected.length === 1);
  }

  protected markForCheck() {
    this.cd.markForCheck();
  }
}
