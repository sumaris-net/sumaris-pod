import {Injectable} from '@angular/core';
import {Platform} from "@ionic/angular";
import {ConfigService} from './config.service';
import {NetworkService} from "./network.service";
import {Platforms} from "@ionic/core";
import {SplashScreen} from "@ionic-native/splash-screen/ngx";
import {StatusBar} from "@ionic-native/status-bar/ngx";
import {Keyboard} from "@ionic-native/keyboard/ngx";


@Injectable()
export class PlatformService {

  private _started = false;
  private _startPromise: Promise<void>;

  public mobile: boolean;
  public touchUi: boolean;

  get started(): boolean {
    return this._started;
  }

  constructor(
    private platform: Platform,
    private splashScreen: SplashScreen,
    private statusBar: StatusBar,
    private keyboard: Keyboard,
    private configurationService: ConfigService,
    private networkService: NetworkService
  ) {

    this.start();
  }

  is(platformName: Platforms): boolean {
    return this.platform.is(platformName);
  }

  protected async start() {
    if (this._startPromise) return this._startPromise;
    if (this._started) return;

    this._started = false;

    this._startPromise = Promise.all([
      this.platform.ready()
        .then(() => {

          this.configureCordovaPlugins();

          this.touchUi = this.platform.is('mobile') || this.platform.is('tablet') || this.platform.is('phablet');
          this.mobile = this.platform.is('mobile');
        }),
      this.networkService.ready()
    ])
      .then(() => {
        this._started = true;
        this._startPromise = undefined;

        // Wait 1 more seconds, before hiding the splash screen
        setTimeout(() => {
          this.splashScreen.hide();
        }, 1000);
      });
    return this._startPromise;
  }

  ready(): Promise<void> {
    if (this._started) return Promise.resolve();
    if (this._startPromise) return this._startPromise;
    return this.start();
  }

  protected configureCordovaPlugins() {
    console.info("[platform] Setting Cordova plugins...");
    this.statusBar.styleDefault();
    this.statusBar.overlaysWebView(false);
    this.keyboard.hideFormAccessoryBar(true);
  }
}

