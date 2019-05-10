import {Injectable} from "@angular/core";
import {Account, LocalSettings, Peer, UsageMode} from "./model";
import {TranslateService} from "@ngx-translate/core";
import {Storage} from '@ionic/storage';

import {isNotNil} from "../../shared/shared.module";
import {environment} from "../../../environments/environment";
import {Subject} from "rxjs";
import {isNotNilOrBlank} from "../../shared/functions";

export const SETTINGS_STORAGE_KEY = "settings";

@Injectable()
export class LocalSettingsService {

  private _startPromise: Promise<any>;
  private _started = false;
  private data: LocalSettings;

  public onChange = new Subject<LocalSettings>();

  get settings(): LocalSettings {
    return this.data;
  }

  get locale(): string {
    return this.data && this.data.locale || this.translate.currentLang || this.translate.defaultLang;
  }

  get latLongFormat(): string {
    return this.data && this.data.latLongFormat || 'DDMM';
  }

  constructor(
    private translate: TranslateService,
    private storage: Storage
  ) {

    this.resetData();

    this.start();
  }

  private resetData() {
    this.data = this.data || {};

    this.data.locale = this.translate.currentLang || this.translate.defaultLang;
    this.data.latLongFormat = environment.defaultLatLongFormat || 'DDMM';
    this.data.usageMode = 'DESK';
    this.data.accountInheritance = true;

    const defaultPeer = environment.defaultPeer && Peer.fromObject(environment.defaultPeer);
    this.data.peerUrl = defaultPeer && defaultPeer.url || undefined;

    if (this._started) this.onChange.next(this.data);
  }

  async start() {
    if (this._startPromise) return this._startPromise;
    if (this._started) return;

    // Restoring local settings
    this._startPromise = this.restoreLocally()
      .then((settings) => {
        this._started = true;
        this._startPromise = undefined;
        return settings;
      });
    return this._startPromise;
  }

  public isStarted(): boolean {
    return this._started;
  }

  public ready(): Promise<void> {
    if (this._started) return Promise.resolve();
    return this.start();
  }


  public isUsageMode(mode: UsageMode): boolean {
    return (this.data && this.data.usageMode || 'DESK') === mode;
  }

  public async restoreLocally(): Promise<LocalSettings | undefined> {

    // Restore from storage
    const settingsStr = await this.storage.get(SETTINGS_STORAGE_KEY);

    // Restore local settings (or keep old settings)
    if (isNotNilOrBlank(settingsStr)) {
      this.data = JSON.parse(settingsStr);
    }

    // Emit event
    this.onChange.next(this.data);

    return this.data;
  }

  getLocalSetting(key: string, defaultValue?: string): string {
    return this.data && isNotNil(this.data[key]) && this.data[key] || defaultValue;
  }

  async saveLocalSettings(settings: LocalSettings) {
    this.data = this.data || {};

    Object.assign(this.data, settings || {});

    // Save locally
    await this.saveLocally();

    // Emit event
    this.onChange.next(this.data);
  }


  async setLocalSetting(propertyName: string, value: any) {
    const data = {};
    data[propertyName] = value;
    await this.saveLocalSettings(data);
  }

  public getPageSettings(pageId: string, propertyName?: string): string[] {
    const key = pageId.replace(/[/]/g, '__');
    return this.data && this.data.pages
      && this.data.pages[key] && (propertyName && this.data.pages[key][propertyName] || this.data.pages[key]);
  }

  public async savePageSetting(pageId: string, value: any, propertyName?: string) {
    const key = pageId.replace(/[/]/g, '__');

    this.data = this.data || {};
    this.data.pages = this.data.pages || {}
    if (propertyName) {
      this.data.pages[key] = this.data.pages[key] || {};
      this.data.pages[key][propertyName] = value;
    }
    else {
      this.data.pages[key] = value;
    }

    // Update local settings
    await this.saveLocally();
  }

  /* -- Protected methods -- */


  private saveLocally(): Promise<any> {
    if (!this.data) {
      console.debug("[settings] Removing local settings fro storage");
      return this.storage.remove(SETTINGS_STORAGE_KEY);
    }
    else {
      console.debug("[settings] Store local settings", this.data);
      return this.storage.set(SETTINGS_STORAGE_KEY, JSON.stringify(this.data));
    }
  }


}
