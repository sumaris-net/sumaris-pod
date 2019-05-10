import {ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy, OnInit} from '@angular/core';
import {AccountService} from '../services/account.service';
import {Account, LocalSettings, Peer, referentialToString, UsageMode} from '../services/model';
import {FormBuilder, FormControl} from '@angular/forms';
import {AppForm} from '../form/form.class';
import {Moment} from 'moment/moment';
import {DateAdapter, MatCheckboxChange} from "@angular/material";
import {AppFormUtils} from '../form/form.utils';
import {TranslateService} from "@ngx-translate/core";
import {ValidatorService} from "angular4-material-table";
import {LocalSettingsValidatorService} from "../services/local-settings.validator";
import {PlatformService} from "../services/platform.service";
import {NetworkService} from "../services/network.service";
import {isNilOrBlank, isNotNil, toBoolean} from "../../shared/functions";
import {LocalSettingsService} from "../services/local-settings.service";
import {distinctUntilChanged, startWith} from "rxjs/operators";
import {merge} from "rxjs";

@Component({
  selector: 'page-settings',
  templateUrl: 'settings.page.html',
  providers: [
    {provide: ValidatorService, useClass: LocalSettingsValidatorService},
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SettingsPage extends AppForm<LocalSettings> implements OnInit, OnDestroy {

  private _data: LocalSettings;

  loading = true;
  saving = false;
  usageModes: UsageMode[] = ['FIELD', 'DESK'];
  localeMap = {
    'fr': 'Français',
    'en': 'English'
  };
  locales: String[] = [];
  latLongFormats = ['DDMMSS', 'DDMM', 'DD'];

  get accountInheritance(): boolean {
    return this.form.controls['accountInheritance'].value;
  }

  get isLogin(): boolean {
    return this.accountService.isLogin();
  }

  constructor(
    protected dateAdapter: DateAdapter<Moment>,
    protected platform: PlatformService,
    protected validatorService: LocalSettingsValidatorService,
    protected translate: TranslateService,
    protected networkService: NetworkService,
    protected formBuilder: FormBuilder,
    protected accountService: AccountService,
    protected settingsService: LocalSettingsService,
    protected cd: ChangeDetectorRef
  ) {
    super(dateAdapter, validatorService.getFormGroup());

    // Fill locales
    for (let locale in this.localeMap) {
      this.locales.push(locale);
    }

    // By default, disable the form
    this._enable = false;
  }

  async ngOnInit() {
    super.ngOnInit();

    // Make sure platform is ready
    await this.platform.ready();

    await this.load();

    this.accountService.onLogin.subscribe(() => this.setAccountInheritance(this.accountInheritance));
  }

  async load() {

    this.loading = true;
    console.debug("[settings] Loading settings...");

    const data = this.settingsService.settings || {};

    // Set defaults
    data.accountInheritance = toBoolean(data.accountInheritance, true);
    data.locale = data.locale || this.translate.currentLang || this.translate.defaultLang;
    data.latLongFormat = data.latLongFormat || 'DDMMSS';
    data.usageMode = data.usageMode || 'DESK';

    // Set peer
    if (isNilOrBlank(data.peerUrl)) {
      await this.networkService.ready();
      const peer = this.networkService.peer;
      data.peerUrl = peer && peer.url;
    }

    // Remember data
    this._data = data;

    this.form.patchValue(data, {emitEvent: false});
    this.markAsPristine();

    this.enable({emitEvent: false});

    // Apply inheritance
    this.setAccountInheritance(data.accountInheritance, {emitEvent: false});

    this.loading = false;
    this.error = null;
    this.markForCheck();


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

      await this.settingsService.saveLocalSettings(data);
      this._data = data;
      this.markAsPristine();

      // Update the network peer
      if (peerChanged) {
        this.networkService.peer = Peer.parseUrl(data.peerUrl);
      }

    } catch (err) {
      console.error(err);
      this.error = err && err.message || err;
    } finally {
      this.enable({emitEvent: false});

      // Apply inheritance
      this.setAccountInheritance(data.accountInheritance, {emitEvent: false});

      this.saving = false;
      this.markForCheck();
    }
  }

  public setAccountInheritance(enable: boolean, opts?: {emitEvent?: boolean;}) {
    // Make sure to update the value in control
    this.form.controls['accountInheritance'].setValue(enable, opts);
    if (this._data.accountInheritance !== enable) {
      this.form.controls['accountInheritance'].markAsDirty({onlySelf: false});
    }

    if (enable) {
      if (this.isLogin) {
        // Force using account settings
        const account = this.accountService.account;

        // Copy values
        if (account.settings) {
          if (account.settings.locale) this.form.get('locale').setValue(account.settings.locale, opts);
          if (account.settings.latLongFormat) this.form.get('latLongFormat').setValue(account.settings.latLongFormat, opts);
        }
        // Disable fields
        this.form.get('locale').disable(opts);
        this.form.get('latLongFormat').disable(opts);
      } else {
        // Enable fields
        this.form.get('locale').enable(opts);
        this.form.get('latLongFormat').enable(opts);
      }
    } else {
      // Restore previous values
      this.form.get('latLongFormat').setValue(this._data.locale, opts);
      this.form.get('latLongFormat').setValue(this._data.latLongFormat, opts);

      // Enable fields
      this.form.get('locale').enable(opts);
      this.form.get('latLongFormat').enable(opts);
    }

    // Mark for check, if need
    if (!opts || opts.emitEvent) {
      this.markForCheck();
    }
  }

  showAccount() {
    console.log("TODO: open account");
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
