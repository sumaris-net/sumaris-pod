import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Injector, OnInit} from "@angular/core";
import {
  AppTable,
  AppTableDataSource,
  environment, isNotNil
} from "../../../core/core.module";
import {Person, PRIORITIZED_USER_PROFILES, referentialToString, DefaultStatusList} from "../../../core/services/model";
import {PersonFilter, PersonService} from "../../services/person.service";
import {PersonValidatorService} from "../../services/person.validator";
import {AlertController, ModalController} from "@ionic/angular";
import {ActivatedRoute, Router} from "@angular/router";
import {AccountService} from "../../../core/services/account.service";
import {Location} from '@angular/common';
import {FormBuilder, FormGroup} from "@angular/forms";
import {RESERVED_END_COLUMNS, RESERVED_START_COLUMNS} from "../../../core/table/table.class";
import {ValidatorService} from "angular4-material-table";
import {FormFieldDefinition} from "../../../shared/form/field.model";
import {PlatformService} from "../../../core/services/platform.service";
import {LocalSettingsService} from "../../../core/services/local-settings.service";

@Component({
  selector: 'app-users-table',
  templateUrl: 'users.html',
  styleUrls: ['./users.scss'],
  providers: [
    {provide: ValidatorService, useExisting: PersonValidatorService}
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class UsersPage extends AppTable<Person, PersonFilter> implements OnInit {

  canEdit = false;
  filterForm: FormGroup;
  profiles: string[] = PRIORITIZED_USER_PROFILES;
  additionalFields: FormFieldDefinition[];
  statusList = DefaultStatusList;
  statusById;
  any;

  constructor(
    protected route: ActivatedRoute,
    protected router: Router,
    protected platform: PlatformService,
    protected location: Location,
    protected modalCtrl: ModalController,
    protected accountService: AccountService,
    protected settings: LocalSettingsService,
    protected validatorService: ValidatorService,
    protected dataService: PersonService,
    protected cd: ChangeDetectorRef,
    formBuilder: FormBuilder,
    injector: Injector
  ) {
    super(route, router, platform, location, modalCtrl, settings,
      RESERVED_START_COLUMNS
        .concat([
          'avatar',
          'lastName',
          'firstName',
          'email',
          'profile',
          'status',
          'pubkey'
        ])
        .concat(accountService.additionalFields.map(field => field.key))
        .concat(RESERVED_END_COLUMNS),
      new AppTableDataSource<Person, PersonFilter>(Person, dataService, validatorService, {
        prependNewElements: false,
        suppressErrors: environment.production,
        serviceOptions: {
          saveOnlyDirtyRows: true
        }
      }),
      null,
      injector
    );

    // Allow inline edition only if admin
    this.inlineEdition = accountService.isAdmin(); // TODO: only if desktop ?
    this.canEdit = accountService.isAdmin();
    this.confirmBeforeDelete = true;

    this.i18nColumnPrefix = 'USER.';
    this.filterForm = formBuilder.group({
      'searchText': [null]
    });

    // Fill statusById
    this.statusById = {};
    this.statusList.forEach((status) => this.statusById[status.id] = status);

    this.additionalFields = this.accountService.additionalFields;

    (this.additionalFields || [])
      .filter(field => isNotNil(field.autocomplete))
      .forEach(field => {
        field.autocomplete = this.registerAutocompleteField(field.key, {
          ...field.autocomplete
        });

      });

    // For DEV only --
    //this.debug = !environment.production;
  }

  ngOnInit() {
    super.ngOnInit();

    // Update filter when changes
    this.registerSubscription(
      this.filterForm.valueChanges.subscribe(() => {
        this.filter = this.filterForm.value;
      }));

    this.registerSubscription(
      this.onRefresh.subscribe(() => {
        this.filterForm.markAsUntouched();
        this.filterForm.markAsPristine();
        this.cd.markForCheck();
      }));

  }

  referentialToString = referentialToString;

  /* -- protected methods -- */

  protected markForCheck() {
    this.cd.markForCheck();
  }

}

