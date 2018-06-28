import { Component, OnDestroy } from '@angular/core';
import { DatePipe } from "@angular/common";
import { ActivatedRoute } from '@angular/router';
import { ModalController } from 'ionic-angular';
import { RegisterModal } from '../register/modal/modal-register';
import { Subscription } from 'rxjs';
import { AccountService } from '../services/account.service';
import { Account } from '../services/model';

// import fade in animation
import { fadeInAnimation } from '../../shared/material/material.animations';

@Component({
  moduleId: module.id.toString(),
  selector: 'page-home',
  templateUrl: 'home.html',
  // make fade in animation available to this component
  animations: [fadeInAnimation],

  // attach the fade in animation to the host (root) element of this component
  host: { '[@fadeInAnimation]': '' }
})
export class HomePage implements OnDestroy {

  bgImage: String;
  displayName: String = '';
  isLogin: boolean;
  subscriptions: Subscription[] = [];

  constructor(
    public accountService: AccountService,
    public activatedRoute: ActivatedRoute,
    public modalCtrl: ModalController
  ) {
    this.bgImage = this.getRandomImage();
    this.isLogin = accountService.isLogin();
    if (this.isLogin) {
      this.onLogin(this.accountService.account);
    }

    // Subscriptions
    this.subscriptions.push(this.accountService.onLogin.subscribe(account => this.onLogin(account)));
    this.subscriptions.push(this.accountService.onLogout.subscribe(() => this.onLogout()));
  };

  ngOnDestroy() {
    this.subscriptions.forEach(s => s.unsubscribe());
    this.subscriptions = [];
  }

  onLogin(account: Account) {
    //console.debug('[home] Logged account: ', account);
    this.isLogin = true;
    this.displayName = account &&
      ((account.firstName && (account.firstName + " ") || "") +
        (account.lastName || "")) || "";
  }

  onLogout() {
    //console.log('[home] Logout');
    this.isLogin = false;
    this.displayName = "";
  }

  getRandomImage() {

    var imageCount = 7;
    const kind = 'ray';

    if (imageCount == 0) return this.getRandomImage();
    var imageIndex = Math.floor(Math.random() * imageCount) + 1;
    return './assets/img/bg/' + kind + '-' + imageIndex + '.jpg';
  }

  register() {
    let modal = this.modalCtrl.create(RegisterModal);
    modal.present();
  }

  logout(event: any) {
    this.accountService.logout();
  }

}
