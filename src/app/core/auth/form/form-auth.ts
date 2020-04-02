import {Component, EventEmitter, OnInit, Output} from "@angular/core";
import {FormBuilder, FormGroup, Validators} from "@angular/forms";
import {ModalController} from "@ionic/angular";
import {RegisterModal} from '../../register/modal/modal-register';
import {AuthData} from "../../services/account.service";
import {environment} from "../../../../environments/environment";
import {NetworkService} from "../../services/network.service";
import {LocalSettingsService} from "../../services/local-settings.service";
import {fadeInAnimation, slideUpDownAnimation} from "../../../shared/material/material.animations";


@Component({
  selector: 'app-form-auth',
  templateUrl: 'form-auth.html',
  styleUrls: ['./form-auth.scss'],
  animations: [slideUpDownAnimation]
})
export class AuthForm implements OnInit {

  loading = false;
  form: FormGroup;
  error: string = null;
  canWorkOffline = false;

  public get value(): AuthData {
    return this.form.value;
  }

  public get valid(): boolean {
    return this.form.valid;
  }

  public get invalid(): boolean {
    return this.form.invalid;
  }

  @Output()
  onCancel: EventEmitter<any> = new EventEmitter<any>();

  @Output()
  onSubmit: EventEmitter<AuthData> = new EventEmitter<AuthData>();

  constructor(
    public formBuilder: FormBuilder,
    public modalCtrl: ModalController,
    public network: NetworkService,
    public settings: LocalSettingsService
  ) {
    this.form = formBuilder.group({
      username: [null, Validators.compose([Validators.required, Validators.email])],
      password: [null, Validators.required],
      offline: [network.offline]
    });

    this.canWorkOffline = this.settings.hasOfflineFeature();
  }

  ngOnInit() {
    // For DEV only
    if (environment.production === false) {
      this.form.patchValue({
        username: 'admin@sumaris.net',
        password: 'admin'
      });
    }
  }

  cancel() {
    this.onCancel.emit();
  }

  doSubmit(event: any) {
    if (this.form.invalid || this.loading) return;

    this.loading = true;
    const data = this.form.value;
    this.error = null;
    this.onSubmit
      .subscribe(res => {
        setTimeout(() => {
          this.loading = false;
        }, 500);
      });

    setTimeout(() => this.onSubmit.emit({
      username: data.username,
      password: data.password,
      offline: data.offline
    }));
  }

  register() {
    this.onCancel.emit();
    setTimeout(async () => {
      const modal = await this.modalCtrl.create({
        component: RegisterModal
      });
      return modal.present();
    }, 200);
  }
}
