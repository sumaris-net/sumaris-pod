import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Injector, OnDestroy, OnInit} from "@angular/core";
import {ValidatorService} from "angular4-material-table";
import {AcquisitionLevelCodes, AppTableDataSource, isNil} from "../../core/core.module";
import {
  getPmfmName,
  Landing,
  ObservedLocation,
  personsToString,
  referentialToString,
  Trip,
  vesselFeaturesToString
} from "../services/trip.model";
import {isNotNil} from "../../shared/shared.module";
import {LandingFilter, LandingService} from "../services/landing.service";
import {LandingValidatorService} from "../services/landing.validator";
import {AppMeasurementsTable} from "../measurement/measurements.table.class";

const LANDING_RESERVED_START_COLUMNS: string[] = ['vessel', 'landingDateTime', 'observers'];
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

  constructor(
    injector: Injector,
    landingService: LandingService,
    protected cd: ChangeDetectorRef
  ) {
    super(injector,
      Landing,
      LANDING_RESERVED_START_COLUMNS,
      LANDING_RESERVED_END_COLUMNS,
      new AppTableDataSource<Landing, LandingFilter>(Landing,
        landingService,
        null, {
          prependNewElements: false,
          suppressErrors: false,
          useRowValidator: false,
          onRowCreated: (row) => this.onRowCreated(row)
        })
    );
    this.i18nColumnPrefix = 'LANDING.TABLE.';
    this.autoLoad = false;
    this.debug = true;
  }

  ngOnInit() {
    super.ngOnInit();

    // Set default acquisition Level
    if (isNil(this.acquisitionLevel)) {
      this.acquisitionLevel = AcquisitionLevelCodes.LANDING;
    }
  }

  setParent(data: ObservedLocation | Trip) {
    if (data) {
      if (data instanceof ObservedLocation) {
        this.setParentData({observedLocationId: data.id});
      } else if (data instanceof Trip) {
        this.setParentData({tripId: data.id});
      }
    }
  }

  setParentData(parent: any) {
    this.filter = Object.assign(this.filter || {}, parent);
    this.dataSource.serviceOptions = Object.assign(this.dataSource.serviceOptions || {}, parent);
    if (isNotNil(parent)) {
      this.onRefresh.emit();
    }
  }

  referentialToString = referentialToString;
  getPmfmColumnHeader = getPmfmName;
  vesselFeaturesToString = vesselFeaturesToString;
  personsToString = personsToString;

  protected markForCheck() {
    this.cd.markForCheck();
  }
}

