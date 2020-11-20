import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnDestroy, OnInit} from '@angular/core';
import {ModalController} from '@ionic/angular';
import {Subscription, timer} from 'rxjs';
import {Configuration} from '../services/model/config.model';
import {ConfigService} from '../services/config.service';
import {fadeInAnimation, isNotEmptyArray, isNotNilOrBlank, slideUpDownAnimation} from "../../shared/shared.module";
import {PlatformService} from "../services/platform.service";
import {distinctUntilChanged, filter, map, mergeMap, tap} from "rxjs/operators";
import {environment} from "../../../environments/environment";
import {NetworkService} from "../services/network.service";
import {ConfigOptions} from "../services/config/core.config";
import {VersionUtils} from "../../shared/version/versions";


export declare type InstallAppLink = { name: string; url: string; platform?: 'android' | 'ios'; version?: string; };

@Component({
  selector: 'app-network-status-card',
  templateUrl: 'network-status-card.component.html',
  styleUrls: ['./network-status-card.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  animations: [fadeInAnimation, slideUpDownAnimation]
})
export class NetworkStatusCard implements OnInit, OnDestroy {

  private _subscription = new Subscription();

  loading = true;
  waitingNetwork = false;
  allLinks: InstallAppLink[];
  installLinks: InstallAppLink[];
  updateLinks: InstallAppLink[];
  offline: boolean;
  upgradeVersion: string;


  @Input()
  isLogin: boolean;


  @Input()
  showUpgradeWarning: boolean = true;

  @Input()
  showOfflineWarning: boolean = true;

  @Input()
  showInstallButton: boolean = false;

  constructor(
    private modalCtrl: ModalController,
    private configService: ConfigService,
    private platform: PlatformService,
    private cd: ChangeDetectorRef,
    public network: NetworkService
  ) {

  }

  async ngOnInit() {

    await this.platform.ready();

    this.offline = this.network.offline;

    // Listen pod config
    this._subscription.add(
      this.configService.config
        .subscribe(config => {
          console.info("[network-status] Checking if upgrade  or install is need...");

          // Check for upgrade
          this.updateLinks = this.getCompatibleUpgradeLinks(config);

          // Check for install links (if no upgrade need)
          this.installLinks = !this.updateLinks && this.getCompatibleInstallLinks(config);

          setTimeout(() => {
            this.loading = false;
            this.markForCheck();
          }, 2000) // Add a delay, for animation
        }));

    // Listen network changes
    this._subscription.add(
      this.network.onNetworkStatusChanges
        .pipe(
          //debounceTime(450),
          //tap(() => this.waitingNetwork = false),
          map(connectionType => connectionType === 'none'),
          distinctUntilChanged()
        )
        .subscribe(offline => {
          this.offline = offline;
          this.markForCheck();
        })
    );
  }

  ngOnDestroy() {
    this._subscription.unsubscribe();
  }

  downloadApp(event: UIEvent, link: InstallAppLink) {
    event.preventDefault();

    if (link && link.url) {
      console.info(`[network-status] Opening App download link: ${link.url}`);
      this.platform.open(link.url, '_system', 'location=yes');
      return false;
    }
  }

  tryOnline() {
    this.waitingNetwork = true;
    this.markForCheck();

    this.network.tryOnline({
      showLoadingToast: false,
      showOnlineToast: true,
      showOfflineToast: false
    })
      .then(() => {
        this.waitingNetwork = false;
        this.markForCheck();
      });

  }

  getPlatformName(platform: 'android'|'ios') {
    switch (platform) {
      case 'android':
        return 'Android'
      case 'ios':
        return 'iOS'
      default:
        return ''
    }
  }

  /* -- protected method  -- */

  private getCompatibleInstallLinks(config: Configuration): InstallAppLink[] {
    const links = this.getAllInstallLinks(config)
      .filter(link => this.platform.is('mobileweb') || (!link.platform ||  this.platform.is(link.platform)));

    return isNotEmptyArray(links) ? links : undefined;
  }

  private getCompatibleUpgradeLinks(config: Configuration): InstallAppLink[] {
    const appMinVersion = config.getProperty(ConfigOptions.APP_MIN_VERSION);

    const needUpgrade = appMinVersion && !VersionUtils.isCompatible(appMinVersion, environment.version);
    if (!needUpgrade) return undefined;

    const upgradeLinks = this.getAllInstallLinks(config)
      .filter(link => this.platform.is('mobileweb') || (link.platform &&  this.platform.is(link.platform)));

    // Use min version as default version
    upgradeLinks.forEach(link => {
      link.version = link.version || appMinVersion;
    });

    return isNotEmptyArray(upgradeLinks) ? upgradeLinks : undefined;
  }

  private getAllInstallLinks(config: Configuration): InstallAppLink[] {
    const result = [];

    // Android
    {
      let url = config.getProperty(ConfigOptions.ANDROID_INSTALL_URL);
      const name: string = isNotNilOrBlank(url) && config.label || environment.defaultAppName || 'SUMARiS';
      let version;
      if (isNotNilOrBlank(url)) {
        url = environment.defaultAndroidInstallUrl || null;
      }
      result.push({ name, url, platform: 'android', version });
    }

    // iOS - TODO
    //{
    //  (...)
    //}

    return result;
  }



  protected markForCheck() {
    this.cd.markForCheck();
  }
}
