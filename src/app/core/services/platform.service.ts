import {Injectable} from '@angular/core';
import {Platform} from "@ionic/angular";
import {ConfigService} from './config.service';
import {NetworkService} from "./network.service";
import {Platforms} from "@ionic/core";


export declare interface BasePlatform {
  is(platformName: Platforms): boolean;
  mobile: boolean;
}

@Injectable()
export class PlatformService implements BasePlatform {

  private _started = false;
  private _startPromise: Promise<void>;

  public mobile: boolean;
  public touchUi: boolean;

  get started(): boolean {
    return this._started;
  }

  constructor(
    private platform: Platform,
    private configurationService: ConfigService,
    private networkService: NetworkService
  ) {

    this.start();

    this.touchUi = platform.is('mobile') || platform.is('tablet') || platform.is('phablet');
    this.mobile = platform.is('mobile');
  }

  is(platformName: Platforms): boolean {
    return this.platform.is(platformName);
  }

  protected async start() {
    if (this._startPromise) return this._startPromise;

    this._started = false;

    this._startPromise = Promise.all([
      this.platform.ready(),
      this.networkService.ready()
    ])
      .then(() => {
        this._started = true;
        this._startPromise = undefined;
      });
    return this._startPromise;
  }

  ready(): Promise<void> {
    if (this._started) return Promise.resolve();
    if (this._startPromise) return this._startPromise;
    return this.start();
  }
}

