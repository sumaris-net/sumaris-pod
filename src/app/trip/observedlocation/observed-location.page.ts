import {ChangeDetectionStrategy, Component, Injector, OnInit, ViewChild} from '@angular/core';
import {fadeInOutAnimation, isNil} from '../../shared/shared.module';
import * as moment from "moment";
import {ObservedLocationForm} from "./observed-location.form";
import {ObservedLocationService} from "../services/observed-location.service";
import {LandingsTable} from "../landing/landings.table";
import {AppRootDataEditor} from "../../data/form/root-data-editor.class";
import {FormGroup} from "@angular/forms";
import {EntityServiceLoadOptions} from "../../shared/services/entity-service.class";
import {ModalController} from "@ionic/angular";
import {environment} from "../../core/core.module";
import {HistoryPageReference} from "../../core/services/model/settings.model";
import {ReferentialUtils} from "../../core/services/model/referential.model";
import {SelectVesselsModal} from "./vessels/select-vessel.modal";
import {ObservedLocation} from "../services/model/observed-location.model";
import {Landing} from "../services/model/landing.model";
import {LandingFilter} from "../services/landing.service";
import {LandingEditor, ProgramProperties} from "../../referential/services/config/program.config";
import {VesselSnapshot} from "../../referential/services/model/vessel-snapshot.model";
import {BehaviorSubject} from "rxjs";
import {firstNotNilPromise} from "../../shared/observables";
import {filter} from "rxjs/operators";
import {AggregatedLandingsTable} from "../aggregated-landing/aggregated-landings.table";

@Component({
  selector: 'app-observed-location-page',
  templateUrl: './observed-location.page.html',
  animations: [fadeInOutAnimation],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ObservedLocationPage extends AppRootDataEditor<ObservedLocation, ObservedLocationService> implements OnInit {

  aggregatedLandings: boolean;

  $childLoaded = new BehaviorSubject<boolean>(false);

  @ViewChild('observedLocationForm', {static: true}) observedLocationForm: ObservedLocationForm;

  @ViewChild('landingsTable') landingsTable: LandingsTable;

  @ViewChild('aggregatedLandingsTable', {static: false}) aggregatedLandingsTable: AggregatedLandingsTable;

  get landingEditor(): LandingEditor {
    return this.landingsTable ? this.landingsTable.detailEditor : undefined;
  }

  set landingEditor(value: LandingEditor) {
    if (this.landingsTable) this.landingsTable.detailEditor = value;
  }

  constructor(
    injector: Injector,
    dataService: ObservedLocationService,
    protected modalCtrl: ModalController
  ) {
    super(injector,
      ObservedLocation,
      dataService,
      {
        pathIdAttribute: 'observedLocationId',
        tabCount: 2
      });

    this.defaultBackHref = "/observations";


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
        this.aggregatedLandings = program.getPropertyAsBoolean(ProgramProperties.OBSERVED_LOCATION_AGGREGATED_LANDINGS_ENABLE);
        this.cd.detectChanges();

        if (this.landingsTable) {
          this.landingsTable.showDateTimeColumn = program.getPropertyAsBoolean(ProgramProperties.LANDING_DATE_TIME_ENABLE);
          const editorName = program.getProperty<LandingEditor>(ProgramProperties.LANDING_EDITOR);
          this.landingsTable.detailEditor = (editorName === 'landing' || editorName === 'control' || editorName === 'trip') ? editorName : 'landing';

        } else if (this.aggregatedLandingsTable) {

          this.aggregatedLandingsTable.nbDays = parseInt(program.getProperty(ProgramProperties.OBSERVED_LOCATION_AGGREGATED_LANDINGS_DAY_COUNT));
          this.aggregatedLandingsTable.program = program.getProperty(ProgramProperties.OBSERVED_LOCATION_AGGREGATED_LANDINGS_PROGRAM);
        }

        this.$childLoaded.next(true);
        // this.registerAdditionalFormsAndTables();
      });
  }

  protected get form(): FormGroup {
    return this.observedLocationForm.form;
  }

  protected registerForms() {
    // Register forms & tables
    this.addChildForms([
      this.observedLocationForm,
      () => this.landingsTable
    ]);
  }

  protected async onNewEntity(data: ObservedLocation, options?: EntityServiceLoadOptions): Promise<void> {
    // If is on field mode, fill default values
    if (this.isOnFieldMode) {
      data.startDateTime = moment();
    }
  }

  protected async onEntityLoaded(data: ObservedLocation, options?: EntityServiceLoadOptions): Promise<void> {
    // Move to second tab
    this.selectedTabIndex = 1;
    this.tabGroup.realignInkBar();
  }

  protected async setValue(data: ObservedLocation) {

    const isNew = isNil(data.id);
    if (!isNew) {
      // Propagate program to form
      this.programSubject.next(data.program.label);
    }

    // Set data to form
    this.observedLocationForm.value = data;

    // Wait for child table ready
    await this.ready();
    this.updateViewState(data);

    // Propagate parent to landings table
    if (this.landingsTable && data) {
      if (this.debug) console.debug("[observed-location] Propagate observed location to landings table");
      this.landingsTable.setParent(data);
    }
    if (this.aggregatedLandingsTable) {
      if (this.debug) console.debug("[observed-location] Propagate observed location to aggregated landings form");
      this.aggregatedLandingsTable.setParent(data);
    }
  }

  protected async ready(): Promise<void> {
    // Wait child loaded
    if (this.$childLoaded.getValue() !== true) {
      if (this.debug) console.debug('[observed-location] waiting child to be ready...');
      await firstNotNilPromise(this.$childLoaded
        .pipe(
          filter((childLoaded) => childLoaded === true)
        ));
    }
  }

  protected async getJsonValueToSave(): Promise<any> {
    const json = await super.getJsonValueToSave();

    if (this.landingsTable && this.landingsTable.dirty) {
      await this.landingsTable.save();
    }

    return json;
  }

  protected computeTitle(data: ObservedLocation): Promise<string> {
    // new data
    if (this.isNewData) {
      return this.translate.get('OBSERVED_LOCATION.NEW.TITLE').toPromise();
    }

    // Existing data
    return this.translate.get('OBSERVED_LOCATION.EDIT.TITLE', {
      location: data.location && (data.location.name || data.location.label),
      dateTime: data.startDateTime && this.dateFormat.transform(data.startDateTime) as string
    }).toPromise();
  }

  protected getFirstInvalidTabIndex(): number {
    return this.observedLocationForm.invalid ? 0
      : (this.landingsTable && this.landingsTable.invalid ? 1
        : -1);
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
      this.loading = true;
      this.markForCheck();

      try {
        const vessel = await this.openSelectVesselModal();
        if (vessel) {
          const rankOrder = (await this.landingsTable.getMaxRankOrder() || 0) + 1;
          await this.router.navigateByUrl(`/observations/${this.data.id}/${this.landingEditor}/new?vessel=${vessel.id}&rankOrder=${rankOrder}`);
        }
      } finally {
        this.loading = false;
        this.markForCheck();
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
      this.loading = true;
      this.markForCheck();

      try {
        await this.router.navigateByUrl(`/observations/${this.data.id}/${this.landingEditor}/new?vessel=${row.currentData.vesselSnapshot.id}&landing=${row.currentData.id}`);
      } finally {
        this.loading = false;
        this.markForCheck();
      }
    }

  }

  /* -- protected methods -- */

  async openSelectVesselModal(): Promise<VesselSnapshot | undefined> {
    if (!this.data.startDateTime || !this.data.program) {
      throw new Error('Root entity has no program and start date. Cannot open select vessels modal');
    }

    const startDate = this.data.startDateTime.clone().add(-15, "days");
    const endDate = this.data.startDateTime;

    const landingFilter = <LandingFilter>{
      programLabel: this.data.program && this.data.program.label,
      startDate,
      endDate,
      locationId: ReferentialUtils.isNotEmpty(this.data.location) ? this.data.location.id : undefined
    };

    const modal = await this.modalCtrl.create({
      component: SelectVesselsModal,
      componentProps: {
        allowMultiple: false,
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
      return data as VesselSnapshot;
    } else {
      console.debug("[observed-location] Vessel selection modal was cancelled");
    }
  }

  protected addToPageHistory(page: HistoryPageReference) {
    // Add entity icon
    page.matIcon = 'verified_user';
    super.addToPageHistory(page);
  }

  addRow($event: MouseEvent) {
    if (this.landingsTable) {
      this.landingsTable.addRow($event);
    } else if (this.aggregatedLandingsTable) {
      this.aggregatedLandingsTable.addRow($event);
    }
  }
}
