import {AfterViewInit, Component, Input, OnDestroy, OnInit, ViewChild} from "@angular/core";
import {ModalController} from "@ionic/angular";
import {BehaviorSubject, Subject, Subscription} from "rxjs";
import {AppFormUtils} from "../../core/form/form.utils";
import {TranslateService} from "@ngx-translate/core";
import {AggregatedLandingForm, AggregatedLandingFormOption} from "./aggregated-landing.form";
import {AggregatedLanding} from "../services/model/aggregated-landing.model";
import {referentialToString} from "../../core/core.module";

@Component({
  selector: 'app-aggregated-landing-modal',
  templateUrl: './aggregated-landing.modal.html'
})
export class AggregatedLandingModal implements OnInit, OnDestroy, AfterViewInit {

  loading = false;
  subscription = new Subscription();
  $title = new BehaviorSubject<string>('');

  @ViewChild('form', {static: true}) form: AggregatedLandingForm;

  @Input() data: AggregatedLanding;
  @Input() options: AggregatedLandingFormOption;

  get disabled() {
    return this.form.disabled;
  }

  get enabled() {
    return this.form.enabled;
  }

  get valid() {
    return this.form && this.form.valid || false;
  }

  constructor(
    protected viewCtrl: ModalController,
    protected translate: TranslateService
  ) {

  }

  ngOnInit(): void {
  }

  ngAfterViewInit(): void {

    setTimeout(() => {
      this.enable();
      this.form.setValue(this.data);
      this.updateTitle();
    });

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
      const value = this.form.value;
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

}
