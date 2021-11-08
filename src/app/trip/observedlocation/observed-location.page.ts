import { ChangeDetectionStrategy, Component, Injector, OnInit, ViewChild } from '@angular/core';
import * as momentImported from 'moment';
import { ObservedLocationForm } from './observed-location.form';
import { ObservedLocationService } from '../services/observed-location.service';
import { LandingsTable } from '../landing/landings.table';
import { AppRootDataEditor } from '@app/data/form/root-data-editor.class';
import { FormGroup } from '@angular/forms';
import {
  Alerts,
  AppTable,
  ConfigService,
  EntityServiceLoadOptions,
  fadeInOutAnimation,
  firstTruePromise,
  HistoryPageReference,
  isNil,
  isNotNil,
  PlatformService,
  ReferentialRef,
  ReferentialUtils, StatusIds,
  toBoolean,
  UsageMode, waitIdle,
} from '@sumaris-net/ngx-components';
import { ModalController } from '@ionic/angular';
import { SelectVesselsModal } from './vessels/select-vessel.modal';
import { ObservedLocation } from '../services/model/observed-location.model';
import { Landing } from '../services/model/landing.model';
import { LandingEditor, ProgramProperties } from '@app/referential/services/config/program.config';
import { VesselSnapshot } from '@app/referential/services/model/vessel-snapshot.model';
import { BehaviorSubject } from 'rxjs';
import { filter, first, tap } from 'rxjs/operators';
import { AggregatedLandingsTable } from '../aggregated-landing/aggregated-landings.table';
import { Program } from '@app/referential/services/model/program.model';
import { ObservedLocationsPageSettingsEnum } from './observed-locations.page';
import { environment } from '@environments/environment';
import { DATA_CONFIG_OPTIONS } from 'src/app/data/services/config/data.config';
import { LandingFilter } from '../services/filter/landing.filter';
import { ContextService } from '@app/shared/context.service';
import { VesselFilter } from '@app/vessel/services/filter/vessel.filter';
import { waitForAsync } from '@angular/core/testing';

const moment = momentImported;


const OBSERVED_LOCATION_DEFAULT_I18N_PREFIX = 'OBSERVED_LOCATION.EDIT.';

const ObservedLocationPageTabs = {
  GENERAL: 0,
  LANDINGS: 1
};

@Component({
  selector: 'app-observed-location-page',
  templateUrl: './observed-location.page.html',
  styleUrls: ['./observed-location.page.scss'],
  animations: [fadeInOutAnimation],
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    {provide: AppRootDataEditor, useExisting: ObservedLocationPage}
  ],
})
export class ObservedLocationPage extends AppRootDataEditor<ObservedLocation, ObservedLocationService> implements OnInit {

  @ViewChild('observedLocationForm', {static: true}) observedLocationForm: ObservedLocationForm;
  @ViewChild('landingsTable') landingsTable: LandingsTable;
  @ViewChild('aggregatedLandingsTable') aggregatedLandingsTable: AggregatedLandingsTable;

  mobile: boolean;
  showLandingTab = false;
  aggregatedLandings: boolean;
  allowAddNewVessel: boolean;
  showVesselType: boolean;
  addLandingUsingHistoryModal: boolean;
  $ready = new BehaviorSubject<boolean>(false);
  showQualityForm = false;
  showRecorder = true;
  landingEditor: LandingEditor = undefined;

  get table(): AppTable<any> & { setParent(value: ObservedLocation | undefined) } {
    return this.landingsTable || this.aggregatedLandingsTable;
  }

  constructor(
    injector: Injector,
    dataService: ObservedLocationService,
    protected modalCtrl: ModalController,
    protected platform: PlatformService,
    protected configService: ConfigService,
    protected context: ContextService
  ) {
    super(injector,
      ObservedLocation,
      dataService,
      {
        pathIdAttribute: 'observedLocationId',
        tabCount: 2,
        autoOpenNextTab: !platform.mobile,
        i18nPrefix: OBSERVED_LOCATION_DEFAULT_I18N_PREFIX
      });
    this.defaultBackHref = '/observations';
    this.mobile = this.platform.mobile;

    // FOR DEV ONLY ----
    this.debug = !environment.production;
  }

  ngOnInit() {
    super.ngOnInit();

    this.registerSubscription(
      this.configService.config.subscribe(config => {
        if (!config) return;
        this.showRecorder = config.getPropertyAsBoolean(DATA_CONFIG_OPTIONS.SHOW_RECORDER);
        this.markForCheck();
      })
    );

  }

  /* -- protected methods  -- */

  updateViewState(data: ObservedLocation, opts?: {onlySelf?: boolean, emitEvent?: boolean; }) {
    super.updateViewState(data);

    // Update tabs state (show/hide)
    this.updateTabsState(data);
  }

  updateTabsState(data: ObservedLocation) {
    // Enable landings tab
    this.showLandingTab = this.showLandingTab || (!this.isNewData || this.isOnFieldMode);

    // Move to second tab
    if (this.showLandingTab && !this.isNewData && !this.isOnFieldMode && this.selectedTabIndex === 0) {
      this.selectedTabIndex = 1;
      this.tabGroup.realignInkBar();
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

    const startDate = this.data.startDateTime.clone().add(-15, 'days');
    const endDate = this.data.startDateTime.clone();
    const programLabel = (this.aggregatedLandingsTable?.programLabel) || this.data.program.label;
    const excludeVesselIds = (toBoolean(excludeExistingVessels, false) && this.aggregatedLandingsTable
      && (await this.aggregatedLandingsTable.vesselIdsAlreadyPresent())) || [];

    const landingFilter = LandingFilter.fromObject({
      programLabel,
      startDate,
      endDate,
      locationId: ReferentialUtils.isNotEmpty(this.data.location) ? this.data.location.id : undefined,
      groupByVessel: (this.landingsTable && this.landingsTable.isTripDetailEditor) || (isNotNil(this.aggregatedLandingsTable)),
      excludeVesselIds,
      synchronizationStatus: 'SYNC' // only remote entities. This is required to read 'Remote#LandingVO' local storage
    });

    const modal = await this.modalCtrl.create({
      component: SelectVesselsModal,
      componentProps: {
        allowMultiple: false,
        allowAddNewVessel: this.allowAddNewVessel,
        showVesselTypeColumn: this.showVesselType,
        landingFilter,
        vesselFilter: <VesselFilter>{
          statusIds: [StatusIds.TEMPORARY, StatusIds.ENABLE],
          onlyWithRegistration: true
        }
      },
      keyboardClose: true,
      cssClass: 'modal-large'
    });

    // Open the modal
    modal.present();

    // Wait until closed
    const {data} = await modal.onDidDismiss();

    // If modal return a landing, use it
    if (data && data[0] instanceof Landing) {
      console.debug('[observed-location] Vessel selection modal result:', data);
      return (data[0] as Landing).vesselSnapshot;
    }
    if (data && data[0] instanceof VesselSnapshot) {
      console.debug('[observed-location] Vessel selection modal result:', data);
      const vessel = data[0] as VesselSnapshot;
      if (excludeVesselIds.includes(data.id)) {
        await Alerts.showError('AGGREGATED_LANDING.VESSEL_ALREADY_PRESENT', this.alertCtrl, this.translate);
        return;
      }
      return vessel;
    } else {
      console.debug('[observed-location] Vessel selection modal was cancelled');
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

  protected async setProgram(program: Program) {
    await super.setProgram(program);
    if (!program) return; // Skip

    this.observedLocationForm.showEndDateTime = program.getPropertyAsBoolean(ProgramProperties.OBSERVED_LOCATION_END_DATE_TIME_ENABLE);
    this.observedLocationForm.showStartTime = program.getPropertyAsBoolean(ProgramProperties.OBSERVED_LOCATION_START_TIME_ENABLE);
    this.observedLocationForm.locationLevelIds = program.getPropertyAsNumbers(ProgramProperties.OBSERVED_LOCATION_LOCATION_LEVEL_IDS);
    this.aggregatedLandings = program.getPropertyAsBoolean(ProgramProperties.OBSERVED_LOCATION_AGGREGATED_LANDINGS_ENABLE);
    if (this.aggregatedLandings) {
      // Force some date properties
      this.observedLocationForm.showEndDateTime = true;
      this.observedLocationForm.showStartTime = false;
      this.observedLocationForm.showEndTime = false;
      this.observedLocationForm.filterStartDateDay = parseInt(program.getProperty(ProgramProperties.OBSERVED_LOCATION_AGGREGATED_LANDINGS_START_DAY));
      this.observedLocationForm.forceDurationDays = parseInt(program.getProperty(ProgramProperties.OBSERVED_LOCATION_AGGREGATED_LANDINGS_DAY_COUNT));
    }
    this.allowAddNewVessel = program.getPropertyAsBoolean(ProgramProperties.OBSERVED_LOCATION_CREATE_VESSEL_ENABLE);
    this.addLandingUsingHistoryModal = program.getPropertyAsBoolean(ProgramProperties.OBSERVED_LOCATION_SHOW_LANDINGS_HISTORY);

    let i18nSuffix = program.getProperty(ProgramProperties.I18N_SUFFIX);
    i18nSuffix = i18nSuffix !== 'legacy' ? i18nSuffix : '';
    this.i18nContext.suffix = i18nSuffix;

    this.landingEditor = program.getProperty<LandingEditor>(ProgramProperties.LANDING_EDITOR);
    this.showVesselType = program.getPropertyAsBoolean(ProgramProperties.VESSEL_TYPE_ENABLE);

    this.cd.detectChanges();
    const landingsTable = this.landingsTable;
    if (landingsTable) {
      landingsTable.i18nColumnSuffix = i18nSuffix;
      landingsTable.detailEditor = this.landingEditor;

      landingsTable.showDateTimeColumn = program.getPropertyAsBoolean(ProgramProperties.LANDING_DATE_TIME_ENABLE);
      landingsTable.showVesselTypeColumn = this.showVesselType;
      landingsTable.showObserversColumn = program.getPropertyAsBoolean(ProgramProperties.LANDING_OBSERVERS_ENABLE);
      landingsTable.showCreationDateColumn = program.getPropertyAsBoolean(ProgramProperties.LANDING_CREATION_DATE_ENABLE);
      landingsTable.showRecorderPersonColumn = program.getPropertyAsBoolean(ProgramProperties.LANDING_RECORDER_PERSON_ENABLE);
      landingsTable.showVesselBasePortLocationColumn = program.getPropertyAsBoolean(ProgramProperties.LANDING_VESSEL_BASE_PORT_LOCATION_ENABLE);
      landingsTable.showLocationColumn = program.getPropertyAsBoolean(ProgramProperties.LANDING_LOCATION_ENABLE);
      landingsTable.showSamplesCountColumn = program.getPropertyAsBoolean(ProgramProperties.LANDING_SAMPLES_COUNT_ENABLE);
    } else if (this.aggregatedLandingsTable) {
      this.aggregatedLandingsTable.nbDays = parseInt(program.getProperty(ProgramProperties.OBSERVED_LOCATION_AGGREGATED_LANDINGS_DAY_COUNT));
      this.aggregatedLandingsTable.programLabel = program.getProperty(ProgramProperties.OBSERVED_LOCATION_AGGREGATED_LANDINGS_PROGRAM);
    }

    this.$ready.next(true);

    // Listen program, to reload if changes
    this.startListenProgramRemoteChanges(program);
  }

  protected async onNewEntity(data: ObservedLocation, options?: EntityServiceLoadOptions): Promise<void> {
    // If is on field mode, fill default values
    if (this.isOnFieldMode) {
      data.startDateTime = moment();

      console.debug('[observed-location] New entity: set default values...');

      // Fill defaults, using filter applied on trips table
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
            first(),
            tap(() => this.save())
          )
          .subscribe()
      );
    }

    // Set contextual program, if any
    {
      const contextualProgram = this.context.getValue('program') as Program;
      if (contextualProgram?.label) {
        data.program = ReferentialRef.fromObject(contextualProgram);;
        this.$programLabel.next(data.program.label);
      }
    }
  }

  protected async onEntityLoaded(data: ObservedLocation, options?: EntityServiceLoadOptions): Promise<void> {

    const programLabel = data.program && data.program.label;
    this.$programLabel.next(programLabel);
  }

  protected async setValue(data: ObservedLocation) {
    // Set data to form
    this.observedLocationForm.value = data;

    console.info('[observed-location] Setting data', data);

    const isNew = isNil(data.id);
    if (!isNew) {
      // Propage to table parent
      this.table?.setParent(data)
    }
  }

  protected async ready(): Promise<any> {
    // Wait child loaded
    if (this.$ready.value !== true) {
      if (this.debug) console.debug('[observed-location] waiting program to be loaded...');
      await firstTruePromise(this.$ready);
    }

    if (!this.table) {
      if (this.debug) console.debug('[observed-location] waiting page (table) to be set...');
      return setTimeout(() => {
        return !!this.table;
      }, 200);
    }
  }

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
      // Use landings table as child, only if editable
      () => this.landingsTable?.canEdit && this.landingsTable,
      () => this.aggregatedLandingsTable
    ]);
  }

  protected async computeTitle(data: ObservedLocation): Promise<string> {

    // new data
    if (this.isNewData) {
      return this.translate.get('OBSERVED_LOCATION.NEW.TITLE').toPromise();
    }

    // Make sure page is ready (e.g. i18nContext has been loaded, in setProgram())
    await this.ready();

    // Existing data
    return this.translate.get(`OBSERVED_LOCATION.EDIT.${this.i18nContext.suffix}TITLE`, {
      location: data.location && (data.location.name || data.location.label),
      dateTime: data.startDateTime && this.dateFormat.transform(data.startDateTime) as string
    }).toPromise();
  }

  protected async computePageHistory(title: string): Promise<HistoryPageReference> {
    return {
      ... (await super.computePageHistory(title)),
      icon: 'location'
    };
  }

  protected async getJsonValueToSave(): Promise<any> {
    const json = await super.getJsonValueToSave();

    if (this.landingsTable && this.landingsTable.dirty && this.landingsTable.canEdit) {
      await this.landingsTable.save();
    }
    if (this.aggregatedLandingsTable && this.aggregatedLandingsTable.dirty) {
      await this.aggregatedLandingsTable.save();
    }

    return json;
  }

  protected getFirstInvalidTabIndex(): number {
    return this.observedLocationForm.invalid ? 0
      : ((this.landingsTable && this.landingsTable.invalid) || (this.aggregatedLandingsTable && this.aggregatedLandingsTable.invalid) ? 1
        : -1);
  }

  protected markForCheck() {
    this.cd.markForCheck();
  }
}
