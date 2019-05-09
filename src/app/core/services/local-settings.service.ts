import {Injectable} from "@angular/core";
import {LocalSettings, Peer, UsageMode} from "./model";
import {TranslateService} from "@ngx-translate/core";
import {Storage} from '@ionic/storage';

import {isNotNil} from "../../shared/shared.module";
import {environment} from "../../../environments/environment";

export const SETTINGS_STORAGE_KEY = "settings";

@Injectable()
export class LocalSettingsService {

  private _startPromise: Promise<any>;
  private _started = false;
  private _data: LocalSettings;

  get settings(): LocalSettings {
    return this._data;
  }

  get locale(): string {
    return this._data && this._data.locale || this.translate.currentLang || this.translate.defaultLang;
  }

  get latLongFormat(): string {
    return this._data && this._data.latLongFormat || 'DDMM';
  }

  constructor(
    private translate: TranslateService,
    private storage: Storage
  ) {

    this.resetData();

    this.start();
  }

  private resetData() {
    this._data = this._data || {};

    this._data.locale = this.translate.currentLang || this.translate.defaultLang;
    this._data.latLongFormat = environment.defaultLatLongFormat || 'DDMM';
    this._data.usageMode = 'DESK';

    const defaultPeer = environment.defaultPeer && Peer.fromObject(environment.defaultPeer);
    this._data.peerUrl = defaultPeer && defaultPeer.url || undefined;
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
    return (this._data && this._data.usageMode || 'DESK') === mode;
  }

  public async restoreLocally(): Promise<LocalSettings | undefined> {

    // Restore from storage
    const settingsStr = await this.storage.get(SETTINGS_STORAGE_KEY);

    // Restore local settings
    this._data = settingsStr && JSON.parse(settingsStr) || {};

    return this._data;
  }

  getLocalSetting(key: string, defaultValue?: string): string {
    return this._data && isNotNil(this._data[key]) && this._data[key] || defaultValue;
  }

  async saveLocalSettings(settings: LocalSettings) {
    this._data = this._data || {};

    Object.assign(this._data, settings || {});

    // Save locally
    await this.saveLocally();
  }

  public getPageSettings(pageId: string, propertyName?: string): string[] {
    const key = pageId.replace(/[/]/g, '__');
    return this._data && this._data.pages
      && this._data.pages[key] && (propertyName && this._data.pages[key][propertyName] || this._data.pages[key]);
  }

  public async savePageSetting(pageId: string, value: any, propertyName?: string) {
    const key = pageId.replace(/[/]/g, '__');

    this._data = this._data || {};
    this._data.pages = this._data.pages || {}
    if (propertyName) {
      this._data.pages[key] = this._data.pages[key] || {};
      this._data.pages[key][propertyName] = value;
    }
    else {
      this._data.pages[key] = value;
    }

    // Update local settings
    await this.saveLocally();
  }

  /* -- Protected methods -- */


  private saveLocally(): Promise<any> {
    if (!this._data) {
      console.debug("[settings] Removing local settings fro storage");
      return this.storage.remove(SETTINGS_STORAGE_KEY);
    }
    else {
      console.debug("[settings] Store local settings", this._data);
      return this.storage.set(SETTINGS_STORAGE_KEY, JSON.stringify(this._data));
    }
  }


}
