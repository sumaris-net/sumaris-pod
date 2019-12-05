import {ChangeDetectionStrategy, Component, Injector, OnInit, ViewChild} from '@angular/core';
import {fadeInOutAnimation, isNil, isNotNil} from '../../shared/shared.module';
import * as moment from "moment";
import {ObservedLocationForm} from "./observed-location.form";
import {EntityUtils, Landing, ObservedLocation} from "../services/trip.model";
import {ObservedLocationService} from "../services/observed-location.service";
import {LandingsTable} from "../landing/landings.table";
import {LandingEditor, ProgramProperties} from "../../referential/services/model";
import {AppDataEditorPage} from "../form/data-editor-page.class";
import {FormGroup} from "@angular/forms";
import {EditorDataServiceLoadOptions} from "../../shared/services/data-service.class";
import {ModalController} from "@ionic/angular";
import {LandingsTablesModal} from "../landing/landings-table.modal";
import {environment} from "../../core/core.module";
import {HistoryPageReference} from "../../core/services/model";
import {TableElement} from "angular4-material-table";

@Component({
  selector: 'app-observed-location-page',
  templateUrl: './observed-location.page.html',
  animations: [fadeInOutAnimation],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ObservedLocationPage extends AppDataEditorPage<ObservedLocation, ObservedLocationService> implements OnInit {


  landingEditor: LandingEditor;

  @ViewChild('observedLocationForm', { static: true }) observedLocationForm: ObservedLocationForm;

  @ViewChild('landingsTable', { static: true }) landingsTable: LandingsTable;

  constructor(
    injector: Injector,
    dataService: ObservedLocationService,
    protected modalCtrl: ModalController
  ) {
    super(injector,
      ObservedLocation,
      dataService);

    this.defaultBackHref = "/observations";
    this.idAttribute = 'observedLocationId';

    // Default value
    this.landingEditor = 'landing';

    // FOR DEV ONLY ----
    this.debug = !environment.production;
  }

  ngOnInit() {
    super.ngOnInit();

    // Configure using program properties
    this.onProgramChanged
      .subscribe(program => {
        if (this.debug) console.debug(`[observed-location] Program ${program.label} loaded, with properties: `, program.properties);
        this.observedLocationForm.showEndDateTime = program.getPropertyAsBoolean(ProgramProperties.OBSERVED_LOCATION_END_DATE_TIME_ENABLE);
        this.observedLocationForm.locationLevelIds = program.getPropertyAsNumbers(ProgramProperties.OBSERVED_LOCATION_LOCATION_LEVEL_IDS);
        this.landingsTable.showDateTimeColumn = program.getPropertyAsBoolean(ProgramProperties.LANDING_DATE_TIME_ENABLE);

        const landingEditor = program.getProperty<LandingEditor>(ProgramProperties.LANDING_EDITOR);
        this.landingEditor = (landingEditor === 'landing' || landingEditor === 'control' || landingEditor === 'landed_trip') ? landingEditor : 'landing';
      });
  }

  protected get form(): FormGroup {
    return this.observedLocationForm.form;
  }

  protected registerFormsAndTables() {
    // Register forms & tables
    this.registerForms([this.observedLocationForm])
      .registerTables([this.landingsTable]);
  }

  protected async onNewEntity(data: ObservedLocation, options?: EditorDataServiceLoadOptions): Promise<void> {
    // If is on field mode, fill default values
    if (this.isOnFieldMode) {
      data.startDateTime = moment();
    }
  }

  protected async onEntityLoaded(data: ObservedLocation, options?: EditorDataServiceLoadOptions): Promise<void> {
    // Move to second tab
    this.selectedTabIndex = 1;
    this.tabGroup.realignInkBar();
  }

  protected setValue(data: ObservedLocation) {

    const isNew = isNil(data.id);
    if (!isNew) {
      // Propagate program to form
      this.programSubject.next(data.program.label);
    }

    // Set data to form
    this.observedLocationForm.value = data;

    // Propagate parent to landings table
    if (this.landingsTable && data) {
      if (this.debug) console.debug("[observed-location] Propagate observed location to landings table");
      this.landingsTable.setParent(data);
    }
  }

  protected async computeTitle(data: ObservedLocation): Promise<string> {
    // new data
    if (this.isNewData) {
      return await this.translate.get('OBSERVED_LOCATION.NEW.TITLE').toPromise();
    }

    // Existing data
    const title = await this.translate.get('OBSERVED_LOCATION.EDIT.TITLE', {
      location: data.location && (data.location.name || data.location.label),
      dateTime: data.startDateTime && this.dateFormat.transform(data.startDateTime) as string
    }).toPromise();

    return title;
  }

  protected getFirstInvalidTabIndex(): number {
    return this.observedLocationForm.invalid ? 0
      : (this.landingsTable && this.landingsTable.invalid ? 1
        : -1);
  }

  async onOpenLanding({ id, row }) {
    const savedOrContinue = await this.saveIfDirtyAndConfirm();
    if (savedOrContinue) {
      // default url
      let url = `/observations/${this.data.id}/${this.landingEditor}/${id}`;
      // specific parameter for landed_trip : /landingId/tripId
      if (this.landingEditor === 'landed_trip') {
        url = isNotNil(row.currentData.tripId) ? url.concat(`/${row.currentData.tripId}`) : url.concat('/new');
      }
      await this.router.navigateByUrl(url);
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
      this.loading = true;
      this.markForCheck();

      try {
        const landing = await this.openVesselSelectionModal();
        if (landing && landing.vesselSnapshot) {
          const rankOrder = (await this.landingsTable.getMaxRankOrder() || 0) + 1;
          await this.router.navigateByUrl(`/observations/${this.data.id}/${this.landingEditor}/new?vessel=${landing.vesselSnapshot.id}&rankOrder=${rankOrder}`);
        }
      } finally {
        this.loading = false;
        this.markForCheck();
      }
    }
  }

  /* -- protected methods -- */

  async openVesselSelectionModal(): Promise<Landing | undefined> {
    const modal = await this.modalCtrl.create({
      component: LandingsTablesModal, componentProps: {
        program: this.data.program && this.data.program.label,
        acquisitionLevel: this.landingsTable.acquisitionLevel,
        filter: {
          programLabel: this.data.program && this.data.program.label,
          startDate: moment(this.data.startDateTime).subtract(15, "days"),
          endDate: moment(this.data.startDateTime).add(1, "days"),
          locationId: EntityUtils.isNotEmpty(this.data.location) ? this.data.location.id : undefined
        }
      }, keyboardClose: true
    });

    // Open the modal
    modal.present();

    // Wait until closed
    const res = await modal.onDidDismiss();

    // If new vessel added, use it
    if (res && res.data instanceof Landing) {
      console.debug("[observed-location] Vessel selection modal result:", res.data);
      return res.data as Landing;
    } else {
      console.debug("[observed-location] No vessel added (user cancelled)");
    }
  }

  protected addToPageHistory(page: HistoryPageReference) {
    // Add entity icon
    page.matIcon = 'verified_user';
    super.addToPageHistory(page);
  }
}
