import {
  AfterViewInit,
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  Input,
  OnDestroy,
  OnInit,
  ViewChild
} from "@angular/core";
import {ModalController} from "@ionic/angular";
import {BehaviorSubject, Subject, Subscription} from "rxjs";
import {AppFormUtils} from "../../core/form/form.utils";
import {TranslateService} from "@ngx-translate/core";
import {AggregatedLandingForm, AggregatedLandingFormOption} from "./aggregated-landing.form";
import {AggregatedLanding} from "../services/model/aggregated-landing.model";
import {referentialToString} from "../../core/core.module";

@Component({
  selector: 'app-aggregated-landing-modal',
  templateUrl: './aggregated-landing.modal.html',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AggregatedLandingModal implements OnInit, OnDestroy, AfterViewInit {

  loading = false;
  subscription = new Subscription();
  $title = new BehaviorSubject<string>('');

  @ViewChild('form', {static: false}) form: AggregatedLandingForm;

  @Input() data: AggregatedLanding;
  @Input() options: AggregatedLandingFormOption;

  get disabled() {
    return !this.form ? true : this.form.disabled;
  }

  get canValidate(): boolean {
    return !this.loading && this.form ? (this.form.enabled && this.form.dirty) : false;
  }

  constructor(
    protected viewCtrl: ModalController,
    protected translate: TranslateService,
    protected cd: ChangeDetectorRef
  ) {

  }

  ngOnInit(): void {
  }

  ngAfterViewInit(): void {

    this.loading = true;

    // setTimeout(() => {
      this.enable();
      this.form.data = this.data;
      this.updateTitle();

      this.loading = false;
    // });

  }

  addActivity() {
    this.form.addActivity();
  }

  protected async updateTitle() {
    const title = await this.translate.get(
      'AGGREGATED_LANDING.TITLE',
      {vessel: referentialToString(this.data?.vesselSnapshot, ['exteriorMarking', 'name'])}
    ).toPromise();
    this.$title.next(title);
  }

  async onSave(event: any): Promise<any> {

    // Avoid multiple call
    if (this.disabled) return;

    await AppFormUtils.waitWhilePending(this.form);

    if (this.form.invalid) {
      AppFormUtils.logFormErrors(this.form.form);
      return;
    }

    this.loading = true;

    try {
      const value = this.form.data;
      this.disable();
      await this.viewCtrl.dismiss(value);
      this.form.error = null;
    } catch (err) {
      this.form.error = err && err.message || err;
      this.enable();
      this.loading = false;
    }
  }

  disable() {
    this.form.disable();
  }

  enable() {
    this.form.enable();
  }

  cancel() {
    this.viewCtrl.dismiss();
  }

  ngOnDestroy(): void {
    this.subscription.unsubscribe();
  }

  markForCheck() {
    this.cd.markForCheck();
  }

}
