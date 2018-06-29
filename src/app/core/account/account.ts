import { Component, OnDestroy } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Subscription } from 'rxjs';
import { AccountService } from '../services/account.service';
import { Account, StatusIds } from '../services/model';
import { FormGroup, FormBuilder, Validators, AbstractControl } from '@angular/forms';


@Component({
  selector: 'page-account',
  templateUrl: 'account.html'
})
export class AccountPage implements OnDestroy {

  isLogin: boolean;
  subscriptions: Subscription[] = [];
  changesSubscription: Subscription;
  account: Account;
  email: any = {
    confirmed: false,
    notConfirmed: false,
    sending: false,
    error: undefined
  }
  error: String;
  form: FormGroup;
  settingsForm: FormGroup;
  localeMap = {
    'fr': 'Français',
    'en': 'English'
  };
  locales: String[] = [];
  saving: boolean = false;


  constructor(
    public formBuilder: FormBuilder,
    public accountService: AccountService,
    public activatedRoute: ActivatedRoute
  ) {
    this.form = formBuilder.group({
      email: ['', Validators.compose([Validators.required, Validators.email])],
      firstName: ['', Validators.compose([Validators.required, Validators.minLength(2)])],
      lastName: ['', Validators.compose([Validators.required, Validators.minLength(2)])],
      settings: formBuilder.group({
        locale: ['', Validators.required]
      })
    });
    this.settingsForm = this.form.controls.settings as FormGroup;

    this.locales;
    for (let locale in this.localeMap) {
      this.locales.push(locale);
    }

    // Subscriptions
    this.subscriptions.push(this.accountService.onLogin.subscribe(account => this.onLogin(account)));
    this.subscriptions.push(this.accountService.onLogout.subscribe(() => this.onLogout()));

    if (accountService.isLogin()) {
      this.onLogin(this.accountService.account);
    }
  };

  ngOnDestroy() {
    this.subscriptions.forEach(s => s.unsubscribe());
    this.subscriptions = [];
    this.stopListenChanges();
  }

  onLogin(account: Account) {
    //console.debug('[account] Logged account: ', account);
    this.isLogin = true;
    this.account = account;
    this.email.confirmed = account && account.email && (account.statusId != StatusIds.TEMPORARY);
    this.email.notConfirmed = account && account.email && (!account.statusId || account.statusId == StatusIds.TEMPORARY);

    this.setValue(account);
    this.form.controls.email.disable();
    this.form.markAsPristine();

    this.startListenChanges();
  }

  onLogout() {
    this.isLogin = false;
    this.email.confirmed = false;
    this.email.notConfirmed = false;
    this.email.sending = false;
    this.email.error = undefined;
    this.form.reset();
    this.form.controls.email.enable();

    this.stopListenChanges();
  }

  startListenChanges() {
    if (this.changesSubscription) return; // already started
    this.changesSubscription = this.accountService.listenChanges();
  }

  stopListenChanges() {
    if (!this.changesSubscription) return;
    this.changesSubscription.unsubscribe();
    this.changesSubscription = undefined;
  }

  setValue(data: any) {
    let value = this.getValue(this.form, data);
    this.form.setValue(value);
  }

  getValue(form: FormGroup, data: any) {
    let value = {};
    form = form || this.form;
    for (let key in form.controls) {
      if (form.controls[key] instanceof FormGroup) {
        value[key] = this.getValue(form.controls[key] as FormGroup, data[key]);
      }
      else {
        value[key] = data[key] || null;
      }
    }
    return value;
  }

  sendConfirmationEmail(event: MouseEvent) {
    if (!this.account.email || !this.email.notConfirmed) {
      event.preventDefault();
      return false;
    }

    this.email.sending = true;
    console.debug("[account] Sending confirmation email...");
    this.accountService.sendConfirmationEmail(
      this.account.email,
      this.account.settings.locale
    )
      .then((res) => {
        console.debug("[account] Confirmation email sent.");
        this.email.sending = false;
      })
      .catch(err => {
        this.email.sending = false;
        this.email.error = err && err.message || err;
      });
  }

  async doSave(event: MouseEvent, data: any) {
    if (this.form.invalid) return;

    this.saving = true;
    let newAccount = this.account.clone();
    let json = newAccount.asObject();

    let settings = Object.assign({}, data.settings); // Need to be copied first
    Object.assign(json, data);
    json.settings = Object.assign(this.account.settings.asObject(), settings);
    newAccount.fromObject(json);

    console.log("[account] Updating account...", newAccount);
    try {
      await this.accountService.saveRemotely(newAccount)
    }
    catch (err) {
      this.error = err && err.message || err;
    }
    finally {
      this.saving = false;
    }
  }

  cancel(event: any) {
    this.setValue(this.account);
    this.form.markAsPristine();
  }
}
