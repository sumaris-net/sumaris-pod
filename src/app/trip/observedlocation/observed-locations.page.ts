import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Injector, OnInit, ViewChild} from "@angular/core";
import {TableElement} from "@e-is/ngx-material-table";
import {ActivatedRoute, Router} from "@angular/router";
import {ModalController} from "@ionic/angular";
import {Location} from "@angular/common";
import {ReferentialRefService} from "../../referential/services/referential-ref.service";
import {FormBuilder} from "@angular/forms";
import {EntitiesTableDataSource, PersonService, PersonUtils} from '@sumaris-net/ngx-components';
import {ObservedLocationService} from "../services/observed-location.service";
import {LocationLevelIds} from '@app/referential/services/model/model.enum';
import {LocalSettingsService}  from "@sumaris-net/ngx-components";
import {PlatformService}  from "@sumaris-net/ngx-components";
import {ObservedLocation} from "../services/model/observed-location.model";
import {SharedValidators} from "@sumaris-net/ngx-components";
import {StatusIds}  from "@sumaris-net/ngx-components";
import {AppRootTable} from '@app/data/table/root-table.class';
import {OBSERVED_LOCATION_FEATURE_NAME, TRIP_CONFIG_OPTIONS} from "../services/config/trip.config";
import {RESERVED_END_COLUMNS, RESERVED_START_COLUMNS}  from "@sumaris-net/ngx-components";
import {environment} from '@environments/environment';
import {ConfigService}  from "@sumaris-net/ngx-components";
import {BehaviorSubject} from "rxjs";
import {ObservedLocationOfflineModal} from "./offline/observed-location-offline.modal";
import {ProgramRefService} from '@app/referential/services/program-ref.service';
import {DATA_CONFIG_OPTIONS} from "src/app/data/services/config/data.config";
import {HammerSwipeEvent} from "@sumaris-net/ngx-components";
import {ObservedLocationFilter, ObservedLocationOfflineFilter} from "../services/filter/observed-location.filter";
import {MatExpansionPanel} from "@angular/material/expansion";


export const ObservedLocationsPageSettingsEnum = {
  PAGE_ID: "observedLocations",
  FILTER_KEY: "filter",
  FEATURE_ID: OBSERVED_LOCATION_FEATURE_NAME
};

@Component({
  selector: 'app-observed-locations-page',
  templateUrl: 'observed-locations.page.html',
  styleUrls: ['observed-locations.page.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ObservedLocationsPage extends
  AppRootTable<ObservedLocation, ObservedLocationFilter, number> implements OnInit {

  highlightedRow: TableElement<ObservedLocation>;
  $title = new BehaviorSubject<string>('');

  @ViewChild(MatExpansionPanel, {static: true}) filterExpansionPanel: MatExpansionPanel;

  constructor(
    protected injector: Injector,
    protected route: ActivatedRoute,
    protected router: Router,
    protected platform: PlatformService,
    protected location: Location,
    protected modalCtrl: ModalController,
    protected settings: LocalSettingsService,
    protected dataService: ObservedLocationService,
    protected personService: PersonService,
    protected referentialRefService: ReferentialRefService,
    protected programRefService: ProgramRefService,
    protected formBuilder: FormBuilder,
    protected configService: ConfigService,
    protected cd: ChangeDetectorRef
  ) {
    super(route, router, platform, location, modalCtrl, settings,
      RESERVED_START_COLUMNS
        .concat([
          'quality',
          'program',
          'location',
          'startDateTime',
          'observers',
          'comments'])
        .concat(RESERVED_END_COLUMNS),
      dataService,
      new EntitiesTableDataSource(ObservedLocation, dataService),
      null, // Filter
      injector
    );
    this.i18nColumnPrefix = 'OBSERVED_LOCATION.TABLE.';
    this.filterForm = formBuilder.group({
      program: [null, SharedValidators.entity],
      location: [null, SharedValidators.entity],
      startDate: [null, SharedValidators.validDate],
      endDate: [null, SharedValidators.validDate],
      synchronizationStatus: [null],
      recorderDepartment: [null, SharedValidators.entity],
      recorderPerson: [null, SharedValidators.entity],
      // TODO: add observer filter ?
      //,'observer': [null]
    });
    this.autoLoad = false;
    this.defaultSortBy = 'startDateTime';
    this.defaultSortDirection = 'desc';

    this.settingsId = ObservedLocationsPageSettingsEnum.PAGE_ID; // Fixed value, to be able to reuse it in the editor page
    this.featureId = ObservedLocationsPageSettingsEnum.FEATURE_ID;

    // FOR DEV ONLY ----
    this.debug = !environment.production;
  }

  ngOnInit() {
    super.ngOnInit();

    // Programs combo (filter)
    this.registerAutocompleteField('program', {
      service: this.referentialRefService,
      filter: {
        entityName: 'Program'
      },
      mobile: this.mobile
    });

    // Locations combo (filter)
    this.registerAutocompleteField('location', {
      service: this.referentialRefService,
      filter: {
        entityName: 'Location',
        levelIds: [LocationLevelIds.AUCTION, LocationLevelIds.PORT]
      },
      mobile: this.mobile
    });

    // Combo: recorder department
    this.registerAutocompleteField('department', {
      service: this.referentialRefService,
      filter: {
        entityName: 'Department'
      },
      mobile: this.mobile
    });

    // Combo: recorder person
    this.registerAutocompleteField('person', {
      service: this.personService,
      filter: {
        statusIds: [StatusIds.TEMPORARY, StatusIds.ENABLE]
      },
      attributes: ['lastName', 'firstName', 'department.name'],
      displayWith: PersonUtils.personToString,
      mobile: this.mobile
    });

    this.registerSubscription(
      this.configService.config.subscribe(config => {
        const title = config && config.getProperty(TRIP_CONFIG_OPTIONS.OBSERVED_LOCATION_NAME);
        this.$title.next(title);

        // Disable quality column
        this.setShowColumn('quality', config.getPropertyAsBoolean(DATA_CONFIG_OPTIONS.DATA_QUALITY_PROCESS_ENABLE));
      })
    );

    // Restore filter from settings, or load all
    this.restoreFilterOrLoad();
  }

  clickRow(event: MouseEvent|undefined, row: TableElement<ObservedLocation>): boolean {
    this.highlightedRow = row;
    return super.clickRow(event, row);
  }

  applyFilterAndClosePanel(event?: UIEvent) {
    this.onRefresh.emit(event);
    this.filterExpansionPanel.close();
  }

  resetFilter(event?: UIEvent) {
    super.resetFilter(event);
    this.filterExpansionPanel.close();
  }

  /**
   * Action triggered when user swipes
   */
  onSwipeTab(event: HammerSwipeEvent): boolean {
    // DEBUG
    // if (this.debug) console.debug("[observed-locations] onSwipeTab()");

    // Skip, if not a valid swipe event
    if (!event
      || event.defaultPrevented || (event.srcEvent && event.srcEvent.defaultPrevented)
      || event.pointerType !== 'touch'
    ) {
      return false;
    }

    this.toggleSynchronizationStatus();
    return true;
  }

  async openTrashModal(event?: UIEvent) {
    console.debug('[observed-locations] Opening trash modal...');
    // TODO BLA
    /*const modal = await this.modalCtrl.create({
      component: TripTrashModal,
      componentProps: {
        synchronizationStatus: this.filter.synchronizationStatus
      },
      keyboardClose: true,
      cssClass: 'modal-large'
    });

    // Open the modal
    await modal.present();

    // On dismiss
    const res = await modal.onDidDismiss();
    if (!res) return; // CANCELLED*/
  }

  async prepareOfflineMode(event?: UIEvent, opts?: {
    toggleToOfflineMode?: boolean;
    showToast?: boolean;
    filter?: any;
  }): Promise<undefined | boolean> {
    if (this.importing) return; // Skip

    if (event) {
      const feature = this.settings.getOfflineFeature(this.dataService.featureName) || {
        name: this.dataService.featureName
      };
      const value = <ObservedLocationOfflineFilter>{
        ...this.filter,
        ...feature.filter
      };
      const modal = await this.modalCtrl.create({
        component: ObservedLocationOfflineModal,
        componentProps: {
          value
        }, keyboardClose: true
      });

      // Open the modal
      modal.present();

      // Wait until closed
      const res = await modal.onDidDismiss();
      if (!res || !res.data) return; // User cancelled

      // Update feature filter, and save it into settings
      feature.filter = res && res.data;
      this.settings.saveOfflineFeature(feature);

      // DEBUG
      console.debug('[observed-location-table] Will prepare offline mode, using filter:', feature.filter);
    }

    return super.prepareOfflineMode(event, opts);
  }

  /* -- protected methods -- */



  protected markForCheck() {
    this.cd.markForCheck();
  }
}
