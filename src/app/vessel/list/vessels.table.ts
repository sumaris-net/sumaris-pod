import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Injector, Input, OnInit, ViewChild} from "@angular/core";
import {ValidatorService} from "@e-is/ngx-material-table";
import {VesselValidatorService} from "../services/validator/vessel.validator";
import {VesselService} from "../services/vessel-service";
import {VesselModal, VesselModalOptions} from "../modal/vessel-modal";
import {Vessel} from "../services/model/vessel.model";
import {DefaultStatusList, ReferentialRef, referentialToString} from "../../core/services/model/referential.model";
import {ModalController} from "@ionic/angular";
import {ActivatedRoute, Router} from "@angular/router";
import {AccountService} from "../../core/services/account.service";
import {Location} from '@angular/common';
import {Observable} from 'rxjs';
import {FormBuilder, FormGroup} from "@angular/forms";
import {LocalSettingsService} from "../../core/services/local-settings.service";
import {SharedValidators} from "../../shared/validator/validators";
import {isNil, isNotNil, toBoolean} from "../../shared/functions";
import {statusToColor} from "../../data/services/model/model.utils";
import {LocationLevelIds} from "../../referential/services/model/model.enum";
import {ReferentialRefService} from "../../referential/services/referential-ref.service";
import {RESERVED_END_COLUMNS, RESERVED_START_COLUMNS} from "../../core/table/table.class";
import {EntitiesTableDataSource} from "../../core/table/entities-table-datasource.class";
import {environment} from "../../../environments/environment";
import {PlatformService} from "../../core/services/platform.service";
import {AppRootTable} from "../../data/table/root-table.class";
import {VESSEL_FEATURE_NAME} from "../services/config/vessel.config";
import {StatusIds} from "../../core/services/model/model.enum";
import {SynchronizationStatusEnum} from "../../data/services/model/root-data-entity.model";
import {VesselFilter} from "../services/filter/vessel.filter";
import {MatExpansionPanel} from "@angular/material/expansion";


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

  isAdmin: boolean;
  filterForm: FormGroup;
  filterIsEmpty = true;
  locations: Observable<ReferentialRef[]>;
  vesselTypes: Observable<ReferentialRef[]>;
  statusList = DefaultStatusList;
  statusById: any;

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
          'features.exteriorMarking',
          'registration.registrationCode'])
        .concat(platform.mobile ? [] : [
          'features.startDate',
          'features.endDate'
        ])
        .concat([
          'features.name',
          'vesselType',
          'features.basePortLocation'
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
    this.filterForm = formBuilder.group({
      program: [null, SharedValidators.entity],
      date: [null, SharedValidators.validDate],
      searchText: [null],
      statusId: [null],
      synchronizationStatus: [null]
    });
    this.autoLoad = false;
    this.inlineEdition = false;
    this.confirmBeforeDelete = true;

    // Fill statusById
    this.statusById = {};
    this.statusList.forEach((status) => this.statusById[status.id] = status);

    this.debug = !environment.production;
    this.settingsId = VesselsTableSettingsEnum.TABLE_ID; // Fixed value, to be able to reuse it in vessel modal
    this.featureId = VesselsTableSettingsEnum.FEATURE_ID;
  }

  ngOnInit() {

    super.ngOnInit();

    // Locations
    this.registerAutocompleteField('location', {
      service: this.referentialRefService,
      filter: {
        entityName: 'Location',
        levelId: LocationLevelIds.PORT
      },
      mobile: this.mobile
    });

    // TODO fill vessel types

    // Restore filter from settings, or load all vessels
    this.restoreFilterOrLoad();
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

