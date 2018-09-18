import { Component, OnInit, ViewChild } from "@angular/core";
import { AppTable, AppTableDataSource } from "../../../core/core.module";
import { Person } from "../../../core/services/model";
import { PersonService, PersonFilter } from "../../services/person-service";
import { PersonValidatorService } from "../validator/validators";
import { ModalController, Platform } from "@ionic/angular";
import { Router, ActivatedRoute } from "@angular/router";
import { AccountService } from "../../../core/services/account.service";
import { Location } from '@angular/common';
import { FormGroup, FormBuilder } from "@angular/forms";

@Component({
  selector: 'page-users',
  templateUrl: 'users.html',
  styleUrls: ['./users.scss']
})
export class UsersPage extends AppTable<Person, PersonFilter> implements OnInit {

  filterForm: FormGroup;

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
      ['select',
        'id',
        'avatar',
        'lastName', 'firstName',
        'email'],
      new AppTableDataSource<Person, PersonFilter>(Person, dataService, validatorService)
    );
    this.inlineEdition = true; // Froce inline edition
    this.i18nColumnPrefix = 'USER.';
    this.filterForm = formBuilder.group({
      'searchText': [null]
    });
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
}

