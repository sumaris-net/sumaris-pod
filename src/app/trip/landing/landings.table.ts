import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Injector, Input, OnDestroy, OnInit} from "@angular/core";
import {ValidatorService} from "angular4-material-table";
import {environment, isNil, StatusIds} from "../../core/core.module";
import {
  Landing,
  ObservedLocation,
  personsToString,
  referentialToString,
  Trip,
  vesselSnapshotToString
} from "../services/trip.model";
import {LandingFilter, LandingService} from "../services/landing.service";
import {LandingValidatorService} from "../services/landing.validator";
import {AppMeasurementsTable} from "../measurement/measurements.table.class";
import {measurementValueToString} from "../services/model/measurement.model";
import {AcquisitionLevelCodes} from "../../referential/referential.module";
import {VesselSnapshotService} from "../../referential/services/vessel-snapshot.service";
import {Moment} from "moment";

export const LANDING_RESERVED_START_COLUMNS: string[] = ['vessel', 'dateTime', 'observers'];
export const LANDING_RESERVED_END_COLUMNS: string[] = ['comments'];

@Component({
  selector: 'app-landing-table',
  templateUrl: 'landings.table.html',
  providers: [
    {provide: ValidatorService, useExisting: LandingValidatorService}
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class LandingsTable extends AppMeasurementsTable<Landing, LandingFilter> implements OnInit, OnDestroy {

  private _parent: ObservedLocation | Trip;
  protected cd: ChangeDetectorRef;
  protected vesselSnapshotService: VesselSnapshotService;

  @Input() canEdit = true;
  @Input() canDelete = true;
  @Input() showFabButton = false;

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
    this._enable = this.canEdit;
    this.pageSize = 1000; // Do not use paginator
    this.vesselSnapshotService = injector.get(VesselSnapshotService);

    // Set default acquisition level
    this.acquisitionLevel = AcquisitionLevelCodes.LANDING;

    // FOR DEV ONLY ----
    this.debug = !environment.production;
  }

  ngOnInit() {
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
    this._parent = data;
    if (!data) {
      this.setFilter({});
    } else if (data instanceof ObservedLocation) {
      this.setFilter({observedLocationId: data.id}, {emitEvent: true/*refresh*/});
    } else if (data instanceof Trip) {
      this.setFilter({tripId: data.id}, {emitEvent: true/*refresh*/});
    }
  }

  async getMaxRankOrder(): Promise<number> {
    return super.getMaxRankOrder();
  }

  referentialToString = referentialToString;
  vesselSnapshotToString = vesselSnapshotToString;
  personsToString = personsToString;
  measurementValueToString = measurementValueToString;

  getLandingDate(landing?: Landing): Moment {
    if (isNil(landing) || isNil(landing.dateTime)) return undefined;

    // return nothing if the landing date equals parent date
    if (this._parent && this._parent instanceof ObservedLocation) {
      if (landing.dateTime.isSame(this._parent.startDateTime)) {
        return undefined;
      }
    }

    // default
    return landing.dateTime;
  }

  /* -- protected methods -- */

  protected markForCheck() {
    this.cd.markForCheck();
  }
}

