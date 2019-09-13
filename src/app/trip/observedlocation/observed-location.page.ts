import {ChangeDetectionStrategy, Component, Injector, OnInit, ViewChild} from '@angular/core';
import {isNil, isNotNil} from '../../shared/shared.module';
import * as moment from "moment";
import {ObservedLocationForm} from "./observed-location.form";
import {EntityUtils, Landing, ObservedLocation} from "../services/trip.model";
import {ObservedLocationService} from "../services/observed-location.service";
import {LandingsTable} from "../landing/landings.table";
import {LocationLevelIds, ProgramProperties} from "../../referential/services/model";
import {AppDataEditorPage} from "../form/data-editor-page.class";
import {FormGroup} from "@angular/forms";
import {EditorDataServiceLoadOptions} from "../../shared/services/data-service.class";
import {ModalController} from "@ionic/angular";
import {LandingsTablesModal} from "../landing/landings-table.modal";

@Component({
  selector: 'page-observed-location',
  templateUrl: './observed-location.page.html',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ObservedLocationPage extends AppDataEditorPage<ObservedLocation> implements OnInit {

  landingEditor = 'landing';

  @ViewChild('observedLocationForm') observedLocationForm: ObservedLocationForm;

  @ViewChild('landingsTable') landingsTable: LandingsTable;

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

    // FOR DEV ONLY ----
    //this.debug = !environment.production;
  }

  ngOnInit() {
    super.ngOnInit();

    // Configure using program properties
    this.onProgramChanged
      .subscribe(program => {
        if (this.debug) console.debug(`[observed-location] Program ${program.label} loaded, with properties: `, program.properties);
        this.observedLocationForm.showEndDateTime = program.getPropertyAsBoolean(ProgramProperties.OBSERVED_LOCATION_END_DATE_TIME_ENABLE);
        this.observedLocationForm.locationLevelIds = program.getPropertyAsNumbers(ProgramProperties.OBSERVED_LOCATION_LOCATION_LEVEL_IDS);

        const landingEditor = program.getProperty(ProgramProperties.LANDING_EDITOR);
        this.landingEditor = (landingEditor === 'landing' || landingEditor === 'control') ? landingEditor : 'landing';
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
    // nothing to do
  }

  protected setValue(data: ObservedLocation) {
    this.observedLocationForm.value = data;

    if (data && isNotNil(data.id)) {
      console.debug("[observed-location] Sending program to landings table");
      this.landingsTable.setParent(data);
      this.landingsTable.program = data.program.label;
      //this.landingsTable.value = data.landings || [];
    }
  }

  protected async computeTitle(data: ObservedLocation): Promise<string> {
    // new data
    if (!data || isNil(data.id)) {
      return await this.translate.get('OBSERVED_LOCATION.NEW.TITLE').toPromise();
    }

    // Existing data
    return await this.translate.get('OBSERVED_LOCATION.EDIT.TITLE', {
      location: data.location && (data.location.name || data.location.label),
      dateTime: data.startDateTime && this.dateFormat.transform(data.startDateTime) as string
    }).toPromise();
  }

  protected getFirstInvalidTabIndex(): number {
    return this.observedLocationForm.invalid ? 0
      : (this.landingsTable.invalid ? 1
        : -1);
  }

  async onOpenLanding({id}) {
    const savedOrContinue = await this.saveIfDirtyAndConfirm();
    if (savedOrContinue) {
      await this.router.navigateByUrl(`/observations/${this.data.id}/${this.landingEditor}/${id}`);
    }
  }

  async onNewLanding(event?: any) {

    const landing = await this.openVesselSelectionModal();

    if (landing && landing.vesselFeatures) {
      const savedOrContinue = await this.saveIfDirtyAndConfirm();
      if (savedOrContinue) {
        await this.router.navigateByUrl(`/observations/${this.data.id}/${this.landingEditor}/new?vessel=${landing.vesselFeatures.vesselId}`);
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
}
