// Auth
import { Injectable } from '@angular/core';
import { CanActivate, ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { Observable } from 'rxjs/Observable';
import { ModalController } from "ionic-angular";
import { AuthModal } from "../auth/modal/modal-auth";
import { AccountService } from "./account.service";
import { Router } from '@angular/router';

@Injectable()
export class AuthGuardService implements CanActivate {
  constructor(private accountService: AccountService,
    private modalCtrl: ModalController,
    private router: Router
  ) { }

  canActivate(
    next: ActivatedRouteSnapshot,
    state: RouterStateSnapshot
  ): Observable<boolean> | Promise<boolean> | boolean {

    if (!this.accountService.isLogin()) {
      console.debug("[auth-gard] Need authentication for page /" + next.url.join('/'));
      return this.login(next)
        .then(res => {
          if (!res) {
            console.debug("[auth-gard] Authentication cancelled. Could not access to /" + next.url.join('/'));
            this.redirectToHome();
          }
          return res;
        });
    } else {
      console.debug("[auth-gard] Authorized access to /" + next.url.join('/'));
      return true;
    }
  }

  login(next?: ActivatedRouteSnapshot): Promise<boolean> {
    return new Promise<boolean>((resolve) => {
      let modal = this.modalCtrl.create(AuthModal, { next: next });
      modal.onDidDismiss(() => {
        if (this.accountService.isLogin()) {
          resolve(true);
          return;
        }
        console.debug("[auth-gard] Authentication cancelled.");
        resolve(false);
      });
      return modal.present();
    });
  }

  redirectToHome() {
    this.router.navigate(['/home']);
  }
}
