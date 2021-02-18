import {ChangeDetectionStrategy, Component, Injector, ViewChild} from '@angular/core';
import * as momentImported from "moment";
const moment = momentImported;
import {ObservedLocationForm} from "./observed-location.form";
import {ObservedLocationService} from "../services/observed-location.service";
import {LandingsTable} from "../landing/landings.table";
import {AppRootDataEditor} from "../../data/form/root-data-editor.class";
import {FormGroup} from "@angular/forms";
import {EntityServiceLoadOptions} from "../../shared/services/entity-service.class";
import {ModalController} from "@ionic/angular";
import {HistoryPageReference, UsageMode} from "../../core/services/model/settings.model";
import {ReferentialRef, ReferentialUtils} from "../../core/services/model/referential.model";
import {SelectVesselsModal} from "./vessels/select-vessel.modal";
import {ObservedLocation} from "../services/model/observed-location.model";
import {Landing} from "../services/model/landing.model";
import {LandingFilter} from "../services/landing.service";
import {LandingEditor, ProgramProperties} from "../../referential/services/config/program.config";
import {VesselSnapshot} from "../../referential/services/model/vessel-snapshot.model";
import {BehaviorSubject} from "rxjs";
import {firstNotNilPromise, firstTruePromise} from "../../shared/observables";
import {filter, first, tap} from "rxjs/operators";
import {AggregatedLandingsTable} from "../aggregated-landing/aggregated-landings.table";
import {showError} from "../../shared/alerts";
import {Program} from "../../referential/services/model/program.model";
import {PlatformService} from "../../core/services/platform.service";
import {ObservedLocationsPageSettingsEnum} from "./observed-locations.page";
import {fadeInOutAnimation} from "../../shared/material/material.animations";
import {isNil, isNotNil, toBoolean} from "../../shared/functions";
import {environment} from "../../../environments/environment";
import {TRIP_CONFIG_OPTIONS} from "../services/config/trip.config";
import {ConfigService} from "../../core/services/config.service";


const OBSERVED_LOCATION_DEFAULT_I18N_PREFIX = 'OBSERVED_LOCATION.EDIT.';

const ObservedLocationPageTabs = {
  GENERAL: 0,
  LANDINGS: 1
};

@Component({
  selector: 'app-observed-location-page',
  templateUrl: './observed-location.page.html',
  animations: [fadeInOutAnimation],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ObservedLocationPage extends AppRootDataEditor<ObservedLocation, ObservedLocationService> {

  showLandingTab = false;
  aggregatedLandings: boolean;
  allowAddNewVessel: boolean;
  addLandingUsingHistoryModal: boolean;
  $ready = new BehaviorSubject<boolean>(false);
  i18nPrefix = OBSERVED_LOCATION_DEFAULT_I18N_PREFIX;
  i18nSuffix = '';
  observedLocationNewName = '';

  @ViewChild('observedLocationForm', {static: true}) observedLocationForm: ObservedLocationForm;

  @ViewChild('landingsTable') landingsTable: LandingsTable;

  @ViewChild('aggregatedLandingsTable') aggregatedLandingsTable: AggregatedLandingsTable;

  get landingEditor(): LandingEditor {
    return this.landingsTable ? this.landingsTable.detailEditor : undefined;
  }

  set landingEditor(value: LandingEditor) {
    if (this.landingsTable) this.landingsTable.detailEditor = value;
  }

  constructor(
    injector: Injector,
    dataService: ObservedLocationService,
    protected modalCtrl: ModalController,
    protected platform: PlatformService,
    protected configService: ConfigService
  ) {
    super(injector,
      ObservedLocation,
      dataService,
      {
        pathIdAttribute: 'observedLocationId',
        tabCount: 2,
        autoOpenNextTab: !platform.mobile
      });
    this.defaultBackHref = "/observations";

    // FOR DEV ONLY ----
    this.debug = !environment.production;
  }

  ngOnInit() {
    super.ngOnInit();

    this.registerSubscription(
      this.configService.config.subscribe(config => {
        this.observedLocationNewName = config && config.getProperty(TRIP_CONFIG_OPTIONS.OBSERVED_LOCATION_NEW_NAME);
      })
    );

  }

  /* -- protected methods  -- */

  protected async setProgram(program: Program) {
    if (!program) return; // Skip

    if (this.debug) console.debug(`[observed-location] Program ${program.label} loaded, with properties: `, program.properties);
    this.observedLocationForm.showEndDateTime = program.getPropertyAsBoolean(ProgramProperties.OBSERVED_LOCATION_END_DATE_TIME_ENABLE);
    this.observedLocationForm.locationLevelIds = program.getPropertyAsNumbers(ProgramProperties.OBSERVED_LOCATION_LOCATION_LEVEL_ID);
    this.aggregatedLandings = program.getPropertyAsBoolean(ProgramProperties.OBSERVED_LOCATION_AGGREGATED_LANDINGS_ENABLE);
    this.allowAddNewVessel = program.getPropertyAsBoolean(ProgramProperties.OBSERVED_LOCATION_CREATE_VESSEL_ENABLE);
    this.addLandingUsingHistoryModal = program.getPropertyAsBoolean(ProgramProperties.OBSERVED_LOCATION_SHOW_LANDINGS_HISTORY);
    this.cd.detectChanges();

    if (this.landingsTable) {
      this.landingsTable.showDateTimeColumn = program.getPropertyAsBoolean(ProgramProperties.LANDING_DATE_TIME_ENABLE);
      this.landingsTable.showVesselTypeColumn = program.getPropertyAsBoolean(ProgramProperties.VESSEL_TYPE_ENABLE);
      this.landingsTable.showObserversColumn = program.getPropertyAsBoolean(ProgramProperties.LANDING_OBSERVERS_ENABLE);
      this.landingsTable.showCreationDateColumn = program.getPropertyAsBoolean(ProgramProperties.LANDING_CREATION_DATE_ENABLE);
      this.landingsTable.showRecorderPersonColumn = program.getPropertyAsBoolean(ProgramProperties.LANDING_RECORDER_PERSON_ENABLE);
      this.landingsTable.showVesselBasePortLocationColumn = program.getPropertyAsBoolean(ProgramProperties.LANDING_VESSEL_BASE_PORT_LOCATION_ENABLE);
      this.landingsTable.showLocationColumn =  program.getPropertyAsBoolean(ProgramProperties.LANDING_LOCATION_ENABLE);
      this.landingEditor = program.getProperty<LandingEditor>(ProgramProperties.LANDING_EDITOR);

    } else if (this.aggregatedLandingsTable) {

      this.aggregatedLandingsTable.nbDays = parseInt(program.getProperty(ProgramProperties.OBSERVED_LOCATION_AGGREGATED_LANDINGS_DAY_COUNT));
      this.aggregatedLandingsTable.program = program.getProperty(ProgramProperties.OBSERVED_LOCATION_AGGREGATED_LANDINGS_PROGRAM);
    }

    const i18nSuffix = program.getProperty(ProgramProperties.I18N_SUFFIX);
    this.i18nSuffix = i18nSuffix !== 'legacy' ? i18nSuffix : '';
    this.i18nPrefix = OBSERVED_LOCATION_DEFAULT_I18N_PREFIX + this.i18nSuffix;

    this.$ready.next(true);
  }


  protected async onNewEntity(data: ObservedLocation, options?: EntityServiceLoadOptions): Promise<void> {
    // If is on field mode, fill default values
    if (this.isOnFieldMode) {
      data.startDateTime = moment();

      console.debug("[trip] New entity: set default values...");

      // Fil defaults, using filter applied on trips table
      const searchFilter = this.settings.getPageSettings<any>(ObservedLocationsPageSettingsEnum.PAGE_ID, ObservedLocationsPageSettingsEnum.FILTER_KEY);
      if (searchFilter) {
        // Synchronization status
        if (searchFilter.synchronizationStatus && searchFilter.synchronizationStatus !== 'SYNC') {
          data.synchronizationStatus = 'DIRTY';
        }

        // program
        if (searchFilter.program && searchFilter.program.label) {
          data.program = ReferentialRef.fromObject(searchFilter.program);
          this.$programLabel.next(data.program.label);
        }

        // Location
        if (searchFilter.location) {
          data.location = ReferentialRef.fromObject(searchFilter.location);
        }
      }

      this.showLandingTab = true;

      // Listen first opening the operations tab, then save
      this.registerSubscription(
        this.tabGroup.selectedTabChange
          .pipe(
            filter(event => event.index === ObservedLocationPageTabs.LANDINGS),
            first()
          )
          .subscribe(event => this.save())
        );
    }
  }

  protected async onEntityLoaded(data: ObservedLocation, options?: EntityServiceLoadOptions): Promise<void> {
    // Move to second tab
    if (!this.isNewData && !this.isOnFieldMode) {
      this.selectedTabIndex = 1;
      this.tabGroup.realignInkBar();
    }
  }

  updateViewState(data: ObservedLocation, opts?: {onlySelf?: boolean, emitEvent?: boolean; }) {
    super.updateViewState(data);

    // Update tabs state (show/hide)
    this.updateTabsState(data);
  }

  updateTabsState(data: ObservedLocation) {
    // Enable landings tab
    this.showLandingTab = this.showLandingTab || (isNotNil(data.id) || this.isOnFieldMode);
  }

  protected async setValue(data: ObservedLocation) {
    // Set data to form
    this.observedLocationForm.value = data;

    const isNew = isNil(data.id);
    if (!isNew) {
      // Propagate program to form
      this.$programLabel.next(data.program.label);
    }

    // Wait for child table ready
    await this.ready();
    this.updateViewState(data);

    // Propagate parent to landings table
    if (!isNew) {
      if (this.landingsTable) {
        if (this.debug) console.debug("[observed-location] Propagate observed location to landings table");
        this.landingsTable.setParent(data);
      }
      if (this.aggregatedLandingsTable) {
        if (this.debug) console.debug("[observed-location] Propagate observed location to aggregated landings form");
        this.aggregatedLandingsTable.setParent(data);
      }
    }
  }

  protected async ready(): Promise<void> {
    // Wait child loaded
    if (this.$ready.getValue() !== true) {
      if (this.debug) console.debug('[observed-location] waiting child to be ready...');
      await firstTruePromise(this.$ready);
    }
  }

  async onOpenLanding({id, row}) {
    const savedOrContinue = await this.saveIfDirtyAndConfirm();
    if (savedOrContinue) {
      await this.router.navigateByUrl(`/observations/${this.data.id}/${this.landingEditor}/${id}`);
    }
  }

  async onNewLanding(event?: any) {

    const savePromise: Promise<boolean> = this.isOnFieldMode && this.dirty
      // If on field mode: try to save silently
      ? this.save(event)
      // If desktop mode: ask before save
      : this.saveIfDirtyAndConfirm();

    const savedOrContinue = await savePromise;
    if (savedOrContinue) {
      this.markAsLoading();

      try {
        // Add landing using vessels modal
        if (this.addLandingUsingHistoryModal) {
          const vessel = await this.openSelectVesselModal();
          if (vessel && this.landingsTable) {
            const rankOrder = (await this.landingsTable.getMaxRankOrderOnVessel(vessel) || 0) + 1;
            await this.router.navigateByUrl(`/observations/${this.data.id}/${this.landingEditor}/new?vessel=${vessel.id}&rankOrder=${rankOrder}`);
          }
        }
        // Create landing without vessel selection
        else {
          const rankOrder = (await this.landingsTable.getMaxRankOrder() || 0) + 1;
          await this.router.navigateByUrl(`/observations/${this.data.id}/${this.landingEditor}/new?rankOrder=${rankOrder}`);
        }
      } finally {
        this.markAsLoaded();
      }
    }
  }


  async onNewAggregatedLanding(event?: any) {
    const savePromise: Promise<boolean> = this.isOnFieldMode && this.dirty
      // If on field mode: try to save silently
      ? this.save(event)
      // If desktop mode: ask before save
      : this.saveIfDirtyAndConfirm();

    const savedOrContinue = await savePromise;
    if (savedOrContinue) {
      this.markAsLoading();

      try {
        const vessel = await this.openSelectVesselModal(true);
        if (vessel && this.aggregatedLandingsTable) {
          await this.aggregatedLandingsTable.addAggregatedRow(vessel);
        }
      } finally {
        this.markAsLoaded();
      }
    }
  }

  async onNewTrip({id, row}) {
    const savePromise: Promise<boolean> = this.isOnFieldMode && this.dirty
      // If on field mode: try to save silently
      ? this.save(undefined)
      // If desktop mode: ask before save
      : this.saveIfDirtyAndConfirm();

    const savedOrContinue = await savePromise;
    if (savedOrContinue) {
      this.markAsLoading();

      try {
        await this.router.navigateByUrl(`/observations/${this.data.id}/${this.landingEditor}/new?vessel=${row.currentData.vesselSnapshot.id}&landing=${row.currentData.id}`);
      } finally {
        this.markAsLoaded();
      }
    }

  }

  async openSelectVesselModal(excludeExistingVessels?: boolean): Promise<VesselSnapshot | undefined> {
    if (!this.data.startDateTime || !this.data.program) {
      throw new Error('Root entity has no program and start date. Cannot open select vessels modal');
    }

    const startDate = this.data.startDateTime.clone().add(-15, "days");
    const endDate = this.data.startDateTime;
    const programLabel = (this.aggregatedLandingsTable && this.aggregatedLandingsTable.program) || this.data.program.label;
    const excludeVesselIds = (toBoolean(excludeExistingVessels, false) && this.aggregatedLandingsTable
      && await this.aggregatedLandingsTable.vesselIdsAlreadyPresent()) || [];

    const landingFilter = <LandingFilter>{
      programLabel,
      startDate,
      endDate,
      locationId: ReferentialUtils.isNotEmpty(this.data.location) ? this.data.location.id : undefined,
      groupByVessel: (this.landingsTable && this.landingsTable.isTripDetailEditor) || (isNotNil(this.aggregatedLandingsTable)),
      excludeVesselIds
    };

    const modal = await this.modalCtrl.create({
      component: SelectVesselsModal,
      componentProps: {
        allowMultiple: false,
        allowAddNewVessel: this.allowAddNewVessel,
        showVesselTypeColumn: this.landingsTable.showVesselTypeColumn,
        landingFilter
      },
      keyboardClose: true,
      cssClass: 'modal-large'
    });

    // Open the modal
    modal.present();

    // Wait until closed
    const res = await modal.onDidDismiss();

    // If modal return a landing, use it
    let data = res && res.data && res.data[0];
    if (data instanceof Landing) {
      console.debug("[observed-location] Vessel selection modal result:", data);
      data = (data as Landing).vesselSnapshot;
    }
    if (data instanceof VesselSnapshot) {
      console.debug("[observed-location] Vessel selection modal result:", data);
      const vessel = data as VesselSnapshot;
      if (excludeVesselIds.includes(data.id)) {
        await showError('AGGREGATED_LANDING.VESSEL_ALREADY_PRESENT', this.alertCtrl, this.translate);
        return;
      }
      return vessel;
    } else {
      console.debug("[observed-location] Vessel selection modal was cancelled");
    }
  }


  addRow($event: MouseEvent) {
    if (this.landingsTable) {
      this.landingsTable.addRow($event);
    } else if (this.aggregatedLandingsTable) {
      this.aggregatedLandingsTable.addRow($event);
    }
  }

  /* -- protected methods -- */



  protected get form(): FormGroup {
    return this.observedLocationForm.form;
  }

  protected canUserWrite(data: ObservedLocation): boolean {
    return isNil(data.validationDate) && this.dataService.canUserWrite(data);
  }

  protected computeUsageMode(data: ObservedLocation): UsageMode {
    return this.settings.isUsageMode('FIELD') || data.synchronizationStatus === 'DIRTY'  ? 'FIELD' : 'DESK';
  }

  protected registerForms() {
    this.addChildForms([
      this.observedLocationForm,
      () => this.landingsTable,
      () => this.aggregatedLandingsTable
    ]);
  }

  protected async computeTitle(data: ObservedLocation): Promise<string> {

    //await this.ready();
    await firstNotNilPromise(this.$ready);

    // new data
    if (this.isNewData) {
      return this.translate.get(`${this.observedLocationNewName}`).toPromise();
    }

    // Existing data
    return this.translate.get(`OBSERVED_LOCATION.EDIT.${this.i18nSuffix}TITLE`, {
      location: data.location && (data.location.name || data.location.label),
      dateTime: data.startDateTime && this.dateFormat.transform(data.startDateTime) as string
    }).toPromise();
  }

  protected async computePageHistory(title: string): Promise<HistoryPageReference> {
    return {
      ... (await super.computePageHistory(title)),
      matIcon: 'verified_user'
    };
  }

  protected async getJsonValueToSave(): Promise<any> {
    const json = await super.getJsonValueToSave();

    if (this.landingsTable && this.landingsTable.dirty) {
      await this.landingsTable.save();
    }
    if (this.aggregatedLandingsTable && this.aggregatedLandingsTable.dirty) {
      await this.aggregatedLandingsTable.save();
    }

    return json;
  }


  // TODO BLA: manage langinTable2
  protected getFirstInvalidTabIndex(): number {
    return this.observedLocationForm.invalid ? 0
      : ((this.landingsTable && this.landingsTable.invalid) || (this.aggregatedLandingsTable && this.aggregatedLandingsTable.invalid) ? 1
        : -1);
  }

  protected markForCheck() {
    this.cd.markForCheck();
  }
}
