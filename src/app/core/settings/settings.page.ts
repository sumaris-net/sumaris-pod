import {ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy, OnInit} from '@angular/core';
import {AccountService} from '../services/account.service';
import {Account, LocalSettings, Peer, referentialToString, UsageMode} from '../services/model';
import {FormBuilder, FormControl} from '@angular/forms';
import {AppForm} from '../form/form.class';
import {Moment} from 'moment/moment';
import {DateAdapter} from "@angular/material";
import {Platform} from '@ionic/angular';
import {AppFormUtils} from '../form/form.utils';
import {TranslateService} from "@ngx-translate/core";
import {ValidatorService} from "angular4-material-table";
import {LocalSettingsValidatorService} from "../services/local-settings.validator";
import {PlatformService} from "../services/platform.service";
import {NetworkService} from "../services/network.service";
import {isNilOrBlank} from "../../shared/functions";

@Component({
  selector: 'page-settings',
  templateUrl: 'settings.page.html',
  providers: [
    {provide: ValidatorService, useClass: LocalSettingsValidatorService},
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SettingsPage extends AppForm<LocalSettings> implements OnInit, OnDestroy {

  loading = true;
  saving = false;
  isLogin = false;
  data: LocalSettings;
  usageModes: UsageMode[] = ['FIELD', 'DESK'];
  localeMap = {
    'fr': 'Français',
    'en': 'English'
  };
  locales: String[] = [];
  latLongFormats = ['DDMMSS', 'DDMM', 'DD'];

  constructor(
    protected dateAdapter: DateAdapter<Moment>,
    protected platform: PlatformService,
    protected validatorService: LocalSettingsValidatorService,
    protected translate: TranslateService,
    protected networkService: NetworkService,
    protected formBuilder: FormBuilder,
    protected accountService: AccountService,
    protected cd: ChangeDetectorRef
  ) {
    super(dateAdapter, validatorService.getFormGroup());

    // Fill locales
    for (let locale in this.localeMap) {
      this.locales.push(locale);
    }

    // By default, disable the form
    this.disable();
  }

  async ngOnInit() {
    super.ngOnInit();

    // Make sure platform is ready
    this.platform.ready()
      // Then load data
      .then(() => {
        return this.load();
      });
  }

  async load() {

    this.loading = true;
    console.debug("[settings] Loading settings...");

    const data = this.accountService.localSettings || {};

    // If user login, will use account settings
    if (this.accountService.isLogin()) {
      this.copyFromAccount(data, this.accountService.account);
    }

    // Set defaults
    data.locale = data.locale || this.translate.currentLang || this.translate.defaultLang;
    data.latLongFormat = data.latLongFormat || 'DDMMSS';
    data.usageMode = data.usageMode || 'DESK';

    // Set peer
    if (isNilOrBlank(data.peerUrl)) {
      await this.networkService.ready();
      const peer = this.networkService.peer;
      data.peerUrl = peer && peer.url;
    }

    this.setValue(data);
    this.markAsPristine();
    this.enable();

    this.loading = false;
    this.error = null;
    this.markForCheck();

    this.accountService.onLogin.subscribe(account => this.onLogin(account));

  }

  async save(event: MouseEvent) {
    if (this.form.invalid) {
      AppFormUtils.logFormErrors(this.form);
      return;
    }

    console.debug("[settings] Saving local settings...");

    this.saving = true;
    this.error = undefined;
    const data = this.form.value;

    // Check peer alive, before saving
    const peerChanged = this.form.get('peerUrl').dirty;

    try {
      this.disable();

      await this.accountService.saveLocalSettings(data);
      this.markAsPristine();

      // Update the network peer
      if (peerChanged) {
        this.networkService.peer = Peer.parseUrl(data.peerUrl);
      }

    } catch (err) {
      console.error(err);
      this.error = err && err.message || err;
    } finally {
      this.saving = false;
      this.enable();
    }
  }

  onLogin(account: Account) {
    this.isLogin = true;

    // Force using account settings
    const data = this.form.value;
    this.copyFromAccount(data, account);

    this.setValue(data);
    this.markAsPristine();
    this.enable();
    this.markForCheck();
  }

  copyFromAccount(data: LocalSettings, account: Account) {
    data = data || {};

    // Force using account settings
    data.locale = (account.settings && account.settings.locale) || this.data.locale;
    data.latLongFormat = (account.settings && account.settings.latLongFormat) || this.data.latLongFormat;
  }

  async showSelectPeerModal() {
    const peer = await this.networkService.showSelectPeerModal();
    if (peer && peer.url) {
      const control = this.form.get('peerUrl') as FormControl;
      control.setValue(peer.url, {emitEvent: true, onlySelf: false});
      control.markAsDirty({onlySelf: false});
      this.markAsDirty();
    }

  }

  async cancel() {
    await this.load();
  }

  referentialToString = referentialToString;

  /* -- protected functions -- */

  protected markForCheck() {
    this.cd.markForCheck();
  }
}
