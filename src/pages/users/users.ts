import { Component, OnInit, ViewChild } from "@angular/core";
import { AppTableDataSource } from "../../app/material/material.table";
import { Person } from "../../services/model";
import { PersonService, PersonFilter } from "../../services/person-service";
import { PersonValidatorService } from "./validator/validators";
import { MatButtonToggleGroup } from "@angular/material";
import { AppTable } from "../../app/table/table";
import { ModalController, Platform } from "ionic-angular";
import { Router, ActivatedRoute } from "@angular/router";
import { AccountService } from "../../services/account-service";
import { Location } from '@angular/common';
import { FormGroup, FormBuilder } from "@angular/forms";

@Component({
  selector: 'page-users',
  templateUrl: 'users.html'
})
export class UsersPage extends AppTable<Person, PersonFilter> implements OnInit {

  filterForm: FormGroup;
  @ViewChild(MatButtonToggleGroup) clickModeGroup: MatButtonToggleGroup;

  constructor(
    protected route: ActivatedRoute,
    protected router: Router,
    protected platform: Platform,
    protected location: Location,
    protected modalCtrl: ModalController,
    protected accountService: AccountService,
    protected personService: PersonService,
    protected personValidatorService: PersonValidatorService,
    formBuilder: FormBuilder
  ) {
    super(route, router, platform, location, modalCtrl, accountService,
      personValidatorService,
      new AppTableDataSource<Person, PersonFilter>(Person, personService, personValidatorService),
      ['select',
        'id',
        'avatar',
        'lastName', 'firstName',
        'email'],
      {
        email: null,
        pubkey: null,
        searchText: null
      }
    );
    this.inlineEdition = true;
    this.i18nColumnPrefix = 'USER.';
    this.filterForm = formBuilder.group({
      'searchText': [null]
    });
  };

  ngOnInit() {
    super.ngOnInit();

    this.clickModeGroup.value = 'edit';
    this.clickModeGroup.valueChange.subscribe((value) => {
      this.inlineEdition = (value === "edit");
    });

    // Update filter when changes
    this.filterForm.valueChanges.subscribe(() => {
      this.filter = this.filterForm.value;
    });

    this.onRefresh.subscribe(() => {
      this.filterForm.markAsUntouched();
      this.filterForm.markAsPristine();
    });

  }

  addRowModal(): Promise<any> {
    return Promise.resolve();
  }
}

