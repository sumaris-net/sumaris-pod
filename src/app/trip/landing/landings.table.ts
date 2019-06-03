import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Injector, Input, OnDestroy, OnInit} from "@angular/core";
import {ValidatorService} from "angular4-material-table";
import {AcquisitionLevelCodes, AppTableDataSource, environment, isNil} from "../../core/core.module";
import {
  getPmfmName,
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

const LANDING_RESERVED_START_COLUMNS: string[] = ['vessel', 'dateTime', 'observers'];
const LANDING_RESERVED_END_COLUMNS: string[] = ['comments'];

@Component({
  selector: 'app-landing-table',
  templateUrl: 'landings.table.html',
  styleUrls: ['landings.table.scss'],
  providers: [
    {provide: ValidatorService, useClass: LandingValidatorService}
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
        suppressErrors: false,
        onRowCreated: (row) => this.onRowCreated(row),
        reservedStartColumns: LANDING_RESERVED_START_COLUMNS,
        reservedEndColumns: LANDING_RESERVED_END_COLUMNS
      }
    );
    this.cd = injector.get(ChangeDetectorRef);
    this.i18nColumnPrefix = 'LANDING.TABLE.';
    this.autoLoad = false;
    this.inlineEdition = false;
    this.debug = !environment.production;
  }

  ngOnInit() {
    super.ngOnInit();

    // Set default acquisition Level
    if (isNil(this.acquisitionLevel)) {
      this.acquisitionLevel = AcquisitionLevelCodes.LANDING;
    }
  }

  setParent(data: ObservedLocation | Trip) {
    if (!data) {
      this.setFilter({});
    } else if (data instanceof ObservedLocation) {
      this.setFilter({observedLocationId: data.id});
    } else if (data instanceof Trip) {
      this.setFilter({tripId: data.id});
    }
  }

  referentialToString = referentialToString;
  getPmfmColumnHeader = getPmfmName;
  vesselFeaturesToString = vesselFeaturesToString;
  personsToString = personsToString;
  measurementValueToString = measurementValueToString;

  protected markForCheck() {
    this.cd.markForCheck();
  }
}

