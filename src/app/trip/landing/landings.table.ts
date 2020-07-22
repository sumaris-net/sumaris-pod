import {
  AfterViewInit,
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  EventEmitter,
  Injector,
  Input,
  OnDestroy,
  OnInit,
  Output
} from "@angular/core";
import {TableElement, ValidatorService} from "angular4-material-table";

import {personsToString} from "../../core/services/model/person.model";
import {referentialToString} from "../../core/services/model/referential.model";
import {LandingFilter, LandingService} from "../services/landing.service";
import {AppMeasurementsTable} from "../measurement/measurements.table.class";
import {AcquisitionLevelCodes} from "../../referential/services/model/model.enum";
import {VesselSnapshotService} from "../../referential/services/vessel-snapshot.service";
import {Moment} from "moment";
import {LandingValidatorService} from "../services/validator/landing.validator";
import {Trip} from "../services/model/trip.model";
import {ObservedLocation} from "../services/model/observed-location.model";
import {Landing} from "../services/model/landing.model";
import {environment} from "../../../environments/environment";
import {LandingEditor} from "../../referential/services/config/program.config";
import {StatusIds} from "../../core/services/model/model.enum";

export const LANDING_RESERVED_START_COLUMNS: string[] = ['vessel', 'vesselType', 'vesselBasePortLocation', 'dateTime', 'observers'];
export const LANDING_RESERVED_END_COLUMNS: string[] = ['comments'];

@Component({
  selector: 'app-landings-table',
  templateUrl: 'landings.table.html',
  styleUrls: ['landings.table.scss'],
  providers: [
    {provide: ValidatorService, useExisting: LandingValidatorService}
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class LandingsTable extends AppMeasurementsTable<Landing, LandingFilter> implements OnInit, AfterViewInit, OnDestroy {

  private _parentDateTime;
  private _detailEditor: LandingEditor;
  private pageIndex;

  protected cd: ChangeDetectorRef;
  protected vesselSnapshotService: VesselSnapshotService;

  @Output() onNewTrip = new EventEmitter<{ id?: number; row: TableElement<Landing> }>();

  @Input() canEdit = true;
  @Input() canDelete = true;
  @Input() showFabButton = false;
  @Input() showError = true;

  @Input() set detailEditor(value: LandingEditor) {
    if (value !== this._detailEditor) {
      this._detailEditor = value;
      // TODO: should be set with another setter, configure from a ProgramProperties option
      this.inlineEdition = value === 'trip';
    }
  }

  get detailEditor(): LandingEditor {
    return this._detailEditor;
  }

  get isTripDetailEditor(): boolean {
    return this._detailEditor === 'trip';
  }

  @Input()
  set showObserversColumn(value: boolean) {
    this.setShowColumn('observers', value);
  }

  get showObserversColumn(): boolean {
    return this.getShowColumn('observers');
  }

  @Input()
  set showDateTimeColumn(value: boolean) {
    this.setShowColumn('dateTime', value);
  }

  get showDateTimeColumn(): boolean {
    return this.getShowColumn('dateTime');
  }

  @Input()
  set showIdColumn(value: boolean) {
    this.setShowColumn('id', value);
  }
  get showIdColumn(): boolean {
    return this.getShowColumn('id');
  }

  constructor(
    injector: Injector
  ) {
    super(injector,
      Landing,
      injector.get(LandingService),
      injector.get(ValidatorService),
      {
        prependNewElements: false,
        suppressErrors: environment.production,
        reservedStartColumns: LANDING_RESERVED_START_COLUMNS,
        reservedEndColumns: LANDING_RESERVED_END_COLUMNS,
        mapPmfms: (pmfms) => pmfms.filter(p => p.required)
      });
    this.cd = injector.get(ChangeDetectorRef);
    this.i18nColumnPrefix = 'LANDING.TABLE.';
    this.autoLoad = false; // waiting parent to be loaded
    this.inlineEdition = false;
    this.confirmBeforeDelete = true;
    // TODO  ::: USE NAVIGATOR (check service)
    this.pageSize = 200; // normal high value
    this.vesselSnapshotService = injector.get(VesselSnapshotService);

    // Set default acquisition level
    this.acquisitionLevel = AcquisitionLevelCodes.LANDING;

    // FOR DEV ONLY ----
    this.debug = !environment.production;
  }

  ngOnInit() {

    this._enabled = this.canEdit;

    super.ngOnInit();

    this.registerAutocompleteField('vesselSnapshot', {
      service: this.vesselSnapshotService,
      attributes: this.settings.getFieldDisplayAttributes('vesselSnapshot', ['exteriorMarking', 'name']),
      filter: {
        statusIds: [StatusIds.ENABLE, StatusIds.TEMPORARY]
      }
    });
  }

  setParent(data: ObservedLocation | Trip) {
    if (!data) {
      this._parentDateTime = undefined;
      this.setFilter({});
    } else if (data instanceof ObservedLocation) {
      this._parentDateTime = data.startDateTime;
      this.setFilter({observedLocationId: data.id}, {emitEvent: true/*refresh*/});
    } else if (data instanceof Trip) {
      this._parentDateTime = data.departureDateTime;
      this.setFilter({tripId: data.id}, {emitEvent: true/*refresh*/});
    }
  }

  /**
   * Publish the existing protected method (need by observed location page)
   */
  async getMaxRankOrder(): Promise<number> {
    return super.getMaxRankOrder();
  }

  referentialToString = referentialToString;
  personsToString = personsToString;

  getLandingDate(landing?: Landing): Moment {
    if (!landing || !landing.dateTime) return undefined;

    // return nothing if the landing date equals parent date
    if (this._parentDateTime && landing.dateTime.isSame(this._parentDateTime)) {
      return undefined;
    }

    // default
    return landing.dateTime;
  }

  addRow(event?: any): boolean {

    if (this.isTripDetailEditor) {
      if (!this._enabled) return false;
      if (this.debug) console.debug("[landings-table] Asking for new landing...");

      // Force modal
      this.openNewRowDetail(event);
      return false;
    }

    // default behavior
    return super.addRow(event);
  }

  confirmAndEditTrip(event?: MouseEvent, row?: TableElement<Landing>): boolean {
    if (event) event.stopPropagation();

    if (!this.confirmEditCreate(event, row)) {
      return false;
    }

    if (row.currentData.tripId) {
      // Edit trip
      this.onOpenRow.emit({id: row.currentData.tripId, row: row});
    } else {
      // New trip
      this.onNewTrip.emit({id: null, row: row});
    }
  }


  /* -- protected methods -- */

  protected markForCheck() {
    this.cd.markForCheck();
  }
}

