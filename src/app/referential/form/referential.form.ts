import {AppForm, Referential, StatusIds} from "../../core/core.module";
import {DateAdapter} from "@angular/material/core";
import {Moment} from "moment";
import {ReferentialValidatorService} from "../services/referential.validator";
import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnInit} from "@angular/core";
import {ValidatorService} from "angular4-material-table";
import {DefaultStatusList, StatusValue} from "../../core/services/model";

@Component({
  selector: 'app-referential-form',
  templateUrl: './referential.form.html',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ReferentialForm extends AppForm<Referential> implements OnInit {

  private _statusList = DefaultStatusList;
  statusById: { [id: number]: StatusValue; };

  @Input() showError = true;
  @Input() entityName;

  @Input()
  set statusList(values: StatusValue[]) {
    this._statusList = values;

    // Fill statusById
    this.statusById = {};
    this.statusList.forEach((status) => this.statusById[status.id] = status);
  }

  get statusList(): StatusValue[] {
    return this._statusList;
  }

  constructor(
    protected dateAdapter: DateAdapter<Moment>,
    protected validatorService: ReferentialValidatorService,
    protected cd: ChangeDetectorRef
  ) {
    super(dateAdapter, validatorService.getFormGroup());

  }

  ngOnInit() {
    super.ngOnInit();

    // Fill statusById
    if (this._statusList && !this.statusById) {
      this.statusById = {};
      this._statusList.forEach((status) => this.statusById[status.id] = status);
    }
  }

  protected markForCheck() {
    this.cd.markForCheck();
  }
}
