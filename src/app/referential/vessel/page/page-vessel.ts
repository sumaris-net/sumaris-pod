import {AfterViewInit, ChangeDetectionStrategy, Component, Injector, OnInit, ViewChild} from '@angular/core';
import {VesselService} from '../../services/vessel-service';
import {VesselForm} from '../form/form-vessel';
import {fromDateISOString, isNotNil, toDateISOString, Vessel} from '../../services/model';
import {AccountService} from "../../../core/services/account.service";
import {AppEditorPage} from "../../../core/form/editor-page.class";
import {AbstractControl, FormControl, FormGroup, ValidationErrors, ValidatorFn, Validators} from "@angular/forms";
import {DateFormatPipe, EditorDataServiceLoadOptions} from "../../../shared/shared.module";
import * as moment from "moment";
import {VesselFeaturesHistoryComponent} from "./vessel-features-history.component";
import {VesselRegistrationHistoryComponent} from "./vessel-registration-history.component";
import {SharedValidators} from "../../../shared/validator/validators";
import {Moment} from "moment";
import {control} from "leaflet";
import {DateAdapter} from "@angular/material";
import {TranslateService} from "@ngx-translate/core";

@Component({
  selector: 'page-vessel',
  templateUrl: './page-vessel.html',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class VesselPage extends AppEditorPage<Vessel> implements OnInit, AfterViewInit {

  previousData: Vessel;
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

  @ViewChild('vesselForm', { static: true }) private vesselForm: VesselForm;

  @ViewChild('featuresHistoryTable', { static: true }) private featuresHistoryTable: VesselFeaturesHistoryComponent;

  @ViewChild('registrationHistoryTable', { static: true }) private registrationHistoryTable: VesselRegistrationHistoryComponent;

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
  }

  ngOnInit() {
    // Make sure template has a form
    if (!this.form) throw "[VesselPage] no form for value setting";
    this.form.disable();

    super.ngOnInit();
  }

  ngAfterViewInit(): void {

    this.registerSubscription(
      this.onRefresh.subscribe(() => {
          this.featuresHistoryTable.setFilter({vesselId: this.data.id});
          this.registrationHistoryTable.setFilter({vesselId: this.data.id});
        }
      )
    );

  }

  protected registerFormsAndTables() {
    this.registerForm(this.vesselForm); //.registerTables([this.featuresHistoryTable, this.registrationHistoryTable]);
  }

  protected async onNewEntity(data: Vessel, options?: EditorDataServiceLoadOptions): Promise<void> {
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
    this.previousData = undefined;
  }

  protected canUserWrite(data: Vessel): boolean {
    return !this.editing && this.accountService.canUserWriteDataForDepartment(data.recorderDepartment);
  }

  protected setValue(data: Vessel) {
    // Set data to form
    this.vesselForm.value = data;
  }

  protected getFirstInvalidTabIndex(): number {
    return this.vesselForm.invalid ? 0 : 0; // no other tab for now
  }

  protected async computeTitle(data: Vessel): Promise<string> {

      if (this.isNewData) {
        return await this.translate.get('VESSEL.NEW.TITLE').toPromise();
      }

      return await this.translate.get('VESSEL.EDIT.TITLE', data.features).toPromise();
  }

  async cancel(): Promise<void> {
    await this.reload();
  }

  async doReload(): Promise<void> {
    this.loading = true;
    await this.load(this.data && this.data.id);
  }

  editFeatures() {

    this.editing = true;
    this.previousData = undefined;
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
    this.previousData = Vessel.fromObject(json);

    this.form.setValue({ ...json , ...{id: null, startDate: null, endDate: null} } );

    this.form.get("startDate").setValidators(Validators.compose([
      Validators.required,
      SharedValidators.dateIsAfter(this.previousData.features.startDate,
        this.dateAdapter.format(this.previousData.features.startDate, this.translate.instant('COMMON.DATE_PATTERN')))
    ]));
    this.form.enable();

    this.form.get("registration").disable();
    this.form.get("statusId").disable();
  }

  editRegistration() {

    this.editing = true;
    this.previousData = undefined;
    this.form.enable();

    // disable registration start date
    this.form.get("registration.startDate").disable();

    // disable features controls
    this.form.get("features").disable();
    this.form.get("statusId").disable();

  }

  newRegistration() {

    this.isNewRegistration = true;

    const json = this.form.value;
    this.previousData = Vessel.fromObject(json);

    this.form.setValue({ ...json , ...{registrationId: null, registrationCode: null, registrationStartDate: null, registrationEndDate: null} } );

    this.form.get("registrationStartDate").setValidators(Validators.compose([
      Validators.required,
      SharedValidators.dateIsAfter(this.previousData.registration.startDate,
        this.dateAdapter.format(this.previousData.registration.startDate, this.translate.instant('COMMON.DATE_PATTERN')))
    ]));
    this.form.enable();

    this.form.get("features").disable();
    this.form.get("statusId").disable();

  }

  editStatus() {

    this.editing = true;
    this.previousData = undefined;
    this.form.enable();

    // disable features controls
    this.form.get("features").disable();
    this.form.get("registration").disable();
  }

  protected getJsonValueToSave(): Promise<any> {
    this.form.enable();
    return super.getJsonValueToSave();
  }

  async save(event): Promise<boolean> {

    // save previous form first
    if (this.previousData && (this.isNewFeatures || this.isNewRegistration)) {

      // save previous features
      if (this.isNewFeatures) {

        // set end date = new start date - 1
        const newStartDate = fromDateISOString(this.form.get("features.startDate").value);
        newStartDate.subtract(1, "seconds");
        this.previousData.features.endDate = newStartDate;

      } else if (this.isNewRegistration) {

        // set registration end date = new registration start date - 1
        const newRegistrationStartDate = fromDateISOString(this.form.get("registration.startDate").value);
        newRegistrationStartDate.subtract(1, "seconds");
        this.previousData.registration.endDate = newRegistrationStartDate;

      }

      this.saving = true;
      try {

        // save previous data first
        const saved = await this.vesselService.save(this.previousData);

        // copy update date to new data
        this.form.get('updateDate').setValue(toDateISOString(saved.updateDate));

      } catch (err) {
        console.error(err);
        this.error = err && err.message || err;
        return false;
      } finally {
        this.saving = false;
      }
    }

    // then save new data
    return super.save(event);
  }
}
