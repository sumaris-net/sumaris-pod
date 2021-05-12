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
import {AlertController, ModalController} from "@ionic/angular";
import {BehaviorSubject, Subject, Subscription} from "rxjs";
import {AppFormUtils} from "../../core/form/form.utils";
import {TranslateService} from "@ngx-translate/core";
import {AggregatedLandingForm, AggregatedLandingFormOption} from "./aggregated-landing.form";
import {AggregatedLanding, VesselActivity} from "../services/model/aggregated-landing.model";
import {Alerts} from "../../shared/alerts";
import {referentialToString} from "../../core/services/model/referential.model";
import {isNil} from "../../shared/functions";

@Component({
  selector: 'app-aggregated-landing-modal',
  templateUrl: './aggregated-landing.modal.html',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AggregatedLandingModal implements OnInit, OnDestroy {

  loading = true;
  subscription = new Subscription();
  $title = new BehaviorSubject<string>('');

  @ViewChild('form', {static: true}) form: AggregatedLandingForm;

  @Input() data: AggregatedLanding;
  @Input() options: AggregatedLandingFormOption;

  get disabled() {
    return !this.form ? true : this.form.disabled;
  }

  get canValidate(): boolean {
    return !this.loading && this.dirty;
  }

  get dirty(): boolean {
    return this.form ? (this.form.enabled && this.form.dirty) : false
  }

  constructor(
    protected viewCtrl: ModalController,
    protected alertCtrl: AlertController,
    protected translate: TranslateService,
    protected cd: ChangeDetectorRef
  ) {
  }

  ngOnInit(): void {
    this.enable();
    this.form.data = this.data;
    this.updateTitle();

    this.loading = false;
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
      const value = {
        aggregatedLanding: this.form.data,
        saveOnDismiss: false,
        tripToOpen: undefined
      };
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
    this.viewCtrl.dismiss({
      aggregatedLanding: undefined,
      saveOnDismiss: false,
      tripToOpen: undefined
    });
  }

  ngOnDestroy(): void {
    this.subscription.unsubscribe();
  }

  markForCheck() {
    this.cd.markForCheck();
  }

  async openTrip($event: { activity: VesselActivity }) {
    if (!$event || !$event.activity)
      return;

    let saveBeforeLeave: boolean;
    if (this.dirty) {
      console.warn("The activity is dirty, must save first");

      saveBeforeLeave = await Alerts.askSaveBeforeLeave(this.alertCtrl, this.translate);
      if (isNil(saveBeforeLeave)) {
        // user cancel
        return;
      }
    }
    // set last activity
    this.viewCtrl.dismiss({
      aggregatedLanding: undefined,
      saveOnDismiss: saveBeforeLeave,
      tripToOpen: $event.activity
    });
  }
}
