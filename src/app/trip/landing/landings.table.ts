import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Injector, Input, OnDestroy, OnInit} from "@angular/core";
import {ValidatorService} from "angular4-material-table";
import {environment} from "../../core/core.module";
import {
  Landing,
  ObservedLocation,
  personsToString,
  referentialToString,
  Trip,
  vesselFeaturesToString
} from "../services/trip.model";
import {LandingFilter, LandingService} from "../services/landing.service";
import {LandingValidatorService} from "../services/landing.validator";
import {AppMeasurementsTable} from "../measurement/measurements.table.class";
import {measurementValueToString} from "../services/model/measurement.model";
import {AcquisitionLevelCodes} from "../../referential/referential.module";

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

  protected cd: ChangeDetectorRef;

  @Input() canEdit = true;
  @Input() canDelete = true;

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
    this._enable = this.canEdit;

    // Set default acquisition level
    this.acquisitionLevel = AcquisitionLevelCodes.LANDING;

    // FOR DEV ONLY ----
    this.debug = !environment.production;
  }

  ngOnInit() {
    super.ngOnInit();
  }

  setParent(data: ObservedLocation | Trip) {
    if (!data) {
      this.setFilter({});
    } else if (data instanceof ObservedLocation) {
      this.setFilter({observedLocationId: data.id}, {emitEvent: true/*refresh*/});
    } else if (data instanceof Trip) {
      this.setFilter({tripId: data.id}, {emitEvent: true/*refresh*/});
    }
  }

  referentialToString = referentialToString;
  vesselFeaturesToString = vesselFeaturesToString;
  personsToString = personsToString;
  measurementValueToString = measurementValueToString;

  /* -- protected methods -- */

  protected markForCheck() {
    this.cd.markForCheck();
  }
}

