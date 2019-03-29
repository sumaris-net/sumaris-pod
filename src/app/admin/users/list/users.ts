import { Component, OnInit, ViewChild } from "@angular/core";
import { AppTable, AppTableDataSource, AppFormUtils } from "../../../core/core.module";
import { Person, referentialToString, PRIORITIZED_USER_PROFILES, StatusIds } from "../../../core/services/model";
import { PersonService, PersonFilter } from "../../services/person.service";
import { PersonValidatorService } from "../../services/person.validator";
import { ModalController, Platform } from "@ionic/angular";
import { Router, ActivatedRoute } from "@angular/router";
import { AccountService, AccountFieldDef } from "../../../core/services/account.service";
import { Location } from '@angular/common';
import { FormGroup, FormBuilder } from "@angular/forms";
import { RESERVED_START_COLUMNS, RESERVED_END_COLUMNS } from "../../../core/table/table.class";

@Component({
  selector: 'page-configuration',
  templateUrl: 'users.html',
  styleUrls: ['./users.scss']
})
export class UsersPage extends AppTable<Person, PersonFilter> implements OnInit {

  filterForm: FormGroup;
  profiles: string[] = PRIORITIZED_USER_PROFILES;
  additionalFields: AccountFieldDef[];
  statusList: any[] = [
    {
      id: StatusIds.ENABLE,
      icon: 'checkmark',
      label: 'REFERENTIAL.STATUS_ENABLE'
    },
    {
      id: StatusIds.DISABLE,
      icon: 'close',
      label: 'REFERENTIAL.STATUS_DISABLE'
    },
    {
      id: StatusIds.TEMPORARY,
      icon: 'warning',
      label: 'REFERENTIAL.STATUS_TEMPORARY'
    }
  ];
  statusById; any;

  constructor(
    protected route: ActivatedRoute,
    protected router: Router,
    protected platform: Platform,
    protected location: Location,
    protected modalCtrl: ModalController,
    protected accountService: AccountService,
    protected validatorService: PersonValidatorService,
    protected dataService: PersonService,
    formBuilder: FormBuilder
  ) {
    super(route, router, platform, location, modalCtrl, accountService,
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
        .concat(accountService.additionalAccountFields.map(field => field.name))
        .concat(RESERVED_END_COLUMNS),
      new AppTableDataSource<Person, PersonFilter>(Person, dataService, validatorService, {
        prependNewElements: false,
        suppressErrors: false,
        serviceOptions: {
          saveOnlyDirtyRows: true
        }
      })
    );

    // Allow inline edition only if admin
    this.inlineEdition = accountService.isAdmin();

    this.i18nColumnPrefix = 'USER.';
    this.filterForm = formBuilder.group({
      'searchText': [null]
    });

    // Fill statusById
    this.statusById = {};
    this.statusList.forEach((status) => this.statusById[status.id] = status);

    this.additionalFields = this.accountService.additionalAccountFields;
  };

  ngOnInit() {
    super.ngOnInit();

    // Update filter when changes
    this.filterForm.valueChanges.subscribe(() => {
      this.filter = this.filterForm.value;
    });

    this.onRefresh.subscribe(() => {
      this.filterForm.markAsUntouched();
      this.filterForm.markAsPristine();
    });
  }

  referentialToString = referentialToString;
}

