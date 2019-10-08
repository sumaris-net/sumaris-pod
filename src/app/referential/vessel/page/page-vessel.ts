import {Component, OnInit, ViewChild} from '@angular/core';
import {ActivatedRoute} from "@angular/router";
import {VesselService} from '../../services/vessel-service';
import {VesselForm} from '../form/form-vessel';
import {isNil, VesselFeatures} from '../../services/model';
import {Subject} from "rxjs";
import {TranslateService} from "@ngx-translate/core";
import {AccountService} from "../../../core/services/account.service";

@Component({
  selector: 'page-vessel',
  templateUrl: './page-vessel.html'
})
export class VesselPage implements OnInit {

  loading = true;
  data: VesselFeatures;
  $title = new Subject<string>();
  canEdit = false;

  @ViewChild('form', { static: true }) private form: VesselForm;

  constructor(
    private route: ActivatedRoute,
    private accountService: AccountService,
    private vesselService: VesselService,
    private translate: TranslateService
  ) {
  }

  ngOnInit() {
    // Make sure template has a form
    if (!this.form) throw "[VesselPage] no form for value setting";
    this.form.disable();

    this.route.params.subscribe(res => {
      const id = res && res["id"];
      if (!id || id === "new") {
        this.load();
      }
      else {
        this.load(parseInt(id));
      }
    });
  }

  async load(id?: number) {
    if (id) {
      const vessel = await this.vesselService.loadByVesselFeaturesId(id);
      this.updateView(vessel);
      this.loading = false;
    }
    else {
      this.updateView(new VesselFeatures());
      this.loading = false;
    }
  }

  updateView(data: VesselFeatures | null) {
    this.form.setValue(data);
    this.data = data;

    this.canEdit = this.accountService.canUserWriteDataForDepartment(data.recorderDepartment);

    this.computeTitle();

    this.form.markAsPristine();
    this.form.markAsUntouched();

    if (this.canEdit) {
      this.form.enable();
    } else {
      this.form.disable();
    }
  }

  async save(event?: UIEvent): Promise<any> {
    if (this.loading) return;

    // Update Vessel from JSON
    const json = this.form.value;
    this.data.fromObject(json);
    this.form.error = null;

    this.form.disable();

    try {
      const updatedData = await this.vesselService.save(this.data);
      this.updateView(updatedData);

      this.form.markAsPristine();

    } catch(err ) {
      console.error(err && err.message || err, err);
      this.form.error = err && err.message || err;
      this.form.enable();
    }
    finally {
    }
  }

  async cancel() {
    // reload
    this.loading = true;
    await this.load(this.data.id);
  }

  /* -- protected -- */

  protected async computeTitle() {
    if (isNil(this.data.id)) {
      this.$title.next(await this.translate.get('VESSEL.NEW.TITLE').toPromise());
    }
    else {
      this.$title.next(await this.translate.get('VESSEL.EDIT.TITLE', this.data).toPromise());
    }
  }
}
