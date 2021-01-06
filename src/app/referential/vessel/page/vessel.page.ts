import {AfterViewInit, ChangeDetectionStrategy, Component, Injector, OnInit, ViewChild} from '@angular/core';
import {VesselService} from '../../services/vessel-service';
import {VesselForm} from '../form/form-vessel';
import {Vessel} from '../../services/model/vessel.model';
import {AccountService} from "../../../core/services/account.service";
import {AppEntityEditor} from "../../../core/form/editor.class";
import {FormGroup, Validators} from "@angular/forms";
import {DateFormatPipe, EntityServiceLoadOptions, isNotNil} from "../../../shared/shared.module";
import * as moment from "moment";
import {VesselFeaturesHistoryComponent} from "./vessel-features-history.component";
import {VesselRegistrationHistoryComponent} from "./vessel-registration-history.component";
import {SharedValidators} from "../../../shared/validator/validators";
import {HistoryPageReference} from "../../../core/services/model/history.model";

@Component({
  selector: 'app-vessel-page',
  templateUrl: './vessel.page.html',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class VesselPage extends AppEntityEditor<Vessel, VesselService> implements OnInit, AfterViewInit {

  previousVessel: Vessel;
  isNewFeatures = false;
  isNewRegistration = false;
  private _editing = false;
  get editing(): boolean {
    return this._editing || this.isNewFeatures || this.isNewRegistration;
  }

  set editing(value: boolean) {
    if (!value) {
      this.isNewFeatures = false;
      this.isNewRegistration = false;
    }
    this._editing = value;
  }

  @ViewChild('vesselForm', {static: true}) private vesselForm: VesselForm;

  @ViewChild('featuresHistoryTable', {static: true}) private featuresHistoryTable: VesselFeaturesHistoryComponent;

  @ViewChild('registrationHistoryTable', {static: true}) private registrationHistoryTable: VesselRegistrationHistoryComponent;

  protected get form(): FormGroup {
    return this.vesselForm.form;
  }

  constructor(
    private injector: Injector,
    private accountService: AccountService,
    private vesselService: VesselService,
    private dateAdapter: DateFormatPipe
  ) {
    super(injector, Vessel, vesselService);
    this.defaultBackHref = '/referential/vessels';
  }

  ngOnInit() {
    // Make sure template has a form
    if (!this.form) throw new Error("No form for value setting");
    this.form.disable();

    super.ngOnInit();
  }

  ngAfterViewInit() {
    super.ngAfterViewInit();

    this.registerSubscription(
      this.onUpdateView.subscribe(() => {
          this.featuresHistoryTable.setFilter({vesselId: this.data.id});
          this.registrationHistoryTable.setFilter({vesselId: this.data.id});
        }
      )
    );

  }

  protected registerForms() {
    this.addChildForm(this.vesselForm);
  }

  protected async onNewEntity(data: Vessel, options?: EntityServiceLoadOptions): Promise<void> {
    // If is on field mode, fill default values
    if (this.isOnFieldMode) {
      data.features.startDate = moment();
      data.registration.startDate = moment();
    }
  }

  updateViewState(data: Vessel, opts?: { onlySelf?: boolean; emitEvent?: boolean }) {
    super.updateViewState(data, opts);

    this.form.disable();
    this.editing = false;
    this.previousVessel = undefined;
  }

  canUserWrite(data: Vessel): boolean {
    return !this.editing && this.accountService.canUserWriteDataForDepartment(data.recorderDepartment);
  }

  protected setValue(data: Vessel) {
    // Set data to form
    this.vesselForm.value = data;
  }

  protected getFirstInvalidTabIndex(): number {
    return this.vesselForm.invalid ? 0 : -1;
  }

  protected async computeTitle(data: Vessel): Promise<string> {

    if (this.isNewData) {
      return await this.translate.get('VESSEL.NEW.TITLE').toPromise();
    }

    return await this.translate.get('VESSEL.EDIT.TITLE', data.features).toPromise();
  }

  protected async computePageHistory(title: string): Promise<HistoryPageReference> {
    return {
      ...(await super.computePageHistory(title)),
      icon: 'boat',
      subtitle: 'MENU.VESSELS'
    };
  }

  async cancel(): Promise<void> {
    await this.reloadWithConfirmation();
  }

  async reload(): Promise<void> {
    this.loading = true;
    await this.load(this.data && this.data.id);
  }

  editFeatures() {

    this.editing = true;
    this.previousVessel = undefined;
    this.form.enable();

    // disable start date
    this.form.get("features.startDate").disable();

    // disable registration controls
    this.form.get("registration").disable();
    this.form.get("statusId").disable();
  }

  newFeatures() {

    this.isNewFeatures = true;

    const json = this.form.value;
    this.previousVessel = Vessel.fromObject(json);

    this.form.setValue({...json, ...{features: { ...json.features, id: null, startDate: null, endDate: null}}});

    this.form.get("features.startDate").setValidators([
      Validators.required,
      SharedValidators.dateIsAfter(this.previousVessel.features.startDate,
        this.dateAdapter.format(this.previousVessel.features.startDate, this.translate.instant('COMMON.DATE_PATTERN')))
    ]);
    this.form.enable();

    this.form.get("registration").disable();
    this.form.get("statusId").disable();
  }

  editRegistration() {

    this.editing = true;
    this.previousVessel = undefined;
    this.form.enable();

    // disable registration start date, if already exists (must not change it)
    const registrationStartDate = this.form.get("registration.startDate").value;
    if (isNotNil(registrationStartDate)) {
      this.form.get("registration.startDate").disable();
    }

    // disable features controls
    this.form.get("features").disable();
    this.form.get("vesselType").disable();
    this.form.get("statusId").disable();

  }

  newRegistration() {

    this.isNewRegistration = true;

    const json = this.form.value;
    this.previousVessel = Vessel.fromObject(json);

    this.form.setValue({
      ...json, ...{
        registration: {
          ...json.registration,
          id: null,
          registrationCode: null,
          startDate: null,
          endDate: null
        }
      }
    });

    this.form.get("registration.startDate").setValidators([
      Validators.required,
      SharedValidators.dateIsAfter(this.previousVessel.registration.startDate,
        this.dateAdapter.format(this.previousVessel.registration.startDate, this.translate.instant('COMMON.DATE_PATTERN')))
    ]);
    this.form.enable();

    this.form.get("features").disable();
    this.form.get("vesselType").disable();
    this.form.get("statusId").disable();

  }

  editStatus() {

    this.editing = true;
    this.previousVessel = undefined;
    this.form.enable();

    // disable features controls
    this.form.get("features").disable();
    this.form.get("registration").disable();
    this.form.get("vesselType").disable();
  }

  async save(event, options?: any): Promise<boolean> {
    const res = await super.save(event, {
      previousVessel: this.previousVessel,
      isNewFeatures: this.isNewFeatures,
      isNewRegistration: this.isNewRegistration
    });
    return res;
  }

  protected getJsonValueToSave(): Promise<any> {
    return this.form.getRawValue();
  }
}
