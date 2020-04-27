import {
  AfterViewInit,
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  Input,
  OnInit,
  ViewChild
} from "@angular/core";
import {AlertController, ModalController} from "@ionic/angular";
import {AcquisitionLevelCodes, isNil} from "../../referential/services/model";
import {PhysicalGearForm} from "./physicalgear.form";
import {BehaviorSubject} from "rxjs";
import {TranslateService} from "@ngx-translate/core";
import {PlatformService} from "../../core/services/platform.service";
import {Alerts} from "../../shared/alerts";
import {PhysicalGear, Trip} from "../services/model/trip.model";
import {AppTabPage, TableSelectColumnsComponent} from "../../core/core.module";
import {FormBuilder, FormControl, FormGroup, Validators} from "@angular/forms";
import {SelectPhysicalGearModal} from "./select-physicalgear.modal";
import {PhysicalGearFilter} from "../services/physicalgear.service";

@Component({
  selector: 'app-physical-gear-modal',
  templateUrl: './physicalgear.modal.html',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class PhysicalGearModal implements OnInit, AfterViewInit {

  loading = false;
  originalData: PhysicalGear;
  $title = new BehaviorSubject<string>(undefined);
  selectModalFilter: PhysicalGearFilter;


  @Input() acquisitionLevel: string;

  @Input() program: string;

  @Input() disabled = false;

  @Input() isNew = false;

  @Input() set value(value: PhysicalGear) {
    this.originalData = value;
  }

  @Input() mobile: boolean;

  @Input() parent: Trip;


  @ViewChild('form', {static: true}) form: PhysicalGearForm;

  constructor(
    protected alertCtrl: AlertController,
    protected viewCtrl: ModalController,
    protected translate: TranslateService,
    protected platform: PlatformService,
    protected cd: ChangeDetectorRef
  ) {

    // Default values
    this.acquisitionLevel = AcquisitionLevelCodes.PHYSICAL_GEAR;
    this.mobile = platform.mobile;
  }

  ngOnInit() {
    if (this.disabled) {
      this.form.disable();
    }

    this.form.value = this.originalData || new PhysicalGear();

    if (this.parent && this.parent instanceof Trip) {
      this.selectModalFilter =  {
        vesselId: this.parent.vesselSnapshot && this.parent.vesselSnapshot.id,
        endDate: this.parent.departureDateTime || this.parent.returnDateTime
        // TODO: startDate = endDate - 6 month ?
      };
    }

    // Compute the title
    this.computeTitle();

  }

  ngAfterViewInit(): void {
    // Focus on the first field, is not in mobile
     if (this.isNew && !this.mobile) {
       setTimeout(() => this.form.focusFirstInput(), 400);
     }
  }

  async onCopyClick(event) {

    const modal = await this.viewCtrl.create({
      component: SelectPhysicalGearModal,
      componentProps: {
        filter: this.selectModalFilter,
        program: this.program,
        acquisitionLevel: this.acquisitionLevel
      }
    });

    // Open the modal
    await modal.present();

    // On dismiss
    const res = await modal.onDidDismiss();

    console.log(res);

    if (!res || !res.data) return; // CANCELLED
  }


  async cancel(event: UIEvent) {
    await this.saveIfDirtyAndConfirm(event);

    // Continue (if event not cancelled)
    if (!event.defaultPrevented) {
      await this.viewCtrl.dismiss();
    }
  }

  async save(event?: UIEvent): Promise<boolean> {
    if (!this.form.valid || this.loading) return false;
    this.loading = true;

    // Nothing to save: just leave
    if (!this.form.dirty) {
      await this.viewCtrl.dismiss();
      return false;
    }

    try {
      const gear = this.form.value;
      return await this.viewCtrl.dismiss(gear);
    }
    catch (err) {
      this.loading = false;
      this.form.error = err && err.message || err;
      return false;
    }
  }

  /* -- protected functions -- */

  protected async saveIfDirtyAndConfirm(event: UIEvent): Promise<void> {
    if (!this.form.dirty) return; // skip, if nothing to save

    const confirmation = await Alerts.askSaveBeforeLeave(this.alertCtrl, this.translate, event);

    // User cancelled
    if (isNil(confirmation) || event && event.defaultPrevented) {
      return;
    }

    if (confirmation === false) {
      return;
    }

    // If user confirm: save
    const saved = await this.save(event);

    // Error while saving: avoid to close
    if (!saved) event.preventDefault();
  }

  protected markForCheck() {
    this.cd.markForCheck();
  }

  protected async computeTitle() {
    if (this.isNew || !this.originalData) {
      this.$title.next(await this.translate.get('TRIP.PHYSICAL_GEAR.NEW.TITLE').toPromise());
    }
    else {
      this.$title.next(await this.translate.get('TRIP.PHYSICAL_GEAR.EDIT.TITLE', this.originalData).toPromise());
    }
  }
}
