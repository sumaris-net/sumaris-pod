import {EventEmitter, Inject, Injectable, Optional} from "@angular/core";
import {CryptoService} from "./crypto.service";
import {TranslateService} from "@ngx-translate/core";
import {Storage} from '@ionic/storage';
import {environment} from "../../../environments/environment";
import {LocalSettings, Peer} from "./model";
import {IonicSafeString, ModalController, ToastController} from "@ionic/angular";
import {SelectPeerModal} from "../peer/select-peer.modal";
import {BehaviorSubject, Subject, Subscription} from "rxjs";
import {LocalSettingsService, SETTINGS_STORAGE_KEY} from "./local-settings.service";
import {SplashScreen} from "@ionic-native/splash-screen/ngx";
import {HttpClient} from "@angular/common/http";
import {isNotNil, isNotNilOrBlank, toBoolean} from "../../shared/shared.module";
import {Connection, Network} from '@ionic-native/network/ngx';
import {DOCUMENT} from "@angular/common";
import {CacheService} from "ionic-cache";
import {Toasts} from "../../shared/toasts";
import {distinctUntilChanged, filter, map} from "rxjs/operators";
import {ToastOptions} from "@ionic/core";

export interface NodeInfo {
  softwareName: string;
  softwareVersion: string;
  nodeLabel?: string;
  nodeName?: string;
}


export type ConnectionType = 'none' | 'wifi' | 'ethernet' | 'cell' | 'unknown' ;

export function getConnectionType(type: number) {
  switch (type) {
    case Connection.NONE:
      return 'none';
    case Connection.WIFI:
      return 'wifi';
    case Connection.CELL:
    case Connection.CELL_2G:
    case Connection.CELL_3G:
    case Connection.CELL_4G:
      return 'cell';
    case Connection.ETHERNET:
      return 'ethernet';
    case Connection.UNKNOWN:
      return 'unknown';
    default:
      return 'ethernet';
  }
}

@Injectable({providedIn: 'root'})
export class NetworkService {

  private _started = false;
  private _startPromise: Promise<any>;
  private _subscription = new Subscription();
  private _debug = false;
  private _peer: Peer;
  private _deviceConnectionType: ConnectionType;
  private _forceOffline: boolean;

  onStart = new Subject<Peer>();
  onPeerChanges = this.onStart.pipe(
    map(peer => peer && peer.url),
    filter(isNotNilOrBlank),
    distinctUntilChanged<string>()
  );
  onNetworkStatusChanges = new BehaviorSubject<ConnectionType>(null);
  onResetNetworkCache = new EventEmitter(true);

  get online(): boolean {
    return this.connectionType !== 'none';
  }

  get offline(): boolean {
    return this.connectionType === 'none';
  }

  get connectionType(): ConnectionType{
    // If force offline: return 'none'
    return this._forceOffline && 'none'
      // Else, return device connection type (or unknown)
      || (this._started && this._deviceConnectionType || 'unknown');
  }

  get peer(): Peer {
    return this._peer && this._peer.clone();
  }

  set peer(peer: Peer) {
    this.restart(peer);
  }

  get started(): boolean {
    return this._started;
  }

  constructor(
    @Inject(DOCUMENT) private _document: HTMLDocument,
    private modalCtrl: ModalController,
    private cryptoService: CryptoService,
    private storage: Storage,
    private http: HttpClient,
    private splashScreen: SplashScreen,
    private settings: LocalSettingsService,
    private network: Network,
    private cache: CacheService,
    @Optional() private translate: TranslateService,
    @Optional() private toastController: ToastController
  ) {
    this.resetData();

    // Start the service
    this.start();

    // For DEV only
    this._debug = !environment.production;
  }

  async start(peer?: Peer): Promise<any> {
    if (this._startPromise) return this._startPromise;
    if (this._started) return;

    console.info("[network] Starting network...");

    // Restoring local settings
    this._startPromise = (!peer && this.restoreLocally() || Promise.resolve(peer))
      .then(async (peer: Peer | undefined) => {

        // Make sure to hide the splashscreen, before open the modal
        if (!peer) this.splashScreen.hide();

        // No peer in settings: ask user to choose
        while (!peer) {
          console.debug("[network] No peer defined. Asking user to choose a peer.");
          peer = await this.showSelectPeerModal({allowSelectDownPeer: false});
        }
        this._peer = peer;
        this._started = true;
        this._startPromise = undefined;

        this.onStart.next(peer);
        console.info(`[platform] Starting network [OK] {online: ${this.online}}`);
      })
      .catch((err) => {
        console.error(err && err.message || err, err);
        this._started = false;
        this._startPromise = undefined;
      })

      // Wait settings starts, then save peer in settings
      .then(() => this.settings.ready())
      .then(() => this.settings.apply({peerUrl: this._peer.url}))
      .then(() => this.onDeviceConnectionChanged(this.network.type));

    // Listen for network changes
    this._subscription.add(this.network.onDisconnect().subscribe(() => this.onDeviceConnectionChanged('none')));
    this._subscription.add(this.network.onConnect().subscribe(() => this.onDeviceConnectionChanged(this.network.type)));


    return this._startPromise;
  }

  ready(): Promise<void> {
    if (this._started) return Promise.resolve();
    return this.start();
  }

  async stop() {
    this.resetData();
    this._started = false;
    this._startPromise = undefined;

    this._subscription.unsubscribe();
    this._subscription = new Subscription();
  }

  async restart(peer?: Peer) {
    if (this._started) {
      await this.stop()
        .then(() => this.start(peer));
    } else {
      await this.start(peer);
    }
  }

  async tryOnline(): Promise<boolean> {
    // If offline mode not forced, and device says there is no connection: skip
    if (!this._forceOffline || this._deviceConnectionType === 'none') return false;

    console.info("[network] Checking connection to pod...");
    const settings: LocalSettings = await this.settings.ready();

    if (!settings.peerUrl) return false; // No peer define. Skip

    const peer = Peer.parseUrl(settings.peerUrl);
    const alive = await this.checkPeerAlive(peer);
    if (!alive)  return false;

    // Disable the offline mode
    this.setForceOffline(false);

    // Restart
    await this.restart(peer);

    return this._started;
  }

  /**
   * Try to restore peer from the local storage
   */
  async restoreLocally(): Promise<Peer | undefined> {

    // Restore from storage
    const settingsStr = await this.storage.get(SETTINGS_STORAGE_KEY);
    const settings = settingsStr && JSON.parse(settingsStr) || undefined;
    if (settings && settings.peerUrl) {
      console.debug(`[network] Use peer {${settings.peerUrl}} (found in the local storage)`);
      return Peer.parseUrl(settings.peerUrl);
    }

    // Else, use default peer in env, if exists
    if (environment.defaultPeer) {
      return Peer.fromObject(environment.defaultPeer);
    }

    // Else, if App is hosted, try the web site as a peer
    const location = this._document && this._document.location;
    if (location && location.protocol && location.protocol.startsWith("http")) {
      const hostname = this._document.location.host;
      const detectedPeer = Peer.parseUrl(`${this._document.location.protocol}${hostname}${environment.baseUrl}`);
      if (await this.checkPeerAlive(detectedPeer)) {
        return detectedPeer;
      }
    }

    return undefined;
  }

  /**
   * Check if the peer is alive
   * @param email
   */
  async checkPeerAlive(peer: string | Peer): Promise<boolean> {
    try {
      await this.getNodeInfo(peer);
      return true;
    } catch (err) {
      return false;
    }
  }

  getNodeInfo(peer: string | Peer): Promise<NodeInfo> {
    const peerUrl = (peer instanceof Peer) ? peer.url : (peer as string);
    return this.get(peerUrl + '/api/node/info');
  }

  /**
   * Allow to force offline mode
   */
  setForceOffline(value?: boolean, opts?: {
    displayToast?: boolean; // Display a toast ?
  }) {
    value = toBoolean(value, true);
    if (this._forceOffline !== value) {
      const previousConnectionType = this.connectionType;
      this._forceOffline = value;
      const currentConnectionType = this.connectionType;

      if (previousConnectionType !== currentConnectionType) {
        console.info(`[network] Connection changed to {${currentConnectionType}}`);
        this.onNetworkStatusChanges.next(currentConnectionType);

        // Offline mode: alert the user
        if (currentConnectionType === 'none' && (!opts || opts.displayToast !== false)) {
          this.showToast({message: 'NETWORK.INFO.OFFLINE_HELP'});
        }
      }
    }
  }

  async showSelectPeerModal(opts?: {allowSelectDownPeer?: boolean; canCancel?: boolean}): Promise<Peer | undefined> {

    opts = opts || {};

    const $peers = new Subject();

    const modal = await this.modalCtrl.create({
      component: SelectPeerModal,
      componentProps: {
        peers: $peers,
        canCancel: toBoolean(opts.canCancel, true),
        allowSelectDownPeer: toBoolean(opts.allowSelectDownPeer, true)
      },
      keyboardClose: true,
      showBackdrop: true
    });
    await modal.present();

    const peers = await this.getDefaultPeers();
    $peers.next(peers || []);

    return modal.onDidDismiss()
      .then((res) => {
        return res && res.data && (res.data as Peer) || undefined;
      });
  }

  async clearCache(opts?: { emitEvent?: boolean; }): Promise<void> {

    const now = this._debug && Date.now();

    console.info("[network] Clearing all caches...");
    return this.cache.clearAll()
      .then(() => {
        // Emit event
        if (!opts || opts.emitEvent !== false && this.onResetNetworkCache.observers.length) {
          this.onResetNetworkCache.emit();

          // Wait observers clean their caches, if need
          return setTimeout(() => {/*empty*/}, 500);
        }
      })
      .then(() => {
        if (this._debug) console.debug(`[network] All cache cleared, in ${Date.now() - now}ms`);
      });
  }

  /* -- Protected methods -- */

  protected onDeviceConnectionChanged(connectionType?: string) {
    connectionType = (connectionType || 'unknown').toLowerCase();
    if (connectionType.startsWith('cell')) connectionType = 'cell';
    if (connectionType !== this._deviceConnectionType) {
      this._deviceConnectionType = connectionType as ConnectionType;

      // If NOT  already forced as offline, emit event
      if (!this._forceOffline) {
        console.info(`[network] Connection changed to {${this._deviceConnectionType}}`);
        this.onNetworkStatusChanges.next(this._deviceConnectionType);

        if (this._deviceConnectionType === 'none') {
          // Alert the user
          this.showToast({message: 'NETWORK.INFO.OFFLINE'});
        }
      }
    }
  }

  protected resetData() {
    this._peer = null;
  }

  protected async get<T>(uri: string): Promise<T> {
    try {
      return (await this.http.get(uri).toPromise()) as T;
    } catch (err) {
      if (err && err.message) {
        console.error("[network] " + err.message);
      }
      else {
        console.error(`[network] Error on get request ${uri}: ${err.status}`);
      }
      throw {code: err.status, message: "ERROR.UNKNOWN_NETWORK_ERROR"};
    }
  }

  /**
   * Get default peers, from environment
   */
  protected async getDefaultPeers(): Promise<Peer[]> {
    const peers = (environment.defaultPeers || []).map(Peer.fromObject);
    return Promise.resolve(peers);
  }

  protected showToast(opts: ToastOptions): Promise<void> {
    if (!this.toastController || !this.translate) {
      console.error("[network] Cannot show toast - missing toastController or translate");
      if (opts.message instanceof IonicSafeString) console.info("[network] toast message: " + (this.translate && this.translate.instant(opts.message.value) || opts.message.value));
      else if (typeof opts.message === "string") console.info("[network] toast message: " + (this.translate && this.translate.instant(opts.message) || opts.message));
      return Promise.resolve();
    }
    return Toasts.show(this.toastController, this.translate, opts);
  }
}
