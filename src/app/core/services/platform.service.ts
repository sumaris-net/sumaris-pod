import {Injectable, Optional} from '@angular/core';
import {Platform} from "@ionic/angular";
import {NetworkService} from "./network.service";
import {Platforms} from "@ionic/core";
import {SplashScreen} from "@ionic-native/splash-screen/ngx";
import {StatusBar} from "@ionic-native/status-bar/ngx";
import {Keyboard} from "@ionic-native/keyboard/ngx";
import {LocalSettingsService} from "./local-settings.service";
import {CacheService} from "ionic-cache";
import {AudioProvider} from "../../shared/audio/audio";

import {InAppBrowser} from "@ionic-native/in-app-browser/ngx";
import {isNotNil} from "../../shared/functions";

@Injectable({providedIn: 'root'})
export class PlatformService {

  private _started = false;
  private _startPromise: Promise<void>;
  private _mobile: boolean;

  touchUi: boolean;

  get started(): boolean {
    return this._started;
  }

  get mobile(): boolean {
    return isNotNil(this._mobile) ? this._mobile : this.platform.is('mobile');
  }

  constructor(
    private platform: Platform,
    private splashScreen: SplashScreen,
    private statusBar: StatusBar,
    private keyboard: Keyboard,
    private settings: LocalSettingsService,
    private networkService: NetworkService,
    private cache: CacheService,
    private audioProvider: AudioProvider,
    @Optional() private browser: InAppBrowser
  ) {

    this.start();
  }

  is(platformName: Platforms): boolean {
    return this.platform.is(platformName);
  }

  /**
   * Say if opened has been opened from a web browser (and NOT inside an Android or iOs App).
   * This is used to known if there is cordova features
   */
  isWebOrDesktop(): boolean {
    return !this.platform.is('mobile') || this.platform.is('mobileweb');
  }

  width(): number {
    return this.platform.width();
  }

  height(): number {
    return this.platform.height();
  }

  protected async start() {
    if (this._startPromise) return this._startPromise;
    if (this._started) return;

    this._started = false;
    console.info("[platform] Starting platform...");

    this._startPromise = Promise.all([
      this.platform.ready()
        .then(() => {

          this.configureCordovaPlugins();

          this._mobile = this.platform.is('mobile');
          this.touchUi = this._mobile || this.platform.is('tablet') || this.platform.is('phablet');

          // Force mobile in settings
          if (this._mobile) {
            this.settings.mobile = this._mobile;
            this.settings.touchUi = this.touchUi;
          }
        }),
      this.cache.ready().then(() => this.configureCache()),
      this.settings.ready(),
      this.networkService.ready(),
      this.audioProvider.ready()
    ])
      .then(() => {
        this._started = true;
        this._startPromise = undefined;
        console.info(`[platform] Starting platform [OK] {mobile: ${this._mobile}}, {touchUi: ${this.touchUi}}`);

        // Update cache configuration when network changed
        this.networkService.onNetworkStatusChanges.subscribe((type) => this.configureCache(type !== 'none'));

        // Wait 1 more seconds, before hiding the splash screen
        setTimeout(() => {
          this.splashScreen.hide();

          // Play startup sound
          this.audioProvider.playStartupSound();
        }, 1000);
      });
    return this._startPromise;
  }

  ready(): Promise<void> {
    if (this._started) return Promise.resolve();
    if (this._startPromise) return this._startPromise;
    return this.start();
  }

  open(url: string, target?: string, features?: string, replace?: boolean) {
    if (this.browser && this._mobile) {
      this.browser.create(url, target, features).show();
    }
    else {
      window.open(url, target, features, replace);
    }
  }

  /* -- protected methods -- */

  protected configureCordovaPlugins() {
    console.info("[platform] Setting Cordova plugins...");
    this.statusBar.styleDefault();
    this.statusBar.overlaysWebView(false);
    this.keyboard.hideFormAccessoryBar(true);

    // Force to use InAppBrowser instead of default window.open()
    if (this.browser) {
      // FIXME: this create a infinite loop (e.g. when downloading an extraction file)
      // window.open = (url?: string, target?: string, features?: string, replace?: boolean) => {
      //   console.debug("[platform] Call to window.open() redirected to InAppBrowser.open()");
      //   this.browser.create(url, target, features).show();
      //   return window;
      // };
    }
  }

  protected configureCache(online?: boolean) {
    online = isNotNil(online) ? online : this.cache.isOnline();
    const cacheTTL = online ? 3600 /* 1h */ : 3600 * 24 * 30; /* 1 month */
    console.info(`[platform] Configuring cache [OK] {online: ${online}}, {timeToLive: ${cacheTTL / 3600}h}, {offlineInvalidate: false)`);
    this.cache.setDefaultTTL(cacheTTL);
    this.cache.setOfflineInvalidate(false); // Do not invalidate cache when offline
  }
}

