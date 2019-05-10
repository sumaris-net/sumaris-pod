import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Input} from '@angular/core';
import {ModalController} from '@ionic/angular';
import {Peer} from "../services/model";
import {Observable, Subject} from "rxjs";
import {fadeInAnimation} from "../../shared/material/material.animations";
import {HttpClient} from "@angular/common/http";

@Component({
  selector: 'select-peer-modal',
  templateUrl: 'select-peer.modal.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  animations: [fadeInAnimation]
})
export class SelectPeerModal {

  loading = true;
  $peers = new Subject<Peer[]>();

  @Input() canCancel = true;
  @Input() allowSelectDownPeer = true;

  set peers(peers: Observable<Peer[]>) {
    peers.subscribe(res => this.refreshPeers(res));
  }


  constructor(
    private http: HttpClient,
    private viewCtrl: ModalController,
    private cd: ChangeDetectorRef
  ) {
  }

  cancel() {
    this.viewCtrl.dismiss();
  }

  selectPeer(item: Peer) {
    if (this.allowSelectDownPeer || item.reachable) {
      console.debug("[select-peer-modal] User select the peer:", item);
      this.viewCtrl.dismiss(item);
    }
  }

  async refreshPeers(peers: Peer[]) {
    peers = peers || [];

    const data: Peer[] = [];
    const jobs = Promise.all(
      peers.map(async (peer) => {
        await this.refreshPeer(peer);
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

    this.$peers
      // .pipe(
      //   debounceTime(500)
      // )
      .subscribe(() => {
        this.cd.markForCheck();
      });

    try {
      await jobs;
    }
    catch(err) {
      console.error(err);
    }
    this.loading = false;
    this.cd.markForCheck();
  }

  protected async refreshPeer(peer: Peer): Promise<Peer> {
    const uri = peer.url + '/api/node/info';
    try {
      const summary: any = await this.http.get(uri).toPromise();
      peer.status = 'UP';
      peer.softwareName = summary.softwareName;
      peer.softwareVersion = summary.softwareVersion;
      peer.label = summary.nodeLabel;
    } catch (err) {
      console.error(`[select-peer] Could not access to {${uri}}: ${err && err.statusText}`);
      peer.status = 'DOWN';
    }
    return peer;
  }
}
