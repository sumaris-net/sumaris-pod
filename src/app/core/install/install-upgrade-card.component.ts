import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  EventEmitter,
  Input,
  OnDestroy,
  OnInit,
  Output
} from '@angular/core';
import {ModalController} from '@ionic/angular';
import {Subscription} from 'rxjs';
import {Configuration} from '../services/model/config.model';
import {ConfigService} from '../services/config.service';
import {PlatformService} from "../services/platform.service";
import {distinctUntilChanged, map} from "rxjs/operators";
import {environment} from "../../../environments/environment";
import {NetworkService} from "../services/network.service";
import {ConfigOptions} from "../services/config/core.config";
import {VersionUtils} from "../../shared/version/versions";
import {fadeInAnimation, slideUpDownAnimation} from "../../shared/material/material.animations";
import {isNilOrBlank, isNotEmptyArray, isNotNilOrBlank} from "../../shared/functions";


export declare type InstallAppLink = { name: string; url: string; platform?: 'android' | 'ios'; version?: string; };

@Component({
  selector: 'app-install-upgrade-card',
  templateUrl: 'install-upgrade-card.component.html',
  styleUrls: ['./install-upgrade-card.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  animations: [fadeInAnimation, slideUpDownAnimation]
})
export class AppInstallUpgradeCard implements OnInit, OnDestroy {

  private _subscription = new Subscription();
  private _showUpdateOfflineFeature = false;

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

  @Input()
  set showUpdateOfflineFeature(value: boolean) {
    if (value === this._showUpdateOfflineFeature) return; // Skip
    this._showUpdateOfflineFeature = value;
    this.markForCheck();
  }

  get showUpdateOfflineFeature(): boolean {
    return this._showUpdateOfflineFeature;
  }

  @Output()
  onUpdateOfflineModeClick = new EventEmitter<UIEvent>();

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
          console.info("[install] Checking if upgrade  or install is need...");

          const installLinks = this.getAllInstallLinks(config);

          // Check for upgrade
          this.updateLinks = this.getCompatibleUpgradeLinks(installLinks, config);

          // Check for install links (if no upgrade need)
          this.installLinks = !this.updateLinks && this.getCompatibleInstallLinks(installLinks);

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
      console.info(`[install] Opening App download link: ${link.url}`);
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

  private getCompatibleInstallLinks(installLinks: InstallAppLink[]): InstallAppLink[] {

    // Cordova already running: not need to install
    if (this.platform.is('cordova')) return undefined;

    // If mobile web: return all
    if (this.platform.is('mobileweb')) {
      return installLinks;
    }

    return undefined;
  }

  private getCompatibleUpgradeLinks(installLinks: InstallAppLink[], config: Configuration): InstallAppLink[] {
    const appMinVersion = config.getProperty(ConfigOptions.APP_MIN_VERSION);

    const needUpgrade = appMinVersion && !VersionUtils.isCompatible(appMinVersion, environment.version);
    if (!needUpgrade) return undefined;

    const upgradeLinks = installLinks
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
      if (isNilOrBlank(url)) {
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
