import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Injector, Input, OnInit, ViewChild} from '@angular/core';
import {ValidatorService} from '@e-is/ngx-material-table';
import {VesselValidatorService} from '../services/validator/vessel.validator';
import {VesselService} from '../services/vessel-service';
import {VesselModal, VesselModalOptions} from '../modal/vessel-modal';
import {Vessel} from '../services/model/vessel.model';
import {
  AccountService,
  EntitiesTableDataSource,
  isNil,
  isNotNil,
  LocalSettingsService,
  PlatformService,
  ReferentialRef,
  referentialToString,
  RESERVED_END_COLUMNS,
  RESERVED_START_COLUMNS,
  SharedValidators,
  StatusById,
  StatusIds,
  StatusList
} from '@sumaris-net/ngx-components';
import {ModalController} from '@ionic/angular';
import {ActivatedRoute, Router} from '@angular/router';
import {Location} from '@angular/common';
import {Observable} from 'rxjs';
import {FormBuilder} from '@angular/forms';
import {statusToColor, SynchronizationStatusEnum} from '@app/data/services/model/model.utils';
import {LocationLevelIds} from '@app/referential/services/model/model.enum';
import {ReferentialRefService} from '@app/referential/services/referential-ref.service';
import {environment} from '@environments/environment';
import {AppRootTable} from '@app/data/table/root-table.class';
import {VESSEL_FEATURE_NAME} from '../services/config/vessel.config';
import {VesselFilter} from '../services/filter/vessel.filter';
import {MatExpansionPanel} from '@angular/material/expansion';


export const VesselsTableSettingsEnum = {
  TABLE_ID: "vessels",
  FEATURE_ID: VESSEL_FEATURE_NAME
};


@Component({
  selector: 'app-vessels-table',
  templateUrl: 'vessels.table.html',
  styleUrls: ['./vessels.table.scss'],
  providers: [
    { provide: ValidatorService, useClass: VesselValidatorService }
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class VesselsTable extends AppRootTable<Vessel, VesselFilter> implements OnInit {

  locations: Observable<ReferentialRef[]>;
  vesselTypes: Observable<ReferentialRef[]>;

  readonly statusList = StatusList;
  readonly statusById = StatusById;

  @Input() canEdit: boolean;
  @Input() canDelete: boolean;
  @Input() showFabButton = false;
  @Input() showError = true;
  @Input() showToolbar = true;
  @Input() showPaginator = true;

  @Input()
  set showIdColumn(value: boolean) {
    this.setShowColumn('id', value);
  }

  get showIdColumn(): boolean {
    return this.getShowColumn('id');
  }

  @Input()
  set showVesselTypeColumn(value: boolean) {
    this.setShowColumn('vesselType', value);
  }

  get showVesselTypeColumn(): boolean {
    return this.getShowColumn('vesselType');
  }

  @ViewChild(MatExpansionPanel, {static: true}) filterExpansionPanel: MatExpansionPanel;

  constructor(
    protected route: ActivatedRoute,
    protected router: Router,
    protected platform: PlatformService,
    protected location: Location,
    protected modalCtrl: ModalController,
    protected accountService: AccountService,
    protected settings: LocalSettingsService,
    protected vesselService: VesselService,
    protected referentialRefService: ReferentialRefService,
    protected cd: ChangeDetectorRef,
    formBuilder: FormBuilder,
    injector: Injector
  ) {
    super(route, router, platform, location, modalCtrl, settings,
      // columns
      RESERVED_START_COLUMNS
        .concat([
          'status',
          'vesselFeatures.exteriorMarking',
          'vesselRegistrationPeriod.registrationCode'])
        .concat(platform.mobile ? [] : [
          'vesselFeatures.startDate',
          'vesselFeatures.endDate'
        ])
        .concat([
          'vesselFeatures.name',
          'vesselType',
          'vesselFeatures.basePortLocation'
        ])
        .concat(platform.mobile ? [] : [
          'comments'
        ])
        .concat(RESERVED_END_COLUMNS),
      vesselService,
      new EntitiesTableDataSource<Vessel, VesselFilter>(Vessel, vesselService, null, {
        prependNewElements: false,
        suppressErrors: environment.production,
        dataServiceOptions: {
          saveOnlyDirtyRows: true
        }
      }),
      null,
      injector
    );
    this.i18nColumnPrefix = 'VESSEL.';
    this.defaultSortBy = 'vesselFeatures.exteriorMarking';
    this.defaultSortDirection = 'asc';
    this.filterForm = formBuilder.group({
      program: [null, SharedValidators.entity],
      basePortLocation: [null, SharedValidators.entity],
      registrationLocation: [null, SharedValidators.entity],
      vesselType: [null, SharedValidators.entity],
      date: [null, SharedValidators.validDate],
      searchText: [null],
      statusId: [null],
      synchronizationStatus: [null]
    });
    this.inlineEdition = false;
    this.confirmBeforeDelete = true;
    this.autoLoad = false;
    this.showIdColumn = accountService.isAdmin();
    this.settingsId = VesselsTableSettingsEnum.TABLE_ID; // Fixed value, to be able to reuse it in vessel modal
    this.featureId = VesselsTableSettingsEnum.FEATURE_ID;

    this.debug = !environment.production;
  }

  ngOnInit() {

    super.ngOnInit();

    // Locations
    const locationAttributes = this.settings.getFieldDisplayAttributes('location');

    // Base port locations
    this.registerAutocompleteField('basePortLocation', {
      attributes: locationAttributes,
      service: this.referentialRefService,
      filter: {
        entityName: 'Location',
        levelId: LocationLevelIds.PORT
      },
      mobile: this.mobile
    });

    // Registration locations
    this.registerAutocompleteField('registrationLocation', {
      attributes: locationAttributes,
      service: this.referentialRefService,
      filter: {
        entityName: 'Location',
        levelId: LocationLevelIds.COUNTRY
      },
      mobile: this.mobile
    });

    // Vessel type
    this.registerAutocompleteField('vesselType', {
      attributes: ['name'],
      service: this.referentialRefService,
      filter: {
        entityName: 'VesselType',
        statusIds: [StatusIds.TEMPORARY, StatusIds.ENABLE]
      },
      mobile: this.mobile
    });

    // TODO fill vessel types

    // Restore filter from settings, or load all
    this.restoreFilterOrLoad({emitEvent: this.autoLoad});
  }

  async openNewRowDetail(): Promise<boolean> {
    if (this.loading) return Promise.resolve(false);


    const defaultStatus = this.synchronizationStatus !== 'SYNC' ? StatusIds.TEMPORARY : undefined;
    const modal = await this.modalCtrl.create({
      component: VesselModal,
      componentProps: <VesselModalOptions>{
        defaultStatus,
        synchronizationStatus: this.synchronizationStatus !== 'SYNC' ? SynchronizationStatusEnum.DIRTY : undefined,
        canEditStatus: isNil(defaultStatus)
      },
      backdropDismiss: false,
      cssClass: 'modal-large'
    });

    await modal.present();

    const {data} = await modal.onDidDismiss();

    // if new vessel added, refresh the table
    if (isNotNil(data)) this.onRefresh.emit();

    return true;
  }

  applyFilterAndClosePanel(event?: UIEvent) {
    this.onRefresh.emit(event);
    this.filterExpansionPanel.close();
  }

  resetFilter(event?: UIEvent) {
    super.resetFilter(event);
    this.filterExpansionPanel.close();
  }

  clearFilterStatus(event: UIEvent) {
    if (event) {
      event.preventDefault();
      event.stopPropagation();
    }
    this.filterForm.patchValue({statusId: null});
  }

  referentialToString = referentialToString;
  statusToColor = statusToColor;

  /* -- protected methods -- */

  protected markForCheck() {
    this.cd.markForCheck();
  }
}

