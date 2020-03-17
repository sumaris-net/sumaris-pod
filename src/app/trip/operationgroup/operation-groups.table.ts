import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Injector, Input, OnDestroy, OnInit} from "@angular/core";
import {environment, isNotNil} from "../../core/core.module";
import {PmfmStrategy, referentialToString, Trip} from "../services/trip.model";
import {Platform} from "@ionic/angular";
import {AcquisitionLevelCodes} from "../../referential/services/model";
import {OperationFilter} from "../services/operation.service";
import {AppMeasurementsTable} from "../measurement/measurements.table.class";
import {OperationGroup} from "../services/model/trip.model";
import {OperationGroupService} from "../services/operation-group.service";
import {OperationGroupValidatorService} from "../services/operation-group.validator";
import {MetierRef} from "../../referential/services/model/taxon.model";
import {BehaviorSubject} from "rxjs";
import {MeasurementValuesUtils} from "../services/model/measurement.model";


@Component({
  selector: 'app-operation-group-table',
  templateUrl: 'operation-groups.table.html',
  styleUrls: ['operation-groups.table.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class OperationGroupTable extends AppMeasurementsTable<OperationGroup, OperationFilter> implements OnInit, OnDestroy {

  displayAttributes: {
    [key: string]: string[]
  };

  @Input() tripId: number;

  @Input() metiersSubject: BehaviorSubject<MetierRef[]>;

  constructor(
    injector: Injector,
    protected platform: Platform,
    protected validatorService: OperationGroupValidatorService,
    protected dataService: OperationGroupService,
    protected cd: ChangeDetectorRef
  ) {
    super(injector,
      OperationGroup,
      dataService,
      validatorService,
      {
        prependNewElements: false,
        suppressErrors: environment.production,
        reservedStartColumns: ['metier', 'physicalGear', 'targetSpecies'],
        reservedEndColumns: platform.is('mobile') ? [] : ['comments'],
        mapPmfms: (pmfms) => this.mapPmfms(pmfms),
      });
    this.i18nColumnPrefix = 'TRIP.OPERATION.LIST.';
    this.autoLoad = false; // waiting parent to be loaded
    this.inlineEdition = true;
    this.confirmBeforeDelete = true;
    this.pageSize = 1000; // Do not use paginator

    // Set default acquisition level
    this.acquisitionLevel = AcquisitionLevelCodes.OPERATION;

    // FOR DEV ONLY ----
    this.debug = !environment.production;
  }

  ngOnInit() {
    super.ngOnInit();

    this.displayAttributes = {
      gear: this.settings.getFieldDisplayAttributes('gear'),
      taxonGroup: ['taxonGroup.label', 'taxonGroup.name']
    };

    // Metier combo
    this.registerAutocompleteField('metier', {
      showAllOnFocus: true,
      items: this.metiersSubject
    });

    // Apply trip id, if already set
    if (isNotNil(this.tripId)) {
      this.setTripId(this.tripId);
    }
  }

  setTrip(data: Trip) {
    this.setTripId(data && data.id || undefined);
  }

  setTripId(id: number) {
    this.tripId = id;
    const filter = this.filter || {};
    filter.tripId = id;
    this.dataSource.serviceOptions = this.dataSource.serviceOptions || {};
    this.dataSource.serviceOptions.tripId = id;
    this.setFilter(filter, {emitEvent: isNotNil(id)});
  }

  referentialToString = referentialToString;
  measurementValueToString = MeasurementValuesUtils.valueToString;

  /* -- protected methods -- */

  protected markForCheck() {
    this.cd.markForCheck();
  }

  private mapPmfms(pmfms: PmfmStrategy[]): PmfmStrategy[] {

    if (this.platform.is('mobile')) {
      // hide pmfms on mobile
      return [];
    }

    return pmfms;
  }
}

