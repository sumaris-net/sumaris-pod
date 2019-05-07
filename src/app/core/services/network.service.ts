import {Injectable} from "@angular/core";
import {CryptoService} from "./crypto.service";
import {TranslateService} from "@ngx-translate/core";
import {Storage} from '@ionic/storage';
import {environment} from "../../../environments/environment";
import {Peer} from "./model";
import {ModalController} from "@ionic/angular";
import {SelectPeerModal} from "../peer/select-peer.modal";
import {Subject} from "rxjs";
import {SETTINGS_STORAGE_KEY} from "../constants";

@Injectable()
export class NetworkService {

  private _debug = false;
  private _peer: Peer;
  private _startPromise: Promise<any>;
  private _started = false;

  public onStart = new Subject<Peer>();

  get peer(): Peer {
    return this._peer && this._peer.clone();
  }

  set peer(peer: Peer) {
    if (this._started) {
      this.stop()
        .then(() => this.start(peer));
    }
    else {
      this.start(peer);
    }
  }

  get started(): boolean {
    return this._started;
  }

  constructor(
    private translate: TranslateService,
    private modalCtrl: ModalController,
    private cryptoService: CryptoService,
    private storage: Storage
  ) {
    this.resetData();

    // Start the service
    this.start();

    // For DEV only
    this._debug = true;
  }

  public async start(peer?: Peer): Promise<any> {
    if (this._startPromise) return this._startPromise;
    if (this._started) return;

    // Restoring local settings
    this._startPromise = (!peer && this.restoreLocally() || Promise.resolve(peer))
      .then(async (peer: Peer|undefined) => {

        // No peer in settings: ask user to choose
        while (!peer) {
          console.debug("[network] No peer defined. Asking user to choose a peer.");
          peer = await this.showSelectPeerModal();
        }
        this._peer = peer;
        this._started = true;
        this._startPromise = undefined;

        this.onStart.next(peer);
      });
    return this._startPromise;
  }

  ready(): Promise<any> {
    if (this._started) return Promise.resolve();
    return this.start();
  }

  async stop() {
    this.resetData();
    this._started = false;
    this._startPromise = undefined;
  }

  /**
   * Try to restore peer from the local storage
   */
  async restoreLocally(): Promise<Peer | undefined> {

    // Restore from storage
    const settingsStr = await this.storage.get(SETTINGS_STORAGE_KEY);
    const settings = settingsStr && JSON.parse(settingsStr) || undefined;
    if (settings && settings.peerUrl) {
      console.debug(`[network] Will peer {${settings.peerUrl}} (found in the local storage)`);
      return Peer.parseUrl(settings.peerUrl);
    }
  }

  /**
   * Check if the peer is alive
   * @param email
   */
  async checkPeerAlive(serverUrl: string): Promise<boolean> {
    // TODO check on a ping URL ?
    return true;
  }

  /* -- Protected methods -- */

  protected resetData() {
    this._peer = null;
  }



  public async showSelectPeerModal(): Promise<Peer | undefined> {

    const $peers = new Subject();

    const modal = await this.modalCtrl.create({
      component: SelectPeerModal,
      componentProps: {
        peers: $peers,
        canCancel: false
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

  /**
   * Get default peers, from environment
   */
  protected async getDefaultPeers(): Promise<Peer[]> {
    const peers = (environment.defaultPeers || []).map(item => {
      return Peer.fromObject({
        dns: item.host,
        port: item.port
      });
    });
    return Promise.resolve(peers);
  }
}
