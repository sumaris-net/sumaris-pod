import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnDestroy, Optional} from '@angular/core';
import {ModalController, Platform} from '@ionic/angular';
import {Peer} from "../services/model/peer.model";
import {Observable, Subject, Subscription} from "rxjs";
import {fadeInAnimation} from "../../shared/material/material.animations";
import {HttpClient} from "@angular/common/http";
import {NetworkUtils, NodeInfo} from "../services/network.utils";
import {HTTP} from "@ionic-native/http/ngx";

@Component({
  selector: 'select-peer-modal',
  templateUrl: 'select-peer.modal.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  animations: [fadeInAnimation]
})
export class SelectPeerModal implements OnDestroy {

  private _subscription = new Subscription();
  loading = true;
  $peers = new Subject<Peer[]>();

  private readonly httpClient: HTTP | HttpClient;

  @Input() canCancel = true;
  @Input() allowSelectDownPeer = true;

  set peers(peers: Observable<Peer[]>) {
    this._subscription.add(
      peers.subscribe(res => this.refreshPeers(res))
    );
  }


  constructor(

    private viewCtrl: ModalController,
    private cd: ChangeDetectorRef,
    platform: Platform,
    http: HttpClient,
    @Optional() nativeHttp: HTTP
  ) {

    this.httpClient = platform.is('mobile') ? nativeHttp : http;
  }

  cancel() {
    this.viewCtrl.dismiss();
  }

  ngOnDestroy(): void {
    this._subscription.unsubscribe();
  }

  selectPeer(item: Peer) {
    if (this.allowSelectDownPeer || item.reachable) {
      console.debug(`[select-peer-modal] Selected peer: {url: '${item.url}'}`);
      this.viewCtrl.dismiss(item);
    }
  }

  async refreshPeers(peers: Peer[]) {
    peers = peers || [];

    const data: Peer[] = [];
    const jobs = Promise.all(
      peers.map(async (peer) => {
        await this.refreshPeer(peer);

        if (this._subscription.closed) return; // component destroyed

        data.push(peer);

        // Sort (by reachable, then host)
        data.sort((a, b) => {
          if (a.reachable && !b.reachable) return -1;
          if (!a.reachable && b.reachable) return 1;
          if (a.hostAndPort < b.hostAndPort) return -1;
          if (a.hostAndPort > b.hostAndPort) return 1;
          return 0;
        });

        this.$peers.next(data);
        return peer;
      }));

    this._subscription.add(
      this.$peers
        .subscribe(() => {
          this.cd.markForCheck();
        }));

    try {
      await jobs;
    }
    catch(err) {
      if (!this._subscription.closed) console.error(err);
    }
    this.loading = false;
    this.cd.markForCheck();
  }

  protected async refreshPeer(peer: Peer): Promise<Peer> {
    try {
      const summary: NodeInfo = await NetworkUtils.getNodeInfo(this.httpClient, peer.url);
      peer.status = 'UP';
      peer.softwareName = summary.softwareName;
      peer.softwareVersion = summary.softwareVersion;
      peer.label = summary.nodeLabel;
      peer.name = summary.nodeName;
    } catch (err) {
      if (!this._subscription.closed)  {
        if (err && err.message) {
          console.error("[select-peer] " + err.message, err);
        }
      }
      peer.status = 'DOWN';
    }
    return peer;
  }
}
