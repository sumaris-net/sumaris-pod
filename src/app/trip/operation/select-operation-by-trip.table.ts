import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnDestroy, OnInit} from '@angular/core';
import {TableElement, ValidatorService} from '@e-is/ngx-material-table';
import {OperationValidatorService} from '../services/validator/operation.validator';
import {AlertController, ModalController, Platform} from '@ionic/angular';
import {ActivatedRoute, Router} from '@angular/router';
import {Location} from '@angular/common';
import {OperationService, OperationServiceWatchOptions} from '../services/operation.service';
import {TranslateService} from '@ngx-translate/core';
import {
  AccountService,
  AppTable,
  EntitiesTableDataSource,
  LatLongPattern,
  LocalSettingsService,
  NetworkService,
  ReferentialRef,
  RESERVED_END_COLUMNS,
  RESERVED_START_COLUMNS
} from '@sumaris-net/ngx-components';
import {environment} from '@environments/environment';
import {Operation, PhysicalGear, Trip} from '../services/model/trip.model';
import {OperationFilter} from '@app/trip/services/filter/operation.filter';
import {TripService} from '@app/trip/services/trip.service';
import {debounceTime, distinctUntilChanged, filter} from 'rxjs/operators';
import {AbstractControl, FormBuilder, FormGroup} from '@angular/forms';
import moment from 'moment/moment';
import {Metier} from '@app/referential/services/model/taxon.model';
import {METIER_DEFAULT_FILTER} from '@app/referential/services/metier.service';
import {ReferentialRefService} from '@app/referential/services/referential-ref.service';
import {BehaviorSubject} from 'rxjs';

@Component({
  selector: 'app-select-operation-by-trip-table',
  templateUrl: 'select-operation-by-trip.table.html',
  styleUrls: ['select-operation-by-trip.table.scss'],
  providers: [
    {provide: ValidatorService, useExisting: OperationValidatorService}
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SelectOperationByTripTable extends AppTable<Operation, OperationFilter> implements OnInit, OnDestroy {

  private GROUP_BY_COLUMN = 'tripId';

  limitDateForLostOperation = moment().add(-4, 'day');
  trips = new Array<Trip>();
  isGrouping = false;
  _taxonGroupsSubject = new BehaviorSubject<ReferentialRef[]>(undefined);
  filterForm: FormGroup;

  displayAttributes: {
    [key: string]: string[]
  };
  highlightedRow: TableElement<Operation>;

  @Input() latLongPattern: LatLongPattern;
  @Input() tripId: number;
  @Input() program: string;
  @Input() showToolbar = true;
  @Input() showPaginator = false;
  @Input() showFilter = true;
  @Input() useSticky = true;
  @Input() enableGeolocation = false;
  @Input() physicalGears: PhysicalGear[];

  get sortActive(): string {
    const sortActive = super.sortActive;
    // Local sort
    if (this.tripId < 0) {
      switch (sortActive) {
        case 'physicalGear':
          return 'physicalGear.gear.' + this.displayAttributes.gear[0];
        case 'targetSpecies':
          return 'metier.taxonGroup.' + this.displayAttributes.taxonGroup[0];
        case 'tripId':
          return 'trip';
        default:
          return sortActive;
      }
    }
    // Remote sort
    else {
      switch (sortActive) {
        case 'targetSpecies':
          return 'metier';
        case 'tripId':
          return 'trip';
        default:
          return sortActive;
      }
    }
  }

  constructor(
    protected route: ActivatedRoute,
    protected router: Router,
    protected platform: Platform,
    protected location: Location,
    protected modalCtrl: ModalController,
    protected settings: LocalSettingsService,
    protected validatorService: ValidatorService,
    protected dataService: OperationService,
    protected referentialRefService: ReferentialRefService,
    protected tripService: TripService,
    protected alertCtrl: AlertController,
    protected translate: TranslateService,
    protected accountService: AccountService,
    protected network: NetworkService,
    formBuilder: FormBuilder,
    protected cd: ChangeDetectorRef
  ) {
    super(route, router, platform, location, modalCtrl, settings,
      RESERVED_START_COLUMNS
        .concat(
          ['tripId',
            'physicalGear',
            'targetSpecies',
            'startDateTime',
            'startPosition',
            'fishingStartDateTime',
            'endPosition'])
        .concat(RESERVED_END_COLUMNS),
      new EntitiesTableDataSource<Operation, OperationFilter, number, OperationServiceWatchOptions>(Operation,
        dataService,
        null,
        // DataSource options
        {
          prependNewElements: false,
          suppressErrors: environment.production,
          dataServiceOptions: {
            readOnly: true,
            withBatchTree: false,
            withSamples: false,
            withTotal: true
          },
          OperationServiceWatchOptions: {
            computeRankOrder: false
          }
        })
    );
    this.i18nColumnPrefix = 'TRIP.OPERATION.LIST.';

    this.readOnly = true;
    this.inlineEdition = false;
    this.confirmBeforeDelete = true;
    this.saveBeforeSort = false;
    this.saveBeforeFilter = false;
    this.saveBeforeDelete = false;
    this.autoLoad = false; // waiting parent to be loaded

    this.defaultPageSize = -1; // Do not use paginator
    this.defaultSortBy = this.mobile ? 'startDateTime' : 'endDateTime';
    this.defaultSortDirection = this.mobile ? 'desc' : 'asc';
    this.excludesColumns = ['select'];
    settings.ready().then(() => {
      if (this.settings.settings.accountInheritance) {
        const account = this.accountService.account;
        this.latLongPattern = account && account.settings && account.settings.latLongFormat || this.settings.latLongFormat;
      } else {
        this.latLongPattern = this.settings.latLongFormat;
      }
    });

    this.filterForm = formBuilder.group({
      'startDate': null,
      'gearIds': [null],
      'taxonGroupLabels': [null]
    });

    // Update filter when changes
    this.registerSubscription(
      this.filterForm.valueChanges
        .pipe(
          debounceTime(250),
          filter(() => this.filterForm.valid)
        )
        // Applying the filter
        .subscribe((json) => this.setFilter({
            ...this.filter, // Keep previous filter
            ...json
          },
          {emitEvent: this.mobile}))
    );
  }

  ngOnInit() {
    super.ngOnInit();
    if (this.filter && this.filter.startDate) {
      this.filterForm.get('startDate').setValue(this.filter.startDate, {emitEvent: false});
    }
    if (this.filter && this.filter.gearIds.length === 1) {
      this.filterForm.get('gearIds').setValue(this.filter.gearIds[0], {emitEvent: false});
    }

    this.displayAttributes = {
      gear: this.settings.getFieldDisplayAttributes('gear'),
      taxonGroup: this.settings.getFieldDisplayAttributes('taxonGroup'),
    };

    this.registerSubscription(
      this.settings.onChange.subscribe((settings) => {
        if (this.loading) return; // skip
        this.latLongPattern = settings.latLongFormat;
        this.markForCheck();
      }));

    this.loadTaxonGroups();

    this.registerSubscription(
      this._dataSource.datasourceSubject.pipe(
        distinctUntilChanged()
      ).subscribe(async (data: any) => {
        if (this.enableGeolocation && (this.sortActive === 'startPosition' || this.sortActive === 'endPosition')) {
          data = await this.dataService.sortByDistance(data, this.sortDirection, this.sortActive);
        }
        if (!this.isGrouping) {
          this.isGrouping = true;
          const tripsIds = data.map(ope => ope.tripId).filter((v, i, a) => a.indexOf(v) === i);

          if (!this.trips || this.trips.length === 0 || this.trips.length < tripsIds.length) {
            if (this.network.offline) {
              this.trips = data.map(operation => operation.trip);
            } else {
              const ids = tripsIds.filter((v) => this.trips && !this.trips.some(trip => trip.id === v));
              const res =
                await this.tripService.loadAll(0, 999, null, null,
                  {includedIds: ids});
              this.trips = this.trips.concat(res.data);
            }
          }
          const operations = this.addGroups(data);
          this._dataSource.updateDatasource(operations, {emitEvent: false});
          this.isGrouping = false;
        }
      }));
  }

  clickRow(event: MouseEvent | undefined, row: TableElement<Operation>): boolean {
    this.highlightedRow = row;

    return super.clickRow(event, row);
  }

  addGroups(data: any[]): any[] {
    const groups = this.uniqueBy(
      data.map(
        row => {
          const result = new Operation();
          result.id = null;
          result.trip = this.trips.find(trip => trip.id === row.tripId) || new Trip();
          result.trip.id = row.tripId;
          result[this.GROUP_BY_COLUMN] = row[this.GROUP_BY_COLUMN];
          return result;
        }
      ),
      JSON.stringify);

    let subGroups = [];
    groups.forEach(group => {
      const rowsInGroup = data.filter(row => group[this.GROUP_BY_COLUMN] === row[this.GROUP_BY_COLUMN]);
      rowsInGroup.unshift(group);
      subGroups = subGroups.concat(rowsInGroup);
    });
    return subGroups;
  }

  uniqueBy(a, key) {
    const seen = {};
    return a.filter((item) => {
      const k = key(item);
      return seen.hasOwnProperty(k) ? false : (seen[k] = true);
    });
  }

  isTrip(index, item): boolean {
    return item.currentData && !item.currentData.id && item.currentData.trip;
  }

  clearControlValue(event: UIEvent, formControl: AbstractControl): boolean {
    if (event) event.stopPropagation(); // Avoid to enter input the field
    formControl.setValue(null);
    return false;
  }

  isCurrentData(row: any) {
    return this.filter.orIncludedIds && this.filter.orIncludedIds.length > 0 && row.currentData.id === this.filter.orIncludedIds[0];
  }

  /* -- protected methods -- */
  protected markForCheck() {
    this.cd.markForCheck();
  }

  protected async loadTaxonGroups() {
    const res = await this.referentialRefService.loadAll(0, 100, null, null,
      {
        entityName: 'Metier',
        ...METIER_DEFAULT_FILTER,
        searchJoin: 'TaxonGroup',
        levelIds: this.physicalGears.map(physicalGear => physicalGear.gear.id),
      },
      {
        withTotal: false
      });
    const metierTaxonGroups = (res.data || []).reduce((res, metier) => {
      if (res.find(m => m.label === metier.label) === undefined){
        return res.concat(metier);
      }
      else {
        return res;
      }
    }, [] )

    this._taxonGroupsSubject.next(metierTaxonGroups);
  }
}

