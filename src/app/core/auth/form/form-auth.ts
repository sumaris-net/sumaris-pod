import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  EventEmitter,
  Inject,
  OnInit,
  Output
} from "@angular/core";
import {FormBuilder, Validators} from "@angular/forms";
import {ModalController} from "@ionic/angular";
import {RegisterModal} from '../../register/modal/modal-register';
import {AuthData} from "../../services/account.service";
import {NetworkService} from "../../services/network.service";
import {LocalSettingsService} from "../../services/local-settings.service";
import {slideUpDownAnimation} from "../../../shared/material/material.animations";
import {PlatformService} from "../../services/platform.service";
import {DateAdapter} from "@angular/material/core";
import {Moment} from "moment";
import {debounceTime} from "rxjs/operators";
import {AppForm} from "../../form/form.class";
import {EnvironmentService} from "../../../../environments/environment.class";


@Component({
  selector: 'app-form-auth',
  templateUrl: 'form-auth.html',
  styleUrls: ['./form-auth.scss'],
  animations: [slideUpDownAnimation],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AuthForm extends AppForm<AuthData> implements OnInit {

  loading = false;
  readonly mobile: boolean;
  canWorkOffline = false;
  showPwd = false;

  @Output()
  onCancel = new EventEmitter<any>();

  @Output()
  onSubmit = new EventEmitter<AuthData>();

  disable(opts?: { onlySelf?: boolean; emitEvent?: boolean }) {
    super.disable(opts);
    this.showPwd = false; // Hide pwd when disable (e.g. when submitted)
  }

  constructor(
    platform: PlatformService,
    dateAdapter: DateAdapter<Moment>,
    formBuilder: FormBuilder,
    settings: LocalSettingsService,
    private modalCtrl: ModalController,
    public network: NetworkService,
    private cd: ChangeDetectorRef,
    @Inject(EnvironmentService) protected environment
  ) {
    super(dateAdapter,
      formBuilder.group({
      username: [null, Validators.compose([Validators.required, Validators.email])],
      password: [null, Validators.required],
      offline: [network.offline]
    }), settings);

    this.mobile = platform.mobile;
    this.canWorkOffline = this.settings.hasOfflineFeature();
    this._enable = true;
  }

  ngOnInit() {
    super.ngOnInit();
    // For DEV only
    if (this.environment.production === false) {
      this.form.patchValue({
        username: 'admin@sumaris.net',
        password: 'admin'
      });
    }
  }

  cancel() {
    this.onCancel.emit();
  }

  doSubmit(event?: UIEvent) {
    if (event) {
      event.preventDefault();
      event.stopPropagation();
    }

    if (this.form.invalid || this.loading) return;

    this.loading = true;
    const data = this.form.value;
    this.showPwd = false; // Hide password
    this.error = null; // Reset error

    this.registerSubscription(
      this.onSubmit
        .pipe(debounceTime(500))
        .subscribe(res => {
          this.loading = false;
        }));

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
        component: RegisterModal,
        backdropDismiss: false
      });
      return modal.present();
    }, 200);
  }

  /* -- protected functions -- */

  protected markForCheck() {
    this.cd.markForCheck();
  }
}
