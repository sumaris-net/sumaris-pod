import { Component, ViewChild } from '@angular/core';
import { ModalController } from '@ionic/angular';
import { AccountService } from "../../services/account.service";
import { AuthForm } from '../form/form-auth';
import {firstNotNil, firstNotNilPromise} from "../../../shared/observables";

@Component({
  templateUrl: 'modal-auth.html',
  styleUrls: ['./modal-auth.scss']
})
export class AuthModal {

  loading = false;

  @ViewChild('form', { static: true }) private form: AuthForm;

  constructor(private accountService: AccountService,
    public viewCtrl: ModalController) {
  }

  cancel() {
    this.viewCtrl.dismiss();
  }

  async doSubmit(): Promise<any> {
    if (!this.form.valid) return;
    this.loading = true;

    try {
      const data = this.form.value;
      const account = await this.accountService.login(data);
      return this.viewCtrl.dismiss(account);
    }
    catch (err) {
      this.loading = false;
      this.form.error = err && err.message || err;
      firstNotNilPromise(this.form.form.valueChanges).then(() => {
        this.form.error = null;
      });
      return;
    }
  }
}
