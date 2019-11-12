import {AfterViewInit, Component, Injector, OnInit, ViewChild} from '@angular/core';
import {VesselService} from '../../services/vessel-service';
import {VesselForm} from '../form/form-vessel';
import {VesselFeatures} from '../../services/model';
import {AccountService} from "../../../core/services/account.service";
import {AppEditorPage} from "../../../core/form/editor-page.class";
import {FormGroup} from "@angular/forms";
import {EditorDataServiceLoadOptions} from "../../../shared/shared.module";
import * as moment from "moment";
import {VesselFeaturesHistoryComponent} from "./vessel-features-history.component";

@Component({
  selector: 'page-vessel',
  templateUrl: './page-vessel.html'
})
export class VesselPage extends AppEditorPage<VesselFeatures> implements OnInit, AfterViewInit {

  @ViewChild('vesselForm', { static: true }) private vesselForm: VesselForm;

  @ViewChild('featuresHistoryTable', { static: true }) private featuresHistoryTable: VesselFeaturesHistoryComponent;

  protected get form(): FormGroup {
    return this.vesselForm.form;
  }

  constructor(
    private injector: Injector,
    private accountService: AccountService,
    private vesselService: VesselService
  ) {
    super(injector, VesselFeatures, vesselService);
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
          this.featuresHistoryTable.setFilter({vesselId: this.data.vesselId});
        }
      )
    );

  }

  protected registerFormsAndTables() {
    this.registerForm(this.vesselForm).registerTable(this.featuresHistoryTable);
  }

  protected async onNewEntity(data: VesselFeatures, options?: EditorDataServiceLoadOptions): Promise<void> {
    // If is on field mode, fill default values
    if (this.isOnFieldMode) {
      data.startDate = moment();
    }
  }

  protected canUserWrite(data: VesselFeatures): boolean {
    return this.accountService.canUserWriteDataForDepartment(data.recorderDepartment);
  }

  protected setValue(data: VesselFeatures) {
    // Set data to form
    this.vesselForm.value = data;
  }

  protected getFirstInvalidTabIndex(): number {
    return this.vesselForm.invalid ? 0 : 0; // no other tab for now
  }

  protected async computeTitle(data: VesselFeatures): Promise<string> {

      if (this.isNewData) {
        return await this.translate.get('VESSEL.NEW.TITLE').toPromise();
      }

      return await this.translate.get('VESSEL.EDIT.TITLE', data).toPromise();
  }

}
